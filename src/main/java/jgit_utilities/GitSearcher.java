package jgit_utilities;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import model.CommitDetails;
import model.CommitFileDetails;
import model.Issue;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import utils.Utils;
import main.RetrieveTicketsID;

public class GitSearcher {

    private GitSearcher() {
        throw new IllegalStateException("Utility class");
    }

    public static int getFileAge(RevCommit startCommit, String filename) throws IOException {
        Date dateFirstCommit;
        Date dateCurrentCommit;
        int age;
        try (Repository repository = openJGitRepository()) {
            RevWalk revWalk = new RevWalk(repository);
            revWalk.markStart( revWalk.parseCommit(repository.resolve(startCommit.getName())));
            revWalk.setTreeFilter( PathFilter.create(filename) );
            revWalk.sort( RevSort.COMMIT_TIME_DESC );
            revWalk.sort( RevSort.REVERSE, true );
            RevCommit commit = revWalk.next();
            dateFirstCommit = commit.getAuthorIdent().getWhen();
            dateCurrentCommit = startCommit.getAuthorIdent().getWhen();

            long diffInMillies = Math.abs(dateCurrentCommit.getTime() - dateFirstCommit.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
            age = (int) (diffInDays/7);
        }
        return age;
    }

    public static Repository openJGitRepository() throws IOException{
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        String repopath_f;
        try (BufferedReader br = new BufferedReader(new FileReader("path.txt"))) {
            repopath_f = br.readLine();
        }
        return builder.setGitDir(new File(repopath_f))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
    }

    public static List<CommitDetails> getAllCommitDetails(Repository repository, String filterParam, Issue issue)
            throws IOException, GitAPIException {
        Collection<Ref> allRefs = repository.getAllRefs().values();
        List<CommitDetails> listOfCommits = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        try (RevWalk revWalk = new RevWalk( repository )) {
            revWalk.setRevFilter(MessageRevFilter.create(filterParam));

            for( Ref ref : allRefs ) {
                revWalk.markStart( revWalk.parseCommit( ref.getObjectId() ));
            }

            for( RevCommit commit : revWalk ) {
                LocalDate commitLocalDate = commit.getCommitterIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                int version = RetrieveTicketsID.getVersionFromLocalDate( commitLocalDate );
                if ( issue.getFixVersion() >= version ) {
                    CommitDetails commitDetail = new CommitDetails();
                    commitDetail.setCommit(commit);
                    commitDetail.setVersion(version);
                    commitDetail.setFilesChanged(commitChanges(commit, commitDetail, issue));
                    commitDetail.setPerson(commit.getAuthorIdent());
                    commitDetail.setAdded_loc();
                    commitDetail.setDeleted_loc();
                    commitDetail.setFullMessage(commit.getFullMessage());
                    commitDetail.setCommitDate(formatter.format(commitDetail.getPerson().getWhen()));

                    listOfCommits.add(commitDetail);
                }
            }
        }
        return listOfCommits;
    }


    public static List<CommitFileDetails> commitChanges(RevCommit commit, CommitDetails commitObject, Issue issue) throws IOException, GitAPIException {
        String fileExtension = ".java";
        String repopath_f;
        try (BufferedReader br = new BufferedReader(new FileReader("path.txt"))) {
            repopath_f = br.readLine();
        }
        Git git = Git.open(new File(repopath_f));
        List<CommitFileDetails> changedFilesList = new ArrayList<>();
        int linesAdded = 0;
        int linesDeleted = 0;
        int linesReplaced = 0;

        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(openJGitRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);

        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                .call();

        for (DiffEntry diff : diffs) {
            String fileName = diff.getNewPath();
            linesAdded = 0;
            linesDeleted = 0;
            linesReplaced = 0;
            if (!fileName.endsWith(fileExtension)) {
                continue;
            }
            String fileText = getTextfromCommittedFile(commit, fileName);

            int age = getFileAge(commit, fileName);

            for (Edit edit : df.toFileHeader(diff).toEditList()) {
                if ( edit.getBeginA() < edit.getEndA() && edit.getBeginB() < edit.getEndB() ){
                    linesReplaced += edit.getEndB() - edit.getBeginB();
                }
                if ( edit.getBeginA() < edit.getEndA() && edit.getBeginB() == edit.getEndB() ){
                    linesDeleted += edit.getEndA() - edit.getBeginA();
                }
                if ( edit.getBeginA() == edit.getEndA() && edit.getBeginB() < edit.getEndB() ){
                    linesAdded += edit.getEndB() - edit.getBeginB();
                }
            }

            boolean buggy = (commitObject.getVersion() < issue.getFixVersion()) && (commitObject.getVersion() >= issue.getInjectedVersion());
            int numImports = Utils.getNumImports(fileText);
            int numComments = Utils.getNumComments(fileText);

            CommitFileDetails commitFileDetails = new CommitFileDetails(fileName, linesAdded, linesDeleted, linesReplaced,
                    Utils.countLineBufferedReader(fileText), fileText, age);

            commitFileDetails.setBuggy(buggy);
            commitFileDetails.setNumImports(numImports);
            commitFileDetails.setNumComments(numComments);

            changedFilesList.add(commitFileDetails);
        }
        df.close();
        return changedFilesList;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();

            return treeParser;
        }
    }

    public static String getTextfromCommittedFile(RevCommit commit, String filename) throws IOException {
        RevTree tree = commit.getTree();
        Repository repository = openJGitRepository();
        String fileText;

        // now try to find a specific file
        try (TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filename));
            if (!treeWalk.next()) {
                return "";
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            loader.copyTo(stream);
            fileText = stream.toString();
            return fileText;

        }
    }
}

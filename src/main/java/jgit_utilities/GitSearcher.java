package jgit_utilities;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(GitSearcher.class.getName());

    private GitSearcher() {
        throw new IllegalStateException("Utility class");
    }

    public static int getFileAge(RevCommit startCommit, String filename) throws IOException {
        /*
            Funzione che ricava l'age di un file a partire da un commit e da un @filename. Ritorna quindi l'age
            del file calcolato in settimane
        */
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
        // Funzione necessaria per l'accesso a git della libreria JGit
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        String repopathF;
        try (BufferedReader br = new BufferedReader(new FileReader("path.txt"))) {
            repopathF = br.readLine();
        }
        return builder.setGitDir(new File(repopathF))
                .readEnvironment()
                .findGitDir()
                .build();
    }

    public static List<CommitDetails> getAllCommitDetails(Repository repository, String filterParam, Issue issue)
            throws IOException, GitAPIException {
        /*
            Questa funzione effettua la logWalk andando a ricercare tra i commit quelli che nel commit message
            hanno l'ID preso attraverso issue (@filterParam).
        */
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
                /*
                    Andiamo a considerare solamente i commit tali per cui abbiamo che la fixVersion indicata sul task
                    di Jira è superiore a quella del commit
                */
                if ( issue.getFixVersion() >= version ) {
                    CommitDetails commitDetail = new CommitDetails();
                    commitDetail.setCommit(commit);
                    commitDetail.setVersion(version);
                    commitDetail.setFilesChanged(commitChanges(commit, commitDetail, issue));
                    commitDetail.setPerson(commit.getAuthorIdent());
                    commitDetail.setAddedLoc();
                    commitDetail.setDeletedLoc();
                    commitDetail.setFullMessage(commit.getFullMessage());
                    commitDetail.setCommitDate(formatter.format(commitDetail.getPerson().getWhen()));

                    // Aggiungo le informazioni alla lista di commit relativi all'issue
                    listOfCommits.add(commitDetail);
                }
            }
        }
        return listOfCommits;
    }


    public static List<CommitFileDetails> commitChanges(RevCommit commit, CommitDetails commitObject, Issue issue)
            throws IOException, GitAPIException {
        /*
            Questa funzione effettua il retrieve di tutte le informazioni necessarie al computo delle metriche
        */
        String fileExtension = ".java";
        String repopathF;

        // Andiamo a leggere il file path.txt che contiene il path al file .git che vogliamo indicare
        try (BufferedReader br = new BufferedReader(new FileReader("path.txt"))) {
            repopathF = br.readLine();
        }

        Git git = Git.open(new File(repopathF));
        List<CommitFileDetails> changedFilesList = new ArrayList<>();
        int linesAdded;
        int linesDeleted;
        int linesReplaced;

        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(openJGitRepository());
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);

        final List<DiffEntry> diffs;

        try {
            diffs = git.diff()
                    .setOldTree(prepareTreeParser(openJGitRepository(), commit.getParent(0).getId().getName()))
                    .setNewTree(prepareTreeParser(openJGitRepository(), commit.getId().getName()))
                    .call();
        }
        catch (ArrayIndexOutOfBoundsException e){
            return null;
        }

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
                switch (Utils.modifiedLocRetrieverMode(edit)) {
                    case "REPLACED" -> linesReplaced += edit.getEndB() - edit.getBeginB();
                    case "DELETED" -> linesDeleted += edit.getEndA() - edit.getBeginA();
                    case "ADDED" -> linesAdded += edit.getEndB() - edit.getBeginB();
                    default -> LOGGER.info("default in switch");
                }
            }

            boolean buggy = Utils.retrieveBugginess(commitObject.getVersion(), issue.getFixVersion(), issue.getInjectedVersion());
            int numImports = Utils.getNumImports(fileText);
            int numPublicAttributesOrMethods = Utils.getPublicAttributesOrMethods(fileText);

            CommitFileDetails commitFileDetails = new CommitFileDetails(fileName, linesAdded, linesDeleted, linesReplaced,
                    Utils.countLineBufferedReader(fileText), fileText, age);

            commitFileDetails.setBuggy(buggy);
            commitFileDetails.setNumImports(numImports);
            commitFileDetails.setNumPublicAttributerOrMethods(numPublicAttributesOrMethods);

            changedFilesList.add(commitFileDetails);
        }
        df.close();
        return changedFilesList;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // Funzione necessaria a ricavare le grandezze relative alle righe aggiunte, modificate ed eliminate nei commit
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
        /*
            Funzione che effettua il retrieve del codice effettivo del file relativo a @commit.
            Necessario per ricavare delle metriche
        */
        RevTree tree = commit.getTree();
        Repository repository = openJGitRepository();
        String fileText;

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

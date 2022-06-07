package model;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class CommitDetails {
    private PersonIdent person;
    private List<CommitFileDetails> filesChanged = new ArrayList<>();
    private RevCommit commit;
    private String fullMessage;
    private int addedLoc;
    private int deletedLoc;
    private int version;
    private String commitDate;
    private Issue issue;

    public List<CommitFileDetails> getFilesChanged() {
        return filesChanged;
    }

    public void setFilesChanged(List<CommitFileDetails> filesChanged) {
        this.filesChanged = filesChanged;
    }

    public PersonIdent getPerson() {
        return person;
    }

    public void setPerson(PersonIdent person) {
        this.person = person;
    }

    public RevCommit getCommit() {
        return commit;
    }

    public void setCommit(RevCommit commit) {
        this.commit = commit;
    }

    public int getAddedLoc() {
        return addedLoc;
    }

    public void setAdded_loc() {
        for( CommitFileDetails commitFileDetails: this.filesChanged){
            this.addedLoc += commitFileDetails.getAddedLOC();
        }
    }

    public int getDeletedLoc() {
        return deletedLoc;
    }

    public void setDeleted_loc() {
        for( CommitFileDetails commitFileDetails: this.filesChanged){
            this.deletedLoc += commitFileDetails.getDeletedLOC();
        }
    }

    public String getFullMessage() {
        return fullMessage;
    }

    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    public void setAddedLoc(int addedLoc) {
        this.addedLoc = addedLoc;
    }

    public void setDeletedLoc(int deletedLoc) {
        this.deletedLoc = deletedLoc;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(String commitDate) {
        this.commitDate = commitDate;
    }
}
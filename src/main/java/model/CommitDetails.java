package model;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class CommitDetails {
    private PersonIdent person;
    private List<CommitFileDetails> filesChanged = new ArrayList<>();
    private RevCommit commit;
    public String fullMessage;
    public int addedLoc;
    public int deletedLoc;
    public int version;
    public String commitDate;
    public Issue issue;

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
            this.addedLoc += commitFileDetails.added_LOC;
        }
    }

    public int getDeletedLoc() {
        return deletedLoc;
    }

    public void setDeleted_loc() {
        for( CommitFileDetails commitFileDetails: this.filesChanged){
            this.deletedLoc += commitFileDetails.deleted_LOC;
        }
    }


}
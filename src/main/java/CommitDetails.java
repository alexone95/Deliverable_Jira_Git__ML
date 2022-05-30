import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class CommitDetails {
    public PersonIdent person;
    public List<CommitFileDetails> files_changed = new ArrayList<>();
    public RevCommit commit;
    public String full_message;
    public int added_loc;
    public int deleted_loc;
    public int version;
    public String commit_date;
    public Issue issue;

    public List<CommitFileDetails> getFiles_changed() {
        return files_changed;
    }

    public void setFiles_changed(List<CommitFileDetails> files_changed) {
        this.files_changed = files_changed;
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

    public int getAdded_loc() {
        return added_loc;
    }

    public void setAdded_loc() {
        for( CommitFileDetails commitFileDetails: this.files_changed ){
            this.added_loc += commitFileDetails.added_LOC;
        }
    }

    public int getDeleted_loc() {
        return deleted_loc;
    }

    public void setDeleted_loc() {
        for( CommitFileDetails commitFileDetails: this.files_changed ){
            this.deleted_loc += commitFileDetails.deleted_LOC;
        }
    }
}
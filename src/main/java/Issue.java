import java.util.ArrayList;
import java.util.List;

public class Issue {

    public List<CommitDetails> commits;
    public String issue_key;
    public String resolutionDate;
    public String creationDate;
    public int fix_version;
    public int opening_version;
    public int injected_version;
    public ArrayList<String> affected_version;
    public ArrayList<Integer> affected_version_index;

    public Issue(String issue_key, String resolutionDate, String creationDate, ArrayList<String> affected_version) {
        this.issue_key = issue_key;
        this.resolutionDate = resolutionDate;
        this.creationDate = creationDate;
        this.affected_version = affected_version;
    }

    public Issue(List<CommitDetails> commits, String issue_key, String resolutionDate, String creationDate, ArrayList<String> affected_version) {
        this.commits = commits;
        this.issue_key = issue_key;
        this.resolutionDate = resolutionDate;
        this.creationDate = creationDate;
        this.affected_version = affected_version;
    }

    public List<CommitDetails> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitDetails> commits) {
        this.commits = commits;
    }

    public String getIssue_key() {
        return issue_key;
    }

    public void setIssue_key(String issue_key) {
        this.issue_key = issue_key;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(String resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public int getFix_version() {
        return fix_version;
    }

    public void setFix_version(int fix_version) {
        this.fix_version = fix_version;
    }

    public int getOpening_version() {
        return opening_version;
    }

    public void setOpening_version(int opening_version) {
        this.opening_version = opening_version;
    }

    public int getInjected_version() {
        return injected_version;
    }

    public void setInjected_version(int injected_version) {
        this.injected_version = injected_version;
    }

    public ArrayList<String> getAffected_version() {
        return affected_version;
    }

    public void setAffected_version(ArrayList<String> affected_version) {
        this.affected_version = affected_version;
    }
}

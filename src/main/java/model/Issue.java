package model;

import java.util.ArrayList;
import java.util.List;


public class Issue {

    private List<CommitDetails> commits;
    private String issueKey;
    private String resolutionDate;
    private String creationDate;
    private int fixVersion;
    private int openingVersion;
    private int injectedVersion;
    private ArrayList<String> affectedVersion;
    private ArrayList<Integer> affectedVersionIndex;

    public Issue(String issueKey, String resolutionDate, String creationDate, List<String> affectedVersion) {
        this.issueKey = issueKey;
        this.resolutionDate = resolutionDate;
        this.creationDate = creationDate;
        this.affectedVersion = (ArrayList<String>) affectedVersion;
    }

    public Issue(List<CommitDetails> commits, String issueKey, String resolutionDate, String creationDate, List<String> affectedVersion) {
        this.commits = commits;
        this.issueKey = issueKey;
        this.resolutionDate = resolutionDate;
        this.creationDate = creationDate;
        this.affectedVersion = (ArrayList<String>) affectedVersion;
    }

    public List<CommitDetails> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitDetails> commits) {
        this.commits = commits;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
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

    public int getFixVersion() {
        return fixVersion;
    }

    public void setFixVersion(int fixVersion) {
        this.fixVersion = fixVersion;
    }

    public int getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(int openingVersion) {
        this.openingVersion = openingVersion;
    }

    public int getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(int injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public List<String> getAffectedVersion() {
        return affectedVersion;
    }

    public void setAffectedVersion(List<String> affectedVersion) {
        this.affectedVersion = (ArrayList<String>) affectedVersion;
    }

    public List<Integer> getAffectedVersionIndex() {
        return affectedVersionIndex;
    }

    public void setAffectedVersionIndex(List<Integer> affectedVersionIndex) {
        this.affectedVersionIndex = (ArrayList<Integer>) affectedVersionIndex;
    }
}

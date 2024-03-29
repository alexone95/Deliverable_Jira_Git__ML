package model;

public class CommitFileDetails {
    private String modifiedFileName;
    private int addedLOC;
    private int deletedLOC;
    private long loc;
    private String fileText;
    private int age;
    private boolean buggy;
    private int churn;
    private int numPrivateAttributerOrMethods;
    private int numPublicAttributerOrMethods;
    private int locTouched;

    public CommitFileDetails(String modifiedFileName, int addedLOC, int deletedLOC, int replacedLines, long loc, String fileText,
                             int age) {
        this.modifiedFileName = modifiedFileName;
        this.addedLOC = addedLOC;
        this.deletedLOC = deletedLOC;
        this.loc = loc;
        this.fileText = fileText;
        this.age = age;
        this.churn = addedLOC - deletedLOC;
        this.locTouched = replacedLines + addedLOC + deletedLOC;
    }

    public String getModifiedFileName() {
        return modifiedFileName;
    }

    public void setModifiedFileName(String modifiedFileName) {
        this.modifiedFileName = modifiedFileName;
    }

    public int getAddedLOC() {
        return addedLOC;
    }

    public void setAddedLOC(int addedLOC) {
        this.addedLOC = addedLOC;
    }

    public int getDeletedLOC() {
        return deletedLOC;
    }

    public void setDeletedLOC(int deletedLOC) {
        this.deletedLOC = deletedLOC;
    }

    public long getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public String getFileText() {
        return fileText;
    }

    public void setFileText(String fileText) {
        this.fileText = fileText;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getNumPrivateAttributerOrMethods() {
        return numPrivateAttributerOrMethods;
    }

    public void setNumPrivateAttributerOrMethods(int numPrivateAttributerOrMethods) {
        this.numPrivateAttributerOrMethods = numPrivateAttributerOrMethods;
    }

    public int getNumPublicAttributerOrMethods() {
        return numPublicAttributerOrMethods;
    }

    public void setNumPublicAttributerOrMethods(int numPublicAttributerOrMethods) {
        this.numPublicAttributerOrMethods = numPublicAttributerOrMethods;
    }

    public int getLocTouched() {
        return locTouched;
    }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
    }
}

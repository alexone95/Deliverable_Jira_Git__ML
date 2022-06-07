package model;

public class CommitFileDetails {
    private String modifiedFileName;
    private int addedLOC;
    private int deletedLOC;
    private long LOC;
    private String fileText;
    private int age;
    private boolean buggy;
    private int churn;
    private int numImports;
    private int numComments;
    private int LOC_touched;

    public CommitFileDetails(String modifiedFileName, int addedLOC, int deletedLOC, int replaced_lines, long LOC, String fileText,
                             int age) {
        this.modifiedFileName = modifiedFileName;
        this.addedLOC = addedLOC;
        this.deletedLOC = deletedLOC;
        this.LOC = LOC;
        this.fileText = fileText;
        this.age = age;
        this.churn = addedLOC - deletedLOC;
        this.LOC_touched = replaced_lines + addedLOC + deletedLOC;
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

    public long getLOC() {
        return LOC;
    }

    public void setLOC(int LOC) {
        this.LOC = LOC;
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

    public int getNumImports() {
        return numImports;
    }

    public void setNumImports(int numImports) {
        this.numImports = numImports;
    }

    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public int getLOC_touched() {
        return LOC_touched;
    }

    public void setLOC_touched(int LOC_touched) {
        this.LOC_touched = LOC_touched;
    }

    public void setLOC(long LOC) {
        this.LOC = LOC;
    }
}

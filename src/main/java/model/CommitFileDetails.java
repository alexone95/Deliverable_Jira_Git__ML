package model;

public class CommitFileDetails {
    public String modified_file_name;
    public int added_LOC;
    public int deleted_LOC;
    public long LOC;
    public String file_text;
    public int age;
    public boolean buggy;
    public int churn;
    public int numImports;
    public int numComments;
    public int LOC_touched;

    public CommitFileDetails(String modified_file_name, int added_LOC, int deleted_LOC, int replaced_lines, long LOC, String file_text,
                             int age, boolean buggy, int numImports, int numComments) {
        this.modified_file_name = modified_file_name;
        this.added_LOC = added_LOC;
        this.deleted_LOC = deleted_LOC;
        this.LOC = LOC;
        this.file_text = file_text;
        this.age = age;
        this.buggy = buggy;
        this.churn = added_LOC - deleted_LOC;
        this.numImports = numImports;
        this.numComments = numComments;
        this.LOC_touched = replaced_lines + added_LOC + deleted_LOC;
    }

    public String getModified_file_name() {
        return modified_file_name;
    }

    public void setModified_file_name(String modified_file_name) {
        this.modified_file_name = modified_file_name;
    }

    public int getAdded_LOC() {
        return added_LOC;
    }

    public void setAdded_LOC(int added_LOC) {
        this.added_LOC = added_LOC;
    }

    public int getDeleted_LOC() {
        return deleted_LOC;
    }

    public void setDeleted_LOC(int deleted_LOC) {
        this.deleted_LOC = deleted_LOC;
    }

    public long getLOC() {
        return LOC;
    }

    public void setLOC(int LOC) {
        this.LOC = LOC;
    }
}

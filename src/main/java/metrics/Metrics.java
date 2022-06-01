package metrics;

import java.util.ArrayList;

public class Metrics {

    private int                 version;
    private String              filepath;
    private int                 NR;
    private ArrayList<String>   AUTHORS = new ArrayList<>();
    private int                 LOC;
    private int                 LOC_TOUCHED;
    private int                 AGE;
    private int                 CHURN;
    private int                 MAX_LOC_ADDED;
    private int                 AVG_LOC_ADDED;
    private int                 AVG_CHANGE_SET;
    private int                 MAX_CHANGE_SET;
    private int                 numImports;
    private int                 numComments;
    private String              BUGGYNESS;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public int getNR() {
        return NR;
    }

    public void setNR(int NR) {
        this.NR = NR;
    }

    public ArrayList<String> getAUTHORS() {
        return AUTHORS;
    }

    public void setAUTHORS(ArrayList<String> AUTHORS) {
        this.AUTHORS = AUTHORS;
    }

    public int getLOC() {
        return LOC;
    }

    public void setLOC(int LOC) {
        this.LOC = LOC;
    }

    public int getLOC_TOUCHED() {
        return LOC_TOUCHED;
    }

    public void setLOC_TOUCHED(int LOC_TOUCHED) {
        this.LOC_TOUCHED = LOC_TOUCHED;
    }

    public int getAGE() {
        return AGE;
    }

    public void setAGE(int AGE) {
        this.AGE = AGE;
    }

    public int getCHURN() {
        return CHURN;
    }

    public void setCHURN(int CHURN) {
        this.CHURN = CHURN;
    }

    public int getMAX_LOC_ADDED() {
        return MAX_LOC_ADDED;
    }

    public void setMAX_LOC_ADDED(int MAX_LOC_ADDED) {
        this.MAX_LOC_ADDED = MAX_LOC_ADDED;
    }

    public int getAVG_LOC_ADDED() {
        return AVG_LOC_ADDED;
    }

    public void setAVG_LOC_ADDED(int AVG_LOC_ADDED) {
        this.AVG_LOC_ADDED = AVG_LOC_ADDED;
    }

    public int getAVG_CHANGE_SET() {
        return AVG_CHANGE_SET;
    }

    public void setAVG_CHANGE_SET(int AVG_CHANGE_SET) {
        this.AVG_CHANGE_SET = AVG_CHANGE_SET;
    }

    public int getMAX_CHANGE_SET() {
        return MAX_CHANGE_SET;
    }

    public void setMAX_CHANGE_SET(int MAX_CHANGE_SET) {
        this.MAX_CHANGE_SET = MAX_CHANGE_SET;
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

    public String getBUGGYNESS() {
        return BUGGYNESS;
    }

    public void setBUGGYNESS(String BUGGYNESS) {
        this.BUGGYNESS = BUGGYNESS;
    }

    public void appendAuthor(String author ){
        this.AUTHORS.add(author);
    }

    public void update( Metrics oldMetrics ) {
        // Sum up all NR.
        this.NR += oldMetrics.getNR();
        /* Append this author to the list of authors who have worked at this file within the release.
           At the end, the size of this array will represent the total number of authors.*/
        if ( !oldMetrics.getAUTHORS().contains(this.AUTHORS.get(0)) ){
            ArrayList<String> updateAuthors = oldMetrics.getAUTHORS();
            updateAuthors.add( this.AUTHORS.get(0) );
            this.AUTHORS = updateAuthors;
        } else { this.AUTHORS = oldMetrics.getAUTHORS(); }
        this.CHURN += oldMetrics.getCHURN();
        this.LOC_TOUCHED += oldMetrics.getLOC_TOUCHED();
        // Sum up all LOC ADDED within the release.
        this.AVG_LOC_ADDED += oldMetrics.getAVG_LOC_ADDED();
        // Get the oldest version of this file within the release.
        if ( oldMetrics.getAGE() > this.AGE ){
            this.AGE = oldMetrics.getAGE();
        }
        // Sum up all LOC reported for this file over all commits within the release.
        this.LOC += oldMetrics.getLOC();
        // Update MAX LOC ADDED only if it is greater than the max loc added reached by previous commits within the release.
        if (!( this.MAX_LOC_ADDED > oldMetrics.getMAX_LOC_ADDED())){
            this.MAX_LOC_ADDED = oldMetrics.getMAX_LOC_ADDED();
        }
        // Sum up all CHANGE SET SIZE over commits within the release.
        this.AVG_CHANGE_SET += oldMetrics.getAVG_CHANGE_SET();
        // Update MAX CHANGE SET only if it is greater than the max chg set reached by previous commits within the release.
        if (!( this.MAX_CHANGE_SET> oldMetrics.getMAX_CHANGE_SET())){
            this.MAX_CHANGE_SET = oldMetrics.getMAX_CHANGE_SET();
        }
        // Sum up all LOC reported for this file over all commits within the release.
        this.numImports += oldMetrics.getNumImports();
        // Sum up all LOC reported for this file over all commits within the release.
        this.numComments += oldMetrics.getNumComments();

    }


}
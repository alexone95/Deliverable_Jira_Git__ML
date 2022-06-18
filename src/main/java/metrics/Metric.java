package metrics;

import java.util.ArrayList;
import java.util.List;

public class Metric {

    private int version;
    private String filepath;
    private int nr;
    private ArrayList<String> authors = new ArrayList<>();
    private int loc;
    private int locTouched;
    private int age;
    private int churn;
    private int maxLocAdded;
    private int avgLocAdded;
    private int avgChangeSet;
    private int maxChangeSet;
    private int numImports;
    private int numComments;
    private String buggyness;

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

    public int getNr() {
        return nr;
    }

    public void setNr(int nr) {
        this.nr = nr;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = (ArrayList<String>) authors;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getLocTouched() {
        return locTouched;
    }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(int maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public int getAvgLocAdded() {
        return avgLocAdded;
    }

    public void setAvgLocAdded(int avgLocAdded) {
        this.avgLocAdded = avgLocAdded;
    }

    public int getAvgChangeSet() {
        return avgChangeSet;
    }

    public void setAvgChangeSet(int avgChangeSet) {
        this.avgChangeSet = avgChangeSet;
    }

    public int getMaxChangeSet() {
        return maxChangeSet;
    }

    public void setMaxChangeSet(int maxChangeSet) {
        this.maxChangeSet = maxChangeSet;
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

    public String getBuggyness() {
        return buggyness;
    }

    public void setBuggyness(String buggyness) {
        this.buggyness = buggyness;
    }

    public void appendAuthor(String author ){
        this.authors.add(author);
    }

    public void update( Metric oldMetric) {
        // Sum up all nr.
        this.nr += oldMetric.getNr();
        /* Append this author to the list of authors who have worked at this file within the release.
           At the end, the size of this array will represent the total number of authors.*/
        if ( !oldMetric.getAuthors().contains(this.authors.get(0)) ){
            ArrayList<String> updateAuthors = (ArrayList<String>) oldMetric.getAuthors();
            updateAuthors.add( this.authors.get(0) );
            this.authors = updateAuthors;
        } else { this.authors = (ArrayList<String>) oldMetric.getAuthors(); }
        this.churn += oldMetric.getChurn();
        this.locTouched += oldMetric.getLocTouched();
        // Sum up all loc ADDED within the release.
        this.avgLocAdded += oldMetric.getAvgLocAdded();
        // Get the oldest version of this file within the release.
        if ( oldMetric.getAge() > this.age){
            this.age = oldMetric.getAge();
        }
        // Sum up all loc reported for this file over all commits within the release.
        this.loc += oldMetric.getLoc();
        // Update MAX loc ADDED only if it is greater than the max loc added reached by previous commits within the release.
        if ( this.maxLocAdded <= oldMetric.getMaxLocAdded()){
            this.maxLocAdded = oldMetric.getMaxLocAdded();
        }
        // Sum up all CHANGE SET SIZE over commits within the release.
        this.avgChangeSet += oldMetric.getAvgChangeSet();
        // Update MAX CHANGE SET only if it is greater than the max chg set reached by previous commits within the release.
        if (this.maxChangeSet <= oldMetric.getMaxChangeSet()){
            this.maxChangeSet = oldMetric.getMaxChangeSet();
        }
        // Sum up all loc reported for this file over all commits within the release.
        this.numImports += oldMetric.getNumImports();
        // Sum up all loc reported for this file over all commits within the release.
        this.numComments += oldMetric.getNumComments();

    }


}
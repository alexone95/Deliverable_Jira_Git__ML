package metrics;

import java.util.ArrayList;
import java.util.List;

public class Metric {

    private int version;
    private String filepath;
    private int nr; //Numero di revisioni
    private ArrayList<String> authors = new ArrayList<>();
    private int loc;
    private int locTouched;
    private int age;
    private int churn;
    private int maxLocAdded;
    private int avgLocAdded;
    private int avgChangeSet;
    private int maxChangeSet;
    private int numPrivateAttributesOrMethods;
    private int numPublicAttributesOrMethods;
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

    public int getNumPrivateAttributesOrMethods() {
        return numPrivateAttributesOrMethods;
    }

    public void setNumPrivateAttributesOrMethods(int numPrivateAttributesOrMethods) {
        this.numPrivateAttributesOrMethods = numPrivateAttributesOrMethods;
    }

    public int getNumPublicAttributesOrMethods() {
        return numPublicAttributesOrMethods;
    }

    public void setNumPublicAttributesOrMethods(int numPublicAttributesOrMethods) {
        this.numPublicAttributesOrMethods = numPublicAttributesOrMethods;
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

        this.nr += oldMetric.getNr();
        /* Appende l'autore alla lista degli autori, se non già presente, la metrica utilizzata sarà la dimensione
            della lista. */
        if ( !oldMetric.getAuthors().contains(this.authors.get(0)) ){
            ArrayList<String> updateAuthors = (ArrayList<String>) oldMetric.getAuthors();
            updateAuthors.add( this.authors.get(0) );
            this.authors = updateAuthors;
        }
        else {
            this.authors = (ArrayList<String>) oldMetric.getAuthors();
        }

        this.churn += oldMetric.getChurn();
        this.locTouched += oldMetric.getLocTouched();

        // Somma tutte le LOC aggiunte nella release
        this.avgLocAdded += oldMetric.getAvgLocAdded();

        // Prende la versione con age maggiore del file
        if ( oldMetric.getAge() > this.age){
            this.age = oldMetric.getAge();
        }

        // Somma tutte le LOC riportate dai commit relativi a questo file per una determinata versione
        this.loc += oldMetric.getLoc();
        if ( this.maxLocAdded <= oldMetric.getMaxLocAdded()){
            this.maxLocAdded = oldMetric.getMaxLocAdded();
        }

        this.avgChangeSet += oldMetric.getAvgChangeSet();
        if (this.maxChangeSet <= oldMetric.getMaxChangeSet()){
            this.maxChangeSet = oldMetric.getMaxChangeSet();
        }

        // Calcola il numero totale di import su tutti i commit della release
        this.numPrivateAttributesOrMethods += oldMetric.getNumPrivateAttributesOrMethods();
        // Calcola il numero totale degli attributi pubblici su tutti i commit della release
        this.numPublicAttributesOrMethods += oldMetric.getNumPublicAttributesOrMethods();

    }


}
package metrics;

import com.google.common.collect.Multimap;
import model.CommitDetails;
import model.CommitFileDetails;
import model.Issue;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DatasetBuilder {

    // MultiKeyMap che costituisce il dataset con chiave :<versione,filepath> e valore <metriche>
    private MultiKeyMap<Object, Object> fileDataset = MultiKeyMap.multiKeyMap( new LinkedMap<>() );

    private Multimap<LocalDate,String> versionMap;

    private final int lastVersion;

    private String projectName;


    public DatasetBuilder( Multimap<LocalDate,String> versionMap, String projectName){
        this.versionMap = versionMap;
        this.lastVersion =  (versionMap.size()/2)/2;
        this.projectName = projectName;
    }

    /*  Metodo utilizzato per popolare @fileDataset  */
    public void populateFileDataset( List<Issue> issues ){


        // Costruisce una lista con i filename collegati ai vari commit relativi alle issues
        ArrayList<String> fileNameList = (ArrayList<String>) buildFilenameList(issues);

        for ( Issue issue : issues ){
            for ( CommitDetails commit : issue.getCommits() ) {
                if (commit.getFilesChanged() != null) {
                    setMetrics(commit);
                }
            }
        }

        setFileDataset(fileNameList);


    }

    /* Questo metodo si occupa di scrivere il CSV che rappresenta il dataset */
    public void writeToCSV(String projectName) throws IOException {
        try (FileWriter csvWriter = new FileWriter("src/csv_output/" + projectName + "_dataset.csv")) {

            csvWriter.append("Version Number");
            csvWriter.append(",");
            csvWriter.append("File Name");
            csvWriter.append(",");
            csvWriter.append("NumberRevisions");
            csvWriter.append(",");
            csvWriter.append("NumberAuthors");
            csvWriter.append(",");
            csvWriter.append("LOC");
            csvWriter.append(",");
            csvWriter.append("AGE");
            csvWriter.append(",");
            csvWriter.append("CHURN");
            csvWriter.append(",");
            csvWriter.append("LOC_TOUCHED");
            csvWriter.append(",");
            csvWriter.append("Avg_LOC_Added");
            csvWriter.append(",");
            csvWriter.append("MaxLocAdded");
            csvWriter.append(",");
            csvWriter.append("Avg_Chg_Set");
            csvWriter.append(",");
            csvWriter.append("Max_Chg_Set");
            csvWriter.append(",");
            csvWriter.append("numImports");
            csvWriter.append(",");
            csvWriter.append("numPublicAttOrMet");
            csvWriter.append(",");
            csvWriter.append("Buggy");
            csvWriter.append("\n");

            // Utilizzo questa mappa per inserire in modo ordinato (per versione) le entry nel csv
            Map<String, Metric> orderedMap = new TreeMap<>();
            var iterator = fileDataset.mapIterator();

            // Itero sul dataset per ordinarlo per versione
            while (iterator.hasNext()) {
                iterator.next();
                var key = iterator.getKey();

                int version = (int) key.getKey(0);
                if (version <= lastVersion + 1){
                    String newKey = String.valueOf( version );
                    if (newKey.length() == 1) {
                        newKey = "0" + newKey;
                    }
                    Metric metric = (Metric) fileDataset.get(key.getKey(0), key.getKey(1));

                    orderedMap.put(newKey + "," + key.getKey(1), metric);
                }
            }

            // Itero su orderedMap che è un istanza di Map ordinata per versione
            for (Map.Entry<String, Metric> entry : orderedMap.entrySet()) {
                Metric metric = entry.getValue();
                int number = metric.getNr();
                String nAuth = Integer.toString(metric.getAuthors().size());
                String loc = Integer.toString(metric.getLoc()/number);
                String age = Integer.toString(metric.getAge());
                String churn = Integer.toString(metric.getChurn());
                String locTouched = Integer.toString(metric.getLocTouched());
                String avgLocAdded = Integer.toString( ( metric.getAvgLocAdded()/number ) );
                String maxLocAdded = Integer.toString(metric.getMaxLocAdded());
                String avgChgSet = Integer.toString(metric.getAvgChangeSet()/number);
                String maxChgSet = Integer.toString(metric.getMaxChangeSet());
                String numImports = Integer.toString(metric.getNumImports());
                String numPublicAttributesOrMethods = Integer.toString(metric.getNumPublicAttributesOrMethods());
                String buggy = metric.getBuggyness().equals("true") ? "Yes" : "No";

                csvWriter.append(entry.getKey().split(",")[0]).append(",").
                        append(entry.getKey().split(",")[1]).append(",").append(String.valueOf(metric.getNr())).
                        append(",").append(nAuth).append(",").append(loc).append(",").append(age).append(",").
                        append(churn).append(",").append(locTouched).append(",").append(avgLocAdded).append(",").
                        append(maxLocAdded).append(",").append(avgChgSet).append(",").append(maxChgSet).append(",").
                        append(numImports).append(",").append(numPublicAttributesOrMethods).append(",").append(buggy);

                csvWriter.append("\n");

            }
            csvWriter.flush();
        }
    }

    /*
        Costruisce una lista con i filename collegati ai vari commit relativi alle issues
    */
    public List<String> buildFilenameList(List<Issue> issues){
        ArrayList<String> fileNameList = new ArrayList<>();
        for ( Issue issue : issues ) {
            for (CommitDetails commit : issue.getCommits()) {
                if (commit.getFilesChanged() != null) {
                    for (CommitFileDetails file : commit.getFilesChanged()) {
                        String filename = file.getModifiedFileName();
                        if (!fileNameList.contains(filename)){
                            fileNameList.add(filename);
                        }
                    }
                }
            }
        }
        return fileNameList;
    }

    public void setFileDataset(List<String> fileNameList){
        int limit = 8;
        for (int versionFile=0; versionFile <= limit; versionFile++){
            for (String entry: fileNameList ){
                if (!fileDataset.containsKey(versionFile, entry)) {
                    Metric newMetric = new Metric();
                    newMetric.setVersion(versionFile);
                    newMetric.setFilepath(entry);
                    newMetric.setNr(1);
                    newMetric.setAge(0);
                    newMetric.setChurn(0);
                    newMetric.appendAuthor("");
                    newMetric.setLocTouched(0);
                    newMetric.setMaxLocAdded(0);
                    newMetric.setLoc(0);
                    newMetric.setAvgLocAdded(0);
                    newMetric.setAvgChangeSet(0);
                    newMetric.setMaxChangeSet(0);
                    newMetric.setNumImports(0);
                    newMetric.setNumPublicAttributesOrMethods(0);
                    newMetric.setBuggyness("false");
                    fileDataset.put(versionFile, entry, newMetric);
                }
            }
        }
    }

    /*
        Costruisce l'oggetto Metric e lo aggiunge al dataset
    */
    public void setMetrics(CommitDetails commit)
    {
        int     version;
        String  filepath;
        for (CommitFileDetails file : commit.getFilesChanged()) {
            Metric newMetric = new Metric();
            version = commit.getVersion();
            filepath = file.getModifiedFileName();
            newMetric.setVersion(version);
            newMetric.setFilepath(filepath);
            newMetric.setNr(1);
            newMetric.setAge(file.getAge());
            newMetric.setChurn(file.getChurn());
            newMetric.appendAuthor(commit.getPerson().getName());
            newMetric.setLocTouched(file.getLocTouched());
            newMetric.setMaxLocAdded(file.getAddedLOC());
            newMetric.setLoc((int) file.getLoc());
            newMetric.setAvgLocAdded(file.getAddedLOC());
            newMetric.setAvgChangeSet(commit.getFilesChanged().size());
            newMetric.setMaxChangeSet(commit.getFilesChanged().size());
            newMetric.setNumImports(file.getNumImports());
            newMetric.setNumPublicAttributesOrMethods(file.getNumPublicAttributerOrMethods());
            newMetric.setBuggyness(String.valueOf(file.isBuggy()));
            if (!fileDataset.containsKey(version, filepath)) {
                fileDataset.put(version, filepath, newMetric);
            } else {
                Metric oldMetric = (Metric) fileDataset.get(version, filepath);
                newMetric.update(oldMetric);
                fileDataset.put(version, filepath, newMetric);
            }
        }
    }
}
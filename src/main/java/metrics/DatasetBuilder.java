package metrics;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Multimap;
import model.*;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;

public class DatasetBuilder {

    // Dataset as a MultiKeyMap with key :<version,filepath> and value <metrics>
    private MultiKeyMap<Object, Object> fileDataset = MultiKeyMap.multiKeyMap( new LinkedMap<>() );

    private Multimap<LocalDate,String> versionMap;

    private final int lastVersion;

    private String projectName;


    public DatasetBuilder( Multimap<LocalDate,String> versionMap, String projectName){
        this.versionMap = versionMap;
        this.lastVersion =  (versionMap.size() / 2) / 2;
        this.projectName = projectName;
    }

    /*  This Method is used to populate the  Multi Key Map representing the dataset to be created. */
    public void populateFileDataset( List<Issue> issues ){
        int     version;
        String  filepath;
        for ( Issue issue : issues ){
            for ( CommitDetails commit : issue.getCommits() ){
                for ( CommitFileDetails file : commit.getFilesChanged() ){
                    Metrics newMetrics = new Metrics();
                    version = commit.getVersion();
                    filepath = file.getModifiedFileName();
                    newMetrics.setVersion(version);
                    newMetrics.setFilepath(filepath);
                    newMetrics.setNr( 1 );
                    newMetrics.setAge( file.getAge());
                    newMetrics.setChurn(file.getChurn());
                    newMetrics.appendAuthor(commit.getPerson().getName());
                    newMetrics.setLocTouched(file.getLocTouched());
                    newMetrics.setMaxLocAdded(file.getAddedLOC());
                    newMetrics.setLoc((int) file.getLoc());
                    newMetrics.setAvgLocAdded(file.getAddedLOC());
                    newMetrics.setAvgChangeSet(commit.getFilesChanged().size());
                    newMetrics.setMaxChangeSet(commit.getFilesChanged().size());
                    newMetrics.setNumImports(file.getNumImports());
                    newMetrics.setNumComments(file.getNumComments());
                    newMetrics.setBuggyness(String.valueOf(file.isBuggy()));
                    if( !fileDataset.containsKey(version,filepath) ){
                        fileDataset.put( version, filepath, newMetrics );
                    } else{
                        Metrics oldMetrics = ( Metrics ) fileDataset.get( version, filepath );
                        newMetrics.update( oldMetrics );
                        fileDataset.put( version, filepath, newMetrics );
                    }
                }
            }
        }
    }


    public void writeToCSV(String projectName) throws IOException {

        // Set the name of the file
        try (FileWriter csvWriter = new FileWriter("src/csv_output/" + projectName + "_dataset.csv")) {

            /*
             * metrics.Metrics Data Structure
             *   (version)
             *   (filepath)
             *  0 - NumberRevisions
             *  1 - NumberAuthors
             *  2 - LOC
             *  3 - AGE
             *  4 - CHURN
             *  5 - LOC_TOUCHED
             *  6 - Max_Loc_Added
             *  7 - Avg_Chg_set
             *  8 - Max_Chg_Set
             *  9 - Avg_LOC_Added
             *  10 - numImports
             *  11 - numComments
             * 	12 - Buggyness
             * */

            // Append the first line
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
            csvWriter.append("numComments");
            csvWriter.append(",");
            csvWriter.append("Buggy");
            csvWriter.append("\n");

            // The following Tree Map is used to insert dataset entries in the csv following the correct order (by version).
            Map<String, Metrics> orderedMap = new TreeMap<>();
            var dataSetIterator = fileDataset.mapIterator();

            // Iterate over the dataset
            while ( dataSetIterator.hasNext() ) {
                dataSetIterator.next();
                var key = dataSetIterator.getKey();

                int version = (int) key.getKey(0);
                if ( version <= lastVersion + 1 ){
                    String revisedKey = String.valueOf( version );
                    if ( revisedKey.length() == 1 ){ revisedKey = "0" + revisedKey; }
                    Metrics metrics = (Metrics) fileDataset.get(key.getKey(0), key.getKey(1));

                    orderedMap.put( revisedKey + "," + (String)key.getKey(1), metrics );
                }
            }

            for ( Map.Entry<String, Metrics> entry : orderedMap.entrySet() ) {

                Metrics metrics = entry.getValue();
                // Check that the version index is contained in the first half of the releases
                    int nr = metrics.getNr();
                    String nAuth = Integer.toString(metrics.getAuthors().size());
                    String loc = Integer.toString(metrics.getLoc()/nr);
                    String age = Integer.toString(metrics.getAge());
                    String churn = Integer.toString(metrics.getChurn());
                    String locTouched = Integer.toString(metrics.getLocTouched());
                    String avgLocAdded = Integer.toString( ( metrics.getAvgLocAdded()/nr ) );
                    String maxLocAdded = Integer.toString(metrics.getMaxLocAdded());
                    String avgChgSet = Integer.toString(metrics.getAvgChangeSet()/nr);
                    String maxChgSet = Integer.toString(metrics.getMaxChangeSet());
                    String numImports = Integer.toString(metrics.getNumImports());
                    String numComments = Integer.toString(metrics.getNumComments());
                    String buggy = metrics.getBuggyness().equals("true") ? "Yes" : "No";

                    // Append the data
                    csvWriter.append(entry.getKey().split(",")[0] + "," + entry.getKey().split(",")[1] + "," + metrics.getNr() + "," + nAuth + ","
                            + loc + "," + age + "," + churn + ","+ locTouched + "," + avgLocAdded + "," +  maxLocAdded + ","
                            + avgChgSet + "," + maxChgSet + ","  + numImports + ","  + numComments + "," +  buggy);

                    csvWriter.append("\n");

            }

            // Flush data to CSV
            csvWriter.flush();
        }
    }
}
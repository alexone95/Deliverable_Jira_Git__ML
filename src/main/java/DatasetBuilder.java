import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.Multimap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;

public class DatasetBuilder {

    // Dataset as a MultiKeyMap with key :<version,filepath> and value <metrics>
    private MultiKeyMap fileDataset = MultiKeyMap.multiKeyMap( new LinkedMap() );

    private Multimap<LocalDate,String> versionMap;

    private int lastVersion;

    private String projectName;


    public DatasetBuilder( Multimap<LocalDate,String> versionMap, String projectName){
        this.versionMap = versionMap;
        this.lastVersion =  (versionMap.size() / 2) / 2;
        this.projectName = projectName;
    }

    /*  This Method is used to populate the  Multi Key Map representing the dataset to be created. */
    public void populateFileDataset( ArrayList<Issue> issues ){
        int     version;
        String  filepath;
        for ( Issue issue : issues ){
            for ( CommitDetails commit : issue.getCommits() ){
                for ( CommitFileDetails file : commit.getFiles_changed() ){
                    Metrics newMetrics = new Metrics();
                    version = commit.version;
                    filepath = file.modified_file_name;
                    newMetrics.setVersion(version);
                    newMetrics.setFilepath(filepath);
                    newMetrics.setNR( 1 );
                    newMetrics.setAGE( file.age);
                    newMetrics.setCHURN(file.churn);
                    newMetrics.appendAuthor(commit.person.getName());
                    newMetrics.setLOC_TOUCHED(file.LOC_touched);
                    newMetrics.setMAX_LOC_ADDED(file.added_LOC);
                    newMetrics.setLOC((int) file.LOC);
                    newMetrics.setAVG_LOC_ADDED(file.added_LOC);
                    newMetrics.setAVG_CHANGE_SET(commit.files_changed.size());
                    newMetrics.setMAX_CHANGE_SET(commit.files_changed.size());
                    newMetrics.setNumImports(file.numImports);
                    newMetrics.setNumComments(file.numComments);
                    newMetrics.setBUGGYNESS(String.valueOf(file.buggy));
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
             * Metrics Data Structure
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
            MapIterator dataSetIterator = fileDataset.mapIterator();

            // Iterate over the dataset
            while ( dataSetIterator.hasNext() ) {
                dataSetIterator.next();
                MultiKey key = (MultiKey) dataSetIterator.getKey();

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
                    int nr =                metrics.getNR();
                    String NR =             Integer.toString((metrics.getNR()));
                    String NAUTH =          Integer.toString(metrics.getAUTHORS().size());
                    String LOC =            Integer.toString((int)metrics.getLOC()/nr);
                    String AGE =            Integer.toString(metrics.getAGE());
                    String CHURN =          Integer.toString(metrics.getCHURN());
                    String LOC_TOUCHED = 	Integer.toString(metrics.getLOC_TOUCHED());
                    String AvgLocAdded =    Integer.toString((int) ( metrics.getAVG_LOC_ADDED()/nr ) );
                    String MaxLocAdded =    Integer.toString(metrics.getMAX_LOC_ADDED());
                    String AvgChgSet =      Integer.toString((int)metrics.getAVG_CHANGE_SET()/nr);
                    String MaxChgSet =      Integer.toString(metrics.getMAX_CHANGE_SET());
                    String numImports = 	Integer.toString(metrics.getNumImports());
                    String numComments = 	Integer.toString(metrics.getNumComments());
                    String buggy =          metrics.getBUGGYNESS().equals("true") ? "Yes" : "No";

                    // Append the data
                    csvWriter.append(entry.getKey().split(",")[0] + "," + entry.getKey().split(",")[1] + "," + metrics.getNR() + "," + NAUTH + ","
                            + LOC + "," + AGE + "," + CHURN + ","+ LOC_TOUCHED + "," + AvgLocAdded + "," +  MaxLocAdded + ","
                            + AvgChgSet + "," + MaxChgSet + ","  + numImports + ","  + numComments + "," +  buggy);

                    csvWriter.append("\n");

            }

            // Flush data to CSV
            csvWriter.flush();
        }
    }
}
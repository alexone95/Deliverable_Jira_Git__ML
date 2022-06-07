package utils;

import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import model.*;

public class Utils {

    private static final String TRAINING = "_training.arff";
    private static final String TESTING = "_testing.arff";

    private static final String OVER_SAMPLING = "Over sampling";
    private static final String UNDER_SAMPLING = "Under sampling";
    private static final String SMOTE = "Smote";
    private static final String NO_SAMPLING = "No sampling";

    public static void print_files_infoprint_files_info(List<CommitFileDetails> commitFileDetailsList){
        for(CommitFileDetails commitFileDetails : commitFileDetailsList){
            System.out.println("\n");
            System.out.println("FILE NAME: " + commitFileDetails.getModifiedFileName());
            System.out.println("DELETED LOC: " + commitFileDetails.getDeletedLOC() + "| LOC: " + commitFileDetails.getLOC() +
                    "| ADDED LOC: " + commitFileDetails.getAddedLOC() + "| AGE: " + commitFileDetails.getAge());
            System.out.println("BUGGY: " + commitFileDetails.isBuggy());
            System.out.println("\n");
        }
    }

    public static void print_ticket_list(List<Issue> issues){
        for(Issue commitIssue : issues){
            System.out.println("\nKEY: " + commitIssue.issue_key );
            System.out.println("RESOLUTION DATE: " + commitIssue.resolutionDate );
            System.out.println("CREATION DATE: " + commitIssue.creationDate );
            System.out.println("AV: " + commitIssue.affected_version.toString() );
            System.out.println("FIX VERSION: " + commitIssue.fix_version + "| OPENING VERSION: " + commitIssue.opening_version + "| INJECTED VERSION: " + commitIssue.injected_version);
            for(String affected_version: commitIssue.affected_version){
                System.out.println("AFFECTED VERSION: " + affected_version );
            }
            for(Integer affected_versionindex: commitIssue.affected_version_index){
                System.out.println("AFFECTED VERSION INDEX: " + affected_versionindex );

            }

        }
    }

    public static void print_commit_details_from_ticket(List<Issue> issues){
        for(Issue commitIssue : issues){
            System.out.println("\n");
            System.out.println("KEY: " + commitIssue.issue_key );
            for(CommitDetails commitDetails : commitIssue.commits){
                System.out.println("COMMIT HASH: " + commitDetails.getCommit().getName());
                System.out.println("ADDED LOC: " + commitDetails.getAddedLoc());
                System.out.println("DELETED LOC: " + commitDetails.getDeletedLoc());
                System.out.println("PERSON: " + commitDetails.getPerson().getName());
                System.out.println("COMMIT DATE: " + commitDetails.getCommitDate());
            }
        }
    }

    public static void print_full_info_from_ticket(List<Issue> issues){
        for(Issue commitIssue : issues){
            System.out.println("\n");
            System.out.println("KEY: " + commitIssue.issue_key );
            System.out.println("RESOLUTION DATE: " + commitIssue.resolutionDate + "| CREATION DATE: " + commitIssue.creationDate );
            System.out.println("AV: " + commitIssue.affected_version.toString()  + "AV INDEX: " + commitIssue.affected_version_index.toString() + "\n");
            for(CommitDetails commitDetails : commitIssue.commits){
                System.out.println("COMMIT HASH: " + commitDetails.getCommit().getName() + "| COMMIT DATE: "
                        + commitDetails.getCommitDate() + "| VERSION: " + commitDetails.getVersion() + "| PERSON:" +
                        commitDetails.getPerson().getName());
                print_files_infoprint_files_info(commitDetails.getFilesChanged());
                System.out.println("\n");
            }
        }
    }

    public static void print_file_details_from_ticket(List<Issue> issues){
        for(Issue commitIssue : issues){
            System.out.println("\n");
            System.out.println("KEY: " + commitIssue.issue_key );
            for(CommitDetails commitDetails : commitIssue.commits){
                System.out.println("COMMIT HASH: " + commitDetails.getCommit().getName() + "\n");
                print_files_infoprint_files_info(commitDetails.getFilesChanged());
            }
        }
    }

    public static long countLineBufferedReader(String file_text) {

        long lines = 0;
        String line= "";
        try (BufferedReader reader = new BufferedReader(new StringReader(file_text))) {
            while(line != null ){
                line = reader.readLine();
                lines++;
            }
        } catch (IOException e) {
            return 0;
        }
        return lines;

    }

    public static int appendToCSV(FileWriter csvWriter, String line) throws IOException {
        int counterDefective = 0;
        // Append the row readed from the CSV file, but without the first 2 column
        String[] array = line.split(",");
        for (int i = 2; i < array.length; i++) {
            if (i == array.length - 1) {
                if(array[i].equals("Yes"))
                    counterDefective = counterDefective + 1;

                csvWriter.append(array[i] + "\n");
            } else {
                csvWriter.append(array[i] + ",");
            }
        }
        return counterDefective;
    }

    /** Apply the walk forward technique to build training set building the arff
     */
    public static List<Integer> walkForwardTraining(String projectName, int trainingLimit) throws IOException {

        int counterElement = 0;
        int counterDefective = 0;

        ArrayList<Integer> counterList = new ArrayList<>();

        // Create the output ARFF file
        try (FileWriter csvWriter = new FileWriter(projectName + TRAINING)) {

            // Append the static line of the ARFF file
            csvWriter.append("@relation " + projectName + "\n\n");
            csvWriter.append("@attribute NumberRevisions real\n");
            csvWriter.append("@attribute NumberAuthors real\n");
            csvWriter.append("@attribute LOC real\n");
            csvWriter.append("@attribute AGE real\n");
            csvWriter.append("@attribute CHURN real\n");
            csvWriter.append("@attribute LOC_TOUCHED real\n");
            csvWriter.append("@attribute AvgLocAdded real\n");
            csvWriter.append("@attribute MaxLocAdded real\n");
            csvWriter.append("@attribute AvgChgSet real\n");
            csvWriter.append("@attribute MaxChgSet real\n");
            csvWriter.append("@attribute numImports real\n");
            csvWriter.append("@attribute numComments real\n");
            csvWriter.append("@attribute Buggy {Yes, No}\n\n");
            csvWriter.append("@data\n");

            // Read the project dataset
            try (BufferedReader br = new BufferedReader(new FileReader("src/csv_output/" + projectName + "_dataset.csv"))){

                // Skip the first line (contains just column name)
                String line = br.readLine();

                // Read till the last row
                while ((line = br.readLine()) != null){

                    // Check if the version number is contained in the limit index
                    if (Integer.parseInt(line.split(",")[0]) <= trainingLimit ) {

                        counterElement = counterElement + 1;

                        counterDefective = counterDefective + appendToCSV(csvWriter, line);
                    }
                }

                // Flush the file to the disk
                csvWriter.flush();

                counterList.add(counterElement);
                counterList.add(counterDefective);

                return counterList;

            }
        }
    }

    /** Apply the walk forward technique to build testing set building the arff
     */
    public static List<Integer> walkForwardTesting(String projectName, int testing) throws IOException, NoVersionException {

        int counterElement = 0;
        int counterDefective = 0;
        ArrayList<Integer> counterList = new ArrayList<>();
        // Create the output ARFF file
        try (FileWriter csvWriter = new FileWriter(projectName + TESTING)) {

            // Append the static line of the ARFF file
            csvWriter.append("@relation " + projectName + "\n\n");
            csvWriter.append("@attribute NumberRevisions real\n");
            csvWriter.append("@attribute NumberAuthors real\n");
            csvWriter.append("@attribute LOC real\n");
            csvWriter.append("@attribute AGE real\n");
            csvWriter.append("@attribute CHURN real\n");
            csvWriter.append("@attribute LOC_TOUCHED real\n");
            csvWriter.append("@attribute AvgLocAdded real\n");
            csvWriter.append("@attribute MaxLocAdded real\n");
            csvWriter.append("@attribute AvgChgSet real\n");
            csvWriter.append("@attribute MaxChgSet real\n");
            csvWriter.append("@attribute numImports real\n");
            csvWriter.append("@attribute numComments real\n");
            csvWriter.append("@attribute Buggy {Yes, No}\n\n");
            csvWriter.append("@data\n");

            // Read the project dataset
            try (BufferedReader br = new BufferedReader(new FileReader("src/csv_output/" + projectName + "_dataset.csv"))){

                // Skip the first line (contains just column name)
                String line = br.readLine();

                // Read till the last row
                while ((line = br.readLine()) != null){

                    // Check if the version number is equal to the one equal to the test index
                    if (Integer.parseInt(line.split(",")[0]) == testing ) {

                        counterElement = counterElement + 1;

                        // Append the row readed from the CSV file, but without the first 2 column
                        counterDefective = counterDefective + appendToCSV(csvWriter, line);
                    }
                }

                // Flush the file to the disk
                csvWriter.flush();
                counterList.add(counterElement);
                counterList.add(counterDefective);

            }
        }

        if ( counterElement <= 5 ) {
            throw(new NoVersionException("Non ci sono entry per la versione", testing ));
        }

        return counterList;
    }

    public static String retrieveMetrics(Evaluation eval, String classifier, String balancing, String featureSelection) {

        return classifier + "," + balancing + "," + featureSelection + "," + eval.truePositiveRate(1)  + "," + eval.falsePositiveRate(1)  + "," + eval.trueNegativeRate(1)  + "," + eval.falseNegativeRate(1)  + "," + eval.precision(1)  + "," + eval.recall(1)  + "," + eval.areaUnderROC(1)  + "," + eval.kappa() + "\n";
    }

    public static int getNumImports( String fileText ){
        int numImports = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine()) {
                if( line.contains( "import" )){
                    numImports ++;
                }
            }
        } catch ( IOException e ) {
            return 0;
        }
        return numImports;
    }

    public static int getNumComments( String fileText ){
        int numComments = 0;
        try ( BufferedReader reader = new BufferedReader(new StringReader(fileText)) ) {
            for ( String line = reader.readLine(); line != null; line = reader.readLine()) {
                if( line.contains( "//" ) || line.contains( "/*" ) || line.contains( "*/" )|| line.contains( "*" )){
                    numComments ++;
                }
            }
        } catch ( IOException e ) {
            return 0;
        }
        return numComments;
    }


    /** Feature selection */
    public static List<String> applyFeatureSelection(Instances training, Instances testing, double percentageMajorityClass) throws Exception {

        // Create filter
        AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);
        filter.setEvaluator(eval);
        filter.setSearch(search);

        filter.setInputFormat(training);
        // Apply the filter to the training set
        Instances filteredTraining =  Filter.useFilter(training, filter);
        // Apply the filter to the test set
        Instances testingFiltered = Filter.useFilter(testing, filter);
        int numAttrFiltered = filteredTraining.numAttributes();
        filteredTraining.setClassIndex(numAttrFiltered - 1);
        testingFiltered.setClassIndex(numAttrFiltered - 1);

        // Apply sampling to evaluate over filtered dataset
        return applySampling(filteredTraining, testingFiltered, percentageMajorityClass, "True");


    }


    /** This function build the ARFF file relative to the dataset
     */
    public static void addResult(Evaluation eval, List<String> result, String classifierAbb, String sampling, String featureSelection) {

        result.add(retrieveMetrics(eval,classifierAbb, sampling, featureSelection));

    }

    /** This function build apply the specified filter with the sampling technique to the evaluator
     */
    public static Evaluation applyFilterForSampling(FilteredClassifier fc, Evaluation eval, Instances training, Instances testing, AbstractClassifier classifierName) {

        try {
            if (fc != null) {
                fc.setClassifier(classifierName);
                fc.buildClassifier(training);
                eval.evaluateModel(fc, testing);

            } else {
                eval.evaluateModel(classifierName, testing);

            }
        } catch (Exception e) {
            System.out.println("Attenzione. Classe minoritaria insufficiente per SMOTE.");
        }
        return eval;
    }

    /** Apply sampling to filtered dataset with classifier (IBK, RF, NB) and evaluate
     */
    public static List<String> applySampling(Instances training, Instances testing, double percentageMajorityClass, String featureSelection) throws Exception {

        ArrayList<String> result = new ArrayList<>();

        IBk classifierIBk = new IBk();
        RandomForest classifierRF = new RandomForest();
        NaiveBayes classifierNB = new NaiveBayes();

        int numAttrNoFilter = training.numAttributes();
        training.setClassIndex(numAttrNoFilter - 1);
        testing.setClassIndex(numAttrNoFilter - 1);

        // Build the classifier
        classifierNB.buildClassifier(training);
        classifierRF.buildClassifier(training);
        classifierIBk.buildClassifier(training);
        // Get an evaluation object

        // Evaluate with no sampling e no feature selection
        Evaluation eval;
        eval = new Evaluation(training);
        addResult(applyFilterForSampling(null, eval, training, testing, classifierRF), result, "RF", NO_SAMPLING, featureSelection);

        eval = new Evaluation(training);
        addResult(applyFilterForSampling(null, eval, training, testing, classifierIBk), result, "IBk", NO_SAMPLING, featureSelection);

        eval = new Evaluation(training);
        addResult(applyFilterForSampling(null, eval, training, testing, classifierNB), result, "NB", NO_SAMPLING, featureSelection);

        // Apply undersampling
        FilteredClassifier fc = new FilteredClassifier();
        SpreadSubsample underSampling = new SpreadSubsample();
        underSampling.setInputFormat(training);
        String[] opts = new String[]{ "-M", "1.0"};
        underSampling.setOptions(opts);
        fc.setFilter(underSampling);

        eval = new Evaluation(training);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierRF), result, "RF", UNDER_SAMPLING, featureSelection);

        eval = new Evaluation(training);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierIBk), result, "IBk", UNDER_SAMPLING, featureSelection);

        eval = new Evaluation(training);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierNB), result, "NB", UNDER_SAMPLING, featureSelection);

        // Apply oversampling
        fc = new FilteredClassifier();
        Resample overSampling = new Resample();
        overSampling.setInputFormat(training);
        String[] optsOverSampling = new String[]{"-B", "1.0", "-Z", String.valueOf(2*percentageMajorityClass*100)};
        overSampling.setOptions(optsOverSampling);
        fc.setFilter(overSampling);

        eval = new Evaluation(testing);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierRF), result, "RF", OVER_SAMPLING, featureSelection);

        eval = new Evaluation(testing);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierIBk), result, "IBk", OVER_SAMPLING, featureSelection);

        eval = new Evaluation(testing);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierNB), result, "NB", OVER_SAMPLING, featureSelection);

        // Apply SMOTE
        weka.filters.supervised.instance.SMOTE smote = new SMOTE();
        fc = new FilteredClassifier();
        smote.setInputFormat(training);
        fc.setFilter(smote);

        // Evaluate the three classifiers
        eval = new Evaluation(testing);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierRF), result, "RF", SMOTE, featureSelection);

        eval = new Evaluation(testing);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierIBk), result, "IBk", SMOTE, featureSelection);

        eval = new Evaluation(testing);
        addResult(applyFilterForSampling(fc, eval, training, testing, classifierNB), result, "NB", SMOTE, featureSelection);

        return result;


    }

}

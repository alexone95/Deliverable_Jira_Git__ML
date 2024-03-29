package utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import model.CommitDetails;
import model.CommitFileDetails;
import model.Issue;
import org.eclipse.jgit.diff.Edit;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private Utils(){

    }
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    private static final String TRAINING = "_training.arff";
    private static final String TESTING = "_testing.arff";

    private static final String OVER_SAMPLING = "Over sampling";
    private static final String UNDER_SAMPLING = "Under sampling";
    private static final String SMOTE = "Smote";
    private static final String NO_SAMPLING = "No sampling";

    /* Metodo per la stampa di caratteristiche dei file */
    public static void printFilesInfoprintFilesInfo(List<CommitFileDetails> commitFileDetailsList){
        for(CommitFileDetails commitFileDetails : commitFileDetailsList){
            LOGGER.info("\n");
            LOGGER.info("FILE NAME: " + commitFileDetails.getModifiedFileName());
            LOGGER.info("DELETED LOC: " + commitFileDetails.getDeletedLOC() + "| LOC: " + commitFileDetails.getLoc() +
                    "| ADDED LOC: " + commitFileDetails.getAddedLOC() + "| AGE: " + commitFileDetails.getAge());
            LOGGER.info("BUGGY: " + commitFileDetails.isBuggy());
            LOGGER.info("\n");
        }
    }

    /* Metodo per la stampa della lista delle issue */
    public static void printTicketList(List<Issue> issues){
        for(Issue commitIssue : issues){
            LOGGER.info("\n " + commitIssue.getIssueKey());
            LOGGER.info("RESOLUTION DATE: " + commitIssue.getResolutionDate() );
            LOGGER.info("CREATION DATE: " + commitIssue.getCreationDate() );
            LOGGER.info("FIX VERSION: " + commitIssue.getFixVersion() + "| OPENING VERSION: " + commitIssue.getOpeningVersion() + "| INJECTED VERSION: " + commitIssue.getInjectedVersion());
            for(String affected_version: commitIssue.getAffectedVersion()){
                LOGGER.log(Level.SEVERE, "AFFECTED VERSION: {0}", affected_version );
            }
            for(Integer affected_versionindex: commitIssue.getAffectedVersionIndex()){
                LOGGER.log(Level.SEVERE, "AFFECTED VERSION INDEX: {0}", affected_versionindex );
            }

        }
    }

    /* Metodo per la stampa dei dettagli delle commit */
    public static void printCommitDetailsFromTicket(List<Issue> issues){
        for(Issue commitIssue : issues){
            LOGGER.info("\n");
            LOGGER.info(commitIssue.getIssueKey());
            for(CommitDetails commitDetails : commitIssue.getCommits()){
                LOGGER.info("ADDED LOC: " + commitDetails.getAddedLoc());
                LOGGER.info("DELETED LOC: " + commitDetails.getDeletedLoc());
                LOGGER.info("PERSON: " + commitDetails.getPerson().getName());
                LOGGER.info("COMMIT DATE: " + commitDetails.getCommitDate());
            }
        }
    }

    /* Metodo per la stampa della lista delle issue */
    public static void printFullInfoFromTicket(List<Issue> issues){
        for(Issue commitIssue : issues){
            LOGGER.info("\n");
            LOGGER.info(commitIssue.getIssueKey());
            LOGGER.info("RESOLUTION DATE: " + commitIssue.getResolutionDate() + "| CREATION DATE: " + commitIssue.getCreationDate() );
            for(CommitDetails commitDetails : commitIssue.getCommits()){
                LOGGER.info("COMMIT HASH: " + commitDetails.getCommit().getName() + "| COMMIT DATE: "
                        + commitDetails.getCommitDate() + "| VERSION: " + commitDetails.getVersion() + "| PERSON:" +
                        commitDetails.getPerson().getName());
                printFilesInfoprintFilesInfo(commitDetails.getFilesChanged());
                LOGGER.info("\n");
            }
        }
    }

    public static void printFileDetailsFromTicket(List<Issue> issues){
        for(Issue commitIssue : issues){
            LOGGER.info("\n");
            LOGGER.info("KEY: " + commitIssue.getIssueKey());
            for(CommitDetails commitDetails : commitIssue.getCommits()){
                printFilesInfoprintFilesInfo(commitDetails.getFilesChanged());
            }
        }
    }

    /* Funzione utilizzata per contare le righe di codice di un file .java */
    public static long countLineBufferedReader(String fileText) {
        long lines = 0;
        String line= "";
        try (BufferedReader reader = new BufferedReader(new StringReader(fileText))) {
            while(line != null ){
                line = reader.readLine();
                lines++;
            }
        } catch (IOException e) {
            return 0;
        }
        return lines;

    }

    /*
        Questo metodo è di appoggio alla creazione del file ARFF
    */
    public static int appendToCSV(FileWriter csvWriter, String line) throws IOException {
        int counterDefective = 0;
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

    /*
        Applica la tecnica di walk forward per la costruzione del training set, costruendo l'ARFF
    */
    public static List<Integer> walkForwardTraining(String projectName, int trainingLimit) throws IOException {

        int counterElement = 0;
        int counterDefective = 0;

        ArrayList<Integer> counterList = new ArrayList<>();

        // Creazione del file ARFF necessario alla classificazione
        try (FileWriter csvWriter = new FileWriter(projectName + TRAINING)) {

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
            csvWriter.append("@attribute numPrivateAttributesOrMethods real\n");
            csvWriter.append("@attribute numPublicAttributesOrMethods real\n");
            csvWriter.append("@attribute Buggy {Yes, No}\n\n");
            csvWriter.append("@data\n");

            // Apriamo il dataset CSV creato nella
            try (BufferedReader br = new BufferedReader(new FileReader("src/csv_output/" + projectName + "_dataset.csv"))){

                // Skip della prima riga essendo che contiene le intestazioni
                String line = br.readLine();

                // Legge fino all'ultima riga
                while ((line = br.readLine()) != null){
                    // Controlla se la versione è inferiore al limite imposto (metà delle versioni)
                    if (Integer.parseInt(line.split(",")[0]) <= trainingLimit ) {
                        counterElement = counterElement + 1;
                        counterDefective = counterDefective + appendToCSV(csvWriter, line);
                    }
                }

                csvWriter.flush();
                counterList.add(counterElement);
                counterList.add(counterDefective);

                return counterList;
            }
        }
    }

    /*
        Applica la tecnica di walk forward per la costruzione del testing set, costruendo l'ARFF
    */
    public static List<Integer> walkForwardTesting(String projectName, int testing) throws IOException, NoVersionException {

        int counterElement = 0;
        int counterDefective = 0;
        ArrayList<Integer> counterList = new ArrayList<>();

        try (FileWriter csvWriter = new FileWriter(projectName + TESTING)) {

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
            csvWriter.append("@attribute numPrivateAttributesOrMethods real\n");
            csvWriter.append("@attribute numPublicAttributesOrMethods real\n");
            csvWriter.append("@attribute Buggy {Yes, No}\n\n");
            csvWriter.append("@data\n");

            try (BufferedReader br = new BufferedReader(new FileReader("src/csv_output/" + projectName + "_dataset.csv"))){
                // Salta l'intestazione
                String line = br.readLine();

                while ((line = br.readLine()) != null){
                    if (Integer.parseInt(line.split(",")[0]) == testing ) {
                        counterElement = counterElement + 1;
                        counterDefective = counterDefective + appendToCSV(csvWriter, line);
                    }
                }

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

    /* Questo metodo ritorna una stringa contenente le rilevazioni prese dall'evaluator */
    public static String retrieveMetrics(Evaluation eval, String classifier, String balancing, String featureSelection) {

        return classifier + "," + balancing + "," + featureSelection + "," + eval.truePositiveRate(1)  + "," + eval.falsePositiveRate(1)  + "," + eval.trueNegativeRate(1)  + "," + eval.falseNegativeRate(1)  + "," + eval.precision(1)  + "," + eval.recall(1)  + "," + eval.areaUnderROC(1)  + "," + eval.kappa() + "\n";
    }

    /* Questo metodo ritorna il numero di attributi privati in un file .java */
    public static int getPrivateAttributesOrMethods(String fileText){
        int numPrivateAttributesOrMethods = 0;
        String regex = "\\bprivate\\b"; //regex che vada a prendere la singola parola private
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        try (BufferedReader reader = new BufferedReader(new StringReader(fileText))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                matcher = pattern.matcher(line);
                if(matcher.find()){
                    numPrivateAttributesOrMethods ++;
                }
            }
        } catch (IOException e) {
            return 0;
        }
        return numPrivateAttributesOrMethods;
    }

    /* Questo metodo ritorna il numero di attributi pubblici in un file .java */
    public static int getPublicAttributesOrMethods(String fileText){
        int numPublicAttributesOrMethods = 0;
        String regex = "\\bpublic\\b"; //regex che vada a prendere la singola parola public
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        try (BufferedReader reader = new BufferedReader(new StringReader(fileText))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                matcher = pattern.matcher(line);
                if(matcher.find()){
                    numPublicAttributesOrMethods ++;
                }
            }
        } catch (IOException e) {
            return 0;
        }
        return numPublicAttributesOrMethods;
    }

    /* Funzione d'utility per discriminare le variazioni di righe nei commit */
    public static String modifiedLocRetrieverMode(Edit edit){
        if (edit.getBeginA() < edit.getEndA() && edit.getBeginB() < edit.getEndB()){
            return "REPLACED";
        }
        if (edit.getBeginA() < edit.getEndA() && edit.getBeginB() == edit.getEndB()){
            return "DELETED";
        }
        if (edit.getBeginA() == edit.getEndA() && edit.getBeginB() < edit.getEndB()){
            return "ADDED";
        }
        return "";
    }

    /* Funzione che determina la bugginess */
    public static boolean retrieveBugginess(int commitVersion, int fixVersion, int injectedVersion){
        return (commitVersion < fixVersion) && (commitVersion >= injectedVersion);
    }

    public static List<Integer> fillAffectedVersionList(List<String> affectedVersions, Multimap<LocalDate, String> versionMap){
        ArrayList<Integer> avs = new ArrayList<>();
        for ( String version : affectedVersions ){
            for( LocalDate date : versionMap.keySet() ){
                if ( Iterables.get(versionMap.get(date),0).equals( version )){
                    avs.add( Integer.valueOf( Iterables.getLast( versionMap.get(date) )) );
                    break;
                }
            }
        }
        return avs;
    }

    /* Funzione che applica la feature selection */
    public static List<String> applyFeatureSelection(Instances training, Instances testing, double percentageMajorityClass) throws Exception {

        AttributeSelection filter = new AttributeSelection();
        CfsSubsetEval eval = new CfsSubsetEval();
        GreedyStepwise search = new GreedyStepwise();
        search.setSearchBackwards(true);
        filter.setEvaluator(eval);
        filter.setSearch(search);

        filter.setInputFormat(training);
        // Applica il filtro al training set
        Instances filteredTraining =  Filter.useFilter(training, filter);
        // Applica il filtro al testing set
        Instances testingFiltered = Filter.useFilter(testing, filter);
        int numAttrFiltered = filteredTraining.numAttributes();
        filteredTraining.setClassIndex(numAttrFiltered - 1);
        testingFiltered.setClassIndex(numAttrFiltered - 1);

        // Applica il sampling sul database filtrato
        return applySampling(filteredTraining, testingFiltered, percentageMajorityClass, "True");

    }

    public static double average( List<Double> array ){
        double avg;
        double sum = 0.0;
        for ( double p : array ){
            sum += p;
        }
        avg = ( sum/( array.size() ) );
        return avg;
    }

    public static void addResult(Evaluation eval, List<String> result, String classifierAbb, String sampling, String featureSelection) {
        result.add(retrieveMetrics(eval,classifierAbb, sampling, featureSelection));

    }

    /*
        Applica il filtro relativo al sampling (OVER, UNDER, NO SAMPLING)
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
            LOGGER.info("Attenzione. Classe minoritaria insufficiente per SMOTE.");
        }
        return eval;
    }

    /*
        Applica il sampling ai tre classificatori e ne effettua l'evaluate
    */
    public static List<String> applySampling(Instances training, Instances testing, double percentageMajorityClass, String featureSelection) throws Exception {

        ArrayList<String> result = new ArrayList<>();

        IBk classifierIBk = new IBk();
        RandomForest classifierRF = new RandomForest();
        NaiveBayes classifierNB = new NaiveBayes();

        int numAttrNoFilter = training.numAttributes();
        training.setClassIndex(numAttrNoFilter - 1);
        testing.setClassIndex(numAttrNoFilter - 1);

        classifierNB.buildClassifier(training);
        classifierRF.buildClassifier(training);
        classifierIBk.buildClassifier(training);

        // Valutazione del sistema senza feature selection e senza sampling
        addResult(applyFilterForSampling(null, new Evaluation(training), training, testing, classifierRF),
                result, "RF", NO_SAMPLING, featureSelection);

        addResult(applyFilterForSampling(null, new Evaluation(training), training, testing, classifierIBk),
                result, "IBk", NO_SAMPLING, featureSelection);

        addResult(applyFilterForSampling(null, new Evaluation(training), training, testing, classifierNB),
                result, "NB", NO_SAMPLING, featureSelection);

        // Applica l'undersampling
        FilteredClassifier fc = new FilteredClassifier();
        SpreadSubsample underSampling = new SpreadSubsample();
        underSampling.setInputFormat(training);
        String[] opts = new String[]{ "-M", "1.0"};
        underSampling.setOptions(opts);
        fc.setFilter(underSampling);

        addResult(applyFilterForSampling(fc, new Evaluation(training), training, testing, classifierRF),
                result, "RF", UNDER_SAMPLING, featureSelection);

        addResult(applyFilterForSampling(fc, new Evaluation(training), training, testing, classifierIBk),
                result, "IBk", UNDER_SAMPLING, featureSelection);

        addResult(applyFilterForSampling(fc, new Evaluation(training), training, testing, classifierNB),
                result, "NB", UNDER_SAMPLING, featureSelection);

        // Applica l'oversampling
        fc = new FilteredClassifier();
        Resample overSampling = new Resample();
        overSampling.setInputFormat(training);
        String[] optsOverSampling = new String[]{"-B", "1.0", "-Z", String.valueOf(2*percentageMajorityClass*100)};
        overSampling.setOptions(optsOverSampling);
        fc.setFilter(overSampling);

        addResult(applyFilterForSampling(fc, new Evaluation(testing), training, testing, classifierRF),
                result, "RF", OVER_SAMPLING, featureSelection);

        addResult(applyFilterForSampling(fc, new Evaluation(testing), training, testing, classifierIBk),
                result, "IBk", OVER_SAMPLING, featureSelection);

        addResult(applyFilterForSampling(fc, new Evaluation(testing), training, testing, classifierNB),
                result, "NB", OVER_SAMPLING, featureSelection);

        // Applica SMOTE
        weka.filters.supervised.instance.SMOTE smote = new SMOTE();
        fc = new FilteredClassifier();
        smote.setInputFormat(training);
        fc.setFilter(smote);

        addResult(applyFilterForSampling(fc, new Evaluation(testing), training, testing, classifierRF),
                result, "RF", SMOTE, featureSelection);

        addResult(applyFilterForSampling(fc, new Evaluation(testing), training, testing, classifierIBk),
                result, "IBk", SMOTE, featureSelection);

        addResult(applyFilterForSampling(fc, new Evaluation(testing), training, testing, classifierNB),
                result, "NB", SMOTE, featureSelection);

        return result;
    }

}

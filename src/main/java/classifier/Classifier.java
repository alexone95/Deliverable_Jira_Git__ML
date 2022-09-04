package classifier;

import utils.NoVersionException;
import utils.Utils;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Classifier {
    private static final String TRAINING = "_training.arff";
    private static final String TESTING = "_testing.arff";
    private static final String PROJ_NAME = "BOOKKEEPER";

    private Classifier() {
    }

    public static void train() throws Exception{
        List<Integer> resultTesting;
        List<Integer> resultTraining;

        // Numero di versioni considerate
        int limit = 8;  //8 per Bookkeeper e 14 per openJPA

        try (FileWriter csvWriter = new FileWriter("src/output/" + PROJ_NAME + "_out.csv")) {

            csvWriter.append("Dataset, #Version Training, % Training, % Defect Training, % Defect Testing, Classifier, Balancing, FeatureSelection, TP, FP, TN, FN, Precision, Recall, ROC Area, Kappa\n");

            for (int i = 1; i < limit; i++) {

                // Crea il training set fino alla i-esima versione
                resultTraining = Utils.walkForwardTraining(PROJ_NAME, i);

                // Crea il file ARFF per il testing set, con i+1 versioni
                try {
                    resultTesting = Utils.walkForwardTesting(PROJ_NAME, i + 1);
                } catch (NoVersionException e) {
                    continue;
                }

                double percentTraining = resultTraining.get(0) / (double) (resultTraining.get(0) + resultTesting.get(0));
                double percentDefectTraining = resultTraining.get(1) / (double) resultTraining.get(0);
                double percentDefectTesting = resultTesting.get(1) / (double) resultTesting.get(0);
                double percentageMajorityClass = 1 - ((resultTraining.get(1) + resultTesting.get(1)) / (double) (resultTraining.get(0) + resultTesting.get(0)));

                // Legge i file ARFF creati precedentemente
                DataSource source1 = new DataSource(PROJ_NAME + TRAINING);
                Instances training = source1.getDataSet();

                DataSource source2 = new DataSource(PROJ_NAME + TESTING);
                Instances testing = source2.getDataSet();

                // Sampling
                List<String> samplingResult = Utils.applySampling(training, testing, percentageMajorityClass, "False");
                for (String result : samplingResult) {
                    csvWriter.append(PROJ_NAME + "," + i  + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
                }

                // Feature selection
                List<String> featureSelectionResult = Utils.applyFeatureSelection(training, testing, percentageMajorityClass);
                for (String result : featureSelectionResult) {
                    csvWriter.append(PROJ_NAME + "," + i  + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
                }
            }

            // Cancello i file ARFF temporanei
            Files.deleteIfExists(Paths.get(PROJ_NAME + TESTING));
            Files.deleteIfExists(Paths.get(PROJ_NAME + TRAINING));
            csvWriter.flush();
        }
    }
}

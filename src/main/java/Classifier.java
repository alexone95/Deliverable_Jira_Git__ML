import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import weka.core.Instances;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.lazy.IBk;

public class Classifier {
    private static final String TRAINING = "_training.arff";
    private static final String TESTING = "_testing.arff";
    private static final String projName = "STORM";

    public static void train() throws Exception{
        List<Integer> resultTesting, resultTraining;

        // Declare the number of revision for each dataset
        int limit = 14;

        // Open the FileWriter for the output file
        try (FileWriter csvWriter = new FileWriter("src/output/" + projName+ "_out.csv")) {

            // Append the first line of the result file
            csvWriter.append("Dataset,# Training,% Training,% Defect Training,%Defect Testing,Classifier,Balancing,FeatureSelection,TP,FP,TN,FN,Precision,Recall,ROC Area,Kappa\n");

            // Iterate over the single version for the WalkForward technique...
            for (int i = 1; i < limit; i++) {

                // Create the ARFF file for the training, till the i-th version
                resultTraining = Utils.walkForwardTraining(projName, i);

                // Create the ARFF file for testing, with the i+1 version, checking out for Exception
                try {
                    resultTesting = Utils.walkForwardTesting(projName, i + 1);
                } catch (NoVersionException e) {
                    continue;
                }

                // Evaluate percentage
                double percentTraining = resultTraining.get(0) / (double) (resultTraining.get(0) + resultTesting.get(0));
                double percentDefectTraining = resultTraining.get(1) / (double) resultTraining.get(0);
                double percentDefectTesting = resultTesting.get(1) / (double) resultTesting.get(0);
                double percentageMajorityClass = 1 - ((resultTraining.get(1) + resultTesting.get(1)) / (double) (resultTraining.get(0) + resultTesting.get(0)));

                // Read the Datasource created before and get each dataset
                DataSource source1 = new DataSource(projName + TRAINING);
                Instances training = source1.getDataSet();
                DataSource source2 = new DataSource(projName + TESTING);
                Instances testing = source2.getDataSet();

                // Get the number of attributes
                int numAttr = training.numAttributes();

                /* Set the number of attributes for each dataset,
                 * remembering that the last attribute is the one that we want to predict
                 * */
                training.setClassIndex(numAttr - 1);
                testing.setClassIndex(numAttr - 1);

                // Get the three classifier
                IBk classifierIBk = new IBk();
                RandomForest classifierRF = new RandomForest();
                NaiveBayes classifierNB = new NaiveBayes();

                // Build the classifier
                classifierNB.buildClassifier(training);
                classifierRF.buildClassifier(training);
                classifierIBk.buildClassifier(training);

                // Get an evaluation object
                Evaluation eval = new Evaluation(training);

                // Evaluate each model and add the result to the output file
                eval.evaluateModel(classifierNB, testing);
                csvWriter.append(projName + "," + i + ",NaiveBayes," + eval.precision(0) + "," + eval.recall(0) + "," + eval.areaUnderROC(0) + "," + eval.kappa() + "\n");

                eval.evaluateModel(classifierRF, testing);
                csvWriter.append(projName + "," + i + ",RandomForest," + eval.precision(0) + "," + eval.recall(0) + "," + eval.areaUnderROC(0) + "," + eval.kappa() + "\n");

                eval.evaluateModel(classifierIBk, testing);
                csvWriter.append(projName + "," + i + ",IBk," + eval.precision(0) + "," + eval.recall(0) + "," + eval.areaUnderROC(0) + "," + eval.kappa() + "\n");

                // Apply sampling
                List<String> samplingResult = Utils.applySampling(training, testing, percentageMajorityClass, "False");
                for (String result : samplingResult) {
                    csvWriter.append(projName + "," + i  + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
                }

                // Feature selection
                List<String> featureSelectionResult = Utils.applyFeatureSelection(training, testing, percentageMajorityClass);
                for (String result : featureSelectionResult) {
                    csvWriter.append(projName + "," + i  + "," + percentTraining  + "," + percentDefectTraining  + "," + percentDefectTesting +"," + result);
                }
            }

            // Delete the temp file
            Files.deleteIfExists(Paths.get(projName + TESTING));
            Files.deleteIfExists(Paths.get(projName + TRAINING));
            csvWriter.flush();
        }
    }
}

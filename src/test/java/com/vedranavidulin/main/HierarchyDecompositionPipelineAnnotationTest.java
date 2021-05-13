/*
 * Copyright (c) 2021 Vedrana Vidulin
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.vedranavidulin.main;

import com.google.common.collect.Table;
import com.vedranavidulin.Utils;
import com.vedranavidulin.evaluation.AreaUnderAveragePrecisionRecallCurve;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import static com.vedranavidulin.data.DataReadWrite.*;
import static com.vedranavidulin.main.Settings.errorMsg;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vedrana Vidulin
 */
public class HierarchyDecompositionPipelineAnnotationTest {
    private static final File testAnnotationFolder = new File("test-project/annotations/");
    private static float auprc;

    @BeforeAll
    public static void hierarchyDecompositionPipelineAnnotation() throws Exception {
        if (!testAnnotationFolder.exists() && !testAnnotationFolder.mkdirs())
            errorMsg("Can't create test folder " + testAnnotationFolder.getAbsolutePath());

        String datasetPath = "src/test/resources/enron.arff.zip";
        String trainingSetPath = "src/test/resources/enron-training_set.arff";
        String unlabelledSetPath = "src/test/resources/enron-unlabelled_set.arff";
        if (!new File(trainingSetPath + ".zip").exists() || !new File(unlabelledSetPath + ".zip").exists())
            createTrainingAndUnlabelledSets(new File(datasetPath), 70, trainingSetPath, unlabelledSetPath);

        HierarchyDecompositionPipeline.runPipeline(new Utils().getSettingsForAnnotation(trainingSetPath + ".zip", unlabelledSetPath + ".zip",
                                                   testAnnotationFolder.getParentFile().getAbsolutePath()));

        auprc = computeTestAUPRC(new File(datasetPath), new File(unlabelledSetPath + ".zip"));
    }

    @Test
    public void baseline_classifier_has_expected_AUPRC_on_test_set() {
        assertEquals(0.8243f, auprc);
    }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testAnnotationFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testAnnotationFolder.getParentFile().getAbsolutePath()); }
    }

    private static float computeTestAUPRC(File inDatasetFile, File inUnlabelledSetFile) throws IOException, ExecutionException, InterruptedException {
        Table<String, String, Float> exampleLabelConfidence = new Utils().loadExampleLabelConfidence(new File(testAnnotationFolder + "/Baseline/Confidences.csv.gz"));

        Set<String> unlabelledSetExampleIDs = new HashSet<>();
        BufferedReader br = findReaderType(inUnlabelledSetFile);
        String line;
        while((line = br.readLine()) != null)
            if (!line.startsWith("@") && !line.isEmpty())
                unlabelledSetExampleIDs.add(line.substring(0, line.indexOf(",")));

        Map<String, Set<String>> label2examples = new HashMap<>();
        br = findReaderType(inDatasetFile);
        while((line = br.readLine()) != null)
            if (!line.startsWith("@") && !line.isEmpty()) {
                String exampleID = line.substring(0, line.indexOf(","));
                if (unlabelledSetExampleIDs.contains(exampleID)) {
                    String[] labels = line.substring(line.lastIndexOf(",") + 1).trim().split("@");
                    for (String label : labels) {
                        Set<String> examples = new HashSet<>();
                        if (label2examples.containsKey(label))
                            examples = label2examples.get(label);
                        examples.add(exampleID);
                        label2examples.put(label, examples);
                    }
                }
            }

        float microAvgAuprc = new AreaUnderAveragePrecisionRecallCurve().run(exampleLabelConfidence, label2examples, label2examples.keySet(),
                                    new File(testAnnotationFolder + "/Baseline/Area_under_average_precision_recall_curve.txt"));

        System.out.println("Area under average precision-recall curve: " + microAvgAuprc);

        return microAvgAuprc;
    }

    private static void createTrainingAndUnlabelledSets(File inDatasetFile, int percentageOfExamplesInTrainingSet, String trainingSetPath, String unlabelledSetPath) throws IOException {
        List<String> exampleIDs = new ArrayList<>();
        BufferedReader br = findReaderType(inDatasetFile);
        String line;
        while((line = br.readLine()) != null)
            if (!line.startsWith("@") && !line.isEmpty())
                exampleIDs.add(line.substring(0, line.indexOf(",")));

        Set<String> trainingSetExamples = new HashSet<>();
        int numTrainingSetExamples = Math.round((float)exampleIDs.size() * (float)percentageOfExamplesInTrainingSet / 100f);
        for (String exampleID : shuffleElementsInList(exampleIDs)) {
            if (trainingSetExamples.size() == numTrainingSetExamples)
                break;
            trainingSetExamples.add(exampleID);
        }
        System.out.println("Training set examples: " + trainingSetExamples.size());

        try (BufferedWriter bwTrainingSet = new BufferedWriter(new FileWriter(trainingSetPath))) {
            try (BufferedWriter bwUnlabelledSet = new BufferedWriter(new FileWriter(unlabelledSetPath))) {
                br = findReaderType(inDatasetFile);
                while((line = br.readLine()) != null)
                    if (line.startsWith("@") || line.isEmpty()) {
                        bwTrainingSet.write(line + "\n");
                        bwUnlabelledSet.write(line + "\n");
                    } else if (trainingSetExamples.contains(line.substring(0, line.indexOf(","))))
                        bwTrainingSet.write(line + "\n");
                    else
                        bwUnlabelledSet.write(line.substring(0, line.lastIndexOf(",") + 1) + "root\n");
            }
        }

        zipFile(trainingSetPath, trainingSetPath + ".zip");
        zipFile(unlabelledSetPath, unlabelledSetPath + ".zip");
        if (!new File(trainingSetPath).delete()) System.err.println("Can't delete " + trainingSetPath);
        if (!new File(unlabelledSetPath).delete()) System.err.println("Can't delete " + unlabelledSetPath);
    }

    private static List<String> shuffleElementsInList(List<String> inputList) {
        List<String> shuffledList = new ArrayList<>();

        int[] array = new int[inputList.size()];
        for (int i = 0; i < inputList.size(); i++)
            array[i] = i;

        Random r = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = r.nextInt(i);
            int tmp = array[index];
            array[index] = array[i];
            array[i] = tmp;
        }

        for (int index : array)
            shuffledList.add(inputList.get(index));

        return shuffledList;
    }
}

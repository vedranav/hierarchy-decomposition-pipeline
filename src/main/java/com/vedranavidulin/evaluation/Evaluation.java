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
package com.vedranavidulin.evaluation;

import com.google.common.collect.Table;
import static com.vedranavidulin.data.DataReadWrite.listFilesWithEnding;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import static org.apache.commons.math3.util.Precision.round;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.*;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author Vedrana Vidulin
 */
public class Evaluation {
    public static void evaluate(String decomposition) throws IOException, InterruptedException, ExecutionException {
        PredictionExtractionTools pet = new PredictionExtractionTools();
        
        Table<String, String, Float> exampleLabelConfidence;
        if (decomposition.equals(decompositions[3]) || decomposition.equals(decompositions[4]))
            exampleLabelConfidence = pet.collectPredictions_PartialDecompositions(
                                                    listFilesWithEnding(settings.getCrossValidationPath() + decomposition + "/Results/", ".pred.arff.gz"),
                                                    new File(settings.getCrossValidationPath() + decomposition + "/Confidences.csv.gz"));
        else
           exampleLabelConfidence = pet.collectPredictions_Baseline_CompleteDecompositions(
                                                    listFilesWithEnding(settings.getCrossValidationPath() + decomposition + "/Results/", ".pred.arff.gz"),
                                                    new File(settings.getCrossValidationPath() + decomposition + "/Confidences.csv.gz"), true);
        
        
        Map<String, Set<String>> label2examples = baselineDataset.getLabel2Examples();

        System.out.println("Computing precision-recall and AUPRC");
        Map<String, Float> label2AUPRC = new PrecisionRecallAndAUPRC().run(exampleLabelConfidence, label2examples, new File(settings.getCrossValidationPath() + decomposition));
        
        System.out.println("Computing ROC and AUC");
        Map<String, Float> label2AUC = new ROCAndAUC().run(exampleLabelConfidence, label2examples, new File(settings.getCrossValidationPath() + decomposition));

        System.out.println("Computing area under average precision-recall curve");
        Set<String> theMostSpecificLabels = baselineDataset.getTheMostSpecificLabels();
        float microAvgAuprc = new AreaUnderAveragePrecisionRecallCurve().run(exampleLabelConfidence, label2examples, theMostSpecificLabels,
                                    new File(settings.getCrossValidationPath() + decomposition + "/Area_under_average_precision_recall_curve.txt"));

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(settings.getCrossValidationPath() + decomposition + "/Evaluation_report.csv"))) {
            SummaryStatistics avgAUPRC = new SummaryStatistics();
            SummaryStatistics avgAUC = new SummaryStatistics();

            bw.write("Label");
            for (float t : settings.getThresholds())
                bw.write(",TP-" + t + ",FP-" + t + ",TN-" + t + ",FN-" + t + ",Precision-" + t + ",Recall-" + t + ",F-measure-" + t + ",Accuracy-" + t);
            bw.write(",AUPRC,AUC,Included in averaged measures\n");

            for (String label : exampleLabelConfidence.columnKeySet()) {
                bw.write(label);
                for (float t : settings.getThresholds()) {
                    int TP = 0;
                    int FP = 0;
                    int TN = 0;
                    int FN = 0;

                    for (String example : exampleLabelConfidence.rowKeySet()) {
                        float conf = exampleLabelConfidence.get(example, label);
                        if (conf >= t && baselineDataset.getLabelValues().get(example).contains(label))
                            TP++;
                        else if (conf >= t && !baselineDataset.getLabelValues().get(example).contains(label))
                            FP++;
                        else if (conf < t && !baselineDataset.getLabelValues().get(example).contains(label))
                            TN++;
                        else if (conf < t && baselineDataset.getLabelValues().get(example).contains(label))
                            FN++;
                    }

                    float precision = ((TP + FP) == 0 ? 0 : round((float) TP / (float) (TP + FP), 4));
                    float recall = ((TP + FN) == 0 ? 0 : round((float) TP / (float) (TP + FN), 4));
                    float fMeasure = ((precision + recall) == 0 ? 0 : round((2 * precision * recall) / (precision + recall), 4));
                    float accuracy = round((float) (TP + TN) / (float) (TP + FP + TN + FN), 4);

                    bw.write("," + TP + "," + FP + "," + TN + "," + FN + "," + precision + "," + recall + "," + fMeasure +
                            "," + accuracy);
                }
                bw.write("," + label2AUPRC.get(label) + "," + label2AUC.get(label) + "," +
                        (theMostSpecificLabels.contains(label) ? "Yes" : "No") + "\n");

                if (theMostSpecificLabels.contains(label)) {
                    avgAUPRC.addValue(label2AUPRC.get(label));
                    avgAUC.addValue(label2AUC.get(label));
                }
            }

            bw.write("\nAverages:\n");
            bw.write("AUPRC," + round(avgAUPRC.getMean(), 4) + "\n");
            bw.write("AUC," + round(avgAUC.getMean(), 4) + "\n");
            bw.write("Area under average precision-recall curve," + microAvgAuprc + "\n");
        }
    }
}

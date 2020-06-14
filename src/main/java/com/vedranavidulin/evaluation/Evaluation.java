/*
 * Copyright (c) 2020 Vedrana Vidulin <vedrana.vidulin@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vedranavidulin.evaluation;

import com.google.common.collect.Table;
import static com.vedranavidulin.data.DataReadWrite.listFilesWithEnding;
import com.vedranavidulin.data.Dataset;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import static org.apache.commons.math3.util.Precision.round;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.decompositions;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class Evaluation {
    public static void evaluate(String decomposition, String outFolderPath, Dataset baselineDataset, Set<Double> thresholds, int numProcessors) throws IOException, InterruptedException, ExecutionException {
        PredictionExtractionTools pet = new PredictionExtractionTools(baselineDataset);
        
        Table<String, String, Double> exampleLabelConfidence;
        if (decomposition.equals(decompositions[3]) || decomposition.equals(decompositions[4]))
            exampleLabelConfidence = pet.collectPredictions_PartialDecompositions(
                                                    listFilesWithEnding(outFolderPath + "/" + decomposition + "/Results/", ".pred.arff.gz"),
                                                    new File(outFolderPath + decomposition + "/Confidences.csv.gz"));
        else
           exampleLabelConfidence = pet.collectPredictions_Baseline_CompleteDecompositions(
                                                    listFilesWithEnding(outFolderPath + "/" + decomposition + "/Results/", ".pred.arff.gz"),
                                                    new File(outFolderPath + "/" + decomposition + "/Confidences.csv.gz"));
        
        
        Map<String, Set<String>> label2examples = baselineDataset.getLabel2Examples();

        System.out.println("Computing precision-recall and AUPRC");
        Map<String, Double> label2AUPRC = new PrecisionRecallAndAUPRC().run(exampleLabelConfidence, label2examples, numProcessors, new File(outFolderPath + decomposition));
        
        System.out.println("Computing ROC and AUC");
        Map<String, Double> label2AUC = new ROCAndAUC().run(exampleLabelConfidence, label2examples, new File(outFolderPath + decomposition), numProcessors);

        System.out.println("Computing area under average precision-recall curve");
        Set<String> theMostSpecificLabels = baselineDataset.getTheMostSpecificLabels();
        double microAvgAuprc = new AreaUnderAveragePrecisionRecallCurve().run(exampleLabelConfidence, label2examples,
                                    theMostSpecificLabels, numProcessors,
                                    new File(outFolderPath + "/" + decomposition + "/Area_under_average_precision_recall_curve.txt"));
        
        compileReport(exampleLabelConfidence, baselineDataset, label2AUPRC, label2AUC, microAvgAuprc, thresholds, theMostSpecificLabels,
                        new File(outFolderPath + "/" + decomposition + "/Evaluation_report.csv"));
    }
    
    public static void compileReport(Table<String, String, Double> exampleLabelConfidence, Dataset baselineDataset,
                                      Map<String, Double> label2AUPRC, Map<String, Double> label2AUC, double microAvgAuprc,
                                      Set<Double> thresholds, Set<String> theMostSpecificLabels, File outSummaryFile) throws IOException {
        Map<String, Set<String>> exampleId2labels = baselineDataset.getLabelValues();
        
        SummaryStatistics avgAUPRC = new SummaryStatistics();
        SummaryStatistics avgAUC = new SummaryStatistics();
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(outSummaryFile));
        
        bw.write("Label");
        for (double t : thresholds)
            bw.write(",TP-" + t + ",FP-" + t + ",TN-" + t + ",FN-" + t + ",Precision-" + t + ",Recall-" + t + ",F-measure-" + t +
                     ",Accuracy-" + t);
        bw.write(",AUPRC,AUC,Included in averaged measures\n");
        
        for (String label : exampleLabelConfidence.columnKeySet()) {
            bw.write(label);
            for (double t : thresholds) {
                int TP = 0;
                int FP = 0;
                int TN = 0;
                int FN = 0;
                
                for (String example : exampleLabelConfidence.rowKeySet()) {
                    double conf = exampleLabelConfidence.get(example, label);
                    if (conf >= t && exampleId2labels.get(example).contains(label))
                        TP++;
                    else if (conf >= t && !exampleId2labels.get(example).contains(label))
                        FP++;
                    else if (conf < t && !exampleId2labels.get(example).contains(label))
                        TN++;
                    else if (conf < t && exampleId2labels.get(example).contains(label))
                        FN++;
                }
                
                double precision = ((TP + FP) == 0 ? 0 : round((double)TP / (double)(TP + FP), 4));
                double recall = ((TP + FN) == 0 ? 0 : round((double)TP / (double)(TP + FN), 4));
                double fMeasure = ((precision + recall) == 0 ? 0 : round((2 * precision * recall) / (precision + recall), 4));
                double accuracy = round((double)(TP + TN) / (double)(TP + FP + TN + FN), 4);
                
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
        
        bw.close();
    }
}

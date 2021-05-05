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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import static org.apache.commons.math3.util.Precision.round;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.settings;

/**
 *
 * @author Vedrana Vidulin
 */
public class AreaUnderAveragePrecisionRecallCurve {
    private static Map<String, Set<String>> label2examples;
    private static Table<String, String, Float> exampleLabelConfidence;
    private static List<String> labels;
    
    public AreaUnderAveragePrecisionRecallCurve() {}
    
    public float run(Table<String, String, Float> exampleLabelConfidence, Map<String, Set<String>> label2examples, Set<String> theMostSpecificLabels, File outFile) throws IOException, InterruptedException, ExecutionException {
        float auprc;

        AreaUnderAveragePrecisionRecallCurve.label2examples = label2examples;
        AreaUnderAveragePrecisionRecallCurve.exampleLabelConfidence = exampleLabelConfidence;
        AreaUnderAveragePrecisionRecallCurve.labels = new ArrayList<>(exampleLabelConfidence.columnKeySet());

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), settings.getNumProcessors()));
        CompletionService<Map<Float, List<Integer>>> cservice = new ExecutorCompletionService<>(executor);
        for (int i = 0; i < labels.size(); i++)
            cservice.submit(new Task(i));

        Map<Float, Map<String, List<Integer>>> thresholdLabelTpFpFn = new TreeMap<>();
        for (int i = 0; i < labels.size(); i++) {
            Map<Float, List<Integer>> thresholdTpFpFn = cservice.take().get();

            String label = labels.get(thresholdTpFpFn.get(-1f).get(0));
            thresholdTpFpFn.remove(-1f);

            for (float t : thresholdTpFpFn.keySet()) {
                List<Integer> tpFpFn = thresholdTpFpFn.get(t);

                Map<String, List<Integer>> labelTpFpFn = new HashMap<>();
                if (thresholdLabelTpFpFn.containsKey(t))
                    labelTpFpFn = thresholdLabelTpFpFn.get(t);

                labelTpFpFn.put(label, tpFpFn);
                thresholdLabelTpFpFn.put(t, labelTpFpFn);
            }
        }

        Map<Float, List<Float>> recallPrecisions = new TreeMap<>();
        for (int i = 0; i <= 100; i++) {
            float threshold = round((float)i / 100f, 2);
                        
            if (!thresholdLabelTpFpFn.containsKey(threshold))
                continue;
            
            int tTPSum = 0;
            int tFPSum = 0;
            int tFNSum = 0;
            
            for (String label : labels)
                if (theMostSpecificLabels.contains(label)) {
                    List<Integer> tpFpFn = new ArrayList<>();

                    if (!thresholdLabelTpFpFn.get(threshold).containsKey(label)) {
                        for (float tt : thresholdLabelTpFpFn.keySet())
                            if (tt > threshold && thresholdLabelTpFpFn.get(tt).containsKey(label)) {
                                tpFpFn = thresholdLabelTpFpFn.get(tt).get(label);
                                break;
                            }
                    } else
                        tpFpFn = thresholdLabelTpFpFn.get(threshold).get(label);

                    if (tpFpFn.size() == 3) {
                        tTPSum += tpFpFn.get(0);
                        tFPSum += tpFpFn.get(1);
                        tFNSum += tpFpFn.get(2);
                    }
                }
            
            float microAvgPrecision = 0f;
            if (tTPSum + tFPSum > 0)
                microAvgPrecision = round((float)tTPSum / (float)(tTPSum + tFPSum), 2);
            
            float microAvgRecall = 0f;
            if (tTPSum + tFNSum > 0)
                microAvgRecall = round((float)tTPSum / (float)(tTPSum + tFNSum), 2);
            
            List<Float> precisions = new ArrayList<>();
            if (recallPrecisions.containsKey(microAvgRecall))
                precisions = recallPrecisions.get(microAvgRecall);
            precisions.add(microAvgPrecision);
            recallPrecisions.put(microAvgRecall, precisions);
        }
        
        if (!recallPrecisions.containsKey(0f))
            recallPrecisions.put(0f, recallPrecisions.get(Collections.min(recallPrecisions.keySet())));

        double[] r = new double[recallPrecisions.size()];
        double[] p = new double[recallPrecisions.size()];
            
        int cnt = 0;
        for (float rr : recallPrecisions.keySet()) {
            r[cnt] = rr;
            p[cnt] = Collections.max(recallPrecisions.get(rr));
            cnt++;
        }
            
        UnivariateFunction interpolationFunction = new LinearInterpolator().interpolate(r, p);
            
        for (int i = 0; i <= 100; i++) {
            float rec = round((float)i / 100f, 2);
            if (!recallPrecisions.containsKey(rec)) {
                List<Float> precisions = new ArrayList<>();
                precisions.add((float)round(interpolationFunction.value(rec), 2));
                recallPrecisions.put(rec, precisions);
            }
        }

        auprc = round((float)new TrapezoidIntegrator().integrate(500000, interpolationFunction, 0.0, 1.0), 4);
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            bw.write("# Area under the average precision recall curve: " + auprc + "\n");
            bw.write("# Recall\tPrecision\n");
            for (float recall : recallPrecisions.keySet())
                bw.write(recall + "\t" + Collections.max(recallPrecisions.get(recall)) + "\n");
        }
        
        shutdownExecutor(executor);

        return auprc;
    }
    
    public static class Task implements Callable <Map<Float, List<Integer>>> {
        private final int labelPosition;
        
        public Task(int labelPosition) {
            this.labelPosition = labelPosition;
        }
        
        @Override
        public Map<Float, List<Integer>> call() {
            Map<Float, List<Integer>> thresholdTpFpFn = new TreeMap<>();
            Set<Float> thresholds = new HashSet<>();
            for (String example : exampleLabelConfidence.rowKeySet())
                thresholds.add(round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 2));

            for (float t : thresholds) {
                int TP = 0;
                int FP = 0;
                int FN = 0;

                for (String example : exampleLabelConfidence.rowKeySet()) {
                    float conf = round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 2);
                    if (conf >= t && label2examples.containsKey(labels.get(labelPosition)) && label2examples.get(labels.get(labelPosition)).contains(example))
                        TP++;
                    else if (conf >= t && (!label2examples.containsKey(labels.get(labelPosition)) || !label2examples.get(labels.get(labelPosition)).contains(example)))
                        FP++;
                    else if (conf < t && label2examples.containsKey(labels.get(labelPosition)) && label2examples.get(labels.get(labelPosition)).contains(example))
                        FN++;
                }

                List<Integer> tpFpFn = new ArrayList<>();
                tpFpFn.add(TP);
                tpFpFn.add(FP);
                tpFpFn.add(FN);
                thresholdTpFpFn.put(t, tpFpFn);
            }

            List<Integer> labelPos = new ArrayList<>();
            labelPos.add(labelPosition);
            thresholdTpFpFn.put(-1f, labelPos);

            return thresholdTpFpFn;
        }
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Tasks interrupted!");
        } finally {
            if (!executor.isTerminated())
                System.err.println("Canceled non-finished tasks!");
            
            executor.shutdownNow();
        }
    }
}

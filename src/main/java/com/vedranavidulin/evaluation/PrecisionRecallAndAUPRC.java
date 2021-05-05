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

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import java.io.File;
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
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import static org.apache.commons.math3.util.Precision.round;
import static com.vedranavidulin.data.DataReadWrite.writeTableEval;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.vedranavidulin.main.HierarchyDecompositionPipeline.settings;

/**
 *
 * @author Vedrana Vidulin
 */
public class PrecisionRecallAndAUPRC {
    private static Map<String, Set<String>> label2examples;
    private static Table<String, String, Float> exampleLabelConfidence;
    private static List<String> labels;
    
    public PrecisionRecallAndAUPRC() {}
    
    public Map<String, Float> run(Table<String, String, Float> exampleLabelConfidence, Map<String, Set<String>> label2examples, File outFolder) throws IOException, InterruptedException, ExecutionException {
        Map<String, Float> label2AUPRC = new HashMap<>();
        
        PrecisionRecallAndAUPRC.label2examples = label2examples;
        PrecisionRecallAndAUPRC.exampleLabelConfidence = exampleLabelConfidence;
        PrecisionRecallAndAUPRC.labels = new ArrayList<>(exampleLabelConfidence.columnKeySet());
        List<Float> recallValues = IntStream.rangeClosed(0, 1000).mapToObj(t -> (float)t / 1000f).collect(Collectors.toList());
        Table<Float, String, Float> recallLabelPrecision = ArrayTable.create(recallValues, PrecisionRecallAndAUPRC.exampleLabelConfidence.columnKeySet());

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), settings.getNumProcessors()));
        CompletionService<Map<Float, Float>> cservice = new ExecutorCompletionService<>(executor);
        for (int i = 0; i < labels.size(); i++)
            cservice.submit(new Task(i));

        for (int i = 0; i < PrecisionRecallAndAUPRC.exampleLabelConfidence.columnKeySet().size(); i++) {
            Map<Float, Float> recallPrecision = cservice.take().get();
            
            String label = labels.get(recallPrecision.get(-1f).intValue());
            recallPrecision.remove(-1f);
            
            double[] r = new double[recallPrecision.size()];
            double[] p = new double[recallPrecision.size()];
            
            int cnt = 0;
            for (float rr : recallPrecision.keySet()) {
                r[cnt] = rr;
                p[cnt] = recallPrecision.get(rr);
                cnt++;
            }
            
            UnivariateFunction interpolationFunction = new LinearInterpolator().interpolate(r, p);
            
            for (float rr : recallLabelPrecision.rowKeySet())
                if (recallPrecision.containsKey(rr))
                    recallLabelPrecision.put(rr, label, round(recallPrecision.get(rr), 3));
                else
                    recallLabelPrecision.put(rr, label, (float)round(interpolationFunction.value(rr), 3));
            
            label2AUPRC.put(label, (float)round(new TrapezoidIntegrator().integrate(500000, interpolationFunction, 0.0, 1.0), 4));
            
            System.out.println(label);
        }

        writeTableEval(recallLabelPrecision, "Recall/Precision for a label", new File(outFolder + "/Precision_recall.csv.gz"));
        
        shutdownExecutor(executor);
        
        return label2AUPRC;
    }
    

    public static class Task implements Callable <Map<Float, Float>> {
        private final int labelPosition;
        
        public Task(int labelPosition) {
            this.labelPosition = labelPosition;
        }
        
        @Override
        public Map<Float, Float> call() {
            Map<Float, Float> recallPrecision = new TreeMap<>();
            Map<Float, Set<Float>> recallPrecisionsSet = new TreeMap<>();
            
            Set<Float> thresholds = new HashSet<>();
            
            for (String example : exampleLabelConfidence.rowKeySet())
                thresholds.add(round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 3));
            
            if (thresholds.size() == 1 && thresholds.contains(0f)) {
                recallPrecision.put(0f, 0f);
                recallPrecision.put(1f, 0f);
                recallPrecision.put(-1f, (float)labelPosition);
                return recallPrecision;
            }
            
            thresholds.add(0f);
            thresholds.add(1f);
            
            for (float t : thresholds) {
                int TP = 0;
                int FP = 0;
                
                for (String example : exampleLabelConfidence.rowKeySet()) {
                    float conf = round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 3);
                    if (conf >= t && label2examples.get(labels.get(labelPosition)).contains(example))
                        TP++;
                    else if (conf >= t && !label2examples.get(labels.get(labelPosition)).contains(example))
                        FP++;
                }

                float precision = 0;
                if (TP + FP > 0)
                    precision = round((float)TP / (float)(TP + FP), 3);
                
                float recall = round((float)TP / (float)label2examples.get(labels.get(labelPosition)).size(), 3);
                
                Set<Float> precisionValues = new HashSet<>();
                if (recallPrecisionsSet.containsKey(recall))
                    precisionValues = recallPrecisionsSet.get(recall);
                precisionValues.add(precision);
                recallPrecisionsSet.put(recall, precisionValues);
            }
            
            for (float recall : recallPrecisionsSet.keySet())
                recallPrecision.put(recall, Collections.max(recallPrecisionsSet.get(recall)));
            
            if (!recallPrecision.containsKey(0f))
                recallPrecision.put(0f, recallPrecision.get(Collections.min(recallPrecision.keySet())));
            
            if (!recallPrecision.containsKey(1f))
                recallPrecision.put(1f, recallPrecision.get(Collections.max(recallPrecision.keySet())));
            
            recallPrecision.put(-1f, (float)labelPosition);
            
            return recallPrecision;
        }
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Tasks interrupted!");
        } finally {
            if (!executor.isTerminated())
                System.err.println("Canceled non-finished tasks!");
            
            executor.shutdownNow();
        }
    }
}

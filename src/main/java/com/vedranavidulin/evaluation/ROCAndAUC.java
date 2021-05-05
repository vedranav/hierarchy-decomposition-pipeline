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

import static com.vedranavidulin.main.Settings.errorMsg;
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
public class ROCAndAUC {
    private static Map<String, Set<String>> label2examples;
    private static Table<String, String, Float> exampleLabelConfidence;
    private static List<String> labels;
        
    public ROCAndAUC() {}
    
    public Map<String, Float> run(Table<String, String, Float> exampleLabelConfidence, Map<String, Set<String>> label2examples, File outFolder) throws IOException, InterruptedException, ExecutionException{
        if (!outFolder.exists() && !outFolder.mkdirs())
            errorMsg("Can't create folder " + outFolder.getAbsolutePath());
        
        Map<String, Float> label2AUC = new HashMap<>();
        
        ROCAndAUC.label2examples = label2examples;
        ROCAndAUC.exampleLabelConfidence = exampleLabelConfidence;
        ROCAndAUC.labels = new ArrayList<>(exampleLabelConfidence.columnKeySet());
        List<Float> fprVals = IntStream.rangeClosed(0, 1000).mapToObj(t -> (float)t / 1000f).collect(Collectors.toList());
        Table<Float, String, Float> fprLabelTpr = ArrayTable.create(fprVals, ROCAndAUC.exampleLabelConfidence.columnKeySet());
        
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), settings.getNumProcessors()));
        CompletionService<Map<Float, Float>> cservice = new ExecutorCompletionService<>(executor);
        for (int i = 0; i < labels.size(); i++)
            cservice.submit(new Task(i));
        
        for (int i = 0; i < ROCAndAUC.exampleLabelConfidence.columnKeySet().size(); i++) {
            Map<Float, Float> fprTpr = cservice.take().get();
            
            String label = labels.get(fprTpr.get(-1f).intValue());
            fprTpr.remove(-1f);
            
            double[] fpr = new double[fprTpr.size()];
            double[] tpr = new double[fprTpr.size()];
            
            int cnt = 0;
            for (float f : fprTpr.keySet()) {
                fpr[cnt] = f;
                tpr[cnt] = fprTpr.get(f);
                
                cnt++;
            }
            
            UnivariateFunction interpolationFunction = new LinearInterpolator().interpolate(fpr, tpr);
            
            for (float f : fprLabelTpr.rowKeySet())
                if (fprTpr.containsKey(f))
                    fprLabelTpr.put(f, label, round(fprTpr.get(f), 3));
                else
                    fprLabelTpr.put(f, label, (float)round(interpolationFunction.value(f), 3));
            
            
            label2AUC.put(label, (float)round(new TrapezoidIntegrator().integrate(500000, interpolationFunction, 0.0, 1.0), 4));

            System.out.println(label);
        }
        
        writeTableEval(fprLabelTpr, "False positive rate/True positive rate for a label", new File(outFolder + "/ROC.csv.gz"));
        
        shutdownExecutor(executor);
        
        return label2AUC;
    }
    
    public static class Task implements Callable <Map<Float, Float>> {
        private final int labelPosition;
        
        public Task(int labelPosition) {
            this.labelPosition = labelPosition;
        }
        
        @Override
        public Map<Float, Float> call() {
            Map<Float, Float> fprTpr = new TreeMap<>();
            Map<Float, Set<Float>> fprTprSet = new TreeMap<>();
            
            Set<Float> thresholds = new HashSet<>();
            
            for (String example : exampleLabelConfidence.rowKeySet())
                thresholds.add(round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 3));
            
            if (thresholds.size() == 1 && thresholds.contains(0f)) {
                fprTpr.put(0f, 0f);
                fprTpr.put(1f, 0f);
                fprTpr.put(-1f, (float)labelPosition);
                return fprTpr;
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

                float tpr = 0;
                if (label2examples.get(labels.get(labelPosition)).size() > 0)
                    tpr = round((float)TP / (float)label2examples.get(labels.get(labelPosition)).size(), 3);
                
                float fpr = 0;
                if (exampleLabelConfidence.rowKeySet().size() - label2examples.get(labels.get(labelPosition)).size() > 0)
                    fpr = round((float)FP / (float)(exampleLabelConfidence.rowKeySet().size() - label2examples.get(labels.get(labelPosition)).size()), 3);
                
                Set<Float> tprs = new HashSet<>();
                if (fprTprSet.containsKey(fpr))
                    tprs = fprTprSet.get(fpr);
                tprs.add(tpr);
                fprTprSet.put(fpr, tprs);
            }
            
            for (float fpr : fprTprSet.keySet())
                fprTpr.put(fpr, Collections.max(fprTprSet.get(fpr)));
            
            if (!fprTpr.containsKey(0f))
                fprTpr.put(0f, fprTpr.get(Collections.min(fprTpr.keySet())));
            
            if (!fprTpr.containsKey(1f))
                fprTpr.put(1f, fprTpr.get(Collections.max(fprTpr.keySet())));
            
            fprTpr.put(-1f, (float)labelPosition);
            
            return fprTpr;
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

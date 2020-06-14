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

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class ROCAndAUC {
    private static Map<String, Set<String>> label2examples;
    private static Table<String, String, Double> exampleLabelConfidence;
    private static List<String> labels;
        
    public ROCAndAUC() {}
    
    public Map<String, Double> run(Table<String, String, Double> exampleLabelConfidence, Map<String, Set<String>> label2examples, File outFolder, int maxNumProcessors) throws IOException, InterruptedException, ExecutionException{
        if (!outFolder.exists())
            outFolder.mkdirs();
        
        Map<String, Double> label2AUC = new HashMap<>();
        
        int numProcessors = Runtime.getRuntime().availableProcessors() > maxNumProcessors ? maxNumProcessors : Runtime.getRuntime().availableProcessors();
        
        ROCAndAUC.label2examples = label2examples;
        ROCAndAUC.exampleLabelConfidence = exampleLabelConfidence;
        ROCAndAUC.labels = new ArrayList<>(exampleLabelConfidence.columnKeySet());
        
        List<Double> fprVals = new ArrayList<>();
        for (double t = 0; t < 1; t += 0.001)
            fprVals.add(round(t, 3));
        fprVals.add(1.0);
        
        Table<Double, String, Double> fprLabelTpr = ArrayTable.create(fprVals, ROCAndAUC.exampleLabelConfidence.columnKeySet());
        
        
        ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
        CompletionService<Map<Double, Double>> cservice = new ExecutorCompletionService<>(executor);
        for (int i = 0; i < labels.size(); i++)
            cservice.submit(new ROCAndAUC.Task(i));
        
        for (int i = 0; i < ROCAndAUC.exampleLabelConfidence.columnKeySet().size(); i++) {
            Map<Double, Double> fprTpr = (Map<Double, Double>)cservice.take().get();
            
            String label = labels.get(fprTpr.get(-1.0).intValue());
            fprTpr.remove(-1.0);
            
            double[] fpr = new double[fprTpr.size()];
            double[] tpr = new double[fprTpr.size()];
            
            int cnt = 0;
            for (double f : fprTpr.keySet()) {
                fpr[cnt] = f;
                tpr[cnt] = fprTpr.get(f);
                
                cnt++;
            }
            
            UnivariateFunction interpolationFunction = new LinearInterpolator().interpolate(fpr, tpr);
            
            for (double f : fprLabelTpr.rowKeySet())
                if (fprTpr.containsKey(f))
                    fprLabelTpr.put(f, label, round(fprTpr.get(f), 3));
                else
                    fprLabelTpr.put(f, label, round(interpolationFunction.value(f), 3));
            
            
            label2AUC.put(label, round(new TrapezoidIntegrator().integrate(500000, interpolationFunction, 0.0, 1.0), 4));

            System.out.println(label);
        }
        
        writeTableEval(fprLabelTpr, "False positive rate/True positive rate for a label", new File(outFolder + "/ROC.csv.gz"));
        
        shutdownExecutor(executor);
        
        return label2AUC;
    }
    
    public class Task implements Callable <Map<Double, Double>> {
        private final int labelPosition;
        
        public Task(int labelPosition) {
            this.labelPosition = labelPosition;
        }
        
        @Override
        public Map<Double, Double> call() throws Exception {
            Map<Double, Double> fprTpr = new TreeMap<>();
            Map<Double, Set<Double>> fprTprSet = new TreeMap<>();
            
            Set<Double> thresholds = new HashSet<>();
            
            for (String example : exampleLabelConfidence.rowKeySet())
                thresholds.add(round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 3));
            
            if (thresholds.size() == 1 && thresholds.contains(0.0)) {
                fprTpr.put(0.0, 0.0);
                fprTpr.put(1.0, 0.0);
                fprTpr.put(-1.0, (double)labelPosition);
                return fprTpr;
            }
            
            thresholds.add(0.0);
            thresholds.add(1.0);
            
            for (double t : thresholds) {
                int TP = 0;
                int FP = 0;
                
                for (String example : exampleLabelConfidence.rowKeySet()) {
                    double conf = round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 3);
                    if (conf >= t && label2examples.get(labels.get(labelPosition)).contains(example))
                        TP++;
                    else if (conf >= t && !label2examples.get(labels.get(labelPosition)).contains(example))
                        FP++;
                }

                double tpr = 0;
                if (label2examples.get(labels.get(labelPosition)).size() > 0)
                    tpr = round((double)TP / (double)label2examples.get(labels.get(labelPosition)).size(), 3);
                
                double fpr = 0;
                if (exampleLabelConfidence.rowKeySet().size() - label2examples.get(labels.get(labelPosition)).size() > 0)
                    fpr = round((double)FP / (double)(exampleLabelConfidence.rowKeySet().size() - label2examples.get(labels.get(labelPosition)).size()), 3);
                
                Set<Double> tprs = new HashSet<>();
                if (fprTprSet.containsKey(fpr))
                    tprs = fprTprSet.get(fpr);
                tprs.add(tpr);
                fprTprSet.put(fpr, tprs);
            }
            
            for (double fpr : fprTprSet.keySet())
                fprTpr.put(fpr, Collections.max(fprTprSet.get(fpr)));
            
            if (!fprTpr.containsKey(0.0))
                fprTpr.put(0.0, fprTpr.get(Collections.min(fprTpr.keySet())));
            
            if (!fprTpr.containsKey(1.0))
                fprTpr.put(1.0, fprTpr.get(Collections.max(fprTpr.keySet())));
            
            fprTpr.put(-1.0, (double)labelPosition);
            
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

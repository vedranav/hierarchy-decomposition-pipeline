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
public class PrecisionRecallAndAUPRC {
    private static Map<String, Set<String>> label2examples;
    private static Table<String, String, Double> exampleLabelConfidence;
    private static List<String> labels;
    
    public PrecisionRecallAndAUPRC() {}
    
    public Map<String, Double> run(Table<String, String, Double> exampleLabelConfidence, Map<String, Set<String>> label2examples,
                                   int maxNumProcessors, File outFolder) throws IOException, InterruptedException, ExecutionException {
        if (!outFolder.exists())
            outFolder.mkdirs();
        
        Map<String, Double> label2AUPRC = new HashMap<>();
        
        int numProcessors = Runtime.getRuntime().availableProcessors() > maxNumProcessors ? maxNumProcessors : Runtime.getRuntime().availableProcessors();
        
        PrecisionRecallAndAUPRC.label2examples = label2examples;
        PrecisionRecallAndAUPRC.exampleLabelConfidence = exampleLabelConfidence;
        PrecisionRecallAndAUPRC.labels = new ArrayList<>(exampleLabelConfidence.columnKeySet());
        
        List<Double> recallVals = new ArrayList<>();
        for (double t = 0; t < 1; t += 0.001)
            recallVals.add(round(t, 3));
        recallVals.add(1.0);
        
        Table<Double, String, Double> recallLabelPrecision = ArrayTable.create(recallVals, PrecisionRecallAndAUPRC.exampleLabelConfidence.columnKeySet());
        
        ExecutorService executor = Executors.newFixedThreadPool(numProcessors);
        CompletionService<Map<Double, Double>> cservice = new ExecutorCompletionService<>(executor);
        for (int i = 0; i < labels.size(); i++)
            cservice.submit(new PrecisionRecallAndAUPRC.Task(i));
        
        for (int i = 0; i < PrecisionRecallAndAUPRC.exampleLabelConfidence.columnKeySet().size(); i++) {
            Map<Double, Double> recallPrecision = (Map<Double, Double>)cservice.take().get();
            
            String label = labels.get(recallPrecision.get(-1.0).intValue());
            recallPrecision.remove(-1.0);
            
            double[] r = new double[recallPrecision.size()];
            double[] p = new double[recallPrecision.size()];
            
            int cnt = 0;
            for (double rr : recallPrecision.keySet()) {
                r[cnt] = rr;
                p[cnt] = recallPrecision.get(rr);
                cnt++;
            }
            
            UnivariateFunction interpolationFunction = new LinearInterpolator().interpolate(r, p);
            
            for (double rr : recallLabelPrecision.rowKeySet())
                if (recallPrecision.containsKey(rr))
                    recallLabelPrecision.put(rr, label, round(recallPrecision.get(rr), 3));
                else
                    recallLabelPrecision.put(rr, label, round(interpolationFunction.value(rr), 3));
            
            label2AUPRC.put(label, round(new TrapezoidIntegrator().integrate(500000, interpolationFunction, 0.0, 1.0), 4));
            
            System.out.println(label);
        }
        
        writeTableEval(recallLabelPrecision, "Recall/Precision for a label", new File(outFolder + "/Precision_recall.csv.gz"));
        
        shutdownExecutor(executor);
        
        return label2AUPRC;
    }
    

    public class Task implements Callable <Map<Double, Double>> {
        private final int labelPosition;
        
        public Task(int labelPosition) {
            this.labelPosition = labelPosition;
        }
        
        @Override
        public Map<Double, Double> call() throws Exception {
            Map<Double, Double> recallPrecision = new TreeMap<>();
            Map<Double, Set<Double>> recallPrecisionsSet = new TreeMap<>();
            
            Set<Double> thresholds = new HashSet<>();
            
            for (String example : exampleLabelConfidence.rowKeySet())
                thresholds.add(round(exampleLabelConfidence.get(example, labels.get(labelPosition)), 3));
            
            if (thresholds.size() == 1 && thresholds.contains(0.0)) {
                recallPrecision.put(0.0, 0.0);
                recallPrecision.put(1.0, 0.0);
                recallPrecision.put(-1.0, (double)labelPosition);
                return recallPrecision;
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

                double precision = 0;
                if (TP + FP > 0)
                    precision = round((double)TP / (double)(TP + FP), 3);
                
                double recall = round((double)TP / (double)label2examples.get(labels.get(labelPosition)).size(), 3);
                
                Set<Double> precs = new HashSet<>();
                if (recallPrecisionsSet.containsKey(recall))
                    precs = recallPrecisionsSet.get(recall);
                precs.add(precision);
                recallPrecisionsSet.put(recall, precs);
            }
            
            for (double recall : recallPrecisionsSet.keySet())
                recallPrecision.put(recall, Collections.max(recallPrecisionsSet.get(recall)));
            
            if (!recallPrecision.containsKey(0.0))
                recallPrecision.put(0.0, recallPrecision.get(Collections.min(recallPrecision.keySet())));
            
            if (!recallPrecision.containsKey(1.0))
                recallPrecision.put(1.0, recallPrecision.get(Collections.max(recallPrecision.keySet())));
            
            recallPrecision.put(-1.0, (double)labelPosition);
            
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

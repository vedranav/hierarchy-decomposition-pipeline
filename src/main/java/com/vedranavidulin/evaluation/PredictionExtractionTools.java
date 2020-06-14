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
import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import com.vedranavidulin.data.Dataset;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import static org.apache.commons.math3.util.Precision.round;
import static com.vedranavidulin.data.DataReadWrite.writeTableConf;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class PredictionExtractionTools {
    private final Dataset baselineDataset;
    
    public PredictionExtractionTools(Dataset baselineDataset) {
        this.baselineDataset = baselineDataset;
    }
    
    public Table<String, String, Double> collectPredictions_Baseline_CompleteDecompositions(File[] inPredictionsArffFiles,
                                                                                            File outFileWithConfidences) throws IOException {
        Table<String, String, Double> exampleLabelConfidence = ArrayTable.create(baselineDataset.getLabelValues().keySet(),
                                                                                 baselineDataset.getLabels());
        
        for (File f : inPredictionsArffFiles) {
            List<String> labelsInArffFile = new ArrayList<>();
            List<Integer> labelPositions = new ArrayList<>();
        
            BufferedReader br = findReaderType(f);
            String line;
            int cntAtt = -1;
            while((line=br.readLine()) != null) {
                if (line.toUpperCase().startsWith("@ATTRIBUTE"))
                    cntAtt++;
                
                if (line.toUpperCase().startsWith("@ATTRIBUTE") && line.toUpperCase().endsWith("NUMERIC")) {
                    String label = line.substring(line.lastIndexOf("-p-") + 3, line.toUpperCase().lastIndexOf("NUMERIC")).trim(); //Baseline - DAG hierarchy
                    if (label.endsWith("-0"))
                        continue;
                    else if (label.endsWith("-1")) //Complete decompositions
                        label = label.substring(0, label.lastIndexOf("-"));
                    else if (label.contains("/")) //Baseline - Tree hierarchy
                        label = label.substring(label.lastIndexOf("/") + 1);
                    
                    labelsInArffFile.add(label);
                    labelPositions.add(cntAtt);
                } else if (!line.startsWith("@") && !line.isEmpty()) {
                    String[] parts = line.split(",");
                    for (int position : labelPositions)
                        exampleLabelConfidence.put(parts[0], labelsInArffFile.get(labelPositions.indexOf(position)),
                                                   round(Double.parseDouble(parts[position].trim()), 4));
                }
            }
        }
        
        for (String example : exampleLabelConfidence.rowKeySet())
            for (String label : exampleLabelConfidence.columnKeySet())
                if (exampleLabelConfidence.get(example, label) == null)
                    exampleLabelConfidence.put(example, label, 0.0);
        
        writeTableConf(exampleLabelConfidence, "Example ID/Confidence for a label", outFileWithConfidences);
        
        return exampleLabelConfidence;
    }
    
    public Table<String, String, Double> collectPredictions_PartialDecompositions(File[] inPredictionsArffFiles,
                                                                                  File outFileWithConfidences) throws IOException {
        Map<String, Map<String, Map<String, Double>>> example2child2parent2confidence = new TreeMap<>();
        
        for (File f : inPredictionsArffFiles) {
            String parentName = f.getName();
            if (parentName.startsWith("Child_")) {
                parentName = parentName.substring(parentName.indexOf("-parent_") + 8);
                parentName = parentName.substring(0, parentName.indexOf("-"));
            } else {
                parentName = parentName.substring(parentName.indexOf("_") + 1);
                parentName = parentName.substring(0, parentName.indexOf("-"));
            }
                        
            List<String> labelsInArffFile = new ArrayList<>();
            List<Integer> labelPositions = new ArrayList<>();
        
            BufferedReader br = findReaderType(f);
            String line;
            int cntAtt = -1;
            while((line=br.readLine()) != null) {
                if (line.toUpperCase().startsWith("@ATTRIBUTE"))
                    cntAtt++;
                
                if (line.toUpperCase().startsWith("@ATTRIBUTE") && line.toUpperCase().endsWith("NUMERIC")) {
                    String label = line.substring(line.lastIndexOf("-p-") + 3, line.toUpperCase().lastIndexOf("NUMERIC")).trim();
                    if (label.endsWith("-1")) {
                        String labelName = label.substring(0, label.lastIndexOf("-"));
                        if (labelName.endsWith("-1"))
                            labelName = labelName.substring(0, labelName.lastIndexOf("-"));
                        
                        if (labelName.contains("_"))
                            labelName = labelName.substring(labelName.indexOf("_") + 1);
                        
                        labelsInArffFile.add(labelName);
                        labelPositions.add(cntAtt);
                    }
                } else if (!line.startsWith("@") && !line.isEmpty()) {
                    String[] parts = line.split(",");
                    Map<String, Map<String, Double>> child2parent2confidence = new TreeMap<>();
                    if (example2child2parent2confidence.containsKey(parts[0]))
                        child2parent2confidence = example2child2parent2confidence.get(parts[0]);
                        
                    for (int position : labelPositions) {
                        double conf = round(Double.parseDouble(parts[position].trim()), 4);
                        if (conf > 0) {
                            String child = labelsInArffFile.get(labelPositions.indexOf(position));
                            
                            Map<String, Double> parent2confidence = new TreeMap<>();
                            if (child2parent2confidence.containsKey(child))
                                parent2confidence = child2parent2confidence.get(child);
                            
                            parent2confidence.put(parentName, conf);
                            
                            child2parent2confidence.put(child, parent2confidence);
                        }
                    }
                    example2child2parent2confidence.put(parts[0], child2parent2confidence);
                }
            }
        }
        
        Table<String, String, Double> exampleLabelConfidence = applyHierarchicalConstraint(example2child2parent2confidence,
                                                                                           outFileWithConfidences);
        
        return exampleLabelConfidence;
    }
    
    private Table<String, String, Double> applyHierarchicalConstraint(Map<String, Map<String, Map<String, Double>>> example2child2parent2confidence,
                                                                      File outFileWithConfidences) throws IOException {
        if (!outFileWithConfidences.getParentFile().exists())
            outFileWithConfidences.getParentFile().mkdirs();
        
        Table<String, String, Double> exampleLabelConfidence = ArrayTable.create(example2child2parent2confidence.keySet(),
                                                                                 collectLabelsFromMapWithConfidences(example2child2parent2confidence));
        
        Map<String, List<List<String>>> labelPaths = baselineDataset.getLabelPaths();
        
        for (String example : exampleLabelConfidence.rowKeySet()) {
            Map<String, Map<String, Double>> childParentConfidence = example2child2parent2confidence.get(example);
            
            for (String label : exampleLabelConfidence.columnKeySet()) {
                SummaryStatistics pathConfidences = new SummaryStatistics();
        
                for (List<String> path : labelPaths.get(label)) {
                    double pathConfidence = 1;
                    for (int i = 0; i < path.size() - 1; i++) { //ignore root
                        double edgeConf = 0;
                        
                        if (childParentConfidence.containsKey(path.get(i)) && childParentConfidence.get(path.get(i)).containsKey(path.get(i + 1)))
                            edgeConf = childParentConfidence.get(path.get(i)).get(path.get(i + 1));
                        
                        pathConfidence *= edgeConf;
                    }                    
                    pathConfidences.addValue(pathConfidence);
                }
                
                exampleLabelConfidence.put(example, label, round(pathConfidences.getMin(), 4));
            }
        }
        
        for (String example : exampleLabelConfidence.rowKeySet())
            for (String label : exampleLabelConfidence.columnKeySet())
                if (exampleLabelConfidence.get(example, label) == null)
                    exampleLabelConfidence.put(example, label, 0.0);
        
        writeTableConf(exampleLabelConfidence, "Example ID/Confidence for a label", outFileWithConfidences);
            
        return exampleLabelConfidence;
    }
    
    private Set<String> collectLabelsFromMapWithConfidences(Map<String, Map<String, Map<String, Double>>> example2child2parent2confidence) {
        Set<String> labels = new TreeSet<>();
        
        for (Map<String, Map<String, Double>> child2parent2confidence : example2child2parent2confidence.values())
            for (String child : child2parent2confidence.keySet()) {
                labels.add(child);
                labels.addAll(child2parent2confidence.get(child).keySet());
            }
        
        if (labels.contains("root"))
            labels.remove("root");
        
        return labels;
    }
}

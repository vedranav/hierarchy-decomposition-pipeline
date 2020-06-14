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
package com.vedranavidulin.decomposition;

import static com.vedranavidulin.data.DataReadWrite.zipFile;
import com.vedranavidulin.data.Dataset;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class Decomposition {
    private final Dataset baselineDataset;
    private Map<String, String> treeHierarchy;
    private final Map<String, Set<String>> exampleId2labels;
    private final Map<String, String> exampleId2attributeValues;
    
    public Decomposition(Dataset baselineDataset) {
        this.baselineDataset = baselineDataset;
        exampleId2labels = baselineDataset.getLabelValues();
        exampleId2attributeValues = baselineDataset.getAttributeValues();
    }
    
    private void createDataset(Set<String> keepExamples, Set<String> keepLabels, String classificationProblem, String outFilePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outFilePath)))) {
            bw.write(baselineDataset.getHeader());
            
            switch (classificationProblem) {
                case "DAG":
                    bw.write("@ATTRIBUTE CLASS HIERARCHICAL " + baselineDataset.getHierarchy() + "\n");
                    break;
                case "TREE":
                    treeHierarchy = baselineDataset.pairBasedHierarchyIntoTreePathHierarchy();
                    bw.write("@ATTRIBUTE CLASS HIERARCHICAL " + treeHierarchyHeader(treeHierarchy) + "\n");
                    break;
                case "COMPLETE":
                case "PARTIAL":
                    for (String label : keepLabels)
                        bw.write("@ATTRIBUTE " + label + " {0, 1}\n");
                    break;
            }
            
            bw.write("\n@DATA\n");
            
            for (String example : exampleId2labels.keySet())
                if (keepExamples.contains(example)) {
                    bw.write(exampleId2attributeValues.get(example));

                    Set<String> exampleLabels = exampleId2labels.get(example);
                    switch (classificationProblem) {
                        case "DAG":
                            bw.write("," + exampleLabels.toString().replace("[", "").replace("]", "").replace(", ", "@"));
                            break;
                        case "TREE":
                            Set<String> treeExampleLabels = new TreeSet<>();
                            for (String label : exampleLabels)
                                treeExampleLabels.add(treeHierarchy.get(label));
                            removeOverlapingPaths(treeExampleLabels);
                            bw.write("," + treeExampleLabels.toString().replace("[", "").replace("]", "").replace(", ", "@"));
                            break;
                        case "COMPLETE":
                            for (String label : keepLabels)
                                bw.write("," + (exampleLabels.contains(label) && baselineDataset.isTheMostSpecificLabel(label, exampleLabels) ? "1" : "0"));
                            break;
                        case "PARTIAL":
                            for (String label : keepLabels)
                                bw.write("," + (exampleLabels.contains(label) ? "1" : "0"));
                            break;
                    }
                    
                    bw.write("\n");
                }
        }
        
        zipFile(outFilePath, outFilePath + ".zip");
        new File(outFilePath).delete();
    }
    
    public void baseline(Set<String> keepExamples, String outFilePath) throws IOException {
        createDataset(keepExamples, null, baselineDataset.isTreeHierarchy() ? "TREE" : "DAG", outFilePath);
    }
    
    public void completeDecomposition(Set<String> keepExamples, String outFilePath) throws IOException {
       createDataset(keepExamples, baselineDataset.getTheMostSpecificLabels(), "COMPLETE", outFilePath);
    }
    
    public void partialDecomposition_ChildVsParentLabel(Set<String> keepExamples, boolean keepAllExamples, String outFilePathTemplate) throws IOException {
        Set<List<String>> parentChildLabelPairs = baselineDataset.getParentChildLabelPairs();
        for (List<String> parentChildLabelPair : parentChildLabelPairs) {
            String parent = parentChildLabelPair.get(0);
            String child = parentChildLabelPair.get(1);
                        
            Set<String> keepExamplesWithParentLabel = new HashSet<>();
            if (!keepAllExamples && !parent.equals("root"))
                for (String example : keepExamples)
                    if (exampleId2labels.get(example).contains(parent))
                        keepExamplesWithParentLabel.add(example);
            
            Set<String> keepLabels = new HashSet<>();
            keepLabels.add(child);
            
            String outFilePath = outFilePathTemplate.substring(0, outFilePathTemplate.lastIndexOf("/") + 1) + 
                                 "Child_" + child + "-parent_" + parent +
                                 outFilePathTemplate.substring(outFilePathTemplate.lastIndexOf("/") + 1);
            
            createDataset(keepAllExamples || parent.equals("root") ? keepExamples : keepExamplesWithParentLabel, keepLabels, "PARTIAL", outFilePath);
        }
    }
    
    public void partialDecomposition_LabelSpecialization(Set<String> keepExamples, boolean keepAllExamples, String outFilePathTemplate) throws IOException {
        for (String parent : baselineDataset.getParent2childrenLabels().keySet()) {
            Set<String> keepExamplesWithParentLabel = new HashSet<>();
            if (!keepAllExamples  && !parent.equals("root"))
                for (String example : keepExamples)
                    if (exampleId2labels.get(example).contains(parent))
                        keepExamplesWithParentLabel.add(example);
            
            String outFilePath = outFilePathTemplate.substring(0, outFilePathTemplate.lastIndexOf("/") + 1) + 
                                 "Parent_" + parent +
                                 outFilePathTemplate.substring(outFilePathTemplate.lastIndexOf("/") + 1);
            
            createDataset(keepAllExamples || parent.equals("root") ? keepExamples : keepExamplesWithParentLabel, baselineDataset.getParent2childrenLabels().get(parent), "PARTIAL", outFilePath);
        }
    }
    
    private void removeOverlapingPaths(Set<String> labels) {
        Set<String> removeLabels = new TreeSet<>();
        for (String label : labels)
            for (String labelForComparison : labels)
                if (labelForComparison.startsWith(label + "/"))
                    removeLabels.add(label);
        labels.removeAll(removeLabels);
    }
    
    private String treeHierarchyHeader(Map<String, String> label2path) {
        Set<String> header = new TreeSet<>();
        for (String path : label2path.values())
            header.add(path);
        
        return header.toString().replace("[", "").replace("]", "").replace(", ", ",");
    }
}

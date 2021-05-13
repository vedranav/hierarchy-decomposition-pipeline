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
package com.vedranavidulin.decomposition;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.baselineDataset;
import static com.vedranavidulin.data.DataReadWrite.zipFile;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.settings;

/**
 *
 * @author Vedrana Vidulin
 */
public class Decomposition {
    private Map<String, String> treeHierarchy;

    private void createDataset(Set<String> keepExamples, Set<String> keepLabels, String classificationProblem, String outFilePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFilePath))) {
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
            
            for (String example : baselineDataset.getLabelValues().keySet())
                if (keepExamples.contains(example)) {
                    bw.write(baselineDataset.getAttributeValues().get(example));

                    Set<String> exampleLabels = baselineDataset.getLabelValues().get(example);
                    switch (classificationProblem) {
                        case "DAG":
                            bw.write("," + exampleLabels.toString().replace("[", "").replace("]", "").replace(", ", "@"));
                            break;
                        case "TREE":
                            Set<String> treeExampleLabels = new TreeSet<>();
                            for (String label : exampleLabels)
                                treeExampleLabels.add(treeHierarchy.get(label));
                            removeOverlappingPaths(treeExampleLabels);
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
        if (!new File(outFilePath).delete())
            System.err.println("Can't remove " + outFilePath);
    }
    
    public void baseline(Set<String> keepExamples, String outFilePath) throws IOException {
        createDataset(keepExamples, null, baselineDataset.isTreeHierarchy() ? "TREE" : "DAG", outFilePath);
    }

    public void baselineAnnotate(String outTrainingFilePath, String outUnlabelledFilePath) throws IOException {
        createDataset(baselineDataset.getLabelValues().keySet(), null, baselineDataset.isTreeHierarchy() ? "TREE" : "DAG", outTrainingFilePath);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outUnlabelledFilePath))) {
            bw.write(baselineDataset.getHeader());
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL " + (baselineDataset.isTreeHierarchy() ?
                        treeHierarchyHeader(baselineDataset.pairBasedHierarchyIntoTreePathHierarchy()) : baselineDataset.getHierarchy()) + "\n");
            bw.write("\n@DATA\n");

            String dummyLabel = baselineDataset.isTreeHierarchy() ? baselineDataset.getLabels().iterator().next() : "root";
            BufferedReader br = findReaderType(settings.getUnlabelledSet());
            String line;
            while((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("@") && !line.isEmpty() && !line.startsWith("%")) {
                    if (line.contains("%"))
                        line = line.substring(0, line.indexOf("%")).trim();
                    bw.write(line.substring(0, line.lastIndexOf(",") + 1) + dummyLabel + "\n");
                }
            }
        }

        zipFile(outUnlabelledFilePath, outUnlabelledFilePath + ".zip");
        if (!new File(outUnlabelledFilePath).delete())
            System.err.println("Can't remove " + outUnlabelledFilePath);
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
                    if (baselineDataset.getLabelValues().get(example).contains(parent))
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
                    if (baselineDataset.getLabelValues().get(example).contains(parent))
                        keepExamplesWithParentLabel.add(example);
            
            String outFilePath = outFilePathTemplate.substring(0, outFilePathTemplate.lastIndexOf("/") + 1) + 
                                 "Parent_" + parent +
                                 outFilePathTemplate.substring(outFilePathTemplate.lastIndexOf("/") + 1);
            
            createDataset(keepAllExamples || parent.equals("root") ? keepExamples : keepExamplesWithParentLabel, baselineDataset.getParent2childrenLabels().get(parent), "PARTIAL", outFilePath);
        }
    }
    
    private void removeOverlappingPaths(Set<String> labels) {
        Set<String> removeLabels = new TreeSet<>();
        for (String label : labels)
            for (String labelForComparison : labels)
                if (labelForComparison.startsWith(label + "/"))
                    removeLabels.add(label);
        labels.removeAll(removeLabels);
    }
    
    private String treeHierarchyHeader(Map<String, String> label2path) {
        return new TreeSet<>(label2path.values()).toString().replace("[", "").replace("]", "").replace(", ", ",");
    }
}

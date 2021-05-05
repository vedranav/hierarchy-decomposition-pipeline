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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.*;

/**
 *
 * @author Vedrana Vidulin
 */
public class ClusSettingsFile {
    private final int hierarchicalClassLabelIndex = baselineDataset.getClassLabelIndex();

    private String sFileContent(String trainSetPath, String testSetPath, boolean isClassHierarchical, boolean isTreeHierarchy,
                                int indexOfTheLastLabel, int enableOnlyLabel) {
        StringBuilder sText = new StringBuilder();
        
        sText.append("[Data]\n");
        sText.append("File = ").append(trainSetPath.replace("//", "/")).append("\n");
        sText.append("TestSet = ").append(testSetPath.replace("//", "/")).append("\n");
        
        sText.append("\n[Attributes]\n");
        String target = enableOnlyLabel > 0 ? String.valueOf(enableOnlyLabel) :
                                              (hierarchicalClassLabelIndex + (indexOfTheLastLabel > 0 ? "-" + indexOfTheLastLabel : ""));
        sText.append("Target = ").append(target).append("\n");
        sText.append("Clustering = ").append(target).append("\n");
        if (enableOnlyLabel > 0 && indexOfTheLastLabel > 0)
            sText.append("Disable = ").append(rangeOfLabelsToDisable(enableOnlyLabel, hierarchicalClassLabelIndex, indexOfTheLastLabel)).append("\n");
        sText.append("Descriptive = 2-").append(hierarchicalClassLabelIndex - 1).append("\n");
        sText.append("Key = 1\n");
        
        sText.append("\n[Tree]\n");
        sText.append("PruningMethod = None\n");
        
        sText.append("\n[Ensemble]\n");
        sText.append("Iterations = ").append(settings.getNumTrees()).append("\n");
        sText.append("EnsembleMethod = RForest\n");
        sText.append("SelectRandomSubspaces = sqrt\n");
        sText.append("Optimize = Yes\n");
        sText.append("NumberOfThreads = ").append(settings.getNumProcessors()).append("\n");
        
        sText.append("\n[Output]\n");
        sText.append("AllFoldModels = No\n");
        sText.append("WritePredictions = Test\n");
        sText.append("GzipOutput = Yes\n");
        
        if (isClassHierarchical) {
            sText.append("\n[Hierarchical]\n");
            sText.append("Type = ").append(isTreeHierarchy ? "Tree" : "DAG").append("\n");
            sText.append("HSeparator = /\n");
            sText.append("WType = ExpAvgParentWeight\n");
        }
        
        return sText.toString();
    }
    
    public void baseline(String sFilePath, String trainSetPath, String testSetPath, boolean isTreeHierarchy) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(sFilePath))) {
            bw.write(sFileContent(trainSetPath, testSetPath, true, isTreeHierarchy, -1, -1));
        }
    }
    
    public void completeDecomposition_LabelsWithoutHierarchicalRelations(String sFilePath, String trainSetPath, String testSetPath, int numberOfLabels) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(sFilePath))) {
             bw.write(sFileContent(trainSetPath, testSetPath, false, false, hierarchicalClassLabelIndex + numberOfLabels - 1, -1));
        }
    }
    
    public void completeDecomposition_LabelVsTheRest(String sFilePath, String trainSetPath, String testSetPath, Set<String> leafLabels) throws IOException {
        for (int i = 0; i < leafLabels.size(); i++)
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(sFilePath.substring(0, sFilePath.lastIndexOf("/")) +
                                                                                "/Label_position_" + (hierarchicalClassLabelIndex + i) +
                                                                                sFilePath.substring(sFilePath.lastIndexOf("/") + 1)))) {
                bw.write(sFileContent(trainSetPath, testSetPath, false, false, hierarchicalClassLabelIndex + leafLabels.size() - 1,
                                      hierarchicalClassLabelIndex + i));
            }
    }
    
    public void partialDecomposition_ChildVsParentLabel(String sFilePath, String trainSetPath, String testSetPath) throws IOException {
        Set<List<String>> parentChildLabelPairs = baselineDataset.getParentChildLabelPairs();
        for (List<String> parentChildLabelPair : parentChildLabelPairs) {
            String parent = parentChildLabelPair.get(0);
            String child = parentChildLabelPair.get(1);
                   
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(sFilePath.substring(0, sFilePath.lastIndexOf("/")) +
                                                                                "/Child_" + child + "-parent_" + parent +
                                                                                sFilePath.substring(sFilePath.lastIndexOf("/") + 1)))) {
                bw.write(sFileContent(
                    trainSetPath.substring(0, trainSetPath.lastIndexOf("/")) + "/Child_" + child + "-parent_" + parent +
                        trainSetPath.substring(trainSetPath.lastIndexOf("/") + 1),
                    testSetPath.substring(0, testSetPath.lastIndexOf("/")) + "/Child_" + child + "-parent_" + parent +
                        testSetPath.substring(testSetPath.lastIndexOf("/") + 1),
                    false, false, -1, -1));
            }
        }
    }
    
    public void partialDecomposition_LabelSpecialization(String sFilePath, String trainSetPath, String testSetPath) throws IOException {
        Map<String, Set<String>> parent2ChildrenLabels = baselineDataset.getParent2childrenLabels();
        for (String parent : parent2ChildrenLabels.keySet()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(sFilePath.substring(0, sFilePath.lastIndexOf("/")) +
                                                                                "/Parent_" + parent +
                                                                                sFilePath.substring(sFilePath.lastIndexOf("/") + 1)))) {
                bw.write(sFileContent(
                    trainSetPath.substring(0, trainSetPath.lastIndexOf("/")) + "/Parent_" + parent +
                        trainSetPath.substring(trainSetPath.lastIndexOf("/") + 1),
                    testSetPath.substring(0, testSetPath.lastIndexOf("/")) + "/Parent_" + parent +
                        testSetPath.substring(testSetPath.lastIndexOf("/") + 1),
                    false, false, hierarchicalClassLabelIndex + parent2ChildrenLabels.get(parent).size() - 1, -1));
            }
        }
    }
    
    private String rangeOfLabelsToDisable(int enableLabel, int indexOfTheFirstLabel, int indexOfTheLastLabel) {
        if (enableLabel == indexOfTheFirstLabel)
            return (indexOfTheFirstLabel + 1) + "-" + indexOfTheLastLabel;
        else if (enableLabel == indexOfTheLastLabel)
            return indexOfTheFirstLabel + "-" + (indexOfTheLastLabel - 1);
        else if (enableLabel == indexOfTheFirstLabel + 1)
            return indexOfTheFirstLabel + "," + (enableLabel + 1) + "-" + indexOfTheLastLabel;
        else if (enableLabel == indexOfTheLastLabel - 1)
            return indexOfTheFirstLabel + "-" + (enableLabel-1) + "," + indexOfTheLastLabel;
        else
            return indexOfTheFirstLabel + "-" + (enableLabel-1) + "," + (enableLabel + 1) + "-" + indexOfTheLastLabel;
    }
}
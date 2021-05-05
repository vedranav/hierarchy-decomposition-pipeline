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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Random;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.*;
import static com.vedranavidulin.data.DataReadWrite.findReaderType;

/**
 *
 * @author Vedrana Vidulin
 */
public class CrossValidation {
    public Map<String, Integer> divideExamplesIntoFolds() throws IOException {
        Map<String, Integer> exampleId2fold = new HashMap<>();
        List<String> examplesShuffled = shuffleElementsInList(new ArrayList<>(baselineDataset.getLabelValues().keySet()));
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(settings.getExampleId2foldFile()));
        bw.write("#Example ID\tFold\n");
        int fold = 0;
        for (String exampleID : examplesShuffled) {
            fold++;
            if (fold > settings.getNumFolds())
                fold = 1;

            bw.write(exampleID + "\t" + fold + "\n");
            exampleId2fold.put(exampleID, fold);
        }        
        bw.close();
        
        return exampleId2fold;
    }
    
    public void generatePerFoldDatasets(String decompositionType, Map<String, Integer> exampleId2fold) throws IOException {
        Decomposition decomposition = new Decomposition();

        for (int i = 1; i <= settings.getNumFolds(); i++) {
            Set<String> trainingExamples = new HashSet<>();
            Set<String> testExamples = new HashSet<>();
            
            for (String example : exampleId2fold.keySet())
                if (exampleId2fold.get(example) == i)
                    testExamples.add(example);
                else
                    trainingExamples.add(example);

            if (decompositionType.equals(decompositions[0])) {
                decomposition.baseline(trainingExamples, settings.getCrossValidationPath() + decompositions[0] + "/Dataset/Train-fold_" + i + ".arff");
                decomposition.baseline(testExamples, settings.getCrossValidationPath() + decompositions[0] + "/Dataset/Test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[1])) {
                decomposition.completeDecomposition(trainingExamples, settings.getCrossValidationPath() + decompositions[1] + "/Dataset/Train-fold_" + i + ".arff");
                decomposition.completeDecomposition(testExamples, settings.getCrossValidationPath() + decompositions[1] + "/Dataset/Test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[2])) {
                decomposition.completeDecomposition(trainingExamples, settings.getCrossValidationPath() + decompositions[2] + "/Dataset/Train-fold_" + i + ".arff");
                decomposition.completeDecomposition(testExamples, settings.getCrossValidationPath() + decompositions[2] + "/Dataset/Test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[3])) {
                decomposition.partialDecomposition_ChildVsParentLabel(trainingExamples, false, settings.getCrossValidationPath() + decompositions[3] + "/Dataset/-train-fold_" + i + ".arff");
                decomposition.partialDecomposition_ChildVsParentLabel(testExamples, true, settings.getCrossValidationPath() + decompositions[3] + "/Dataset/-test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[4])) {
                decomposition.partialDecomposition_LabelSpecialization(trainingExamples, false, settings.getCrossValidationPath() + decompositions[4] + "/Dataset/-train-fold_" + i + ".arff");
                decomposition.partialDecomposition_LabelSpecialization(testExamples, true, settings.getCrossValidationPath() + decompositions[4] + "/Dataset/-test-fold_" + i + ".arff");
            }
        }
    }
    
    public void generatePerFoldClusSettingsFiles(String decompositionType) throws IOException {
        ClusSettingsFile sFile = new ClusSettingsFile();
        for (int i = 1; i <= settings.getNumFolds(); i++) {
            if (decompositionType.equals(decompositions[0])) {
                sFile.baseline(
                        settings.getCrossValidationPath() + decompositions[0] + "/Results/Fold_" + i + ".s",
                        settings.getCrossValidationPath() + decompositions[0] + "/Dataset/Train-fold_" + i + ".arff.zip",
                        settings.getCrossValidationPath() + decompositions[0] + "/Dataset/Test-fold_" + i + ".arff.zip",
                        baselineDataset.isTreeHierarchy());
            } else if (decompositionType.equals(decompositions[1])) {
                sFile.completeDecomposition_LabelsWithoutHierarchicalRelations(
                        settings.getCrossValidationPath() + decompositions[1] + "/Results/Fold_" + i + ".s",
                        settings.getCrossValidationPath() + decompositions[1] + "/Dataset/Train-fold_" + i + ".arff.zip",
                        settings.getCrossValidationPath() + decompositions[1] + "/Dataset/Test-fold_" + i + ".arff.zip",
                        baselineDataset.getTheMostSpecificLabels().size());
            } else if (decompositionType.equals(decompositions[2])) {
                sFile.completeDecomposition_LabelVsTheRest(
                        settings.getCrossValidationPath() + decompositions[2] + "/Results/-fold_" + i + ".s",
                        settings.getCrossValidationPath() + decompositions[2] + "/Dataset/Train-fold_" + i + ".arff.zip",
                        settings.getCrossValidationPath() + decompositions[2] + "/Dataset/Test-fold_" + i + ".arff.zip",
                        baselineDataset.getTheMostSpecificLabels());
            } else if (decompositionType.equals(decompositions[3])) {
                sFile.partialDecomposition_ChildVsParentLabel(
                        settings.getCrossValidationPath() + decompositions[3] + "/Results/-fold_" + i + ".s",
                        settings.getCrossValidationPath() + decompositions[3] + "/Dataset/-train-fold_" + i + ".arff.zip",
                        settings.getCrossValidationPath() + decompositions[3] + "/Dataset/-test-fold_" + i + ".arff.zip");
            } else if (decompositionType.equals(decompositions[4])) {
                sFile.partialDecomposition_LabelSpecialization(
                        settings.getCrossValidationPath() + decompositions[4] + "/Results/-fold_" + i + ".s",
                        settings.getCrossValidationPath() + decompositions[4] + "/Dataset/-train-fold_" + i + ".arff.zip",
                        settings.getCrossValidationPath() + decompositions[4] + "/Dataset/-test-fold_" + i + ".arff.zip");
            }
        }
    }
    
    public Map<String, Integer> loadExampleId2fold(File inExampleId2foldFile) throws IOException {
        Map<String, Integer> exampleId2fold = new HashMap<>();
        
        BufferedReader br = findReaderType(inExampleId2foldFile);
        String line;
        while((line=br.readLine()) != null)
            if (!line.startsWith("#"))
                exampleId2fold.put(line.substring(0, line.indexOf("\t")), Integer.parseInt(line.substring(line.indexOf("\t") + 1)));
        
        return exampleId2fold;
    }
    
    private List<String> shuffleElementsInList(List<String> inputList) {
        List<String> shuffledList = new ArrayList<>();
        
        int[] array = new int[inputList.size()];
        for (int i = 0; i < inputList.size(); i++)
            array[i] = i;

        Random r = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = r.nextInt(i);
            int tmp = array[index];
            array[index] = array[i];
            array[i] = tmp;
        }
        
        for (int index : array)
            shuffledList.add(inputList.get(index));
        
        return shuffledList;
    }
}
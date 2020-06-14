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

import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import com.vedranavidulin.data.Dataset;
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
import java.util.Properties;
import java.util.Random;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.decompositions;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class CrossValidation {
    private final int numFolds;
    private final Dataset baselineDataset;
    private final String outFolderPath;
    
    public CrossValidation(int numFolds, Dataset baselineDataset, String outFolderPath) {
        this.numFolds = numFolds;
        this.baselineDataset = baselineDataset;
        this.outFolderPath = outFolderPath;
    }
    
    public Map<String, Integer> divideExamplesIntoFolds() throws IOException {
        Map<String, Integer> exampleId2fold = new HashMap<>();
        List<String> examplesShuffled = shuffleElementsInList(new ArrayList(baselineDataset.getLabelValues().keySet()));
        
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outFolderPath + "/exampleId2fold.txt")));
        bw.write("#Example ID\tFold\n");
        int fold = 0;
        for (int i = 0; i < examplesShuffled.size(); i++) {
            fold++;
            if (fold > numFolds)
                fold = 1;
            
            bw.write(examplesShuffled.get(i) + "\t" + fold + "\n");
            exampleId2fold.put(examplesShuffled.get(i), fold);
        }        
        bw.close();
        
        return exampleId2fold;
    }
    
    public void generatePerFoldDatasets(String decompositionType, Map<String, Integer> exampleId2fold) throws IOException {
        Decomposition decomposition = new Decomposition(baselineDataset);
        for (int i = 1; i <= numFolds; i++) {
            Set<String> trainingExamples = new HashSet<>();
            Set<String> testExamples = new HashSet<>();
            
            for (String example : exampleId2fold.keySet())
                if (exampleId2fold.get(example) == i)
                    testExamples.add(example);
                else
                    trainingExamples.add(example);
            
            if (decompositionType.equals(decompositions[0])) {
                decomposition.baseline(trainingExamples, outFolderPath + "/" + decompositions[0] + "/Dataset/Train-fold_" + i + ".arff");
                decomposition.baseline(testExamples, outFolderPath + "/" + decompositions[0] + "/Dataset/Test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[1])) {
                decomposition.completeDecomposition(trainingExamples, outFolderPath + "/" + decompositions[1] + "/Dataset/Train-fold_" + i + ".arff");
                decomposition.completeDecomposition(testExamples, outFolderPath + "/" + decompositions[1] + "/Dataset/Test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[2])) {
                decomposition.completeDecomposition(trainingExamples, outFolderPath + "/" + decompositions[2] + "/Dataset/Train-fold_" + i + ".arff");
                decomposition.completeDecomposition(testExamples, outFolderPath + "/" + decompositions[2] + "/Dataset/Test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[3])) {
                decomposition.partialDecomposition_ChildVsParentLabel(trainingExamples, false, outFolderPath + "/" + decompositions[3] + "/Dataset/-train-fold_" + i + ".arff");
                decomposition.partialDecomposition_ChildVsParentLabel(testExamples, true, outFolderPath + "/" + decompositions[3] + "/Dataset/-test-fold_" + i + ".arff");
            } else if (decompositionType.equals(decompositions[4])) {
                decomposition.partialDecomposition_LabelSpecialization(trainingExamples, false, outFolderPath + "/" + decompositions[4] + "/Dataset/-train-fold_" + i + ".arff");
                decomposition.partialDecomposition_LabelSpecialization(testExamples, true, outFolderPath + "/" + decompositions[4] + "/Dataset/-test-fold_" + i + ".arff");
            }
        }
    }
    
    public void generatePerFoldClusSettingsFiles(String decompositionType, Properties prop) throws IOException {
        ClusSettingsFile sFile = new ClusSettingsFile(prop, baselineDataset.getClassLabelIndex(), baselineDataset);
        for (int i = 1; i <= numFolds; i++) {
            if (decompositionType.equals(decompositions[0])) {
                sFile.baseline(
                        prop.getProperty("outputFolder") + "/" + decompositions[0] + "/Results/Fold_" + i + ".s",
                        prop.getProperty("outputFolder") + "/" + decompositions[0] + "/Dataset/Train-fold_" + i + ".arff.zip",
                        prop.getProperty("outputFolder") + "/" + decompositions[0] + "/Dataset/Test-fold_" + i + ".arff.zip",
                        baselineDataset.isTreeHierarchy());
            } else if (decompositionType.equals(decompositions[1])) {
                sFile.completeDecomposition_LabelsWithoutHierarchicalRelations(
                        prop.getProperty("outputFolder") + "/" + decompositions[1] + "/Results/Fold_" + i + ".s",
                        prop.getProperty("outputFolder") + "/" + decompositions[1] + "/Dataset/Train-fold_" + i + ".arff.zip",
                        prop.getProperty("outputFolder") + "/" + decompositions[1] + "/Dataset/Test-fold_" + i + ".arff.zip",
                        baselineDataset.getTheMostSpecificLabels().size());
            } else if (decompositionType.equals(decompositions[2])) {
                sFile.completeDecomposition_LabelVsTheRest(
                        prop.getProperty("outputFolder") + "/" + decompositions[2] + "/Results/-fold_" + i + ".s",
                        prop.getProperty("outputFolder") + "/" + decompositions[2] + "/Dataset/Train-fold_" + i + ".arff.zip",
                        prop.getProperty("outputFolder") + "/" + decompositions[2] + "/Dataset/Test-fold_" + i + ".arff.zip",
                        baselineDataset.getTheMostSpecificLabels());
            } else if (decompositionType.equals(decompositions[3])) {
                sFile.partialDecomposition_ChildVsParentLabel(
                        prop.getProperty("outputFolder") + "/" + decompositions[3] + "/Results/-fold_" + i + ".s",
                        prop.getProperty("outputFolder") + "/" + decompositions[3] + "/Dataset/-train-fold_" + i + ".arff.zip",
                        prop.getProperty("outputFolder") + "/" + decompositions[3] + "/Dataset/-test-fold_" + i + ".arff.zip");
            } else if (decompositionType.equals(decompositions[4])) {
                sFile.partialDecomposition_LabelSpecialization(
                        prop.getProperty("outputFolder") + "/" + decompositions[4] + "/Results/-fold_" + i + ".s",
                        prop.getProperty("outputFolder") + "/" + decompositions[4] + "/Dataset/-train-fold_" + i + ".arff.zip",
                        prop.getProperty("outputFolder") + "/" + decompositions[4] + "/Dataset/-test-fold_" + i + ".arff.zip");
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

        //Fisher-Yates shuffle - http://en.algoritmy.net/article/43676/Fisher-Yates-shuffle
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
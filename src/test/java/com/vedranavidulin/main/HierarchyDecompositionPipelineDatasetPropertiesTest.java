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
package com.vedranavidulin.main;

import com.vedranavidulin.Utils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import static com.vedranavidulin.main.Settings.errorMsg;
import static org.apache.commons.math3.util.Precision.round;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vedrana Vidulin
 */
public class HierarchyDecompositionPipelineDatasetPropertiesTest {
    private static final File testDatasetPropertiesPipelineFolder = new File("test-project/dataset-properties-pipeline-test/");
    private static int maxDepth;
    private static boolean isTreeHierarchy;
    private static float minForwardBranchingFactor;
    private static float minBackwardBranchingFactor;
    private static float avgForwardBranchingFactor;
    private static float avgBackwardBranchingFactor;
    private static float maxForwardBranchingFactor;
    private static float maxBackwardBranchingFactor;
    private static int numExamplesInUnlabelledSet;

    @BeforeAll
    public static void datasetPropertiesPipelineTest() throws Exception {
        if (!testDatasetPropertiesPipelineFolder.exists() && !testDatasetPropertiesPipelineFolder.mkdirs())
            errorMsg("Can't create test folder " + testDatasetPropertiesPipelineFolder.getAbsolutePath());

        String phyleticProfilesPath = "src/test/resources/phyletic_profiles.harff.zip";
        String trainingSetPath = "src/test/resources/enron-training_set.harff.zip";
        String unlabelledSetPath = "src/test/resources/enron-unlabelled_set.harff.zip";

        HierarchyDecompositionPipeline.runPipeline(new Utils().getSettingsForDatasetPropertiesBaseline(phyleticProfilesPath, testDatasetPropertiesPipelineFolder.getAbsolutePath()));

        BufferedReader br = findReaderType(new File(testDatasetPropertiesPipelineFolder + "/Dataset_properties.txt"));
        String line;
        int branchingFactorType = 0;
        while((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("Forward branching factor"))
                branchingFactorType = 1;
            if (line.startsWith("Backward branching factor"))
                branchingFactorType = 2;

            if (!line.contains(":"))
                continue;

            String property = line.substring(0, line.lastIndexOf(":"));
            String value = line.substring(line.lastIndexOf(":") + 1).trim();
            switch (property) {
                case "Maximal depth":
                    maxDepth = Integer.parseInt(value);
                    break;
                case "Type of hierarchy":
                    isTreeHierarchy = value.equals("tree");
                    break;
                case "Minimal":
                    if (branchingFactorType == 1)
                        minForwardBranchingFactor = round(Float.parseFloat(value), 2);
                    else
                        minBackwardBranchingFactor = round(Float.parseFloat(value), 2);
                    break;
                case "Average":
                    if (branchingFactorType == 1)
                        avgForwardBranchingFactor = round(Float.parseFloat(value), 2);
                    else
                        avgBackwardBranchingFactor = round(Float.parseFloat(value), 2);
                    break;
                case "Maximal":
                    if (branchingFactorType == 1)
                        maxForwardBranchingFactor = round(Float.parseFloat(value), 2);
                    else
                        maxBackwardBranchingFactor = round(Float.parseFloat(value), 2);
                    break;
            }
        }

        HierarchyDecompositionPipeline.runPipeline(new Utils().getSettingsForDatasetPropertiesBaselineAndUnlabelled(trainingSetPath, unlabelledSetPath, testDatasetPropertiesPipelineFolder.getAbsolutePath()));
        br = findReaderType(new File(testDatasetPropertiesPipelineFolder + "/Dataset_properties.txt"));
        while((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("Examples in unlabelled set:"))
                numExamplesInUnlabelledSet = Integer.parseInt(line.substring(line.lastIndexOf(":") + 1).trim());
        }
    }

    @Test
    public void maxDepthIsCorrect() {
        assertEquals(14, maxDepth);
    }

    @Test
    public void isTreeHierarchyIsCorrect() { assertFalse(isTreeHierarchy); }

    @Test
    public void minForwardBranchingFactorIsCorrect() { assertEquals(1f, minForwardBranchingFactor); }

    @Test
    public void avgForwardBranchingFactorIsCorrect() { assertEquals(2.63f, avgForwardBranchingFactor); }

    @Test
    public void maxForwardBranchingFactorIsCorrect() { assertEquals(44f, maxForwardBranchingFactor); }

    @Test
    public void minBackwardBranchingFactorIsCorrect() { assertEquals(1f, minBackwardBranchingFactor); }

    @Test
    public void avgBackwardBranchingFactorIsCorrect() { assertEquals(1.85f, avgBackwardBranchingFactor); }

    @Test
    public void maxBackwardBranchingFactorIsCorrect() { assertEquals(8f, maxBackwardBranchingFactor); }

    @Test
    public void numExamplesInUnlabelledSetIsCorrect() { assertEquals(494, numExamplesInUnlabelledSet); }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testDatasetPropertiesPipelineFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testDatasetPropertiesPipelineFolder.getParentFile().getAbsolutePath()); }
    }
}

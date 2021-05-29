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
package com.vedranavidulin.data;

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
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Vedrana Vidulin
 */
public class DatasetPropertiesTest {
    private static final File testFolder = new File("test-project/datasetPropertiesTest/");
    private static final File datasetPropertiesFile = new File(testFolder + "/Enron-dataset_properties.txt");

    private static int numExamples = 0;
    private static int numNominalAttributes = 0;
    private static int numNumericAttributes = 0;
    private static int numLabels = 0;
    private static int numLeaves = 0;
    private static int maxDepth = 0;
    private static boolean isTreeHierarchy = false;
    private static float minForwardBranchingFactor = 0f;
    private static float avgForwardBranchingFactor = 0f;
    private static float maxForwardBranchingFactor = 0f;
    private static float minBackwardBranchingFactor = 0f;
    private static float avgBackwardBranchingFactor = 0f;
    private static float maxBackwardBranchingFactor = 0f;
    private static int numMostSpecificLabels = 0;
    private static float cardinalityCompleteDecompositions = 0f;
    private static float cardinalityBaselineAndPartialDecompositions = 0f;
    private static float cardinality = 0f;
    private static float cardinalityLeaves = 0f;
    private static int numNonLeafMostSpecificAnnotationPaths = 0;
    private static int numPaths = 0;

    @BeforeAll
    public static void datasetPropertiesTest() throws IOException {
        if (!testFolder.exists() && !testFolder.mkdirs())
            errorMsg("Can't create test folder " + testFolder.getAbsolutePath());

        new DatasetProperties().extractDatasetStatistics(new File("src/test/resources/enron.harff.zip"), null, datasetPropertiesFile);

        BufferedReader br = findReaderType(datasetPropertiesFile);
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
                case "Examples":
                    numExamples = Integer.parseInt(value);
                    break;
                case "Nominal":
                    numNominalAttributes = Integer.parseInt(value);
                    break;
                case "Numeric":
                    numNumericAttributes = Integer.parseInt(value);
                    break;
                case "Labels":
                    numLabels = Integer.parseInt(value);
                    break;
                case "Leaves":
                    numLeaves = Integer.parseInt(value);
                    break;
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
                case "Most specific labels":
                    numMostSpecificLabels = Integer.parseInt(value);
                    break;
                case "Complete hierarchy decomposition algorithms":
                    cardinalityCompleteDecompositions = round(Float.parseFloat(value), 2);
                    break;
                case "Hierarchical algorithms (baseline and partial hierarchy decomposition algorithms)":
                    cardinalityBaselineAndPartialDecompositions = round(Float.parseFloat(value), 2);
                    break;
                case "Data set":
                    cardinality = round(Float.parseFloat(value),2);
                    break;
                case "Leaf labels":
                    cardinalityLeaves = round(Float.parseFloat(value), 2);
                    break;
                case "Incomplete paths (the most specific annotation is not a leaf label)":
                    numNonLeafMostSpecificAnnotationPaths = Integer.parseInt(value);
                    break;
                case "Total number of paths":
                    numPaths = Integer.parseInt(value);
                    break;
            }
        }
    }

    @Test
    public void numExamplesIsCorrect() { assertEquals(1648, numExamples); }

    @Test
    public void numNominalAttributesIsCorrect() {
        assertEquals(1001, numNominalAttributes);
    }

    @Test
    public void numNumericAttributesIsCorrect() {
        assertEquals(0, numNumericAttributes);
    }

    @Test
    public void numLabelsIsCorrect() {
        assertEquals(56, numLabels);
    }

    @Test
    public void numLeavesIsCorrect() {
        assertEquals(52, numLeaves);
    }

    @Test
    public void maxDepthIsCorrect() {
        assertEquals(3, maxDepth);
    }

    @Test
    public void isTreeHierarchyIsCorrect() { assertTrue(isTreeHierarchy); }

    @Test
    public void minForwardBranchingFactorIsCorrect() { assertEquals(3f, minForwardBranchingFactor); }

    @Test
    public void avgForwardBranchingFactorIsCorrect() { assertEquals(11.2f, avgForwardBranchingFactor); }

    @Test
    public void maxForwardBranchingFactorIsCorrect() { assertEquals(19f, maxForwardBranchingFactor); }

    @Test
    public void minBackwardBranchingFactorIsCorrect() { assertEquals(1f, minBackwardBranchingFactor); }

    @Test
    public void avgBackwardBranchingFactorIsCorrect() { assertEquals(1f, avgBackwardBranchingFactor); }

    @Test
    public void maxBackwardBranchingFactorIsCorrect() { assertEquals(1f, maxBackwardBranchingFactor); }

    @Test
    public void numMostSpecificLabelsIsCorrect() {
        assertEquals(53, numMostSpecificLabels);
    }

    @Test
    public void cardinalityCompleteDecompositionsIsCorrect() { assertEquals(2.87f, cardinalityCompleteDecompositions); }

    @Test
    public void cardinalityBaselineAndPartialDecompositionsIsCorrect() { assertEquals(3.37f, cardinalityBaselineAndPartialDecompositions); }

    @Test
    public void cardinalityIsCorrect() { assertEquals(5.3f, cardinality); }

    @Test
    public void cardinalityLeavesIsCorrect() { assertEquals(2.85f, cardinalityLeaves); }

    @Test
    public void numNonLeafMostSpecificAnnotationPathsIsCorrect() { assertEquals(30, numNonLeafMostSpecificAnnotationPaths); }

    @Test
    public void numPathsIsCorrect() { assertEquals(4722, numPaths); }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testFolder.getParentFile().getAbsolutePath()); }
    }
}
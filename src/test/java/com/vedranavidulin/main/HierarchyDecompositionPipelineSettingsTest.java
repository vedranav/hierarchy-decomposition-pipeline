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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static com.vedranavidulin.main.HierarchyDecompositionPipeline.runPipeline;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.tools;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.settings;
import static com.vedranavidulin.main.Settings.errorMsg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vedrana Vidulin
 */
public class HierarchyDecompositionPipelineSettingsTest {
    private static final File testSettingsPipelineFolder = new File("test-project/settings-pipeline-test/");

    @BeforeAll
    public static void settingsTest() throws Exception {
        if (!testSettingsPipelineFolder.exists() && !testSettingsPipelineFolder.mkdirs())
            errorMsg("Can't create test folder " + testSettingsPipelineFolder.getAbsolutePath());

        String[] args = {"baselineDataset = src/test/resources/enron-training_set.harff.zip", "thresholds = 0.5",
                         "unlabelledSet = src/test/resources/enron-unlabelled_set.harff.zip",
                         "outputFolder=test-project/settings-pipeline-test/", "tools = 8, 1", "memory = 4g"};
        runPipeline(args);
    }

    @Test
    public void toolsAreCorrect() { assertEquals(new HashSet<>(Arrays.asList(1, 8)), tools); }

    @Test
    public void baselineDatasetIsCorrect() { assertTrue(settings.getBaselineDataset().getAbsolutePath().endsWith("src/test/resources/enron-training_set.harff.zip")); }

    @Test
    public void outputFolderIsCorrect() { assertTrue(settings.getOutputPath().endsWith("test-project/settings-pipeline-test/")); }

    @Test
    public void numTreesIsCorrect() { assertEquals(500, settings.getNumTrees()); }

    @Test
    public void memoryIsCorrect() { assertEquals("4g", settings.getMemory()); }

    @Test
    public void numProcessorsIsCorrect() { assertEquals(2, settings.getNumProcessors()); }

    @Test
    public void numFoldsIsCorrect() { assertEquals(10, settings.getNumFolds()); }

    @Test
    public void thresholdsIsCorrect() { assertEquals(new HashSet<>(Arrays.asList(0.5f)), settings.getThresholds()); }

    @Test
    public void labelSubsetIsCorrect() { assertEquals("mostSpecific", settings.getLabelSubset()); }

    @Test
    public void unlabelledSetIsCorrect() { assertTrue(settings.getUnlabelledSet().getAbsolutePath().endsWith("src/test/resources/enron-unlabelled_set.harff.zip")); }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testSettingsPipelineFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testSettingsPipelineFolder.getParentFile().getAbsolutePath()); }
    }
}

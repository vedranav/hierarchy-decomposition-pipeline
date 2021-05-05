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

import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import com.google.common.io.Files;
import com.vedranavidulin.Utils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static com.vedranavidulin.main.Settings.errorMsg;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vedrana Vidulin
 */
public class HierarchyDecompositionPipelineCrossValidationTest {
    private static final File testCrossValidationFolder = new File("test-project/cross-validation/");

    @BeforeAll
    public static void hierarchyDecompositionPipelineCrossValidation() throws Exception {
        if (!testCrossValidationFolder.exists() && !testCrossValidationFolder.mkdirs())
            errorMsg("Can't create test folder " + testCrossValidationFolder.getAbsolutePath());

        FileUtils.copyFile(new File("src/test/resources/exampleId2fold.txt"), new File(testCrossValidationFolder + "/exampleId2fold.txt"));

        HierarchyDecompositionPipeline.runPipeline(new Utils().getSettingsForCrossValidation("2"/*-6"*/, testCrossValidationFolder.getParentFile().getAbsolutePath()));
    }

    @Test
    public void baseline_classifier_has_expected_AUPRC() throws IOException {
        assertEquals(0.6463f, extractAUPRCFromEvaluationReport(new File(testCrossValidationFolder + "/Baseline/Evaluation_report.csv")));
    }

    /*@Test
    public void child_vs_parent_label_classifier_has_expected_AUPRC() throws IOException {
        assertEquals(0.6481f, extractAUPRCFromEvaluationReport(new File(testCrossValidationFolder + "/ChildVsParentLabel/Evaluation_report.csv")));
    }

    @Test
    public void label_specialization_classifier_has_expected_AUPRC() throws IOException {
        assertEquals(0.6573f, extractAUPRCFromEvaluationReport(new File(testCrossValidationFolder + "/LabelSpecialization/Evaluation_report.csv")));
    }

    @Test
    public void labels_without_hierarchical_relations_classifier_has_expected_AUPRC() throws IOException {
        assertEquals(0.5327f, extractAUPRCFromEvaluationReport(new File(testCrossValidationFolder + "/LabelsWithoutHierarchicalRelations/Evaluation_report.csv")));
    }

    @Test
    public void label_vs_the_rest_classifier_has_expected_AUPRC() throws IOException {
        assertEquals(0.532f, extractAUPRCFromEvaluationReport(new File(testCrossValidationFolder + "/LabelVsTheRest/Evaluation_report.csv")));
    }*/

    private float extractAUPRCFromEvaluationReport(File evaluationReportFile) throws IOException {
        for (String line : Files.readLines(evaluationReportFile, Charsets.UTF_8))
            if (line.startsWith("Area under average precision-recall curve"))
                return Float.parseFloat(line.substring(line.indexOf(",") + 1));
        return 0f;
    }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testCrossValidationFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testCrossValidationFolder.getParentFile().getAbsolutePath()); }
    }
}
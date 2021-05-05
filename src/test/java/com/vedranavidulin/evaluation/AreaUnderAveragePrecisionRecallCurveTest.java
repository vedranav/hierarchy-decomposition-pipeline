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
package com.vedranavidulin.evaluation;

import com.vedranavidulin.Utils;
import com.vedranavidulin.data.Dataset;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import static com.vedranavidulin.main.Settings.errorMsg;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Vedrana Vidulin
 */
class AreaUnderAveragePrecisionRecallCurveTest {
    private static final File testFolder = new File("test-project/areaUnderAveragePrecisionRecallCurveTest/");
    private static float microAvgAuprc;

    @BeforeAll
    public static void areaUnderAvgPRCurveTest() throws IOException, ExecutionException, InterruptedException {
        if (!testFolder.exists() && !testFolder.mkdirs())
            errorMsg("Can't create test folder " + testFolder.getAbsolutePath());

        new Utils().loadSettingsForCrossValidation("1", testFolder.getAbsolutePath());

        Dataset baselineDataset = new Dataset();
        microAvgAuprc = new AreaUnderAveragePrecisionRecallCurve().run(
                                    new Utils().loadExampleLabelConfidence(new File("src/test/resources/Confidences.csv.gz")),
                                    baselineDataset.getLabel2Examples(),
                                    baselineDataset.getTheMostSpecificLabels(),
                                    new File(testFolder.getAbsolutePath() + "/Area_under_average_precision_recall_curve.txt"));
    }

    @Test
    public void areaUnderAveragePrecisionRecallCurveIsCorrect() {
        assertEquals(0.6463f, microAvgAuprc);
    }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testFolder.getParentFile().getAbsolutePath()); }
    }
}
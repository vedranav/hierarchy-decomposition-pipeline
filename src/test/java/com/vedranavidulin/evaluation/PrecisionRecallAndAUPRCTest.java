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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.vedranavidulin.main.Settings.errorMsg;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vedrana Vidulin
 */
public class PrecisionRecallAndAUPRCTest {
    private static final File testFolder = new File("test-project/precisionRecallAndAUPRCTest/");
    private static Map<String, Float> label2AUPRC;

    @BeforeAll
    public static void precisionRecallAndAUPRCTest() throws IOException, ExecutionException, InterruptedException {
        if (!testFolder.exists() && !testFolder.mkdirs())
            errorMsg("Can't create test folder " + testFolder.getAbsolutePath());

        new Utils().loadSettingsForCrossValidation("1", testFolder.getAbsolutePath());

        label2AUPRC = new PrecisionRecallAndAUPRC().run(
                            new Utils().loadExampleLabelConfidence(new File("src/test/resources/Confidences.csv.gz")),
                            new Dataset().getLabel2Examples(),
                            testFolder);
    }

    @Test
    public void auprcsForLabelsAreCorrect() {
        Map<String, Float> expectedLabel2AUPRC = Stream.of(new String[][] {
                {"1", "1.0"},
                {"1.1", "0.8498"},
                {"1.1.1", "0.3904"},
                {"1.1.10", "0.242"},
                {"1.1.11", "0.0658"},
                {"1.1.12", "0.1295"},
                {"1.1.13", "0.3774"},
                {"1.1.2", "0.178"},
                {"1.1.3", "0.2156"},
                {"1.1.4", "0.0929"},
                {"1.1.5", "0.1453"},
                {"1.1.6", "0.6733"},
                {"1.1.7", "0.346"},
                {"1.1.8", "0.1522"},
                {"1.1.9", "0.1781"},
                {"1.2", "0.498"},
                {"1.3", "0.4468"},
                {"1.4", "0.7609"},
                {"1.5", "0.3122"},
                {"1.6", "0.3859"},
                {"1.7", "0.0484"},
                {"1.8", "0.0379"},
                {"2", "0.9508"},
                {"2.1", "0.6796"},
                {"2.10", "0.2392"},
                {"2.11", "0.3143"},
                {"2.12", "0.0122"},
                {"2.13", "0.5862"},
                {"2.2", "0.8049"},
                {"2.3", "0.0434"},
                {"2.4", "0.888"},
                {"2.5", "0.0154"},
                {"2.6", "0.1587"},
                {"2.7", "0.0793"},
                {"2.8", "0.1328"},
                {"2.9", "0.3753"},
                {"4", "0.3538"},
                {"4.1", "0.3737"},
                {"4.10", "0.3562"},
                {"4.11", "0.0309"},
                {"4.12", "0.1025"},
                {"4.13", "0.0031"},
                {"4.14", "0.0043"},
                {"4.15", "0.0103"},
                {"4.16", "0.0676"},
                {"4.17", "0.0005"},
                {"4.18", "0.0005"},
                {"4.19", "0.0727"},
                {"4.2", "0.0136"},
                {"4.3", "0.1828"},
                {"4.4", "0.0116"},
                {"4.5", "0.0105"},
                {"4.6", "0.0444"},
                {"4.7", "0.0197"},
                {"4.8", "0.3055"},
                {"4.9", "0.0664"}
        }).collect(Collectors.toMap(data -> data[0], data -> Float.parseFloat(data[1])));

        assertEquals(expectedLabel2AUPRC, label2AUPRC);
    }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testFolder.getParentFile().getAbsolutePath()); }
    }
}
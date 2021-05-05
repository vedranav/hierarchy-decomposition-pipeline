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
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Vedrana Vidulin
 */
public class ROCAndAUCTest {
    private static final File testFolder = new File("test-project/rocAndAUCTest/");
    private static Map<String, Float> label2AUC;

    @BeforeAll
    public static void rocAndAucTest() throws IOException, ExecutionException, InterruptedException {
        if (!testFolder.exists() && !testFolder.mkdirs())
            errorMsg("Can't create test folder " + testFolder.getAbsolutePath());

        new Utils().loadSettingsForCrossValidation("1", testFolder.getAbsolutePath());

        label2AUC = new ROCAndAUC().run(
                            new Utils().loadExampleLabelConfidence(new File("src/test/resources/Confidences.csv.gz")),
                            new Dataset().getLabel2Examples(),
                            testFolder);
    }

    @Test
    public void aucsForLabelsAreCorrect() {
        Map<String, Float> expectedLabel2AUC = Stream.of(new String[][] {
                {"1", "1.0"},
                {"1.1", "0.8437"},
                {"1.1.1", "0.8433"},
                {"1.1.10", "0.8431"},
                {"1.1.11", "0.7108"},
                {"1.1.12", "0.844"},
                {"1.1.13", "0.894"},
                {"1.1.2", "0.7099"},
                {"1.1.3", "0.8062"},
                {"1.1.4", "0.7281"},
                {"1.1.5", "0.7659"},
                {"1.1.6", "0.9215"},
                {"1.1.7", "0.7349"},
                {"1.1.8", "0.7131"},
                {"1.1.9", "0.79"},
                {"1.2", "0.8805"},
                {"1.3", "0.8146"},
                {"1.4", "0.8611"},
                {"1.5", "0.8479"},
                {"1.6", "0.8214"},
                {"1.7", "0.745"},
                {"1.8", "0.6689"},
                {"2", "0.8991"},
                {"2.1", "0.8044"},
                {"2.10", "0.9219"},
                {"2.11", "0.9292"},
                {"2.12", "0.7379"},
                {"2.13", "0.8712"},
                {"2.2", "0.8378"},
                {"2.3", "0.6746"},
                {"2.4", "0.9819"},
                {"2.5", "0.6631"},
                {"2.6", "0.8787"},
                {"2.7", "0.7971"},
                {"2.8", "0.805"},
                {"2.9", "0.8976"},
                {"4", "0.7299"},
                {"4.1", "0.7575"},
                {"4.10", "0.865"},
                {"4.11", "0.6595"},
                {"4.12", "0.6561"},
                {"4.13", "0.4185"},
                {"4.14", "0.6755"},
                {"4.15", "0.861"},
                {"4.16", "0.68"},
                {"4.17", "0.3145"},
                {"4.18", "0.4"},
                {"4.19", "0.6549"},
                {"4.2", "0.5606"},
                {"4.3", "0.6983"},
                {"4.4", "0.5084"},
                {"4.5", "0.6437"},
                {"4.6", "0.7327"},
                {"4.7", "0.5995"},
                {"4.8", "0.8717"},
                {"4.9", "0.5479"}
        }).collect(Collectors.toMap(data -> data[0], data -> Float.parseFloat(data[1])));

        assertEquals(expectedLabel2AUC, label2AUC);
    }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testFolder.getParentFile().getAbsolutePath()); }
    }
}
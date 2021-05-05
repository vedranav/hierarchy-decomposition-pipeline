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
package com.vedranavidulin;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.checkProperties;

/**
 * @author Vedrana Vidulin
 */
public class Utils {
    public String[] getSettingsForCrossValidation(String tasks, String outputPath) {
        return new String[]{"tasks = " + tasks,
                            "baselineDataset = " + new File("src/test/resources/enron.arff.zip").getAbsolutePath(),
                            "outputFolder = " + outputPath,
                            "numFolds = 10",
                            "numTrees = 500",
                            "Xmx = 8g",
                            "numProcessors = 4",
                            "labelSubset = mostSpecific",
                            "thresholds = 0.5,0.7,0.9"};
    }

    public void loadSettingsForCrossValidation(String tasks, String outputPath) {
        Properties prop = new Properties();
        Arrays.asList(getSettingsForCrossValidation(tasks, outputPath)).forEach(setting -> prop.setProperty(setting.substring(0, setting.indexOf("=")).trim(), setting.substring(setting.indexOf("=") + 1).trim()));
        checkProperties(prop);
    }

    public String[] getSettingsForAnnotation(String trainingSetPath, String unlabelledSetPath, String outputPath) {
        return new String[]{"tasks = 7",
                            "baselineDataset = " + new File(trainingSetPath).getAbsolutePath(),
                            "unlabelledSet = " + new File(unlabelledSetPath).getAbsolutePath(),
                            "outputFolder = " + outputPath,
                            "numTrees = 500",
                            "Xmx = 8g",
                            "numProcessors = 4"};
    }

    public Table<String, String, Float> loadExampleLabelConfidence(File tableFile) throws IOException {
        List<String> examples = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        BufferedReader br = findReaderType(tableFile);
        String line;
        while((line = br.readLine()) != null) {
            if (line.startsWith("Example ID")) {
                String[] parts = line.split(",");
                labels.addAll(Arrays.asList(parts).subList(1, parts.length));
            } else
                examples.add(line.substring(0, line.indexOf(",")));
        }

        Table<String, String, Float> exampleLabelConfidence = ArrayTable.create(examples, labels);

        br = findReaderType(tableFile);
        while((line = br.readLine()) != null)
            if (!line.startsWith("Example ID")) {
                String[] parts = line.split(",");
                for (int i = 1; i < parts.length; i++)
                    exampleLabelConfidence.put(parts[0], labels.get(i - 1), Float.parseFloat(parts[i]));
            }

        return exampleLabelConfidence;
    }
}

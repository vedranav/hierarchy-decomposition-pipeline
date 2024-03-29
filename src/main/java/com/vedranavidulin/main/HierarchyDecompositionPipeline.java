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

import java.io.*;
import java.util.*;
import com.vedranavidulin.data.Dataset;
import static com.vedranavidulin.data.DataReadWrite.*;
import static com.vedranavidulin.evaluation.Evaluation.evaluate;
import static com.vedranavidulin.main.Settings.errorMsg;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.vedranavidulin.data.DatasetFormat;
import com.vedranavidulin.data.DatasetProperties;
import com.vedranavidulin.data.InputFileFormatException;
import com.vedranavidulin.decomposition.Clus;
import com.vedranavidulin.decomposition.ClusSettingsFile;
import com.vedranavidulin.decomposition.CrossValidation;
import com.vedranavidulin.decomposition.Decomposition;
import com.vedranavidulin.evaluation.PredictionExtractionTools;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Vedrana Vidulin
 */
public class HierarchyDecompositionPipeline {
    public static final String[] decompositions = {"Baseline", "LabelsWithoutHierarchicalRelations", "LabelVsTheRest", "ChildVsParentLabel", "LabelSpecialization"};
    public static Settings settings = new Settings();
    public static Dataset baselineDataset;

    private static CrossValidation cv;
    private static Clus clus;
    
    private static Map<String, Integer> exampleId2fold;
    public static Set<Integer> tools;

    public static void main(String[] args) throws Exception {
        runPipeline(args);
    }
    
    public static void runPipeline(String[] args) throws Exception {
        Properties prop = new Properties();
        if (args.length == 1)
            if (new File(args[0]).exists())
                prop.load(new FileInputStream(args[0]));
            else
                errorMsg("Expected settings file, but \"" + args[0] + "\" doesn't exist");
        else if (args.length > 1)
            Arrays.asList(args).forEach(setting -> prop.setProperty(setting.substring(0, setting.indexOf("=")).trim(), setting.substring(setting.indexOf("=") + 1).trim()));
        else
            errorMsg("Please provide a settings file or pass settings as arguments");
        checkProperties(prop);

        checkDatasetFormat();

        baselineDataset = new Dataset();
        cv = new CrossValidation();
        clus = new Clus();

        exampleId2fold = new HashMap<>();

        File clusFile = new File(System.getProperty("user.dir") + "/clus.jar");
        if (tools.stream().anyMatch(tool -> tool > 1))
            try {
                FileUtils.copyInputStreamToFile(HierarchyDecompositionPipeline.class.getClassLoader().getResourceAsStream("libs/clus.jar"),
                                                new File(System.getProperty("user.dir") + "/clus.jar"));
            } catch (IOException e) { errorMsg("Can't extract clus library!"); }

        //(1)
        //PERFORM N-FOLD CROSS-VALIDATION
        if (tools.contains(1)) {
            System.out.println("Dividing examples into " + settings.getNumFolds() + " folds");
            exampleId2fold = cv.divideExamplesIntoFolds();
        } else if (tools.stream().anyMatch(tool -> tool < 7)) {
             if (!settings.getExampleId2foldFile().exists())
                errorMsg("Cross-validation can't be performed before dividing examples into folds. Add tool 1 to the list of tools.");
            exampleId2fold = cv.loadExampleId2fold(settings.getExampleId2foldFile());
        }
        
        //(2)
        //BASELINE
        if (tools.contains(2)) {
            System.out.println("\n#################################################################");
            System.out.println("Baseline data set");
            System.out.println("#################################################################");
            decompose(decompositions[0]);
        }
        
        //(3)
        //COMPLETE DECOMPOSITION: LABELS WITHOUT HIERARCHICAL RELATIONS
        if (tools.contains(3)) {
            System.out.println("\n#################################################################");
            System.out.println("Complete decomposition - Labels without hierarchical relations");
            System.out.println("#################################################################");
            decompose(decompositions[1]);
        }
        
        //(4)
        //COMPLETE DECOMPOSITION: LABEL VS. THE REST
        if (tools.contains(4)) {
            System.out.println("\n#################################################################");
            System.out.println("Complete decomposition - Label vs. the rest");
            System.out.println("#################################################################");
            decompose(decompositions[2]);
        }
        
        //(5)
        //PARTIAL DECOMPOSITION: CHILD VS. PARENT LABEL
        if (tools.contains(5)) {
            System.out.println("\n#################################################################");
            System.out.println("Partial decomposition - Child vs. parent label");
            System.out.println("#################################################################");
            decompose(decompositions[3]);
        }
        
        //(6)
        //PARTIAL DECOMPOSITION: LABEL SPECIALIZATION
        if (tools.contains(6)) {
            System.out.println("\n#################################################################");
            System.out.println("Partial decomposition - Label specialization");
            System.out.println("#################################################################");
            decompose(decompositions[4]);
        }

        //(7)
        if (tools.contains(7)) {
            System.out.println("\n#################################################################");
            System.out.println("Annotate unlabelled examples - Baseline");
            System.out.println("#################################################################");
            annotate(decompositions[0]);
        }

        //(8)
        if (tools.contains(8)) {
            System.out.println("\n#################################################################");
            System.out.println("Data set properties");
            System.out.println("#################################################################");
            System.out.println("Computing data set properties");
            new DatasetProperties().extractDatasetStatistics(settings.getBaselineDataset(), settings.getUnlabelledSet(), new File(settings.getOutputPath() + "Dataset_properties.txt"));
        }

        if (clusFile.exists()) clusFile.delete();
    }
    
    private static void decompose(String decomposition) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.nanoTime();
        createFolders(decomposition, true);

        System.out.println("Creating cross-validation folds");
        cv.generatePerFoldDatasets(decomposition, exampleId2fold);

        System.out.println("Creating CLUS settings files");
        cv.generatePerFoldClusSettingsFiles(decomposition);

        System.out.println("Constructing classification models");
        clus.constructModels(decomposition, true);

        System.out.println("Evaluating classification model");
        evaluate(decomposition);

        removeDatasets(decomposition, true);
        removeResults(decomposition, true);
        removeHierarchyFiles();

        settings.writeSettingsToFile(decomposition, true);
        writeTimeToStdoutAndFile(startTime, decomposition, true);
    }

    private static void annotate(String decomposition) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        createFolders(decomposition, false);

        System.out.println("Preparing data sets");
        String trainSetPath = settings.getAnnotationsPath() + decomposition + "/Dataset/TrainingSet.harff";
        String unlabelledSetPath = settings.getAnnotationsPath() + decomposition + "/Dataset/UnlabelledSet.harff";
        new Decomposition().baselineAnnotate(trainSetPath, unlabelledSetPath);

        System.out.println("Creating CLUS settings file");
        new ClusSettingsFile().baseline(settings.getAnnotationsPath() + decomposition + "/Results/Settings.s", trainSetPath, unlabelledSetPath,
                                        baselineDataset.isTreeHierarchy());

        System.out.println("Constructing classification model and annotating unlabelled examples");
        clus.constructModels(decomposition, false);

        new PredictionExtractionTools().collectPredictions_Baseline_CompleteDecompositions(
                listFilesWithEnding(settings.getAnnotationsPath() + decomposition + "/Results/", ".pred.arff.gz"),
                new File(settings.getAnnotationsPath() + decomposition + "/Confidences.csv.gz"), false);

        removeDatasets(decomposition, false);
        removeResults(decomposition, false);
        removeHierarchyFiles();

        settings.writeSettingsToFile(decomposition, false);
        writeTimeToStdoutAndFile(startTime, decomposition, false);
    }

    public static void checkProperties(Properties properties) {
        Set<String> propNames = properties.stringPropertyNames();
        if (!propNames.contains("tools")) errorMsg("Settings don't contain the required property \"tools\"");

        String[] toolArray = properties.getProperty("tools").split(",");
        tools = new HashSet<>();
        try {
            for (String tool : toolArray) {
                tool = tool.trim();
                if (tool.contains("-")) {
                    int lowerBound = Integer.parseInt(tool.substring(0, tool.indexOf("-")));
                    int upperBound = Integer.parseInt(tool.substring(tool.indexOf("-") + 1));
                    for (int i = lowerBound; i <= upperBound; i++)
                        tools.add(i);
                } else
                    tools.add(Integer.parseInt(tool));
            }

            if (tools.stream().anyMatch(tool -> tool >= 2 && tool <= 6) && !tools.contains(1) && !new File(properties.getProperty("outputFolder") + "/cross-validation/exampleId2fold.txt").exists())
                errorMsg("Cross-validation can't be performed before dividing examples into folds. Add tool 1 to the list of tools.");

            tools.forEach(tool -> {if (tool < 1 || tool > 8) errorMsg("Tools range from 1 to 8");});
        } catch (NumberFormatException nfe) { errorMsg("Tools should be correctly specified in settings"); }

        String[] requiredProperties = {"baselineDataset", "outputFolder"};
        String[] machineLearningProperties = {"numTrees", "memory", "numProcessors"};
        String[] crossValidationProperties = {"numFolds", "thresholds", "labelSubset"};
        String[] annotationProperties = {"unlabelledSet"};

        Arrays.asList(requiredProperties).forEach(rp -> {if (!propNames.contains(rp)) errorMsg("Settings don't contain the required property \"" + rp + "\"");});
        settings.setBaselineDataset(properties.getProperty(requiredProperties[0]));
        settings.setOutputFolder(properties.getProperty(requiredProperties[1]));

        settings.setNumTrees(propNames.contains(machineLearningProperties[0]) ? properties.getProperty(machineLearningProperties[0]) : "500");
        settings.setMemory(propNames.contains(machineLearningProperties[1]) ? properties.getProperty(machineLearningProperties[1]) : "2g");
        settings.setNumProcessors(propNames.contains(machineLearningProperties[2]) ? properties.getProperty(machineLearningProperties[2]) : "2");

        settings.setNumFolds(propNames.contains(crossValidationProperties[0]) ? properties.getProperty(crossValidationProperties[0]) : "10");
        settings.setThresholds(propNames.contains(crossValidationProperties[1]) ? properties.getProperty(crossValidationProperties[1]) : "0.5, 0.7, 0.9");
        settings.setLabelSubset(propNames.contains(crossValidationProperties[2]) ? properties.getProperty(crossValidationProperties[2]) : "mostSpecific");

        if (tools.stream().anyMatch(tool -> tool >= 1 && tool <= 6))
            settings.setCrossValidationFolder();

        if (tools.contains(7)) {
            Arrays.asList(annotationProperties).forEach(ap -> {if (!propNames.contains(ap)) errorMsg("Settings don't contain the required property \"" + ap + "\"");});
            settings.setAnnotationsFolder();
            settings.setUnlabelledSet(properties.getProperty(annotationProperties[0]));
        }

        if (tools.contains(8) && properties.getProperty(annotationProperties[0]) != null)
            settings.setUnlabelledSet(properties.getProperty(annotationProperties[0]));
    }

    private static void checkDatasetFormat() throws IOException {
        try {
            new DatasetFormat(settings.getBaselineDataset()).checkFormat();
        } catch (InputFileFormatException e) {
            System.err.println("Error in baseline data set!");
            errorMsg(e.getMessage());
        }

        if (settings.getUnlabelledSet() != null)
            try {
                new DatasetFormat(settings.getUnlabelledSet()).checkFormat();
            } catch (InputFileFormatException e) {
                System.err.println("Error in unlabelled set!");
                errorMsg(e.getMessage());
            }
    }

    private static void writeTimeToStdoutAndFile(long startTime, String decomposition, boolean crossValidation) throws IOException {
        String elapsedTime = formatTime(TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
        writeTime(decomposition, crossValidation, elapsedTime);
        System.out.println("Execution time: " + elapsedTime);
    }

    private static String formatTime(long secondsIn) {
        long days = TimeUnit.SECONDS.toDays(secondsIn);
        long hours = TimeUnit.SECONDS.toHours(secondsIn) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(secondsIn) - (TimeUnit.SECONDS.toHours(secondsIn) * 60);
        long secondsOut = TimeUnit.SECONDS.toSeconds(secondsIn) - (TimeUnit.SECONDS.toMinutes(secondsIn) * 60);
        return (days > 0 ? days + " day(s) " : "") + (hours > 0 ? hours + " hour(s) " : "") + (minutes > 0 ? minutes + " minute(s) " : "") + (secondsOut > 0 ? secondsOut + " second(s)" : "");
    }
}

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

package com.vedranavidulin.main;

import com.vedranavidulin.decomposition.Clus;
import com.vedranavidulin.decomposition.CrossValidation;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import com.vedranavidulin.data.Dataset;
import static com.vedranavidulin.data.DataReadWrite.*;
import static com.vedranavidulin.evaluation.Evaluation.evaluate;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 *
 * @author Vedrana Vidulin <vedrana.vidulin@gmail.com>
 */
public class HierarchyDecompositionPipeline {
    public static String[] decompositions = {"Baseline", "LabelsWithoutHierarchicalRelations", "LabelVsTheRest",
                                             "ChildVsParentLabel", "LabelSpecialization"};
    
    private static Dataset baselineDataset;
    private static CrossValidation cv;
    private static Clus clus;
    
    private static Map<String, Integer> exampleId2fold;
    private static Properties prop;
    private static Set<Double> thresholds;
    private static Set<Integer> tasks;
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Please provide a settings file");
            System.exit(-1);
        }
        
        prop = new Properties();
	    prop.load(new FileInputStream(args[0]));
        checkProperties();
        
        baselineDataset = new Dataset(prop);
        cv = new CrossValidation(Integer.parseInt(prop.getProperty("numFolds")), baselineDataset, prop.getProperty("outputFolder"));
        clus = new Clus(prop.getProperty("outputFolder"), prop.getProperty("Xmx"));
        
        exampleId2fold = new HashMap<>();
        
        //(1)
        //PERFORM N-FOLD CROSS-VALIDATION
        if (tasks.contains(1)) {
            System.out.println("Dividing examples into " + prop.getProperty("numFolds") + " folds");
            exampleId2fold = cv.divideExamplesIntoFolds();
        } else if (new File(prop.getProperty("outputFolder") + "/exampleId2fold.txt").exists())
            exampleId2fold = cv.loadExampleId2fold(new File(prop.getProperty("outputFolder") + "/exampleId2fold.txt"));
        
        //(2)
        //BASELINE
        if (tasks.contains(2)) {
            System.out.println("\n#################################################################");
            System.out.println("Baseline data set");
            System.out.println("#################################################################");
            decompose(decompositions[0]);
        }
        
        //(3)
        //COMPLETE DECOMPOSITION: LABELS WITHOUT HIERARCHICAL RELATIONS
        if (tasks.contains(3)) {
            System.out.println("\n#################################################################");
            System.out.println("Complete decomposition - Labels without hierarchical relations");
            System.out.println("#################################################################");
            decompose(decompositions[1]);
        }
        
        //(4)
        //COMPLETE DECOMPOSITION: LABEL VS. THE REST
        if (tasks.contains(4)) {
            System.out.println("\n#################################################################");
            System.out.println("Complete decomposition - Label vs. the rest");
            System.out.println("#################################################################");
            decompose(decompositions[2]);
        }
        
        //(5)
        //PARTIAL DECOMPOSITION: CHILD VS. PARENT LABEL
        if (tasks.contains(5)) {
            System.out.println("\n#################################################################");
            System.out.println("Partial decomposition - Child vs. parent label");
            System.out.println("#################################################################");
            decompose(decompositions[3]);
        }
        
        //(6)
        //PARTIAL DECOMPOSITION: LABEL SPECIALIZATION
        if (tasks.contains(6)) {
            System.out.println("\n#################################################################");
            System.out.println("Partial decomposition - Label specialization");
            System.out.println("#################################################################");
            decompose(decompositions[4]);
        }
    }
    
    private static void decompose(String decomposition) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.nanoTime();
        createFolders(prop.getProperty("outputFolder"), decomposition);

        System.out.println("Creating cross-validation folds");
        cv.generatePerFoldDatasets(decomposition, exampleId2fold);

        System.out.println("Creating CLUS settings files");
        cv.generatePerFoldClusSettingsFiles(decomposition, prop);

        System.out.println("Constructing classification models");
        clus.constructModels(decomposition);

        System.out.println("Evaluating classification model");
        evaluate(decomposition, prop.getProperty("outputFolder"), baselineDataset, thresholds, Integer.parseInt(prop.getProperty("numProcessors")));

        removeDatasets(prop.getProperty("outputFolder"), decomposition);
        removeResults(prop.getProperty("outputFolder"), decomposition);
        
        long elapsedTimeInSecond = TimeUnit.SECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        writeTime(prop.getProperty("outputFolder"), decomposition, elapsedTimeInSecond);
        System.out.println("Execution time: " + elapsedTimeInSecond + " sec");
    }
    
    private static void checkProperties() {
        Set<String> propNames = prop.stringPropertyNames();
        String[] requiredProperties = {"tasks", "baselineDataset", "outputFolder", "numFolds", "nTrees", "Xmx", "numProcessors", "labelSubset", "thresholds"};
        for (String rp : requiredProperties)
            if (!propNames.contains(rp)) {
                System.err.println("Settings file doesn't contain a required property " + rp);
                System.exit(-1);
            }
        
        String[] taskArray = prop.getProperty("tasks").split(",");
        tasks = new HashSet<>();
        try {
            for (String task : taskArray) {
                task = task.trim();
                if (task.contains("-")) {
                    int lowerBound = Integer.parseInt(task.substring(0, task.indexOf("-")));
                    int upperBound = Integer.parseInt(task.substring(task.indexOf("-") + 1));
                    
                    for (int i = lowerBound; i <= upperBound; i++)
                        tasks.add(i);
                } else
                    tasks.add(Integer.parseInt(task));
            }
            
            if (!tasks.contains(1) && !new File(prop.getProperty("outputFolder") + "/exampleId2fold.txt").exists()) {
                System.err.println("Decompositions can't be applied before dividing examples into n-folds for cross-validation. " + 
                                   "Add task 1 to the list of tasks.");
                System.exit(-1);
            }
            
            for (int task : tasks)
                if (task < 1 || task > 6) {
                    System.err.println("Tasks range from 1 to 6");
                    System.exit(-1);
                }
            
        } catch (NumberFormatException nfe) {
            System.err.println("Tasks should be correctly specified in a settings file");
            System.exit(-1);
        }
        
        if (!new File(prop.getProperty("baselineDataset")).exists()) {
            System.err.println("Can't find a baseline data set");
            System.exit(-1);
        }
        
        File outFolder = new File(prop.getProperty("outputFolder"));
        if (!outFolder.exists() && !outFolder.mkdirs()) {
            System.err.println("Can't create an output folder");
            System.exit(-1);
        } else if (!prop.getProperty("outputFolder").endsWith("/"))
            prop.setProperty("outputFolder", prop.getProperty("outputFolder") + "/");
        
        try {
            int numFolds = Integer.parseInt(prop.getProperty("numFolds"));
            if (numFolds < 2) {
                System.err.println("The minimal number of cross-validation folds is two");
                System.exit(-1);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("A number of folds for cross-validation should be correctly specified in a settings file");
            System.exit(-1);
        }
        
        try {
            int nTrees = Integer.parseInt(prop.getProperty("nTrees"));
            if (nTrees < 2) {
                System.err.println("The number of trees in a random forest should be at least two");
                System.exit(-1);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("A number of trees in a random forest should be correctly specified in a settings file");
            System.exit(-1);
        }
            
        boolean matches = Pattern.compile("((\\d)+[m|g|k|M|G|K]+)").matcher(prop.getProperty("Xmx")).matches();
        if (!matches) {
            System.err.println("Xmx should be defined as a number and a letter 'k' or 'K' when the number indicates kilobytes, " +
                               "'m' or 'M' when the number indicates megabytes, or 'g' or 'G' when the number indicates gigabytes.");
            System.exit(-1);
        }
        
        try {
            int numProcessors = Integer.parseInt(prop.getProperty("numProcessors"));
            if (numProcessors < 1) {
                System.err.println("The number of processors should be at least one");
                System.exit(-1);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("A number of processors should be correctly specified in a settings file");
            System.exit(-1);
        }

        String labelSubset = prop.getProperty("labelSubset").trim();
        if (!labelSubset.equals("mostSpecific") && !labelSubset.equals("hierarchyLeaves")) {
            System.err.println("Subset of labels available to the complete decomposition approaches and used for computing area under average " +
                               "precision-recall curve can take one of the following two values:\n" +
                               "1) mostSpecific = labels that appear at least once as the most specific label assigned to an instance;\n" +
                               "2) hierarchyLeaves = labels that are leaves of a class hierarchy.");
            System.exit(-1);
        }
        
        try {
            String[] thresholdsArray = prop.getProperty("thresholds").split(",");
            thresholds = new TreeSet<>();
            for (int i = 0 ; i < thresholdsArray.length; i++) {
                double t = Double.parseDouble(thresholdsArray[i].trim());
                if (t < 0 || t > 1) {
                    System.err.println("A value of threshold should be in [0, 1] interval");
                    System.exit(-1);
                } else
                    thresholds.add(t);
            }
        } catch (NumberFormatException nfe) {
            System.err.println("Thresholds for counting label-based predictions should be correctly specified in a settings file");
            System.exit(-1);
        }
    }
}

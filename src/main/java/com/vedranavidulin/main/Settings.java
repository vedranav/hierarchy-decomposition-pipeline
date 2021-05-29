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

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * @author Vedrana Vidulin
 */
public class Settings {
    private File baselineDataset;
    private File unlabelledSet;
    private String outputPath;
    private String crossValidationPath;
    private File exampleId2foldFile;
    private String annotationsPath;
    private int numFolds;
    private int numTrees;
    private String xmx;
    private int numProcessors;
    private String labelSubset;
    private Set<Float> thresholds;

    public void setBaselineDataset(String baselineDatasetPath) {
        baselineDataset = new File(baselineDatasetPath);
        if (!baselineDataset.exists())
            errorMsg("Can't find the baseline data set");
    }

    public File getBaselineDataset() { return baselineDataset; }

    public void setUnlabelledSet(String unlabelledSetPath) {
        unlabelledSet = new File(unlabelledSetPath);
        if (!unlabelledSet.exists())
            errorMsg("Can't find the unlabelled set");
    }

    public File getUnlabelledSet() { return unlabelledSet; }

    public void setOutputFolder(String outputFolderPath) {
        File outputFolder = new File(outputFolderPath);
        if (!outputFolder.exists() && !outputFolder.mkdirs())
            errorMsg("Can't create the output folder " + outputFolderPath);

        this.outputPath = outputFolderPath;
        if (!outputPath.endsWith("/"))
            outputPath += "/";
    }

    public String getOutputPath() { return outputPath; }

    public void setCrossValidationFolder() {
        crossValidationPath = outputPath + "cross-validation/";
        File crossValidationFolder = new File(crossValidationPath);
        if (!crossValidationFolder.exists() && !crossValidationFolder.mkdirs())
            errorMsg("Can't create the cross-validation folder " + crossValidationPath);
        exampleId2foldFile = new File(crossValidationPath + "exampleId2fold.txt");
    }

    public String getCrossValidationPath() { return crossValidationPath; }

    public File getExampleId2foldFile() { return exampleId2foldFile; }

    public void setAnnotationsFolder() {
        annotationsPath = outputPath + "annotations/";
        File annotationsFolder = new File(annotationsPath);
        if (!annotationsFolder.exists() && !annotationsFolder.mkdirs())
            errorMsg("Can't create the annotations folder " + annotationsPath);
    }

    public String getAnnotationsPath() { return  annotationsPath; }

    public void setNumFolds(String numFoldsStr) {
        try {
            numFolds = Integer.parseInt(numFoldsStr);
            if (Integer.parseInt(numFoldsStr) < 2)
                errorMsg("The minimal number of cross-validation folds is two");
        } catch (NumberFormatException nfe) { errorMsg("A number of folds for cross-validation should be correctly specified"); }
    }

    public int getNumFolds() { return numFolds; }

    public void setNumTrees(String numTreesStr) {
        try {
            numTrees = Integer.parseInt(numTreesStr);
            if (numTrees < 2)
                errorMsg("A number of trees in a random forest should be at least two");
        } catch (NumberFormatException nfe) { errorMsg("A number of trees in a random forest should be correctly specified"); }
    }

    public int getNumTrees() { return numTrees; }

    public void setXmx(String xmx) {
        this.xmx = xmx;
        if (!Pattern.compile("((\\d)+[mgkMGK]+)").matcher(xmx).matches())
            errorMsg("Xmx should be defined as a number and a letter 'k' or 'K' when the number indicates kilobytes, " +
                    "'m' or 'M' when the number indicates megabytes, or 'g' or 'G' when the number indicates gigabytes.");
    }

    public String getXmx() { return xmx; }

    public void setNumProcessors(String numProcessorsStr) {
        try {
            numProcessors = Integer.parseInt(numProcessorsStr);
            if (numProcessors < 1)
                errorMsg("A number of processors should be at least one");
        } catch (NumberFormatException nfe) { errorMsg("A number of processors should be correctly specified"); }
    }

    public int getNumProcessors() { return numProcessors; }

    public void setLabelSubset(String labelSubset) {
        this.labelSubset = labelSubset.trim();
        if (!this.labelSubset.equals("mostSpecific") && !this.labelSubset.equals("hierarchyLeaves"))
            errorMsg("Subset of labels used by the complete decomposition algorithms, and for computing area under average " +
                    "precision-recall curve can be defined as:\n" +
                    "1) mostSpecific = labels associated with at least one example as the most specific label in a path;\n" +
                    "2) hierarchyLeaves = labels that are class hierarchy leaves.");
    }

    public String getLabelSubset() { return labelSubset; }

    public void setThresholds(String thresholdsStr) {
        try {
            String[] thresholdsArray = thresholdsStr.split(",");
            thresholds = new TreeSet<>();
            Arrays.stream(thresholdsArray).mapToDouble(s -> Double.parseDouble(s.trim())).forEach(t -> {
                if (t < 0 || t > 1)
                    errorMsg("A value of threshold should be in the [0, 1] interval");
                else
                    thresholds.add((float)t);
            });
        } catch (NumberFormatException nfe) { errorMsg("Thresholds for counting label-based predictions should be correctly specified"); }
    }

    public Set<Float> getThresholds() { return thresholds; }

    public static void errorMsg(String msg) {
        System.err.println(msg);
        System.exit(-1);
    }
}

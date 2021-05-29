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

import java.io.*;
import java.util.*;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import static org.apache.commons.math3.util.Precision.round;
import static com.vedranavidulin.data.DataReadWrite.findReaderType;

/**
 * @author Vedrana Vidulin
 */
public class DatasetProperties {
    private String datasetName = "";
    private int numExamples = 0;
    private int numExamplesInUnlabelledSet = -1;
    private int numNominalAttributes = 0;
    private int numNumericAttributes = 0;
    private final Set<String> labels = new TreeSet<>();
    private Set<String> leafLabels = new TreeSet<>();
    private int maxDepth = 0;
    private boolean isTreeHierarchy = false;
    private final SummaryStatistics forwardBranchingFactors = new SummaryStatistics();
    private final SummaryStatistics backwardBranchingFactors = new SummaryStatistics();
    private final Set<String> mostSpecificLabels = new TreeSet<>();
    private final SummaryStatistics cardinalityForCompleteDecompositions = new SummaryStatistics();
    private final SummaryStatistics cardinalityForBaselineAndPartialDecompositions = new SummaryStatistics();
    private final SummaryStatistics cardinality = new SummaryStatistics();
    private final SummaryStatistics cardinalityLeaves = new SummaryStatistics();
    private int numPaths = 0;
    private int numNonLeafMostSpecificAnnotationPaths = 0;
    private Map<String, Set<String>> parent2childrenLabels = new HashMap<>();

    public void extractDatasetStatistics(File inBaselineDatasetFile, File inUnlabelledSet, File outPropertiesFile) throws IOException {
        Map<String, Set<String>> child2parentsLabels = new HashMap<>();
        Map<String, Set<String>> exampleId2annotations = new HashMap<>();

        BufferedReader br = findReaderType(inBaselineDatasetFile);
        String line;
        while((line = br.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("%"))
                continue;

            if (line.contains("%"))
                line = line.substring(0, line.indexOf("%")).trim();

            if (line.toUpperCase().startsWith("@RELATION")) {
                datasetName = line.substring(10).replace("\"", "").trim();
                if (datasetName.startsWith("'"))
                    datasetName = datasetName.substring(1).trim();
                if (datasetName.endsWith("'"))
                    datasetName = datasetName.substring(0, datasetName.length() - 1);
            } else if (line.toUpperCase().startsWith("@ATTRIBUTE CLASS")) {
                String hierarchy = line.substring(line.toUpperCase().lastIndexOf("HIERARCHICAL") + 12).trim();
                isTreeHierarchy = isTreeHierarchy(hierarchy);
                parent2childrenLabels = collectParent2childrenLabels(hierarchy);
                child2parentsLabels = collectChild2parentsLabels(hierarchy);
                for (String parent : parent2childrenLabels.keySet()) {
                    labels.add(parent);
                    labels.addAll(parent2childrenLabels.get(parent));
                }
                leafLabels = new TreeSet<>(labels);
                leafLabels.removeAll(parent2childrenLabels.keySet());

                List<String> labelsList = new ArrayList<>(labels);
                DirectedAcyclicGraph dag = new DirectedAcyclicGraph(labelsList.size());
                for (String parent : parent2childrenLabels.keySet())
                    parent2childrenLabels.get(parent).forEach(child -> dag.addEdge(labelsList.indexOf(child), labelsList.indexOf(parent)));

                for (int i = 0; i < labelsList.size(); i++) {
                    List<List<Integer>> allPathsIndices = dag.findAllPaths(i, labelsList.indexOf("root"));
                    for (List<Integer> path : allPathsIndices) {
                        int depth = path.size() - 1;
                        if (depth > maxDepth)
                            maxDepth = depth;
                    }
                }
            } else if (line.toUpperCase().startsWith("@ATTRIBUTE")) {
                if (line.toUpperCase().endsWith("NUMERIC"))
                    numNumericAttributes++;
                else if (line.contains("{") && line.endsWith("}"))
                    numNominalAttributes++;
            } else if (!line.startsWith("@") && !line.isEmpty()) {
                numExamples++;
                String[] annotations = line.substring(line.lastIndexOf(",") + 1).trim().split("@");
                cardinality.addValue(annotations.length);
                Set<String> leafLabelAnnotations = new HashSet<>();
                Collections.addAll(leafLabelAnnotations, annotations);
                leafLabelAnnotations.retainAll(leafLabels);
                cardinalityLeaves.addValue(leafLabelAnnotations.size());
                exampleId2annotations.put(line.substring(0, line.indexOf(",")).trim(), new HashSet<>(Arrays.asList(annotations)));
            }
        }

        for (Set<String> annotations : exampleId2annotations.values()) {
            Set<String> mostSpecificAnnotations = getMostSpecificAnnotations(annotations);

            numPaths += mostSpecificAnnotations.size();
            for (String mostSpecificAnnotation : mostSpecificAnnotations)
                if (!leafLabels.contains(mostSpecificAnnotation))
                    numNonLeafMostSpecificAnnotationPaths++;

            cardinalityForCompleteDecompositions.addValue(mostSpecificAnnotations.size());
            mostSpecificLabels.addAll(mostSpecificAnnotations);
        }

        for (Set<String> annotations : exampleId2annotations.values()) {
            Set<String> allMostSpecificAnnotations = new HashSet<>(annotations);
            allMostSpecificAnnotations.retainAll(mostSpecificLabels);
            cardinalityForBaselineAndPartialDecompositions.addValue(allMostSpecificAnnotations.size());
        }

        for (Set<String> childrenLabels : parent2childrenLabels.values())
            forwardBranchingFactors.addValue(childrenLabels.size());

        for (Set<String> parentsLabels : child2parentsLabels.values())
            backwardBranchingFactors.addValue(parentsLabels.size());

        if (inUnlabelledSet != null) {
            numExamplesInUnlabelledSet = 0;
            br = findReaderType(inUnlabelledSet);
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("@") && !line.isEmpty() && !line.startsWith("%"))
                    numExamplesInUnlabelledSet++;
            }
        }

        writeDatasetPropertiesToFile(outPropertiesFile);
    }

    private boolean isTreeHierarchy(String hierarchy) {
        Set<String> children = new HashSet<>();
        for (String pair : hierarchy.split(",")) {
            String child = pair.substring(pair.indexOf("/") + 1).trim();
            if (children.contains(child))
                return false;
            else
                children.add(child);
        }
        return true;
    }

    private Map<String, Set<String>> collectParent2childrenLabels(String hierarchy) {
        parent2childrenLabels = new HashMap<>();
        for (String pair : hierarchy.split(",")) {
            String parent = pair.substring(0, pair.indexOf("/")).trim();
            String child = pair.substring(pair.indexOf("/") + 1).trim();
            Set<String> children = new HashSet<>();
            if (parent2childrenLabels.containsKey(parent))
                children = parent2childrenLabels.get(parent);
            children.add(child);
            parent2childrenLabels.put(parent, children);
        }
        return parent2childrenLabels;
    }

    private Map<String, Set<String>> collectChild2parentsLabels(String hierarchy) {
        Map<String, Set<String>> child2parentsLabels = new HashMap<>();
        for (String pair : hierarchy.split(",")) {
            String parent = pair.substring(0, pair.indexOf("/")).trim();
            String child = pair.substring(pair.indexOf("/") + 1).trim();
            Set<String> parents = new HashSet<>();
            if (child2parentsLabels.containsKey(child))
                parents = child2parentsLabels.get(child);
            parents.add(parent);
            child2parentsLabels.put(child, parents);
        }
        return child2parentsLabels;
    }

    private Set<String> getMostSpecificAnnotations(Set<String> annotations) {
        Set<String> mostSpecificAnnotations = new TreeSet<>();
        for (String annotation : annotations)
            if (isMostSpecificAnnotation(annotation, annotations))
                mostSpecificAnnotations.add(annotation);

        return mostSpecificAnnotations;
    }

    private boolean isMostSpecificAnnotation(String annotation, Set<String> annotations) {
        if (parent2childrenLabels.containsKey(annotation))
            for (String child : parent2childrenLabels.get(annotation))
                if (annotations.contains(child))
                    return false;

        return true;
    }

    private void writeDatasetPropertiesToFile(File outPropertiesFile) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outPropertiesFile))) {
            bw.write("---------------------------------------------------------------------------\n");
            bw.write("Data set properties for " + datasetName + "\n");
            bw.write("---------------------------------------------------------------------------\n");
            bw.write("General info\n");
            bw.write("\tExamples: " + numExamples + "\n");
            if (numExamplesInUnlabelledSet != -1)
                bw.write("\tExamples in unlabelled set: " + numExamplesInUnlabelledSet + "\n");
            bw.write("\tAttributes\n");
            bw.write("\t\tNominal: " + numNominalAttributes + "\n");
            bw.write("\t\tNumeric: " + numNumericAttributes + "\n");

            bw.write("\nClass hierarchy\n");
            bw.write("\tLabels: " + (labels.size() - 1) + "\n");
            bw.write("\tLeaves: " + leafLabels.size() + "\n");
            bw.write("\tMaximal depth: " + maxDepth + "\n");
            bw.write("\tType of hierarchy: " + (isTreeHierarchy ? "tree" : "DAG") + "\n");
            bw.write("\tForward branching factor\n");
            bw.write("\t\tMinimal: " + round(forwardBranchingFactors.getMin(), 2) + "\n");
            bw.write("\t\tAverage: " + round(forwardBranchingFactors.getMean(), 2) + "\n");
            bw.write("\t\tMaximal: " + round(forwardBranchingFactors.getMax(), 2) + "\n");
            bw.write("\tBackward branching factor\n");
            bw.write("\t\tMinimal: " + round(backwardBranchingFactors.getMin(), 2) + "\n");
            bw.write("\t\tAverage: " + round(backwardBranchingFactors.getMean(), 2) + "\n");
            bw.write("\t\tMaximal: " + round(backwardBranchingFactors.getMax(), 2) + "\n");

            bw.write("\nAnnotations\n");
            bw.write("\tMost specific labels: " + mostSpecificLabels.size() + "\n");
            bw.write("\tCardinality\n");
            bw.write("\t\tComplete hierarchy decomposition algorithms: " + round(cardinalityForCompleteDecompositions.getMean(), 2) + "\n");
            bw.write("\t\tHierarchical algorithms (baseline and partial hierarchy decomposition algorithms): " + round(cardinalityForBaselineAndPartialDecompositions.getMean(), 2) + "\n");
            bw.write("\t\tData set: " + round(cardinality.getMean(), 2) + "\n");
            bw.write("\t\tLeaf labels: " + round(cardinalityLeaves.getMean(), 2) + "\n");
            bw.write("\tIncomplete paths\n");
            bw.write("\t\tIncomplete paths (the most specific annotation is not a leaf label): " + numNonLeafMostSpecificAnnotationPaths + "\n");
            bw.write("\t\tTotal number of paths: " + numPaths + "\n");
            bw.write("\t\tShare of incomplete paths: " + round((float) numNonLeafMostSpecificAnnotationPaths / (float) numPaths * 100f, 2) + "%\n");
        }
    }
}

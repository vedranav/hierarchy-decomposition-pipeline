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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import static com.vedranavidulin.main.HierarchyDecompositionPipeline.settings;

/**
 *
 * @author Vedrana Vidulin
 */
public class Dataset {
    private final String header;
    private String hierarchy;
    private final Map<String, String> exampleId2attributeValues;
    private final Map<String, Set<String>> exampleId2labels;
    private Map<String, Set<String>> parent2childrenLabels;
    private Set<List<String>> parentChildLabelPairs;
    private Set<String> labels;
    private Map<String, List<List<String>>> labelPaths;

    public Dataset() throws IOException {
        StringBuilder headerSb = new StringBuilder();
        exampleId2attributeValues = new HashMap<>();
        exampleId2labels = new TreeMap<>();

        BufferedReader br = findReaderType(settings.getBaselineDataset());
        String line;
        while((line=br.readLine()) != null)
            if (line.toUpperCase().startsWith("@ATTRIBUTE CLASS HIERARCHICAL"))
                hierarchy = line.substring(line.toUpperCase().indexOf("HIERARCHICAL") + 12).trim();
            else if (line.toUpperCase().startsWith("@RELATION"))
                headerSb.append(line).append("\n\n");
            else if (line.toUpperCase().startsWith("@ATTRIBUTE"))
                headerSb.append(line).append("\n");
            else if (!line.startsWith("@") && !line.isEmpty()) {
                String exampleId = line.substring(0, line.indexOf(","));
                exampleId2attributeValues.put(exampleId, line.substring(0, line.lastIndexOf(",")));
                exampleId2labels.put(exampleId, new TreeSet<>(Arrays.asList(line.substring(line.lastIndexOf(",") + 1).trim().split("@"))));
            }
        
        header = headerSb.toString();
        collectParent2childrenLabels();
        collectParentChildLabelPairs();
        collectLabels();
        propagateLabels();
    }
    
    public String getHeader() {
        return header;
    }
    
    public String getHierarchy() {
        return hierarchy;
    }
    
    public Map<String, String> getAttributeValues() {
        return exampleId2attributeValues;
    }
    
    public Map<String, Set<String>> getLabelValues() {
        return exampleId2labels;
    }
    
    public Map<String, Set<String>> getLabel2Examples() {
        Map<String, Set<String>> label2examples = new HashMap<>();
        for (String example : exampleId2labels.keySet())
            for (String label : exampleId2labels.get(example)) {
                Set<String> examples = new HashSet<>();
                if (label2examples.containsKey(label))
                    examples = label2examples.get(label);
                examples.add(example);
                label2examples.put(label, examples);
            }
        return label2examples;
    }
    
    public int getClassLabelIndex() {
        int numAttributes = 0;
        String[] lines = header.split("\n");
        for (String line : lines)
            if (line.toUpperCase().startsWith("@ATTRIBUTE"))
                numAttributes++;
        return numAttributes + 1;
    }
    
    public Set<String> getLabels() {
        return labels;
    }
        
    public boolean isTreeHierarchy() {
        Set<String> children = new HashSet<>();
        for (String pair : hierarchy.split(",")) {
            String child = pair.substring(pair.indexOf("/") + 1);
            if (children.contains(child))
                return false;
            else
                children.add(child);
        }            
        return true;
    }
    
    public Map<String, Set<String>> getParent2childrenLabels() {
        return parent2childrenLabels;
    }
    
    public Set<List<String>> getParentChildLabelPairs() {
        return parentChildLabelPairs;
    }
    
    public Map<String, List<List<String>>> getLabelPaths() {
        return labelPaths;
    }
    
    private void collectLabels() {
        labels = new TreeSet<>();
        for (String pair : hierarchy.split(","))
            labels.addAll(Arrays.asList(pair.split("/")));
        labels.remove("root");
    }    
    
    private void collectParent2childrenLabels() {
        parent2childrenLabels = new HashMap<>();
        for (String pair : hierarchy.split(",")) {
            String parent = pair.substring(0, pair.indexOf("/"));
            String child = pair.substring(pair.indexOf("/") + 1);
            Set<String> children = new HashSet<>();
            if (parent2childrenLabels.containsKey(parent))
                children = parent2childrenLabels.get(parent);
            children.add(child);
            parent2childrenLabels.put(parent, children);
        }
    }
    
    private void collectParentChildLabelPairs() {
        parentChildLabelPairs = new HashSet<>();
        for (String part : hierarchy.split(",")) {
            List<String> pair = new ArrayList<>();
            pair.add(part.substring(0, part.indexOf("/")).trim());
            pair.add(part.substring(part.indexOf("/") + 1).trim());
            parentChildLabelPairs.add(pair);
        }
    }
    
    private void propagateLabels() {
        labelPaths = new TreeMap<>();
        List<String> labelsList = new ArrayList<>(labels);
        labelsList.add("root");
        DirectedAcyclicGraph dag = new DirectedAcyclicGraph(labelsList.size());
        for (List<String> pair : parentChildLabelPairs)
            dag.addEdge(labelsList.indexOf(pair.get(1)), labelsList.indexOf(pair.get(0)));
        
        for (int i = 0; i < labelsList.size(); i++)
            if (!labelsList.get(i).equals("root")) {
                List<List<String>> allPaths = new ArrayList<>();
                List<List<Integer>> allPathsIndices = dag.findAllPaths(i, labelsList.indexOf("root"));
                for (List<Integer> pathIndices : allPathsIndices) {
                    List<String> path = new ArrayList<>();
                    for (Integer pathIndex : pathIndices) path.add(labelsList.get(pathIndex));
                    allPaths.add(path);
                }
                labelPaths.put(labelsList.get(i), allPaths);
            }
    }
    
    public Map<String, String> pairBasedHierarchyIntoTreePathHierarchy() {
        Map<String, String> label2path = new HashMap<>();
        for (String label : labelPaths.keySet()) {
            List<String> pathReverse = new ArrayList<>();
            for (int i = labelPaths.get(label).get(0).size() - 2; i >= 0; i--)
                pathReverse.add(labelPaths.get(label).get(0).get(i));
            label2path.put(label, pathReverse.toString().replace("[", "").replace("]", "").replace(", ", "/"));
        }
        return label2path;
    }
    
    public Set<String> getTheMostSpecificLabels() {
        if (settings.getLabelSubset().equals("hierarchyLeaves"))
            return getLeavesOfHierarchy();
        else {
            Set<String> theMostSpecificLabels = new TreeSet<>();
            for (Set<String> exampleLabels : exampleId2labels.values())
                theMostSpecificLabels.addAll(getTheMostSpecificLabelsForExample(exampleLabels));
            return theMostSpecificLabels;
        }
    }

    private Set<String> getTheMostSpecificLabelsForExample(Set<String> exampleLabels) {
        Set<String> theMostSpecificLabels = new TreeSet<>();
        for (String label : exampleLabels)
            if (isTheMostSpecificLabel(label, exampleLabels))
                theMostSpecificLabels.add(label);
        
        return theMostSpecificLabels;
    }
    
    public boolean isTheMostSpecificLabel(String label, Set<String> exampleLabels) {
        if (parent2childrenLabels.containsKey(label))
            for (String child : parent2childrenLabels.get(label))
                if (exampleLabels.contains(child))
                    return false;
        
        return true;
    }
    
    private Set<String> getLeavesOfHierarchy() {
        Set<String> leaves = new TreeSet<>();
        
        for (Set<String> children : parent2childrenLabels.values())
            for (String child : children)
                if (!parent2childrenLabels.containsKey(child))
                    leaves.add(child);
        
        return leaves;
    }
}

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

import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import static com.vedranavidulin.data.DataReadWrite.findReaderType;
import static com.vedranavidulin.data.Dataset.propagateLabels;

/**
 * @author Vedrana Vidulin
 */
public class DatasetFormat {
    private final File dataset;

    public DatasetFormat(File dataset) {
        this.dataset = dataset;
    }

    public void checkFormat() throws IOException, InputFileFormatException {
        if (!dataset.getName().toLowerCase().endsWith(".arff") && !dataset.getName().toLowerCase().endsWith(".arff.zip"))
            throw new InputFileFormatException("FILE-EXT", "Data set file extension must be “.arff” or “.arff.zip” in case that the file is compressed");

        Map<String, List<List<String>>> labelPaths = new HashMap<>();

        BufferedReader br = findReaderType(dataset);
        String line;
        int cntLine = 0;
        boolean firstNonCommentLine = true;
        int cntAttribute = 0;
        String lastAttribute = "";
        Set<String> attributeNamesSet = new HashSet<>();
        boolean isData = false;
        Set<String> exampleIDs = new HashSet<>();
        List<String> attributeNames = new ArrayList<>();
        List<List<String>> attributeTypes = new ArrayList<>();
        Set<String> labelsInHeader = new HashSet<>();
        Set<String> labelsInExamples = new HashSet<>();
        while((line = br.readLine()) != null) {
            cntLine++;
            line = line.trim();
            if (!line.startsWith("%")) {
                if (line.contains("%"))
                    line = line.substring(0, line.indexOf("%")).trim();

                if (line.contains("\t"))
                    throw new InputFileFormatException("TAB", "Elements in the data set must be separated with spaces and not tabs. Tab detected at line " + cntLine);

                boolean relationLine = line.toUpperCase().startsWith("@RELATION");
                if (firstNonCommentLine && !relationLine)
                    throw new InputFileFormatException("REL", "The first line of the data set must contain data set name, defined as @RELATION [data set name]");

                if (relationLine) {
                    String datasetName = line.substring(line.indexOf(" ")).trim();
                    if (datasetName.contains(" ") && !isTextQuoted(datasetName))
                        throw new InputFileFormatException("REL-QUOTE", "Quote data set name when containing spaces.");
                }

                if (line.toUpperCase().startsWith("@ATTRIBUTE")) {
                    cntAttribute++;

                    if (cntAttribute > 1 && !line.toUpperCase().startsWith("@ATTRIBUTE CLASS HIERARCHICAL")) {
                        String attTypeMsg = "Attributes that describe properties of examples must be one of the two types: numeric or nominal";
                        if (!line.toLowerCase().endsWith("numeric"))
                            if (!line.contains("{"))
                                throw new InputFileFormatException("ATT-TYPE", attTypeMsg);
                            else if (!line.endsWith("}"))
                                throw new InputFileFormatException("ATT-TYPE", attTypeMsg);

                        String attributeName = line.substring(10, line.toLowerCase().endsWith("numeric") ? line.toLowerCase().lastIndexOf("numeric") :
                                line.lastIndexOf("{")).trim();
                        attributeNamesSet.add(attributeName);
                        attributeNames.add(attributeName);

                        List<String> typeValues = new ArrayList<>();
                        if (line.endsWith("}"))
                            Arrays.asList(line.substring(line.lastIndexOf("{") + 1, line.lastIndexOf("}")).trim().split(",")).
                                    forEach(val -> typeValues.add(val.trim()));
                        else
                            typeValues.add("numeric");
                        attributeTypes.add(typeValues);
                    }

                    if (!line.toUpperCase().startsWith("@ATTRIBUTE CLASS HIERARCHICAL")) {
                        String attributeName = line.substring(10).trim();
                        if (attributeName.toLowerCase().contains("{"))
                            attributeName = attributeName.substring(0, attributeName.lastIndexOf("{")).trim();
                        else
                            attributeName = attributeName.substring(0, attributeName.lastIndexOf(" "));

                        if (attributeName.contains(" ") && !isTextQuoted(attributeName))
                            throw new InputFileFormatException("ATT-QUOTE", "Quote attribute name when containing spaces.");
                    }

                    lastAttribute = line;
                }

                if (cntAttribute == 1 && !line.toLowerCase().endsWith("string"))
                    throw new InputFileFormatException("EX-ID-FIRST", "The first attribute in the data set must contain Example ID attribute, defined as @ATTRIBUTE [name] string");

                if (line.toUpperCase().startsWith("@ATTRIBUTE CLASS HIERARCHICAL")) {
                    if (!line.substring(line.toUpperCase().indexOf("CLASS"), line.toUpperCase().indexOf("HIERARCHICAL") + 12).matches("[A-Z ]+"))
                        throw new InputFileFormatException("HIE-CAPS", "CLASS HIERARCHICAL keywords in class hierarchy definition must be written in capital letters");

                    String[] pairs = line.substring(line.indexOf("HIERARCHICAL") + 12).trim().split(",");

                    String hieFormatMsg = "Class hierarchy must be defined by enumerating all parent-child pairs in the hierarchy";
                    if (pairs.length < 3)
                        throw new InputFileFormatException("HIE-FORMAT", hieFormatMsg);

                    boolean hasRoot = false;
                    for (String pair : pairs)
                        if (pair.trim().startsWith("root/")) {
                            hasRoot = true;
                            break;
                        }

                    if (!hasRoot)
                        throw new InputFileFormatException("HIE-ROOT", "Root of class hierarchy must be defined in the list of parent-child pairs by using the keyword \"root\"");

                    Set<List<String>> parentChildLabelPairs = new HashSet<>();

                    labelsInHeader = new HashSet<>();
                    for (String pair : pairs) {
                        List<String> pairList = Arrays.asList(pair.split("/"));
                        if (pairList.size() != 2)
                            throw new InputFileFormatException("HIE-FORMAT", hieFormatMsg);

                        List<String> trimmedPair = new ArrayList<>();
                        for (String label : pairList) {
                            labelsInHeader.add(label.trim());
                            trimmedPair.add(label.trim());
                        }
                        parentChildLabelPairs.add(trimmedPair);
                    }

                    Set<String> incorrectLabelNames = new TreeSet<>();
                    for (String label : labelsInHeader)
                        if (!label.trim().matches("[a-zA-Z0-9.]+"))
                            incorrectLabelNames.add(label);

                    if (!incorrectLabelNames.isEmpty())
                        throw new InputFileFormatException("LABS-ALPHANUM-AND-DOT", "Labels names must contain only alphanumeric characters and dots. Please change names of the following labels: " + incorrectLabelNames.toString().replace("[", "").replace("]", ""));

                    labelPaths = propagateLabels(labelsInHeader, parentChildLabelPairs);
                    for (String label : labelPaths.keySet())
                        if (labelPaths.get(label).isEmpty())
                            throw new InputFileFormatException("HIE-PATH", "Error in line @ATTRIBUTE CLASS HIERARCHICAL: one or several paths don't begin in the root of a hierarchy");
                }

                if (line.toUpperCase().startsWith("@DATA")) {
                    if (!lastAttribute.toUpperCase().startsWith("@ATTRIBUTE CLASS HIERARCHICAL"))
                        throw new InputFileFormatException("HIE-LAST", "Class attribute must be the last attribute in a data set, defined as @ATTRIBUTE CLASS HIERARCHICAL [class hierarchy]");

                    if (cntAttribute < 3)
                        throw new InputFileFormatException("ATT-DEF", "Attributes must be defined in a header as @ATTRIBUTE [name] [type]");

                    if (cntAttribute - 2 != attributeNamesSet.size())
                        throw new InputFileFormatException("ATT-DUPLICATE", "There is a duplicate attribute name in data set");

                    isData = true;
                }

                if (isData && !line.toUpperCase().startsWith("@DATA")) {
                    String[] attVals = line.split(",");
                    if (attVals.length != cntAttribute)
                        throw new InputFileFormatException("EXP-NUM-VALS", "Number of attributes defined in header is " + cntAttribute + " while the number of attributes in line " + cntLine + " is " + attVals.length);

                    if (attVals[0].trim().equals("?"))
                        throw new InputFileFormatException("EXP-ID-MISSING", "Example ID attribute value can't be missing - at line " + cntLine);

                    if (attVals[attVals.length - 1].trim().equals("?"))
                        throw new InputFileFormatException("EXP-CLASS-VAL-MISSING", "Class attribute value can't be missing - at line " + cntLine);

                    if (exampleIDs.contains(attVals[0]))
                        throw new InputFileFormatException("EXP-DUPLICATE-ID", "Duplicate example ID at line " + cntLine);
                    exampleIDs.add(attVals[0]);

                    int numMissingValues = 0;
                    for (int i = 1; i < attVals.length - 1; i++) {
                        if (attVals[i].trim().equals("?")) {
                            numMissingValues++;
                            continue;
                        }

                        if (attributeTypes.get(i - 1).size() == 1 && attributeTypes.get(i - 1).get(0).equals("numeric")) {
                            if (!Pattern.compile("-?\\d+(\\.\\d+)?").matcher(attVals[i].trim()).matches())
                                throw new InputFileFormatException("EXP-NUM-VAL-ERR", "Value of the attribute " + attributeNames.get(i - 1) + " should be a number, but instead is " + attVals[i]);
                        } else if (!attributeTypes.get(i - 1).contains(attVals[i].trim()))
                            throw new InputFileFormatException("EXP-NOMINAL-VAL-ERR", "Value of the attribute " + attributeNames.get(i - 1) + " should have one of the values " +
                                    attributeTypes.get(i - 1).toString() + ", but instead is " + attVals[i]);
                    }

                    if (numMissingValues == attVals.length - 2)
                        throw new InputFileFormatException("EXP-ALL-VALS-MISSING", "All attribute values are missing in the example at line " + cntLine);

                    String labelsStr = line.substring(line.lastIndexOf(",") + 1).trim();
                    if (!labelsStr.matches("^[a-zA-Z0-9@.]*$"))
                        throw new InputFileFormatException("EXP-LABS-ALPHANUM-AND-DOT", "Label names must contain only alphanumeric characters and dots. " +
                                "When associated with examples labels must be separated by @ sign. " +
                                "Spaces around @ sign will result in error. The error is in line " + cntLine + " for labels " + labelsStr);

                    List<String> labels = Arrays.asList(labelsStr.split("@"));
                    labelsInExamples.addAll(labels);

                    if (labels.size() > 1 || (labels.size() == 1 && !labels.get(0).equals("root"))) {
                        Set<String> missingLabels = new HashSet<>();
                        for (String label : labels) {
                            if (!labelPaths.containsKey(label))
                                throw new InputFileFormatException("EXP-LAB-NOT-IN-HEADER", "Label " + label + " associated with example with ID " + attVals[0] + " in line " + cntLine + " is not defined in header");

                            for (List<String> path : labelPaths.get(label))
                                for (String pathElement : path)
                                    if (!pathElement.equals("root"))
                                        if (!labels.contains(pathElement))
                                            missingLabels.add(pathElement);
                        }
                        if (!missingLabels.isEmpty())
                            throw new InputFileFormatException("EXP-INVALID-PATH", "Example with ID " + attVals[0] + " in line " + cntLine + " is not labeled with valid paths. Following labels are missing " + missingLabels);
                    }
                }
                firstNonCommentLine = false;
            }
        }

        if (labelsInExamples.size() > 1 || (labelsInExamples.size() == 1 && !labelsInExamples.iterator().next().equals("root"))) {
            Set<String> labelsInHeaderThatAreNotRepresentedWithExamples = new TreeSet<>(Sets.difference(labelsInHeader, labelsInExamples));
            labelsInHeaderThatAreNotRepresentedWithExamples.remove("root");
            if (!labelsInHeaderThatAreNotRepresentedWithExamples.isEmpty())
                throw new InputFileFormatException("EXP-NOT-REPRESENTED-LABS", "Labels " + labelsInHeaderThatAreNotRepresentedWithExamples + " are defined in header, but are not associated with any of examples in the data set");
        }

        if (!isData)
            throw new InputFileFormatException("DATA-KEYWORD-MISSING", "@DATA keyword missing in the data set");
    }

    private boolean isTextQuoted(String text) {
        if (!text.startsWith("\"") && !text.startsWith("'"))
            return false;
        if (text.startsWith("\"") && !text.endsWith("\""))
            return false;
        if (text.startsWith("'") && !text.endsWith("'"))
            return false;
        return true;
    }
}

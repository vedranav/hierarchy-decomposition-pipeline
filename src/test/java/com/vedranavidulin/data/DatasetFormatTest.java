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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.vedranavidulin.main.Settings.errorMsg;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Vedrana Vidulin
 */
public class DatasetFormatTest {
    private static final File testFolder = new File("test-project/datasetFormatTest/");

    private String relation() {
        return "@RELATION \"product categorization\"\n\n";
    }

    private String exampleIDAttribute() {
        return "@ATTRIBUTE ID string\n";
    }

    private String attributesDescribingPropertiesOfExamples() {
        return "@ATTRIBUTE feet_related_product {yes, no}\n" +
               "@ATTRIBUTE most_represented_material {cotton, linen, elastane, polyester, leather, rubber, textile, plastic}\n" +
               "@ATTRIBUTE second_most_represented_material {cotton, linen, elastane, polyester, leather, rubber, textile, plastic, none}\n" +
               "@ATTRIBUTE length_cm numeric\n" +
               "@ATTRIBUTE has_sleeves {yes, no}\n";
    }

    private String classAttribute() {
        return "@ATTRIBUTE CLASS HIERARCHICAL root/apparel, apparel/shirts, apparel/trousers, trousers/long, trousers/short, root/footwear, footwear/sneakers, footwear/socks, footwear/sandals\n\n";
    }

    private String examples() {
        return "1, yes, rubber, plastic, 28, no, footwear@sandals\n" +
               "2, yes, leather, cotton, ?, no, footwear@sneakers@socks\n" +
               "3, no, cotton, elastane, 77, yes, apparel@shirts\n" +
               "4, no, polyester, elastane, 45, no, apparel@trousers@short\n" +
               "5, no, polyester, elastane, 85, no, apparel@trousers@long\n" +
               "6, no, linen, none, 74, yes, apparel@shirts\n";
    }

    private String checkFormat(File dataset) throws IOException {
        try {
            new DatasetFormat(dataset).checkFormat();
        } catch (InputFileFormatException e) {
            return e.getCode();
        }
        return "NO-ERRORS";
    }

    @BeforeAll
    public static void createTestFolder() {
        if (!testFolder.exists() && !testFolder.mkdirs())
            errorMsg("Can't create test folder " + testFolder.getAbsolutePath());
    }

    @Test
    public void dataset_file_extension_is_incorrect() throws IOException {
        File dataset = new File(testFolder + "/dataset_file_extension_is_incorrect.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("FILE-EXT", checkFormat(dataset));
    }

    @Test
    public void tab_used_as_a_separator() throws IOException {
        File dataset = new File(testFolder + "/tab_used_as_a_separator.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write("@RELATION\t\"product categorization\"\n\n");
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("TAB", checkFormat(dataset));
    }

    @Test
    public void relation_is_not_in_the_first_line() throws IOException {
        File dataset = new File(testFolder + "/relation_is_not_in_the_first_line.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("REL", checkFormat(dataset));
    }

    @Test
    public void dataset_name_containing_spaces_is_not_quoted() throws IOException {
        File dataset = new File(testFolder + "/dataset_name_containing_spaces_is_not_quoted.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write("@RELATION product categorization\n\n");
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("REL-QUOTE", checkFormat(dataset));
    }

    @Test
    public void attribute_type_is_neither_numeric_nor_nominal() throws IOException {
        File dataset = new File(testFolder + "/attribute_type_is_neither_numeric_nor_nominal.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            String[] attributes = attributesDescribingPropertiesOfExamples().split("\n");
            for (int i = 0; i < attributes.length; i++)
                if (!attributes[i].isEmpty())
                    if (i == 2)
                        bw.write("@ATTRIBUTE second_most_represented_material plastic, none}\n");
                    else
                        bw.write(attributes[i] + "\n");
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("ATT-TYPE", checkFormat(dataset));
    }

    @Test
    public void attribute_name_containing_spaces_is_not_quoted() throws IOException {
        File dataset = new File(testFolder + "/attribute_name_containing_spaces_is_not_quoted.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write("@ATTRIBUTE Example ID string\n");
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("ATT-QUOTE", checkFormat(dataset));
    }

    @Test
    public void example_ID_attribute_is_not_the_first_attribute() throws IOException {
        File dataset = new File(testFolder + "/example_ID_attribute_is_not_the_first_attribute.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(exampleIDAttribute());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("EX-ID-FIRST", checkFormat(dataset));
    }

    @Test
    public void hierarchy_format_less_than_three_elements() throws IOException {
        File dataset = new File(testFolder + "/hierarchy_format_less_than_three_elements.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL root/apparel, apparel/shirts\n\n");
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("HIE-FORMAT", checkFormat(dataset));
    }

    @Test
    public void class_hierarchical_in_capital_letters() throws IOException {
        File dataset = new File(testFolder + "/class_hierarchical_in_capital_letters.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@ATTRIBUTE class hierarchical root/apparel, apparel/shirts, apparel/trousers, trousers/long, trousers/short, root/footwear, footwear/sneakers, footwear/socks, footwear/sandals\n\n");
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("HIE-CAPS", checkFormat(dataset));
    }

    @Test
    public void incorrectly_defined_parent_child_pairs() throws IOException {
        File dataset = new File(testFolder + "/incorrectly_defined_parent_child_pairs.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL root/apparel, apparel/shirts, apparel/trousers/long, trousers/long, trousers/short, root/footwear, footwear/sneakers, footwear/socks, footwear/sandals\n\n");
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("HIE-FORMAT", checkFormat(dataset));
    }

    @Test
    public void root_is_not_in_the_list_of_parent_child_pairs() throws IOException {
        File dataset = new File(testFolder + "/root_is_not_in_the_list_of_parent_child_pairs.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL apparel/shirts, apparel/trousers, trousers/long, trousers/short, footwear/sneakers, footwear/socks, footwear/sandals\n\n");
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("HIE-ROOT", checkFormat(dataset));
    }

    @Test
    public void labels_contain_characters_other_than_alphanumeric() throws IOException {
        File dataset = new File(testFolder + "/labels_contain_characters_other_than_alphanumeric.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL root/apparel, apparel/shirts, apparel/trousers, trousers/long, trousers/short, root/footwear, footwear/sneakers, footwear/short-socks, footwear/sandals\n\n");
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("LABS-ALPHANUM-AND-DOT", checkFormat(dataset));
    }

    @Test
    public void path_does_not_reach_the_root_of_hierarchy() throws IOException {
        File dataset = new File(testFolder + "/path_does_not_reach_the_root_of_hierarchy.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL apparel/shirts, apparel/trousers, trousers/long, trousers/short, root/footwear, footwear/sneakers, footwear/socks, footwear/sandals\n\n");
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("HIE-PATH", checkFormat(dataset));
    }

    @Test
    public void class_hierarchy_attribute_is_not_the_last_attribute() throws IOException {
        File dataset = new File(testFolder + "/class_hierarchy_attribute_is_not_the_last_attribute.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(classAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("HIE-LAST", checkFormat(dataset));
    }

    @Test
    public void missing_attributes_that_describe_properties_of_examples() throws IOException {
        File dataset = new File(testFolder + "/missing_attributes_that_describe_properties_of_examples.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("ATT-DEF", checkFormat(dataset));
    }

    @Test
    public void duplicate_attribute_name() throws IOException {
        File dataset = new File(testFolder + "/duplicate_attribute_name.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());

            String[] attribDefinitions = attributesDescribingPropertiesOfExamples().split("\n");
            for (int i = 0; i < attribDefinitions.length; i++)
                if (i == 2)
                    bw.write("@ATTRIBUTE most_represented_material {cotton, linen}\n");
                else if (!attribDefinitions[i].isEmpty())
                    bw.write(attribDefinitions[i] + "\n");

            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("ATT-DUPLICATE", checkFormat(dataset));
    }

    @Test
    public void number_of_attributes_in_example_and_header_do_not_match() throws IOException {
        File dataset = new File(testFolder + "/number_of_attributes_in_example_and_header_do_not_match.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 4)
                    bw.write("5, no, polyester, elastane, 85, apparel@trousers@long\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-NUM-VALS", checkFormat(dataset));
    }

    @Test
    public void example_ID_attribute_value_is_missing() throws IOException {
        File dataset = new File(testFolder + "/example_ID_attribute_value_is_missing.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 5)
                    bw.write("?, no, linen, none, 74, yes, apparel@shirts\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-ID-MISSING", checkFormat(dataset));
    }

    @Test
    public void class_attribute_value_is_missing() throws IOException {
        File dataset = new File(testFolder + "/class_attribute_value_is_missing.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 1)
                    bw.write("2, yes, leather, cotton, ?, no, ?\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-CLASS-VAL-MISSING", checkFormat(dataset));
    }

    @Test
    public void duplicate_example_ID() throws IOException {
        File dataset = new File(testFolder + "/duplicate_example_ID.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 1)
                    bw.write("1, yes, leather, cotton, ?, no, footwear@sneakers@socks\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-DUPLICATE-ID", checkFormat(dataset));
    }

    @Test
    public void numeric_attribute_has_non_numeric_value() throws IOException {
        File dataset = new File(testFolder + "/numeric_attribute_has_non_numeric_value.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 5)
                    bw.write("6, no, linen, none, no, yes, apparel@shirts\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-NUM-VAL-ERR", checkFormat(dataset));
    }

    @Test
    public void nominal_attribute_has_non_nominal_value() throws IOException {
        File dataset = new File(testFolder + "/nominal_attribute_has_non_nominal_value.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 3)
                    bw.write("4, no, 56, elastane, 45, no, apparel@trousers@short\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-NOMINAL-VAL-ERR", checkFormat(dataset));
    }

    @Test
    public void all_attribute_values_are_missing() throws IOException {
        File dataset = new File(testFolder + "/all_attribute_values_are_missing.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 2)
                    bw.write("3, ?, ?, ?, ?, ?, apparel@shirts\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-ALL-VALS-MISSING", checkFormat(dataset));
    }

    @Test
    public void labels_are_incorrectly_formatted() throws IOException {
        File dataset = new File(testFolder + "/labels_are_incorrectly_formatted.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 0)
                    bw.write("1, yes, rubber, plastic, 28, no, footwear @ yellow-sandals\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-LABS-ALPHANUM-AND-DOT", checkFormat(dataset));
    }

    @Test
    public void label_not_defined_in_header() throws IOException {
        File dataset = new File(testFolder + "/label_not_defined_in_header.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 0)
                    bw.write("1, yes, rubber, plastic, 28, no, footwear@yellowSandals\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-LAB-NOT-IN-HEADER", checkFormat(dataset));
    }

    @Test
    public void example_labeled_with_invalid_path() throws IOException {
        File dataset = new File(testFolder + "/example_labeled_with_invalid_path.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 0)
                    bw.write("1, yes, rubber, plastic, 28, no, footwear@short\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-INVALID-PATH", checkFormat(dataset));
    }

    @Test
    public void label_defined_in_header_but_not_associated_with_any_of_the_examples() throws IOException {
        File dataset = new File(testFolder + "/label_defined_in_header_but_not_associated_with_any_of_the_examples.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");

            String[] examples = examples().split("\n");
            for (int i = 0; i < examples.length; i++)
                if (i == 4)
                    bw.write("5, no, polyester, elastane, 85, no, apparel@trousers\n");
                else if (!examples[i].isEmpty())
                    bw.write(examples[i] + "\n");
        }

        assertEquals("EXP-NOT-REPRESENTED-LABS", checkFormat(dataset));
    }

    @Test
    public void data_keyword_missing_in_the_data_set() throws IOException {
        File dataset = new File(testFolder + "/data_keyword_missing_in_the_data_set.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write(examples());
        }

        assertEquals("DATA-KEYWORD-MISSING", checkFormat(dataset));
    }

    @Test
    public void comments() throws IOException {
        File dataset = new File(testFolder + "/comments.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write("% Test data set\n");
            bw.write("%Another comment\n");
            bw.write("@RELATION \"product categorization\" % Attributes:\n\n");
            bw.write(exampleIDAttribute().trim() + " % Example ID attribute\n");
            for (String line : attributesDescribingPropertiesOfExamples().split("\n"))
                bw.write(line + " % Attribute\n");
            bw.write("@ATTRIBUTE CLASS HIERARCHICAL root/apparel, apparel/shirts, apparel/trousers, trousers/long, trousers/short, root/footwear, footwear/sneakers, footwear/socks, footwear/sandals%Class hierarchy\n\n");
            bw.write("@DATA%Data section\n");
            bw.write("% Examples:\n");
            bw.write(examples());
            bw.write("  % The end");
        }

        assertEquals("NO-ERRORS", checkFormat(dataset));
    }

    @Test
    public void no_errors() throws IOException {
        File dataset = new File(testFolder + "/no_errors.arff");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dataset))) {
            bw.write(relation());
            bw.write(exampleIDAttribute());
            bw.write(attributesDescribingPropertiesOfExamples());
            bw.write(classAttribute());
            bw.write("@DATA\n");
            bw.write(examples());
        }

        assertEquals("NO-ERRORS", checkFormat(dataset));
    }

    @AfterAll
    public static void removeTestFolder() {
        try {
            FileUtils.deleteDirectory(testFolder.getParentFile());
        } catch (IOException e) { System.err.println("Can't delete test folder " + testFolder.getParentFile().getAbsolutePath()); }
    }
}
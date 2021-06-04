---
sort: 1
---

# JAR file

Prerequisite to run the pipeline is [Java 8](https://www.oracle.com/java/technologies/javase-downloads.html).

[Download the JAR file](https://github.com/vedranav/hierarchy-decomposition-pipeline/releases/download/v0.0.1/hierarchy-decomposition-pipeline-0.0.1.jar) or use the wget tool:
```
wget https://github.com/vedranav/hierarchy-decomposition-pipeline/releases/download/v0.0.1/hierarchy-decomposition-pipeline-0.0.1.jar
```

To run the JAR file type the following into Terminal/Command Prompt:

```
java -jar hierarchy-decomposition-pipeline-0.0.1.jar settings.s
```

where settings.s is a file with [settings](https://vedranav.github.io/hierarchy-decomposition-pipeline/inputs/settings.html) that define which tools will be run and how.

Examples of settings files for three use cases, namely cross-validation, annotation and data set properties extraction, follow. All examples use the Enron data set.


## Compare algorithms' performance in cross-validation

The minimal settings file to compare performance of the Baseline and Labels without hierarchical relations algorithms on the [Enron data set](https://github.com/vedranav/hierarchy-decomposition-pipeline/raw/master/src/test/resources/enron.harff.zip) contains:

**enron-cross-validation.s**
```
tools = 1-3
baselineDataset = enron.harff.zip
outputFolder = enron/
```

The first line indicates that tools 1, 2 and 3 will run. The tool 1 divides examples into [cross-validation](https://vedranav.github.io/hierarchy-decomposition-pipeline/tools/cross-validation.html) folds. The tools 2 and 3 perform cross-validation for the two algorithms using the same folds. The second line contains path to the Enron data set. The last line contains path to an output folder. If the output folder does not exist, the pipeline will create it.

To run the pipeline:
- paste the three lines into a file
- save the file as enron-cross-validation.s (in the same folder as the JAR file and the Enron data set)
- run the pipeline by typing:

```
java -jar hierarchy-decomposition-pipeline-0.0.1.jar enron-cross-validation.s
```

In this example, the pipeline used the default values for other settings relevant in the cross-validation context. Those settings are:
- numTrees - number of trees in random forest (default 500)
- numFolds - number of cross-validation folds (default 10)
- thresholds - confidence thresholds at which the threshold-dependent performance is measured (default 0.5, 0.7, 0.9)
- memory - maximal amount of memory available to machine learning algorithms (default 2g)
- numProcessors - maximal number of processors available for parallel tasks (default 2)


## Annotate unlabelled set

The minimal settings file to construct a classification model from the [Enron training set](https://github.com/vedranav/hierarchy-decomposition-pipeline/raw/master/src/test/resources/enron-training_set.harff.zip) and to use the model to annotate [unlabelled examples](https://github.com/vedranav/hierarchy-decomposition-pipeline/raw/master/src/test/resources/enron-unlabelled_set.harff.zip) contains:

**enron-annotation.s**
```
tools = 7
baselineDataset = enron-training_set.harff.zip
unlabelledSet = enron-unlabelled_set.harff.zip
outputFolder = enron/
```

The lines indicate:
1. the annotation task
2. path to the training set
3. path to the unlabelled set
4. the output folder

Assuming that the enron-annotation.s settings file is in the same folder as the JAR file, training and unlabelled set, type the following to run the pipeline:

```
java -jar hierarchy-decomposition-pipeline-0.0.1.jar enron-annotation.s
```

Currently, only the Baseline algorithm can be used to construct a model.

Results of both cross-validation and annotation tasks can be outputted in the same folder. The pipeline creates separate subfolders for the two tasks.

Other settings applicable to the annotation task are: numTrees, memory and numProcessors.


## Compute data set properties

The settings file to compute [data set properties](https://vedranav.github.io/hierarchy-decomposition-pipeline/inputs/repository.html) for the Enron data set is:

**enron-dataset-properties.s**
```
tools = 8
baselineDataset = enron.harff.zip
outputFolder = enron/
```

The settings file to extract data set properties for the Enron training and unlabelled sets is:

**enron-dataset-properties-training-and-unlabelled-set.s**
```
tools = 8
baselineDataset = enron-training_set.harff.zip
unlabelledSet = enron-unlabelled_set.harff.zip
outputFolder = enron/
```
Other settings do not apply to this tool.

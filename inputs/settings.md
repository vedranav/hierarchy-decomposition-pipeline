---
sort: 3
---

# Settings

Settings define which tools will the pipeline run and how. There are four groups of settings:
1. **Mandatory** are essential settings that must be defined for the pipeline to run.
2. **Machine learning** settings define the number of trees in random forest ensembles and limit computational resources available to machine learning algorithms.
3. **Cross-validation** settings define the number of cross-validation folds and enumerate thresholds at which the threshold-dependent performance is measured.
4. **Annotation** setting sets path to unlabelled set.

***

## Mandatory

### Tools

Sets which of the following tools will be executed:
1. Distribute examples into cross-validation folds
2. Run baseline algorithm
3. Run complete hierarchy decomposition algorithm "Labels without hierarchical relations"
4. Run complete hierarchy decomposition algorithm "Label vs. the rest"
5. Run partial hierarchy decomposition algorithm "Child vs. parent label"
6. Run partial hierarchy decomposition algorithm "Label specialization"
7. Annotate unlabelled set using classification model constructed with the baseline algorithm
8. Compute data set statistics

For example, this line will run the first six tools:
```
tools = 1-6
```
Value of this setting is a comma separated list of numbers representing tools, ranges of those numbers (e.g., 1-6) or a combination thereof (e.g., 1,3-6).

The precondition for running tools 2-6 is to first distribute examples into cross-validation folds (1). Suppose that we want to run the algorithms 3 and 5 in separate runs. In the first run we will set "tools = 1, 3" and in the second "tools = 5". In this manner, both algorithms will run on the same cross-validation folds.

### Baseline data set

Sets path to a data set with hierarchical class.
```
baselineDataset = data/Enron.harff.zip
```

### Output folder

Sets path to a folder where the pipeline will write its output.
```
outputFolder = hierarchy-decomposition-pipeline/output/Enron/
```
***

## Machine learning

### Number of trees

The pipeline constructs random forest ensembles. This setting sets the number of trees in the forest.
```
numTrees = 500
```
Default value is 500.

### Memory available to machine learning algorithms

Maximal amount of memory available to machine learning algorithms.
```
memory = 5g
```
Value is composed of a number and a letter 'k' or 'K' when the number indicates kilobytes, 'm' or 'M' when the number indicates megabytes, or 'g' or 'G' when the number indicates gigabytes. Default value is 2g.

### Processors available to machine learning algorithms

Maximal number of processors available for parallel tasks.
```
numProcessors = 4
```
Default value is 2.

***

## Cross-validation

### Number of folds
Sets the number of cross-validation folds.
```
numFolds = 10
```
Default value is 10.

### Thresholds
Sets a comma separated list of confidence thresholds at which the [threshold-dependent performance](https://vedranav.github.io/hierarchy-decomposition-pipeline/tools/cross-validation.html#threshold-dependent-measures) is measured. Thresholds are probabilities.
```
thresholds = 0.5, 0.7
```
Default value is "0.5, 0.7, 0.9".

***

## Annotation

### Unlabelled set
Sets path to an unlabelled set. This setting is mandatory when annotation tool is used.
```
unlabelledSet = data/Enron-unlabelledSet.harff.zip
```

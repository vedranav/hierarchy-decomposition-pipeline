---
sort: 2
title: Cross-validation
---

# Cross-validation

Cross-validation is a resampling procedure used to estimate model's predictive performance on a limited data sample. The procedure takes as inputs a data set and a value of the parameter *n*. Then, it randomly divides examples from the data set into *n* folds of approximately equal size. Finally, it repeats *n* times:
  - Hold *i*<sup>th</sup> fold as a test set
  - Construct a model from the rest of the folds (training set)
  - Use the model to annotate examples in the test set

![Cross-validation](https://vedranav.github.io/hierarchy-decomposition-pipeline/images/tools/Cross-validation.png)

For each ensemble model, the cross-validation procedure outputs a table with confidences. Rows in the table are examples, columns are labels (from a hierarchical class) and values are probabilities that the labels are associated with the examples. The probabilities indicate how confident the model is in the established associations. The table aggregates examples from *n* test sets.

```tip
In the pipeline, there is a task that divides examples from an input data set (named baseline data set) into *n* folds. Once the cross-validation folds are created, the pipeline saves the information on which examples are associated with each of the folds. If you want to compare the algorithms on the same cross-validation folds, run this task only once. Then you can run any combination of the algorithms and all of them will be evaluated on the same cross-validation folds.
```


# Measures of model's predictive performance

From the table with confidences the pipeline computes two types of measures of model's predictive performance:
- **Threshold-dependent measures** set a confidence threshold that differentiates between positive and negative associations of labels with examples. The measures are precision, recall, F-measure and accuracy.
- **Threshold independent measures** summarize predictive performance over a range of confidence thresholds. The measures are AUPRC and AUC.

## Threshold-dependent measures

### Prediction

Suppose that a model predicts a label *l* for a set of examples. For each example, it will output a confidence. To compute threshold-dependent measures from the confidences the pipeline follows the steps:
- Set a confidence threshold *t*
- Associate with *l* all the examples with confidence for *l* higher than or equal to *t*
- Associate with not-*l* the rest of the examples

Steps are repeated for all of the labels in a data set. In analogy with table with confidences, the confidences are substituted with ones and zeros indicating *l* and not-*l* associations for a specific *t*.

### Comparison of predicted labels with actual labels

Model's decision to associate *l* or not-*l* with an example may or may not be correct. By making such observations over a set of examples, four outcomes can be recorded for the model and the label *l*:
- **True positives (TP)** are examples for which the model correctly predicted *l*
- **False positives (FP)** are examples for which the model incorrectly predicted *l*
- **True negatives (TN)** are examples for which the model correctly predicted not-*l*
- **False negatives (FN)** are examples for which the model incorrectly predicted not-*l*

The outcomes can be represented with a table named **confusion matrix** as follows:

<table style="text-align:center;">
    <tr>
        <th style = "background: linear-gradient(to top right, white 49.5%, black 49.5%, black 50.5%, white 50.5%); color: black; line-height: 1;"><div style = "margin-left: 2em; text-align: right;">Actual<br>label</div><div style = "margin-right: 2em; text-align: left;">Predicted &#160;</div></th>
        <th><i>l</i></th>
        <th>not-<i>l</i></th>
    </tr>
    <tr>
        <th><i>l</i></th>
        <td>TP</td>
        <td>FP</td>
    </tr>
    <tr>
        <th>not-<i>l</i></th>
        <td>FN</td>
        <td>TN</td>
    </tr>
</table>


### Label-based measures

From the confusion matrix for a label *l*, the pipeline computes four measures of model's predictive performance for *l*:

- **Accuracy** is a share of examples with correct predictions in all of the examples

$$ \text{accuracy} = \frac{\text{TP + TN}}{\text{TP + FP + TN + FN}} $$

- **Precision** is a share of examples with correctly predicted *l* in the set of examples with predicted *l*

$$ \text{precision} = \frac{\text{TP}}{\text{TP + FP}} $$

- **Recall** is a share of examples with correctly predicted *l* in the set of examples that are actually associated with *l*

$$ \text{recall} = \frac{\text{TP}}{\text{TP + FN}} $$

- **F-measure** is a harmonic mean of precision and recall

$$ \text{F-measure} = \frac{2 \times \text{precision} \times \text{recall}}{\text{precision + recall}} $$

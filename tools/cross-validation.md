---
sort: 2
title: Cross-validation
---

# Tool that estimates models' predictive performance using cross-validation

## Cross-validation

Cross-validation is a resampling procedure used to estimate model's predictive performance on a limited data sample. The procedure takes as inputs a data set and a value of the parameter *n*. Then, it randomly divides examples from the data set into *n* folds of approximately equal size. Finally, it repeats *n* times:
  - Hold *i*<sup>th</sup> fold as a test set
  - Construct a model from the rest of the folds (training set)
  - Use the model to annotate examples in the test set

<img src = "https://vedranav.github.io/hierarchy-decomposition-pipeline/images/tools/Cross-validation.png" alt = "Cross-validation" width = "450">

### Output

For each ensemble model the cross-validation procedure outputs a **table with confidences**. Rows in the table are examples, columns are labels (from a hierarchical class) and values are probabilities that the labels are associated with the examples. The probabilities indicate how confident the model is in the established associations. The table aggregates examples from *n* test sets.

<div style = "background-color: #F8F8FF; border: 1px solid #B0C4DE; border-radius: 8px; padding: 0px 20px;">

### Example
Suppose that we have a simple data set with a tree-shaped hierarchical class of five labels connected in the following manner:

```mermaid
graph TB;
    A(( ))-->B((l1))
    A-->C((l2))
    C-->D((l3))
    C-->E((l4))
    C-->F((l5))
```

The data set has ten examples. Two-fold cross-validation randomly divides examples in two groups of five: (e2, e3, e7, e9, e10) and (e1, e4, e5, e6, e8). The procedure first constructs a model using the baseline algorithm from the first set of examples and uses the model to make prediction for the second. Then it constructs a model from the second set of examples and makes prediction for the first. The resulting table with confidences is:

<table>
    <tr>
        <th rowspan = "2" style = "vertical-align: bottom">Example ID</th>
        <th colspan = "5">Labels</th>
    </tr>
    <tr>        
        <th>l1</th>
        <th>l2</th>
        <th>l3</th>
        <th>l4</th>
        <th>l5</th>
    </tr>
    <tr style = "text-align: right">
        <td>e1</td> <td>0.12</td> <td>0.87</td> <td>0.05</td> <td>0.61</td> <td>0.79</td>
    </tr>
    <tr style = "text-align: right">
        <td>e2</td> <td>0.98</td> <td>0.05</td> <td>0</td> <td>0</td> <td>0.01</td>
    </tr>
    <tr style = "text-align: right">
        <td>e3</td> <td>0.02</td> <td>0.59</td> <td>0.05</td> <td>0.24</td> <td>0.59</td>
    </tr>    
    <tr style = "text-align: right">
        <td>e4</td> <td>0</td> <td>0.99</td> <td>0.81</td> <td>0.33</td> <td>0.4</td>
    </tr>
    <tr style = "text-align: right">
        <td>e5</td> <td>0.31</td> <td>0.55</td> <td>0.12</td> <td>0.05</td> <td>0.01</td>
    </tr>
    <tr style = "text-align: right">
        <td>e6</td> <td>0.19</td> <td>0.91</td> <td>0.88</td> <td>0.02</td> <td>0</td>
    </tr>
    <tr style = "text-align: right">
        <td>e7</td> <td>0.84</td> <td>0.12</td> <td>0.01</td> <td>0</td> <td>0</td>
    </tr>
    <tr style = "text-align: right">
        <td>e8</td> <td>0.14</td> <td>0.74</td> <td>0.09</td> <td>0.71</td> <td>0.73</td>
    </tr>
    <tr style = "text-align: right">
        <td>e9</td> <td>0.31</td> <td>0.89</td> <td>0.27</td> <td>0.88</td> <td>0.84</td>
    </tr>
    <tr style = "text-align: right">
        <td>e10</td> <td>0.92</td> <td>0.05</td> <td>0</td> <td>0</td> <td>0.01</td>
    </tr>
</table>

The table shows which paths from the hierarchy are more or less likely associated with each of the examples. For example, the baseline model is strongly confident in the association between the example e6 and l2 -> l3 path from the hierarchy (confidence &ge; 0.88).

Note that confidence values for an individual example (within a row) satisfy hierarchy constraint. In other words, confidences for labels do not surpass the confidence of their parent label.

</div>

```tip
The pipeline has a separate task that divides examples from an input data set (named baseline data set) into *n* folds. Once the cross-validation folds are created, the pipeline saves the information on which examples are associated with each of the folds. If you want to compare the algorithms on the same cross-validation folds, run this task only once. Then you can run any combination of the algorithms and all of them will be evaluated on the same cross-validation folds.
```

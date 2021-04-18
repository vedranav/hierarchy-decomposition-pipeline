---
sort: 1
---

# Gene function prediction

The steps of the gene function prediction task are:
1. Create a data set representing genes with known biological function,
2. Induce a classification model from the data,
3. Use the model to predict biological function of genes with unknown or partially known function.

![Gene function prediction data set](images/applications/GFP_dataset.png)

The data set has:
- Class labels that represent multiple aspects of genes' biological functions. Labels are taken from [Gene Ontology](http://geneontology.org) and are represented with ID numbers (e.g., GO:0006629) and descriptions (e.g., lipid metabolic process).
- Labels organised in a hierarchy with nodes representing functions and relations among nodes bottom-up generalisation of gene function (e.g., membrane is a cellular anatomical entity).
- Hierarchy in the form of a directed acyclic graph, where a node can have multiple parents.
- Examples annotated with one or several paths in the hierarchy. For example:
  * Gene *g*<sub>1</sub> is annotated with one path, which shows that its function is manifested in a nucleoid (GO:0009295), which is a cellular anatomical entity (GO:0110165).
  * Gene *g*<sub>2</sub> is annotated with two paths:
    1. the gene participates in the lipid metabolic process: the path beginning with GO:0006629,
    2. the process is manifested in membrane: the path beginning with GO:0016020.

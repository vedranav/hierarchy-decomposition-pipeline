---
sort: 2
title: Repository of data sets
---

# Repository of data sets with hierarchical class

## General info and files

| Data set | Domain              | Examples | Nominal attributes | Numeric attributes | Source                | File                                              |
| -------- | ------------------- | -------: | -----------------: | -----------------: | --------------------- | ------------------------------------------------- |
| Enron    | text categorization | 1,648    | 1,001              | 0                  | Klimt and Yang, 2004  | [<i class="fa fa-download" aria-hidden="true"></i>](https://github.com/vedranav/hierarchy-decomposition-pipeline/raw/master/src/test/resources/enron.harff.zip) |
| Phyletic profiles | functional genomics | 15,313 | 2,071 | 0 | Vidulin et al., 2016 | [<i class="fa fa-download" aria-hidden="true"></i>](https://github.com/vedranav/hierarchy-decomposition-pipeline/raw/master/src/test/resources/phyletic_profiles.harff.zip) |

Domains:
- **Text categorization** is a problem of automatic annotation of textual documents with one or several categories.
- **Functional genomics** annotates genes with their biological functions.

Data sets:
- **Enron** data set contains bag-of-words descriptions of e-mails from the Enron corporation officials. Hierarchically organized categories define genre, emotional tone and topic.
- **Phyletic profiles** data set contains presence and absence patterns of gene families (clusters of genes that share function) in 2,071 bacterial and archaeal genomes. Gene families are annotated with functions from Gene Ontology.


Data sets are in [HARFF format](https://vedranav.github.io/hierarchy-decomposition-pipeline/inputs/dataset.html), which is a valid input into the pipeline.

Data set properties from the tables are explained [here](https://vedranav.github.io/hierarchy-decomposition-pipeline/tools/properties.html).


## Hierarchical class

| Data set          | Labels | Leaves | Maximal depth | Type | Average forward branching factor | Average backward branching factor |
| ----------------- | -----: | -----: | ------------: | ---- | -------------------------------: | --------------------------------: |
| Enron             | 56     | 52     | 3             | tree | 11.20                            | 1                                 |
| Phyletic profiles | 1,260  | 377    | 14            | DAG  | 2.63                             | 1.85                              |


## Annotations

| Data set          | Most specific labels | Cardinality - complete | Cardinality - hierarchical |
| ----------------- | -------------------: | ---------------------: | -------------------------: |
| Enron             | 53                   | 2.87                   | 3.37                       |
| Phyletic profiles | 947                  | 2.59                   | 16.67                      |


## Sources

- **Enron**: Klimt B., Yang Y. (2004) The Enron corpus: A new dataset for email classification research. In *European Conference on Machine Learning* (pp. 217-226). Springer, Berlin, Heidelberg.
- **Phyletic profiles**: Vidulin V., Šmuc T., Supek F. (2016) Extensive complementarity between gene function prediction methods. *Bioinformatics*, 32(23), 3645-3653.

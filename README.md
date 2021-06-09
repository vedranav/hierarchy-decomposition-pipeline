# Hierarchy decomposition pipeline

Hierarchy decomposition pipeline is a supervised machine learning tool that constructs random forest ensembles from data sets with hierarchical class.

<img src = "images/HMC_dataset.png" alt = "Data set with hierarchical class" width = "500">

Suitable data sets have:
- Class labels organised in a hierarchy
- Hierarchy in the shape of a tree or directed acyclic graph
- Examples annotated with one or several paths from the hierarchy

## Features

- Five algorithms that construct ensemble models from data sets with hierarchical class
- Tool for comparing algorithms' predictive performances in cross-validation
- Tool for predicting paths from the hierarchy that best describe unlabelled examples
- Tool that computes data set properties

## Quick start

- Prerequisite to run the pipeline is [Java 8](https://www.oracle.com/java/technologies/javase-downloads.html)
- [Download the JAR file](https://github.com/vedranav/hierarchy-decomposition-pipeline/releases/download/v0.0.1/hierarchy-decomposition-pipeline-0.0.1.jar)
- [Download a data set from the repository](https://vedranav.github.io/hierarchy-decomposition-pipeline/inputs/repository.html) or use your own data set in [HARFF format](https://vedranav.github.io/hierarchy-decomposition-pipeline/inputs/dataset.html)
- [Create settings file](https://vedranav.github.io/hierarchy-decomposition-pipeline/pipeline/jar.html)
- Assuming that the JAR file and settings file (e.g., named settings.s) are in the same folder, run the pipeline by typing:
```
java -jar hierarchy-decomposition-pipeline-0.0.1.jar settings.s
```

## Reference

The pipeline is based on ideas presented in the following paper:

Vidulin V., Džeroski S. (2020) Hierarchy Decomposition Pipeline: A Toolbox for Comparison of Model Induction Algorithms on Hierarchical Multi-label Classification Problems. In: Appice A., Tsoumakas G., Manolopoulos Y., Matwin S. (eds) Discovery Science. DS 2020. Lecture Notes in Computer Science, vol 12323. Springer, Cham. [https://doi.org/10.1007/978-3-030-61527-7_32](https://doi.org/10.1007/978-3-030-61527-7_32)

If you find the pipeline useful, please cite that reference.

## Contact

If you have a data mining problem with hierarchical class and are interested in cooperation, feel free to contact me.

## Warning
This website is a work in progress. It may contain incomplete information and possibly errors. You can help by reviewing the content and posting your comments and corrections [here](https://github.com/vedranav/hierarchy-decomposition-pipeline/issues).

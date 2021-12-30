# RDF Differential Privacy - improved implementation

This project is an improved version of differential privacy implementation in RDF.

This previous implementation was created for the paper [Differential Privacy and SPARQL, swj2610](http://www.semantic-web-journal.net/content/differential-privacy-and-sparql) in this repo: [PrivacyTest1](https://github.com/cbuil/PrivacyTest1).

## What is improved?

- [x] Java styleguide standardization (Google Java Format)
- [x] A clean code
- [x] Theoretical optimization for the function:
  - Let P(k) be a polynomial and _k_ in _\[0, graphSize\]_, **max eˆ(-beta\ k)\*P(k)**
    - Improved calculating the derivatives for max and min values of the function, minimizing the iterations for maximize as a function of k.

- [ ] Use of caches to improve the response time for differentially private queries.
  - [ ] Cache for differentially privacy queries.
  - [ ] Cache for most map values.


## How to use

### First, install/add the next dependencies:
In Maven:
- google.code.gson:2.8.9
- rdfhdt.hdt.jena:2.1.2
- rdfhdt.hdt.java.core:2.1.2
- apache.jena.core:3.7.0
- google.guava:2.8.9
- apache.jena.arq:3.7.0
- apache.logging.log4j.core:2.14.1

In _lib_ folder:
- SymJava-1.1.2 ([Github](https://github.com/yuemingl/SymJava))
- matheclipse-core ([Github](https://github.com/axkr/symja_android_library), compiled 9-nov-2021)

### Next, see the README in Run folder:
- Generate Count Queries: generate queries in the _resources_ folder.
- RunQueriesParallel: run queries with asynchronous methods.
- RunSparqlQuery: run SPARQL query and print the result.
- RunSymbolic: process the queries for differential privacy.
- RunQuery: run SPARQL query, write the result in an output file.

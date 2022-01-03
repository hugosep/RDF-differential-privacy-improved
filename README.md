# RDF Differential Privacy - improved implementation

This project is an improved version of differential privacy implementation in RDF.

This previous implementation was created for the paper [Differential Privacy and SPARQL, swj2610](http://www.semantic-web-journal.net/content/differential-privacy-and-sparql) in this repo: [PrivacyTest1](https://github.com/cbuil/PrivacyTest1).

## What is improved?

- [x] Java styleguide standardization (Google Java Format)
- [x] A clean code
- [x] Theoretical optimization for the function:
  - Let P(k) be a polynomial and _k_ in _\[0, graphSize\]_, **max eˆ(-beta\ k)\*P(k)**
    - Improved calculating the derivatives for max and min values of the function, minimizing the iterations for maximize as a function of k.

- [x] Use of caches to improve the response time for differentially private queries.
  - [x] Cache for differentially privacy queries.
  - [x] Cache for most map values.


## How to use

### First, install/add the next dependencies:
In Maven:
- com.google.code.gson:gson:2.8.9
- org.rdfhdt:hdt-jena:2.1.2
- org.rdfhdt:hdt-java-core:2.1.2
- org.apache.jena:jena-core:3.7.0
- com.google.guava:guava:31.0.1-jre
- org.apache.jena:jena-arq:3.7.0
- com.github.ben-manes.caffeine:caffeine:3.0.5
- org.apache.logging.log4j:log4j-core:2.16.0

In _lib_ folder:
- matheclipse-core ([Github](https://github.com/axkr/symja_android_library), compiled 9-nov-2021)

### Next, see the README in Run folder:
- Generate Count Queries: generate queries in the _resources_ folder.
- RunQueriesParallel: run queries with asynchronous methods.
- RunSparqlQuery: run SPARQL query and print the result.
- RunSymbolic: process the queries for differential privacy.
- RunQuery: run SPARQL query, write the result in an output file.

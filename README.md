# RDF Differential Privacy - improved implementation

This project is an improved version of differential privacy implementation in RDF.

This previous implementation was created for the paper [Differential Privacy and SPARQL, swj2610](http://www.semantic-web-journal.net/content/differential-privacy-and-sparql) in this repo: [PrivacyTest1](https://github.com/cbuil/PrivacyTest1).

## What is improved?

- [ ] Java styleguide standardization
- [ ] A cleaner code
- [ ] Theoretical optimization for the function:
  - Let be P(k) a polynomial and k in \[0, graphSize\], **max Eˆ(-beta*k)*P(k)**
    - Improved calculating the derivatives for max and min values of the function, minimizing the iterations for maximize as a function of k.

- [ ] Use of caches for improve the response time for differentially private queries.
  - [ ] Cache for graph sizes results. 
  - [ ] Cache for most map values.


## How to use

### First, install/add the next dependencies:

Next, see the README in Run folder.

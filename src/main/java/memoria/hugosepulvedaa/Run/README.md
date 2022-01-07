### GenerateCountQueries

Generate count queries. Possible input:

```
$ -f {input SPARQL query file}
$ -o {output files}
```

### RunQueriesParallel

Run asynchronous queries in parallel. Possible input:

```
$ -f {input SPARQL query file}
$ -d {HDT data file}
$ -c {cores}
$ -o {output files}
```

### RunQuery

Run SPARQL query. Possible input:

```
$ -f {input SPARQL query file}
$ -d {HDT data file}
$ -e {endpoint address}
```

### RunSymbolic

Determine the polinomial associated to a count query and the elastic sensitivity calculus.

```
$ -q {input SPARQL query}
$ -f {input SPARQL query file}
$ -d {HDT data file}
$ -e {query directory}
$ -o {output file}
$ -v {evaluation}
$ -eps {epsilon}
```

### RunTime

Run a query or queries. Used to the test _working_ queries:

```
$ -q {input SPARQL query}
$ -f {input SPARQL query file}
$ -d {HDT data file}
$ -e {query directory}
$ -o {output file}
$ -v {evaluation}
$ -eps {epsilon}
```

### RunTime

Run a cache plan, reading lines of the _planCache.txt_. Used to the test _working_ queries and the eviction policy of cache:

```
$ -q {input SPARQL query}
$ -f {input SPARQL query file}
$ -d {HDT data file}
$ -e {query directory}
$ -o {output file}
$ -v {evaluation}
$ -eps {epsilon}
```
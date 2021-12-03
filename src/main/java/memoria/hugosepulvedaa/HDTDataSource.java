package memoria.hugosepulvedaa;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import memoria.hugosepulvedaa.utils.Helper;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** @author cbuil */
public class HDTDataSource implements DataSource {

    final HDT dataSource;
    final NodeDictionary dictionary;

    HDTGraph graph;
    Model triples;

    private static final Logger logger = LogManager.getLogger(HDTDataSource.class.getName());
    private static LoadingCache<MaxFreqQuery, Integer> mostFrequentResultCache;
    private static LoadingCache<String, Integer> countQueriesCache;
    private static LoadingCache<Query, Model> graphModelsCache;
    private static LoadingCache<List<List<String>>, Long> graphSizeTriplesCache;
    private static final Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
    private static final Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

    public HDTDataSource(String hdtFile) throws IOException {

        dataSource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(dataSource.getDictionary());
        graph = new HDTGraph(dataSource);
        triples = ModelFactory.createModelForGraph(graph);

        mostFrequentResultCache =
                CacheBuilder.newBuilder()
                        .recordStats()
                        .maximumWeight(100000)
                        .weigher(
                                (Weigher<MaxFreqQuery, Integer>)
                                        (k, resultSize) -> k.getQuerySize())
                        .build(
                                new CacheLoader<MaxFreqQuery, Integer>() {

                                    @Override
                                    public Integer load(MaxFreqQuery s) {
                                        logger.info(
                                                "into mostPopularValueCache CacheLoader, loading: "
                                                        + s);
                                        return getMostFrequentResult(
                                                s.getOriginalQuery(),
                                                s.getQuery(),
                                                s.getVariableString());
                                    }
                                });

        graphModelsCache =
                CacheBuilder.newBuilder()
                        .recordStats()
                        .maximumWeight(1000)
                        .weigher((Weigher<Query, Model>) (k, resultSize) -> k.toString().length())
                        .build(
                                new CacheLoader<Query, Model>() {

                                    @Override
                                    public Model load(Query query) {
                                        logger.debug(
                                                "into graphSizeCache CacheLoader, loading: "
                                                        + query);
                                        return executeConstructQuery(query);
                                    }
                                });

        graphSizeTriplesCache =
                CacheBuilder.newBuilder()
                        .recordStats()
                        .maximumWeight(1000)
                        .weigher(
                                (Weigher<List<List<String>>, Long>)
                                        (k, resultSize) -> k.toString().length())
                        .build(
                                new CacheLoader<List<List<String>>, Long>() {

                                    @Override
                                    public Long load(List<List<String>> triplePatternsCount) {
                                        logger.debug(
                                                "into graphSizeCacheTriples CacheLoader, loading: "
                                                        + triplePatternsCount);
                                        return calculateGraphSizeTriples(triplePatternsCount);
                                    }
                                });
    }

    @Override
    public long getGraphSize(Query query) {
        return (graphModelsCache.getUnchecked(query).size());
    }

    public Model executeConstructQuery(Query query) {
        Element queryPattern = query.getQueryPattern();

        String constructQuery = "CONSTRUCT " + queryPattern + " WHERE " + queryPattern;

        logger.info("constructQuery: " + constructQuery);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, triples)) {
            Model results = qexec.execConstruct();
            return results;
        }
    }

    @Override
    public int executeCountQuery(String queryString, boolean principal) {

        Query query = QueryFactory.create(queryString);

        /* no entiendo por que esta esto
        if (queryString.contains("http://www.wikidata.org/prop/direct/P31") &&
                (queryString.lastIndexOf('?') != queryString.indexOf('?'))) {
            return 85869721;
        }*/
        QueryExecution qexec;

        try {
            Model modelQuery = graphModelsCache.get(query);
            qexec = QueryExecutionFactory.create(query, modelQuery);

        } catch (ExecutionException e) {
            qexec = QueryExecutionFactory.create(query, triples);
        }

        ResultSet results = qexec.execSelect();
        QuerySolution soln = results.nextSolution();

        logger.info("Count query executed... ");

        qexec.close();

        RDFNode x = soln.get(soln.varNames().next());
        int countResult = x.asLiteral().getInt();

        logger.info("Count query result (endpoint): " + countResult);
        return countResult;
    }

    /*
       @description Sum all COUNTs of every generated query.
    */
    @Override
    public Long getGraphSizeTriples(List<List<String>> triplePatternsCount) {
        return (graphSizeTriplesCache.getUnchecked(triplePatternsCount));
    }

    public Long calculateGraphSizeTriples(List<List<String>> triplePatternsCount) {

        long count = 0L;
        System.out.println("triplePatternsCount:" + triplePatternsCount);

        // triplePatternsCount has all triples generated by setMostFreqValueMaps method
        for (List<String> star : triplePatternsCount) {

            StringBuilder construct = new StringBuilder();

            for (String tp : star) {
                construct.append(tp).append(" . ");
            }

            logger.info("Construct query for graph size so far: " + construct);
            count +=
                    executeCountQuery(
                            "SELECT (COUNT(*) as ?count) WHERE { " + construct + "} ", false);
            logger.info("Graph size so far: " + count);
        }
        logger.info("count: " + count);
        return count;
    }

    @Override
    public int mostFrequentResult(Query originalQuery, MaxFreqQuery maxFreqQuery) {
        return mostFrequentResultCache.getUnchecked(maxFreqQuery);
    }

    @Override
    public void setMostFreqValueMaps(
            Query originalQuery,
            Map<String, List<TriplePath>> starQueriesMap,
            List<List<String>> triplePatterns) {

        Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
        Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

        // EDITED
        logger.info("StarQueriesMap: " + starQueriesMap);

        for (String key : starQueriesMap.keySet()) {

            // EDITED
            logger.info("Key: " + key);

            List<String> listTriple = new ArrayList<>();
            List<TriplePath> starQueryLeft = starQueriesMap.get(key);
            Set<String> varStrings = new LinkedHashSet<>();

            int i = 0;

            for (TriplePath triplePath : starQueryLeft) {
                String triple = "";

                if (triplePath.getSubject().isVariable()) {
                    varStrings.add(triplePath.getSubject().getName());
                    triple += "?" + triplePath.getSubject().getName();

                } else {
                    triple += " ?s" + i + " ";
                }

                triple += " <" + triplePath.getPredicate().getURI() + "> ";

                if (triplePath.getObject().isVariable()) {
                    varStrings.add(triplePath.getObject().getName());
                    triple += "?" + triplePath.getObject().getName();

                } else {
                    triple += " ?o" + i + " ";
                }
                i++;

                // EDITED
                logger.info("Triple: " + triple);

                listTriple.add(triple);
            }

            triplePatterns.add(listTriple);

            for (String var : varStrings) {
                // EDITED
                logger.info("var: " + var);

                MaxFreqQuery query =
                        new MaxFreqQuery(
                                originalQuery, Helper.getStarQueryString(starQueryLeft), var);

                if (mapMostFreqValue.containsKey(var)) {
                    List<Integer> mostFreqValues = mapMostFreqValue.get(var);
                    List<StarQuery> mostFreqValuesStar = mapMostFreqValueStar.get(var);

                    if (!mostFreqValues.isEmpty()) {
                        mostFreqValues.add(mostFrequentResultCache.getUnchecked(query));
                        mapMostFreqValue.put(var, mostFreqValues);

                        mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                        mapMostFreqValueStar.put(var, mostFreqValuesStar);
                    }

                } else {
                    List<Integer> mostFreqValues = new ArrayList<>();
                    logger.info("Query: " + query.getQuery());
                    logger.info("Variable: " + query.getVariableString());

                    mostFreqValues.add(mostFrequentResultCache.getUnchecked(query));
                    mapMostFreqValue.put(var, mostFreqValues);

                    List<StarQuery> mostFreqValuesStar = new ArrayList<>();

                    mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                    mapMostFreqValueStar.put(var, mostFreqValuesStar);
                }
            }
        }
    }

    @Override
    public Map<String, List<StarQuery>> getMapMostFreqValueStar() {
        return mapMostFreqValueStar;
    }

    @Override
    public Map<String, List<Integer>> getMapMostFreqValue() {
        return mapMostFreqValue;
    }

    private int getMostFrequentResult(Query originalQuery, String starQuery, String variableName) {

        variableName = variableName.replace("“", "").replace("”", "");

        String maxFreqQueryString =
                "SELECT (COUNT(?"
                        + variableName
                        + ") AS ?count) WHERE { SELECT ?"
                        + variableName
                        + " WHERE { "
                        + starQuery
                        + "} LIMIT 1000000 "
                        + "} GROUP BY ?"
                        + variableName
                        + " "
                        + "ORDER BY ?"
                        + variableName
                        + " DESC (?count) LIMIT 1 ";

        logger.info("query at getMostFrequentResult: " + maxFreqQueryString);

        Query query = QueryFactory.create(maxFreqQueryString);
        QueryExecution qexec;

        try {
            Model modelQuery = graphModelsCache.get(originalQuery);
            qexec = QueryExecutionFactory.create(query, modelQuery);

        } catch (ExecutionException e) {
            qexec = QueryExecutionFactory.create(query, triples);
        }

        ResultSet results = qexec.execSelect();

        if (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            RDFNode x = soln.get("count");
            int res = x.asLiteral().getInt();
            logger.info("max freq value: " + res + " for variable " + variableName);
            return res;
        } else {
            return 0;
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa;

import memoria.hugosepulvedaa.utils.Helper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * @author cbuil
 */
public class EndpointDataSource implements DataSource {

    private static final Logger logger = LogManager.getLogger(EndpointDataSource.class.getName());
    private final String dataSource;
    private static LoadingCache<MaxFreqQuery, Integer> mostFrequentResultCache;
    private static LoadingCache<String, Integer> countQueriesCache;
    private static LoadingCache<Query, Long> graphSizeCache;
    private final static Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
    private final static Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

    public EndpointDataSource(String endpoint) {

        dataSource = endpoint;

        mostFrequentResultCache = CacheBuilder
                .newBuilder()
                .recordStats()
                .maximumWeight(100000)
                .weigher((Weigher<MaxFreqQuery, Integer>) (k, resultSize) -> k.getQuerySize())
                .build(new CacheLoader<MaxFreqQuery, Integer>() {

                    @Override public Integer load(MaxFreqQuery s) {
                        logger.info("into mostPopularValueCache CacheLoader, loading: " + s);
                        return getMostFrequentResult(s.getQuery(), s.getVariableString());
                    }
                });

        graphSizeCache = CacheBuilder
                .newBuilder()
                .recordStats()
                .maximumWeight(1000)
                .weigher((Weigher<Query, Long>) (k, resultSize) -> k.toString().length())
                .build(new CacheLoader<Query, Long>() {

                    @Override public Long load(Query q) {
                        logger.debug("into graphSizeCache CacheLoader, loading: " + q);
                        return getGraphSize(q);
                    }
                });
    }

    @Override public ResultSet executeQuery(Query query) {

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, query)) {
            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            qexec.close();
            return results;
        }
    }

    @Override public long getGraphSize(Query query) {
        Long resultSize = graphSizeCache.getIfPresent(query);

        System.out.println("Query:" + query);
        System.out.println("Get query pattern:" + query.getQueryPattern());

        if (resultSize == null) {
            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, query)) {
                Model results = qexec.execConstruct();
                resultSize = results.size();
                logger.info("results: " + results);
                qexec.close();

            }
        }
        return resultSize;
    }

    @Override public int executeCountQuery(String queryString) {

        Query query = QueryFactory.create(queryString);

        Element queryPattern = query.getQueryPattern();

        logger.info("query: " + query);
        logger.info("queryPattern: " + queryPattern);

        String constructQuery = "CONSTRUCT" + queryPattern + " WHERE " + queryPattern;

        System.out.println("constructQuery: " + constructQuery);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, constructQuery)) {
            Model results = qexec.execConstruct();
            long resultSize = results.size();
            logger.info("results: " + resultSize);
        }

        /* no entiendo por que esta esto
        if (queryString.contains("http://www.wikidata.org/prop/direct/P31") &&
                (queryString.lastIndexOf('?') != queryString.indexOf('?'))) {
            return 85869721;
        }*/

        QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, query);

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
    @Override public long getGraphSizeTriples(List<List<String>> triplePatternsCount) {

        long count = 0;
        System.out.println("triplePatternsCount:" + triplePatternsCount);

        // triplePatternsCount has all triples generated by setMostFreqValueMaps method
        for (List<String> star : triplePatternsCount) {

            StringBuilder construct = new StringBuilder();

            for (String tp : star) {
                construct.append(tp).append(" . ");
            }

            logger.info("Construct query for graph size so far: " + construct);
            count += executeCountQuery("SELECT (COUNT(*) as ?count) WHERE { " + construct + "} ");
            logger.info("Graph size so far: " + count);
        }
        logger.info("count: " + count);
        return count;
    }

    @Override public int mostFrequentResult(MaxFreqQuery maxFreqQuery) {
        try {
            return mostFrequentResultCache.get(maxFreqQuery);
        } catch (ExecutionException ex) {
            java.util.logging.Logger.getLogger(EndpointDataSource.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    @Override public void setMostFreqValueMaps(Map<String, List<TriplePath>> starQueriesMap,
                                               List<List<String>> triplePatterns) throws ExecutionException {

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

                triple += "<" + triplePath.getPredicate().getURI() + "> ";

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

                MaxFreqQuery query = new MaxFreqQuery(Helper.getStarQueryString(starQueryLeft), var);

                if (mapMostFreqValue.containsKey(var)) {
                    List<Integer> mostFreqValues = mapMostFreqValue.get(var);
                    List<StarQuery> mostFreqValuesStar = mapMostFreqValueStar.get(var);

                    if (!mostFreqValues.isEmpty()) {
                        mostFreqValues.add(mostFrequentResultCache.get(query));
                        mapMostFreqValue.put(var, mostFreqValues);

                        mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                        mapMostFreqValueStar.put(var, mostFreqValuesStar);
                    }

                } else {
                    List<Integer> mostFreqValues = new ArrayList<>();
                    logger.info("Query: " + query.getQuery());
                    logger.info("Variable: " + query.getVariableString());

                    mostFreqValues.add(mostFrequentResultCache.get(query));
                    mapMostFreqValue.put(var, mostFreqValues);

                    List<StarQuery> mostFreqValuesStar = new ArrayList<>();

                    mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                    mapMostFreqValueStar.put(var, mostFreqValuesStar);
                }
            }
        }
    }

    @Override public Map<String, List<StarQuery>> getMapMostFreqValueStar() {
        return mapMostFreqValueStar;
    }

    @Override public Map<String, List<Integer>> getMapMostFreqValue() {
        return mapMostFreqValue;
    }

    private int getMostFrequentResult(String starQuery,
            String variableName) {

        variableName = variableName.replace("“", "").replace("”", "");
        String maxFreqQueryString = "SELECT (COUNT(?" + variableName
                + ") AS ?count) WHERE { SELECT ?" + variableName + " WHERE { " + starQuery + "} LIMIT 1000000 " + "} GROUP BY ?"
                + variableName + " " + "ORDER BY ?" + variableName
                + " DESC (?count) LIMIT 1 ";

        logger.info("query at getMostFrequentResult: " + maxFreqQueryString);

        Query query = QueryFactory.create(maxFreqQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, query)) {
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
}
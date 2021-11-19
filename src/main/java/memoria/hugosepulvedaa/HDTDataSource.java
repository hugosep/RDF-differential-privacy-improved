package memoria.hugosepulvedaa;

import memoria.hugosepulvedaa.utils.Helper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class HDTDataSource implements DataSource {

    private static final Logger logger = LogManager.getLogger(HDTDataSource.class.getName());

    final HDT datasource;
    final NodeDictionary dictionary;

    HDTGraph graph;
    Model triples;

    final LoadingCache<MaxFreqQuery, Integer> mostFrequenResultCache;
    final LoadingCache<Query, Long> graphSizeCache;

    private static final Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
    private static final Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

    @Override
    public Map<String, List<Integer>> getMapMostFreqValue() {
        return mapMostFreqValue;
    }

    @Override
    public Map<String, List<StarQuery>> getMapMostFreqValueStar() {
        return mapMostFreqValueStar;
    }

    /**
     * Creates a new HdtDataSource.
     *
     * @param hdtFile the HDT datafile
     * @throws IOException if the file cannot be loaded
     */
    public HDTDataSource(String hdtFile) throws IOException {

        datasource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(datasource.getDictionary());
        graph = new HDTGraph(datasource);
        triples = ModelFactory.createModelForGraph(graph);

        mostFrequenResultCache =
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
                                        logger.debug(
                                                "into mostPopularValueCache CacheLoader, loading: "
                                                        + s);
                                        return getMostFrequentResult(
                                                s.getQuery(), s.getVariableString());
                                    }
                                });

        graphSizeCache =
                CacheBuilder.newBuilder()
                        .recordStats()
                        .maximumWeight(1000)
                        .weigher((Weigher<Query, Long>) (k, resultSize) -> k.toString().length())
                        .build(
                                new CacheLoader<Query, Long>() {
                                    @Override
                                    public Long load(Query q) {
                                        logger.debug(
                                                "into graphSizeCache CacheLoader, loading: " + q);
                                        return getGraphSize(q);
                                    }
                                });
    }

    private int getMostFrequentResult(String starQuery, String variableName) {

        variableName = variableName.replace("“", "").replace("”", "");

        String maxFreqQueryString =
                "select (count(?"
                        + variableName
                        + ") as ?count) where { "
                        + starQuery
                        + " "
                        + "} GROUP BY ?"
                        + variableName
                        + " "
                        + "ORDER BY ?"
                        + variableName
                        + " DESC (?count) LIMIT 1 ";

        Query query = QueryFactory.create(maxFreqQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, triples)) {
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

    @Override
    public long getGraphSize(Query query) {
        try (QueryExecution qexec = QueryExecutionFactory.create(query, triples)) {
            Model results = qexec.execConstruct();
            long resultSize = results.size();
            qexec.close();
            return resultSize;
        }
    }

    @Override
    public int executeCountQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        logger.info("count query: " + queryString);
        /*ResultSet results = executeQuery(query);
        QuerySolution soln = results.nextSolution();
        RDFNode x = soln.get(soln.varNames().next());
        int countResult = x.asLiteral().getInt();
        */

        // INFINITE RECURSION
        int countResult = executeCountQuery(queryString);
        logger.info("count query result (dataset): " + countResult);
        // return countResult;
        return countResult;
    }

    @Override
    public Long getGraphSizeTriples(List<List<String>> triplePatternsCount) {
        long count = 0;

        for (List<String> star : triplePatternsCount) {

            StringBuilder construct = new StringBuilder();

            for (String tp : star) {
                construct.append(tp).append(" . ");
            }

            logger.info("construct query for graph size so far: " + construct);
            count += executeCountQuery("SELECT (COUNT(*) as ?count) WHERE {" + construct + "}");
            logger.info("graph size so far: " + count);
        }
        return count;
    }

    @Override
    public void setMostFreqValueMaps(
            Map<String, List<TriplePath>> starQueriesMap, List<List<String>> triplePatterns)
            throws ExecutionException {

        Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
        Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

        // EDITED
        logger.info("StarQueriesMap:" + starQueriesMap);

        for (String key : starQueriesMap.keySet()) {
            // EDITED
            logger.info("Key:" + key);
            ArrayList<String> listTriple = new ArrayList<>();
            List<TriplePath> starQueryLeft = starQueriesMap.get(key);
            List<String> varStrings = new ArrayList<>();

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
                    triple += "?o" + i + " ";
                }
                i++;
                listTriple.add(triple);
            }
            // EDITED
            logger.info("Triple:" + listTriple);

            triplePatterns.add(listTriple);

            Set<String> listWithoutDuplicates = new LinkedHashSet<>(varStrings);

            varStrings.clear();
            varStrings.addAll(listWithoutDuplicates);

            for (String var : varStrings) {

                MaxFreqQuery query =
                        new MaxFreqQuery(Helper.getStarQueryString(starQueryLeft), var);

                if (mapMostFreqValue.containsKey(var)) {
                    List<Integer> mostFreqValues = mapMostFreqValue.get(var);
                    List<StarQuery> mostFreqValuesStar = mapMostFreqValueStar.get(var);

                    if (!mostFreqValues.isEmpty()) {
                        mostFreqValues.add(this.mostFrequenResultCache.get(query));
                        mapMostFreqValue.put(var, mostFreqValues);

                        mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                        mapMostFreqValueStar.put(var, mostFreqValuesStar);
                    }
                } else {
                    List<Integer> mostFreqValues = new ArrayList<>();
                    mostFreqValues.add(this.mostFrequenResultCache.get(query));
                    mapMostFreqValue.put(var, mostFreqValues);
                    List<StarQuery> mostFreqValuesStar = new ArrayList<>();
                    mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                    mapMostFreqValueStar.put(var, mostFreqValuesStar);
                }
            }
        }
    }

    @Override
    public int mostFrequentResult(MaxFreqQuery maxFreqQuery) {

        try {
            return this.mostFrequenResultCache.get(maxFreqQuery);

        } catch (ExecutionException ex) {
            java.util.logging.Logger.getLogger(HDTDataSource.class.getName())
                    .log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    /*public String mostFrequentResultStats() {
        return mostFrequenResultCache.stats().toString();
    }*/
}

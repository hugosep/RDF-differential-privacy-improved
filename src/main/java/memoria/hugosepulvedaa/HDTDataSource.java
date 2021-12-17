package memoria.hugosepulvedaa;

import com.google.common.cache.*;
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
import java.util.logging.Level;

/** @author cbuil */
public class HDTDataSource implements DataSource {

    private static final Logger logger = LogManager.getLogger(HDTDataSource.class.getName());
    private static LoadingCache<Query, DPQuery> DPQueriesCache;
    private final LoadingCache<MaxFreqQuery, Integer> mostFrequentResultCache;
    private static final Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
    private static final Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();
    private double EPSILON;

    final HDT dataSource;
    final NodeDictionary dictionary;

    HDTGraph graph;
    Model triples;

    public HDTDataSource(String hdtFile, double EPSILON) throws IOException {

        this.EPSILON = EPSILON;

        dataSource = HDTManager.mapIndexedHDT(hdtFile, null);
        dictionary = new NodeDictionary(dataSource.getDictionary());
        graph = new HDTGraph(dataSource);
        triples = ModelFactory.createModelForGraph(graph);

        DPQueriesCache =
                CacheBuilder.newBuilder()
                        .recordStats()
                        .maximumWeight(100000)
                        .weigher(
                                (Weigher<Query, DPQuery>)
                                        (k, resultSize) -> k.getQueryPattern().toString().length())
                        .build(
                                new CacheLoader<Query, DPQuery>() {

                                    @Override
                                    public DPQuery load(Query key) {
                                        logger.info("into DPQueries CacheLoader, loading: " + key);
                                        DPQuery dpQuery = new DPQuery();
                                        dpQuery.setModel(executeConstructQuery(key));
                                        List<List<String>> triplePatterns = new ArrayList<>();

                                        Map<String, List<TriplePath>> starQueriesMap =
                                                Helper.getStarPatterns(key);

                                        setMostFreqValueMaps(
                                                dpQuery.getModel(),
                                                dpQuery.getMostFrequentResults(),
                                                key,
                                                starQueriesMap,
                                                triplePatterns);

                                        long graphSize =
                                                calculateGraphSizeTriples(
                                                        dpQuery.getModel(), triplePatterns);

                                        dpQuery.setGraphSizeTriples(graphSize);

                                        double DELTA = 1 / Math.pow(graphSize, 2);
                                        double beta = EPSILON / (2 * Math.log(2 / DELTA));

                                        String elasticStability = "0";
                                        int k = 0;

                                        Sensitivity smoothSensitivity;

                                        dpQuery.setIsStarQuery(Helper.isStarQuery(key));

                                        if (dpQuery.isStarQuery()) {
                                            elasticStability = "x";

                                            Sensitivity sensitivity =
                                                    new Sensitivity(1.0, elasticStability);

                                            smoothSensitivity =
                                                    GraphElasticSensitivity
                                                            .smoothElasticSensitivityStar(
                                                                    elasticStability,
                                                                    sensitivity,
                                                                    beta,
                                                                    k);

                                            logger.info(
                                                    "Star query (smooth) sensitivity: "
                                                            + smoothSensitivity.getSensitivity());
                                            dpQuery.setElasticStability(elasticStability);

                                        } else {
                                            List<StarQuery> listStars = new ArrayList<>();

                                            for (List<TriplePath> tp : starQueriesMap.values()) {
                                                listStars.add(new StarQuery(tp));
                                            }

                                            StarQuery sq =
                                                    GraphElasticSensitivity.calculateSensitivity(
                                                            HDTDataSource.this,
                                                            dpQuery.getMostFrequentResults(),
                                                            listStars);

                                            logger.info(
                                                    "Elastic Stability: "
                                                            + sq.getElasticStability());

                                            smoothSensitivity =
                                                    GraphElasticSensitivity
                                                            .smoothElasticSensitivity(
                                                                    sq.getElasticStability(),
                                                                    0,
                                                                    beta,
                                                                    k,
                                                                    graphSize);

                                            logger.info(
                                                    "Path Smooth Sensitivity: "
                                                            + smoothSensitivity.getSensitivity());
                                            dpQuery.setElasticStability(sq.getElasticStability());
                                        }
                                        logger.info("SmoothSensitivity:" + smoothSensitivity);
                                        dpQuery.setSmoothSensitivity(smoothSensitivity);

                                        return dpQuery;
                                    }
                                });

        mostFrequentResultCache =
                CacheBuilder.newBuilder()
                        .recordStats()
                        .maximumWeight(100000)
                        .weigher(
                                new Weigher<MaxFreqQuery, Integer>() {
                                    public int weigh(MaxFreqQuery k, Integer resultSize) {
                                        return k.getQuerySize();
                                    }
                                })
                        .build(
                                new CacheLoader<MaxFreqQuery, Integer>() {
                                    @Override
                                    public Integer load(MaxFreqQuery s) throws Exception {
                                        logger.debug(
                                                "into mostPopularValueCache CacheLoader, loading: "
                                                        + s.toString());
                                        return getMostFrequentResult(
                                                s.getQuery(), s.getVariableString());
                                    }
                                });
    }

    public DPQuery getDPQuery(Query key) {
        return DPQueriesCache.getUnchecked(key);
    }

    @Override
    public long getGraphSize(Query query) {
        return (DPQueriesCache.getUnchecked(query).getModel().size());
    }

    public Model executeConstructQuery(Query query) {
        Element queryPattern = query.getQueryPattern();

        String constructQuery = "CONSTRUCT " + queryPattern + " WHERE " + queryPattern;

        logger.info("constructQuery: " + constructQuery);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, triples)) {
            return qexec.execConstruct();
        }
    }

    @Override
    public int executeCountQuery(String queryString, boolean principal) {

        Query query = QueryFactory.create(queryString);

        // no entiendo por que esta esto
        if (queryString.contains("http://www.wikidata.org/prop/direct/P31")
                && (queryString.lastIndexOf('?') != queryString.indexOf('?'))) {
            return 85869721;
        }

        QueryExecution qexec = qexec = QueryExecutionFactory.create(query, triples);

        ResultSet results = qexec.execSelect();
        QuerySolution soln = results.nextSolution();

        logger.info("Count query executed... ");

        qexec.close();

        RDFNode x = soln.get(soln.varNames().next());
        int countResult = x.asLiteral().getInt();

        logger.info("Count query result (endpoint): " + countResult);
        return countResult;
    }

    public int executeCountQueryInternal(Model model, String queryString) {

        Query query = QueryFactory.create(queryString);

        // no entiendo por que esta esto
        if (queryString.contains("http://www.wikidata.org/prop/direct/P31")
                && (queryString.lastIndexOf('?') != queryString.indexOf('?'))) {
            return 85869721;
        }

        QueryExecution qexec = QueryExecutionFactory.create(query, model);

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
    public Long getGraphSizeTriples(Query query) {
        return (DPQueriesCache.getUnchecked(query).getGraphSizeTriples());
    }

    public Long calculateGraphSizeTriples(Model model, List<List<String>> triplePatternsCount) {

        long count = 0L;

        // triplePatternsCount has all triples generated by setMostFreqValueMaps method
        for (List<String> star : triplePatternsCount) {

            StringBuilder construct = new StringBuilder();

            for (String tp : star) {
                construct.append(tp).append(" . ");
            }

            logger.info("Construct query for graph size so far: " + construct);
            count +=
                    executeCountQueryInternal(
                            model, "SELECT (COUNT(*) as ?count) WHERE { " + construct + "} ");
            logger.info("Graph size so far: " + count);
        }
        logger.info("count: " + count);
        return count;
    }

    @Override
    public int mostFrequentResult(MaxFreqQuery maxFreqQuery) {
        try {
            return this.mostFrequentResultCache.get(maxFreqQuery);

        } catch (ExecutionException ex) {
            java.util.logging.Logger.getLogger(HDTDataSource.class.getName())
                    .log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    @Override
    public void setMostFreqValueMaps(
            Model model,
            HashMap<MaxFreqQuery, Integer> mostFrequentResults,
            Query originalQuery,
            Map<String, List<TriplePath>> starQueriesMap,
            List<List<String>> triplePatterns) {

        Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
        Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();

        // EDITED
        logger.info("StarQueriesMap: " + starQueriesMap);
        logger.info("triplePatterns: " + triplePatterns);

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
                        new MaxFreqQuery(Helper.getStarQueryString(starQueryLeft), var);

                if (mapMostFreqValue.containsKey(var)) {
                    List<Integer> mostFreqValues = mapMostFreqValue.get(var);
                    List<StarQuery> mostFreqValuesStar = mapMostFreqValueStar.get(var);

                    if (!mostFreqValues.isEmpty()) {
                        mostFrequentResults.computeIfAbsent(
                                query,
                                k -> getMostFrequentResult(k.getQuery(), k.getVariableString()));

                        mostFreqValues.add(mostFrequentResults.get(query));
                        mapMostFreqValue.put(var, mostFreqValues);

                        mostFreqValuesStar.add(new StarQuery(starQueryLeft));
                        mapMostFreqValueStar.put(var, mostFreqValuesStar);
                    }

                } else {
                    List<Integer> mostFreqValues = new ArrayList<>();
                    mostFrequentResults.computeIfAbsent(
                            query, k -> getMostFrequentResult(k.getQuery(), k.getVariableString()));
                    mostFreqValues.add(mostFrequentResults.get(query));
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

    @Override
    public int getMostFrequentResult(String starQuery, String variableName) {

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
        QueryExecution qexec = qexec = QueryExecutionFactory.create(query, triples);

        /*try {
            Model modelQuery = DPQueriesCache.get(originalQuery).getModel();
            qexec = QueryExecutionFactory.create(query, modelQuery);

        } catch (ExecutionException e) {
            qexec = QueryExecutionFactory.sparqlService(dataSource, query);
        }*/

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

    public CacheStats getDPQueriesCache() {
        return DPQueriesCache.stats();
    }

    public CacheStats getMostFrequentResultCache() {
        return mostFrequentResultCache.stats();
    }
}

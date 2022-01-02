package memoria.hugosepulvedaa;

import com.github.benmanes.caffeine.cache.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import memoria.hugosepulvedaa.utils.Helper;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** @author cbuil */
public class EndpointDataSource implements DataSource {

    private static final Logger logger = LogManager.getLogger(EndpointDataSource.class.getName());
    private final String dataSource;
    private static LoadingCache<Query, DPQuery> DPQueriesCache;
    private final LoadingCache<MaxFreqQuery, Integer> mostFrequentResultCache;
    private static final Map<String, List<Integer>> mapMostFreqValue = new HashMap<>();
    private static final Map<String, List<StarQuery>> mapMostFreqValueStar = new HashMap<>();
    private double EPSILON;

    public EndpointDataSource(String endpoint, double EPSILON) {

        dataSource = endpoint;
        this.EPSILON = EPSILON;

        DPQueriesCache =
                Caffeine.newBuilder()
                        .recordStats()
                        .maximumWeight(120)
                        .weigher(
                                (Weigher<Query, DPQuery>)
                                        (k, v) -> (int) (v.getExecutionTime() / 1E9))
                        .expireAfterAccess(120, TimeUnit.MINUTES)
                        .build(
                                key -> {
                                    logger.info("into DPQueries CacheLoader, loading: " + key);

                                    DPQuery dpQuery = new DPQuery();

                                    long startTime = System.nanoTime();

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
                                    dpQuery.setDelta(DELTA);
                                    dpQuery.setBeta(beta);

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
                                                        EndpointDataSource.this,
                                                        dpQuery.getMostFrequentResults(),
                                                        listStars);

                                        logger.info(
                                                "Elastic Stability: " + sq.getElasticStability());

                                        smoothSensitivity =
                                                GraphElasticSensitivity.smoothElasticSensitivity(
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

                                    // stopTime
                                    long endTime = System.nanoTime();
                                    long duration = (endTime - startTime);
                                    double durationInSeconds = (double) (duration / 1000000000);
                                    logger.info("Time: " + durationInSeconds + " seconds");
                                    logger.info("Time: " + duration + " nanoseconds");
                                    dpQuery.setExecutionTime(durationInSeconds);
                                    return dpQuery;
                                });

        mostFrequentResultCache =
                Caffeine.newBuilder()
                        .recordStats()
                        .maximumWeight(10000)
                        .weigher(
                                (Weigher<MaxFreqQuery, Integer>)
                                        (k, resultSize) -> k.getNumberOfVariables())
                        .expireAfterAccess(120, TimeUnit.MINUTES)
                        .build(
                                maxFreqQuery -> {
                                    logger.debug(
                                            "into mostPopularValueCache CacheLoader, loading: "
                                                    + maxFreqQuery);
                                    return getMostFrequentResult(
                                            maxFreqQuery.getQuery(),
                                            maxFreqQuery.getVariableString());
                                });
    }

    public DPQuery getDPQuery(Query key) {
        return DPQueriesCache.get(key);
    }

    @Override
    public long getGraphSize(Query query) {
        return (DPQueriesCache.get(query).getModel().size());
    }

    public Model executeConstructQuery(Query query) {
        Element queryPattern = query.getQueryPattern();

        String cleanConstructQuery =
                queryPattern.toString().replaceAll(".\n *(FILTER *(.*) *)", ".");
        cleanConstructQuery = cleanConstructQuery.replaceAll("(FILTER *(.*) *)", "");

        String constructQuery = "CONSTRUCT " + cleanConstructQuery + " WHERE " + queryPattern;

        logger.info("constructQuery: " + constructQuery);

        try (QueryExecution qexec =
                QueryExecutionFactory.sparqlService(dataSource, constructQuery)) {
            Model model = qexec.execConstruct();
            qexec.close();
            return model;
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

        QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, query);

        ResultSet results = qexec.execSelect();
        QuerySolution soln = results.nextSolution();
        logger.info("Count query executed... ");

        RDFNode x = soln.get(soln.varNames().next());
        int countResult = x.asLiteral().getInt();
        qexec.close();
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
        return (DPQueriesCache.get(query).getGraphSizeTriples());
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
        return this.mostFrequentResultCache.get(maxFreqQuery);
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
        QueryExecution qexec = QueryExecutionFactory.sparqlService(dataSource, query);

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
            qexec.close();
            return res;
        } else {
            qexec.close();
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

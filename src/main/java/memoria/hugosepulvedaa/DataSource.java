/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.TriplePath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** @author cbuil */
public interface DataSource {

    int mostFrequentResult(Query originalQuery, MaxFreqQuery maxFreqQuery);

    long getGraphSize(Query query);

    int executeCountQuery(String queryString, boolean principal);

    Long getGraphSizeTriples(List<List<String>> triplePatternsCount);

    void setMostFreqValueMaps(Query originalQuery,
            Map<String, List<TriplePath>> starQueriesMap, List<List<String>> triplePatterns)
            throws ExecutionException;

    Map<String, List<StarQuery>> getMapMostFreqValueStar();

    Map<String, List<Integer>> getMapMostFreqValue();
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.TriplePath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** @author cbuil */
public interface DataSource {

    DPQuery getDPQuery(Query query);

    long getGraphSize();

    int executeCountQuery(String queryString, boolean principal);

    Long getGraphSizeTriples();

    int getMostFrequentResult(String starQuery, String variableName);

    int mostFrequentResult(MaxFreqQuery maxFreqQuery);

    void setMostFreqValueMaps(
            HashMap<MaxFreqQuery, Integer> mostFrequentResults,
            Query originalQuery,
            Map<String, List<TriplePath>> starQueriesMap,
            List<List<String>> triplePatterns)
            throws ExecutionException;

    Map<String, List<StarQuery>> getMapMostFreqValueStar();

    Map<String, List<Integer>> getMapMostFreqValue();

    CacheStats getDPQueriesCache();

    CacheStats getMostFrequentResultCache();
}

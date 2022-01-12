/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.jena.query.Query;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** @author cbuil */
public interface DataSource {

    DPQuery getDPQuery(Query query);

    int executeCountQuery(String queryString, boolean principal);

    int getMostFrequentResult(String starQuery, String variableName);

    int mostFrequentResult(MaxFreqQuery maxFreqQuery);

    void setMostFreqValueMaps(DPQuery dpQuery) throws ExecutionException;

    Map<String, List<StarQuery>> getMapMostFreqValuesStar(Query query);

    Map<String, List<Integer>> getMapMostFreqValues(Query query);

    CacheStats getDPQueriesCache();

    CacheStats getMostFrequentResultCache();
}

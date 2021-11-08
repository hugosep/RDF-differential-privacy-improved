/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa;

import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.TriplePath;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author cbuil
 * @description
 * @last_editor
 */

public interface DataSource {

    int mostFrequentResult(MaxFreqQuery maxFreqQuery);

    ResultSet executeQuery(Query query);

    long getGraphSize(Query query);

    int executeCountQuery(String queryString);

    long getGraphSizeTriples(List<List<String>> triplePatternsCount);

    void setMostFreqValueMaps(
            Map<String, List<TriplePath>> starQueriesMap,
            List<List<String>> triplePatterns)
             throws ExecutionException;

    Map<String, List<StarQuery>> getMapMostFreqValueStar();

    Map<String, List<Integer>> getMapMostFreqValue();
}
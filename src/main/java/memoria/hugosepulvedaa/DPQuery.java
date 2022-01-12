package memoria.hugosepulvedaa;

import org.apache.jena.sparql.core.TriplePath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DPQuery {
    private Long graphSizeTriples;
    private boolean isStarQuery;
    private HashMap<MaxFreqQuery, Integer> mostFrequentResults;
    private HashMap<String, List<Integer>> mapMostFreqValues;
    private HashMap<String, List<StarQuery>> mapMostFreqValuesStar;
    private double executionTime;
    private List<StarQuery> listStars;
    private StarQuery starQuery;

    DPQuery() {
        this.mostFrequentResults = new HashMap<>();
        this.mapMostFreqValues = new HashMap<>();
        this.mapMostFreqValuesStar = new HashMap<>();
    }

    public Map<String, List<Integer>> getMapMostFreqValues() {
        return mapMostFreqValues;
    }

    public Map<String, List<StarQuery>> getMapMostFreqValuesStar() {
        return mapMostFreqValuesStar;
    }

    public Map<String, List<TriplePath>> getStarQueriesMap() {
        return starQueriesMap;
    }

    public void setStarQueriesMap(Map<String, List<TriplePath>> starQueriesMap) {
        this.starQueriesMap = starQueriesMap;
    }

    public List<List<String>> getTriplePatterns() {
        return triplePatterns;
    }

    public void setTriplePatterns(List<List<String>> triplePatterns) {
        this.triplePatterns = triplePatterns;
    }

    private Map<String, List<TriplePath>> starQueriesMap;
    private List<List<String>> triplePatterns;

    public StarQuery getStarQuery() {
        return starQuery;
    }

    public void setStarQuery(StarQuery starQuery) {
        this.starQuery = starQuery;
    }

    public List<StarQuery> getListStars() {
        return listStars;
    }

    public void setListStars(List<StarQuery> listStars) {
        this.listStars = listStars;
    }

    public double getExecutionTime() {
        return this.executionTime;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    public HashMap<MaxFreqQuery, Integer> getMostFrequentResults() {
        return mostFrequentResults;
    }

    public boolean isStarQuery() {
        return isStarQuery;
    }

    public Long getGraphSizeTriples() {
        return this.graphSizeTriples;
    }

    public void setGraphSizeTriples(Long graphSizeTriples) {
        this.graphSizeTriples = graphSizeTriples;
    }

    public void setIsStarQuery(boolean isStarQuery) {
        this.isStarQuery = isStarQuery;
    }
}

package memoria.hugosepulvedaa;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.TriplePath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DPQuery {
    private Model model;
    private Long graphSizeTriples;
    private boolean isStarQuery;
    private double EPSILON;
    private String elasticStability;
    private Sensitivity smoothSensitivity;
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

    public void setStarQuery(boolean starQuery) {
        isStarQuery = starQuery;
    }

    public void setMostFrequentResults(HashMap<MaxFreqQuery, Integer> mostFrequentResults) {
        this.mostFrequentResults = mostFrequentResults;
    }

    public Map<String, List<Integer>> getMapMostFreqValues() {
        return mapMostFreqValues;
    }

    public void setMapMostFreqValues(HashMap<String, List<Integer>> mapMostFreqValues) {
        this.mapMostFreqValues = mapMostFreqValues;
    }

    public Map<String, List<StarQuery>> getMapMostFreqValuesStar() {
        return mapMostFreqValuesStar;
    }

    public void setMapMostFreqValuesStar(HashMap<String, List<StarQuery>> mapMostFreqValuesStar) {
        this.mapMostFreqValuesStar = mapMostFreqValuesStar;
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

    public Integer getMostFrequentResult(MaxFreqQuery maxFreqQuery) {
        return mostFrequentResults.get(maxFreqQuery);
    }

    public Sensitivity getSmoothSensitivity() {
        return smoothSensitivity;
    }

    public boolean isStarQuery() {
        return isStarQuery;
    }

    public Model getModel() {
        return this.model;
    }

    public Long getGraphSizeTriples() {
        return this.graphSizeTriples;
    }

    public String getElasticStability() {
        return this.elasticStability;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public void setGraphSizeTriples(Long graphSizeTriples) {
        this.graphSizeTriples = graphSizeTriples;
    }

    public void setIsStarQuery(boolean isStarQuery) {
        this.isStarQuery = isStarQuery;
    }

    public void setSmoothSensitivity(Sensitivity smoothSensitivity) {
        this.smoothSensitivity = smoothSensitivity;
    }

    public void setElasticStability(String elasticStability) {
        this.elasticStability = elasticStability;
    }
}

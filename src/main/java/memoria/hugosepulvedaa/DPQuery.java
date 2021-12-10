package memoria.hugosepulvedaa;

import org.apache.jena.rdf.model.Model;

import java.util.HashMap;

public class DPQuery {
    private Model model;
    private Long graphSizeTriples;
    private boolean isStarQuery;
    private double EPSILON;
    private double beta;
    private double delta;
    private String elasticStability;
    private Sensitivity smoothSensitivity;
    private HashMap<MaxFreqQuery, Integer> mostFrequentResults;
    private double executionTime;

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

    DPQuery() {
        mostFrequentResults = new HashMap<>();
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

    public double getEPSILON() {
        return this.EPSILON;
    }

    public double getBeta() {
        return this.beta;
    }

    public double getDelta() {
        return this.delta;
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

    public void setEPSILON(double EPSILON) {
        this.EPSILON = EPSILON;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }
}

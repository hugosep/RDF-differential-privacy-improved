package memoria.hugosepulvedaa;

import org.apache.jena.rdf.model.Model;

import java.util.HashMap;

public class DPQuery {
    private Long graphSizeTriples;
    private boolean isStarQuery;
    private String elasticStability;
    private Sensitivity smoothSensitivity;
    private HashMap<MaxFreqQuery, Integer> mostFrequentResults;
    private double executionTime;

    public double getDelta() {
        return DELTA;
    }

    public void setDelta(double DELTA) {
        this.DELTA = DELTA;
    }

    private double DELTA;

    public double getExecutionTime() {
        return this.executionTime;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    public HashMap<MaxFreqQuery, Integer> getMostFrequentResults() {
        return mostFrequentResults;
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

    public Long getGraphSizeTriples() {
        return this.graphSizeTriples;
    }

    public String getElasticStability() {
        return this.elasticStability;
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

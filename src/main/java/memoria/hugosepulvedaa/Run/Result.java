package memoria.hugosepulvedaa.Run;

import memoria.hugosepulvedaa.StarQuery;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Map;

public class Result {

    private String query;
    private double epsilon;
    private double delta;
    private List<Double> privateResult;
    private double sensitivity;
    private List<Integer> result;
    private Map<String, List<Integer>> mapMostFreqValue;
    private Map<String, List<StarQuery>> mapMostFreqValueStar;
    private int maxK;
    private double scale;
    private String elasticStability;
    private long graphSize;
    private boolean starQuery;

    public Result(
            String query,
            double epsilon,
            double delta,
            List<Double> resultList,
            double sensitivity,
            List<Integer> result,
            int maxK,
            double scale,
            String elasticStability,
            long graphSize,
            boolean starQuery,
            Map<String, List<Integer>> mapMostFreqValue,
            Map<String, List<StarQuery>> mapMostFreqValueStar) {

        this.query = query;
        this.epsilon = epsilon;
        this.delta = delta;
        this.privateResult = resultList;
        this.sensitivity = sensitivity;
        this.result = result;
        this.maxK = maxK;
        this.scale = scale;
        this.elasticStability = elasticStability;
        this.graphSize = graphSize;
        this.starQuery = starQuery;
        this.mapMostFreqValue = mapMostFreqValue;
        this.mapMostFreqValueStar = mapMostFreqValueStar;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}

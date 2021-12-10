package memoria.hugosepulvedaa;

import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.core.TriplePath;

import java.util.ArrayList;
import java.util.List;

public class StarQuery implements Comparable<StarQuery> {

    private final List<TriplePath> triples;

    // the smoothed sensitivity of the star query
    // private Sensitivity querySensitivity;

    // elastic stability is the formula by which we calculate the sensitivity,
    // only appears when there are more than two star queries
    private String elasticStability;

    private String mpValue;

    public StarQuery(List<TriplePath> triples) {
        this.triples = triples;
    }

    public StarQuery() {
        this.triples = new ArrayList<>();
    }

    public void addStarQuery(List<TriplePath> triples) {
        this.triples.addAll(triples);
    }

    public List<String> getVariables() {

        List<String> variables = new ArrayList<>();

        for (TriplePath triplePath : triples) {

            if (triplePath.getSubject().isVariable()) {

                if (!variables.contains(triplePath.getSubject().getName())) {
                    variables.add(triplePath.getSubject().getName());
                }
            }

            if (triplePath.getPredicate().isVariable()) {

                if (!variables.contains(triplePath.getPredicate().getName())) {
                    variables.add(triplePath.getPredicate().getName());
                }
            }

            if (triplePath.getObject().isVariable()) {

                if (!variables.contains(triplePath.getObject().getName())) {
                    variables.add(triplePath.getObject().getName());
                }
            }
        }
        return variables;
    }

    public String toString() {

        StringBuilder result = new StringBuilder();

        for (TriplePath triplePath : triples) {

            String subject = "";

            if (triplePath.asTriple().getMatchSubject() instanceof Node_URI) {
                subject = "<" + triplePath.asTriple().getMatchSubject().getURI() + ">";

            } else if (triplePath.asTriple().getMatchSubject() instanceof Node_Variable) {
                subject = "?" + triplePath.asTriple().getMatchSubject().getName();
            }

            String pred = "";

            if (triplePath.asTriple().getMatchPredicate() instanceof Node_URI) {

                pred = "<" + triplePath.asTriple().getMatchPredicate().getURI() + ">";
            } else if (triplePath.asTriple().getMatchPredicate() instanceof Node_Variable) {
                pred = "?" + triplePath.asTriple().getMatchPredicate().getName();
            }

            String object = "";

            if (triplePath.asTriple().getMatchObject() instanceof Node_URI) {
                object = "<" + triplePath.asTriple().getMatchObject().getURI() + ">";
            } else if (triplePath.asTriple().getMatchObject() instanceof Node_Variable) {
                object = "?" + triplePath.asTriple().getMatchObject().getName();
            }

            String triple = String.format("%s %s %s . \n", subject, pred, object);
            result.append(triple);
        }

        return result.toString();
    }

    public List<TriplePath> getTriples() {
        return triples;
    }

    public String getElasticStability() {
        return elasticStability;
    }

    public void setElasticStability(String elasticStability) {
        this.elasticStability = elasticStability;
    }

    public String getMostPopularValue() {
        return mpValue;
    }

    public void setMostPopularValue(String mpValue) {
        this.mpValue = mpValue;
    }

    @Override
    public int compareTo(StarQuery o) {
        List<String> joinVariables = o.getVariables();
        joinVariables.retainAll(this.getVariables());
        return joinVariables.size();
    }
}

import java.util.ArrayList;

public class Polynomial {

    private String variable;
    private ArrayList<String> binomials;

    public Polynomial(String variable) {
        this.variable = variable;
        binomials = new ArrayList<String>();
    }

    public Polynomial(String variable, String initialBinomial) {
        this(variable);
        binomials.add(initialBinomial);
    }

    public void addBinomial(String binomial) {
        binomials.add(binomial);
    }

    @Override public String toString() {
        StringBuilder strElements = new StringBuilder();

        for(String binomial : binomials) {
            strElements.append(binomial);
            strElements.append("*");
        }

        return strElements.toString();
    }
}
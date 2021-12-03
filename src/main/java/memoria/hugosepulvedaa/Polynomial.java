package memoria.hugosepulvedaa;

import java.util.ArrayList;

public class Polynomial {

    private String variable;
    private ArrayList<String> binomials;

    public Polynomial(String variable) {
        this.variable = variable;
        binomials = new ArrayList<>();
    }

    public Polynomial(String variable, String initialBinomial) {
        this(variable);
        binomials.add(initialBinomial);
    }

    public static String createBinomial(String variable, String constant) {
        return "(" + variable + "+" + constant + ")";
    }

    public void addBinomial(String binomial) {
        binomials.add(binomial);
    }

    @Override public String toString() {
        return String.join("*", binomials);
    }
}
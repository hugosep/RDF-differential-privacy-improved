package memoria.hugosepulvedaa;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaxFreqQuery {
    private final String query;
    private final String variable;
    private int numberOfVariables;

    public MaxFreqQuery(String tp, String var) {
        this.query = tp;
        this.variable = var;
        this.numberOfVariables = numberOfVariables(query);
    }

    public int getQuerySize() {
        return query.length();
    }

    public String getQuery() {
        return query;
    }

    public String getVariableString() {
        return variable;
    }

    @Override
    public String toString() {
        return query;
    }

    /*public boolean equals(MaxFreqQuery another) {
        if (this == another) return true;
        if (another == null || getClass() != another.getClass()) return false;

        return another.getVariableString().equals(variable);
    }*/

    private int numberOfVariables(String strQuery) {
        System.out.println("aaa: " + strQuery);
        Pattern variable = Pattern.compile("\\?\\w+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = variable.matcher(strQuery);
        HashSet<String> variables = new HashSet<>();

        while (matcher.find()) {
            variables.add(matcher.group());
        }

        return (variables.size());
    }

    public int getNumberOfVariables() {
        return this.numberOfVariables;
    }
}

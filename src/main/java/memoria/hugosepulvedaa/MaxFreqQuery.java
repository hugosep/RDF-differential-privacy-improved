package memoria.hugosepulvedaa;

import org.apache.jena.query.Query;

public class MaxFreqQuery {
    private final Query originalQuery;
    private final String query;
    private final String variable;

    public MaxFreqQuery(Query originalQuery, String tp, String var) {
        this.originalQuery = originalQuery;
        query = tp;
        variable = var;
    }

    public Query getOriginalQuery() {
        return originalQuery;
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
}

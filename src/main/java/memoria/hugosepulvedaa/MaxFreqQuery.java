package memoria.hugosepulvedaa;

public class MaxFreqQuery {
    private final String query;
    private final String variable;

    public MaxFreqQuery(String tp, String var) {
        query = tp;
        variable = var;
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
    public String toString(){
        return query;
    }
            
}
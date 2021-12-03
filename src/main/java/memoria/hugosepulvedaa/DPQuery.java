package memoria.hugosepulvedaa;

import org.apache.jena.rdf.model.Model;

import java.util.List;

class DPQuery {
    private Model model;
    private List<String> variables;
    private MaxFreqQuery maxFreqQuery;

    DPQuery(Model model) {
        this.model = model;
    }
}

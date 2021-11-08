package memoria.hugosepulvedaa.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import java.util.*;

public class Helper {

    public static List<String> getJoinVariables(List<TriplePath> starQueryLeft,
            List<TriplePath> starQueryRight) {

        List<String> rightVariables = new ArrayList<String>();
        List<String> joinVariables = new ArrayList<String>();

        for (TriplePath triplePath : starQueryRight) {

            if (triplePath.getSubject().isVariable()) {
                rightVariables.add(triplePath.getSubject().getName());
            }
        }

        for (TriplePath triplePath : starQueryLeft) {

            if (triplePath.getSubject().isVariable()) {

                if (rightVariables.contains(triplePath.getSubject().getName())) {
                    joinVariables.add(triplePath.getSubject().getName());
                }
            }
        }
        return joinVariables;
    }

    /* @description create a map where:
     *  key = variable
     *  value = list of all triples that contains the variable as subject
     */
    public static Map<String, List<TriplePath>> getStarPatterns(Query query) {

        List<Element> elements = ((ElementGroup)query.getQueryPattern()).getElements();
        List<TriplePath> tripleList;
        Map<String, List<TriplePath>> starMap = new HashMap<>();

        ElementPathBlock element = (ElementPathBlock) elements.get(0);
        List<TriplePath> triplePath = element.getPattern().getList();
        Node tempNode;
        String varName = "";
        /*
         * triplePath contains all triples of the query
         *
         * for each triple, create a new (key, value), where:
         *   key = variable
         *   value = list of all triples that contains the variable as subject
         */

        for (TriplePath tripleInQuery : triplePath) {

            tempNode = tripleInQuery.getSubject();

            if (tempNode.isVariable()) {
                varName = tempNode.getName();
            } else if (tempNode.isURI()) {
                varName = tempNode.getURI();
            }

            if (!starMap.containsKey(varName)) {
                tripleList = new ArrayList<>();
            } else {
                tripleList = starMap.get(tempNode.getName());
            }

            tripleList.add(tripleInQuery);
            starMap.put(tempNode.getName(), tripleList);
        }

        return starMap;
    }

    /*
     * checks for a star query of the form ?s a ?p1 . ?s b ?p2
     */
    public static boolean isStarQuery(Query query) {

        List<Element> elements = ((ElementGroup)query.getQueryPattern()).getElements();

        ElementPathBlock element = (ElementPathBlock) elements.get(0);
        List<TriplePath> triplePath = element.getPattern().getList();

        String starQueryVariable = "";

        for (TriplePath tripleInQuery : triplePath) {

            if (tripleInQuery.getSubject().isVariable()) {

                if (starQueryVariable.compareTo("") == 0) {
                    starQueryVariable = tripleInQuery.getSubject().getName();
                } else {
                    if (starQueryVariable.compareTo(tripleInQuery.getSubject().getName()) != 0) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static String getStarQueryString(List<TriplePath> starQuery) {

        StringBuffer result = new StringBuffer();

        for (TriplePath triplePath : starQuery) {
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
            result.append(subject);
            result.append(" ");
            result.append(pred);
            result.append(" ");
            result.append(object);
            result.append(" . \n");
        }
        return result.toString();
    }

    public static List<String> triplePartExtractor(TriplePath triplePath) {
        List<String> result = new ArrayList<>();
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

        result.add(subject);
        result.add(pred);
        result.add(object);

        return result;
    }

    public static String tripleFixer(TriplePath triplePath) {

        List<String> aux = triplePartExtractor(triplePath);
        String result = aux.get(0) + " " + aux.get(1) + " " + aux.get(2);

        return result;
    }

    public static int getMaxFreq(HashMap<String, Integer> map) {

        String highestMap;
        int mostFreqValue = 0;

        for (Map.Entry<String, Integer> entry : map.entrySet()) {

            if (entry.getValue() > mostFreqValue) {
                highestMap = entry.getKey();
                mostFreqValue = entry.getValue();
            }
        }
        return mostFreqValue;
    }

    public static boolean extractor(TriplePath triplePath, HashSet<String> ancestors) {

        List<String> aux = triplePartExtractor(triplePath);
        String subject = aux.get(0);
        String object = aux.get(2);

        if (ancestors.contains(subject) || ancestors.contains(object)) {
            ancestors.add(subject);
            ancestors.add(object);
            return true;

        } else {
            ancestors.add(subject);
            ancestors.add(object);
            return false;
        }
    }
}

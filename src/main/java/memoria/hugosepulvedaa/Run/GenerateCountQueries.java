package memoria.hugosepulvedaa.Run;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GenerateCountQueries {
    
    private static final Logger logger = LogManager
            .getLogger(GenerateCountQueries.class.getName());

//%SELECT DISTINCT ?var1  ?var1Label  ?var2  ?var2Label  ?var3  ?var4 WHERE {  ?var2 ( <http://www.wikidata.org/prop/direct/P31> / <http://www.wikidata.org/prop/direct/P279> *) <http://www.wikidata.org/entity/Q515> .  ?var1  <http://www.wikidata.org/prop/direct/P31>  <http://www.wikidata.org/entity/Q644371> .  ?var1  ?var5  ?var2 .  ?var1  <http://www.wikidata.org/prop/direct/P238>  ?var3 . SERVICE  <http://wikiba.se/ontology#around>   {    ?var2  <http://www.wikidata.org/prop/direct/P625>  ?var6 .    <http://www.bigdata.com/rdf#serviceParam>  <http://wikiba.se/ontology#center>  "POINT(-90 30)"^^<http://www.opengis.net/ont/geosparql#wktLiteral> .    <http://www.bigdata.com/rdf#serviceParam>  <http://wikiba.se/ontology#radius>  "200".    <http://www.bigdata.com/rdf#serviceParam>  <http://wikiba.se/ontology#distance>  ?var7 .  } SERVICE  <http://wikiba.se/ontology#label>   {    <http://www.bigdata.com/rdf#serviceParam>  <http://wikiba.se/ontology#language>  "en".  } OPTIONAL {  ?var2  <http://www.wikidata.org/prop/direct/P625>  ?var4 . }}ORDER BY ASC( ?var7 )LIMIT 1

    public static void main(String[] args) {

        Options options = new Options();

        options.addOption("f", "queryFile", true, "input SPARQL query file");
        options.addOption("o", "dir", true, "output directory");

        CommandLineParser parser = new DefaultParser();

        try {
            String queryString;
            String queryFile = "";
            String outputDir = "";

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
                queryFile = cmd.getOptionValue("f");
            } else {
                logger.info("Missing SPARQL query file.");
            }

            if (cmd.hasOption("o")) {
                outputDir = cmd.getOptionValue("o");
            } else {
                logger.info("Missing query directory.");
            }

            Path queryLocation = Paths.get(queryFile);

            if (Files.isRegularFile(queryLocation)) {
                queryString = new Scanner(new File(queryFile))
                        .useDelimiter("\\Z")
                        .next();
                System.out.println("queryString: " + queryString);

                runQueryGeneration(queryString, outputDir, queryString);

            } else if (Files.isDirectory(queryLocation)) {
                System.out.println("not queryString");

                Iterator<Path> filesPath = Files
                        .list(Paths.get(queryFile))
                        .filter(p -> p
                                .toString()
                                .endsWith(".rq"))
                        .iterator();

                while (filesPath.hasNext()) {

                    Path nextQuery = filesPath.next();
                    queryString = new Scanner(nextQuery)
                            .useDelimiter("\\Z")
                            .next();

                    logger.info("Next query path: " + nextQuery);
                    runQueryGeneration(queryString, outputDir, nextQuery.toString());
                }
            }

        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }

    private static boolean findTriple(Triple triple, LinkedList<Triple> linkedList) {

        for (Triple innerTriple : linkedList) {

            if (varsIn(innerTriple, triple)) {
                return true;
            }
        }
        return false;
    }

    private static boolean varsIn(Triple tripleKey, Triple triple) {
        ArrayList<String> vars = new ArrayList<String>();

        if (tripleKey.getSubject().isVariable()) {
            vars.add(tripleKey.getSubject().getName());
        }

        if (tripleKey.getObject().isVariable()) {
            vars.add(tripleKey.getObject().getName());
        }

        if (tripleKey.getPredicate().isVariable()) {
            vars.add(tripleKey.getPredicate().getName());
        }

        if (triple.getSubject().isVariable()) {
            if (vars.contains(triple.getSubject().getName()))
                return true;
        }

        if (triple.getObject().isVariable()) {
            if (vars.contains(triple.getObject().getName())) {
                return true;
            }
        }

        if (triple.getPredicate().isVariable()) {
            return vars.contains(triple.getPredicate().getName());
        }

        return false;
    }

    private static void runQueryGeneration(String queryString, String outputDir,
            String queryFile) throws IOException {

        ArrayList<LinkedList<Triple>> relatedTriplesList = new ArrayList<LinkedList<Triple>>();
        Query q = QueryFactory.create(queryString);
        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        LinkedList<Triple> triplesAdjList;

        for (Element element : elementList) {

            System.out.println("element:" + element);

            if (element instanceof ElementPathBlock) {

                ElementPathBlock triplesBlock = (ElementPathBlock) element;

                for (TriplePath triplePath : triplesBlock.getPattern()) {
                    Triple triple = triplePath.asTriple();

                    if (relatedTriplesList.isEmpty()) {

                        triplesAdjList = new LinkedList<Triple>();
                        triplesAdjList.add(triple);
                        relatedTriplesList.add(triplesAdjList);

                    } else {
                        boolean isIn = false;

                        for (LinkedList<Triple> tripleList : relatedTriplesList) {

                            if (varsIn(tripleList.element(), triple)) {

                                tripleList.add(triple);
                                isIn = true;
                                break;
                            }
                        }

                        if (!isIn) {
                            triplesAdjList = new LinkedList<Triple>();
                            triplesAdjList.add(triple);
                            relatedTriplesList.add(triplesAdjList);
                        }
                    }
                }
            }
        }

        int i = 0;

        while (i + 1 < relatedTriplesList.size()) {

            LinkedList<Triple> tripleList = relatedTriplesList.get(i);
            int j = 0;

            while (j < tripleList.size()) {

                Triple triple = tripleList.getFirst();
                tripleList.removeFirst();

                if (i + 1 < relatedTriplesList.size()) {

                    if (findTriple(triple, relatedTriplesList.get(i + 1))) {
                        tripleList.add(triple);
                        break;

                    } else {
                        tripleList.add(triple);
                    }
                }
                j++;
            }
            i++;
        }

        for (Var var : q.getProjectVars()) {

            String varStr = var.getName();
            StringBuffer newQueryString = new StringBuffer("SELECT (COUNT(?"
                    + varStr + ") as " + "?count_" + varStr + ") WHERE {\n");

            for (LinkedList<Triple> tripleList : relatedTriplesList) {

                for (Triple triple : tripleList) {

                    String subject;
                    String predicate;
                    String object;

                    if (triple.getSubject().isURI()) {
                        subject = "<" + triple.getSubject() + ">";
                    } else {
                        subject = triple.getSubject().toString();
                    }

                    if (triple.getPredicate().isURI()) {
                        predicate = "<" + triple.getPredicate() + ">";
                    } else {
                        predicate = triple.getPredicate().toString();
                    }

                    if (triple.getObject().isURI()) {
                        object = "<" + triple.getObject() + ">";
                    } else {
                        object = triple.getObject().toString();
                    }

                    String newTriple = String.format("%s %s %s . \n", subject, predicate, object);

                    newQueryString.append(newTriple);
                }
            }
            newQueryString.append("}");

            /*Files.write(
                    Paths.get(outputDir + queryFile.replaceAll(".rq", "") + "."
                            + var.getName() + ".rq"),
                    newQueryString.toString().getBytes());*/
            Files.write(
                    Paths.get(outputDir + var.getName() + ".rq"),
                    newQueryString.toString().getBytes());

            logger.info(newQueryString.toString());
        }
    }
}

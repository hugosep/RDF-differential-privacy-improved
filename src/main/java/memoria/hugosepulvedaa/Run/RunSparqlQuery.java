/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa.Run;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Callable;

/** @author cbuil */
public class RunSparqlQuery implements Callable<String> {

    Model triples;
    String queryString;
    String queryFileName;

    private static final Logger logger = LogManager.getLogger(RunSparqlQuery.class.getName());

    RunSparqlQuery(Model dataSource, String queryString, String queryFileName) {
        this.triples = dataSource;
        this.queryString = queryString;
        this.queryFileName = queryFileName;
    }

    @Override
    public String call() {
        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, triples)) {

            ResultSet results = qexec.execSelect();
            results = ResultSetFactory.copyResults(results);
            qexec.close();
            QuerySolution soln = results.nextSolution();
            RDFNode x = soln.get(soln.varNames().next());
            int countResult = x.asLiteral().getInt();
            logger.info("count query result (dataset): " + countResult);
            return queryFileName + '\t' + countResult;
        } catch (Exception e) {
            System.out.println("Exception is caught:" + e.getMessage());
        }
        return queryFileName + "\t-1";
    }
}

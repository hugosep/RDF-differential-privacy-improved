/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoria.hugosepulvedaa.Run;

import org.apache.commons.cli.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.rdfhdt.hdtjena.NodeDictionary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** @author cbuil */
public class RunQueriesParallel {

    private static final Logger logger =
            LogManager.getLogger(memoria.hugosepulvedaa.Run.RunQueriesParallel.class.getName());

    private static String queryFilesDir = "";
    private static String dataFile = "";
    private static String outputFile = "";
    private static String cores = "";
    private static boolean evaluation = false;

    private static HDT datasource;
    private static NodeDictionary dictionary;
    private static HDTGraph graph;
    private static Model triples;

    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException {
        parseInput(args);

        List<String> queryFiles;

        try (Stream<Path> paths = Files.walk(Paths.get(queryFilesDir))) {
            queryFiles =
                    paths.filter(Files::isRegularFile)
                            .map(Path::toString)
                            .collect(Collectors.toList());
        }

        datasource = HDTManager.mapIndexedHDT(dataFile, null);
        dictionary = new NodeDictionary(datasource.getDictionary());
        graph = new HDTGraph(datasource);
        triples = ModelFactory.createModelForGraph(graph);
        ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(cores));

        ArrayList<RunSparqlQuery> tasks = new ArrayList();

        for (String queryFile : queryFiles) {
            String queryString = new Scanner(new File(queryFile)).useDelimiter("\\Z").next();
            tasks.add(new RunSparqlQuery(triples, queryString, queryFile));
        }

        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.info("error timeout");
        }

        StringBuilder resultsBuffer = new StringBuilder();

        for (Future<String> future : futures) {
            resultsBuffer.append(future.get());
            resultsBuffer.append("\n");
        }

        Files.write(
                Paths.get(outputFile),
                resultsBuffer.toString().getBytes(),
                StandardOpenOption.APPEND);
    }

    private static void parseInput(String[] args) throws IOException {
        // create Options object
        Options options = new Options();

        options.addOption("f", "qFile", true, "input SPARQL query file");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("c", "cores", true, "cores");
        options.addOption("o", "output", true, "output file");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
                queryFilesDir = cmd.getOptionValue("f");
                //                queryString = new Scanner(new File(queryFile))
                //                        .useDelimiter("\\Z").next();
                // transform into Jena Query object
            } else {
                logger.info("Missing SPARQL query file");
            }

            if (cmd.hasOption("d")) {
                dataFile = cmd.getOptionValue("d");
            } else {
                logger.info("Missing data file");
            }

            if (cmd.hasOption("c")) {
                cores = cmd.getOptionValue("c");
            } else {
                logger.info("Missing number of cores");
            }

            if (cmd.hasOption("o")) {
                outputFile = cmd.getOptionValue("o");

                if (!Files.exists(Paths.get(outputFile))) {
                    Files.createFile(Paths.get(outputFile));
                }
            } else {
                logger.info("Missing output file");
            }

            if (cmd.hasOption("v")) {
                evaluation = true;
            }

        } catch (ParseException e1) {
            System.out.println(e1.getMessage());
            System.exit(-1);
        }
    }
}

package memoria.hugosepulvedaa.Run;

import memoria.hugosepulvedaa.*;
import org.apache.commons.cli.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class RunTime {

    private static final Logger logger = LogManager.getLogger(RunTime.class.getName());

    // privacy budget
    private static double EPSILON = 0.1;

    private static String queryString;
    private static String queryFile;
    private static String dataFile;
    private static String outputFile;
    private static String endpoint;
    private static boolean evaluation = false;
    private static boolean is_endpoint = false;

    private static long startTime;
    private static long endTime;
    private static HashMap<String, List<Double>> mapTimes = new HashMap<>();

    public static void main(String[] args) throws IOException {

        DataSource dataSource;

        parseInput(args);

        try {
            if (is_endpoint) {
                dataSource = new EndpointDataSource(endpoint, EPSILON);
            } else {
                dataSource = new HDTDataSource(dataFile, EPSILON);
            }

            Path queryLocation = Paths.get(queryFile);

            if (Files.isRegularFile(queryLocation)) {

                queryString = new Scanner(new File(queryFile)).useDelimiter("\\Z").next();
                runAnalysis(queryFile, queryString, dataSource, outputFile, evaluation, EPSILON);

            } else if (Files.isDirectory(queryLocation)) {

                SecureRandom secureRandom = new SecureRandom();

                List<Path> filesPath =
                        Files.list(Paths.get(queryFile))
                                .filter(p -> p.toString().endsWith(".rq"))
                                .collect(Collectors.toList());

                for(Path filePath : filesPath) {
                    mapTimes.put(filePath.toString(), new ArrayList<>());
                }

                logger.info("Running analysis to DIRECTORY: " + queryLocation);

                for (int i = 0; i < 100; i++) {

                    Path chosenFile = filesPath.get(secureRandom.nextInt(filesPath.size()));

                    logger.info("Running analysis to query: " + chosenFile.toString());

                    queryString = new Scanner(chosenFile).useDelimiter("\\Z").next();

                    try {

                        runAnalysis(
                                chosenFile.toString(),
                                queryString,
                                dataSource,
                                outputFile,
                                evaluation,
                                EPSILON);
                    } catch (Exception e) {
                        logger.error("query failed - " + chosenFile + ": " + e.getMessage());
                    }
                }
            } else {
                if (Files.notExists(queryLocation)) {
                    throw new FileNotFoundException("No query directory or file");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private static void runAnalysis(
            String queryFile,
            String queryString,
            DataSource dataSource,
            String outputFile,
            boolean evaluation,
            double EPSILON)
            throws IOException {

        // execute COUNT query
        int countQueryResult = dataSource.executeCountQuery(queryString, true);

        startTime = System.nanoTime();

        Query q = QueryFactory.create(queryString);
        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();

        Sensitivity smoothSensitivity;
        Element element = elementList.get(0);

        if (element instanceof ElementPathBlock) {

            DPQuery dpQuery = dataSource.getDPQuery(q);

            // delta parameter: use 1/n^2, with n = size of the data in the query
            double DELTA = dpQuery.getDelta();
            String elasticStability = dpQuery.getElasticStability();

            smoothSensitivity = dpQuery.getSmoothSensitivity();

            double scale = 2 * smoothSensitivity.getSensitivity() / EPSILON;

            writeAnalysisResult(
                    scale,
                    queryFile,
                    EPSILON,
                    DELTA,
                    evaluation,
                    countQueryResult,
                    elasticStability,
                    dpQuery.getGraphSizeTriples(),
                    dpQuery.isStarQuery(),
                    dataSource,
                    smoothSensitivity,
                    outputFile);
        }
    }

    private static void parseInput(String[] args) throws IOException {

        Options options = new Options();

        options.addOption("q", "query", true, "input SPARQL query");
        options.addOption("endpoint", "SPARQLendpoint", true, "SPARQl query endpoint");
        options.addOption("f", "qFile", true, "input SPARQL query file");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "dir", true, "query directory");
        options.addOption("o", "dir", true, "output file");
        options.addOption("v", "evaluation", true, "evaluation");
        options.addOption("eps", "epsilon", true, "epsilon");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("q")) {
                queryString = cmd.getOptionValue("q");
                queryString = queryString.substring(1, queryString.length() - 1);
                logger.info("SPARQL query");
            }

            if (cmd.hasOption("endpoint")) {
                endpoint = cmd.getOptionValue("endpoint");
                is_endpoint = true;
                logger.info("SPARQL query endpoint");
            }

            if (cmd.hasOption("eps")) {
                EPSILON = Double.parseDouble(cmd.getOptionValue("eps"));
                logger.info("Epsilon parameter");
            }

            if (cmd.hasOption("f")) {
                queryFile = cmd.getOptionValue("f");
                logger.info("SPARQL query file");
            }

            if (cmd.hasOption("d")) {
                dataFile = cmd.getOptionValue("d");
                logger.info("Data file");
            }

            if (cmd.hasOption("e")) {
                queryFile = cmd.getOptionValue("e");
                logger.info("Queries directory");
            }

            if (cmd.hasOption("o")) {
                outputFile = cmd.getOptionValue("o");

                if (!Files.exists(Paths.get(outputFile))) {
                    Files.createFile(Paths.get(outputFile));
                }
                logger.info("Output file");
            }

            if (cmd.hasOption("v")) {
                evaluation = true;
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private static void writeAnalysisResult(
            double scale,
            String queryFile,
            double EPSILON,
            double DELTA,
            boolean evaluation,
            int countQueryResult,
            String elasticStability,
            long graphSize,
            boolean starQuery,
            DataSource dataSource,
            Sensitivity smoothSensitivity,
            String outputFile)
            throws IOException {

        SecureRandom random = new SecureRandom();

        int times = 1;

        if (evaluation) {
            times = 100;
        }

        List<Double> privateResultList = new ArrayList<>();
        List<Integer> resultList = new ArrayList<>();

        for (int i = 0; i < times; i++) {

            double u = 0.5 - random.nextDouble();
            // LaplaceDistribution l = new LaplaceDistribution(u, scale);
            double log = Math.log(1 - 2 * Math.abs(u));
            double noise = -Math.signum(u) * scale * log;

            logger.info("Math.log(1 - 2 * Math.abs(u)): " + log);

            double finalResult1 = countQueryResult + noise;
            // double finalResult2 = countQueryResult + l.sample();

            logger.info("Original result: " + countQueryResult);
            logger.info("Noise added: " + Math.round(noise));
            logger.info("Private Result: " + Math.round(finalResult1));

            privateResultList.add(finalResult1);
            resultList.add(countQueryResult);
        }

        // stopTime
        endTime = System.nanoTime();
        long duration = (endTime - startTime);
        double durationInSeconds = (double) (duration / 1000000000);
        logger.info("Time: " + durationInSeconds + " seconds");

        mapTimes.get(queryFile).add(durationInSeconds);

        Result result =
                new Result(
                        queryFile,
                        EPSILON,
                        DELTA,
                        privateResultList,
                        smoothSensitivity.getSensitivity(),
                        resultList,
                        smoothSensitivity.getMaxK(),
                        scale,
                        elasticStability,
                        graphSize,
                        starQuery,
                        dataSource.getMapMostFreqValue(),
                        dataSource.getMapMostFreqValueStar());

        String resultsBuffer = result.toString().replace('\n', ' ') + "\n";

        Files.write(Paths.get(outputFile), resultsBuffer.getBytes(), StandardOpenOption.APPEND);

        for (String key : mapTimes.keySet()) {
            List<Double> durations = mapTimes.get(key);

            for(Double d: durations) {
                Files.write(Paths.get("fileTimes"), Double.toString(d).getBytes(), StandardOpenOption.APPEND);
            }
        }
    }
}
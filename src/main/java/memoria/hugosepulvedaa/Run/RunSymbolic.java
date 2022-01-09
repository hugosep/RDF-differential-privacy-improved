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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class RunSymbolic {

    private static final Logger logger = LogManager.getLogger(RunSymbolic.class.getName());

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
    private static final HashMap<String, List<Long>> mapTimes = new HashMap<>();

    public static void main(String[] args) throws IOException {

        DataSource dataSource;

        parseInput(args);

        try {
            if (is_endpoint) {
                dataSource = new EndpointDataSource(endpoint);
            } else {
                dataSource = new HDTDataSource(dataFile);
            }

            Path queryLocation = Paths.get(queryFile);

            if (Files.isRegularFile(queryLocation)) {

                queryString = new Scanner(new File(queryFile)).useDelimiter("\\Z").next();
                mapTimes.put(queryFile, new ArrayList<>());
                runAnalysis(queryFile, queryString, dataSource, outputFile, evaluation, EPSILON);

            } else if (Files.isDirectory(queryLocation)) {

                List<Path> filesPath =
                        Files.list(Paths.get(queryFile))
                                .filter(p -> p.toString().endsWith(".rq"))
                                .collect(Collectors.toList());

                logger.info("Running analysis to DIRECTORY: " + queryLocation);

                for (Path filePath : filesPath) {
                    mapTimes.put(filePath.toString(), new ArrayList<>());
                    logger.info("Running analysis to query: " + filePath);

                    queryString = new Scanner(filePath).useDelimiter("\\Z").next();

                    try {
                        runAnalysis(
                                filePath.toString(),
                                queryString,
                                dataSource,
                                outputFile,
                                evaluation,
                                EPSILON);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
                }

                try {
                    File fileTime = new File("fileTime.txt");

                    if (fileTime.createNewFile()) {
                        System.out.println("File created: " + fileTime.getName());
                    } else {
                        System.out.println("File already exists.");
                    }

                    logger.info("length mapTimes: " + mapTimes.size());

                    for (String key : mapTimes.keySet()) {
                        List<Long> durations = mapTimes.get(key);
                        String strKey = key + " " + durations.size() + "\n";

                        Files.write(
                                Paths.get("fileTime.txt"),
                                strKey.getBytes(),
                                StandardOpenOption.APPEND);

                        for (Long d : durations) {
                            String strD = d + "\n";
                            Files.write(
                                    Paths.get("fileTime.txt"),
                                    strD.getBytes(),
                                    StandardOpenOption.APPEND);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
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
            double DELTA = 1 / Math.pow(dpQuery.getGraphSizeTriples(), 2);
            double beta = EPSILON / (2 * Math.log(2 / DELTA));

            String elasticStability = "0";
            int k = 0;

            if (dpQuery.isStarQuery()) {
                elasticStability = "x";

                Sensitivity sensitivity = new Sensitivity(1.0, elasticStability);

                smoothSensitivity =
                        GraphElasticSensitivity.smoothElasticSensitivityStar(
                                elasticStability, sensitivity, beta, k);

                logger.info(
                        "Star query (smooth) sensitivity: " + smoothSensitivity.getSensitivity());

            } else {
                smoothSensitivity =
                        GraphElasticSensitivity.smoothElasticSensitivity(
                                dpQuery.getStarQuery().getElasticStability(),
                                0,
                                beta,
                                k,
                                dpQuery.getGraphSizeTriples());

                logger.info("Path Smooth Sensitivity: " + smoothSensitivity.getSensitivity());
            }
            logger.info("SmoothSensitivity:" + smoothSensitivity);

            double scale = 2 * smoothSensitivity.getSensitivity() / EPSILON;

            writeAnalysisResult(
                    q,
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
            Query query,
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
        logger.info("Time: " + duration + " nanoseconds");

        mapTimes.get(queryFile).add(duration);

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
                        dataSource.getDPQuery(query).getMapMostFreqValues(),
                        dataSource.getDPQuery(query).getMapMostFreqValuesStar());

        String resultsBuffer = result.toString().replace('\n', ' ') + "\n";

        Files.write(Paths.get(outputFile), resultsBuffer.getBytes(), StandardOpenOption.APPEND);
    }
}

package memoria.hugosepulvedaa.Run;

import memoria.hugosepulvedaa.*;
import memoria.hugosepulvedaa.utils.Helper;
import org.apache.commons.cli.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import symjava.symbolic.Expr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static symjava.symbolic.Symbol.x;

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

    public static void main(String[] args)
            throws IOException, CloneNotSupportedException, ExecutionException {

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
                runAnalysis(queryFile, queryString, dataSource, outputFile, evaluation, EPSILON);

            } else if (Files.isDirectory(queryLocation)) {

                Iterator<Path> filesPath =
                        Files.list(Paths.get(queryFile))
                                .filter(p -> p.toString().endsWith(".rq"))
                                .iterator();
                logger.info("Running analysis to DIRECTORY: " + queryLocation);

                while (filesPath.hasNext()) {

                    Path nextQuery = filesPath.next();

                    logger.info("Running analysis to query: " + nextQuery.toString());

                    queryString = new Scanner(nextQuery).useDelimiter("\\Z").next();

                    try {
                        runAnalysis(
                                nextQuery.toString(),
                                queryString,
                                dataSource,
                                outputFile,
                                evaluation,
                                EPSILON);
                    } catch (Exception e) {
                        logger.error("query failed!!: " + nextQuery);
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
            throws IOException, CloneNotSupportedException, ExecutionException {

        // execute COUNT query
        int countQueryResult = dataSource.executeCountQuery(queryString);

        startTime = System.nanoTime();

        Query q = QueryFactory.create(queryString);

        List<List<String>> triplePatterns = new ArrayList<>();
        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();

        Sensitivity smoothSensitivity;
        Element element = elementList.get(0);
        boolean starQuery = false;

        if (element instanceof ElementPathBlock) {

            Expr elasticStability = Expr.valueOf(0);
            int k = 1;

            Map<String, List<TriplePath>> starQueriesMap = Helper.getStarPatterns(q);

            dataSource.setMostFreqValueMaps(starQueriesMap, triplePatterns);

            long graphSize = dataSource.getGraphSizeTriples(triplePatterns);

            // esta demas, ya que en la funcion se agrega al logger
            logger.info("graph size: " + graphSize);

            // delta parameter: use 1/n^2, with n = size of the data in the query
            double DELTA = 1 / Math.pow(graphSize, 2);
            double beta = EPSILON / (2 * Math.log(2 / DELTA));

            if (Helper.isStarQuery(q)) {
                starQuery = true;
                elasticStability = x;

                Sensitivity sensitivity = new Sensitivity(1.0, elasticStability);

                smoothSensitivity =
                        GraphElasticSensitivity.smoothElasticSensitivityStar(
                                elasticStability, sensitivity, beta, k, graphSize);

                logger.info(
                        "star query (smooth) sensitivity: " + smoothSensitivity.getSensitivity());

            } else {
                /* elasticStability = GraphElasticSensitivity.calculateSensitivity(
                                            k,
                                            starQueriesMap,
                                            EPSILON,
                                            hdtDataSource);
                */

                List<StarQuery> listStars = new ArrayList<>();

                for (List<TriplePath> tp : starQueriesMap.values()) {
                    listStars.add(new StarQuery(tp));
                }

                StarQuery sq = GraphElasticSensitivity.calculateSensitivity(listStars, dataSource);

                logger.info("Elastic Stability: " + sq.getElasticStability());

                smoothSensitivity =
                        GraphElasticSensitivity.smoothElasticSensitivity(
                                sq.getElasticStability(), 0, beta, k, graphSize);

                logger.info("Path Smooth Sensitivity: " + smoothSensitivity.getSensitivity());
            }

            double scale = 2 * smoothSensitivity.getSensitivity() / EPSILON;

            writeAnalysisResult(
                    scale,
                    queryFile,
                    EPSILON,
                    DELTA,
                    evaluation,
                    countQueryResult,
                    elasticStability,
                    graphSize,
                    starQuery,
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
            } else {
                logger.info("Missing SPARQL query");
            }

            if (cmd.hasOption("endpoint")) {
                endpoint = cmd.getOptionValue("endpoint");
                is_endpoint = true;
            } else {
                logger.info("Missing SPARQL query endpoint");
            }

            if (cmd.hasOption("eps")) {
                EPSILON = Double.parseDouble(cmd.getOptionValue("eps"));
            } else {
                logger.info("Missing epsilon parameter");
            }

            if (cmd.hasOption("f")) {
                queryFile = cmd.getOptionValue("f");
            } else {
                logger.info("Missing SPARQL query file");
            }

            if (cmd.hasOption("d")) {
                dataFile = cmd.getOptionValue("d");
            } else {
                logger.info("Missing data file");
            }

            if (cmd.hasOption("e")) {
                queryFile = cmd.getOptionValue("e");
            } else {
                logger.info("Missing queries directory");
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

    private static void writeAnalysisResult(
            double scale,
            String queryFile,
            double EPSILON,
            double DELTA,
            boolean evaluation,
            int countQueryResult,
            Expr elasticStability,
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
        System.out.println("Time: " + durationInSeconds + " seconds");

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
    }
}

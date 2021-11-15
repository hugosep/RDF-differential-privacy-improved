package memoria.hugosepulvedaa.Run;

import memoria.hugosepulvedaa.EndpointDataSource;
import memoria.hugosepulvedaa.HDTDataSource;
import org.apache.commons.cli.*;
import org.apache.jena.query.*;
import org.apache.jena.sparql.core.ResultBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Scanner;

public class RunQuery {

    private static final Logger logger = LogManager.getLogger(RunQuery.class.getName());

    static String queryString;
    static String queryFile;
    static String dataFile;
    static String endpoint;
    static CommandLine cmd;
    static HDTDataSource hdtDataSource;
    static EndpointDataSource endpointDataSource;

    public static void main(String[] args) {

        Options options = new Options();

        options.addOption("f", "qFile", true, "input SPARQL query file");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "endpoint", true, "endpoint address");

        SecureRandom sc = new SecureRandom();

        for (int i = 0; i < 100; i++) {
            logger.info("secure random: " + sc.nextDouble());
        }

        CommandLineParser parser = new DefaultParser();

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("f")) {
                queryString = cmd.getOptionValue("f");
            } else {
                logger.info("Missing query file");
            }

            if (cmd.hasOption("d")) {
                dataFile = cmd.getOptionValue("d");
            } else {
                logger.info("Missing data file");
            }

            if (cmd.hasOption("e")) {
                endpoint = cmd.getOptionValue("e");
                System.out.println("First endpoint:" + endpoint);
            } else {
                logger.info("Missing endpoint address");
            }

        } catch (ParseException e1) {
            System.out.println(e1.getMessage());
            System.exit(-1);
        }

        try {

            if (cmd.hasOption("f")) {
                System.out.println("Endpoint");
                endpointDataSource = new EndpointDataSource(endpoint);
            } else {
                hdtDataSource = new HDTDataSource(queryString);
            }

            Path queryLocation = Paths.get(queryString);
            logger.info(queryFile);

            if (Files.isRegularFile(queryLocation)) {

                queryString = new Scanner(new File(queryString)).useDelimiter("\\Z").next();

                logger.info("queryString: " + queryString);

                Query query = QueryFactory.create(queryString);
                ResultSet rs;

                if (cmd.hasOption("f")) {
                    rs = endpointDataSource.executeQuery(query);
                } else {
                    rs = hdtDataSource.executeQuery(query);
                }

                int i = 0;

                while (rs.hasNext()) {
                    logger.info(
                            ((ResultBinding) rs.next())
                                    .getBinding()
                                    .get(query.getProjectVars().get(0)));
                    i++;
                }
                logger.info("results: " + i);
            }
        } catch (IOException e1) {
            System.out.println("Exception: " + e1.getMessage());
            System.exit(-1);
        }
    }
}

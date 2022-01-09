package memoria.hugosepulvedaa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;

import java.util.*;
import java.util.stream.Collectors;

public class GraphElasticSensitivity {

    private static final Logger logger =
            LogManager.getLogger(GraphElasticSensitivity.class.getName());

    /*public static double setOfMappingsSensitivity(
            Expr elasticSensitivity, double prevSensitivity, double beta, int k) {

        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double smoothSensitivity = Math.exp(-k * beta) * func1.apply(k);

        if (func1.apply(0) == 0 || (smoothSensitivity < prevSensitivity)) {
            return prevSensitivity;
        } else {
            return setOfMappingsSensitivity(elasticSensitivity, smoothSensitivity, beta, k + 1);
        }
    }*/

    /* previous function
    public static StarQuery calculateSensitivity(int k,
                                                 List<StarQuery> listStars,
                                                 double EPSILON,
                                                 DataSource dataSource)
    */
    public static StarQuery calculateSensitivity(
            DataSource dataSource,
            HashMap<MaxFreqQuery, Integer> mostFrequentResults,
            List<StarQuery> listStars) {

        logger.info("h2: " + listStars.size());
        StarQuery starQueryFirst = Collections.max(listStars);
        listStars.remove(starQueryFirst);

        // calculate sensibility for starQuery
        // Expr elasticStabilityFirstStar = Expr.valueOf(1); // f(x) = 1
        String elasticStabilityFirstStar = "1";
        starQueryFirst.setElasticStability(elasticStabilityFirstStar);
        StarQuery starQuerySecond;

        if (listStars.size() > 1) {
            starQuerySecond = calculateSensitivity(dataSource, mostFrequentResults, listStars);

        } else {
            // second star query in the map
            starQuerySecond = Collections.max(listStars);
            listStars.remove(starQuerySecond);

            // Expr elasticStabilityPrime = x;
            // Expr elasticStabilityPrime = Expr.valueOf(1);
            String elasticStabilityPrime = "1";
            starQuerySecond.setElasticStability(elasticStabilityPrime);
        }
        // now we join, S_G(star2, G)
        return calculateJoinSensitivity(
                dataSource, mostFrequentResults, starQueryFirst, starQuerySecond);
    }

    private static StarQuery calculateJoinSensitivity(
            DataSource dataSource,
            HashMap<MaxFreqQuery, Integer> mostFrequentResults,
            StarQuery starQueryLeft,
            StarQuery starQueryRight) {

        List<String> joinVariables = starQueryLeft.getVariables();
        joinVariables.retainAll(starQueryRight.getVariables());

        String mostPopularValueLeft;
        String mostPopularValueRight;

        if (starQueryLeft.getMostPopularValue() == null) {
            mostPopularValueLeft =
                    mostPopularValue(joinVariables.get(0), starQueryLeft, dataSource);
            logger.info("mostPopularValueLeft: " + mostPopularValueLeft);
            starQueryLeft.setMostPopularValue(mostPopularValueLeft);

        } else {
            mostPopularValueLeft = starQueryLeft.getMostPopularValue();
        }

        if (starQueryRight.getMostPopularValue() == null) {
            mostPopularValueRight =
                    mostPopularValue(joinVariables.get(0), starQueryRight, dataSource);
            logger.info("mostPopularValueRight: " + mostPopularValueRight);
            starQueryRight.setMostPopularValue(mostPopularValueRight);
        } else {
            mostPopularValueRight = starQueryRight.getMostPopularValue();
        }

        String stabilityRight = starQueryRight.getElasticStability();
        String stabilityLeft = starQueryLeft.getElasticStability();

        // new stability
        Polynomial polynomialf1 = new Polynomial("x", mostPopularValueRight);
        Polynomial polynomialf2 = new Polynomial("x", mostPopularValueLeft);

        polynomialf1.addBinomial(stabilityLeft);
        polynomialf2.addBinomial(stabilityRight);

        // Polynomial f1 = new Func("f1", mostPopularValueRight.multiply(stabilityLeft));
        // Polynomial f2 = new Func("f2", mostPopularValueLeft.multiply(stabilityRight));

        // I generate new starQueryPrime
        StarQuery newStarQueryPrime = new StarQuery(starQueryLeft.getTriples());
        newStarQueryPrime.addStarQuery(starQueryRight.getTriples());

        ExprEvaluator exprEvaluator = new ExprEvaluator(false, (short) 100);
        IExpr result;

        result = exprEvaluator.eval("Function({x}, " + polynomialf1 + ")[1]");

        // double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
        // double f2Val = Math.round(f2.toBytecodeFunc().apply(1));

        double f1Val = Math.round(exprEvaluator.evalf(result));

        result = exprEvaluator.eval("Function({x}, " + polynomialf2 + ")[1]");
        double f2Val = Math.round(exprEvaluator.evalf(result));

        if (f1Val > f2Val) {
            newStarQueryPrime.setElasticStability(polynomialf1.toString());
            newStarQueryPrime.setMostPopularValue(mostPopularValueRight);
        } else {
            newStarQueryPrime.setElasticStability(polynomialf2.toString());
            newStarQueryPrime.setMostPopularValue(mostPopularValueLeft);
        }
        return newStarQueryPrime;
    }

    /*
     * mostPopularValue(joinVariable a, StarQuery starQuery, DataSource)
     */
    private static String mostPopularValue(String var, StarQuery starQuery, DataSource dataSource) {
        // base case: mp(a,s_1,G)
        // Expr expr = x;
        MaxFreqQuery maxFreqQuery = new MaxFreqQuery(starQuery.toString(), var);

        String constant = Integer.toString(dataSource.mostFrequentResult(maxFreqQuery));

        /*expr =
                expr.plus(
                        dataSource.mostFrequentResult(new MaxFreqQuery(starQuery.toString(), var)));
        */
        return Polynomial.createBinomial("x", constant);
    }

    public static Sensitivity smoothElasticSensitivity(
            String elasticSensitivity, double prevSensitivity, double beta, int k, long graphSize) {

        Sensitivity sensitivity = new Sensitivity(prevSensitivity, elasticSensitivity);

        // history capacity, this can be minor
        short historyCapacity = 100;
        ExprEvaluator exprEvaluator = new ExprEvaluator(false, historyCapacity);

        logger.info("Function: E^(-" + beta + "*x)*" + elasticSensitivity);

        // derivative of the function
        IExpr result = exprEvaluator.eval("diff(E^(-" + beta + "*x)*" + elasticSensitivity + ",x)");
        String derivative = result.toString();

        // replace the value of E in the derivative string
        derivative = derivative.replaceAll("2.718281828459045", "E");
        logger.info("Derivative function: " + derivative);

        // simplification of the function, deleting the exponential part and only getting the polynomial
        String simplified = derivative.replaceAll("\\*E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "");
        simplified = simplified.replaceAll("E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)\\*", "");
        simplified = simplified.replaceAll("/E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "");
        simplified = simplified.replaceAll("\\+E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "+1");
        simplified = simplified.replaceAll("E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)\\+", "1+");
        simplified = simplified.replaceAll("-E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "-1");
        simplified = simplified.replaceAll("E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)-", "1-");

        logger.info("Simplified function: " + simplified);

        // NRoots function uses Laguerre's method, returning real and complex solutions
        result = exprEvaluator.eval("NRoots(" + simplified + "==0)");

        String strResult = result.toString();

        double ceilMaxCandidate = k;
        int maxI = k;

        if (strResult.equals("{}")) {
            logger.info("The function has no roots.");
        } else {
            logger.info("Candidates: "+ result);
            /* OPTIMIZATION
             * If we maximize E^(-beta*x)*P(x), where P(x) is a polynomial.
             * The maximal value can be determined finding the max maximal
             * of the function, where it is decreasing to infinite.
             */

            strResult = strResult.substring(1, strResult.length() - 1);

            String[] arrayZeros = strResult.split(",");
            List<String> listStrZeros = new ArrayList<>(Arrays.asList(arrayZeros));

            // clean the string returned
            /*listStrZeros.replaceAll(zero -> zero.substring(1, zero.length() - 1));
            listStrZeros.replaceAll(zero -> zero.substring(3));*/

            // remove complex solutions
            listStrZeros.removeIf(zero -> zero.contains("I"));
            listStrZeros.removeIf(zero -> zero.contains("i"));

            List<Double> listDoubleZeros =
                    listStrZeros.stream().map(exprEvaluator::evalf).collect(Collectors.toList());

            double maxCandidate = Collections.max(listDoubleZeros);

            ceilMaxCandidate = (int) Math.ceil(maxCandidate);

            logger.info("graphSize: " + graphSize);
            logger.info("maxCandidate: " + maxCandidate);

            if (ceilMaxCandidate < 0) {
                ceilMaxCandidate = k;

            } else if(ceilMaxCandidate > graphSize) {
                ceilMaxCandidate = graphSize;
            }
        }

        for (int i = k; i <= ceilMaxCandidate; i++) {
            result = exprEvaluator.eval("Function({x}, " + elasticSensitivity + ")[" + i + "]");

            double kPrime = exprEvaluator.evalf(result);
            double smoothSensitivity = Math.exp(-i * beta) * kPrime;

            if (smoothSensitivity > prevSensitivity) {
                prevSensitivity = smoothSensitivity;
                maxI = i;
            }
        }

        sensitivity.setMaxK(maxI);
        sensitivity.setSensitivity(prevSensitivity);

        return sensitivity;
    }

    public static Sensitivity smoothElasticSensitivityStar(
            String elasticSensitivity, Sensitivity prevSensitivity, double beta, int k) {

        /* OPTIMIZATION
         * It doesn't make sense iterate for f(x)=E^(-beta*x), because the function is a decreasing
         * function.
         * For this reason, the max value will be E^(-beta*x) with the initial x, in this case the
         * parameter k.
         */
        logger.info("Function: E^(-" + beta + "*k)");

        double exponentialPart = Math.exp(-beta * k);
        Sensitivity smoothSensitivity = new Sensitivity(exponentialPart, elasticSensitivity);
        smoothSensitivity.setMaxK(k);

        if (smoothSensitivity.getSensitivity() > prevSensitivity.getSensitivity()) {
            return smoothSensitivity;
        }

        return prevSensitivity;
    }
}

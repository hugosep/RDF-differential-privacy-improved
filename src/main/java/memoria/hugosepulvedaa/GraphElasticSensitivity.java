package memoria.hugosepulvedaa;

import org.apache.jena.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Func;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GraphElasticSensitivity {

    private static final Logger logger =
            LogManager.getLogger(GraphElasticSensitivity.class.getName());

    public static double setOfMappingsSensitivity(
            Expr elasticSensitivity, double prevSensitivity, double beta, int k) {

        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        double smoothSensitivity = Math.exp(-k * beta) * func1.apply(k);

        if (func1.apply(0) == 0 || (smoothSensitivity < prevSensitivity)) {
            return prevSensitivity;
        } else {
            return setOfMappingsSensitivity(elasticSensitivity, smoothSensitivity, beta, k + 1);
        }
    }

    /* previous function
    public static StarQuery calculateSensitivity(int k,
                                                 List<StarQuery> listStars,
                                                 double EPSILON,
                                                 DataSource dataSource)
    */
    public static StarQuery calculateSensitivity(
            Query originalQuery, List<StarQuery> listStars, DataSource dataSource) {

        StarQuery starQueryFirst = Collections.max(listStars);
        listStars.remove(starQueryFirst);

        // calculate sensibility for starQuery
        // Expr elasticStabilityFirstStar = Expr.valueOf(1); // f(x) = 1
        String elasticStabilityFirstStar = "1";
        starQueryFirst.setElasticStability(elasticStabilityFirstStar);
        StarQuery starQuerySecond;

        if (listStars.size() > 1) {
            starQuerySecond = calculateSensitivity(originalQuery, listStars, dataSource);

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
        return calculateJoinSensitivity(originalQuery, starQueryFirst, starQuerySecond, dataSource);
    }

    private static StarQuery calculateJoinSensitivity(
            Query originalQuery,
            StarQuery starQueryLeft,
            StarQuery starQueryRight,
            DataSource hdtDataSource) {

        List<String> joinVariables = starQueryLeft.getVariables();
        joinVariables.retainAll(starQueryRight.getVariables());

        String mostPopularValueLeft;
        String mostPopularValueRight;

        if (starQueryLeft.getMostPopularValue() == null) {
            mostPopularValueLeft =
                    mostPopularValue(
                            originalQuery, joinVariables.get(0), starQueryLeft, hdtDataSource);
            logger.info("mostPopularValueLeft: " + mostPopularValueLeft);
            starQueryLeft.setMostPopularValue(mostPopularValueLeft);

        } else {
            mostPopularValueLeft = starQueryLeft.getMostPopularValue();
        }

        if (starQueryRight.getMostPopularValue() == null) {
            mostPopularValueRight =
                    mostPopularValue(
                            originalQuery, joinVariables.get(0), starQueryRight, hdtDataSource);
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

        ExprEvaluator util = new ExprEvaluator(false, (short) 100);
        IExpr result;

        result = util.eval("Function({x}, " + polynomialf1 + ")[1]");

        // double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
        // double f2Val = Math.round(f2.toBytecodeFunc().apply(1));

        double f1Val = Math.round(util.evalf(result));

        result = util.eval("Function({x}, " + polynomialf2 + ")[1]");
        double f2Val = Math.round(util.evalf(result));

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
    private static String mostPopularValue(
            Query originalQuery, String var, StarQuery starQuery, DataSource dataSource) {
        // base case: mp(a,s_1,G)
        // Expr expr = x;
        String constant =
                Integer.toString(
                        dataSource.mostFrequentResult(
                                originalQuery,
                                new MaxFreqQuery(originalQuery, starQuery.toString(), var)));
        /*expr =
                expr.plus(
                        dataSource.mostFrequentResult(new MaxFreqQuery(starQuery.toString(), var)));
        */
        return Polynomial.createBinomial("x", constant);
    }

    public static Sensitivity smoothElasticSensitivity(
            String elasticSensitivity, double prevSensitivity, double beta, int k, long graphSize) {

        Sensitivity sensitivity = new Sensitivity(prevSensitivity, elasticSensitivity);

        /*Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();
        */

        // System.out.println("f1:" + f1);

        // this can be minor
        short a = 100;
        ExprEvaluator util = new ExprEvaluator(false, a);
        IExpr result = util.eval("diff(E^(-" + beta + "*x)*" + elasticSensitivity + ",x)");
        result = util.eval("NSolve(0==" + result.toString() + ",x)");
        String strResult = result.toString();

        double ceilMaxCandidate = k;
        int maxI = k;

        if (strResult.equals("{}")) {
            logger.info("The function has no roots.");
        } else {

            /* OPTIMIZATION
             * If we maximize E^(-beta*x)*P(x), where P(x) is a polynomial.
             * The maximal value can be determined finding the max maximal of the function, where it is decreasing to
             * infinite.
             */

            strResult = strResult.substring(1, strResult.length() - 1);

            String[] arrayZeros = strResult.split(",");
            List<String> listStrZeros = new ArrayList<>(Arrays.asList(arrayZeros));

            listStrZeros.replaceAll(zero -> zero.substring(1, zero.length() - 1));
            listStrZeros.replaceAll(zero -> zero.substring(3));

            listStrZeros.removeIf(zero -> zero.contains("I"));
            listStrZeros.removeIf(zero -> zero.contains("i"));

            List<Double> listDoubleZeros =
                    listStrZeros.stream().map(util::evalf).collect(Collectors.toList());

            double maxCandidate = Collections.max(listDoubleZeros);

            ceilMaxCandidate = (int) Math.ceil(maxCandidate);

            logger.info("graphSize: " + graphSize);
            logger.info("maxCandidate: " + maxCandidate);

            if (ceilMaxCandidate < 0) {
                ceilMaxCandidate = k;
            }
        }

        for (int i = k; i <= ceilMaxCandidate; i++) {
            result = util.eval("Function({x}, " + elasticSensitivity + ")[1]");
            // double kPrime = func1.apply(i);
            double kPrime = util.evalf(result);
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

        double exponentialPart = Math.exp(-k * beta);
        Sensitivity smoothSensitivity = new Sensitivity(exponentialPart, elasticSensitivity);
        smoothSensitivity.setMaxK(k);

        if (smoothSensitivity.getSensitivity() > prevSensitivity.getSensitivity()) {
            return smoothSensitivity;
        }

        return prevSensitivity;
    }
}

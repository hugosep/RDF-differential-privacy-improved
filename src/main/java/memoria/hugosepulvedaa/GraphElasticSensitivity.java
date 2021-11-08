package memoria.hugosepulvedaa;

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
import java.util.concurrent.ExecutionException;

import static symjava.symbolic.Symbol.x;

public class GraphElasticSensitivity {

    private static final Logger logger = LogManager.getLogger(GraphElasticSensitivity.class.getName());

    public static double setOfMappingsSensitivity(Expr elasticSensitivity,
                                                  double prevSensitivity,
                                                  double beta,
                                                  int k) {

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
    public static StarQuery calculateSensitivity(List<StarQuery> listStars,
                                                 DataSource dataSource)
            throws ExecutionException,
            CloneNotSupportedException {

        StarQuery starQueryFirst = Collections.max(listStars);
        listStars.remove(starQueryFirst);

        // calculate sensibility for starQuery
        Expr elasticStabilityFirstStar = Expr.valueOf(1); // f(x) = 1
        starQueryFirst.setElasticStability(elasticStabilityFirstStar);
        StarQuery starQuerySecond;

        if(listStars.size() > 1) {
            starQuerySecond = calculateSensitivity(listStars, dataSource);

        } else {
            // second star query in the map
            starQuerySecond = Collections.max(listStars);
            listStars.remove(starQuerySecond);

            // Expr elasticStabilityPrime = x;
            Expr elasticStabilityPrime = Expr.valueOf(1);
            starQuerySecond.setElasticStability(elasticStabilityPrime);
        }
        // now we join, S_G(star2, G)
        return calculateJoinSensitivity(starQueryFirst, starQuerySecond, dataSource);
    }

    private static StarQuery calculateJoinSensitivity(StarQuery starQueryLeft,
                                                      StarQuery starQueryRight,
                                                      DataSource hdtDataSource) {

        List<String> joinVariables = starQueryLeft.getVariables();
        joinVariables.retainAll(starQueryRight.getVariables());

        Expr mostPopularValueLeft;
        Expr mostPopularValueRight;

        if (starQueryLeft.getMostPopularValue() == null) {
            mostPopularValueLeft = mostPopularValue(joinVariables.get(0), starQueryLeft, hdtDataSource);
            logger.info("mostPopularValueLeft: " + mostPopularValueLeft);
            starQueryLeft.setMostPopularValue(mostPopularValueLeft);

        } else {
            mostPopularValueLeft = starQueryLeft.getMostPopularValue();
        }

        if (starQueryRight.getMostPopularValue() == null) {
            mostPopularValueRight = mostPopularValue(joinVariables.get(0), starQueryRight, hdtDataSource);
            logger.info("mostPopularValueRight: " + mostPopularValueRight);
            starQueryRight.setMostPopularValue(mostPopularValueRight);
        } else {
            mostPopularValueRight = starQueryRight.getMostPopularValue();
        }

        Expr stabilityRight = starQueryRight.getElasticStability();
        Expr stabilityLeft = starQueryLeft.getElasticStability();

        // new stability
        Func f1 = new Func("f1", mostPopularValueRight.multiply(stabilityLeft));
        Func f2 = new Func("f2", mostPopularValueLeft.multiply(stabilityRight));

        // I generate new starQueryPrime
        StarQuery newStarQueryPrime = new StarQuery(starQueryLeft.getTriples());
        newStarQueryPrime.addStarQuery(starQueryRight.getTriples());

        double f1Val = Math.round(f1.toBytecodeFunc().apply(1));
        double f2Val = Math.round(f2.toBytecodeFunc().apply(1));

        if (f1Val > f2Val) {
            newStarQueryPrime.setElasticStability(f1);
            newStarQueryPrime.setMostPopularValue(mostPopularValueRight);
        } else {
            newStarQueryPrime.setElasticStability(f2);
            newStarQueryPrime.setMostPopularValue(mostPopularValueLeft);
        }
        return newStarQueryPrime;
    }

    /*
     * mostPopularValue(joinVariable a, StarQuery starQuery, DataSource)
     */
    private static Expr mostPopularValue(String var,
                                         StarQuery starQuery,
                                         DataSource dataSource) {
        // base case: mp(a,s_1,G)
        Expr expr = x;
        expr = expr.plus(dataSource.mostFrequentResult(new MaxFreqQuery(starQuery.toString(), var)));
        return expr;
    }

    public static Sensitivity smoothElasticSensitivity(Expr elasticSensitivity,
                                                       double prevSensitivity,
                                                       double beta,
                                                       int k,
                                                       long graphSize) {

        Sensitivity sensitivity = new Sensitivity(prevSensitivity, elasticSensitivity);

        Func f1 = new Func("f1", elasticSensitivity);
        BytecodeFunc func1 = f1.toBytecodeFunc();

        System.out.println("f1:" + f1);

        // this can be minor
        short a = 100;
        ExprEvaluator util = new ExprEvaluator(false, a);
        IExpr result = util.eval("diff(E^(-" + beta + "*x)*(" + f1 + "),x)");
        result = util.eval("NSolve(0==" + result.toString() + ",x)");
        String strResult = result.toString();

        if(strResult.equals("{}")) {
            logger.info("The function has no roots.");
        } else {
            /* OPTIMIZATION
             * If we maximize E^(-beta*x)*P(x), where P(x) is a polynomial.
             * The maximal value can be determined finding the max maximal of the function, where it is decreasing to
             * infinite.
             */

            strResult = strResult.substring(1, strResult.length() - 1);

            String[] arrayZeros = strResult.split(",");
            List<String> listZeros = new ArrayList<>(Arrays.asList(arrayZeros));

            listZeros.replaceAll(zero -> zero.substring(1, zero.length() - 1));
            listZeros.replaceAll(zero -> zero.substring(3));

            listZeros.removeIf(zero -> zero.contains("I"));
            listZeros.removeIf(zero -> zero.contains("i"));

            System.out.println("Out[3]: " + result);
            System.out.println("Out[3]: " + listZeros);

            double ceilValue;
            double floorValue;
            double zeroFloat;

            List<Integer> candidates = new ArrayList<Integer>(listZeros.size() * 2);

            for (String zero : listZeros) {
                zeroFloat = util.evalf(zero);
                ceilValue = Math.ceil(zeroFloat);
                floorValue = Math.floor(zeroFloat);
                candidates.add((int) ceilValue);
                candidates.add((int) floorValue);
            }
            int maxCandidate = Collections.max(candidates);

            logger.info("graphSize: " + graphSize);
            logger.info("maxCandidate: " + maxCandidate);

            int maxI = 0;
            if(maxCandidate < 0) {
                maxCandidate = 0;
            }
            //for (int i = 0; i < maxCandidate; i++) {

            for (int i = 0; i < maxCandidate; i++) {
                double kPrime = func1.apply(k);
                double smoothSensitivity = Math.exp(-k * beta) * kPrime;

                if (smoothSensitivity > prevSensitivity) {
                    prevSensitivity = smoothSensitivity;
                    maxI = i;
                }
                k++;
            }
            sensitivity.setMaxK(maxI);
            sensitivity.setSensitivity(prevSensitivity);
        }
        return sensitivity;
    }

    public static Sensitivity smoothElasticSensitivityStar(
            Expr elasticSensitivity,
            Sensitivity prevSensitivity,
            double beta,
            int k,
            long graphSize) {

        int maxI = 0;

        // OPTIMIZATION

        // It doesn't make sense iterate for f(x)=E^(-beta*x), because the function is a decreasing function.
        // For this reason, the max value will be E^(-beta*x) with the initial x, in this case the parameter k.

        /*for (int i = 0; i < graphSize; i++) {

            Sensitivity smoothSensitivity = new Sensitivity(Math.exp(-k * beta), elasticSensitivity);

            if (smoothSensitivity.getSensitivity() > prevSensitivity.getSensitivity()) {
                prevSensitivity = smoothSensitivity;
                maxI = i;
            }
            k++;
        }*/
        Sensitivity smoothSensitivity = new Sensitivity(Math.exp(-k * beta), elasticSensitivity);
        Sensitivity sens = new Sensitivity(
                prevSensitivity.getSensitivity(),
                elasticSensitivity);

        sens.setMaxK(maxI);
        return sens;
    }
}
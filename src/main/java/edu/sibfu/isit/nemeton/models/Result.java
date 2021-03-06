/*
 * The MIT License
 *
 * Copyright 2016 Max Balushkin.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package edu.sibfu.isit.nemeton.models;

import edu.sibfu.isit.nemeton.models.functions.NFunction;
import edu.sibfu.isit.nemeton.algorithms.OptimizationAlgorithm;

/**
 * Contains result of algorithm run.
 * 
 * @author Max Balushkin
 */
public class Result {
    
    private final OptimizationAlgorithm algorithm;
    private final NFunction function;
    private final CalculatedPoint[] values;
    
    private final int iterations;
    private final int evaluations;
    private final double accuracy;
    
    private PointHistory history;
    private String endClause;
    
    /**
     * Creates new algorithm result.
     * 
     * @param aAlgorithm algorithm
     * @param aFunction optimised function
     * @param aValues solutions
     * @param aIterations iterations
     * @param aEvaluations function evaluations
     * @param aAccuracy accuracy
     */
    public Result(
        OptimizationAlgorithm aAlgorithm,
        NFunction aFunction, 
        CalculatedPoint[] aValues,
        int aIterations, 
        int aEvaluations, 
        double aAccuracy
    ) {
        algorithm = aAlgorithm;
        function = aFunction;
        values = aValues;
        iterations = aIterations;
        evaluations = aEvaluations;
        accuracy = aAccuracy;
        
        endClause = "нет данных";
    }
    
    /**
     * Returns function.
     * 
     * @return function
     */
    public NFunction getFunction() {
        return function;
    }
    
    /**
     * Returns problem solutions.
     * 
     * @return solutions
     */
    public CalculatedPoint[] getValues() {
        return values;
    }
    
    /**
     * Returns algorithm.
     * 
     * @return algorithm
     */
    public OptimizationAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns search history.
     * 
     * @return search history
     */
    public PointHistory getHistory() {
        return history;
    }

    /**
     * Sets search history.
     * 
     * @param aHistory search history
     */
    public void setHistory( PointHistory aHistory ) {
        history = aHistory;
    }
    
    /**
     * Sets algorithm end clause.
     * 
     * @param aEndClause end clause
     */
    public void setEndClause( String aEndClause ) {
        endClause = aEndClause;
    }
    
    /**
     * Returns end clause.
     * 
     * @return End clause
     */
    public String getEndClause() {
        return endClause;
    }
    
    /**
     * Returns iterations.
     * 
     * @return Iterations
     */
    public int getIterations() {
        return iterations;
    }
    
    /**
     * Returns amount of function evaluations.
     * 
     * @return Evaluations
     */
    public int getEvaluations() {
        return evaluations;
    }
    
    /**
     * Returns agorithm accuracy.
     * 
     * @return accuracy
     */
    public double getAccuracy() {
        return accuracy;
    }

    @Override
    public String toString() {
        return algorithm.toString();
    }
    
}

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
package edu.sibfu.isit.nemeton.algorithms.sac;

import edu.sibfu.isit.nemeton.algorithms.OptimizationAlgorithm;
import edu.sibfu.isit.nemeton.algorithms.sac.kernels.ParabolicKernel;
import edu.sibfu.isit.nemeton.algorithms.sac.kernels.SelectiveKernel;
import edu.sibfu.isit.nemeton.models.CalculatedPoint;
import edu.sibfu.isit.nemeton.models.Pair;
import edu.sibfu.isit.nemeton.models.Point;
import edu.sibfu.isit.nemeton.models.Result;
import edu.sibfu.isit.nemeton.models.functions.NFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 *
 * @author Max Balushkin
 */
public class SACAlgorithm extends OptimizationAlgorithm {

    @FunctionalInterface
    private interface Function3<Value, Min, Max, R> {
        public R apply(Value aValue, Min aMin, Max aMax);
    }
   
    private final Random rnd;
    private Function3<Double, Double, Double, Double> transition;
    
    private Point centre = Point.zero(2);
    private Point delta = new Point(10, 10);
    private double selectiveness = 10;
    private SelectiveKernel kernel = new ParabolicKernel();
    
    private double accuracy = 0.00001;
    
    private int evaluations;
    
    public SACAlgorithm(final NFunction aFunction) {
        super(aFunction);
        rnd = new Random();
        evaluations = 0;
    }
    
    /**
     * Generates sample point around centre.
     * 
     * @param aN Amount of points in sample
     * @param aCentre Centre of hypercube (x)
     * @param aDelta Hypercube size (delta x)
     * @return Points and distances between them and centre
     */
    private ArrayList<Pair<CalculatedPoint,Point>> generateSample(final int aN, final Point aCentre, final Point aDelta) {
        final ArrayList<Pair<CalculatedPoint, Point>> sample = new ArrayList<>();
        for (int i = 0; i < aN; i++) {
            final int arity = aCentre.getArity();
            Point point = new Point(aCentre);
            Point uPoint = Point.zero(arity);
            for (int v = 0; v < arity; v++) {
                double u = rnd.nextDouble() * 2 - 1;
                double dx = aDelta.get(v) * u;
                point = point.add(dx, v);
                uPoint = uPoint.add(u, v);
            }
            final Pair<CalculatedPoint, Point> pair = new Pair<>(
                new CalculatedPoint(point, f.eval(point)), 
                uPoint
            );
            sample.add(pair);
        }
        
        evaluations += aN;
        return sample;
    }
    
    private Point nextDelta(
        final ArrayList<Pair<CalculatedPoint, Point>> aPoints, 
        final Point aDelta, final double aGamma, final double aQ, 
        final double[] aKernelUndim, final double aKernelSum
    ) {
        Point delta = aDelta.mul(aGamma);
        final int arity = aDelta.getArity();
        Point uSum = Point.zero(arity);
        final int n = aPoints.size();
        for (int i = 0; i < n; i++) {
            double p = aKernelUndim[i] / aKernelSum;
            Point u = aPoints.get(i).right().pow(aQ);
            uSum = uSum.add(u.mul(p));
        }
        delta = delta.mul(uSum.pow(1.0 / aQ));
        return delta;
    }
    
    private Pair<Point, Point> uMin(
        final int aArity, final ArrayList<Pair<CalculatedPoint, Point>> aPoints,
        final Point aDelta
    ) {
        final double values[] = aPoints
            .stream()
            .mapToDouble(
                (Pair<CalculatedPoint, ?> pair) -> { return pair.left().getValue(); }
            ).toArray();
        final double min = DoubleStream.of(values).min().getAsDouble();
        final double max = DoubleStream.of(values).max().getAsDouble();
        
        final double kernelUndim[] = DoubleStream.of(values)
            .map(
                (value) -> { 
                    double g = transition.apply(value, min, max);
                    return kernel.eval(selectiveness, g); 
                }
            ).toArray();
        final double kernelSum = DoubleStream.of(kernelUndim).sum();
        
        int n = aPoints.size();
        Point uMin = Point.zero(aArity);
        for (int i = 0; i < n; i++) {
            double p = kernelUndim[i] / kernelSum;
            Point u = aPoints.get(i).right();
            uMin = uMin.add(u.mul(p));
        }
        
        Point delta = nextDelta(aPoints, aDelta, 1, 4, kernelUndim, kernelSum);
        return new Pair<>(uMin, delta);
    }
    
    private boolean stopCondition(final Point aDelta, final double aEps) {
        int n = aDelta.getArity();
        for (int i = 0; i < n; i++) {
            if (aDelta.get(i) > aEps) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Result run(Comparator<Point> comparator) {
        String endClause = "нет данных";
        
        for (int it = 0; it < 1000; it++) {
            ArrayList<Pair<CalculatedPoint, Point>> points = generateSample(50, centre, delta);
            Pair<Point, Point> res = uMin(centre.getArity(), points, delta);
            Point uMin = res.left();
            centre = centre.add(delta.mul(uMin));
            delta = res.right();
            if (stopCondition(delta, accuracy)) {
                endClause = "по размеру delta x";
                break;
            }
        }
        Result result = new Result(
            this, f, 
            new CalculatedPoint[] { new CalculatedPoint(centre, f.eval(centre)) }, 
            evaluations, evaluations
        );
                
        return result;
    }

    @Override
    public Result maximize() {
        transition = (val, min, max) -> { return (max - val) / (max - min); };
        return super.maximize(); 
    }

    @Override
    public Result minimize() {
        transition = (val, min, max) -> { return (val - min) / (max - min); };
        return super.minimize();
    }
    
}

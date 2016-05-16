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
package edu.sibfu.isit.nemeton.controllers;

import edu.sibfu.isit.nemeton.algorithms.AlgorithmBuilder;
import edu.sibfu.isit.nemeton.controllers.providers.Functions;
import edu.sibfu.isit.nemeton.controllers.providers.FunctionProviderSubscriber;
import edu.sibfu.isit.nemeton.jzy.JzyChart;
import edu.sibfu.isit.nemeton.jzy.JzyContourPlot;
import edu.sibfu.isit.nemeton.jzy.JzyLinePlot;
import edu.sibfu.isit.nemeton.jzy.JzyScatterPlot;
import edu.sibfu.isit.nemeton.jzy.JzySurface;
import edu.sibfu.isit.nemeton.lib.JzyHelper;
import edu.sibfu.isit.nemeton.models.CalculatedPoint;
import edu.sibfu.isit.nemeton.models.Result;
import edu.sibfu.isit.nemeton.models.functions.NFunction;
import edu.sibfu.isit.nemeton.views.HistoryView;
import edu.sibfu.isit.nemeton.views.MainView;
import edu.sibfu.isit.nemeton.views.ResultView;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import edu.sibfu.isit.nemeton.algorithms.OptimizationAlgorithm;
import edu.sibfu.isit.nemeton.framework.Subscriber;


/**
 *
 * @author Max Balushkin
 */
public class MainController {
    
    public final Subscriber<NFunction> functionRegisterCallback = new Subscriber<NFunction>() {
        @Override
        public void publish( final NFunction aFunc ) {
            functionsListModel.addElement( aFunc );
        }
    };
    
    public final Subscriber<AlgorithmBuilder> builderRegisterCallback = new Subscriber<AlgorithmBuilder>() {
        @Override
        public void publish( final AlgorithmBuilder aBldr ) {
            buildersTableModel.addRow( new Object[] { true, aBldr } );
        }
    };
    
    MainView view;
    DefaultTableModel buildersTableModel;
    DefaultComboBoxModel<NFunction> functionsListModel;
    
    public enum Goal {
        Minimize,
        Maximize
    }
    
    public MainController(MainView view) {
        this.view = view;
        buildersTableModel = 
            new DefaultTableModel( new Object[][] {}, new String[] { "", "Название" } ) {
                Class[] types = new Class [] {
                    Boolean.class, AlgorithmBuilder.class
                };

                @Override
                public Class getColumnClass( int columnIndex ) {
                    return types [ columnIndex ];
                }
            };
        functionsListModel = new DefaultComboBoxModel<>();
    }
    
    public TableModel getAlghotitmsTableModel() {
        return buildersTableModel;
    }
    
    public ComboBoxModel getFunctionListModel() {
        return functionsListModel;
    }
   
    public NFunction getFunction( int index ) {
        return Functions.get(index);
    }
    
    public void runAlgorithms( Goal aGoal, final NFunction aFunction, final boolean showSurface, final boolean showHistory ) {
        final ArrayList<OptimizationAlgorithm> algorithms = new ArrayList<>();
        for ( Object item : buildersTableModel.getDataVector().toArray() ) {
            final Vector vector = (Vector) item;
            if ( (Boolean) vector.get( 0 ) ) {
                AlgorithmBuilder bldr = (AlgorithmBuilder) vector.get( 1 );
                algorithms.add( bldr.build( aFunction ) );
            }
        }
 
        final ArrayList<Result> results = new ArrayList<>();
        for ( OptimizationAlgorithm al : algorithms ) {
            switch ( aGoal ) {
                case Maximize:
                    results.add( al.maximize() );
                    break;
                case Minimize:
                    results.add( al.minimize() );
            }
        }
        
        if ( showSurface ) show( aFunction, results );
        
        ResultView resultView = new ResultView( results );
        resultView.setVisible( true );
        
        if ( showHistory ) showHistory( results );
    }
    
    public void error( String message ) {
        JOptionPane.showMessageDialog( view, message );
    }
    
    public void showHistory( final ArrayList<Result> aResults ) {
        HistoryView history = new HistoryView( aResults );
        history.showAsFrame();
    }
    
    public void show( final NFunction aFunction, final ArrayList<Result> aResults ) {
        if ( !aFunction.isMapped() ) {
            error( "Specified function can not be mapped with Jzy3d" );
            return;
        }
        
        // Calculate plot range: 
        //  [ min(X) - margin .. max(X) + margin ] 
        //      where X in [ sloutions, point history ]
        Range range = new Range( Double.MAX_VALUE, Double.MIN_VALUE );
        for ( Result result : aResults ) {
            Range r = JzyHelper.range( result.getValues(), 5 );
            range = JzyHelper.union( range, r );
            
            final int n = result.getHistory().size();
            for ( int i = 0; i < n; i++ ) {
                r = JzyHelper.range( result.getHistory().get( i ), 5 );
                range = JzyHelper.union( range, r );
            }
        }

        JzyChart chart = new JzyChart( Quality.Advanced );
        
        // Surface plot
        final int steps = 80;
        JzySurface surface = new JzySurface( aFunction, range, steps )
            .colourize( new ColorMapRainbow(), 0.95f );
        
        // Contour plot
        JzyContourPlot contour = new JzyContourPlot(
            surface, 
            new ColorMapRainbow(), 
            0f, 
            10, 
            chart.getBounds()
        );
        
        chart.addPlot( surface ).addContourPlot( contour );
        
        for ( Result result : aResults ) {
            // Scatter plot. Solutions
            final CalculatedPoint[] solution = result.getValues();
            final Color colour = Color.random();
            JzyScatterPlot scatterPlot = new JzyScatterPlot( solution, colour );
            chart.addPlot( scatterPlot );
            
            // Best solution iterations
            // final int n = result.getHistory().size();
            final int n = 1;
            for ( int i = 0; i < n; i++ ) {
                final ArrayList<CalculatedPoint> points = result.getHistory().get( i );
                JzyLinePlot line = new JzyLinePlot( points );
                chart.addPlot(line);
            }
        }
        
        chart.show( "График функции и найденные точки" );
    }
}

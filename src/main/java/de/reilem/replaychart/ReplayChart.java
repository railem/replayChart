package de.reilem.replaychart;

import de.reilem.replaychart.donadigo.DonadigoReplayBuilder;
import de.reilem.replaychart.gbx.E_TmVersion;
import de.reilem.replaychart.gbx.GbxInputExtractor;
import de.reilem.replaychart.gbx.GbxSteeringInput;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

public final class ReplayChart
{
    private boolean overlaySteering = false;
    private boolean donadigoInput   = false;
    private boolean invertSteering  = false;

    private List<ReplayData> replays = new ArrayList<>();

    /**
     * read data and show charts
     */
    public void init( List<String> arguments, boolean overlaySteering, boolean invertSteering, boolean isUiMode )
    {
        this.overlaySteering = overlaySteering;
        this.invertSteering = invertSteering;
        if ( donadigoInput )
        {
            if ( arguments.size() == 1 && new File( arguments.get( 0 ) ).isDirectory() ) //read all files in folder
            {
                String[] files = new File( arguments.get( 0 ) ).list();
                Arrays.stream( files )
                        .forEach( f -> replays.add( DonadigoReplayBuilder.buildReplay( arguments.get( 0 ) + "/" + f, overlaySteering ) ) );
            }
            else //read each given file
            {
                arguments.forEach( arg -> replays.add( DonadigoReplayBuilder.buildReplay( arg, overlaySteering ) ) );
            }
        }
        else // read gbx
        {
            if ( arguments.size() == 1 && new File( arguments.get( 0 ) ).isDirectory() ) //read all gbx in folder
            {
                String[] files = new File( arguments.get( 0 ) ).list();
                Arrays.stream( files ).forEach( f ->
                {
                    if ( f.toLowerCase().endsWith( ".replay.gbx" ) )
                    {
                        try
                        {
                            replays.add( GbxInputExtractor.parseReplayData( arguments.get( 0 ) + "/" + f, invertSteering ) );
                        }
                        catch ( Throwable e )
                        {
                            System.out.println( "Unable to extract input from: " + f );
                            e.printStackTrace();
                        }
                    }
                } );
            }
            else
            {
                arguments.forEach( arg ->
                {
                    try
                    {
                        replays.add( GbxInputExtractor.parseReplayData( arg, invertSteering ) );
                    }
                    catch ( Throwable e )
                    {
                        System.out.println( "Unable to extract input from: " + arg );
                        e.printStackTrace();
                    }
                } );
            }
        }

        if ( overlaySteering ) //overlay all datasets in one chart
        {
            XYChart steeringChart = new XYChartBuilder().width( 1440 ).height( 320 ).build();
            steeringChart.getStyler().setTheme( new ReplayTheme() );
            initChart( steeringChart, replays.get( 0 ) );
            steeringChart.getStyler().setLegendPosition( Styler.LegendPosition.InsideNW );
            replays.forEach( r ->
                    steeringChart.addSeries( r.getChartTitleShort(), r.getTimestamps(), r.getSteering() ).setMarker( SeriesMarkers.NONE ) );

            List<XYChart> charts = new ArrayList<>();
            charts.add( steeringChart );
            JFrame frame = new SwingWrapper( charts, charts.size(), 1 ).setTitle( "Replay Chart" ).displayChartMatrix();

            frame.setMinimumSize( new Dimension( 900, 320 ) );
            resizeLegend( frame, steeringChart, replays.size() );
            if ( isUiMode )
            {
                try
                {
                    frame.setIconImage( ImageIO.read(
                            Thread.currentThread().getContextClassLoader().getResourceAsStream( "icon.png" ) ) );
                }
                catch ( Throwable e )
                {
                    //ignore it
                }
                javax.swing.SwingUtilities.invokeLater(
                        () -> frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE )
                );
            }

            frame.addComponentListener( new ComponentAdapter()
            {
                public void componentResized( ComponentEvent evt )
                {
                    resizeLegend( frame, steeringChart, replays.size() );
                }
            } );
        }
        else //render every dataset in separate chart
        {
            List<XYChart> charts = new ArrayList<>();
            replays.forEach( r ->
            {
                XYChart chart = new XYChartBuilder().width( 1440 ).height( 200 ).build();
                chart.getStyler().setTheme( new ReplayTheme() );
                initChart( chart, r );
                chart.getStyler().setLegendVisible( false );
                chart.setTitle( r.getChartTitle() );

                if ( r.getAcceleration() != null )
                {
                    XYSeries accelerationSeries = chart.addSeries( "Acceleration", r.getTimestamps(), r.getAcceleration() );
                    accelerationSeries.setXYSeriesRenderStyle( XYSeries.XYSeriesRenderStyle.Area );
                    accelerationSeries.setMarker( SeriesMarkers.NONE );
                    accelerationSeries.setFillColor( new Color( 180, 255, 160 ) );
                    accelerationSeries.setLineColor( new Color( 0, 0, 0, 0 ) );
                }

                if ( !overlaySteering && r.getBrake() != null )
                {
                    XYSeries brakeSeries = chart.addSeries( "Brake", r.getTimestamps(), r.getBrake() );
                    brakeSeries.setXYSeriesRenderStyle( XYSeries.XYSeriesRenderStyle.Area );
                    brakeSeries.setMarker( SeriesMarkers.NONE );
                    brakeSeries.setFillColor( new Color( 255, 180, 160 ) );
                    brakeSeries.setLineColor( new Color( 0, 0, 0, 0 ) );
                }

                if ( !overlaySteering && r.getSteering() != null )
                {
                    XYSeries steeringSeries = chart.addSeries( "Steering", r.getTimestamps(), r.getSteering() );
                    steeringSeries.setLineWidth( 1.4f );
                    steeringSeries.setMarker( SeriesMarkers.NONE );
                    steeringSeries.setLineColor( Color.BLACK );
                }
                if ( !overlaySteering && r.getRespawns().size() > 0 )
                {
                    r.getRespawns().forEach( time -> drawRespawn( chart, time ) );
                }

                initCustomLegend( chart, r );
                charts.add( chart );
            } );

            JFrame frame = new SwingWrapper( charts, charts.size(), 1 ).setTitle( "Replay Chart" ).displayChartMatrix();
            frame.setMinimumSize( new Dimension( 1000, 160 * replays.size() ) );
            if ( isUiMode )
            {
                try
                {
                    frame.setIconImage( ImageIO.read(
                            Thread.currentThread().getContextClassLoader().getResourceAsStream( "icon.png" ) ) );
                }
                catch ( Throwable e )
                {
                    //ignore it
                }
                javax.swing.SwingUtilities.invokeLater(
                        () -> frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE )
                );
            }

            frame.addComponentListener( new ComponentAdapter()
            {
                public void componentResized( ComponentEvent evt )
                {
                    if ( frame.getHeight() > 300 * replays.size() )
                    {
                        frame.resize( new Dimension( frame.getWidth(), 300 * replays.size() ) );
                    }
                }
            } );
        }
    }

    private void drawRespawn( XYChart chart, Integer time )
    {
        AnnotationLine respwnLine = new AnnotationLine( time, true, false );
        respwnLine.setColor( new Color( 0, 150, 150, 150 ) );
        chart.addAnnotation( respwnLine );

        Font font = new Font( Font.MONOSPACED, 0, 7 );

        AnnotationText textR = new AnnotationText( "R", time + (time / 90), GbxSteeringInput.MAX - 10000, false );
        textR.setFontColor( new Color( 0, 100, 100, 180 ) );
        textR.setTextFont( font );
        chart.addAnnotation( textR );

        AnnotationText textE = new AnnotationText( "E", time + (time / 90), GbxSteeringInput.MAX - 17000, false );
        textE.setFontColor( new Color( 0, 100, 100, 180 ) );
        textE.setTextFont( font );
        chart.addAnnotation( textE );

        AnnotationText textS = new AnnotationText( "S", time + (time / 90), GbxSteeringInput.MAX - 24000, false );
        textS.setFontColor( new Color( 0, 100, 100, 180 ) );
        textS.setTextFont( font );
        chart.addAnnotation( textS );

        AnnotationText textP = new AnnotationText( "P", time + (time / 90), GbxSteeringInput.MAX - 31000, false );
        textP.setFontColor( new Color( 0, 100, 100, 180 ) );
        textP.setTextFont( font );
        chart.addAnnotation( textP );

        AnnotationText textA = new AnnotationText( "A", time + (time / 90), GbxSteeringInput.MAX - 38000, false );
        textA.setFontColor( new Color( 0, 100, 100, 180 ) );
        textA.setTextFont( font );
        chart.addAnnotation( textA );

        AnnotationText textW = new AnnotationText( "W", time + (time / 90), GbxSteeringInput.MAX - 45000, false );
        textW.setFontColor( new Color( 0, 100, 100, 180 ) );
        textW.setTextFont( font );
        chart.addAnnotation( textW );

        AnnotationText textN = new AnnotationText( "N", time + (time / 90), GbxSteeringInput.MAX - 52000, false );
        textN.setFontColor( new Color( 0, 100, 100, 180 ) );
        textN.setTextFont( font );
        chart.addAnnotation( textN );
    }

    /**
     * resizes the max/min of the x-axis to fit the legend in the graph
     *
     * @param frame
     * @param chart
     * @param size
     */
    private void resizeLegend( JFrame frame, XYChart chart, int size )
    {
        int height = frame.getHeight();
        if ( height < 400 )
        {
            chart.getStyler().setYAxisMax( GbxSteeringInput.MAX + ((10000000 * size) / frame.getHeight()) );
        }
        else if ( height < 500 )
        {
            chart.getStyler().setYAxisMax( GbxSteeringInput.MAX + ((7250000 * size) / frame.getHeight()) );
        }
        else
        {
            chart.getStyler().setYAxisMax( GbxSteeringInput.MAX + ((5000000 * size) / frame.getHeight()) );
        }
    }

    /**
     * add acceleration and brake legend
     *
     * @param chart
     * @param r
     */
    private void initCustomLegend( XYChart chart, ReplayData r )
    {
        AnnotationText accelerationLegend;
        AnnotationText brakeLegend;
        AnnotationText deviceLegend;
        AnnotationText timeOnLegend;
        double offset = (r.getReplayTime() / 100) * 2;

        accelerationLegend = new AnnotationText( "Throttle", r.getReplayTime() + offset, GbxSteeringInput.MAX - 10000, false );
        brakeLegend = new AnnotationText( "Brake", r.getReplayTime() + offset, GbxSteeringInput.MIN + 10000, false );

        deviceLegend = new AnnotationText(
                "Input: [" + r.getType() + "]   Time: [" + formatTime( (double) r.getReplayTime(), r.getTmVersion() ) + "]"
                , r.getReplayTime() / 6, GbxSteeringInput.MAX + 16000, false );

        timeOnLegend = new AnnotationText(
                r.getPercentTimeOnThrottle() + "   " + r.getPercentTimeOnBrake()
                , r.getReplayTime() - r.getReplayTime() / 6, GbxSteeringInput.MAX + 16000, false );

        accelerationLegend.setFontColor( new Color( 60, 150, 40 ) );
        brakeLegend.setFontColor( new Color( 200, 80, 60 ) );
        deviceLegend.setFontColor( Color.BLACK );
        timeOnLegend.setFontColor( Color.BLACK );

        chart.addAnnotation( accelerationLegend );
        chart.addAnnotation( brakeLegend );
        chart.addAnnotation( deviceLegend );
        chart.addAnnotation( timeOnLegend );
    }

    /**
     * style the given chart
     *
     * @param chart
     * @param r
     */
    private void initChart( XYChart chart, ReplayData r )
    {
        chart.setXAxisTitle( "Time (s)" );
        chart.setYAxisTitle( "Steering" );
        chart.getStyler().setYAxisTicksVisible( false );
        chart.getStyler().setYAxisTitleVisible( false );
        chart.getStyler().setXAxisTitleVisible( false );
        chart.getStyler().setYAxisMax( GbxSteeringInput.MAX );
        chart.getStyler().setYAxisMin( GbxSteeringInput.MIN );

        chart.getStyler().setxAxisTickLabelsFormattingFunction( aDouble -> formatTimeFullSecond( aDouble, r ) );

        double offset = -(r.getReplayTime() / 100) * 3;
        if ( invertSteering )
        {
            chart.addAnnotation( new AnnotationText( "Left", offset, GbxSteeringInput.MAX - 10000, false ) );
            chart.addAnnotation( new AnnotationText( "Right", offset, GbxSteeringInput.MIN + 10000, false ) );
        }
        else
        {
            chart.addAnnotation( new AnnotationText( "Right", offset, GbxSteeringInput.MAX - 10000, false ) );
            chart.addAnnotation( new AnnotationText( "Left", offset, GbxSteeringInput.MIN + 10000, false ) );
        }
        chart.addAnnotation( new AnnotationText( "Steering", offset, 0, false ) );
        chart.getStyler().setXAxisMin( r.getReplayTime() < 30000 ? -500.0 : -1000.0 );

        //min max lines
        AnnotationLine maxY = new AnnotationLine( GbxSteeringInput.MAX, false, false );
        maxY.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( maxY );

        AnnotationLine minY = new AnnotationLine( GbxSteeringInput.MIN, false, false );
        minY.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( minY );

        AnnotationLine zeroY = new AnnotationLine( 0, false, false );
        zeroY.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( zeroY );

        AnnotationLine minX = new AnnotationLine( 0, true, false );
        minX.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( minX );

        AnnotationLine maxX = new AnnotationLine( r.getReplayTime(), true, false );
        maxX.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( maxX );
    }

    /**
     * formats the time
     *
     * @param aDouble
     * @return
     */
    public static String formatTime( Double aDouble, E_TmVersion tmVersion )
    {
        String value;
        if ( tmVersion != E_TmVersion.TM2 )
        {
            value = aDouble.toString().replaceAll( "0\\.0", "" ); //cut the 0.0

            int length = value.length();
            if ( length < 3 )
            {
                return value;
            }
            return value.substring( 0, length - 2 ) + "." + value.substring( length - 2 );
        }
        else
        {
            value = aDouble.toString().replaceAll( "\\.0", "" );

            int length = value.length();
            if ( length < 4 )
            {
                return value;
            }
            return value.substring( 0, length - 3 ) + "." + value.substring( length - 3 );
        }
    }

    /**
     * formats the time - full second
     *
     * @param aDouble
     * @param r
     * @return
     */
    public static String formatTimeFullSecond( Double aDouble, ReplayData r )
    {
        String value = aDouble.toString();
        if ( value.equals( "0.0" ) )
        {
            return "0 s";
        }
        else if ( value.startsWith( "-" ) )
        {
            return " ";
        }

        value = value.replaceAll( "0\\.0", "" ); //cut the 0.0
        int length = value.length();

        if ( length < 3 )
        {
            return value;
        }
        return value.substring( 0, length - 2 ) + " s";
    }

    /**
     * rounds to two decimal places
     *
     * @param value
     * @return
     */
    public static double roundDoubleTwoDecimalPlaces( double value )
    {
        try
        {
            BigDecimal bd = BigDecimal.valueOf( value );
            bd = bd.setScale( 2, RoundingMode.HALF_UP );
            return bd.doubleValue();
        }
        catch ( Throwable e )
        {
            return 0.0;
        }
    }
}
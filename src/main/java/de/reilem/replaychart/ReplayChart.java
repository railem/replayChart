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
import java.util.concurrent.TimeUnit;

public final class ReplayChart
{
    private boolean overlaySteering = false;
    private boolean donadigoInput   = false;
    private boolean invertSteering  = false;
    private boolean matchTimeline = false;

    private List<ReplayData> replays = new ArrayList<>();

    /**
     * read data and show charts
     */
    public void init( List<String> arguments, boolean overlaySteering, boolean invertSteering, boolean isUiMode, boolean matchTimeline )
    {
        this.overlaySteering = overlaySteering;
        this.invertSteering = invertSteering;
        this.matchTimeline = matchTimeline;
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
            initChart( steeringChart, replays.get( 0 ), -1, -1 );
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

            int fastestTime = replays.get( 0 ).getReplayTime();
            int slowestTime = replays.get( 0 ).getReplayTime();
            for( ReplayData r : replays )
            {
                fastestTime = r.getReplayTime() < fastestTime ? r.getReplayTime() : fastestTime;
                slowestTime = r.getReplayTime() > slowestTime ? r.getReplayTime() : slowestTime;
            }

            List<Double> timestampsLong = new ArrayList<>();
            if( matchTimeline ) //create new timestamp list equal for all replays
            {
                double tsIndex = 0.0;
                while ( tsIndex < slowestTime )
                {
                    timestampsLong.add( tsIndex );
                    tsIndex += 10;
                }
            }

            //sort by time
            Collections.sort( replays, ( r1, r2 ) -> r1.getReplayTime() == r2.getReplayTime() ? 0 : r1.getReplayTime() < r2.getReplayTime() ? -1 : 1 );

            for( ReplayData r : replays )
            {
                XYChart chart = new XYChartBuilder().width( 1440 ).height( 200 ).build();
                chart.getStyler().setTheme( new ReplayTheme() );
                initChart( chart, r, fastestTime, charts.size()  );
                chart.getStyler().setLegendVisible( false );
                chart.setTitle( r.getChartTitle() );

                if ( r.getAcceleration() != null )
                {
                    if( r.getTmVersion() == E_TmVersion.TM2 )
                    {
                        slowestTime = slowestTime - ( slowestTime % 10 );
                    }
                    if( matchTimeline && r.getReplayTime() < slowestTime )
                    {
                        int index = r.getReplayTime();

                        while( index < slowestTime )
                        {
                            r.addAcceleration( 0.0 );
                            index += 10;
                        }
                    }

                    //create chart, use universal timestampList in align mode
                    XYSeries accelerationSeries = chart.addSeries( "Acceleration", matchTimeline ? ReplayData.listToArray( timestampsLong ) : r.getTimestamps(), r.getAcceleration() );
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

                initCustomLegend( chart, r, matchTimeline ? slowestTime : r.getReplayTime() );
                charts.add( chart );
            }

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
        respwnLine.setColor( new Color( 0, 100, 100, 180 ) );
        chart.addAnnotation( respwnLine );

        AnnotationText textR = new AnnotationText( "    █ Respawn", time, GbxSteeringInput.MAX + 33000, false );
        textR.setFontColor( new Color( 0, 100, 100, 255 ) );
        textR.setTextFont( new Font( Font.MONOSPACED, 0, 10 ) );
        chart.addAnnotation( textR );
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
     *  @param chart
     * @param r
     * @param time
     */
    private void initCustomLegend( XYChart chart, ReplayData r, int time )
    {
        AnnotationText accelerationLegend;
        AnnotationText brakeLegend;
        AnnotationText deviceLegend;
        AnnotationText timeOnLegend;
        double offset = (r.getReplayTime() / 100) * 2;

        accelerationLegend = new AnnotationText( "Throttle", time + offset, GbxSteeringInput.MAX - 10000, false );
        brakeLegend = new AnnotationText( "Brake", time + offset, GbxSteeringInput.MIN + 10000, false );

        deviceLegend = new AnnotationText(
                "Input: [" + r.getType() + "]   Time: [" + formatTime( (double) r.getReplayTime(), r.getTmVersion() ) + "]"
                , time / 6, GbxSteeringInput.MAX + 16000, false );

        timeOnLegend = new AnnotationText(
                r.getPercentTimeOnThrottle() + "   " + r.getPercentTimeOnBrake()
                , r.getReplayTime() - time / 6, GbxSteeringInput.MAX + 16000, false );

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
    private void initChart( XYChart chart, ReplayData r, int fastestTime, int replayIndex )
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

        if( fastestTime != -1 )
        {
            AnnotationLine fastestTimeLine = new AnnotationLine( fastestTime, true, false );
            fastestTimeLine.setColor( new Color( 70, 0, 160, 170 ) );
            fastestTimeLine.setStroke( new BasicStroke(1.0f) );
            chart.addAnnotation( fastestTimeLine );

            if( replayIndex == 0 )// fastest replay
            {
                String spacer = r.getTmVersion() == E_TmVersion.TM2 ? "    " : "    ";
                AnnotationText fastestTimeText = new AnnotationText( spacer + "█ " + formatTime( (double) r.getReplayTime(), r.getTmVersion() ) + "",
                        fastestTime, GbxSteeringInput.MAX + 33000, false );
                fastestTimeText.setFontColor( new Color( 60, 0, 130, 255 ) );
                fastestTimeText.setTextFont( new Font( Font.MONOSPACED, 0, 10 ) );
                chart.addAnnotation( fastestTimeText );
            }
        }
    }

    /**
     * formats the time
     *
     * @param aDouble
     * @return
     */
    public static String formatTime( Double aDouble, E_TmVersion tmVersion )
    {
        long m = TimeUnit.MILLISECONDS.toMinutes( aDouble.intValue() );
        long s = TimeUnit.MILLISECONDS.toSeconds( aDouble.intValue() ) - TimeUnit.MINUTES.toSeconds( m );
        long ms = aDouble.intValue() - (TimeUnit.MINUTES.toMillis( m ) + TimeUnit.SECONDS.toMillis( s ));

        if( tmVersion != E_TmVersion.TM2 )
        {
            ms = ms/10;
        }

        return String.format( "%d:%d.%d", m, s, ms );
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
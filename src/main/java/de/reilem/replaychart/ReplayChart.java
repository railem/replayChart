package de.reilem.replaychart;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

public final class ReplayChart
{
    private boolean overlaySteering = false;
    private boolean invertSteering  = false;

    private double max_steering = 65536.0;
    private double min_steering = -65536.0;

    private List<ReplayData> replays = new ArrayList<>();

    public static void main( String[] args )
    {
        new ReplayChart().init( args );
    }

    /**
     * read data and show charts
     *
     * @param args
     */
    private void init( String[] args )
    {
        List<String> arguments = new ArrayList<>( Arrays.asList( args ) );

        if ( arguments.contains( "-o" ) ) //check for overlay mode
        {
            overlaySteering = true;
            arguments.remove( "-o" );
        }
        if ( arguments.contains( "-i" ) ) //check for inverted steering mode
        {
            invertSteering = true;
            max_steering = -65536.0; //invert
            min_steering = 65536.0; //invert
            arguments.remove( "-i" );
        }

        if ( arguments.size() == 1 && new File( arguments.get( 0 ) ).isDirectory() ) //read all files in folder
        {
            String[] files = new File( arguments.get( 0 ) ).list();
            Arrays.stream( files ).forEach( f -> replays.add( createDataset( arguments.get( 0 ) + "/" + f ) ) );
        }
        else //read each given file
        {
            arguments.forEach( arg -> replays.add( createDataset( arg ) ) );
        }

        if ( overlaySteering ) //overlay all datasets in one chart
        {
            XYChart steeringChart = new XYChartBuilder().width( 1440 ).height( 320 ).build();
            steeringChart.getStyler().setTheme( new ReplayTheme() );
            initChart( steeringChart, replays.get( 0 ) );
            steeringChart.getStyler().setLegendPosition( Styler.LegendPosition.InsideNW );
            replays.forEach( r ->
                    steeringChart.addSeries( r.getChartTitleShort(), r.getTimestamps(), r.getSteering() ).setMarker( SeriesMarkers.NONE ) );
            JFrame frame = new SwingWrapper( steeringChart ).displayChart();
            frame.setMinimumSize( new Dimension( 900, 320 ) );
            resizeLegend( frame, steeringChart, replays.size() );

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

                if ( r.getBrake() != null )
                {
                    XYSeries brakeSeries = chart.addSeries( "Brake", r.getTimestamps(), r.getBrake() );
                    brakeSeries.setXYSeriesRenderStyle( XYSeries.XYSeriesRenderStyle.Area );
                    brakeSeries.setMarker( SeriesMarkers.NONE );
                    brakeSeries.setFillColor( new Color( 255, 180, 160 ) );
                    brakeSeries.setLineColor( new Color( 0, 0, 0, 0 ) );
                }

                if ( r.getSteering() != null )
                {
                    XYSeries steeringSeries = chart.addSeries( "Steering", r.getTimestamps(), r.getSteering() );
                    steeringSeries.setLineWidth( 1.4f );
                    steeringSeries.setMarker( SeriesMarkers.NONE );
                    steeringSeries.setLineColor( Color.BLACK );
                }

                initCustomLegend( chart, r );
                charts.add( chart );
            } );

            JFrame frame = new SwingWrapper( charts, charts.size(), 1 ).setTitle( "Replay Chart" ).displayChartMatrix();
            frame.setMinimumSize( new Dimension( 900, 200 * replays.size() ) );
        }
    }

    /**
     * resizes the max/min of the x-axis to fit the legend in the graph
     *  @param frame
     * @param chart
     * @param size
     */
    private void resizeLegend( JFrame frame, XYChart chart, int size )
    {
        int height = frame.getHeight();
        if ( !invertSteering )
        {
            if ( height < 400 )
            {
                chart.getStyler().setYAxisMax( max_steering + ( ( 10000000 * size ) / frame.getHeight()) );
            }
            else if ( height < 500 )
            {
                chart.getStyler().setYAxisMax( max_steering + ( ( 7250000 * size ) / frame.getHeight()) );
            }
            else
            {
                chart.getStyler().setYAxisMax( max_steering + ( ( 5000000 * size ) / frame.getHeight()) );
            }
        }
        else
        {
            if ( height < 400 )
            {
                chart.getStyler().setYAxisMax( max_steering - ( ( 10000000 * size ) / frame.getHeight()) );
            }
            else if ( height < 500 )
            {
                chart.getStyler().setYAxisMax( max_steering - ( ( 7250000 * size ) / frame.getHeight()) );
            }
            else
            {
                chart.getStyler().setYAxisMax( max_steering - ( ( 5000000 * size ) / frame.getHeight()) );
            }
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
        double offset = r.getReplayTime() < 30000 ? 500 : 1100;

        if ( !invertSteering )
        {
            accelerationLegend = new AnnotationText( "Throttle", r.getReplayTime() + offset, max_steering - 10000, false );
            brakeLegend = new AnnotationText( "Brake", r.getReplayTime() + offset, min_steering + 10000, false );

            deviceLegend = new AnnotationText(
                    "Device: [" + r.getType().name() + "]   Time: [" + formatTime( (double) r.getReplayTime() ) + "]"
                    , r.getReplayTime() / 6, max_steering + 16000, false );

            timeOnLegend = new AnnotationText(
                    r.getPercentTimeOnThrottle() + "   " + r.getPercentTimeOnBrake()
                    , r.getReplayTime() - r.getReplayTime() / 6, max_steering + 16000, false );
        }
        else
        {
            accelerationLegend = new AnnotationText( "Throttle", r.getReplayTime() + offset, max_steering + 10000, false );
            brakeLegend = new AnnotationText( "Brake", r.getReplayTime() + offset, min_steering - 10000, false );

            deviceLegend = new AnnotationText(
                    "Device: [" + r.getType().name() + "]   Time: [" + formatTime( (double) r.getReplayTime() ) + "]"
                    , r.getReplayTime() / 6, max_steering - 16000, false );

            timeOnLegend = new AnnotationText(
                    r.getPercentTimeOnThrottle() + "   " + r.getPercentTimeOnBrake()
                    , r.getReplayTime() - r.getReplayTime() / 6, max_steering - 16000, false );
        }
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
        chart.getStyler().setYAxisMax( max_steering );
        chart.getStyler().setYAxisMin( min_steering );

        chart.getStyler().setxAxisTickLabelsFormattingFunction( aDouble -> formatTimeFullSecond( aDouble, r ) );

        double offset = r.getReplayTime() < 30000 ? -800 : -1600;
        if ( !invertSteering )
        {
            chart.addAnnotation( new AnnotationText( "Right", offset, max_steering - 10000, false ) );
            chart.addAnnotation( new AnnotationText( "Left", offset, min_steering + 10000, false ) );
        }
        else
        {
            chart.addAnnotation( new AnnotationText( "Left", offset, max_steering + 10000, false ) );
            chart.addAnnotation( new AnnotationText( "Right", offset, min_steering - 10000, false ) );
        }
        chart.addAnnotation( new AnnotationText( "Steering", offset, 0, false ) );
        chart.getStyler().setXAxisMin( r.getReplayTime() < 30000 ? -500.0 : -1000.0 );

        //min max lines
        AnnotationLine maxY = new AnnotationLine( max_steering , false, false );
        maxY.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( maxY );

        AnnotationLine minY = new AnnotationLine( min_steering, false, false );
        minY.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( minY );

        AnnotationLine minX = new AnnotationLine( 0, true, false );
        minX.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( minX );

        AnnotationLine maxX = new AnnotationLine( r.getReplayTime(), true, false );
        maxX.setColor( new Color( 0, 0, 0, 40 ) );
        chart.addAnnotation( maxX );
    }

    /**
     * read the given file line by line and extract timestamps and steering
     *
     * @param fileName
     * @return
     */
    private ReplayData createDataset( String fileName )
    {
        ReplayData replayData = new ReplayData();

        List<String> accelerationList = new ArrayList<>();
        List<String> brakeList = new ArrayList<>();
        List<SteeringAction> steeringList = new ArrayList<>();

        int padSteeringCounter = 0;
        int keyboardSteeringCounter = 0;

        File file = new File( fileName );
        try ( BufferedReader br = new BufferedReader( new FileReader( file ) ) )
        {
            String line;
            while ( (line = br.readLine()) != null )
            {
                String[] parts = line.split( " " );

                if ( isInteger( parts[0] ) ) //pad steering
                {
                    steeringList.add( new SteeringAction( parts[0], parts[2] ) );
                    padSteeringCounter++;
                }
                else if ( parts[0].contains( "-" ) && (parts[2].equals( "left" ) || parts[2].equals( "right" )) ) //keyboard steering
                {
                    steeringList.add( new SteeringAction( parts[0], parts[2] ) );
                    keyboardSteeringCounter++;
                }
                else if ( parts[2].equals( "up" ) ) //acceleration
                {
                    accelerationList.add( parts[0] );
                }
                else if ( parts[2].equals( "down" ) ) //brake
                {
                    brakeList.add( parts[0] );
                }
            }

            calculateReplayLength( steeringList, accelerationList, brakeList, replayData );

            for ( int i = 0; i < replayData.getReplayTime(); i = i + 10 ) //add a timestamp for the x-axis for every 10ms to please XChart
            {
                replayData.addTimestamp( i );
            }

            if ( padSteeringCounter != 0 && keyboardSteeringCounter != 0 )
            {
                System.err.println( "Mixed ( Pad + Keyboard ) runs are not supported yet!" );
                return new ReplayData();
            }
            else if ( padSteeringCounter > keyboardSteeringCounter )
            {
                replayData.setType( E_SteeringType.PAD );
                extractPadSteering( steeringList, replayData );
            }
            else
            {
                replayData.setType( E_SteeringType.KEYBOARD );
                extractKeyboardSteering( steeringList, replayData );
            }

            if ( !overlaySteering ) // don't show acceleration and brake in overlay mode
            {
                extractAccelerationOrBrake( true, accelerationList, replayData );
                extractAccelerationOrBrake( false, brakeList, replayData );
            }

            String[] fileNameParts = fileName.split( "/" );
            replayData.setFileName( fileNameParts[fileNameParts.length - 1] );
        }
        catch ( Throwable e )
        {
            e.printStackTrace();
        }

        return replayData;
    }

    /**
     * extracts the steering inputs
     *
     * @param steeringList
     * @param replayData
     */
    private void extractPadSteering( List<SteeringAction> steeringList, ReplayData replayData )
    {
        int lastTimeStamp = 0;
        double lastSteering = 0.0;

        for ( SteeringAction input : steeringList )
        {
            int currentTime = Integer.parseInt( input.getTime() );

            while ( lastTimeStamp + 10 < currentTime ) //timestamps are missing
            {
                replayData.addSteering( lastSteering );
                lastTimeStamp = lastTimeStamp + 10;
            }

            lastSteering = Integer.parseInt( input.getValue() );
            replayData.addSteering( lastSteering );
            lastTimeStamp = currentTime;
        }

        while ( replayData.getSteeringLegth() * 10 < replayData.getReplayTime() ) //fill rest of the replay with same steering
        {
            replayData.addSteering( lastSteering );
        }
    }

    /**
     * extracts the steering input
     *
     * @param steeringList
     * @param replayData
     */
    private void extractKeyboardSteering( List<SteeringAction> steeringList, ReplayData replayData )
    {
        int lastTimeStamp = 0;

        for ( SteeringAction input : steeringList )
        {
            double steer = 0;

            int startTime = Integer.parseInt( input.getTime().split( "-" )[0] );
            int endTime = Integer.parseInt( input.getTime().split( "-" )[1] );

            while ( lastTimeStamp + 10 < startTime ) //fill the time that passed since the last steering command with no steering
            {
                replayData.addSteering( steer );
                lastTimeStamp = lastTimeStamp + 10;
            }

            if ( startTime <= lastTimeStamp ) //left and right are pressed at the same time
            {
                startTime = lastTimeStamp + 10; //start when only one button is pressed
                //TODO not accurate, since pressing both left and right at the same time results in going LEFT
            }

            steer = input.getValue().equals( "left" ) ? -65536 : 65536; // keyboards can only fullsteer
            for ( int i = startTime; i <= endTime; i = i + 10 )
            {
                replayData.addSteering( steer );
            }
            if ( endTime > lastTimeStamp )
            {
                lastTimeStamp = endTime;
            }
        }

        while ( replayData.getSteeringLegth() * 10 < replayData.getReplayTime() ) //fill rest of the replay with no steering
        {
            replayData.addSteering( 0.0 );
        }
    }

    /**
     * finds the latest timestamp in the input
     *
     * @param steeringList
     * @param accelerationList
     * @param brakeList
     * @param replayData
     */
    private void calculateReplayLength( List<SteeringAction> steeringList, List<String> accelerationList, List<String> brakeList,
            ReplayData replayData )
    {
        SteeringAction lastSteerPair = steeringList.get( steeringList.size() - 1 );
        String lastAccelerationString = accelerationList.get( accelerationList.size() - 1 );
        String lastBrakeString = brakeList.get( brakeList.size() - 1 );

        int lastAcceleration = Integer.parseInt( lastAccelerationString.split( "-" )[1] );
        int lastBrake = Integer.parseInt( lastBrakeString.split( "-" )[1] );

        int lastSteer = 0;
        if ( lastSteerPair.getTime().contains( "-" ) ) // keyboard
        {
            lastSteer = Integer.parseInt( lastSteerPair.getTime().split( "-" )[1] );
        }
        else //pad
        {
            lastSteer = Integer.parseInt( lastSteerPair.getTime() );
        }

        replayData.setReplayTime( Math.max( Math.max( lastAcceleration, lastBrake ), lastSteer ) );
    }

    /**
     * extract the brake / acceleration inputs
     *
     * @param isAcceleration
     * @param inputList
     * @param replayData
     */
    private void extractAccelerationOrBrake( boolean isAcceleration, List<String> inputList, ReplayData replayData )
    {
        int lastTime = 0;

        if ( inputList.isEmpty() && isAcceleration )
        {
            inputList.add( "0-0" ); //ESWC runs sometimes? don't have acceleration at all, assume it was pressed all the time
        }

        for ( String s : inputList )
        {
            int startTime = Integer.parseInt( s.split( "-" )[0] );
            int endTime = Integer.parseInt( s.split( "-" )[1] );

            if ( lastTime < startTime ) //input doesn't start at 0 || input hasn't been pressed in a while
            {
                while ( lastTime < startTime )
                {
                    if ( isAcceleration )
                    {
                        replayData.addAcceleration( 0.0 );
                    }
                    else
                    {
                        replayData.addBrake( 0.0 );
                    }
                    lastTime = lastTime + 10;
                }
            }

            //ESWC runs that press acceleration in the complete run show 0-0
            if ( endTime == 0 || endTime > replayData.getReplayTime() )
            {
                endTime = replayData.getReplayTime();
            }

            if ( endTime != 0 ) //key is not pressed until the end of the run
            {
                while ( lastTime < endTime )
                {
                    if ( isAcceleration )
                    {
                        replayData.addAcceleration( max_steering );
                        replayData.addThrottle();
                    }
                    else
                    {
                        replayData.addBrake( min_steering );
                        replayData.addBrake();
                    }

                    lastTime = lastTime + 10;
                }
            }

            if ( inputList.indexOf( s ) == inputList.size() - 1 && endTime < replayData
                    .getReplayTime() ) //is the last input && run is not over yet
            {
                while ( lastTime < replayData.getReplayTime() ) //fill rest of the run with no input
                {
                    if ( isAcceleration )
                    {
                        replayData.addAcceleration( 0.0 );
                    }
                    else
                    {
                        replayData.addBrake( 0.0 );
                    }
                    lastTime = lastTime + 10;
                }
            }
        }
    }

    /**
     * checks if string is integer
     *
     * @param s
     * @return
     */
    public static boolean isInteger( String s )
    {
        try
        {
            Integer.parseInt( s );
        }
        catch ( Throwable e )
        {
            return false;
        }
        return true;
    }

    /**
     * formats the time
     *
     * @param aDouble
     * @return
     */
    public static String formatTime( Double aDouble )
    {
        String value = aDouble.toString().replaceAll( "0\\.0", "" ); //cut the 0.0
        int length = value.length();

        if ( length < 3 )
        {
            return value;
        }
        return value.substring( 0, length - 2 ) + "." + value.substring( length - 2 );
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
        else if( value.startsWith( "-" ) )
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
     * @param value
     * @return
     */
    public static double roundDoubleTwoDecimalPlaces( double value )
    {
        BigDecimal bd = BigDecimal.valueOf( value );
        bd = bd.setScale( 2, RoundingMode.HALF_UP );
        return bd.doubleValue();
    }
}
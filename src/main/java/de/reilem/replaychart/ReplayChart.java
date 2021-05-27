package de.reilem.replaychart;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public final class ReplayChart
{
    private boolean overlaySteering = false;
    private boolean invertSteering = false;

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
            XYChart chart = new XYChartBuilder().width( 1440 ).height( 300 ).build();
            initChart( chart );
            replays.forEach( r -> chart.addSeries( r.getTitle(), r.getTimestamps(), r.getSteering() ).setMarker( SeriesMarkers.NONE ) );
            new SwingWrapper( chart ).displayChart();
        }
        else //render every dataset in separate chart
        {
            List<XYChart> charts = new ArrayList<>();
            replays.forEach( r ->
            {
                XYChart chart = new XYChartBuilder().width( 1440 ).height( 200 ).theme( Styler.ChartTheme.XChart).build();
                initChart( chart );
                chart.getStyler().setLegendVisible( false );
                chart.setTitle( r.getTitle() );

                XYSeries accelerationSeries = chart.addSeries( "Acceleration", r.getTimestamps(), r.getAcceleration() );
                accelerationSeries.setXYSeriesRenderStyle( XYSeries.XYSeriesRenderStyle.Area );
                accelerationSeries.setMarker( SeriesMarkers.NONE );
                accelerationSeries.setFillColor( new Color( 180, 255, 160 ) );
                accelerationSeries.setLineColor( new Color( 0, 0, 0, 0 ) );

                XYSeries brakeSeries = chart.addSeries( "Brake", r.getTimestamps(), r.getBrake() );
                brakeSeries.setXYSeriesRenderStyle( XYSeries.XYSeriesRenderStyle.Area );
                brakeSeries.setMarker( SeriesMarkers.NONE );
                brakeSeries.setFillColor( new Color( 255, 180, 160 ) );
                brakeSeries.setLineColor( new Color( 0, 0, 0, 0 ) );

                XYSeries steeringSeries = chart.addSeries( "Steering", r.getTimestamps(), r.getSteering() );
                steeringSeries.setMarker( SeriesMarkers.NONE );
                steeringSeries.setLineColor( Color.BLACK );

                initCustomLegend( chart );

                charts.add( chart );
            } );

            new SwingWrapper( charts, charts.size(), 1 ).setTitle( "Replay Chart" ).displayChartMatrix();
        }
    }

    /**
     * add acceleration and brake legend
     *
     * @param chart
     */
    private void initCustomLegend( XYChart chart )
    {
        AnnotationText accelerationLegend;
        AnnotationText brakeLegend;

        if( !invertSteering )
        {
            accelerationLegend = new AnnotationText( "Throttle", -1000, max_steering + 16000, false );
            brakeLegend = new AnnotationText( "Brake", -1000, min_steering - 22000, false );
        }
        else
        {
            accelerationLegend = new AnnotationText( "Throttle", -1000, max_steering - 16000, false );
            brakeLegend = new AnnotationText( "Brake", -1000, min_steering + 22000, false );
        }

        accelerationLegend.setFontColor( new Color( 60, 150, 40 ) );
        brakeLegend.setFontColor( new Color( 200, 80, 60 ) );

        chart.addAnnotation( accelerationLegend );
        chart.addAnnotation( brakeLegend );
    }

    /**
     * style the given chart
     *
     * @param chart
     */
    private void initChart( XYChart chart )
    {
        chart.setXAxisTitle( "Time (s)" );
        chart.setYAxisTitle( "Steering" );
        chart.getStyler().setYAxisTicksVisible( false );
        chart.getStyler().setYAxisMax( max_steering );
        chart.getStyler().setYAxisMin( min_steering );
        chart.getStyler().setZoomEnabled( true );
        chart.getStyler().setXAxisLabelRotation(30);

        chart.getStyler().setxAxisTickLabelsFormattingFunction( aDouble ->
        {
            String value = aDouble.toString();
            if ( value.equals( "0.0" ) )
            {
                return "0";
            }

            value = value.replaceAll( "0\\.0", "" ); //cut the 0.0
            int length = value.length();

            if( length < 3 )
            {
                return value;
            }
            return value.substring( 0, length - 2 ) + "." + value.substring( length - 2 );
        });

        if( !invertSteering )
        {
            chart.addAnnotation( new AnnotationText( "Right", -1000, max_steering - 10000, false ) );
            chart.addAnnotation( new AnnotationText( "Left", -1000, min_steering + 10000, false ) );
        }
        else
        {
            chart.addAnnotation( new AnnotationText( "Left", -1000, max_steering + 10000, false ) );
            chart.addAnnotation( new AnnotationText( "Right", -1000, min_steering - 10000, false ) );
        }
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
        int lastTimeStamp = 0;
        double lastSteering = 0.0;

        List<String> accelerationList = new ArrayList<>();
        List<String> brakeList = new ArrayList<>();

        File file = new File( fileName );
        try ( BufferedReader br = new BufferedReader( new FileReader( file ) ) )
        {
            String line;
            while ( (line = br.readLine()) != null )
            {
                String[] parts = line.split( " " );

                if ( isInteger( parts[0] ) ) //pad steering
                {
                    replayData.setType( "Pad" );
                    int currentTime = Integer.parseInt( parts[0] );
                    if ( currentTime > lastTimeStamp + 10 ) //timestamps are missing
                    {
                        while ( lastTimeStamp + 10 < currentTime )
                        {
                            replayData.addSteering( lastSteering );
                            lastTimeStamp = lastTimeStamp + 10;
                        }
                    }

                    lastSteering = Integer.parseInt( parts[2] );
                    replayData.addSteering( lastSteering );
                    lastTimeStamp = currentTime;
                }
                else if ( parts[0].contains( "-" ) && (parts[2].equals( "left" ) || parts[2].equals( "right" )) ) //keyboard steering
                {
                    replayData.setType( "Keyboard" );
                    int startTime = Integer.parseInt( parts[0].split( "-" )[0] );
                    int endTime = Integer.parseInt( parts[0].split( "-" )[1] );
                    double steer = 0;

                    while ( lastTimeStamp + 10 < startTime ) //fill the time that passed since the last steering command with no steering
                    {
                        replayData.addSteering( steer );
                        lastTimeStamp = lastTimeStamp + 10;
                    }

                    steer = parts[2].equals( "left" ) ? -65536 : 65536; // keyboards can only fullsteer
                    for ( int i = startTime; i <= endTime; i = i + 10 )
                    {
                        replayData.addSteering( steer );
                    }
                    if ( endTime > lastTimeStamp )
                    {
                        lastTimeStamp = endTime;
                    }
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

            int totalTime = replayData.getSteeringLegth() * 10;
            for ( int i = 0; i < totalTime; i = i + 10 ) //add a timestamp for the x-axis for every 10ms to please XChart
            {
                replayData.addTimestamp( i );
            }

            if( !overlaySteering ) // don't show acceleration and brake in overlay mode
            {
                extractAccelerationOrBrake( true, accelerationList, replayData, totalTime );
                extractAccelerationOrBrake( false, brakeList, replayData, totalTime );
            }

            String[] fileNameParts = fileName.split( "/" );
            replayData.setName( fileNameParts[fileNameParts.length - 1] );
        }
        catch ( Throwable e )
        {
            e.printStackTrace();
        }

        return replayData;
    }

    private void extractAccelerationOrBrake( boolean isAcceleration, List<String> inputList, ReplayData replayData, int totalTime )
    {
        int lastTime = 0;

        if ( inputList.isEmpty() && isAcceleration )
        {
            inputList.add( "0-0" ); //some runs don't have acceleration at all, assume it was pressed all the time
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

            if ( endTime == 0 || endTime > totalTime ) //key is pressed until the end of the run
            {
                endTime = totalTime;
            }

            if ( endTime != 0 ) //key is not pressed until the end of the run
            {
                while ( lastTime < endTime )
                {
                    if ( isAcceleration )
                    {
                        replayData.addAcceleration( max_steering );
                    }
                    else
                    {
                        replayData.addBrake( min_steering );
                    }

                    lastTime = lastTime + 10;
                }
            }

            if ( inputList.indexOf( s ) == inputList.size() - 1 && endTime < totalTime ) //is the last input && run is not over yet
            {
                while ( lastTime < totalTime ) //fill rest of the run with no input
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
}
package de.reilem.replaychart;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public final class ReplayChart
{

    private boolean overlayMode = false;
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
        List<String> arguments = new ArrayList<>(Arrays.asList( args ));

        if( arguments.contains( "-overlay" ) ) //check for overlay mode
        {
            overlayMode = true;
            arguments.remove( "-overlay" );
        }

        if( arguments.size() == 1 && new File( arguments.get( 0 ) ).isDirectory() ) //read all files in folder
        {
            String[] files = new File( arguments.get( 0 ) ).list();
            Arrays.stream( files ).forEach( f -> replays.add( createDataset( arguments.get( 0 ) + "/" + f ) ) );
        }
        else //read each given file
        {
            arguments.forEach( arg -> replays.add( createDataset( arg ) ) );
        }

        if( overlayMode ) //overlay all datasets in one chart
        {
            XYChart chart = new XYChartBuilder().width( 1440 ).height( 800 ).build();
            initChart(chart);
            replays.forEach( r -> chart.addSeries( r.getTitle(), r.getTimestamps(), r.getSteering() ).setMarker( SeriesMarkers.NONE ) );
            new SwingWrapper( chart ).displayChart();
        }
        else //render every dataset in separate chart
        {
            List<XYChart> charts = new ArrayList<>();
            replays.forEach( r ->
            {
                XYChart chart = new XYChartBuilder().width( 1440 ).height( 200 ).build();
                initChart(chart);
                chart.getStyler().setLegendVisible(false);
                chart.setTitle( r.getTitle() );

                chart.addSeries( " ", r.getTimestamps(), r.getSteering() ).setMarker( SeriesMarkers.NONE );
                charts.add( chart );
            });

            new SwingWrapper( charts, charts.size(),1 ).setTitle( "Replay Steering Chart" ).displayChartMatrix();
        }
    }

    /**
     * style the given chart
     *
     * @param chart
     */
    private void initChart( XYChart chart )
    {
        chart.setXAxisTitle( "Time (ms)" );
        chart.setYAxisTitle( " < Left | Right >" );
        chart.getStyler().setYAxisMax( 65536.0 );
        chart.getStyler().setYAxisMin( -65536.0 );
        chart.getStyler().setYAxisTicksVisible( false );
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

        File file = new File( fileName );
        try ( BufferedReader br = new BufferedReader( new FileReader( file ) ) )
        {
            String line;
            while ( (line = br.readLine()) != null )
            {
                String[] parts = line.split( " " );

                if ( parts[0] == "0" ) //start of replay
                {
                    replayData.addSteering( 0.0 );
                }
                else if ( isInteger( parts[0] ) ) // takes out acceleration and brake
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
                else if ( parts[0].contains( "-" ) && ( parts[2].equals( "left" ) || parts[2].equals( "right" ) ) ) //check for keyboard input
                {
                    replayData.setType( "Keyboard" );
                    int startTime = Integer.parseInt( parts[0].split( "-" )[0] );
                    int endTime = Integer.parseInt( parts[0].split( "-" )[1] );
                    double steer = 0;

                    while( lastTimeStamp < startTime ) //fill the time that passed since the last steering command with no steering
                    {
                        replayData.addSteering( steer );
                        lastTimeStamp = lastTimeStamp +10;
                    }

                    steer = parts[2].equals( "left" ) ? -65536 : 65536; // keyboards can only fullsteer
                    for ( int i = startTime; i <= endTime; i = i + 10 )
                    {
                        replayData.addSteering( steer );
                    }
                    if( endTime > lastTimeStamp)
                    {
                        lastTimeStamp = endTime;
                    }
                }
            }

            for ( int i = 0; i < replayData.getSteeringLegth() * 10; i = i + 10 ) //add a timestamp for the x-axis for every 10ms to please XChart
            {
                replayData.addTimestamp( i );
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
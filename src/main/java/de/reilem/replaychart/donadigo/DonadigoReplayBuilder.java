package de.reilem.replaychart.donadigo;

import de.reilem.replaychart.E_SteeringType;
import de.reilem.replaychart.ReplayData;
import de.reilem.replaychart.SteeringAction;
import de.reilem.replaychart.gbx.GbxSteeringInput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class DonadigoReplayBuilder
{
    /**
     * read the given file line by line and extract timestamps and steering
     *
     * @param fileName
     * @return
     */
    public static ReplayData buildReplay( String fileName, boolean overlaySteering )
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
                replayData.setType( E_SteeringType.DIGITAL );
                extractPadSteering( steeringList, replayData );
            }
            else
            {
                replayData.setType( E_SteeringType.ANALOG );
                extractKeyboardSteering( steeringList, replayData );
            }

            if ( !overlaySteering ) // don't show acceleration and brake in overlay mode
            {
                extractAccelerationOrBrake( true, accelerationList, replayData );
                extractAccelerationOrBrake( false, brakeList, replayData );
            }


            String separator = System.getProperty("file.separator");
            String[] fileNameParts = fileName.split( separator );
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
    private static void extractPadSteering( List<SteeringAction> steeringList, ReplayData replayData )
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
    private static void extractKeyboardSteering( List<SteeringAction> steeringList, ReplayData replayData )
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
    private static void calculateReplayLength( List<SteeringAction> steeringList, List<String> accelerationList, List<String> brakeList,
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
    private static void extractAccelerationOrBrake( boolean isAcceleration, List<String> inputList, ReplayData replayData )
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
                        replayData.addAcceleration( GbxSteeringInput.MAX );
                    }
                    else
                    {
                        replayData.addBrake( GbxSteeringInput.MIN );
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
}

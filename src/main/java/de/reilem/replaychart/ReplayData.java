package de.reilem.replaychart;

import de.reilem.replaychart.gbx.E_TmVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds one replays steering movement for each timestamp
 */
public class ReplayData
{
    private String         fileName;
    private E_SteeringType type;
    private int            replayTime;
    private E_TmVersion    tmVersion;

    private List<Double>  steering;
    private List<Double>  acceleration;
    private List<Double>  brake;
    private List<Double>  timestamps;
    private List<Integer> respawns;

    private int timeOnThrottle;
    private int timeOnBrake;
    private int keyboardSteers;
    private int padSteers;

    public ReplayData()
    {
        steering = new ArrayList<>();
        acceleration = new ArrayList<>();
        brake = new ArrayList<>();
        timestamps = new ArrayList<>();
        respawns = new ArrayList<>();
        timeOnThrottle = 0;
        timeOnBrake = 0;
        keyboardSteers = 0;
        padSteers = 0;
    }

    public int getSteeringLegth()
    {
        return steering.size();
    }

    public double[] getSteering()
    {
        return steering.isEmpty() ? null : listToArray( steering );
    }

    public double[] getTimestamps()
    {
        return timestamps.isEmpty() ? null : listToArray( timestamps );
    }

    public void addSteering( double d )
    {
        steering.add( d );
    }

    public void addTimestamp( double d )
    {
        timestamps.add( d );
    }

    public String getChartTitle()
    {
        return fileName;
    }

    public String getChartTitleShort()
    {
        return fileName + " [" + type.name() + "] [" + ReplayChart.formatTime( (double) replayTime, tmVersion ) + "]";
    }

    public void setFileName( String name )
    {
        this.fileName = name;
    }

    public void setType( E_SteeringType type )
    {
        this.type = type;
    }

    public double[] getAcceleration()
    {
        return acceleration.isEmpty() ? null : listToArray( acceleration );
    }

    public double[] getBrake()
    {
        return brake.isEmpty() ? null : listToArray( brake );
    }

    public void addAcceleration( double d )
    {
        acceleration.add( d );
    }

    public void addBrake( double d )
    {
        brake.add( d );
    }

    public int getReplayTime()
    {
        return replayTime;
    }

    public void setReplayTime( int replayTime )
    {
        this.replayTime = replayTime;
    }

    public String getType()
    {
        if ( padSteers > keyboardSteers )
        {
            type = E_SteeringType.DIGITAL;
            return type + " (" + ReplayChart.roundDoubleTwoDecimalPlaces(
                    ((double) padSteers / (double) (padSteers + keyboardSteers)) * 100 ) + "%)";
        }
        else
        {
            type = E_SteeringType.ANALOG;
            return type + " (" + ReplayChart.roundDoubleTwoDecimalPlaces(
                    ((double) keyboardSteers / (double) (padSteers + keyboardSteers)) * 100 ) + "%)";
        }
    }

    public int getTimeOnThrottle()
    {
        return timeOnThrottle;
    }

    public int getTimeOnBrake()
    {
        return timeOnBrake;
    }

    public void addThrottleAction()
    {
        timeOnThrottle++;
    }

    public void addBrakeAction()
    {
        timeOnBrake++;
    }

    public void addPadAction()
    {
        padSteers++;
    }

    public void addKeyboardAction()
    {
        keyboardSteers++;
    }

    public List<Integer> getRespawns()
    {
        return respawns;
    }

    public void addRespawn( int time )
    {
        respawns.add( time );
    }

    public E_TmVersion getTmVersion()
    {
        return tmVersion;
    }

    public void setTmVersion( E_TmVersion tmVersion )
    {
        this.tmVersion = tmVersion;
    }

    public String getPercentTimeOnThrottle()
    {
        return "Throttle: [" + ReplayChart.roundDoubleTwoDecimalPlaces( ((double) (timeOnThrottle * 10) / (double) replayTime) * 100 )
                + "%]";
    }

    public String getPercentTimeOnBrake()
    {
        return "Brake: [" + ReplayChart.roundDoubleTwoDecimalPlaces( ((double) (timeOnBrake * 10) / (double) replayTime) * 100 ) + "%]";
    }

    private double[] listToArray( List<Double> list )
    {
        double[] array = new double[list.size()];
        for ( int i = 0; i < array.length; i++ )
        {
            array[i] = list.get( i );
        }
        return array;
    }
}

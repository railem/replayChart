package de.reilem.replaychart;

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

    private List<Double> steering;
    private List<Double> acceleration;
    private List<Double> brake;
    private List<Double> timestamps;

    private int timeOnThrottle;
    private int timeOnBrake;

    public ReplayData()
    {
        steering = new ArrayList<>();
        acceleration = new ArrayList<>();
        brake = new ArrayList<>();
        timestamps = new ArrayList<>();
        timeOnThrottle = 0;
        timeOnBrake = 0;
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
        return fileName + " [" + type.name() + "] [" + ReplayChart.formatTime( (double) replayTime ) + "]";
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

    public E_SteeringType getType()
    {
        return type;
    }

    public int getTimeOnThrottle()
    {
        return timeOnThrottle;
    }

    public int getTimeOnBrake()
    {
        return timeOnBrake;
    }

    public void addThrottle()
    {
        timeOnThrottle = timeOnThrottle + 10;
    }

    public void addBrake()
    {
        timeOnBrake = timeOnBrake + 10;
    }

    public String getPercentTimeOnThrottle()
    {
        return "Throttle: [" + ReplayChart.roundDoubleTwoDecimalPlaces( ((double) timeOnThrottle / (double) replayTime) * 100 ) + "%]";
    }

    public String getPercentTimeOnBrake()
    {
        return "Brake: [" + ReplayChart.roundDoubleTwoDecimalPlaces( ((double) timeOnBrake / (double) replayTime) * 100 ) + "%]";
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

package de.reilem.replaychart;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds one replays steering movement for each timestamp
 */
public class ReplayData
{
    private String       name;
    private String       type;
    private List<Double> steering;
    private List<Double> acceleration;
    private List<Double> brake;
    private List<Double> timestamps;

    public ReplayData()
    {
        steering = new ArrayList<>();
        acceleration = new ArrayList<>();
        brake = new ArrayList<>();
        timestamps = new ArrayList<>();
    }

    public int getSteeringLegth()
    {
        return steering.size();
    }

    public int getTimestampsLegth()
    {
        return timestamps.size();
    }

    public double[] getSteering()
    {
        return listToArray( steering );
    }

    public double[] getTimestamps()
    {
        return listToArray( timestamps );
    }

    public void addSteering( double d )
    {
        steering.add( d );
    }

    public void addTimestamp( double d )
    {
        timestamps.add( d );
    }

    public String getTitle()
    {
        return name + " [" + type + "]";
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setType( String type )
    {
        this.type = type;
    }
    public double[] getAcceleration()
    {
        return listToArray( acceleration );
    }

    public double[] getBrake()
    {
        return listToArray( brake );
    }

    public void addAcceleration( double d )
    {
        acceleration.add( d );
    }

    public void addBrake( double d )
    {
        brake.add( d );
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

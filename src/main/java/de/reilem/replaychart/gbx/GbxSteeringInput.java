package de.reilem.replaychart.gbx;

public class GbxSteeringInput
{
    public static double MAX = 65536.0;
    public static double MIN = -65536.0;

    private int            time;
    private E_GbxInputType type;
    private int            value;

    public int getTime()
    {
        return time;
    }

    public void setTime( int time )
    {
        this.time = time;
    }

    public E_GbxInputType getType()
    {
        return type;
    }

    public void setType( E_GbxInputType type )
    {
        this.type = type;
    }

    public int getValue()
    {
        return value;
    }

    public void setValue( int value )
    {
        this.value = value;
    }
}

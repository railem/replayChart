package de.reilem.replaychart;

public class SteeringAction
{
    String time;
    String value;

    public SteeringAction( String time, String value )
    {
        this.time = time;
        this.value = value;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime( String time )
    {
        this.time = time;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }
}

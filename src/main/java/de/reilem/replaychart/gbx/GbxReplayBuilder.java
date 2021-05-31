package de.reilem.replaychart.gbx;

import de.reilem.replaychart.ReplayData;

import java.util.List;

public class GbxReplayBuilder
{
    private int    timestamp    = 0;
    private double acceleration = 0.0;
    private double brake        = 0.0;
    private double steering     = 0.0; //pad
    private double steer_right  = 0.0; //kb
    private double steer_left   = 0.0; //kb

    private List<GbxSteeringInput> inputs;
    private ReplayData             replay;

    public ReplayData build( int replayTime, List<GbxSteeringInput> inputs, boolean invertedSteering,
            E_TmVersion tmVersion )
    {
        this.inputs = inputs;
        replay = new ReplayData();
        replay.setReplayTime( replayTime );

        while ( timestamp < replayTime )
        {
            evaluateEventsAt( timestamp, invertedSteering );
            addTimeStamp();
            timestamp += 10;
        }

        for ( GbxSteeringInput input : inputs )
        {
            if ( input.getType() == E_GbxInputType.RESPAWN && input.getValue() == 1 )
            {
                replay.addRespawn( input.getTime() );
            }
        }

        return replay;
    }

    private void evaluateEventsAt( int timestamp, boolean invertedSteering )
    {
        for ( GbxSteeringInput input : inputs )
        {
            if ( input.getTime() == timestamp )
            {
                if ( input.getType() == E_GbxInputType.ACCELERATE )
                {
                    acceleration = input.getValue() > 0.0 ? GbxSteeringInput.MAX : 0.0;
                }
                else if ( input.getType() == E_GbxInputType.BRAKE )
                {
                    brake = input.getValue() > 0.0 ? GbxSteeringInput.MIN : 0.0;
                }
                else if ( input.getType() == E_GbxInputType.STEER_RIGHT )
                {
                    steer_right = input.getValue() == 0.0 ? 0.0 : invertedSteering ? GbxSteeringInput.MIN : GbxSteeringInput.MAX;
                }
                else if ( input.getType() == E_GbxInputType.STEER_LEFT )
                {
                    steer_left = input.getValue() == 0.0 ? 0.0 : invertedSteering ? GbxSteeringInput.MAX : GbxSteeringInput.MIN;
                }
                else if ( input.getType() == E_GbxInputType.STEER )
                {
                    steering = invertedSteering ? input.getValue() * -1.0 : input.getValue();
                }
            }
        }
    }

    private void addTimeStamp()
    {
        replay.addTimestamp( timestamp );
        replay.addAcceleration( acceleration );
        replay.addBrake( brake );

        if ( acceleration != 0.0 )
        {
            replay.addThrottleAction();
        }
        if ( brake != 0.0 )
        {
            replay.addBrakeAction();
        }

        if ( steering != 0.0 ) //define that pad steering is stronger than keyboard
        {
            replay.addSteering( steering );
            replay.addPadAction();
        }
        else if ( steer_left != 0.0 ) //left is stronger than right
        {
            replay.addSteering( steer_left );
            replay.addKeyboardAction();
        }
        else if ( steer_right != 0.0 )
        {
            replay.addSteering( steer_right );
            replay.addKeyboardAction();
        }
        else
        {
            replay.addSteering( 0.0 );
        }
    }

}

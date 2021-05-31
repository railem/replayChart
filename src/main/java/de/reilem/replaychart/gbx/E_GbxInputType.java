package de.reilem.replaychart.gbx;

public enum E_GbxInputType
{
    START, ACCELERATE, BRAKE, STEER, STEER_RIGHT, STEER_LEFT, FINISH, RESPAWN, UNKNOWN;

    public static E_GbxInputType getType( String controlName )
    {
        switch ( controlName )
        {
        case "_FakeIsRaceRunning":
            return START;
        case "Accelerate":
            return ACCELERATE;
        case "Brake":
            return BRAKE;
        case "SteerRight":
            return STEER_RIGHT;
        case "SteerLeft":
            return STEER_LEFT;
        case "Steer":
            return STEER;
        case "_FakeFinishLine":
            return FINISH;
        case "Respawn":
            return RESPAWN;
        default:
            return UNKNOWN;
        }
    }
}

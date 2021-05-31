package de.reilem.replaychart.gbx;

import de.reilem.replaychart.E_SteeringType;
import de.reilem.replaychart.ReplayData;
import org.anarres.lzo.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GbxInputExtractor
{
    public static ReplayData parseReplayData( String replayFilePath, boolean invertedSteering ) throws DecoderException, IOException
    {
        byte[] replayFileBytes = Files.readAllBytes( Paths.get( replayFilePath ) );

        String replayFileHex = Hex.encodeHexString( replayFileBytes );
        String[] split = replayFileHex.split( "3c2f6865616465723e" ); //split after header
        E_TmVersion tmVersion = extractTmVersion( split[0] );

        //skip reference table
        int uncompressedBodySize;
        int compressedBodySize;
        byte[] compressedBodyBytes;

        if ( tmVersion != E_TmVersion.TM2 ) //eswc & forever
        {
            uncompressedBodySize = hexToInt( split[1].substring( 16, 24 ) ); //uncompressed body size
            compressedBodySize = hexToInt( split[1].substring( 24, 32 ) ); //compressed body size
            compressedBodyBytes = Hex.decodeHex( split[1].substring( 32 ) ); //compressed body bytes
        }
        else
        {
            int gbxMagicStringLocation = split[1].indexOf( "474258" );

            int start = gbxMagicStringLocation - 36;
            if ( split[1].substring( start, start + 2 ).equals( "00" ) )
            {
                start += 2;
            }

            uncompressedBodySize = hexToInt( split[1].substring( start, start + 8 ) );//uncompressed body size
            compressedBodySize = hexToInt( split[1].substring( start + 8, start + 16 ) ); //compressed body size
            compressedBodyBytes = Hex.decodeHex( split[1].substring( start + 16 ) ); //compressed body bytes
        }

        byte[] uncompressedBody = decompress( compressedBodyBytes, compressedBodySize, uncompressedBodySize );
        String uncompressedBodyHex = Hex.encodeHexString( uncompressedBody ); //uncompressed body hex

        //Files.write(Paths.get("/home/jedingerd@procilon.local/Downloads/tm2Test"), uncompressedBody);

        String inputMarker = tmVersion == E_TmVersion.FOREVER ? "19200903" : tmVersion == E_TmVersion.ESWC ? "0df00324" : "25200903";

        String inputBlock = uncompressedBodyHex.split( inputMarker )[1]; //skip to input block
        if ( tmVersion == E_TmVersion.TM2 )
        {
            inputBlock = inputBlock.substring( 24 );
        }
        int replayTime = hexToInt( inputBlock.substring( 0, 8 ) ); //time driven in the replay
        int amountOfDifferentInputs = hexToInt( inputBlock.substring( 16, 24 ) ); //amount of different inputs

        List<String> controlNames = new ArrayList<>(); //list of different inputs

        int index = 32; //start of the controlNames list
        while ( controlNames.size() < amountOfDifferentInputs )
        {
            int length = hexToInt( inputBlock.substring( index, index + 8 ) ); //length of the input string
            controlNames.add( hexToAscii( inputBlock.substring( index + 8, index + 8 + (length * 2) ) ) ); //read and add
            index = index + 8 + (length * 2); //set index
            if ( controlNames.size() < amountOfDifferentInputs )
            {
                index += 8; //add index if we are not done yet
            }
        }

        int amountOfInputs = hexToInt( inputBlock.substring( index, index + 8 ) ); //amount of inputs by player
        index += 16;

        List<GbxSteeringInput> inputs = new ArrayList<>();

        while ( inputs.size() < amountOfInputs )
        {
            GbxSteeringInput in = new GbxSteeringInput();

            E_GbxInputType type = E_GbxInputType
                    .getType( controlNames.get( hexToInt( inputBlock.substring( index + 8, index + 10 ) ) ) ); //get type of input
            in.setType( type );

            int value;
            if ( in.getType() == E_GbxInputType.STEER )
            {
                value = hexToInt24( inputBlock.substring( index + 10, index + 18 ) ); //get pad steer input
            }
            else
            {
                value = hexToInt( inputBlock.substring( index + 10, index + 18 ) );
            }
            in.setValue( value );

            int time = hexToInt( inputBlock.substring( index, index + 8 ) ) - 100010;

            if ( tmVersion == E_TmVersion.ESWC )
            {
                //round up to nearest 10 in eswc because eswc sometimes uses exact ms times
                time = (int) (Math.round( time / 10.0 ) * 10);
            }
            if ( tmVersion == E_TmVersion.TM2 && time < 0 ) //tm2 events start before 0
            {
                time = 0;
            }

            if ( value != 1.0 && type != E_GbxInputType.STEER && type != E_GbxInputType.FINISH && type != E_GbxInputType.START )
            {
                time += 10;
            }
            in.setTime( time ); // read time + 100010

            inputs.add( in );
            index += 18;
        }

        ReplayData replayData = new GbxReplayBuilder().build( replayTime, inputs, invertedSteering, tmVersion );
        replayData.setType( E_SteeringType.DIGITAL );

        replayData.setFileName( extractReplayName( replayFilePath ) );
        replayData.setTmVersion( tmVersion );

        return replayData;
    }

    private static E_TmVersion extractTmVersion( String header )
    {
        String versionText = hexToAscii( header ).split( "exever=\"" )[1];

        if ( versionText.startsWith( "0." ) )
        {
            return E_TmVersion.ESWC;
        }
        else if ( versionText.startsWith( "2." ) )
        {
            return E_TmVersion.FOREVER;
        }
        else if ( versionText.startsWith( "3." ) )
        {
            return E_TmVersion.TM2;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported TM version!" );
        }
    }

    private static String extractReplayName( String replayFilePath )
    {
        String seperator = System.getProperty("os.name").toLowerCase().contains( "win" ) ? "\\" : "/";
        String[] filePathParts = replayFilePath.split( seperator );
        String fileName = filePathParts[filePathParts.length - 1];
        return fileName.substring( 0, fileName.length() - 11 );
    }

    private static byte[] decompress( byte[] src, int compressedSize, int uncompressedSize ) throws IOException
    {
        LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor( LzoAlgorithm.LZO1X, null );
        byte[] uncompressedBody = new byte[uncompressedSize];
        int lzoReturnCode = decompressor.decompress( src, 0, compressedSize, uncompressedBody, 0, new lzo_uintp( uncompressedSize ) );
        if ( lzoReturnCode == LzoTransformer.LZO_E_OK )
        {
            return uncompressedBody;
        }
        throw new IOException( "Unable to decompress data, lzo code: " + lzoReturnCode );
    }

    private static int hexToInt( String hex )
    {
        int index = 0;
        StringBuilder reversedHex = new StringBuilder();

        while ( index + 2 <= hex.length() )
        {
            reversedHex.insert( 0, hex, index, index + 2 );
            index = index + 2;
        }

        return Integer.parseInt( reversedHex.toString(), 16 );
    }

    private static int hexToInt24( String hex ) throws DecoderException
    {
        byte[] input = Hex.decodeHex( hex.substring( 0, 6 ) ); //d2fcff
        return -((input[2]) << 16 | (input[1] & 0xFF) << 8 | (input[0] & 0xFF));
    }

    private static String hexToAscii( String hexString )
    {
        StringBuilder sb = new StringBuilder();

        for ( int i = 0; i < hexString.length(); i += 2 )
        {
            String s = hexString.substring( i, i + 2 );
            sb.append( (char) Integer.parseInt( s, 16 ) );
        }
        return sb.toString();
    }
}

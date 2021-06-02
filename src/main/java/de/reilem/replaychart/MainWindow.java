package de.reilem.replaychart;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainWindow extends javax.swing.JFrame
{
    public static void main( String[] args )
    {
        if ( args.length == 0 ) //show UI
        {
            MainWindow window = new MainWindow();
            window.start();
        }
        else
        {
            List<String> arguments = new ArrayList<>( Arrays.asList( args ) );
            boolean overlaySteering = false;
            boolean invertSteering = false;
            boolean matchTimeline = false;

            if ( arguments.contains( "-o" ) ) //check for overlay mode
            {
                overlaySteering = true;
                arguments.remove( "-o" );
            }
            if ( arguments.contains( "-i" ) ) //check for inverted steering mode
            {
                invertSteering = true;
                arguments.remove( "-i" );
            }
            if ( arguments.contains( "-m" ) ) //check for inverted steering mode
            {
                matchTimeline = true;
                arguments.remove( "-m" );
            }

            new ReplayChart().init( arguments, overlaySteering, invertSteering, false, matchTimeline );
        }
    }

    private JCheckBox overlayCb;
    private JCheckBox invertCb;
    private JCheckBox matchTimelineCb;

    private void start()
    {
        try
        {
            this.setIconImage( ImageIO.read(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream( "icon.png" ) ) );
        }
        catch ( Throwable e )
        {
            //ignore it
        }
        this.setTitle( "ReplayChart v1.4" );
        this.setSize( 500, 210 );
        this.setResizable( false );
        centerWindow();
        this.setVisible( true );
        this.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
        JPanel panel = new JPanel();
        panel.setLayout( null );
        this.add( panel );

        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled( true );
        fc.setFileFilter( new FileFilter()
        {
            @Override public boolean accept( File f )
            {
                if ( f.isDirectory() )
                {
                    return true;
                }
                return f.getName().toLowerCase().contains( ".replay" ) && f.getName().toLowerCase().contains( ".gbx" );
            }

            @Override public String getDescription()
            {
                return null;
            }
        } );

        JLabel descriptionLabel = new JLabel( "<html>Visualize one or multiple TrackMania replays in charts.<br><br>"
                + "Each replay's <i>Steering</i>, <font color='green'><i>Throttle</i></font> "
                + "and <font color='red'><i>Brake</i></font> inputs will be displayed. "
                + "The chart will also contain each replay's used input type, the time of the run, the percentage spend on the throttle and brake "
                + "and <font color='#00AAAA'><i>Respawns</i></font>.</html>" );
        panel.add( descriptionLabel );
        descriptionLabel.setBounds( 10, 10, 480, 100 );

        overlayCb = new JCheckBox( "Overlay in one chart" );
        panel.add( overlayCb );
        overlayCb.setBounds( 10, 120, 170, 25 );
        overlayCb.setToolTipText( "Displays all selected replays in one chart. Only shows the steering movement!" );

        invertCb = new JCheckBox( "Invert Steering" );
        panel.add( invertCb );
        invertCb.setBounds( 205, 120, 150, 25 );
        invertCb.setToolTipText( "Inverts left and right steering in the chart. Useful to follow along the timeline easier!" );

        matchTimelineCb = new JCheckBox( "Match Timeline" );
        panel.add( matchTimelineCb );
        matchTimelineCb.setBounds( 360, 120, 160, 25 );
        matchTimelineCb.setToolTipText( "Matches the Timeline (X-Axis) of all selected replays. Eases the comparison of replays!" );

        invertCb.setSelected( true );
        matchTimelineCb.setSelected( true );

        JButton selectButton = new JButton( "Select Replays" );
        panel.add( selectButton );
        selectButton.setBounds( 10, 150, 480, 25 );
        selectButton.addActionListener( e ->
        {
            int returnVal = fc.showDialog( this, "Open" );
            if ( returnVal == 0 )
            {
                File[] files = fc.getSelectedFiles();

                List<String> arguments = new ArrayList<>();
                Arrays.asList( files ).forEach( file -> arguments.add( file.getAbsolutePath() ) );

                new ReplayChart().init( arguments, overlayCb.isSelected(), invertCb.isSelected(), true, matchTimelineCb.isSelected() );
            }
        } );
    }

    /**
     * centers the window on the screen
     */
    public void centerWindow()
    {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
        this.setLocation( x, y );
    }
}

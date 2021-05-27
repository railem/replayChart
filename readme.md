# Replay Chart

**Replay Chart** is a Java Tool that can visualize TrackMania steering, acceleration and brake inputs in charts.
It can visualize multiple replays at once, either individually (each in separate charts) or overlapped (all in the same chart).
Both pad and keyboard runs can be processed and will be marked accordingly in the chart.

In order to produce data that this tool can read, use donadigo's [generate_input_file.py](https://github.com/donadigo/gbxtools/blob/master/generate_input_file.py "generate_input_file.py") python script to extract the steering inputs from .gbx files.

Before we go into the examples, I'd like to point out that nothing suspicious is going on the in these example screenshots,
but we can see how different everyone is using their device :)

### [Download the latest Release [v1.0]](https://github.com/railem/replayChart/releases/download/1.0/replayChart-1.0.jar)

## Parameters & Features
`-o` - Overlays the steering of multiple replays in one chart.<br>
Acceleration and brake can not be displayed in this mode.
<br><br>
`-i` - Inverts left and right.<br>
Might help with orientation when following the timeline.

**Zooming**<br>
By selecting areas in a chart you can zoom into that area.

## Example 1

This example visualizes all input files in the given folder in separate charts:

`java -jar replayChart-1.1.jar /path/to/folder`

![](https://i.imgur.com/nJBLvbC.png"")

## Example 2

This example visualizes the given input files in separate charts:

`java -jar replayChart-1.1.jar file1 file2`

![](https://i.imgur.com/XYqTerR.png"")

## Example 3

This example visualizes the given input files in the same chart:

`java -jar replayChart-1.1.jar file1 file2 -o`

![](https://i.imgur.com/1bOe5gR.png"")

## Known Issues
- Driving on pad with a d-pad (partially or fully) might mark the run as keyboard.
- The last seconds of some replays are missing sometimes, this is hard to overcome since I don't have the actual time of the run. So if someone hasn't steered in the last 10 seconds of the run, it won't be shown.

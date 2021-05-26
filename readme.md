# Replay Steering Chart

**Replay Steering Chart** is a Java Tool that can visualize TrackMania steering inputs as charts.
It can visualize multiple replays at once, either individually (each in their own chart) or overlapped (all in the same graph).
Both pad and keyboard runs can be processed and will be marked accordingly in the chart.

In order to produce data that this tool can read, use donadigo's [generate_input_file.py](https://github.com/donadigo/gbxtools/blob/master/generate_input_file.py "generate_input_file.py") python script to extract the steering inputs from .gbx files.

Before we go into the examples, I'd like to point out that nothing suspicious is going on the in these example screenshots,
but we can see how different everyone is using their device :)

### [Download the latest Release [v1.0]](https://github.com/railem/replayChart/releases/download/1.0/replayChart-1.0.jar)

## Example 1

This example visualizes the given input files each in their own chart:

`java -jar replayChart-1.0.jar file1 file2`

![](https://i.imgur.com/f9T2lAh.png"")

## Example 2

This example visualizes the given input files in the same chart:

`java -jar replayChart-1.0.jar file1 file2 -overlay`

![](https://i.imgur.com/KWR1O5F.png"")

## Example 3

This example visualizes all input files in the given folder in the same chart:

`java -jar replayChart-1.0.jar /path/to/folder`

![](https://i.imgur.com/C9HHLDh.png"")

## Known Issues
- Driving on pad with a d-pad (partially or fully) might mark the run as keyboard.
- The last seconds of some replays are missing sometimes, this is hard to overcome since I don't have the actual time of the run. So if someone hasn't steered in the last 10 seconds of the run, it won't be shown.

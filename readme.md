# Replay Chart

**Replay Chart** is a Java Tool that can visualize TrackMania steering, throttle and brake inputs in charts.
It can visualize multiple replays at once, either individually (each in separate charts) or overlapped (all in the same chart).
<br>Both pad and keyboard runs can be processed and will be marked accordingly in the chart.
<br>The Tool also displays the time of each run above the chart, as well as the percentage of the run spend pressing the brake and throttle!

In order to produce data that this tool can read, use donadigo's [generate_input_file.py](https://github.com/donadigo/gbxtools/blob/master/generate_input_file.py "generate_input_file.py") python script to extract the steering inputs from .gbx files.

Before we go into the examples, I'd like to point out that nothing suspicious is going on the in these example screenshots,
but we can see how different everyone is using their device :)

### [Download the latest Release [v1.1]](https://github.com/railem/replayChart/releases/download/1.1/replayChart-1.1.jar)

## Parameters
`-o` - Overlays the steering of multiple replays in one chart.<br>
Acceleration and brake can not be displayed in this mode.
<br><br>
`-i` - Inverts left and right.<br>
Might help with orientation when following the timeline.

## Example 1

This example visualizes all input files in the given folder in separate charts:

`java -jar replayChart-1.2.jar /path/to/folder`

![](https://i.imgur.com/yJdcNy4.png"")

## Example 2

This example visualizes the given input files in separate charts:

`java -jar replayChart-1.2.jar file1 file2`

![](https://i.imgur.com/JfWDa29.png"")

## Example 3

This example visualizes the given input files in the same chart:

`java -jar replayChart-1.2.jar file1 file2 -o`

![](https://i.imgur.com/pHBvITM.png"")

## Known Issues
- Driving keyboard/d-pad and pad mixed is currently not supported.
- ESWC replay times and graphs are not displayed correctly. They might end too soon.

## Changelog

### 1.1
- added brake and throttle graphs to chart
- improved legend & labels
- renamed -overlay parameter to (-o)
- added zoom feature
- added invert parameter (-i)

### 1.2
- made the graph calculation more accurate
- added replay time display
- moved legend into the chart on overlay mode
- added label showing the time spend pressing acceleration & brake in percent
- added custom theme for a clean for a look
- drawing custom lines to help orientation
- removed broken zoom feature 
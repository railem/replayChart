# Replay Chart

**Replay Chart** is a Java Tool that can visualize TrackMania steering, throttle and brake inputs in charts.
It can visualize multiple replays at once, either individually (each in separate charts) or overlapped (all in the same chart).
<br>Both pad and keyboard runs can be processed and will be marked accordingly in the chart. 
The percentage behind the input type shows how much of the run is driven on with that type (Analog/Digital).
<br>The Tool also displays the time of each run above the chart, as well as the percentage of the run spend pressing the brake and throttle!

The tool needs no external dependencies to function!<br>
With the help of lx and donadigo the tool now extracts the input of each replay itself!

Supported TrackMania version: TMO, TMS, TMN ESWC, TMNF, TMUF, TM2

### [Download the latest Release [v1.3]](https://github.com/railem/replayChart/releases/download/1.3/replayChart-1.3.jar)

## Tool Usage
In order to run the Tool ou need to execute it via `java -jar replayChart-1.4.jar`.<br>
You can then select the replays you want to analyze.


## Example 1 (TMNF - ESL-Hockolicious)

![](https://i.imgur.com/IKrcuCQ.png"")

## Example 2 (TMUF Coast - C3)

![](https://i.imgur.com/Nc88023.png"")

## Example 3 (TMN ESWC - reRun)

![](https://i.imgur.com/8R6Zzpc.png"")

## Commandline
The tool still supports the commandline variant. Just execute the jar with the extra parameters.

`-o` - Overlays the steering of multiple replays in one chart.<br>
Acceleration and brake, and other individual stats will not be displayed in this mode.
<br><br>
`-i` - Inverts left and right.<br>
Might help with orientation when following the timeline.
<br><br>
`/path/to/file` - path to folder or replay to analyze.<br>
Can be used multiple times to add more than one file.

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

### 1.3
- showing the percentage of the run driven on each device
- added support for replay.gbx input extraction
- added gui


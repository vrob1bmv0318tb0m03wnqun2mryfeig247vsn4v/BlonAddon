# Video Player for Blon Addon

This adds video playback capability to the Blon Minecraft addon using text displays and the command block core.

## Components

### 1. Python Video Processor (`video_processor.py`)
Converts video files to text display frame data.

**Requirements:**
- Python 3.6+
- OpenCV (`pip install opencv-python`)
- NumPy (usually included with OpenCV)

**Usage:**
```bash
python3 video_processor.py input_video.mp4 output.json --fps 10 --quality 5
```

**Options:**
- `--fps`: Target frames per second (default: 10)
- `--max-frames`: Limit frames for testing
- `--tile-size`: Rows per text display tile (default: 10)
- `--quality`: Quality level 1-10 (default: 5, higher = more detail I think ~2 is best idk tho i havent tested a lot.)

**Output:**
- JSON file with frame data optimized for Minecraft text displays
- Places in `.minecraft/videos/` directory

### 2. VideoPlayer Module
Java module that loads the JSON and plays the video using command blocks.

**Settings:**
- `video-file`: Name of JSON file in videos folder
- `playback-fps`: Playback speed
- `scale`, `vertical-scale`, `font-ratio`: Display scaling
- `distance-from-player`: How far in front to display
- `loop`: Whether to loop the video
- `tile-spacing`: Space between display tiles
- `packets-per-tick`: Commands per tick (performance tuning also to work around server packet limit [99% of the time it is not a problem])

## How to Use

1. **Process Video:**
    ex:
   ```bash
   python3 video_processor.py myvideo.mp4 myvideo.json --fps 15 --quality 7
   ```

2. **In Minecraft:**
   - Place a command block core: `.core 16 16 32`
   - Enable VideoPlayer module
   - Set `video-file` to `myvideo.json`
   - Adjust settings as needed
   - Toggle the module to start playback

## Technical Details

- Videos are split into tiles (text displays) for large resolutions
- Uses optimized text component format like ImageEggs
- Command blocks execute `/summon` for initial setup, `/data modify` for frame updates
- Playback synchronized with Minecraft ticks
- Supports looping and adjustable playback speed

## Performance Notes

- Higher quality = more text displays = more commands
- Use appropriate `packets-per-tick` to avoid lag
- Lower FPS for smoother playback on large videos
- Core size affects maximum concurrent commands

## File Locations

- Video JSONs: `.minecraft/videos/`
- Python script: Project root
- Java module: `src/main/java/skid/supreme/blon/modules/VideoPlayer.java`

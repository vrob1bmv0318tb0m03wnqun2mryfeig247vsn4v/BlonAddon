package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SongPlayer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPerformance = settings.createGroup("Performance");

    public final Setting<Boolean> info = sgGeneral.add(new BoolSetting.Builder()
            .name("info")
            .description("Sends usage info to chat")
            .defaultValue(false)
            .onChanged(v -> {
                if (v) {
                    showInfo();
                    // We can't auto-reset 'info' here easily without self-reference issues during
                    // init.
                    // Users will just have to toggle it off and on again.
                    // Or we could run a task to reset it, but simple is better.
                }
            })
            .build());

    public final Setting<String> functionFile = sgGeneral.add(new StringSetting.Builder()
            .name("datapack-file")
            .description("Name of .zip datapack file")
            .defaultValue("song.zip")
            .build());

    public final Setting<String> startFunction = sgGeneral.add(new StringSetting.Builder()
            .name("start-function")
            .description("Function to start (play namespace:function/play)")
            .defaultValue("play")
            .build());

    private final Setting<Double> volume = sgGeneral.add(new DoubleSetting.Builder()
            .name("volume")
            .description("Volume multiplier")
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(2.0)
            .build());

    private final Setting<Integer> packetsPerTick = sgPerformance.add(new IntSetting.Builder()
            .name("packets-per-tick")
            .description("C2S packets sent per tick (for slow internet)")
            .defaultValue(100)
            .min(1)
            .max(10000)
            .sliderMin(1)
            .sliderMax(500)
            .build());

    private final Setting<Integer> batchSize = sgPerformance.add(new IntSetting.Builder()
            .name("batch-size")
            .description("Commands processed per batch")
            .defaultValue(50)
            .min(1)
            .max(500)
            .sliderMin(1)
            .sliderMax(200)
            .build());

    private final Setting<Integer> maxPlaysounds = sgPerformance.add(new IntSetting.Builder()
            .name("max-playsounds")
            .description("Max playsounds per tick to prevent kicks")
            .defaultValue(240)
            .min(1)
            .max(1000)
            .build());

    private final Setting<Boolean> showProgress = sgGeneral.add(new BoolSetting.Builder()
            .name("show-progress")
            .description("Show playback progress above hotbar")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoSchedule = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-schedule")
            .description("Automatically follow schedule commands")
            .defaultValue(true)
            .build());

    // Loaded functions from datapack
    private Map<String, List<String>> functions = new HashMap<>();
    private Queue<ScheduledFunction> scheduledFunctions = new LinkedList<>();
    private List<String> currentBatch = new ArrayList<>();

    private static class ScheduledFunction {
        String functionPath;
        long executeAtTick;

        ScheduledFunction(String functionPath, long executeAtTick) {
            this.functionPath = functionPath;
            this.executeAtTick = executeAtTick;
        }
    }

    private String namespace = "";
    private long startTime = 0;
    private long gameTick = 0;
    private int totalCommandsExecuted = 0;
    private boolean isPlaying = false;
    private int playsoundsThisTick = 0;

    public SongPlayer() {
        super(Blon.Main, "song-player", "Plays music datapacks through the core");
    }

    @Override
    public void onActivate() {
        if (CoreCommand.corePositions == null || CoreCommand.corePositions.isEmpty()) {
            error("Core not found! Use .core command first.");
            toggle();
            return;
        }

        functions.clear();
        scheduledFunctions.clear();
        currentBatch.clear();
        gameTick = 0;
        totalCommandsExecuted = 0;
        startTime = System.currentTimeMillis();
        playsoundsThisTick = 0;

        if (!loadDatapack()) {
            error("Failed to load datapack!");
            toggle();
            return;
        }

        // Queue the starting function (execute immediately at tick 0)
        String startFunc = namespace + ":" + startFunction.get();
        scheduledFunctions.offer(new ScheduledFunction(startFunc, 0));

        isPlaying = true;
    }

    @Override
    public void onDeactivate() {
        // Clear all command blocks in the core
        if (CoreCommand.corePositions != null && !CoreCommand.corePositions.isEmpty()) {
            List<String> clearCommands = new ArrayList<>();
            for (int i = 0; i < CoreCommand.corePositions.size(); i++) {
                clearCommands.add(""); // Empty command
            }

            CoreUpdater.startRedstone(
                    CoreCommand.corePositions,
                    clearCommands,
                    false,
                    false,
                    packetsPerTick.get());

            for (int i = 0; i < 10; i++) {
                CoreUpdater.onTick();
            }

            info("Cleared all command blocks");
        }

        if (showProgress.get()) {
            info("Stopped. Executed " + totalCommandsExecuted + " commands in " +
                    formatTime(getElapsedSeconds()));
        }
        isPlaying = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isPlaying || CoreCommand.corePositions.isEmpty())
            return;

        gameTick++;
        playsoundsThisTick = 0; // Reset counter each tick

        // Process scheduled functions that are ready
        while (!scheduledFunctions.isEmpty()) {
            ScheduledFunction scheduled = scheduledFunctions.peek();
            if (scheduled.executeAtTick <= gameTick) {
                scheduledFunctions.poll();
                executeFunction(scheduled.functionPath);
            } else {
                break; // Wait for the right tick
            }
        }

        // Show progress every 1 second (20 ticks) with specific command block toggling
        if (showProgress.get() && CoreCommand.corePositions.size() > 1) {
            BlockPos actionbarPos = CoreCommand.corePositions.get(CoreCommand.corePositions.size() - 1);

            if (gameTick % 20 == 0) {
                // Step 1: Set to IMPULSE, ALWAYS ACTIVE, with Title Command
                String timeStr = formatTime(getElapsedSeconds());
                String totalTimeStr = formatTime(getTotalSeconds());
                String titleCmd = String.format(
                        "title @a actionbar {\"text\":\"[BLON] Playing %s %s/%s\",\"color\":\"red\"}",
                        functionFile.get(), timeStr, totalTimeStr);

                sendActionBarPacket(actionbarPos, titleCmd, true);
            } else if (gameTick % 20 == 1) {
                // Step 2: Set to IMPULSE, NOT ALWAYS ACTIVE, Empty Command (Reset)
                sendActionBarPacket(actionbarPos, "", false);
            }
        }
    }

    private void sendActionBarPacket(BlockPos pos, String cmd, boolean alwaysActive) {
        if (mc.player == null)
            return;

        mc.player.networkHandler.sendPacket(new UpdateCommandBlockC2SPacket(
                pos,
                cmd,
                CommandBlockBlockEntity.Type.REDSTONE, // Impulse
                false, // trackOutput
                false, // conditional
                alwaysActive));
    }

    private void executeFunction(String functionPath) {
        // Parse function path: "namespace:function/path"
        String funcKey = functionPath.replace(namespace + ":", "");

        List<String> commands = functions.get(funcKey);
        if (commands == null || commands.isEmpty()) {
            return;
        }

        currentBatch.clear();

        for (String cmd : commands) {
            // Handle schedule commands
            if (cmd.startsWith("schedule function ")) {
                if (autoSchedule.get()) {
                    String[] scheduleParts = cmd.split(" ");
                    if (scheduleParts.length >= 3) {
                        String scheduledFunc = scheduleParts[2];
                        // Parse delay: "1t" = 1 tick
                        long delayTicks = 1; // default 1 tick
                        if (scheduleParts.length >= 4) {
                            String delayStr = scheduleParts[3].replace("t", "");
                            try {
                                delayTicks = Long.parseLong(delayStr);
                            } catch (NumberFormatException e) {
                                delayTicks = 1;
                            }
                        }

                        long executeAt = gameTick + delayTicks;
                        scheduledFunctions.offer(new ScheduledFunction(scheduledFunc, executeAt));
                    }
                }
            } else if (cmd.startsWith("playsound")) {
                if (playsoundsThisTick >= maxPlaysounds.get()) {
                    continue; // Skip excess sounds to prevent kicks
                }

                String processedCmd = adjustVolume(cmd);
                currentBatch.add(processedCmd);
                playsoundsThisTick++;
            } else {
                currentBatch.add(cmd);
            }
        }

        // Send batch to core
        if (!currentBatch.isEmpty()) {

            // Calculate how many core blocks we need
            // Reserve last block for action bar if we have more than 1 block
            int availableBlocks = CoreCommand.corePositions.size();
            if (showProgress.get() && availableBlocks > 1) {
                availableBlocks--;
            }

            int coreSize = Math.min(currentBatch.size(), availableBlocks);

            // Use AUTO mode (repeating blocks that execute immediately)
            CoreUpdater.startAuto(
                    CoreCommand.corePositions.subList(0, coreSize),
                    currentBatch.subList(0, coreSize),
                    false, // conditional
                    false, // trackOutput
                    packetsPerTick.get());

            // Let CoreUpdater send the packets
            for (int i = 0; i < 10; i++) { // Multiple ticks to ensure all packets sent
                CoreUpdater.onTick();
            }

            totalCommandsExecuted += coreSize;
        }
    }

    private String adjustVolume(String cmd) {
        if (volume.get() == 1.0)
            return cmd; // No change needed

        try {
            // format: playsound <sound> <source> <targets> [pos] [volume] [pitch]
            // [minVolume]
            // split by space
            String[] parts = cmd.split(" ");
            if (parts.length < 6)
                return cmd; // Not enough args for volume

            // parts[0] = playsound
            // parts[1] = sound
            // parts[2] = source
            // parts[3] = targets

            // Position can be ~ ~ ~ (3 parts) or ^ ^ ^ or x y z
            // We need to find where the numbers start.
            // Usually position is at index 4, 5, 6.

            // Basic heuristic: check if parts[4] is part of pos.
            // Volume is usually after pos (3 args). So volume is at index 7.
            // Unless pos is omitted? playsound spec says pos is optional but usually
            // present in mcfunctions.
            // But sometimes people do `playsound ... master @a 1 1` (no pos? wait, pos is
            // required if using volume)
            // Correct syntax: playsound <sound> <source> <targets> [x] [y] [z] [volume]
            // [pitch] [minVolume]

            // If length >= 7, likely x y z included.
            // Index 0: playsound
            // Index 1: sound
            // Index 2: source
            // Index 3: targets
            // Index 4: x
            // Index 5: y
            // Index 6: z
            // Index 7: volume
            // Index 8: pitch
            // Index 9: minVolume

            if (parts.length >= 8) {
                double vol = Double.parseDouble(parts[7]);
                vol *= volume.get();
                // format back
                // We reconstruct the command string
                StringBuilder newCmd = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    newCmd.append(parts[i]).append(" ");
                }
                newCmd.append(String.format(Locale.US, "%.4f", vol));

                // Add remaining parts
                for (int i = 8; i < parts.length; i++) {
                    newCmd.append(" ").append(parts[i]);
                }
                return newCmd.toString();
            }

            return cmd;

        } catch (Exception e) {
            return cmd; // Fallback to original
        }
    }

    private boolean loadDatapack() {
        try {
            File dir = new File(mc.runDirectory, "cmdSongs");
            if (!dir.exists()) {
                dir.mkdirs();
                error("Created cmdSongs folder at: " + dir.getAbsolutePath());
                return false;
            }

            File file = new File(dir, functionFile.get());

            if (!file.exists()) {
                error("File not found: " + file.getAbsolutePath());
                return false;
            }

            if (!file.getName().endsWith(".zip")) {
                error("Only .zip datapacks are supported!");
                return false;
            }

            return loadFromZip(file);

        } catch (Exception e) {
            error("Failed to load datapack: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadFromZip(File zipFile) throws IOException {
        int totalLoaded = 0;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            // First pass: find namespace
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("data/") && (name.contains("/function/") || name.contains("/functions/"))) {
                    String[] parts = name.split("/");
                    if (parts.length >= 2) {
                        namespace = parts[1];
                        break;
                    }
                }
            }

            if (namespace.isEmpty()) {
                error("No valid datapack structure found!");
                error("Make sure the zip contains: data/<namespace>/function/");
                return false;
            }

            // Second pass: load all functions
            entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                // Match: data/namespace/function/.../*.mcfunction OR
                // data/namespace/functions/.../*.mcfunction
                if ((name.startsWith("data/" + namespace + "/function/") ||
                        name.startsWith("data/" + namespace + "/functions/")) &&
                        name.endsWith(".mcfunction")) {

                    // Extract function path
                    String funcPath = name
                            .replace("data/" + namespace + "/function/", "")
                            .replace("data/" + namespace + "/functions/", "")
                            .replace(".mcfunction", "");

                    // Load commands
                    List<String> commands = new ArrayList<>();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(zip.getInputStream(entry)))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            // Skip empty lines and comments
                            if (!line.isEmpty() && !line.startsWith("#")) {
                                // Remove leading slash if present
                                if (line.startsWith("/")) {
                                    line = line.substring(1);
                                }
                                commands.add(line);
                            }
                        }
                    }

                    if (!commands.isEmpty()) {
                        functions.put(funcPath, commands);
                        totalLoaded += commands.size();
                    }
                }
            }
        }

        if (functions.isEmpty()) {
            error("No functions found in datapack!");
            return false;
        }

        info("Loaded " + functions.size() + " functions with " + totalLoaded + " total commands");
        info("Started playing: " + functionFile.get());

        isPlaying = true;
        return true;
    }

    private int getElapsedSeconds() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    private int getTotalSeconds() {
        if (functions.isEmpty())
            return 0;
        // Estimate based on number of functions (each frame is roughly 1 tick = 0.05
        // seconds)
        int totalFrames = functions.size();
        return totalFrames / 20; // 20 ticks per second
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    private void showInfo() {
        File dir = new File(mc.runDirectory, "cmdSongs");
        info("Use https://github.com/Thorioum/eidolon to generate a datapack");
        info("Place the .zip file in cmdSongs folder");
        info("Make sure core is spawned (.core)");
        info("Then play the song (Default mcfunction name from thoriums tool is play)");
    }
}

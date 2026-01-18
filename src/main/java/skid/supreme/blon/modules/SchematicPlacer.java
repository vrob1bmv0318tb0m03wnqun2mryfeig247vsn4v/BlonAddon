package skid.supreme.blon.modules;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

public class SchematicPlacer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacement = settings.createGroup("Placement");
    private final SettingGroup sgCore = settings.createGroup("Core Settings");

    private final Setting<String> schematicFile = sgGeneral.add(new StringSetting.Builder()
            .name("schematic-file")
            .description("Name of the schematic file (without extension) from .minecraft/schematics/")
            .defaultValue("")
            .build());

    private final Setting<PlacementMode> placementMode = sgPlacement.add(new EnumSetting.Builder<PlacementMode>()
            .name("placement-mode")
            .description("How to position the schematic")
            .defaultValue(PlacementMode.Relative)
            .build());

    private final Setting<BlockPos> placementOffset = sgPlacement.add(new BlockPosSetting.Builder()
            .name("placement-offset")
            .description("Offset from placement position")
            .defaultValue(new BlockPos(0, 0, 0))
            .build());

    private final Setting<Integer> packetsPerTick = sgCore.add(new IntSetting.Builder()
            .name("packets-per-tick")
            .description("How many command blocks to update per tick")
            .defaultValue(50)
            .min(1)
            .max(100000)
            .sliderMin(1)
            .sliderMax(50000)
            .build());

    private final Setting<CoreUpdater.Mode> coreMode = sgCore.add(new EnumSetting.Builder<CoreUpdater.Mode>()
            .name("core-mode")
            .description("Command block execution mode")
            .defaultValue(CoreUpdater.Mode.AUTO)
            .build());

    private final Setting<Boolean> ignoreAir = sgPlacement.add(new BoolSetting.Builder()
            .name("ignore-air")
            .description("Don't place air blocks")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> placeWaterlogged = sgPlacement.add(new BoolSetting.Builder()
            .name("place-waterlogged")
            .description("Place waterlogged blocks")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> useVolumeOptimization = sgPlacement.add(new BoolSetting.Builder()
            .name("use-volume-optimization")
            .description("Use fill commands instead of setblock for better performance")
            .defaultValue(true)
            .build());

    private final Setting<Integer> minFillSize = sgPlacement.add(new IntSetting.Builder()
            .name("min-fill-size")
            .description("Minimum size for fill commands (blocks)")
            .defaultValue(8)
            .min(2)
            .max(64)
            .build());

    private SchematicData currentSchematic;
    private List<String> placementCommands;
    private boolean isPlacing;
    private int expectedTicks;

    public SchematicPlacer() {
        super(Blon.Main, "SchematicPlacer", "Places schematics using the command block core");
    }

    @Override
    public void onActivate() {
        if (schematicFile.get().isEmpty()) {
            error("No schematic file specified");
            toggle();
            return;
        }

        if (CoreCommand.corePositions.isEmpty()) {
            error("No command block core found. Use .core first");
            toggle();
            return;
        }

        loadSchematic();
        if (currentSchematic == null || currentSchematic.blocks == null || currentSchematic.palette == null) {
            error("Failed to load schematic or invalid schematic format");
            toggle();
            return;
        }

        // Check schematic size limits to prevent memory issues
        long totalBlocks = (long) currentSchematic.width * currentSchematic.height * currentSchematic.length;
        if (totalBlocks > 16_777_216) { // 4096^3 max reasonable size
            error("Schematic too large: " + totalBlocks + " blocks (max 16M)");
            toggle();
            return;
        }

        // Estimate memory usage for work arrays (conservative estimate)
        long estimatedMemoryMB = totalBlocks * 4L / (1024 * 1024); // 4 bytes per int
        if (estimatedMemoryMB > 512) { // Limit to 512MB for work arrays
            error("Schematic requires too much memory: ~" + estimatedMemoryMB + "MB");
            toggle();
            return;
        }

        // Debug spawn position
        BlockPos firstBlock = getPlacementPosition();
        info("Schematic will start at world position: " + firstBlock);

        generatePlacementCommands();
        if (placementCommands.isEmpty()) {
            error("No blocks to place in schematic");
            toggle();
            return;
        }

        // If volume optimization generated too many commands, fall back to setblock
        if (useVolumeOptimization.get() && placementCommands.size() > totalBlocks * 0.8) {
            warning("Volume optimization ineffective, falling back to setblock commands");
            // Regenerate with setblock
            placementCommands.clear();
            generateSetblockCommands(getPlacementPosition());
        }

        // Check command count for reasonableness
        if (placementCommands.size() > 500000) {
            warning("Large number of commands generated: " + placementCommands.size());
        }

        startPlacement();
        info("Starting schematic placement: " + placementCommands.size() + " commands");

        // Send all commands using CoreUpdater.startAuto like other modules
        CoreUpdater.startAuto(CoreCommand.corePositions, placementCommands, false, true, packetsPerTick.get());
    }

    @Override
    public void onDeactivate() {
        isPlacing = false;
        currentSchematic = null;
        placementCommands = null;
        expectedTicks = 0;
        CoreUpdater.onTick(); // Clear any pending updates
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isPlacing) return;

        if (expectedTicks > 0 || coreMode.get() == CoreUpdater.Mode.AUTO) {
            CoreUpdater.onTick();
            if (coreMode.get() != CoreUpdater.Mode.AUTO) expectedTicks--;
        }

        if (expectedTicks <= 0 && coreMode.get() != CoreUpdater.Mode.AUTO) {
            info("Schematic placement completed");
            toggle();
        }
    }

    private void loadSchematic() {
        Path schematicsDir = getSchematicsDirectory();
        if (schematicsDir == null) {
            error("Could not find schematics directory");
            return;
        }

        try {
            // Use reflection to load Litematica classes at runtime
            Class<?> litematicaSchematicClass = Class.forName("fi.dy.masa.litematica.schematic.LitematicaSchematic");
            java.lang.reflect.Method createFromFileMethod = litematicaSchematicClass.getMethod("createFromFile", Path.class, String.class);

            Object schematic = createFromFileMethod.invoke(null, schematicsDir, schematicFile.get());
            if (schematic == null) {
                error("Failed to load schematic: " + schematicFile.get());
                return;
            }

            currentSchematic = extractSchematicData(schematic);
        } catch (Exception e) {
            error("Failed to load schematic using Litematica: " + e.getMessage());
        }
    }

    private SchematicData extractSchematicData(Object schematic) {
        try {
            SchematicData data = new SchematicData();

            // Get size using reflection
            java.lang.reflect.Method getTotalSizeMethod = schematic.getClass().getMethod("getTotalSize");
            Vec3i size = (Vec3i) getTotalSizeMethod.invoke(schematic);
            data.width = size.getX();
            data.height = size.getY();
            data.length = size.getZ();

            // Get area sizes and positions
            java.lang.reflect.Method getAreaSizesMethod = schematic.getClass().getMethod("getAreaSizes");
            Map<String, BlockPos> areaSizes = (Map<String, BlockPos>) getAreaSizesMethod.invoke(schematic);

            java.lang.reflect.Method getAreaPositionsMethod = schematic.getClass().getMethod("getAreaPositions");
            Map<String, BlockPos> areaPositions = (Map<String, BlockPos>) getAreaPositionsMethod.invoke(schematic);

            if (areaSizes.isEmpty()) {
                throw new IllegalStateException("No areas found in schematic");
            }

            // Use the first region
            String regionName = areaSizes.keySet().iterator().next();
            BlockPos regionSize = areaSizes.get(regionName);
            BlockPos regionOrigin = areaPositions.get(regionName);
            data.origin = regionOrigin;

            info("Loading region: " + regionName + " size: " + regionSize + " origin: " + regionOrigin + " total areas: " + areaSizes.size());

            // Get the block container
            java.lang.reflect.Method getSubRegionContainerMethod = schematic.getClass().getMethod("getSubRegionContainer", String.class);
            Object container = getSubRegionContainerMethod.invoke(schematic, regionName);

            if (container == null) {
                throw new IllegalStateException("No block container found in schematic");
            }

            // Get container size to verify
            java.lang.reflect.Method getContainerSizeMethod = container.getClass().getMethod("getSize");
            Vec3i containerSize = (Vec3i) getContainerSizeMethod.invoke(container);
            info("Container size: " + containerSize);

            // Extract blocks and palette
            int volume = data.width * data.height * data.length;
            data.blocks = new int[volume];
            data.palette = new ArrayList<>();

            // Build palette from all blocks
            Map<String, Integer> paletteMap = new HashMap<>();
            int nonAirBlocks = 0;

            // Cache the get method for performance
            java.lang.reflect.Method getMethod = container.getClass().getMethod("get", int.class, int.class, int.class);

            for (int x = 0; x < data.width; x++) {
                for (int y = 0; y < data.height; y++) {
                    for (int z = 0; z < data.length; z++) {
                        // Get block state using container.get(x, y, z)
                        BlockState state = (BlockState) getMethod.invoke(container, x, y, z);

                        // Get proper block state string for commands
                        String stateString = getBlockStateString(state);

                        // Count non-air blocks
                        if (!stateString.contains("minecraft:air")) {
                            nonAirBlocks++;
                        }

                        Integer paletteIndex = paletteMap.get(stateString);
                        if (paletteIndex == null) {
                            paletteIndex = data.palette.size();
                            data.palette.add(stateString);
                            paletteMap.put(stateString, paletteIndex);
                        }

                        int index = (y * data.length + z) * data.width + x;
                        data.blocks[index] = paletteIndex;
                    }
                }
            }

            info("Schematic loaded: " + data.width + "x" + data.height + "x" + data.length +
                 " (" + (data.width * data.height * data.length) + " total blocks), " +
                 nonAirBlocks + " non-air blocks, " + data.palette.size() + " unique block types");

            return data;
        } catch (Exception e) {
            error("Failed to extract schematic data: " + e.getMessage());
            // Return dummy data
            SchematicData data = new SchematicData();
            data.width = 1;
            data.height = 1;
            data.length = 1;
            data.blocks = new int[]{0};
            data.palette = new ArrayList<>();
            data.palette.add("minecraft:stone");
            return data;
        }
    }

    private Path getSchematicsDirectory() {
        if (mc.runDirectory == null) return null;
        return Paths.get(mc.runDirectory.getAbsolutePath(), "schematics");
    }

    private void generatePlacementCommands() {
        if (currentSchematic == null) return;

        placementCommands = new ArrayList<>();
        BlockPos basePos = getPlacementPosition();

        if (useVolumeOptimization.get()) {
            // Simple X-only strip optimization
            generateOptimizedCommands(basePos);
        } else {
            // Generate individual setblock commands for each non-air block
            generateSetblockCommands(basePos);
        }
    }

    private void generateOptimizedCommands(BlockPos basePos) {
        // Simple X-only strip detection for /fill commands
        // Only merge along X axis for same blockstate, size >= minFillSize
        for (int y = 0; y < currentSchematic.height; y++) {
            for (int z = 0; z < currentSchematic.length; z++) {
                for (int x = 0; x < currentSchematic.width; ) {
                    int index = (y * currentSchematic.length + z) * currentSchematic.width + x;
                    if (index >= currentSchematic.blocks.length) {
                        x++;
                        continue;
                    }

                    int paletteIndex = currentSchematic.blocks[index];
                    if (paletteIndex < 0 || paletteIndex >= currentSchematic.palette.size()) {
                        x++;
                        continue;
                    }
                    String blockState = currentSchematic.palette.get(paletteIndex);
                    if (blockState == null) {
                        x++;
                        continue;
                    }

                    // Skip air blocks if ignoreAir is enabled
                    if (ignoreAir.get() && (blockState.equals("minecraft:air") || blockState.equals("minecraft:cave_air"))) {
                        x++;
                        continue;
                    }

                    // Skip waterlogged blocks if disabled
                    if (!placeWaterlogged.get() && blockState.contains("waterlogged=true")) {
                        x++;
                        continue;
                    }

                    // Find consecutive identical blocks along X axis
                    int stripLength = 1;
                    for (int i = 1; i < currentSchematic.width - x; i++) {
                        int nextIndex = (y * currentSchematic.length + z) * currentSchematic.width + (x + i);
                        if (nextIndex >= currentSchematic.blocks.length) break;

                        int nextPaletteIndex = currentSchematic.blocks[nextIndex];
                        if (nextPaletteIndex < 0 || nextPaletteIndex >= currentSchematic.palette.size()) break;
                        String nextBlockState = currentSchematic.palette.get(nextPaletteIndex);

                        // Stop if different blockstate or problematic block
                        if (!blockState.equals(nextBlockState)) break;
                        if (ignoreAir.get() && (nextBlockState.equals("minecraft:air") || nextBlockState.equals("minecraft:cave_air"))) break;
                        if (!placeWaterlogged.get() && nextBlockState.contains("waterlogged=true")) break;

                        stripLength++;
                    }

                    // Decide whether to use fill or individual setblock
                    if (stripLength >= minFillSize.get()) {
                        // Use /fill command for the strip
                        generateFillCommand(basePos, x, y, z, stripLength, blockState);
                        x += stripLength; // Skip processed blocks
                    } else {
                        // Use individual setblock commands for small strips
                        for (int i = 0; i < stripLength; i++) {
                            generateSetblockCommand(basePos, x + i, y, z, blockState);
                        }
                        x += stripLength;
                    }
                }
            }
        }
    }

    private void generateFillCommand(BlockPos basePos, int x, int y, int z, int length, String blockState) {
        // Calculate start and end positions (both at same Y,Z, spanning X)
        BlockPos startPos = basePos.add(x, y, z);
        BlockPos endPos = startPos.add(length - 1, 0, 0);

        // Validate world bounds (X, Y, Z)
        int startX = startPos.getX();
        int endX = endPos.getX();
        int startY = startPos.getY();
        int endY = endPos.getY();
        int startZ = startPos.getZ();
        int endZ = endPos.getZ();
        if (startX < -30000000 || startX > 30000000 || endX < -30000000 || endX > 30000000 ||
            startY < -64 || startY > 319 || endY < -64 || endY > 319 ||
            startZ < -30000000 || startZ > 30000000 || endZ < -30000000 || endZ > 30000000) {
            // Fall back to individual setblocks for this strip
            for (int i = 0; i < length; i++) {
                generateSetblockCommand(basePos, x + i, y, z, blockState);
            }
            return;
        }

        String fillCommand;
        if (ignoreAir.get()) {
            fillCommand = String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s replace air",
                    startPos.getX(), startPos.getY(), startPos.getZ(),
                    endPos.getX(), endPos.getY(), endPos.getZ(),
                    blockState);
        } else {
            fillCommand = String.format(Locale.ROOT, "fill %d %d %d %d %d %d %s",
                    startPos.getX(), startPos.getY(), startPos.getZ(),
                    endPos.getX(), endPos.getY(), endPos.getZ(),
                    blockState);
        }

        placementCommands.add(fillCommand);
    }

    private void generateSetblockCommand(BlockPos basePos, int x, int y, int z, String blockState) {
        BlockPos worldPos = basePos.add(x, y, z);

        // Validate world bounds (X, Y, Z)
        int worldX = worldPos.getX();
        int worldY = worldPos.getY();
        int worldZ = worldPos.getZ();
        if (worldX < -30000000 || worldX > 30000000 ||
            worldY < -64 || worldY > 319 ||
            worldZ < -30000000 || worldZ > 30000000) {
            return; // Skip blocks outside world bounds
        }

        String setblockCommand = String.format(Locale.ROOT, "setblock %d %d %d %s",
                worldPos.getX(), worldPos.getY(), worldPos.getZ(), blockState);

        placementCommands.add(setblockCommand);
    }

    private void generateSetblockCommands(BlockPos basePos) {
        for (int x = 0; x < currentSchematic.width; x++) {
            for (int y = 0; y < currentSchematic.height; y++) {
                for (int z = 0; z < currentSchematic.length; z++) {
                    int index = (y * currentSchematic.length + z) * currentSchematic.width + x;
                    if (index >= currentSchematic.blocks.length) continue;

                    int paletteIndex = currentSchematic.blocks[index];
                    if (paletteIndex < 0 || paletteIndex >= currentSchematic.palette.size()) continue;
                    String blockState = currentSchematic.palette.get(paletteIndex);
                    if (blockState == null) continue;

                    // Skip air blocks if ignoreAir is enabled
                    if (ignoreAir.get() && (blockState.equals("minecraft:air") || blockState.equals("minecraft:cave_air"))) {
                        continue;
                    }

                    // Skip waterlogged blocks if disabled
                    if (!placeWaterlogged.get() && blockState.contains("waterlogged=true")) {
                        continue;
                    }

                    // Calculate world position (schematic coordinates + base position)
                    BlockPos worldPos = basePos.add(x, y, z);

                    // Validate world bounds (X, Y, Z)
                    int worldX = worldPos.getX();
                    int worldY = worldPos.getY();
                    int worldZ = worldPos.getZ();
                    if (worldX < -30000000 || worldX > 30000000 ||
                        worldY < -64 || worldY > 319 ||
                        worldZ < -30000000 || worldZ > 30000000) {
                        continue; // Skip blocks outside world bounds
                    }

                    // Generate setblock command
                    String setblockCommand = String.format(Locale.ROOT, "setblock %d %d %d %s",
                            worldPos.getX(), worldPos.getY(), worldPos.getZ(), blockState);

                    placementCommands.add(setblockCommand);
                }
            }
        }
    }

    private int getPaletteIndex(int[] blocks, int index) {
        // Our new format stores palette indices directly in int array
        return blocks[index];
    }

    private BlockPos getPlacementPosition() {
        if (mc.player == null) return BlockPos.ORIGIN;

        BlockPos basePos;
        switch (placementMode.get()) {
            case Relative -> basePos = mc.player.getBlockPos(); // player feet
            case Absolute -> basePos = BlockPos.ORIGIN;
            default -> basePos = BlockPos.ORIGIN;
        }

        // Apply placement offset (X, Y, Z)
        basePos = basePos.add(placementOffset.get());

        // Only subtract origin X/Z to align horizontally
        if (currentSchematic != null && currentSchematic.origin != null) {
            basePos = basePos.subtract(new BlockPos(currentSchematic.origin.getX(), 0, currentSchematic.origin.getZ()));
        }

        return basePos;
    }

    private void startPlacement() {
        isPlacing = true;
        expectedTicks = (placementCommands.size() + packetsPerTick.get() - 1) / packetsPerTick.get();
        info("Expected ticks: " + expectedTicks + " for " + placementCommands.size() + " commands");
    }

    private String getBlockStateString(BlockState state) {
        // Get the block's registry name
        String blockName = state.getBlock().getRegistryEntry().getIdAsString();

        // Check if the state has any properties
        if (state.getEntries().isEmpty()) {
            return blockName;
        }

        // Build the properties string in sorted order for consistency
        StringBuilder properties = new StringBuilder();
        properties.append('[');

        // Sort properties by key name for consistent command format
        state.getEntries().entrySet().stream()
            .sorted((a, b) -> a.getKey().getName().compareTo(b.getKey().getName()))
            .forEach(entry -> {
                if (properties.length() > 1) {
                    properties.append(',');
                }
                properties.append(entry.getKey().getName()).append('=').append(entry.getValue());
            });

        properties.append(']');
        return blockName + properties.toString();
    }

    private static class SchematicData {
        int width, height, length;
        BlockPos origin;           // region origin offset
        int[] blocks;              // palette indices
        List<String> palette;      // blockstate strings
    }

    public enum PlacementMode {
        Relative,
        Absolute
    }
}

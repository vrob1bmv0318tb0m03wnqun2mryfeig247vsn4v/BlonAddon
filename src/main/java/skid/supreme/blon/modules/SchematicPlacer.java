package skid.supreme.blon.modules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.unimi.dsi.fastutil.longs.LongArrayList;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.block.BlockState;
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
            .build());

    private final Setting<CoreUpdater.Mode> coreMode = sgCore.add(new EnumSetting.Builder<CoreUpdater.Mode>()
            .name("core-mode")
            .description("Command block execution mode")
            .defaultValue(CoreUpdater.Mode.SINGLE)
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

    private SchematicData currentSchematic;
    private List<String> placementCommands;
    private boolean isPlacing;
    private int commandIndex;

    // Volume optimization data
    private int[][][] workArr;
    private LongArrayList fillVolumes;

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

        generatePlacementCommands();
        if (placementCommands.isEmpty()) {
            error("No blocks to place in schematic");
            toggle();
            return;
        }

        startPlacement();
        info("Starting schematic placement: " + placementCommands.size() + " commands");
    }

    @Override
    public void onDeactivate() {
        isPlacing = false;
        currentSchematic = null;
        placementCommands = null;
        commandIndex = 0;
        CoreUpdater.onTick(); // Clear any pending updates
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isPlacing || placementCommands == null) return;

        if (commandIndex >= placementCommands.size()) {
            info("Schematic placement completed");
            toggle();
            return;
        }

        // Send next batch of commands immediately
        int endIndex = Math.min(commandIndex + packetsPerTick.get(), placementCommands.size());
        List<String> batch = placementCommands.subList(commandIndex, endIndex);

        CoreUpdater.sendCommandsImmediately(CoreCommand.corePositions, batch, coreMode.get(), false, false);
        commandIndex = endIndex;
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

            for (int x = 0; x < data.width; x++) {
                for (int y = 0; y < data.height; y++) {
                    for (int z = 0; z < data.length; z++) {
                        // Get block state using container.get(x, y, z)
                        java.lang.reflect.Method getMethod = container.getClass().getMethod("get", int.class, int.class, int.class);
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

        // Generate individual setblock commands for each non-air block
        generateSetblockCommands(basePos);
    }

    private void generateSetblockCommands(BlockPos basePos) {
        for (int x = 0; x < currentSchematic.width; x++) {
            for (int y = 0; y < currentSchematic.height; y++) {
                for (int z = 0; z < currentSchematic.length; z++) {
                    int index = (y * currentSchematic.length + z) * currentSchematic.width + x;
                    if (index >= currentSchematic.blocks.length) continue;

                    int paletteIndex = currentSchematic.blocks[index];
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

                    // Calculate world position (schematic coordinates + region origin + base position)
                    BlockPos schematicPos = new BlockPos(x, y, z);
                    if (currentSchematic.origin != null) {
                        schematicPos = schematicPos.add(currentSchematic.origin);
                    }
                    BlockPos worldPos = basePos.add(schematicPos);

                    // Generate setblock command
                    String setblockCommand = String.format("setblock %d %d %d %s",
                            worldPos.getX(), worldPos.getY(), worldPos.getZ(), blockState);

                    placementCommands.add(setblockCommand);
                }
            }
        }
    }

    private void generateFillVolumes(BlockPos basePos) {
        if (currentSchematic == null) return;

        int width = currentSchematic.width;
        int height = currentSchematic.height;
        int length = currentSchematic.length;

        // Initialize work array for the entire schematic
        workArr = new int[width][height][length];
        fillVolumes = new LongArrayList();

        // Generate strips along the X axis (EAST direction)
        generateStrips(workArr, 0, 1, 2, width, height, length); // Direction: EAST (positive X)
        // Combine strips into layers and layers into volumes
        combineStripsToLayers(workArr, 0, 1, 2, width, height, length); // Directions: EAST, SOUTH (positive Z), UP (positive Y)
    }

    private void generateStrips(int[][][] workArr, int stripDir, int combineDir, int layerDir, int width, int height, int length) {
        // For each layer in the strip direction
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * length + z) * width + x;
                    if (index >= currentSchematic.blocks.length) continue;

                    int paletteIndex = getPaletteIndex(currentSchematic.blocks, index);
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

                    // Find the length of consecutive identical blocks in the strip direction
                    int stripLength = 1;
                    for (int i = 1; i < width - x; i++) {
                        int nextIndex = (y * length + z) * width + (x + i);
                        if (nextIndex >= currentSchematic.blocks.length) break;

                        int nextPaletteIndex = getPaletteIndex(currentSchematic.blocks, nextIndex);
                        String nextBlockState = currentSchematic.palette.get(nextPaletteIndex);

                        if (!blockState.equals(nextBlockState)) break;
                        stripLength++;
                    }

                    workArr[x][y][z] = stripLength;
                    x += stripLength - 1; // Skip processed blocks
                }
            }
        }
    }

    private void combineStripsToLayers(int[][][] workArr, int stripDir, int combineDir, int layerDir, int width, int height, int length) {
        // Combine strips into layers (2D)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    int stripLength = workArr[x][y][z];
                    if (stripLength == 0) continue;

                    int index = (y * length + z) * width + x;
                    int paletteIndex = getPaletteIndex(currentSchematic.blocks, index);
                    String blockState = currentSchematic.palette.get(paletteIndex);

                    // Find consecutive strips in the combine direction
                    int layerCount = 1;
                    for (int i = 1; i < length - z; i++) {
                        int nextIndex = (y * length + (z + i)) * width + x;
                        if (nextIndex >= currentSchematic.blocks.length) break;

                        int nextPaletteIndex = getPaletteIndex(currentSchematic.blocks, nextIndex);
                        String nextBlockState = currentSchematic.palette.get(nextPaletteIndex);

                        if (!blockState.equals(nextBlockState) || workArr[x][y][z + i] != stripLength) break;
                        layerCount++;
                    }

                    // Clear processed strips
                    for (int i = 0; i < layerCount; i++) {
                        workArr[x][y][z + i] = 0;
                    }

                    // Encode the layer information
                    int packedSize = (stripLength & 0xF) | ((layerCount & 0xF) << 4);
                    workArr[x][y][z] = packedSize;
                    z += layerCount - 1; // Skip processed strips
                }
            }
        }

        // Combine layers into volumes (3D)
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < height; y++) {
                    int packedSize = workArr[x][y][z];
                    if (packedSize == 0) continue;

                    int index = (y * length + z) * width + x;
                    int paletteIndex = getPaletteIndex(currentSchematic.blocks, index);
                    String blockState = currentSchematic.palette.get(paletteIndex);

                    // Find consecutive layers in the layer direction
                    int volumeCount = 1;
                    for (int i = 1; i < height - y; i++) {
                        int nextIndex = ((y + i) * length + z) * width + x;
                        if (nextIndex >= currentSchematic.blocks.length) break;

                        int nextPaletteIndex = getPaletteIndex(currentSchematic.blocks, nextIndex);
                        String nextBlockState = currentSchematic.palette.get(nextPaletteIndex);

                        if (!blockState.equals(nextBlockState) || workArr[x][y + i][z] != packedSize) break;
                        volumeCount++;
                    }

                    // Clear processed layers
                    for (int i = 0; i < volumeCount; i++) {
                        workArr[x][y + i][z] = 0;
                    }

                    // Create volume encoding: [coordBits(24)][height(8)][depth(8)][width(8)]
                    int stripLength = packedSize & 0xF;
                    int layerCount = (packedSize >> 4) & 0xF;
                    // Pack coordinates into 24 bits (x:8, y:8, z:8) - sufficient for most schematics
                    long coordBits = ((long) (x & 0xFF) << 16) | ((long) (y & 0xFF) << 8) | (long) (z & 0xFF);
                    long encodedVolume = (coordBits << 24) | ((long) (volumeCount - 1) << 16) | ((long) (layerCount - 1) << 8) | (long) (stripLength - 1);
                    fillVolumes.add(encodedVolume);
                    y += volumeCount - 1; // Skip processed layers
                }
            }
        }
    }

    private void generateCommandsFromVolumes(BlockPos basePos) {
        for (long encodedVolume : fillVolumes) {
            // Decode volume: [coordBits(48)][height(8)][depth(8)][width(8)]
            long coordBits = encodedVolume >>> 24;
            int x = unpackX(coordBits);
            int y = unpackY(coordBits);
            int z = unpackZ(coordBits);

            int volumeWidth = (int) ((encodedVolume >> 8) & 0xFF) + 1;
            int volumeDepth = (int) ((encodedVolume >> 16) & 0xFF) + 1;
            int volumeHeight = (int) ((encodedVolume >> 24) & 0xFF) + 1;

            // Get block state from the schematic
            int index = (y * currentSchematic.length + z) * currentSchematic.width + x;
            if (index >= currentSchematic.blocks.length) continue;

            int paletteIndex = getPaletteIndex(currentSchematic.blocks, index);
            String blockState = currentSchematic.palette.get(paletteIndex);
            if (blockState == null) continue;

            // Debug: Log the first few blocks
            if (placementCommands.size() < 3) {
                info("Block at (" + x + "," + y + "," + z + "): paletteIndex=" + paletteIndex + ", blockState='" + blockState + "'");
            }

            // Calculate world positions
            BlockPos startPos = basePos.add(x, y, z);
            BlockPos endPos = startPos.add(volumeWidth - 1, volumeHeight - 1, volumeDepth - 1);

            // Generate fill command
            String fillCommand = String.format("fill %d %d %d %d %d %d %s",
                    startPos.getX(), startPos.getY(), startPos.getZ(),
                    endPos.getX(), endPos.getY(), endPos.getZ(),
                    blockState);

            placementCommands.add(fillCommand);
        }
    }

    private long packCoordinate(int x, int y, int z) {
        return ((long) x & 0xFFFF) | (((long) y & 0xFFFF) << 16) | (((long) z & 0xFFFF) << 32);
    }

    private int unpackX(long coord) {
        return (int) (coord & 0xFF);
    }

    private int unpackY(long coord) {
        return (int) ((coord >> 8) & 0xFF);
    }

    private int unpackZ(long coord) {
        return (int) ((coord >> 16) & 0xFF);
    }

    private int getPaletteIndex(int[] blocks, int index) {
        // Our new format stores palette indices directly in int array
        return blocks[index];
    }

    private BlockPos getPlacementPosition() {
        BlockPos basePos;
        switch (placementMode.get()) {
            case Relative -> {
                if (mc.player == null) return BlockPos.ORIGIN;
                basePos = mc.player.getBlockPos();
            }
            case Absolute -> {
                basePos = BlockPos.ORIGIN;
            }
            default -> basePos = BlockPos.ORIGIN;
        }

        return basePos.add(placementOffset.get());
    }

    private void startPlacement() {
        isPlacing = true;
        commandIndex = 0;
    }

    private String getBlockStateString(BlockState state) {
        // Get the block's registry name
        String blockName = state.getBlock().getRegistryEntry().getIdAsString();

        // Check if the state has any properties
        if (state.getEntries().isEmpty()) {
            return blockName;
        }

        // Build the properties string
        StringBuilder properties = new StringBuilder();
        properties.append('[');

        boolean first = true;
        for (Map.Entry<?, ?> entry : state.getEntries().entrySet()) {
            if (!first) {
                properties.append(',');
            }
            properties.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }

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

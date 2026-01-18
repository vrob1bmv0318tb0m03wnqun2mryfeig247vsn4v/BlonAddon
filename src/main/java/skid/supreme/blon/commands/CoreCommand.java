package skid.supreme.blon.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import skid.supreme.blon.Blon;

import java.util.ArrayList;
import java.util.List;

public class CoreCommand extends Command {

    // Static list accessible by other commands
    public static final List<BlockPos> corePositions = new ArrayList<>();
    public static final List<BlockPos> MODEL_CORES = new ArrayList<>();
    public static final List<BlockPos> FIRE_CORES = new ArrayList<>();

    // Static variables for refresh
    private static Integer lastCenterX = null;
    private static Integer lastCenterZ = null;
    private static Integer lastY = null;
    private static Integer lastWidth = 16;
    private static Integer lastLength = 16;
    private static Integer lastHeight = 32;
    private static Integer lastChunkX = null;
    private static Integer lastChunkZ = null;
    private static String lastBlock = "minecraft:red_stained_glass";

    public CoreCommand() {
        super("core", "Spawns a core with custom dimensions: .core [w] [l] [h] [block]. Default 16x16x32 with red_stained_glass.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Default: 16x16x32
        builder.executes(ctx -> {
            placeCore(16, 16, 32, "minecraft:red_stained_glass", false);
            return SINGLE_SUCCESS;
        });

        // .core <height> -> 16x16x<height>
        builder.then(argument("height", IntegerArgumentType.integer(1, 256)).executes(ctx -> {
            int height = IntegerArgumentType.getInteger(ctx, "height");
            placeCore(16, 16, height, "minecraft:red_stained_glass", false);
            return SINGLE_SUCCESS;
        }));

        // .core <width> <length> <height>
        builder.then(argument("width", IntegerArgumentType.integer(3, 128))
                .then(argument("length", IntegerArgumentType.integer(3, 128))
                        .then(argument("height", IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> {
                                    int width = IntegerArgumentType.getInteger(ctx, "width");
                                    int length = IntegerArgumentType.getInteger(ctx, "length");
                                    int height = IntegerArgumentType.getInteger(ctx, "height");
                                    placeCore(width, length, height, "minecraft:red_stained_glass", false);
                                    return SINGLE_SUCCESS;
                                }))));

        // .core <width> <length> <height> <block>
        builder.then(argument("width", IntegerArgumentType.integer(3, 128))
                .then(argument("length", IntegerArgumentType.integer(3, 128))
                        .then(argument("height", IntegerArgumentType.integer(1, 256))
                                .then(argument("block", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            int width = IntegerArgumentType.getInteger(ctx, "width");
                                            int length = IntegerArgumentType.getInteger(ctx, "length");
                                            int height = IntegerArgumentType.getInteger(ctx, "height");
                                            String block = StringArgumentType.getString(ctx, "block");
                                            placeCore(width, length, height, block, false);
                                            return SINGLE_SUCCESS;
                                        })))));

        // .core <block> 16x16x32 with custom block
        builder.then(argument("block", StringArgumentType.greedyString()).executes(ctx -> {
            String block = StringArgumentType.getString(ctx, "block");
            placeCore(16, 16, 32, block, false);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("decore").executes(ctx -> {
            decore();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("refresh").executes(ctx -> {
            refreshCore();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("version").executes(ctx -> {
            Blon.use12111Format = !Blon.use12111Format;
            Blon.saveGameruleVersion();
            info("Switched to " + (Blon.use12111Format ? "1.21.11" : "1.21.10") + " gamerule format.");
            return SINGLE_SUCCESS;
        }));
    }

    // Calculate positions based on specified dimensions
    private void calculateCorePositions(int width, int length, int height, boolean isRefresh) {
        corePositions.clear();
        if (mc.player == null)
            return;

        int chunkX, chunkZ, centerX, centerZ, y;

        if (isRefresh && lastCenterX != null) {
            chunkX = lastChunkX;
            chunkZ = lastChunkZ;
            centerX = lastCenterX;
            centerZ = lastCenterZ;
            y = lastY;
        } else {
            BlockPos playerPos = mc.player.getBlockPos();
            chunkX = playerPos.getX() >> 4;
            chunkZ = playerPos.getZ() >> 4;
            centerX = (chunkX << 4) + 8;
            centerZ = (chunkZ << 4) + 8;
            y = Math.min(playerPos.getY() + 60, 303);
        }

        int halfWidth = width / 2;
        int halfLength = length / 2;

        int startX = centerX - halfWidth;
        int endX = startX + width - 1;

        int startZ = centerZ - halfLength;
        int endZ = startZ + length - 1;

        for (int x = startX + 1; x < endX; x++) {
            for (int z = startZ + 1; z < endZ; z++) {
                for (int h = y + 1; h <= y + height - 2; h++) {
                    corePositions.add(new BlockPos(x, h, z));
                }
            }
        }

        MODEL_CORES.clear();
        FIRE_CORES.clear();
        int split = corePositions.size() / 2;
        MODEL_CORES.addAll(corePositions.subList(0, split));
        FIRE_CORES.addAll(corePositions.subList(split, corePositions.size()));

        lastCenterX = centerX;
        lastCenterZ = centerZ;
        lastY = y;
        lastWidth = width;
        lastLength = length;
        lastHeight = height;
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
    }

    private void placeCore(int width, int length, int height, String block, boolean isRefresh) {
        if (mc.player == null)
            return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }
        calculateCorePositions(width, length, height, isRefresh);
        lastBlock = block;
        int chunkX = lastChunkX;
        int chunkZ = lastChunkZ;
        int centerX = lastCenterX;
        int centerZ = lastCenterZ;
        int y = lastY;

        int halfWidth = width / 2;
        int halfLength = length / 2;
        int startX = centerX - halfWidth;
        int endX = startX + width - 1;
        int startZ = centerZ - halfLength;
        int endZ = startZ + length - 1;
        mc.player.networkHandler.sendChatCommand("forceload add " + chunkX + " " + chunkZ);
        if (!Blon.use12111Format) {
            mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 2000000");
        }
        String outputRule = Blon.use12111Format ? "command_Block_Output" : "commandBlockOutput";
        mc.player.networkHandler.sendChatCommand("gamerule " + outputRule + " false");
        mc.player.networkHandler.sendChatCommand(
                "fill " +
                        startX + " " + y + " " + startZ + " " +
                        endX + " " + (y + height - 1) + " " + endZ +
                        " " + block);

        mc.player.networkHandler.sendChatCommand(
                "fill " +
                        (startX + 1) + " " + (y + 1) + " " + (startZ + 1) + " " +
                        (endX - 1) + " " + (y + height - 2) + " " + (endZ - 1) +
                        " minecraft:command_block[facing=up]{auto:0b}");

        if (!Blon.use12111Format) {
            mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 32768");
        }

        info(width + "x" + length + "x" + height + " core spawned in chunk (" + chunkX + ", " + chunkZ + ") at Y=" + y + " with " + block);
    }

    private void decore() {
        if (mc.player == null)
            return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        if (lastCenterX == null) {
            error("No core to remove. Place one first.");
            return;
        }

        int chunkX = lastChunkX;
        int chunkZ = lastChunkZ;
        int centerX = lastCenterX;
        int centerZ = lastCenterZ;
        int y = lastY;
        int width = lastWidth;
        int length = lastLength;
        int height = lastHeight;

        int halfWidth = width / 2;
        int halfLength = length / 2;
        int startX = centerX - halfWidth;
        int endX = startX + width - 1;
        int startZ = centerZ - halfLength;
        int endZ = startZ + length - 1;

        mc.player.networkHandler.sendChatCommand("forceload add " + chunkX + " " + chunkZ);

        if (!Blon.use12111Format) {
            mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 2000000");
        }

        mc.player.networkHandler.sendChatCommand(
                "fill " +
                        startX + " " + y + " " + startZ + " " +
                        endX + " " + (y + height - 1) + " " + endZ +
                        " minecraft:air");

        if (!Blon.use12111Format) {
            mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 32768");
        }

        corePositions.clear();
        MODEL_CORES.clear();
        FIRE_CORES.clear();
        lastCenterX = null;
        lastCenterZ = null;
        lastY = null;
        lastWidth = 16;
        lastLength = 16;
        lastHeight = 32;
        lastChunkX = null;
        lastChunkZ = null;
        lastBlock = "minecraft:red_stained_glass";

        info("Core removed in chunk (" + chunkX + ", " + chunkZ + ") at Y=" + y);
    }

    private void refreshCore() {
        if (mc.player == null)
            return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        if (lastCenterX == null) {
            error("No core to refresh.");
            return;
        }

        // reuse placeCore logic
        placeCore(lastWidth, lastLength, lastHeight, lastBlock, true);
        info("Refreshed");
    }
}

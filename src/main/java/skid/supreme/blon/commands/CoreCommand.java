package skid.supreme.blon.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CoreCommand extends Command {

    // Static list accessible by other commands
    public static final List<BlockPos> corePositions = new ArrayList<>();

    // Static variables for refresh
    private static Integer lastCenterX = null;
    private static Integer lastCenterZ = null;
    private static Integer lastY = null;
    private static Integer lastWidth = 16;
    private static Integer lastLength = 16;
    private static Integer lastHeight = 32;
    private static Integer lastChunkX = null;
    private static Integer lastChunkZ = null;

    public CoreCommand() {
        super("core", "Spawns a core with custom dimensions: .core [w] [l] [h]. Default 16x16x32.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Default: 16x16x32
        builder.executes(ctx -> {
            placeCore(16, 16, 32, false);
            return SINGLE_SUCCESS;
        });

        // .core <height> -> 16x16x<height>
        builder.then(argument("height", IntegerArgumentType.integer(1, 256)).executes(ctx -> {
            int height = IntegerArgumentType.getInteger(ctx, "height");
            placeCore(16, 16, height, false);
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
                                    placeCore(width, length, height, false);
                                    return SINGLE_SUCCESS;
                                }))));

        builder.then(literal("decore").executes(ctx -> {
            decore();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("refresh").executes(ctx -> {
            refreshCore();
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
            // Use stored values
            chunkX = lastChunkX;
            chunkZ = lastChunkZ;
            centerX = lastCenterX;
            centerZ = lastCenterZ;
            y = lastY;
        } else {
            // New position from player
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

        // Inner bounds (shrink by 1 on all sides)
        for (int x = startX + 1; x < endX; x++) {
            for (int z = startZ + 1; z < endZ; z++) {
                for (int h = y + 1; h <= y + height - 2; h++) {
                    corePositions.add(new BlockPos(x, h, z));
                }
            }
        }

        // Store last values
        lastCenterX = centerX;
        lastCenterZ = centerZ;
        lastY = y;
        lastWidth = width;
        lastLength = length;
        lastHeight = height;
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;
    }

    private void placeCore(int width, int length, int height, boolean isRefresh) {
        if (mc.player == null)
            return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        // Must update positions and static vars first
        calculateCorePositions(width, length, height, isRefresh);

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

        // Force load chunk
        mc.player.networkHandler.sendChatCommand("forceload add " + chunkX + " " + chunkZ);

        // Increase gamerule limit temporarily
        mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 2000000");

        // Shell
        mc.player.networkHandler.sendChatCommand(
                "fill " +
                        startX + " " + y + " " + startZ + " " +
                        endX + " " + (y + height - 1) + " " + endZ +
                        " minecraft:red_stained_glass");

        // Inner command blocks
        mc.player.networkHandler.sendChatCommand(
                "fill " +
                        (startX + 1) + " " + (y + 1) + " " + (startZ + 1) + " " +
                        (endX - 1) + " " + (y + height - 2) + " " + (endZ - 1) +
                        " minecraft:command_block[facing=up]{auto:0b}");

        // Reset gamerule
        mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 32768");

        info(width + "x" + length + "x" + height + " core spawned in chunk (" + chunkX + ", " + chunkZ + ") at Y=" + y);
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

        // Increase limit for clearing large cores
        mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 2000000");

        mc.player.networkHandler.sendChatCommand(
                "fill " +
                        startX + " " + y + " " + startZ + " " +
                        endX + " " + (y + height - 1) + " " + endZ +
                        " minecraft:air");

        mc.player.networkHandler.sendChatCommand("gamerule commandModificationBlockLimit 32768");

        corePositions.clear();
        lastCenterX = null;
        lastCenterZ = null;
        lastY = null;
        lastWidth = 16;
        lastLength = 16;
        lastHeight = 32;
        lastChunkX = null;
        lastChunkZ = null;

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
        placeCore(lastWidth, lastLength, lastHeight, true);
        info("Refreshed");
    }
}

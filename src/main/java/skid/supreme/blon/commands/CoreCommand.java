package skid.supreme.blon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CoreCommand extends Command {

    // Static list accessible by other commands
    public static final List<BlockPos> corePositions = new ArrayList<>();

    public CoreCommand() {
        super("core", "Spawns a chunk-perfect 16x16x32 command block core.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            calculateCorePositions();
            placeCore();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("decore").executes(ctx -> {
            decore();
            return SINGLE_SUCCESS;
        }));
    }

    // Calculate positions only
    private void calculateCorePositions() {
        corePositions.clear();
        if (mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();

        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;

        int y = Math.min(playerPos.getY() + 60, 303);
        int height = 32;

        for (int x = centerX - 7; x <= centerX + 6; x++) {
            for (int z = centerZ - 7; z <= centerZ + 6; z++) {
                for (int h = y + 1; h <= y + height - 2; h++) {
                    corePositions.add(new BlockPos(x, h, z));
                }
            }
        }
    }

    // Execute the fill commands as before
    private void placeCore() {
        if (mc.player == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        int y = Math.min(playerPos.getY() + 60, 303);
        int height = 32;

        // Force load the chunk
        mc.player.networkHandler.sendChatCommand("forceload add " + chunkX + " " + chunkZ);

        // Red stained glass shell
        mc.player.networkHandler.sendChatCommand(
            "fill " +
            (centerX - 8) + " " + y + " " + (centerZ - 8) + " " +
            (centerX + 7) + " " + (y + height - 1) + " " + (centerZ + 7) +
            " minecraft:red_stained_glass"
        );

        // Inner command blocks
        mc.player.networkHandler.sendChatCommand(
            "fill " +
            (centerX - 7) + " " + (y + 1) + " " + (centerZ - 7) + " " +
            (centerX + 6) + " " + (y + height - 2) + " " + (centerZ + 6) +
            " minecraft:command_block[facing=up]{auto:1b}"
        );

        info("16x16x32 core spawned in chunk (" + chunkX + ", " + chunkZ + ") at Y=" + y);
    }

    private void decore() {
        if (mc.player == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        int y = Math.min(playerPos.getY() + 60, 303);
        int height = 32;

        // Force load the chunk
        mc.player.networkHandler.sendChatCommand("forceload add " + chunkX + " " + chunkZ);

        // Fill the entire core area with air
        mc.player.networkHandler.sendChatCommand(
            "fill " +
            (centerX - 8) + " " + y + " " + (centerZ - 8) + " " +
            (centerX + 7) + " " + (y + height - 1) + " " + (centerZ + 7) +
            " minecraft:air"
        );

        info("16x16x32 core removed in chunk (" + chunkX + ", " + chunkZ + ") at Y=" + y);
    }
}

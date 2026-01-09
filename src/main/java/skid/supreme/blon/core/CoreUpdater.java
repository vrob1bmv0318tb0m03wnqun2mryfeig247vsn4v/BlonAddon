package skid.supreme.blon.core;

import java.util.Iterator;
import java.util.List;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

public class CoreUpdater {

    private static Iterator<BlockPos> iterator;
    private static int delayTicks;

    private static String command;
    private static List<String> commands;
    private static int currentCommandIndex;
    private static Mode mode;
    private static boolean conditional;
    private static boolean trackOutput;
    private static int packetsPerTick = 1;

    /* ================= ENUM ================= */

    public enum Mode {
        AUTO,        // repeating
        REDSTONE,    // impulse
        SEQUENCE     // chain
    }

    /* ================= START METHODS ================= */

    public static void startAuto(
        List<BlockPos> positions,
        List<String> cmds,
        boolean conditional,
        boolean trackOutput,
        int ppt
    ) {
        start(positions, cmds, Mode.AUTO, conditional, trackOutput, ppt);
    }

    public static void startRedstone(
        List<BlockPos> positions,
        List<String> cmds,
        boolean conditional,
        boolean trackOutput,
        int ppt
    ) {
        start(positions, cmds, Mode.REDSTONE, conditional, trackOutput, ppt);
    }

    public static void startSequence(
        List<BlockPos> positions,
        List<String> cmds,
        boolean conditional,
        boolean trackOutput,
        int ppt
    ) {
        start(positions, cmds, Mode.SEQUENCE, conditional, trackOutput, ppt);
    }

    /* ================= INTERNAL START ================= */

    private static void start(
        List<BlockPos> positions,
        List<String> cmds,
        Mode mode,
        boolean conditional,
        boolean trackOutput,
        int ppt
    ) {
        if (cmds == null || cmds.isEmpty()) return;

        CoreUpdater.iterator = positions.iterator();
        CoreUpdater.mode = mode;
        CoreUpdater.conditional = conditional;
        CoreUpdater.trackOutput = trackOutput;
        CoreUpdater.packetsPerTick = Math.max(1, ppt);
        CoreUpdater.currentCommandIndex = 0;

        if (cmds.size() == 1) {
            CoreUpdater.command = cmds.get(0);
            CoreUpdater.commands = null;
        } else {
            CoreUpdater.commands = new java.util.ArrayList<>(cmds);
            CoreUpdater.command = null;
        }

        // allow server to finish /fill + tile creation
        CoreUpdater.delayTicks = 0;
    }

    /* ================= TICK ================= */

    public static void onTick() {
        if (MeteorClient.mc.player == null || MeteorClient.mc.world == null) return;

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        if (iterator == null) return;

        for (int i = 0; i < packetsPerTick; i++) {
            if (!iterator.hasNext()) return;

            BlockPos pos = iterator.next();
            if (!isCommandBlock(pos)) continue;

            String cmd = commands != null ? commands.get(currentCommandIndex++) : command;
            sendPacket(pos, cmd);
        }
    }

    /* ================= PACKET ================= */

    private static void sendPacket(BlockPos pos, String cmd) {
        CommandBlockBlockEntity.Type type;
        boolean alwaysActive;

        switch (mode) {
            case AUTO -> {
                type = CommandBlockBlockEntity.Type.AUTO;
                alwaysActive = true;
            }
            case REDSTONE -> {
                type = CommandBlockBlockEntity.Type.REDSTONE;
                alwaysActive = false;
            }
            case SEQUENCE -> {
                type = CommandBlockBlockEntity.Type.SEQUENCE;
                alwaysActive = false; // REQUIRED
            }
            default -> {
                return;
            }
        }

        MeteorClient.mc.player.networkHandler.sendPacket(
            new UpdateCommandBlockC2SPacket(
                pos,
                cmd,
                type,
                trackOutput,
                conditional,
                alwaysActive
            )
        );
    }

    /* ================= VALIDATION ================= */

    private static boolean isCommandBlock(BlockPos pos) {
        Block block = MeteorClient.mc.world.getBlockState(pos).getBlock();
        return block == Blocks.COMMAND_BLOCK
            || block == Blocks.REPEATING_COMMAND_BLOCK
            || block == Blocks.CHAIN_COMMAND_BLOCK;
    }
}

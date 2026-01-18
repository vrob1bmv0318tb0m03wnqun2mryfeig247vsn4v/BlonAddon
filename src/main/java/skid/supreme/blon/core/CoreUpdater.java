package skid.supreme.blon.core;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

public class CoreUpdater {

    private static List<BlockPos> positions = new ArrayList<>();
    private static List<String> commands = new ArrayList<>();

    private static Mode mode = Mode.AUTO;
    private static boolean conditional;
    private static boolean trackOutput;
    private static int packetsPerTick = 1;
    private static int delayTicks = 0;

    private static int positionIndex = 0;
    private static int commandIndex = 0;
    private static boolean isRunning = false;

    private enum SinglePhase {
        EXECUTE,
        CLEANUP
    }

    private static class SingleTask {
        BlockPos pos;
        String command;
        SinglePhase phase = SinglePhase.EXECUTE;
        int delay = 0;

        SingleTask(BlockPos pos, String command) {
            this.pos = pos;
            this.command = command;
        }
    }

    private static final List<SingleTask> singleQueue = new ArrayList<>();

    public enum Mode {
        AUTO,

        REDSTONE,

        SEQUENCE,

        SINGLE

    }

    public static void sendCommandsImmediately(
            List<BlockPos> posList,
            List<String> cmdList,
            Mode mode,
            boolean conditional,
            boolean trackOutput) {

        if (invalidEnv() || posList == null || posList.isEmpty() || cmdList == null || cmdList.isEmpty())
            return;

        CoreUpdater.trackOutput = trackOutput;
        CoreUpdater.conditional = conditional;
        CoreUpdater.mode = mode;

        int localCmdIndex = 0;

        for (BlockPos pos : posList) {
            if (!isCommandBlock(pos))
                continue;

            String cmd = cmdList.get(localCmdIndex % cmdList.size());

            if (mode == Mode.SINGLE) {

                sendPacket(pos, "/", CommandBlockBlockEntity.Type.REDSTONE, false);
                sendPacket(pos, cmd, CommandBlockBlockEntity.Type.REDSTONE, true);
                sendPacket(pos, "/", CommandBlockBlockEntity.Type.REDSTONE, false);

                localCmdIndex++;
            }
        }
    }

    public static void startAuto(List<BlockPos> p, List<String> c, boolean cond, boolean track, int ppt) {
        start(p, c, Mode.AUTO, cond, track, ppt);
    }

    public static void startRedstone(List<BlockPos> p, List<String> c, boolean cond, boolean track, int ppt) {
        start(p, c, Mode.REDSTONE, cond, track, ppt);
    }

    public static void startSequence(List<BlockPos> p, List<String> c, boolean cond, boolean track, int ppt) {
        start(p, c, Mode.SEQUENCE, cond, track, ppt);
    }

    public static void startSingle(List<BlockPos> p, List<String> c, boolean cond, boolean track, int ppt) {
        if (p == null || p.isEmpty() || c == null || c.isEmpty()) {
            isRunning = false;
            return;
        }

        startSingleInternal(p, c);

        mode = Mode.SINGLE;
        conditional = cond;
        trackOutput = track;
        packetsPerTick = Math.max(1, ppt);
        delayTicks = 0;
    }

    private static void start(
            List<BlockPos> p,
            List<String> c,
            Mode m,
            boolean cond,
            boolean track,
            int ppt) {

        if (p == null || p.isEmpty() || c == null || c.isEmpty()) {
            isRunning = false;
            return;
        }

        positions = new ArrayList<>(p);
        commands = new ArrayList<>(c);

        mode = m;
        conditional = cond;
        trackOutput = track;
        packetsPerTick = Math.max(1, ppt);

        positionIndex = 0;
        commandIndex = 0;
        delayTicks = 0;
        isRunning = true;
    }

    private static void startSingleInternal(List<BlockPos> p, List<String> c) {

        for (SingleTask task : singleQueue) {
            sendPacket(task.pos, "", CommandBlockBlockEntity.Type.REDSTONE, false);
        }

        singleQueue.clear();

        int blockIndex = 0;
        for (String cmd : c) {
            BlockPos pos = p.get(blockIndex % p.size());
            if (isCommandBlock(pos)) {
                singleQueue.add(new SingleTask(pos, cmd));
            }
            blockIndex++;
        }

        isRunning = !singleQueue.isEmpty();
    }

    public static void stop() {
        isRunning = false;
        positions.clear();
        commands.clear();
        singleQueue.clear();
    }

    public static void onTick() {
        if (!isRunning || invalidEnv()) return;

        if (mode != Mode.SINGLE) {

            if (delayTicks > 0) {
                delayTicks--;
                return;
            }

            if (positions.isEmpty()) {
                isRunning = false;
                return;
            }

            for (int i = 0; i < packetsPerTick; i++) {
                if (positionIndex >= positions.size())
                    positionIndex = 0;
                if (commandIndex >= commands.size()) {
                    commandIndex = 0;
                }

                BlockPos pos = positions.get(positionIndex);

                if (!isCommandBlock(pos)) {
                    positionIndex++;
                    continue;
                }

                String cmd = commands.get(commandIndex);
                sendPacket(pos, cmd);

                positionIndex++;
                commandIndex++;
            }
            return;
        }

        int sent = 0;
        int i = 0;
        while (i < singleQueue.size() && sent < packetsPerTick) {
            SingleTask task = singleQueue.get(i);
            boolean incremented = false;
            if (task.phase == SinglePhase.EXECUTE) {
                sendPacket(
                    task.pos,
                    task.command,
                    CommandBlockBlockEntity.Type.REDSTONE,
                    true
                );
                task.phase = SinglePhase.CLEANUP;
                task.delay = 2;
                sent++;
                incremented = true;
            } else if (task.phase == SinglePhase.CLEANUP) {
                if (task.delay == 0) {
                    sendPacket(
                        task.pos,
                        "", 

                        CommandBlockBlockEntity.Type.REDSTONE,
                        false
                    );
                    singleQueue.remove(i);
                    sent++;
                    incremented = false;
                } else {
                    task.delay--;
                    incremented = true;
                }
            }
            if (incremented) {
                i++;
            }
        }

        if (singleQueue.isEmpty()) {
            isRunning = false;
        }
    }

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

                alwaysActive = true;
            }
            default -> {

                type = CommandBlockBlockEntity.Type.REDSTONE;
                alwaysActive = false;
            }
        }

        sendPacket(pos, cmd, type, alwaysActive);
    }

    private static void sendPacket(BlockPos pos, String cmd, CommandBlockBlockEntity.Type type, boolean alwaysActive) {
        if (MeteorClient.mc.player == null)
            return;

        MeteorClient.mc.player.networkHandler.sendPacket(
                new UpdateCommandBlockC2SPacket(
                        pos,
                        cmd,
                        type,
                        trackOutput,
                        conditional,
                        alwaysActive));
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static boolean isSingleRunning() {
        return isRunning && mode == Mode.SINGLE;
    }

    public static void sendCommandToBlock(BlockPos pos, String cmd) {
        if (!isCommandBlock(pos))
            return;

        sendPacket(pos, cmd, CommandBlockBlockEntity.Type.AUTO, true);
    }

    private static boolean isCommandBlock(BlockPos pos) {
        if (invalidEnv())
            return false;

        if (!MeteorClient.mc.world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }

        Block block = MeteorClient.mc.world.getBlockState(pos).getBlock();
        return block == Blocks.COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK
                || block == Blocks.CHAIN_COMMAND_BLOCK;
    }

    private static boolean invalidEnv() {
        return MeteorClient.mc.player == null || MeteorClient.mc.world == null;
    }
}


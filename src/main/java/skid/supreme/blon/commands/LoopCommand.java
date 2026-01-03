package skid.supreme.blon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class LoopCommand extends Command {
    private final Map<String, BlockPos> activeLoops = new HashMap<>();
    private int nextLoopId = 0;

    public LoopCommand() {
        super("loop", "");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(argument("command", StringArgumentType.greedyString())
                .executes(context -> {
                    String command = context.getArgument("command", String.class);
                    startLoop(command);
                    return SINGLE_SUCCESS;
                }))
            .then(literal("deloop")
                .then(argument("command", StringArgumentType.greedyString())
                    .executes(context -> {
                        String command = context.getArgument("command", String.class);
                        stopLoop(command);
                        return SINGLE_SUCCESS;
                    })))
            .then(literal("list")
                .executes(context -> {
                    listLoops();
                    return SINGLE_SUCCESS;
                }))
            .then(literal("clear")
                .executes(context -> {
                    clearAllLoops();
                    return SINGLE_SUCCESS;
                }));
    }

    private void startLoop(String command) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required for command block manipulation!");
            return;
        }

        if (activeLoops.containsKey(command)) {
            error("Command is already being looped: " + command);
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos loopPos = playerPos.add(0, 2 + (nextLoopId * 2), 0);
        nextLoopId++;

        mc.player.networkHandler.sendChatCommand(
            "setblock " + loopPos.getX() + " " + loopPos.getY() + " " + loopPos.getZ() +
            " repeating_command_block[facing=up]{auto:1b}"
        );

        mc.player.networkHandler.sendPacket(
            new UpdateCommandBlockC2SPacket(
                loopPos,
                command,
                CommandBlockBlockEntity.Type.AUTO,
                true,
                false,
                true
            )
        );

        activeLoops.put(command, loopPos);

        info("Started looping command: " + command);
        info("Command block at: " + loopPos.getX() + ", " + loopPos.getY() + ", " + loopPos.getZ());
    }

    private void stopLoop(String command) {
        BlockPos loopPos = activeLoops.get(command);

        if (loopPos == null) {
            error("No active loop found for command: " + command);
            return;
        }

        mc.player.networkHandler.sendChatCommand(
            "setblock " + loopPos.getX() + " " + loopPos.getY() + " " + loopPos.getZ() + " air"
        );

        activeLoops.remove(command);

        info("Stopped looping command: " + command);
    }

    private void listLoops() {
        if (activeLoops.isEmpty()) {
            info("No active loops.");
            return;
        }

        info("Active command loops:");
        for (Map.Entry<String, BlockPos> entry : activeLoops.entrySet()) {
            BlockPos pos = entry.getValue();
            info("  '" + entry.getKey() + "' -> [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]");
        }
    }

    private void clearAllLoops() {
        int count = activeLoops.size();

        for (BlockPos pos : activeLoops.values()) {
            mc.player.networkHandler.sendChatCommand(
                "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " air"
            );
        }

        activeLoops.clear();
        nextLoopId = 0;

        info("Cleared " + count + " active loops.");
    }
}

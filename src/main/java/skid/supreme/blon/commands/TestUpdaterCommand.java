package skid.supreme.blon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import skid.supreme.blon.core.CoreUpdater;

import java.util.Collections;
import java.util.List;

public class TestUpdaterCommand extends Command {

    public TestUpdaterCommand() {
        super("testupdater", "Test sending a command block update packet using CoreUpdater methods. Usage: .testupdater <mode> <command>");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("mode", StringArgumentType.word())
                .then(argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String modeStr = StringArgumentType.getString(ctx, "mode");
                            String cmd = StringArgumentType.getString(ctx, "command");
                            testUpdate(modeStr, cmd);
                            return SINGLE_SUCCESS;
                        })));
    }

    private void testUpdate(String modeStr, String cmd) {
        if (mc.player == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        if (CoreCommand.corePositions.isEmpty()) {
            error("Core not placed. Use .core first.");
            return;
        }

        CoreUpdater.Mode mode;
        try {
            mode = CoreUpdater.Mode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            error("Invalid mode. Use AUTO, REDSTONE, SEQUENCE, or SINGLE.");
            return;
        }

        List<String> cmds = Collections.singletonList(cmd);
        List<BlockPos> positions = Collections.singletonList(CoreCommand.corePositions.get(0));
        BlockPos pos = positions.get(0);

        // Call the appropriate start method
        switch (mode) {
            case AUTO -> CoreUpdater.startAuto(positions, cmds, false, true, 1);
            case REDSTONE -> CoreUpdater.startRedstone(positions, cmds, false, true, 1);
            case SEQUENCE -> CoreUpdater.startSequence(positions, cmds, false, true, 1);
            case SINGLE -> CoreUpdater.startSingle(positions, cmds, false, true, 1);
        }

        CoreUpdater.onTick();

        info("Test update sent to command block at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " with mode " + mode + " and command: " + cmd);
    }
}

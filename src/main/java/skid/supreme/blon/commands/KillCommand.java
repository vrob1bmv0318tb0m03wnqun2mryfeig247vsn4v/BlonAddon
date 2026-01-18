package skid.supreme.blon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

public class KillCommand extends Command {

    public KillCommand() {
        super("k", "Kill all entities except players.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            killEntities();
            return SINGLE_SUCCESS;
        });
    }

    private void killEntities() {
        if (mc.player == null) return;

        mc.player.networkHandler.sendChatCommand("kill @e[type=!player]");
        info("Killed all entities except players.");
    }
}

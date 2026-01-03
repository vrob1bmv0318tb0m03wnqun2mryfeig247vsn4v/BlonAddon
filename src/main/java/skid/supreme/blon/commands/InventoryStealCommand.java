package skid.supreme.blon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.util.math.BlockPos;

public class InventoryStealCommand extends Command {

    public InventoryStealCommand() {
        super("steal-inventory", "");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .executes(ctx -> {
                steal(ctx.getArgument("player", String.class));
                return SINGLE_SUCCESS;
            })
        );
    }

    private void steal(String target) {
        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        BlockPos start = mc.player.getBlockPos().up(5);
        int commands =
                9
              + 27
              + 4
              + 1
              + 1;

        BlockPos end = start.add(commands, 0, 0);

        mc.player.networkHandler.sendChatCommand(
            "fill " + start.getX() + " " + start.getY() + " " + start.getZ() + " "
                   + end.getX() + " " + end.getY() + " " + end.getZ()
                   + " command_block{auto:1b}"
        );

        int i = 0;

        for (int s = 0; s <= 8; s++) {
            write(start.add(i++, 0, 0),
                "item replace entity @p hotbar." + s +
                " from entity " + target + " hotbar." + s);
        }

        for (int s = 0; s <= 26; s++) {
            write(start.add(i++, 0, 0),
                "item replace entity @p inventory." + s +
                " from entity " + target + " inventory." + s);
        }

        write(start.add(i++, 0, 0),
            "item replace entity @p armor.head from entity " + target + " armor.head");
        write(start.add(i++, 0, 0),
            "item replace entity @p armor.chest from entity " + target + " armor.chest");
        write(start.add(i++, 0, 0),
            "item replace entity @p armor.legs from entity " + target + " armor.legs");
        write(start.add(i++, 0, 0),
            "item replace entity @p armor.feet from entity " + target + " armor.feet");

        write(start.add(i++, 0, 0),
            "item replace entity @p weapon.offhand from entity " + target + " weapon.offhand");

        write(start.add(i, 0, 0),
            "fill " + start.getX() + " " + start.getY() + " " + start.getZ() + " "
                 + end.getX() + " " + end.getY() + " " + end.getZ() + " air"
        );

        info("Inventory stolen from " + target + ".");
    }

    private void write(BlockPos pos, String command) {
        mc.player.networkHandler.sendPacket(
            new UpdateCommandBlockC2SPacket(
                pos,
                command,
                CommandBlockBlockEntity.Type.REDSTONE,
                false,
                false,
                true
            )
        );
    }
}

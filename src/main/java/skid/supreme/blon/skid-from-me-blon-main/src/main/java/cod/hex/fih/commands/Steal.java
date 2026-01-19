package cod.hex.fih.commands;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;

import java.util.UUID;

public class Steal extends Command {

    public Steal() {
        super("steal", "Copies a players mainhand.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if (profile != null) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(player -> player.getProfile().id().equals(profile.id()))) {
                    ChatUtils.sendMsg(Text.of("Copying item from player: " + profile.name()));
                    AbstractClientPlayerEntity target = (AbstractClientPlayerEntity) mc.world.getPlayers().stream().filter((player) -> player.getUuid().equals(profile.id())).toArray()[0];

                    mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), target.getMainHandStack()));

                } else {
                    error("Player not found in the current server");
                }
            } else {
                error("Player profile not found");
            }

            return SINGLE_SUCCESS;
        }));

    }

    public static int[] uuidToIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();

        return new int[]{
            (int)(most >> 32),
            (int)most,
            (int)(least >> 32),
            (int)least
        };
    }
}

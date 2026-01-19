package cod.hex.fih.modules;

import cod.hex.fih.Fih;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.Hand;


public class RainbowArmor extends Module {
    SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder().name("speed").min(0).sliderRange(0, 1).defaultValue(.005).build());


    public RainbowArmor() {
        super(Fih.CATEGORY, "rainbow-armor", "");
    }
    private double t = 0;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        t += speed.get();
        if (t >= 360) t = 0;
        Color color = Color.fromHsv(t, 1, 1);

        int old = mc.player.getInventory().getSelectedSlot();

        ItemStack helmet = Items.LEATHER_HELMET.getDefaultStack();
        ItemStack chestplate = Items.LEATHER_CHESTPLATE.getDefaultStack();
        ItemStack leggings = Items.LEATHER_LEGGINGS.getDefaultStack();
        ItemStack boots = Items.LEATHER_BOOTS.getDefaultStack();
        helmet.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color.getPacked()));
        chestplate.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color.getPacked()));
        leggings.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color.getPacked()));
        boots.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color.getPacked()));
        int slot = 36 + 8;
        mc.player.getInventory().setSelectedSlot(8);
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, helmet));
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, chestplate));
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, leggings));
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, boots));
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(old);
    }
}


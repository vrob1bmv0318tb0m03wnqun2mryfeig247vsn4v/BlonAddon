package cod.hex.fih.modules;

import cod.hex.fih.Fih;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;

public class SpamDrop extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> itemsPerTick = sgGeneral.add(new IntSetting.Builder().name("items-per-tick").range(0, Integer.MAX_VALUE).noSlider().build());
    public SpamDrop() {
        super(Fih.CATEGORY, "spam-drop", "");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        ItemStack target = mc.player.getMainHandStack();
        for (int i = 0; i < itemsPerTick.get(); i++) {
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, mc.player.getBlockPos(), Direction.DOWN));
            for (int j = 0; j < 36; j++) {
                mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(j, target));
            }
            mc.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36 + mc.player.getInventory().getSelectedSlot(), target));
        }
    }
}

package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import skid.supreme.blon.Blon;

public class NoParticleSpamModule extends Module {
    public NoParticleSpamModule() {
        super(Blon.Main, "no-command-block-spam", "Blocks command block output messages in chat.");
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof GameMessageS2CPacket packet) {
            Text msg = packet.content();
            String msgStr = msg.getString().toLowerCase();

            // Block messages related to command block output
            if (msgStr.contains("command set:")) {
                event.cancel();
            }
        }
    }
}

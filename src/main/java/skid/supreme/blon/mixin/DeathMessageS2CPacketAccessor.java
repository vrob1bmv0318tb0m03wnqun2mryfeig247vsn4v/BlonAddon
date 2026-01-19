package skid.supreme.blon.mixin;

import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeathMessageS2CPacket.class)
public interface DeathMessageS2CPacketAccessor {
    @Accessor
    public void setMessage(Text message);
}

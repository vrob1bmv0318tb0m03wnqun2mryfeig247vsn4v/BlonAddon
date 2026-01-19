package cod.hex.fih.mixin;

import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatMessageS2CPacket.class)
public interface ChatMessageS2CPacketAccessor {
    @Accessor
    public void setUnsignedContent(Text unsignedContent);
}

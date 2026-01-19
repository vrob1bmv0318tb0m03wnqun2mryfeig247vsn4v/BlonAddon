package skid.supreme.blon.mixin;

import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleS2CPacket.class)
public interface TitleS2CPacketAccessor {
    @Accessor
    public void setText(Text text);
}

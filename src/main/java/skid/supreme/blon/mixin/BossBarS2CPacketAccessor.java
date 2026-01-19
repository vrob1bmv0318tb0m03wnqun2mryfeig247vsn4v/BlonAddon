package skid.supreme.blon.mixin;

import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BossBarS2CPacket.class)
public interface BossBarS2CPacketAccessor {
    @Accessor("name")
    Text getName();

    @Accessor("name")
    void setName(Text name);
}

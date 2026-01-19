package skid.supreme.blon.mixin;

import skid.supreme.blon.VFih;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BossBar.class)
public class BossBarMixin {
    @ModifyReturnValue(at = @At("RETURN"), method = "getName")
    private Text getName(Text original) {
        if (original != null && VFih.useTextLimiter() && original.getString().length() > VFih.mAntiCrash.bossbar.get()) return VFih.textLengthError(VFih.mAntiCrash.bossbar.get());
        return original;
    }
}

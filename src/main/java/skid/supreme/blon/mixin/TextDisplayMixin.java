package skid.supreme.blon.mixin;

import skid.supreme.blon.VFih;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DisplayEntity.TextDisplayEntity.class)
public class TextDisplayMixin {
    @ModifyReturnValue(at = @At("RETURN"), method = "getText")
    private Text getText(Text original) {
        if (original != null && VFih.useTextLimiter() && original.getString().length() > VFih.mAntiCrash.textDisplay.get()) return VFih.textLengthError(VFih.mAntiCrash.textDisplay.get());
        return original;
    }
}

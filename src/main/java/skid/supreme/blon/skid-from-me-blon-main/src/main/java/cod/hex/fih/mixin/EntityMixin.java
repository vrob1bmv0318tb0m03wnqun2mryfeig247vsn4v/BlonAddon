package cod.hex.fih.mixin;

import cod.hex.fih.VFih;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyReturnValue(at = @At("RETURN"), method = "getCustomName")
    private Text getCustomName(Text original) {
        if (original != null && VFih.useTextLimiter() && original.getString().length() > VFih.mAntiCrash.entityName.get()) return VFih.textLengthError(VFih.mAntiCrash.entityName.get());
        return original;
    }
}

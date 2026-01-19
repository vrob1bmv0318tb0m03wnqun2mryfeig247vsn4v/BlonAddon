package cod.hex.fih.mixin;

import cod.hex.fih.VFih;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @ModifyReturnValue(at = @At("RETURN"), method = "getName")
    private Text getName(Text original) {
        if (original != null && VFih.useTextLimiter() && original.getString().length() > VFih.mAntiCrash.itemName.get()) return VFih.textLengthError(VFih.mAntiCrash.itemName.get());
        return original;
    }
    @ModifyReturnValue(at = @At("RETURN"), method = "getTooltip")
    private List<Text> getTooltip(List<Text> original) {
        if (original != null && !VFih.useTextLimiter()) return original;
        List<Text> modified = new ArrayList<>();

        for (Text t : original) {
            if (t.getString().length() > VFih.mAntiCrash.itemName.get()) modified.add(VFih.textLengthError(VFih.mAntiCrash.itemTooltip.get()));
            else modified.add(t);
        }

        return modified;
    }
}

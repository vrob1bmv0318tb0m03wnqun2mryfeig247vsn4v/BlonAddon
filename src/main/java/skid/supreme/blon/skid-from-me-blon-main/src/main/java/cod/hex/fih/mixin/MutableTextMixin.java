package cod.hex.fih.mixin;

import cod.hex.fih.VFih;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MutableText.class)
public class MutableTextMixin {
    @Shadow
    @Final
    @Mutable
    private TextContent content;

    @Shadow
    @Final
    @Mutable
    private List<Text> siblings;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onConstructor(TextContent cContent, List<Text> sSiblings, Style style, CallbackInfo ci) {
        if (VFih.useTextLimiter()) {
            int general = Math.max(100, VFih.mAntiCrash.general.get());
            if (content.toString().length() > general) content = VFih.textLengthError(general).getContent();
            List<Text> modifiedS = new ArrayList<>();
            for (Text t : siblings) {
                if (t.getString().length() > general) modifiedS.add(VFih.textLengthError(general));
                else modifiedS.add(t);
            }
            siblings = modifiedS;
        }
    }
}

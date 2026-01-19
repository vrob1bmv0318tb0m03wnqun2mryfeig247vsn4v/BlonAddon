package cod.hex.fih.mixin;

import cod.hex.fih.VFih;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(TranslatableTextContent.class)
public class TranslatableTextContentMixin {
    @Mutable
    @Final
    @Shadow
    private String key;
    @Mutable
    @Final
    @Shadow private String fallback;
    @Mutable
    @Final
    @Shadow private Object[] args;

    @Unique private static final String REPLACE_TEXT = "§4§lTranslation Blocked§r";

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void atConstructor(String key, String fallback, Object[] args, CallbackInfo ci) {
        if (VFih.useTranslationAntiCrash()) {
            if (tooManyTranslations(key) || tooManyTranslations(fallback)) {
                if (VFih.mAntiCrash.smartMode.get()) {
                    this.key = "";
                    this.fallback = "%1$s";
                } else {
                    this.key = REPLACE_TEXT;
                    this.fallback = REPLACE_TEXT;
                    this.args = new Object[]{};
                }
            }
        }
    }
    @Inject(method = "of", at = @At(value = "HEAD"), cancellable = true)
    private static void atOf(String key, Optional<String> fallback, Optional<List<Object>> args, CallbackInfoReturnable<TranslatableTextContent> cir) {
        if (VFih.useTranslationAntiCrash()) {
            if (args.isPresent() && args.get() != null) {
                for (Object arg : args.get()) {
                    if (arg instanceof TranslatableTextContent) {
                        if (VFih.mAntiCrash.smartMode.get()) cir.setReturnValue(new TranslatableTextContent("", "%1$s", cir.getReturnValue().getArgs()));
                        else cir.setReturnValue(new TranslatableTextContent(REPLACE_TEXT, null, null));
                    }
                }
            }
        }
    }

    @Unique
    private static boolean tooManyTranslations(String s) {
        if (s != null && Modules.get() != null) {
            String regex = "";
            if (VFih.mAntiCrash.defaultArgumentRegex.get()) regex = "%(?:(\\d+)\\$)?([A-Za-z%]|$)";
            else regex = VFih.mAntiCrash.argumentRegex.get();
            return VFih.countMatches(s, regex) > VFih.mAntiCrash.maxTranslations.get();
        }
        else return false;
    }
}

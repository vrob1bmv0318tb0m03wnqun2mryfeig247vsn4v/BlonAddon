package skid.supreme.blon.mixin;

import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import skid.supreme.blon.VFih;

@Mixin(BufferBuilder.class)
public class BufferBuilderMixin {

    @Shadow
    private int vertices;
    // not sure if this works i think it patches it though from my very minimal testing lmk if this breaks
    @Inject(method = "method_60805", at = @At("HEAD"))
    private void capVertexCount(CallbackInfo ci) {
        if (VFih.mAntiCrash != null && VFih.mAntiCrash.bufferLimiter.get()) {
            int max = VFih.mAntiCrash.maxBufferVertices.get();
            if (vertices > max) {
                vertices = max;
            }
        }
    }
}

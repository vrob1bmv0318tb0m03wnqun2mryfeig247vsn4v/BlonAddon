package skid.supreme.blon.mixin;

import skid.supreme.blon.VFih;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntityRenderer.class)
public class BeaconBlockEntityRendererMixin {
    @Inject(at = @At("HEAD"), method = "renderBeam(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/util/Identifier;FFIIIFF)V", cancellable = true)
    private static void renderBeam(MatrixStack matrices, OrderedRenderCommandQueue queue, Identifier textureId, float beamHeight, float beamRotationDegrees, int minHeight, int maxHeight, int color, float innerScale, float outerScale, CallbackInfo ci) {
        if (VFih.useRenderQuality() && !VFih.mAntiCrash.renderLightBeam.get()) ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderBeam(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;FFIII)V", cancellable = true)
    private static void renderBeam(MatrixStack matrices, OrderedRenderCommandQueue queue, float scale, float rotationDegrees, int minHeight, int maxHeight, int color, CallbackInfo ci) {
        if (VFih.useRenderQuality() && !VFih.mAntiCrash.renderLightBeam.get()) ci.cancel();
    }
}

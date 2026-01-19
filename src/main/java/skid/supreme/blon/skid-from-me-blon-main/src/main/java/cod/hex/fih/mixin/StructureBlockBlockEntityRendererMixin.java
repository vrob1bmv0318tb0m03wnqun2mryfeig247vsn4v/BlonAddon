package cod.hex.fih.mixin;

import cod.hex.fih.VFih;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.StructureBlockBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.StructureBlockBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureBlockBlockEntityRenderer.class)
public class StructureBlockBlockEntityRendererMixin {
    @Inject(at = @At("HEAD"), method = "renderInvisibleBlocks", cancellable = true)
    private void renderInvisibleBlocks(StructureBlockBlockEntityRenderState state, BlockPos pos, Vec3i size, OrderedRenderCommandQueue queue, MatrixStack matrices, CallbackInfo ci) {
        if (VFih.useRenderQuality() && !VFih.mAntiCrash.renderStructureBlockOverlay.get()) ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "renderStructureVoids", cancellable = true)
    private void renderStructureVoids(StructureBlockBlockEntityRenderState state, BlockPos pos, Vec3i size, VertexConsumer vertexConsumer, Matrix4f matrix4f, CallbackInfo ci) {
        if (VFih.useRenderQuality() && !VFih.mAntiCrash.renderStructureBlockOverlay.get()) ci.cancel();
    }
}

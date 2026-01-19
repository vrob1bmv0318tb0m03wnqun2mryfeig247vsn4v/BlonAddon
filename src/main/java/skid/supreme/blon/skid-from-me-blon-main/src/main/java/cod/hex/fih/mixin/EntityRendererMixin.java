package cod.hex.fih.mixin;

import cod.hex.fih.VFih;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(at = @At("HEAD"), method = "updateShadow(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;)V", cancellable = true)
    private <T extends Entity, S extends EntityRenderState> void updateShadow(T entity, S renderState, CallbackInfo ci) {
        if (VFih.useRenderQuality() && !VFih.mAntiCrash.renderShadows.get()) ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "updateShadow(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/world/World;)V", cancellable = true)
    private <S extends EntityRenderState> void updateShadow(S renderState, MinecraftClient client, World world, CallbackInfo ci) {
        if (VFih.useRenderQuality() && !VFih.mAntiCrash.renderShadows.get()) ci.cancel();
    }
    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private <T extends Entity, S extends EntityRenderState> void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (VFih.useRenderQuality() && entity.getBoundingBox().getLengthY() > VFih.mAntiCrash.maxEntitySize.get()) cir.setReturnValue(false);
    }
}

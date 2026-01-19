package skid.supreme.blon.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @WrapMethod(method = "syncWorldEvent")
    private void syncWorldEvent(Entity source, int eventId, BlockPos pos, int data, Operation<Void> original) {
        try {
            original.call(source, eventId, pos, data);
        } catch (Exception e) {
            e.addSuppressed(e);
        }
    }
}

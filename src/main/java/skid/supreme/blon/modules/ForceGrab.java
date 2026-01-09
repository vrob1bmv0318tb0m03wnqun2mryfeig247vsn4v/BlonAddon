package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;

import java.util.Locale;

public class ForceGrab extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("distance")
            .description("Distance to hold the entity.")
            .defaultValue(3.0)
            .min(1.0)
            .sliderMax(20.0)
            .build());

    private Entity target;
    private BlockPos coreBlockPos;

    public ForceGrab() {
        super(Blon.Main, "force-grab",
                "Grabs entities and moves them around using the Core. Middle Click or '[' to grab.");
    }

    @Override
    public void onActivate() {
        if (CoreCommand.corePositions.isEmpty()) {
            error("No Core found! Run .core first.");
            toggle();
            return;
        }

        // Use the first block of the core
        coreBlockPos = CoreCommand.corePositions.get(0);

        info("Force Grab active. Middle Click or '[' to grab/release a player.");
    }

    @Override
    public void onDeactivate() {
        releaseTarget();
        coreBlockPos = null;
    }

    private boolean inputWasPressed = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null)
            return;

        // Poll Input
        if (mc.currentScreen == null) {
            boolean mousePressed = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
                    GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
            boolean keyPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(),
                    GLFW.GLFW_KEY_LEFT_BRACKET) == GLFW.GLFW_PRESS;

            boolean pressed = mousePressed || keyPressed;

            if (pressed && !inputWasPressed) {
                // Initial press
                handleGrabInput();
            }
            inputWasPressed = pressed;
        }

        if (target != null && !target.isAlive()) { // Corrected from original instruction's `!target.isAlive() ||
                                                   // !target.isAlive()`
            // Target died or invalidated
            releaseTarget();
            return;
        }

        if (target == null)
            return;
        if (coreBlockPos == null)
            return;

        // Calculate target position
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d targetPos = eyePos.add(lookVec.multiply(distance.get()));

        // Format command: tp <uuid> x y z
        String cmd = String.format(Locale.US, "tp %s %.2f %.2f %.2f",
                target.getUuidAsString(),
                targetPos.x,
                targetPos.y,
                targetPos.z);

        // Send packet to update Core block to Repeating (AUTO) + Always Active
        mc.player.networkHandler.sendPacket(new UpdateCommandBlockC2SPacket(
                coreBlockPos,
                cmd,
                CommandBlockBlockEntity.Type.AUTO, // Repeating
                false, // TrackOutput
                false, // Conditional
                true // Always Active
        ));
    }

    private void releaseTarget() {
        if (coreBlockPos != null && mc.player != null) {
            // Clear the command block
            mc.player.networkHandler.sendPacket(new UpdateCommandBlockC2SPacket(
                    coreBlockPos,
                    "",
                    CommandBlockBlockEntity.Type.AUTO,
                    false,
                    false,
                    false));
        }
        target = null;
    }

    private void handleGrabInput() {
        HitResult result = mc.crosshairTarget;
        if (result instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();

            if (target != null && target.equals(entity)) {
                releaseTarget();
                ChatUtils.info("Released " + entity.getName().getString());
            } else {
                target = entity;
                ChatUtils.info("Grabbed " + entity.getName().getString());
            }
        } else {
            if (target != null) {
                releaseTarget();
                ChatUtils.info("Released target.");
            }
        }
    }
}

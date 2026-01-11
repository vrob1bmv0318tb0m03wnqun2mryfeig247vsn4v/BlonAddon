package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

import java.util.ArrayList;
import java.util.List;
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

    private final Setting<Boolean> tracer = sgGeneral.add(new BoolSetting.Builder()
            .name("tracer")
            .description("Draws a line of particles to the target.")
            .defaultValue(true)
            .build());

    private final Setting<String> particleType = sgGeneral.add(new StringSetting.Builder()
            .name("particle-type")
            .description("The type of particle to use.")
            .defaultValue("end_rod")
            .visible(tracer::get)
            .build());

    private final Setting<Double> throwForce = sgGeneral.add(new DoubleSetting.Builder()
            .name("throw-force")
            .description("Force to throw the entity.")
            .defaultValue(5.0)
            .min(1.0)
            .sliderMax(20.0)
            .build());

    private final Setting<Boolean> throwTrigger = sgGeneral.add(new BoolSetting.Builder()
            .name("throw")
            .description("Throws the currently grabbed entity.")
            .defaultValue(false)
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
    private boolean throwInputWasPressed = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null)
            return;

        // Process Throw Trigger from GUI
        if (throwTrigger.get()) {
            if (target != null) {
                throwTarget();
            }
            throwTrigger.set(false);
        }

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

            boolean throwPressed = GLFW.glfwGetKey(mc.getWindow().getHandle(),
                    GLFW.GLFW_KEY_RIGHT_BRACKET) == GLFW.GLFW_PRESS;
            if (throwPressed && !throwInputWasPressed) {
                if (target != null) {
                    throwTarget();
                }
            }
            throwInputWasPressed = throwPressed;
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

        // Update Tracer
        if (tracer.get()) {
            updateTracer(eyePos, targetPos);
        } else {
            clearTracer();
        }
    }

    private void updateTracer(Vec3d start, Vec3d end) {
        if (CoreCommand.corePositions.size() < 2)
            return; // Need at least 1 block for grab, others for tracer

        List<Vec3d> points = new ArrayList<>();
        double dist = start.distanceTo(end);
        int count = (int) (dist * 2); // 2 particles per block
        for (int i = 0; i <= count; i++) {
            double t = (double) i / count;
            points.add(start.lerp(end, t));
        }

        // We can use core blocks starting from index 1 (0 is used for grab)
        List<BlockPos> availableCores = CoreCommand.corePositions.subList(1, CoreCommand.corePositions.size());
        int needed = Math.min(points.size(), availableCores.size());

        List<BlockPos> coresToUse = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        for (int i = 0; i < needed; i++) {
            Vec3d pos = points.get(i);
            coresToUse.add(availableCores.get(i));
            commands.add(String.format(Locale.US,
                    "particle minecraft:%s %.3f %.3f %.3f 0 0 0 0.001 1",
                    particleType.get(),
                    pos.x, pos.y, pos.z));
        }

        // Reuse CoreUpdater logic
        CoreUpdater.startAuto(coresToUse, commands, false, true, 50);
        CoreUpdater.onTick();
    }

    private void clearTracer() {
        if (CoreCommand.corePositions.size() < 2)
            return;
        List<BlockPos> availableCores = CoreCommand.corePositions.subList(1, CoreCommand.corePositions.size());

        List<String> emptyCommands = new ArrayList<>();
        for (int i = 0; i < availableCores.size(); i++) {
            emptyCommands.add("");
        }
        // Clear them
        CoreUpdater.startAuto(availableCores, emptyCommands, false, false, 50);
        CoreUpdater.onTick();
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
        clearTracer();
    }

    private void throwTarget() {
        if (target == null || coreBlockPos == null)
            return;

        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Vec3d velocity = lookVec.multiply(throwForce.get());

        // command: data modify entity <uuid> Motion set value [x, y, z]
        String cmd = String.format(Locale.US, "data modify entity %s Motion set value [%.2f, %.2f, %.2f]",
                target.getUuidAsString(),
                velocity.x,
                velocity.y,
                velocity.z);

        // Send as impulse to apply immediately
        mc.player.networkHandler.sendPacket(new UpdateCommandBlockC2SPacket(
                coreBlockPos,
                cmd,
                CommandBlockBlockEntity.Type.REDSTONE, // Impulse
                false,
                false,
                true // Always Active
        ));

        ChatUtils.info("Threw " + target.getName().getString());
        releaseTarget();
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

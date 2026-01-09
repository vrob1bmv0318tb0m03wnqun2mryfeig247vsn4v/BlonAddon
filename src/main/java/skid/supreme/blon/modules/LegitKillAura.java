package skid.supreme.blon.modules;

import java.util.Random;
import java.util.Set;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Tameable;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import skid.supreme.blon.Blon;

public final class LegitKillAura extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Double> range = sg.add(new DoubleSetting.Builder()
        .name("range").defaultValue(4.2).min(1).max(6).build());

    private final Setting<Double> fov = sg.add(new DoubleSetting.Builder()
        .name("fov").defaultValue(360).min(30).max(360).build());

    private final Setting<Double> minDelay = sg.add(new DoubleSetting.Builder()
        .name("min-delay").defaultValue(25).min(1).max(100).build());

    private final Setting<Double> maxDelay = sg.add(new DoubleSetting.Builder()
        .name("max-delay").defaultValue(70).min(1).max(150).build());

    private final Setting<Boolean> targetInvis = sg.add(new BoolSetting.Builder()
        .name("target-invis").defaultValue(true).build());

    private final Setting<Boolean> ignoreTamed = sg.add(new BoolSetting.Builder()
        .name("ignore-tamed").defaultValue(true).build());

    private final Setting<Boolean> ignoreNamed = sg.add(new BoolSetting.Builder()
        .name("ignore-named").defaultValue(false).build());

    private final Setting<Set<EntityType<?>>> entities = sg.add(
        new EntityTypeListSetting.Builder()
            .name("entities")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );

    private Entity target, lastTarget;
    private float serverYaw, serverPitch;
    private long nextAttack, acquireTime;
    private int stickTicks;

    private final Random random = new Random();

    public LegitKillAura() {
        super(Blon.Main, "legit-kill-aura", "");
    }

    @Override
    public void onActivate() {
        target = lastTarget = null;
        nextAttack = acquireTime = 0;
        stickTicks = 0;
        serverYaw = mc.player.getYaw();
        serverPitch = mc.player.getPitch();
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        if (mc.currentScreen instanceof HandledScreen) return;

        selectTarget();

        if (target == null) return;

        if (random.nextDouble() < 0.015) return;

        rotateHuman(target);

        if (!hasLOS(target)) return;

        if (shouldMiss()) {
            mc.player.swingHand(Hand.MAIN_HAND);
            return;
        }

        if (canAttack()) attack();
    }

    private void selectTarget() {
        double best = Double.MAX_VALUE;
        double rangeSq = range.get() * range.get();
        target = null;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            if (!entities.get().contains(e.getType())) continue;
            if (!targetInvis.get() && e.isInvisible()) continue;
            if (ignoreNamed.get() && e.hasCustomName()) continue;
            if (ignoreTamed.get() && e instanceof Tameable t &&
                t.getOwner() != null && t.getOwner().equals(mc.player)) continue;

            double d = mc.player.squaredDistanceTo(e);
            if (d > rangeSq) continue;
            if (fov.get() < 360 && angleTo(e) > fov.get() / 2f) continue;

            if (e == lastTarget && stickTicks < 6) {
                target = e;
                stickTicks++;
                return;
            }

            if (d < best) {
                best = d;
                target = e;
            }
        }

        if (target != null && target != lastTarget)
            acquireTime = System.currentTimeMillis() + 80 + random.nextInt(120);

        lastTarget = target;
        stickTicks = 0;
    }

    private void rotateHuman(Entity e) {
        Vec3d eyes = mc.player.getEyePos();

        Vec3d aim = new Vec3d(e.getX(), e.getY(), e.getZ()).add(
            0,
            e.getHeight() * 0.5 + (random.nextGaussian() * 0.02),
            0
        );

        Vec3d delta = aim.subtract(eyes);
        float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
        float pitch = (float)-Math.toDegrees(Math.atan2(delta.y,
                Math.sqrt(delta.x * delta.x + delta.z * delta.z)));

        float yawDiff = MathHelper.wrapDegrees(yaw - serverYaw);
        float pitchDiff = pitch - serverPitch;

        float speed = humanSpeed(e);

        if (Math.abs(yawDiff) < 4 && Math.abs(pitchDiff) < 4)
            speed *= 0.25f;

        serverYaw += MathHelper.clamp(yawDiff, -speed, speed);
        serverPitch += MathHelper.clamp(pitchDiff, -speed, speed);

        serverYaw += random.nextGaussian() * 0.15f;
        serverPitch += random.nextGaussian() * 0.15f;

        mc.player.setYaw(serverYaw);
        mc.player.setPitch(serverPitch);
    }

    private float humanSpeed(Entity e) {
        double dist = mc.player.distanceTo(e);
        float base = 90f + random.nextFloat() * 220f;

        if (dist > 4) base *= 1.2f;
        if (dist < 2.8) base *= 0.6f;
        if (e.getVelocity().horizontalLengthSquared() > 0.02) base *= 0.85f;

        return MathHelper.clamp(base + (float)(random.nextGaussian() * 15f), 30f, 600f);
    }

    private boolean canAttack() {
        if (System.currentTimeMillis() < nextAttack) return false;
        if (System.currentTimeMillis() < acquireTime) return false;

        float cd = mc.player.getAttackCooldownProgress(0.5f);
        return cd > 0.82f + random.nextFloat() * 0.1f;
    }

    private void attack() {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        nextAttack = System.currentTimeMillis() + delay();
    }

    private boolean shouldMiss() {
        return random.nextDouble() < (mc.player.isSprinting() ? 0.12 : 0.05);
    }

    private long delay() {
        return (long)(minDelay.get() +
            random.nextDouble() * (maxDelay.get() - minDelay.get()));
    }

    private boolean hasLOS(Entity e) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d pos = new Vec3d(e.getX(), e.getY(), e.getZ()).add(
            0, e.getHeight() * 0.5, 0
        );

        HitResult hit = mc.world.raycast(new RaycastContext(
            eyes, pos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    private float angleTo(Entity e) {
        Box b = e.getBoundingBox();
        double dx = b.getCenter().x - mc.player.getX();
        double dz = b.getCenter().z - mc.player.getZ();
        float yaw = (float)(Math.atan2(dz, dx) * 180 / Math.PI - 90);
        return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
    }
}

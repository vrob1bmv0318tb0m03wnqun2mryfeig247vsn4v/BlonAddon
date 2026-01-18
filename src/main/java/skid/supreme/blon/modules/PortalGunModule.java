package skid.supreme.blon.modules;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

public class PortalGunModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to place portals.")
            .defaultValue(50.0)
            .min(5.0)
            .sliderMax(100.0)
            .build());

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("Height of the portal portal.")
            .defaultValue(2.0)
            .min(1.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
            .name("width")
            .description("Width of the portal.")
            .defaultValue(1.0)
            .min(0.5)
            .sliderMax(3.0)
            .build());

    private final Setting<Double> density = sgGeneral.add(new DoubleSetting.Builder()
            .name("density")
            .description("Particle density multiplier.")
            .defaultValue(1.0)
            .min(0.1)
            .max(5.0)
            .build());

    private final Setting<Double> teleportTrigger = sgGeneral.add(new DoubleSetting.Builder()
            .name("teleport-trigger")
            .description("Distance from portal to trigger teleport.")
            .defaultValue(1.0)
            .min(0.1)
            .max(5.0)
            .build());

    private final Setting<Double> teleportOffset = sgGeneral.add(new DoubleSetting.Builder()
            .name("teleport-offset")
            .description("Distance to offset teleporation destination from center.")
            .defaultValue(0.5)
            .min(0.1)
            .max(5.0)
            .build());

    private final Setting<Boolean> gunModel = sgGeneral.add(new BoolSetting.Builder()
            .name("gun-model")
            .description("Render the custom portal gun model.")
            .defaultValue(true)
            .build());

    private final Setting<Double> gunX = sgGeneral.add(new DoubleSetting.Builder()
            .name("gun-x")
            .description("X offset for the gun model.")
            .defaultValue(0.5)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-2.0)
            .sliderMax(2.0)
            .build());

    private final Setting<Double> gunY = sgGeneral.add(new DoubleSetting.Builder()
            .name("gun-y")
            .description("Y offset for the gun model. Negative values move the gun down from eye level (towards hands).")
            .defaultValue(-0.5)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-2.0)
            .sliderMax(2.0)
            .build());

    private final Setting<Double> gunZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("gun-z")
            .description("Z offset for the gun model.")
            .defaultValue(1.0)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-2.0)
            .sliderMax(2.0)
            .build());

    private final List<GunPart> gunParts = new ArrayList<>();
    private final List<String> summonCmds = new ArrayList<>();

    private Portal orangePortal = null;
    private Portal bluePortal = null;

    private boolean leftClickPressed = false;
    private boolean rightClickPressed = false;
    private boolean summoned = false;

    public PortalGunModule() {
        super(Blon.Main, "Portal Gun", "Creates portals using particles and the Command Block Core.");
    }

    @Override
    public void onActivate() {
        summoned = false;
        if (gunModel.get()) {
            loadAndSummonGun();
        }
    }

    @Override
    public void onDeactivate() {
        summoned = false;

        orangePortal = null;
        bluePortal = null;

        removeGun();
    }

    private void loadAndSummonGun() {
        gunParts.clear();
        summonCmds.clear();
        if (mc.player == null) return;
        try (InputStream stream = PortalGunModule.class.getResourceAsStream("/portalgun.txt")) {
            if (stream == null) {
                ChatUtils.error("Could not find portalgun.txt resource.");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("summon "))
                    continue;

                String[] parts = line.split("\\s+", 6);
                if (parts.length < 5)
                    continue;

                String entity = parts[1];
                String xStr = parts[2];
                String yStr = parts[3];
                String zStr = parts[4];
                String nbt = (parts.length > 5) ? parts[5] : "{}";

                double xOff = parseRelative(xStr);
                double yOff = parseRelative(yStr);
                double zOff = parseRelative(zStr);

                Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(xOff, yOff, zOff);

                String tag = "blon_pg_part_" + index++;

                String taggedNbt;
                if (nbt.trim().startsWith("{")) {
                    taggedNbt = "{Tags:[\"" + tag + "\"]," + nbt.trim().substring(1);
                } else {
                    taggedNbt = "{Tags:[\"" + tag + "\"]}";
                }

                String summonCmd = String.format(Locale.US, "summon %s %.3f %.3f %.3f %s", entity, pos.x, pos.y, pos.z, taggedNbt);
                summonCmds.add(summonCmd);

                gunParts.add(new GunPart(tag, new Vec3d(xOff, yOff, zOff)));
            }
        } catch (Exception e) {
            ChatUtils.error("Failed to load portal gun model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double parseRelative(String s) {
        if (s.startsWith("~")) {
            if (s.length() == 1)
                return 0.0;
            try {
                return Double.parseDouble(s.substring(1));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        return 0.0;
    }

    private void removeGun() {
        if (!gunParts.isEmpty()) {
            ChatUtils.sendPlayerMsg("/kill @e[tag=blon_pg_part]"); 

            for (GunPart part : gunParts) {
                ChatUtils.sendPlayerMsg("/kill @e[tag=" + part.tag + "]");
            }
            gunParts.clear();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null)
            return;

        handleInput(); 

        renderPortals(); 

    }

    private void handleInput() {
        if (mc.currentScreen != null)
            return;

        boolean leftDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (leftDown && !leftClickPressed) {
            placePortal(true);
        }
        leftClickPressed = leftDown;

        if (rightDown && !rightClickPressed) {
            placePortal(false);
        }
        rightClickPressed = rightDown;
    }

    private void placePortal(boolean isBlue) {
        HitResult hit = mc.player.raycast(range.get(), 0f, false);

        if (hit instanceof BlockHitResult blockHit) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = blockHit.getBlockPos();
                Direction side = blockHit.getSide();
                Vec3d exactPos = blockHit.getPos();

                Portal newPortal = new Portal(exactPos, side, isBlue);

                if (isBlue) {
                    bluePortal = newPortal;
                    ChatUtils.info("Placed Blue Portal at " + pos.toShortString());
                } else {
                    orangePortal = newPortal;
                    ChatUtils.info("Placed Orange Portal at " + pos.toShortString());
                }
            }
        }
    }

    private void renderPortals() {
        if (CoreCommand.corePositions.isEmpty())
            return;

        String playerName = mc.player.getName().getString();

        List<String> commands = new ArrayList<>();
        List<BlockPos> coresUsed = new ArrayList<>();

        List<Vec3d> particles = new ArrayList<>();
        List<String> types = new ArrayList<>();

        int max = CoreCommand.corePositions.size();
        if (max == 0)
            return;

        int activePortals = (bluePortal != null ? 1 : 0) + (orangePortal != null ? 1 : 0);
        int gunPartCount = gunParts.size();
        int summonCount = summoned ? 0 : summonCmds.size();

        int totalCoresNeededForLogic = summonCount + ((activePortals == 2) ? 2 : 0) + gunPartCount;

        int particleBlocksAvailable = max - totalCoresNeededForLogic;
        if (particleBlocksAvailable < 0) {

            particleBlocksAvailable = 0;

        }

        int budgetPerPortal = (activePortals > 0) ? (particleBlocksAvailable / activePortals) : 0;

        if (!summoned) {
            commands.addAll(summonCmds);
            summoned = true;
        }

        if (activePortals == 2 && bluePortal != null && orangePortal != null && max >= 2) {
            addTeleportCommand(bluePortal, orangePortal, commands, playerName);
            addTeleportCommand(orangePortal, bluePortal, commands, playerName);
        }

        if (gunModel.get() && !gunParts.isEmpty()) {
            double gx = gunX.get();
            double gy = gunY.get();
            double gz = gunZ.get();

            for (GunPart part : gunParts) {

                double tx = gx + part.offset.x;
                double ty = gy + part.offset.y;
                double tz = gz + part.offset.z;

                String cmd = String.format(Locale.US,
                        "execute as %s at @s anchored eyes run tp @e[tag=%s,limit=1] ^%.3f ^%.3f ^%.3f ~ ~",
                        playerName, part.tag, tx, ty, tz);
                commands.add(cmd);
            }
        }

        if (activePortals > 0) {
            accumulateParticles(bluePortal, particles, types, budgetPerPortal);
            accumulateParticles(orangePortal, particles, types, budgetPerPortal);
        }

        int logicCount = commands.size(); 

        if (logicCount > max) {

            logicCount = max;
            commands = commands.subList(0, max);
        }

        for (int i = 0; i < logicCount; i++) {
            coresUsed.add(CoreCommand.corePositions.get(i));
        }

        int particleCount = Math.min(particles.size(), max - logicCount);
        for (int i = 0; i < particleCount; i++) {
            Vec3d p = particles.get(i);
            String type = types.get(i);
            BlockPos core = CoreCommand.corePositions.get(i + logicCount);

            coresUsed.add(core);
            commands.add(String.format(Locale.US,
                    "particle minecraft:%s %.3f %.3f %.3f 0 0 0 0 1",
                    type, p.x, p.y, p.z));
        }

        if (!coresUsed.isEmpty()) {
            CoreUpdater.startAuto(coresUsed, commands, false, true, coresUsed.size());
            CoreUpdater.onTick();
        }
    }

    private void accumulateParticles(Portal portal, List<Vec3d> positions, List<String> types, int maxCount) {
        if (portal == null)
            return;

        List<Vec3d> shape = getEllipsePoints(portal);

        int count = Math.min(shape.size(), maxCount);

        String particleType = portal.isBlue ? "soul_fire_flame" : "flame";

        for (int i = 0; i < count; i++) {
            positions.add(shape.get(i));
            types.add(particleType);
        }
    }

    private List<Vec3d> getEllipsePoints(Portal portal) {
        List<Vec3d> points = new ArrayList<>();

        Vec3d center = portal.center;
        Direction normal = portal.facing;

        Vec3d uAxis;
        Vec3d vAxis;

        if (normal == Direction.UP || normal == Direction.DOWN) {

            uAxis = new Vec3d(1, 0, 0);
            vAxis = new Vec3d(0, 0, 1);
        } else {

            vAxis = new Vec3d(0, 1, 0);

            uAxis = new Vec3d(normal.getOffsetX(), normal.getOffsetY(), normal.getOffsetZ())
                    .crossProduct(vAxis)
                    .normalize();
        }

        Vec3d offsetCenter = center.add(
                normal.getOffsetX() * 0.1,
                normal.getOffsetY() * 0.1,
                normal.getOffsetZ() * 0.1);

        int pointCount = (int) (40 * density.get());
        double w = width.get() / 2.0;
        double h = height.get() / 2.0;

        for (int i = 0; i < pointCount; i++) {
            double t = 2 * Math.PI * i / pointCount;

            Vec3d p = offsetCenter
                    .add(uAxis.multiply(w * Math.cos(t)))
                    .add(vAxis.multiply(h * Math.sin(t)));

            points.add(p);
        }

        return points;
    }

    private void addTeleportCommand(Portal from, Portal to, List<String> commands, String playerName) {

        double triggerDist = teleportTrigger.get();
        double offset = teleportOffset.get();

        Vec3d normal = new Vec3d(to.facing.getOffsetX(), to.facing.getOffsetY(), to.facing.getOffsetZ());

        Vec3d dest = to.center.add(
                normal.x * offset,
                normal.y * offset,
                normal.z * offset);

        String cmd = String.format(Locale.US,
                "execute positioned %.3f %.3f %.3f if entity @p[distance=..%.1f] run tp @p %.3f %.3f %.3f",
                from.center.x, from.center.y, from.center.z,
                triggerDist,
                dest.x, dest.y, dest.z);

        commands.add(cmd);
    }

    private static class Portal {
        Vec3d center;
        Direction facing;
        boolean isBlue;

        Portal(Vec3d center, Direction facing, boolean isBlue) {
            this.center = center;
            this.facing = facing;
            this.isBlue = isBlue;
        }
    }

    private static class GunPart {
        String tag;
        Vec3d offset;

        GunPart(String tag, Vec3d offset) {
            this.tag = tag;
            this.offset = offset;
        }
    }
}

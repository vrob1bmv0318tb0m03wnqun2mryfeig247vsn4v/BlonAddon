package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

import java.util.ArrayList;
import java.util.List;

public class ParticleAuraModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ParticleStyle> style = sgGeneral.add(new EnumSetting.Builder<ParticleStyle>()
        .name("particle-style")
        .description("The style of the particle aura.")
        .defaultValue(ParticleStyle.CUBE)
        .build()
    );

    private final Setting<String> particleType = sgGeneral.add(new StringSetting.Builder()
        .name("particle-type")
        .description("The type of particle to use (e.g., flame, dust, etc.)")
        .defaultValue("enchanted_hit")
        .build()
    );

    private final Setting<Integer> maxPacketsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-packets-per-tick")
        .description("Maximum number of C2S packets sent per tick")
        .defaultValue(10)
        .min(1)
        .max(50000)
        .sliderMin(1)
        .sliderMax(5000)
        .build()
    );

    private final Setting<Double> rotationSpeedX = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotation-speed-x")
        .description("Speed of rotation around X axis (radians per second)")
        .defaultValue(0.0)
        .min(0.0)
        .max(10.0)
        .sliderMin(0.0)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Double> rotationSpeedY = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotation-speed-y")
        .description("Speed of rotation around Y axis (radians per second)")
        .defaultValue(0.5)
        .min(0.0)
        .max(10.0)
        .sliderMin(0.0)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Double> rotationSpeedZ = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotation-speed-z")
        .description("Speed of rotation around Z axis (radians per second)")
        .defaultValue(0.0)
        .min(0.0)
        .max(10.0)
        .sliderMin(0.0)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Double> quality = sgGeneral.add(new DoubleSetting.Builder()
        .name("quality")
        .description("Quality multiplier for particle density (0.1-2.0)")
        .defaultValue(1.0)
        .min(0.1)
        .max(2.0)
        .sliderMin(0.1)
        .sliderMax(2.0)
        .build()
    );

    private Vec3d lastPlayerPos = null;
    private ParticleStyle lastStyle;

    public ParticleAuraModule() {
        super(Blon.Main, "Particle Aura", "Advanced geometric particle aura using the core.");
        lastStyle = style.get();
    }

    public enum ParticleStyle {
        CUBE,
        SPHERE,
        SPIRAL,
        TORUS,
        PYRAMID,
        DOUBLE_HELIX,
        ICOSAHEDRON,
        STAR,
        HELIX_RING,
        DIAMOND
    }

    public enum RotationAxis {
        X, Y, Z
    }

    @Override
    public void onDeactivate() {
        clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || CoreCommand.corePositions.isEmpty()) return;

        // Clear if style changed
        if (lastStyle != style.get()) {
            clear();
            lastStyle = style.get();
        }

        Vec3d currentPos = new Vec3d(mc.player.getX(), mc.player.getY() + 1.5, mc.player.getZ());
        Vec3d velocity = lastPlayerPos == null ? Vec3d.ZERO : currentPos.subtract(lastPlayerPos);
        lastPlayerPos = currentPos;

        // 1. Compute particle positions for this tick
        List<Vec3d> particlePositions = getParticlePositions(currentPos, style.get(), velocity);

        // 2. Map particle positions to core blocks
        List<BlockPos> core = CoreCommand.corePositions;
        int needed = Math.min(particlePositions.size(), core.size());

        // 3. Build commands for each core block and collect positions
        List<BlockPos> coresToUse = new ArrayList<>();
        List<String> commands = new ArrayList<>();
        for (int i = 0; i < needed; i++) {
            Vec3d pos = particlePositions.get(i);
            coresToUse.add(core.get(i));
            commands.add(String.format(
                "particle minecraft:%s %.3f %.3f %.3f 0 0 0 0.001 1",
                particleType.get(),
                pos.x, pos.y, pos.z
            ));
        }

        // 4. Queue all commands in CoreUpdater at once
        CoreUpdater.startAuto(coresToUse, commands, false, true, maxPacketsPerTick.get());

        // 5. Tick the CoreUpdater to send packets
        CoreUpdater.onTick();
    }

    private List<Vec3d> getParticlePositions(Vec3d center, ParticleStyle style, Vec3d velocity) {
        List<Vec3d> positions = new ArrayList<>();
        long time = System.currentTimeMillis();
        double t = (time % 10000) / 10000.0 * 2 * Math.PI;

        switch (style) {

            case CUBE -> {
                double size = 1.2;
                Vec3d[] corners = new Vec3d[8];
                int idx = 0;
                for (int i = -1; i <= 1; i += 2) {
                    for (int j = -1; j <= 1; j += 2) {
                        for (int k = -1; k <= 1; k += 2) {
                            double x = i * size;
                            double y = j * size;
                            double z = k * size;
                            corners[idx++] = center.add(x, y, z);
                        }
                    }
                }
                int[][] edges = {{0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}};
                int edgePoints = Math.max(1, (int)(20 * quality.get()));
                for (int[] e : edges) {
                    Vec3d c1 = corners[e[0]], c2 = corners[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
            }

            case SPHERE -> {
                int density = Math.max(1, (int)(200 * quality.get()));
                double radius = 1.5;
                for (int i = 0; i < density; i++) {
                    double u = Math.random();
                    double v = Math.random();
                    double theta = 2 * Math.PI * u;
                    double phi = Math.acos(2.0 * v - 1.0);
                    double x = radius * Math.sin(phi) * Math.cos(theta);
                    double y = radius * Math.sin(phi) * Math.sin(theta);
                    double z = radius * Math.cos(phi);
                    positions.add(center.add(x, y, z));
                }
            }

            case SPIRAL -> {
                int spiralPoints = Math.max(1, (int)(200 * quality.get()));
                for (int i = 0; i < spiralPoints; i++) {
                    double angle = i * 0.04 + t;
                    double r = 1.2 + i*0.005;
                    positions.add(center.add(Math.cos(angle)*r, i*0.008, Math.sin(angle)*r));
                }
            }

            case TORUS -> {
                int uSteps = Math.max(1, (int)(24 * quality.get()));
                int vSteps = Math.max(1, (int)(12 * quality.get()));
                double R = 2.0, r = 0.5;
                for (int i = 0; i < uSteps; i++) {
                    double u = 2 * Math.PI * i / uSteps + t;
                    for (int j = 0; j < vSteps; j++) {
                        double v = 2 * Math.PI * j / vSteps;
                        double x = (R + r*Math.cos(v)) * Math.cos(u);
                        double y = r * Math.sin(v);
                        double z = (R + r*Math.cos(v)) * Math.sin(u);
                        positions.add(center.add(x, y, z));
                    }
                }
            }

            case PYRAMID -> {
                double h = 1.5, s = 1.2;
                Vec3d apex = center.add(0, h, 0);
                Vec3d[] base = {
                    center.add(-s, 0, -s),
                    center.add(s, 0, -s),
                    center.add(s, 0, s),
                    center.add(-s, 0, s)
                };
                int edgePoints = Math.max(1, (int)(20 * quality.get()));
                // Base edges
                int[][] baseEdges = {{0,1},{1,2},{2,3},{3,0}};
                for (int[] e : baseEdges) {
                    Vec3d c1 = base[e[0]], c2 = base[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
                // Side edges
                for (Vec3d b : base) {
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(b.add(apex.subtract(b).multiply(alpha)));
                    }
                }
            }

            case DOUBLE_HELIX -> {
                int helixPoints = Math.max(1, (int)(50 * quality.get()));
                for (int i = 0; i < helixPoints; i++) {
                    double angle = i * 0.08 + t;
                    double y = i*0.04;
                    positions.add(center.add(Math.cos(angle)*0.6, y, Math.sin(angle)*0.6));
                    positions.add(center.add(Math.cos(angle+Math.PI)*0.6, y, Math.sin(angle+Math.PI)*0.6));
                }
            }

            case ICOSAHEDRON -> {
                double phi = (1+Math.sqrt(5))/2;
                Vec3d[] vertices = {
                    center.add(0,1,phi).multiply(0.7),
                    center.add(0,-1,phi).multiply(0.7),
                    center.add(0,1,-phi).multiply(0.7),
                    center.add(0,-1,-phi).multiply(0.7),
                    center.add(1,phi,0).multiply(0.7),
                    center.add(-1,phi,0).multiply(0.7),
                    center.add(1,-phi,0).multiply(0.7),
                    center.add(-1,-phi,0).multiply(0.7),
                    center.add(phi,0,1).multiply(0.7),
                    center.add(-phi,0,1).multiply(0.7),
                    center.add(phi,0,-1).multiply(0.7),
                    center.add(-phi,0,-1).multiply(0.7)
                };
                int[][] edges = {
                    {0,1},{0,4},{0,5},{0,8},{0,9},
                    {1,6},{1,7},{1,8},{1,9},
                    {2,3},{2,4},{2,5},{2,10},{2,11},
                    {3,6},{3,7},{3,10},{3,11},
                    {4,5},{4,8},{4,9},{4,10},{4,11},
                    {5,8},{5,9},{5,10},{5,11},
                    {6,7},{6,8},{6,9},{6,10},{6,11},
                    {7,8},{7,9},{7,10},{7,11},
                    {8,9},{8,10},{8,11},
                    {9,10},{9,11},
                    {10,11}
                };
                int edgePoints = Math.max(1, (int)(10 * quality.get()));
                for (int[] e : edges) {
                    Vec3d c1 = vertices[e[0]], c2 = vertices[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
            }

            case STAR -> {
                double size = 1.5;
                Vec3d[] vertices = {
                    center.add(size, 0, 0),
                    center.add(-size, 0, 0),
                    center.add(0, size, 0),
                    center.add(0, -size, 0),
                    center.add(0, 0, size),
                    center.add(0, 0, -size)
                };
                int[][] edges = {
                    {0,2},{0,3},{0,4},{0,5},
                    {1,2},{1,3},{1,4},{1,5},
                    {2,4},{2,5},{3,4},{3,5}
                };
                int edgePoints = Math.max(1, (int)(20 * quality.get()));
                for (int[] e : edges) {
                    Vec3d c1 = vertices[e[0]], c2 = vertices[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
            }

            case HELIX_RING -> {
                int points = Math.max(1, (int)(200 * quality.get()));
                double radius = 1.5;
                for (int i = 0; i < points; i++) {
                    double angle = 2*Math.PI*i/points + t;
                    double y = Math.sin(angle*2)*0.5;
                    positions.add(center.add(Math.cos(angle)*radius, y, Math.sin(angle)*radius));
                }
            }

            case DIAMOND -> {
                double h = 1.5;
                Vec3d top = center.add(0, h, 0);
                Vec3d bottom = center.add(0, -h, 0);
                double s = 1.0;
                Vec3d[] equator = {
                    center.add(s, 0, 0),
                    center.add(-s, 0, 0),
                    center.add(0, 0, s),
                    center.add(0, 0, -s)
                };
                int edgePoints = Math.max(1, (int)(20 * quality.get()));
                // Top to equator edges
                for (Vec3d eq : equator) {
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(top.add(eq.subtract(top).multiply(alpha)));
                    }
                }
                // Bottom to equator edges
                for (Vec3d eq : equator) {
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double)edgePoints;
                        positions.add(bottom.add(eq.subtract(bottom).multiply(alpha)));
                    }
                }
            }
        }

        // Apply rotation if enabled
        double currentTime = System.currentTimeMillis() / 1000.0;
        Vec3d pos;
        for (int i = 0; i < positions.size(); i++) {
            pos = positions.get(i);
            if (rotationSpeedX.get() > 0) {
                double angleX = currentTime * rotationSpeedX.get() % (2 * Math.PI);
                pos = rotate(pos, center, angleX, RotationAxis.X);
            }
            if (rotationSpeedY.get() > 0) {
                double angleY = currentTime * rotationSpeedY.get() % (2 * Math.PI);
                pos = rotate(pos, center, angleY, RotationAxis.Y);
            }
            if (rotationSpeedZ.get() > 0) {
                double angleZ = currentTime * rotationSpeedZ.get() % (2 * Math.PI);
                pos = rotate(pos, center, angleZ, RotationAxis.Z);
            }
            positions.set(i, pos);
        }

        return positions;
    }

    private void clear() {
        if (CoreCommand.corePositions.isEmpty()) return;
        List<String> emptyCommands = new ArrayList<>();
        for (int i = 0; i < CoreCommand.corePositions.size(); i++) {
            emptyCommands.add("say ");
        }
        CoreUpdater.startAuto(CoreCommand.corePositions, emptyCommands, false, true, maxPacketsPerTick.get());
        CoreUpdater.onTick();
    }

    private Vec3d rotate(Vec3d point, Vec3d center, double angle, RotationAxis axis) {
        Vec3d relative = point.subtract(center);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = relative.x, y = relative.y, z = relative.z;
        switch (axis) {
            case X -> {
                double ny = y * cos - z * sin;
                double nz = y * sin + z * cos;
                return center.add(x, ny, nz);
            }
            case Y -> {
                double nx = x * cos + z * sin;
                double nz = -x * sin + z * cos;
                return center.add(nx, y, nz);
            }
            case Z -> {
                double nx = x * cos - y * sin;
                double ny = x * sin + y * cos;
                return center.add(nx, ny, z);
            }
        }
        return point;
    }
}

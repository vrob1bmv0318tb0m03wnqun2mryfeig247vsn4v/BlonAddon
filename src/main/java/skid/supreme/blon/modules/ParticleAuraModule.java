package skid.supreme.blon.modules;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

public class ParticleAuraModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ParticleStyle> style = sgGeneral.add(new EnumSetting.Builder<ParticleStyle>()
            .name("particle-style")
            .description("The style of the particle aura.")
            .defaultValue(ParticleStyle.CUBE)
            .visible(() -> false)
            .build());

    private final Setting<String> particleType = sgGeneral.add(new StringSetting.Builder()
            .name("particle-type")
            .description("The type of particle to use (e.g., flame, dust, etc.)")
            .defaultValue("enchanted_hit")
            .build());

    public void setParticleType(String type) {
        if (particleType != null) {
            particleType.set(type);
        }
    }

    private final Setting<Integer> maxPacketsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-packets-per-tick")
            .description("Maximum number of C2S packets sent per tick")
            .defaultValue(10)
            .min(1)
            .max(50000)
            .sliderMin(1)
            .sliderMax(5000)
            .build());

    private final Setting<Double> rotationSpeedX = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-speed-x")
            .description("Speed of rotation around X axis (radians per second)")
            .defaultValue(0.0)
            .min(0.0)
            .max(10.0)
            .sliderMin(0.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> rotationSpeedY = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-speed-y")
            .description("Speed of rotation around Y axis (radians per second)")
            .defaultValue(0.5)
            .min(0.0)
            .max(10.0)
            .sliderMin(0.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> rotationSpeedZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-speed-z")
            .description("Speed of rotation around Z axis (radians per second)")
            .defaultValue(0.0)
            .min(0.0)
            .max(10.0)
            .sliderMin(0.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> quality = sgGeneral.add(new DoubleSetting.Builder()
            .name("quality")
            .description("Quality multiplier for particle density (0.1-2.0)")
            .defaultValue(1.0)
            .min(0.1)
            .max(2.0)
            .sliderMin(0.1)
            .sliderMax(2.0)
            .build());

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
            .name("radius")
            .description("The radius/scale of the shape.")
            .defaultValue(1.5)
            .min(0.5)
            .max(5.0)
            .sliderMin(0.5)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> translateX = sgGeneral.add(new DoubleSetting.Builder()
            .name("translate-x")
            .description("Offset on X axis.")
            .defaultValue(0.0)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> translateY = sgGeneral.add(new DoubleSetting.Builder()
            .name("translate-y")
            .description("Offset on Y axis.")
            .defaultValue(0.0)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> translateZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("translate-z")
            .description("Offset on Z axis.")
            .defaultValue(0.0)
            .min(-5.0)
            .max(5.0)
            .sliderMin(-5.0)
            .sliderMax(5.0)
            .build());

    private final Setting<Double> rotateX = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotate-x")
            .description("Rotation on X axis (Pitch).")
            .defaultValue(0.0)
            .min(0.0)
            .max(360.0)
            .sliderMax(360.0)
            .build());

    private final Setting<Double> rotateY = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotate-y")
            .description("Rotation on Y axis (Yaw).")
            .defaultValue(0.0)
            .min(0.0)
            .max(360.0)
            .sliderMax(360.0)
            .build());

    private final Setting<Double> rotateZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotate-z")
            .description("Rotation on Z axis (Roll).")
            .defaultValue(0.0)
            .min(0.0)
            .max(360.0)
            .sliderMax(360.0)
            .build());

    private final Setting<Boolean> scrollCycle = sgGeneral.add(new BoolSetting.Builder()
            .name("scroll-cycle")
            .description("Scroll mouse wheel to change particle style")
            .defaultValue(true)
            .build());

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
        DIAMOND,
        ATOM,
        HEART,
        VORTEX,
        TESSERACT,
        OCTAHEDRON,
        DODECAHEDRON,
        MERKABA,
        MOBIUS,
        SATURN,
        INFINITY,
        TRIFORCE,
        CROSS,
        PENTAGRAM,
        CYLINDER,
        CONE,
        PRISM,
        TETRAHEDRON,
        TRUNCATED_OCTAHEDRON,
        RHOMBIC_DODECAHEDRON,
        BUCKYBALL,
        TORUS_KNOT,
        KLEIN_BOTTLE,
        ASTROID,
        SPIRAL_SPHERE,
        ROSE,
        BUTTERFLY,
        LISSAJOUS,
        HELICOID,
        CATENOID,
        HYPERBOLOID,
        UMBRELLA,
        FLOWER,
        SNOWFLAKE,
        DNA_RUNGS,
        SPHERE_GRID,
        CUBE_GRID,
        PENTAGONAL_PRISM,
        HEXAGONAL_PRISM,
        PENTAGONAL_PYRAMID,
        HEXAGONAL_PYRAMID,
        FIG_8_KNOT,
        CINQUEFOIL_KNOT,
        GALAXY,
        BLACK_HOLE,
        QUASAR,
        WINGS,
        HALO,
        SHIELD,
        GEODESIC_DOME,
        SIERPINSKI_TRIANGLE,
        ENNEAGRAM,
        DODECAGRAM,
        CROWN
    }

    public enum RotationAxis {
        X, Y, Z
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(list, theme);
        return list;
    }

    private void fillWidget(WVerticalList list, GuiTheme theme) {
        list.clear();
        for (ParticleStyle s : ParticleStyle.values()) {
            WHorizontalList h = list.add(theme.horizontalList()).expandX().widget();
            h.add(theme.label(s.name())).expandX();

            WCheckbox c = h.add(theme.checkbox(style.get() == s)).widget();
            c.action = () -> {
                style.set(s);
                fillWidget(list, theme);
            };
        }
    }

    @Override
    public void onDeactivate() {
        clear();
    }

    @EventHandler
    private void onScroll(MouseScrollEvent event) {
        if (!scrollCycle.get())
            return;
        if (mc.currentScreen != null)
            return; // prevent GUI scrolling

        int dir = event.value > 0 ? 1 : -1;

        ParticleStyle[] values = ParticleStyle.values();
        int index = style.get().ordinal();
        int next = (index + dir + values.length) % values.length;

        style.set(values[next]);
        info("§c[BLON] Particle style: §a" + style.get().name());
        event.cancel(); // stop normal scroll behavior
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || CoreCommand.corePositions.isEmpty())
            return;

        // Clear if style changed
        if (lastStyle != style.get()) {
            clear();
            lastStyle = style.get();
        }

        Vec3d currentPos = new Vec3d(mc.player.getX(), mc.player.getY() + 1.5, mc.player.getZ());

        // Apply Translation
        currentPos = currentPos.add(translateX.get(), translateY.get(), translateZ.get());

        Vec3d velocity = lastPlayerPos == null ? Vec3d.ZERO : currentPos.subtract(lastPlayerPos);
        lastPlayerPos = currentPos;

        // 1. Compute particle positions for this tick
        List<Vec3d> particlePositions = getParticlePositions(currentPos, style.get(), velocity);

        // Apply Static Rotation
        double rx = Math.toRadians(rotateX.get());
        double ry = Math.toRadians(rotateY.get());
        double rz = Math.toRadians(rotateZ.get());

        if (rx != 0 || ry != 0 || rz != 0) {
            for (int i = 0; i < particlePositions.size(); i++) {
                Vec3d p = particlePositions.get(i);
                if (rx != 0)
                    p = rotate(p, currentPos, rx, RotationAxis.X);
                if (ry != 0)
                    p = rotate(p, currentPos, ry, RotationAxis.Y);
                if (rz != 0)
                    p = rotate(p, currentPos, rz, RotationAxis.Z);
                particlePositions.set(i, p);
            }
        }

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
                    pos.x, pos.y, pos.z));
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

        double r = radius.get();

        switch (style) {

            case CUBE -> {
                double size = r; // Use radius as half-size for cube
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
                int[][] edges = { { 0, 1 }, { 0, 2 }, { 0, 4 }, { 1, 3 }, { 1, 5 }, { 2, 3 }, { 2, 6 }, { 3, 7 },
                        { 4, 5 }, { 4, 6 }, { 5, 7 }, { 6, 7 } };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                for (int[] e : edges) {
                    Vec3d c1 = corners[e[0]], c2 = corners[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
            }

            case SPHERE -> {
                int density = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                for (int i = 0; i < density; i++) {
                    double u = Math.random();
                    double v = Math.random();
                    double theta = 2 * Math.PI * u;
                    double phi = Math.acos(2.0 * v - 1.0);
                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.sin(phi) * Math.sin(theta);
                    double z = r * Math.cos(phi);
                    positions.add(center.add(x, y, z));
                }
            }

            case SPIRAL -> {
                int spiralPoints = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                for (int i = 0; i < spiralPoints; i++) {
                    double angle = i * 0.04 + t;
                    double currentR = r * (0.5 + (i / (double) spiralPoints) * 0.5); // vary radius slightly
                    positions.add(center.add(Math.cos(angle) * currentR, (i - spiralPoints / 2.0) * 0.01,
                            Math.sin(angle) * currentR));
                }
            }

            case TORUS -> {
                int uSteps = Math.max(1, (int) (24 * quality.get() * (r / 1.5)));
                int vSteps = Math.max(1, (int) (12 * quality.get()));
                double R = r, tubeR = r * 0.25;
                for (int i = 0; i < uSteps; i++) {
                    double u = 2 * Math.PI * i / uSteps + t;
                    for (int j = 0; j < vSteps; j++) {
                        double v = 2 * Math.PI * j / vSteps;
                        double x = (R + tubeR * Math.cos(v)) * Math.cos(u);
                        double y = tubeR * Math.sin(v);
                        double z = (R + tubeR * Math.cos(v)) * Math.sin(u);
                        positions.add(center.add(x, y, z));
                    }
                }
            }

            case PYRAMID -> {
                double h = r, s = r;
                Vec3d apex = center.add(0, h, 0);
                Vec3d[] base = {
                        center.add(-s, 0, -s),
                        center.add(s, 0, -s),
                        center.add(s, 0, s),
                        center.add(-s, 0, s)
                };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                // Base edges
                int[][] baseEdges = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 } };
                for (int[] e : baseEdges) {
                    Vec3d c1 = base[e[0]], c2 = base[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
                // Side edges
                for (Vec3d b : base) {
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(b.add(apex.subtract(b).multiply(alpha)));
                    }
                }
            }

            case DOUBLE_HELIX -> {
                int helixPoints = Math.max(1, (int) (50 * quality.get() * (r / 1.5)));
                double height = r * 2;
                for (int i = 0; i < helixPoints; i++) {
                    double ratio = i / (double) helixPoints;
                    double angle = i * 0.2 + t;
                    double y = (ratio - 0.5) * height;
                    double helixR = r * 0.5;
                    positions.add(center.add(Math.cos(angle) * helixR, y, Math.sin(angle) * helixR));
                    positions
                            .add(center.add(Math.cos(angle + Math.PI) * helixR, y, Math.sin(angle + Math.PI) * helixR));
                }
            }

            case ICOSAHEDRON -> {
                double phi = (1 + Math.sqrt(5)) / 2;
                double scale = r * 0.5;
                // Re-center vertices logic fixed below using rawVerts

                Vec3d[] rawVerts = {
                        new Vec3d(0, 1, phi), new Vec3d(0, -1, phi), new Vec3d(0, 1, -phi), new Vec3d(0, -1, -phi),
                        new Vec3d(1, phi, 0), new Vec3d(-1, phi, 0), new Vec3d(1, -phi, 0), new Vec3d(-1, -phi, 0),
                        new Vec3d(phi, 0, 1), new Vec3d(-phi, 0, 1), new Vec3d(phi, 0, -1), new Vec3d(-phi, 0, -1)
                };

                // Edges array is same
                int[][] edges = {
                        { 0, 1 }, { 0, 4 }, { 0, 5 }, { 0, 8 }, { 0, 9 },
                        { 1, 6 }, { 1, 7 }, { 1, 8 }, { 1, 9 },
                        { 2, 3 }, { 2, 4 }, { 2, 5 }, { 2, 10 }, { 2, 11 },
                        { 3, 6 }, { 3, 7 }, { 3, 10 }, { 3, 11 },
                        { 4, 5 }, { 4, 8 }, { 4, 9 }, { 4, 10 }, { 4, 11 },
                        { 5, 8 }, { 5, 9 }, { 5, 10 }, { 5, 11 },
                        { 6, 7 }, { 6, 8 }, { 6, 9 }, { 6, 10 }, { 6, 11 },
                        { 7, 8 }, { 7, 9 }, { 7, 10 }, { 7, 11 },
                        { 8, 9 }, { 8, 10 }, { 8, 11 },
                        { 9, 10 }, { 9, 11 },
                        { 10, 11 }
                };

                int edgePoints = Math.max(1, (int) (10 * quality.get() * (r / 1.5)));
                for (int[] e : edges) {
                    Vec3d p1 = rawVerts[e[0]].multiply(scale);
                    Vec3d p2 = rawVerts[e[1]].multiply(scale);
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(alpha))));
                    }
                }
            }

            case STAR -> {
                double size = r;
                Vec3d[] vertices = {
                        center.add(size, 0, 0),
                        center.add(-size, 0, 0),
                        center.add(0, size, 0),
                        center.add(0, -size, 0),
                        center.add(0, 0, size),
                        center.add(0, 0, -size)
                };
                int[][] edges = {
                        { 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 },
                        { 1, 2 }, { 1, 3 }, { 1, 4 }, { 1, 5 },
                        { 2, 4 }, { 2, 5 }, { 3, 4 }, { 3, 5 }
                };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                for (int[] e : edges) {
                    Vec3d c1 = vertices[e[0]], c2 = vertices[e[1]];
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(c1.add(c2.subtract(c1).multiply(alpha)));
                    }
                }
            }

            case HELIX_RING -> {
                int points = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                double helixR = r;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points + t;
                    double y = Math.sin(angle * 2) * (r * 0.3);
                    positions.add(center.add(Math.cos(angle) * helixR, y, Math.sin(angle) * helixR));
                }
            }

            case DIAMOND -> {
                double h = r;
                Vec3d top = center.add(0, h, 0);
                Vec3d bottom = center.add(0, -h, 0);
                double s = r * 0.7;
                Vec3d[] equator = {
                        center.add(s, 0, 0),
                        center.add(-s, 0, 0),
                        center.add(0, 0, s),
                        center.add(0, 0, -s)
                };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                for (Vec3d eq : equator) {
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(top.add(eq.subtract(top).multiply(alpha)));
                    }
                }
                for (Vec3d eq : equator) {
                    for (int p = 0; p <= edgePoints; p++) {
                        double alpha = p / (double) edgePoints;
                        positions.add(bottom.add(eq.subtract(bottom).multiply(alpha)));
                    }
                }
            }

            case ATOM -> {
                // Nucleus
                positions.add(center);
                int nucleusPoints = Math.max(4, (int) (8 * quality.get()));
                for (int i = 0; i < nucleusPoints; i++) {
                    positions.add(center.add((Math.random() - 0.5) * r * 0.2, (Math.random() - 0.5) * r * 0.2,
                            (Math.random() - 0.5) * r * 0.2));
                }
                // 3 Electron Rings
                int ringPoints = Math.max(1, (int) (40 * quality.get() * (r / 1.5)));
                for (int ring = 0; ring < 3; ring++) {
                    for (int i = 0; i < ringPoints; i++) {
                        double angle = 2 * Math.PI * i / ringPoints + t * 2;

                        Vec3d point = new Vec3d(r * Math.cos(angle), 0, r * Math.sin(angle)); // XZ circle

                        // Specific rotations for each ring
                        if (ring == 0) {
                            // XZ plane, no tilt needed, maybe just spin
                        } else if (ring == 1) {
                            // Rotate 60 deg around X
                            point = rotate(point, Vec3d.ZERO, Math.PI / 3, RotationAxis.X);
                        } else if (ring == 2) {
                            // Rotate -60 deg around X
                            point = rotate(point, Vec3d.ZERO, -Math.PI / 3, RotationAxis.X);
                        }

                        positions.add(center.add(point));
                    }
                }
            }

            case HEART -> {
                int points = Math.max(1, (int) (80 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    // Heart curve formula
                    // x = 16 sin^3 t
                    // y = 13 cos t - 5 cos 2t - 2 cos 3t - cos 4t
                    // Scale it down significantly
                    double scale = r * 0.05;
                    double x = 16 * Math.pow(Math.sin(angle), 3);
                    double y = 13 * Math.cos(angle) - 5 * Math.cos(2 * angle) - 2 * Math.cos(3 * angle)
                            - Math.cos(4 * angle);

                    // Center the heart (roughly)
                    y += 2;

                    positions.add(center.add(x * scale, y * scale, 0));
                }
            }

            case VORTEX -> {
                int points = Math.max(1, (int) (150 * quality.get() * (r / 1.5)));
                double height = r * 2.0;
                for (int i = 0; i < points; i++) {
                    double ratio = i / (double) points; // 0 to 1
                    double y = (ratio - 0.5) * height; // Bottom to top
                    double coneR = r * (0.2 + 0.8 * ratio); // 0.2r at bottom, 1.0r at top
                    double angle = i * 0.3 + t * 3; // Spin faster
                    positions.add(center.add(Math.cos(angle) * coneR, y, Math.sin(angle) * coneR));
                }
            }

            case TESSERACT -> {
                // Hypercube rotation in 4D projected to 3D
                double size = r;
                // 16 vertices of a tesseract (±1, ±1, ±1, ±1)
                // We will animate rotations in ZW and XW planes to show 4D structure

                double[][] verts4d = new double[16][4];
                int idx = 0;
                for (int x = -1; x <= 1; x += 2)
                    for (int y = -1; y <= 1; y += 2)
                        for (int z = -1; z <= 1; z += 2)
                            for (int w = -1; w <= 1; w += 2)
                                verts4d[idx++] = new double[] { x * size, y * size, z * size, w * size };

                // Edges: connect if distance is 2*size (change in exactly one coordinate)
                List<int[]> edges = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    for (int j = i + 1; j < 16; j++) {
                        int diffs = 0;
                        for (int k = 0; k < 4; k++)
                            if (verts4d[i][k] != verts4d[j][k])
                                diffs++;
                        if (diffs == 1)
                            edges.add(new int[] { i, j });
                    }
                }

                // Rotate and Project
                Vec3d[] projected = new Vec3d[16];
                for (int i = 0; i < 16; i++) {
                    double[] p = verts4d[i].clone();
                    // Rotate ZW plane
                    double ang = t;
                    double nz = p[2] * Math.cos(ang) - p[3] * Math.sin(ang);
                    double nw = p[2] * Math.sin(ang) + p[3] * Math.cos(ang);
                    p[2] = nz;
                    p[3] = nw;

                    // Rotate XW plane
                    double ang2 = t * 0.5;
                    double nx = p[0] * Math.cos(ang2) - p[3] * Math.sin(ang2);
                    nw = p[0] * Math.sin(ang2) + p[3] * Math.cos(ang2);
                    p[0] = nx;
                    p[3] = nw;

                    // Project 4D to 3D: perspective w
                    double distance = 3.0 * size;
                    double wFactor = 1 / (distance - p[3]);
                    projected[i] = new Vec3d(p[0] * wFactor * distance, p[1] * wFactor * distance,
                            p[2] * wFactor * distance);
                }

                int edgePoints = Math.max(1, (int) (10 * quality.get()));
                for (int[] e : edges) {
                    Vec3d p1 = projected[e[0]];
                    Vec3d p2 = projected[e[1]];
                    for (int p_step = 0; p_step <= edgePoints; p_step++) {
                        double alpha = p_step / (double) edgePoints;
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(alpha))));
                    }
                }
            }

            case OCTAHEDRON -> {
                double s = r;
                Vec3d[] verts = {
                        new Vec3d(s, 0, 0), new Vec3d(-s, 0, 0),
                        new Vec3d(0, s, 0), new Vec3d(0, -s, 0),
                        new Vec3d(0, 0, s), new Vec3d(0, 0, -s)
                };
                int[][] edges = {
                        { 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 },
                        { 1, 2 }, { 1, 3 }, { 1, 4 }, { 1, 5 },
                        { 2, 4 }, { 2, 5 }, { 3, 4 }, { 3, 5 }
                };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                for (int[] e : edges) {
                    Vec3d p1 = verts[e[0]];
                    Vec3d p2 = verts[e[1]];
                    for (int i = 0; i <= edgePoints; i++) {
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(i / (double) edgePoints))));
                    }
                }
            }

            case DODECAHEDRON -> {
                double phi = (1 + Math.sqrt(5)) / 2;
                double s = r * 0.6;
                double invPhi = 1.0 / phi;

                List<Vec3d> verts = new ArrayList<>();
                // Cube vertices (+-1, +-1, +-1)
                for (int i = -1; i <= 1; i += 2)
                    for (int j = -1; j <= 1; j += 2)
                        for (int k = -1; k <= 1; k += 2)
                            verts.add(new Vec3d(i, j, k).multiply(s));

                // (0, +-phi, +-1/phi)
                for (int i = -1; i <= 1; i += 2)
                    for (int j = -1; j <= 1; j += 2)
                        verts.add(new Vec3d(0, i * phi, j * invPhi).multiply(s));

                // (+-1/phi, 0, +-phi)
                for (int i = -1; i <= 1; i += 2)
                    for (int j = -1; j <= 1; j += 2)
                        verts.add(new Vec3d(i * invPhi, 0, j * phi).multiply(s));

                // (+-phi, +-1/phi, 0)
                for (int i = -1; i <= 1; i += 2)
                    for (int j = -1; j <= 1; j += 2)
                        verts.add(new Vec3d(i * phi, j * invPhi, 0).multiply(s));

                double edgeLen = 2.0 / phi * s;
                double tolerance = 0.1 * s;

                int edgePoints = Math.max(1, (int) (10 * quality.get() * (r / 1.5)));
                for (int i = 0; i < verts.size(); i++) {
                    for (int j = i + 1; j < verts.size(); j++) {
                        if (Math.abs(verts.get(i).distanceTo(verts.get(j)) - edgeLen) < tolerance) {
                            Vec3d p1 = verts.get(i), p2 = verts.get(j);
                            for (int k = 0; k <= edgePoints; k++) {
                                positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                            }
                        }
                    }
                }
            }

            case MERKABA -> {
                // Two Tetrahedrons
                double s = r;
                Vec3d[] t1 = {
                        new Vec3d(s, s, s), new Vec3d(-s, -s, s), new Vec3d(-s, s, -s), new Vec3d(s, -s, -s)
                };
                Vec3d[] t2 = { // Inverted
                        new Vec3d(-s, -s, -s), new Vec3d(s, s, -s), new Vec3d(s, -s, s), new Vec3d(-s, s, s)
                };

                int[][] edges = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 2 }, { 1, 3 }, { 2, 3 } };
                int edgePoints = Math.max(1, (int) (15 * quality.get() * (r / 1.5)));

                for (int[] e : edges) {
                    Vec3d p1 = t1[e[0]], p2 = t1[e[1]];
                    for (int k = 0; k <= edgePoints; k++)
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));

                    p1 = t2[e[0]];
                    p2 = t2[e[1]];
                    for (int k = 0; k <= edgePoints; k++)
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                }
            }

            case MOBIUS -> {
                int uSteps = Math.max(1, (int) (100 * quality.get() * (r / 1.5)));
                int vSteps = Math.max(1, (int) (10 * quality.get()));
                for (int i = 0; i < uSteps; i++) {
                    double u = 2 * Math.PI * i / uSteps + t;
                    for (int j = 0; j < vSteps; j++) {
                        double v = r * 0.4 * (j / (double) vSteps - 0.5) * 2; // Width relative to radius
                        double x = (r + v * Math.cos(u / 2)) * Math.cos(u);
                        double y = (r + v * Math.cos(u / 2)) * Math.sin(u);
                        double z = v * Math.sin(u / 2);
                        positions.add(center.add(x, y, z));
                    }
                }
            }

            case SATURN -> {
                // Sphere
                int density = Math.max(1, (int) (100 * quality.get() * (r / 1.5)));
                double sphereR = r * 0.7;
                for (int i = 0; i < density; i++) {
                    double u = Math.random();
                    double v = Math.random();
                    double theta = 2 * Math.PI * u;
                    double phi = Math.acos(2.0 * v - 1.0);
                    double x = sphereR * Math.sin(phi) * Math.cos(theta);
                    double y = sphereR * Math.sin(phi) * Math.sin(theta);
                    double z = sphereR * Math.cos(phi);
                    positions.add(center.add(x, y, z));
                }
                // Ring
                int ringPoints = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                for (int i = 0; i < ringPoints; i++) {
                    double ang = 2 * Math.PI * i / ringPoints + t;
                    double ringR = r * (1.2 + Math.random() * 0.4); // 1.2 to 1.6 r
                    positions.add(center.add(Math.cos(ang) * ringR, 0, Math.sin(ang) * ringR));
                }
            }

            case INFINITY -> {
                int points = Math.max(1, (int) (150 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double ang = 2 * Math.PI * i / points + t;
                    // Lemniscate of Bernoulli
                    double scale = r * 1.5;
                    double denom = 1 + Math.sin(ang) * Math.sin(ang);
                    double x = scale * Math.cos(ang) / denom;
                    double z = scale * Math.sin(ang) * Math.cos(ang) / denom;
                    positions.add(center.add(x, z, 0)); // Flat infinity standing up? or laying down? x,0,z
                }
            }

            case TRIFORCE -> {
                double s = r;
                // Top triangle center (0, s, 0) ? No, typically flat 2D symbol
                // Let's make it 3D triangular prism edges? Or just the 2D symbol floating.
                // 3 vertices of big triangle: (0, s), (-s, -s), (s, -s)
                Vec3d top = new Vec3d(0, s, 0);
                Vec3d botL = new Vec3d(-s, -s, 0);
                Vec3d botR = new Vec3d(s, -s, 0);
                Vec3d midL = top.add(botL).multiply(0.5);
                Vec3d midR = top.add(botR).multiply(0.5);
                Vec3d botM = botL.add(botR).multiply(0.5);

                // 3 Triangles: Top (top, midL, midR), BotL (midL, botL, botM), BotR (midR,
                // botM, botR)
                Vec3d[][] tris = {
                        { top, midL, midR },
                        { midL, botL, botM },
                        { midR, botM, botR }
                };

                int edgePoints = Math.max(1, (int) (10 * quality.get() * (r / 1.5)));
                for (Vec3d[] tri : tris) {
                    // Edges 0-1, 1-2, 2-0
                    for (int i = 0; i < 3; i++) {
                        Vec3d p1 = tri[i];
                        Vec3d p2 = tri[(i + 1) % 3];
                        for (int k = 0; k <= edgePoints; k++) {
                            positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                        }
                    }
                }
            }

            case CROSS -> {
                // Simple 3 axis cross or religious cross?
                // User likely means "Geometric shapes" -> Cross usually 3 axes.
                // But "Triforce" is pop culture. Maybe Christian cross?
                // Let's do a 3D geometric cross (6-pointed star / Jax) first as it fits
                // "geometric shapes".
                // Actually, "Cross" typically implies the religious one if listed with things
                // like Pentagram.
                // I'll do a 3D "Jack" shape (3 axes intersecting) as it's more generic.
                double s = r;
                int points = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                Vec3d[] dirs = {
                        new Vec3d(s, 0, 0), new Vec3d(-s, 0, 0),
                        new Vec3d(0, s, 0), new Vec3d(0, -s, 0),
                        new Vec3d(0, 0, s), new Vec3d(0, 0, -s)
                };
                for (Vec3d d : dirs) {
                    for (int k = 0; k <= points; k++) {
                        positions.add(center.add(d.multiply(k / (double) points)));
                    }
                }
            }

            case PENTAGRAM -> {
                double s = r;
                Vec3d[] pts = new Vec3d[5];
                for (int i = 0; i < 5; i++) {
                    double a = 2 * Math.PI * i / 5 - Math.PI / 2; // Start top
                    pts[i] = new Vec3d(Math.cos(a) * s, Math.sin(a) * s, 0);
                }
                // Connect 0-2, 2-4, 4-1, 1-3, 3-0
                int[] flow = { 0, 2, 4, 1, 3, 0 };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                for (int i = 0; i < 5; i++) {
                    Vec3d p1 = pts[flow[i]];
                    Vec3d p2 = pts[flow[i + 1]];
                    for (int k = 0; k <= edgePoints; k++) {
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                    }
                }
                // Circle around it
                int circPoints = Math.max(1, (int) (50 * quality.get()));
                for (int i = 0; i < circPoints; i++) {
                    double a = 2 * Math.PI * i / circPoints;
                    positions.add(center.add(Math.cos(a) * s, Math.sin(a) * s, 0));
                }
            }

            case CYLINDER -> {
                int points = Math.max(1, (int) (100 * quality.get()));
                double h = r;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points + t;
                    // Top rim
                    positions.add(center.add(Math.cos(angle) * r, h, Math.sin(angle) * r));
                    // Bottom rim
                    positions.add(center.add(Math.cos(angle) * r, -h, Math.sin(angle) * r));
                    // Side lines (vertical) - fewer of them
                    if (i % 5 == 0) {
                        int segs = 10;
                        for (int j = 0; j <= segs; j++) {
                            double y = -h + (2 * h * j / segs);
                            positions.add(center.add(Math.cos(angle) * r, y, Math.sin(angle) * r));
                        }
                    }
                }
            }

            case CONE -> {
                int points = Math.max(1, (int) (100 * quality.get()));
                double h = r;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points + t;
                    // Base rim
                    positions.add(center.add(Math.cos(angle) * r, -h, Math.sin(angle) * r));
                    // Lines to apex (0, h, 0)
                    if (i % 5 == 0) {
                        int segs = 10;
                        for (int j = 0; j <= segs; j++) {
                            double frac = j / (double) segs;
                            double curR = r * (1 - frac);
                            double y = -h + (2 * h * frac);
                            positions.add(center.add(Math.cos(angle) * curR, y, Math.sin(angle) * curR));
                        }
                    }
                }
            }

            case PRISM -> {
                double h = r;
                double s = r;
                // Triangular prism
                for (int i = 0; i < 3; i++) {
                    double a1 = 2 * Math.PI * i / 3 - Math.PI / 2;
                    double a2 = 2 * Math.PI * (i + 1) / 3 - Math.PI / 2;

                    Vec3d p1_top = new Vec3d(Math.cos(a1) * s, h, Math.sin(a1) * s);
                    Vec3d p2_top = new Vec3d(Math.cos(a2) * s, h, Math.sin(a2) * s);

                    Vec3d p1_bot = new Vec3d(Math.cos(a1) * s, -h, Math.sin(a1) * s);
                    Vec3d p2_bot = new Vec3d(Math.cos(a2) * s, -h, Math.sin(a2) * s);

                    int edgePoints = Math.max(1, (int) (15 * quality.get() * (r / 1.5)));
                    for (int k = 0; k <= edgePoints; k++) {
                        double alpha = k / (double) edgePoints;
                        // Top edge
                        positions.add(center.add(p1_top.add(p2_top.subtract(p1_top).multiply(alpha))));
                        // Bottom edge
                        positions.add(center.add(p1_bot.add(p2_bot.subtract(p1_bot).multiply(alpha))));
                        // Vertical edge at p1
                        positions.add(center.add(p1_bot.add(p1_top.subtract(p1_bot).multiply(alpha))));
                    }
                }
            }

            case TETRAHEDRON -> {
                double s = r;
                // Regular tetrahedron: (1,1,1), (1,-1,-1), (-1,1,-1), (-1,-1,1)
                Vec3d[] verts = {
                        new Vec3d(s, s, s),
                        new Vec3d(s, -s, -s),
                        new Vec3d(-s, s, -s),
                        new Vec3d(-s, -s, s)
                };
                int[][] edges = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 2 }, { 1, 3 }, { 2, 3 } };
                int edgePoints = Math.max(1, (int) (20 * quality.get() * (r / 1.5)));
                for (int[] e : edges) {
                    Vec3d p1 = verts[e[0]];
                    Vec3d p2 = verts[e[1]];
                    for (int k = 0; k <= edgePoints; k++) {
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                    }
                }
            }

            case TRUNCATED_OCTAHEDRON -> {
                // Vertices are permutations of (0, ±1, ±2) scaled
                double s = r * 0.5;
                // Permutations of (0, +/-s, +/-2s)
                List<Vec3d> raw = new ArrayList<>();
                for (int i = -1; i <= 1; i += 2) {
                    for (int j = -1; j <= 1; j += 2) {
                        raw.add(new Vec3d(0, i * s, j * 2 * s));
                        raw.add(new Vec3d(0, i * 2 * s, j * s));
                        raw.add(new Vec3d(i * s, 0, j * 2 * s));
                        raw.add(new Vec3d(i * 2 * s, 0, j * s));
                        raw.add(new Vec3d(i * s, j * 2 * s, 0));
                        raw.add(new Vec3d(i * 2 * s, j * s, 0));
                    }
                }
                // 4*6 = 24 verts.
                // Edges: dist = sqrt(1^2 + 1^2) * s = sqrt(2)*s.
                double edgeLen = Math.sqrt(2) * s;
                double tol = 0.1 * s;
                int edgePoints = Math.max(1, (int) (10 * quality.get() * (r / 1.5)));

                for (int i = 0; i < raw.size(); i++) {
                    for (int j = i + 1; j < raw.size(); j++) {
                        if (Math.abs(raw.get(i).distanceTo(raw.get(j)) - edgeLen) < tol) {
                            Vec3d p1 = raw.get(i), p2 = raw.get(j);
                            for (int k = 0; k <= edgePoints; k++)
                                positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                        }
                    }
                }
            }

            case RHOMBIC_DODECAHEDRON -> {
                // Vertices: (+-1, +-1, +-1) and (+-2, 0, 0) perms
                double s = r * 0.5;
                List<Vec3d> verts = new ArrayList<>();
                for (int i = -1; i <= 1; i += 2)
                    for (int j = -1; j <= 1; j += 2)
                        for (int k = -1; k <= 1; k += 2)
                            verts.add(new Vec3d(i * s, j * s, k * s));

                for (int i = -1; i <= 1; i += 2) {
                    verts.add(new Vec3d(i * 2 * s, 0, 0));
                    verts.add(new Vec3d(0, i * 2 * s, 0));
                    verts.add(new Vec3d(0, 0, i * 2 * s));
                }
                // 8 + 6 = 14 verts.
                // Edges connect (1,1,1) type to (2,0,0) type?
                // dist between (1,1,1) and (2,0,0) is sqrt(1+1+1) = sqrt(3).
                double edgeLen = Math.sqrt(3) * s;
                double tol = 0.1 * s;
                int edgePoints = Math.max(1, (int) (10 * quality.get() * (r / 1.5)));
                for (int i = 0; i < verts.size(); i++) {
                    for (int j = i + 1; j < verts.size(); j++) {
                        if (Math.abs(verts.get(i).distanceTo(verts.get(j)) - edgeLen) < tol) {
                            Vec3d p1 = verts.get(i), p2 = verts.get(j);
                            for (int k = 0; k <= edgePoints; k++)
                                positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                        }
                    }
                }
            }

            case BUCKYBALL -> {
                // Truncated Icosahedron. Complex.
                // Let's do a wireframe sphere approximation with hexagons/pentagons pattern
                // logic is hard.
                // Simpler: Geodesic sphere points.
                // Actual buckyball: 60 vertices.
                // Use Golden Ratio phi
                double phi = (1 + Math.sqrt(5)) / 2;
                // Vertices are even permutations of:
                // (0, ±1, ±3phi)
                // (±2, ±(1+2phi), ±phi)
                // (±1, ±(2+phi), ±2phi)
                // SCALED DOWN.

                // Alternative: Just generate points on sphere surface with rejection sampling
                // to look like "nodes" or just simple dense sphere (we have SPHERE).
                // Let's do a C60 molecule look:
                // 60 points.
                // We can use the Icosahedron vertices and subdivide or truncate.
                // Truncation: Cut tips of Icosahedron at 1/3 distance.

                // Icosahedron Verts:
                List<Vec3d> ico = new ArrayList<>();
                for (int i = -1; i <= 1; i += 2)
                    for (int k = -1; k <= 1; k += 2) {
                        ico.add(new Vec3d(0, i, k * phi));
                        ico.add(new Vec3d(i, k * phi, 0));
                        ico.add(new Vec3d(k * phi, 0, i));
                    }
                // 12 verts.
                // Get edges of Icosahedron and put 2 points at 1/3 and 2/3.
                List<Vec3d> buckyVerts = new ArrayList<>();

                for (int i = 0; i < ico.size(); i++) {
                    for (int j = i + 1; j < ico.size(); j++) {
                        // Edge len is 2 for (0,1,phi) construction?
                        // Check dist: (0,1,phi) to (0,-1,phi) = 2. Correct.
                        // (0,1,phi) to (phi,0,1)? sqrt(phi^2 + 1 + (phi-1)^2) ?
                        if (Math.abs(ico.get(i).distanceTo(ico.get(j)) - 2.0) < 0.1) {
                            Vec3d p1 = ico.get(i);
                            Vec3d p2 = ico.get(j);
                            buckyVerts.add(p1.add(p2.subtract(p1).multiply(1.0 / 3.0)));
                            buckyVerts.add(p1.add(p2.subtract(p1).multiply(2.0 / 3.0)));
                        }
                    }
                }
                // Scale and offset
                for (Vec3d v : buckyVerts)
                    positions.add(center.add(v.multiply(r / Math.sqrt(1 + phi * phi)))); // Normalize size
            }

            case TORUS_KNOT -> {
                int p = 2, q = 3; // Trefoil
                int points = Math.max(1, (int) (300 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double phi = 2 * Math.PI * i / points + t;
                    double r_knot = r * 0.5;
                    double x = (r + r_knot * Math.cos(q * phi)) * Math.cos(p * phi);
                    double y = r_knot * Math.sin(q * phi);
                    double z = (r + r_knot * Math.cos(q * phi)) * Math.sin(p * phi);
                    positions.add(center.add(x, y, z));
                }
            }

            case KLEIN_BOTTLE -> {
                int uSteps = Math.max(1, (int) (60 * quality.get()));
                int vSteps = Math.max(1, (int) (20 * quality.get()));
                for (int i = 0; i < uSteps; i++) {
                    double u = 2 * Math.PI * i / uSteps;
                    for (int j = 0; j < vSteps; j++) {
                        double v = 2 * Math.PI * j / vSteps;
                        double scale = r * 0.2;
                        double a_klein = 3;
                        double x = (a_klein + Math.cos(u / 2) * Math.sin(v) - Math.sin(u / 2) * Math.sin(2 * v))
                                * Math.cos(u);
                        double y = (a_klein + Math.cos(u / 2) * Math.sin(v) - Math.sin(u / 2) * Math.sin(2 * v))
                                * Math.sin(u);
                        double z = Math.sin(u / 2) * Math.sin(v) + Math.cos(u / 2) * Math.sin(2 * v);

                        positions.add(center.add(new Vec3d(x, y, z).multiply(scale)));
                    }
                }
            }

            case ASTROID -> {
                int points = Math.max(1, (int) (100 * quality.get()));
                for (int i = 0; i < points; i++) {
                    // Astroid is 2D. 3D is Astroidal ellipsoid?
                    // x^(2/3) + y^(2/3) + z^(2/3) = a^(2/3)
                    // Parametric: x = a cos^3 u cos^3 v, y = a sin^3 u cos^3 v, z = a sin^3 v

                    // Random sampling or systematic
                    double u = Math.random() * 2 * Math.PI;
                    double v = Math.random() * Math.PI - Math.PI / 2;

                    double cu = Math.cos(u), su = Math.sin(u);
                    double cv = Math.cos(v), sv = Math.sin(v);

                    double x = r * Math.pow(cu, 3) * Math.pow(cv, 3);
                    double y = r * Math.pow(su, 3) * Math.pow(cv, 3);
                    double z = r * Math.pow(sv, 3);
                    positions.add(center.add(x, y, z));
                }
            }

            case SPIRAL_SPHERE -> {
                int points = Math.max(1, (int) (300 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double lat = Math.acos(2.0 * i / points - 1.0) - Math.PI / 2;
                    double lon = Math.sqrt(points * Math.PI) * lat + t;
                    double x = r * Math.cos(lat) * Math.cos(lon);
                    double y = r * Math.cos(lat) * Math.sin(lon);
                    double z = r * Math.sin(lat);
                    positions.add(center.add(x, y, z));
                }
            }

            case ROSE -> {
                int k = 4; // Petals
                int points = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double theta = 2 * Math.PI * i / points + t;
                    double radius = r * Math.cos(k * theta);
                    // Map to 2D in 3D space
                    positions.add(center.add(radius * Math.cos(theta), radius * Math.sin(theta), 0));
                    // Add a second orthogonal one
                    positions.add(center.add(radius * Math.cos(theta), 0, radius * Math.sin(theta)));
                }
            }

            case BUTTERFLY -> {
                int points = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double u = 12 * Math.PI * i / points;
                    double val = Math.exp(Math.cos(u)) - 2 * Math.cos(4 * u) - Math.pow(Math.sin(u / 12), 5);
                    double x = r * 0.3 * Math.sin(u) * val;
                    double y = r * 0.3 * Math.cos(u) * val; // z?
                    // Add rotation or volume
                    positions.add(center.add(x, y, 0));
                }
            }

            case LISSAJOUS -> {
                int points = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                double A = r, B = r, C = r;
                double a = 3, b = 2, c = 4; // Frequencies
                for (int i = 0; i < points; i++) {
                    double ang = 2 * Math.PI * i / points + t;
                    double x = A * Math.sin(a * ang);
                    double y = B * Math.sin(b * ang);
                    double z = C * Math.sin(c * ang);
                    positions.add(center.add(x, y, z));
                }
            }

            case HELICOID -> {
                int uSteps = 20;
                int vSteps = Math.max(1, (int) (10 * quality.get()));
                for (int i = 0; i < uSteps; i++) {
                    double u = i * 0.5 - 5; // range
                    for (int j = 0; j < vSteps; j++) {
                        double v = 2 * Math.PI * j / vSteps + t;
                        double x = u * Math.cos(v);
                        double y = u * Math.sin(v);
                        double z = v * 0.5; // pitch
                        // Scale down
                        positions.add(center.add(new Vec3d(x, z, y).multiply(r * 0.2)));
                    }
                }
            }

            case CATENOID -> {
                int uSteps = Math.max(1, (int) (30 * quality.get()));
                int vSteps = Math.max(1, (int) (30 * quality.get()));
                double c = r * 0.5;
                for (int i = 0; i < uSteps; i++) {
                    double u = (i / (double) uSteps - 0.5) * 2; // -1 to 1
                    for (int j = 0; j < vSteps; j++) {
                        double v = 2 * Math.PI * j / vSteps;
                        double x = c * Math.cosh(u / c) * Math.cos(v);
                        double z = c * Math.cosh(u / c) * Math.sin(v);
                        double y = u;
                        positions.add(center.add(x, y, z));
                    }
                }
            }

            case HYPERBOLOID -> {
                // One sheet
                int uSteps = Math.max(1, (int) (30 * quality.get()));
                int vSteps = Math.max(1, (int) (30 * quality.get()));
                double a = r * 0.5, b = r * 0.5, c = r * 0.5;
                for (int i = 0; i < uSteps; i++) {
                    double u = (i / (double) uSteps - 0.5) * 4; // -2 to 2
                    for (int j = 0; j < vSteps; j++) {
                        double v = 2 * Math.PI * j / vSteps + t;
                        double x = a * Math.sqrt(1 + u * u) * Math.cos(v);
                        double z = b * Math.sqrt(1 + u * u) * Math.sin(v);
                        double y = c * u;
                        positions.add(center.add(x, y, z));
                    }
                }
            }

            case UMBRELLA -> {
                int points = Math.max(1, (int) (150 * quality.get()));
                for (int i = 0; i < points; i++) {
                    // Dome: y = - (x^2 + z^2)
                    double angle = Math.random() * 2 * Math.PI;
                    double rad = Math.sqrt(Math.random()) * r;
                    double y = -(rad * rad) / r + r; // Shift up
                    double x = rad * Math.cos(angle);
                    double z = rad * Math.sin(angle);
                    positions.add(center.add(x, y, z));
                    // Handle: line down from center
                    if (i < points / 5) {
                        double hy = r - (i / (double) (points / 5)) * 2 * r;
                        positions.add(center.add(0, hy, 0));
                    }
                }
            }

            case FLOWER -> {
                int points = Math.max(1, (int) (200 * quality.get() * (r / 1.5)));
                for (int i = 0; i < points; i++) {
                    double u = 2 * Math.PI * i / points;
                    double v = 2 * Math.PI * (i * 10) / points + t; // Petals
                    double rad = r * (0.5 + 0.5 * Math.sin(5 * u)); // 5 petals
                    // Cup shape
                    double x = rad * Math.cos(u) * Math.sin(v / 2); // Variating
                    double z = rad * Math.sin(u) * Math.sin(v / 2);
                    double y = rad * Math.cos(v / 2);

                    // Simplified flower:
                    rad = r * Math.abs(Math.cos(2.5 * u)); // 5 petals
                    x = rad * Math.cos(u);
                    z = rad * Math.sin(u);
                    y = -(x * x + z * z) / r;
                    positions.add(center.add(x, y, z));
                }
            }

            case SNOWFLAKE -> {
                // 6 spokes with branches
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3 + t;
                    Vec3d dir = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
                    int segs = Math.max(5, (int) (10 * quality.get()));
                    for (int j = 0; j < segs; j++) {
                        double dist = r * j / segs;
                        Vec3d p = center.add(dir.multiply(dist));
                        positions.add(p);
                        // Branches
                        if (j > segs / 2 && j % 2 == 0) {
                            double bLen = r * 0.2 * (1 - j / (double) segs);
                            double bAngle = Math.PI / 3;
                            // Rotate dir by bAngle
                            Vec3d bDir1 = new Vec3d(Math.cos(angle + bAngle), 0, Math.sin(angle + bAngle));
                            Vec3d bDir2 = new Vec3d(Math.cos(angle - bAngle), 0, Math.sin(angle - bAngle));
                            for (int k = 1; k <= 3; k++) {
                                positions.add(p.add(bDir1.multiply(bLen * k / 3)));
                                positions.add(p.add(bDir2.multiply(bLen * k / 3)));
                            }
                        }
                    }
                }
            }

            case DNA_RUNGS -> {
                int helixPoints = Math.max(1, (int) (50 * quality.get() * (r / 1.5)));
                double height = r * 2;
                double helixR = r * 0.5;
                for (int i = 0; i < helixPoints; i++) {
                    double ratio = i / (double) helixPoints;
                    double angle = i * 0.2 + t;
                    double y = (ratio - 0.5) * height;
                    Vec3d p1 = center.add(Math.cos(angle) * helixR, y, Math.sin(angle) * helixR);
                    Vec3d p2 = center.add(Math.cos(angle + Math.PI) * helixR, y, Math.sin(angle + Math.PI) * helixR);
                    positions.add(p1);
                    positions.add(p2);
                    // Rungs
                    if (i % 5 == 0) {
                        for (int k = 1; k < 5; k++) {
                            positions.add(p1.add(p2.subtract(p1).multiply(k / 5.0)));
                        }
                    }
                }
            }

            case SPHERE_GRID -> {
                int latLines = Math.max(4, (int) (10 * quality.get()));
                int lonLines = Math.max(4, (int) (10 * quality.get()));
                for (int i = 0; i <= latLines; i++) {
                    double phi = Math.PI * i / latLines;
                    for (int j = 0; j < 100 * quality.get(); j++) {
                        double theta = 2 * Math.PI * j / (100 * quality.get());
                        positions.add(center.add(r * Math.sin(phi) * Math.cos(theta), r * Math.cos(phi),
                                r * Math.sin(phi) * Math.sin(theta)));
                    }
                }
                for (int i = 0; i < lonLines; i++) {
                    double theta = 2 * Math.PI * i / lonLines;
                    for (int j = 0; j < 100 * quality.get(); j++) {
                        double phi = Math.PI * j / (100 * quality.get());
                        positions.add(center.add(r * Math.sin(phi) * Math.cos(theta), r * Math.cos(phi),
                                r * Math.sin(phi) * Math.sin(theta)));
                    }
                }
            }

            case CUBE_GRID -> {
                double size = r;
                int gridLines = Math.max(2, (int) (5 * quality.get()));
                for (int i = -gridLines; i <= gridLines; i++) {
                    double offset = size * i / gridLines;
                    for (int j = 0; j < 50 * quality.get(); j++) {
                        double p = size * (j / (50.0 * quality.get()) * 2 - 1);
                        positions.add(center.add(offset, p, size));
                        positions.add(center.add(offset, p, -size));
                        positions.add(center.add(offset, size, p));
                        positions.add(center.add(offset, -size, p));
                        positions.add(center.add(size, offset, p));
                        positions.add(center.add(-size, offset, p));
                    }
                }
            }

            case PENTAGONAL_PRISM -> {
                double h = r;
                double s = r;
                for (int i = 0; i < 5; i++) {
                    double a1 = 2 * Math.PI * i / 5 - Math.PI / 2;
                    double a2 = 2 * Math.PI * (i + 1) / 5 - Math.PI / 2;
                    Vec3d p1_top = new Vec3d(Math.cos(a1) * s, h, Math.sin(a1) * s);
                    Vec3d p2_top = new Vec3d(Math.cos(a2) * s, h, Math.sin(a2) * s);
                    Vec3d p1_bot = new Vec3d(Math.cos(a1) * s, -h, Math.sin(a1) * s);
                    Vec3d p2_bot = new Vec3d(Math.cos(a2) * s, -h, Math.sin(a2) * s);
                    int edgePoints = Math.max(1, (int) (15 * quality.get()));
                    for (int k = 0; k <= edgePoints; k++) {
                        double alpha = k / (double) edgePoints;
                        positions.add(center.add(p1_top.add(p2_top.subtract(p1_top).multiply(alpha))));
                        positions.add(center.add(p1_bot.add(p2_bot.subtract(p1_bot).multiply(alpha))));
                        positions.add(center.add(p1_bot.add(p1_top.subtract(p1_bot).multiply(alpha))));
                    }
                }
            }

            case HEXAGONAL_PRISM -> {
                double h = r;
                double s = r;
                for (int i = 0; i < 6; i++) {
                    double a1 = 2 * Math.PI * i / 6 - Math.PI / 2;
                    double a2 = 2 * Math.PI * (i + 1) / 6 - Math.PI / 2;
                    Vec3d p1_top = new Vec3d(Math.cos(a1) * s, h, Math.sin(a1) * s);
                    Vec3d p2_top = new Vec3d(Math.cos(a2) * s, h, Math.sin(a2) * s);
                    Vec3d p1_bot = new Vec3d(Math.cos(a1) * s, -h, Math.sin(a1) * s);
                    Vec3d p2_bot = new Vec3d(Math.cos(a2) * s, -h, Math.sin(a2) * s);
                    int edgePoints = Math.max(1, (int) (15 * quality.get()));
                    for (int k = 0; k <= edgePoints; k++) {
                        double alpha = k / (double) edgePoints;
                        positions.add(center.add(p1_top.add(p2_top.subtract(p1_top).multiply(alpha))));
                        positions.add(center.add(p1_bot.add(p2_bot.subtract(p1_bot).multiply(alpha))));
                        positions.add(center.add(p1_bot.add(p1_top.subtract(p1_bot).multiply(alpha))));
                    }
                }
            }

            case PENTAGONAL_PYRAMID -> {
                double h = r;
                double s = r;
                Vec3d apex = new Vec3d(0, h, 0);
                for (int i = 0; i < 5; i++) {
                    double a1 = 2 * Math.PI * i / 5 - Math.PI / 2;
                    double a2 = 2 * Math.PI * (i + 1) / 5 - Math.PI / 2;
                    Vec3d p1 = new Vec3d(Math.cos(a1) * s, -h, Math.sin(a1) * s);
                    Vec3d p2 = new Vec3d(Math.cos(a2) * s, -h, Math.sin(a2) * s);
                    int edgePoints = Math.max(1, (int) (15 * quality.get()));
                    for (int k = 0; k <= edgePoints; k++) {
                        double alpha = k / (double) edgePoints;
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(alpha))));
                        positions.add(center.add(p1.add(apex.subtract(p1).multiply(alpha))));
                    }
                }
            }

            case HEXAGONAL_PYRAMID -> {
                double h = r;
                double s = r;
                Vec3d apex = new Vec3d(0, h, 0);
                for (int i = 0; i < 6; i++) {
                    double a1 = 2 * Math.PI * i / 6 - Math.PI / 2;
                    double a2 = 2 * Math.PI * (i + 1) / 6 - Math.PI / 2;
                    Vec3d p1 = new Vec3d(Math.cos(a1) * s, -h, Math.sin(a1) * s);
                    Vec3d p2 = new Vec3d(Math.cos(a2) * s, -h, Math.sin(a2) * s);
                    int edgePoints = Math.max(1, (int) (15 * quality.get()));
                    for (int k = 0; k <= edgePoints; k++) {
                        double alpha = k / (double) edgePoints;
                        positions.add(center.add(p1.add(p2.subtract(p1).multiply(alpha))));
                        positions.add(center.add(p1.add(apex.subtract(p1).multiply(alpha))));
                    }
                }
            }

            case FIG_8_KNOT -> {
                int points = Math.max(1, (int) (300 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double u = 2 * Math.PI * i / points + t;
                    double x = r * (2 + Math.cos(2 * u)) * Math.cos(3 * u) * 0.4;
                    double y = r * (2 + Math.cos(2 * u)) * Math.sin(3 * u) * 0.4;
                    double z = r * Math.sin(4 * u) * 0.4;
                    positions.add(center.add(x, y, z));
                }
            }

            case CINQUEFOIL_KNOT -> {
                int points = Math.max(1, (int) (300 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double u = 2 * Math.PI * i / points + t;
                    double x = r * (2 + Math.cos(5 * u / 2)) * Math.cos(u) * 0.4;
                    double y = r * (2 + Math.cos(5 * u / 2)) * Math.sin(u) * 0.4;
                    double z = r * Math.sin(5 * u / 2) * 0.4;
                    positions.add(center.add(x, y, z));
                }
            }

            case GALAXY -> {
                int points = Math.max(1, (int) (400 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double angle = i * 0.1 + t;
                    double dist = (i / (double) points) * r;
                    double spiral = angle + dist * 2;
                    double x = Math.cos(spiral) * dist;
                    double z = Math.sin(spiral) * dist;
                    double y = (Math.random() - 0.5) * 0.2 * (1 - dist / r);
                    positions.add(center.add(x, y, z));
                    positions.add(center.add(-x, -y, -z));
                }
            }

            case BLACK_HOLE -> {
                int points = Math.max(1, (int) (300 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double angle = Math.random() * 2 * Math.PI + t * 5;
                    double dist = (0.2 + Math.random() * 0.8) * r;
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    double y = -1.0 / (dist + 0.1); // Gravity well effect
                    positions.add(center.add(x, y + 2, z));
                }
                // Event horizon
                for (int i = 0; i < 50; i++) {
                    double angle = 2 * Math.PI * i / 50 + t * 2;
                    positions.add(center.add(Math.cos(angle) * 0.2 * r, 2, Math.sin(angle) * 0.2 * r));
                }
            }

            case QUASAR -> {
                int points = Math.max(1, (int) (300 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double angle = Math.random() * 2 * Math.PI + t;
                    double dist = Math.random() * r;
                    positions.add(
                            center.add(Math.cos(angle) * dist, (Math.random() - 0.5) * 0.1, Math.sin(angle) * dist));
                }
                // Jets
                for (int i = 0; i < 50; i++) {
                    double h = (i / 50.0) * r * 3;
                    double spread = 0.1 * (h / r);
                    positions.add(center.add((Math.random() - 0.5) * spread, h, (Math.random() - 0.5) * spread));
                    positions.add(center.add((Math.random() - 0.5) * spread, -h, (Math.random() - 0.5) * spread));
                }
            }

            case WINGS -> {
                int points = Math.max(1, (int) (200 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double u = (i / (double) points) * Math.PI;
                    double x = Math.sin(u) * Math.cos(u) * r * 1.5;
                    double y = Math.sin(u) * r;
                    double z = -Math.abs(Math.cos(u)) * 0.5 * r;
                    positions.add(center.add(x, y, z));
                    positions.add(center.add(-x, y, z));
                }
            }

            case HALO -> {
                int points = Math.max(1, (int) (100 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points + t;
                    positions.add(center.add(Math.cos(angle) * r, r * 1.2, Math.sin(angle) * r));
                }
            }

            case SHIELD -> {
                int points = Math.max(1, (int) (200 * quality.get()));
                for (int i = 0; i < points; i++) {
                    double phi = Math.acos(2.0 * Math.random() - 1.0);
                    double theta = 2 * Math.PI * Math.random();
                    if (Math.cos(theta) > 0) { // Half sphere in front
                        positions.add(center.add(r * Math.sin(phi) * Math.cos(theta),
                                r * Math.sin(phi) * Math.sin(theta), r * Math.cos(phi)));
                    }
                }
            }

            case GEODESIC_DOME -> {
                double phi = (1 + Math.sqrt(5)) / 2;
                Vec3d[] ico = {
                        new Vec3d(0, 1, phi), new Vec3d(0, -1, phi), new Vec3d(0, 1, -phi), new Vec3d(0, -1, -phi),
                        new Vec3d(1, phi, 0), new Vec3d(-1, phi, 0), new Vec3d(1, -phi, 0), new Vec3d(-1, -phi, 0),
                        new Vec3d(phi, 0, 1), new Vec3d(-phi, 0, 1), new Vec3d(phi, 0, -1), new Vec3d(-phi, 0, -1)
                };
                int[][] edges = {
                        { 0, 1 }, { 0, 4 }, { 0, 5 }, { 0, 8 }, { 0, 9 }, { 1, 6 }, { 1, 7 }, { 1, 8 }, { 1, 9 },
                        { 2, 3 }, { 2, 4 }, { 2, 5 }, { 2, 10 }, { 2, 11 },
                        { 3, 6 }, { 3, 7 }, { 3, 10 }, { 3, 11 }, { 4, 5 }, { 4, 8 }, { 4, 10 }, { 5, 9 }, { 5, 11 },
                        { 6, 7 }, { 6, 8 }, { 6, 10 }, { 7, 9 }, { 7, 11 },
                        { 8, 10 }, { 9, 11 }
                };
                for (int[] e : edges) {
                    Vec3d p1 = ico[e[0]].normalize().multiply(r);
                    Vec3d p2 = ico[e[1]].normalize().multiply(r);
                    if (p1.y >= 0 && p2.y >= 0) {
                        int edgePoints = Math.max(1, (int) (10 * quality.get()));
                        for (int k = 0; k <= edgePoints; k++) {
                            positions.add(center.add(p1.add(p2.subtract(p1).multiply(k / (double) edgePoints))));
                        }
                    }
                }
            }

            case SIERPINSKI_TRIANGLE -> {
                double s = r * 2;
                Vec3d p1 = new Vec3d(0, s, 0);
                Vec3d p2 = new Vec3d(-s, -s, 0);
                Vec3d p3 = new Vec3d(s, -s, 0);
                Vec3d curr = p1;
                for (int i = 0; i < 500 * quality.get(); i++) {
                    int rand = (int) (Math.random() * 3);
                    Vec3d target = rand == 0 ? p1 : (rand == 1 ? p2 : p3);
                    curr = curr.add(target).multiply(0.5);
                    positions.add(center.add(curr));
                }
            }

            case ENNEAGRAM -> {
                double s = r;
                Vec3d[] pts = new Vec3d[9];
                for (int i = 0; i < 9; i++) {
                    double a = 2 * Math.PI * i / 9 - Math.PI / 2;
                    pts[i] = new Vec3d(Math.cos(a) * s, Math.sin(a) * s, 0);
                }
                int[] flow = { 0, 3, 6, 0 };
                int[] flow2 = { 1, 4, 7, 1 };
                int[] flow3 = { 2, 5, 8, 2 };
                int[][] allFlows = { flow, flow2, flow3, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 0 } };
                for (int[] f : allFlows) {
                    for (int i = 0; i < f.length - 1; i++) {
                        Vec3d v1 = pts[f[i]], v2 = pts[f[i + 1]];
                        for (int k = 0; k <= 10 * quality.get(); k++) {
                            positions.add(center.add(v1.add(v2.subtract(v1).multiply(k / (10.0 * quality.get())))));
                        }
                    }
                }
            }

            case DODECAGRAM -> {
                double s = r;
                Vec3d[] pts = new Vec3d[12];
                for (int i = 0; i < 12; i++) {
                    double a = 2 * Math.PI * i / 12;
                    pts[i] = new Vec3d(Math.cos(a) * s, Math.sin(a) * s, 0);
                }
                for (int i = 0; i < 12; i++) {
                    Vec3d v1 = pts[i], v2 = pts[(i + 5) % 12];
                    for (int k = 0; k <= 10 * quality.get(); k++) {
                        positions.add(center.add(v1.add(v2.subtract(v1).multiply(k / (10.0 * quality.get())))));
                    }
                }
            }

            case CROWN -> {
                int points = 50;
                for (int i = 0; i < points; i++) {
                    double a = 2 * Math.PI * i / points + t;
                    double y = r * 0.5 + Math.abs(Math.sin(a * 4)) * r * 0.5;
                    positions.add(center.add(Math.cos(a) * r, y, Math.sin(a) * r));
                    positions.add(center.add(Math.cos(a) * r, r * 0.5, Math.sin(a) * r));
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
        if (CoreCommand.corePositions.isEmpty())
            return;
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

package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import java.util.List;
import java.util.ArrayList;

public class ParticleAuraModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<ParticleStyle> style = sgGeneral.add(new EnumSetting.Builder<ParticleStyle>()
        .name("particle-style")
        .description("The style of the particle aura.")
        .defaultValue(ParticleStyle.NORMAL)
        .build()
    );

    private final Setting<String> particleType = sgGeneral.add(new StringSetting.Builder()
        .name("particle-type")
        .description("The type of particle to use (e.g., flame, dust, etc.).")
        .defaultValue("flame")
        .build()
    );

    public enum ParticleStyle {
        NORMAL,
        SPHERE,
        HALO,
        ORBIT,
        RINGS,
        WINGS,
        CUBE,
        ARROWS,
        BEAM,
        PULSE,
        SPIN,
        SPIRAL,
        VORTEX,
        WHIRL,
        WHIRLWIND,
        TWINS,
        QUADHELIX,
        ICOSPHERE,
        CELEBRATION,
        CHAINS,
        COMPANION,
        POINT,
        POPPER,
        THICK,
        OUTLINE,
        OVERHEAD,
        FEET,
        TRAIL,
        SWORDS,
        TELEPORT,
        DEATH,
        HURT,
        MOVE,
        FISHING,
        BLOCKBREAK,
        BLOCKPLACE,
        BATMAN,
        INVOCATION
    }

    private int index = 0;

    public ParticleAuraModule() {
        super(Blon.Main, "Particle Aura", "Dynamic particle aura using the core.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || CoreCommand.corePositions == null || CoreCommand.corePositions.isEmpty()) return;

        Vec3d center = new Vec3d(mc.player.getX(), mc.player.getY() + 1.5, mc.player.getZ());

        List<Vec3d> positions = getParticlePositions(center, style.get());

        for (int i = 0; i < positions.size(); i++) {
            Vec3d particlePos = positions.get(i);

            // Pick a command block from the core
            BlockPos blockPos = CoreCommand.corePositions.get(index % CoreCommand.corePositions.size());

            // Build the command
            String cmd = String.format(
                "particle minecraft:%s %.3f %.3f %.3f 0 0 0 0.001 1",
                particleType.get(),
                particlePos.x,
                particlePos.y,
                particlePos.z
            );

            // Only the blocks currently being used become REPEATING; others stay REDSTONE
            sendBlock(blockPos, cmd, true);

            index = (index + 1) % CoreCommand.corePositions.size();
        }

        // Optional: reset unused blocks to REDSTONE (if you're reusing blocks)
        for (int i = positions.size(); i < CoreCommand.corePositions.size(); i++) {
            sendBlock(CoreCommand.corePositions.get(i), "", false);
        }
    }

    private List<Vec3d> getParticlePositions(Vec3d center, ParticleStyle style) {
        List<Vec3d> positions = new ArrayList<>();
        switch (style) {
            case NORMAL:
                // Simple circle
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = center.x + 3 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 3 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case SPHERE:
                // Sphere distribution
                int density = 15;
                double radius = 1.5;
                for (int i = 0; i < density; i++) {
                    double u = Math.random();
                    double v = Math.random();
                    double theta = Math.PI * 2 * u;
                    double phi = Math.acos(2.0 * v - 1.0);
                    double dx = radius * Math.sin(phi) * Math.cos(theta);
                    double dy = radius * Math.sin(phi) * Math.sin(theta);
                    double dz = radius * Math.cos(phi);
                    positions.add(center.add(dx, dy, dz));
                }
                break;
            case HALO:
                // Halo above head
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    double x = center.x + 1.5 * Math.cos(angle);
                    double y = center.y + 1.0;
                    double z = center.z + 1.5 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case ORBIT:
                // Orbiting particles
                for (int i = 0; i < 10; i++) {
                    double angle = 2 * Math.PI * i / 10 + (System.currentTimeMillis() % 10000) / 10000.0 * 2 * Math.PI;
                    double x = center.x + 2 * Math.cos(angle);
                    double y = center.y + Math.sin(angle);
                    double z = center.z + 2 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case RINGS:
                // Multiple rings
                for (int ring = 1; ring <= 3; ring++) {
                    double r = ring * 0.8;
                    int points = 8 * ring;
                    for (int i = 0; i < points; i++) {
                        double angle = 2 * Math.PI * i / points;
                        double x = center.x + r * Math.cos(angle);
                        double y = center.y;
                        double z = center.z + r * Math.sin(angle);
                        positions.add(new Vec3d(x, y, z));
                    }
                }
                break;
            case WINGS:
                // Wing-like shape
                for (int i = 0; i < 10; i++) {
                    double x = center.x + (i - 5) * 0.3;
                    double y = center.y + Math.abs(i - 5) * 0.1;
                    double z = center.z;
                    positions.add(new Vec3d(x, y, z));
                    positions.add(new Vec3d(x, y, z + 0.5));
                    positions.add(new Vec3d(x, y, z - 0.5));
                }
                break;
            case CUBE:
                // Cube outline
                double size = 1.0;
                for (int i = 0; i <= 1; i++) {
                    for (int j = 0; j <= 1; j++) {
                        for (int k = 0; k <= 1; k++) {
                            if (i == 0 || i == 1 || j == 0 || j == 1 || k == 0 || k == 1) {
                                double x = center.x + (i * 2 - 1) * size;
                                double y = center.y + (j * 2 - 1) * size;
                                double z = center.z + (k * 2 - 1) * size;
                                positions.add(new Vec3d(x, y, z));
                            }
                        }
                    }
                }
                break;
            case SPIRAL:
                // Spiral
                for (int i = 0; i < 20; i++) {
                    double angle = i * 0.3;
                    double r = i * 0.1;
                    double x = center.x + r * Math.cos(angle);
                    double y = center.y + i * 0.05;
                    double z = center.z + r * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case BEAM:
                // Vertical beam
                for (int i = 0; i < 10; i++) {
                    double y = center.y + (i - 5) * 0.5;
                    positions.add(new Vec3d(center.x, y, center.z));
                }
                break;
            case PULSE:
                // Pulsing circle
                long time = System.currentTimeMillis() % 2000;
                double pulseRadius = 2 + Math.sin(time / 500.0) * 0.5;
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = center.x + pulseRadius * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + pulseRadius * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case SPIN:
                // Spinning particles
                double spinAngle = (System.currentTimeMillis() % 3600) / 10.0;
                for (int i = 0; i < 12; i++) {
                    double angle = 2 * Math.PI * i / 12 + spinAngle;
                    double x = center.x + 2 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 2 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case VORTEX:
                // Vortex
                for (int i = 0; i < 15; i++) {
                    double height = (i - 7) * 0.2;
                    double r = 2 - Math.abs(height) * 0.5;
                    double angle = i * 0.4 + (System.currentTimeMillis() % 5000) / 5000.0 * 2 * Math.PI;
                    double x = center.x + r * Math.cos(angle);
                    double y = center.y + height;
                    double z = center.z + r * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case WHIRL:
                // Whirl
                for (int i = 0; i < 20; i++) {
                    double angle = i * 0.2;
                    double r = 2;
                    double x = center.x + r * Math.cos(angle);
                    double y = center.y + i * 0.1 - 1;
                    double z = center.z + r * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case TWINS:
                // Two orbiting particles
                for (int twin = 0; twin < 2; twin++) {
                    double offset = twin * Math.PI;
                    for (int i = 0; i < 8; i++) {
                        double angle = 2 * Math.PI * i / 8 + offset + (System.currentTimeMillis() % 3000) / 3000.0 * 2 * Math.PI;
                        double x = center.x + 1.5 * Math.cos(angle);
                        double y = center.y;
                        double z = center.z + 1.5 * Math.sin(angle);
                        positions.add(new Vec3d(x, y, z));
                    }
                }
                break;
            case ICOSPHERE:
                // Icosphere approximation with points
                double phi = (1 + Math.sqrt(5)) / 2;
                double[][] icoPoints = {
                    {0, 1, phi}, {0, -1, phi}, {0, 1, -phi}, {0, -1, -phi},
                    {1, phi, 0}, {-1, phi, 0}, {1, -phi, 0}, {-1, -phi, 0},
                    {phi, 0, 1}, {-phi, 0, 1}, {phi, 0, -1}, {-phi, 0, -1}
                };
                for (double[] point : icoPoints) {
                    double scale = 0.5;
                    double x = center.x + point[0] * scale;
                    double y = center.y + point[1] * scale;
                    double z = center.z + point[2] * scale;
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case CELEBRATION:
                // Celebration bursts
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 2;
                    double x = center.x + dist * Math.cos(angle);
                    double y = center.y + Math.random() * 2;
                    double z = center.z + dist * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case ARROWS:
                // Arrow patterns pointing outwards
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * i / 8;
                    double x = center.x + 2 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 2 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                    positions.add(new Vec3d(center.x + 1.5 * Math.cos(angle), center.y + 0.5, center.z + 1.5 * Math.sin(angle)));
                }
                break;
            case WHIRLWIND:
                // Whirlwind effect
                for (int i = 0; i < 25; i++) {
                    double angle = i * 0.25;
                    double r = 2.5 - i * 0.05;
                    double x = center.x + r * Math.cos(angle);
                    double y = center.y + i * 0.1 - 1.25;
                    double z = center.z + r * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case QUADHELIX:
                // Four helical strands
                for (int helix = 0; helix < 4; helix++) {
                    double offset = helix * Math.PI / 2;
                    for (int i = 0; i < 8; i++) {
                        double angle = i * 0.4 + offset;
                        double x = center.x + 1.2 * Math.cos(angle);
                        double y = center.y + i * 0.15 - 0.6;
                        double z = center.z + 1.2 * Math.sin(angle);
                        positions.add(new Vec3d(x, y, z));
                    }
                }
                break;
            case CHAINS:
                // Chain-like links
                for (int i = 0; i < 12; i++) {
                    double angle = 2 * Math.PI * i / 12;
                    double x = center.x + 1.8 * Math.cos(angle);
                    double y = center.y + Math.sin(angle * 3) * 0.3;
                    double z = center.z + 1.8 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case COMPANION:
                // Companion orb
                for (int i = 0; i < 12; i++) {
                    double angle = 2 * Math.PI * i / 12 + (System.currentTimeMillis() % 4000) / 4000.0 * 2 * Math.PI;
                    double x = center.x + 0.8 * Math.cos(angle);
                    double y = center.y + 0.3;
                    double z = center.z + 0.8 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case POINT:
                // Single point at center
                positions.add(center);
                break;
            case POPPER:
                // Popping particles
                long timePop = System.currentTimeMillis() % 1500;
                double popRadius = (timePop < 750) ? timePop / 750.0 * 2 : (1500 - timePop) / 750.0 * 2;
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * i / 8;
                    double x = center.x + popRadius * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + popRadius * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case THICK:
                // Thick ring
                for (int i = 0; i < 24; i++) {
                    double angle = 2 * Math.PI * i / 24;
                    for (int j = 0; j < 3; j++) {
                        double r = 1.5 + j * 0.3;
                        double x = center.x + r * Math.cos(angle);
                        double y = center.y;
                        double z = center.z + r * Math.sin(angle);
                        positions.add(new Vec3d(x, y, z));
                    }
                }
                break;
            case OUTLINE:
                // Outline of a person
                // Simple approximation with points
                positions.add(new Vec3d(center.x, center.y + 1.8, center.z)); // Head top
                positions.add(new Vec3d(center.x, center.y + 1.3, center.z)); // Head bottom
                positions.add(new Vec3d(center.x, center.y + 0.9, center.z)); // Torso
                positions.add(new Vec3d(center.x, center.y + 0.2, center.z)); // Legs
                positions.add(new Vec3d(center.x + 0.3, center.y + 1.0, center.z)); // Arms
                positions.add(new Vec3d(center.x - 0.3, center.y + 1.0, center.z));
                break;
            case OVERHEAD:
                // Overhead particles
                for (int i = 0; i < 12; i++) {
                    double angle = 2 * Math.PI * i / 12;
                    double x = center.x + 1.2 * Math.cos(angle);
                    double y = center.y + 2.0;
                    double z = center.z + 1.2 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case FEET:
                // At feet level
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = center.x + 0.8 * Math.cos(angle);
                    double y = center.y - 1.0;
                    double z = center.z + 0.8 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case TRAIL:
                // Trail behind
                for (int i = 0; i < 10; i++) {
                    double x = center.x - i * 0.3;
                    double y = center.y;
                    double z = center.z;
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case SWORDS:
                // Sword-like crosses
                for (int i = 0; i < 4; i++) {
                    double angle = 2 * Math.PI * i / 4;
                    double x = center.x + 1.5 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 1.5 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                    positions.add(new Vec3d(x, y + 0.5, z));
                    positions.add(new Vec3d(x, y - 0.5, z));
                }
                break;
            case TELEPORT:
                // Teleport effect
                long timeTel = System.currentTimeMillis() % 2000;
                double telY = center.y + (timeTel / 2000.0) * 3;
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * i / 8;
                    double x = center.x + Math.cos(angle);
                    double z = center.z + Math.sin(angle);
                    positions.add(new Vec3d(x, telY, z));
                }
                break;
            case DEATH:
                // Death particles spreading
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 3;
                    double x = center.x + dist * Math.cos(angle);
                    double y = center.y + Math.random() * 2;
                    double z = center.z + dist * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case HURT:
                // Hurt effect
                for (int i = 0; i < 10; i++) {
                    double x = center.x + (Math.random() - 0.5) * 2;
                    double y = center.y + Math.random() * 1.5;
                    double z = center.z + (Math.random() - 0.5) * 2;
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case MOVE:
                // Movement trail
                for (int i = 0; i < 8; i++) {
                    double x = center.x - i * 0.2;
                    double y = center.y;
                    double z = center.z;
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case FISHING:
                // Fishing line effect
                for (int i = 0; i < 10; i++) {
                    double x = center.x;
                    double y = center.y - i * 0.2;
                    double z = center.z;
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case BLOCKBREAK:
                // Block breaking particles
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 6; k++) {
                            double x = center.x + (i - 3) * 0.1;
                            double y = center.y + (j - 3) * 0.1;
                            double z = center.z + (k - 3) * 0.1;
                            positions.add(new Vec3d(x, y, z));
                        }
                    }
                }
                break;
            case BLOCKPLACE:
                // Block placement effect
                for (int i = 0; i < 4; i++) {
                    double x = center.x + (i % 2 == 0 ? 0.5 : -0.5);
                    double y = center.y;
                    double z = center.z + (i / 2 == 0 ? 0.5 : -0.5);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case BATMAN:
                // Batman logo approximation
                positions.add(new Vec3d(center.x, center.y + 0.5, center.z));
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI * i / 3;
                    double x = center.x + 0.8 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 0.8 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
            case INVOCATION:
                // Invocation circle
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    double x = center.x + 2 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 2 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                // Add some vertical elements
                for (int i = 0; i < 5; i++) {
                    positions.add(new Vec3d(center.x, center.y + i * 0.4, center.z));
                }
                break;
            default:
                // Default to normal
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = center.x + 3 * Math.cos(angle);
                    double y = center.y;
                    double z = center.z + 3 * Math.sin(angle);
                    positions.add(new Vec3d(x, y, z));
                }
                break;
        }
        return positions;
    }

    private void sendBlock(BlockPos pos, String command, boolean active) {
        mc.player.networkHandler.sendPacket(
            new net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket(
                pos,
                command,
                active ? net.minecraft.block.entity.CommandBlockBlockEntity.Type.AUTO
                       : net.minecraft.block.entity.CommandBlockBlockEntity.Type.REDSTONE,
                true,
                false,
                true
            )
        );
    }
}

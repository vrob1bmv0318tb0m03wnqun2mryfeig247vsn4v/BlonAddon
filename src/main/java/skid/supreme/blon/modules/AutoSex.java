package skid.supreme.blon.modules;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
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

public class AutoSex extends Module {

    private final SettingGroup sgPlacement = settings.createGroup("Placement");

    private final Setting<PlacementMode> placementMode = sgPlacement.add(
        new EnumSetting.Builder<PlacementMode>()
            .name("placement-mode")
            .description("How to position the dildo")
            .defaultValue(PlacementMode.Relative)
            .build()
    );

    private final Setting<BlockPos> placementOffset = sgPlacement.add(
        new BlockPosSetting.Builder()
            .name("placement-offset")
            .description("Offset from base placement position")
            .defaultValue(BlockPos.ORIGIN)
            .build()
    );

    private final SettingGroup sgTeleport = settings.createGroup("Teleport");

    private final Setting<String> playerName = sgTeleport.add(
        new StringSetting.Builder()
            .name("player-name")
            .description("Name of the player to teleport")
            .defaultValue("")
            .build()
    );

    private final Setting<Double> teleportOffsetX = sgTeleport.add(
        new DoubleSetting.Builder()
            .name("teleport-offset-x")
            .description("X offset from block display position for player teleport")
            .defaultValue(0.976)
            .build()
    );

    private final Setting<Double> teleportOffsetY = sgTeleport.add(
        new DoubleSetting.Builder()
            .name("teleport-offset-y")
            .description("Y offset from block display position for player teleport")
            .defaultValue(-3.0)
            .build()
    );

    private final Setting<Double> teleportOffsetZ = sgTeleport.add(
        new DoubleSetting.Builder()
            .name("teleport-offset-z")
            .description("Z offset from block display position for player teleport")
            .defaultValue(0.497)
            .build()
    );

    private final Setting<Double> teleportSpeed = sgTeleport.add(
        new DoubleSetting.Builder()
            .name("teleport-speed")
            .description("Speed of the up/down teleportation movement")
            .defaultValue(1.0)
            .min(0.1)
            .max(10.0)
            .build()
    );

    private final List<String> baseBlockDisplays = new ArrayList<>();
    private final List<String> spawnCommands = new ArrayList<>();
    private final List<String> teleportCommands = new ArrayList<>();
    private Vec3d baseTeleportPos;
    private int teleportDelayTicks = 0;
    private boolean waitingForTeleportDelay = false;

    public AutoSex() {
        super(
            Blon.Main,
            "autosex",
            "Spawns block displays using the command block core"
        );
        loadBlockDisplays();
    }

    @Override
    public void onActivate() {
        if (CoreCommand.corePositions.isEmpty()) {
            error("No command block core found. Use .core first.");
            toggle();
            return;
        }

        generateSpawnCommands();

        if (spawnCommands.isEmpty()) {
            error("No block displays loaded.");
            toggle();
            return;
        }

        CoreUpdater.startSingle(
            CoreCommand.corePositions,
            spawnCommands,
            false,
            true,
            10
        );

        BlockPos basePos = getPlacementPosition();
        baseTeleportPos = new Vec3d(
            basePos.getX() - teleportOffsetX.get(),
            basePos.getY() - teleportOffsetY.get(),
            basePos.getZ() - teleportOffsetZ.get()
        );

        if (!playerName.get().isEmpty()) {
            teleportDelayTicks = 40; // Wait at least 40 ticks after block displays finish
            waitingForTeleportDelay = true;
        }

        info("Started spawning " + spawnCommands.size() + " block displays.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        CoreUpdater.onTick();

        // Handle delayed teleport command via command blocks
        if (waitingForTeleportDelay && !CoreUpdater.isSingleRunning()) {
            if (teleportDelayTicks > 0) {
                teleportDelayTicks--;
            } else {
                // Start teleport command via command blocks
                teleportCommands.clear();
                String teleportCmd = String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f",
                    playerName.get(), baseTeleportPos.getX(), baseTeleportPos.getY(), baseTeleportPos.getZ());
                teleportCommands.add(teleportCmd);

                CoreUpdater.startSingle(
                    CoreCommand.corePositions,
                    teleportCommands,
                    false,
                    true,
                    10
                );

                waitingForTeleportDelay = false;
                info("Started teleport command via command blocks.");
            }
        }

        // Continuous teleportation movement using repeating command block
        if (baseTeleportPos != null && !playerName.get().isEmpty() && !waitingForTeleportDelay) {
            double time = System.currentTimeMillis() * teleportSpeed.get() * 0.001;
            double yOffset = Math.sin(time) * 1.0;

            Vec3d newPos = new Vec3d(
                baseTeleportPos.getX(),
                baseTeleportPos.getY() + yOffset,
                baseTeleportPos.getZ()
            );
            String command = String.format(Locale.ROOT, "tp %s %.3f %.3f %.3f",
                playerName.get(), newPos.getX(), newPos.getY(), newPos.getZ());
            CoreUpdater.sendCommandToBlock(CoreCommand.corePositions.get(0), command);
        }
    }

    private void generateSpawnCommands() {
        spawnCommands.clear();
        BlockPos basePos = getPlacementPosition();

        for (String cmd : baseBlockDisplays) {
            spawnCommands.add(applyPlacement(cmd, basePos));
        }
    }

    private String applyPlacement(String baseCommand, BlockPos basePos) {
        try {
            String[] parts = baseCommand.split(" ");
            if (parts.length < 5) return baseCommand;

            double x = basePos.getX() + parseCoord(parts[2]);
            double y = basePos.getY() + parseCoord(parts[3]);
            double z = basePos.getZ() + parseCoord(parts[4]);

            int nbtStart = baseCommand.indexOf('{');
            if (nbtStart == -1) return baseCommand;

            String nbt = baseCommand.substring(nbtStart);

            return String.format(
                Locale.ROOT,
                "summon minecraft:block_display %f %f %f %s",
                x, y, z, nbt
            );
        } catch (NumberFormatException e) {
            error("Invalid coordinate in block display command: " + baseCommand);
            return baseCommand;
        }
    }

    private double parseCoord(String coord) {
        if (coord.startsWith("~")) {
            return coord.length() == 1 ? 0.0 : Double.parseDouble(coord.substring(1));
        }
        return Double.parseDouble(coord);
    }

    private void loadBlockDisplays() {
        baseBlockDisplays.clear();

        try {
            InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream("blockdisplay.txt");

            if (stream == null) {
                error("blockdisplay.txt not found in resources.");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .forEach(baseBlockDisplays::add);

            info("Loaded " + baseBlockDisplays.size() + " block displays from resources.");

        } catch (Exception e) {
            error("Failed to load blockdisplay.txt: " + e.getMessage());
            info("Something broke talk to me in discord");
        }
    }

    private BlockPos getPlacementPosition() {
        if (mc.player == null) return BlockPos.ORIGIN;

        BlockPos base = placementMode.get() == PlacementMode.Relative
            ? mc.player.getBlockPos()
            : BlockPos.ORIGIN;

        return base.add(placementOffset.get());
    }



    public enum PlacementMode {
        Relative,
        Absolute
    }
}

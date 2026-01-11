package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;

import java.util.*;

public class PlayerRotatorModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("Maximum number of players to rotate.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .build());

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
            .name("radius")
            .description("Radius of the rotation.")
            .defaultValue(5.0)
            .min(1.0)
            .sliderMax(20.0)
            .build());

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Rotation speed.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderMax(5.0)
            .build());

    private final UUID[] slots = new UUID[20]; // Fixed size to avoid dynamic reallocation jitter
    private double angle = 0;

    public PlayerRotatorModule() {
        super(Blon.Main, "player-rotator", "Rotates players around you using the Core.");
    }

    @Override
    public void onActivate() {
        if (CoreCommand.corePositions.isEmpty()) {
            error("No Core found! Run .core first.");
            toggle();
            return;
        }
        angle = 0;
        Arrays.fill(slots, null);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 1. Cleanup dead/missing/too far players
        for (int i = 0; i < slots.length; i++) {
            UUID uuid = slots[i];
            if (uuid == null)
                continue;
            PlayerEntity p = mc.world.getPlayerByUuid(uuid);
            if (p == null || !p.isAlive() || p.distanceTo(mc.player) > 30) {
                slots[i] = null;
            }
        }

        // 2. Fill slots if space available
        List<UUID> activeInSlots = Arrays.stream(slots).filter(Objects::nonNull).toList();
        int currentCount = activeInSlots.size();

        if (currentCount < amount.get()) {
            List<? extends PlayerEntity> candidates = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player && p.isAlive() && !activeInSlots.contains(p.getUuid()))
                    .sorted(Comparator.comparingDouble(p -> p.distanceTo(mc.player)))
                    .toList();

            for (PlayerEntity p : candidates) {
                if (currentCount >= amount.get())
                    break;
                // Find first empty slot
                for (int i = 0; i < amount.get(); i++) {
                    if (slots[i] == null) {
                        slots[i] = p.getUuid();
                        currentCount++;
                        break;
                    }
                }
            }
        }

        // 3. Trim if amount decreased (clear slots beyond amount)
        for (int i = amount.get(); i < slots.length; i++) {
            slots[i] = null;
        }

        angle += speed.get() * 0.1;
        if (angle > Math.PI * 2)
            angle -= Math.PI * 2;

        List<BlockPos> cores = CoreCommand.corePositions;
        List<String> commands = new ArrayList<>();

        for (int i = 0; i < amount.get(); i++) {
            if (slots[i] == null)
                continue;
            if (commands.size() >= cores.size())
                break;

            PlayerEntity target = mc.world.getPlayerByUuid(slots[i]);
            if (target == null)
                continue;

            // Distribute based on slots for stability
            double playerAngle = angle + (i * (Math.PI * 2 / amount.get()));
            double yaw = Math.toDegrees(playerAngle);

            // Use caretaker notation for server-side relative movement
            // execute at <Player> rotated <Yaw> 0 run tp <Target> ^ ^ ^<Radius>
            String cmd = String.format(Locale.US, "execute at %s rotated %.2f 0 run tp %s ^ ^ ^%.2f",
                    mc.player.getName().getString(), yaw, target.getUuidAsString(), radius.get());

            commands.add(cmd);
        }

        if (commands.isEmpty())
            return;

        // Send packets to core
        List<BlockPos> positionsToUse = cores.subList(0, Math.min(cores.size(), commands.size()));
        CoreUpdater.startAuto(positionsToUse, commands, false, false, 50);
        CoreUpdater.onTick();
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null || CoreCommand.corePositions.isEmpty())
            return;

        // Clear the commands in the cores we were using
        List<String> emptyCommands = new ArrayList<>();
        for (int i = 0; i < amount.get(); i++) {
            emptyCommands.add("");
        }

        List<BlockPos> positionsToClear = CoreCommand.corePositions.subList(0,
                Math.min(CoreCommand.corePositions.size(), emptyCommands.size()));
        CoreUpdater.startAuto(positionsToClear, emptyCommands, false, false, 50);
        CoreUpdater.onTick();
        Arrays.fill(slots, null);
    }
}

package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import skid.supreme.blon.Blon;

public class TotemBypass extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");

    private final Setting<WeaponType> weapon = sgGeneral.add(new EnumSetting.Builder<WeaponType>()
            .name("weapon")
            .description("Weapon to use for the exploit.")
            .defaultValue(WeaponType.Mace)
            .build());

    private final Setting<ServerType> serverType = sgGeneral.add(new EnumSetting.Builder<ServerType>()
            .name("server-type")
            .description("Server type for positioning calculations.")
            .defaultValue(ServerType.Paper)
            .build());

    private final Setting<Integer> fallHeight = sgGeneral.add(new IntSetting.Builder()
            .name("fall-height")
            .description("Simulated fall height for mace power.")
            .defaultValue(23)
            .min(1)
            .sliderRange(1, 1000)
            .build());

    private final Setting<Integer> minHeight = sgGeneral.add(new IntSetting.Builder()
            .name("min-height")
            .description("Minimum height for Paper mode calculations.")
            .defaultValue(23)
            .min(1)
            .sliderRange(1, 1000)
            .build());

    private final Setting<Integer> maxHeight = sgGeneral.add(new IntSetting.Builder()
            .name("max-height")
            .description("Maximum height for Paper mode calculations.")
            .defaultValue(40)
            .min(1)
            .sliderRange(1, 1000)
            .build());

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
            .name("delay-ticks")
            .description("Delay between position packets in ticks.")
            .defaultValue(4)
            .min(1)
            .sliderRange(1, 10)
            .build());

    private final Setting<Boolean> checkObstacles = sgGeneral.add(new BoolSetting.Builder()
            .name("check-obstacles")
            .description("Check for obstacles before teleporting.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> attackCount = sgGeneral.add(new IntSetting.Builder()
            .name("attack-count")
            .description("Number of attacks to perform per interaction.")
            .defaultValue(1)
            .min(0)
            .sliderRange(0, 50)
            .build());

    private final Setting<Boolean> autoSwap = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-swap-back")
            .description("Automatically swap back to previous item.")
            .defaultValue(false)
            .build());

    private final Setting<Integer> extraHeightsCount = sgAdvanced.add(new IntSetting.Builder()
            .name("extra-heights-count")
            .description("Number of extra height values to use (0-40).")
            .defaultValue(0)
            .min(0)
            .max(40)
            .sliderRange(0, 40)
            .build());

    private final Setting<Integer>[] extraHeights = new Setting[40];

    private LivingEntity targetEntity;
    private boolean attacking;
    private boolean swapping;
    private int swapSlot = -1;
    private int tickCounter;

    public TotemBypass() {
        super(Blon.Main, "TotemBypass", "");

        for (int i = 0; i < 40; i++) {
            final int index = i;
            extraHeights[i] = sgAdvanced.add(new IntSetting.Builder()
                    .name("extra-height-" + (i + 1))
                    .description("Extra height value " + (i + 1))
                    .defaultValue(60 + (i + 1) * 15)
                    .min(1)
                    .sliderRange(1, 1000)
                    .visible(() -> extraHeightsCount.get() > index)
                    .build());
        }
    }

    @Override
    public void onActivate() {
        targetEntity = null;
        attacking = false;
        swapping = false;
        swapSlot = -1;
        tickCounter = 0;
    }

    @Override
    public void onDeactivate() {
        if (swapping && swapSlot != -1) {
            InvUtils.swap(swapSlot, false);
            swapping = false;
            swapSlot = -1;
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || attacking) return;

        if (!(event.packet instanceof IPlayerInteractEntityC2SPacket packet)) return;
        if (packet.meteor$getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        if (!(packet.meteor$getEntity() instanceof LivingEntity entity)) return;
        if (entity == mc.player) return;

        if (mc.player.getMainHandStack().getItem() != Items.MACE) return;

        targetEntity = entity;
        attacking = true;

        if (autoSwap.get()) {
            swapSlot = mc.player.getInventory().selectedSlot;
        }

        int attacks = attackCount.get();
        if (attacks == 0) {
            java.util.List<Integer> heights = getHeightsList();
            for (int height : heights) {
                performAttack(entity, height);
            }
        } else {
            for (int i = 0; i < attacks; i++) {
                performAttack(entity, fallHeight.get());
            }
        }

        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));

        attacking = false;
        targetEntity = null;
    }

    private void performAttack(LivingEntity entity, int height) {
        switch (serverType.get()) {
            case Paper -> performPaperAttack(entity, height);
            case Vanilla -> performVanillaAttack(entity, height);
        }

        // Removed Thread.sleep to prevent freezing the main game thread
    }

    private void performPaperAttack(LivingEntity entity, int height) {
        if (mc.player == null) return;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double targetY = playerPos.getY() + height;

        if (checkObstacles.get()) {
            BlockPos checkPos1 = BlockPos.ofFloored(playerPos.getX(), targetY, playerPos.getZ());
            BlockPos checkPos2 = checkPos1.up();
            if (!isAir(checkPos1) || !isAir(checkPos2)) return;
        }

        PlayerMoveC2SPacket movePacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                playerPos.getX(), targetY, playerPos.getZ(), false, mc.player.horizontalCollision);
        PlayerMoveC2SPacket returnPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                playerPos.getX(), playerPos.getY(), playerPos.getZ(), false, mc.player.horizontalCollision);

        ((IPlayerMoveC2SPacket) movePacket).meteor$setTag(1337);
        ((IPlayerMoveC2SPacket) returnPacket).meteor$setTag(1337);

        mc.player.networkHandler.sendPacket(movePacket);
        mc.player.networkHandler.sendPacket(returnPacket);

        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
    }

    private void performVanillaAttack(LivingEntity entity, int height) {
        if (mc.player == null) return;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(playerPos.getX(), playerPos.getY() + height, playerPos.getZ());
        Vec3d returnPos = playerPos;

        PlayerMoveC2SPacket moveUp = new PlayerMoveC2SPacket.PositionAndOnGround(
                targetPos.getX(), targetPos.getY(), targetPos.getZ(), false, mc.player.horizontalCollision);
        PlayerMoveC2SPacket moveDown = new PlayerMoveC2SPacket.PositionAndOnGround(
                returnPos.getX(), returnPos.getY(), returnPos.getZ(), false, mc.player.horizontalCollision);

        ((IPlayerMoveC2SPacket) moveUp).meteor$setTag(1337);
        ((IPlayerMoveC2SPacket) moveDown).meteor$setTag(1337);

        mc.player.networkHandler.sendPacket(moveUp);
        mc.player.networkHandler.sendPacket(moveDown);

        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, false));
    }

    private java.util.List<Integer> getHeightsList() {
        java.util.List<Integer> heights = new java.util.ArrayList<>();
        heights.add(minHeight.get());
        heights.add(maxHeight.get());

        int extraCount = Math.min(extraHeightsCount.get(), 40);
        for (int i = 0; i < extraCount; i++) {
            heights.add(extraHeights[i].get());
        }

        return heights;
    }

    private boolean isAir(BlockPos pos) {
        return mc.world != null && mc.world.getBlockState(pos).isAir();
    }

    private enum WeaponType {
        Mace
    }

    private enum ServerType {
        Paper,
        Vanilla
    }
}

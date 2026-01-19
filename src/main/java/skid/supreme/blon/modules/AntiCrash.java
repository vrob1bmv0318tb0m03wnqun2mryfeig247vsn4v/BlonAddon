package skid.supreme.blon.modules;

import skid.supreme.blon.Blon;
import skid.supreme.blon.VFih;
import skid.supreme.blon.mixin.ChatMessageS2CPacketAccessor;
import skid.supreme.blon.mixin.DeathMessageS2CPacketAccessor;
import skid.supreme.blon.mixin.TitleS2CPacketAccessor;
import skid.supreme.blon.mixin.BossBarS2CPacketAccessor;
import skid.supreme.blon.mixin.EntitySpawnS2CPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;

import java.util.stream.StreamSupport;

public class AntiCrash extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> textLimiter = sgGeneral.add(new BoolSetting.Builder().name("text-limiter").description("Limits the size of text.").defaultValue(true).build());
    public final Setting<Boolean> translationAntiCrash = sgGeneral.add(new BoolSetting.Builder().name("translation-anti-crash").description("Prevents translation crashes from working.").defaultValue(true).build());
    public final Setting<Boolean> particleLimiter = sgGeneral.add(new BoolSetting.Builder().name("particle-limiter").description("Limits the attributes and number of particles.").defaultValue(true).build());
    public final Setting<Boolean> entityLimiter = sgGeneral.add(new BoolSetting.Builder().name("entity-limiter").description("Limits the number of entities.").defaultValue(true).build());
    public final Setting<Boolean> renderingQuality = sgGeneral.add(new BoolSetting.Builder().name("rendering-quality").description("Toggles the rendering of different things.").defaultValue(true).build());
    public final Setting<Boolean> rateLimits = sgGeneral.add(new BoolSetting.Builder().name("rate-limits").description("Rate limits of different things.").defaultValue(true).build());
    public final Setting<Boolean> fpsChecker = sgGeneral.add(new BoolSetting.Builder().name("fps-checker").description("Performs an action when the fps goes below a certain point.").defaultValue(true).build());
    public final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder().name("notifications").description("Shows notifications when protections are triggered.").defaultValue(false).build());

    private final SettingGroup sgTextLimiter = settings.createGroup("Text Limiter");
    public final Setting<Integer> itemName = sgTextLimiter.add(new IntSetting.Builder().name("item-name").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> itemTooltip = sgTextLimiter.add(new IntSetting.Builder().name("item-tooltip").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> chat = sgTextLimiter.add(new IntSetting.Builder().name("chat").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> bossbar = sgTextLimiter.add(new IntSetting.Builder().name("bossbar").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> entityName = sgTextLimiter.add(new IntSetting.Builder().name("entity-name").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> title = sgTextLimiter.add(new IntSetting.Builder().name("title").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> textDisplay = sgTextLimiter.add(new IntSetting.Builder().name("text-display").min(0).sliderRange(0, 100000).defaultValue(1024).build());
    public final Setting<Integer> general = sgTextLimiter.add(new IntSetting.Builder().name("general").min(100).sliderRange(0, 100000).defaultValue(1024).build());

    private final SettingGroup sgTranslationAntiCrash = settings.createGroup("Translation Anti Crash");
    public final Setting<Boolean> defaultArgumentRegex = sgTranslationAntiCrash.add(new BoolSetting.Builder().name("use-default-argument-regex").defaultValue(true).build());
    public final Setting<String> argumentRegex = sgTranslationAntiCrash.add(new StringSetting.Builder().name("argument-regex").visible(() -> !defaultArgumentRegex.get()).defaultValue("%(?:(\\d+)\\$)?([A-Za-z%]|$)").build());
    public final Setting<Boolean> smartMode = sgTranslationAntiCrash.add(new BoolSetting.Builder().name("smart-mode").description("Replace all translations with %1$s").defaultValue(true).build());
    public final Setting<Integer> maxTranslations = sgTranslationAntiCrash.add(new IntSetting.Builder().name("max-translations").min(1).sliderRange(1, 100000).defaultValue(1).build());

    private final SettingGroup sgParticleLimiter = settings.createGroup("Particle Limiter");
    private final Setting<Integer> maxSpeed = sgParticleLimiter.add(new IntSetting.Builder().name("max-particle-speed").min(0).sliderRange(0, 100000).defaultValue(1000).build());
    private final Setting<Integer> maxParticlesPerTick = sgParticleLimiter.add(new IntSetting.Builder().name("max-particles-per-tick").min(0).sliderRange(0, 100000).defaultValue(10000).build());

    private final SettingGroup sgEntityLimiter = settings.createGroup("Entity Limiter");
    private final Setting<Integer> maxEntities = sgEntityLimiter.add(new IntSetting.Builder().name("max-entities").min(0).defaultValue(1024).sliderRange(0, 100000).build());

    private final SettingGroup sgRenderingQuality = settings.createGroup("Rendering Quality");
    public final Setting<Boolean> renderLightBeam = sgRenderingQuality.add(new BoolSetting.Builder().name("render-light-beam").defaultValue(true).build());
    public final Setting<Boolean> renderStructureBlockOverlay = sgRenderingQuality.add(new BoolSetting.Builder().name("render-structure-block-overlay").defaultValue(true).build());
    public final Setting<Boolean> renderShadows = sgRenderingQuality.add(new BoolSetting.Builder().name("render-shadows").defaultValue(true).build());
    public final Setting<Integer> maxEntitySize = sgRenderingQuality.add(new IntSetting.Builder().name("max-entity-size").min(0).defaultValue(1024).sliderRange(0, 100000).build());

    private final SettingGroup sgRateLimits = settings.createGroup("Rate Limits");
    private final Setting<Integer> maxSoundsPerTick = sgRateLimits.add(new IntSetting.Builder().name("max-sounds-per-tick").min(1).sliderRange(1, 100000).defaultValue(1).build());
    private final Setting<Integer> maxChatsPerTick = sgRateLimits.add(new IntSetting.Builder().name("max-chats-per-tick").min(1).sliderRange(1, 100000).defaultValue(1).build());

    private final SettingGroup sgBufferLimiter = settings.createGroup("Buffer Limiter");
    public final Setting<Boolean> bufferLimiter = sgBufferLimiter.add(new BoolSetting.Builder().name("buffer-limiter").description("Limits the number of vertices in BufferBuilder to prevent crashes.").defaultValue(true).build());
    public final Setting<Integer> maxBufferVertices = sgBufferLimiter.add(new IntSetting.Builder().name("max-buffer-vertices").min(1000).sliderRange(1000, 16000000).defaultValue(8000000).build());

    private final SettingGroup sgFpsChecker = settings.createGroup("FPS Checker");
    private final Setting<Integer> kickLimit = sgFpsChecker.add(new IntSetting.Builder().name("kick-limit").min(1).sliderRange(1, 100000).defaultValue(100).build());
    private final Setting<Integer> commandLimit = sgFpsChecker.add(new IntSetting.Builder().name("command-limit").min(1).sliderRange(1, 100000).defaultValue(50).build());
    private final Setting<String> command = sgFpsChecker.add(new StringSetting.Builder().name("command").defaultValue("/kill @e[type=!player]").build());

    private int sounds = 0;
    private int chats = 0;
    private int lowFpsTicks = 0;
    private int lastFps = 60;
    // if you want hex_cod's original project it is https://github.com/1peroxide/skid-from-me-blon
    public AntiCrash() {
        super(Blon.Main, "anti-crash", "CREDITS: Hex_Cod");
        VFih.mAntiCrash = this;
    }

    private void notify(String message) {
        if (notifications.get()) {
            ChatUtils.info("AntiCrash", message);
        }
    }

    @EventHandler
    private void onRender(Render2DEvent event) {

        int currentFps = mc.getCurrentFps();
        if (currentFps < 5) { 

            lowFpsTicks++;
        } else {
            lowFpsTicks = Math.max(0, lowFpsTicks - 1); 

        }
        lastFps = currentFps;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        VFih.particlesPerTick = 0;
        sounds = 0;
        chats = 0;

        if (fpsChecker.get()) {
            if (lowFpsTicks > kickLimit.get()) mc.getNetworkHandler().getConnection().disconnect(Text.of("AntiCrash prevented crash."));
            if (lowFpsTicks > commandLimit.get() && !command.get().isEmpty()) ChatUtils.sendPlayerMsg(command.get());
        }

        if (entityLimiter.get()) {
            Entity[] entities = StreamSupport.stream(mc.world.getEntities().spliterator(), false).toArray(Entity[]::new);

            int i = 0;
            for (Entity entity : entities) {

                boolean invalid = false;

                if (Double.isNaN(entity.getX()) || Double.isInfinite(entity.getX()) ||
                    Double.isNaN(entity.getY()) || Double.isInfinite(entity.getY()) ||
                    Double.isNaN(entity.getZ()) || Double.isInfinite(entity.getZ())) {
                    invalid = true;
                }

                if (Double.isNaN(entity.getVelocity().x) || Double.isInfinite(entity.getVelocity().x) ||
                    Double.isNaN(entity.getVelocity().y) || Double.isInfinite(entity.getVelocity().y) ||
                    Double.isNaN(entity.getVelocity().z) || Double.isInfinite(entity.getVelocity().z)) {
                    invalid = true;
                }

                if (Float.isNaN(entity.getYaw()) || Float.isNaN(entity.getPitch())) {
                    invalid = true;
                }

                var box = entity.getBoundingBox();
                if (box != null) {
                    double lengthX = box.getLengthX();
                    double lengthY = box.getLengthY();
                    double lengthZ = box.getLengthZ();

                    if (lengthX < 0 || lengthY < 0 || lengthZ < 0 ||
                        lengthX > 1000 || lengthY > 1000 || lengthZ > 1000) {
                        invalid = true;
                    }

                    if (Double.isNaN(lengthX) || Double.isNaN(lengthY) || Double.isNaN(lengthZ)) {
                        invalid = true;
                    }
                }

                if (invalid || i > maxEntities.get()) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
                i++;
            }
        }

    }

    @Override
    public void onActivate() {
        VFih.antiCrash = true;
    }

    @Override
    public void onDeactivate() {
        VFih.antiCrash = false;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (VFih.useTextLimiter()) {
            if (event.packet instanceof ChatMessageS2CPacket chat) {
                if (chat.unsignedContent().getString().length() > VFih.mAntiCrash.chat.get()) ((ChatMessageS2CPacketAccessor)(Object)chat).setUnsignedContent(VFih.textLengthError(VFih.mAntiCrash.chat.get()));
            } else if (event.packet instanceof DeathMessageS2CPacket death) {
                if (death.message().getString().length() > VFih.mAntiCrash.chat.get()) ((DeathMessageS2CPacketAccessor)(Object)death).setMessage(VFih.textLengthError(VFih.mAntiCrash.chat.get()));
            } else if (event.packet instanceof TitleS2CPacket title) {
                if (title.text().getString().length() > VFih.mAntiCrash.title.get()) ((TitleS2CPacketAccessor)(Object)title).setText(VFih.textLengthError(VFih.mAntiCrash.title.get()));
            } else if (event.packet instanceof BossBarS2CPacket bossBar) {
                Text bossName = ((BossBarS2CPacketAccessor) bossBar).getName();
                if (bossName != null && bossName.getString().length() > VFih.mAntiCrash.bossbar.get()) {
                    ((BossBarS2CPacketAccessor) bossBar).setName(VFih.textLengthError(VFih.mAntiCrash.bossbar.get()));
                }
            } else if (event.packet instanceof EntitySpawnS2CPacket entitySpawn) {
                Text customName = ((EntitySpawnS2CPacketAccessor) entitySpawn).getCustomName();
                if (customName != null && customName.getString().length() > VFih.mAntiCrash.entityName.get()) {
                    ((EntitySpawnS2CPacketAccessor) entitySpawn).setCustomName(VFih.textLengthError(VFih.mAntiCrash.entityName.get()));
                }
            }
        }
        if (particleLimiter.get()) {
            if (event.packet instanceof ParticleS2CPacket particle) {
                if (VFih.particlesPerTick < maxParticlesPerTick.get()) {
                    if (particle.getSpeed() > maxSpeed.get()) event.cancel();
                    VFih.particlesPerTick += particle.getCount();
                } else event.cancel();
            }
        }
        if (rateLimits.get()) {
            if (event.packet instanceof ChatMessageS2CPacket || event.packet instanceof DeathMessageS2CPacket || event.packet instanceof GameMessageS2CPacket) {
                if (chats > maxChatsPerTick.get()) event.cancel();
                chats++;
            }
            if (event.packet instanceof StopSoundS2CPacket || event.packet instanceof PlaySoundS2CPacket || event.packet instanceof PlaySoundFromEntityS2CPacket) {
                if (rateLimits.get() && sounds > maxSoundsPerTick.get()) event.cancel();
                sounds++;
            }
        }
    }
}

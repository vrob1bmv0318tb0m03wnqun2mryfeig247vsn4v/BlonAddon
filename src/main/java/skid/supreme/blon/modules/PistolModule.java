package skid.supreme.blon.modules;


import org.lwjgl.glfw.GLFW;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.projectile.ProjectileUtil;
import skid.supreme.blon.Blon;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.core.CoreUpdater;


public class PistolModule extends Module {
   private final SettingGroup sgGeneral = settings.getDefaultGroup();


   private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
           .name("range")
           .description("Maximum distance the pistol can shoot.")
           .defaultValue(50.0)
           .min(5.0)
           .sliderMax(100.0)
           .build());


   private final Setting<String> particleType = sgGeneral.add(new StringSetting.Builder()
           .name("particle")
           .description("Particle to spawn on shot.")
           .defaultValue("explosion")
           .build());


   private final Setting<Integer> maxPacketsPerTick = sgGeneral.add(new IntSetting.Builder()
           .name("max-packets-per-tick")
           .description("Maximum number of C2S packets sent per tick")
           .defaultValue(10)
           .min(1)
           .max(50000)
           .sliderMin(1)
           .sliderMax(5000)
           .build());


   private final Setting<Boolean> gunModel = sgGeneral.add(new BoolSetting.Builder()
           .name("gun-model")
           .description("Render the custom pistol model.")
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
           .description("Y offset for the gun model.")
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


   private final Setting<String> killSound = sgGeneral.add(new StringSetting.Builder()
           .name("kill-sound")
           .description("Sound played when killing a player.")
           .defaultValue("minecraft:entity.zombie.break_wooden_door")
           .build());


   private final Setting<String> breakSound = sgGeneral.add(new StringSetting.Builder()
           .name("break-sound")
           .description("Sound played when breaking a block.")
           .defaultValue("minecraft:entity.zombie.break_wooden_door")
           .build());


   private final Setting<Double> soundPitch = sgGeneral.add(new DoubleSetting.Builder()
           .name("sound-pitch")
           .description("Pitch of the pistol sounds.")
           .defaultValue(2.0)
           .min(0.1)
           .max(2.0)
           .sliderMin(0.1)
           .sliderMax(2.0)
           .build());


   private boolean leftClickPressed = false;


   private final List<String> summonCmds = new ArrayList<>();


   private boolean summoned = false;
   private boolean isSummoning = false;
   private boolean isPositioningRunning = false;
   private int spawnWaitTicks = 0;
   private int summonTicks = 0;


   public PistolModule() {
       super(Blon.Main, "Pistol", "Shoots and kills players or breaks blocks.");
   }


   @Override
   public void onActivate() {
       summoned = false;
       isSummoning = false;
       isPositioningRunning = false;
       spawnWaitTicks = 0;
       summonTicks = 0;
       if (gunModel.get()) {
           loadAndSummonGun();
       }
   }


   @Override
   public void onDeactivate() {
       summoned = false;
       isSummoning = false;
       isPositioningRunning = false;
       spawnWaitTicks = 0;
       removeGun();
       CoreUpdater.stop();
       clear();
   }


   @EventHandler
   private void onTick(TickEvent.Pre event) {
       if (mc.player == null || mc.world == null)
           return;


       handleInput();
       positionGun();
       CoreUpdater.onTick();
   }


   private void handleInput() {
       if (mc.currentScreen != null)
           return;


       boolean leftDown = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(),
               GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;


       if (leftDown && !leftClickPressed) {
           shoot();
       }
       leftClickPressed = leftDown;
   }


   private void clear() {
       if (CoreCommand.corePositions.isEmpty())
           return;
       List<String> emptyCommands = new ArrayList<>();
       for (int i = 0; i < CoreCommand.corePositions.size(); i++) {
           emptyCommands.add("say ");
       }
       CoreUpdater.startSingle(CoreCommand.corePositions, emptyCommands, false, true, CoreCommand.corePositions.size());
   }


   private void shoot() {
       if (CoreCommand.FIRE_CORES.isEmpty()) return;

       if (isSummoning && summonTicks <= 5) return;
       if (CoreUpdater.isSingleRunning()) return;

       Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
       List<String> commands = new ArrayList<>();
       List<BlockPos> coresUsed = new ArrayList<>(CoreCommand.FIRE_CORES);

       HitResult result = null;
       if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
           result = mc.crosshairTarget;
       } else {
           result = ProjectileUtil.raycast(
               mc.player,
               mc.player.getCameraPosVec(1.0f),
               mc.player.getCameraPosVec(1.0f).add(mc.player.getRotationVec(1.0f).multiply(range.get())),
               mc.player.getBoundingBox().stretch(mc.player.getRotationVec(1.0f).multiply(range.get())).expand(1.0, 1.0, 1.0),
               e -> e instanceof PlayerEntity && e != mc.player,
               range.get() * range.get()
           );
           if (result == null) result = mc.player.raycast(range.get(), 0f, false);
       }


       if (result != null) {
           if (result.getType() == HitResult.Type.ENTITY) {
               EntityHitResult entityHit = (EntityHitResult) result;
               Vec3d entityPos = entityHit.getPos();
               if (entityHit.getEntity() instanceof PlayerEntity player) {
                    String playerName = player.getName().getString();
                    commands.add("/kill " + playerName);
                    commands.add(String.format(Locale.US, "/particle minecraft:%s %.3f %.3f %.3f 0 0 0 0 1", particleType.get(), entityPos.x, entityPos.y, entityPos.z));
                    commands.add(String.format(Locale.US, "/playsound %s ui @a ~ ~ ~ 10000 %.1f", killSound.get(), soundPitch.get()));
                    ChatUtils.info("slimed out " + playerName);
               }
           } else if (result.getType() == HitResult.Type.BLOCK) {
               BlockHitResult blockHit = (BlockHitResult) result;
               BlockPos pos = blockHit.getBlockPos();
               commands.add("/fill " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " air");
               Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
               commands.add(String.format(Locale.US, "/particle minecraft:%s %.3f %.3f %.3f 0 0 0 0 1", particleType.get(), blockCenter.x, blockCenter.y, blockCenter.z));
               commands.add(String.format(Locale.US, "/playsound %s ui @a ~ ~ ~ 10000 %.1f", breakSound.get(), soundPitch.get()));
           }
       }


       if (!commands.isEmpty()) {
           CoreUpdater.startSingle(coresUsed, commands, false, true, maxPacketsPerTick.get());
       }
   }


   private void loadAndSummonGun() {
       summonCmds.clear();
       if (mc.player == null) return;
       try (InputStream stream = PistolModule.class.getResourceAsStream("/pistol.txt")) {
           if (stream == null) {
               ChatUtils.error("Could not find pistol.txt resource.");
               return;
           }
           BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
           StringBuilder fullSummon = new StringBuilder();
           String line;
           while ((line = reader.readLine()) != null) {
               line = line.trim();
               if (line.startsWith("/")) line = line.substring(1);
               fullSummon.append(line);
           }


           String summonText = fullSummon.toString();
           if (summonText.startsWith("summon ")) {
               summonCmds.add(summonText);
           }
       } catch (Exception e) {
           ChatUtils.error("Failed to load pistol model: " + e.getMessage());
           e.printStackTrace();
       }
   }


   private double parseRelative(String s) {
       if (s.startsWith("~")) {
           if (s.length() == 1) return 0.0;
           try { return Double.parseDouble(s.substring(1)); }
           catch (NumberFormatException e) { return 0.0; }
       }
       return 0.0;
   }


   private void removeGun() {
       ChatUtils.sendPlayerMsg("/kill @e[tag=blon_pistol]");
       summoned = false;
       isSummoning = false;
       isPositioningRunning = false;
   }


   private void startPositioning() {
       List<BlockPos> cores = CoreCommand.MODEL_CORES;
       if (cores.isEmpty()) return;


       // Use only the first core for positioning to avoid multiple command blocks
       List<BlockPos> positioningCore = new ArrayList<>();
       positioningCore.add(cores.get(0));


       String playerName = mc.player.getName().getString();
       double gx = gunX.get();
       double gy = gunY.get();
       double gz = gunZ.get();


       List<String> tpCommands = new ArrayList<>();
       String cmd = String.format(
           Locale.US,
           "execute as %s at @s anchored eyes run tp @e[tag=blon_pistol,limit=67,sort=nearest] ^%.3f ^%.3f ^%.3f ~ ~",
           playerName,
           gx,
           gy,
           gz
       );
       tpCommands.add(cmd);


       CoreUpdater.startAuto(
           positioningCore,
           tpCommands,
           false,
           true,
           tpCommands.size()
       );
       isPositioningRunning = true;
   }


   private void positionGun() {
       if (!gunModel.get() || summonCmds.isEmpty()) return;


       List<BlockPos> cores = CoreCommand.MODEL_CORES;
       if (cores.isEmpty()) return;


       if (!summoned) {
           if (!isSummoning) {
               ChatUtils.info("Summoning gun model...");
               CoreUpdater.startSingle(
                   cores,
                   summonCmds,
                   false,
                   true,
                   1
               );
               isSummoning = true;
               summonTicks = 0;
           }


           summonTicks++;
           if (summonTicks > 5) {
               summoned = true;
               isSummoning = false;
               spawnWaitTicks = 20;
           }
           return;
       }


       if (spawnWaitTicks-- > 0) return;


       if (!isPositioningRunning) {
           startPositioning();
       }
   }


}

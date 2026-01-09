package skid.supreme.blon.modules;


import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import skid.supreme.blon.Blon;


public class ChatBubble extends Module {
   private final SettingGroup sg = settings.getDefaultGroup();


   private final Setting<Double> heightOffset = sg.add(
       new DoubleSetting.Builder()
           .name("height-offset")
           .defaultValue(2.0)
           .min(0.5)
           .sliderMax(5)
           .build()
   );


   private BlockPos coreCenter;
   private String playerName;


   private long bubbleSpawnTime = 0;


   public ChatBubble() {
       super(Blon.Main, "chat-bubble", "");
   }


   @Override
   public void onActivate() {
       if (!mc.player.getAbilities().creativeMode) {
           error("Creative mode required.");
           toggle();
           return;
       }


       playerName = mc.player.getName().getString();
       placeCore();
       configureCore();


       info("ChatBubble core deployed 50 blocks above player.");
   }


   @Override
   public void onDeactivate() {
       if (coreCenter != null) {
           int radius = 7;
           int yStart = coreCenter.getY() - 7;
           int yEnd = coreCenter.getY() + 7;


           mc.player.networkHandler.sendChatCommand(
               "fill " +
               (coreCenter.getX() - radius) + " " + yStart + " " + (coreCenter.getZ() - radius) + " " +
               (coreCenter.getX() + radius) + " " + yEnd + " " + (coreCenter.getZ() + radius) +
               " air replace command_block"
           );


           mc.player.networkHandler.sendChatCommand("kill @e[type=minecraft:text_display,tag=BLON]");
           bubbleSpawnTime = 0;
       }
   }


   @EventHandler
   private void onSendMessage(SendMessageEvent event) {
       if (coreCenter == null) return;


       String text = escape(event.message);


       if (bubbleSpawnTime != 0) {
           mc.player.networkHandler.sendChatCommand(
               "data merge entity @e[type=minecraft:text_display,tag=BLON,limit=1] {text:{text:\"" + text + "\"}}"
           );
       } else {
           mc.player.networkHandler.sendChatCommand(
               "execute at " + playerName +
               " run summon text_display ~ ~" + heightOffset.get() + " ~ " +
               "{billboard:\"vertical\",text_opacity:255,Tags:[\"BLON\"],text:{\"text\":\"" + text + "\"}}"
           );


           bubbleSpawnTime = System.currentTimeMillis();
       }
   }


   @EventHandler
   private void onTick(TickEvent.Pre event) {
       if (bubbleSpawnTime != 0 && System.currentTimeMillis() - bubbleSpawnTime >= 5000) {
           mc.player.networkHandler.sendChatCommand("kill @e[type=minecraft:text_display,tag=BLON]");
           bubbleSpawnTime = 0;
       }
   }


   private void placeCore() {
       BlockPos playerPos = mc.player.getBlockPos();
       int radius = 7;
       int yStart = playerPos.getY() + 50;
       int yEnd = yStart + 14;

       mc.player.networkHandler.sendChatCommand("forceload add ~ ~ ~ ~");

       mc.player.networkHandler.sendChatCommand(
           "fill " +
           (playerPos.getX() - radius) + " " + yStart + " " + (playerPos.getZ() - radius) + " " +
           (playerPos.getX() + radius) + " " + yEnd + " " + (playerPos.getZ() + radius) +
           " command_block[facing=up]{auto:1b}"
       );

       coreCenter = new BlockPos(playerPos.getX(), yStart + 7, playerPos.getZ());
   }


   private void configureCore() {
       sendBlock(coreCenter.add(1, 0, 0), "execute at " + playerName +
           " run teleport @e[type=minecraft:text_display,tag=BLON] ~ ~" + heightOffset.get() + " ~");
   }


   private void sendBlock(BlockPos pos, String command) {
       mc.player.networkHandler.sendPacket(
           new UpdateCommandBlockC2SPacket(
               pos,
               command,
               CommandBlockBlockEntity.Type.AUTO,
               true,   // trackOutput
               false,  // conditional
               true    // alwaysActive
           )
       );
   }


   private String escape(String s) {
       return s.replace("\\", "\\\\").replace("\"", "\\\"");
   }
}

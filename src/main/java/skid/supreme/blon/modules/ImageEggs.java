package skid.supreme.blon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.util.Hand;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import java.io.StringReader;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import skid.supreme.blon.Blon;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class ImageEggs extends Module {

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> imageFile = sg.add(new StringSetting.Builder()
            .name("image-file")
            .defaultValue("image.png")
            .build());

    private final Setting<Double> scale = sg.add(new DoubleSetting.Builder()
            .name("scale")
            .defaultValue(0.1)
            .min(0.01)
            .sliderMax(1.0)
            .build());

    private final Setting<Double> verticalScale = sg.add(new DoubleSetting.Builder()
            .name("vertical-scale")
            .defaultValue(0.2)
            .min(0.1)
            .sliderMax(3.0)
            .build());

    private final Setting<Double> fontRatio = sg.add(new DoubleSetting.Builder()
            .name("font-ratio")
            .defaultValue(1.8)
            .min(1.0)
            .sliderMax(3.0)
            .build());

    private final Setting<Integer> quality = sg.add(new IntSetting.Builder()
            .name("quality")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 10)
            .build());

    private final Setting<Integer> tickDelay = sg.add(new IntSetting.Builder()
            .name("tick-delay")
            .defaultValue(2)
            .min(0)
            .sliderMax(20)
            .build());

    private final Setting<Double> distance = sg.add(new DoubleSetting.Builder()
            .name("distance-from-player")
            .defaultValue(3.0)
            .min(0.0)
            .sliderMax(10.0)
            .build());

    private final Setting<Mode> mode = sg.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Switch between normal and overlapping text displays")
            .defaultValue(Mode.NORMAL)
            .build());

    public enum Mode {
        NORMAL, OVERLAP
    }

    public enum RenderMode {
        QUALITY,
        PERFORMANCE
    }

    public enum PositionMode {
        OPERATOR,
        CREATIVE
    }

    private final Setting<RenderMode> renderMode = sg.add(
            new EnumSetting.Builder<RenderMode>()
                    .name("render-mode")
                    .description("Quality = gap-filled, overlapped. Performance = fewer entities.")
                    .defaultValue(RenderMode.QUALITY)
                    .build());

    private final Setting<String> billboardMode = sg.add(new StringSetting.Builder()
            .name("billboard-mode")
            .description("Choose how the text display is oriented")
            .defaultValue("center")
            .build());

    private final Setting<Double> overlapFactor = sg.add(new DoubleSetting.Builder()
            .name("overlap-factor")
            .description("Fraction of line/char width to overlap (only for OVERLAP mode)")
            .defaultValue(0.2)
            .min(0.0)
            .max(0.9)
            .visible(() -> mode.get() == Mode.OVERLAP)
            .build());

    private final Setting<Boolean> newestVersion = sg.add(new BoolSetting.Builder()
            .name("newest-version")
            .description("Use the latest text_display NBT format (text:{color:...,text:...})")
            .defaultValue(false)
            .build());

    private final Setting<Integer> tileSize = sg.add(
            new IntSetting.Builder()
                    .name("tile-size")
                    .defaultValue(10)
                    .min(2)
                    .sliderMax(32)
                    .build());

    private final Setting<PositionMode> positionMode = sg.add(
            new EnumSetting.Builder<PositionMode>()
                    .name("position-mode")
                    .description("OPERATOR uses Pos NBT. CREATIVE uses transformation translation.")
                    .defaultValue(PositionMode.OPERATOR)
                    .build());

    private static final String PIXEL = "█";

    private record Pixel(Vec3d pos, String json, int width, float scale) {
    }

    private final Queue<Pixel> queue = new LinkedList<>();
    private int ticks;

    public ImageEggs() {
        super(Blon.Main, "image-eggs", "Spawns images using text_display spawn eggs");
    }

    @Override
    public void onActivate() {
        queue.clear();
        ticks = 0;
        loadImage();
    }

    @EventHandler
    private void onTick(TickEvent.Post e) {
        if (queue.isEmpty() || mc.currentScreen != null)
            return;

        if (++ticks < tickDelay.get())
            return;
        ticks = 0;

        Pixel p = queue.poll();
        spawnTextDisplay(p);
    }

    private void loadImage() {
        try {
            File dir = new File(mc.runDirectory, "images");
            if (!dir.exists())
                dir.mkdirs();

            File file = new File(dir, imageFile.get());
            info("Loading image from: " + file.getAbsolutePath());

            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                error("ImageIO.read returned null (unsupported or corrupt image).");
                return;
            }

            int step = 11 - quality.get();
            if (renderMode.get() == RenderMode.PERFORMANCE) {
                step += 1;
            }
            int rowsPerTile = tileSize.get();

            Vec3d base = mc.player.getEntityPos().add(mc.player.getRotationVector().multiply(distance.get()));
            double px = scale.get() * 0.55;
            double py = scale.get() * fontRatio.get() * verticalScale.get();

            double offsetX = 0.025 * scale.get();

            double finalVerticalScale = scale.get() * verticalScale.get() * fontRatio.get();
            double offsetY = 0.0756 * finalVerticalScale;

            boolean fillGapsEnabled = renderMode.get() == RenderMode.QUALITY;

            int totalTiles = (int) Math.ceil((double) img.getHeight() / (step * rowsPerTile));

            for (int tileY = 0; tileY < img.getHeight(); tileY += step * rowsPerTile) {
                StringBuilder json = new StringBuilder("[");
                boolean firstComponent = true;

                for (int row = 0; row < rowsPerTile; row++) {
                    int y = tileY + row * step;
                    if (y >= img.getHeight())
                        break;

                    Color lastColor = null;
                    StringBuilder currText = new StringBuilder();

                    for (int x = 0; x < img.getWidth(); x += step) {
                        Color c = new Color(img.getRGB(x, y), true);
                        if (c.getAlpha() == 0)
                            c = Color.WHITE;
                        c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);

                        if (renderMode.get() == RenderMode.PERFORMANCE) {
                            c = quantize(c, 6);
                        }

                        if (lastColor != null && !lastColor.equals(c)) {
                            if (!firstComponent)
                                json.append(",");
                            json.append("{\"text\":\"")
                                    .append(currText)
                                    .append("\",\"color\":\"")
                                    .append(toHex(lastColor))
                                    .append("\"}");
                            firstComponent = false;
                            currText.setLength(0);
                        }
                        lastColor = c;
                        currText.append(PIXEL);
                    }

                    if (currText.length() > 0) {
                        if (!firstComponent)
                            json.append(",");
                        json.append("{\"text\":\"")
                                .append(currText)
                                .append("\",\"color\":\"")
                                .append(toHex(lastColor))
                                .append("\"}");
                        firstComponent = false;
                    }

                    if (row < rowsPerTile - 1) {
                        if (!firstComponent)
                            json.append(",");
                        json.append("{\"text\":\"\\n\"}");
                        firstComponent = false;
                    }
                }

                json.append("]");

                int tileIndex = tileY / (step * rowsPerTile);
                double tileHeight = rowsPerTile * py;
                double yPos = base.y + (totalTiles * tileHeight / 2.0) - (tileIndex * tileHeight);

                double xPos = base.x - (img.getWidth() * px / 2);

                int charsPerRow = img.getWidth() / step;
                int lineWidth = charsPerRow * 10;

                double gapOffset = py * 0.18;

                double[][] offsets = fillGapsEnabled ? new double[][] {
                        { 0, 0 }, { 0, offsetY }, { offsetX, 0 }, { offsetX, offsetY }
                } : new double[][] { { 0, 0 } };

                for (double[] offset : offsets) {
                    queue.add(new Pixel(
                            new Vec3d(xPos + offset[0], yPos + offset[1], base.z),
                            json.toString(),
                            lineWidth,
                            scale.get().floatValue()));

                    if (fillGapsEnabled) {
                        queue.add(new Pixel(
                                new Vec3d(xPos + offset[0], yPos + offset[1] - gapOffset, base.z),
                                json.toString(),
                                lineWidth,
                                scale.get().floatValue()));
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            error("Failed to load image: " + ex.getMessage());
        }
    }

    private void spawnTextDisplay(Pixel p) {
        try {
            if (!mc.player.getAbilities().creativeMode)
                return;

            BlockHitResult hit = (BlockHitResult) mc.player.raycast(
                    6.0,
                    1.0f,
                    false);
            Vec3d spawnPos = hit.getPos();
            Vec3d targetPos = p.pos;
            Vec3d offset = targetPos.subtract(spawnPos);

            ItemStack egg = new ItemStack(Items.SHEEP_SPAWN_EGG);
            NbtCompound entity = new NbtCompound();
            entity.putString("id", "minecraft:text_display");

            if (positionMode.get() == PositionMode.OPERATOR) {
                NbtList posList = new NbtList();
                posList.add(NbtDouble.of(targetPos.x));
                posList.add(NbtDouble.of(targetPos.y));
                posList.add(NbtDouble.of(targetPos.z));
                entity.put("Pos", posList);
            } else {
                NbtList posList = new NbtList();
                posList.add(NbtDouble.of(spawnPos.x));
                posList.add(NbtDouble.of(spawnPos.y));
                posList.add(NbtDouble.of(spawnPos.z));
                entity.put("Pos", posList);
            }

            if (newestVersion.get()) {
                NbtList extraList = new NbtList();

                try {
                    JsonArray jsonArray = JsonParser.parseString(p.json).getAsJsonArray();
                    for (JsonElement element : jsonArray) {
                        if (element.isJsonObject()) {
                            JsonObject obj = element.getAsJsonObject();
                            NbtCompound comp = new NbtCompound();
                            if (obj.has("text")) {
                                comp.putString("text", obj.get("text").getAsString());
                            }
                            if (obj.has("color")) {
                                comp.putString("color", obj.get("color").getAsString());
                            }
                            if (obj.has("bold")) {
                                comp.putBoolean("bold", obj.get("bold").getAsBoolean());
                            }
                            if (obj.has("italic")) {
                                comp.putBoolean("italic", obj.get("italic").getAsBoolean());
                            }
                            if (obj.has("underlined")) {
                                comp.putBoolean("underlined", obj.get("underlined").getAsBoolean());
                            }
                            if (obj.has("strikethrough")) {
                                comp.putBoolean("strikethrough", obj.get("strikethrough").getAsBoolean());
                            }
                            if (obj.has("obfuscated")) {
                                comp.putBoolean("obfuscated", obj.get("obfuscated").getAsBoolean());
                            }
                            extraList.add(comp);
                        }
                    }
                } catch (Exception e) {
                    error("Failed to parse text component JSON: " + e.getMessage());
                    return;
                }

                NbtCompound textComponent = new NbtCompound();
                textComponent.putString("text", "");
                textComponent.put("extra", extraList);

                entity.put("text", textComponent);
            } else {
                entity.putString("text", p.json);
            }

            entity.putInt("line_width", p.width);
            entity.putString("billboard", "fixed");

            NbtList rotation = new NbtList();
            rotation.add(NbtFloat.of(0.0f));
            rotation.add(NbtFloat.of(0.0f));
            entity.put("Rotation", rotation);

            NbtCompound transform = new NbtCompound();

            NbtList scaleList = new NbtList();
            scaleList.add(NbtFloat.of(p.scale));
            scaleList.add(NbtFloat.of(p.scale));
            scaleList.add(NbtFloat.of(p.scale));
            transform.put("scale", scaleList);

            NbtList translation = new NbtList();

            if (positionMode.get() == PositionMode.CREATIVE) {
                translation.add(NbtFloat.of((float) offset.x));
                translation.add(NbtFloat.of((float) offset.y));
                translation.add(NbtFloat.of((float) offset.z));
            } else {
                translation.add(NbtFloat.of(0f));
                translation.add(NbtFloat.of(0f));
                translation.add(NbtFloat.of(0f));
            }

            transform.put("translation", translation);

            NbtList leftRot = new NbtList();
            leftRot.add(NbtFloat.of(0f));
            leftRot.add(NbtFloat.of(0f));
            leftRot.add(NbtFloat.of(0f));
            leftRot.add(NbtFloat.of(1f));

            NbtList rightRot = new NbtList();
            rightRot.add(NbtFloat.of(0f));
            rightRot.add(NbtFloat.of(0f));
            rightRot.add(NbtFloat.of(0f));
            rightRot.add(NbtFloat.of(1f));

            transform.put("left_rotation", leftRot);
            transform.put("right_rotation", rightRot);

            entity.put("transformation", transform);

            TypedEntityData<EntityType<?>> entityData = TypedEntityData.create(EntityType.TEXT_DISPLAY, entity);
            egg.applyChanges(ComponentChanges.builder()
                    .add(DataComponentTypes.ENTITY_DATA, entityData)
                    .build());

            ItemStack old = mc.player.getMainHandStack();
            mc.interactionManager.clickCreativeStack(egg, 36 + mc.player.getInventory().selectedSlot);

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.interactionManager.clickCreativeStack(old, 36 + mc.player.getInventory().selectedSlot);

        } catch (Throwable t) {
            error("Failed to spawn TextDisplay (possible vertex overflow). Skipping this pixel.");
        }
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private Color quantize(Color c, int levels) {
        int r = (c.getRed() * levels / 256) * (256 / levels);
        int g = (c.getGreen() * levels / 256) * (256 / levels);
        int b = (c.getBlue() * levels / 256) * (256 / levels);
        return new Color(r, g, b);
    }

    private void probeTextDisplayLimit() {
        if (!mc.player.getAbilities().creativeMode)
            return;

        info("Starting text_display brute-force probe...");

        info("Running PROBE 2: Worst-case color entropy...");
        for (int components = 1; components <= 3000; components++) {
            boolean success = tryWorstCaseText(components);

            if (!success) {
                error("PROBE 2 LIMIT HIT!");
                error("Max safe components ≈ " + (components - 1));
                break;
            }

            info("PROBE 2 OK components=" + components);
        }

        info("Running PROBE 3: Newline stress...");
        for (int rows = 1; rows <= 1000; rows++) {
            boolean success = tryNewlineStress(rows);

            if (!success) {
                error("PROBE 3 LIMIT HIT!");
                error("Max safe rows ≈ " + (rows - 1));
                break;
            }

            info("PROBE 3 OK rows=" + rows);
        }

        info("Running PROBE 4: Real tile emulation...");
        for (int tileSize = 1; tileSize <= 100; tileSize++) {
            boolean success = tryRealTile(tileSize);

            if (!success) {
                error("PROBE 4 LIMIT HIT!");
                error("Max safe tile size ≈ " + (tileSize - 1));
                break;
            }

            info("PROBE 4 OK tileSize=" + tileSize);
        }

        info("All probes finished.");
    }

    private boolean trySpawnDebugText(JsonArray arr, int chars, int components) {
        try {
            ItemStack egg = new ItemStack(Items.SHEEP_SPAWN_EGG);
            NbtCompound entity = new NbtCompound();
            entity.putString("id", "minecraft:text_display");

            Vec3d pos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(0, 1.5, 0);

            NbtList posList = new NbtList();
            posList.add(NbtDouble.of(pos.x));
            posList.add(NbtDouble.of(pos.y));
            posList.add(NbtDouble.of(pos.z));
            entity.put("Pos", posList);

            NbtList extra = new NbtList();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                NbtCompound c = new NbtCompound();
                c.putString("text", o.get("text").getAsString());
                c.putString("color", o.get("color").getAsString());
                extra.add(c);
            }

            NbtCompound text = new NbtCompound();
            text.putString("text", "");
            text.put("extra", extra);
            entity.put("text", text);

            entity.putInt("line_width", 2000);
            entity.putString("billboard", "fixed");

            TypedEntityData<EntityType<?>> data = TypedEntityData.create(EntityType.TEXT_DISPLAY, entity);

            egg.applyChanges(ComponentChanges.builder()
                    .add(DataComponentTypes.ENTITY_DATA, data)
                    .build());

            ItemStack old = mc.player.getMainHandStack();
            mc.interactionManager.clickCreativeStack(egg, 36 + mc.player.getInventory().selectedSlot);

            BlockPos bp = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ());

            BlockHitResult bhr = new BlockHitResult(
                    mc.player.getEntityPos(),
                    Direction.UP,
                    bp,
                    false);

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
            mc.interactionManager.clickCreativeStack(old, 36 + mc.player.getInventory().selectedSlot);

            return true;
        } catch (Throwable t) {
            error("Spawn failed at components=" + components + " chars=" + chars);
            return false;
        }
    }

    private boolean tryWorstCaseText(int components) {
        JsonArray arr = new JsonArray();
        int chars = 0;

        for (int i = 0; i < components; i++) {
            JsonObject o = new JsonObject();
            o.addProperty("text", "█");
            o.addProperty("color",
                    String.format("#%06X", (i * 997) & 0xFFFFFF));
            arr.add(o);
            chars++;
        }

        return trySpawnDebugText(arr, chars, components);
    }

    private boolean tryNewlineStress(int rows) {
        JsonArray arr = new JsonArray();
        int chars = 0;

        for (int i = 0; i < rows; i++) {
            JsonObject o = new JsonObject();
            o.addProperty("text", "██████████\n");
            o.addProperty("color", "#ffffff");
            arr.add(o);
            chars += 11;
        }

        return trySpawnDebugText(arr, chars, rows);
    }

    private boolean tryRealTile(int tileSize) {
        JsonArray arr = new JsonArray();
        int chars = 0;

        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                JsonObject o = new JsonObject();
                o.addProperty("text", "█");
                o.addProperty("color",
                        ((x + y) & 1) == 0 ? "#ffffff" : "#000000");
                arr.add(o);
                chars++;
            }
            JsonObject nl = new JsonObject();
            nl.addProperty("text", "\n");
            nl.addProperty("color", "#ffffff");
            arr.add(nl);
            chars++;
        }

        return trySpawnDebugText(arr, chars, arr.size());
    }
}

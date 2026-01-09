package skid.supreme.blon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import skid.supreme.blon.modules.SongPlayer;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;

public class FunctionCommand extends Command {

    public FunctionCommand() {
        super("function", "Control the MCFunction player", "func", "play");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // .function play <file>
        builder.then(literal("play")
                .then(argument("file", StringArgumentType.word())
                        .executes(ctx -> {
                            String fileName = ctx.getArgument("file", String.class);
                            playFunction(fileName, "play");
                            return SINGLE_SUCCESS;
                        })
                        .then(argument("start", StringArgumentType.word())
                                .executes(ctx -> {
                                    String fileName = ctx.getArgument("file", String.class);
                                    String start = ctx.getArgument("start", String.class);
                                    playFunction(fileName, start);
                                    return SINGLE_SUCCESS;
                                }))));

        // .function stop
        builder.then(literal("stop").executes(ctx -> {
            stopFunction();
            return SINGLE_SUCCESS;
        }));

        // .function list
        builder.then(literal("list").executes(ctx -> {
            listFunctions();
            return SINGLE_SUCCESS;
        }));

        // .function inspect <file>
        builder.then(literal("inspect")
                .then(argument("file", StringArgumentType.word())
                        .executes(ctx -> {
                            String fileName = ctx.getArgument("file", String.class);
                            inspectDatapack(fileName);
                            return SINGLE_SUCCESS;
                        })));

        // .function info
        builder.then(literal("info").executes(ctx -> {
            showInfo();
            return SINGLE_SUCCESS;
        }));
    }

    private void playFunction(String fileName, String startFunc) {
        SongPlayer player = Modules.get().get(SongPlayer.class);

        if (player == null) {
            error("SongPlayer module not found!");
            return;
        }

        // Set the file name and start function
        player.functionFile.set(fileName);
        player.startFunction.set(startFunc);

        // Restart playback
        if (player.isActive()) {
            info("Restarting: " + fileName);
            player.toggle(); // Stop
        }
        player.toggle(); // Start
    }

    private void stopFunction() {
        SongPlayer player = Modules.get().get(SongPlayer.class);

        if (player == null || !player.isActive()) {
            error("Player is not running!");
            return;
        }

        player.toggle();
        info("Stopped playback.");
    }

    private void listFunctions() {
        File dir = new File(mc.runDirectory, "cmdSongs");

        if (!dir.exists()) {
            dir.mkdirs();
            info("Created cmdSongs folder at: " + dir.getAbsolutePath());
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".zip"));

        if (files == null || files.length == 0) {
            info("No .zip datapacks found in: " + dir.getAbsolutePath());
            return;
        }

        info("Available datapacks (" + files.length + "):");
        for (File file : files) {
            long sizeKB = file.length() / 1024;
            info("  - " + file.getName() + " (" + sizeKB + " KB)");
        }
    }

    private void inspectDatapack(String fileName) {
        File dir = new File(mc.runDirectory, "cmdSongs");
        File file = new File(dir, fileName);

        if (!file.exists()) {
            error("File not found: " + fileName);
            return;
        }

        try (ZipFile zip = new ZipFile(file)) {
            info("══════════════════════════════════════");
            info("Inspecting: " + fileName);
            info("══════════════════════════════════════");

            String namespace = "";
            int functionCount = 0;

            // Find namespace
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("data/") && (name.contains("/function/") || name.contains("/functions/"))) {
                    String[] parts = name.split("/");
                    if (parts.length >= 2) {
                        namespace = parts[1];
                        break;
                    }
                }
            }

            if (namespace.isEmpty()) {
                error("No valid datapack structure found!");
                return;
            }

            info("Namespace: " + namespace);
            info("");
            info("Functions found:");

            // List all functions
            entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if ((name.startsWith("data/" + namespace + "/function/") ||
                        name.startsWith("data/" + namespace + "/functions/")) &&
                        name.endsWith(".mcfunction")) {

                    String funcPath = name
                            .replace("data/" + namespace + "/function/", "")
                            .replace("data/" + namespace + "/functions/", "")
                            .replace(".mcfunction", "");

                    info("  - " + funcPath);
                    functionCount++;
                }
            }

            info("");
            info("Total: " + functionCount + " functions");
            info("To play: .function play " + fileName + " <function_name>");
            info("══════════════════════════════════════");

        } catch (Exception e) {
            error("Failed to inspect datapack: " + e.getMessage());
        }
    }

    private void showInfo() {
        File dir = new File(mc.runDirectory, "cmdSongs");
        info("══════════════════════════════════════");
        info("SongPlayer - Music Datapack Player");
        info("══════════════════════════════════════");
        info("Songs folder: " + dir.getAbsolutePath());
        info("");
        info("Commands:");
        info("  .function play <file> [start]");
        info("    Play a datapack (default start: 'play')");
        info("  .function stop");
        info("    Stop playback");
        info("  .function list");
        info("    List available datapacks");
        info("  .function inspect <file>");
        info("    Show functions in a datapack");
        info("  .function info");
        info("    Show this help");
        info("");
        info("How to use:");
        info("1. Download a music datapack (like eidolon packs)");
        info("2. Place the .zip file in cmdSongs folder");
        info("3. Make sure core is spawned (.core)");
        info("4. Inspect: .function inspect yourpack.zip");
        info("5. Play: .function play yourpack.zip play");
        info("");
        info("Settings (in module):");
        info("- packets-per-tick: Adjust for your internet (1-10000)");
        info("- batch-size: Commands per batch (1-500)");
        info("- auto-schedule: Follow schedule commands");
        info("- volume: Adjust playback volume");
        info("- max-playsounds: Safety limit per tick");
        info("══════════════════════════════════════");
    }
}
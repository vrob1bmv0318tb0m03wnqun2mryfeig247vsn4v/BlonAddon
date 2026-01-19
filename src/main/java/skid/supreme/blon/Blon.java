package skid.supreme.blon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.item.Items;
import skid.supreme.blon.commands.AdCommand;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.commands.FunctionCommand;
import skid.supreme.blon.commands.InventoryStealCommand;
import skid.supreme.blon.commands.KillCommand;
import skid.supreme.blon.commands.LoopCommand;
import skid.supreme.blon.commands.ParticleSelectCommand;
import skid.supreme.blon.commands.TestUpdaterCommand;
import skid.supreme.blon.modules.AutoSex;
import skid.supreme.blon.modules.ChatBubble;
import skid.supreme.blon.modules.ForceGrab;
import skid.supreme.blon.modules.ImageEggs;
import skid.supreme.blon.modules.LegitKillAura;
import skid.supreme.blon.modules.NoParticleSpamModule;
import skid.supreme.blon.modules.ParticleAuraModule;
import skid.supreme.blon.modules.PlayerRotatorModule;
import skid.supreme.blon.modules.SchematicPlacer;
import skid.supreme.blon.modules.SongPlayer;
import skid.supreme.blon.modules.TotemBypass;
import skid.supreme.blon.modules.PistolModule;
import skid.supreme.blon.modules.PortalGunModule;
import skid.supreme.blon.modules.AntiCrash;

import skid.supreme.blon.themes.BlonGuiTheme;

public class Blon extends MeteorAddon {
        public static final Logger LOG = LoggerFactory.getLogger(Blon.class);
        public static final Category Main = new Category("Blon", Items.RED_DYE.getDefaultStack());
        public static boolean use12111Format = false;

        @Override
        public void onInitialize() {
                LOG.info("Initializing Blon Addon");

                // Load gamerule version setting
                loadGameruleVersion();

                // Register themes
                GuiThemes.add(new BlonGuiTheme());

                // Register modules
                Modules.get().add(new TotemBypass());
                Modules.get().add(new ImageEggs());
                Modules.get().add(new ChatBubble());
                Modules.get().add(new LegitKillAura());
                Modules.get().add(new ParticleAuraModule());
                Modules.get().add(new NoParticleSpamModule());
                Modules.get().add(new SongPlayer());
                Modules.get().add(new ForceGrab());
                Modules.get().add(new PlayerRotatorModule());
                Modules.get().add(new SchematicPlacer());

                Modules.get().add(new AutoSex());
                Modules.get().add(new PortalGunModule());
                Modules.get().add(new PistolModule());
                Modules.get().add(new AntiCrash());

                // Register commands
                Commands.add(new InventoryStealCommand());
                Commands.add(new LoopCommand());
                Commands.add(new CoreCommand());
                Commands.add(new AdCommand());
                Commands.add(new FunctionCommand());
                Commands.add(new ParticleSelectCommand());
                Commands.add(new TestUpdaterCommand());
                Commands.add(new KillCommand());

                LOG.info("Blon Addon skidded all your mods for you.");
        }

        @Override
        public void onRegisterCategories() {
                Modules.registerCategory(Main);
        }

        public String getPackage() {
                return "skid.supreme.blon";
        }

        private void loadGameruleVersion() {
                Path configDir = Paths.get(System.getProperty("user.home"), ".meteorclient", "addons", "blon");
                Path configFile = configDir.resolve("gamerule_version.txt");
                try {
                        if (Files.exists(configFile)) {
                                use12111Format = Boolean.parseBoolean(Files.readString(configFile).trim());
                        }
                } catch (Exception e) {
                        LOG.error("Failed to load gamerule version setting", e);
                }
        }

        public static void saveGameruleVersion() {
                Path configDir = Paths.get(System.getProperty("user.home"), ".meteorclient", "addons", "blon");
                Path configFile = configDir.resolve("gamerule_version.txt");
                try {
                        Files.createDirectories(configDir);
                        Files.writeString(configFile, String.valueOf(use12111Format));
                } catch (Exception e) {
                        LOG.error("Failed to save gamerule version setting", e);
                }
        }
}

package skid.supreme.blon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import skid.supreme.blon.modules.ParticleAuraModule;
import net.minecraft.item.Items;
import skid.supreme.blon.commands.InventoryStealCommand;
import skid.supreme.blon.commands.CoreCommand;
import skid.supreme.blon.commands.LoopCommand;
import skid.supreme.blon.modules.ChatBubble;
import skid.supreme.blon.modules.ImageEggs;
import skid.supreme.blon.modules.LegitKillAura;
import skid.supreme.blon.modules.TotemBypass;


public class Blon extends MeteorAddon {
        public static final Logger LOG = LoggerFactory.getLogger(Blon.class);
        public static final Category Main = new Category("Blon", Items.RED_DYE.getDefaultStack());

        @Override
        public void onInitialize() {
                LOG.info("Initializing");
                // Please give me more module ideas also stop snooping!
                
                // Register modules
                Modules.get().add(new TotemBypass());
                Modules.get().add(new ImageEggs());
                Modules.get().add(new ChatBubble());
                Modules.get().add(new LegitKillAura());
                Modules.get().add(new ParticleAuraModule());

                // Register commands
                Commands.add(new InventoryStealCommand());
                Commands.add(new LoopCommand());
                Commands.add(new CoreCommand());
                
        }

        @Override
        public void onRegisterCategories() {
                Modules.registerCategory(Main);
        }

        public String getPackage() {
                return "skid.supreme.blon";
        }

}

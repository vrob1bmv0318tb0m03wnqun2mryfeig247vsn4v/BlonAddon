package cod.hex.fih;

import cod.hex.fih.commands.Crash;
import cod.hex.fih.commands.Explosion;
import cod.hex.fih.commands.Steal;
import cod.hex.fih.commands.Tp;
import cod.hex.fih.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

public class Fih extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Fih");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Fihherator");

        Modules.get().add(new AntiCrash());
        Modules.get().add(new RainbowArmor());
        Modules.get().add(new SpamDrop());

        Commands.add(new Crash());
        Commands.add(new Explosion());
        Commands.add(new Steal());
        Commands.add(new Tp());

        ChatUtils.registerCustomPrefix(getPackage() + ".modules", this::getPrefix);
    }

    private Text getPrefix() {
        MutableText value = Text.literal("Fih").withColor(Color.fromRGBA(0, 25, 255, 255));
        MutableText prefix = Text.literal("");

        prefix.setStyle(prefix.getStyle().withFormatting(Formatting.DARK_GRAY))
            .append(Text.literal("["))
            .append(value)
            .append(Text.literal("] "));
        return prefix;
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "cod.hex.fih";
    }

}

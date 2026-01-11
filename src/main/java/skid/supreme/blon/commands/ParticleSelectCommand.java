package skid.supreme.blon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import skid.supreme.blon.modules.ParticleAuraModule;

public class ParticleSelectCommand extends Command {
    public ParticleSelectCommand() {
        super("particle-select", "Selects a particle for ParticleAura.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            ParticleAuraModule module = Modules.get().get(ParticleAuraModule.class);
            if (module == null) {
                error("ParticleAura module not found.");
                return SINGLE_SUCCESS;
            }

            mc.setScreen(GuiThemes.get().moduleScreen(module));
            return SINGLE_SUCCESS;
        });
    }
}

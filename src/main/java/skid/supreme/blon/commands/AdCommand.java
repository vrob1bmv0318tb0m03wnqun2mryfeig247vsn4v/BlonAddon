package skid.supreme.blon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import skid.supreme.blon.core.CoreUpdater;
import skid.supreme.blon.commands.CoreCommand;

import java.util.List;
import java.util.Collections;

public class AdCommand extends Command {
// horribly coded i might be retarded
    public AdCommand() {
        super("ad", "ad tellraw command in the first core impulse block (always active).");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            ad();
            return SINGLE_SUCCESS;
        }).then(literal("off").executes(ctx -> {
            adOff();
            return SINGLE_SUCCESS;
        }));
    }

    private void ad() {
        if (mc.player == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        if (CoreCommand.corePositions.isEmpty()) {
            error("Core not placed. Use .core first.");
            return;
        }

        String cmd = "/tellraw @a [{\"color\":\"#450108\",\"text\":\"[\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#490108\",\"text\":\"B\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#4C0108\",\"text\":\"L\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#500108\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#540107\",\"text\":\"N\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#570107\",\"text\":\"] \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#5B0107\",\"text\":\"M\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#5F0107\",\"text\":\"E\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#620107\",\"text\":\"T\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#660107\",\"text\":\"E\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#690106\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#6D0106\",\"text\":\"R \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#710106\",\"text\":\"C\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#740106\",\"text\":\"L\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#780106\",\"text\":\"I\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#7C0106\",\"text\":\"E\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#7F0105\",\"text\":\"N\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#830105\",\"text\":\"T \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#870105\",\"text\":\"A\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#8A0105\",\"text\":\"D\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#8E0105\",\"text\":\"D\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#920105\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#950105\",\"text\":\"N \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#990104\",\"text\":\"C\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#9D0104\",\"text\":\"L\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#A00104\",\"text\":\"I\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#A40004\",\"text\":\"C\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#A70004\",\"text\":\"K \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#AB0004\",\"text\":\"H\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#AF0003\",\"text\":\"E\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#B20003\",\"text\":\"R\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#B60003\",\"text\":\"E \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#BA0003\",\"text\":\"F\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#BD0003\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#C10003\",\"text\":\"R \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#C50003\",\"text\":\"D\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#C80002\",\"text\":\"I\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#CC0002\",\"text\":\"S\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#D00002\",\"text\":\"C\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#D30002\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#D70002\",\"text\":\"R\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#DB0002\",\"text\":\"D \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#DE0001\",\"text\":\"+ \",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#E20001\",\"text\":\"D\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#E50001\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#E90001\",\"text\":\"W\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#ED0001\",\"text\":\"N\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#F00001\",\"text\":\"L\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#F40000\",\"text\":\"O\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#F80000\",\"text\":\"A\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}},{\"color\":\"#FF0000\",\"text\":\"D\",\"click_event\":{\"action\":\"open_url\",\"url\":\"https://discord.gg/5PFjMYh6SG\"}}]";
        
        List<String> cmds = Collections.singletonList(cmd);

        List<BlockPos> corePosition = Collections.singletonList(CoreCommand.corePositions.get(0));

        CoreUpdater.startAuto(corePosition, cmds, false, true, 1); 

        CoreUpdater.onTick(); 

        info("Ad command sent using the first core block (always active).");
    }

    private void adOff() {
        if (mc.player == null) return;

        if (!mc.player.getAbilities().creativeMode) {
            error("Creative mode required.");
            return;
        }

        if (CoreCommand.corePositions.isEmpty()) {
            error("Core not placed. Use .core first.");
            return;
        }

        BlockPos pos = CoreCommand.corePositions.get(0);
        String cmd = "/data modify block " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " Command set value \"say \"";

        List<String> cmds = Collections.singletonList(cmd);

        List<BlockPos> corePosition = Collections.singletonList(pos);

        CoreUpdater.startAuto(corePosition, cmds, false, true, 1);

        CoreUpdater.onTick();

        info("Ad command block set to 'say '.");
    }
}

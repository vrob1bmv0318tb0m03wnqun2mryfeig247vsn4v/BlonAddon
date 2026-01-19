package cod.hex.fih;

import cod.hex.fih.modules.AntiCrash;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VFih {
    public static boolean antiCrash = false;
    public static AntiCrash mAntiCrash;

    public static int particlesPerTick = 0;

    public static boolean useTextLimiter() {
        return (VFih.antiCrash && Modules.get() != null && VFih.mAntiCrash.textLimiter.get());
    }
    public static boolean useTranslationAntiCrash() {
        return (VFih.antiCrash && Modules.get() != null && VFih.mAntiCrash.translationAntiCrash.get());
    }
    public static boolean useRenderQuality() {
        return (VFih.antiCrash && Modules.get() != null && VFih.mAntiCrash.renderingQuality.get());
    }

    public static Text textLengthError(int a) {
        MutableText text = MutableText.of(Text.of("[Text with length >(%s)]".formatted(a)).getContent());
        text.setStyle(Style.EMPTY.withColor(Color.fromRGBA(255,0,0, 255)));

        return text;
    }

    public static int countMatches(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}

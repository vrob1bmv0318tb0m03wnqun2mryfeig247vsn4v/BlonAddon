package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.*;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.*;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

import skid.supreme.blon.animation.AnimationType;

public class BlonGuiTheme extends MeteorGuiTheme {

    public enum Accents {
        Aristois,
        Boze,
        Mercury,
        Sigma,
        ThunderHack,
        Blon,
        GlossyPurple,
        Fih,
        Forest
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAnimations = settings.createGroup("Animations");

    public final Setting<Accents> accent = sgGeneral.add(new EnumSetting.Builder<Accents>()
            .name("accent")
            .description("The accent of the Blon theme.")
            .defaultValue(Accents.Aristois)
            .onChanged(this::updateTheme)
            .build()
    );

    // Module Animation Settings
    public final Setting<AnimationType> moduleHoverAnimation = sgAnimations.add(new EnumSetting.Builder<AnimationType>()
            .name("module-hover-animation")
            .description("Animation type for module hover effects.")
            .defaultValue(AnimationType.EaseOut)
            .build()
    );

    public final Setting<AnimationType> moduleGlowAnimation = sgAnimations.add(new EnumSetting.Builder<AnimationType>()
            .name("module-glow-animation")
            .description("Animation type for module active glow effects.")
            .defaultValue(AnimationType.EaseOut)
            .build()
    );

    public final Setting<AnimationType> moduleScaleAnimation = sgAnimations.add(new EnumSetting.Builder<AnimationType>()
            .name("module-scale-animation")
            .description("Animation type for module scaling on hover.")
            .defaultValue(AnimationType.Back)
            .build()
    );

    public final Setting<AnimationType> moduleSparkleAnimation = sgAnimations.add(new EnumSetting.Builder<AnimationType>()
            .name("module-sparkle-animation")
            .description("Animation type for sparkle effects on active modules.")
            .defaultValue(AnimationType.Bounce)
            .build()
    );

    public final Setting<Integer> moduleAnimationSpeed = sgAnimations.add(new IntSetting.Builder()
            .name("module-animation-speed")
            .description("Base speed for module animations in milliseconds.")
            .defaultValue(200)
            .min(50)
            .max(1000)
            .sliderMin(50)
            .sliderMax(1000)
            .build()
    );

    public final Setting<Double> moduleScaleFactor = sgAnimations.add(new DoubleSetting.Builder()
            .name("module-scale-factor")
            .description("How much modules scale up on hover (0 = no scaling).")
            .defaultValue(0.08)
            .min(0.0)
            .max(0.2)
            .sliderMin(0.0)
            .sliderMax(0.2)
            .decimalPlaces(3)
            .build()
    );

    public final Setting<Double> glowIntensity = sgAnimations.add(new DoubleSetting.Builder()
            .name("glow-intensity")
            .description("Intensity of the glow effects (0 = no glow).")
            .defaultValue(1.0)
            .min(0.0)
            .max(2.0)
            .sliderMin(0.0)
            .sliderMax(2.0)
            .decimalPlaces(2)
            .build()
    );

    public final Setting<Boolean> enableSparkles = sgAnimations.add(new BoolSetting.Builder()
            .name("enable-sparkles")
            .description("Show sparkle effects on active modules.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> enableScaling = sgAnimations.add(new BoolSetting.Builder()
            .name("enable-scaling")
            .description("Allow modules to scale up on hover.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Boolean> enableGlow = sgAnimations.add(new BoolSetting.Builder()
            .name("enable-glow")
            .description("Show glow effects around active modules.")
            .defaultValue(true)
            .build()
    );

    public BlonGuiTheme() {
        super();

        updateTheme(accent.get());
    }

    @Override
    public String toString() {
        return "Blon";
    }

    private void updateTheme(Accents flavor) {
        switch (flavor) {
            case Aristois:
                applyAristoisTheme();
                break;
            case Boze:
                applyBozeTheme();
                break;
            case Mercury:
                applyMercuryTheme();
                break;
            case Sigma:
                applySigmaTheme();
                break;
            case ThunderHack:
                applyThunderHackTheme();
                break;
            case Blon:
                applyBlonTheme();
                break;
            case GlossyPurple:
                applyGlossyPurpleTheme();
                break;
            case Fih:
                applyFihTheme();
                break;
            case Forest:
                applyForestTheme();
                break;
        }
    }

    private void applyAristoisTheme() {
        scale.set(1.115);
        accentColor.set(new SettingColor(33, 40, 50, 255));
        backgroundColor.get().set(new SettingColor(33, 40, 50, 255));
        moduleBackground.get().set(new SettingColor(33, 40, 50, 255));
        moduleAlignment.set(AlignmentX.Left);
    }

    private void applyBozeTheme() {
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(25, 25, 25, 255));
        backgroundColor.get().set(new SettingColor(25, 25, 25, 255));
        moduleBackground.get().set(new SettingColor(149, 123, 214, 255));
    }

    private void applyMercuryTheme() {
        moduleAlignment.set(AlignmentX.Center);
        accentColor.set(new SettingColor(73, 73, 73, 255));
        placeholderColor.set(new SettingColor(33, 173, 169, 255));
        moduleBackground.get().set(new SettingColor(10, 10, 10, 108));
        backgroundColor.get().set(new SettingColor(30, 30, 30, 181));
    }

    private void applySigmaTheme() {
        scale.set(1.3);
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(241, 247, 248, 255));
        placeholderColor.set(new SettingColor(21, 162, 251));
        moduleBackground.get().set(new SettingColor(21, 162, 251));
        backgroundColor.get().set(new SettingColor(241, 247, 248, 255));
    }

    private void applyThunderHackTheme() {
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(21, 21, 21, 255));
        moduleBackground.get().set(new SettingColor(251, 129, 187, 255));
        backgroundColor.get().set(new SettingColor(35, 34, 34, 255));
    }

    private void applyBlonTheme() {
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(255, 0, 0, 255)); // Red
        backgroundColor.get().set(new SettingColor(0, 0, 0, 255)); // Black
        moduleBackground.get().set(new SettingColor(50, 50, 50, 255));
    }

    private void applyGlossyPurpleTheme() {
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(128, 0, 128, 255)); // Purple
        backgroundColor.get().set(new SettingColor(20, 20, 20, 255));
        moduleBackground.get().set(new SettingColor(60, 0, 60, 255));
    }

    private void applyFihTheme() {
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(128, 128, 128, 255)); // Gray
        backgroundColor.get().set(new SettingColor(0, 0, 139, 255)); // Blue
        moduleBackground.get().set(new SettingColor(70, 70, 70, 255));
    }

    private void applyForestTheme() {
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(34, 139, 34, 255)); // Green
        backgroundColor.get().set(new SettingColor(139, 69, 19, 255)); // Brown
        moduleBackground.get().set(new SettingColor(85, 107, 47, 255));
    }

    public Color textColor() {
        if (accent.get() == Accents.Sigma || accent.get() == Accents.Blon || accent.get() == Accents.GlossyPurple || accent.get() == Accents.Fih || accent.get() == Accents.Forest) {
            return new SettingColor(0, 0, 0, 255);
        }
        return Color.WHITE;
    }

    public Color titleTextColor() {
        if (accent.get() == Accents.Sigma) {
            return new SettingColor(146, 146, 154, 255);
        }
        return Color.WHITE;
    }

    @Override
    public double scale(double value) {
        return value * scale.get();
    }

    @Override
    public boolean categoryIcons() {
        return true;
    }

    // Animation getters
    public AnimationType getModuleHoverAnimation() {
        return moduleHoverAnimation.get();
    }

    public AnimationType getModuleGlowAnimation() {
        return moduleGlowAnimation.get();
    }

    public AnimationType getModuleScaleAnimation() {
        return moduleScaleAnimation.get();
    }

    public AnimationType getModuleSparkleAnimation() {
        return moduleSparkleAnimation.get();
    }

    public int getModuleAnimationSpeed() {
        return moduleAnimationSpeed.get();
    }

    public double getModuleScaleFactor() {
        return moduleScaleFactor.get();
    }

    public double getGlowIntensity() {
        return glowIntensity.get();
    }

    public boolean isSparklesEnabled() {
        return enableSparkles.get();
    }

    public boolean isScalingEnabled() {
        return enableScaling.get();
    }

    public boolean isGlowEnabled() {
        return enableGlow.get();
    }

    @Override
    public boolean hideHUD() {
        return false;
    }

    // Get current accent color based on selected theme
    public Color accentColor() {
        switch (accent.get()) {
            case Aristois -> {
                return new SettingColor(33, 40, 50, 255);
            }
            case Boze -> {
                return new SettingColor(149, 123, 214, 255);
            }
            case Mercury -> {
                return new SettingColor(33, 173, 169, 255);
            }
            case Sigma -> {
                return new SettingColor(21, 162, 251, 255);
            }
            case ThunderHack -> {
                return new SettingColor(251, 129, 187, 255);
            }
            case Blon -> {
                return new SettingColor(255, 0, 0, 255);
            }
            case GlossyPurple -> {
                return new SettingColor(128, 0, 128, 255);
            }
            case Fih -> {
                return new SettingColor(0, 0, 139, 255);
            }
            case Forest -> {
                return new SettingColor(34, 139, 34, 255);
            }
        }
        return new SettingColor(255, 0, 0, 255); // Default fallback
    }

    // Widget factory overrides for animations
    @Override
    public WWidget module(Module module) {
        return w(new skid.supreme.blon.themes.widgets.WBlonModule(module));
    }
}

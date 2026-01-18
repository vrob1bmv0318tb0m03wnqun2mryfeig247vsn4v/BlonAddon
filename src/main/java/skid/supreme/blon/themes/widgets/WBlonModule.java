package skid.supreme.blon.themes.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import skid.supreme.blon.animation.Animation;
import skid.supreme.blon.animation.AnimationType;
import skid.supreme.blon.animation.Direction;
import skid.supreme.blon.themes.BlonGuiTheme;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WBlonModule extends WPressable {
    private final Module module;

    private double titleWidth;
    private boolean wasHovered = false;
    private boolean wasActive = false;
    private boolean isFirstInCategory = false;
    private boolean isLastInCategory = false;

    private Animation glowAnimation;
    private Animation hoverAnimation;
    private Animation scaleAnimation;
    private Animation sparkleAnimation;

    private Color highlightedColor;
    private Color semiTransparentColor;
    private Color transparentColor;
    private Color sparkleColor;
    private Color glowColor1;
    private Color glowColor2;

    public WBlonModule(Module module) {
        this.module = module;
        this.tooltip = module.description;
    }

    @Override
    public void init() {
        boolean isActive = module.isActive();
        wasActive = isActive;

        if (theme instanceof BlonGuiTheme blonTheme) {
            glowAnimation = new Animation(blonTheme.getModuleGlowAnimation(), blonTheme.getModuleAnimationSpeed());
            glowAnimation.finishedAt(isActive ? Direction.FORWARDS : Direction.BACKWARDS);

            hoverAnimation = new Animation(blonTheme.getModuleHoverAnimation(), blonTheme.getModuleAnimationSpeed());

            scaleAnimation = new Animation(blonTheme.getModuleScaleAnimation(), blonTheme.getModuleAnimationSpeed() + 50);

            sparkleAnimation = new Animation(blonTheme.getModuleSparkleAnimation(), blonTheme.getModuleAnimationSpeed() * 2);
        } else {
            // Fallback for non-animated themes
            glowAnimation = new Animation(AnimationType.EaseOut, 200);
            glowAnimation.finishedAt(isActive ? Direction.FORWARDS : Direction.BACKWARDS);

            hoverAnimation = new Animation(AnimationType.EaseOut, 200);
            scaleAnimation = new Animation(AnimationType.Back, 250);
            sparkleAnimation = new Animation(AnimationType.Bounce, 400);
        }

        highlightedColor = getAccentColor().copy();
        transparentColor = highlightedColor.copy().a(5);
        semiTransparentColor = highlightedColor.copy().a(40);

        sparkleColor = new Color(255, 255, 255, 200);
        // Create glow colors based on accent color
        Color accent = getAccentColor();
        glowColor1 = new Color(
            Math.min(255, accent.r + 50),
            Math.min(255, accent.g + 50),
            Math.min(255, accent.b + 50),
            150
        );
        glowColor2 = new Color(
            Math.max(0, accent.r - 50),
            Math.max(0, accent.g - 50),
            Math.max(0, accent.b - 50),
            100
        );

        List<Module> modules = Modules.get().getGroup(module.category);
        if (!modules.isEmpty()) {
            isFirstInCategory = modules.get(0).equals(module);
            isLastInCategory = modules.get(modules.size() - 1).equals(module);
        }
    }

    private Color getTextColor() {
        // Use theme text color if available
        if (theme instanceof BlonGuiTheme blonTheme) {
            return blonTheme.textColor();
        }
        return Color.WHITE;
    }

    private Color getAccentColor() {
        // Get the actual theme accent color
        try {
            if (theme instanceof BlonGuiTheme blonTheme) {
                // Try to get accent color from current theme variant
                return blonTheme.accentColor();
            }
            // Fallback to red if theme doesn't provide accent color
            return new Color(255, 0, 0, 255);
        } catch (Exception e) {
            return new Color(255, 0, 0, 255);
        }
    }

    private Color getBaseColor() {
        // Use dark background color
        return new Color(30, 30, 30, 255);
    }

    @Override
    public double pad() {
        return theme.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        if (titleWidth == 0) titleWidth = theme.textWidth(module.title);

        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT)
            module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT)
            mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        boolean moduleActive = module.isActive();
        double pad = pad();

        // Advanced animation handling
        if (moduleActive != wasActive) {
            wasActive = moduleActive;
            glowAnimation.start(moduleActive ? Direction.FORWARDS : Direction.BACKWARDS);
            if (moduleActive) sparkleAnimation.start(); // Sparkle when activated
        }

        // Enhanced hover handling with scale effect
        if (mouseOver != wasHovered) {
            wasHovered = mouseOver;
            if (mouseOver) {
                hoverAnimation.start();
                scaleAnimation.start();
            } else {
                hoverAnimation.finishedAt(Direction.BACKWARDS);
                scaleAnimation.finishedAt(Direction.BACKWARDS);
            }
        }

        double hoverProgress = hoverAnimation.getProgress();
        double glowProgress = glowAnimation.getProgress();
        double scaleProgress = scaleAnimation.getProgress();
        double sparkleProgress = sparkleAnimation.getProgress();
        double highlightProgress = Math.min(glowProgress + hoverProgress, 1.0);

        double scaleFactor = 1.0;
        double scaledWidth = width;
        double scaledHeight = height;
        double offsetX = 0;
        double offsetY = 0;

        boolean enableScaling = true;
        boolean enableGlow = true;
        boolean enableSparkles = true;
        double glowIntensity = 1.0;

        if (theme instanceof BlonGuiTheme blonTheme) {
            scaleFactor = 1.0 + (scaleProgress * blonTheme.getModuleScaleFactor());
            enableScaling = blonTheme.isScalingEnabled();
            enableGlow = blonTheme.isGlowEnabled();
            enableSparkles = blonTheme.isSparklesEnabled();
            glowIntensity = blonTheme.getGlowIntensity();
        } else {
            scaleFactor = 1.0 + (scaleProgress * 0.08);
        }

        if (enableScaling) {
            scaledWidth = width * scaleFactor;
            scaledHeight = height * scaleFactor;
            offsetX = (scaledWidth - width) / 2;
            offsetY = (scaledHeight - height) / 2;
        }

        if (highlightProgress > 0 || (scaleProgress > 0 && enableScaling)) {
            if (enableGlow && glowProgress > 0) {
                double glowLength = 8 * glowProgress * glowIntensity;

                if (!isFirstInCategory)
                    renderer.quad(
                            x - offsetX,
                            y - offsetY - glowLength,
                            scaledWidth,
                            glowLength,
                            transparentColor,
                            transparentColor,
                            glowColor1,
                            glowColor1
                    );

                if (!isLastInCategory)
                    renderer.quad(
                            x - offsetX,
                            y - offsetY + scaledHeight,
                            scaledWidth,
                            glowLength,
                            glowColor1,
                            glowColor1,
                            transparentColor,
                            transparentColor
                    );

                double secondaryGlow = glowLength * 0.6;
                if (!isFirstInCategory)
                    renderer.quad(
                            x - offsetX,
                            y - offsetY - secondaryGlow,
                            scaledWidth,
                            secondaryGlow,
                            glowColor2,
                            glowColor2,
                            transparentColor,
                            transparentColor
                    );

                if (!isLastInCategory)
                    renderer.quad(
                            x - offsetX,
                            y - offsetY + scaledHeight - secondaryGlow,
                            scaledWidth,
                            secondaryGlow,
                            transparentColor,
                            transparentColor,
                            glowColor2,
                            glowColor2
                    );
            }

            int alpha = (int) (255 * highlightProgress);
            renderer.quad(x - offsetX, y - offsetY, scaledWidth, scaledHeight, highlightedColor.copy().a(alpha));

            if (enableSparkles && moduleActive && sparkleProgress > 0) {
                double sparkleSize = 3 * sparkleProgress;
                double sparkleX = x + width - pad - sparkleSize;
                double sparkleY = y + pad;

                renderer.quad(sparkleX, sparkleY, sparkleSize, sparkleSize, sparkleColor.copy().a((int)(200 * sparkleProgress)));
                renderer.quad(sparkleX - sparkleSize * 1.5, sparkleY, sparkleSize * 0.8, sparkleSize * 0.8, sparkleColor.copy().a((int)(150 * sparkleProgress)));
            }
        }

        // Enhanced text rendering with better color transitions
        double textX = (x - offsetX) + pad;
        double textY = (y - offsetY) + pad;

        // Superior color interpolation with multiple color stops
        Color textColor;
        if (highlightProgress > 0.8) {
            // Bright highlight color
            textColor = interpolateColor(getTextColor(), new Color(255, 255, 100, 255), (highlightProgress - 0.8) * 5);
        } else if (highlightProgress > 0.5) {
            // Transition color
            textColor = interpolateColor(getTextColor(), new Color(200, 200, 150, 255), (highlightProgress - 0.5) * 4);
        } else {
            // Normal interpolation
            textColor = interpolateColor(getTextColor(), getBaseColor(), highlightProgress * 2);
        }

        renderer.text(module.title, textX, textY, textColor, false);
    }

    private Color interpolateColor(Color from, Color to, double progress) {
        int r = (int) (from.r + (to.r - from.r) * progress);
        int g = (int) (from.g + (to.g - from.g) * progress);
        int b = (int) (from.b + (to.b - from.b) * progress);
        int a = (int) (from.a + (to.a - from.a) * progress);
        return new Color(r, g, b, a);
    }
}

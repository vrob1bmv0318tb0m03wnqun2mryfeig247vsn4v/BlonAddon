package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.*;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.*;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screen.Screen;

public class Aristois extends MeteorGuiTheme {
    public Aristois() {
        super();

        // Configure with Aristois settings
        scale.set(1.115);
        placeholderColor.set(new SettingColor(110, 255, 159, 255));
        accentColor.set(new SettingColor(33, 40, 50, 255));
        backgroundColor.get().set(new SettingColor(33, 40, 50, 255));
        moduleBackground.get().set(new SettingColor(33, 40, 50, 255));
        moduleAlignment.set(AlignmentX.Left);
    }

    @Override
    public String toString() {
        return "Aristois";
    }

    public Color titleTextColor() {
        return Color.WHITE;
    }

    public Color textColor() {
        return Color.WHITE;
    }

    public Color textSecondaryColor() {
        return Color.GRAY;
    }

    public Color textHighlightColor() {
        return new SettingColor(33, 40, 50, 255);
    }

    @Override
    public Color starscriptTextColor() {
        return Color.WHITE;
    }

    @Override
    public Color starscriptBraceColor() {
        return Color.GRAY;
    }

    @Override
    public Color starscriptParenthesisColor() {
        return Color.WHITE;
    }

    @Override
    public Color starscriptDotColor() {
        return Color.WHITE;
    }

    @Override
    public Color starscriptCommaColor() {
        return Color.WHITE;
    }

    @Override
    public Color starscriptOperatorColor() {
        return Color.GRAY;
    }

    @Override
    public Color starscriptStringColor() {
        return Color.GREEN;
    }

    @Override
    public Color starscriptNumberColor() {
        return Color.BLUE;
    }

    @Override
    public Color starscriptKeywordColor() {
        return Color.ORANGE;
    }

    @Override
    public Color starscriptAccessedObjectColor() {
        return new Color(128, 0, 128, 255);
    }
}

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

public class Boze extends MeteorGuiTheme {
    public Boze() {
        super();

        // Configure with Boze settings
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(25, 25, 25, 255));
        placeholderColor.set(new SettingColor(44, 44, 44, 255));
        moduleBackground.get().set(new SettingColor(149, 123, 214, 255));
        backgroundColor.get().set(new SettingColor(25, 25, 25, 255));
    }

    @Override
    public String toString() {
        return "Boze";
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
        return new SettingColor(25, 25, 25, 255);
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

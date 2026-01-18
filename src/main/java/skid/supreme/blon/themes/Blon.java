package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class Blon extends MeteorGuiTheme {
    @Override
    public String toString() {
        return "Blon";
    }

    public Blon() {
        super();

        // Configure with Blon settings (Red + Black)
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(255, 0, 0, 255)); // Red
        backgroundColor.get().set(new SettingColor(0, 0, 0, 255)); // Black
        moduleBackground.get().set(new SettingColor(50, 50, 50, 255));
    }

    @Override
    public Color textColor() {
        return Color.BLACK;
    }
}

package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class GlossyPurple extends MeteorGuiTheme {
    @Override
    public String toString() {
        return "GlossyPurple";
    }

    public GlossyPurple() {
        super();

        // Configure with GlossyPurple settings
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(128, 0, 128, 255)); // Purple
        backgroundColor.get().set(new SettingColor(20, 20, 20, 255));
        moduleBackground.get().set(new SettingColor(60, 0, 60, 255));
    }

    @Override
    public Color textColor() {
        return Color.BLACK;
    }
}

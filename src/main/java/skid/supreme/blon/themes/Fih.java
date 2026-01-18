package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class Fih extends MeteorGuiTheme {
    @Override
    public String toString() {
        return "Fih";
    }

    public Fih() {
        super();

        // Configure with Fih settings (Gray + Blue)
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(128, 128, 128, 255)); // Gray
        backgroundColor.get().set(new SettingColor(0, 0, 139, 255)); // Blue
        moduleBackground.get().set(new SettingColor(70, 70, 70, 255));
    }

    @Override
    public Color textColor() {
        return Color.BLACK;
    }
}

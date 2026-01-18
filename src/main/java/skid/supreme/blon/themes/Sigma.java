package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class Sigma extends MeteorGuiTheme {
    @Override
    public String toString() {
        return "Sigma";
    }

    public Sigma() {
        super();

        // Configure with Sigma settings
        scale.set(1.3);
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(241, 247, 248, 255));
        placeholderColor.set(new SettingColor(21, 162, 251));
        moduleBackground.get().set(new SettingColor(21, 162, 251));
        backgroundColor.get().set(new SettingColor(241, 247, 248, 255));
    }

    @Override
    public Color textColor() {
        return Color.BLACK;
    }


}

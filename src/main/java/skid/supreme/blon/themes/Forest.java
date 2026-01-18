package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class Forest extends MeteorGuiTheme {
    @Override
    public String toString() {
        return "Forest";
    }

    public Forest() {
        super();

        // Configure with Forest settings (Green + Brown)
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(34, 139, 34, 255)); // Green
        backgroundColor.get().set(new SettingColor(139, 69, 19, 255)); // Brown
        moduleBackground.get().set(new SettingColor(85, 107, 47, 255));
    }

    @Override
    public Color textColor() {
        return Color.BLACK;
    }
}

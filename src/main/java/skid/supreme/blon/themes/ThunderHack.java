package skid.supreme.blon.themes;

import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ThunderHack extends MeteorGuiTheme {
    @Override
    public String toString() {
        return "ThunderHack";
    }

    public ThunderHack() {
        super();

        // Configure with ThunderHack settings
        moduleAlignment.set(AlignmentX.Left);
        accentColor.set(new SettingColor(21, 21, 21, 255));
        moduleBackground.get().set(new SettingColor(251, 129, 187, 255));
        backgroundColor.get().set(new SettingColor(35, 34, 34, 255));
    }
}

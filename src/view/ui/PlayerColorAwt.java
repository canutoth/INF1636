package view.ui;

import java.awt.Color;
import model.api.dto.PlayerColor;

/**
 * Classe utilitária para conversão entre PlayerColor e Color do AWT.
 */

public final class PlayerColorAwt {

    private PlayerColorAwt() {}

    public static Color toColor(PlayerColor c) {
        if (c == null) return null;
        switch (c) {
            case RED: return Color.RED;
            case BLUE: return Color.BLUE;
            case ORANGE: return new Color(255, 165, 0);
            case YELLOW: return Color.YELLOW;
            case PURPLE: return new Color(128, 0, 128);
            case GRAY: return new Color(128, 128, 128);
            default: return Color.BLACK;
        }
    }


}

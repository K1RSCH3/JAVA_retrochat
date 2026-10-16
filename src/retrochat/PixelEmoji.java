package retrochat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

// Hand-drawn pixels keep emoji consistent without relying on system emoji fonts.
enum PixelEmoji implements Icon {
    SMILE("smile", new Color(255, 204, 64),
        "..oooo..", ".oooooo.", "oo.oo.oo", "oooooooo", "oo.oo.oo", "ooo..ooo", ".oooooo.", "..oooo.."),
    WINK("wink", new Color(255, 204, 64),
        "..oooo..", ".oooooo.", "oo.ooooo", "oooo..oo", "oo.oo.oo", "ooo..ooo", ".oooooo.", "..oooo.."),
    SAD("sad", new Color(64, 188, 255),
        "..oooo..", ".oooooo.", "oo.oo.oo", "oooooooo", "ooo..ooo", "oo.oo.oo", ".oooooo.", "..oooo.."),
    COOL("cool", new Color(255, 204, 64),
        "..oooo..", ".oooooo.", "........", "o..oo..o", "oooooooo", "ooo...oo", ".oooooo.", "..oooo.."),
    HEART("heart", new Color(236, 0, 140),
        ".oo..oo.", "oooooooo", "oooooooo", "oooooooo", ".oooooo.", "..oooo..", "...oo...", "........"),
    STAR("star", new Color(255, 184, 32),
        "...oo...", "...oo...", "oooooooo", ".oooooo.", "..oooo..", ".oooooo.", ".oo..oo.", "........");

    final String code;
    private final Color color;
    private final String[] pixels;

    PixelEmoji(String name, Color color, String... pixels) {
        this.code = ":" + name + ":";
        this.color = color;
        this.pixels = pixels;
    }

    static PixelEmoji fromCode(String code) {
        for (PixelEmoji emoji : values()) {
            if (emoji.code.equals(code)) return emoji;
        }
        return null;
    }

    @Override public int getIconWidth() { return 20; }
    @Override public int getIconHeight() { return 20; }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics copy = graphics.create();
        // A dark square backdrop also forms the faces' eyes and mouths.
        copy.setColor(new Color(24, 24, 24));
        copy.fillRect(x, y, 20, 20);
        copy.setColor(color);
        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[row].length(); col++) {
                if (pixels[row].charAt(col) == 'o') {
                    copy.fillRect(x + 2 + col * 2, y + 2 + row * 2, 2, 2);
                }
            }
        }
        copy.dispose();
    }
}

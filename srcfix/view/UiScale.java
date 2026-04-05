package view;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Locale;

public final class UiScale {

    private static final double SCALE = detectScale();

    private UiScale() {}

    public static double factor() {
        return SCALE;
    }

    public static int scale(int value) {
        return Math.max(1, (int) Math.round(value * SCALE));
    }

    public static float scale(float value) {
        return (float) (value * SCALE);
    }

    public static Dimension dimension(int width, int height) {
        return new Dimension(scale(width), scale(height));
    }

    public static Font font(String family, int style, int size) {
        return new Font(family, style, Math.max(1, scale(size)));
    }

    public static Font font(Font base, int size) {
        return base.deriveFont((float) Math.max(1, scale(size)));
    }

    private static double detectScale() {
        double deviceScale = 1.0;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            if (gd != null) {
                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                if (gc != null) {
                    AffineTransform tx = gc.getDefaultTransform();
                    deviceScale = Math.max(tx.getScaleX(), tx.getScaleY());
                }
            }
        } catch (HeadlessException ignored) {
        }

        double dpiScale = 1.0;
        try {
            dpiScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        } catch (HeadlessException ignored) {
        }

        double scale = Math.max(deviceScale, dpiScale);
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("linux")) {
            scale = Math.max(scale, 1.25);
        }
        return Math.max(1.0, scale);
    }
}
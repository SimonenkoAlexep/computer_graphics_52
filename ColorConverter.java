public class ColorConverter {

    public static float[] rgbToCmyk(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float k = 1 - Math.max(rf, Math.max(gf, bf));
        float c = 0, m = 0, y = 0;
        if (k < 1.0f) {
            c = (1 - rf - k) / (1 - k);
            m = (1 - gf - k) / (1 - k);
            y = (1 - bf - k) / (1 - k);
        }

        return new float[]{c, m, y, k};
    }

    public static int[] cmykToRgb(float c, float m, float y, float k) {
        c = clamp01(c);
        m = clamp01(m);
        y = clamp01(y);
        k = clamp01(k);

        int r = Math.round(255 * (1 - c) * (1 - k));
        int g = Math.round(255 * (1 - m) * (1 - k));
        int b = Math.round(255 * (1 - y) * (1 - k));

        return new int[]{r, g, b};
    }

    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f;
        if (delta > 0) {
            if (max == rf) {
                h = ((gf - bf) / delta) % 6f;
            } else if (max == gf) {
                h = ((bf - rf) / delta) + 2f;
            } else {
                h = ((rf - gf) / delta) + 4f;
            }
            h *= 60f;
            if (h < 0) h += 360f;
        }

        float s = (max == 0) ? 0 : delta / max;
        float v = max;

        return new float[]{h, s, v};
    }

    public static int[] hsvToRgb(float h, float s, float v) {
        h = (h % 360 + 360) % 360;
        s = clamp01(s);
        v = clamp01(v);

        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;

        float rf = 0, gf = 0, bf = 0;
        if (h < 60) {
            rf = c; gf = x; bf = 0;
        } else if (h < 120) {
            rf = x; gf = c; bf = 0;
        } else if (h < 180) {
            rf = 0; gf = c; bf = x;
        } else if (h < 240) {
            rf = 0; gf = x; bf = c;
        } else if (h < 300) {
            rf = x; gf = 0; bf = c;
        } else {
            rf = c; gf = 0; bf = x;
        }

        int r = Math.round((rf + m) * 255);
        int g = Math.round((gf + m) * 255);
        int b = Math.round((bf + m) * 255);

        return new int[]{r, g, b};
    }


    public static float[] cmykToHsv(float c, float m, float y, float k) {
        int[] rgb = cmykToRgb(c, m, y, k);
        return rgbToHsv(rgb[0], rgb[1], rgb[2]);
    }

    public static float[] hsvToCmyk(float h, float s, float v) {
        int[] rgb = hsvToRgb(h, s, v);
        return rgbToCmyk(rgb[0], rgb[1], rgb[2]);
    }


    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
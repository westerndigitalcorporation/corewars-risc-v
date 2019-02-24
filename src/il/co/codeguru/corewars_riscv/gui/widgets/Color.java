package il.co.codeguru.corewars_riscv.gui.widgets;

public class Color {
    public static String BLACK = "#000000";
    public static String black = "#000000";
    public static String WHITE = "#ffffff";
    public static Color COL_BLACK = new Color(0,0,0);

    int m_r, m_g, m_b; // 0-255
    float m_a; // 0-1
    public boolean m_isDark;

    private static final double FACTOR = 0.7;

    public Color(int r, int g, int b) {
        m_r = r;
        m_g = g;
        m_b = b;
        m_a = 1;
        m_isDark = (0.3*r + 0.58*g + 0.12*b) < 128;
        //Console.log("Color " + toString() + "  dark=" + Boolean.toString(m_isDark));
    }
    public Color(int r, int g, int b, float a) {
        m_r = r;
        m_g = g;
        m_b = b;
        m_a = a;
        m_isDark = (0.3*r + 0.58*g + 0.12*b) < 128;
        //Console.log("Color2 " + toString() + "  dark=" + Boolean.toString(m_isDark));
    }


    public static Color HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
            case 0:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (q * 255.0f + 0.5f);
                break;
            }
        }
        return new Color(r, b, g);//0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

    public static Color getHSBColor(float h, float s, float b) {
        return HSBtoRGB(h, s, b);
    }

    public int getRed() {
        return m_r;
    }
    public int getGreen() {
        return m_g;
    }
    public int getBlue() {
        return m_b;
    }
    public float getAlpha() {
        return m_a;
    }

    public Color darker() {
        return new Color(Math.max((int)(getRed()  *FACTOR), 0),
                         Math.max((int)(getGreen()*FACTOR), 0),
                         Math.max((int)(getBlue() *FACTOR), 0),
                         getAlpha());
    }

    public String toString() {
        if (m_a == 1.0)
            return "rgb(" + Integer.toString(m_r) + "," + Integer.toString(m_g) + "," + Integer.toString(m_b) + ")";
        return "rgba(" + Integer.toString(m_r) + "," + Integer.toString(m_g) + "," + Integer.toString(m_b) + "," + Float.toString(m_a) + ")";
    }
}
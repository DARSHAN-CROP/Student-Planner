package com.darshan.studyplanner;

import android.graphics.Color;
import org.json.JSONObject;

/** Appearance: theme colour, dark mode, text size, bold and contrast. Stored in the same "settings" object as the web app, so Google sync carries it. */
final class Theme {
    private Theme() {}

    static final String[] COLORS = {"#FF5A5F", "#2F6FED", "#1E9E63", "#7C4DFF", "#F57C00", "#00897B", "#E91E63", "#2B2B2B"};
    static final float[] SCALES = {0.9f, 1f, 1.15f, 1.3f};

    static int accent = 0xFFFF5A5F, bg = 0xFFF6F5F7, ink = 0xFF1B1B1F, muted = 0xFF6B6B76, card = 0xFFFFFFFF, grey = 0xFFEEEDF2, stroke = 0xFFE6E3EA;
    static boolean bold = false, dark = false, hc = false;
    static float scale = 1f;
    static int size = 1;

    static void load(Store st) {
        JSONObject s = st.getObj("settings");
        if (s == null) s = new JSONObject();
        accent = parse(s.optString("color", COLORS[0]));
        dark = s.optBoolean("dark", false);
        bold = s.optBoolean("bold", false);
        hc = s.optBoolean("hc", false);
        size = Math.max(0, Math.min(SCALES.length - 1, s.optInt("size", 1)));
        scale = SCALES[size];
        if (dark) {
            bg = 0xFF111215; card = 0xFF1D1F24; ink = 0xFFECEEF1; muted = hc ? 0xFFD5D9DF : 0xFF9AA0A8; grey = 0xFF2B2E34; stroke = 0xFF2B2E34;
        } else {
            bg = 0xFFF6F5F7; card = 0xFFFFFFFF; ink = 0xFF1B1B1F; muted = hc ? 0xFF3A3F47 : 0xFF6B6B76; grey = 0xFFEEEDF2; stroke = 0xFFE6E3EA;
        }
    }

    static int parse(String c) {
        try { return Color.parseColor(c); } catch (Exception e) { return 0xFFFF5A5F; }
    }
}

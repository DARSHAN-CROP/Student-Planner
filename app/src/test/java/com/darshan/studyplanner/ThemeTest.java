package com.darshan.studyplanner;

import static org.junit.Assert.*;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ThemeTest {
    static Button find(View v, String text) {
        if (v instanceof Button && ((Button) v).getText().toString().contains(text)) return (Button) v;
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) { Button b = find(g.getChildAt(i), text); if (b != null) return b; } }
        return null;
    }

    @Test
    public void appearanceOptionsWork() throws Exception {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        L.gu = false;
        a.tab = 6; a.render();
        Button dark = find(a.content, "Dark mode: Off");
        assertNotNull("dark toggle", dark);
        dark.performClick();
        assertTrue(Theme.dark);
        assertEquals(0xFF111215, MainActivity.BG);
        Button size = find(a.content, "Text size");
        assertNotNull(size);
        size.performClick();
        assertEquals(2, Theme.size);
        assertEquals(1.15f, Theme.scale, 0.001f);
        Button bold = find(a.content, "Bold text: Off");
        bold.performClick();
        assertTrue(Theme.bold);
        // pick the blue colour swatch (second one)
        JSONObject s = a.st.getObj("settings");
        s.put("color", "#2F6FED");
        a.st.save();
        a.render();
        assertEquals(0xFF2F6FED, MainActivity.CORAL);
        // saved in the same settings object the web app uses
        Store st2 = new Store(a);
        JSONObject s2 = st2.getObj("settings");
        assertTrue(s2.optBoolean("dark") && s2.optBoolean("bold") && s2.optInt("size") == 2);
        // reset
        find(a.content, "Reset").performClick();
        assertFalse(Theme.dark);
        assertEquals(0xFFFF5A5F, MainActivity.CORAL);
        assertEquals(1f, Theme.scale, 0.001f);
        // other screens still render in dark + large text
        a.setSetting("dark", true);
        for (int t = 0; t <= 6; t++) { a.tab = t; a.render(); }
    }
}

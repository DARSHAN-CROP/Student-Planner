package com.darshan.studyplanner;

import static org.junit.Assert.*;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PlanTabsTest {
    @Test
    public void mathIsRight() throws Exception {
        assertEquals(40.0, PlanTabs.pct(2, 5), 0.001);
        assertEquals(7, PlanTabs.need(2, 5, 75));
        assertEquals(0, PlanTabs.need(9, 12, 75));
        assertEquals(1, PlanTabs.canMiss(10, 12, 75)); // 10/13=76.9% ok, 10/14=71%
        assertEquals(0, PlanTabs.canMiss(9, 12, 75));
        JSONArray m = new JSONArray("[{\"credits\":4,\"gp\":8},{\"credits\":2,\"gp\":9},{\"credits\":null,\"gp\":null}]");
        assertEquals(8.33, PlanTabs.cgpa(m), 0.001);
        assertEquals(-1, PlanTabs.cgpa(new JSONArray("[{}]")), 0.001);
    }

    static void texts(View v, StringBuilder sb) {
        if (v instanceof TextView) sb.append(((TextView) v).getText()).append(" | ");
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) texts(g.getChildAt(i), sb); }
    }

    static String screen(MainActivity a) { StringBuilder sb = new StringBuilder(); texts(a.content, sb); return sb.toString(); }

    @Test
    public void screensWork() throws Exception {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        L.gu = true;
        a.tab = 5; PlanTabs.sub = 0; a.render();
        assertTrue(screen(a), screen(a).contains("ક્લાસ ઉમેરો"));
        // timetable: add a class
        for (int i = 0; i < a.content.getChildCount(); i++) { }
        a.st.arr("timetable").put(new JSONObject("{\"id\":\"a\",\"day\":" + PlanTabs.day + ",\"start\":\"00:00\",\"end\":\"23:59\",\"subject\":\"Java\",\"room\":\"Lab 3\"}"));
        a.render();
        String s = screen(a);
        assertTrue(s, s.contains("Java") && s.contains("Lab 3") && s.contains("ચાલુ"));
        // attendance
        PlanTabs.sub = 1;
        a.st.arr("attendance").put(new JSONObject("{\"id\":\"b\",\"subject\":\"Python\",\"present\":2,\"total\":5}"));
        a.render();
        s = screen(a);
        assertTrue(s, s.contains("40%") && s.contains("7"));
        // marks
        PlanTabs.sub = 2;
        a.st.arr("marks").put(new JSONObject("{\"id\":\"c\",\"subject\":\"DBMS\",\"got\":72,\"max\":100,\"credits\":4,\"gp\":8}"));
        a.render();
        s = screen(a);
        assertTrue(s, s.contains("CGPA: 8.00") && s.contains("72%"));
        // dialog add mark
        L.gu = false; a.render();
        Button add = null;
        for (int i = 0; i < a.content.getChildCount(); i++) { View v = a.content.getChildAt(i); if (v instanceof Button) add = (Button) v; }
        assertNotNull(add);
        add.performClick();
        AlertDialog d = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(d);
        a.st.save();
        assertTrue(a.st.arr("marks").length() == 1);
        // survives reload and keeps unknown keys
        Store st2 = new Store(a);
        assertEquals(1, st2.arr("timetable").length());
        assertEquals(1, st2.arr("attendance").length());
    }
}

package com.darshan.studyplanner;

import static org.junit.Assert.*;

import android.content.Context;
import org.robolectric.RuntimeEnvironment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SyncTest {
    @Test
    public void roundTripAndMerge() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        Store s = new Store(c);
        JSONObject hw = new JSONObject().put("subject", "Java").put("task", "Lab 5").put("due", 1767000000000L).put("done", false).put("id", 1L);
        s.arr("homework").put(hw);
        s.arr("exams").put(new JSONObject().put("subject", "Python").put("date", "2026-12-01"));
        s.arr("notes").put(new JSONObject().put("title", "t").put("body", "b"));
        s.arr("subjects").put(new JSONObject().put("name", "DS").put("topics", new JSONArray().put(new JSONObject().put("t", "Tree").put("done", true))));
        s.put("breakStart", 11 * 60 + 15);
        s.put("breakMin", 40);
        s.save();
        JSONObject w = Sync.toWeb(s);
        assertEquals("11:15", w.getJSONObject("settings").getString("breakStart"));
        assertEquals(1, w.getJSONArray("homework").length());
        String due = w.getJSONArray("homework").getJSONObject(0).getString("due");
        assertTrue(due, due.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d"));
        assertTrue(w.getDouble("updated") > 0);

        // a web-made copy with different data replaces the phone copy
        JSONObject remote = new JSONObject(w.toString());
        remote.getJSONArray("homework").put(new JSONObject().put("id", "x1").put("subject", "Web").put("task", "T2").put("due", "2026-11-20T09:30").put("done", true));
        remote.put("updated", w.getDouble("updated") + 5000);
        Sync.fromWeb(remote, s);
        Store s2 = new Store(c);
        assertEquals(2, s2.arr("homework").length());
        assertEquals("Web", s2.arr("homework").getJSONObject(1).getString("subject"));
        assertTrue(s2.arr("homework").getJSONObject(1).getLong("due") > 0);
        assertTrue(s2.arr("homework").getJSONObject(1).getBoolean("done"));
        assertEquals(40, s2.getInt("breakMin", 0));

        // first sign-in merge keeps both sides, no duplicates
        JSONObject a = new JSONObject(w.toString());
        JSONObject b = new JSONObject(w.toString());
        b.getJSONArray("notes").put(new JSONObject().put("id", "n9").put("title", "other").put("body", "z"));
        JSONObject m = Sync.merge(a, b);
        assertEquals(2, m.getJSONArray("notes").length());
        assertEquals(1, m.getJSONArray("exams").length());
        assertEquals(1, m.getJSONArray("homework").length());
    }
}

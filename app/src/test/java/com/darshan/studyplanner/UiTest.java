package com.darshan.studyplanner;

import static org.junit.Assert.*;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.robolectric.Shadows.shadowOf;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiTest {
    static void shot(MainActivity a, String name) throws Exception {
        String dir = System.getProperty("shots.dir", "build/screenshots");
        new File(dir).mkdirs();
        View root = a.getWindow().getDecorView();
        int w = 1179, h = 2556;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bmp));
        try (FileOutputStream o = new FileOutputStream(new File(dir, name))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, o); }
    }

    @Test
    public void walkThroughAllScreens() throws Exception {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        L.gu = true;
        a.st.setLang("gu");
        long now = System.currentTimeMillis();
        String[][] hw = {{"Python", "Lab record પૂરું કરવું", "10"}, {".NET", "Assignment 3", "30"}, {"ICN", "Unit 4 નોંધ", "90"}};
        for (String[] h : hw) {
            JSONObject o = new JSONObject();
            o.put("subject", h[0]); o.put("task", h[1]); o.put("due", now + Long.parseLong(h[2]) * 3600000L); o.put("done", false);
            a.st.arr("homework").put(o);
        }
        JSONObject dn = new JSONObject();
        dn.put("subject", "Java"); dn.put("task", "Practical 5"); dn.put("due", now + 5 * 3600000L); dn.put("done", true);
        a.st.arr("homework").put(dn);
        a.st.save();
        a.tab = 2; a.render();
        shot(a, "1_homework_gu.png");
        a.tab = 5; a.render();
        shot(a, "2_settings_gu.png");
        L.gu = false; a.tab = 2; a.render();
        shot(a, "3_homework_en.png");
        a.tab = 3; a.render();
        a.startBtn.performClick();
        assertTrue(a.running);
        a.resetTimer();
        assertFalse(a.running);
        a.tab = 0; a.render();
        a.tab = 1; a.render();
        a.tab = 4; a.render();
        assertEquals("Student Planner", L.t("app"));

        // ---- alarm rules
        Context ctx = RuntimeEnvironment.getApplication();
        Store s = new Store(ctx);
        long t0 = Alarms.nextTick(now);
        assertTrue(t0 > now && t0 - now <= 20 * 60000L);
        assertEquals(0, Alarms.minuteOfDay(t0) % 20);
        assertEquals(1, Alarms.dueSoon(s, now).size());
        assertTrue(Alarms.hasPending(s, now));
    }

    @Test
    public void ringRules() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        Store s = new Store(ctx);
        long now = System.currentTimeMillis();
        JSONObject o = new JSONObject();
        o.put("subject", "Python"); o.put("task", "Lab"); o.put("due", now + 5 * 3600000L); o.put("done", false);
        s.arr("homework").put(o);
        s.put("quietStart", 0); s.put("quietEnd", 0);   // quiet hours off
        s.save();
        assertFalse(Alarms.inQuiet(s, now));
        // a tick with homework due soon: rings and schedules the next tick
        new RingReceiver().onReceive(ctx, new Intent(ctx, RingReceiver.class).setAction(Alarms.ACT_TICK));
        Intent started = shadowOf((Application) ctx).getNextStartedService();
        assertNotNull(started);
        assertEquals(RingService.class.getName(), started.getComponent().getClassName());
        assertEquals(60, started.getIntExtra("secs", 0));
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        assertNotNull(shadowOf(am).getNextScheduledAlarm());
        // quiet hours cover now: no ring
        int m = Alarms.minuteOfDay(now);
        s.put("quietStart", (m + 1440 - 5) % 1440); s.put("quietEnd", (m + 60) % 1440); s.save();
        assertTrue(Alarms.inQuiet(new Store(ctx), now));
        shadowOf((Application) ctx).clearStartedServices();
        new RingReceiver().onReceive(ctx, new Intent(ctx, RingReceiver.class).setAction(Alarms.ACT_TICK));
        assertNull(shadowOf((Application) ctx).getNextStartedService());
        // homework due in 3 days: no ring
        s.put("quietStart", 0); s.put("quietEnd", 0);
        s.arr("homework").getJSONObject(0).put("due", now + 72 * 3600000L);
        s.save();
        new RingReceiver().onReceive(ctx, new Intent(ctx, RingReceiver.class).setAction(Alarms.ACT_TICK));
        assertNull(shadowOf((Application) ctx).getNextStartedService());
        // break time moves to tomorrow if already past
        s.put("breakStart", (m + 1440 - 1) % 1440); s.save();
        long nb = Alarms.nextBreak(new Store(ctx), now);
        assertTrue(nb > now);
        // break notification text
        BreakReceiver.notifyBreak(ctx, new Store(ctx), "2026-11-05");
        assertEquals("2026-11-05", new Store(ctx).getS("lastBreakDay", ""));
        // ring service starts and stops
        org.robolectric.android.controller.ServiceController<RingService> sc =
                Robolectric.buildService(RingService.class, new Intent(ctx, RingService.class).putExtra("secs", 5));
        sc.create().startCommand(0, 1);
        sc.destroy();
    }
}

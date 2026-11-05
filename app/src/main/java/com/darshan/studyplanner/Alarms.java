package com.darshan.studyplanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Alarm scheduling and the rules for "when should the homework ring". */
final class Alarms {
    private Alarms() {}
    static final String ACT_TICK = "com.darshan.studyplanner.TICK", ACT_BREAK = "com.darshan.studyplanner.BREAK";
    static final long DAY = 24L * 60 * 60 * 1000;
    static final float CAMPUS_RADIUS_M = 600f;

    /** Next wall-clock :00, :20 or :40 after now (3 rings per hour). */
    static long nextTick(long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        int nm = (c.get(Calendar.MINUTE) / 20 + 1) * 20;
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, nm);
        return c.getTimeInMillis();
    }

    static int minuteOfDay(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    /** Quiet hours (no ring). Same start and end means quiet hours are off. */
    static boolean inQuiet(Store s, long now) {
        int qs = s.getInt("quietStart", 22 * 60), qe = s.getInt("quietEnd", 7 * 60), m = minuteOfDay(now);
        if (qs == qe) return false;
        return qs < qe ? (m >= qs && m < qe) : (m >= qs || m < qe);
    }

    /** Homework not finished and due within the next 24 hours. */
    static List<JSONObject> dueSoon(Store s, long now) {
        List<JSONObject> out = new ArrayList<>();
        JSONArray a = s.arr("homework");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null || o.optBoolean("done")) continue;
            long due = o.optLong("due");
            if (due > now && due - now <= DAY) out.add(o);
        }
        return out;
    }

    /** Any unfinished homework that is still in the future. */
    static boolean hasPending(Store s, long now) {
        JSONArray a = s.arr("homework");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && !o.optBoolean("done") && o.optLong("due") > now) return true;
        }
        return false;
    }

    static long nextBreak(Store s, long now) {
        int bs = s.getInt("breakStart", 11 * 60);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, bs / 60);
        c.set(Calendar.MINUTE, bs % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    static void set(Context c, long at, String action, int req) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Class<?> target = ACT_TICK.equals(action) ? RingReceiver.class : BreakReceiver.class;
        Intent i = new Intent(c, target).setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    static void cancel(Context c, String action, int req) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Class<?> target = ACT_TICK.equals(action) ? RingReceiver.class : BreakReceiver.class;
        PendingIntent pi = PendingIntent.getBroadcast(c, req, new Intent(c, target).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    /** Call after any change (homework, break time, boot). */
    static void scheduleAll(Context c) {
        Store s = new Store(c);
        long now = System.currentTimeMillis();
        if (hasPending(s, now)) set(c, nextTick(now), ACT_TICK, 1);
        else cancel(c, ACT_TICK, 1);
        set(c, nextBreak(s, now), ACT_BREAK, 2);
    }
}

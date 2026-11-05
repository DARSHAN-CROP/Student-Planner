package com.darshan.studyplanner;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Fires at the daily break time; notifies once if you are at the campus. */
public class BreakReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, Intent intent) {
        final Store s = new Store(ctx);
        long now = System.currentTimeMillis();
        Alarms.set(ctx, Alarms.nextBreak(s, now), Alarms.ACT_BREAK, 2);
        if (!s.has("campusLat") || !s.has("campusLon")) return;
        final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));
        if (today.equals(s.getS("lastBreakDay", ""))) return;
        final PendingResult pr = goAsync();
        Locator.locate(ctx, 20 * 60 * 1000L, loc -> {
            try {
                if (loc != null) {
                    float[] r = new float[1];
                    Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), s.getD("campusLat", 0), s.getD("campusLon", 0), r);
                    if (r[0] <= Alarms.CAMPUS_RADIUS_M) notifyBreak(ctx, s, today);
                }
            } finally {
                pr.finish();
            }
        });
    }

    static void notifyBreak(Context ctx, Store s, String today) {
        L.gu = s.lang().equals("gu");
        RingService.channels(ctx);
        int pending = 0;
        long now = System.currentTimeMillis();
        for (Object o : Alarms.dueSoon(s, now)) pending++;
        Intent open = new Intent(ctx, MainActivity.class).putExtra("tab", 2).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 7, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification n = RingService.builder(ctx, "brk")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(L.t("brk_title"))
                .setContentText(pending > 0 ? String.format(Locale.US, L.t("brk_text"), pending) : L.t("brk_text0"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(3, n);
        s.put("lastBreakDay", today);
        s.save();
    }
}

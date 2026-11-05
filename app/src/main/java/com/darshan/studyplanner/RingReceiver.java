package com.darshan.studyplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires every 20 minutes while homework is pending, and after boot / time changes. */
public class RingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String a = intent == null ? null : intent.getAction();
        if (Alarms.ACT_TICK.equals(a)) {
            Store s = new Store(ctx);
            long now = System.currentTimeMillis();
            if (Alarms.hasPending(s, now)) Alarms.set(ctx, Alarms.nextTick(now), Alarms.ACT_TICK, 1);
            if (!Alarms.dueSoon(s, now).isEmpty() && !Alarms.inQuiet(s, now)) {
                Intent si = new Intent(ctx, RingService.class);
                si.putExtra("secs", 60);
                ctx.startForegroundService(si);
            }
        } else {
            Alarms.scheduleAll(ctx);
        }
    }
}

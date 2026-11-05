package com.darshan.studyplanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.List;
import org.json.JSONObject;

/** Rings (alarm sound + vibration) for about a minute, with a Stop button. */
public class RingService extends Service {
    static final String CH_RING = "hw_ring", CH_INFO = "hw_info", ACT_STOP = "com.darshan.studyplanner.STOP";
    private MediaPlayer mp;
    private Vibrator vib;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable stopper = this::finishRing;
    private boolean finished = false;

    @Override public IBinder onBind(Intent i) { return null; }

    static void channels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel r = new NotificationChannel(CH_RING, "Homework ring", NotificationManager.IMPORTANCE_HIGH);
        r.setSound(null, null);
        nm.createNotificationChannel(r);
        nm.createNotificationChannel(new NotificationChannel(CH_INFO, "Homework reminders", NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel("brk", "Break", NotificationManager.IMPORTANCE_DEFAULT));
    }

    static Notification.Builder builder(Context c, String ch) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, ch) : new Notification.Builder(c);
    }

    static String summary(Store s, long now) {
        List<JSONObject> due = Alarms.dueSoon(s, now);
        StringBuilder sb = new StringBuilder();
        for (JSONObject o : due) {
            long hrs = (o.optLong("due") - now) / 3600000L;
            if (sb.length() > 0) sb.append("\n");
            String sub = o.optString("subject");
            sb.append("\u2022 ").append(sub.isEmpty() ? "" : sub + ": ").append(o.optString("task")).append(" (").append(hrs).append("h)");
        }
        return sb.toString();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null && ACT_STOP.equals(i.getAction())) {
            finishRing();
            return START_NOT_STICKY;
        }
        int secs = i == null ? 60 : i.getIntExtra("secs", 60);
        Store s = new Store(this);
        L.gu = s.lang().equals("gu");
        long now = System.currentTimeMillis();
        channels(this);
        String text = summary(s, now);
        if (text.isEmpty()) text = L.t("ring_title");
        Intent open = new Intent(this, MainActivity.class).putExtra("tab", 2).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 5, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, RingService.class).setAction(ACT_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 6, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = builder(this, CH_RING)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(L.t("ring_title"))
                .setContentText(text.split("\n")[0])
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(openPi)
                .addAction(android.R.drawable.ic_media_pause, L.t("ring_stop"), stopPi)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(1, n);
        startSound();
        h.removeCallbacks(stopper);
        h.postDelayed(stopper, secs * 1000L);
        return START_NOT_STICKY;
    }

    private void startSound() {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (mp != null) mp.release();
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            mp.setDataSource(this, u);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            if (mp != null) { mp.release(); mp = null; }
        }
        try {
            vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vib != null) vib.vibrate(VibrationEffect.createWaveform(new long[]{0, 700, 500}, 0));
        } catch (Exception ignored) { }
    }

    private void finishRing() {
        if (finished) return;
        finished = true;
        h.removeCallbacks(stopper);
        if (mp != null) {
            try { mp.stop(); } catch (Exception ignored) { }
            mp.release();
            mp = null;
        }
        if (vib != null) vib.cancel();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        finished = false;
        finishRing();
        super.onDestroy();
    }
}

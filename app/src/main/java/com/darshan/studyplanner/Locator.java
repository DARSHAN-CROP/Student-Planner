package com.darshan.studyplanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One-shot location lookup (last known if fresh, else ask once). Result may be null. */
final class Locator {
    private Locator() {}

    @SuppressWarnings("MissingPermission")
    static void locate(Context c, long maxAgeMs, Consumer<Location> cb) {
        final AtomicBoolean done = new AtomicBoolean(false);
        final Consumer<Location> once = loc -> { if (done.compareAndSet(false, true)) cb.accept(loc); };
        if (c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            once.accept(null);
            return;
        }
        final LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && System.currentTimeMillis() - l.getTime() <= maxAgeMs && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (Exception ignored) { }
        }
        if (best != null) { once.accept(best); return; }
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> once.accept(null), 25000);
        try {
            String prov = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER
                    : (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : null);
            if (prov == null) { once.accept(null); return; }
            if (Build.VERSION.SDK_INT >= 30) {
                lm.getCurrentLocation(prov, null, c.getMainExecutor(), once::accept);
            } else {
                lm.requestSingleUpdate(prov, new LocationListener() {
                    @Override public void onLocationChanged(Location l) { once.accept(l); }
                    @Override public void onStatusChanged(String p, int s, Bundle e) { }
                    @Override public void onProviderEnabled(String p) { }
                    @Override public void onProviderDisabled(String p) { }
                }, Looper.getMainLooper());
            }
        } catch (Exception e) {
            once.accept(null);
        }
    }
}

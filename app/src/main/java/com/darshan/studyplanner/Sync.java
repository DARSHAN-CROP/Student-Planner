package com.darshan.studyplanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Google account sync with the web app, using Firebase over plain HTTPS (no extra libraries). */
final class Sync {
    private Sync() {}

    static final String API_KEY = "AIzaSyASuS7s9YNWW3fDjaV5NAitiSBpumxs46c";
    static final String PROJECT = "student-planner-darshan";
    static final String LOGIN_URL = "https://1darshankumarsavsani-blip.github.io/Student-Planner/app-auth.html";
    static final String DOC = "https://firestore.googleapis.com/v1/projects/" + PROJECT + "/databases/(default)/documents/planner/";

    /** Set by the open screen; runs on the UI thread when synced data replaced the local copy. */
    static Runnable onChanged;
    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat DT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("sync", Context.MODE_PRIVATE); }
    static boolean signedIn(Context c) { return !sp(c).getString("refresh", "").isEmpty(); }
    static String email(Context c) { return sp(c).getString("email", ""); }
    static String status(Context c) { return sp(c).getString("status", ""); }

    static void signOut(Context c) { sp(c).edit().clear().apply(); }

    static void startLogin(Activity a) {
        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL)));
    }

    /** Called with the link the sign-in page opens after Google login. Returns true if it was ours. */
    static boolean handleIntent(Context c, Intent in) {
        Uri u = in == null ? null : in.getData();
        if (u == null || !"darshanplanner".equals(u.getScheme())) return false;
        String r = u.getQueryParameter("refresh"), uid = u.getQueryParameter("uid");
        if (r == null || r.isEmpty() || uid == null || uid.isEmpty()) return false;
        String em = u.getQueryParameter("email");
        sp(c).edit().putString("refresh", r).putString("uid", uid).putString("email", em == null ? "" : em)
                .putBoolean("first", true).putString("status", "").apply();
        return true;
    }

    /** Sync a few seconds after the last change. */
    static void poke(Context c) {
        if (!signedIn(c)) return;
        final Context app = c.getApplicationContext();
        UI.removeCallbacksAndMessages(null);
        UI.postDelayed(() -> syncNow(app), 3000);
    }

    static void syncNow(final Context app) {
        if (!signedIn(app)) return;
        new Thread(() -> {
            boolean changed = false;
            String st;
            synchronized (LOCK) {
                try {
                    changed = run(app);
                    st = "OK " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                } catch (Exception e) {
                    st = "Error: " + e.getMessage();
                }
            }
            sp(app).edit().putString("status", st).apply();
            final boolean ch = changed;
            UI.post(() -> { if (onChanged != null) onChanged.run(); });
        }).start();
    }

    private static boolean run(Context app) throws Exception {
        SharedPreferences p = sp(app);
        // 1) fresh id token from the saved refresh token
        JSONObject tok = new JSONObject(http("POST", "https://securetoken.googleapis.com/v1/token?key=" + API_KEY,
                "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(p.getString("refresh", ""), "UTF-8"),
                "application/x-www-form-urlencoded", null, false));
        String idToken = tok.getString("id_token");
        String uid = tok.optString("user_id", p.getString("uid", ""));
        p.edit().putString("refresh", tok.optString("refresh_token", p.getString("refresh", ""))).putString("uid", uid).apply();
        // 2) read the cloud copy
        String url = DOC + uid;
        JSONObject remote = null;
        String body = http("GET", url, null, null, idToken, true);
        if (body != null) {
            JSONObject f = new JSONObject(body).optJSONObject("fields");
            if (f != null && f.optJSONObject("json") != null) remote = new JSONObject(f.getJSONObject("json").getString("stringValue"));
        }
        Store s = new Store(app);
        JSONObject local = toWeb(s);
        double lu = local.optDouble("updated", 0), ru = remote == null ? -1 : remote.optDouble("updated", 0);
        boolean first = p.getBoolean("first", false);
        boolean changed = false;
        if (remote != null && first) {
            JSONObject merged = merge(local, remote);
            merged.put("updated", System.currentTimeMillis());
            fromWeb(merged, s);
            push(url, merged, idToken);
            changed = true;
        } else if (remote != null && ru > lu) {
            fromWeb(remote, s);
            changed = true;
        } else if (remote == null || ru < lu) {
            push(url, local, idToken);
        }
        p.edit().putBoolean("first", false).apply();
        return changed;
    }

    private static void push(String url, JSONObject data, String idToken) throws Exception {
        JSONObject v = new JSONObject().put("stringValue", data.toString());
        JSONObject doc = new JSONObject().put("fields", new JSONObject().put("json", v));
        http("PATCH", url + "?updateMask.fieldPaths=json", doc.toString(), "application/json", idToken, false);
    }

    private static String http(String method, String url, String body, String type, String bearer, boolean nullOn404) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod(method.equals("PATCH") ? "POST" : method);
        if (method.equals("PATCH")) c.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        if (bearer != null) c.setRequestProperty("Authorization", "Bearer " + bearer);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", type);
            try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes("UTF-8")); }
        }
        int code = c.getResponseCode();
        if (code == 404 && nullOn404) return null;
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (in != null) { byte[] b = new byte[4096]; int n; while ((n = in.read(b)) > 0) bo.write(b, 0, n); }
        String out = bo.toString("UTF-8");
        if (code >= 400) throw new Exception("HTTP " + code + " " + (out.length() > 120 ? out.substring(0, 120) : out));
        return out;
    }

    // ---- format conversion: phone <-> web app ----
    private static String sid() { return Long.toString(System.currentTimeMillis(), 36) + Long.toString((long) (Math.random() * 1679616), 36); }

    static JSONObject toWeb(Store s) throws Exception {
        JSONObject w = new JSONObject();
        JSONArray ex = new JSONArray(), su = new JSONArray(), hw = new JSONArray(), nt = new JSONArray();
        JSONArray a = s.arr("exams");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (!o.has("id")) o.put("id", sid());
            ex.put(new JSONObject().put("id", o.optString("id")).put("subject", o.optString("subject")).put("date", o.optString("date")));
        }
        a = s.arr("subjects");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (!o.has("id")) o.put("id", sid());
            JSONArray tp = new JSONArray(), src = o.optJSONArray("topics");
            if (src != null) for (int k = 0; k < src.length(); k++) tp.put(new JSONObject().put("t", src.getJSONObject(k).optString("t")).put("done", src.getJSONObject(k).optBoolean("done")));
            su.put(new JSONObject().put("id", o.optString("id")).put("name", o.optString("name")).put("topics", tp));
        }
        a = s.arr("homework");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (!o.has("id")) o.put("id", sid());
            hw.put(new JSONObject().put("id", o.optString("id")).put("subject", o.optString("subject")).put("task", o.optString("task"))
                    .put("due", DT.format(new Date(o.optLong("due")))).put("done", o.optBoolean("done")));
        }
        a = s.arr("notes");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            if (!o.has("id")) o.put("id", sid());
            nt.put(new JSONObject().put("id", o.optString("id")).put("title", o.optString("title")).put("body", o.optString("body")));
        }
        int bs = s.getInt("breakStart", 11 * 60);
        w.put("exams", ex).put("subjects", su).put("homework", hw).put("notes", nt)
                .put("settings", new JSONObject().put("lang", s.lang()).put("breakStart", String.format(Locale.US, "%02d:%02d", bs / 60, bs % 60))
                        .put("breakMin", s.getInt("breakMin", 30)))
                .put("updated", s.getD("updated", 0));
        s.saveRaw();
        return w;
    }

    static void fromWeb(JSONObject w, Store s) throws Exception {
        JSONArray ex = new JSONArray(), su = new JSONArray(), hw = new JSONArray(), nt = new JSONArray();
        JSONArray a = w.optJSONArray("exams");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); ex.put(new JSONObject().put("id", o.optString("id")).put("subject", o.optString("subject")).put("date", o.optString("date"))); }
        a = w.optJSONArray("subjects");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            JSONArray tp = new JSONArray(), src = o.optJSONArray("topics");
            if (src != null) for (int k = 0; k < src.length(); k++) tp.put(new JSONObject().put("t", src.getJSONObject(k).optString("t")).put("done", src.getJSONObject(k).optBoolean("done")));
            su.put(new JSONObject().put("id", o.optString("id")).put("name", o.optString("name")).put("topics", tp));
        }
        a = w.optJSONArray("homework");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            long due;
            try { due = DT.parse(o.optString("due")).getTime(); } catch (Exception e) { due = System.currentTimeMillis() + 86400000L; }
            hw.put(new JSONObject().put("id", o.optString("id")).put("subject", o.optString("subject")).put("task", o.optString("task")).put("due", due).put("done", o.optBoolean("done")));
        }
        a = w.optJSONArray("notes");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); nt.put(new JSONObject().put("id", o.optString("id")).put("title", o.optString("title")).put("body", o.optString("body"))); }
        s.put("exams", ex); s.put("subjects", su); s.put("homework", hw); s.put("notes", nt);
        JSONObject st = w.optJSONObject("settings");
        if (st != null) {
            String b = st.optString("breakStart", "11:00");
            try { String[] x = b.split(":"); s.put("breakStart", Integer.parseInt(x[0]) * 60 + Integer.parseInt(x[1])); } catch (Exception ignored) { }
            s.put("breakMin", Math.max(5, Math.min(180, st.optInt("breakMin", 30))));
        }
        s.put("updated", w.optDouble("updated", System.currentTimeMillis()));
        s.saveRaw();
        Alarms.scheduleAll(s.context());
    }

    /** First sign-in: keep everything from both sides (same item = same text). */
    static JSONObject merge(JSONObject a, JSONObject b) throws Exception {
        JSONObject m = new JSONObject(b.toString());
        String[][] keys = {{"exams", "subject", "date"}, {"subjects", "name", ""}, {"homework", "task", "subject"}, {"notes", "title", "body"}};
        for (String[] k : keys) {
            JSONArray out = m.optJSONArray(k[0]);
            if (out == null) { out = new JSONArray(); m.put(k[0], out); }
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < out.length(); i++) seen.add(key(out.getJSONObject(i), k));
            JSONArray src = a.optJSONArray(k[0]);
            if (src != null) for (int i = 0; i < src.length(); i++) {
                JSONObject o = src.getJSONObject(i);
                if (seen.add(key(o, k))) out.put(o);
            }
        }
        return m;
    }

    private static String key(JSONObject o, String[] k) {
        return o.optString(k[1]) + "|" + (k[2].isEmpty() ? "" : o.optString(k[2]));
    }
}

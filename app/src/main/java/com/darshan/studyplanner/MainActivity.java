package com.darshan.studyplanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static int CORAL = 0xFFFF5A5F, BG = 0xFFF6F5F7, INK = 0xFF1B1B1F, MUTED = 0xFF6B6B76;
    static final int GREEN = 0xFF1E9E63, AMBER = 0xFFE8A200, RED = 0xFFD64545, BLUE = 0xFF0F90FF;
    

    Store st;
    long focusMs() { return Math.max(1, Math.min(180, st.getInt("focusMin", 25))) * 60000L; }
    long breakMs() { return Math.max(1, Math.min(60, st.getInt("breakMin", 5))) * 60000L; }
    LinearLayout content, nav, root, head;
    TextView title;
    Button langBtn;
    int tab = 0;

    // timer state lives in the activity so it keeps running while you look at other tabs
    long remain = 25 * 60 * 1000L;
    CamNav cam;
    android.view.GestureDetector swipe;
    boolean running = false, focusMode = true;
    CountDownTimer cdt;
    TextView timerView;
    Button startBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Store(this);
        Sync.handleIntent(this, getIntent());
        Sync.onChanged = () -> { st = new Store(MainActivity.this); L.gu = st.lang().equals("gu"); render(); };
        L.gu = st.lang().equals("gu");
        remain = focusMs();
        tab = getIntent().getIntExtra("tab", 0);
        RingService.channels(this);
        Alarms.scheduleAll(this);
        getWindow().setStatusBarColor(CORAL);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        head = Ui.row(this);
        head.setBackgroundColor(CORAL);
        head.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        title = Ui.text(this, "", 20, Color.WHITE, true);
        head.addView(title, Ui.weight(1));
        langBtn = Ui.button(this, "", 0x33FFFFFF, Color.WHITE, v -> {
            L.gu = !L.gu;
            st.setLang(L.gu ? "gu" : "en");
            render();
        });
        head.addView(langBtn);
        root.addView(head, Ui.match());

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        swipe = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(android.view.MotionEvent a, android.view.MotionEvent b, float vx, float vy) {
                if (a == null || b == null) return false;
                float dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
                if (Math.abs(dx) < Ui.dp(MainActivity.this, 90) || Math.abs(dx) < Math.abs(dy) * 2f || Math.abs(vx) < 600) return false;
                int nt = tab + (dx < 0 ? 1 : -1);
                if (nt < 0 || nt > 6) return false;
                tab = nt;
                render();
                return true;
            }
        });
        sv.setOnTouchListener((v, ev) -> { swipe.onTouchEvent(ev); return false; });
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        nav = Ui.row(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        root.addView(nav, Ui.match());

        setContentView(root);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sync.onChanged = () -> { st = new Store(MainActivity.this); L.gu = st.lang().equals("gu"); render(); };
        if (Sync.signedIn(this)) Sync.syncNow(getApplicationContext());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Sync.handleIntent(this, intent)) {
            Toast.makeText(this, L.t("sync_signed"), Toast.LENGTH_LONG).show();
            tab = 6;
            Sync.syncNow(getApplicationContext());
            render();
        }
    }

    @Override
    protected void onDestroy() {
        if (cdt != null) cdt.cancel();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- shell

    void render() {
        Theme.load(st);
        CORAL = Theme.accent; BG = Theme.bg; INK = Theme.ink; MUTED = Theme.muted;
        root.setBackgroundColor(BG);
        head.setBackgroundColor(CORAL);
        getWindow().setStatusBarColor(CORAL);
        nav.setBackgroundColor(Theme.card);
        title.setText(L.t("app"));
        langBtn.setText(L.t("lang_btn"));
        String[] keys = {"tab_exams", "tab_syllabus", "tab_homework", "tab_timer", "tab_notes", "tab_more", "tab_settings"};
        String[] labels = new String[keys.length];
        for (int i = 0; i < keys.length; i++) labels[i] = L.t(keys[i]);
        if (cam == null) {
            cam = new CamNav(this, keys.length, i -> { tab = i; render(); });
            nav.removeAllViews();
            nav.addView(cam, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        }
        cam.select(tab);
        cam.style(labels, CORAL, MUTED);
        content.removeAllViews();
        timerView = null;
        startBtn = null;
        if (tab == 0) exams();
        else if (tab == 1) syllabus();
        else if (tab == 2) homework();
        else if (tab == 3) timer();
        else if (tab == 4) notes();
        else if (tab == 5) PlanTabs.build(this);
        else settings();
    }

    Button primary(String s, View.OnClickListener l) {
        Button x = Ui.button(this, "+  " + s, CORAL, Color.WHITE, l);
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, 0, 0, Ui.dp(this, 12));
        x.setLayoutParams(lp);
        return x;
    }

    TextView empty(String s) {
        TextView t = Ui.text(this, s, 15, MUTED, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 30), 0, 0);
        return t;
    }

    EditText input(String hint, boolean multi) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(5);
            e.setGravity(Gravity.TOP);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setSingleLine(true);
        }
        return e;
    }

    LinearLayout dialogBox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 20), 0);
        return l;
    }

    void ask(String message, Runnable yes) {
        new AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton(L.t("yes"), (d, w) -> yes.run())
                .setNegativeButton(L.t("cancel"), null).show();
    }

    void oneName(String dialogTitle, String hint, java.util.function.Consumer<String> done) {
        LinearLayout box = dialogBox();
        final EditText e = input(hint, false);
        box.addView(e);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(dialogTitle).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) { Toast.makeText(this, L.t("need_name"), Toast.LENGTH_SHORT).show(); return; }
            dlg.dismiss();
            done.accept(s);
        }));
        dlg.show();
    }

    // ---------------------------------------------------------------- exams

    static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    static int daysLeft(String iso) {
        try {
            Calendar t = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
            Date d = ISO.parse(iso);
            return (int) Math.round((d.getTime() - t.getTimeInMillis()) / 86400000.0);
        } catch (Exception e) { return 0; }
    }

    static String pretty(String iso) {
        try { return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(ISO.parse(iso)); } catch (Exception e) { return iso; }
    }

    void exams() {
        content.addView(primary(L.t("add_exam"), v -> addExam()));
        JSONArray a = st.arr("exams");
        ArrayList<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) list.add(a.optJSONObject(i));
        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject x, JSONObject y) { return x.optString("date").compareTo(y.optString("date")); }
        });
        if (list.isEmpty()) { content.addView(empty(L.t("no_exams"))); return; }
        for (final JSONObject o : list) {
            LinearLayout card = Ui.card(this);
            LinearLayout r = Ui.row(this);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(Ui.text(this, o.optString("subject"), 18, INK, true));
            left.addView(Ui.text(this, pretty(o.optString("date")), 14, MUTED, false));
            r.addView(left, Ui.weight(1));
            int n = daysLeft(o.optString("date"));
            String chip; int col;
            if (n < 0) { chip = L.t("done"); col = MUTED; }
            else if (n == 0) { chip = L.t("today"); col = RED; }
            else { chip = String.format(Locale.US, L.t("days_left"), n); col = n <= 3 ? RED : (n <= 7 ? AMBER : GREEN); }
            TextView c = Ui.text(this, chip, 14, Color.WHITE, true);
            c.setBackground(Ui.box(this, col, 20, 0));
            c.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
            r.addView(c);
            TextView x = Ui.text(this, "  \u2715", 18, MUTED, true);
            x.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
            x.setOnClickListener(v -> ask(L.t("sure"), () -> { remove(a, o); st.save(); render(); }));
            r.addView(x);
            card.addView(r);
            content.addView(card);
        }
    }

    static void remove(JSONArray a, JSONObject o) {
        for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) == o) { a.remove(i); return; }
    }

    void addExam() {
        LinearLayout box = dialogBox();
        final EditText e = input(L.t("subject"), false);
        box.addView(e);
        final String[] date = {null};
        final Button pick = Ui.button(this, L.t("pick_date"), Theme.grey, INK, null);
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(this, 12), 0, 0);
        box.addView(pick, lp);
        pick.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 7);
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar s = Calendar.getInstance();
                s.set(y, m, d);
                date[0] = ISO.format(s.getTime());
                pick.setText(pretty(date[0]));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.t("add_exam")).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) { Toast.makeText(this, L.t("need_name"), Toast.LENGTH_SHORT).show(); return; }
            if (date[0] == null) { Toast.makeText(this, L.t("pick_date"), Toast.LENGTH_SHORT).show(); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("subject", s);
                o.put("date", date[0]);
                st.arr("exams").put(o);
            } catch (JSONException ignored) { }
            st.save();
            dlg.dismiss();
            render();
        }));
        dlg.show();
    }

    // ---------------------------------------------------------------- syllabus

    void syllabus() {
        content.addView(primary(L.t("add_subject"), v -> oneName(L.t("add_subject"), L.t("subject"), s -> {
            try {
                JSONObject o = new JSONObject();
                o.put("name", s);
                o.put("topics", new JSONArray());
                st.arr("subjects").put(o);
            } catch (JSONException ignored) { }
            st.save();
            render();
        })));
        final JSONArray subs = st.arr("subjects");
        if (subs.length() == 0) { content.addView(empty(L.t("no_subjects"))); return; }
        for (int i = 0; i < subs.length(); i++) {
            final JSONObject sub = subs.optJSONObject(i);
            final JSONArray topics = sub.optJSONArray("topics");
            int done = 0;
            for (int k = 0; k < topics.length(); k++) if (topics.optJSONObject(k).optBoolean("done")) done++;
            int pct = topics.length() == 0 ? 0 : Math.round(100f * done / topics.length());

            LinearLayout card = Ui.card(this);
            LinearLayout head = Ui.row(this);
            head.addView(Ui.text(this, sub.optString("name"), 18, INK, true), Ui.weight(1));
            head.addView(Ui.text(this, String.format(Locale.US, L.t("progress"), pct), 14, pct == 100 ? GREEN : BLUE, true));
            TextView x = Ui.text(this, "  \u2715", 18, MUTED, true);
            x.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
            x.setOnClickListener(v -> ask(L.t("sure"), () -> { remove(subs, sub); st.save(); render(); }));
            head.addView(x);
            card.addView(head);
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(pct);
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 8));
            pl.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 6));
            card.addView(pb, pl);
            if (topics.length() == 0) card.addView(Ui.text(this, L.t("no_topics"), 14, MUTED, false));
            for (int k = 0; k < topics.length(); k++) {
                final JSONObject tp = topics.optJSONObject(k);
                LinearLayout r = Ui.row(this);
                CheckBox cb = new CheckBox(this);
                cb.setText(tp.optString("t"));
                cb.setTextSize(16);
                cb.setTextColor(INK);
                cb.setChecked(tp.optBoolean("done"));
                cb.setOnCheckedChangeListener((bv, on) -> {
                    try { tp.put("done", on); } catch (JSONException ignored) { }
                    st.save();
                    render();
                });
                r.addView(cb, Ui.weight(1));
                TextView rx = Ui.text(this, "\u2715", 15, MUTED, true);
                rx.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
                rx.setOnClickListener(v -> ask(L.t("sure"), () -> { remove(topics, tp); st.save(); render(); }));
                r.addView(rx);
                card.addView(r);
            }
            Button add = Ui.button(this, "+  " + L.t("add_topic"), Theme.grey, INK, v -> oneName(L.t("add_topic"), L.t("topic"), s -> {
                try {
                    JSONObject o = new JSONObject();
                    o.put("t", s);
                    o.put("done", false);
                    topics.put(o);
                } catch (JSONException ignored) { }
                st.save();
                render();
            }));
            LinearLayout.LayoutParams al = Ui.match();
            al.setMargins(0, Ui.dp(this, 8), 0, 0);
            card.addView(add, al);
            content.addView(card);
        }
    }

    // ---------------------------------------------------------------- timer

    static String mmss(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    void timer() {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 28), Ui.dp(this, 14), Ui.dp(this, 28));
        TextView mode = Ui.text(this, L.t(focusMode ? "focus" : "break"), 18, focusMode ? CORAL : GREEN, true);
        mode.setGravity(Gravity.CENTER);
        card.addView(mode, Ui.match());
        timerView = Ui.text(this, mmss(remain), 64, INK, true);
        timerView.setGravity(Gravity.CENTER);
        card.addView(timerView, Ui.match());
        LinearLayout r = Ui.row(this);
        r.setGravity(Gravity.CENTER);
        startBtn = Ui.button(this, L.t(running ? "pause" : "start"), CORAL, Color.WHITE, v -> toggleTimer());
        r.addView(startBtn);
        Button rs = Ui.button(this, L.t("reset"), Theme.grey, INK, v -> resetTimer());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(this, 12), 0, 0, 0);
        r.addView(rs, lp);
        LinearLayout.LayoutParams rl = Ui.match();
        rl.setMargins(0, Ui.dp(this, 20), 0, 0);
        card.addView(r, rl);
        content.addView(card);
        content.addView(timerSetup());
        TextView s = Ui.text(this, String.format(Locale.US, L.t("sessions"), st.sessionsToday()), 16, INK, true);
        s.setGravity(Gravity.CENTER);
        content.addView(s, Ui.match());
        TextView n = Ui.text(this, L.t("timer_note"), 13, MUTED, false);
        n.setGravity(Gravity.CENTER);
        n.setPadding(0, Ui.dp(this, 8), 0, 0);
        content.addView(n, Ui.match());
    }


    LinearLayout timerSetup() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, L.t("t_custom"), 16, INK, true));
        c.addView(stepRow("focusMin", "t_focus_min", 25, 1, 180, 5));
        c.addView(stepRow("breakMin", "t_break_min", 5, 1, 60, 1));
        LinearLayout pr = Ui.row(this);
        int[][] pre = {{15, 3}, {25, 5}, {45, 10}, {60, 15}};
        for (int[] p : pre) {
            final int f = p[0], b = p[1];
            boolean sel = st.getInt("focusMin", 25) == f && st.getInt("breakMin", 5) == b;
            Button x = Ui.button(this, f + "/" + b, sel ? CORAL : Theme.grey, sel ? Color.WHITE : INK, v -> { if (!running) setTimes(f, b); else Toast.makeText(this, L.t("t_stop_first"), Toast.LENGTH_SHORT).show(); });
            x.setTextSize(13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1);
            lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 8));
            pr.addView(x, lp);
        }
        c.addView(pr, Ui.match());
        c.addView(Ui.text(this, L.t("t_tip"), 12, MUTED, false));
        return c;
    }

    LinearLayout stepRow(final String key, String label, final int def, final int lo, final int hi, final int step) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 8), 0, 0);
        r.addView(Ui.text(this, L.t(label), 15, INK, false), Ui.weight(1));
        final int cur = st.getInt(key, def);
        Button m = Ui.button(this, "−", Theme.grey, INK, v -> bump(key, def, lo, hi, -step));
        Button val = Ui.button(this, cur + " " + L.t("t_min"), Color.TRANSPARENT, CORAL, v -> typeValue(key, def, lo, hi, label));
        Button p = Ui.button(this, "+", Theme.grey, INK, v -> bump(key, def, lo, hi, step));
        m.setMinimumWidth(Ui.dp(this, 48)); p.setMinimumWidth(Ui.dp(this, 48));
        m.setPadding(0, 0, 0, 0); p.setPadding(0, 0, 0, 0);
        r.addView(m, new LinearLayout.LayoutParams(Ui.dp(this, 48), ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(val, new LinearLayout.LayoutParams(Ui.dp(this, 96), ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(p, new LinearLayout.LayoutParams(Ui.dp(this, 48), ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }

    void bump(String key, int def, int lo, int hi, int d) {
        if (running) { Toast.makeText(this, L.t("t_stop_first"), Toast.LENGTH_SHORT).show(); return; }
        int v = Math.max(lo, Math.min(hi, st.getInt(key, def) + d));
        st.put(key, v);
        applyTimes();
    }

    void typeValue(final String key, final int def, final int lo, final int hi, String label) {
        if (running) { Toast.makeText(this, L.t("t_stop_first"), Toast.LENGTH_SHORT).show(); return; }
        LinearLayout box = dialogBox();
        final EditText e = input(lo + "–" + hi, false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(st.getInt(key, def)));
        box.addView(e);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.t(label)).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int n;
            try { n = Integer.parseInt(e.getText().toString().trim()); } catch (Exception ex) { n = -1; }
            if (n < lo || n > hi) { Toast.makeText(this, lo + "–" + hi + " " + L.t("t_min"), Toast.LENGTH_SHORT).show(); return; }
            dlg.dismiss();
            st.put(key, n);
            applyTimes();
        }));
        dlg.show();
    }

    void setTimes(int f, int b) { st.put("focusMin", f); st.put("breakMin", b); applyTimes(); }

    void applyTimes() {
        st.save();
        if (cdt != null) cdt.cancel();
        running = false;
        remain = focusMode ? focusMs() : breakMs();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        render();
    }

    void toggleTimer() {
        if (running) {
            cdt.cancel();
            running = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (startBtn != null) startBtn.setText(L.t("start"));
            return;
        }
        running = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (startBtn != null) startBtn.setText(L.t("pause"));
        cdt = new CountDownTimer(remain, 250) {
            public void onTick(long ms) {
                remain = ms;
                if (timerView != null) timerView.setText(mmss(ms));
            }
            public void onFinish() {
                running = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (focusMode) {
                    st.addSession();
                    Toast.makeText(MainActivity.this, String.format(Locale.US, L.t("focus_done"), (int) (focusMs() / 60000L), (int) (breakMs() / 60000L)), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, L.t("break_done"), Toast.LENGTH_LONG).show();
                }
                focusMode = !focusMode;
                remain = focusMode ? focusMs() : breakMs();
                if (tab == 3) render();
            }
        }.start();
    }

    void resetTimer() {
        if (cdt != null) cdt.cancel();
        running = false;
        focusMode = true;
        remain = focusMs();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        render();
    }

    // ---------------------------------------------------------------- notes

    void notes() {
        content.addView(primary(L.t("add_note"), v -> editNote(null)));
        final JSONArray a = st.arr("notes");
        if (a.length() == 0) { content.addView(empty(L.t("no_notes"))); return; }
        for (int i = a.length() - 1; i >= 0; i--) {
            final JSONObject o = a.optJSONObject(i);
            LinearLayout card = Ui.card(this);
            String t = o.optString("title");
            card.addView(Ui.text(this, t.isEmpty() ? L.t("untitled") : t, 17, INK, true));
            TextView body = Ui.text(this, o.optString("body"), 14, MUTED, false);
            body.setMaxLines(3);
            body.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(body);
            card.setOnClickListener(v -> editNote(o));
            content.addView(card);
        }
    }

    void editNote(final JSONObject existing) {
        LinearLayout box = dialogBox();
        final EditText t = input(L.t("note_title"), false);
        final EditText b = input(L.t("note_body"), true);
        if (existing != null) { t.setText(existing.optString("title")); b.setText(existing.optString("body")); }
        box.addView(t);
        box.addView(b);
        AlertDialog.Builder bld = new AlertDialog.Builder(this).setTitle(L.t(existing == null ? "add_note" : "edit_note")).setView(box)
                .setPositiveButton(L.t("save"), (d, w) -> {
                    String ts = t.getText().toString().trim(), bs = b.getText().toString().trim();
                    if (ts.isEmpty() && bs.isEmpty()) return;
                    try {
                        JSONObject o = existing != null ? existing : new JSONObject();
                        o.put("title", ts);
                        o.put("body", bs);
                        if (existing == null) st.arr("notes").put(o);
                    } catch (JSONException ignored) { }
                    st.save();
                    render();
                })
                .setNegativeButton(L.t("cancel"), null);
        if (existing != null) bld.setNeutralButton(L.t("delete"), (d, w) -> ask(L.t("sure"), () -> { remove(st.arr("notes"), existing); st.save(); render(); }));
        bld.show();
    }

    // ---------------------------------------------------------------- homework

    static final SimpleDateFormat DT = new SimpleDateFormat("dd MMM, HH:mm", Locale.US);

    void homework() {
        content.addView(primary(L.t("add_hw"), v -> addHomework()));
        final JSONArray a = st.arr("homework");
        long now = System.currentTimeMillis();
        int soon = Alarms.dueSoon(st, now).size();
        if (soon > 0) {
            LinearLayout w = Ui.card(this);
            w.setBackground(Ui.box(this, 0xFFFFF0F0, 14, 0xFFFFC9C9));
            w.addView(Ui.text(this, String.format(Locale.US, L.t("hw_soon"), soon), 15, RED, true));
            content.addView(w);
        }
        if (a.length() == 0) { content.addView(empty(L.t("no_hw"))); return; }
        ArrayList<JSONObject> pend = new ArrayList<>(), fin = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o.optBoolean("done")) fin.add(o); else pend.add(o);
        }
        Collections.sort(pend, new Comparator<JSONObject>() {
            public int compare(JSONObject x, JSONObject y) { return Long.compare(x.optLong("due"), y.optLong("due")); }
        });
        for (JSONObject o : pend) content.addView(hwCard(o, a, now));
        if (!fin.isEmpty()) {
            TextView h = Ui.text(this, L.t("hw_finished") + " (" + fin.size() + ")", 14, MUTED, true);
            h.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 6));
            content.addView(h);
            for (JSONObject o : fin) content.addView(hwCard(o, a, now));
        }
    }

    View hwCard(final JSONObject o, final JSONArray a, long now) {
        boolean done = o.optBoolean("done");
        long due = o.optLong("due");
        boolean hot = !done && due - now <= Alarms.DAY;
        LinearLayout card = Ui.card(this);
        LinearLayout row = Ui.row(this);
        CheckBox cb = new CheckBox(this);
        cb.setChecked(done);
        cb.setOnCheckedChangeListener((btn, on) -> {
            try { o.put("done", on); } catch (JSONException ignored) { }
            st.save();
            Alarms.scheduleAll(this);
            render();
        });
        row.addView(cb);
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView task = Ui.text(this, o.optString("task"), 16, done ? MUTED : (hot ? RED : INK), true);
        if (done) task.setPaintFlags(task.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        mid.addView(task);
        String sub = o.optString("subject");
        mid.addView(Ui.text(this, (sub.isEmpty() ? "" : sub + "  \u2022  ") + DT.format(new Date(due)), 13, MUTED, false));
        row.addView(mid, Ui.weight(1));
        if (!done) {
            String tag;
            long ms = due - now;
            if (ms < 0) tag = L.t("overdue");
            else if (ms < Alarms.DAY) tag = String.format(Locale.US, L.t("hrs_left"), (int) (ms / 3600000L));
            else tag = String.format(Locale.US, L.t("days_left"), (int) Math.ceil(ms / (double) Alarms.DAY));
            row.addView(Ui.text(this, tag, 13, hot || ms < 0 ? RED : MUTED, true));
        }
        TextView x = Ui.text(this, "  \u2715", 18, MUTED, true);
        x.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
        x.setOnClickListener(v -> ask(L.t("sure"), () -> {
            for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) == o) { a.remove(i); break; }
            st.save();
            Alarms.scheduleAll(this);
            render();
        }));
        row.addView(x);
        card.addView(row);
        return card;
    }

    void addHomework() {
        LinearLayout box = dialogBox();
        final EditText subj = input(L.t("hw_subject"), false);
        final EditText task = input(L.t("hw_task"), false);
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final Button bd = Ui.button(this, "", Theme.grey, INK, null);
        final Button bt = Ui.button(this, "", Theme.grey, INK, null);
        final Runnable upd = () -> {
            bd.setText(new SimpleDateFormat("dd MMM yyyy", Locale.US).format(cal.getTime()));
            bt.setText(new SimpleDateFormat("HH:mm", Locale.US).format(cal.getTime()));
        };
        upd.run();
        bd.setOnClickListener(v -> new DatePickerDialog(this, (view, yy, mm, dd) -> {
            cal.set(yy, mm, dd);
            upd.run();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show());
        bt.setOnClickListener(v -> new android.app.TimePickerDialog(this, (view, hh, mi) -> {
            cal.set(Calendar.HOUR_OF_DAY, hh);
            cal.set(Calendar.MINUTE, mi);
            upd.run();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show());
        box.addView(subj);
        box.addView(task);
        LinearLayout r = Ui.row(this);
        r.addView(bd, Ui.weight(1));
        r.addView(bt, Ui.weight(1));
        box.addView(r);
        AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.t("add_hw")).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String k = task.getText().toString().trim();
            if (k.isEmpty()) { Toast.makeText(this, L.t("hw_need_task"), Toast.LENGTH_SHORT).show(); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("id", System.currentTimeMillis());
                o.put("subject", subj.getText().toString().trim());
                o.put("task", k);
                o.put("due", cal.getTimeInMillis());
                o.put("done", false);
                st.arr("homework").put(o);
            } catch (JSONException ignored) { }
            st.save();
            Alarms.scheduleAll(this);
            dlg.dismiss();
            render();
        }));
        dlg.show();
    }

    // ---------------------------------------------------------------- settings

    static String hm(int minutes) { return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60); }

    void timeRow(LinearLayout card, String labelKey, final String key, int def, final boolean reschedule) {
        final int cur = st.getInt(key, def);
        Button b = Ui.button(this, String.format(Locale.US, L.t(labelKey), hm(cur)), Theme.grey, INK, v ->
                new android.app.TimePickerDialog(this, (view, hh, mi) -> {
                    st.put(key, hh * 60 + mi);
                    st.save();
                    if (reschedule) Alarms.scheduleAll(this);
                    render();
                }, cur / 60, cur % 60, true).show());
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(b, lp);
    }

    void syncCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, L.t("sync_title"), 17, INK, true));
        boolean in = Sync.signedIn(this);
        String info = in ? String.format(Locale.US, L.t("sync_on"), Sync.email(this)) : L.t("sync_off");
        c.addView(Ui.text(this, info, 14, MUTED, false));
        if (in && !Sync.status(this).isEmpty()) c.addView(Ui.text(this, Sync.status(this), 13, MUTED, false));
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(this, 8), 0, 0);
        if (!in) {
            c.addView(Ui.button(this, L.t("sync_signin"), CORAL, Color.WHITE, v -> Sync.startLogin(this)), lp);
        } else {
            c.addView(Ui.button(this, L.t("sync_now"), CORAL, Color.WHITE, v -> {
                Toast.makeText(this, L.t("sync_wait"), Toast.LENGTH_SHORT).show();
                Sync.syncNow(getApplicationContext());
            }), lp);
            LinearLayout.LayoutParams lp2 = Ui.match();
            lp2.setMargins(0, Ui.dp(this, 8), 0, 0);
            c.addView(Ui.button(this, L.t("sync_out"), Theme.grey, INK, v -> { Sync.signOut(this); render(); }), lp2);
        }
        content.addView(c);
    }

    void settings() {
        content.addView(appearanceCard());
        syncCard();
        // break
        LinearLayout c1 = Ui.card(this);
        c1.addView(Ui.text(this, L.t("set_break"), 17, INK, true));
        timeRow(c1, "break_start", "breakStart", 11 * 60, true);
        Button len = Ui.button(this, String.format(Locale.US, L.t("break_len"), st.getInt("breakMin", 30)), Theme.grey, INK, v -> {
            LinearLayout box = dialogBox();
            final EditText e = input(L.t("break_len_q"), false);
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
            e.setText(String.valueOf(st.getInt("breakMin", 30)));
            box.addView(e);
            new AlertDialog.Builder(this).setTitle(L.t("break_len_q")).setView(box)
                    .setPositiveButton(L.t("save"), (d, w) -> {
                        int m = 30;
                        try { m = Integer.parseInt(e.getText().toString().trim()); } catch (Exception ignored) { }
                        st.put("breakMin", Math.max(5, Math.min(180, m)));
                        st.save();
                        render();
                    }).setNegativeButton(L.t("cancel"), null).show();
        });
        LinearLayout.LayoutParams lp1 = Ui.match();
        lp1.setMargins(0, Ui.dp(this, 8), 0, 0);
        c1.addView(len, lp1);
        content.addView(c1);

        // campus
        LinearLayout c2 = Ui.card(this);
        c2.addView(Ui.text(this, L.t("campus"), 17, INK, true));
        String status = st.has("campusLat")
                ? String.format(Locale.US, L.t("campus_ok"), String.format(Locale.US, "%.4f, %.4f", st.getD("campusLat", 0), st.getD("campusLon", 0)))
                : L.t("campus_unset");
        c2.addView(Ui.text(this, status, 14, MUTED, false));
        Button cb = Ui.button(this, L.t("campus_btn"), CORAL, Color.WHITE, v -> saveCampus());
        LinearLayout.LayoutParams lp2 = Ui.match();
        lp2.setMargins(0, Ui.dp(this, 8), 0, 0);
        c2.addView(cb, lp2);
        content.addView(c2);

        // quiet
        LinearLayout c3 = Ui.card(this);
        c3.addView(Ui.text(this, L.t("quiet"), 17, INK, true));
        timeRow(c3, "quiet_from", "quietStart", 22 * 60, false);
        timeRow(c3, "quiet_to", "quietEnd", 7 * 60, false);
        content.addView(c3);

        // permissions
        LinearLayout c4 = Ui.card(this);
        c4.addView(Ui.text(this, L.t("perms"), 17, INK, true));
        Button pb = Ui.button(this, L.t("perm_btn"), Theme.grey, INK, v -> askPerms());
        Button ab = Ui.button(this, L.t("alarm_btn"), Theme.grey, INK, v -> askAlarm());
        Button tb = Ui.button(this, L.t("test_ring"), CORAL, Color.WHITE, v -> {
            Intent i = new Intent(this, RingService.class).putExtra("secs", 5);
            startForegroundService(i);
        });
        for (Button b : new Button[]{pb, ab, tb}) {
            LinearLayout.LayoutParams lp = Ui.match();
            lp.setMargins(0, Ui.dp(this, 8), 0, 0);
            c4.addView(b, lp);
        }
        TextView tip = Ui.text(this, L.t("hint_battery"), 13, MUTED, false);
        tip.setPadding(0, Ui.dp(this, 10), 0, 0);
        c4.addView(tip);
        content.addView(c4);
    }

    void saveCampus() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            askPerms();
            return;
        }
        Locator.locate(this, 30 * 60 * 1000L, loc -> runOnUiThread(() -> {
            if (loc == null) { Toast.makeText(this, L.t("campus_fail"), Toast.LENGTH_LONG).show(); return; }
            st.put("campusLat", loc.getLatitude());
            st.put("campusLon", loc.getLongitude());
            st.save();
            Toast.makeText(this, L.t("campus_saved"), Toast.LENGTH_SHORT).show();
            render();
        }));
    }

    void askPerms() {
        ArrayList<String> p = new ArrayList<>();
        p.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        p.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (android.os.Build.VERSION.SDK_INT >= 33) p.add("android.permission.POST_NOTIFICATIONS");
        requestPermissions(p.toArray(new String[0]), 11);
    }

    void askAlarm() {
        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:" + getPackageName())));
        } else {
            Toast.makeText(this, L.t("alarm_ok"), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 11 && android.os.Build.VERSION.SDK_INT >= 29
                && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                && checkSelfPermission("android.permission.ACCESS_BACKGROUND_LOCATION") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setMessage(L.t("perm_bg_msg"))
                    .setPositiveButton(L.t("yes"), (d, w) -> requestPermissions(new String[]{"android.permission.ACCESS_BACKGROUND_LOCATION"}, 12))
                    .show();
        }
    }

    // ---------------------------------------------------------------- appearance

    void setSetting(String key, Object val) {
        try {
            JSONObject s = st.getObj("settings");
            if (s == null) s = new JSONObject();
            if (val == null) s.remove(key); else s.put(key, val);
            st.put("settings", s);
        } catch (JSONException ignored) { }
        st.save();
        render();
    }

    View appearanceCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, L.t("ap"), 17, INK, true));
        TextView lbl = Ui.text(this, L.t("ap_color"), 13, MUTED, false);
        lbl.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 6));
        c.addView(lbl);
        LinearLayout sw = Ui.row(this);
        for (final String col : Theme.COLORS) {
            boolean on = Theme.parse(col) == Theme.accent;
            Button b = Ui.button(this, on ? "\u2713" : "", Theme.parse(col), Color.WHITE, v -> setSetting("color", col));
            b.setMinWidth(0);
            b.setPadding(0, 0, 0, 0);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1);
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            sw.addView(b, lp);
        }
        c.addView(sw, Ui.match());
        c.addView(toggle("ap_dark", Theme.dark, "dark"));
        String[] sz = {"ap_s0", "ap_s1", "ap_s2", "ap_s3"};
        Button sb = Ui.button(this, String.format(Locale.US, L.t("ap_size"), L.t(sz[Theme.size])), Theme.grey, INK, v -> setSetting("size", (Theme.size + 1) % 4));
        LinearLayout.LayoutParams l1 = Ui.match();
        l1.setMargins(0, Ui.dp(this, 8), 0, 0);
        c.addView(sb, l1);
        c.addView(toggle("ap_bold", Theme.bold, "bold"));
        c.addView(toggle("ap_hc", Theme.hc, "hc"));
        Button rs = Ui.button(this, L.t("ap_reset"), Theme.grey, INK, v -> {
            try {
                JSONObject s = st.getObj("settings");
                if (s != null) for (String k : new String[]{"color", "dark", "size", "bold", "hc"}) s.remove(k);
            } catch (Exception ignored) { }
            st.save();
            render();
        });
        c.addView(rs, l1);
        return c;
    }

    Button toggle(String labelKey, final boolean on, final String key) {
        Button b = Ui.button(this, String.format(Locale.US, L.t(labelKey), L.t(on ? "ap_on" : "ap_off")), on ? CORAL : Theme.grey, on ? Color.WHITE : INK, v -> setSetting(key, !on));
        LinearLayout.LayoutParams lp = Ui.match();
        lp.setMargins(0, Ui.dp(this, 8), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }
}

package com.darshan.studyplanner;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** V3 screens: weekly timetable, attendance tracker, marks and CGPA. Data is stored in the same JSON as the web app, so Google sync carries it. */
final class PlanTabs {
    private PlanTabs() {}

    static int sub = 0;
    static int day = -1; // 0 = Sunday, like the web app

    // ---------------------------------------------------------------- pure logic (unit tested)

    static double pct(int present, int total) { return total == 0 ? 0 : Math.round(present * 1000.0 / total) / 10.0; }

    /** Classes still needed to reach the target (all attended). 0 if already there. */
    static int need(int present, int total, int target) {
        double r = target / 100.0;
        if (total == 0 || present >= r * total || r >= 1) return 0;
        return (int) Math.ceil((r * total - present) / (1 - r));
    }

    /** Classes that can still be missed while staying at or above the target. */
    static int canMiss(int present, int total, int target) {
        double r = target / 100.0;
        if (r <= 0) return 0;
        return Math.max(0, (int) Math.floor(present / r - total));
    }

    /** CGPA = sum(credits * gp) / sum(credits) over rows that have both; -1 if none. */
    static double cgpa(JSONArray marks) {
        double cs = 0, cg = 0;
        for (int i = 0; i < marks.length(); i++) {
            JSONObject o = marks.optJSONObject(i);
            if (o == null || o.isNull("credits") || o.isNull("gp") || !o.has("credits") || !o.has("gp")) continue;
            double c = o.optDouble("credits", 0);
            if (c > 0) { cs += c; cg += c * o.optDouble("gp", 0); }
        }
        return cs > 0 ? Math.round(cg * 100 / cs) / 100.0 : -1;
    }

    static int toMin(String s) {
        try { String[] p = s.split(":"); return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]); } catch (Exception e) { return 0; }
    }

    static String num(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v); }

    // ---------------------------------------------------------------- shell

    static void build(final MainActivity a) {
        if (day < 0) day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        LinearLayout bar = Ui.row(a);
        String[] keys = {"tab_tt", "tab_att", "tab_marks"};
        for (int i = 0; i < keys.length; i++) {
            final int idx = i;
            boolean sel = i == sub;
            Button b = Ui.button(a, L.t(keys[i]), sel ? MainActivity.CORAL : Theme.grey, sel ? Color.WHITE : MainActivity.INK, v -> { sub = idx; a.render(); });
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), Ui.dp(a, 10));
            bar.addView(b, lp);
        }
        a.content.addView(bar, Ui.match());
        if (sub == 0) timetable(a); else if (sub == 1) attendance(a); else marks(a);
    }

    static EditText numInput(MainActivity a, String hint) {
        EditText e = a.input(hint, false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    static double dbl(EditText e) {
        try { return Double.parseDouble(e.getText().toString().trim()); } catch (Exception x) { return Double.NaN; }
    }

    static void remove(JSONArray arr, JSONObject o) {
        for (int i = 0; i < arr.length(); i++) if (arr.optJSONObject(i) == o) { arr.remove(i); return; }
    }

    static TextView x(final MainActivity a, final JSONArray arr, final JSONObject o) {
        TextView x = Ui.text(a, "  \u2715", 18, MainActivity.MUTED, true);
        x.setPadding(Ui.dp(a, 10), Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6));
        x.setOnClickListener(v -> a.ask(L.t("sure"), () -> { remove(arr, o); a.st.save(); a.render(); }));
        return x;
    }

    // ---------------------------------------------------------------- timetable

    static void timetable(final MainActivity a) {
        LinearLayout days = Ui.row(a);
        int[] order = {1, 2, 3, 4, 5, 6, 0};
        for (final int d : order) {
            boolean sel = d == day;
            Button b = Ui.button(a, L.t("d" + d), sel ? MainActivity.CORAL : Theme.grey, sel ? Color.WHITE : MainActivity.INK, v -> { day = d; a.render(); });
            b.setTextSize(12);
            b.setPadding(Ui.dp(a, 1), Ui.dp(a, 8), Ui.dp(a, 1), Ui.dp(a, 8));
            LinearLayout.LayoutParams lp = Ui.weight(1);
            lp.setMargins(Ui.dp(a, 2), 0, Ui.dp(a, 2), Ui.dp(a, 10));
            days.addView(b, lp);
        }
        a.content.addView(days, Ui.match());
        a.content.addView(a.primary(L.t("tt_add"), v -> addClass(a)));
        final JSONArray arr = a.st.arr("timetable");
        ArrayList<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optInt("day", -1) == day) list.add(o);
        }
        if (list.isEmpty()) { a.content.addView(a.empty(L.t("tt_none"))); return; }
        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject p, JSONObject q) { return Integer.compare(toMin(p.optString("start")), toMin(q.optString("start"))); }
        });
        Calendar c = Calendar.getInstance();
        boolean today = c.get(Calendar.DAY_OF_WEEK) - 1 == day;
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        boolean nextDone = false;
        for (JSONObject o : list) {
            int s = toMin(o.optString("start")), e = toMin(o.optString("end"));
            String tag = "";
            int col = MainActivity.MUTED;
            if (today && now >= s && now < e) { tag = L.t("tt_now"); col = MainActivity.GREEN; }
            else if (today && !nextDone && s > now) { nextDone = true; tag = L.t("tt_next"); }
            LinearLayout card = Ui.card(a);
            LinearLayout row = Ui.row(a);
            LinearLayout left = new LinearLayout(a);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(Ui.text(a, o.optString("start"), 16, MainActivity.CORAL, true));
            left.addView(Ui.text(a, o.optString("end"), 12, MainActivity.MUTED, false));
            left.setPadding(0, 0, Ui.dp(a, 14), 0);
            row.addView(left);
            LinearLayout mid = new LinearLayout(a);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.addView(Ui.text(a, o.optString("subject"), 16, MainActivity.INK, true));
            String room = o.optString("room");
            if (!room.isEmpty()) mid.addView(Ui.text(a, room, 13, MainActivity.MUTED, false));
            row.addView(mid, Ui.weight(1));
            if (!tag.isEmpty()) row.addView(Ui.text(a, tag, 13, col, true));
            row.addView(x(a, arr, o));
            card.addView(row);
            a.content.addView(card);
        }
    }

    static void addClass(final MainActivity a) {
        LinearLayout box = a.dialogBox();
        final EditText subj = a.input(L.t("tt_sub"), false);
        final EditText room = a.input(L.t("tt_room"), false);
        final int[] t = {9 * 60, 10 * 60};
        final Button b1 = Ui.button(a, "", Theme.grey, MainActivity.INK, null);
        final Button b2 = Ui.button(a, "", Theme.grey, MainActivity.INK, null);
        final Runnable upd = () -> { b1.setText(L.t("tt_start") + " " + MainActivity.hm(t[0])); b2.setText(L.t("tt_end") + " " + MainActivity.hm(t[1])); };
        upd.run();
        b1.setOnClickListener(v -> new TimePickerDialog(a, (view, hh, mi) -> { t[0] = hh * 60 + mi; upd.run(); }, t[0] / 60, t[0] % 60, true).show());
        b2.setOnClickListener(v -> new TimePickerDialog(a, (view, hh, mi) -> { t[1] = hh * 60 + mi; upd.run(); }, t[1] / 60, t[1] % 60, true).show());
        box.addView(subj);
        box.addView(room);
        LinearLayout r = Ui.row(a);
        r.addView(b1, Ui.weight(1));
        r.addView(b2, Ui.weight(1));
        box.addView(r);
        final AlertDialog dlg = new AlertDialog.Builder(a).setTitle(L.t("tt_add")).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(xx -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = subj.getText().toString().trim();
            if (s.isEmpty() || t[1] <= t[0]) { Toast.makeText(a, L.t("tt_bad"), Toast.LENGTH_SHORT).show(); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("id", Long.toString(System.currentTimeMillis(), 36));
                o.put("day", day);
                o.put("start", MainActivity.hm(t[0]));
                o.put("end", MainActivity.hm(t[1]));
                o.put("subject", s);
                o.put("room", room.getText().toString().trim());
                a.st.arr("timetable").put(o);
            } catch (JSONException ignored) { }
            a.st.save();
            dlg.dismiss();
            a.render();
        }));
        dlg.show();
    }

    // ---------------------------------------------------------------- attendance

    static int target(Store st) {
        JSONObject s = st.getObj("settings");
        int t = s == null ? 75 : s.optInt("target", 75);
        return t < 1 || t > 100 ? 75 : t;
    }

    static void attendance(final MainActivity a) {
        final int tg = target(a.st);
        a.content.addView(a.primary(L.t("at_add"), v -> a.oneName(L.t("at_add"), L.t("at_sub"), name -> {
            try {
                JSONObject o = new JSONObject();
                o.put("id", Long.toString(System.currentTimeMillis(), 36));
                o.put("subject", name);
                o.put("present", 0);
                o.put("total", 0);
                a.st.arr("attendance").put(o);
            } catch (JSONException ignored) { }
            a.st.save();
            a.render();
        })));
        Button tb = Ui.button(a, String.format(Locale.US, L.t("at_target"), tg), Theme.grey, MainActivity.INK, v -> {
            int n = tg >= 95 ? 60 : tg + 5;
            try {
                JSONObject s = a.st.getObj("settings");
                if (s == null) s = new JSONObject();
                s.put("target", n);
                a.st.put("settings", s);
            } catch (JSONException ignored) { }
            a.st.save();
            a.render();
        });
        LinearLayout.LayoutParams tlp = Ui.match();
        tlp.setMargins(0, 0, 0, Ui.dp(a, 12));
        a.content.addView(tb, tlp);
        final JSONArray arr = a.st.arr("attendance");
        if (arr.length() == 0) { a.content.addView(a.empty(L.t("at_none"))); return; }
        int P = 0, T = 0;
        for (int i = 0; i < arr.length(); i++) { JSONObject o = arr.optJSONObject(i); P += o.optInt("present"); T += o.optInt("total"); }
        LinearLayout sum = Ui.card(a);
        sum.addView(Ui.text(a, L.t("at_over"), 13, MainActivity.MUTED, false));
        sum.addView(Ui.text(a, num(pct(P, T)) + "%  (" + P + "/" + T + ")", 22, MainActivity.INK, true));
        a.content.addView(sum);
        for (int i = 0; i < arr.length(); i++) {
            final JSONObject o = arr.optJSONObject(i);
            final int p = o.optInt("present"), t = o.optInt("total");
            boolean low = t > 0 && pct(p, t) < tg;
            String msg = t == 0 ? "" : (low ? String.format(Locale.US, L.t("at_need"), need(p, t, tg)) : String.format(Locale.US, L.t("at_can"), canMiss(p, t, tg)));
            LinearLayout card = Ui.card(a);
            LinearLayout row = Ui.row(a);
            LinearLayout mid = new LinearLayout(a);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.addView(Ui.text(a, o.optString("subject"), 16, low ? MainActivity.RED : MainActivity.INK, true));
            mid.addView(Ui.text(a, p + " / " + t + (msg.isEmpty() ? "" : "  \u2022  " + msg), 13, MainActivity.MUTED, false));
            row.addView(mid, Ui.weight(1));
            row.addView(Ui.text(a, num(pct(p, t)) + "%", 20, low ? MainActivity.RED : MainActivity.CORAL, true));
            row.addView(x(a, arr, o));
            card.addView(row);
            LinearLayout btns = Ui.row(a);
            Button yes = Ui.button(a, L.t("at_p"), MainActivity.GREEN, Color.WHITE, v -> mark(a, o, true));
            Button no = Ui.button(a, L.t("at_a"), Theme.grey, MainActivity.INK, v -> mark(a, o, false));
            LinearLayout.LayoutParams l1 = Ui.weight(1), l2 = Ui.weight(1);
            l1.setMargins(0, Ui.dp(a, 8), Ui.dp(a, 4), 0);
            l2.setMargins(Ui.dp(a, 4), Ui.dp(a, 8), 0, 0);
            btns.addView(yes, l1);
            btns.addView(no, l2);
            card.addView(btns);
            a.content.addView(card);
        }
    }

    static void mark(MainActivity a, JSONObject o, boolean present) {
        try {
            o.put("total", o.optInt("total") + 1);
            if (present) o.put("present", o.optInt("present") + 1);
        } catch (JSONException ignored) { }
        a.st.save();
        a.render();
    }

    // ---------------------------------------------------------------- marks

    static void marks(final MainActivity a) {
        a.content.addView(a.primary(L.t("mk_add"), v -> addMark(a)));
        final JSONArray arr = a.st.arr("marks");
        if (arr.length() == 0) { a.content.addView(a.empty(L.t("mk_none"))); return; }
        double G = 0, M = 0;
        for (int i = 0; i < arr.length(); i++) { JSONObject o = arr.optJSONObject(i); G += o.optDouble("got"); M += o.optDouble("max"); }
        double cg = cgpa(arr);
        LinearLayout sum = Ui.card(a);
        sum.addView(Ui.text(a, L.t("mk_pct") + ": " + num(M == 0 ? 0 : Math.round(G * 1000 / M) / 10.0) + "%  (" + num(G) + "/" + num(M) + ")", 17, MainActivity.INK, true));
        sum.addView(Ui.text(a, "CGPA: " + (cg < 0 ? "-" : String.format(Locale.US, "%.2f", cg)), 17, MainActivity.CORAL, true));
        a.content.addView(sum);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            double g = o.optDouble("got"), m = o.optDouble("max");
            LinearLayout card = Ui.card(a);
            LinearLayout row = Ui.row(a);
            LinearLayout mid = new LinearLayout(a);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.addView(Ui.text(a, o.optString("subject"), 16, MainActivity.INK, true));
            String extra = "";
            if (!o.isNull("credits") && o.has("credits")) extra += "  \u2022  " + num(o.optDouble("credits")) + " cr";
            if (!o.isNull("gp") && o.has("gp")) extra += "  \u2022  GP " + num(o.optDouble("gp"));
            mid.addView(Ui.text(a, num(g) + " / " + num(m) + extra, 13, MainActivity.MUTED, false));
            row.addView(mid, Ui.weight(1));
            row.addView(Ui.text(a, num(m == 0 ? 0 : Math.round(g * 1000 / m) / 10.0) + "%", 20, MainActivity.CORAL, true));
            row.addView(x(a, arr, o));
            card.addView(row);
            a.content.addView(card);
        }
    }

    static void addMark(final MainActivity a) {
        LinearLayout box = a.dialogBox();
        final EditText subj = a.input(L.t("mk_sub"), false);
        final EditText got = numInput(a, L.t("mk_got"));
        final EditText max = numInput(a, L.t("mk_max"));
        final EditText cr = numInput(a, L.t("mk_cr"));
        final EditText gp = numInput(a, L.t("mk_gp"));
        box.addView(subj);
        box.addView(got);
        box.addView(max);
        box.addView(cr);
        box.addView(gp);
        box.addView(Ui.text(a, L.t("mk_hint"), 12, MainActivity.MUTED, false));
        final AlertDialog dlg = new AlertDialog.Builder(a).setTitle(L.t("mk_add")).setView(box)
                .setPositiveButton(L.t("save"), null).setNegativeButton(L.t("cancel"), null).create();
        dlg.setOnShowListener(xx -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = subj.getText().toString().trim();
            double g = dbl(got), m = dbl(max);
            if (s.isEmpty() || Double.isNaN(g) || Double.isNaN(m) || m <= 0 || g < 0 || g > m) { Toast.makeText(a, L.t("mk_bad"), Toast.LENGTH_SHORT).show(); return; }
            try {
                JSONObject o = new JSONObject();
                o.put("id", Long.toString(System.currentTimeMillis(), 36));
                o.put("subject", s);
                o.put("got", g);
                o.put("max", m);
                double c = dbl(cr), p = dbl(gp);
                o.put("credits", Double.isNaN(c) ? JSONObject.NULL : c);
                o.put("gp", Double.isNaN(p) ? JSONObject.NULL : p);
                a.st.arr("marks").put(o);
            } catch (JSONException ignored) { }
            a.st.save();
            dlg.dismiss();
            a.render();
        }));
        dlg.show();
    }
}

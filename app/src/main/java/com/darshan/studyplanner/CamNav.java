package com.darshan.studyplanner;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** iPhone-camera style mode picker: the selected tab sits in the centre, scroll or swipe and it snaps to the nearest one. */
final class CamNav extends HorizontalScrollView {
    interface Pick { void on(int idx); }
    private final LinearLayout strip;
    private final TextView[] items;
    private final int itemW;
    private int committed = -1, live = -1;
    private boolean touching = false;
    private Pick pick;
    private int accent = 0xFFFF5A5F, muted = 0xFF6B6B76;
    private final Runnable settle = new Runnable() {
        public void run() {
            if (touching) return;
            int i = nearest();
            smoothScrollTo(i * itemW, 0);
            if (i != committed) { committed = i; if (pick != null) pick.on(i); }
        }
    };

    CamNav(Context c, int n, Pick p) {
        super(c);
        pick = p;
        itemW = Ui.dp(c, 92);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        strip = new LinearLayout(c);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        items = new TextView[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            TextView t = new TextView(c);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setOnClickListener(v -> smoothScrollTo(idx * itemW, 0));
            items[i] = t;
            strip.addView(t, new LinearLayout.LayoutParams(itemW, Ui.dp(c, 40)));
        }
        addView(strip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        int pad = Math.max(0, (w - itemW) / 2);
        strip.setPadding(pad, 0, pad, 0);
        final int at = committed < 0 ? 0 : committed;
        post(() -> scrollTo(at * itemW, 0));
    }

    int nearest() {
        int i = Math.round(getScrollX() / (float) itemW);
        return Math.max(0, Math.min(items.length - 1, i));
    }

    void style(String[] labels, int accentColor, int mutedColor) {
        accent = accentColor; muted = mutedColor;
        for (int i = 0; i < items.length; i++) items[i].setText(labels[i]);
        live = -1;
        paint(live < 0 ? Math.max(0, committed) : live);
    }

    private void paint(int sel) {
        live = sel;
        for (int i = 0; i < items.length; i++) {
            TextView t = items[i];
            boolean s = i == sel;
            t.setTextSize(s ? 15 : 13);
            t.setTypeface(s ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            t.setTextColor(s ? 0xFFFFFFFF : muted);
            t.setBackground(s ? Ui.box(getContext(), accent, 20, 0) : null);
            t.setAlpha(s ? 1f : Math.max(0.35f, 1f - 0.25f * Math.abs(i - sel)));
        }
    }

    /** Move to a tab from code (no callback). */
    void select(int idx) {
        committed = idx;
        paint(idx);
        if (Math.abs(getScrollX() - idx * itemW) > 3) smoothScrollTo(idx * itemW, 0);
    }

    @Override protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        int i = nearest();
        if (i != live) paint(i);
        removeCallbacks(settle);
        if (!touching) postDelayed(settle, 110);
    }

    @Override public void fling(int v) { super.fling(v / 3); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) { touching = true; removeCallbacks(settle); }
        boolean r = super.onTouchEvent(e);
        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            touching = false;
            removeCallbacks(settle);
            postDelayed(settle, 120);
        }
        return r;
    }
}

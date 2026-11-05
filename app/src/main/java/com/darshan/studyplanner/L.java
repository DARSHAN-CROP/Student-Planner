package com.darshan.studyplanner;

import java.util.HashMap;
import java.util.Map;

/** Gujarati + English texts. */
final class L {
    private L() {}
    static boolean gu = true;
    private static final Map<String, String[]> M = new HashMap<>();
    private static void p(String k, String g, String e) { M.put(k, new String[]{g, e}); }
    static {
        p("sync_title", "Google sync", "Google sync");
        p("sync_on", "જોડાયેલ: %s", "Signed in: %s");
        p("sync_off", "Google થી sign in કરો તો ફોન અને PC પર એક જ ડેટા દેખાશે.", "Sign in with Google to see the same data on phone and PC.");
        p("sync_signin", "Google થી sign in", "Sign in with Google");
        p("sync_now", "હમણાં sync કરો", "Sync now");
        p("sync_out", "Sign out", "Sign out");
        p("sync_wait", "sync થઈ રહ્યું છે…", "Syncing…");
        p("sync_signed", "Sign in થઈ ગયું ✅", "Signed in ✅");
        p("app", "વિદ્યાર્થી પ્લાનર", "Student Planner");
        p("tab_exams", "પરીક્ષા", "Exams");
        p("tab_syllabus", "અભ્યાસક્રમ", "Syllabus");
        p("tab_timer", "ટાઇમર", "Timer");
        p("tab_notes", "નોંધ", "Notes");
        p("lang_btn", "English", "ગુજરાતી");
        p("add", "ઉમેરો", "Add");
        p("cancel", "રદ કરો", "Cancel");
        p("save", "સાચવો", "Save");
        p("delete", "કાઢી નાખો", "Delete");
        p("yes", "હા", "Yes");
        p("sure", "કાઢી નાખવું છે?", "Delete this?");
        p("add_exam", "પરીક્ષા ઉમેરો", "Add exam");
        p("subject", "વિષયનું નામ", "Subject name");
        p("pick_date", "તારીખ પસંદ કરો", "Pick date");
        p("need_name", "નામ લખો", "Enter a name");
        p("days_left", "%d દિવસ બાકી", "%d days left");
        p("today", "આજે પરીક્ષા!", "Exam today!");
        p("done", "પૂરી થઈ", "Finished");
        p("no_exams", "હજી કોઈ પરીક્ષા નથી. ઉપર પરીક્ષા ઉમેરો.", "No exams yet. Add one above.");
        p("add_subject", "વિષય ઉમેરો", "Add subject");
        p("add_topic", "ટોપિક ઉમેરો", "Add topic");
        p("topic", "ટોપિકનું નામ", "Topic name");
        p("progress", "%d%% પૂર્ણ", "%d%% done");
        p("no_subjects", "હજી કોઈ વિષય નથી. ઉપર વિષય ઉમેરો.", "No subjects yet. Add one above.");
        p("no_topics", "હજી કોઈ ટોપિક નથી.", "No topics yet.");
        p("focus", "અભ્યાસનો સમય", "Focus time");
        p("break", "થોડો આરામ", "Break time");
        p("start", "શરૂ કરો", "Start");
        p("pause", "રોકો", "Pause");
        p("reset", "ફરીથી", "Reset");
        p("sessions", "આજના અભ્યાસ સેશન: %d", "Study sessions today: %d");
        p("timer_note", "ટાઇમર ચાલુ હોય ત્યારે app બંધ ન કરશો.", "Keep the app open while the timer runs.");
        p("focus_done", "શાબાશ! 25 મિનિટ પૂરી. હવે 5 મિનિટ આરામ.", "Well done! 25 minutes finished. Take a 5 minute break.");
        p("break_done", "આરામ પૂરો. ફરી અભ્યાસ શરૂ કરો.", "Break over. Back to studying.");
        p("add_note", "નોંધ ઉમેરો", "Add note");
        p("edit_note", "નોંધ સુધારો", "Edit note");
        p("note_title", "શીર્ષક", "Title");
        p("note_body", "અહીં નોંધ લખો", "Write your note here");
        p("no_notes", "હજી કોઈ નોંધ નથી. ઉપર નોંધ ઉમેરો.", "No notes yet. Add one above.");
        p("untitled", "(શીર્ષક વગર)", "(Untitled)");
        p("tab_homework", "હોમવર્ક", "Homework");
        p("tab_settings", "સેટિંગ્સ", "Settings");
        p("add_hw", "Homework ઉમેરો", "Add homework");
        p("hw_subject", "વિષય", "Subject");
        p("hw_task", "કામ (શું કરવાનું છે)", "Task (what to do)");
        p("no_hw", "હજી કોઈ Homework નથી. ઉપર ઉમેરો.", "No homework yet. Add one above.");
        p("hw_soon", "⚠ %d Homework 24 કલાકમાં પૂરું કરવાનું છે", "⚠ %d homework due within 24 hours");
        p("hrs_left", "%d કલાક બાકી", "%d h left");
        p("overdue", "મોડું થઈ ગયું!", "Overdue!");
        p("hw_finished", "પૂરા થયેલા", "Completed");
        p("hw_need_task", "કામ લખો", "Enter the task");
        p("set_break", "કોલેજ બ્રેક", "College break");
        p("break_start", "બ્રેક શરૂ: %s", "Break starts: %s");
        p("break_len", "બ્રેક કેટલી મિનિટ: %d", "Break length: %d min");
        p("break_len_q", "બ્રેક કેટલી મિનિટનો?", "Break length in minutes?");
        p("campus", "કેમ્પસ (Atmiya University)", "Campus (Atmiya University)");
        p("campus_btn", "આ કેમ્પસ છે (અહીંથી સેટ કરો)", "This is the campus (set from here)");
        p("campus_unset", "હજી કેમ્પસ સેટ નથી. કેમ્પસમાં જઈને બટન દબાવો.", "Campus not set yet. Press the button while you are on campus.");
        p("campus_ok", "કેમ્પસ સેટ છે: %s", "Campus is set: %s");
        p("campus_saved", "કેમ્પસની જગ્યા સચવાઈ", "Campus location saved");
        p("campus_fail", "લોકેશન મળ્યું નથી. GPS ચાલુ કરી ફરી પ્રયત્ન કરો.", "Could not get location. Turn on GPS and try again.");
        p("quiet", "શાંત સમય (આ સમયે રિંગ નહીં)", "Quiet hours (no ring)");
        p("quiet_from", "શરૂ: %s", "From: %s");
        p("quiet_to", "પૂરું: %s", "Until: %s");
        p("perms", "પરવાનગીઓ", "Permissions");
        p("perm_btn", "Notification અને Location ની પરવાનગી", "Notification and Location permission");
        p("perm_bg_msg", "બ્રેક નોટિફિકેશન માટે Location માં \"હંમેશા મંજૂરી આપો\" પસંદ કરો.", "For break notifications choose \"Allow all the time\" for Location.");
        p("alarm_btn", "Alarm ની પરવાનગી", "Alarm permission");
        p("alarm_ok", "Alarm ની પરવાનગી મળેલી છે", "Alarm permission is already allowed");
        p("test_ring", "ટેસ્ટ રિંગ (5 સેકન્ડ)", "Test ring (5 seconds)");
        p("hint_battery", "ટીપ: Battery saver માં આ app ને \"Unrestricted\" રાખો, નહીંતર રિંગ મોડી વાગી શકે.", "Tip: set this app to \"Unrestricted\" in battery settings, or rings can be late.");
        p("ring_title", "Homework બાકી છે!", "Homework is due!");
        p("ring_stop", "બંધ કરો", "Stop");
        p("brk_title", "બ્રેક ટાઇમ", "Break time");
        p("brk_text", "આરામ કરો. %d Homework બાકી છે.", "Take a break. %d homework pending.");
        p("brk_text0", "આરામ કરો, સારું કામ!", "Take a break, good work!");
    }
    static String t(String k) {
        String[] v = M.get(k);
        return v == null ? k : v[gu ? 0 : 1];
    }
}

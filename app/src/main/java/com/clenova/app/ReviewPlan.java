package com.clenova.app;

import java.util.Locale;

/** Sample selection state only. Amounts use tenths of a GB to avoid rounding drift. */
final class ReviewPlan {
    static final String[] NAMES = {"Duplicate photos", "Similar photos", "Large videos"};
    static final String[] DETAILS = {"48 extra copies", "80 photos", "12 videos"};
    static final String[] ICONS = {"copy", "images", "video"};
    static final int[] TENTHS = {12, 10, 26};
    static final int[] COUNTS = {48, 80, 12};
    private final boolean[] selected = {true, false, false};
    boolean isSelected(int i) { return selected[i]; }
    void setSelected(int i, boolean value) { selected[i] = value; }
    int total() {
        int total = 0;
        for (int i = 0; i < selected.length; i++) if (selected[i]) total += TENTHS[i];
        return total;
    }
    int count() {
        int count = 0;
        for (int i = 0; i < selected.length; i++) if (selected[i]) count += COUNTS[i];
        return count;
    }
    static String gb(int tenths) { return String.format(Locale.US, "%.1f GB", tenths / 10.0); }
}

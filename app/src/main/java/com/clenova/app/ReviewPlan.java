package com.clenova.app;

import java.math.BigDecimal;
import java.util.Arrays;

/** Design-only sample data, in decimal MB. Originals are never selectable. */
final class ReviewPlan {
    static final String[] NAMES = {"Duplicate photos", "Similar photos", "Large videos"};
    static final String[] DETAILS = {"48 extra copies", "80 photos", "12 videos"};
    static final String[] ICONS = {"copy", "images", "video"};
    static final int[] MB = {1200, 1000, 2600};
    static final int[] COUNTS = {48, 80, 12};
    static final int GROUPS = 24;
    static final int COPY_MB = 25;
    private boolean[] copies = new boolean[GROUPS * 2];
    private final boolean[] selected = {true, false, false};
    ReviewPlan() { Arrays.fill(copies,true); }
    boolean[] draftCopies() { return copies.clone(); }
    void confirmCopies(boolean[] draft) {
        if (draft.length != GROUPS * 2) throw new IllegalArgumentException("Invalid selection");
        copies = draft.clone();
    }
    static int selectedCount(boolean[] values) {
        int count=0; for(boolean value:values) if(value) count++; return count;
    }
    boolean isSelected(int i) { return i==0 ? selectedCount(copies)>0 : selected[i]; }
    void setSelected(int i, boolean value) {
        if(i==0) Arrays.fill(copies,value); else selected[i]=value;
    }
    int categoryMb(int i) { return i==0 ? selectedCount(copies)*COPY_MB : selected[i]?MB[i]:0; }
    int total() { return categoryMb(0)+categoryMb(1)+categoryMb(2); }
    int count() { return selectedCount(copies)+(selected[1]?COUNTS[1]:0)+(selected[2]?COUNTS[2]:0); }
    static String gb(int mb) {
        if(mb<1000) return mb+" MB";
        BigDecimal value=BigDecimal.valueOf(mb,3).stripTrailingZeros();
        return (value.scale()<1?value.setScale(1):value).toPlainString()+" GB";
    }
}

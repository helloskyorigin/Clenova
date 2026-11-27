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
    static final int SIMILAR_GROUPS=32;
    static final int SIMILAR_COUNT=80;
    static final double SIMILAR_MB=12.5;
    private boolean[] similar=new boolean[SIMILAR_COUNT];
    private boolean similarReviewed;
    static final int[] VIDEO_MB={480,360,320,280,250,220,180,150,120,100,80,60};
    static final String[] VIDEO_DURATION={"12:48","08:24","06:12","04:38","03:25","05:12","03:56","02:41","02:18","01:54","01:26","01:08"};
    private boolean[] videos=new boolean[VIDEO_MB.length];
    private boolean videosReviewed;
    boolean[] draftVideos() {
        boolean[] draft=videos.clone();
        if(!videosReviewed)Arrays.fill(draft,true);
        return draft;
    }
    void confirmVideos(boolean[] draft) {
        if(draft.length!=VIDEO_MB.length)throw new IllegalArgumentException("Invalid selection");
        videos=draft.clone();videosReviewed=true;
    }
    static int videoMb(boolean[] draft) {
        if(draft.length!=VIDEO_MB.length)throw new IllegalArgumentException("Invalid selection");
        int total=0;for(int i=0;i<draft.length;i++)if(draft[i])total+=VIDEO_MB[i];
        return total;
    }
    boolean[] draftSimilar() {
        boolean[] draft=similar.clone();
        if(!similarReviewed) Arrays.fill(draft,true);
        return draft;
    }
    void confirmSimilar(boolean[] draft) {
        if(draft.length!=SIMILAR_COUNT)throw new IllegalArgumentException("Invalid selection");
        similar=draft.clone();similarReviewed=true;
    }
    static int similarIndex(int group,int photo) {
        if(group<0||group>=SIMILAR_GROUPS||photo<1||photo>=(group<16?4:3))throw new IllegalArgumentException("Not a selectable photo");
        return (group<16?group*3:48+(group-16)*2)+photo-1;
    }
    ReviewPlan() { Arrays.fill(copies,true); }
    boolean[] draftCopies() { return copies.clone(); }
    void confirmCopies(boolean[] draft) {
        if (draft.length != GROUPS * 2) throw new IllegalArgumentException("Invalid selection");
        copies = draft.clone();
    }
    static int selectedCount(boolean[] values) {
        int count=0; for(boolean value:values) if(value) count++; return count;
    }
    boolean isSelected(int i) { return i==0 ? selectedCount(copies)>0 : i==1 ? selectedCount(similar)>0 : selectedCount(videos)>0; }
    void setSelected(int i, boolean value) {
        if(i==0) Arrays.fill(copies,value); else if(i==1){if(value&&!similarReviewed)throw new IllegalStateException("Review similar photos first");Arrays.fill(similar,value);} else {if(value&&!videosReviewed)throw new IllegalStateException("Review videos first");Arrays.fill(videos,value);}
    }
    double categoryMb(int i) { return i==0 ? selectedCount(copies)*COPY_MB : i==1 ? selectedCount(similar)*SIMILAR_MB : videoMb(videos); }
    double total() { return categoryMb(0)+categoryMb(1)+categoryMb(2); }
    int count() { return selectedCount(copies)+selectedCount(similar)+selectedCount(videos); }
    static String gb(double mb) {
        BigDecimal amount=BigDecimal.valueOf(mb).stripTrailingZeros();
        if(mb<1000) return amount.toPlainString()+" MB";
        BigDecimal value=amount.movePointLeft(3).stripTrailingZeros();
        return (value.scale()<1?value.setScale(1):value).toPlainString()+" GB";
    }
}

package com.clenova.app;

/** Preparation progress counts completed metadata stages, not elapsed time or storage savings. */
final class PreparationPolicy {
    static final int STAGES=5;
    static final long COMPLETE_DWELL_MS=420;
    static int stage(int completed){return Math.max(0,Math.min(STAGES,completed));}
    static int percent(int completed){return stage(completed)*100/STAGES;}
    static int rowState(int completed,int row,boolean failed){
        if(row<0||row>=STAGES)throw new IllegalArgumentException("row");
        int n=stage(completed);return row<n?2:row==n&&!failed?1:0;
    }
    private PreparationPolicy(){}
}

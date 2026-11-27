package com.clenova.app;

/** Pure scheduling and access rules. Selected-only grants are foreground-only. */
final class AutopilotPolicy {
    static final long DAY=86400000L;
    static final int DUPLICATES=1, SIMILAR=2, VIDEOS=4;
    static int eligible(int requested,int mediaMask){
        return requested & (((mediaMask&1)!=0?DUPLICATES|SIMILAR:0)|((mediaMask&2)!=0?VIDEOS:0));
    }
    static long interval(int days){return (days==3||days==7?days:1)*DAY;}
    static boolean fresh(long checked,long now,int days){
        return checked>0&&now>=checked&&now-checked<=2*interval(days);
    }
    private AutopilotPolicy(){}
}

package com.clenova.app;

/** Presentation helpers for observed device metadata. No device operations or inferred savings. */
final class HomePresentation {
    static final int BAR_MAX=1000;
    static int progress(long total,long free){
        if(total<=0||free<0||free>total)return -1;
        return (int)Math.round((total-free)*(double)BAR_MAX/total);
    }
    static int columns(int widthDp,float fontScale){return widthDp<340||fontScale>1.15f?1:2;}
    static String status(boolean known,boolean finished,boolean partial){
        if(!finished)return "Reading device data";
        if(!known)return "Storage unavailable · Try again later";
        return partial?"Limited overview · Some data unavailable":"Read from your device";
    }
    private HomePresentation(){}
}

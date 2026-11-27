package com.clenova.app;

/** Shared readable palette; preserve the electric-blue accent while darkening filled CTAs. */
final class UiPolish {
    static final int LIGHT_MUTED=0xff626269;
    static final int CTA_BLUE=0xff0060ce;
    static int actionFill(int fill){return fill==0xff007aff||fill==0xff409cff?CTA_BLUE:fill;}
    static int buttonFill(int fill,int text){return text==0xffffffff?actionFill(fill):fill;}
    static double luminance(int color){
        return .2126*linear((color>>>16)&255)+.7152*linear((color>>>8)&255)+.0722*linear(color&255);
    }
    private static double linear(int value){double v=value/255d;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private UiPolish(){}
}

package com.clenova.app;

/** Pure design tokens shared by native screens. Brand blue stays distinct from readable action colors. */
final class UiPolish {
    static final int BRAND_BLUE=0xff007aff, CTA_BLUE=0xff0060ce;
    static final int LIGHT_MUTED=0xff626269;
    static final int CARD_RADIUS=20, BUTTON_RADIUS=24, CARD_PAD_X=16, CARD_PAD_Y=14;
    static final int ICON_TARGET=48, ICON_SIZE=22, HEADER_HEIGHT=56, HEADER_GAP=8;
    static final int FAST_MOTION_MS=140, STANDARD_MOTION_MS=220;
    static final class Palette {
        final int background,card,ink,muted,accent,tint,success,danger,divider,border,ripple;
        Palette(boolean dark){
            background=dark?0xff141416:0xfff5f5f7;
            card=dark?0xff242426:0xffffffff;
            ink=dark?0xfff4f4f6:0xff111111;
            muted=dark?0xffa2a2aa:LIGHT_MUTED;
            accent=dark?0xff409cff:0xff0064d2;
            tint=dark?0xff19334c:0xffe6f0ff;
            success=dark?0xff61d883:0xff187344;
            danger=dark?0xffff8078:0xffb82d28;
            divider=dark?0xff38383c:0xffe5e5ea;
            border=dark?0xff343438:0xffe8e8ed;
            ripple=dark?0x26ffffff:0x19000000;
        }
    }
    static final Palette LIGHT=new Palette(false), DARK=new Palette(true);
    static Palette palette(boolean dark){return dark?DARK:LIGHT;}
    static boolean dark(String preference,boolean systemDark){return "Dark".equals(preference)||("System".equals(preference)&&systemDark);}
    static int gutter(int widthDp){return widthDp>=600?24:widthDp<360?16:18;}
    static int actionFill(int fill){
        if(fill==BRAND_BLUE||fill==LIGHT.accent||fill==DARK.accent)return CTA_BLUE;
        if(fill==0xffe43e35||fill==0xffff453a||fill==0xffff6961||fill==DARK.danger||fill==0xffd92d20)return LIGHT.danger;
        return fill;
    }
    static int buttonFill(int fill,int text){return text==0xffffffff?actionFill(fill):fill;}
    static int fontWeight(int value){return Math.max(400,Math.min(700,((value+50)/100)*100));}
    static double luminance(int color){
        return .2126*linear((color>>>16)&255)+.7152*linear((color>>>8)&255)+.0722*linear(color&255);
    }
    private static double linear(int value){double v=value/255d;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private UiPolish(){}
}

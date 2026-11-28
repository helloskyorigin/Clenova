package com.clenova.app;
public final class UiPolishTest {
    static int count;
    private static void check(boolean b){count++;if(!b)throw new AssertionError("Assertion "+count);}
    public static void main(String[] args){
        for(boolean dark:new boolean[]{false,true}){
            UiPolish.Palette p=UiPolish.palette(dark);
            for(int surface:new int[]{p.background,p.card})for(int color:new int[]{p.ink,p.muted,p.accent,p.success,p.danger})check(UiPolish.contrast(color,surface)>=4.5);
            check(UiPolish.contrast(p.accent,p.tint)>=4.5);
        }
        check(UiPolish.contrast(UiPolish.CTA_BLUE,0xffffffff)>=4.5);
        check(UiPolish.contrast(UiPolish.LIGHT.danger,0xffffffff)>=4.5);
        check(UiPolish.actionFill(UiPolish.BRAND_BLUE)==UiPolish.CTA_BLUE);
        check(UiPolish.actionFill(UiPolish.LIGHT.accent)==UiPolish.CTA_BLUE);
        check(UiPolish.actionFill(UiPolish.DARK.accent)==UiPolish.CTA_BLUE);
        check(UiPolish.actionFill(UiPolish.DARK.danger)==UiPolish.LIGHT.danger);
        check(UiPolish.buttonFill(UiPolish.DARK.accent,0xff111111)==UiPolish.DARK.accent);
        check(UiPolish.actionFill(UiPolish.DARK.tint)==UiPolish.DARK.tint);
        check(UiPolish.dark("Dark",false));check(!UiPolish.dark("Light",true));check(UiPolish.dark("System",true));check(!UiPolish.dark("System",false));
        check(UiPolish.gutter(320)==16);check(UiPolish.gutter(393)==18);check(UiPolish.gutter(600)==24);
        check(UiPolish.fontWeight(500)==500);check(UiPolish.fontWeight(650)==700);check(UiPolish.fontWeight(100)==400);
        check(UiPolish.ICON_TARGET>=48);check(UiPolish.FAST_MOTION_MS<UiPolish.STANDARD_MOTION_MS);
        System.out.println("UI foundation: "+count+" assertions passed");
    }
}

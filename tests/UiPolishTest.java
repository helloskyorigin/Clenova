package com.clenova.app;
public final class UiPolishTest {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check(UiPolish.contrast(UiPolish.LIGHT_MUTED,0xfff5f5f7)>=4.5);
        check(UiPolish.contrast(UiPolish.LIGHT_MUTED,0xffffffff)>=4.5);
        check(UiPolish.contrast(UiPolish.CTA_BLUE,0xffffffff)>=4.5);
        check(UiPolish.contrast(0xffa2a2aa,0xff242426)>=4.5);
        check(UiPolish.actionFill(0xff007aff)==UiPolish.CTA_BLUE);
        check(UiPolish.actionFill(0xff409cff)==UiPolish.CTA_BLUE);
        check(UiPolish.buttonFill(0xff409cff,0xff111111)==0xff409cff);
        check(UiPolish.actionFill(0xff19334c)==0xff19334c);
        System.out.println("UI palette: 8 assertions passed");
    }
}

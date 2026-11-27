package com.clenova.app;
public final class ClutterRulesTest {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check(ClutterRules.category("x.TMP","application/octet-stream")==ClutterRules.TEMP);
        check(ClutterRules.category("x.crdownload","application/octet-stream")==ClutterRules.INCOMPLETE);
        check(ClutterRules.category("x.apk","application/vnd.android.package-archive")==ClutterRules.INSTALLER);
        check(ClutterRules.category("x.log","text/plain")==ClutterRules.LOG);
        check(ClutterRules.category("family.jpg","image/jpeg")==-1);
        check(ClutterRules.category("x.tmp","image/jpeg")==-1);
        check(ClutterRules.category("report.pdf","application/pdf")==-1);
        check(!ClutterRules.oldEnough(0,1000000000L));
        check(!ClutterRules.oldEnough(1000000000L,1000000000L));
        check(ClutterRules.oldEnough(1,ClutterRules.MIN_AGE+1));
        check(!ClutterRules.removalType(ClutterRules.LOG));
        check(!ClutterRules.removalType(ClutterRules.INCOMPLETE));
        check(ClutterRules.removalType(ClutterRules.TEMP));
        System.out.println("Clutter rules: 13 assertions passed");
    }
}

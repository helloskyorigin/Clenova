package com.clenova.app;
import java.util.Locale;

/** Filename-based review hints, never proof that a file is safe to delete. */
final class ClutterRules {
    static final int TEMP=0,INCOMPLETE=1,INSTALLER=2,LOG=3;
    static final String[] LABELS={"Temporary files","Incomplete downloads","APK installers","Diagnostic logs"};
    static final String[] REASONS={"Temporary-looking filename","Incomplete-download extension","Downloaded APK package","Log-looking filename"};
    static final long MIN_AGE=7L*86400000L;
    static int category(String name,String mime){
        if(name==null||mime==null)return -1;
        String n=name.toLowerCase(Locale.ROOT),m=mime.toLowerCase(Locale.ROOT);
        if(m.startsWith("image/")||m.startsWith("video/")||m.startsWith("audio/"))return -1;
        if(n.endsWith(".crdownload")||n.endsWith(".part")||n.endsWith(".partial")||n.endsWith(".download"))return INCOMPLETE;
        if(n.endsWith(".apk")&&(m.equals("application/vnd.android.package-archive")||m.equals("application/octet-stream")))return INSTALLER;
        if((n.endsWith(".tmp")||n.endsWith(".temp"))&&(m.equals("application/octet-stream")||m.equals("application/x-temp")))return TEMP;
        if(n.endsWith(".log")&&(m.startsWith("text/")||m.equals("application/octet-stream")))return LOG;
        return -1;
    }
    static boolean oldEnough(long modified,long now){return modified>0&&now-modified>=MIN_AGE;}
    // Incomplete downloads and logs can still be active or important, so never remove them here.
    static boolean removalType(int category){return category==TEMP||category==INSTALLER;}
}

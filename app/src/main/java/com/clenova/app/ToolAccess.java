package com.clenova.app;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.util.ArrayList;

/** Explicit feature-scoped permission requests; permission state always comes from Android. */
final class ToolAccess {
    static final int PHOTOS=1,VIDEOS=2,MEDIA=3,CONTACTS=4,CONTACT_WRITE=5;
    private static SharedPreferences flags(Context c){return c.getSharedPreferences("clenova_permission_requests",Context.MODE_PRIVATE);}
    private static String key(String name){return "asked:"+name;}
    static boolean granted(Context c,String p){return c.checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED;}
    static String[] primary(int scope){
        if(scope==CONTACTS)return new String[]{Manifest.permission.READ_CONTACTS};
        if(scope==CONTACT_WRITE)return new String[]{Manifest.permission.WRITE_CONTACTS};
        if(scope<PHOTOS||scope>MEDIA)throw new IllegalArgumentException("Unknown scope");
        if(Build.VERSION.SDK_INT<33)return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        if(scope==PHOTOS)return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        if(scope==VIDEOS)return new String[]{Manifest.permission.READ_MEDIA_VIDEO};
        return new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO};
    }
    static String[] requestNames(int scope){
        String[] primary=primary(scope);
        if(scope<=MEDIA&&Build.VERSION.SDK_INT>=34){
            String[] names=java.util.Arrays.copyOf(primary,primary.length+1);names[primary.length]=Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED;return names;
        }return primary;
    }
    static boolean has(Context c,int scope){
        if(scope==CONTACTS||scope==CONTACT_WRITE)return granted(c,primary(scope)[0]);
        return scope==PHOTOS?SmartMediaScan.hasPhotoAccess(c):scope==VIDEOS?SmartMediaScan.hasVideoAccess(c):scope==MEDIA&&SmartMediaScan.hasAccess(c);
    }
    static int mediaMask(Context c){
        if(Build.VERSION.SDK_INT<33)return granted(c,Manifest.permission.READ_EXTERNAL_STORAGE)?3:0;
        return (granted(c,Manifest.permission.READ_MEDIA_IMAGES)?1:0)|(granted(c,Manifest.permission.READ_MEDIA_VIDEO)?2:0)
            |(Build.VERSION.SDK_INT>=34&&granted(c,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)?4:0);
    }
    static void remember(Context c,String[] names){SharedPreferences.Editor e=flags(c).edit();for(String name:names)e.putBoolean(key(name),true);e.apply();}
    static boolean settingsNeeded(Activity a,int scope){
        String[] names=primary(scope);boolean[] g=new boolean[names.length],asked=new boolean[names.length],r=new boolean[names.length];
        for(int i=0;i<names.length;i++){g[i]=granted(a,names[i]);asked[i]=flags(a).getBoolean(key(names[i]),false);r[i]=a.shouldShowRequestPermissionRationale(names[i]);}
        return AccessDecision.settings(g,asked,r);
    }
    static String action(Activity a,int scope){return settingsNeeded(a,scope)?"Open Android settings":"Allow access";}
    static boolean openSettings(Activity a,int request){
        if(a.isFinishing()||a.isDestroyed())return false;
        try{a.startActivityForResult(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",a.getPackageName(),null)),request);return true;}
        catch(RuntimeException ex){return false;}
    }
    static boolean launch(Activity a,int scope,int request){
        if(a.isFinishing()||a.isDestroyed())return false;
        if(settingsNeeded(a,scope))return openSettings(a,request);
        ArrayList<String> names=new ArrayList<>();
        // Android 14 selection permission accompanies the media request, including reselection.
        for(String name:requestNames(scope))if(!granted(a,name)||Build.VERSION.SDK_INT>=34&&name.equals(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))names.add(name);
        if(names.isEmpty())return false;
        String[] requested=names.toArray(new String[0]);
        try{remember(a,requested);a.requestPermissions(requested,request);return true;}catch(RuntimeException ex){return false;}
    }
    private ToolAccess(){}
}

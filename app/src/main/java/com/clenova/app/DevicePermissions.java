package com.clenova.app;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

/** Read-only OS access snapshot; never requests permissions by itself. */
final class DevicePermissions {
    private final Context context;
    DevicePermissions(Context context){this.context=context.getApplicationContext();}
    static final class Snapshot {
        final String media,storage,notifications,contacts,background;
        Snapshot(String media,String storage,String notifications,String contacts,String background){
            this.media=media;this.storage=storage;this.notifications=notifications;this.contacts=contacts;this.background=background;
        }
    }
    boolean granted(String permission){return context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;}
    private boolean declared(String permission){
        try{
            String[] names=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions;
            if(names!=null)for(String name:names)if(permission.equals(name))return true;
        }catch(PackageManager.NameNotFoundException ignored){}
        return false;
    }
    private String media(){
        if(Build.VERSION.SDK_INT>=33){
            boolean images=granted(Manifest.permission.READ_MEDIA_IMAGES),videos=granted(Manifest.permission.READ_MEDIA_VIDEO);
            boolean selected=Build.VERSION.SDK_INT>=34&&granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            if(images&&videos)return "Allowed";
            if(images||videos||selected)return "Limited";
            return declared(Manifest.permission.READ_MEDIA_IMAGES)||declared(Manifest.permission.READ_MEDIA_VIDEO)?"Not allowed":"Not requested";
        }
        return granted(Manifest.permission.READ_EXTERNAL_STORAGE)?"Allowed":declared(Manifest.permission.READ_EXTERNAL_STORAGE)?"Not allowed":"Not requested";
    }
    private String storage(){
        if(Build.VERSION.SDK_INT>=30&&Environment.isExternalStorageManager())return "Allowed";
        return new SetupFolderAccess(context).current()!=null?"Selected folder":"Picker only";
    }
    private String notifications(){
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager==null)return "Unavailable";
        if(Build.VERSION.SDK_INT>=33&&!granted(Manifest.permission.POST_NOTIFICATIONS))return "Not allowed";
        if(!manager.areNotificationsEnabled())return "Not allowed";
        boolean blocked=false;
        for(String key:AppPreferences.NOTIFICATION_KEYS){
            android.app.NotificationChannel channel=manager.getNotificationChannel(key);
            if(channel!=null&&channel.getImportance()==NotificationManager.IMPORTANCE_NONE)blocked=true;
        }
        return blocked?"Limited":"Allowed";
    }
    private String contacts(){
        return granted(Manifest.permission.READ_CONTACTS)?"Allowed":declared(Manifest.permission.READ_CONTACTS)?"Not allowed":"Not requested";
    }
    private String background(){
        if(Build.VERSION.SDK_INT<28)return "Manage";
        ActivityManager manager=context.getSystemService(ActivityManager.class);
        return manager==null?"Unavailable":manager.isBackgroundRestricted()?"Restricted":"Not restricted";
    }
    Snapshot snapshot(){return new Snapshot(media(),storage(),notifications(),contacts(),background());}
}

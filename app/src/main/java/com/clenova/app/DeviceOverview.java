package com.clenova.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.OperationCanceledException;
import android.os.StatFs;
import android.provider.MediaStore;
import java.util.HashSet;
import java.util.Locale;

/** Bounded local metadata inventory. No file contents, network, deletion or broad storage access. */
final class DeviceOverview {
    static final int STAGES=5, LIMIT=10000;
    interface Progress { void update(int completed,Snapshot snapshot); }
    static final class Media {
        final long bytes;final int count;final boolean partial,available;
        Media(long b,int c,boolean p,boolean a){bytes=b;count=c;partial=p;available=a;}
        static Media unavailable(){return new Media(0,0,false,false);}
        String size(){return available?(partial?"At least ":"")+format(bytes):"Not available";}
        String items(){return available?(partial?"At least ":"")+count+" items":"Access unavailable";}
    }
    static final class Snapshot {
        long total=-1,free=-1;
        Media photos=Media.unavailable(),videos=Media.unavailable();
        int apps=-1;
        boolean usage,folder,agent,limited,finished;
        String mediaAccess="Not checked";
        long checked;
        Snapshot(){}
        Snapshot(Snapshot s){total=s.total;free=s.free;photos=s.photos;videos=s.videos;apps=s.apps;usage=s.usage;folder=s.folder;agent=s.agent;limited=s.limited;finished=s.finished;mediaAccess=s.mediaAccess;checked=s.checked;}
        boolean storageKnown(){return total>0&&free>=0&&free<=total;}
        long used(){return storageKnown()?total-free:-1;}
        int usedPercent(){return storageKnown()?(int)Math.round(100d*used()/total):-1;}
        // Only primary shared-volume media on emulated internal storage are comparable to /data.
        boolean breakdownKnown(){return storageKnown()&&photos.bytes<=used()&&videos.bytes<=used()-photos.bytes;}
        long other(){return breakdownKnown()?used()-photos.bytes-videos.bytes:-1;}
        long[] segments(){if(!storageKnown())return new long[]{0,0,0,0};if(!breakdownKnown())return new long[]{0,0,used(),free};return new long[]{photos.bytes,videos.bytes,other(),free};}
        boolean partial(){return !storageKnown()||limited||!photos.available||!videos.available||photos.partial||videos.partial||apps<0||!breakdownKnown();}
    }
    private final Context context;private final AppPreferences prefs;
    private final CancellationSignal signal=new CancellationSignal();
    DeviceOverview(Context c,AppPreferences prefs){context=c.getApplicationContext();this.prefs=prefs;}
    void cancel(){signal.cancel();}
    private void check(){signal.throwIfCanceled();}
    private boolean granted(String name){return context.checkSelfPermission(name)==PackageManager.PERMISSION_GRANTED;}
    private boolean full(boolean image){return Build.VERSION.SDK_INT>=33?granted(image?Manifest.permission.READ_MEDIA_IMAGES:Manifest.permission.READ_MEDIA_VIDEO):granted(Manifest.permission.READ_EXTERNAL_STORAGE);}
    private String access(){return new DevicePermissions(context).snapshot().media;}
    Snapshot run(Progress progress){
        Snapshot result=new Snapshot();
        result.mediaAccess=access();result.limited=!"Allowed".equals(result.mediaAccess);result.agent=prefs.autopilot();
        result.usage=UnusedAppScan.allowed(context);result.folder=new SetupFolderAccess(context).current()!=null;publish(progress,1,result);
        readStorage(result);publish(progress,2,result);
        result.photos=readMedia(true);publish(progress,3,result);
        result.videos=readMedia(false);publish(progress,4,result);
        result.apps=launcherCount();
        // Selected access may change even while the Activity is alive. Discard aggregates on change.
        if(!result.mediaAccess.equals(access())||!SmartMediaScan.hasPhotoAccess(context))result.photos=Media.unavailable();
        if(!result.mediaAccess.equals(access())||!SmartMediaScan.hasVideoAccess(context))result.videos=Media.unavailable();
        result.mediaAccess=access();result.limited=!"Allowed".equals(result.mediaAccess);
        readStorage(result);result.checked=System.currentTimeMillis();result.finished=true;publish(progress,5,result);return new Snapshot(result);
    }
    private void publish(Progress callback,int stage,Snapshot snapshot){check();callback.update(stage,new Snapshot(snapshot));}
    private void readStorage(Snapshot s){
        try{StatFs stat=new StatFs(Environment.getDataDirectory().getAbsolutePath());long total=stat.getTotalBytes(),free=stat.getAvailableBytes();if(total>0&&free>=0&&free<=total){s.total=total;s.free=free;return;}}catch(RuntimeException ignored){}
        s.total=-1;s.free=-1;
    }
    private Media readMedia(boolean image){
        check();
        if(!(image?SmartMediaScan.hasPhotoAccess(context):SmartMediaScan.hasVideoAccess(context)))return Media.unavailable();
        // Do not mix removable-card bytes with internal filesystem totals.
        if(!Environment.isExternalStorageEmulated())return Media.unavailable();
        Uri uri=Build.VERSION.SDK_INT>=29?(image?MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY):MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)):(image?MediaStore.Images.Media.EXTERNAL_CONTENT_URI:MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        long bytes=0;int count=0;boolean partial=!full(image);
        try(Cursor c=context.getContentResolver().query(uri,new String[]{"_size"},null,null,null,signal)){
            if(c==null)return Media.unavailable();
            while(c.moveToNext()){
                check();if(count>=LIMIT){partial=true;break;}count++;
                if(c.isNull(0)||c.getLong(0)<0){partial=true;continue;}
                long n=c.getLong(0);if(n>Long.MAX_VALUE-bytes){partial=true;break;}bytes+=n;
            }
        }catch(OperationCanceledException e){throw e;}catch(RuntimeException e){return Media.unavailable();}
        if(!(image?SmartMediaScan.hasPhotoAccess(context):SmartMediaScan.hasVideoAccess(context)))return Media.unavailable();
        return new Media(bytes,count,partial||!full(image),true);
    }
    private int launcherCount(){
        check();try{HashSet<String> packages=new HashSet<>();
            for(ResolveInfo info:context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
                check();if(info.activityInfo!=null)packages.add(info.activityInfo.packageName);
            }return packages.size();
        }catch(OperationCanceledException e){throw e;}catch(RuntimeException e){return -1;}
    }
    static String format(long bytes){if(bytes<0)return "Unavailable";if(bytes>=1000000000L)return String.format(Locale.US,"%.1f GB",bytes/1000000000d);if(bytes>=1000000L)return String.format(Locale.US,"%.1f MB",bytes/1000000d);if(bytes>=1000)return String.format(Locale.US,"%.1f KB",bytes/1000d);return bytes+" B";}
}

package com.clenova.app;

import android.content.*;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.effect.Presentation;
import androidx.media3.transformer.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Local encoding and verified copy publication. Never writes to the source. */
@UnstableApi
final class VideoCompressionEngine {
    enum Preset {
        HIGH("High quality","Preserve more detail",1080,8000000,.75),
        BALANCED("Balanced","Recommended for everyday videos",720,3000000,.5),
        SMALL("Maximum compression","Smallest target file",480,1200000,.3);
        final String label,detail;final int height,cap;final double ratio;
        Preset(String l,String d,int h,int c,double r){label=l;detail=d;height=h;cap=c;ratio=r;}
        int targetHeight(Source s){return Math.max(2,Math.min(s.height,height)/2*2);}
        int bitrate(Source s){return (int)Math.max(32000,Math.min(cap,Math.min(s.bitrate*.95,s.bitrate*ratio)));}
        long estimate(Source s){return Math.max(1,Math.round((bitrate(s)+(s.audio?128000:0))*s.duration/8000d*1.03));}
    }
    static final class Source {
        final Uri uri;final String name;final long bytes,duration,bitrate;final int width,height;final boolean audio;
        Source(Uri u,String n,long b,long d,long rate,int w,int h,boolean a){uri=u;name=n;bytes=b;duration=d;bitrate=rate;width=w;height=h;audio=a;}
    }
    static final class Output {
        final File file;final Source meta;final byte[] digest;
        Output(File f,Source m,byte[] d){file=f;meta=m;digest=d;}
    }
    static Source inspect(Context c,Uri uri)throws Exception{
        String name="Video";long bytes=-1;
        if("file".equals(uri.getScheme())){File f=new File(uri.getPath());name=f.getName();bytes=f.length();}
        else {
            try(Cursor cur=c.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
                if(cur!=null&&cur.moveToFirst()){if(!cur.isNull(0))name=cur.getString(0);if(!cur.isNull(1))bytes=cur.getLong(1);}
            }catch(RuntimeException ignored){}
            if(bytes<=0)try(android.os.ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r")){if(fd!=null)bytes=fd.getStatSize();}
        }
        if(bytes<=0)throw new IOException("Unknown file size");
        MediaMetadataRetriever mm=new MediaMetadataRetriever();
        try {
            if("file".equals(uri.getScheme()))mm.setDataSource(uri.getPath());else mm.setDataSource(c,uri);
            long duration=num(mm,MediaMetadataRetriever.METADATA_KEY_DURATION);
            int w=(int)num(mm,MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),h=(int)num(mm,MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            long rotation=num(mm,MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if(rotation==90||rotation==270){int swap=w;w=h;h=swap;}
            long bitrate=num(mm,MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if(duration<=0||w<2||h<2)throw new IOException("Invalid video");
            if(bitrate<=0)bitrate=Math.max(1,Math.round(bytes*8000d/duration));
            return new Source(uri,name,bytes,duration,bitrate,w,h,"yes".equals(mm.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)));
        }finally{mm.release();}
    }
    private static long num(MediaMetadataRetriever r,int key){try{return Long.parseLong(r.extractMetadata(key));}catch(Exception e){return -1;}}
    static BitmapResult thumb(Context c,Uri uri,int max){
        MediaMetadataRetriever r=new MediaMetadataRetriever();
        try{r.setDataSource(c,uri);android.graphics.Bitmap bitmap=Build.VERSION.SDK_INT>=27?r.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,max,max*9/16):null;
            return new BitmapResult(bitmap);
        }catch(Exception e){return new BitmapResult(null);}finally{try{r.release();}catch(Exception ignored){}}
    }
    static final class BitmapResult{final android.graphics.Bitmap bitmap;BitmapResult(android.graphics.Bitmap b){bitmap=b;}}
    static Transformer transformer(Context c,Source s,Preset preset,Transformer.Listener listener){
        DefaultEncoderFactory encoder=new DefaultEncoderFactory.Builder(c)
            .setRequestedVideoEncoderSettings(new VideoEncoderSettings.Builder().setBitrate(preset.bitrate(s)).build()).build();
        return new Transformer.Builder(c).setVideoMimeType(MimeTypes.VIDEO_H264).setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoder).addListener(listener).build();
    }
    static EditedMediaItem edited(Source s,Preset preset){
        Effects effects=new Effects(Collections.emptyList(),Collections.singletonList(Presentation.createForHeight(preset.targetHeight(s))));
        return new EditedMediaItem.Builder(MediaItem.fromUri(s.uri)).setEffects(effects).build();
    }
    static Output validate(Context c,Source original,File file)throws Exception{
        Source result=inspect(c,Uri.fromFile(file));
        if(result.bytes<=0||Math.abs(result.duration-original.duration)>Math.max(1000,Math.min(3000,original.duration/100))||(original.audio&&!result.audio))
            throw new IOException("Output duration or audio validation failed");
        try(InputStream in=new FileInputStream(file)){return new Output(file,result,digest(in));}
    }
    private static void interrupted()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");}
    private static byte[] digest(InputStream input)throws Exception{
        MessageDigest hash=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[128*1024];int n;
        while((n=input.read(buf))!=-1){interrupted();hash.update(buf,0,n);}return hash.digest();
    }
    static boolean matches(Context c,Uri uri,Output result)throws Exception{
        if(result==null||uri==null)return false;
        try(InputStream in=c.getContentResolver().openInputStream(uri)){return in!=null&&MessageDigest.isEqual(result.digest,digest(in));}
    }
    /** A picker save must target a new empty document, not an existing source or alias. */
    static void verifyNewDestination(Context c,Uri source,Uri destination)throws Exception{
        if(destination==null||source.equals(destination)||!"content".equals(destination.getScheme()))
            throw new IOException("Choose a new file");
        long bytes=-1;
        try(Cursor cursor=c.getContentResolver().query(destination,new String[]{OpenableColumns.SIZE},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))bytes=cursor.getLong(0);
        }
        if(bytes<0)try(android.os.ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(destination,"r")){if(fd!=null)bytes=fd.getStatSize();}
        if(bytes!=0)throw new IOException("Destination must be a new empty document");
    }
    static void copy(Context c,Output output,Uri destination)throws Exception{
        if(!output.file.isFile()||output.file.length()!=output.meta.bytes)throw new IOException("Temporary output missing");
        long copied=0;try(InputStream in=new FileInputStream(output.file);OutputStream out=c.getContentResolver().openOutputStream(destination,"w")){
            if(out==null)throw new IOException("Cannot open destination");byte[] buf=new byte[128*1024];int n;
            while((n=in.read(buf))!=-1){interrupted();out.write(buf,0,n);copied+=n;}out.flush();
        }
        if(copied!=output.meta.bytes||!matches(c,destination,output))throw new IOException("Saved copy did not verify");
    }
    static Uri saveGallery(Context c,Output output,String filename)throws Exception{
        if(Build.VERSION.SDK_INT<29)throw new IOException("Use document picker");
        ContentResolver resolver=c.getContentResolver();ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);values.put(MediaStore.MediaColumns.MIME_TYPE,"video/mp4");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Movies/Clenova");values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri uri=resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),values);
        if(uri==null)throw new IOException("Cannot create copy");boolean published=false;
        try{copy(c,output,uri);interrupted();ContentValues ready=new ContentValues();ready.put(MediaStore.MediaColumns.IS_PENDING,0);
            if(resolver.update(uri,ready,null,null)!=1)throw new IOException("Cannot publish copy");published=true;return uri;
        }finally{if(!published)try{resolver.delete(uri,null,null);}catch(RuntimeException ignored){}}
    }
    static String filename(Source source){
        String n=source.name.replaceFirst("(?i)\\.[a-z0-9]{1,8}$","").replaceAll("[^a-zA-Z0-9_-]","_");
        if(n.length()>48)n=n.substring(0,48);return "Clenova_"+n+"_"+UUID.randomUUID().toString().substring(0,8)+".mp4";
    }
}

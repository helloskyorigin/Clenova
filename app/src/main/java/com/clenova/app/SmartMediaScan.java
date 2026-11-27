package com.clenova.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Foreground, read-only MediaStore scan. No file deletion or network access. */
final class SmartMediaScan {
    static final int DUPLICATES=0,VIDEOS=1,SCREENSHOTS=2,SIMILAR=3;
    static final long LARGE_VIDEO=100L*1024*1024, HASH_BUDGET=512L*1024*1024;
    static final int ROW_LIMIT=10000;
    static final String[] LABELS={"Duplicate photos","Large videos","Screenshots","Similar photos"};
    interface Progress {void update(int stage,int done,int total);}
    static final class Item {
        final Uri uri;final String name,path,volume;final long bytes,modified;final boolean image;final Boolean favorite;
        boolean protectedItem;
        long duration=-1,generation=-1;int width=-1,height=-1;String mime="";
        Item(Uri u,String n,String p,String v,long b,long m,boolean image,Boolean f){uri=u;name=n;path=p;volume=v;bytes=b;modified=m;this.image=image;favorite=f;}
    }
    static final class DuplicateGroup {
        final String fingerprint;final List<Item> items;
        DuplicateGroup(String fingerprint,List<Item> items){this.fingerprint=fingerprint;this.items=new ArrayList<>(items);}
    }
    static final class Result {
        final List<DuplicateGroup> duplicates=new ArrayList<>();
        final List<SimilarPhotoScan.Group> similarGroups=new ArrayList<>();
        final List<List<Item>> groups=Arrays.asList(new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>());
        boolean similarPartial,similarScanned;
        int examined,protectedCount,failed;boolean partial,imagesScanned,videosScanned,limited;
        long bytes(int category){long sum=0;for(Item item:groups.get(category))sum+=item.bytes;return sum;}
        long total(){return bytes(0)+bytes(1)+bytes(2)+bytes(3);}
    }
    private final Context context;private final ContentResolver resolver;private final CancellationSignal cancel=new CancellationSignal();
    private final List<ProtectedFiles.Entry> files;private final List<ProtectedFolders.Entry> folders;
    private final boolean favorite, recent,best;private final long now=System.currentTimeMillis();
    SmartMediaScan(Context c,AppPreferences prefs,ProtectedFiles protectedFiles,ProtectedFolders protectedFolders){
        context=c.getApplicationContext();resolver=context.getContentResolver();files=protectedFiles.entries();folders=protectedFolders.entries();
        favorite=prefs.protection(0);recent=prefs.protection(1);best=prefs.protection(3);
    }
    void cancel(){cancel.cancel();}
    private void check(){cancel.throwIfCanceled();}
    private boolean granted(String p){return context.checkSelfPermission(p)==android.content.pm.PackageManager.PERMISSION_GRANTED;}
    static boolean hasPhotoAccess(Context c){
        if(Build.VERSION.SDK_INT>=33)return c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)==0||(Build.VERSION.SDK_INT>=34&&c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)==0);
        return c.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)==0;
    }
    static boolean hasAccess(Context c){
        if(Build.VERSION.SDK_INT>=33)return c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)==0||c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO)==0||(Build.VERSION.SDK_INT>=34&&c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)==0);
        return c.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)==0;
    }
    static boolean hasVideoAccess(Context c){
        if(Build.VERSION.SDK_INT>=33)return c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO)==0||(Build.VERSION.SDK_INT>=34&&c.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)==0);
        return c.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)==0;
    }
    Result runVideos(Progress progress){
        Result r=new Result();List<Item> all=new ArrayList<>();r.videosScanned=hasVideoAccess(context);r.limited=true;
        progress.update(0,0,0);if(r.videosScanned)read(false,all,r);r.examined=all.size();int done=0;
        for(Item i:all){check();i.protectedItem=explicitlyProtected(i)||(favorite&&!Boolean.FALSE.equals(i.favorite))||(recent&&(i.modified<=0||i.modified>=now-ProtectionPolicy.RECENT_WINDOW_MS));
            if(i.protectedItem)r.protectedCount++;else if(i.bytes>=LARGE_VIDEO)r.groups.get(VIDEOS).add(i);
            if(++done%128==0||done==all.size())progress.update(2,done,all.size());}
        r.groups.get(VIDEOS).sort(Comparator.comparingLong((Item i)->i.bytes).reversed().thenComparing(i->i.name));check();return r;
    }
    Result run(Progress progress)throws Exception{
        Result r=new Result();List<Item> all=new ArrayList<>();
        boolean selected=Build.VERSION.SDK_INT>=34&&granted(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        boolean photos=Build.VERSION.SDK_INT>=33?granted(android.Manifest.permission.READ_MEDIA_IMAGES):hasAccess(context);
        boolean videos=Build.VERSION.SDK_INT>=33?granted(android.Manifest.permission.READ_MEDIA_VIDEO):hasAccess(context);
        r.limited=!photos||!videos;r.imagesScanned=photos||selected;r.videosScanned=videos||selected;
        progress.update(0,0,0);
        if(r.imagesScanned)read(true,all,r);if(r.videosScanned)read(false,all,r);
        r.examined=all.size();check();
        Map<Long,List<Item>> bySize=new HashMap<>();for(Item i:all){
            i.protectedItem=explicitlyProtected(i)||(favorite&&!Boolean.FALSE.equals(i.favorite))||(recent&&(i.modified<=0||i.modified>=now-ProtectionPolicy.RECENT_WINDOW_MS));
            if(i.image)bySize.computeIfAbsent(i.bytes,k->new ArrayList<>()).add(i);
        }
        List<Item> hashItems=new ArrayList<>();for(List<Item> batch:bySize.values())if(batch.size()>1)hashItems.addAll(batch);
        hashItems.sort(Comparator.comparingLong((Item i)->i.bytes).thenComparing(i->i.uri.toString()));
        Map<String,List<Item>> exact=new HashMap<>();long remaining=HASH_BUDGET;int done=0;
        progress.update(1,0,hashItems.size());
        for(Item i:hashItems){
            check();if(i.bytes>remaining){r.partial=true;break;}
            remaining-=i.bytes;
            try{String hash=hash(i);exact.computeIfAbsent(i.bytes+":"+hash,k->new ArrayList<>()).add(i);}
            catch(OperationCanceledException e){throw e;}catch(Exception e){r.failed++;r.partial=true;}
            progress.update(1,++done,hashItems.size());
        }
        Set<String> allocated=new HashSet<>(),exactMembers=new HashSet<>();
        for(Map.Entry<String,List<Item>> entry:exact.entrySet()){
            List<Item> group=entry.getValue();if(group.size()<2)continue;
            group.sort(Comparator.comparing((Item i)->!i.protectedItem).thenComparingLong(i->i.modified).thenComparing(i->i.uri.toString()));
            // Always retain at least one verified matching copy, even with protection toggles off.
            Item keep=group.get(0);keep.protectedItem=true;
            r.duplicates.add(new DuplicateGroup(entry.getKey(),group));
            for(Item i:group)exactMembers.add(i.uri.toString());
            for(int k=1;k<group.size();k++){Item i=group.get(k);if(!i.protectedItem&&allocated.add(i.uri.toString()))r.groups.get(DUPLICATES).add(i);}
        }
        r.duplicates.sort(Comparator.comparingLong((DuplicateGroup g)->g.items.get(0).bytes).reversed().thenComparing(g->g.fingerprint));
        progress.update(2,0,all.size());int classified=0;
        for(Item i:all){
            check();
            if(i.protectedItem){r.protectedCount++;}
            else if(!allocated.contains(i.uri.toString())){
                if(!i.image&&i.bytes>=LARGE_VIDEO){allocated.add(i.uri.toString());r.groups.get(VIDEOS).add(i);}
                else if(i.image&&isScreenshot(i)){
                    // Screenshot review is manual; best-shot protection applies to similar-photo groups.
                    // Exact-match group members remain reserved for the duplicate workflow.
                    if(!exactMembers.contains(i.uri.toString())&&(!r.partial||bySize.get(i.bytes).size()==1)){allocated.add(i.uri.toString());r.groups.get(SCREENSHOTS).add(i); }
                }
            }
            if(++classified%128==0||classified==all.size())progress.update(2,classified,all.size());
        }
        if(r.imagesScanned){
            List<Item> visual=new ArrayList<>();for(Item i:all)if(i.image&&!exactMembers.contains(i.uri.toString())&&(!r.partial||bySize.get(i.bytes).size()==1)&&!isScreenshot(i)&&!allocated.contains(i.uri.toString())&&i.path!=null&&i.modified>0)visual.add(i);
            r.similarScanned=true;r.similarGroups.addAll(SimilarPhotoScan.run(resolver,visual,cancel,r,progress));
            for(SimilarPhotoScan.Group g:r.similarGroups)for(Item i:g.items)if(SimilarPhotoScan.candidate(g,i)&&allocated.add(i.uri.toString()))r.groups.get(SIMILAR).add(i);
            if(r.similarPartial)r.partial=true;
        }
        for(List<Item> group:r.groups)group.sort(Comparator.comparingLong((Item i)->i.bytes).reversed().thenComparing(i->i.name));
        check();return r;
    }
    private void read(boolean image,List<Item> out,Result r){
        Uri base=image?MediaStore.Images.Media.EXTERNAL_CONTENT_URI:MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        List<String> cols=new ArrayList<>(Arrays.asList("_id","_display_name","_size","date_modified"));
        if(Build.VERSION.SDK_INT>=29){cols.add("relative_path");cols.add("volume_name");}
        if(Build.VERSION.SDK_INT>=30)cols.add("is_favorite");
        if(!image)cols.add("duration");cols.addAll(Arrays.asList("width","height","mime_type"));if(Build.VERSION.SDK_INT>=30)cols.add("generation_modified");
        try(Cursor c=resolver.query(base,cols.toArray(new String[0]),null,null,"_id ASC",cancel)){
            if(c==null){r.partial=true;r.failed++;return;}
            int read=0;while(c.moveToNext()){
                check();if(read++>=ROW_LIMIT){r.partial=true;break;}
                long bytes=c.getLong(2);if(bytes<=0)continue;
                String name=c.getString(1);if(name==null)name="Media";
                String path=Build.VERSION.SDK_INT>=29?c.getString(4):null,volume=Build.VERSION.SDK_INT>=29?c.getString(5):null;
                Boolean fav=Build.VERSION.SDK_INT>=30&&!c.isNull(6)?c.getInt(6)!=0:null;
                Uri collection=base;
                if(Build.VERSION.SDK_INT>=29&&volume!=null)collection=image?MediaStore.Images.Media.getContentUri(volume):MediaStore.Video.Media.getContentUri(volume);
                Item item=new Item(ContentUris.withAppendedId(collection,c.getLong(0)),name,path,volume,bytes,c.getLong(3)*1000L,image,fav);
                {int d=c.getColumnIndex("duration"),w=c.getColumnIndex("width"),h=c.getColumnIndex("height"),m=c.getColumnIndex("mime_type"),g=c.getColumnIndex("generation_modified");
                    if(d>=0&&!c.isNull(d))item.duration=c.getLong(d);if(w>=0&&!c.isNull(w))item.width=c.getInt(w);if(h>=0&&!c.isNull(h))item.height=c.getInt(h);if(m>=0&&!c.isNull(m))item.mime=c.getString(m);if(g>=0&&!c.isNull(g))item.generation=c.getLong(g);}
                out.add(item);
            }
        }catch(OperationCanceledException e){throw e;}catch(RuntimeException e){r.failed++;r.partial=true;}
    }
    private boolean explicitlyProtected(Item i){
        String external="com.android.externalstorage.documents",media="com.android.providers.media.documents";
        String id=i.volume==null||i.path==null?null:("external_primary".equals(i.volume)?"primary":i.volume)+":"+i.path+i.name;
        for(ProtectedFolders.Entry e:folders){
            // Unknown document-provider aliases or missing paths are conservatively excluded.
            if(!external.equals(e.authority)||id==null)return true;
            if(FolderExclusion.matches(e.authority,e.documentId,external,id))return true;
        }
        for(ProtectedFiles.Entry e:files){
            Uri u=Uri.parse(e.key);if(u.equals(i.uri))return true;
            if(external.equals(u.getAuthority())&&id!=null){
                try{if(DocumentsContract.getDocumentId(u).equalsIgnoreCase(id))return true;}catch(RuntimeException error){return true;}
            }else if(media.equals(u.getAuthority())){
                try{String doc=DocumentsContract.getDocumentId(u);if(doc.equals((i.image?"image:":"video:")+i.uri.getLastPathSegment()))return true;}catch(RuntimeException error){return true;}
            }else return true;
        }
        return false;
    }
    private boolean isScreenshot(Item i){
        String path=i.path==null?"":i.path.toLowerCase(Locale.ROOT);
        for(String part:path.split("/"))if(part.equals("screenshots")||part.equals("screenshot"))return true;
        return false;
    }
    private String hash(Item i)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");long read=0;
        try(InputStream in=resolver.openInputStream(i.uri)){
            if(in==null)throw new IOException("Unreadable media");byte[] buf=new byte[65536];int n;
            while((n=in.read(buf))!=-1){check();read+=n;if(read>i.bytes)throw new IOException("Media changed");digest.update(buf,0,n);}
        }
        if(read!=i.bytes)throw new IOException("Media size changed");
        return android.util.Base64.encodeToString(digest.digest(),android.util.Base64.NO_WRAP);
    }
}

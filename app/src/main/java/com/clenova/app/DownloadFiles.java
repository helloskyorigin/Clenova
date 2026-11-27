package com.clenova.app;

import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;
import java.util.*;

/** Session-only SAF downloads. No broad storage permission or persistent URI grants. */
final class DownloadFiles {
    static final int LIMIT=200, DELETE_LIMIT=50;
    static final long LARGE_MIN=100L*1024*1024;
    static final String EXTERNAL="com.android.externalstorage.documents", DOWNLOADS="com.android.providers.downloads.documents";
    static final int APK=0,ARCHIVE=1,DOC=2,OTHER=3;
    static final class Item {
        final Uri uri;final String id,name,mime;final long bytes,modified,flags;final int type;
        final boolean verifiedLocation,protectedItem,canDelete;
        Item(Uri uri,String id,String name,String mime,long bytes,long modified,long flags,int type,boolean verifiedLocation,boolean protectedItem,boolean canDelete){
            this.uri=uri;this.id=id;this.name=name;this.mime=mime;this.bytes=bytes;this.modified=modified;this.flags=flags;this.type=type;this.verifiedLocation=verifiedLocation;this.protectedItem=protectedItem;this.canDelete=canDelete;
        }
    }
    private final Context context;private final ContentResolver resolver;private final AppPreferences prefs;private final ProtectedFiles files;private final ProtectedFolders folders;private final boolean largeMode,clutterMode;
    DownloadFiles(Context c,AppPreferences p,ProtectedFiles f,ProtectedFolders d){this(c,p,f,d,false);}
    DownloadFiles(Context c,AppPreferences p,ProtectedFiles f,ProtectedFolders d,boolean large){this(c,p,f,d,large,false);}
    DownloadFiles(Context c,AppPreferences p,ProtectedFiles f,ProtectedFolders d,boolean large,boolean clutter){context=c;resolver=c.getContentResolver();prefs=p;files=f;folders=d;largeMode=large;clutterMode=clutter;}
    static int type(String name,String mime){
        String n=name.toLowerCase(Locale.ROOT),m=mime.toLowerCase(Locale.ROOT);
        if(n.endsWith(".apk")||m.equals("application/vnd.android.package-archive"))return APK;
        if(n.matches(".*\\.(zip|rar|7z|tar|gz|tgz|bz2|xz)$")||m.contains("zip")||m.contains("compressed")||m.contains("archive"))return ARCHIVE;
        if(n.matches(".*\\.(pdf|docx?|xlsx?|pptx?|txt|csv|rtf|odt|ods|odp|md)$")||m.startsWith("text/")||m.contains("pdf")||m.contains("officedocument")||m.contains("msword")||m.contains("spreadsheet"))return DOC;
        return OTHER;
    }
    private static boolean externalDownload(String id){
        if(id==null)return false;int colon=id.indexOf(':');if(colon<=0)return false;
        String[] parts=id.substring(colon+1).split("/");
        if(parts.length<2||!parts[0].equalsIgnoreCase("Download"))return false;
        for(String part:parts)if(part.isEmpty()||part.equals(".")||part.equals(".."))return false;return true;
    }
    private static boolean externalPath(String id){
        if(id==null)return false;int colon=id.indexOf(':');if(colon<=0)return false;
        String[] parts=id.substring(colon+1).split("/",-1);if(parts.length<2)return false;
        for(String part:parts)if(part.isEmpty()||part.equals(".")||part.equals(".."))return false;
        return !parts[0].equalsIgnoreCase("Android");
    }
    private static boolean safeRemovalPath(String id){
        if(!externalPath(id))return false;
        String root=id.substring(id.indexOf(':')+1).split("/")[0];
        return root.equalsIgnoreCase("Download")||root.equalsIgnoreCase("Documents");
    }
    // Scan only a folder explicitly granted by the system picker; no automatic whole-device traversal.
    List<Uri> largeFolderUris(Uri tree,CancellationSignal signal){
        List<Uri> result=new ArrayList<>();if((!largeMode&&!clutterMode)||tree==null||!EXTERNAL.equals(tree.getAuthority())||!DocumentsContract.isTreeUri(tree))return result;
        String root=DocumentsContract.getTreeDocumentId(tree);
        if(root==null||root.endsWith(":")||root.substring(root.indexOf(':')+1).toLowerCase(Locale.ROOT).startsWith("android"))return result;
        ArrayDeque<String> dirs=new ArrayDeque<>();Set<String> seen=new HashSet<>();dirs.add(root);int inspected=0;
        while(!dirs.isEmpty()&&inspected<2000&&result.size()<LIMIT){
            signal.throwIfCanceled();String parent=dirs.remove();if(!seen.add(parent))continue;
            Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);
            try(Cursor c=resolver.query(children,new String[]{"document_id","mime_type","_size","_display_name"},null,null,null,signal)){
                if(c==null)continue;
                while(c.moveToNext()&&inspected++<2000&&result.size()<LIMIT){
                    signal.throwIfCanceled();String id=c.getString(0),mime=c.getString(1);
                    if(id==null||!id.startsWith(root+"/")||!externalPath(id))continue;
                    if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){if(dirs.size()<200)dirs.add(id);}
                    else if(clutterMode?ClutterRules.category(c.getString(3),mime)>=0:!c.isNull(2)&&c.getLong(2)>=LARGE_MIN)result.add(DocumentsContract.buildDocumentUriUsingTree(tree,id));
                }
            }catch(android.os.OperationCanceledException e){throw e;}catch(RuntimeException ignored){}
        }return result;
    }
    private boolean granted(Uri uri,int mode){return context.checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),mode)==PackageManager.PERMISSION_GRANTED;}
    Item read(Uri uri,CancellationSignal signal){
        if(uri==null||!"content".equals(uri.getScheme())||!DocumentsContract.isDocumentUri(context,uri))return null;
        String authority=uri.getAuthority();if(!EXTERNAL.equals(authority)&&!DOWNLOADS.equals(authority))return null;
        String id;try{id=DocumentsContract.getDocumentId(uri);}catch(RuntimeException e){return null;}
        boolean verified=EXTERNAL.equals(authority)&&(largeMode||clutterMode?externalPath(id):externalDownload(id));
        if(EXTERNAL.equals(authority)&&!verified)return null;
        if(!granted(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION))return null;
        String[] cols={"_display_name","mime_type","_size","last_modified","flags"};
        try(Cursor c=resolver.query(uri,cols,null,null,null,signal)){
            if(c==null||!c.moveToFirst())return null;
            String name=c.getString(0),mime=c.getString(1);if(name==null||mime==null||DocumentsContract.Document.MIME_TYPE_DIR.equals(mime))return null;
            long bytes=c.isNull(2)?-1:c.getLong(2),modified=c.isNull(3)?-1:c.getLong(3),flags=c.isNull(4)?0:c.getLong(4);
            if(largeMode&&bytes<LARGE_MIN)return null;
            int type=type(name,mime),clutter=ClutterRules.category(name,mime);if(clutterMode&&clutter<0)return null;boolean protectedItem=files.excludes(uri);
            // Resolve known external-storage paths; unknown aliases fail closed.
            for(ProtectedFiles.Entry e:files.entries()){
                Uri other=Uri.parse(e.key);
                if(!verified||!EXTERNAL.equals(other.getAuthority())){protectedItem=true;break;}
                try{if(DocumentsContract.getDocumentId(other).equalsIgnoreCase(id)){protectedItem=true;break;}}catch(RuntimeException ex){protectedItem=true;break;}
            }
            for(ProtectedFolders.Entry e:folders.entries()){
                if(!verified||!EXTERNAL.equals(e.authority)||FolderExclusion.matches(e.authority,e.documentId,authority,id)){protectedItem=true;break;}
            }
            if(prefs.protection(1)&&(modified<=0||modified>=System.currentTimeMillis()-ProtectionPolicy.RECENT_WINDOW_MS))protectedItem=true;
            // Only known APK/archive types are eligible with important-document protection enabled.
            if(prefs.protection(2)&&(type==DOC||type==OTHER))protectedItem=true;
            if(clutterMode&&!ClutterRules.oldEnough(modified,System.currentTimeMillis()))protectedItem=true;
            boolean media=mime.startsWith("image/")||mime.startsWith("video/")||mime.startsWith("audio/")||name.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|webp|heic|heif|gif|bmp|dng|mp4|mkv|mov|avi|webm|3gp|mp3|wav|flac|m4a|ogg|aac)$");
            // Opaque downloads provider IDs and media are review-only. No removal via ambiguous aliases.
            boolean delete=verified&&!protectedItem&&!media&&bytes>=0&&modified>0
                &&(!largeMode||(!DocumentsContract.isTreeUri(uri)&&safeRemovalPath(id)&&type!=OTHER))
                &&(!clutterMode||(!DocumentsContract.isTreeUri(uri)&&externalDownload(id)&&ClutterRules.removalType(clutter)))
                &&(flags&DocumentsContract.Document.FLAG_SUPPORTS_DELETE)!=0
                &&(flags&DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)==0
                &&granted(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return new Item(uri,id,name,mime,bytes,modified,flags,type,verified,protectedItem,delete);
        }catch(android.os.OperationCanceledException e){throw e;}catch(RuntimeException e){return null;}
    }
    List<Item> readAll(Collection<Uri> uris,CancellationSignal signal){
        List<Item> out=new ArrayList<>();Map<String,Item> unique=new LinkedHashMap<>();
        for(Uri uri:uris){signal.throwIfCanceled();Item i=read(uri,signal);if(i==null)continue;String key=i.uri.getAuthority()+":"+i.id;Item old=unique.get(key);
            if(old==null&&unique.size()<LIMIT)unique.put(key,i);
            else if(old!=null&&DocumentsContract.isTreeUri(old.uri)&&!DocumentsContract.isTreeUri(i.uri))unique.put(key,i);
        }out.addAll(unique.values());
        out.sort(Comparator.comparingLong((Item i)->i.modified).reversed().thenComparing(i->i.name));return out;
    }
    static boolean unchanged(Item before,Item after){
        return after!=null&&after.canDelete&&before.canDelete&&before.uri.equals(after.uri)&&before.id.equals(after.id)
            &&before.name.equals(after.name)&&before.mime.equals(after.mime)&&before.bytes==after.bytes&&before.modified==after.modified;
    }
    /** Recheck all files before any removal; recheck each one again immediately before provider operation. */
    int[] remove(List<Item> selected){
        if(selected.isEmpty()||selected.size()>DELETE_LIMIT)return new int[]{0,selected.size()};
        CancellationSignal signal=new CancellationSignal();
        for(Item i:selected)if(!unchanged(i,read(i.uri,signal)))return new int[]{0,selected.size()};
        int removed=0,failed=0;
        for(Item i:selected){
            try{if(unchanged(i,read(i.uri,signal))&&DocumentsContract.deleteDocument(resolver,i.uri))removed++;else failed++;}
            catch(Exception e){failed++;}
        }
        return new int[]{removed,failed};
    }
}

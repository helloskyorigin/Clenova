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
    static final String EXTERNAL="com.android.externalstorage.documents", DOWNLOADS="com.android.providers.downloads.documents";
    static final int APK=0,ARCHIVE=1,DOC=2,OTHER=3;
    static final class Item {
        final Uri uri;final String id,name,mime;final long bytes,modified,flags;final int type;
        final boolean verifiedLocation,protectedItem,canDelete;
        Item(Uri uri,String id,String name,String mime,long bytes,long modified,long flags,int type,boolean verifiedLocation,boolean protectedItem,boolean canDelete){
            this.uri=uri;this.id=id;this.name=name;this.mime=mime;this.bytes=bytes;this.modified=modified;this.flags=flags;this.type=type;this.verifiedLocation=verifiedLocation;this.protectedItem=protectedItem;this.canDelete=canDelete;
        }
    }
    private final Context context;private final ContentResolver resolver;private final AppPreferences prefs;private final ProtectedFiles files;private final ProtectedFolders folders;
    DownloadFiles(Context c,AppPreferences p,ProtectedFiles f,ProtectedFolders d){context=c;resolver=c.getContentResolver();prefs=p;files=f;folders=d;}
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
    private boolean granted(Uri uri,int mode){return context.checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),mode)==PackageManager.PERMISSION_GRANTED;}
    Item read(Uri uri,CancellationSignal signal){
        if(uri==null||!"content".equals(uri.getScheme())||!DocumentsContract.isDocumentUri(context,uri))return null;
        String authority=uri.getAuthority();if(!EXTERNAL.equals(authority)&&!DOWNLOADS.equals(authority))return null;
        String id;try{id=DocumentsContract.getDocumentId(uri);}catch(RuntimeException e){return null;}
        boolean verified=EXTERNAL.equals(authority)&&externalDownload(id);
        if(EXTERNAL.equals(authority)&&!verified)return null;
        if(!granted(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION))return null;
        String[] cols={"_display_name","mime_type","_size","last_modified","flags"};
        try(Cursor c=resolver.query(uri,cols,null,null,null,signal)){
            if(c==null||!c.moveToFirst())return null;
            String name=c.getString(0),mime=c.getString(1);if(name==null||mime==null||DocumentsContract.Document.MIME_TYPE_DIR.equals(mime))return null;
            long bytes=c.isNull(2)?-1:c.getLong(2),modified=c.isNull(3)?-1:c.getLong(3),flags=c.isNull(4)?0:c.getLong(4);
            int type=type(name,mime);boolean protectedItem=files.excludes(uri);
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
            boolean media=mime.startsWith("image/")||mime.startsWith("video/")||mime.startsWith("audio/");
            // Opaque downloads provider IDs and media are review-only. No removal via ambiguous aliases.
            boolean delete=verified&&!protectedItem&&!media&&bytes>=0&&modified>0
                &&(flags&DocumentsContract.Document.FLAG_SUPPORTS_DELETE)!=0
                &&(flags&DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT)==0
                &&granted(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return new Item(uri,id,name,mime,bytes,modified,flags,type,verified,protectedItem,delete);
        }catch(android.os.OperationCanceledException e){throw e;}catch(RuntimeException e){return null;}
    }
    List<Item> readAll(Collection<Uri> uris,CancellationSignal signal){
        List<Item> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Uri uri:uris){signal.throwIfCanceled();if(out.size()>=LIMIT)break;Item i=read(uri,signal);if(i!=null&&seen.add(i.uri.getAuthority()+":"+i.id))out.add(i);}
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

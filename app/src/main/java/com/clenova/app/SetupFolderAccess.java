package com.clenova.app;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;

/** One explicitly selected local folder, read-only. Never scans or deletes on grant. */
final class SetupFolderAccess {
    static final String STORE="clenova_setup_folder";
    private static final String KEY="tree";
    private static final String EXTERNAL="com.android.externalstorage.documents";
    private final Context context;
    SetupFolderAccess(Context c){context=c.getApplicationContext();}
    private android.content.SharedPreferences prefs(){return context.getSharedPreferences(STORE,Context.MODE_PRIVATE);}
    private boolean valid(Uri uri){
        try{
            if(uri==null||!"content".equals(uri.getScheme())||!EXTERNAL.equals(uri.getAuthority())||!DocumentsContract.isTreeUri(uri))return false;
            String id=DocumentsContract.getTreeDocumentId(uri);
            int colon=id.indexOf(':');if(colon<0||colon==id.length()-1)return false;
            String path=id.substring(colon+1).toLowerCase(java.util.Locale.ROOT);
            return !path.startsWith("android");
        }catch(RuntimeException e){return false;}
    }
    private boolean retained(Uri uri){
        try{for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions())
            if(permission.isReadPermission()&&uri.equals(permission.getUri()))return true;}
        catch(RuntimeException ignored){}
        return false;
    }
    Uri current(){
        String raw=prefs().getString(KEY,"");
        if(raw.isEmpty())return null;
        Uri uri=Uri.parse(raw);
        if(valid(uri)&&retained(uri))return uri;
        prefs().edit().remove(KEY).apply();return null;
    }
    boolean save(Intent data){
        if(data==null||!valid(data.getData())||(data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)==0)return false;
        Uri uri=data.getData(),old=current();
        boolean alreadyRetained=retained(uri);
        try{
            context.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(!retained(uri))return false;
            if(!prefs().edit().putString(KEY,uri.toString()).commit()){
                if(!alreadyRetained)release(uri);return false;
            }
            if(old!=null&&!old.equals(uri))release(old);
            return true;
        }catch(RuntimeException e){return false;}
    }
    private void release(Uri uri){
        try{context.getContentResolver().releasePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        catch(RuntimeException ignored){}
    }
    void clear(){Uri uri=current();if(uri!=null)release(uri);prefs().edit().clear().apply();}
}

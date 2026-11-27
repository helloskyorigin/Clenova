package com.clenova.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import org.json.JSONObject;

/** Persistent local exclusions. Only metadata is queried using the picker grant. */
final class ProtectedFiles {
    private final Context context;
    private final SharedPreferences store;
    static final class Entry {
        final String key,name,mime;
        final long size;
        Entry(String key,String name,String mime,long size){this.key=key;this.name=name;this.mime=mime;this.size=size;}
        String icon(){return mime.startsWith("image/")?"images":mime.startsWith("video/")?"video":"document";}
        String typeLabel(){
            if(mime.startsWith("image/"))return "Photo";
            if(mime.startsWith("video/"))return "Video";
            if(mime.startsWith("audio/"))return "Audio";
            if(mime.equals("application/pdf"))return "PDF";
            return "File";
        }
    }
    ProtectedFiles(Context context){
        this.context=context.getApplicationContext();
        store=context.getSharedPreferences("clenova_file_exclusions",Context.MODE_PRIVATE);
    }
    private HashSet<String> keys(){return new HashSet<>(store.getStringSet("files",java.util.Collections.emptySet()));}
    private String identity(Uri uri){
        if(!"content".equals(uri.getScheme())||uri.getAuthority()==null)throw new IllegalArgumentException("Not a content URI");
        if(DocumentsContract.isDocumentUri(context,uri)){
            return DocumentsContract.buildDocumentUri(uri.getAuthority(),DocumentsContract.getDocumentId(uri)).toString();
        }
        return uri.normalizeScheme().toString();
    }
    synchronized List<Entry> entries(){
        List<Entry> out=new ArrayList<>();
        for(String key:keys()){
            try{
                JSONObject meta=new JSONObject(store.getString("meta:"+key,"{}"));
                out.add(new Entry(key,meta.optString("name","File"),meta.optString("mime","application/octet-stream"),meta.optLong("size",-1)));
            }catch(org.json.JSONException ignored){}
        }
        out.sort(Comparator.comparing((Entry e)->e.name,String.CASE_INSENSITIVE_ORDER).thenComparing(e->e.key));
        return out;
    }
    synchronized boolean add(Uri uri){
        String key=identity(uri);
        HashSet<String> all=keys();if(all.contains(key))return false;
        String name=uri.getLastPathSegment(),mime="application/octet-stream";long size=-1;
        if(name==null||name.isEmpty())name="File";
        try{
            String actual=context.getContentResolver().getType(uri);
            if(actual!=null&&!actual.isEmpty())mime=actual;
        }catch(RuntimeException ignored){}
        try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()){
                int n=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME),z=cursor.getColumnIndex(OpenableColumns.SIZE);
                if(n>=0&&!cursor.isNull(n))name=cursor.getString(n);
                if(z>=0&&!cursor.isNull(z))size=cursor.getLong(z);
            }
        }catch(RuntimeException ignored){}
        try{
            JSONObject meta=new JSONObject();meta.put("name",name);meta.put("mime",mime);meta.put("size",size);
            all.add(key);
            if(!store.edit().putStringSet("files",all).putString("meta:"+key,meta.toString()).commit())throw new IllegalStateException("Save failed");
        }catch(org.json.JSONException e){throw new IllegalStateException(e);}
        AutopilotScheduler.changed(context,new AppPreferences(context));return true;
    }
    synchronized boolean remove(String key){
        HashSet<String> all=keys();all.remove(key);
        boolean ok=store.edit().putStringSet("files",all).remove("meta:"+key).commit();if(ok)AutopilotScheduler.changed(context,new AppPreferences(context));return ok;
    }
    /** Future scanner must resolve alternate provider aliases to this same identity first. */
    synchronized boolean excludes(Uri uri){
        try{return keys().contains(identity(uri));}catch(IllegalArgumentException e){return false;}
    }
    synchronized boolean excludesDocument(String authority,String documentId){
        if(authority==null||documentId==null)return false;
        return keys().contains(DocumentsContract.buildDocumentUri(authority,documentId).toString());
    }
}

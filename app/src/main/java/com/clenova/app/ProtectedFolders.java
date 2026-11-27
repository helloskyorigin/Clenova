package com.clenova.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import org.json.JSONObject;

/** Local folder exclusions. No persistent file access is required to store an exclusion. */
final class ProtectedFolders {
    private final Context context;
    private final SharedPreferences store;
    static final class Entry {
        final String uri,name,location,authority,documentId;
        Entry(String uri,String name,String location,String authority,String documentId){
            this.uri=uri;this.name=name;this.location=location;this.authority=authority;this.documentId=documentId;
        }
    }
    ProtectedFolders(Context context){
        this.context=context.getApplicationContext();
        store=context.getSharedPreferences("clenova_folder_exclusions",Context.MODE_PRIVATE);
    }
    List<Entry> entries(){
        List<Entry> out=new ArrayList<>();
        for(String key:new HashSet<>(store.getStringSet("trees",java.util.Collections.emptySet()))){
            Uri uri=Uri.parse(key);
            String id;
            try{id=DocumentsContract.getTreeDocumentId(uri);}catch(IllegalArgumentException e){continue;}
            String name=id,location=uri.getAuthority();
            try{
                JSONObject meta=new JSONObject(store.getString("meta:"+key,"{}"));
                name=meta.optString("name",name);location=meta.optString("location",location);
            }catch(org.json.JSONException ignored){}
            out.add(new Entry(key,name,location,uri.getAuthority(),id));
        }
        out.sort(Comparator.comparing((Entry e)->e.name,String.CASE_INSENSITIVE_ORDER).thenComparing(e->e.uri));
        return out;
    }
    boolean add(Uri uri){
        if(!DocumentsContract.isTreeUri(uri)||!"content".equals(uri.getScheme())||uri.getAuthority()==null)throw new IllegalArgumentException("Not a tree");
        String id=DocumentsContract.getTreeDocumentId(uri);
        String key=DocumentsContract.buildTreeDocumentUri(uri.getAuthority(),id).toString();
        HashSet<String> keys=new HashSet<>(store.getStringSet("trees",java.util.Collections.emptySet()));
        if(keys.contains(key))return false;
        // Query only the chosen folder's display name using the temporary picker grant.
        // No file enumeration, no persistent read grant, no write access.
        String name=id;
        if("com.android.externalstorage.documents".equals(uri.getAuthority())){
            int slash=Math.max(id.lastIndexOf('/'),id.indexOf(':'));if(slash>=0&&slash+1<id.length())name=id.substring(slash+1);
        }
        Uri document=DocumentsContract.buildDocumentUriUsingTree(uri,id);
        try(Cursor cursor=context.getContentResolver().query(document,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);
        }catch(RuntimeException ignored){}
        String location="com.android.externalstorage.documents".equals(uri.getAuthority())?id.replaceFirst("^primary:","Internal storage / "):uri.getAuthority();
        try{
            JSONObject meta=new JSONObject();meta.put("name",name);meta.put("location",location);
            keys.add(key);
            if(!store.edit().putStringSet("trees",keys).putString("meta:"+key,meta.toString()).commit())throw new IllegalStateException("Save failed");
        }catch(org.json.JSONException e){throw new IllegalStateException(e);}
        return true;
    }
    boolean remove(String key){
        HashSet<String> keys=new HashSet<>(store.getStringSet("trees",java.util.Collections.emptySet()));
        keys.remove(key);return store.edit().putStringSet("trees",keys).remove("meta:"+key).commit();
    }
    /** For future scanner candidates with external-storage document IDs.
     * Opaque provider IDs are exact-match only; a future scanner must resolve ancestry separately.
     */
    boolean excludesDocument(String authority,String id){
        for(Entry entry:entries())if(FolderExclusion.matches(entry.authority,entry.documentId,authority,id))return true;
        return false;
    }
}

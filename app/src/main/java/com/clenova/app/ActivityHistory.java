package com.clenova.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/** Local event journal. Records observed app actions only. */
final class ActivityHistory {
    private final SharedPreferences store;
    static final class Entry {
        final String id,category,title,detail;
        final long timestamp;
        final double mb;
        final int count;
        final boolean preview;
        Entry(JSONObject e) throws JSONException{
            id=e.getString("id");category=e.getString("category");title=e.getString("title");
            detail=e.getString("detail");timestamp=e.getLong("timestamp");mb=e.getDouble("mb");
            count=e.getInt("count");preview=e.getBoolean("preview");
        }
    }
    ActivityHistory(Context context){store=context.getSharedPreferences("clenova_activity_history",Context.MODE_PRIVATE);}
    private JSONArray read() throws JSONException{return new JSONArray(store.getString("events","[]"));}
    synchronized List<Entry> entries(){
        List<Entry> out=new ArrayList<>();
        try{
            JSONArray data=read();
            for(int i=data.length()-1;i>=0;i--){
                try{out.add(new Entry(data.getJSONObject(i)));}catch(JSONException ignored){}
            }
        }catch(JSONException ignored){}
        out.sort(Comparator.comparingLong((Entry e)->e.timestamp).reversed());
        return out;
    }
    synchronized boolean add(String id,String category,String title,String detail,double mb,int count,boolean preview){
        try{
            JSONArray data=read();
            String eventId=id==null?UUID.randomUUID().toString():id;
            for(int i=0;i<data.length();i++)if(eventId.equals(data.getJSONObject(i).optString("id")))return true;
            JSONObject e=new JSONObject();
            e.put("id",eventId);e.put("category",category);e.put("title",title);e.put("detail",detail);
            e.put("timestamp",System.currentTimeMillis());e.put("mb",mb);e.put("count",count);e.put("preview",preview);
            data.put(e);
            return store.edit().putString("events",data.toString()).commit();
        }catch(JSONException ex){return false;}
    }
}

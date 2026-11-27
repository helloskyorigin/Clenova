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
    private static final Object JOURNAL_LOCK=new Object();
    static final class Entry {
        final String id,category,title,detail;
        final long timestamp;
        final double mb;
        final int count;
        final boolean preview;
        final double[] categoryMb;
        final int[] categoryCounts;
        Entry(JSONObject e) throws JSONException{
            id=e.getString("id");category=e.getString("category");title=e.getString("title");
            detail=e.getString("detail");timestamp=e.getLong("timestamp");mb=e.getDouble("mb");
            count=e.getInt("count");preview=e.getBoolean("preview");
            JSONArray sizes=e.optJSONArray("categoryMb"),counts=e.optJSONArray("categoryCounts");
            if(sizes!=null&&counts!=null&&sizes.length()==3&&counts.length()==3){
                double[] a=new double[3];int[] n=new int[3];boolean valid=true;
                double sum=0;int totalCount=0;
                for(int i=0;i<3;i++){a[i]=sizes.optDouble(i,-1);n[i]=counts.optInt(i,-1);
                    if(!Double.isFinite(a[i])||a[i]<0||n[i]<0||(a[i]==0)!=(n[i]==0))valid=false;
                    sum+=a[i];totalCount+=n[i];
                }
                if(valid&&Math.abs(sum-mb)<0.0001&&totalCount==count){categoryMb=a;categoryCounts=n;}
                else{categoryMb=null;categoryCounts=null;}
            }else{categoryMb=null;categoryCounts=null;}
        }
    }
    ActivityHistory(Context context){store=context.getSharedPreferences("clenova_activity_history",Context.MODE_PRIVATE);}
    private JSONArray read() throws JSONException{return new JSONArray(store.getString("events","[]"));}
    List<Entry> entries(){synchronized(JOURNAL_LOCK){
        List<Entry> out=new ArrayList<>();
        try{
            JSONArray data=read();
            for(int i=data.length()-1;i>=0;i--){
                try{out.add(new Entry(data.getJSONObject(i)));}catch(JSONException ignored){}
            }
        }catch(JSONException ignored){}
        out.sort(Comparator.comparingLong((Entry e)->e.timestamp).reversed());
        return out;
    }}
    boolean clear(){synchronized(JOURNAL_LOCK){return store.edit().remove("events").commit();}}
    String exportJson() throws JSONException{synchronized(JOURNAL_LOCK){
        JSONObject result=new JSONObject();
        result.put("schemaVersion",1);result.put("exportedAt",System.currentTimeMillis());
        result.put("events",read());return result.toString(2);
    }}
    Entry find(String id){synchronized(JOURNAL_LOCK){
        if(id==null)return null;
        for(Entry e:entries())if(id.equals(e.id))return e;
        return null;
    }}
    boolean add(String id,String category,String title,String detail,double mb,int count,boolean preview){synchronized(JOURNAL_LOCK){
        return add(id,category,title,detail,mb,count,preview,null,null);
    }}
    boolean add(String id,String category,String title,String detail,double mb,int count,boolean preview,double[] amounts,int[] counts){synchronized(JOURNAL_LOCK){
        try{
            JSONArray data=read();
            String eventId=id==null?UUID.randomUUID().toString():id;
            for(int i=0;i<data.length();i++)if(eventId.equals(data.getJSONObject(i).optString("id")))return true;
            JSONObject e=new JSONObject();
            e.put("id",eventId);e.put("category",category);e.put("title",title);e.put("detail",detail);
            e.put("timestamp",System.currentTimeMillis());e.put("mb",mb);e.put("count",count);e.put("preview",preview);
            if(amounts!=null&&counts!=null&&amounts.length==3&&counts.length==3){
                JSONArray sizes=new JSONArray(),numbers=new JSONArray();
                for(int i=0;i<3;i++){sizes.put(amounts[i]);numbers.put(counts[i]);}
                e.put("categoryMb",sizes);e.put("categoryCounts",numbers);
            }
            data.put(e);
            return store.edit().putString("events",data.toString()).commit();
        }catch(JSONException ex){return false;}
    }}
}

package com.clenova.app;

import android.content.*;
import org.json.*;
import java.util.*;

/** Private, bounded history. No IP address, SSID, or location is saved. */
final class SpeedTestHistory {
    static final String STORE="clenova_speed_history";
    private static final int LIMIT=50;
    private final SharedPreferences prefs;
    SpeedTestHistory(Context c){prefs=c.getSharedPreferences(STORE,Context.MODE_PRIVATE);}
    synchronized List<InternetSpeedEngine.Result> entries(){
        List<InternetSpeedEngine.Result> out=new ArrayList<>();
        try{JSONArray array=new JSONArray(prefs.getString("results","[]"));
            for(int i=0;i<Math.min(array.length(),LIMIT);i++)try{JSONObject o=array.getJSONObject(i);
                long time=o.getLong("time"),bytes=o.getLong("bytes");String network=o.getString("network");double down=o.getDouble("down"),up=o.getDouble("up"),ping=o.getDouble("ping"),jitter=o.getDouble("jitter");
                if(time>0&&bytes>0&&Double.isFinite(down)&&Double.isFinite(up)&&Double.isFinite(ping)&&Double.isFinite(jitter)&&down>=0&&up>=0&&ping>=0&&jitter>=0)
                    out.add(new InternetSpeedEngine.Result(time,bytes,network,down,up,ping,jitter));
            }catch(JSONException ignored){}
        }catch(JSONException ignored){}
        return out;
    }
    synchronized boolean add(InternetSpeedEngine.Result r){
        List<InternetSpeedEngine.Result> all=entries();all.add(0,r);JSONArray array=new JSONArray();
        try{for(int i=0;i<Math.min(LIMIT,all.size());i++){InternetSpeedEngine.Result v=all.get(i);
            array.put(new JSONObject().put("time",v.timestamp).put("bytes",v.bytes).put("network",v.connection).put("down",v.down).put("up",v.up).put("ping",v.ping).put("jitter",v.jitter));
        }return prefs.edit().putString("results",array.toString()).commit();}catch(JSONException e){return false;}
    }
    synchronized boolean clear(){return prefs.edit().remove("results").commit();}
}

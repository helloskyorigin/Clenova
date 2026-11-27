package com.clenova.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** One temporary local conversation, no archived chats. UI thread only. */
final class AgentChatStore {
    static final int MAX_MESSAGES=48;
    private static final String KEY="active_conversation_v2";
    static final class Message {
        final boolean user;final String text;final int action;
        Message(boolean u,String t,int a){user=u;text=t;action=a>=0&&a<AgentActions.TITLES.length?a:-1;}
    }
    static final class Chat {
        final String id;final List<Message> messages=new ArrayList<>();
        long lastActivity;
        Chat(String id,long now){this.id=id;lastActivity=now;}
    }
    private final SharedPreferences prefs;
    private Chat current;
    AgentChatStore(Context c){
        prefs=c.getSharedPreferences("clenova_agent_chats",Context.MODE_PRIVATE);
        // Legacy archives have no reliable activity timestamp. Discard rather than resurrect them.
        if(prefs.contains("history"))prefs.edit().remove("history").commit();
        try{
            String raw=prefs.getString(KEY,null);if(raw==null)return;
            JSONObject o=new JSONObject(raw);long last=o.getLong("last_activity");
            if(AgentSessionPolicy.expired(last,System.currentTimeMillis())){clear();return;}
            Chat chat=new Chat(o.getString("id"),last);JSONArray list=o.getJSONArray("messages");
            for(int j=Math.max(0,list.length()-MAX_MESSAGES);j<list.length();j++){
                JSONObject m=list.getJSONObject(j);chat.messages.add(new Message(m.optBoolean("user"),limit(m.optString("text")),m.optInt("action",-1)));
            }
            if(chat.messages.isEmpty())clear();else current=chat;
        }catch(Exception e){clear();}
    }
    private static String limit(String s){return s.length()>1600?s.substring(0,1600):s;}
    Chat current(){if(current!=null&&AgentSessionPolicy.expired(current.lastActivity,System.currentTimeMillis()))clear();return current;}
    Chat create(){return new Chat(UUID.randomUUID().toString(),System.currentTimeMillis());}
    boolean add(Chat c,boolean user,String text,int action){
        long now=System.currentTimeMillis();
        if(c==null||AgentSessionPolicy.expired(c.lastActivity,now)||(current!=null&&current!=c))return false;
        c.messages.add(new Message(user,limit(text),action));while(c.messages.size()>MAX_MESSAGES)c.messages.remove(0);
        c.lastActivity=now;current=c;return save();
    }
    boolean clear(){if(current!=null)current.messages.clear();current=null;return prefs.edit().remove(KEY).remove("history").commit();}
    private boolean save(){
        try{JSONObject o=new JSONObject();o.put("id",current.id);o.put("last_activity",current.lastActivity);JSONArray list=new JSONArray();
            for(Message m:current.messages){JSONObject v=new JSONObject();v.put("user",m.user);v.put("text",m.text);v.put("action",m.action);list.put(v);}o.put("messages",list);
            return prefs.edit().remove("history").putString(KEY,o.toString()).commit();
        }catch(Exception e){return false;}
    }
}

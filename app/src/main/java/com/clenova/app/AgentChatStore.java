package com.clenova.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Bounded local conversation history. Never stores credentials or file identifiers. */
final class AgentChatStore {
    static final int MAX_CHATS=30,MAX_MESSAGES=48;
    static final class Message {
        final boolean user;final String text;final int action;
        Message(boolean u,String t,int a){user=u;text=t;action=a>=0&&a<AgentActions.TITLES.length?a:-1;}
    }
    static final class Chat {
        final String id;final List<Message> messages=new ArrayList<>();
        Chat(String id){this.id=id;}
        String title(){for(Message m:messages)if(m.user)return m.text.length()>55?m.text.substring(0,55)+"…":m.text;return "New conversation";}
    }
    private final SharedPreferences prefs;
    private final List<Chat> chats=new ArrayList<>();
    AgentChatStore(Context c){prefs=c.getSharedPreferences("clenova_agent_chats",Context.MODE_PRIVATE);
        try{JSONArray a=new JSONArray(prefs.getString("history","[]"));for(int i=0;i<Math.min(MAX_CHATS,a.length());i++){JSONObject o=a.getJSONObject(i);Chat chat=new Chat(o.getString("id"));JSONArray list=o.getJSONArray("messages");for(int j=Math.max(0,list.length()-MAX_MESSAGES);j<list.length();j++){JSONObject m=list.getJSONObject(j);chat.messages.add(new Message(m.optBoolean("user"),limit(m.optString("text")),m.optInt("action",-1)));}if(!chat.messages.isEmpty())chats.add(chat);}}catch(Exception e){chats.clear();}
    }
    private static String limit(String s){return s.length()>1600?s.substring(0,1600):s;}
    List<Chat> list(){return new ArrayList<>(chats);}
    Chat get(String id){for(Chat c:chats)if(c.id.equals(id))return c;return null;}
    Chat create(){return new Chat(UUID.randomUUID().toString());}
    boolean add(Chat c,boolean user,String text,int action){
        c.messages.add(new Message(user,limit(text),action));while(c.messages.size()>MAX_MESSAGES)c.messages.remove(0);
        chats.remove(c);chats.add(0,c);while(chats.size()>MAX_CHATS)chats.remove(chats.size()-1);return save();
    }
    boolean delete(Chat c){chats.remove(c);return save();}
    boolean clear(){chats.clear();return save();}
    private boolean save(){try{JSONArray a=new JSONArray();for(Chat c:chats){JSONObject o=new JSONObject();o.put("id",c.id);JSONArray list=new JSONArray();for(Message m:c.messages){JSONObject v=new JSONObject();v.put("user",m.user);v.put("text",m.text);v.put("action",m.action);list.put(v);}o.put("messages",list);a.put(o);}return prefs.edit().putString("history",a.toString()).commit();}catch(Exception e){return false;}}
}

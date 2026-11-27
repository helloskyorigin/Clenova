package com.clenova.app;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;

/** Optional private HTTPS relay. No Groq API secret in the APK. */
final class AgentRelay {
    static final class Reply {final String text;final int action;Reply(String t,int a){text=t;action=a;}}
    static boolean validEndpoint(String s){
        try{URI uri=new URI(s);return "https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null&&uri.getFragment()==null&&uri.getQuery()==null&&uri.getPath().equals("/agent");}catch(Exception e){return false;}
    }
    static Reply ask(String endpoint,String token,String prompt,JSONArray context)throws Exception{
        if(!validEndpoint(endpoint)||token.length()<24||prompt.length()>600)throw new IOException("Invalid relay configuration");
        HttpsURLConnection c=(HttpsURLConnection)new URL(endpoint).openConnection();
        try{c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+token);
            byte[] request=new JSONObject().put("message",prompt).put("context",context).toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(request.length);try(OutputStream out=c.getOutputStream()){out.write(request);}
            if(c.getResponseCode()!=200)throw new IOException("Relay unavailable");
            ByteArrayOutputStream data=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){if(data.size()+n>16384)throw new IOException("Reply too large");data.write(buffer,0,n);}}
            JSONObject response=new JSONObject(new String(data.toByteArray(),StandardCharsets.UTF_8));String text=response.getString("text").trim();int action=response.optInt("action",-1);
            if(text.isEmpty()||text.length()>1200||action< -1||action>=AgentActions.TITLES.length)throw new IOException("Invalid reply");
            return new Reply(text,action);
        }finally{c.disconnect();}
    }
}

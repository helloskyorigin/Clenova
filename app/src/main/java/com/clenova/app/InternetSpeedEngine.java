package com.clenova.app;

import android.content.Context;
import android.net.*;
import android.os.SystemClock;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/** Foreground, bounded HTTPS transfer test. Only synthetic payloads leave the device. */
final class InternetSpeedEngine {
    static final String HOST="speed.cloudflare.com",BASE="https://"+HOST;
    static final int WIFI_DOWN=24000000,WIFI_UP=12000000,METERED_DOWN=6000000,METERED_UP=3000000;
    static final int LATENCY=0,DOWNLOAD=1,UPLOAD=2;
    interface Listener {void update(int stage,double speed,double ping,double jitter,long payload);}
    static final class Connection {
        final Network network;final String label;final boolean metered,validated;
        Connection(Network n,String l,boolean m,boolean v){network=n;label=l;metered=m;validated=v;}
        long budget(){return metered?METERED_DOWN+METERED_UP:WIFI_DOWN+WIFI_UP;}
    }
    static final class Result {
        final long timestamp,bytes;final String connection;final double down,up,ping,jitter;
        Result(long t,long b,String c,double d,double u,double p,double j){timestamp=t;bytes=b;connection=c;down=d;up=u;ping=p;jitter=j;}
    }
    private final ConnectivityManager manager;
    private final Connection chosen;
    private final Listener listener;
    private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor();
    private volatile HttpURLConnection active;
    private volatile boolean cancelled,timedOut;
    private long payload,deadline,lastUpdate;private double ping=-1,jitter=-1;
    InternetSpeedEngine(Context c,Connection selected,Listener listener){
        manager=c.getSystemService(ConnectivityManager.class);chosen=selected;this.listener=listener;
    }
    static Connection connection(Context c){
        ConnectivityManager m=c.getSystemService(ConnectivityManager.class);
        if(m==null)return new Connection(null,"No connection",true,false);
        try{
            Network n=m.getActiveNetwork();NetworkCapabilities cap=n==null?null:m.getNetworkCapabilities(n);
            if(cap==null)return new Connection(null,"No connection",true,false);
            String label=cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN)?"VPN":cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile data":cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"Other network";
            return new Connection(n,label,!cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        }catch(RuntimeException e){return new Connection(null,"Connection unavailable",true,false);}
    }
    void cancel(){cancelled=true;HttpURLConnection c=active;if(c!=null){Thread t=new Thread(c::disconnect,"Clenova-speed-cancel");t.setDaemon(true);t.start();}}
    private void check()throws IOException{
        if(timedOut||SystemClock.elapsedRealtime()>deadline)throw new IOException("Test time limit reached. Try again.");
        if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Test cancelled");
        if(manager==null||chosen.network==null||!chosen.network.equals(manager.getActiveNetwork()))throw new IOException("Connection changed. Start a new test.");
        NetworkCapabilities cap=manager.getNetworkCapabilities(chosen.network);
        if(cap==null||!cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))throw new IOException("Internet connection lost.");
        if(!chosen.metered&&!cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))throw new IOException("Connection became metered. Start a new test.");
    }
    private HttpURLConnection open(String path,String method)throws IOException{
        check();URLConnection raw=chosen.network.openConnection(new URL(BASE+path));
        if(!(raw instanceof javax.net.ssl.HttpsURLConnection))throw new IOException("Secure test connection unavailable");
        HttpURLConnection c=(HttpURLConnection)raw;c.setInstanceFollowRedirects(false);c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setUseCaches(false);
        c.setRequestMethod(method);c.setRequestProperty("Cache-Control","no-store, no-cache");c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("User-Agent","Clenova-SpeedTest/1.0");
        active=c;check();return c;
    }
    private void ok(HttpURLConnection c)throws IOException{
        int code=c.getResponseCode();if(code==429)throw new IOException("Test server is busy or rate limited. Try later.");
        if(code<200||code>=300)throw new IOException("Test server unavailable (HTTP "+code+").");
        String encoding=c.getContentEncoding();if(encoding!=null&&!encoding.equalsIgnoreCase("identity"))throw new IOException("Unexpected response encoding");
    }
    private void close(HttpURLConnection c){if(c!=null)c.disconnect();if(active==c)active=null;}
    private void report(int stage,double speed,boolean force){
        long now=SystemClock.elapsedRealtime();if(force||now-lastUpdate>=120){lastUpdate=now;listener.update(stage,speed,ping,jitter,payload);}
    }
    private double latency()throws IOException{
        HttpURLConnection c=null;long start=System.nanoTime();
        try{c=open("/__down?bytes=0&r="+UUID.randomUUID(),"GET");ok(c);double ms=(System.nanoTime()-start)/1e6;
            try(InputStream in=c.getInputStream()){if(in.read()!=-1){payload++;throw new IOException("Unexpected latency response");}}check();return ms;
        }finally{close(c);}
    }
    private long download(int requested,long phaseStart,long prior)throws IOException{
        HttpURLConnection c=null;long read=0;
        try{c=open("/__down?bytes="+requested+"&r="+UUID.randomUUID(),"GET");ok(c);
            try(InputStream in=c.getInputStream()){
                byte[] buffer=new byte[32768];while(read<requested){check();int n=in.read(buffer,0,(int)Math.min(buffer.length,requested-read));if(n<0)break;if(n==0)continue;
                    read+=n;payload+=n;report(DOWNLOAD,SpeedTestMath.mbps(prior+read,Math.max(1,System.nanoTime()-phaseStart)),false);
                }
            }
            if(read!=requested)throw new IOException("Incomplete download response");check();return read;
        }finally{close(c);}
    }
    private long upload(int requested,byte[] random,long phaseStart,long prior)throws IOException{
        HttpURLConnection c=null;long sent=0;
        try{c=open("/__up","POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(requested);c.setRequestProperty("Content-Type","application/octet-stream");
            try(OutputStream out=c.getOutputStream()){while(sent<requested){check();int n=(int)Math.min(random.length,requested-sent);out.write(random,0,n);sent+=n;payload+=n;}out.flush();}
            ok(c);try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n,total=0;while((n=in.read(b,0,Math.min(b.length,65536-total)))>0){check();total+=n;payload+=n;if(total>=65536)throw new IOException("Unexpected upload acknowledgement");}}
            check();report(UPLOAD,SpeedTestMath.mbps(prior+sent,Math.max(1,System.nanoTime()-phaseStart)),true);return sent;
        }finally{close(c);}
    }
    Result run()throws IOException{
        if(chosen.network==null||!chosen.validated)throw new IOException("Connect to the internet first.");
        deadline=SystemClock.elapsedRealtime()+45000;
        watchdog.schedule(()->{timedOut=true;HttpURLConnection c=active;if(c!=null)c.disconnect();},45,TimeUnit.SECONDS);
        try{
            report(LATENCY,0,true);latency();double[] times=new double[5];
            for(int i=0;i<times.length;i++){times[i]=latency();ping=SpeedTestMath.median(Arrays.copyOf(times,i+1));if(i>0)jitter=SpeedTestMath.jitter(Arrays.copyOf(times,i+1));report(LATENCY,0,true);}
            int downBudget=chosen.metered?METERED_DOWN:WIFI_DOWN,upBudget=chosen.metered?METERED_UP:WIFI_UP;
            long down=0,downStart=System.nanoTime();int chunk=256000;report(DOWNLOAD,0,true);
            while(down<downBudget){
                int n=Math.min(chunk,(int)(downBudget-down));down+=download(n,downStart,down);
                double speed=SpeedTestMath.mbps(down,Math.max(1,System.nanoTime()-downStart));report(DOWNLOAD,speed,true);
                if(System.nanoTime()-downStart>=6000000000L)break;
                chunk=SpeedTestMath.nextSize(speed,(int)(downBudget-down),4000000);
            }
            double download=SpeedTestMath.mbps(down,Math.max(1,System.nanoTime()-downStart));byte[] random=new byte[65536];new SecureRandom().nextBytes(random);
            long up=0,upStart=System.nanoTime();chunk=128000;report(UPLOAD,0,true);
            while(up<upBudget){
                int n=Math.min(chunk,(int)(upBudget-up));up+=upload(n,random,upStart,up);
                double speed=SpeedTestMath.mbps(up,Math.max(1,System.nanoTime()-upStart));report(UPLOAD,speed,true);
                if(System.nanoTime()-upStart>=6000000000L)break;
                chunk=SpeedTestMath.nextSize(speed,(int)(upBudget-up),2000000);
            }
            double upload=SpeedTestMath.mbps(up,Math.max(1,System.nanoTime()-upStart));check();
            if(down<=0||up<=0||!Double.isFinite(download)||!Double.isFinite(upload))throw new IOException("No valid speed measurement");
            return new Result(System.currentTimeMillis(),payload,chosen.label,download,upload,ping,jitter);
        }finally{watchdog.shutdownNow();HttpURLConnection c=active;if(c!=null)close(c);}
    }
}

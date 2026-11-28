package com.clenova.app;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Native four-screen speed test; all measured numbers come from actual HTTPS transfers. */
public final class InternetSpeedActivity extends Activity {
    private static final int HOME=0,TEST=1,RESULT=2,HISTORY=3;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayList<Double> samples=new ArrayList<>();
    private AppPreferences prefs;private SpeedTestHistory history;
    private InternetSpeedEngine engine;private Future<?> job;private InternetSpeedEngine.Result result;
    private ConnectivityManager connectivity;private ConnectivityManager.NetworkCallback callback;
    private int phase=HOME,stage=-1,epoch,bg,card,ink,gray,blue,pale,green,red;
    private boolean dark,foreground,destroyed,registered,fromHistory;
    private InternetSpeedEngine.Connection testConnection;
    private String notice="",connectionName="";
    private double down=-1,up=-1,ping=-1,jitter=-1,speed;
    private long payload;
    private TextView stageText,latencyText,downText,upText,jitterText,bytesText,connectionText;
    private Dial dial;private Graph graph;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=new AppPreferences(this);history=new SpeedTestHistory(this);connectivity=getSystemService(ConnectivityManager.class);
        dark=UiKit.dark(this,prefs.appearance());
        UiPolish.Palette palette=UiPolish.palette(dark);bg=palette.background;card=palette.card;ink=palette.ink;gray=palette.muted;
        blue=palette.accent;pale=palette.tint;green=palette.success;red=palette.danger;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        showHome();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String tr(String s){return AppStrings.translate(prefs.language(),s);}
    private GradientDrawable shape(int c,int r){return UiKit.shape(this,c,r);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout p,View v,int w,int h){p.addView(v,new LinearLayout.LayoutParams(w,h));}
    private void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private void gap(LinearLayout p,int n){add(p,new View(this),dp(n),dp(n));}
    private TextView text(String s,int size,int tint,boolean bold){TextView t=new TextView(this);t.setText(tr(s));UiKit.typography(t,size,tint,bold?600:400);return t;}
    private TextView center(String s,int size,int tint,boolean bold){TextView t=text(s,size,tint,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView button(String s,int tint,int fill,Runnable r){TextView t=center(s,14,tint,true);UiKit.buttonSurface(t,fill,tint,dark);t.setPadding(dp(10),dp(8),dp(10),dp(8));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->{if(!destroyed&&foreground)r.run();});return t;}
    private LinearLayout block(){LinearLayout l=col();l.setBackground(UiKit.card(this,dark));l.setPadding(dp(UiPolish.CARD_PAD_X),dp(UiPolish.CARD_PAD_Y),dp(UiPolish.CARD_PAD_X),dp(UiPolish.CARD_PAD_Y));return l;}
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(UiKit.gutter(this)),dp(10),dp(UiKit.gutter(this)),dp(12));return l;}
    private LinearLayout body(LinearLayout l){ScrollView s=new ScrollView(this);s.setVerticalScrollBarEnabled(false);s.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout b=col();s.addView(b,new ScrollView.LayoutParams(-1,-2));l.addView(s,new LinearLayout.LayoutParams(-1,0,1));return b;}
    private void header(LinearLayout l,String title,String action,String description,Runnable run){LinearLayout h=row();android.widget.ImageButton back=UiKit.headerIcon(this,"back",tr("Back"),ink,dark,() -> {if(!destroyed&&foreground)onBackPressed();});add(h,back,dp(UiPolish.ICON_TARGET),dp(UiPolish.ICON_TARGET));gap(h,8);weight(h,center(title,18,ink,true));gap(h,8);
        if(run!=null){TextView a=button(action,ink,card,run);a.setTextSize(23);a.setContentDescription(description);add(h,a,dp(UiPolish.ICON_TARGET),dp(UiPolish.ICON_TARGET));}else add(h,new View(this),dp(UiPolish.ICON_TARGET),dp(UiPolish.ICON_TARGET));add(l,h,-1,dp(UiPolish.HEADER_HEIGHT));gap(l,9);}
    private void primary(LinearLayout l,String s,Runnable r){gap(l,10);add(l,button(s,0xffffffff,blue,r),-1,dp(51));}
    private void foot(LinearLayout l,String s){gap(l,8);add(l,center(s,11,gray,false),-1,-2);}
    private void rule(LinearLayout l){View v=new View(this);v.setBackgroundColor(UiPolish.palette(dark).divider);add(l,v,-1,dp(1));}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
    private void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_SHORT).show();}
    private String num(double v){return v<0||!Double.isFinite(v)?"—":String.format(Locale.getDefault(),"%.1f",v);}
    private String mb(long b){return String.format(Locale.getDefault(),"%.2f MB",b/1000000d);}
    private String date(long time){return android.text.format.DateFormat.getMediumDateFormat(this).format(new Date(time))+" · "+android.text.format.DateFormat.getTimeFormat(this).format(new Date(time));}
    private String networkLabel(){InternetSpeedEngine.Connection c=InternetSpeedEngine.connection(this);return c.validated?c.label+(c.metered?" · Metered":" connected"):c.label.equals("No connection")?"No internet connection":c.label+" · Internet unavailable";}
    private void showHome(){
        phase=HOME;fromHistory=false;dial=null;graph=null;LinearLayout l=root();header(l,"Internet Speed Test","◷","Speed history",this::showHistory);LinearLayout b=body(l);
        connectionText=center(networkLabel(),13,blue,true);add(b,connectionText,-1,-2);gap(b,12);
        Dial go=new Dial(true);go.setContentDescription("GO, start speed test");go.setClickable(true);go.setFocusable(true);go.setOnClickListener(v->confirmStart());add(b,go,-1,dp(245));gap(b,4);add(b,center("Tap GO to test your connection",13,gray,false),-1,-2);gap(b,19);
        add(b,metrics(-1,-1,-1,-1),-1,-2);gap(b,12);
        if(!notice.isEmpty()){LinearLayout box=block();add(box,text(notice,13,gray,false),-1,-2);add(b,box,-1,-2);}
        foot(b,"Server: Cloudflare · HTTPS test");foot(b,"Speed tests consume internet data. Results depend on the network path and test server.");setContentView(l);
    }
    private LinearLayout metrics(double d,double u,double p,double j){
        LinearLayout c=block();LinearLayout speeds=row();LinearLayout a=col(),b=col();add(a,text("↓  DOWNLOAD",11,blue,true),-1,-2);gap(a,7);add(a,text(num(d),29,ink,true),-1,-2);gap(a,3);add(a,text("Mbps",11,gray,false),-1,-2);
        add(b,text("↑  UPLOAD",11,blue,true),-1,-2);gap(b,7);add(b,text(num(u),29,ink,true),-1,-2);gap(b,3);add(b,text("Mbps",11,gray,false),-1,-2);weight(speeds,a);gap(speeds,12);weight(speeds,b);add(c,speeds,-1,-2);gap(c,13);rule(c);gap(c,12);
        LinearLayout delays=row();LinearLayout x=col(),y=col();add(x,text("HTTP PING",11,gray,true),-1,-2);gap(x,5);add(x,text(num(p)+" ms",19,ink,true),-1,-2);add(y,text("JITTER",11,gray,true),-1,-2);gap(y,5);add(y,text(num(j)+" ms",19,ink,true),-1,-2);weight(delays,x);weight(delays,y);add(c,delays,-1,-2);return c;
    }
    private void confirmStart(){
        if(!foreground||engine!=null)return;
        InternetSpeedEngine.Connection c=InternetSpeedEngine.connection(this);if(!c.validated){dialog().setTitle("No internet connection").setMessage("Connect to Wi-Fi or mobile data and try again.").setPositiveButton("Done",null).show();return;}
        dialog().setTitle("Start speed test?").setMessage("Test "+c.label+" using Cloudflare's HTTPS endpoint. Up to "+mb(c.budget())+" of transfer payload plus response and network overhead; your data plan may be charged. The endpoint receives your IP address and synthetic test traffic. No files or account data are sent.").setNegativeButton("Cancel",null).setPositiveButton("Start test",(d,w)->start(c)).show();
    }
    private void start(InternetSpeedEngine.Connection approved){
        if(!foreground||destroyed||engine!=null)return;InternetSpeedEngine.Connection current=InternetSpeedEngine.connection(this);
        if(!current.validated||!Objects.equals(current.network,approved.network)||current.metered!=approved.metered){notice="Connection changed. Confirm a new test.";showHome();return;}
        testConnection=approved;phase=TEST;stage=-1;speed=0;down=up=ping=jitter=-1;payload=0;samples.clear();notice="";connectionName=approved.label;int token=++epoch;showTesting();
        InternetSpeedEngine task=new InternetSpeedEngine(this,approved,(s,v,p,j,b)->main.post(()->{if(destroyed||token!=epoch||phase!=TEST)return;update(s,v,p,j,b);}));
        engine=task;job=worker.submit(()->{
            try{InternetSpeedEngine.Result measured=task.run();main.post(()->{if(destroyed||token!=epoch||phase!=TEST)return;engine=null;job=null;result=measured;boolean saved=history.add(measured);fromHistory=false;showResult();if(!saved)toast("Result measured, but history could not be saved");});}
            catch(Exception e){main.post(()->{if(destroyed||token!=epoch||phase!=TEST)return;engine=null;job=null;String message=e instanceof java.io.IOException&&e.getMessage()!=null?e.getMessage():"Network test could not finish.";notice="Test incomplete. "+message+" No result saved.";showHome();});}
        });
    }
    private void showTesting(){
        LinearLayout l=root();header(l,"Internet Speed Test",null,null,null);LinearLayout b=body(l);
        stageText=center("Testing connection…",14,blue,true);add(b,stageText,-1,-2);gap(b,10);dial=new Dial(false);add(b,dial,-1,dp(228));gap(b,10);
        LinearLayout g=block();graph=new Graph(samples);add(g,graph,-1,dp(73));gap(g,7);add(g,text("Live measured throughput",11,gray,false),-1,-2);add(b,g,-1,-2);gap(b,11);
        LinearLayout stages=block();latencyText=stageLine(stages,"HTTP latency","Waiting");gap(stages,10);downText=stageLine(stages,"Download","Waiting");gap(stages,10);upText=stageLine(stages,"Upload","Waiting");gap(stages,10);jitterText=stageLine(stages,"Jitter","—");add(b,stages,-1,-2);gap(b,9);bytesText=center("Test payload: 0 MB",12,gray,false);add(b,bytesText,-1,-2);gap(l,8);
        add(l,button("Stop test",red,card,()->stop("Test stopped. No incomplete result saved.")),-1,dp(51));setContentView(l);
    }
    private TextView stageLine(LinearLayout p,String label,String value){LinearLayout r=row();weight(r,text(label,14,ink,true));TextView v=text(value,12,gray,false);add(r,v,-2,-2);add(p,r,-1,-2);return v;}
    private void update(int s,double v,double p,double j,long bytes){
        if(stage!=s){if(stage==InternetSpeedEngine.DOWNLOAD)down=speed;stage=s;samples.clear();}
        speed=v;ping=p;jitter=j;payload=bytes;if(s==InternetSpeedEngine.DOWNLOAD)down=v;if(s==InternetSpeedEngine.UPLOAD)up=v;
        if(s!=InternetSpeedEngine.LATENCY&&v>0){samples.add(v);if(samples.size()>60)samples.remove(0);}
        stageText.setText(s==InternetSpeedEngine.LATENCY?"Measuring HTTP latency":s==InternetSpeedEngine.DOWNLOAD?"Testing download speed":"Testing upload speed");
        latencyText.setText(p<0?"Testing…":num(p)+" ms");jitterText.setText(j<0?"—":num(j)+" ms");
        downText.setText(s==InternetSpeedEngine.DOWNLOAD?(v>0?num(v)+" Mbps":"Starting…"):down<0?"Waiting":num(down)+" Mbps");
        upText.setText(s==InternetSpeedEngine.UPLOAD?(v>0?num(v)+" Mbps":"Starting…"):up<0?"Waiting":num(up)+" Mbps");
        bytesText.setText("Test payload: "+mb(bytes));dial.setContentDescription(stageText.getText()+", "+num(v)+" Mbps");dial.invalidate();graph.invalidate();
    }
    private void stop(String reason){epoch++;if(engine!=null){engine.cancel();engine=null;}if(job!=null){job.cancel(true);job=null;}notice=reason;if(!destroyed)showHome();}
    private void showResult(){
        if(result==null){showHome();return;}phase=RESULT;dial=null;graph=null;InternetSpeedEngine.Result r=result;LinearLayout l=root();header(l,"Test results","↗","Share result",()->share(r));LinearLayout b=body(l);
        LinearLayout hero=col();hero.setGravity(Gravity.CENTER);TextView mark=center("✓",40,green,true);mark.setBackground(shape(dark?0xff193c2b:0xffe1f5e7,42));add(hero,mark,dp(75),dp(75));gap(hero,12);
        add(hero,center(r.down>=50&&r.up>=10?"Fast measured connection":"Speed test complete",24,ink,true),-1,-2);gap(hero,6);add(hero,center("Your measured HTTPS connection results.",13,gray,false),-1,-2);add(b,hero,-1,-2);gap(b,17);add(b,metrics(r.down,r.up,r.ping,r.jitter),-1,-2);gap(b,16);
        add(b,text("Connection experience",17,ink,true),-1,-2);gap(b,8);LinearLayout experience=block();experience(experience,"Browsing",r.down>=3&&r.ping<=300);rule(experience);experience(experience,"4K streaming",r.down>=25);rule(experience);experience(experience,"Video calls",r.down>=5&&r.up>=3&&r.ping<=150&&r.jitter<=30);rule(experience);experience(experience,"Online gaming",r.down>=5&&r.up>=1&&r.ping<=60&&r.jitter<=15);foot(experience,"Estimates only; actual service latency, packet loss and performance may differ.");add(b,experience,-1,-2);gap(b,11);
        LinearLayout detail=block();stageLine(detail,"Connection",r.connection);gap(detail,10);stageLine(detail,"Server","Cloudflare");gap(detail,10);stageLine(detail,"Payload transferred",mb(r.bytes));gap(detail,10);stageLine(detail,"Test time",date(r.timestamp));foot(detail,"Network overhead is not included. HTTP ping is not ICMP.");add(b,detail,-1,-2);
        primary(l,"Test again",this::confirmStart);setContentView(l);
    }
    private void experience(LinearLayout l,String label,boolean good){LinearLayout r=row();r.setPadding(0,dp(10),0,dp(10));weight(r,text(label,14,ink,true));add(r,text(good?"Likely good":"May be limited",12,good?green:gray,true),-2,-2);add(l,r,-1,-2);}
    private void share(InternetSpeedEngine.Result r){String s="CLENOVA Internet Speed Test\nDownload: "+num(r.down)+" Mbps\nUpload: "+num(r.up)+" Mbps\nHTTP ping: "+num(r.ping)+" ms\nJitter: "+num(r.jitter)+" ms\nConnection: "+r.connection+"\nServer: Cloudflare\n"+date(r.timestamp);try{startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,s),"Share speed result"));}catch(RuntimeException e){toast("No sharing app available");}}
    private void showHistory(){
        phase=HISTORY;dial=null;graph=null;List<InternetSpeedEngine.Result> all=history.entries();LinearLayout l=root();header(l,"Speed history","×","Clear speed history",()->{if(all.isEmpty()){toast("No history to clear");return;}dialog().setTitle("Clear speed history?").setMessage("All saved speed results on this device will be removed.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{if(!history.clear())toast("Could not clear history");showHistory();}).show();});LinearLayout b=body(l);
        if(all.isEmpty()){LinearLayout c=block();add(c,center("No speed tests yet",18,ink,true),-1,dp(64));add(c,center("Completed tests will appear here.",12,gray,false),-1,dp(34));add(b,c,-1,-2);}
        else{LinearLayout chart=block();add(chart,text("RECENT DOWNLOAD SPEED",11,gray,true),-1,-2);gap(chart,7);add(chart,text(num(all.get(0).down)+" Mbps",25,ink,true),-1,-2);gap(chart,10);List<Double> values=new ArrayList<>();for(int i=Math.min(12,all.size())-1;i>=0;i--)values.add(all.get(i).down);add(chart,new Graph(values),-1,dp(100));add(b,chart,-1,-2);gap(b,12);
            for(InternetSpeedEngine.Result r:all){LinearLayout c=block();LinearLayout h=row();LinearLayout a=col();add(a,text(r.connection,15,ink,true),-1,-2);gap(a,4);add(a,text(date(r.timestamp),11,gray,false),-1,-2);weight(h,a);add(h,text("›",24,gray,false),-2,-2);add(c,h,-1,-2);gap(c,10);rule(c);gap(c,10);LinearLayout details=row();weight(details,text("↓ "+num(r.down)+" Mbps",12,blue,true));weight(details,text("↑ "+num(r.up)+" Mbps",12,ink,true));add(details,text(num(r.ping)+" ms",11,gray,false),-2,-2);add(c,details,-1,-2);c.setClickable(true);c.setFocusable(true);c.setOnClickListener(v->{result=r;fromHistory=true;showResult();});add(b,c,-1,-2);gap(b,9);}}
        foot(l,"Your saved history stays on this device · latest 50 tests.");setContentView(l);
    }
    private final class Dial extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final boolean ready;
        Dial(boolean r){super(InternetSpeedActivity.this);ready=r;}
        private void paint(int c,boolean line,float w){p.setColor(c);p.setStyle(line?Paint.Style.STROKE:Paint.Style.FILL);p.setStrokeWidth(w);p.setStrokeCap(Paint.Cap.ROUND);}
        private void label(Canvas c,String s,float x,float y,int color,int size,boolean bold){paint(color,false,1);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextSize(dp(size));c.drawText(s,x,y,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.max(2,Math.min(x,y)-dp(12));
            if(ready){paint(pale,true,dp(1.4f));c.drawCircle(x,y,r,p);c.drawCircle(x,y,r*.87f,p);paint(pale,false,1);c.drawCircle(x,y,r*.78f,p);paint(blue,true,dp(2.5f));c.drawCircle(x,y,r*.73f,p);paint(blue,false,1);c.drawCircle(x,y,r*.65f,p);label(c,"GO",x,y+dp(12),0xffffffff,34,true);return;}
            float a=r-dp(4);paint(dark?0xff38383c:0xffe2e5ec,true,dp(10));c.drawArc(x-a,y-a,x+a,y+a,150,240,false,p);
            if(stage>0){float frac=(float)Math.min(1,Math.log1p(Math.max(0,speed))/Math.log1p(1000));paint(blue,true,dp(10));c.drawArc(x-a,y-a,x+a,y+a,150,240*frac,false,p);}
            paint(gray,true,dp(1));for(int i=0;i<=30;i++){double angle=Math.toRadians(150+i*8);float inner=a-dp(i%5==0?17:12),outer=a-dp(8);c.drawLine(x+(float)Math.cos(angle)*inner,y+(float)Math.sin(angle)*inner,x+(float)Math.cos(angle)*outer,y+(float)Math.sin(angle)*outer,p);}
            label(c,stage==0?"HTTP LATENCY":stage==2?"UPLOAD":"DOWNLOAD",x,y-dp(33),gray,11,true);label(c,stage==0?num(ping):speed>0?num(speed):"—",x,y+dp(15),ink,42,true);label(c,stage==0?"ms":"Mbps",x,y+dp(37),gray,13,false);
        }
    }
    private final class Graph extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final List<Double> values;
        Graph(List<Double> v){super(InternetSpeedActivity.this);values=v;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float width=getWidth(),height=getHeight(),left=dp(5),bottom=height-dp(5),top=dp(5);p.setColor(dark?0xff39393d:0xffe6e6eb);p.setStrokeWidth(dp(1));for(int i=0;i<4;i++){float y=top+(bottom-top)*i/3;c.drawLine(left,y,width-left,y,p);}
            if(values.isEmpty())return;double max=1;for(double v:values)max=Math.max(max,v);Path line=new Path();for(int i=0;i<values.size();i++){float x=values.size()==1?width/2:left+(width-2*left)*i/(values.size()-1);float y=bottom-(float)(values.get(i)/max)*(bottom-top);if(i==0)line.moveTo(x,y);else line.lineTo(x,y);}
            p.setColor(blue);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2.5f));p.setStrokeJoin(Paint.Join.ROUND);c.drawPath(line,p);p.setStyle(Paint.Style.FILL);if(values.size()==1)c.drawCircle(width/2,bottom-(float)(values.get(0)/max)*(bottom-top),dp(3),p);
        }
    }
    @Override public void onBackPressed(){if(phase==TEST){stop("Test stopped.");return;}if(phase==RESULT&&fromHistory){showHistory();return;}if(phase!=HOME){showHome();return;}finish();}
    @Override protected void onResume(){super.onResume();foreground=true;if(phase==HOME&&connectionText!=null)connectionText.setText(networkLabel());
        if(connectivity!=null&&!registered){callback=new ConnectivityManager.NetworkCallback(){@Override public void onAvailable(Network n){networkChanged();}@Override public void onLost(Network n){networkChanged();}@Override public void onCapabilitiesChanged(Network n,NetworkCapabilities c){networkChanged();}};
            try{connectivity.registerDefaultNetworkCallback(callback);registered=true;}catch(RuntimeException ignored){}
        }}
    private void networkChanged(){main.post(()->{
        if(destroyed||!foreground)return;
        if(phase==HOME&&connectionText!=null)connectionText.setText(networkLabel());
        if(phase==TEST&&testConnection!=null){InternetSpeedEngine.Connection current=InternetSpeedEngine.connection(this);if(!current.validated||!Objects.equals(current.network,testConnection.network)||(!testConnection.metered&&current.metered))stop("Connection changed. No incomplete result saved.");}
    });}
    @Override protected void onPause(){foreground=false;if(registered){try{connectivity.unregisterNetworkCallback(callback);}catch(RuntimeException ignored){}registered=false;}if(phase==TEST)stop("Test stopped because the app left the foreground.");super.onPause();}
    @Override protected void onDestroy(){destroyed=true;epoch++;if(engine!=null)engine.cancel();if(job!=null)job.cancel(true);worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}

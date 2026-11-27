package com.clenova.app;

import android.app.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** Three-screen native speaker tone utility. No automatic volume changes or repair claims. */
public final class SpeakerCleanerActivity extends Activity implements SpeakerPlayback.Listener {
    private static final int HOME=0,PLAYING=1,COMPLETE=2,TESTING=3;
    private AppPreferences prefs;
    private SpeakerPlayback playback;
    private SpeakerTone.Mode selected=SpeakerTone.Mode.QUICK,lastCompleted,activeMode;
    private int bg,card,ink,gray,blue,pale,green,red,phase=HOME;
    private boolean dark,foreground,destroyed;
    private float gain=.55f;
    private String notice="";
    private WaveView wave;
    private TextView progressStatus,progressDetail,levelLabel;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);prefs=new AppPreferences(this);
        dark=prefs.appearance().equals("Dark")||(prefs.appearance().equals("System")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=dark?0xff141416:0xfff5f5f7;card=dark?0xff242426:0xffffffff;ink=dark?0xfff4f4f6:0xff111111;gray=dark?0xffa2a2aa:0xff86868b;blue=dark?0xff409cff:0xff007aff;pale=dark?0xff19334c:0xffe6f0ff;green=dark?0xff61d883:0xff249d42;red=dark?0xffff6961:0xffe43e35;
        if(saved!=null){selected=saved.getBoolean("deep")?SpeakerTone.Mode.DEEP:SpeakerTone.Mode.QUICK;gain=Math.max(.15f,Math.min(.85f,saved.getFloat("gain",.55f)));}
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27&&!dark)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);playback=new SpeakerPlayback(this,this);showHome();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("deep",selected==SpeakerTone.Mode.DEEP);out.putFloat("gain",gain);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String tr(String s){return AppStrings.translate(prefs.language(),s);}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout p,View v,int w,int h){p.addView(v,new LinearLayout.LayoutParams(w,h));}
    private void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private void gap(LinearLayout p,int n){add(p,new View(this),dp(n),dp(n));}
    private TextView text(String s,int size,int tint,boolean bold){TextView t=new TextView(this);t.setText(tr(s));t.setTextSize(size);t.setTextColor(tint);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private TextView center(String s,int size,int tint,boolean bold){TextView t=text(s,size,tint,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView button(String s,int tint,int fill,Runnable action){TextView t=center(s,15,tint,true);t.setBackground(shape(fill,26));t.setPadding(dp(10),dp(8),dp(10),dp(8));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->{if(!destroyed&&foreground)action.run();});return t;}
    private LinearLayout block(){LinearLayout l=col();l.setBackground(shape(card,22));l.setPadding(dp(14),dp(12),dp(14),dp(12));return l;}
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(18),dp(10),dp(18),dp(12));return l;}
    private LinearLayout body(LinearLayout l){ScrollView s=new ScrollView(this);s.setVerticalScrollBarEnabled(false);s.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout b=col();s.addView(b,new ScrollView.LayoutParams(-1,-2));l.addView(s,new LinearLayout.LayoutParams(-1,0,1));return b;}
    private void header(LinearLayout l,boolean info){LinearLayout h=row();TextView back=button("‹",ink,card,this::onBackPressed);back.setTextSize(27);back.setContentDescription("Back");add(h,back,dp(44),dp(44));gap(h,8);weight(h,center("Speaker Cleaner",19,ink,true));gap(h,8);if(info){TextView b=button("ⓘ",ink,card,this::showInfo);b.setTextSize(23);b.setContentDescription("Speaker cleaner information");add(h,b,dp(44),dp(44));}else add(h,new View(this),dp(44),dp(44));add(l,h,-1,dp(52));gap(l,8);}
    private void primary(LinearLayout l,String s,Runnable r){gap(l,9);add(l,button(s,0xffffffff,blue,r),-1,dp(52));}
    private void foot(LinearLayout l,String s){gap(l,8);add(l,center(s,11,gray,false),-1,-2);}
    private void rule(LinearLayout l){View v=new View(this);v.setBackgroundColor(dark?0xff38383c:0xffededf0);add(l,v,-1,dp(1));}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
    private void showInfo(){dialog().setTitle("About speaker cleaning").setMessage("Low-frequency tones may help move small droplets near the speaker opening. Results vary by device; the app cannot detect water, dust or speaker damage. Keep the speaker pointing down, away from your ears. Disconnect external audio devices, use a low comfortable volume, and stop if uncomfortable. Do not use a damaged or submerged phone; follow the manufacturer’s drying guidance.").setPositiveButton("Done",null).show();}
    private void showHome(){
        phase=HOME;wave=null;LinearLayout l=root();header(l,true);LinearLayout b=body(l);
        WaveView hero=new WaveView(false);add(b,hero,-1,dp(205));gap(b,5);add(b,center("Clearer sound.",27,ink,true),-1,-2);gap(b,8);add(b,center("Sound waves may help dislodge trapped water from your speaker.",13,gray,false),-1,-2);gap(b,15);
        LinearLayout options=block();options.setPadding(dp(6),dp(6),dp(6),dp(6));modeRow(options,SpeakerTone.Mode.QUICK,"15-second sound cycle","ϟ");gap(options,3);modeRow(options,SpeakerTone.Mode.DEEP,"30-second sound cycle","≈");add(b,options,-1,-2);gap(b,11);
        LinearLayout volume=block();LinearLayout title=row();weight(title,text("Sound intensity",14,ink,true));levelLabel=text(levelText(),12,blue,true);add(title,levelLabel,-2,-2);add(volume,title,-1,-2);
        SeekBar slider=new SeekBar(this);slider.setMax(70);slider.setProgress(Math.round(gain*100)-15);slider.setProgressTintList(android.content.res.ColorStateList.valueOf(blue));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(blue));slider.setContentDescription("Sound intensity");slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){gain=(p+15)/100f;levelLabel.setText(levelText());}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});add(volume,slider,-1,dp(38));add(volume,text("Start low. Use your phone’s media volume buttons.",11,gray,false),-1,-2);add(b,volume,-1,-2);gap(b,10);
        add(b,text("Disconnect headphones and keep the speaker away from your ears.",12,gray,false),-1,-2);
        if(!notice.isEmpty()){gap(b,10);LinearLayout msg=block();add(msg,text(notice,13,gray,false),-1,-2);add(b,msg,-1,-2);}
        primary(l,"Start cleaning",()->confirmStart(selected));foot(l,"No internet required.");setContentView(l);
    }
    private String levelText(){return Math.round(gain*100)+"% tone level";}
    private void modeRow(LinearLayout l,SpeakerTone.Mode m,String detail,String symbol){LinearLayout r=row();r.setBackground(shape(selected==m?pale:card,15));r.setPadding(dp(12),dp(10),dp(12),dp(10));add(r,center(symbol,26,blue,true),dp(33),dp(38));gap(r,9);LinearLayout words=col();add(words,text(m.label,15,ink,true),-1,-2);gap(words,5);add(words,text(detail,12,gray,false),-1,-2);weight(r,words);add(r,text(selected==m?"●":"○",23,selected==m?blue:gray,true),-2,-2);r.setClickable(true);r.setFocusable(true);r.setContentDescription(m.label+", "+detail+(selected==m?", selected":""));r.setOnClickListener(v->{selected=m;showHome();});add(l,r,-1,-2);}
    private void confirmStart(SpeakerTone.Mode m){
        if(!foreground||playback.active())return;String blocked=playback.readyReason();if(!blocked.isEmpty()){dialog().setTitle("Check audio output").setMessage(blocked).setPositiveButton("Done",null).show();return;}
        dialog().setTitle(m==SpeakerTone.Mode.TEST?"Test speaker?":"Ready to start?").setMessage("Keep the phone away from your ears, speaker facing down, and use a comfortable media volume. Stop if the sound is uncomfortable. This is a sound cycle, not a repair or water detector.").setNegativeButton("Cancel",null).setPositiveButton("Start",(d,w)->{if(!foreground||destroyed)return;notice="";activeMode=m;playback.start(m,gain);}).show();
    }
    @Override public void preparing(){if(destroyed)return;phase=activeMode==SpeakerTone.Mode.TEST?TESTING:PLAYING;showPlaying();}
    private void showPlaying(){
        LinearLayout l=root();header(l,false);LinearLayout b=body(l);wave=new WaveView(true);wave.setTime(0,activeMode.seconds*1000L);add(b,wave,-1,dp(250));gap(b,12);progressStatus=center("Preparing sound…",23,ink,true);add(b,progressStatus,-1,-2);gap(b,8);add(b,center(activeMode==SpeakerTone.Mode.TEST?"Listen at a comfortable volume.":"Keep your speaker facing downward.",13,gray,false),-1,-2);gap(b,22);
        LinearLayout card0=block();LinearLayout r=row();add(r,center("≈",30,blue,true),dp(34),dp(46));gap(r,9);LinearLayout copy=col();add(copy,text(activeMode==SpeakerTone.Mode.TEST?"Speaker test":"Frequency sweep",15,ink,true),-1,-2);gap(copy,5);progressDetail=text("Checking speaker route",12,gray,false);add(copy,progressDetail,-1,-2);weight(r,copy);add(card0,r,-1,-2);add(b,card0,-1,-2);
        gap(l,8);add(l,button(activeMode==SpeakerTone.Mode.TEST?"Stop test":"Stop cleaning",red,card,()->playback.stop("Sound stopped.")),-1,dp(52));foot(l,"Stop immediately if the sound is uncomfortable.");setContentView(l);
    }
    @Override public void progress(long played,long total){if(destroyed||wave==null)return;wave.setTime(played,total);if(played>0){progressStatus.setText(activeMode==SpeakerTone.Mode.TEST?"Testing speaker":"Sound waves active");progressDetail.setText(activeMode==SpeakerTone.Mode.TEST?"Playing test tone":"Playing cleaning tones");}}
    @Override public void finished(SpeakerTone.Mode mode){
        if(destroyed)return;wave=null;
        if(mode==SpeakerTone.Mode.TEST){notice="Test tone completed.";if(lastCompleted!=null)showComplete();else showHome();return;}
        lastCompleted=mode;notice="";showComplete();
    }
    @Override public void stopped(String reason){
        if(destroyed)return;wave=null;notice=reason;
        if(activeMode==SpeakerTone.Mode.TEST&&lastCompleted!=null)showComplete();else showHome();
    }
    private void showComplete(){
        if(lastCompleted==null){showHome();return;}phase=COMPLETE;wave=null;LinearLayout l=root();header(l,false);LinearLayout b=body(l);LinearLayout hero=col();hero.setGravity(Gravity.CENTER);gap(hero,25);
        TextView done=center("✓",48,0xffffffff,true);done.setBackground(shape(0xff34c759,50));add(hero,done,dp(90),dp(90));gap(hero,18);add(hero,center("Cycle complete!",27,ink,true),-1,-2);gap(hero,7);add(hero,center("Check how your speaker sounds now.",14,gray,false),-1,-2);add(b,hero,-1,-2);gap(b,27);
        LinearLayout summary=block();LinearLayout test=row();LinearLayout labels=col();add(labels,text("Test your speaker",16,ink,true),-1,-2);gap(labels,5);add(labels,text("Play a short test sound",12,gray,false),-1,-2);weight(test,labels);add(test,button("▶",blue,pale,()->confirmStart(SpeakerTone.Mode.TEST)),dp(44),dp(44));add(summary,test,-1,-2);gap(summary,14);rule(summary);gap(summary,13);metric(summary,"Cleaning cycle",lastCompleted.label);gap(summary,10);metric(summary,"Duration",lastCompleted.seconds+" seconds");add(b,summary,-1,-2);gap(b,11);
        add(b,center("Cycle finished; water removal or speaker improvement is not measured.",12,gray,false),-1,-2);if(!notice.isEmpty()){gap(b,10);add(b,center(notice,12,gray,false),-1,-2);}
        primary(l,"Done",this::finish);gap(l,8);add(l,button("Clean again",blue,card,()->{lastCompleted=null;notice="";showHome();}),-1,dp(49));setContentView(l);
    }
    private void metric(LinearLayout l,String label,String value){LinearLayout r=row();weight(r,text(label,13,gray,false));add(r,text(value,14,ink,true),-2,-2);add(l,r,-1,-2);}
    private final class WaveView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final boolean live;
        private long played,total=15000;
        WaveView(boolean running){super(SpeakerCleanerActivity.this);live=running;setContentDescription(running?"Cleaning countdown":"Speaker sound waves");}
        void setTime(long p,long t){played=Math.max(0,Math.min(p,t));total=Math.max(1,t);setContentDescription(((total-played+999)/1000)+" seconds remaining");invalidate();}
        private void color(int c,boolean stroke,float w){paint.setColor(c);paint.setStyle(stroke?Paint.Style.STROKE:Paint.Style.FILL);paint.setStrokeWidth(w);paint.setStrokeCap(Paint.Cap.ROUND);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float x=getWidth()/2f,y=getHeight()/2f,r=Math.max(1,Math.min(x,y)-dp(13));
            if(live){
                color(dark?0xff34343a:0xffe0e4eb,true,dp(9));canvas.drawCircle(x,y,r-dp(13),paint);
                color(blue,true,dp(9));canvas.drawArc(x-r+dp(13),y-r+dp(13),x+r-dp(13),y+r-dp(13),-90,360f*played/total,false,paint);
                long seconds=(total-played+999)/1000;paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.DEFAULT);paint.setColor(gray);paint.setTextSize(dp(13));canvas.drawText(activeMode==SpeakerTone.Mode.TEST?"Testing":"Cleaning",x,y-dp(36),paint);
                paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setColor(ink);paint.setTextSize(dp(44));canvas.drawText(String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),x,y+dp(10),paint);
                paint.setColor(blue);for(int i=0;i<13;i++){float h=dp(6)+(played>0?dp(22)*(float)Math.abs(Math.sin(played/130d+i*.7)):0);float pos=x+(i-6)*dp(7);canvas.drawRoundRect(pos-dp(2),y+dp(37)-h/2,pos+dp(2),y+dp(37)+h/2,dp(2),dp(2),paint);}return;
            }
            color(pale,false,1);canvas.drawCircle(x,y,r*.65f,paint);
            for(int n=0;n<3;n++){color(blue,true,dp(1.3f));paint.setAlpha(20+n*12);canvas.drawCircle(x,y,r*(.98f-n*.14f),paint);}paint.setAlpha(255);
            float w=dp(68),h=dp(82);color(blue,false,1);canvas.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,dp(18),dp(18),paint);
            color(0xffc9e1ff,false,1);canvas.drawCircle(x,y-dp(23),dp(6),paint);color(0xff0053b7,false,1);canvas.drawCircle(x,y+dp(12),dp(23),paint);color(0xff469aff,false,1);canvas.drawCircle(x,y+dp(12),dp(16),paint);color(0xffd8e9ff,false,1);canvas.drawCircle(x,y+dp(12),dp(6),paint);
            color(blue,true,dp(2.5f));paint.setAlpha(150);for(int n=0;n<2;n++){float offset=dp(45+n*12),radius=dp(18+n*12);canvas.drawArc(x-offset-radius,y-radius,x-offset+radius,y+radius,130,100,false,paint);canvas.drawArc(x+offset-radius,y-radius,x+offset+radius,y+radius,-50,100,false,paint);}paint.setAlpha(255);
        }
    }
    @Override public void onBackPressed(){if(playback.active()){playback.stop("Sound stopped.");return;}if(phase==COMPLETE){lastCompleted=null;notice="";showHome();return;}finish();}
    @Override protected void onResume(){super.onResume();foreground=true;}
    @Override protected void onPause(){foreground=false;playback.stop("Sound stopped because the app left the foreground.");super.onPause();}
    @Override protected void onStop(){playback.stop("Sound stopped.");super.onStop();}
    @Override protected void onDestroy(){destroyed=true;playback.release();super.onDestroy();}
}

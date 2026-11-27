package com.clenova.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

/** Actual stage completion drives the ring and progress card; no simulated scan timer. */
final class PreparingPhoneView extends ScrollView {
    static final int BG=0xfff5f5f7,BLUE=0xff007aff,GREEN=0xff258553,INK=0xff111111,GRAY=0xff777780;
    private final Ring ring;
    private final TextView heading,description,status,detail,percent,icon;
    private final ProgressBar progress;
    private final Button retry;
    private static final String[] STAGES={"Checking device access","Reading device storage","Preparing photos","Preparing videos","Personalizing Home","Setup complete"};
    private static final String[] DETAILS={"Respecting your permissions","Understanding your space","Reading accessible media metadata","Reading accessible media metadata","Preparing app and access overview","Opening your Home"};
    PreparingPhoneView(Context c,Runnable retryAction){
        super(c);setFillViewport(true);setVerticalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);setBackgroundColor(BG);setLayoutDirection(LAYOUT_DIRECTION_LTR);
        LinearLayout root=col();root.setPadding(dp(24),dp(18),dp(24),dp(26));addView(root,new ScrollView.LayoutParams(-1,-2));
        ring=new Ring(c);ring.setMinimumHeight(dp(300));root.addView(ring,new LinearLayout.LayoutParams(-1,dp(350),1));
        heading=label("Your phone.\nAlmost ready.",30,INK,650);heading.setLetterSpacing(-.04f);append(root,heading,8);
        description=label("Preparing your personal space.\nEverything, right where you need it.",13,GRAY,400);description.setLineSpacing(dp(3),1);append(root,description,14);
        LinearLayout panel=col();panel.setPadding(dp(16),dp(16),dp(16),dp(16));panel.setBackground(shape(Color.WHITE,22));append(root,panel,24);
        LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);panel.addView(row,new LinearLayout.LayoutParams(-1,-2));
        icon=label("⌗",23,BLUE,600);icon.setBackground(shape(0xffe8f2ff,11));icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(icon,new LinearLayout.LayoutParams(dp(35),dp(35)));
        LinearLayout copy=col();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=dp(10);row.addView(copy,cp);
        status=label(STAGES[0],13,0xff152338,600);status.setGravity(Gravity.START);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);append(copy,status,0);
        detail=label(DETAILS[0],11,GRAY,400);detail.setGravity(Gravity.START);append(copy,detail,4);
        percent=label("0%",16,BLUE,650);LinearLayout.LayoutParams pc=new LinearLayout.LayoutParams(-2,-2);pc.leftMargin=dp(8);row.addView(percent,pc);
        progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(DeviceOverview.STAGES);progress.setProgressTintList(ColorStateList.valueOf(BLUE));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xffe9edf3));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(6));pp.topMargin=dp(16);panel.addView(progress,pp);
        append(root,label("✓  Your data stays on your device.",11,GREEN,500),23);
        retry=new Button(c);retry.setAllCaps(false);retry.setText("Retry");retry.setTextColor(BLUE);retry.setVisibility(GONE);retry.setOnClickListener(v->retryAction.run());append(root,retry,8);
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(LinearLayout.VERTICAL);return l;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static Typeface font(int w){return Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,w,false):Typeface.create(w>=500?"sans-serif-medium":"sans-serif",Typeface.NORMAL);}
    private TextView label(String t,int sp,int color,int w){TextView v=new TextView(getContext());v.setText(t);v.setTextSize(sp);v.setTextColor(color);v.setTypeface(font(w));v.setGravity(Gravity.CENTER);v.setIncludeFontPadding(false);return v;}
    private void append(LinearLayout l,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);l.addView(v,p);}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private boolean motion(){AccessibilityManager a=getContext().getSystemService(AccessibilityManager.class);return ValueAnimator.areAnimatorsEnabled()&&(a==null||!a.isTouchExplorationEnabled());}
    void update(int stage,DeviceOverview.Snapshot snapshot){
        retry.setVisibility(GONE);stage=Math.max(0,Math.min(DeviceOverview.STAGES,stage));
        boolean ready=stage==DeviceOverview.STAGES;
        ring.update(snapshot,ready,motion());progress.setProgress(stage,motion());percent.setText(stage*100/DeviceOverview.STAGES+"%");
        status.setText(STAGES[stage]);detail.setText(DETAILS[stage]);
        if(ready){
            heading.setText("You're all set.\nWelcome to CLENOVA.");description.setText(snapshot.partial()?"Your available data is ready.\nManage more access anytime.":"A smarter way to care for your phone.\nAlways in your control.");
            status.setText("Setup complete");detail.setText(snapshot.partial()?"Opening Home · Limited overview":"Opening your Home");
            icon.setText("✓");icon.setTextColor(GREEN);icon.setBackground(shape(0xffe1f3e8,11));percent.setTextColor(GREEN);progress.setProgressTintList(ColorStateList.valueOf(GREEN));
            if(motion()){heading.setAlpha(0);heading.setTranslationY(dp(8));heading.animate().alpha(1).translationY(0).setDuration(350).start();}
        }
    }
    void error(String message){status.setText("Couldn't finish setup");detail.setText(message);retry.setVisibility(VISIBLE);}
    private static final class Ring extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
        private final int[] colors={BLUE,0xff7caff1,0xffa8bfdc,0xffe1e5ed};
        private final Shader halo=new RadialGradient(162,160,152,0xffd7e8ff,0x00f5f5f7,Shader.TileMode.CLAMP);
        private final Shader blue=new LinearGradient(50,40,274,270,0xff76bcff,0xff0065e6,Shader.TileMode.CLAMP);
        private final float[] sweeps=new float[4];private DeviceOverview.Snapshot data=new DeviceOverview.Snapshot();private ValueAnimator animator;private boolean ready;
        Ring(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setContentDescription("Preparing your device");}
        void update(DeviceOverview.Snapshot s,boolean done,boolean animate){
            data=s;ready=done;setContentDescription(done?"Setup complete. "+(s.storageKnown()?DeviceOverview.format(s.free)+" available.":"Storage unavailable."):"Reading available device data");
            if(animator!=null)animator.cancel();
            float[] from=sweeps.clone(),to=new float[4];long[] parts=s.segments();
            if(s.storageKnown())for(int i=0;i<4;i++)to[i]=(float)(360d*parts[i]/s.total);
            if(!animate){System.arraycopy(to,0,sweeps,0,4);invalidate();return;}
            animator=ValueAnimator.ofFloat(0,1);animator.setDuration(460);animator.setInterpolator(new DecelerateInterpolator());animator.addUpdateListener(a->{float v=(float)a.getAnimatedValue();for(int i=0;i<4;i++)sweeps[i]=from[i]+(to[i]-from[i])*v;invalidate();});animator.start();
        }
        @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
        private void fill(int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
        private void label(Canvas c,String text,float x,float y,int size,int color,int weight){fill(color);p.setTypeface(font(weight));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(text,x,y,p);}
        private void star(Canvas c,float x,float y,float r,int color){fill(color);path.reset();path.moveTo(x,y-r);path.quadTo(x+r*.22f,y-r*.22f,x+r,y);path.quadTo(x+r*.22f,y+r*.22f,x,y+r);path.quadTo(x-r*.22f,y+r*.22f,x-r,y);path.quadTo(x-r*.22f,y-r*.22f,x,y-r);path.close();c.drawPath(path,p);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float scale=Math.min(getWidth()/324f,getHeight()/350f);if(scale<=0)return;c.save();c.translate((getWidth()-324*scale)/2,(getHeight()-350*scale)/2);c.scale(scale,scale);
            fill(Color.WHITE);p.setShader(halo);c.drawCircle(162,160,151,p);p.setShader(null);label(c,"C L E N O V A",162,23,11,0xff9696a0,650);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(25);p.setStrokeCap(Paint.Cap.BUTT);p.setColor(0xffe2e6ed);c.drawCircle(162,161,105,p);
            RectF arc=new RectF(57,56,267,266);float start=-90;
            for(int i=0;i<4;i++){float sweep=sweeps[i];p.setShader(i==0?blue:null);p.setColor(colors[i]);if(i==0)p.setShader(blue);if(sweep>0)c.drawArc(arc,start+Math.min(1.5f,sweep/4),Math.max(0,sweep-Math.min(3,sweep/2)),false,p);start+=sweep;}p.setShader(null);
            if(ready){fill(0xffdcefe5);c.drawCircle(162,125,29,p);fill(GREEN);c.drawCircle(162,125,23,p);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);path.reset();path.moveTo(151,125);path.lineTo(159,133);path.lineTo(174,117);c.drawPath(path,p);}
            else{star(c,162,127,25,BLUE);star(c,185,111,13,0xff84baff);}
            label(c,ready?"All set.":"Getting ready",162,184,27,INK,650);label(c,data.storageKnown()?DeviceOverview.format(data.free)+" available":ready?"Available data prepared":"Understanding your phone",162,207,11,0xff858995,400);
            if(data.storageKnown()){String[] names={"Photos","Videos","Other used","Available"};float[] x={33,103,173,255};for(int i=0;i<4;i++){if(i==0&&(!data.photos.available||!data.breakdownKnown())||i==1&&(!data.videos.available||!data.breakdownKnown()))continue;fill(colors[i]);c.drawCircle(x[i]-8,316,4,p);label(c,names[i],x[i]+22,320,10,0xff667386,500);}}
            c.restore();
        }
    }
}

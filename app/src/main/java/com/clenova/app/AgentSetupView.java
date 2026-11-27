package com.clenova.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native Agent setup. A check is shown only after the actual local preference is saved. */
final class AgentSetupView extends ScrollView {
    private static final int BG=0xfff5f5f7, BLUE=0xff007aff, INK=0xff152638, GRAY=0xff777780,GREEN=0xff258553;
    private final AppPreferences prefs;
    private final Runnable finished;
    private final Hero hero;
    private final TextView status,heading,description,notice;
    private final Button primary,skip;
    private final View[] rows=new View[3];
    private final TextView[] checks=new TextView[3],details=new TextView[3];
    private ValueAnimator pulse;
    private boolean active,confirmed,leaving;

    AgentSetupView(Context context,AppPreferences preferences,boolean restoredConfirmation,Runnable next){
        super(context);prefs=preferences;finished=next;
        setFillViewport(true);setBackgroundColor(BG);setVerticalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);setLayoutDirection(LAYOUT_DIRECTION_LTR);
        LinearLayout root=column();root.setPadding(dp(24),dp(12),dp(24),dp(22));addView(root,new ScrollView.LayoutParams(-1,-2));
        LinearLayout stage=column();stage.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(stage,new LinearLayout.LayoutParams(-1,-2,1f));
        hero=new Hero(context);stage.addView(hero,new LinearLayout.LayoutParams(-1,dp(154)));
        status=label("Ready to activate",14,BLUE,600);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);append(stage,status,3);
        String[] titles={"Smart discovery","Proactive recommendations","Approval protection"};
        String[] subtitles={"Find what you no longer need","Review opportunities when you scan","You always make the final decision"};
        for(int i=0;i<3;i++){
            LinearLayout line=new LinearLayout(context);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setMinimumHeight(dp(52));line.setPadding(dp(11),dp(9),dp(11),dp(9));line.setBackground(gradient(0xffffffff,0xffedf3fb,17));
            FrameLayout tile=new FrameLayout(context);tile.setBackground(shape(i==2?0xffe1f3e8:0xffdceaff,10));
            tile.addView(new Glyph(context,i),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));line.addView(tile,new LinearLayout.LayoutParams(dp(32),dp(32)));
            LinearLayout copy=column();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.leftMargin=dp(12);line.addView(copy,cp);
            TextView title=label(titles[i],13,INK,600);title.setGravity(Gravity.START);append(copy,title,0);
            details[i]=label(subtitles[i],11,0xff7d8592,400);details[i].setGravity(Gravity.START);append(copy,details[i],4);
            TextView check=label("✓",14,GREEN,700);check.setBackground(shape(0xffe0f3e7,20));check.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);check.setVisibility(INVISIBLE);
            LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(dp(22),dp(22));cl.leftMargin=dp(6);line.addView(check,cl);checks[i]=check;rows[i]=line;
            append(stage,line,i==0?20:9);
        }
        heading=label("Meet your new\neveryday Agent.",29,0xff111111,650);heading.setLetterSpacing(-.037f);append(root,heading,22);
        description=label("Less managing. More living.\nLet your Agent handle the details.",13,GRAY,400);description.setLineSpacing(dp(3),1);append(root,description,17);
        TextView protection=label("✓  Nothing changes without your approval.",11,GREEN,500);append(root,protection,19);
        notice=label("On-demand scans available. Scheduled scans aren't active yet.",11,GRAY,400);append(root,notice,9);
        primary=button("Activate Agent",true);append(root,primary,19);
        skip=button("Not now",false);append(root,skip,4);
        primary.setOnClickListener(v->activateOrContinue());skip.setOnClickListener(v->selectManual());
        if(restoredConfirmation&&prefs.agentConfigured()&&prefs.autopilot())showConfirmed(false);
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(LinearLayout.VERTICAL);return l;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static Typeface font(int weight){return Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,weight,false):Typeface.create(weight>=500?"sans-serif-medium":"sans-serif",Typeface.NORMAL);}
    private TextView label(String text,int size,int color,int weight){TextView t=new TextView(getContext());t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setTypeface(font(weight));t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.WHITE);return d;}
    private void append(LinearLayout parent,View child,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);parent.addView(child,lp);}
    private Button button(String text,boolean filled){Button b=new Button(getContext());b.setText(text);b.setAllCaps(false);b.setTextSize(filled?16:13);b.setTypeface(font(filled?600:500));b.setTextColor(filled?Color.WHITE:GRAY);b.setLetterSpacing(0);b.setMinHeight(dp(filled?54:44));b.setMinimumHeight(dp(filled?54:44));b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setStateListAnimator(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(filled?0x33ffffff:0x11007aff),shape(filled?BLUE:Color.TRANSPARENT,40),shape(Color.WHITE,40)));return b;}
    private boolean reduce(){AccessibilityManager a=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);return !ValueAnimator.areAnimatorsEnabled()||(a!=null&&a.isTouchExplorationEnabled());}
    boolean confirmed(){return confirmed;}
    private void activateOrContinue(){
        if(leaving)return;
        if(confirmed){leave();return;}
        primary.setEnabled(false);skip.setEnabled(false);
        if(!prefs.saveAgentSetup(true)){notice.setText("Couldn't save your preferences. Please try again.");primary.setEnabled(true);skip.setEnabled(true);return;}
        showConfirmed(true);
    }
    private void selectManual(){
        if(leaving||confirmed)return;
        primary.setEnabled(false);skip.setEnabled(false);
        if(!prefs.saveAgentSetup(false)){notice.setText("Couldn't save your preferences. Please try again.");primary.setEnabled(true);skip.setEnabled(true);return;}
        leave();
    }
    private void leave(){if(leaving)return;leaving=true;setActive(false);finished.run();}
    private void showConfirmed(boolean animate){
        confirmed=true;status.setText("Agent activated");status.setTextColor(GREEN);hero.setConfirmed(true);
        heading.setText("Your Agent.\nReady for you.");description.setText("Your preferences are set.\nYou stay in control.");
        // This is a saved mode and an available on-demand agent, not a completed scan.
        details[0].setText("Discovery preferences saved");details[1].setText("On-demand reviews available");details[2].setText("Approval is always required");
        notice.setText("Scheduled scanning is not active yet.");
        primary.setText("Continue");primary.setEnabled(true);skip.setVisibility(INVISIBLE);skip.setEnabled(false);
        for(int i=0;i<3;i++){TextView check=checks[i];check.animate().cancel();check.setVisibility(VISIBLE);
            if(animate&&!reduce()){check.setScaleX(.6f);check.setScaleY(.6f);check.setAlpha(0);check.animate().scaleX(1).scaleY(1).alpha(1).setStartDelay(i*180L).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();}
            else{check.setAlpha(1);check.setScaleX(1);check.setScaleY(1);}
        }
    }
    void setActive(boolean value){active=value;syncPulse();}
    private void syncPulse(){
        if(!active||!isAttachedToWindow()||!hasWindowFocus()||leaving||reduce()){
            if(pulse!=null){pulse.removeAllUpdateListeners();pulse.cancel();pulse=null;}hero.setTime(0);return;
        }
        if(pulse!=null)return;
        pulse=ValueAnimator.ofFloat(0,1);pulse.setDuration(4000);pulse.setRepeatCount(ValueAnimator.INFINITE);pulse.setInterpolator(new LinearInterpolator());pulse.addUpdateListener(a->hero.setTime((float)a.getAnimatedValue()));pulse.start();
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);syncPulse();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();syncPulse();
        if(!reduce()&&!confirmed){for(int i=0;i<rows.length;i++){View row=rows[i];row.setAlpha(0);row.setTranslationY(dp(13));row.animate().alpha(1).translationY(0).setStartDelay(i*130L).setDuration(440).setInterpolator(new DecelerateInterpolator()).start();}}
    }
    @Override protected void onDetachedFromWindow(){active=false;syncPulse();for(View row:rows)row.animate().cancel();for(View check:checks)check.animate().cancel();super.onDetachedFromWindow();}
    private static final class Hero extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path(),clip=new Path();
        private final Shader blue=new LinearGradient(132,44,228,140,new int[]{0xff82c9ff,0xff258dff,0xff005bd6},new float[]{0,.45f,1},Shader.TileMode.CLAMP);
        private final Shader gloss=new LinearGradient(0,44,0,140,0x88ffffff,0x00ffffff,Shader.TileMode.CLAMP);
        private final Shader halo=new RadialGradient(180,92,133,0xe6b7d8ff,0x00f5f5f7,Shader.TileMode.CLAMP);
        private final Shader beam=new LinearGradient(-18,0,18,0,new int[]{0x00ffffff,0x66ffffff,0x00ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP);
        private float time;private boolean confirmed;
        Hero(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void setTime(float t){time=t;invalidate();}
        void setConfirmed(boolean value){confirmed=value;invalidate();}
        private void fill(int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
        private void spark(Canvas c,float x,float y,float r,int color){fill(color);path.reset();path.moveTo(x,y-r);path.quadTo(x+r*.22f,y-r*.22f,x+r,y);path.quadTo(x+r*.22f,y+r*.22f,x,y+r);path.quadTo(x-r*.22f,y+r*.22f,x-r,y);path.quadTo(x-r*.22f,y-r*.22f,x,y-r);path.close();c.drawPath(path,p);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float scale=Math.min(getWidth()/360f,getHeight()/154f);if(scale<=0)return;c.save();c.translate((getWidth()-360*scale)/2,(getHeight()-154*scale)/2);c.scale(scale,scale);
            float wave=(float)(.5-.5*Math.cos(time*Math.PI*2));fill(Color.WHITE);p.setShader(halo);p.setAlpha((int)(175+80*wave));c.drawCircle(180,92,133,p);p.setShader(null);p.setAlpha(255);
            c.translate(0,-6*wave);fill(0x150069ef);c.drawRoundRect(128,49,232,151,31,31,p);fill(0xff075bc5);c.drawRoundRect(132,49,228,145,29,29,p);
            fill(Color.WHITE);p.setShader(blue);c.drawRoundRect(132,44,228,140,29,29,p);p.setShader(gloss);c.drawRoundRect(133,45,227,139,28,28,p);p.setShader(null);
            spark(c,180,92,27,Color.WHITE);spark(c,204,73,12,0xffd9efff);
            if(time>.25f&&time<.78f){c.save();clip.reset();clip.addRoundRect(132,44,228,140,29,29,Path.Direction.CW);c.clipPath(clip);c.translate(104+160*(time-.25f)/.53f,0);fill(Color.WHITE);p.setShader(beam);c.drawRect(-18,44,18,140,p);p.setShader(null);c.restore();}
            if(confirmed){fill(0xffe1f3e8);c.drawCircle(225,136,13,p);fill(GREEN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);path.reset();path.moveTo(219,136);path.lineTo(223,140);path.lineTo(231,132);c.drawPath(path,p);}
            c.restore();
        }
    }
    private static final class Glyph extends View{
        private final int kind;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
        Glyph(Context c,int kind){super(c);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setColor(kind==2?GREEN:BLUE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);path.reset();
            if(kind==0){c.drawCircle(10,10,6,p);c.drawLine(14.5f,14.5f,20,20,p);}
            else if(kind==1){path.moveTo(2,12);path.lineTo(7,12);path.lineTo(10,4);path.lineTo(14,20);path.lineTo(17,11);path.lineTo(22,11);c.drawPath(path,p);}
            else{path.moveTo(12,2);path.lineTo(21,6);path.lineTo(21,12);path.quadTo(21,18,12,22);path.quadTo(3,18,3,12);path.lineTo(3,6);path.close();c.drawPath(path,p);path.reset();path.moveTo(8,12);path.lineTo(11,15);path.lineTo(16,9);c.drawPath(path,p);}c.restore();}
    }
}

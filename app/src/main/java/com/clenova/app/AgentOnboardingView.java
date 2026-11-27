package com.clenova.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Phase 47: native, illustrative conversation. No actual scan, permission, approval or task runs here. */
final class AgentOnboardingView extends ScrollView {
    static final int BACKGROUND=0xfff5f5f7;
    private static final int BLUE=0xff007aff, INK=0xff111111, MUTED=0xff777780, GREEN=0xff258b53;
    private static final long LOOP_MS=14000;
    private final View[] moments=new View[5];
    private final View progress;
    private ValueAnimator timeline;
    private boolean active;

    AgentOnboardingView(Context context,Runnable next){
        super(context);
        setBackgroundColor(BACKGROUND);setFillViewport(true);
        setVerticalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setFocusableInTouchMode(true);
        LinearLayout body=column();
        body.setPadding(dp(24),dp(12),dp(24),dp(28));
        addView(body,new ScrollView.LayoutParams(-1,-2));

        LinearLayout stage=column();stage.setGravity(Gravity.CENTER_VERTICAL);
        stage.setMinimumHeight(dp(400));stage.setPadding(0,dp(14),0,dp(10));
        body.addView(stage,new LinearLayout.LayoutParams(-1,-2,1f));

        LinearLayout intro=row();intro.setGravity(Gravity.TOP);
        Mark agent=new Mark(context,0);agent.setBackground(shape(BLUE,12));
        intro.addView(agent,new LinearLayout.LayoutParams(dp(33),dp(33)));
        TextView introText=label("Hi, I'm your Agent.\nWhat can I take care of?",18,INK,500);
        introText.setLetterSpacing(-.015f);introText.setLineSpacing(dp(2),1);
        LinearLayout.LayoutParams introLp=new LinearLayout.LayoutParams(0,-2,1);introLp.leftMargin=dp(11);
        intro.addView(introText,introLp);append(stage,intro,0);moments[0]=intro;

        LinearLayout request=row();request.setGravity(Gravity.END);
        TextView requestText=bubble("Take care of my phone.",14);
        request.addView(requestText,new LinearLayout.LayoutParams(-2,-2));
        append(stage,request,21);moments[1]=request;

        LinearLayout plan=column();plan.setPadding(dp(44),0,0,0);
        TextView planText=label("I've prepared a plan.\nShall I get started?",14,INK,500);
        planText.setLineSpacing(dp(3),1);
        append(plan,planText,0);
        LinearLayout access=row();access.setGravity(Gravity.CENTER_VERTICAL);
        Mark shield=new Mark(context,1);
        access.addView(shield,new LinearLayout.LayoutParams(dp(17),dp(17)));
        TextView accessText=label("Within your granted access",11,0xff778392,400);
        LinearLayout.LayoutParams accessLp=new LinearLayout.LayoutParams(0,-2,1);accessLp.leftMargin=dp(7);
        access.addView(accessText,accessLp);
        append(plan,access,12);append(stage,plan,23);moments[2]=plan;

        LinearLayout approval=row();approval.setGravity(Gravity.END);
        TextView approveText=bubble("Approved ✓",13);
        approveText.setPadding(dp(17),dp(9),dp(17),dp(9));
        approval.addView(approveText,new LinearLayout.LayoutParams(-2,-2));
        append(stage,approval,15);moments[3]=approval;

        LinearLayout result=column();LinearLayout done=row();done.setGravity(Gravity.CENTER_VERTICAL);
        Mark check=new Mark(context,2);check.setBackground(shape(0xffe0f4e8,20));
        done.addView(check,new LinearLayout.LayoutParams(dp(30),dp(30)));
        TextView doneText=label("All taken care of.",14,GREEN,600);
        LinearLayout.LayoutParams doneLp=new LinearLayout.LayoutParams(0,-2,1);doneLp.leftMargin=dp(10);
        done.addView(doneText,doneLp);append(result,done,0);
        FrameLayout track=new FrameLayout(context);track.setBackground(shape(0xffe3e7ee,6));track.setClipToOutline(true);
        progress=new View(context);progress.setBackground(shape(0xff25aa61,6));progress.setPivotX(0);
        track.addView(progress,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams trackLp=new LinearLayout.LayoutParams(-1,dp(5));trackLp.topMargin=dp(13);
        result.addView(track,trackLp);append(stage,result,22);moments[4]=result;

        TextView heading=label("Meet your personal\nphone agent.",29,INK,650);
        heading.setGravity(Gravity.CENTER);heading.setLetterSpacing(-.037f);heading.setLineSpacing(dp(1),1);
        append(body,heading,20);
        TextView description=label("More space. Less clutter.\nA smarter phone, always in your control.",13,MUTED,400);
        description.setGravity(Gravity.CENTER);description.setLineSpacing(dp(3),1);
        append(body,description,19);

        Button start=new Button(context);
        start.setText("Get Started");start.setAllCaps(false);start.setTextSize(16);
        start.setTypeface(android.os.Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,600,false):Typeface.create("sans-serif-medium",Typeface.NORMAL));start.setTextColor(0xffffffff);
        start.setLetterSpacing(0);start.setGravity(Gravity.CENTER);start.setMinHeight(dp(54));
        start.setMinimumHeight(dp(54));start.setMinWidth(0);start.setMinimumWidth(0);
        start.setPadding(dp(16),dp(15),dp(16),dp(15));start.setStateListAnimator(null);
        start.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),shape(BLUE,100),shape(0xffffffff,100)));
        start.setOnClickListener(v->{setActive(false);next.run();});
        append(body,start,30);

        render(0f);
        if(reducedMotion())render(.90f);
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String value,int sp,int color,int weight){
        TextView t=new TextView(getContext());t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setIncludeFontPadding(false);
        t.setTypeface(android.os.Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,weight,false):Typeface.create(weight>=500?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        return t;
    }
    private TextView bubble(String value,int sp){
        TextView t=label(value,sp,0xffffffff,400);
        GradientDrawable d=shape(BLUE,21);float r=dp(21),s=dp(5);
        d.setCornerRadii(new float[]{r,r,r,r,s,s,r,r});t.setBackground(d);t.setPadding(dp(17),dp(12),dp(17),dp(12));
        return t;
    }
    private void append(LinearLayout parent,View child,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(child,p);
    }
    private boolean reducedMotion(){
        AccessibilityManager a=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        return !ValueAnimator.areAnimatorsEnabled()||(a!=null&&a.isTouchExplorationEnabled());
    }
    void setActive(boolean value){
        active=value;
        if(!value){if(timeline!=null&&timeline.isRunning())timeline.pause();return;}
        if(!isAttachedToWindow())return;
        if(reducedMotion()){if(timeline!=null){timeline.cancel();timeline=null;}render(.90f);return;}
        if(timeline!=null){if(timeline.isPaused())timeline.resume();return;}
        timeline=ValueAnimator.ofFloat(0f,1f);timeline.setDuration(LOOP_MS);
        timeline.setInterpolator(new LinearInterpolator());timeline.setRepeatCount(ValueAnimator.INFINITE);
        timeline.addUpdateListener(a->render((float)a.getAnimatedValue()));
        timeline.start();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(active)setActive(true);}
    @Override protected void onDetachedFromWindow(){if(timeline!=null){timeline.cancel();timeline=null;}super.onDetachedFromWindow();}
    private float clamp(float f){return Math.max(0f,Math.min(1f,f));}
    private float smooth(float f){f=clamp(f);return f*f*(3-2*f);}
    private void render(float t){
        float[] starts={0f,.16f,.30f,.49f,.60f};
        float[] ends={.05f,.20f,.36f,.54f,.65f};
        float out=1f-smooth((t-.94f)/.06f);
        for(int i=0;i<moments.length;i++){
            float a=smooth((t-starts[i])/(ends[i]-starts[i]));
            View v=moments[i];v.setAlpha(a*out);v.setTranslationY(dp(7)*(1-a));
            int mode=a*out>.7f?IMPORTANT_FOR_ACCESSIBILITY_AUTO:IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
            if(v.getImportantForAccessibility()!=mode)v.setImportantForAccessibility(mode);
        }
        progress.setScaleX(smooth((t-.64f)/.18f));
    }

    /** Small, hand-drawn native glyphs matching the approved preview. */
    private static final class Mark extends View{
        private final int kind;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
        Mark(Context context,int kind){super(context);this.kind=kind;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);canvas.save();float size=Math.min(getWidth(),getHeight());
            canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/24f,size/24f);
            paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);path.reset();
            if(kind==0){
                paint.setColor(0xffffffff);paint.setStyle(Paint.Style.FILL);
                path.moveTo(12,4.7f);path.cubicTo(13,9,15,11,19.3f,12);path.cubicTo(15,13,13,15,12,19.3f);
                path.cubicTo(11,15,9,13,4.7f,12);path.cubicTo(9,11,11,9,12,4.7f);path.close();canvas.drawPath(path,paint);
            }else if(kind==1){
                paint.setColor(BLUE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f);
                path.moveTo(12,3);path.lineTo(20,6);path.lineTo(20,12);path.cubicTo(20,17,12,21,12,21);
                path.cubicTo(12,21,4,17,4,12);path.lineTo(4,6);path.close();canvas.drawPath(path,paint);
                path.reset();path.moveTo(8,12);path.lineTo(11,15);path.lineTo(16,9);canvas.drawPath(path,paint);
            }else{
                paint.setColor(0xff25aa61);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f);
                path.moveTo(6.5f,12);path.lineTo(10.5f,16);path.lineTo(17.5f,8);canvas.drawPath(path,paint);
            }
            canvas.restore();
        }
    }
}

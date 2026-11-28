package com.clenova.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.view.Gravity;
import android.widget.ScrollView;

/** One foreground-only clock for the progress line and the approved animation. */
abstract class OnboardingStoryView extends LinearLayout {
    interface Frame { void render(float fraction); }
    private StoryProgress progress;
    private ScrollView storyScroll;
    private LinearLayout storyBody;
    private Frame frame;
    private Runnable next;
    private ValueAnimator clock;
    private long duration;
    private float finalFrame,position;
    private boolean automatic,active,advanced,touching;

    OnboardingStoryView(Context context){super(context);}
    final void buildStory(int index,boolean dark,String heading,String detail,String action,Runnable callback){
        Context c=getContext();UiPolish.Palette palette=UiPolish.palette(dark);
        android.content.res.Configuration config=getResources().getConfiguration();
        boolean compact=OnboardingPolicy.compact(config.screenWidthDp,config.screenHeightDp);
        setOrientation(VERTICAL);setBackgroundColor(palette.background);
        int gutter=px(UiKit.gutter(c));setPadding(gutter,px(12),gutter,px(16));
        addStoryProgress(this,index,dark);
        storyScroll=new ScrollView(c);storyScroll.setFillViewport(false);storyScroll.setVerticalScrollBarEnabled(true);storyScroll.setOverScrollMode(OVER_SCROLL_NEVER);
        storyBody=new LinearLayout(c);storyBody.setOrientation(VERTICAL);storyBody.setPadding(0,0,0,px(4));
        storyScroll.addView(storyBody,new ScrollView.LayoutParams(-1,-2));
        addView(storyScroll,new LinearLayout.LayoutParams(-1,0,1f));
        TextView step=storyLabel("CLENOVA  /  "+(index+1)+" OF 3",11,palette.accent,600);step.setLetterSpacing(.08f);append(storyBody,step,compact?8:18);
        TextView title=storyLabel(heading,compact?27:30,palette.ink,700);title.setLetterSpacing(-.025f);append(storyBody,title,8);
        TextView desc=storyLabel(detail,14,palette.muted,400);desc.setLineSpacing(px(3),1f);append(storyBody,desc,8);
        OnboardingPanel panel=new OnboardingPanel(c,index,dark,compact);append(storyBody,panel,compact?14:22);
        Button button=new Button(c);button.setText(action);button.setAllCaps(false);button.setLetterSpacing(0);button.setStateListAnimator(null);
        UiKit.typography(button,16,0xffffffff,600);button.setMinHeight(px(52));button.setMinimumHeight(px(52));button.setPadding(px(16),px(12),px(16),px(12));
        button.setBackground(UiKit.ripple(c,UiPolish.CTA_BLUE,UiPolish.BUTTON_RADIUS,dark));button.setOnClickListener(v->advanceStory());append(this,button,compact?10:14);
        configureStory(OnboardingPolicy.duration(index),1f,true,panel::setFraction,callback);
    }
    private TextView storyLabel(String value,int size,int color,int weight){TextView t=new TextView(getContext());t.setText(value);UiKit.typography(t,size,color,weight);t.setGravity(Gravity.START);return t;}
    private void append(LinearLayout root,View view,int margin){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=px(margin);root.addView(view,lp);}
    private boolean mayAutoAdvance(){return OnboardingPolicy.automatic(reducedMotion(),getResources().getConfiguration().fontScale,storyScroll!=null&&storyBody!=null&&storyScroll.getHeight()>0&&storyBody.getHeight()<=storyScroll.getHeight()+px(2));}

    private int px(float dp){return Math.round(dp*getResources().getDisplayMetrics().density);}
    final void addStoryProgress(LinearLayout root,int index,boolean dark){
        progress=new StoryProgress(getContext(),index,dark);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,px(3));
        lp.bottomMargin=px(4);
        root.addView(progress,lp);
    }
    final void configureStory(long millis,float lastFrame,boolean auto,Frame draw,Runnable callback){
        duration=millis;finalFrame=lastFrame;automatic=auto;frame=draw;next=callback;
        render(reducedMotion()?1f:0f);
    }
    private boolean reducedMotion(){
        AccessibilityManager a=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        return !ValueAnimator.areAnimatorsEnabled()||(a!=null&&a.isTouchExplorationEnabled());
    }
    private boolean foreground(){return active&&isAttachedToWindow()&&hasWindowFocus()&&!touching&&!advanced;}
    private void render(float value){
        position=OnboardingPolicy.position(value);
        if(progress!=null)progress.setFraction(position);
        // Hold the final meaningful frame rather than showing the former loop's fade-out.
        if(frame!=null)frame.render(Math.min(position,finalFrame));
    }
    final float storyPosition(){return position;}
    final void restoreStoryPosition(float value){
        stopClock();render(reducedMotion()?1f:value);
        // If recreated at the very end, give the user a fresh final-frame dwell.
        if(automatic&&position>=1f&&!reducedMotion())render(.95f);
        syncClock();
    }
    final void setActive(boolean value){
        active=value;if(!value)touching=false;syncClock();
    }
    private void stopClock(){
        if(clock!=null){
            ValueAnimator old=clock;clock=null;
            old.removeAllUpdateListeners();old.removeAllListeners();old.cancel();
        }
    }
    private void syncClock(){
        if(frame==null||advanced)return;
        if(!foreground()){if(clock!=null&&!clock.isPaused())clock.pause();return;}
        if(reducedMotion()||getResources().getConfiguration().fontScale>1.15f){stopClock();render(1f);return;}
        if(position>=1f)return;
        if(clock!=null){if(clock.isPaused())clock.resume();return;}
        ValueAnimator run=ValueAnimator.ofFloat(position,1f);
        run.setDuration(Math.max(1L,Math.round(duration*(1-position))));
        run.setInterpolator(new LinearInterpolator());clock=run;
        run.addUpdateListener(a->{
            if(clock==a&&!advanced)render((float)a.getAnimatedValue());
        });
        run.addListener(new AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(Animator a){
                if(clock!=a)return;
                clock=null;render(1f);
                if(automatic&&foreground()&&mayAutoAdvance())advanceStory();
            }
        });
        run.start();
    }
    final void advanceStory(){
        if(advanced||next==null||!active||!isAttachedToWindow())return;
        advanced=true;stopClock();render(1f);next.run();
    }
    final void playEntrance(){
        animate().cancel();setAlpha(1f);setTranslationX(0f);
        if(reducedMotion())return;
        setAlpha(.9f);setTranslationX(px(6));
        animate().alpha(1f).translationX(0f).setDuration(OnboardingPolicy.ENTER_MS)
            .setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){touching=true;syncClock();}
        boolean handled=super.dispatchTouchEvent(event);
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||!handled){
            touching=false;syncClock();
        }
        return handled;
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);syncClock();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();syncClock();}
    @Override protected void onDetachedFromWindow(){
        touching=false;active=false;stopClock();animate().cancel();super.onDetachedFromWindow();
    }
    /** Three thin segments, aligned to the shared CTA margins. */
    private static final class StoryProgress extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int index;
        private final UiPolish.Palette colors;
        private float fraction;
        StoryProgress(Context context,int index,boolean dark){
            super(context);this.index=index;colors=UiPolish.palette(dark);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setContentDescription("Introduction "+(index+1)+" of 3");
        }
        void setFraction(float f){fraction=f;invalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float gap=6f*getResources().getDisplayMetrics().density,h=getHeight(),w=(getWidth()-2*gap)/3f;
            if(w<=0||h<=0)return;
            for(int i=0;i<3;i++){
                float x=i*(w+gap);
                paint.setColor(colors.divider);c.drawRoundRect(x,0,x+w,h,h/2,h/2,paint);
                float fill=i<index?1f:i==index?fraction:0f;
                if(fill>0){
                    paint.setColor(colors.accent);
                    float end=x+w*fill,r=Math.min(h/2,(end-x)/2);
                    c.drawRoundRect(x,0,end,h,r,r,paint);
                }
            }
        }
    }
}

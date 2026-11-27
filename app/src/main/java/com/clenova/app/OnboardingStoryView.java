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
import android.widget.ScrollView;

/** One foreground-only clock for the progress line and the approved animation. */
abstract class OnboardingStoryView extends ScrollView {
    interface Frame { void render(float fraction); }
    private StoryProgress progress;
    private Frame frame;
    private Runnable next;
    private ValueAnimator clock;
    private long duration;
    private float finalFrame,position;
    private boolean automatic,active,advanced,touching;

    OnboardingStoryView(Context context){super(context);}
    private int px(float dp){return Math.round(dp*getResources().getDisplayMetrics().density);}
    final void addStoryProgress(LinearLayout root,int index){
        progress=new StoryProgress(getContext(),index);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,px(3));
        lp.leftMargin=px(24);lp.rightMargin=px(24);lp.bottomMargin=px(8);
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
        position=Math.max(0f,Math.min(1f,value));
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
        if(reducedMotion()){stopClock();render(1f);return;}
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
                if(automatic&&foreground()&&!reducedMotion())advanceStory();
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
        setAlpha(0f);setTranslationX(px(14));
        animate().alpha(1f).translationX(0f).setDuration(260L)
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
        private float fraction;
        StoryProgress(Context context,int index){
            super(context);this.index=index;
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
                paint.setColor(0xffdce3ed);c.drawRoundRect(x,0,x+w,h,h/2,h/2,paint);
                float fill=i<index?1f:i==index?fraction:0f;
                if(fill>0){
                    paint.setColor(0xff007aff);
                    float end=x+w*fill,r=Math.min(h/2,(end-x)/2);
                    c.drawRoundRect(x,0,end,h,r,r,paint);
                }
            }
        }
    }
}

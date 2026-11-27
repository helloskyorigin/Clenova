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
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The same card deck and fixed lower controls throughout setup. */
final class PermissionSetupView extends ScrollView {
    private static final int BG=0xfff5f5f7, BLUE=0xff007aff, INK=0xff152638, MUTED=0xff788595,GREEN=0xff258553;
    private static final String[] TITLES={"Photos & videos","Notifications","App usage","File access","All set"};
    private static final String[] DETAILS={"Find clutter and make room\nfor what matters.","Stay informed when your Agent\nneeds your attention.","Identify apps you rarely use.","Organize files and downloads\nin folders you choose.","Your access preferences are ready."};
    private final FrameLayout deck;
    private final Button allow,skip;
    private final TextView feedback;
    private LinearLayout front;
    private int index=-1;
    private boolean transitioning,locked;
    private String primary="Allow",secondary="";
    PermissionSetupView(Context context,Runnable request,Runnable decline){
        super(context);setBackgroundColor(BG);setFillViewport(true);setVerticalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(12),dp(24),dp(22));
        addView(root,new ScrollView.LayoutParams(-1,-2));
        FrameLayout stage=new FrameLayout(context);
        GradientDrawable aura=new GradientDrawable();aura.setGradientType(GradientDrawable.RADIAL_GRADIENT);aura.setGradientRadius(dp(170));aura.setColors(new int[]{0xffdfebfc,BG});stage.setBackground(aura);
        root.addView(stage,new LinearLayout.LayoutParams(-1,dp(348),1));
        deck=new FrameLayout(context);deck.setClipChildren(false);deck.setClipToPadding(false);
        FrameLayout.LayoutParams deckParams=new FrameLayout.LayoutParams(-1,dp(253),Gravity.CENTER);
        deckParams.topMargin=dp(48);stage.addView(deck,deckParams);
        TextView heading=label("Give your Agent\nthe right access.",29,0xff111111,650);heading.setLetterSpacing(-.037f);append(root,heading,10);
        TextView desc=label("A smarter phone starts here.\nOnly the access your Agent needs.",13,0xff777780,400);desc.setLineSpacing(dp(3),1);append(root,desc,16);
        TextView privacy=label("✓  Always under your control.",11,GREEN,500);append(root,privacy,20);
        feedback=label("",12,MUTED,400);feedback.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);feedback.setVisibility(GONE);append(root,feedback,10);
        allow=button("Allow",true);append(root,allow,23);allow.setOnClickListener(v->{if(!transitioning&&!locked)request.run();});
        skip=button("Not now",false);append(root,skip,4);skip.setOnClickListener(v->{if(!transitioning&&!locked)decline.run();});
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Typeface face(int weight){return Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,weight,false):Typeface.create(weight>=500?"sans-serif-medium":"sans-serif",Typeface.NORMAL);}
    private TextView label(String value,int size,int color,int weight){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(face(weight));t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;}
    private GradientDrawable shape(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable glass(int start,int end,float radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xffffffff);return d;}
    private void append(LinearLayout parent,View child,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);parent.addView(child,lp);}
    private Button button(String text,boolean primary){
        Button b=new Button(getContext());b.setText(text);b.setAllCaps(false);b.setTextSize(primary?16:13);b.setTypeface(face(primary?600:500));b.setTextColor(primary?0xffffffff:0xff777780);b.setLetterSpacing(0);
        b.setMinHeight(dp(primary?54:44));b.setMinimumHeight(dp(primary?54:44));b.setPadding(dp(10),dp(12),dp(10),dp(12));b.setStateListAnimator(null);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x33ffffff:0x11007aff),shape(primary?BLUE:0x00000000,40),shape(0xffffffff,40)));return b;
    }
    private boolean reduce(){AccessibilityManager am=(AccessibilityManager)getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);return !ValueAnimator.areAnimatorsEnabled()||(am!=null&&am.isTouchExplorationEnabled());}
    private LinearLayout card(int kind){
        LinearLayout c=new LinearLayout(getContext());c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(glass(0xffffffff,0xffeff4fb,29));c.setElevation(dp(7));
        Mark mark=new Mark(getContext(),kind);mark.setBackground(glass(kind==4?0xff86dfb2:0xff81c9ff,kind==4?0xff259d60:0xff005bd6,27));mark.setElevation(dp(4));
        c.addView(mark,new LinearLayout.LayoutParams(dp(86),dp(86)));
        TextView name=label(TITLES[kind],22,INK,650);name.setLetterSpacing(-.028f);append(c,name,22);
        TextView detail=label(DETAILS[kind],12,MUTED,400);detail.setLineSpacing(dp(3),1);append(c,detail,10);
        return c;
    }
    private LinearLayout historyCard(int kind,String result){
        LinearLayout c=new LinearLayout(getContext());c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(20),dp(11),dp(20),0);c.setBackground(glass(0xffffffff,0xffeff4fb,29));
        TextView t=label(TITLES[kind]+"  ·  "+result,12,("Allowed".equals(result)||"Limited".equals(result))?GREEN:MUTED,600);t.setGravity(Gravity.START);c.addView(t,new LinearLayout.LayoutParams(-1,-2));return c;
    }
    void display(int step,String previous,String action,String alternate,String message){
        primary=action;secondary=alternate;feedback.setText(message);feedback.setVisibility(message.isEmpty()?GONE:VISIBLE);
        if(step==index){updateButtons();return;}
        int old=index;index=step;
        final LinearLayout outgoing=front;
        if(outgoing!=null)outgoing.animate().cancel();
        deck.removeAllViews();
        boolean forward=old>=0&&old<step&&!previous.isEmpty();
        final LinearLayout past=forward?historyCard(old,previous):null;
        if(past!=null){
            deck.addView(past,new FrameLayout.LayoutParams(-1,-1));
            past.setScaleX(.82f);past.setScaleY(.82f);past.setTranslationY(-dp(45));past.setAlpha(.82f);
        }
        final boolean motion=!reduce()&&old>=0;
        if(forward&&motion&&outgoing!=null){
            // Animate the actual former front card behind the new card, then keep its verified status.
            past.setAlpha(0f);
            deck.addView(outgoing,new FrameLayout.LayoutParams(-1,-1));
            outgoing.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            outgoing.setTranslationY(0);outgoing.setScaleX(1);outgoing.setScaleY(1);outgoing.setAlpha(1);
            outgoing.animate().translationY(-dp(45)).scaleX(.82f).scaleY(.82f).alpha(0f)
                .setDuration(520).setInterpolator(new DecelerateInterpolator(1.8f))
                .withEndAction(()->{if(outgoing.getParent()==deck)deck.removeView(outgoing);if(past.getParent()==deck)past.setAlpha(.82f);}).start();
            past.animate().alpha(.82f).setStartDelay(220).setDuration(300).start();
        }
        front=card(step);deck.addView(front,new FrameLayout.LayoutParams(-1,-1));
        if(motion){
            transitioning=true;front.setTranslationY(dp(150));front.setScaleX(.92f);front.setScaleY(.92f);front.setAlpha(0f);
            final LinearLayout incoming=front;
            front.animate().translationY(0).scaleX(1).scaleY(1).alpha(1).setDuration(600).setInterpolator(new DecelerateInterpolator(1.8f)).withEndAction(()->{if(front==incoming){transitioning=false;updateButtons();}}).start();
        }else transitioning=false;
        updateButtons();
    }
    void setBusy(boolean busy){locked=busy;updateButtons();}
    private void updateButtons(){allow.setText(primary);allow.setEnabled(!locked&&!transitioning);skip.setText(secondary);skip.setVisibility(secondary.isEmpty()?INVISIBLE:VISIBLE);skip.setEnabled(!locked&&!transitioning&&!secondary.isEmpty());}
    @Override protected void onDetachedFromWindow(){for(int i=0;i<deck.getChildCount();i++)deck.getChildAt(i).animate().cancel();transitioning=false;super.onDetachedFromWindow();}
    private static final class Mark extends View{
        private final int kind;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();
        Mark(Context c,int k){super(c);kind=k;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private void draw(Canvas c,float... xy){path.reset();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.save();c.translate(getWidth()*.23f,getHeight()*.23f);c.scale(getWidth()*.54f/48,getHeight()*.54f/48);
            p.setColor(0xffffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            if(kind==0){c.drawRoundRect(4,7,44,41,9,9,p);c.drawCircle(15,17,4,p);draw(c,5,34,17,22,27,32,34,24,43,33);}
            else if(kind==1){path.reset();path.moveTo(11,31);path.lineTo(11,20);path.cubicTo(11,3,37,3,37,20);path.lineTo(37,31);path.lineTo(41,35);path.lineTo(7,35);path.close();c.drawPath(path,p);path.reset();path.moveTo(19,40);path.quadTo(24,45,29,40);c.drawPath(path,p);}
            else if(kind==2){for(int i=0;i<4;i++){int x=5+(i%2)*23,y=5+(i/2)*23;c.drawRoundRect(x,y,x+15,y+15,5,5,p);}}
            else if(kind==3){path.reset();path.moveTo(5,13);path.lineTo(19,13);path.lineTo(25,19);path.lineTo(43,19);path.lineTo(43,36);path.quadTo(43,41,38,41);path.lineTo(10,41);path.quadTo(5,41,5,36);path.close();c.drawPath(path,p);}
            else{p.setStrokeWidth(4);draw(c,10,24,20,34,39,14);}
            c.restore();
        }
    }
}

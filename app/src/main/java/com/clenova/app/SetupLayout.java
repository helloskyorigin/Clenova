package com.clenova.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Scrollable native setup content with independent, always available footer actions. */
class SetupLayout extends LinearLayout {
    final UiPolish.Palette colors;
    final boolean dark,compact;
    final LinearLayout body,footer;
    private final ScrollView scroll;
    SetupLayout(Context c,boolean dark){
        super(c);this.dark=dark;colors=UiPolish.palette(dark);
        android.content.res.Configuration config=getResources().getConfiguration();
        compact=OnboardingPolicy.compact(config.screenWidthDp,config.screenHeightDp);
        setOrientation(VERTICAL);setBackgroundColor(colors.background);
        int gutter=dp(UiKit.gutter(c));setPadding(gutter,dp(12),gutter,dp(12));
        scroll=new ScrollView(c);scroll.setFillViewport(false);scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        body=column();body.setPadding(0,0,0,dp(6));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        addView(scroll,new LayoutParams(-1,0,1f));
        footer=column();addView(footer,new LayoutParams(-1,-2));
    }
    final int dp(float value){return UiKit.dp(getContext(),value);}
    final LinearLayout column(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(VERTICAL);return l;}
    final LinearLayout row(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    final TextView label(String text,int size,int color,int weight){
        TextView t=new TextView(getContext());t.setText(text);UiKit.typography(t,size,color,weight);
        t.setGravity(Gravity.START);t.setLineSpacing(dp(2),1f);return t;
    }
    final void append(LinearLayout parent,View child,int margin){LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(margin);parent.addView(child,lp);}
    final void intro(String eyebrow,String title,String description){
        TextView tag=label(eyebrow,11,colors.accent,600);tag.setLetterSpacing(.08f);append(body,tag,compact?4:10);
        TextView heading=label(title,compact?26:30,colors.ink,700);heading.setLetterSpacing(-.025f);append(body,heading,8);
        append(body,label(description,14,colors.muted,400),8);
    }
    final LinearLayout card(){LinearLayout l=column();l.setPadding(dp(16),dp(14),dp(16),dp(14));l.setBackground(UiKit.card(getContext(),dark));return l;}
    final Button action(String text,boolean primary){
        Button b=new Button(getContext());b.setText(text);b.setAllCaps(false);b.setLetterSpacing(0);b.setStateListAnimator(null);
        UiKit.typography(b,primary?16:14,primary?0xffffffff:colors.accent,600);
        b.setMinHeight(dp(primary?52:48));b.setMinimumHeight(dp(primary?52:48));b.setPadding(dp(12),dp(10),dp(12),dp(10));
        b.setBackground(UiKit.ripple(getContext(),primary?UiPolish.CTA_BLUE:colors.card,UiPolish.BUTTON_RADIUS,dark));return b;
    }
    final void enabled(Button b,boolean enabled){b.setEnabled(enabled);b.setAlpha(enabled?1f:.55f);}
    final boolean motion(){AccessibilityManager a=getContext().getSystemService(AccessibilityManager.class);return ValueAnimator.areAnimatorsEnabled()&&(a==null||!a.isTouchExplorationEnabled());}
    final void toTop(){scroll.scrollTo(0,0);}
}

package com.clenova.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Approved branded launch view. The actual user-supplied artwork is a local resource. */
final class LaunchSplashView extends FrameLayout {
    static final int BACKGROUND=0xfffefefe;
    private final LinearLayout identity;
    private final LinearLayout credit;
    private final ImageView logo;
    private boolean revealed;
    LaunchSplashView(Context c){
        super(c);setBackgroundColor(BACKGROUND);
        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        identity=new LinearLayout(c);identity.setOrientation(LinearLayout.VERTICAL);identity.setGravity(Gravity.CENTER_HORIZONTAL);
        identity.setPadding(dp(20),0,dp(20),0);
        int width=c.getResources().getConfiguration().screenWidthDp;
        int height=c.getResources().getConfiguration().screenHeightDp;
        int logoSize=Math.max(100,Math.min(220,Math.min((int)(width*.53f),(int)(height*.40f))));
        logo=new ImageView(c);logo.setImageResource(R.drawable.clenova_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("Clenova app logo");
        identity.addView(logo,new LinearLayout.LayoutParams(dp(logoSize),dp(logoSize)));
        TextView name=label("CLENOVA",width<330?29:34,0xff0b1935,Typeface.create("sans-serif",Typeface.BOLD));
        name.setLetterSpacing(.055f);name.setMaxLines(1);
        name.setAutoSizeTextTypeUniformWithConfiguration(18,34,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams nameLp=new LinearLayout.LayoutParams(-1,dp(49));nameLp.topMargin=dp(3);identity.addView(name,nameLp);
        TextView tagline=label("Your phone. On autopilot.",15,0xff566174,Typeface.create("sans-serif",Typeface.NORMAL));
        tagline.setMaxLines(2);LinearLayout.LayoutParams taglineLp=new LinearLayout.LayoutParams(-1,-2);taglineLp.topMargin=dp(5);identity.addView(tagline,taglineLp);
        FrameLayout.LayoutParams center=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);
        center.bottomMargin=dp(height<460?28:18);addView(identity,center);

        credit=new LinearLayout(c);credit.setGravity(Gravity.CENTER);credit.setOrientation(LinearLayout.HORIZONTAL);
        TextView by=label("by",12,0xff818895,Typeface.create("sans-serif-light",Typeface.NORMAL));
        credit.addView(by,new LinearLayout.LayoutParams(-2,-2));
        TextView sky=label("Skyorigin",18,0xff17253d,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        sky.setLetterSpacing(.07f);LinearLayout.LayoutParams skyLp=new LinearLayout.LayoutParams(-2,-2);skyLp.leftMargin=dp(5);credit.addView(sky,skyLp);
        FrameLayout.LayoutParams footer=new FrameLayout.LayoutParams(-1,dp(40),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        footer.bottomMargin=dp(height<460?8:24);addView(credit,footer);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String value,int sp,int color,Typeface face){
        TextView t=new TextView(getContext());t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setTypeface(face);t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;
    }
    void reveal(){
        if(revealed)return;revealed=true;
        if(!ValueAnimator.areAnimatorsEnabled())return;
        logo.setScaleX(.96f);logo.setScaleY(.96f);logo.animate().scaleX(1).scaleY(1).setDuration(330).setInterpolator(new DecelerateInterpolator()).start();
        identity.setAlpha(.82f);identity.animate().alpha(1).setDuration(230).start();
        credit.setAlpha(.65f);credit.animate().alpha(1).setDuration(260).start();
    }
    @Override protected void onDetachedFromWindow(){logo.animate().cancel();identity.animate().cancel();credit.animate().cancel();super.onDetachedFromWindow();}
}

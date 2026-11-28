package com.clenova.app;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Preference activation is confirmed only after actual local persistence succeeds. */
final class AgentSetupView extends SetupLayout {
    private final AppPreferences prefs;
    private final Runnable finished;
    private final TextView status,heading,description,notice;
    private final TextView[] details=new TextView[3],markers=new TextView[3];
    private final Button primary,skip;
    private boolean confirmed,leaving,active;
    AgentSetupView(Context c,AppPreferences preferences,boolean restoredConfirmation,Runnable next){
        super(c,UiKit.dark(c,preferences.appearance()));prefs=preferences;finished=next;
        status=label("Ready to activate",12,colors.accent,600);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);append(body,status,compact?4:10);
        heading=label("Meet your new\neveryday Agent.",compact?26:30,colors.ink,700);heading.setLetterSpacing(-.025f);append(body,heading,8);
        description=label("Find opportunities to care for your phone. Decide what happens next.",14,colors.muted,400);append(body,description,8);
        LinearLayout panel=card();
        String[] titles={"Smart discovery","Proactive recommendations","Approval protection"};
        String[] subtitles={"Find what you no longer need","Background media reviews while charging","You always make the final decision"};
        for(int i=0;i<3;i++){
            LinearLayout item=row();TextView marker=label(Integer.toString(i+1),14,colors.accent,700);marker.setGravity(android.view.Gravity.CENTER);
            marker.setBackground(UiKit.shape(c,colors.tint,10));item.addView(marker,new LayoutParams(dp(36),dp(36)));markers[i]=marker;
            LinearLayout copy=column();LayoutParams lp=new LayoutParams(0,-2,1f);lp.leftMargin=dp(12);item.addView(copy,lp);
            append(copy,label(titles[i],15,colors.ink,600),0);details[i]=label(subtitles[i],13,colors.muted,400);append(copy,details[i],3);
            append(panel,item,i==0?0:16);
        }append(body,panel,compact?16:22);
        append(body,label("Nothing changes without your approval.",13,colors.success,600),14);
        notice=label("Background checks require full media access and Android scheduling. You can change this anytime.",12,colors.muted,400);append(body,notice,8);
        primary=action("Activate Agent",true);append(footer,primary,8);
        skip=action("Not now",false);append(footer,skip,6);
        primary.setOnClickListener(v->activateOrContinue());skip.setOnClickListener(v->selectManual());
        if(restoredConfirmation&&prefs.agentConfigured()&&prefs.autopilot())showConfirmed(false);
    }
    boolean confirmed(){return confirmed;}
    void setActive(boolean value){active=value;if(!value){heading.animate().cancel();heading.setAlpha(1f);}}
    private void lock(boolean locked){enabled(primary,!locked);enabled(skip,!locked&&!confirmed);}
    private void activateOrContinue(){
        if(leaving||!active)return;if(confirmed){leave();return;}
        lock(true);
        if(!prefs.saveAgentSetup(true)){notice.setText("Couldn't save your preferences. Please try again.");lock(false);return;}
        showConfirmed(true);
    }
    private void selectManual(){
        if(leaving||confirmed||!active)return;lock(true);
        if(!prefs.saveAgentSetup(false)){notice.setText("Couldn't save your preferences. Please try again.");lock(false);return;}
        leave();
    }
    private void leave(){if(leaving)return;leaving=true;lock(true);finished.run();}
    private void showConfirmed(boolean animate){
        confirmed=true;status.setText("Agent activated");status.setTextColor(colors.success);
        heading.setText("Your Agent.\nReady for you.");description.setText("Your preferences are set. You stay in control.");
        details[0].setText("Discovery preferences saved");details[1].setText("On-demand reviews available");details[2].setText("Approval is always required");
        for(TextView marker:markers){marker.setText("✓");marker.setTextColor(colors.success);}
        notice.setText("Background checks depend on Android scheduling and media access.");
        primary.setText("Continue");enabled(primary,true);skip.setVisibility(GONE);enabled(skip,false);toTop();
        if(animate&&motion()){heading.setAlpha(.92f);heading.animate().alpha(1).setDuration(UiPolish.STANDARD_MOTION_MS).start();}
    }
    @Override protected void onDetachedFromWindow(){heading.animate().cancel();super.onDetachedFromWindow();}
}

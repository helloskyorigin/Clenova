package com.clenova.app;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Permission presentation only: OS requests and verified outcomes remain in PermissionSetup. */
final class PermissionSetupView extends SetupLayout {
    private static final String[] TITLES={"Photos & videos","Notifications","App usage","File access","All set"};
    private static final String[] DETAILS={
        "Review accessible photos and videos, including duplicates and screenshots.",
        "Receive Agent updates when scheduled media checks find review candidates.",
        "Review rarely used apps using Android usage information.",
        "Review downloads and files in a folder you choose.",
        "Your access choices are saved. You can manage them later in Settings."
    };
    private static final String[] SAFETY={
        "Android controls access. Review and approval are required before removal.",
        "Notifications do not grant access to your personal files.",
        "Usage access does not uninstall or change apps.",
        "Only the selected folder is accessible through this picker.",
        "Nothing is removed during setup."
    };
    private final TextView stepLabel,title,detail,safety,previousLabel,feedback;
    private final LinearLayout indicators,accessCard;
    private final View[] segments=new View[4];
    private final Button allow,skip;
    private int index=-1;
    private boolean locked;
    private String primary="Allow",secondary="";
    PermissionSetupView(Context context,boolean dark,Runnable request,Runnable decline){
        super(context,dark);
        intro("DEVICE ACCESS","Give your Agent\nthe right access.","Choose what CLENOVA can review. You remain in control.");
        stepLabel=label("",12,colors.accent,600);append(body,stepLabel,compact?12:20);
        indicators=row();for(int i=0;i<segments.length;i++){
            View segment=new View(context);LayoutParams lp=new LayoutParams(0,dp(4),1f);if(i>0)lp.leftMargin=dp(5);indicators.addView(segment,lp);segments[i]=segment;
        }append(body,indicators,8);
        accessCard=card();
        title=label("",22,colors.ink,700);append(accessCard,title,0);
        detail=label("",14,colors.muted,400);append(accessCard,detail,10);
        safety=label("",13,colors.success,500);append(accessCard,safety,14);
        append(body,accessCard,12);
        previousLabel=label("",12,colors.muted,500);previousLabel.setVisibility(GONE);append(body,previousLabel,12);
        feedback=label("",13,colors.ink,500);feedback.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);feedback.setVisibility(GONE);append(body,feedback,12);
        allow=action("Allow",true);append(footer,allow,8);allow.setOnClickListener(v->{if(!locked)request.run();});
        skip=action("Not now",false);append(footer,skip,6);skip.setOnClickListener(v->{if(!locked&&!secondary.isEmpty())decline.run();});
    }
    void display(int step,String previous,String action,String alternate,String message){
        int old=index;index=Math.max(0,Math.min(TITLES.length-1,step));
        primary=action;secondary=alternate;
        stepLabel.setText(index==4?"ACCESS REVIEW COMPLETE":"STEP "+(index+1)+" OF 4");
        for(int i=0;i<segments.length;i++)segments[i].setBackground(UiKit.shape(getContext(),i<=index?colors.accent:colors.divider,2));
        title.setText(TITLES[index]);detail.setText(DETAILS[index]);safety.setText(SAFETY[index]);
        previousLabel.setText(index>0&&!previous.isEmpty()?TITLES[index-1]+" · "+previous:"");
        previousLabel.setVisibility(index>0&&!previous.isEmpty()?VISIBLE:GONE);
        feedback.setText(message);feedback.setVisibility(message.isEmpty()?GONE:VISIBLE);
        if(old!=index){toTop();accessCard.animate().cancel();accessCard.setAlpha(1);accessCard.setTranslationY(0);
            if(old>=0&&motion()){accessCard.setAlpha(.94f);accessCard.setTranslationY(dp(4));accessCard.animate().alpha(1).translationY(0).setDuration(UiPolish.FAST_MOTION_MS).start();}}
        updateButtons();
    }
    void setBusy(boolean busy){locked=busy;updateButtons();}
    private void updateButtons(){
        allow.setText(primary);enabled(allow,!locked);
        skip.setText(secondary);skip.setVisibility(secondary.isEmpty()?GONE:VISIBLE);enabled(skip,!locked&&!secondary.isEmpty());
    }
    @Override protected void onDetachedFromWindow(){accessCard.animate().cancel();super.onDetachedFromWindow();}
}

package com.clenova.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Each status is driven by DeviceOverview callbacks; no synthetic scan or progress timer. */
final class PreparingPhoneView extends SetupLayout {
    private static final String[] TITLES={"Device access","Device storage","Photos","Videos","Apps & Home"};
    private static final String[] PENDING={"Checking permissions","Reading available storage","Reading accessible photo metadata","Reading accessible video metadata","Preparing the local overview"};
    private final TextView heading,description,status,percent,storage,feedback;
    private final TextView[] markers=new TextView[PreparationPolicy.STAGES],details=new TextView[PreparationPolicy.STAGES];
    private final ProgressBar progress;
    private final Button retry;
    private int completed;
    private DeviceOverview.Snapshot last=new DeviceOverview.Snapshot();
    PreparingPhoneView(Context c,boolean dark,Runnable retryAction){
        super(c,dark);
        TextView tag=label("FINISHING SETUP",11,colors.accent,600);tag.setLetterSpacing(.08f);append(body,tag,compact?4:10);
        heading=label("Your phone.\nAlmost ready.",compact?26:30,colors.ink,700);heading.setLetterSpacing(-.025f);append(body,heading,8);
        description=label("Preparing your local overview. No files are changed.",14,colors.muted,400);append(body,description,8);
        LinearLayout panel=card();LinearLayout header=row();
        status=label("Checking device access",14,colors.ink,600);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);header.addView(status,new LayoutParams(0,-2,1f));
        percent=label("0%",17,colors.accent,700);header.addView(percent,new LayoutParams(-2,-2));append(panel,header,0);
        progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(PreparationPolicy.STAGES);
        progress.setProgressTintList(ColorStateList.valueOf(colors.accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(colors.divider));
        LayoutParams bar=new LayoutParams(-1,dp(8));bar.topMargin=dp(12);panel.addView(progress,bar);
        storage=label("Storage details will appear when available.",12,colors.muted,400);append(panel,storage,12);append(body,panel,compact?16:22);
        LinearLayout stages=card();
        for(int i=0;i<TITLES.length;i++){
            LinearLayout line=row();TextView mark=label(Integer.toString(i+1),13,colors.muted,600);mark.setGravity(Gravity.CENTER);mark.setBackground(UiKit.shape(c,colors.background,9));
            line.addView(mark,new LayoutParams(dp(32),dp(32)));markers[i]=mark;
            LinearLayout copy=column();LayoutParams cp=new LayoutParams(0,-2,1f);cp.leftMargin=dp(11);line.addView(copy,cp);
            append(copy,label(TITLES[i],14,colors.ink,600),0);details[i]=label("Waiting",12,colors.muted,400);append(copy,details[i],2);append(stages,line,i==0?0:13);
        }append(body,stages,12);
        feedback=label("Your overview stays on your device.",12,colors.success,500);append(body,feedback,12);
        retry=action("Retry",true);retry.setVisibility(GONE);append(footer,retry,8);retry.setOnClickListener(v->{enabled(retry,false);retryAction.run();});
        render(false);
    }
    void update(int stage,DeviceOverview.Snapshot snapshot){
        completed=PreparationPolicy.stage(stage);last=snapshot;
        retry.setVisibility(GONE);render(false);
    }
    private boolean dataAvailable(int i){
        switch(i){case 0:return "Allowed".equals(last.mediaAccess);case 1:return last.storageKnown();case 2:return last.photos.available&&!last.photos.partial;case 3:return last.videos.available&&!last.videos.partial;default:return last.apps>=0;}
    }
    private String observed(int i){
        switch(i){
            case 0:return "Media access: "+last.mediaAccess;
            case 1:return last.storageKnown()?DeviceOverview.format(last.free)+" available":"Storage unavailable";
            case 2:return last.photos.available?last.photos.items():"Access unavailable";
            case 3:return last.videos.available?last.videos.items():"Access unavailable";
            default:return last.apps>=0?last.apps+" launcher-visible apps":"App overview unavailable";
        }
    }
    private void render(boolean failed){
        boolean done=completed==PreparationPolicy.STAGES&&!failed;
        percent.setText(PreparationPolicy.percent(completed)+"%");percent.setTextColor(done?colors.success:colors.accent);
        progress.setProgressTintList(ColorStateList.valueOf(done?colors.success:colors.accent));progress.setProgress(completed,motion());
        status.setText(failed?"Couldn't finish setup":done?"Setup complete":PENDING[Math.min(completed,TITLES.length-1)]);
        storage.setText(last.storageKnown()?DeviceOverview.format(last.free)+" available of "+DeviceOverview.format(last.total):completed>=2?"Storage unavailable":"Storage details will appear when available.");
        for(int i=0;i<TITLES.length;i++){
            int state=PreparationPolicy.rowState(completed,i,failed);
            boolean available=state==2&&dataAvailable(i);
            markers[i].setText(state==2?(available?"✓":"–"):Integer.toString(i+1));markers[i].setTextColor(available?colors.success:state==1?colors.accent:colors.muted);
            details[i].setText(state==2?observed(i):state==1?PENDING[i]:"Waiting");
        }
        if(failed){heading.setText("Setup needs attention.");description.setText("Your files have not been changed. You can retry.");}
        if(done){heading.setText("You're all set.\nWelcome to CLENOVA.");description.setText(last.partial()?"Your available data is ready. Manage more access anytime.":"Your local overview is ready.");feedback.setText("Opening Home · No files were changed.");}
    }
    void error(String message){render(true);feedback.setText(message);feedback.setTextColor(colors.danger);retry.setVisibility(VISIBLE);enabled(retry,true);}
}

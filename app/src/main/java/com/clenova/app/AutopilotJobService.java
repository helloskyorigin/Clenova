package com.clenova.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** Cancellable read-only background discovery. All persisted results are non-executable aggregates. */
public final class AutopilotJobService extends JobService {
    private final Handler main=new Handler(Looper.getMainLooper());
    private SmartMediaScan scan;
    private JobParameters running;
    private Runnable deadline;
    @Override public boolean onStartJob(JobParameters params){
        AppPreferences prefs=new AppPreferences(this);
        final String revision=params.getExtras().getString("revision","");
        final int mask=ToolAccess.mediaMask(this);
        final int eligible=AutopilotScheduler.eligible(this,prefs);
        if(!prefs.autopilot()||!prefs.onboardingComplete()||eligible==0||revision.isEmpty()||!revision.equals(AutopilotScheduler.revision(this)))return false;
        if(running!=null)return false;
        running=params;
        final SmartMediaScan work=new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this));scan=work;
        deadline=()->{if(running==params){cancelScan();recordFailure(revision,mask,eligible);jobFinished(params,false);}};
        main.postDelayed(deadline,120000);
        new Thread(()->{
            try{
                SmartMediaScan.Result result=work.runScheduled((stage,done,total)->{},(eligible&3)!=0,(eligible&4)!=0,(eligible&1)!=0,(eligible&2)!=0);
                main.post(()->complete(params,work,revision,mask,eligible,result));
            }catch(Exception e){main.post(()->complete(params,work,revision,mask,eligible,null));}
        },"Clenova-autopilot").start();
        return true;
    }
    private void complete(JobParameters params,SmartMediaScan work,String revision,int mask,int eligible,SmartMediaScan.Result result){
        if(running!=params||scan!=work)return;
        cancelScan();
        AppPreferences prefs=new AppPreferences(this);
        if(result!=null&&prefs.autopilot()&&prefs.onboardingComplete()&&mask==ToolAccess.mediaMask(this)
            &&eligible==AutopilotScheduler.eligible(this,prefs)&&revision.equals(AutopilotScheduler.revision(this))){
            int duplicates=(eligible&1)!=0?result.groups.get(SmartMediaScan.DUPLICATES).size():0;
            int similar=(eligible&2)!=0?result.groups.get(SmartMediaScan.SIMILAR).size():0;
            int videos=(eligible&4)!=0?result.groups.get(SmartMediaScan.VIDEOS).size():0;
            int count=duplicates+similar+videos;
            boolean partial=result.partial||eligible!=AutopilotScheduler.requested(prefs)||((eligible&3)!=0&&!result.imagesScanned)||((eligible&4)!=0&&!result.videosScanned);
            boolean saved=AutopilotScheduler.state(this).edit().remove("failed").putLong("checked",System.currentTimeMillis()).putInt("eligible",eligible).putInt("count",count).putInt("duplicates",duplicates).putInt("similar",similar).putInt("videos",videos).putBoolean("partial",partial).commit();
            if(saved&&count>0&&prefs.canNotify(0))notifyReview();
        }else if(result==null)recordFailure(revision,mask,eligible);
        jobFinished(params,false);
    }
    private void recordFailure(String revision,int mask,int eligible){
        AppPreferences p=new AppPreferences(this);
        if(p.autopilot()&&p.onboardingComplete()&&mask==ToolAccess.mediaMask(this)
            &&eligible==AutopilotScheduler.eligible(this,p)&&revision.equals(AutopilotScheduler.revision(this))){
            AutopilotScheduler.state(this).edit().remove("checked").remove("count").remove("duplicates").remove("similar").remove("videos").remove("partial").putInt("eligible",eligible).putLong("failed",System.currentTimeMillis()).commit();
            NotificationManager nm=getSystemService(NotificationManager.class);if(nm!=null)nm.cancel("agent_updates",0);
        }
    }
    private void notifyReview(){
        try{
            NotificationManager nm=getSystemService(NotificationManager.class);if(nm==null)return;
            Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent target=PendingIntent.getActivity(this,AutopilotScheduler.JOB_ID,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notification=new Notification.Builder(this,"agent_updates").setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle("Clenova Agent").setContentText("Cleanup candidates ready for a fresh review.")
                .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(target).setAutoCancel(true).build();
            nm.notify("agent_updates",0,notification);
        }catch(RuntimeException ignored){}
    }
    private void cancelScan(){
        if(deadline!=null)main.removeCallbacks(deadline);deadline=null;
        if(scan!=null)scan.cancel();scan=null;running=null;
    }
    @Override public boolean onStopJob(JobParameters params){if(running==params)cancelScan();return false;}
    @Override public void onDestroy(){cancelScan();super.onDestroy();}
}

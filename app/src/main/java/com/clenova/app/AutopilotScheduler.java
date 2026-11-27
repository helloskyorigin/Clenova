package com.clenova.app;

import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

/** Persistent OS schedule, local aggregate only. Never stores a deletion plan or file URI. */
final class AutopilotScheduler {
    static final int JOB_ID=0x434c55;
    private static final String STORE="clenova_autopilot";
    static SharedPreferences state(Context c){return c.getSharedPreferences(STORE,Context.MODE_PRIVATE);}
    static String revision(Context c){return state(c).getString("revision","");}
    static int requested(AppPreferences p){return (p.discovery(0)?1:0)|(p.discovery(1)?2:0)|(p.discovery(2)?4:0);}
    static int eligible(Context c,AppPreferences p){return AutopilotPolicy.eligible(requested(p),ToolAccess.mediaMask(c));}
    static void clear(Context c){
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js!=null)try{js.cancel(JOB_ID);}catch(RuntimeException ignored){}
        state(c).edit().clear().putString("revision",UUID.randomUUID().toString()).commit();
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        if(nm!=null)nm.cancel("agent_updates",0);
    }
    static void changed(Context c,AppPreferences p){clear(c);reconcile(c,p);}
    static boolean reconcile(Context c,AppPreferences p){
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js==null)return false;
        try{
            if(!p.autopilot()||!p.onboardingComplete()||requested(p)==0){
                if(js.getPendingJob(JOB_ID)!=null)clear(c);
                return false;
            }
            int mask=ToolAccess.mediaMask(c);
            if(eligible(c,p)==0){if(js.getPendingJob(JOB_ID)!=null||state(c).contains("checked")||state(c).contains("failed"))clear(c);return false;}
            long interval=AutopilotPolicy.interval(p.scanDays());
            JobInfo old=js.getPendingJob(JOB_ID);
            if(old!=null&&old.getIntervalMillis()==interval&&state(c).getInt("scheduled_mask",-1)==mask&&old.getExtras().getString("revision","").equals(revision(c))&&!revision(c).isEmpty())return true;
            clear(c);
            String revision=revision(c);if(revision.isEmpty()||!state(c).edit().putInt("scheduled_mask",mask).commit())return false;
            android.os.PersistableBundle extras=new android.os.PersistableBundle();extras.putString("revision",revision);
            JobInfo info=new JobInfo.Builder(JOB_ID,new ComponentName(c,AutopilotJobService.class))
                .setPeriodic(interval,Math.min(interval,6L*60*60*1000))
                .setPersisted(true).setRequiresCharging(true).setRequiresBatteryNotLow(true)
                .setExtras(extras).build();
            return js.schedule(info)==JobScheduler.RESULT_SUCCESS;
        }catch(RuntimeException e){return false;}
    }
    static boolean scheduled(Context c){
        JobScheduler js=c.getSystemService(JobScheduler.class);
        try{return js!=null&&js.getPendingJob(JOB_ID)!=null;}catch(RuntimeException e){return false;}
    }
    static String status(Context c,AppPreferences p){
        if(!p.autopilot())return "Autopilot paused";
        int wanted=requested(p),allowed=eligible(c,p);
        if(wanted==0)return "Choose media discovery in Agent settings";
        if(allowed==0)return "Full media access needed for background checks";
        SharedPreferences s=state(c);
        long checked=s.getLong("checked",0);
        if(s.getInt("eligible",0)==allowed&&AutopilotPolicy.fresh(checked,System.currentTimeMillis(),p.scanDays())){
            String when=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(checked));
            int count=s.getInt("count",0);
            return (count>0?count+" review candidates": "No candidates found")
                +(s.getBoolean("partial",true)?" · Partial check":"")+" · "+when;
        }
        long failed=s.getLong("failed",0);
        if(s.getInt("eligible",0)==allowed&&AutopilotPolicy.fresh(failed,System.currentTimeMillis(),p.scanDays()))return "Last background check incomplete · Try a manual scan";
        return scheduled(c)?"Background check scheduled · While charging":"Background check not scheduled";
    }
    static String breakdown(Context c,AppPreferences p){
        SharedPreferences s=state(c);int allowed=eligible(c,p);
        if(!p.autopilot()||allowed==0||s.getInt("eligible",0)!=allowed||!AutopilotPolicy.fresh(s.getLong("checked",0),System.currentTimeMillis(),p.scanDays()))return "";
        java.util.List<String> items=new java.util.ArrayList<>();
        if((allowed&1)!=0&&s.getInt("duplicates",0)>0)items.add(s.getInt("duplicates",0)+" duplicate copies");
        if((allowed&2)!=0&&s.getInt("similar",0)>0)items.add(s.getInt("similar",0)+" similar photos");
        if((allowed&4)!=0&&s.getInt("videos",0)>0)items.add(s.getInt("videos",0)+" large videos");
        return String.join(" · ",items);
    }
    private AutopilotScheduler(){}
}

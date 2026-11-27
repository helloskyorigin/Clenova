package com.clenova.app;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.provider.Settings;
import java.util.*;

/** Foreground, local review of launcher-visible apps with observed usage. */
final class UnusedAppScan {
    static final long DAY=86400000L;
    static final int LIMIT=250;
    static final class Item {
        final String pkg,name;final long firstInstall,updated,lastUsed,appBytes,dataBytes,cacheBytes;
        final Bitmap icon;
        Item(String pkg,String name,long first,long updated,long last,long app,long data,long cache,Bitmap icon){
            this.pkg=pkg;this.name=name;firstInstall=first;this.updated=updated;lastUsed=last;appBytes=app;dataBytes=data;cacheBytes=cache;this.icon=icon;
        }
        long bytes(){return appBytes<0||dataBytes<0?-1:appBytes+dataBytes;}
        long days(long now){return Math.max(0,(now-Math.max(lastUsed,updated))/DAY);}
    }
    static final class Result {
        final List<Item> items=new ArrayList<>();int unknown,checked;boolean partial,allowed;
    }
    private final Context context;private final PackageManager pm;
    UnusedAppScan(Context c){context=c.getApplicationContext();pm=context.getPackageManager();}
    static boolean allowed(Context c){
        AppOpsManager ops=c.getSystemService(AppOpsManager.class);
        if(ops==null)return false;
        try{return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
        catch(RuntimeException e){return false;}
    }
    private Set<String> protectedPackages(){
        Set<String> protectedNames=new HashSet<>();protectedNames.add(context.getPackageName());
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for(ResolveInfo info:pm.queryIntentActivities(home,0))if(info.activityInfo!=null)protectedNames.add(info.activityInfo.packageName);
        for(Intent intent:new Intent[]{new Intent(Intent.ACTION_DIAL),new Intent(Intent.ACTION_SENDTO,android.net.Uri.parse("smsto:"))}){
            ResolveInfo info=pm.resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);if(info!=null&&info.activityInfo!=null)protectedNames.add(info.activityInfo.packageName);
        }
        DevicePolicyManager dpm=context.getSystemService(DevicePolicyManager.class);
        if(dpm!=null)try{List<ComponentName> admins=dpm.getActiveAdmins();if(admins!=null)for(ComponentName a:admins)protectedNames.add(a.getPackageName());}catch(RuntimeException ignored){}
        for(String key:new String[]{"default_input_method","enabled_accessibility_services","enabled_notification_listeners","always_on_vpn_app"}){
            try{String value=Settings.Secure.getString(context.getContentResolver(),key);if(value==null)continue;
                for(String part:value.split(":")){ComponentName component=ComponentName.unflattenFromString(part);if(component!=null)protectedNames.add(component.getPackageName());else if(part.contains("."))protectedNames.add(part);}
            }catch(RuntimeException ignored){}
        }
        return protectedNames;
    }
    private boolean eligible(ApplicationInfo ai,Set<String> protectedNames){
        return ai!=null&&ai.enabled&&(ai.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0&&!protectedNames.contains(ai.packageName);
    }
    private Map<String,UsageStats> history(long now){
        UsageStatsManager usm=context.getSystemService(UsageStatsManager.class);
        if(usm==null)return Collections.emptyMap();
        Map<String,UsageStats> stats=usm.queryAndAggregateUsageStats(now-180*DAY,now);
        return stats==null?Collections.emptyMap():stats;
    }
    private long lastUsed(Map<String,UsageStats> history,PackageInfo info,long now){
        UsageStats stats=history.get(info.packageName);if(stats==null)return -1;
        long last=stats.getLastTimeUsed();
        // Foreground use or, where available, visible activity/foreground service activity.
        if(Build.VERSION.SDK_INT>=29)last=Math.max(last,Math.max(stats.getLastTimeVisible(),stats.getLastTimeForegroundServiceUsed()));
        if(last<=0||last>now||last<info.firstInstallTime||last<now-180*DAY)return -1;
        // Do not suggest newly updated apps based on pre-update inactivity.
        return last;
    }
    private Bitmap icon(ApplicationInfo ai){
        try{Drawable drawable=pm.getApplicationIcon(ai);Bitmap bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);drawable.setBounds(0,0,96,96);drawable.draw(canvas);return bitmap;}catch(RuntimeException e){return null;}
    }
    Result run(CancellationSignal cancel){
        Result result=new Result();result.allowed=allowed(context);if(!result.allowed)return result;
        long now=System.currentTimeMillis();Map<String,UsageStats> history=history(now);Set<String> protectedNames=protectedPackages();
        Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchers=pm.queryIntentActivities(launcher,0);
        TreeSet<String> packages=new TreeSet<>();for(ResolveInfo info:launchers)if(info.activityInfo!=null)packages.add(info.activityInfo.packageName);
        StorageStatsManager storage=context.getSystemService(StorageStatsManager.class);
        for(String pkg:packages){
            cancel.throwIfCanceled();if(result.checked>=LIMIT){result.partial=true;break;}result.checked++;
            try{PackageInfo info=pm.getPackageInfo(pkg,0);ApplicationInfo ai=info.applicationInfo;if(!eligible(ai,protectedNames))continue;
                long last=lastUsed(history,info,now);if(last<=0){result.unknown++;continue;}if(now-Math.max(last,info.lastUpdateTime)<30*DAY)continue;
                long app=-1,data=-1,cache=-1;
                if(storage!=null&&info.sharedUserId==null)try{StorageStats stats=storage.queryStatsForPackage(ai.storageUuid,pkg,android.os.Process.myUserHandle());app=stats.getAppBytes();data=stats.getDataBytes();cache=stats.getCacheBytes();}catch(Exception ignored){}
                CharSequence label=pm.getApplicationLabel(ai);result.items.add(new Item(pkg,label.toString(),info.firstInstallTime,info.lastUpdateTime,last,app,data,cache,icon(ai)));
            }catch(PackageManager.NameNotFoundException|RuntimeException e){result.partial=true;}
        }
        cancel.throwIfCanceled();if(!allowed(context)){result.items.clear();result.allowed=false;}return result;
    }
    boolean stillEligible(Item old,int days){
        if(!allowed(context))return false;
        try{long now=System.currentTimeMillis();PackageInfo info=pm.getPackageInfo(old.pkg,0);
            if(!eligible(info.applicationInfo,protectedPackages())||info.firstInstallTime!=old.firstInstall||info.lastUpdateTime!=old.updated)return false;
            Intent launch=pm.getLaunchIntentForPackage(old.pkg);if(launch==null)return false;
            long last=lastUsed(history(now),info,now);
            return last==old.lastUsed&&last>0&&now-Math.max(last,info.lastUpdateTime)>=days*DAY;
        }catch(Exception e){return false;}
    }
}

package com.clenova.app;

import android.app.admin.DevicePolicyManager;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import java.util.*;

/** Read-only inventory of launcher-visible apps in the current Android profile. */
final class AppManagerEngine {
    static final long DAY=86400000L,LARGE=500L*1024*1024;
    static final int LIMIT=1000;
    static final class Item {
        final String pkg,name,version,installer;final long installed,updated,versionCode,lastUsed,app,data,cache;
        final boolean system,enabled,protectedApp;final int uid;
        Item(String p,String n,String v,String installer,long i,long u,long vc,long last,long a,long d,long c,boolean system,boolean enabled,boolean protect,int uid){
            pkg=p;name=n;version=v;this.installer=installer;installed=i;updated=u;versionCode=vc;lastUsed=last;app=a;data=d;cache=c;this.system=system;this.enabled=enabled;protectedApp=protect;this.uid=uid;
        }
        long bytes(){return app<0||data<0||app>Long.MAX_VALUE-data?-1:app+data;}
        long userData(){return data<0||cache<0||cache>data?-1:data-cache;}
        boolean unused(long now){return lastUsed>0&&now-Math.max(lastUsed,updated)>=30*DAY&&!protectedApp;}
    }
    static final class Result {
        final List<Item> items=new ArrayList<>();boolean usage,partial;
        int measured(){int n=0;for(Item i:items)if(i.bytes()>=0)n++;return n;}
        long appBytes(){long sum=0;for(Item i:items)if(i.bytes()>=0)sum+=i.app;return sum;}
        long dataBytes(){long sum=0;for(Item i:items)if(i.bytes()>=0)sum+=i.data;return sum;}
    }
    static final class Permission {
        final String name,label;final boolean granted;
        Permission(String n,String l,boolean g){name=n;label=l;granted=g;}
    }
    private final Context context;private final PackageManager pm;
    AppManagerEngine(Context c){context=c.getApplicationContext();pm=context.getPackageManager();}
    static boolean usageAllowed(Context c){return UnusedAppScan.allowed(c);}
    private Set<String> protectedPackages(){
        Set<String> names=new HashSet<>();names.add(context.getPackageName());
        for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0))if(r.activityInfo!=null)names.add(r.activityInfo.packageName);
        for(Intent i:new Intent[]{new Intent(Intent.ACTION_DIAL),new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:")),new Intent(Intent.ACTION_VIEW,Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)}){
            ResolveInfo r=pm.resolveActivity(i,PackageManager.MATCH_DEFAULT_ONLY);if(r!=null&&r.activityInfo!=null)names.add(r.activityInfo.packageName);
        }
        DevicePolicyManager dpm=context.getSystemService(DevicePolicyManager.class);
        if(dpm==null)throw new IllegalStateException("Device policy unavailable");
        List<ComponentName> admins=dpm.getActiveAdmins();if(admins!=null)for(ComponentName a:admins)names.add(a.getPackageName());
        for(String key:new String[]{"default_input_method","enabled_input_methods","enabled_accessibility_services","enabled_notification_listeners","always_on_vpn_app","enabled_vr_listeners"}){
            String value=Settings.Secure.getString(context.getContentResolver(),key);if(value==null)continue;
            for(String part:value.split(":")){String name=part.split(";",2)[0];ComponentName c=ComponentName.unflattenFromString(name);if(c!=null)names.add(c.getPackageName());else if(name.contains("."))names.add(name);}
        }
        return names;
    }
    private boolean system(ApplicationInfo a){return (a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;}
    private boolean protectedApp(ApplicationInfo a,Set<String> protections){return protections==null||system(a)||!a.enabled||protections.contains(a.packageName);}
    private Set<String> launchers(){Set<String> set=new TreeSet<>();Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);for(ResolveInfo r:pm.queryIntentActivities(i,0))if(r.activityInfo!=null)set.add(r.activityInfo.packageName);return set;}
    private Map<String,UsageStats> usage(long now){
        UsageStatsManager manager=context.getSystemService(UsageStatsManager.class);if(manager==null)return Collections.emptyMap();
        Map<String,UsageStats> stats=manager.queryAndAggregateUsageStats(now-180*DAY,now);return stats==null?Collections.emptyMap():stats;
    }
    private long last(Map<String,UsageStats> map,PackageInfo p,long now){
        UsageStats s=map.get(p.packageName);if(s==null)return -1;long value=s.getLastTimeUsed();
        if(Build.VERSION.SDK_INT>=29)value=Math.max(value,Math.max(s.getLastTimeVisible(),s.getLastTimeForegroundServiceUsed()));
        return value<=0||value>now||value<p.firstInstallTime||value<now-180*DAY?-1:value;
    }
    private String installer(String pkg){try{return Build.VERSION.SDK_INT>=30?pm.getInstallSourceInfo(pkg).getInstallingPackageName():pm.getInstallerPackageName(pkg);}catch(Exception e){return null;}}
    private long version(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
    Result scan(CancellationSignal cancel){
        Result r=new Result();r.usage=usageAllowed(context);long now=System.currentTimeMillis();Map<String,UsageStats> used=Collections.emptyMap();Set<String> protections=null;
        try{protections=protectedPackages();}catch(RuntimeException e){r.partial=true;}
        if(r.usage)try{used=usage(now);}catch(RuntimeException e){r.partial=true;}
        StorageStatsManager storage=context.getSystemService(StorageStatsManager.class);Set<String> packages=launchers();int count=0;
        for(String pkg:packages){cancel.throwIfCanceled();if(count++>=LIMIT){r.partial=true;break;}
            try{PackageInfo p=pm.getPackageInfo(pkg,0);ApplicationInfo a=p.applicationInfo;if(a==null)continue;
                long app=-1,data=-1,cache=-1;
                // Cache is already included in Android's dataBytes. Do not count it twice.
                if(r.usage&&storage!=null&&p.sharedUserId==null)try{
                    StorageStats st=storage.queryStatsForPackage(a.storageUuid,pkg,android.os.Process.myUserHandle());
                    long x=st.getAppBytes(),y=st.getDataBytes(),z=st.getCacheBytes();if(x>=0&&y>=0&&x<=Long.MAX_VALUE-y){app=x;data=y;if(z>=0&&z<=y)cache=z;}
                }catch(Exception ignored){}
                r.items.add(new Item(pkg,pm.getApplicationLabel(a).toString(),p.versionName,installer(pkg),p.firstInstallTime,p.lastUpdateTime,version(p),last(used,p,now),app,data,cache,system(a),a.enabled,protectedApp(a,protections),a.uid));
            }catch(PackageManager.NameNotFoundException|RuntimeException e){r.partial=true;}
        }
        cancel.throwIfCanceled();if(r.usage&&!usageAllowed(context)){return scanWithoutUsage(cancel); }
        return r;
    }
    private Result scanWithoutUsage(CancellationSignal cancel){
        Result r=new Result();r.usage=false;r.partial=true;Set<String> protections=null;
        try{protections=protectedPackages();}catch(RuntimeException ignored){}
        int count=0;for(String pkg:launchers()){cancel.throwIfCanceled();if(count++>=LIMIT)break;
            try{PackageInfo p=pm.getPackageInfo(pkg,0);ApplicationInfo a=p.applicationInfo;if(a==null)continue;r.items.add(new Item(pkg,pm.getApplicationLabel(a).toString(),p.versionName,installer(pkg),p.firstInstallTime,p.lastUpdateTime,version(p),-1,-1,-1,-1,system(a),a.enabled,protectedApp(a,protections),a.uid));}
            catch(PackageManager.NameNotFoundException|RuntimeException ignored){}
        }return r;
    }
    Bitmap icon(String pkg){try{Drawable d=pm.getApplicationIcon(pkg);Bitmap b=Bitmap.createBitmap(112,112,Bitmap.Config.ARGB_8888);d.setBounds(0,0,112,112);d.draw(new Canvas(b));return b;}catch(Exception|OutOfMemoryError e){return null;}}
    List<Permission> permissions(Item item)throws Exception{
        PackageInfo p=pm.getPackageInfo(item.pkg,PackageManager.GET_PERMISSIONS);if(!same(item,p))throw new IllegalStateException("App changed");
        List<Permission> list=new ArrayList<>();if(p.requestedPermissions==null)return list;
        for(int i=0;i<p.requestedPermissions.length;i++){String name=p.requestedPermissions[i],label=name;
            try{PermissionInfo info=pm.getPermissionInfo(name,0);CharSequence value=info.loadLabel(pm);if(value!=null)label=value.toString();}catch(PackageManager.NameNotFoundException ignored){}
            boolean granted=p.requestedPermissionsFlags!=null&&i<p.requestedPermissionsFlags.length&&(p.requestedPermissionsFlags[i]&PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0;
            list.add(new Permission(name,label,granted));
        }list.sort(Comparator.comparing((Permission p0)->p0.label,String.CASE_INSENSITIVE_ORDER).thenComparing(p0->p0.name));return list;
    }
    private boolean same(Item old,PackageInfo p){return p.applicationInfo!=null&&p.firstInstallTime==old.installed&&p.lastUpdateTime==old.updated&&version(p)==old.versionCode&&p.applicationInfo.uid==old.uid;}
    boolean eligible(Item old){
        if(old==null||old.protectedApp||old.system||!old.enabled)return false;
        try{PackageInfo p=pm.getPackageInfo(old.pkg,0);return same(old,p)&&launchers().contains(old.pkg)&&!protectedApp(p.applicationInfo,protectedPackages());}
        catch(Exception e){return false;}
    }
    boolean present(String pkg){try{pm.getPackageInfo(pkg,0);return true;}catch(PackageManager.NameNotFoundException e){return false;}}
    boolean confirmedAbsent(String pkg){
        try{pm.getPackageInfo(pkg,0);return false;}catch(PackageManager.NameNotFoundException e){try{return !launchers().contains(pkg);}catch(RuntimeException problem){return false;}}catch(RuntimeException e){return false;}
    }
}

package com.clenova.app;

import android.content.Context;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.os.Process;
import java.io.File;

/** Only app-owned settings and journal stores are reset; user files never touched. */
final class LocalAppData {
    private static final String[] STORES={"clenova_preferences","clenova_folder_exclusions","clenova_file_exclusions","clenova_activity_history","clenova_permission_requests","clenova_agent_chats"};
    static final class Size{
        final long bytes;final boolean complete;
        Size(long bytes,boolean complete){this.bytes=bytes;this.complete=complete;}
    }
    static Size measure(Context context){
        try{
            StorageStatsManager manager=context.getSystemService(StorageStatsManager.class);
            if(manager!=null){
                StorageStats stats=manager.queryStatsForUid(context.getApplicationInfo().storageUuid,Process.myUid());
                return new Size(stats.getDataBytes(),true);
            }
        }catch(java.io.IOException|RuntimeException ignored){}
        try{
            File directory=new File(context.getApplicationInfo().dataDir,"shared_prefs");
            long bytes=0;
            for(String store:STORES)for(String suffix:new String[]{".xml",".xml.bak"}){
                File file=new File(directory,store+suffix);if(file.isFile())bytes+=file.length();
            }
            return new Size(bytes,false);
        }catch(RuntimeException ignored){return new Size(-1,false);}
    }
    static boolean reset(Context context){
        boolean ok=true;
        for(String name:STORES){
            try{if(!context.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear().commit())ok=false;}
            catch(RuntimeException e){ok=false;}
        }
        return ok;
    }
}

package com.clenova.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.NotificationManager;
import android.app.NotificationChannel;

/** Local persistent preferences; no account or network required. */
final class AppPreferences {
    static final String[] LANG_CODES={"en","hi","es","fr","de"};
    static final String[] LANG_NAMES={"English","हिन्दी","Español","Français","Deutsch"};
    static final String[] NOTIFICATION_KEYS={"agent_updates","cleanup_reminders","storage_alerts"};
    static final String[] NOTIFICATION_LABELS={"Agent updates","Cleanup reminders","Storage alerts"};
    private final SharedPreferences store;
    private final NotificationManager manager;
    AppPreferences(Context context){
        store=context.getSharedPreferences("clenova_preferences",Context.MODE_PRIVATE);
        manager=context.getSystemService(NotificationManager.class);
        if(manager!=null)for(int i=0;i<NOTIFICATION_KEYS.length;i++){
            manager.createNotificationChannel(new NotificationChannel(NOTIFICATION_KEYS[i],NOTIFICATION_LABELS[i],NotificationManager.IMPORTANCE_DEFAULT));
        }
    }
    private static final String[] DISCOVERY_KEYS={"discover_duplicates","discover_similar","discover_videos","discover_unnecessary"};
    // Activation is an explicit preference, not proof of scheduled work.
    boolean autopilot(){return store.getBoolean("agent_autopilot",false);}
    boolean agentConfigured(){return store.getBoolean("agent_setup_configured",false);}
    boolean saveAgentSetup(boolean enabled){
        SharedPreferences.Editor edit=store.edit().putBoolean("agent_setup_configured",true).putBoolean("agent_autopilot",enabled);
        if(enabled){
            // Preserve previously customized choices. Initialize only missing preferences.
            if(!store.contains("agent_scan_days"))edit.putInt("agent_scan_days",1);
            for(String key:DISCOVERY_KEYS)if(!store.contains(key))edit.putBoolean(key,true);
            for(String key:PROTECTION_KEYS)if(!store.contains(key))edit.putBoolean(key,true);
        }
        return edit.commit();
    }
    void setAutopilot(boolean enabled){saveAgentSetup(enabled);}
    int scanDays(){int days=store.getInt("agent_scan_days",1);return days==3||days==7?days:1;}
    void setScanDays(int days){if(days!=1&&days!=3&&days!=7)throw new IllegalArgumentException("Invalid interval");store.edit().putInt("agent_scan_days",days).apply();}
    boolean discovery(int index){return store.getBoolean(DISCOVERY_KEYS[index],true);}
    void setDiscovery(int index,boolean enabled){store.edit().putBoolean(DISCOVERY_KEYS[index],enabled).apply();}
    int recommendedMb(){
        if(!autopilot())return 0;
        int total=0;for(int i=0;i<ReviewPlan.MB.length;i++)if(discovery(i))total+=ReviewPlan.MB[i];
        return total;
    }
    private static final String[] PROTECTION_KEYS={"protect_favorites","protect_recent","protect_documents","protect_best_shots"};
    boolean protection(int index){return store.getBoolean(PROTECTION_KEYS[index],true);}
    void setProtection(int index,boolean enabled){store.edit().putBoolean(PROTECTION_KEYS[index],enabled).apply();}
    ProtectionPolicy protectionPolicy(){return new ProtectionPolicy(protection(0),protection(1),protection(2),protection(3));}
    String appearance(){String value=store.getString("appearance","System");return value.equals("Dark")||value.equals("Light")?value:"System";}
    void setAppearance(String value){if(!value.equals("Light")&&!value.equals("Dark")&&!value.equals("System"))throw new IllegalArgumentException();store.edit().putString("appearance",value).apply();}
    String language(){String value=store.getString("language","en");for(String code:LANG_CODES)if(code.equals(value))return value;return "en";}
    String languageName(){for(int i=0;i<LANG_CODES.length;i++)if(LANG_CODES[i].equals(language()))return LANG_NAMES[i];return LANG_NAMES[0];}
    void setLanguage(String code){for(String value:LANG_CODES)if(value.equals(code)){store.edit().putString("language",code).apply();return;}throw new IllegalArgumentException();}
    boolean notificationEnabled(int i){return store.getBoolean(NOTIFICATION_KEYS[i],true);}
    void setNotification(int i,boolean enabled){store.edit().putBoolean(NOTIFICATION_KEYS[i],enabled).apply();if(!enabled&&manager!=null)manager.cancel(NOTIFICATION_KEYS[i],i);}
    boolean systemNotificationsAllowed(){return manager!=null&&manager.areNotificationsEnabled();}
    boolean canNotify(int i){return notificationEnabled(i)&&systemNotificationsAllowed()&&manager.getNotificationChannel(NOTIFICATION_KEYS[i])!=null&&manager.getNotificationChannel(NOTIFICATION_KEYS[i]).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
}

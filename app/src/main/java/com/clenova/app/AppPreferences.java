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
    String appearance(){String value=store.getString("appearance","System");return value.equals("Dark")||value.equals("Light")?value:"System";}
    void setAppearance(String value){if(!value.equals("Light")&&!value.equals("Dark")&&!value.equals("System"))throw new IllegalArgumentException();store.edit().putString("appearance",value).apply();}
    String language(){String value=store.getString("language","en");for(String code:LANG_CODES)if(code.equals(value))return value;return "en";}
    String languageName(){for(int i=0;i<LANG_CODES.length;i++)if(LANG_CODES[i].equals(language()))return LANG_NAMES[i];return LANG_NAMES[0];}
    void setLanguage(String code){for(String value:LANG_CODES)if(value.equals(code)){store.edit().putString("language",code).apply();return;}throw new IllegalArgumentException();}
    boolean notificationEnabled(int i){return store.getBoolean(NOTIFICATION_KEYS[i],true);}
    void setNotification(int i,boolean enabled){store.edit().putBoolean(NOTIFICATION_KEYS[i],enabled).apply();if(!enabled&&manager!=null)manager.cancelAll();}
    boolean systemNotificationsAllowed(){return manager!=null&&manager.areNotificationsEnabled();}
    boolean canNotify(int i){return notificationEnabled(i)&&systemNotificationsAllowed()&&manager.getNotificationChannel(NOTIFICATION_KEYS[i])!=null&&manager.getNotificationChannel(NOTIFICATION_KEYS[i]).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
}

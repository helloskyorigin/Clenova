package com.clenova.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

/** Activity-scoped setup state machine. Every granted state is read from Android. */
final class PermissionSetup {
    static final int MEDIA=210, NOTIFICATIONS=211, USAGE=212, FOLDER=213, MEDIA_SETTINGS=214, NOTIFICATION_SETTINGS=215;
    private final Activity host;
    private final DevicePermissions permissions;
    private final SetupFolderAccess folders;
    private final Runnable finished;
    private final PermissionSetupView view;
    private int step,pending;
    private boolean mediaDenied;
    private String previous="",message="";
    PermissionSetup(Activity activity,Bundle state,boolean dark,Runnable done){
        host=activity;permissions=new DevicePermissions(host);folders=new SetupFolderAccess(host);finished=done;
        if(state!=null){step=Math.max(0,Math.min(4,state.getInt("access_step")));pending=state.getInt("access_pending");mediaDenied=state.getBoolean("access_media_denied");previous=state.getString("access_previous","");message=state.getString("access_message","");}
        view=new PermissionSetupView(host,dark,this::allow,this::skip);render();
    }
    PermissionSetupView view(){return view;}
    boolean busy(){return pending!=0;}
    void save(Bundle out){out.putInt("access_step",step);out.putInt("access_pending",pending);out.putBoolean("access_media_denied",mediaDenied);out.putString("access_previous",previous);out.putString("access_message",message);}
    private String media(){return permissions.snapshot().media;}
    private String notifications(){return permissions.snapshot().notifications;}
    private boolean hasMedia(){return "Allowed".equals(media())||"Limited".equals(media());}
    private boolean usableNotifications(){String value=notifications();return "Allowed".equals(value)||"Limited".equals(value);}
    private void render(){
        String action="Allow",alternate="";
        if(step==0){if(hasMedia())action="Continue";else if(mediaDenied)alternate="Continue without media";}
        else if(step==1){if(usableNotifications())action="Continue";}
        else if(step==2){if(UnusedAppScan.allowed(host))action="Continue";alternate="Not now";}
        else if(step==3){if(folders.current()!=null)action="Continue";alternate="Not now";}
        else action="Continue";
        view.display(step,previous,action,alternate,message);view.setBusy(pending!=0);
    }
    private void advance(String result){pending=0;previous=result;message="";step=Math.min(4,step+1);render();}
    private android.content.SharedPreferences flags(){return host.getSharedPreferences("clenova_permission_requests",Activity.MODE_PRIVATE);}
    private void request(int code,String[] names){
        pending=code;render();
        try{host.requestPermissions(names,code);}catch(RuntimeException e){pending=0;message="Android could not open the permission request.";if(step==0)mediaDenied=true;else if(step==1){advance("Not allowed");return;}render();}
    }
    private void external(int code,Intent intent){
        pending=code;render();
        try{host.startActivityForResult(intent,code);}catch(RuntimeException e){pending=0;message="Android could not open this access setting.";if(step==0)mediaDenied=true;else if(step==1){advance("Not allowed");return;}render();}
    }
    private Intent appSettings(){return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",host.getPackageName(),null));}
    private String[] mediaPermissions(){return ToolAccess.requestNames(ToolAccess.MEDIA);}
    private void allow(){
        if(busy()||host.isFinishing()||host.isDestroyed())return;
        if(step==0){
            if(hasMedia()){advance(media());return;}
            String[] names=mediaPermissions();
            if(ToolAccess.settingsNeeded(host,ToolAccess.MEDIA)){external(MEDIA_SETTINGS,appSettings());return;}
            ToolAccess.remember(host,names);request(MEDIA,names);
        }else if(step==1){
            if(usableNotifications()){advance(notifications());return;}
            if(Build.VERSION.SDK_INT>=33&&!permissions.granted(Manifest.permission.POST_NOTIFICATIONS)&&(!flags().getBoolean("notifications_requested",false)||host.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))){
                flags().edit().putBoolean("notifications_requested",true).apply();request(NOTIFICATIONS,new String[]{Manifest.permission.POST_NOTIFICATIONS});
            }else external(NOTIFICATION_SETTINGS,new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,host.getPackageName()));
        }else if(step==2){
            if(UnusedAppScan.allowed(host)){advance("Allowed");return;}
            external(USAGE,new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }else if(step==3){
            if(folders.current()!=null){advance("Allowed");return;}
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);external(FOLDER,intent);
        }else{
            // Recheck media before leaving. Denial is respected; full media features stay gated by Android.
            if(!hasMedia()&&!mediaDenied){step=0;previous="";message="Media access changed. Review access again.";render();return;}
            finished.run();
        }
    }
    private void skip(){
        if(busy())return;
        if(step==0&&mediaDenied){advance("Not allowed");return;}
        if(step==2||step==3){advance(step==2&&UnusedAppScan.allowed(host)||step==3&&folders.current()!=null?"Allowed":"Skipped");}
    }
    private void mediaResult(){
        pending=0;
        if(hasMedia()){advance(media());return;}
        mediaDenied=true;message="Media access was not granted. Allow access for cleaning, or continue without media features.";render();
    }
    boolean permissionResult(int code){
        if(code!=MEDIA&&code!=NOTIFICATIONS)return false;
        if(pending!=code)return true;
        if(code==MEDIA)mediaResult();else advance(usableNotifications()?notifications():"Not allowed");
        return true;
    }
    boolean activityResult(int code,int result,Intent data){
        if(code<USAGE||code>NOTIFICATION_SETTINGS)return false;
        if(pending!=code)return true;
        pending=0;
        if(code==MEDIA_SETTINGS)mediaResult();
        else if(code==NOTIFICATION_SETTINGS)advance(usableNotifications()?notifications():"Not allowed");
        else if(code==USAGE){if(UnusedAppScan.allowed(host))advance("Allowed");else{message="Usage access not enabled. You can try again or skip.";render();}}
        else if(code==FOLDER){if(result==Activity.RESULT_OK&&folders.save(data))advance("Allowed");else{message=result==Activity.RESULT_OK?"Choose a supported local subfolder, or skip.":"No folder selected. You can try again or skip.";render();}}
        return true;
    }
    void refresh(){if(!busy())render();}
}

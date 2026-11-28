package com.clenova.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private int BG = Color.rgb(245,245,247);
    private int WHITE = Color.WHITE;
    private int INK = Color.rgb(17,17,17);
    private int GRAY = Color.rgb(134,134,139);
    private int BLUE = Color.rgb(0,122,255);
    private int PALE = Color.rgb(230,240,255);
    private int GREEN = Color.rgb(39,152,77);
    private float density;
    private float compact = 1f;
    private boolean toolsVisible;
    private ScrollView toolsScroll;
    private int toolsScrollY;
    private boolean cleanVisible,agentVisible,agentTaskVisible,agentTaskOrigin,agentShortcut;
    private String agentCommand="";
    private int agentTask=-1;
    private android.widget.EditText agentInput;
    private static final int REQUEST_AGENT_MEDIA=104;
    private AgentChatStore agentChats;
    private AgentChatStore.Chat activeChat;
    private final android.os.Handler agentSessionHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable agentExpiry=this::handleAgentExpiry;
    private void handleAgentExpiry(){boolean expired=expireAgentSession();if(expired&&agentVisible&&!settingsVisible){if(agentDrawer!=null)agentDrawer.dismiss();showAgent();}else scheduleAgentExpiry();}
    private SmartMediaScan agentScan;
    private SmartMediaScan.Result agentLiveResult;
    private String agentResultChat="",agentStage="",relayEndpoint="",relayToken="";
    private boolean agentCloudBusy,agentPermissionPending,agentForeground=true,agentTransition;
    private final java.util.concurrent.ThreadPoolExecutor relayWorker=new java.util.concurrent.ThreadPoolExecutor(1,1,15,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.ArrayBlockingQueue<>(1));
    private java.util.concurrent.Future<?> relayRequest;
    private void cancelRelayRequest(){if(relayRequest!=null){relayRequest.cancel(true);relayRequest=null;}relayWorker.purge();}
    private int agentGeneration,agentScanTask;
    private TextView agentProgressText;
    private android.widget.ProgressBar agentProgressBar;
    private android.app.Dialog agentDrawer;
    private static final int REQUEST_SCAN_MEDIA=96;
    private static final int REQUEST_APP_UNINSTALL=103;
    private boolean unusedVisible,unusedReview,unusedExternal,unusedUninstallPending;
    private int unusedGeneration,unusedDays=30,unusedSort,unusedPage;
    private UnusedAppScan.Result unusedResult;
    private final java.util.Set<String> unusedSelected=new java.util.LinkedHashSet<>(),unusedKeep=new java.util.HashSet<>();
    private android.os.CancellationSignal unusedSignal;
    private android.app.Dialog unusedSheet;
    private static final int PICK_DOWNLOAD_FILES=101, PICK_LARGE_FOLDER=102;
    private boolean largeFilesMode,clutterMode;
    private int largeFileThreshold=0;
    private long largeMinimum(){return new long[]{100L*1024*1024,500L*1024*1024,1024L*1024*1024}[largeFileThreshold];}
    private String fileManagerTitle(){return clutterMode?"Unnecessary files":largeFilesMode?"Large files":"Downloads";}
    private boolean downloadsVisible,downloadsReview,downloadsBusy,downloadsPicking,downloadsOpening;
    private int downloadsFilter,downloadsSort,downloadsPage,downloadsGeneration;
    private String downloadsQuery="";
    private final java.util.List<DownloadFiles.Item> downloadItems=new java.util.ArrayList<>();
    private final java.util.Set<String> downloadSelected=new java.util.LinkedHashSet<>(),downloadKeep=new java.util.HashSet<>();
    private android.os.CancellationSignal downloadsSignal;
    private android.app.Dialog downloadSheet;
    private SmartMediaScan runningScan;
    private static final int REQUEST_DUP_TRASH=97, REQUEST_SIMILAR_TRASH=98, REQUEST_VIDEO_TRASH=99, REQUEST_SCREENSHOT_TRASH=100;
    private boolean realShotVisible,realShotReview,realShotPreview,shotAfterScan,shotFromResults;
    private int shotPage,shotFilter,shotSort;
    private final java.util.Set<String> shotSelected=new java.util.LinkedHashSet<>(),shotKeep=new java.util.HashSet<>();
    private boolean realVideoVisible,realVideoReview,realVideoPreview,videoAfterScan,videoFromResults;
    private int realVideoPage,realVideoSort;
    private final java.util.Set<String> realVideoSelected=new java.util.LinkedHashSet<>(),realVideoKeep=new java.util.HashSet<>();
    private android.widget.VideoView activeVideoPlayer;
    private boolean realSimilarVisible,realSimilarReview,similarAfterScan,similarFromResults,similarAll;
    private int realSimilarPage;
    private final java.util.Set<String> realSimilarSelected=new java.util.LinkedHashSet<>(), realSimilarKeep=new java.util.HashSet<>();
    private boolean realDupVisible,realDupReview,dupAfterScan,dupFromResults,dupAll,dupTrashPending;
    private CleanupReceipt cleanupReceipt;
    private boolean verifyingCleanup;
    private String pendingUninstallPackage="",pendingUninstallName="",pendingUninstallId="";
    private int dupPage;
    private volatile int dupThumbGeneration;
    private final java.util.Set<String> realDupSelected=new java.util.LinkedHashSet<>();
    private final java.util.concurrent.ThreadPoolExecutor dupThumbs=PreviewWork.pool(2);
    private final android.util.LruCache<String,android.graphics.Bitmap> dupCache=new android.util.LruCache<String,android.graphics.Bitmap>(PreviewWork.cacheBytes(Runtime.getRuntime().maxMemory())){@Override protected int sizeOf(String key,android.graphics.Bitmap value){return value.getByteCount();}};
    private SmartMediaScan.Result scanResult;
    private boolean scanVisible, scanReviewVisible, requestingScan;
    private final boolean[] scanSelected=new boolean[4];
    private TextView scanStatus,scanProgress;
    private android.widget.ProgressBar scanBar;
    private ScrollView cleanScroll;
    private int cleanScrollY;
    private boolean completionVisible;
    private boolean mediaVerificationPending,mediaVerificationApproved;
    private boolean storageVisible, settingsVisible, agentPrefsVisible, protectionVisible, foldersVisible, filesVisible, historyVisible, permissionsVisible, privacyVisible, helpVisible, feedbackVisible, policyVisible, termsVisible, aboutVisible;
    private ScrollView aboutScroll;
    private int aboutScrollY;
    private static final String PUBLIC_LISTING_URL=""; // Set only after publication.
    private ScrollView policyScroll;
    private int policyScrollY;
    private int feedbackCategory=0;
    private String feedbackText="";
    private boolean feedbackAppInfo=true;
    private android.app.Dialog ratingDialog;
    private int ratingStars;
    private String helpQuery="";
    private int expandedFaq=-1;
    private static final String SUPPORT_EMAIL="hello.skyorigin@gmail.com";
    private static final int EXPORT_ACTIVITY_HISTORY=95;
    private DevicePermissions devicePermissions;
    private static final int REQUEST_NOTIFICATION_ACCESS=94;
    private ActivityHistory history;
    private int historyFilter;
    private String historyDetailId;
    private ScrollView historyScroll;
    private int historyScrollY;
    private ProtectedFiles protectedFiles;
    private static final int PICK_PROTECTED_FILES=93;
    private ProtectedFolders protectedFolders;
    private static final int PICK_PROTECTED_FOLDER=92;
    private AppPreferences prefs;
    private android.widget.PopupWindow preferencePopup;
    private boolean dark;
    private LaunchSplashView launchSplash;
    private AgentOnboardingView onboardingIntro;
    private UnifiedOnboardingView onboardingUnified;
    private AutopilotOnboardingView onboardingAutopilot;
    private PermissionSetup onboardingAccess;
    private AgentSetupView onboardingAgent;
    private boolean agentSetupFromTab;
    private PreparingPhoneView preparingPhone;
    private DeviceOverview overviewJob;
    private DeviceOverview.Snapshot deviceOverview;
    private View dashboardView;
    private boolean dashboardStorage,renderingOverview,preparationReady;
    private final android.os.Handler overviewHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean overviewForeground,overviewObserverRegistered,overviewDirty;
    private final Runnable refreshVisibleOverview=()->{
        if(!overviewForeground||preparingPhone!=null||dashboardView==null||!dashboardView.isAttachedToWindow())return;
        if(overviewJob!=null){overviewDirty=true;return;}
        overviewDirty=false;beginOverview(false);
    };
    private final android.database.ContentObserver overviewObserver=new android.database.ContentObserver(overviewHandler){
        @Override public void onChange(boolean selfChange){queueOverviewRefresh();}
        @Override public void onChange(boolean selfChange,android.net.Uri uri){queueOverviewRefresh();}
    };
    private void queueOverviewRefresh(){
        if(!overviewForeground||dashboardView==null||!dashboardView.isAttachedToWindow()||preparingPhone!=null)return;
        overviewHandler.removeCallbacks(refreshVisibleOverview);
        overviewHandler.postDelayed(refreshVisibleOverview,750);
    }
    private void observeOverview(){
        if(overviewObserverRegistered)return;
        try{
            getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,true,overviewObserver);
            getContentResolver().registerContentObserver(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,true,overviewObserver);
            overviewObserverRegistered=true;
        }catch(RuntimeException e){try{getContentResolver().unregisterContentObserver(overviewObserver);}catch(RuntimeException ignored){}}
    }
    private void stopOverviewObserver(){
        overviewHandler.removeCallbacks(refreshVisibleOverview);overviewDirty=false;
        if(overviewObserverRegistered){try{getContentResolver().unregisterContentObserver(overviewObserver);}catch(RuntimeException ignored){}overviewObserverRegistered=false;}
    }
    private final Runnable finishPreparation=()->{
        if(preparingPhone==null||!preparationReady||isFinishing()||isDestroyed())return;
        if(!prefs.finishOnboarding()){preparationReady=false;preparingPhone.error("Could not save setup. Try again.");return;}
        preparingPhone=null;preparationReady=false;applyPalette();
        renderingOverview=true;try{showHome();}finally{renderingOverview=false;}
    };
    private final android.os.Handler splashHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private long splashDeadline;
    private final Runnable finishSplash=()->{if(launchSplash==null||isFinishing()||isDestroyed())return;launchSplash=null;if(prefs.onboardingComplete())showHome();else showOnboardingIntro();};

    private void showOnboardingIntro(){
        applyPalette();
        onboardingIntro=new AgentOnboardingView(this,dark,()->{
            if(onboardingIntro==null||isFinishing()||isDestroyed())return;
            onboardingIntro.setActive(false);onboardingIntro=null;showOnboardingUnified();
        });
        setContentView(onboardingIntro);onboardingIntro.playEntrance();onboardingIntro.setActive(true);
    }

    private void showOnboardingUnified(){
        applyPalette();
        onboardingUnified=new UnifiedOnboardingView(this,dark,()->{
            if(onboardingUnified==null||isFinishing()||isDestroyed())return;
            onboardingUnified.setActive(false);onboardingUnified=null;showOnboardingAutopilot();
        });
        setContentView(onboardingUnified);onboardingUnified.playEntrance();onboardingUnified.setActive(true);
    }

    private void showOnboardingAutopilot(){
        applyPalette();
        onboardingAutopilot=new AutopilotOnboardingView(this,dark,()->{
            if(onboardingAutopilot==null||isFinishing()||isDestroyed())return;
            onboardingAutopilot.setActive(false);onboardingAutopilot=null;showOnboardingAccess(null);
        });
        setContentView(onboardingAutopilot);onboardingAutopilot.playEntrance();onboardingAutopilot.setActive(true);
    }

    private void showOnboardingAccess(Bundle state){
        applyPalette();
        onboardingAccess=new PermissionSetup(this,state,dark,()->{
            if(onboardingAccess==null||isFinishing()||isDestroyed())return;
            onboardingAccess=null;showAgentSetup(false,false);
        });
        setContentView(onboardingAccess.view());
    }

    private void showAgentSetup(boolean fromTab,boolean confirmed){
        agentSetupFromTab=fromTab;
        applyPalette();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        onboardingAgent=new AgentSetupView(this,prefs,confirmed,()->{
            if(onboardingAgent==null||isFinishing()||isDestroyed())return;
            onboardingAgent.setActive(false);onboardingAgent=null;applyPalette();
            if(agentSetupFromTab){agentSetupFromTab=false;showAgent();}
            else showPreparingPhone();
        });
        setContentView(onboardingAgent);onboardingAgent.setActive(true);
    }

    private void showPreparingPhone(){
        cancelOverview();overviewHandler.removeCallbacks(finishPreparation);preparationReady=false;
        applyPalette();
        preparingPhone=new PreparingPhoneView(this,dark,this::showPreparingPhone);setContentView(preparingPhone);beginOverview(true);
    }
    private void cancelOverview(){if(overviewJob!=null){overviewJob.cancel();overviewJob=null;}}
    private void beginOverview(boolean initial){
        if(overviewJob!=null||isFinishing()||isDestroyed()||!overviewForeground)return;
        final DeviceOverview job=new DeviceOverview(this,prefs);overviewJob=job;
        new Thread(()->{
            try{
                DeviceOverview.Snapshot result=job.run((stage,snapshot)->{if(initial&&stage<DeviceOverview.STAGES)runOnUiThread(()->{if(overviewJob==job&&preparingPhone!=null)preparingPhone.update(stage,snapshot);});});
                runOnUiThread(()->{
                    if(overviewJob!=job||isFinishing()||isDestroyed())return;overviewJob=null;deviceOverview=result;
                    if(initial&&preparingPhone!=null){
                        preparationReady=true;preparingPhone.update(DeviceOverview.STAGES,result);
                        // Only the completed visual confirmation has a short presentation delay.
                        overviewHandler.postDelayed(finishPreparation,ValueAnimator.areAnimatorsEnabled()?PreparationPolicy.COMPLETE_DWELL_MS:0);
                    }else if(dashboardView!=null&&dashboardView.isAttachedToWindow()){renderOverviewDashboard();if(overviewDirty){overviewDirty=false;queueOverviewRefresh();}}
                });
            }catch(Exception e){runOnUiThread(()->{if(overviewJob!=job||isDestroyed())return;overviewJob=null;
                if(initial&&preparingPhone!=null)preparingPhone.error("Please try again. No files were changed.");
                else if(dashboardView!=null&&dashboardView.isAttachedToWindow()){deviceOverview=new DeviceOverview.Snapshot();deviceOverview.finished=true;renderOverviewDashboard();if(overviewDirty){overviewDirty=false;queueOverviewRefresh();}}
            });}
        },"Clenova-device-overview").start();
    }
    private void attachOverviewDashboard(View root,boolean storage){
        dashboardView=root;dashboardStorage=storage;
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){}
            public void onViewDetachedFromWindow(View v){
                if(dashboardView==v){dashboardView=null;overviewHandler.removeCallbacks(refreshVisibleOverview);overviewDirty=false;if(preparingPhone==null)cancelOverview();}
                v.removeOnAttachStateChangeListener(this);
            }
        });
        if(!renderingOverview)root.post(()->{if(dashboardView==root&&root.isAttachedToWindow()&&preparingPhone==null)beginOverview(false);});
    }
    private void renderOverviewDashboard(){
        if(dashboardView==null||!dashboardView.isAttachedToWindow())return;
        if(!dashboardStorage&&dashboardView instanceof HomeDashboard){((HomeDashboard)dashboardView).refresh();return;}
        int y=overviewScroll(dashboardView);boolean storage=dashboardStorage;
        renderingOverview=true;try{setContentView(storage?storageScreen():home());}finally{renderingOverview=false;}
        final View next=dashboardView;if(next!=null)next.post(()->{if(dashboardView==next)restoreOverviewScroll(next,y);});
    }
    private int overviewScroll(View v){
        if(v instanceof ScrollView)return v.getScrollY();
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){int y=overviewScroll(g.getChildAt(i));if(y>0)return y;}}return 0;
    }
    private boolean restoreOverviewScroll(View v,int y){
        if(v instanceof ScrollView){v.scrollTo(0,y);return true;}
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(restoreOverviewScroll(g.getChildAt(i),y))return true;}return false;
    }
    private DeviceOverview.Snapshot overview(){return deviceOverview==null?new DeviceOverview.Snapshot():deviceOverview;}
    @Override public void onCreate(Bundle state) {
        setTheme(R.style.ClenovaTheme);
        super.onCreate(state);
        prefs=new AppPreferences(this);
        agentChats=new AgentChatStore(this);activeChat=agentChats.current();
        protectedFolders=new ProtectedFolders(this);
        protectedFiles=new ProtectedFiles(this);
        history=new ActivityHistory(this);
        devicePermissions=new DevicePermissions(this);
        if(state!=null){toolsVisible=state.getBoolean("tools_visible");toolsScrollY=state.getInt("tools_scroll_y");agentVisible=state.getBoolean("agent_visible");agentShortcut=state.getBoolean("agent_shortcut");largeFilesMode=state.getBoolean("large_files_mode");clutterMode=state.getBoolean("clutter_mode");dupTrashPending=state.getBoolean("dup_trash_pending");cleanVisible=state.getBoolean("clean_visible");cleanScrollY=state.getInt("clean_scroll_y");historyFilter=state.getInt("history_filter");historyDetailId=state.getString("history_detail_id");historyScrollY=state.getInt("history_scroll_y");}
        applyPalette();
        density = getResources().getDisplayMetrics().density;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(dark||android.os.Build.VERSION.SDK_INT<27?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        int height = getResources().getConfiguration().screenHeightDp;
        compact = height < 740 ? .88f : 1f;
        if(state!=null){cleanupReceipt=CleanupReceipt.restore(state.getBundle("cleanup_receipt"));unusedUninstallPending=state.getBoolean("unused_uninstall_pending");
            pendingUninstallPackage=state.getString("uninstall_pkg","");pendingUninstallName=state.getString("uninstall_name","");pendingUninstallId=state.getString("uninstall_id","");
            if(dupTrashPending||unusedUninstallPending||state.getBoolean("media_verification_pending"))agentTaskOrigin=state.getBoolean("cleanup_agent_origin");}
        if(state!=null&&state.getBoolean("media_verification_pending")){finishMediaCleanup(state.getBoolean("media_verification_approved"));return;}
        if(dupTrashPending||unusedUninstallPending){LinearLayout root=scanShell("Waiting for Android",()->{}),body=scanBody(root);add(body,centered("Waiting for your approval",20,INK,600),-1,-2);setContentView(root);return;}
        if(state!=null&&state.getBoolean("preparing_phone_visible")){showPreparingPhone();return;}
        if(state!=null&&state.getBoolean("onboarding_agent_visible")){showAgentSetup(state.getBoolean("agent_setup_from_tab"),state.getBoolean("agent_setup_confirmed"));return;}
        if(state!=null&&state.getBoolean("onboarding_access_visible")){showOnboardingAccess(state);return;}
        if(state!=null&&state.getBoolean("onboarding_autopilot_visible")){showOnboardingAutopilot();onboardingAutopilot.restoreStoryPosition(state.getFloat("onboarding_story_position",0f));return;}
        if(state!=null&&state.getBoolean("onboarding_unified_visible")){showOnboardingUnified();onboardingUnified.restoreStoryPosition(state.getFloat("onboarding_story_position",0f));return;}
        if(state!=null&&state.getBoolean("onboarding_intro_visible")){showOnboardingIntro();onboardingIntro.restoreStoryPosition(state.getFloat("onboarding_story_position",0f));return;}
        if(state!=null&&state.getBoolean("tool_agent_pending")&&activeChat!=null){agentPermissionPending=true;agentScanTask=state.getInt("tool_agent_task");showAgent();return;}
        if(state!=null&&state.getBoolean("tool_scan_pending")){
            showClean();requestingScan=true;agentTaskOrigin=state.getBoolean("tool_agent_origin");
            int route=state.getInt("tool_scan_route");dupAfterScan=route==1;videoAfterScan=route==2;similarAfterScan=route==3;shotAfterScan=route==4;return;
        }
        if(state!=null && state.getBoolean("about_visible")){aboutScrollY=state.getInt("about_scroll_y");openAbout();return;}
        if(state!=null && state.getBoolean("terms_visible")){policyScrollY=state.getInt("policy_scroll_y");openTerms();return;}
        if(state!=null && state.getBoolean("policy_visible")){policyScrollY=state.getInt("policy_scroll_y");openPrivacyPolicy();return;}
        if(state!=null && state.getBoolean("feedback_visible")){feedbackCategory=FeedbackDraft.category(state.getInt("feedback_category"));feedbackText=state.getString("feedback_text","");feedbackAppInfo=state.getBoolean("feedback_app_info",true);openFeedback();return;}
        if(state!=null && state.getBoolean("help_visible")){helpQuery=state.getString("help_query","");expandedFaq=state.getInt("expanded_faq",-1);openHelpSupport();return;}
        if(state!=null && state.getBoolean("privacy_visible")){openPrivacyData();return;}
        if(state!=null && state.getBoolean("permissions_visible")){openAppPermissions();return;}
        if(state!=null && state.getBoolean("history_visible")){if(historyDetailId!=null){openHistoryDetails(historyDetailId);}else openHistory();return;}
        if(state!=null && state.getBoolean("files_visible")){openProtectedFiles();return;}
        if(state!=null && state.getBoolean("folders_visible")){openProtectedFolders();return;}
        if(state!=null && state.getBoolean("protection_visible")){openProtection();return;}
        if(state!=null && state.getBoolean("agent_prefs_visible")){openAgentPreferences();return;}
        if(state!=null && state.getBoolean("settings_visible")){openSettings();if(state.getBoolean("rating_open")){ratingStars=Math.max(0,Math.min(5,state.getInt("rating_stars")));showRatingSheet();}return;}
        if(state!=null && state.getBoolean("storage_visible")){openStorage();return;}
        if(toolsVisible){showTools();return;}
        if(agentVisible){showAgent();return;}
        if(cleanVisible){showClean();return;}
        if(state==null){
            getWindow().setNavigationBarColor(LaunchSplashView.BACKGROUND);
            if(android.os.Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            launchSplash=new LaunchSplashView(this);setContentView(launchSplash);
        }else setContentView(home());
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putBoolean("onboarding_intro_visible",onboardingIntro!=null||(launchSplash!=null&&!prefs.onboardingComplete()));
        state.putBoolean("onboarding_unified_visible",onboardingUnified!=null);
        state.putBoolean("onboarding_autopilot_visible",onboardingAutopilot!=null);
        state.putBoolean("preparing_phone_visible",preparingPhone!=null);
        state.putBoolean("onboarding_agent_visible",onboardingAgent!=null);
        if(onboardingAgent!=null){state.putBoolean("agent_setup_from_tab",agentSetupFromTab);state.putBoolean("agent_setup_confirmed",onboardingAgent.confirmed());}
        state.putBoolean("onboarding_access_visible",onboardingAccess!=null);
        if(onboardingAccess!=null)onboardingAccess.save(state);
        if(onboardingIntro!=null)state.putFloat("onboarding_story_position",onboardingIntro.storyPosition());
        else if(onboardingUnified!=null)state.putFloat("onboarding_story_position",onboardingUnified.storyPosition());
        else if(onboardingAutopilot!=null)state.putFloat("onboarding_story_position",onboardingAutopilot.storyPosition());
        state.putBoolean("storage_visible",storageVisible);
        state.putBoolean("tools_visible",toolsVisible);
        state.putInt("tools_scroll_y",toolsScroll==null?toolsScrollY:toolsScroll.getScrollY());
        state.putBoolean("clean_visible",cleanVisible);state.putBoolean("agent_visible",agentVisible);state.putBoolean("agent_shortcut",agentShortcut);
        state.putBoolean("large_files_mode",largeFilesMode);state.putBoolean("clutter_mode",clutterMode);
        state.putBoolean("dup_trash_pending",dupTrashPending);
        if(cleanupReceipt!=null)state.putBundle("cleanup_receipt",cleanupReceipt.save());
        state.putBoolean("media_verification_pending",mediaVerificationPending);state.putBoolean("media_verification_approved",mediaVerificationApproved);
        state.putBoolean("unused_uninstall_pending",unusedUninstallPending);
        state.putString("uninstall_pkg",pendingUninstallPackage);state.putString("uninstall_name",pendingUninstallName);state.putString("uninstall_id",pendingUninstallId);state.putBoolean("cleanup_agent_origin",agentTaskOrigin);
        state.putBoolean("tool_scan_pending",requestingScan);state.putBoolean("tool_agent_pending",agentPermissionPending);
        state.putInt("tool_scan_route",videoAfterScan?2:dupAfterScan?1:similarAfterScan?3:shotAfterScan?4:0);
        state.putInt("tool_agent_task",agentScanTask);state.putBoolean("tool_agent_origin",agentTaskOrigin);
        state.putInt("clean_scroll_y",cleanScroll==null?cleanScrollY:cleanScroll.getScrollY());
        state.putBoolean("settings_visible",settingsVisible);
        state.putBoolean("rating_open",ratingDialog!=null&&ratingDialog.isShowing());
        state.putInt("rating_stars",ratingStars);
        state.putBoolean("agent_prefs_visible",agentPrefsVisible);
        state.putBoolean("protection_visible",protectionVisible);
        state.putBoolean("folders_visible",foldersVisible);
        state.putBoolean("files_visible",filesVisible);
        state.putBoolean("history_visible",historyVisible);
        state.putBoolean("permissions_visible",permissionsVisible);
        state.putBoolean("privacy_visible",privacyVisible);
        state.putBoolean("help_visible",helpVisible);
        state.putBoolean("policy_visible",policyVisible);
        state.putBoolean("terms_visible",termsVisible);
        state.putBoolean("about_visible",aboutVisible);
        if(aboutVisible)state.putInt("about_scroll_y",aboutScroll==null?aboutScrollY:aboutScroll.getScrollY());
        if(policyVisible||termsVisible)state.putInt("policy_scroll_y",policyScroll==null?policyScrollY:policyScroll.getScrollY());
        if(feedbackVisible){state.putBoolean("feedback_visible",true);state.putInt("feedback_category",feedbackCategory);state.putString("feedback_text",feedbackText);state.putBoolean("feedback_app_info",feedbackAppInfo);}
        state.putString("help_query",helpQuery);
        state.putInt("expanded_faq",expandedFaq);
        state.putInt("history_filter",historyFilter);
        state.putString("history_detail_id",historyDetailId);
        state.putInt("history_scroll_y",historyDetailId==null&&historyScroll!=null?historyScroll.getScrollY():historyScrollY);

    }
    @Override protected void onStop(){
        super.onStop();nextThumbGeneration();dupCache.evictAll();overviewHandler.removeCallbacks(finishPreparation);cancelOverview();deviceOverview=null;preparationReady=false;agentForeground=false;agentSessionHandler.removeCallbacks(agentExpiry);agentLiveResult=null;agentResultChat="";
        if(agentScan!=null){agentScan.cancel();agentScan=null;appendAgent(false,"Analysis interrupted. Start a fresh review when you return.",-1);}
        if(agentCloudBusy){agentCloudBusy=false;agentGeneration++;cancelRelayRequest();appendAgent(false,"Request interrupted. Please send it again.",-1);}
        stopRealVideo();
        if(verifyingCleanup||completionVisible)return;
        if(unusedVisible){if(unusedExternal||unusedUninstallPending)return;showClean();}
        if(downloadsVisible){if(downloadsBusy){if(downloadsSignal!=null)downloadsSignal.cancel();return;}if(downloadsPicking||downloadsOpening)return;showClean();}
        // Do not retain visible media results across backgrounding; selected-photo access may change.
        if(dupTrashPending||verifyingCleanup||requestingScan||agentPermissionPending)return;
        if(runningScan!=null){runningScan.cancel();runningScan=null;scanVisible=false;scanReviewVisible=false;scanResult=null;showClean();}
        else if(scanResult!=null){scanResult=null;if(cleanVisible&&!settingsVisible)showClean();}
    }
    @Override protected void onDestroy(){
        splashHandler.removeCallbacks(finishSplash);launchSplash=null;
        overviewForeground=false;stopOverviewObserver();overviewHandler.removeCallbacks(finishPreparation);cancelOverview();preparingPhone=null;dashboardView=null;deviceOverview=null;
        if(onboardingIntro!=null){onboardingIntro.setActive(false);onboardingIntro=null;}
        if(onboardingUnified!=null){onboardingUnified.setActive(false);onboardingUnified=null;}
        if(onboardingAutopilot!=null){onboardingAutopilot.setActive(false);onboardingAutopilot=null;}
        onboardingAccess=null;
        if(onboardingAgent!=null){onboardingAgent.setActive(false);onboardingAgent=null;}
        agentSessionHandler.removeCallbacks(agentExpiry);
        if(agentScan!=null){agentScan.cancel();agentScan=null;}agentGeneration++;cancelRelayRequest();relayWorker.shutdownNow();if(agentDrawer!=null)agentDrawer.dismiss();relayToken="";
        if(runningScan!=null){runningScan.cancel();runningScan=null;}
        if(unusedSignal!=null)unusedSignal.cancel();unusedGeneration++;if(unusedSheet!=null)unusedSheet.dismiss();
        if(downloadsSignal!=null)downloadsSignal.cancel();downloadsGeneration++;if(downloadSheet!=null)downloadSheet.dismiss();
        stopRealVideo();dupThumbs.shutdownNow();dupCache.evictAll();
        if(preferencePopup!=null)preferencePopup.dismiss();
        if(ratingDialog!=null)ratingDialog.dismiss();
        super.onDestroy();
    }
    private void applyPalette(){
        dark=UiKit.dark(this,prefs.appearance());
        UiPolish.Palette palette=UiPolish.palette(dark);
        BG=palette.background;WHITE=palette.card;INK=palette.ink;GRAY=palette.muted;
        BLUE=palette.accent;PALE=palette.tint;GREEN=palette.success;
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(dark||android.os.Build.VERSION.SDK_INT<27?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private int surface(int c){
        if(!dark)return c;
        switch(c){
            case 0xffeeeef0:case 0xffe6e6eb:case 0xffe5e5ea:case 0xffdfe2e8:case 0xffdcdce1:return 0xff38383c;
            case 0xffe7e7ec:case 0xfff0f0f3:case 0xffe4e8ee:return 0xff343438;
            case 0xfff0f4fa:return 0xff29394a;
            case 0xffe8f6ec:case 0xffeaf7ee:case 0xffe5f5e9:return 0xff203d2b;
            case 0xffc7c7cc:return 0xff72727a;
            default:return c;
        }
    }
    private String tr(String value){return AppStrings.translate(prefs.language(),value);}
    private String gbNumber(double mb){return ReviewPlan.gb(mb).replace(" GB","");}

    private int dp(float n) { return Math.round(n * density); }
    private int v(float n) { return dp(n * compact); }
    private GradientDrawable round(int color, float radius) {
        return UiKit.shape(this,surface(color),radius);
    }
    private void tap(View view, int color, float radius, Runnable action) {
        view.setBackground(UiKit.ripple(this,surface(UiPolish.actionFill(color)),radius,dark));
        view.setClickable(true); view.setFocusable(true);
        view.setOnClickListener(w -> {
            w.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            action.run();
        });
    }
    private TextView txt(String s, float size, int color, int weight) {
        TextView t = new TextView(this);
        t.setText(tr(s));UiKit.typography(t,size,color,weight);return t;
    }
    private TextView centered(String s, float size, int color, int weight) {
        TextView t = txt(s,size,color,weight); t.setGravity(Gravity.CENTER); return t;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private void add(LinearLayout l, View child, int width, int height) {
        l.addView(child, new LinearLayout.LayoutParams(width,height));
    }
    private void weight(LinearLayout l, View child) {
        l.addView(child,new LinearLayout.LayoutParams(0,-2,1f));
    }
    private void space(LinearLayout l,float size) { add(l,new View(this),1,v(size)); }
    private void gap(LinearLayout l,float size) { add(l,new View(this),dp(size),1); }
    private void stretch(LinearLayout l) { l.addView(new View(this),new LinearLayout.LayoutParams(0,1,1f)); }
    private void pad(View w,int h,int vert) { w.setPadding(dp(h),v(vert),dp(h),v(vert)); }
    private LinearLayout card() {
        LinearLayout l=column();l.setBackground(UiKit.card(this,dark));pad(l,UiPolish.CARD_PAD_X,UiPolish.CARD_PAD_Y);return l;
    }
    private LinearLayout home() {
        HomeDashboard root=new HomeDashboard();attachOverviewDashboard(root,false);return root;
    }
    /** Dashboard views stay attached during metadata updates, preserving touch targets and scroll. */
    private final class HomeDashboard extends LinearLayout {
        private final TextView available,used,total,storageStatus,agentMode,agentStatus,mediaSummary,agentAction;
        private final android.widget.ProgressBar usage;
        HomeDashboard(){
            super(MainActivity.this);setOrientation(VERTICAL);setBackgroundColor(BG);
            int gutter=dp(UiKit.gutter(MainActivity.this));
            setPadding(gutter,v(10),gutter,v(8));
            add(this,header(),-1,dp(44));space(this,8);
            ScrollView scroll=new ScrollView(MainActivity.this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout body=column();body.setPadding(0,v(2),0,v(8));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
            addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
            add(body,txt("Your storage, at a glance.",22,INK,700),-1,-2);space(body,9);
            LinearLayout storageCard=card();
            boolean wide=HomePresentation.columns(getResources().getConfiguration().screenWidthDp,getResources().getConfiguration().fontScale)==2;
            LinearLayout details=row();details.setGravity(Gravity.CENTER_VERTICAL);details.setMinimumHeight(dp(48));details.setPadding(dp(4),0,dp(2),0);
            weight(details,txt("Storage details",13,BLUE,600));gap(details,4);add(details,new Glyph(MainActivity.this,"chevron",BLUE),dp(14),dp(14));
            tap(details,WHITE,12,MainActivity.this::openStorage);details.setContentDescription(tr("Storage details"));
            if(wide){
                LinearLayout top=row();weight(top,txt("Available storage",13,GRAY,500));gap(top,6);
                details.setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
                // Wrap the link without an internal weighted child in a wrap-content parent.
                details.getChildAt(0).setLayoutParams(new LinearLayout.LayoutParams(-2,-2));
                add(top,details,-2,-2);add(storageCard,top,-1,-2);
            }else add(storageCard,txt("Available storage",13,GRAY,500),-1,-2);
            available=txt("Checking…",32,INK,700);add(storageCard,available,-1,-2);space(storageCard,12);
            usage=storageProgress();add(storageCard,usage,-1,dp(10));space(storageCard,7);
            LinearLayout figures=row();used=txt("",13,INK,600);total=txt("",12,GRAY,400);weight(figures,used);gap(figures,6);total.setGravity(Gravity.END);weight(figures,total);add(storageCard,figures,-1,-2);
            storageStatus=txt("",12,GRAY,400);space(storageCard,8);add(storageCard,storageStatus,-1,-2);
            if(!wide){space(storageCard,4);add(storageCard,details,-1,-2);}
            add(body,storageCard,-1,-2);space(body,10);
            homeAction(body,"Smart Scan",true,()->{showClean();requestSmartScan();});space(body,16);
            add(body,txt("Agent update",20,INK,700),-1,-2);space(body,8);
            LinearLayout agent=card(),line=row();
            FrameLayout icon=new FrameLayout(MainActivity.this);icon.setBackground(round(PALE,12));icon.addView(new Glyph(MainActivity.this,"bot",BLUE),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
            add(line,icon,dp(40),dp(40));gap(line,11);
            LinearLayout copy=column();agentMode=txt("",16,INK,600);add(copy,agentMode,-1,-2);space(copy,4);agentStatus=txt("",13,GRAY,400);add(copy,agentStatus,-1,-2);weight(line,copy);add(agent,line,-1,-2);
            mediaSummary=txt("",12,GRAY,400);space(agent,8);add(agent,mediaSummary,-1,-2);space(agent,10);
            agentAction=homeAction(agent,"Open Agent",false,()->{if(prefs.autopilot())showAgent();else showAgentSetup(true,false);});
            add(body,agent,-1,-2);space(body,16);
            add(body,txt("Quick actions",20,INK,700),-1,-2);space(body,8);
            String[] labels={"Duplicate photos","Large videos","Large files","Unused apps"};
            String[] icons={"images","video","folder","apps"};
            Runnable[] actions={()->{showClean();openRealDuplicates(false);},()->{showClean();openRealVideos(false);},()->{showClean();openLargeFiles();},()->{showClean();openUnusedApps();}};
            int columns=HomePresentation.columns(getResources().getConfiguration().screenWidthDp,getResources().getConfiguration().fontScale);
            for(int i=0;i<labels.length;i+=columns){
                LinearLayout group=row();group.setGravity(Gravity.TOP);
                for(int j=i;j<Math.min(i+columns,labels.length);j++){
                    if(j>i)gap(group,8);
                    LinearLayout tile=homeShortcut(labels[j],icons[j],actions[j]);
                    group.addView(tile,new LinearLayout.LayoutParams(0,-2,1f));
                }
                add(body,group,-1,-2);space(body,8);
            }
            LinearLayout privacy=row();privacy.setGravity(Gravity.CENTER_VERTICAL);add(privacy,new Glyph(MainActivity.this,"shield",GREEN),dp(16),dp(16));gap(privacy,7);
            weight(privacy,txt("Always in your control.",12,GRAY,400));add(body,privacy,-1,-2);
            space(this,6);add(this,navigation(),-1,dp(64));refresh();
        }
        void refresh(){
            DeviceOverview.Snapshot data=overview();
            boolean known=data.storageKnown();
            available.setText(known?DeviceOverview.format(data.free):tr(data.finished?"Unavailable":"Checking…"));
            usage.setVisibility(known?VISIBLE:GONE);
            if(known)usage.setProgress(HomePresentation.progress(data.total,data.free));
            used.setText(known?tr(DeviceOverview.format(data.used())+" used · "+data.usedPercent()+"%"):"");
            total.setText(known?tr(DeviceOverview.format(data.total)+" total"):"");
            used.setVisibility(known?VISIBLE:GONE);total.setVisibility(known?VISIBLE:GONE);
            storageStatus.setText(tr(HomePresentation.status(known,data.finished,data.partial())));
            boolean configured=prefs.autopilot();
            agentMode.setText(tr(configured?"Agent configured":"Manual mode"));
            agentStatus.setText(tr(configured?AutopilotScheduler.status(MainActivity.this,prefs):"Review your phone when you choose."));
            mediaSummary.setText(tr(!data.finished?"Reading available media…":(data.photos.available?(data.photos.partial?"At least ":"")+data.photos.count+" photos":"Photos unavailable")+" · "+(data.videos.available?(data.videos.partial?"At least ":"")+data.videos.count+" videos":"Videos unavailable")));
            agentAction.setText(tr(configured?"Open Agent":"Activate Agent"));
            agentAction.setContentDescription(tr(configured?"Open Agent":"Activate Agent"));
        }
    }
    private android.widget.ProgressBar storageProgress(){
        android.widget.ProgressBar bar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(HomePresentation.BAR_MAX);bar.setProgressTintList(ColorStateList.valueOf(BLUE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(UiPolish.palette(dark).divider));
        bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return bar;
    }
    private TextView homeAction(LinearLayout parent,String label,boolean primary,Runnable action){
        android.widget.Button button=new android.widget.Button(this);button.setText(tr(label));button.setAllCaps(false);button.setLetterSpacing(0);button.setStateListAnimator(null);
        UiKit.typography(button,15,primary?Color.WHITE:BLUE,600);button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(primary?52:48));button.setMinimumHeight(dp(primary?52:48));button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(dp(14),dp(10),dp(14),dp(10));
        tap(button,primary?BLUE:PALE,UiPolish.BUTTON_RADIUS,action);add(parent,button,-1,-2);return button;
    }
    private LinearLayout homeShortcut(String label,String glyph,Runnable action){
        LinearLayout tile=row();tile.setMinimumHeight(dp(66));tile.setPadding(dp(10),dp(10),dp(10),dp(10));
        FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,10));icon.addView(new Glyph(this,glyph,BLUE),new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER));
        add(tile,icon,dp(32),dp(32));gap(tile,8);weight(tile,txt(label,13,INK,600));
        tap(tile,WHITE,UiPolish.CARD_RADIUS,action);tile.setContentDescription(tr(label));return tile;
    }
    private void returnToDashboard(){
        if(toolsVisible)showTools();else if(agentVisible)showAgent();else if(cleanVisible)showClean();else showHome();
    }
    private void restoreAgentWindow(){
        if(agentInput!=null){
            android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(ime!=null)ime.hideSoftInputFromWindow(agentInput.getWindowToken(),0);
            agentInput=null;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
    }
    private boolean agentBusy(){return agentScan!=null||agentCloudBusy||agentPermissionPending;}
    private boolean expireAgentSession(){
        if(activeChat==null||agentChats==null||!AgentSessionPolicy.expired(activeChat.lastActivity,System.currentTimeMillis()))return false;
        clearAgentSession();return true;
    }
    private void clearAgentSession(){
        agentSessionHandler.removeCallbacks(agentExpiry);
        if(agentScan!=null){agentScan.cancel();agentScan=null;}agentCloudBusy=false;agentGeneration++;cancelRelayRequest();agentPermissionPending=false;
        agentLiveResult=null;agentResultChat="";if(agentVisible&&!agentTaskOrigin)scanResult=null;agentTaskVisible=false;
        if(agentChats!=null&&!agentChats.clear())folderToast("Could not clear saved conversation");
        activeChat=null;
    }
    private void scheduleAgentExpiry(){
        agentSessionHandler.removeCallbacks(agentExpiry);
        if(agentForeground&&activeChat!=null)agentSessionHandler.postDelayed(agentExpiry,Math.max(1,AgentSessionPolicy.remaining(activeChat.lastActivity,System.currentTimeMillis())));
    }
    private void appendAgent(boolean user,String message,int action){
        expireAgentSession();
        if(activeChat==null){if(!user)return;activeChat=agentChats.create();}
        if(!agentChats.add(activeChat,user,message,action))folderToast("Could not save temporary conversation");
        scheduleAgentExpiry();
    }
    private void showAgent(){
        toolsVisible=false;toolsScroll=null;toolsScrollY=0;
        expireAgentSession();scheduleAgentExpiry();
        if(runningScan!=null){runningScan.cancel();runningScan=null;}
        if(downloadsSignal!=null){downloadsSignal.cancel();downloadsSignal=null;}downloadsGeneration++;
        if(unusedSignal!=null){unusedSignal.cancel();unusedSignal=null;}unusedGeneration++;
        downloadsVisible=false;downloadsReview=false;downloadItems.clear();downloadSelected.clear();downloadKeep.clear();
        unusedVisible=false;unusedReview=false;unusedResult=null;unusedSelected.clear();unusedKeep.clear();
        stopRealVideo();realShotVisible=false;realShotReview=false;realShotPreview=false;realVideoVisible=false;realVideoReview=false;realVideoPreview=false;
        realSimilarVisible=false;realSimilarReview=false;realDupVisible=false;realDupReview=false;scanVisible=false;scanReviewVisible=false;
        dupAfterScan=false;similarAfterScan=false;videoAfterScan=false;shotAfterScan=false;nextThumbGeneration();
        settingsVisible=false;agentPrefsVisible=false;historyVisible=false;protectionVisible=false;filesVisible=false;foldersVisible=false;permissionsVisible=false;privacyVisible=false;helpVisible=false;feedbackVisible=false;aboutVisible=false;termsVisible=false;policyVisible=false;
        storageVisible=false;cleanVisible=false;agentVisible=true;agentTaskVisible=activeChat!=null;agentTaskOrigin=false;agentShortcut=false;cleanScroll=null;
        restoreAgentWindow();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setStatusBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility((dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)|(dark||android.os.Build.VERSION.SDK_INT<27?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(10),dp(UiKit.gutter(this)),v(10));
        LinearLayout header=row();add(header,agentHeaderButton("agent_menu","Agent menu",this::showAgentDrawer),dp(42),dp(42));gap(header,10);weight(header,txt("Agent",21,INK,700));add(header,agentHeaderButton("new_chat","New conversation",this::newAgentChat),dp(42),dp(42));add(root,header,-1,-2);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(12),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-1));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        if(activeChat==null){body.setGravity(Gravity.CENTER_VERTICAL);showAgentWelcome(body);}else{renderAgentConversation(body);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
        space(root,8);addAgentComposer(root);
        if(activeChat==null){space(root,8);add(root,navigation(),-1,dp(64));}
        root.setFocusableInTouchMode(true);root.requestFocus();setContentView(root);
        if(agentTransition){agentTransition=false;if(ValueAnimator.areAnimatorsEnabled()){body.setAlpha(0);body.animate().alpha(1).setDuration(200).start();}}
    }
    private void showAgentWelcome(LinearLayout body){
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);add(hero,new AgentOrb(this),dp(146),dp(124));space(hero,8);
        add(hero,centered("How can I help?",26,INK,700),-1,-2);space(hero,8);LinearLayout ready=row();ready.setGravity(Gravity.CENTER);View dot=new View(this);dot.setBackground(round(GREEN,6));add(ready,dot,dp(6),dp(6));gap(ready,6);add(ready,txt("Ready for you",12,GRAY,500),-2,-2);add(hero,ready,-1,-2);add(body,hero,-1,-2);space(body,25);
        if(!prefs.autopilot()){
            LinearLayout setup=card();add(setup,txt("Make your Agent yours",15,INK,600),-1,-2);space(setup,6);
            add(setup,txt("Configure your Agent, or keep using manual requests.",12,GRAY,400),-1,-2);space(setup,10);
            scanButton(setup,"Activate Agent",true,()->showAgentSetup(true,false));add(body,setup,-1,-2);space(body,16);
        }
        add(body,txt("Suggested for your phone",12,GRAY,500),-1,-2);space(body,9);
        long free=-1,total=-1;try{android.os.StatFs stat=new android.os.StatFs(getFilesDir().getAbsolutePath());free=stat.getAvailableBytes();total=stat.getTotalBytes();}catch(RuntimeException ignored){}
        boolean low=free>=0&&total>0&&free/(double)total<0.15;
        String spaceTitle=low?"Free up storage":"Find space to recover";
        if(scanResult!=null&&SmartMediaScan.hasAccess(this)&&scanResult.total()>0)spaceTitle=tr("Review up to")+" "+scanBytes(scanResult.total());
        String dupTitle=scanResult!=null&&SmartMediaScan.hasPhotoAccess(this)&&scanResult.imagesScanned&&scanResult.groups.get(0).size()>0?tr("Review")+" "+scanResult.groups.get(0).size()+" "+tr("duplicate photos"):"Review my duplicate photos";
        add(body,agentSuggestion("sparkles",spaceTitle,0),-1,dp(54));space(body,8);
        add(body,agentSuggestion("images",dupTitle,1),-1,dp(54));space(body,8);
        add(body,agentSuggestion("apps",UnusedAppScan.allowed(this)?"Find apps I rarely use":"Review unused apps",2),-1,dp(54));
        if(prefs.autopilot()){space(body,12);LinearLayout background=card();add(background,txt("Autopilot",14,INK,600),-1,-2);space(background,5);add(background,txt(AutopilotScheduler.status(this,prefs),12,GRAY,400),-1,-2);String breakdown=AutopilotScheduler.breakdown(this,prefs);if(!breakdown.isEmpty()){space(background,5);add(background,txt(breakdown,12,INK,500),-1,-2);}space(background,8);scanButton(background,"Review current media",false,()->openAgentTask(0,"Review current media"));add(body,background,-1,-2);space(body,12);}
    }
    private void addAgentComposer(LinearLayout root){
        LinearLayout composer=row();composer.setBackground(round(WHITE,30));pad(composer,11,5);
        android.widget.EditText input=new android.widget.EditText(this);agentInput=input;input.setSingleLine(true);input.setTextSize(15);input.setTextColor(INK);input.setHintTextColor(GRAY);input.setHint(tr(agentBusy()?"Working…":"Ask Agent..."));input.setEnabled(!agentBusy());input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(dp(5),0,dp(5),0);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(600)});input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);composer.addView(input,new LinearLayout.LayoutParams(0,dp(44),1f));gap(composer,6);
        FrameLayout send=new FrameLayout(this);send.addView(new Glyph(this,"arrow_up",Color.WHITE),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));send.setContentDescription(tr("Send"));tap(send,BLUE,24,()->submitAgentCommand(input.getText().toString()));send.setEnabled(false);send.setAlpha(.45f);
        input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence q,int st,int c,int a){}public void onTextChanged(CharSequence q,int st,int before,int count){boolean enabled=!agentBusy()&&!q.toString().trim().isEmpty();send.setEnabled(enabled);send.setAlpha(enabled?1f:.45f);}public void afterTextChanged(android.text.Editable e){}});
        input.setOnEditorActionListener((v,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_SEND){submitAgentCommand(input.getText().toString());return true;}return false;});add(composer,send,dp(42),dp(42));add(root,composer,-1,-2);
    }
    private FrameLayout agentHeaderButton(String glyph,String label,Runnable action){
        FrameLayout frame=new FrameLayout(this);frame.addView(new Glyph(this,glyph,INK),new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER));frame.setContentDescription(tr(label));tap(frame,WHITE,23,action);return frame;
    }
    private LinearLayout agentSuggestion(String icon,String title,int task){
        LinearLayout line=row();pad(line,14,8);add(line,new Glyph(this,icon,BLUE),dp(22),dp(22));gap(line,12);TextView label=txt(title,14,INK,500);label.setMaxLines(2);weight(line,label);gap(line,6);add(line,new Glyph(this,"up",GRAY),dp(15),dp(15));tap(line,WHITE,16,()->openAgentTask(task,title));line.setContentDescription(tr(title));return line;
    }
    private void renderAgentConversation(LinearLayout body){
        int n=activeChat.messages.size();
        for(int i=0;i<n;i++){AgentChatStore.Message message=activeChat.messages.get(i);space(body,9);
            if(message.user){LinearLayout line=row();line.setGravity(Gravity.END);TextView bubble=txt(message.text,15,INK,500);bubble.setBackground(round(PALE,20));pad(bubble,14,12);bubble.setMaxWidth(dp(300));bubble.setTextIsSelectable(true);add(line,bubble,-2,-2);add(body,line,-1,-2);}
            else{LinearLayout label=row();add(label,new AgentOrb(this),dp(30),dp(28));gap(label,7);add(label,txt("Agent",13,INK,600),-2,-2);add(body,label,-1,-2);space(body,6);TextView reply=txt(message.text,15,INK,400);reply.setLineSpacing(dp(3),1f);reply.setTextIsSelectable(true);add(body,reply,-1,-2);
                if(i==n-1&&message.action>=0&&!agentBusy()){space(body,10);LinearLayout action=card();LinearLayout line=row();add(line,new Glyph(this,"sparkles",BLUE),dp(24),dp(24));gap(line,9);weight(line,txt(AgentActions.TITLES[message.action],15,INK,600));add(action,line,-1,-2);space(action,10);scanButton(action,AgentActions.BUTTONS[message.action],true,()->startAgentTask(message.action));add(body,action,-1,-2);}
            }space(body,9);
        }
        if(agentScan!=null||agentCloudBusy){LinearLayout work=card();agentProgressText=txt(agentScan!=null?agentStage:"Thinking…",14,INK,600);add(work,agentProgressText,-1,-2);space(work,9);agentProgressBar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);agentProgressBar.setIndeterminate(true);agentProgressBar.setProgressTintList(ColorStateList.valueOf(BLUE));agentProgressBar.setIndeterminateTintList(ColorStateList.valueOf(BLUE));add(work,agentProgressBar,-1,dp(6));space(work,10);scanButton(work,"Cancel",false,this::cancelAgentWork);add(body,work,-1,-2);}
        if(agentLiveResult!=null&&activeChat.id.equals(agentResultChat)&&!agentBusy()&&SmartMediaScan.hasAccess(this)){space(body,12);renderAgentResults(body);}
    }
    private void renderAgentResults(LinearLayout body){
        SmartMediaScan.Result result=agentLiveResult;LinearLayout card=card();add(card,txt("Potential space to recover",12,GRAY,500),-1,-2);space(card,6);add(card,txt(scanBytes(result.total()),30,INK,700),-1,-2);space(card,7);
        add(card,txt(result.partial||result.limited?"Partial scan · Accessible media only":"Accessible media reviewed",12,GRAY,400),-1,-2);space(card,10);
        for(int i=0;i<4;i++){final int category=i;boolean scanned=i==1?result.videosScanned:i==3?result.similarScanned:result.imagesScanned;if(!scanned)continue;thinRule(card);LinearLayout line=row();line.setMinimumHeight(dp(53));add(line,new Glyph(this,new String[]{"copy","video","screenshot","images"}[i],BLUE),dp(22),dp(22));gap(line,10);weight(line,txt(SmartMediaScan.LABELS[i],14,INK,500));add(line,txt(scanBytes(result.bytes(i)),12,GRAY,600),-2,-2);gap(line,7);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));tap(line,WHITE,10,()->openAgentMediaReview(category));add(card,line,-1,-2);}
        space(card,10);scanButton(card,"Review cleanup",true,()->{scanResult=result;leaveAgentForTool();showScanResults();});space(card,7);add(card,txt("Nothing removed. Review each file before approving.",12,GREEN,500),-1,-2);add(body,card,-1,-2);
    }
    private void leaveAgentForTool(){agentLiveResult=null;agentResultChat="";agentVisible=false;agentTaskVisible=false;agentTaskOrigin=true;showClean();}
    private void openAgentMediaReview(int category){if(agentLiveResult==null)return;scanResult=agentLiveResult;leaveAgentForTool();if(category==0)openRealDuplicates(false);else if(category==1)openRealVideos(false);else if(category==2)openRealScreenshots(false);else openRealSimilar(false);}
    private void submitAgentCommand(String text){
        expireAgentSession();if(agentBusy())return;String request=text.trim();if(request.isEmpty())return;int task=AgentActions.route(request);
        if(task>=0){openAgentTask(task,request);return;}
        boolean fresh=activeChat==null;appendAgent(true,request,-1);agentTransition=fresh;agentLiveResult=null;agentResultChat="";
        if(!relayEndpoint.isEmpty()&&!relayToken.isEmpty()){agentCloudBusy=true;final int generation=++agentGeneration;final String endpoint=relayEndpoint,token=relayToken;final org.json.JSONArray context=new org.json.JSONArray();java.util.List<String> prior=new java.util.ArrayList<>();for(int i=activeChat.messages.size()-2;i>=0&&prior.size()<3;i--){AgentChatStore.Message msg=activeChat.messages.get(i);if(msg.user&&msg.text.length()<=600)prior.add(0,msg.text);}for(String item:prior)context.put(item);showAgent();
            relayWorker.allowCoreThreadTimeOut(true);try{relayRequest=relayWorker.submit(()->{AgentRelay.Reply reply=null;try{reply=AgentRelay.ask(endpoint,token,request,context);}catch(Exception ignored){}final AgentRelay.Reply response=reply;runOnUiThread(()->{if(isDestroyed()||generation!=agentGeneration||!agentCloudBusy)return;relayRequest=null;agentCloudBusy=false;appendAgent(false,response==null?"AI connection unavailable. Try again or use a local cleanup request.":response.text,response==null?-1:response.action);if(agentVisible&&agentForeground)showAgent();});});}catch(java.util.concurrent.RejectedExecutionException e){agentCloudBusy=false;appendAgent(false,"AI connection busy. Try again.",-1);showAgent();}return;}
        appendAgent(false,"I can help with phone storage, photos, downloads, unused apps and file protection. Other topics aren't supported. Try a supported cleanup request.",-1);showAgent();
    }
    private void openAgentTask(int task,String request){
        expireAgentSession();if(agentBusy())return;boolean fresh=activeChat==null;appendAgent(true,request,-1);agentLiveResult=null;agentResultChat="";appendAgent(false,AgentActions.DESCRIPTIONS[task]+" Nothing will be removed without your approval.",task);agentTransition=fresh;showAgent();
    }
    private int agentScope(int task){return task==5?ToolAccess.VIDEOS:task==1||task==6||task==7?ToolAccess.PHOTOS:ToolAccess.MEDIA;}
    private boolean agentHasAccess(int task){return ToolAccess.has(this,agentScope(task));}
    private void startAgentTask(int task){
        if(expireAgentSession()){showAgent();return;}if(agentBusy()||activeChat==null)return;
        if(task==0||task==1||task==5||task==6||task==7){
            agentScanTask=task;if(agentHasAccess(task)){beginAgentScan(task);return;}
            final int scope=agentScope(task);
            privacyDialog().setTitle(tr("Media access needed")).setMessage(tr("Clenova only scans media you allow. Nothing is removed."))
                .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr(ToolAccess.action(this,scope)),(d,w)->{
                    if(agentBusy()||activeChat==null)return;agentPermissionPending=true;
                    if(!ToolAccess.launch(this,scope,REQUEST_AGENT_MEDIA)){agentPermissionPending=false;folderToast("Could not open access settings");}
                }).show();return;
        }
        if(task==3){agentTaskVisible=false;agentShortcut=true;agentLiveResult=null;scanResult=null;restoreAgentWindow();openProtection();return;}
        leaveAgentForTool();if(task==2)openUnusedApps();else if(task==4)openDownloads();else if(task==8)openLargeFiles();else if(task==9)openClutter();
    }
    private void beginAgentScan(int task){
        if(agentScan!=null||activeChat==null||!agentHasAccess(task))return;agentLiveResult=null;agentResultChat="";agentStage=tr("Reading media library");
        final int accessAtStart=ToolAccess.mediaMask(this);
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);agentScan=job;final String chat=activeChat.id;showAgent();
        new Thread(()->{try{SmartMediaScan.Progress callback=(stage,done,total)->{if(done!=total&&done%16!=0)return;runOnUiThread(()->{if(agentScan!=job||isDestroyed()||!agentForeground)return;agentStage=tr(new String[]{"Reading media library","Checking exact duplicates","Classifying media","Comparing similar photos"}[stage]);if(agentProgressText!=null)agentProgressText.setText(agentStage+(total>0?" · "+done+" / "+total:""));if(agentProgressBar!=null){agentProgressBar.setIndeterminate(total<=0);if(total>0){agentProgressBar.setMax(total);agentProgressBar.setProgress(done);}}});};
            SmartMediaScan.Result result=task==5?job.runVideos(callback):task==0?job.run(callback):job.runPhotos(callback);
            runOnUiThread(()->{if(agentScan!=job||isDestroyed()||activeChat==null||!activeChat.id.equals(chat))return;agentScan=null;if(accessAtStart!=ToolAccess.mediaMask(this)||!agentHasAccess(task)){appendAgent(false,"Media access changed. No results are available.",-1);showAgent();return;}agentLiveResult=result;agentResultChat=chat;scanResult=result;appendAgent(false,"Analysis finished. I checked "+result.examined+" accessible media items. "+(result.partial||result.limited?"Results are partial. ":"")+"Review the candidates below. Nothing has been removed.",-1);showAgent();});
        }catch(Exception ex){runOnUiThread(()->{if(agentScan!=job||isDestroyed())return;agentScan=null;appendAgent(false,"Analysis could not finish. No files were removed.",-1);showAgent();});}},"Clenova-agent-scan").start();
    }
    private void cancelAgentWork(){if(agentScan!=null){agentScan.cancel();agentScan=null;appendAgent(false,"Analysis cancelled. Nothing was removed.",-1);}if(agentCloudBusy){agentCloudBusy=false;agentGeneration++;cancelRelayRequest();appendAgent(false,"Request cancelled.",-1);}if(agentVisible&&!isDestroyed())showAgent();}
    private void newAgentChat(){if(agentBusy()){folderToast("Cancel the current task first");return;}clearAgentSession();agentTransition=true;showAgent();}
    private void showAgentDrawer(){
        if(agentDrawer!=null)agentDrawer.dismiss();android.app.Dialog dialog=new android.app.Dialog(this);agentDrawer=dialog;dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout layout=column();layout.setBackgroundColor(WHITE);pad(layout,16,16);LinearLayout header=row();weight(header,txt("Agent",25,INK,700));add(header,agentHeaderButton("close","Close",dialog::dismiss),dp(40),dp(40));add(layout,header,-1,-2);space(layout,12);
        scanButton(layout,"New conversation",true,()->{dialog.dismiss();newAgentChat();});space(layout,10);
        scanButton(layout,"Activity history",false,()->{if(agentBusy())return;dialog.dismiss();restoreAgentWindow();agentShortcut=true;historyFilter=0;historyScrollY=0;openHistory();});
        if(!prefs.autopilot())scanButton(layout,"Activate Agent",false,()->{if(agentBusy())return;dialog.dismiss();showAgentSetup(true,false);});
        scanButton(layout,"Agent settings",false,()->{if(agentBusy())return;dialog.dismiss();restoreAgentWindow();agentShortcut=true;openAgentPreferences();});
        scanButton(layout,relayToken.isEmpty()?"Connect AI (private test)":"Disconnect AI",false,()->{if(agentBusy())return;dialog.dismiss();if(!relayToken.isEmpty()){relayEndpoint="";relayToken="";folderToast("AI disconnected");}else configureAgentRelay();});
        space(layout,18);add(layout,txt("Temporary conversation",14,INK,600),-1,-2);space(layout,6);
        add(layout,txt("Your current chat stays on this device and expires after 24 hours of inactivity. Starting a new chat clears it.",13,GRAY,400),-1,-2);
        layout.addView(new View(this),new LinearLayout.LayoutParams(1,0,1f));
        space(layout,8);scanButton(layout,"Back to Home",false,()->{if(agentBusy())return;dialog.dismiss();showHome();});
        dialog.setContentView(layout);dialog.setOnDismissListener(v->{if(agentDrawer==dialog)agentDrawer=null;});android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}dialog.show();if(window!=null)window.setLayout(Math.min(dp(320),(int)(getResources().getDisplayMetrics().widthPixels*.86)), -1);
    }
    private void configureAgentRelay(){
        LinearLayout body=column();pad(body,18,12);add(body,txt("Private developer test only. Your typed request and up to 3 previous typed messages in this conversation will be sent to your HTTPS relay and Groq. No photos, file lists or scan results are sent automatically. Do not enter sensitive information. Connection is cleared when the app restarts.",13,GRAY,400),-1,-2);space(body,10);
        android.widget.EditText endpoint=new android.widget.EditText(this);endpoint.setSingleLine();endpoint.setHint("https://your-relay.example/agent");endpoint.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);add(body,endpoint,-1,-2);
        android.widget.EditText token=new android.widget.EditText(this);token.setSingleLine();token.setHint("Private relay token (not Groq API key)");token.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);add(body,token,-1,-2);
        privacyDialog().setTitle("Connect AI").setView(body).setNegativeButton(tr("Cancel"),null).setPositiveButton("Connect & consent",(d,w)->{String url=endpoint.getText().toString().trim(),secret=token.getText().toString().trim();if(!AgentRelay.validEndpoint(url)||secret.length()<32){folderToast("Valid HTTPS endpoint and private token required");return;}relayEndpoint=url;relayToken=secret;folderToast("AI relay configured. Connection checked on next request.");}).show();
    }
    private void leaveAgentTool(){if(agentTaskOrigin)showAgent();else showClean();}

    private final class AgentOrb extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        AgentOrb(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float scale=Math.min(getWidth()/146f,getHeight()/124f);
            c.save();c.translate((getWidth()-146*scale)/2,(getHeight()-124*scale)/2);c.scale(scale,scale);
            paint.setStyle(Paint.Style.FILL);paint.setColor(dark?0xff182d44:0xffedf5ff);c.drawCircle(73,62,59,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.3f);paint.setColor(dark?0xff31557b:0xffc5ddff);c.drawCircle(73,62,47,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(dark?0xff244365:0xffd8eaff);c.drawCircle(73,62,36,paint);paint.setColor(BLUE);c.drawCircle(73,62,29,paint);
            paint.setColor(Color.WHITE);Path star=new Path();star.moveTo(73,40);star.quadTo(76,58,95,62);star.quadTo(76,66,73,84);star.quadTo(70,66,51,62);star.quadTo(70,58,73,40);star.close();c.drawPath(star,paint);
            paint.setColor(BLUE);c.drawCircle(73,3,3,paint);paint.setColor(0xff80baff);c.drawCircle(118,78,3.2f,paint);c.restore();
        }
    }

    private void showClean(){
        toolsVisible=false;toolsScroll=null;toolsScrollY=0;
        agentVisible=false;agentTaskVisible=false;agentShortcut=false;restoreAgentWindow();
        unusedVisible=false;unusedReview=false;unusedExternal=false;unusedGeneration++;if(unusedSignal!=null){unusedSignal.cancel();unusedSignal=null;}unusedResult=null;unusedSelected.clear();unusedKeep.clear();if(unusedSheet!=null)unusedSheet.dismiss();
        downloadsVisible=false;downloadsReview=false;downloadsGeneration++;if(downloadsSignal!=null){downloadsSignal.cancel();downloadsSignal=null;}if(!downloadsBusy){downloadItems.clear();downloadSelected.clear();downloadKeep.clear();}
        realShotVisible=false;realShotReview=false;realShotPreview=false;shotAfterScan=false;shotSelected.clear();shotKeep.clear();
        stopRealVideo();realVideoVisible=false;realVideoReview=false;realVideoPreview=false;videoAfterScan=false;realVideoSelected.clear();realVideoKeep.clear();
        realSimilarVisible=false;realSimilarReview=false;similarAfterScan=false;realSimilarSelected.clear();realSimilarKeep.clear();
        realDupVisible=false;realDupReview=false;dupAfterScan=false;realDupSelected.clear();nextThumbGeneration();dupCache.evictAll();
        scanVisible=false;scanReviewVisible=false;cleanVisible=true;settingsVisible=false;storageVisible=false;
        aboutVisible=false;policyVisible=false;termsVisible=false;feedbackVisible=false;helpVisible=false;
        permissionsVisible=false;privacyVisible=false;historyVisible=false;agentPrefsVisible=false;protectionVisible=false;foldersVisible=false;filesVisible=false;
        setContentView(cleanScreen());
        final ScrollView scroll=cleanScroll;if(scroll!=null)scroll.post(()->scroll.scrollTo(0,cleanScrollY));
    }
    // Phase 38: landing interface only. No utility actions or permissions are requested.
    private void showTools(){
        if(agentBusy())return;
        restoreAgentWindow();
        agentVisible=false;agentTaskVisible=false;agentTaskOrigin=false;agentShortcut=false;
        cleanVisible=false;cleanScroll=null;cleanScrollY=0;
        scanResult=null;scanVisible=false;scanReviewVisible=false;
        settingsVisible=false;storageVisible=false;completionVisible=false;
        aboutVisible=false;policyVisible=false;termsVisible=false;feedbackVisible=false;helpVisible=false;
        permissionsVisible=false;privacyVisible=false;historyVisible=false;agentPrefsVisible=false;
        protectionVisible=false;foldersVisible=false;filesVisible=false;
        toolsVisible=true;
        setContentView(toolsScreen());
        final ScrollView scroll=toolsScroll;
        if(scroll!=null)scroll.post(()->scroll.scrollTo(0,toolsScrollY));
    }
    private LinearLayout toolsScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(10));
        add(root,header(),-1,dp(44));space(root,8);

        ScrollView scroll=new ScrollView(this);toolsScroll=scroll;
        scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        LinearLayout body=column();body.setPadding(0,v(4),0,v(8));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        add(body,txt("Tools",29,INK,700),-1,-2);space(body,4);
        add(body,txt("Make more of your phone.",13,GRAY,400),-1,-2);space(body,19);
        add(body,txt("PHOTOS & VIDEOS",11,GRAY,600),-1,-2);space(body,8);
        LinearLayout media=card();pad(media,13,3);
        toolRow(media,"Swipe Photos","Keep what matters","tool_swipe",BLUE,PALE,true);
        toolRow(media,"Video Compressor","Reduce video size","video",BLUE,PALE,true);
        toolRow(media,"Photo Compressor","Save space effortlessly","images",BLUE,PALE,false);
        add(body,media,-1,-2);space(body,18);
        add(body,txt("PHONE UTILITIES",11,GRAY,600),-1,-2);space(body,8);
        LinearLayout utilities=card();pad(utilities,13,3);
        toolRow(utilities,"Contacts Cleaner","","users",dark?0xff55d5ac:0xff139b78,dark?0xff193b34:0xffe7f6f0,true);
        toolRow(utilities,"App Manager","","apps",dark?0xffb5a0ff:0xff7657db,dark?0xff302747:0xfff0ebff,true);
        toolRow(utilities,"Speaker Cleaner","","tool_speaker",BLUE,PALE,true);
        toolRow(utilities,"Internet Speed Test","","tool_speed",BLUE,PALE,false);
        add(body,utilities,-1,-2);space(body,14);
        LinearLayout footer=row();footer.setGravity(Gravity.CENTER);
        add(footer,new Glyph(this,"shield",GRAY),dp(15),dp(15));gap(footer,5);
        add(footer,txt("Always in your control.",12,GRAY,400),-2,-2);
        add(body,footer,-1,-2);
        space(root,6);add(root,navigation(),-1,dp(64));return root;
    }
    private void toolRow(LinearLayout parent,String title,String subtitle,String glyph,int tint,int iconBg,boolean divider){
        boolean detailed=!subtitle.isEmpty();
        LinearLayout line=row();line.setMinimumHeight(dp(detailed?63:54));
        line.setPadding(0,v(7),0,v(7));
        int iconSize=dp(detailed?42:38);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(iconBg,11));
        mark.addView(new Glyph(this,glyph,tint),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
        add(line,mark,iconSize,iconSize);gap(line,12);
        LinearLayout copy=column();add(copy,txt(title,15,INK,600),-1,-2);
        if(detailed){space(copy,4);add(copy,txt(subtitle,12,GRAY,400),-1,-2);}
        weight(line,copy);gap(line,8);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));
        tap(line,WHITE,12,()->{
            if(title.equals("Swipe Photos")){startActivity(new android.content.Intent(this,SwipePhotosActivity.class));return;}
            if(title.equals("Video Compressor")){startActivity(new android.content.Intent(this,VideoCompressorActivity.class));return;}
            if(title.equals("Photo Compressor")){startActivity(new android.content.Intent(this,PhotoCompressorActivity.class));return;}
            if(title.equals("Contacts Cleaner")){startActivity(new android.content.Intent(this,ContactsCleanerActivity.class));return;}
            if(title.equals("App Manager")){startActivity(new android.content.Intent(this,AppManagerActivity.class));return;}
            if(title.equals("Speaker Cleaner")){startActivity(new android.content.Intent(this,SpeakerCleanerActivity.class));return;}
            if(title.equals("Internet Speed Test")){startActivity(new android.content.Intent(this,InternetSpeedActivity.class));return;}
            privacyDialog().setTitle(tr(title))
                .setMessage(tr("This tool is not available yet. No action has been performed."))
                .setPositiveButton(tr("Done"),null).show();
        });
        line.setContentDescription(tr(title)+(detailed?", "+tr(subtitle):""));
        add(parent,line,-1,-2);if(divider)thinRule(parent);
    }

    private LinearLayout cleanScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(10));
        LinearLayout header=row();LinearLayout heading=column();add(heading,txt("Clean",29,INK,700),-1,-2);space(heading,4);
        add(heading,txt("Make room for what matters.",12,GRAY,400),-1,-2);weight(header,heading);gap(header,10);
        FrameLayout settings=new FrameLayout(this);
        settings.addView(new Glyph(this,"settings",INK),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        settings.setContentDescription(tr("Settings"));tap(settings,WHITE,24,this::openSettings);add(header,settings,dp(44),dp(44));
        add(root,header,-1,-2);space(root,14);
        ScrollView scroll=new ScrollView(this);cleanScroll=scroll;scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,0,0,v(10));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=card();pad(hero,17,16);
        LinearLayout top=row();LinearLayout copy=column();
        LinearLayout kicker=row();add(kicker,new Glyph(this,"sparkles",BLUE),dp(15),dp(15));gap(kicker,5);weight(kicker,txt("SMART CLEANUP",11,BLUE,600));add(copy,kicker,-1,-2);space(copy,11);
        add(copy,txt(scanResult==null?"—":scanBytes(scanResult.total()),35,INK,700),-1,-2);space(copy,5);add(copy,txt("Space you could recover",13,GRAY,400),-1,-2);
        space(copy,5);add(copy,txt(scanResult==null?"Not scanned yet":scanResult.limited||scanResult.partial?"Partial scan results":"Last scan · Review candidates",12,GRAY,500),-1,-2);weight(top,copy);gap(top,10);
        FrameLayout ring=new FrameLayout(this);ring.addView(new CleanScanMark(this),new FrameLayout.LayoutParams(-1,-1));add(top,ring,dp(92),dp(92));add(hero,top,-1,-2);space(hero,17);
        LinearLayout scan=row();scan.setGravity(Gravity.CENTER);add(scan,new Glyph(this,"scan",Color.WHITE),dp(21),dp(21));gap(scan,8);add(scan,txt("Smart Scan",16,Color.WHITE,600),-2,-2);
        tap(scan,BLUE,26,()->{dupAfterScan=false;similarAfterScan=false;videoAfterScan=false;shotAfterScan=false;requestSmartScan();});add(hero,scan,-1,dp(50));
        add(body,hero,-1,-2);space(body,20);
        LinearLayout section=row();weight(section,txt("Media cleanup",20,INK,700));add(section,txt("Gallery",12,GRAY,400),-2,-2);add(body,section,-1,-2);space(body,10);
        String[][] media={{"copy","Duplicates"},{"images","Similar photos"},{"video","Large videos"},{"screenshot","Screenshots"}};
        for(int pair=0;pair<2;pair++){
            if(pair>0)space(body,9);
            LinearLayout tiles=row();tiles.setGravity(Gravity.TOP);
            for(int j=0;j<2;j++){
                if(j>0)gap(tiles,9);
                String[] item=media[pair*2+j];
                LinearLayout tile=column();pad(tile,13,12);tile.setMinimumHeight(dp(132));
                LinearLayout title=row();FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,11));
                icon.addView(new Glyph(this,item[0],BLUE),new FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER));add(title,icon,dp(36),dp(36));stretch(title);
                add(title,new Glyph(this,"up",GRAY),dp(15),dp(15));add(tile,title,-1,-2);space(tile,9);
                int category=new int[]{0,3,1,2}[pair*2+j];
                boolean scanned=scanResult!=null&&category>=0&&(category==1?scanResult.videosScanned:category==3?scanResult.similarScanned:scanResult.imagesScanned);
                add(tile,txt(scanned?scanBytes(scanResult.bytes(category)):"—",23,INK,700),-1,-2);space(tile,5);add(tile,txt(item[1],14,INK,600),-1,-2);space(tile,4);
                add(tile,txt(scanned?scanResult.groups.get(category).size()+" "+tr("items"):tr("Scan to find"),12,GRAY,400),-1,-2);
                tap(tile,WHITE,18,()->{if(category==0){openRealDuplicates(false);}else if(category==3){openRealSimilar(false);}else if(category==1){openRealVideos(false);}else if(category==2){openRealScreenshots(false);}else if(scanned)showScanResults();else cleanPending(item[1]);});tile.setContentDescription(tr(item[1])+", "+(scanned?scanBytes(scanResult.bytes(category)):tr("Not scanned yet")));
                tiles.addView(tile,new LinearLayout.LayoutParams(0,-2,1f));
            }
            add(body,tiles,-1,-2);
        }
        space(body,21);add(body,txt("Files & apps",20,INK,700),-1,-2);space(body,10);
        LinearLayout files=card();pad(files,13,4);
        String[][] options={{"folder_down","Downloads","Review downloaded files"},{"document","Large files","Find files taking up space"},{"apps","Unused apps","Review rarely used apps"},{"file_x","Unnecessary files","Find removable clutter"}};
        for(int i=0;i<options.length;i++){
            String[] item=options[i];if(i>0)thinRule(files);
            LinearLayout line=row();line.setMinimumHeight(dp(63));line.setPadding(0,dp(9),0,dp(9));
            FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,11));icon.addView(new Glyph(this,item[0],BLUE),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
            add(line,icon,dp(38),dp(38));gap(line,12);LinearLayout labels=column();add(labels,txt(item[1],15,INK,600),-1,-2);space(labels,4);add(labels,txt(item[2],12,GRAY,400),-1,-2);
            weight(line,labels);gap(line,8);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));tap(line,WHITE,10,()->{if(item[1].equals("Downloads"))openDownloads();else if(item[1].equals("Large files"))openLargeFiles();else if(item[1].equals("Unused apps"))openUnusedApps();else if(item[1].equals("Unnecessary files"))openClutter();else cleanPending(item[1]);});add(files,line,-1,-2);
        }
        add(body,files,-1,-2);space(body,17);
        LinearLayout footer=row();footer.setGravity(Gravity.CENTER);
        add(footer,new Glyph(this,"shield",GREEN),dp(16),dp(16));gap(footer,6);add(footer,txt("Always protected. Always your choice.",12,GRAY,400),-2,-2);add(body,footer,-1,-2);
        space(root,6);add(root,navigation(),-1,dp(64));return root;
    }
    private String scanBytes(long bytes){
        if(bytes>=1024L*1024*1024)return String.format(java.util.Locale.US,"%.2f GB",bytes/(1024d*1024*1024));
        if(bytes>=1024L*1024)return String.format(java.util.Locale.US,"%.1f MB",bytes/(1024d*1024));
        if(bytes>=1024)return String.format(java.util.Locale.US,"%.1f KB",bytes/1024d);
        return bytes+" B";
    }
    private int scanScope(){return videoAfterScan?ToolAccess.VIDEOS:dupAfterScan||similarAfterScan||shotAfterScan?ToolAccess.PHOTOS:ToolAccess.MEDIA;}
    private void clearScanRequest(){requestingScan=false;dupAfterScan=false;similarAfterScan=false;videoAfterScan=false;shotAfterScan=false;}
    private void requestSmartScan(){
        if(requestingScan||runningScan!=null)return;
        final int scope=scanScope();
        if(ToolAccess.has(this,scope)){startSmartScan();return;}
        privacyDialog().setTitle(tr("Media access needed"))
            .setMessage(tr(scope==ToolAccess.PHOTOS?"Allow access to photos to use this tool.":scope==ToolAccess.VIDEOS?"Allow access to videos to use this tool.":"Smart Scan reads only the photos and videos you allow. Nothing is deleted."))
            .setNegativeButton(tr("Cancel"),(d,w)->{clearScanRequest();if(agentTaskOrigin)showAgent();}).setOnCancelListener(d->clearScanRequest())
            .setPositiveButton(tr(ToolAccess.action(this,scope)),(d,w)->{if(requestingScan||runningScan!=null)return;requestingScan=true;
                if(!ToolAccess.launch(this,scope,REQUEST_SCAN_MEDIA)){clearScanRequest();folderToast("Could not open access settings");}}).show();
    }
    private boolean handleToolPermission(int code){
        if(code==REQUEST_AGENT_MEDIA){
            if(!agentPermissionPending)return true;agentPermissionPending=false;
            if(agentVisible&&activeChat!=null){if(agentHasAccess(agentScanTask))beginAgentScan(agentScanTask);else{appendAgent(false,"Media access was not granted. No files were scanned.",-1);showAgent();}}return true;
        }
        if(code==REQUEST_SCAN_MEDIA){
            if(!requestingScan)return true;requestingScan=false;
            if(ToolAccess.has(this,scanScope()))startSmartScan();
            else{clearScanRequest();folderToast("Media access not granted. You can try again anytime.");if(agentTaskOrigin)showAgent();}
            return true;
        }return false;
    }
    private LinearLayout scanShell(String title,Runnable back){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);header.addView(centered(title,17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout arrow=new FrameLayout(this);arrow.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));tap(arrow,WHITE,22,back);arrow.setContentDescription(tr("Back"));
        header.addView(arrow,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));return root;
    }
    private LinearLayout scanBody(LinearLayout root){
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));return body;
    }
    private void scanButton(LinearLayout root,String label,boolean primary,Runnable action){
        TextView button=centered(label,16,primary?Color.WHITE:INK,600);tap(button,primary?BLUE:WHITE,26,action);add(root,button,-1,dp(52));
    }
    private void startSmartScan(){
        if(runningScan!=null)return;
        if(cleanScroll!=null){cleanScrollY=cleanScroll.getScrollY();cleanScroll=null;}
        scanVisible=true;scanReviewVisible=false;scanResult=null;
        final boolean videoOnly=videoAfterScan,photoOnly=dupAfterScan||similarAfterScan||shotAfterScan;
        final int accessAtStart=ToolAccess.mediaMask(this);
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);runningScan=job;
        LinearLayout root=scanShell(videoOnly?"Large videos":"Smart Scan",this::cancelSmartScan),body=scanBody(root);
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout ring=new FrameLayout(this);ring.addView(new CleanScanMark(this),new FrameLayout.LayoutParams(-1,-1));add(hero,ring,dp(164),dp(164));space(hero,18);
        scanProgress=centered("Scanning",23,INK,700);add(hero,scanProgress,-1,-2);space(hero,8);
        add(hero,centered("Finding what you don't need.",18,INK,600),-1,-2);space(hero,6);
        add(hero,centered(videoOnly?"Analyzing accessible videos.":"Analyzing accessible photos and videos.",13,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,20);
        LinearLayout status=card();scanStatus=txt("Reading media library",15,INK,600);add(status,scanStatus,-1,-2);space(status,13);
        scanBar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        scanBar.setIndeterminate(true);scanBar.setIndeterminateTintList(ColorStateList.valueOf(BLUE));scanBar.setProgressTintList(ColorStateList.valueOf(BLUE));add(status,scanBar,-1,dp(8));space(status,12);
        add(status,txt(videoOnly?"Large videos":"Duplicate photos · Similar photos · Large videos · Screenshots",12,GRAY,400),-1,-2);add(body,status,-1,-2);space(body,16);
        add(body,centered("Your files are safe during scanning.",12,GREEN,500),-1,-2);
        scanButton(root,"Cancel scan",false,this::cancelSmartScan);setContentView(root);
        new Thread(()->{
            try{
                SmartMediaScan.Progress callback=(stage,done,total)->{
                    if(done!=total&&done%16!=0)return;
                    runOnUiThread(()->{if(runningScan!=job||isFinishing()||isDestroyed())return;
                        scanStatus.setText(tr(new String[]{"Reading media library","Checking exact duplicates","Classifying media","Comparing similar photos"}[stage]));
                        scanBar.setIndeterminate(total<=0);if(total>0){scanBar.setMax(total);scanBar.setProgress(done);scanProgress.setText(done+" / "+total);}else scanProgress.setText(tr("Scanning"));
                    });
                };
                SmartMediaScan.Result result=videoOnly?job.runVideos(callback):photoOnly?job.runPhotos(callback):job.run(callback);
                runOnUiThread(()->{if(runningScan!=job||isFinishing()||isDestroyed())return;runningScan=null;
                    if(accessAtStart!=ToolAccess.mediaMask(this)||!ToolAccess.has(this,scanScope())){scanResult=null;showClean();folderToast("Media access changed. Scan again.");return;}
                    scanResult=result;
                    if(result.examined>0||result.imagesScanned||result.videosScanned)recordHistory(null,"scan","Media scan finished",(result.limited||result.partial?"Partial coverage · ":"")+"Accessible media checked. No files removed.",0,result.examined,false);
                    java.util.Arrays.fill(scanSelected,false);if(dupAfterScan){dupAfterScan=false;showRealDuplicates();}else if(similarAfterScan){similarAfterScan=false;showRealSimilar();}else if(videoAfterScan){videoAfterScan=false;showRealVideos();}else if(shotAfterScan){shotAfterScan=false;showRealScreenshots();}else showScanResults();});
            }catch(Exception e){runOnUiThread(()->{if(runningScan!=job||isFinishing()||isDestroyed())return;runningScan=null;showClean();folderToast("Scan could not finish");});}
        },"Clenova-media-scan").start();
    }
    private void cancelSmartScan(){
        if(runningScan!=null){runningScan.cancel();runningScan=null;}
        scanVisible=false;scanReviewVisible=false;if(agentTaskOrigin)showAgent();else showClean();
    }
    private void showScanResults(){
        if(scanResult==null){showClean();return;}
        realShotVisible=false;realShotReview=false;realShotPreview=false;
        stopRealVideo();realVideoVisible=false;realVideoReview=false;realVideoPreview=false;
        realSimilarVisible=false;realSimilarReview=false;
        realDupVisible=false;realDupReview=false;nextThumbGeneration();
        scanVisible=true;scanReviewVisible=false;
        LinearLayout root=scanShell("Scan results",this::cancelSmartScan),body=scanBody(root);
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);add(hero,new Glyph(this,"selected",BLUE),dp(62),dp(62));space(hero,12);
        add(hero,centered(scanResult.partial||scanResult.limited?"Partial scan results":"Scan complete",15,BLUE,600),-1,-2);space(hero,8);
        add(hero,centered(scanBytes(scanResult.total()),35,INK,700),-1,-2);space(hero,5);add(hero,centered("Potential space to recover",14,GRAY,400),-1,-2);
        space(hero,7);add(hero,centered(scanResult.examined+" "+tr("media items checked"),12,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,20);
        add(body,txt("Cleanup opportunities",18,INK,700),-1,-2);space(body,10);
        LinearLayout list=card();pad(list,13,4);TextView review=centered("Review cleanup",16,Color.WHITE,600);
        for(int i=0;i<4;i++){
            if(i>0)thinRule(list);final int index=i;LinearLayout line=row();line.setMinimumHeight(dp(65));line.setPadding(0,dp(9),0,dp(9));
            add(line,new Glyph(this,new String[]{"copy","video","screenshot","images"}[i],BLUE),dp(25),dp(25));gap(line,12);
            LinearLayout labels=column();add(labels,txt(SmartMediaScan.LABELS[i],15,INK,600),-1,-2);space(labels,5);
            boolean scanned=i==1?scanResult.videosScanned:i==3?scanResult.similarScanned:scanResult.imagesScanned;
            add(labels,txt(scanned?scanResult.groups.get(i).size()+" · "+scanBytes(scanResult.bytes(i)):tr("Not scanned"),12,GRAY,400),-1,-2);weight(line,labels);
            {tap(labels,WHITE,8,()->{if(index==0)openRealDuplicates(true);else if(index==1)openRealVideos(true);else if(index==2)openRealScreenshots(true);else openRealSimilar(true);});add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));}
            android.widget.CheckBox check=new android.widget.CheckBox(this);check.setButtonTintList(ColorStateList.valueOf(BLUE));check.setChecked(scanSelected[i]);check.setEnabled(!scanResult.groups.get(i).isEmpty());check.setContentDescription(tr(SmartMediaScan.LABELS[i]));check.setOnCheckedChangeListener((v,checked)->{scanSelected[index]=checked;updateScanReview(review);});add(line,check,dp(44),dp(44));add(list,line,-1,-2);
        }
        add(body,list,-1,-2);space(body,14);
        LinearLayout scope=card();add(scope,txt("Scan coverage",15,INK,600),-1,-2);space(scope,8);
        add(scope,txt("Downloads, Large files and Unnecessary files use separate user-selected file access and are not added to Smart Scan totals. Unused apps uses separate optional Usage Access and is not added to media totals. Similar-photo suggestions require visual review.",13,GRAY,400),-1,-2);
        if(scanResult.limited){space(scope,7);add(scope,txt("Limited media access. Only accessible items were checked.",13,GRAY,400),-1,-2);}
        if(scanResult.partial){space(scope,7);add(scope,txt("Some media could not be checked or the scan limit was reached.",13,GRAY,400),-1,-2);}
        space(scope,7);add(scope,txt("Large videos: 100 MiB or more. Screenshots: folder-based detection.",12,GRAY,400),-1,-2);
        add(body,scope,-1,-2);space(body,12);
        LinearLayout protection=card();add(protection,txt("Protection enabled",15,GREEN,600),-1,-2);space(protection,6);add(protection,txt("Protected or uncertain items are excluded. At least one exact duplicate is retained.",13,GRAY,400),-1,-2);add(body,protection,-1,-2);
        tap(review,BLUE,26,this::showScanReview);updateScanReview(review);add(root,review,-1,dp(52));space(root,7);
        add(root,centered("Nothing is deleted without your approval.",12,GRAY,400),-1,-2);setContentView(root);
    }
    private void updateScanReview(TextView button){
        long bytes=0;for(int i=0;i<4;i++)if(scanSelected[i])bytes+=scanResult.bytes(i);
        button.setEnabled(bytes>0);button.setAlpha(bytes>0?1f:.45f);button.setText(tr("Review cleanup")+" · "+scanBytes(bytes));
    }
    private void showScanReview(){
        if(scanResult==null){showClean();return;}
        scanReviewVisible=true;scanVisible=true;LinearLayout root=scanShell("Review cleanup",this::showScanResults),body=scanBody(root);
        add(body,txt("Review selected media",21,INK,700),-1,-2);space(body,8);
        add(body,txt("This combined review is read-only. Open the individual media categories for their separate Android trash workflows.",13,GRAY,400),-1,-2);space(body,14);
        for(int i=0;i<4;i++)if(scanSelected[i]){
            add(body,txt(SmartMediaScan.LABELS[i],15,INK,600),-1,-2);space(body,8);
            LinearLayout group=card();int shown=0;
            for(SmartMediaScan.Item item:scanResult.groups.get(i)){
                if(shown==100)break;if(shown++>0)thinRule(group);LinearLayout line=row();line.setMinimumHeight(dp(48));
                TextView name=txt(item.name,13,INK,500);name.setMaxLines(2);weight(line,name);gap(line,8);add(line,txt(scanBytes(item.bytes),12,GRAY,400),-2,-2);add(group,line,-1,-2);
            }
            int remaining=scanResult.groups.get(i).size()-shown;if(remaining>0){space(group,8);add(group,txt(remaining+" "+tr("more items not shown"),12,GRAY,400),-1,-2);}
            add(body,group,-1,-2);space(body,8);final int category=i;scanButton(body,"Open "+SmartMediaScan.LABELS[i],false,()->{if(category==0)openRealDuplicates(true);else if(category==1)openRealVideos(true);else if(category==2)openRealScreenshots(true);else openRealSimilar(true);});space(body,15);
        }
        scanButton(root,"Done",true,this::showScanResults);setContentView(root);
    }

    private void openRealDuplicates(boolean fromResults){
        dupFromResults=fromResults;dupPage=0;dupAll=false;similarAfterScan=false;videoAfterScan=false;shotAfterScan=false;realDupSelected.clear();
        if(scanResult!=null&&scanResult.imagesScanned){showRealDuplicates();return;}
        dupAfterScan=true;requestSmartScan();
    }
    private void leaveRealDuplicates(){
        realDupVisible=false;realDupReview=false;nextThumbGeneration();realDupSelected.clear();
        if(dupFromResults&&scanResult!=null)showScanResults();else if(agentTaskOrigin)showAgent();else showClean();
    }
    private long duplicateSelectedBytes(){
        long size=0;if(scanResult!=null)for(SmartMediaScan.Item i:scanResult.groups.get(0))if(realDupSelected.contains(i.uri.toString()))size+=i.bytes;return size;
    }
    private void showRealDuplicates(){
        if(scanResult==null){showClean();return;}
        realDupVisible=true;realDupReview=false;scanVisible=false;scanReviewVisible=false;
        final int generation=nextThumbGeneration();
        LinearLayout root=scanShell("Duplicate photos",this::leaveRealDuplicates),body=scanBody(root);
        LinearLayout hero=card(),summary=row(),copy=column();add(copy,txt("Recoverable space",13,GRAY,400),-1,-2);space(copy,6);
        add(copy,txt(scanBytes(scanResult.bytes(0)),32,INK,700),-1,-2);space(copy,6);
        add(copy,txt(scanResult.groups.get(0).size()+" "+tr("duplicates")+" · "+scanResult.duplicates.size()+" "+tr("groups"),12,GRAY,400),-1,-2);weight(summary,copy);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,18));mark.addView(new Glyph(this,"copy",BLUE),new FrameLayout.LayoutParams(dp(33),dp(33),Gravity.CENTER));add(summary,mark,dp(64),dp(64));add(hero,summary,-1,-2);space(hero,12);thinRule(hero);space(hero,10);
        LinearLayout safe=row();add(safe,new Glyph(this,"shield",GREEN),dp(20),dp(20));gap(safe,8);weight(safe,txt(tr("At least one copy kept")+" · "+scanResult.duplicates.size()+" "+tr("groups"),13,GREEN,600));add(hero,safe,-1,-2);add(body,hero,-1,-2);space(body,14);
        LinearLayout tabs=row();tabs.setBackground(round(surface(0xffe7e7ec),15));pad(tabs,4,4);
        String[] names={"Smart picks","All photos"};for(int j=0;j<2;j++){final boolean all=j==1;TextView tab=centered(names[j],14,dupAll==all?INK:GRAY,600);tap(tab,dupAll==all?WHITE:surface(0xffe7e7ec),12,()->{dupAll=all;dupPage=0;showRealDuplicates();});tabs.addView(tab,new LinearLayout.LayoutParams(0,dp(40),1f));}
        add(body,tabs,-1,-2);space(body,15);
        java.util.List<SmartMediaScan.DuplicateGroup> groups=new java.util.ArrayList<>();
        for(SmartMediaScan.DuplicateGroup g:scanResult.duplicates){boolean candidate=false;for(SmartMediaScan.Item i:g.items)if(!i.protectedItem)candidate=true;if(dupAll||candidate)groups.add(g);}
        // Bound UI work: paginate groups, and each group can reveal its remaining photos by a separate dialog.
        int pages=Math.max(1,(groups.size()+5)/6);dupPage=Math.max(0,Math.min(dupPage,pages-1));
        LinearLayout section=row();weight(section,txt("Duplicate groups",17,INK,600));
        TextView select=txt(realDupSelected.isEmpty()?(scanResult.groups.get(0).size()>DuplicateApproval.MAX_BATCH?"Select 100":"Select all"):"Deselect all",13,BLUE,600);
        tap(select,BG,8,()->{if(!realDupSelected.isEmpty())realDupSelected.clear();else for(SmartMediaScan.Item i:scanResult.groups.get(0)){if(realDupSelected.size()>=DuplicateApproval.MAX_BATCH)break;realDupSelected.add(i.uri.toString());}showRealDuplicates();});add(section,select,-2,dp(44));add(body,section,-1,-2);
        TextView count=txt("",12,GRAY,400),amount=txt("",22,INK,700),review=centered("Review & clean",14,Color.WHITE,600);
        if(groups.isEmpty()){
            LinearLayout empty=card();add(empty,centered(scanResult.imagesScanned?"No removable duplicates found":"Photo access needed",17,INK,600),-1,-2);space(empty,8);add(empty,centered("Only verified identical copies are suggested.",13,GRAY,400),-1,-2);add(body,empty,-1,-2);
        }
        for(int g=dupPage*6;g<Math.min(groups.size(),(dupPage+1)*6);g++){
            SmartMediaScan.DuplicateGroup group=groups.get(g);LinearLayout card=card();pad(card,12,12);
            add(card,txt(tr("Group")+" "+(g+1),15,INK,600),-1,-2);space(card,5);add(card,txt(group.items.size()+" "+tr("identical photos"),12,GRAY,400),-1,-2);space(card,11);
            int shown=Math.min(18,group.items.size());
            for(int start=0;start<shown;start+=3){
                LinearLayout row=row();row.setGravity(Gravity.TOP);for(int col=0;col<3;col++){
                    if(col>0)gap(row,7);int index=start+col;
                    if(index>=shown){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1f));continue;}
                    SmartMediaScan.Item item=group.items.get(index);
                    row.addView(duplicateTile(item,generation,()->updateDuplicateFooter(count,amount,review)),new LinearLayout.LayoutParams(0,-2,1f));
                }add(card,row,-1,-2);space(card,7);
            }
            if(group.items.size()>shown){TextView more=centered(tr("View all in group")+" ("+group.items.size()+")",13,BLUE,600);tap(more,PALE,10,()->showDuplicateGroup(group));add(card,more,-1,dp(42));}
            add(body,card,-1,-2);space(body,11);
        }
        if(pages>1){LinearLayout pager=row();TextView prev=centered("Previous",13,BLUE,600),next=centered("Next",13,BLUE,600);
            tap(prev,WHITE,12,()->{dupPage--;showRealDuplicates();});prev.setEnabled(dupPage>0);prev.setAlpha(dupPage>0?1:.4f);
            tap(next,WHITE,12,()->{dupPage++;showRealDuplicates();});next.setEnabled(dupPage+1<pages);next.setAlpha(dupPage+1<pages?1:.4f);
            weight(pager,prev);weight(pager,centered((dupPage+1)+" / "+pages,13,GRAY,400));weight(pager,next);add(body,pager,-1,dp(48));}
        if(scanResult.partial||scanResult.limited){space(body,8);add(body,txt("Partial scan results",12,GRAY,500),-1,-2);}
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);
        tap(review,BLUE,24,this::showRealDuplicateReview);review.setPadding(dp(13),0,dp(13),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);
        space(root,7);add(root,centered("Your originals stay safe.",12,GRAY,400),-1,-2);updateDuplicateFooter(count,amount,review);setContentView(root);
    }
    private LinearLayout duplicateTile(SmartMediaScan.Item item,int generation,Runnable changed){
        LinearLayout tile=column();tile.setBackground(round(WHITE,10));tile.setClipToOutline(true);
        android.widget.ImageView image=new android.widget.ImageView(this);image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);image.setBackgroundColor(PALE);
        image.setContentDescription(item.name);image.setOnClickListener(v->showRealPhoto(item));add(tile,image,-1,dp(86));loadDupThumb(image,item,256,generation);
        TextView select=centered("",11,GRAY,600);add(tile,select,-1,dp(36));
        Runnable refresh=()->{boolean chosen=realDupSelected.contains(item.uri.toString());select.setText(item.protectedItem?tr("Keep"):((chosen?"✓ ":"")+scanBytes(item.bytes)));
            select.setTextColor(item.protectedItem?GREEN:chosen?Color.WHITE:GRAY);select.setBackground(round(item.protectedItem?surface(0xffe8f6ec):chosen?BLUE:BG,8));
            select.setContentDescription(item.name+", "+tr(item.protectedItem?"Keep":chosen?"Selected":"Not selected"));};
        select.setClickable(!item.protectedItem);select.setEnabled(!item.protectedItem);select.setOnClickListener(v->{
            String id=item.uri.toString();if(!realDupSelected.remove(id)){if(realDupSelected.size()>=DuplicateApproval.MAX_BATCH){folderToast("Select up to 100 photos at a time");return;}realDupSelected.add(id);}refresh.run();changed.run();});
        refresh.run();return tile;
    }
    private void updateDuplicateFooter(TextView count,TextView amount,TextView review){
        count.setText(realDupSelected.size()+" "+tr("photos selected"));amount.setText(scanBytes(duplicateSelectedBytes()));
        review.setEnabled(!realDupSelected.isEmpty());review.setAlpha(realDupSelected.isEmpty()?.45f:1f);
    }
    private int nextThumbGeneration(){dupThumbs.getQueue().clear();return ++dupThumbGeneration;}
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)dupCache.evictAll();}
    @Override public void onLowMemory(){super.onLowMemory();dupCache.evictAll();}
    private boolean thumbAccess(SmartMediaScan.Item item){return item.image?SmartMediaScan.hasPhotoAccess(this):SmartMediaScan.hasVideoAccess(this);}
    private void loadDupThumb(android.widget.ImageView image,SmartMediaScan.Item item,int size,int generation){
        if(generation!=dupThumbGeneration||!thumbAccess(item))return;
        String key=item.uri.toString()+":"+item.modified+":"+item.bytes+":"+item.generation+":"+size;android.graphics.Bitmap cached=dupCache.get(key);if(cached!=null){image.setImageBitmap(cached);return;}
        try{dupThumbs.execute(()->{
            if(generation!=dupThumbGeneration||!thumbAccess(item))return;android.graphics.Bitmap bitmap=null;
            try{
                if(android.os.Build.VERSION.SDK_INT>=29)bitmap=getContentResolver().loadThumbnail(item.uri,new android.util.Size(size,size),null);
                else if(!item.image){
                    if(android.os.Build.VERSION.SDK_INT>=27){android.media.MediaMetadataRetriever retriever=new android.media.MediaMetadataRetriever();try{retriever.setDataSource(this,item.uri);bitmap=retriever.getScaledFrameAtTime(-1,android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,size,size);}finally{retriever.release();}}
                }else{
                    android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inJustDecodeBounds=true;
                    try(java.io.InputStream in=getContentResolver().openInputStream(item.uri)){android.graphics.BitmapFactory.decodeStream(in,null,opts);}
                    if(opts.outWidth<=0||opts.outHeight<=0)return;int factor=1;while(Math.max(opts.outWidth,opts.outHeight)/factor>size*2)factor*=2;
                    opts.inSampleSize=factor;opts.inJustDecodeBounds=false;try(java.io.InputStream in=getContentResolver().openInputStream(item.uri)){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,opts);}
                }
            }catch(Exception ignored){}
            if(bitmap==null)return;final android.graphics.Bitmap loaded=bitmap;
            runOnUiThread(()->{if(generation!=dupThumbGeneration||isDestroyed()||!thumbAccess(item)||!image.isAttachedToWindow()){loaded.recycle();return;}if(loaded.getByteCount()<=dupCache.maxSize()/2)dupCache.put(key,loaded);image.setImageBitmap(loaded);});
        });}catch(java.util.concurrent.RejectedExecutionException ignored){}
    }
    private void showRealPhoto(SmartMediaScan.Item item){
        LinearLayout content=column();pad(content,12,12);android.widget.ImageView image=new android.widget.ImageView(this);image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);image.setBackgroundColor(BG);
        add(content,image,-1,dp(300));space(content,8);add(content,txt(item.name+" · "+scanBytes(item.bytes),13,INK,500),-1,-2);
        loadDupThumb(image,item,1024,dupThumbGeneration);
        privacyDialog().setTitle(tr(item.protectedItem?"Keep":"Photo preview")).setView(content).setPositiveButton(tr("Done"),null).show();
    }
    private void showDuplicateGroup(SmartMediaScan.DuplicateGroup group){
        // Native multi-choice dialog exposes every member without unbounded thumbnail allocations.
        String[] labels=new String[group.items.size()];boolean[] checked=new boolean[labels.length];
        for(int i=0;i<labels.length;i++){SmartMediaScan.Item item=group.items.get(i);labels[i]=(item.protectedItem?tr("Keep")+" · ":"")+item.name+" · "+scanBytes(item.bytes);checked[i]=realDupSelected.contains(item.uri.toString());}
        AlertDialog dialog=privacyDialog().setTitle(tr("Duplicate group")).setMultiChoiceItems(labels,checked,(d,index,value)->{
            SmartMediaScan.Item item=group.items.get(index);String key=item.uri.toString();
            if(item.protectedItem||(value&&!realDupSelected.contains(key)&&realDupSelected.size()>=DuplicateApproval.MAX_BATCH)){
                ((AlertDialog)d).getListView().setItemChecked(index,false);return;
            }
            if(value)realDupSelected.add(key);else realDupSelected.remove(key);
        }).setPositiveButton(tr("Done"),null).create();dialog.setOnDismissListener(d->showRealDuplicates());dialog.show();
    }
    private void showRealDuplicateReview(){
        if(scanResult==null||realDupSelected.isEmpty()){showRealDuplicates();return;}
        realDupReview=true;LinearLayout root=scanShell("Review cleanup",this::showRealDuplicates),body=scanBody(root);
        add(body,txt(realDupSelected.size()+" "+tr("photos selected"),22,INK,700),-1,-2);space(body,6);add(body,txt(scanBytes(duplicateSelectedBytes()),24,BLUE,600),-1,-2);space(body,12);
        add(body,txt("Move only verified duplicate copies to the system trash. Space may not be freed until trash is emptied.",14,GRAY,400),-1,-2);space(body,14);
        LinearLayout items=card();int n=0;for(SmartMediaScan.Item i:scanResult.groups.get(0))if(realDupSelected.contains(i.uri.toString())){
            if(n++>0)thinRule(items);LinearLayout line=row();line.setMinimumHeight(dp(48));TextView name=txt(i.name,13,INK,500);name.setMaxLines(2);weight(line,name);gap(line,6);add(line,txt(scanBytes(i.bytes),12,GRAY,400),-2,-2);add(items,line,-1,-2);
        }add(body,items,-1,-2);
        if(android.os.Build.VERSION.SDK_INT>=30)scanButton(root,"Move to trash",true,this::recheckDuplicateTrash);
        else{space(body,12);add(body,txt("System trash approval requires Android 11 or later. Review only on this device.",13,GRAY,400),-1,-2);scanButton(root,"Done",true,this::showRealDuplicates);}
        setContentView(root);
    }
    private void recheckDuplicateTrash(){
        if(android.os.Build.VERSION.SDK_INT<30||runningScan!=null||scanResult==null||realDupSelected.isEmpty())return;
        final SmartMediaScan.Result before=scanResult;final java.util.Set<String> selected=new java.util.HashSet<>(realDupSelected);
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);runningScan=job;
        LinearLayout root=scanShell("Review cleanup",()->{job.cancel();runningScan=null;showRealDuplicates();}),body=scanBody(root);
        add(body,centered("Rechecking duplicate safety",20,INK,600),-1,-2);space(body,12);
        android.widget.ProgressBar spinner=new android.widget.ProgressBar(this);add(body,spinner,dp(42),dp(42));
        scanButton(root,"Cancel",false,()->{job.cancel();runningScan=null;showRealDuplicates();});setContentView(root);
        new Thread(()->{
            try{
                SmartMediaScan.Result fresh=job.runPhotos((stage,done,total)->{});java.util.List<android.net.Uri> approved=DuplicateApproval.verified(before,fresh,selected);
                runOnUiThread(()->{
                    if(runningScan!=job||isDestroyed()||isFinishing())return;runningScan=null;
                    if(approved.isEmpty()){java.util.Arrays.fill(scanSelected,false);dupCache.evictAll();scanResult=fresh;realDupSelected.clear();showRealDuplicates();folderToast("Files or protection changed. Review again.");return;}
                    try{
                        android.app.PendingIntent pending=android.provider.MediaStore.createTrashRequest(getContentResolver(),approved,true);
                        cleanupReceipt=CleanupReceipt.capture(this,"Duplicate photos",approved,fresh.groups.get(0));dupTrashPending=true;startIntentSenderForResult(pending.getIntentSender(),REQUEST_DUP_TRASH,null,0,0,0);
                    }catch(Exception e){dupTrashPending=false;cleanupReceipt=null;showRealDuplicates();folderToast("Could not open Android trash approval");}
                });
            }catch(Exception e){runOnUiThread(()->{if(runningScan!=job||isDestroyed())return;runningScan=null;showRealDuplicates();folderToast("Safety recheck failed");});}
        },"Clenova-duplicate-recheck").start();
    }

    private void openRealSimilar(boolean fromResults){
        similarFromResults=fromResults;realSimilarPage=0;similarAll=false;realSimilarSelected.clear();realSimilarKeep.clear();
        if(scanResult!=null&&scanResult.similarScanned){showRealSimilar();return;}
        similarAfterScan=true;dupAfterScan=false;videoAfterScan=false;shotAfterScan=false;requestSmartScan();
    }
    private void leaveRealSimilar(){
        realSimilarVisible=false;realSimilarReview=false;nextThumbGeneration();realSimilarSelected.clear();realSimilarKeep.clear();
        if(similarFromResults&&scanResult!=null)showScanResults();else if(agentTaskOrigin)showAgent();else showClean();
    }
    private long similarBytes(){long bytes=0;if(scanResult!=null)for(SmartMediaScan.Item item:scanResult.groups.get(3))if(realSimilarSelected.contains(item.uri.toString()))bytes+=item.bytes;return bytes;}
    private void similarFooter(TextView count,TextView amount,TextView action){
        count.setText(realSimilarSelected.size()+" "+tr("photos selected"));amount.setText(scanBytes(similarBytes()));
        action.setEnabled(!realSimilarSelected.isEmpty());action.setAlpha(realSimilarSelected.isEmpty()?.45f:1f);
    }
    private void showRealSimilar(){
        if(scanResult==null){showClean();return;}
        realSimilarVisible=true;realSimilarReview=false;realDupVisible=false;scanVisible=false;scanReviewVisible=false;int generation=nextThumbGeneration();
        LinearLayout root=scanShell("Similar photos",this::leaveRealSimilar),body=scanBody(root);
        LinearLayout hero=card(),summary=row(),labels=column();add(labels,txt("Potential space to recover",13,GRAY,400),-1,-2);space(labels,6);
        add(labels,txt(scanBytes(scanResult.bytes(3)),32,INK,700),-1,-2);space(labels,5);add(labels,txt(scanResult.groups.get(3).size()+" "+tr("similar photos")+" · "+scanResult.similarGroups.size()+" "+tr("groups"),12,GRAY,400),-1,-2);weight(summary,labels);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,18));mark.addView(new Glyph(this,"images",BLUE),new FrameLayout.LayoutParams(dp(33),dp(33),Gravity.CENTER));add(summary,mark,dp(64),dp(64));add(hero,summary,-1,-2);space(hero,12);thinRule(hero);space(hero,10);
        add(hero,txt(scanResult.similarGroups.size()+" "+tr("best shots recommended"),13,BLUE,600),-1,-2);add(body,hero,-1,-2);space(body,13);
        LinearLayout tabs=row();tabs.setBackground(round(surface(0xffe7e7ec),15));pad(tabs,4,4);
        for(int j=0;j<2;j++){final boolean all=j==1;TextView tab=centered(all?"All photos":"Smart picks",14,similarAll==all?INK:GRAY,600);tap(tab,similarAll==all?WHITE:surface(0xffe7e7ec),12,()->{similarAll=all;realSimilarPage=0;showRealSimilar();});tabs.addView(tab,new LinearLayout.LayoutParams(0,dp(40),1f));}add(body,tabs,-1,-2);space(body,12);
        add(body,txt("Similar groups",18,INK,600),-1,-2);space(body,7);
        add(body,txt("Similar photos are not identical. Review each one before selecting it.",12,GRAY,400),-1,-2);space(body,9);
        java.util.List<SimilarPhotoScan.Group> groups=new java.util.ArrayList<>();
        for(SimilarPhotoScan.Group g:scanResult.similarGroups){boolean candidate=false;for(SmartMediaScan.Item i:g.items)if(SimilarPhotoScan.candidate(g,i)&&!realSimilarKeep.contains(i.uri.toString()))candidate=true;if(similarAll||candidate)groups.add(g);}
        int pages=Math.max(1,(groups.size()+3)/4);realSimilarPage=Math.max(0,Math.min(realSimilarPage,pages-1));
        TextView count=txt("",12,GRAY,400),amount=txt("",22,INK,700),review=centered("Review & clean",14,Color.WHITE,600);
        TextView clear=centered("Deselect all",13,BLUE,600);tap(clear,BG,8,()->{realSimilarSelected.clear();showRealSimilar();});add(body,clear,-1,dp(40));
        if(groups.isEmpty()){LinearLayout empty=card();add(empty,centered(scanResult.similarScanned?"No similar photo suggestions":"Photo access needed",17,INK,600),-1,-2);space(empty,7);add(empty,centered("Only visually close photos in the same folder and time window are grouped.",13,GRAY,400),-1,-2);add(body,empty,-1,-2);}
        for(int n=realSimilarPage*4;n<Math.min(groups.size(),(realSimilarPage+1)*4);n++){
            SimilarPhotoScan.Group g=groups.get(n);LinearLayout group=card();pad(group,12,12);
            add(group,txt(tr("Group")+" "+(n+1)+" · "+g.items.size()+" "+tr("photos"),15,INK,600),-1,-2);space(group,10);
            add(group,similarImage(g,g.best,generation,dp(160),true,()->similarFooter(count,amount,review)), -1,-2);space(group,8);
            add(group,txt("Suggested using sharpness, lighting and resolution estimates.",12,GRAY,400),-1,-2);space(group,10);
            java.util.List<SmartMediaScan.Item> others=new java.util.ArrayList<>();for(SmartMediaScan.Item i:g.items)if(i!=g.best)others.add(i);
            for(int start=0;start<others.size();start+=2){LinearLayout row=row();row.setGravity(Gravity.TOP);for(int col=0;col<2;col++){if(col>0)gap(row,8);int index=start+col;
                if(index>=others.size()){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1f));continue;}
                row.addView(similarImage(g,others.get(index),generation,dp(106),false,()->similarFooter(count,amount,review)),new LinearLayout.LayoutParams(0,-2,1f));
            }add(group,row,-1,-2);space(group,8);}
            TextView compare=centered("Compare photos",14,BLUE,600);tap(compare,PALE,12,()->compareSimilar(g,0));add(group,compare,-1,dp(44));add(body,group,-1,-2);space(body,12);
        }
        if(pages>1){LinearLayout pager=row();TextView prev=centered("Previous",13,BLUE,600),next=centered("Next",13,BLUE,600);
            tap(prev,WHITE,12,()->{realSimilarPage--;showRealSimilar();});prev.setEnabled(realSimilarPage>0);prev.setAlpha(realSimilarPage>0?1:.4f);
            tap(next,WHITE,12,()->{realSimilarPage++;showRealSimilar();});next.setEnabled(realSimilarPage+1<pages);next.setAlpha(realSimilarPage+1<pages?1:.4f);
            weight(pager,prev);weight(pager,centered((realSimilarPage+1)+" / "+pages,13,GRAY,400));weight(pager,next);add(body,pager,-1,dp(48));}
        if(scanResult.partial||scanResult.limited){space(body,8);add(body,txt("Partial scan results",12,GRAY,500),-1,-2);}
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);tap(review,BLUE,24,this::showRealSimilarReview);review.setPadding(dp(13),0,dp(13),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);space(root,7);add(root,centered("Your memories. Your choice.",12,GRAY,400),-1,-2);similarFooter(count,amount,review);setContentView(root);
    }
    private LinearLayout similarImage(SimilarPhotoScan.Group group,SmartMediaScan.Item item,int generation,int height,boolean best,Runnable changed){
        LinearLayout tile=column();tile.setBackground(round(BG,12));tile.setClipToOutline(true);
        android.widget.ImageView image=new android.widget.ImageView(this);image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);image.setBackgroundColor(PALE);image.setContentDescription(item.name);image.setOnClickListener(v->showRealPhoto(item));add(tile,image,-1,height);loadDupThumb(image,item,best?512:256,generation);
        String id=item.uri.toString();TextView select=centered("",12,GRAY,600);add(tile,select,-1,dp(40));
        boolean locked=best||item.protectedItem||realSimilarKeep.contains(id);
        Runnable refresh=()->{boolean chosen=realSimilarSelected.contains(id);select.setText(best?tr("Best shot")+" · "+tr("Keep"):locked?tr("Keep"):((chosen?"✓ ":"")+scanBytes(item.bytes)));select.setTextColor(locked?GREEN:chosen?Color.WHITE:INK);select.setBackground(round(locked?surface(0xffe8f6ec):chosen?BLUE:BG,8));};
        select.setEnabled(!locked);select.setOnClickListener(v->{if(locked)return;if(!realSimilarSelected.remove(id)){if(realSimilarSelected.size()>=100){folderToast("Select up to 100 photos at a time");return;}realSimilarSelected.add(id);}refresh.run();changed.run();});refresh.run();
        if(!best&&!item.protectedItem){TextView keep=centered(realSimilarKeep.contains(id)?"Remove keep mark":"Mark to keep",11,BLUE,500);tap(keep,BG,8,()->{if(!realSimilarKeep.remove(id)){realSimilarKeep.add(id);realSimilarSelected.remove(id);}showRealSimilar();});add(tile,keep,-1,dp(40));}
        return tile;
    }
    private void compareSimilar(SimilarPhotoScan.Group group,int index){
        java.util.List<SmartMediaScan.Item> others=new java.util.ArrayList<>();for(SmartMediaScan.Item i:group.items)if(i!=group.best)others.add(i);if(others.isEmpty())return;
        int pos=Math.max(0,Math.min(index,others.size()-1));SmartMediaScan.Item other=others.get(pos);
        LinearLayout content=column();pad(content,12,12);LinearLayout pair=row();for(SmartMediaScan.Item i:new SmartMediaScan.Item[]{group.best,other}){
            if(i==other)gap(pair,6);LinearLayout col=column();android.widget.ImageView view=new android.widget.ImageView(this);view.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);view.setBackgroundColor(BG);add(col,view,-1,dp(200));loadDupThumb(view,i,768,dupThumbGeneration);space(col,6);add(col,centered(i==group.best?"Best shot":scanBytes(i.bytes),12,GRAY,500),-1,-2);weight(pair,col);
        }add(content,pair,-1,-2);space(content,8);add(content,centered((pos+1)+" / "+others.size(),12,GRAY,400),-1,-2);
        AlertDialog.Builder dialog=privacyDialog().setTitle(tr("Compare photos")).setView(content).setPositiveButton(tr("Done"),null);
        if(pos+1<others.size())dialog.setNeutralButton(tr("Next"),(d,w)->compareSimilar(group,pos+1));if(pos>0)dialog.setNegativeButton(tr("Previous"),(d,w)->compareSimilar(group,pos-1));dialog.show();
    }
    private void showRealSimilarReview(){
        if(scanResult==null||realSimilarSelected.isEmpty()){showRealSimilar();return;}realSimilarReview=true;
        LinearLayout root=scanShell("Review cleanup",this::showRealSimilar),body=scanBody(root);
        add(body,txt(realSimilarSelected.size()+" "+tr("photos selected"),22,INK,700),-1,-2);space(body,6);add(body,txt(scanBytes(similarBytes()),24,BLUE,600),-1,-2);space(body,12);
        add(body,txt("These are different photos. Review each one before moving it to trash.",14,GRAY,400),-1,-2);space(body,12);
        LinearLayout items=card();int n=0;for(SmartMediaScan.Item i:scanResult.groups.get(3))if(realSimilarSelected.contains(i.uri.toString())){
            if(n++>0)thinRule(items);LinearLayout line=row();line.setMinimumHeight(dp(64));android.widget.ImageView thumb=new android.widget.ImageView(this);thumb.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);loadDupThumb(thumb,i,128,dupThumbGeneration);add(line,thumb,dp(48),dp(48));gap(line,9);TextView name=txt(i.name,13,INK,500);name.setMaxLines(2);weight(line,name);add(line,txt(scanBytes(i.bytes),12,GRAY,400),-2,-2);add(items,line,-1,-2);
        }add(body,items,-1,-2);space(body,12);add(body,txt("Space may not be freed until trash is emptied.",12,GRAY,400),-1,-2);
        if(android.os.Build.VERSION.SDK_INT>=30)scanButton(root,"Move to trash",true,this::recheckSimilarTrash);else{add(body,txt("System trash approval requires Android 11 or later. Review only on this device.",13,GRAY,400),-1,-2);scanButton(root,"Done",true,this::showRealSimilar);}setContentView(root);
    }
    private void recheckSimilarTrash(){
        if(android.os.Build.VERSION.SDK_INT<30||runningScan!=null||scanResult==null||realSimilarSelected.isEmpty())return;
        final SmartMediaScan.Result before=scanResult;final java.util.Set<String> selected=new java.util.HashSet<>(realSimilarSelected),keep=new java.util.HashSet<>(realSimilarKeep);
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);runningScan=job;
        Runnable cancel=()->{job.cancel();runningScan=null;showRealSimilar();};LinearLayout root=scanShell("Review cleanup",cancel),body=scanBody(root);
        add(body,centered("Rechecking similar photos",20,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(42),dp(42));scanButton(root,"Cancel",false,cancel);setContentView(root);
        new Thread(()->{try{
            SmartMediaScan.Result fresh=job.runPhotos((stage,done,total)->{});java.util.List<android.net.Uri> approved=SimilarPhotoScan.verify(before,fresh,selected,keep);
            runOnUiThread(()->{if(runningScan!=job||isDestroyed()||isFinishing())return;runningScan=null;
                if(approved.isEmpty()){java.util.Arrays.fill(scanSelected,false);dupCache.evictAll();scanResult=fresh;realSimilarSelected.clear();showRealSimilar();folderToast("Files or protection changed. Review again.");return;}
                try{android.app.PendingIntent pending=android.provider.MediaStore.createTrashRequest(getContentResolver(),approved,true);cleanupReceipt=CleanupReceipt.capture(this,"Similar photos",approved,fresh.groups.get(3));dupTrashPending=true;startIntentSenderForResult(pending.getIntentSender(),REQUEST_SIMILAR_TRASH,null,0,0,0);}
                catch(Exception e){dupTrashPending=false;cleanupReceipt=null;showRealSimilar();folderToast("Could not open Android trash approval");}
            });
        }catch(Exception e){runOnUiThread(()->{if(runningScan!=job||isDestroyed())return;runningScan=null;showRealSimilar();folderToast("Safety recheck failed");});}},"Clenova-similar-recheck").start();
    }

    private void openRealVideos(boolean fromResults){
        videoFromResults=fromResults;realVideoSort=0;realVideoPage=0;realVideoSelected.clear();realVideoKeep.clear();
        if(scanResult!=null&&scanResult.videosScanned){showRealVideos();return;}
        videoAfterScan=true;dupAfterScan=false;similarAfterScan=false;shotAfterScan=false;requestSmartScan();
    }
    private void stopRealVideo(){
        if(activeVideoPlayer!=null){try{activeVideoPlayer.stopPlayback();}catch(RuntimeException ignored){}activeVideoPlayer=null;}
    }
    private void leaveRealVideos(){
        stopRealVideo();realVideoVisible=false;realVideoPreview=false;realVideoReview=false;nextThumbGeneration();realVideoSelected.clear();realVideoKeep.clear();
        if(videoFromResults&&scanResult!=null)showScanResults();else if(agentTaskOrigin)showAgent();else showClean();
    }
    private String videoDuration(long millis){
        if(millis<=0)return tr("Unknown");long sec=millis/1000;
        return sec>=3600?String.format(java.util.Locale.ROOT,"%d:%02d:%02d",sec/3600,(sec/60)%60,sec%60):String.format(java.util.Locale.ROOT,"%02d:%02d",sec/60,sec%60);
    }
    private String videoResolution(SmartMediaScan.Item item){
        return item.width>0&&item.height>0?item.width+" × "+item.height:tr("Unknown");
    }
    private long selectedVideoBytes(){
        long bytes=0;if(scanResult!=null)for(SmartMediaScan.Item item:scanResult.groups.get(1))if(realVideoSelected.contains(item.uri.toString()))bytes+=item.bytes;return bytes;
    }
    private void videoFooter(TextView count,TextView amount,TextView action){
        count.setText(realVideoSelected.size()+" "+tr("videos selected"));amount.setText(scanBytes(selectedVideoBytes()));
        action.setEnabled(!realVideoSelected.isEmpty());action.setAlpha(realVideoSelected.isEmpty()?.45f:1f);
    }
    private void showRealVideos(){
        stopRealVideo();if(scanResult==null){showClean();return;}
        realVideoVisible=true;realVideoReview=false;realVideoPreview=false;realDupVisible=false;realSimilarVisible=false;scanVisible=false;scanReviewVisible=false;
        final int generation=nextThumbGeneration();
        LinearLayout root=scanShell("Large videos",this::leaveRealVideos),body=scanBody(root);
        LinearLayout hero=card(),summary=row(),labels=column();
        add(labels,txt("Potential space to recover",13,GRAY,400),-1,-2);space(labels,6);
        add(labels,txt(scanBytes(scanResult.bytes(1)),32,INK,700),-1,-2);space(labels,5);
        add(labels,txt(scanResult.groups.get(1).size()+" "+tr("large videos found"),12,GRAY,400),-1,-2);weight(summary,labels);gap(summary,10);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,18));mark.addView(new Glyph(this,"video",BLUE),new FrameLayout.LayoutParams(dp(33),dp(33),Gravity.CENTER));add(summary,mark,dp(64),dp(64));add(hero,summary,-1,-2);space(hero,12);thinRule(hero);space(hero,10);
        add(hero,txt("Videos at least 100 MiB · Protected items excluded",12,GRAY,500),-1,-2);add(body,hero,-1,-2);space(body,16);
        java.util.List<SmartMediaScan.Item> videos=new java.util.ArrayList<>(scanResult.groups.get(1));
        java.util.Comparator<SmartMediaScan.Item> comparator=realVideoSort==1?java.util.Comparator.comparingLong((SmartMediaScan.Item i)->i.duration):realVideoSort==2?java.util.Comparator.comparingLong((SmartMediaScan.Item i)->i.modified):java.util.Comparator.comparingLong((SmartMediaScan.Item i)->i.bytes);
        videos.sort(comparator.reversed().thenComparing(i->i.uri.toString()));
        LinearLayout heading=row();weight(heading,txt("Your videos",18,INK,600));
        TextView select=txt(realVideoSelected.isEmpty()?(videos.size()>100?"Select 100":"Select all"):"Deselect all",13,BLUE,600);
        tap(select,BG,8,()->{if(!realVideoSelected.isEmpty())realVideoSelected.clear();else for(SmartMediaScan.Item i:videos){if(realVideoSelected.size()>=VideoApproval.MAX_BATCH)break;if(!realVideoKeep.contains(i.uri.toString()))realVideoSelected.add(i.uri.toString());}showRealVideos();});add(heading,select,-2,dp(44));add(body,heading,-1,-2);
        String[] sorting={"Largest first","Longest first","Newest first"};TextView sort=centered(tr(sorting[realVideoSort])+"  ▾",14,BLUE,600);
        sort.setContentDescription(tr("Sort videos")+", "+tr(sorting[realVideoSort]));
        tap(sort,WHITE,12,()->privacyDialog().setTitle(tr("Sort videos")).setSingleChoiceItems(new String[]{tr(sorting[0]),tr(sorting[1]),tr(sorting[2])},realVideoSort,(d,i)->{realVideoSort=i;realVideoPage=0;d.dismiss();showRealVideos();}).setNegativeButton(tr("Cancel"),null).show());add(body,sort,-1,dp(44));space(body,11);
        TextView count=txt("",12,GRAY,400),amount=txt("",22,INK,700),review=centered("Review & clean",14,Color.WHITE,600);
        int pages=Math.max(1,(videos.size()+14)/15);realVideoPage=Math.max(0,Math.min(realVideoPage,pages-1));
        if(videos.isEmpty()){LinearLayout empty=card();add(empty,centered(scanResult.videosScanned?"No removable large videos found":"Video access needed",17,INK,600),-1,-2);space(empty,8);add(empty,centered("Only accessible videos that pass protection rules are shown.",13,GRAY,400),-1,-2);add(body,empty,-1,-2);}
        for(int n=realVideoPage*15;n<Math.min(videos.size(),(realVideoPage+1)*15);n++){
            SmartMediaScan.Item item=videos.get(n);String id=item.uri.toString();LinearLayout tile=card();pad(tile,12,12);LinearLayout line=row();
            LinearLayout visual=column();visual.setBackground(round(0xff18181b,11));visual.setClipToOutline(true);
            FrameLayout preview=new FrameLayout(this);preview.setBackgroundColor(PALE);android.widget.ImageView thumb=new android.widget.ImageView(this);thumb.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);preview.addView(thumb,new FrameLayout.LayoutParams(-1,-1));loadDupThumb(thumb,item,256,generation);
            FrameLayout play=new FrameLayout(this);play.setBackground(round(0x99000000,20));play.addView(new Glyph(this,"play",Color.WHITE),new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));preview.addView(play,new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER));add(visual,preview,-1,dp(68));add(visual,centered(videoDuration(item.duration),11,Color.WHITE,500),-1,dp(24));visual.setContentDescription(tr("Preview video")+", "+item.name);visual.setOnClickListener(v->showRealVideoPreview(item));add(line,visual,dp(102),-2);gap(line,11);
            LinearLayout details=column();TextView name=txt(item.name,13,INK,500);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(details,name,-1,-2);space(details,5);add(details,txt(scanBytes(item.bytes),20,INK,700),-1,-2);space(details,4);add(details,txt(videoResolution(item),11,GRAY,400),-1,-2);space(details,5);TextView open=txt("Preview video",12,BLUE,600);open.setOnClickListener(v->showRealVideoPreview(item));add(details,open,-1,dp(30));weight(line,details);
            boolean locked=realVideoKeep.contains(id);android.widget.CheckBox check=new android.widget.CheckBox(this);check.setButtonTintList(ColorStateList.valueOf(BLUE));check.setChecked(realVideoSelected.contains(id));check.setEnabled(!locked);check.setContentDescription(item.name);
            check.setOnCheckedChangeListener((v,checked)->{if(checked&&realVideoSelected.size()>=VideoApproval.MAX_BATCH&&!realVideoSelected.contains(id)){v.setChecked(false);folderToast("Select up to 100 videos at a time");return;}if(checked)realVideoSelected.add(id);else realVideoSelected.remove(id);videoFooter(count,amount,review);});
            add(line,check,dp(44),dp(44));add(tile,line,-1,-2);
            if(locked){space(tile,5);add(tile,txt("Keep",12,GREEN,600),-1,-2);}add(body,tile,-1,-2);space(body,9);
        }
        if(pages>1){LinearLayout pager=row();TextView prev=centered("Previous",13,BLUE,600),next=centered("Next",13,BLUE,600);tap(prev,WHITE,12,()->{realVideoPage--;showRealVideos();});prev.setEnabled(realVideoPage>0);prev.setAlpha(realVideoPage>0?1:.4f);tap(next,WHITE,12,()->{realVideoPage++;showRealVideos();});next.setEnabled(realVideoPage+1<pages);next.setAlpha(realVideoPage+1<pages?1:.4f);weight(pager,prev);weight(pager,centered((realVideoPage+1)+" / "+pages,13,GRAY,400));weight(pager,next);add(body,pager,-1,dp(48));}
        if(scanResult.partial||scanResult.limited){space(body,8);add(body,txt("Only scanned, accessible videos are listed.",12,GRAY,500),-1,-2);}
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);
        tap(review,BLUE,24,this::showRealVideoReview);review.setPadding(dp(13),0,dp(13),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);space(root,7);add(root,centered("You decide what stays.",12,GRAY,400),-1,-2);videoFooter(count,amount,review);setContentView(root);
    }
    private void showRealVideoPreview(SmartMediaScan.Item item){
        if(scanResult==null){showClean();return;}stopRealVideo();realVideoPreview=true;realVideoReview=false;
        LinearLayout root=scanShell("Video preview",this::showRealVideos),body=scanBody(root);
        android.widget.VideoView player=new android.widget.VideoView(this);activeVideoPlayer=player;
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.BLACK);frame.addView(player,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));add(body,frame,-1,dp(230));
        android.widget.MediaController controls=new android.widget.MediaController(this);controls.setAnchorView(player);player.setMediaController(controls);
        TextView play=centered("Play video",14,BLUE,600);tap(play,WHITE,12,()->{try{if(player.isPlaying()){player.pause();play.setText(tr("Play video"));}else{player.start();play.setText(tr("Pause video"));controls.show();}}catch(RuntimeException e){folderToast("Video playback unavailable");}});space(body,8);add(body,play,-1,dp(44));
        player.setOnPreparedListener(mp->{player.seekTo(1);play.setEnabled(true);});play.setEnabled(false);
        player.setOnCompletionListener(mp->{play.setText(tr("Play video"));});
        player.setOnErrorListener((mp,what,extra)->{play.setText(tr("Video playback unavailable"));play.setEnabled(false);return true;});
        space(body,15);add(body,txt(item.name,19,INK,700),-1,-2);space(body,12);
        String format=item.mime==null||item.mime.isEmpty()?tr("Unknown"):item.mime;
        String date=item.modified>0?java.text.DateFormat.getDateInstance().format(new java.util.Date(item.modified)):tr("Unknown");
        LinearLayout info=card();String[][] data={{"File size",scanBytes(item.bytes)},{"Resolution",videoResolution(item)},{"Duration",videoDuration(item.duration)},{"Format",format},{"Last modified",date}};
        for(int i=0;i<data.length;i++){if(i>0)thinRule(info);LinearLayout line=row();line.setMinimumHeight(dp(48));weight(line,txt(data[i][0],13,GRAY,400));add(line,txt(data[i][1],13,INK,600),-2,-2);add(info,line,-1,-2);}add(body,info,-1,-2);space(body,15);
        String id=item.uri.toString();scanButton(body,realVideoKeep.contains(id)?"Remove keep mark":"Keep video",false,()->{if(!realVideoKeep.remove(id)){realVideoKeep.add(id);realVideoSelected.remove(id);}showRealVideos();});space(body,8);
        if(!realVideoKeep.contains(id))scanButton(body,realVideoSelected.contains(id)?"Deselect video":"Select for cleanup",true,()->{if(!realVideoSelected.remove(id)){if(realVideoSelected.size()>=100){folderToast("Select up to 100 videos at a time");return;}realVideoSelected.add(id);}showRealVideos();});
        setContentView(root);try{player.setVideoURI(item.uri);}catch(RuntimeException e){play.setText(tr("Video playback unavailable"));play.setEnabled(false);}
    }
    private void showRealVideoReview(){
        stopRealVideo();if(scanResult==null||realVideoSelected.isEmpty()){showRealVideos();return;}realVideoReview=true;realVideoPreview=false;
        LinearLayout root=scanShell("Review cleanup",this::showRealVideos),body=scanBody(root);add(body,txt(realVideoSelected.size()+" "+tr("videos selected"),22,INK,700),-1,-2);space(body,6);add(body,txt(scanBytes(selectedVideoBytes()),24,BLUE,600),-1,-2);space(body,12);
        add(body,txt("Large videos may be important memories. Review each video before moving it to trash.",14,GRAY,400),-1,-2);space(body,12);
        LinearLayout items=card();int n=0;for(SmartMediaScan.Item i:scanResult.groups.get(1))if(realVideoSelected.contains(i.uri.toString())){
            if(n++>0)thinRule(items);LinearLayout line=row();line.setMinimumHeight(dp(64));android.widget.ImageView thumb=new android.widget.ImageView(this);thumb.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);loadDupThumb(thumb,i,128,dupThumbGeneration);add(line,thumb,dp(56),dp(48));gap(line,9);LinearLayout label=column();TextView name=txt(i.name,13,INK,500);name.setMaxLines(2);add(label,name,-1,-2);space(label,4);add(label,txt(videoDuration(i.duration),11,GRAY,400),-1,-2);weight(line,label);add(line,txt(scanBytes(i.bytes),12,GRAY,400),-2,-2);add(items,line,-1,-2);
        }add(body,items,-1,-2);space(body,12);add(body,txt("Space may not be freed until trash is emptied.",12,GRAY,400),-1,-2);
        if(android.os.Build.VERSION.SDK_INT>=30)scanButton(root,"Move to trash",true,this::recheckVideoTrash);else{add(body,txt("System trash approval requires Android 11 or later. Review only on this device.",13,GRAY,400),-1,-2);scanButton(root,"Done",true,this::showRealVideos);}setContentView(root);
    }
    private void recheckVideoTrash(){
        if(android.os.Build.VERSION.SDK_INT<30||runningScan!=null||scanResult==null||realVideoSelected.isEmpty())return;
        final SmartMediaScan.Result before=scanResult;final java.util.Set<String> selected=new java.util.HashSet<>(realVideoSelected),keep=new java.util.HashSet<>(realVideoKeep);
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);runningScan=job;
        Runnable cancel=()->{job.cancel();runningScan=null;showRealVideos();};LinearLayout root=scanShell("Review cleanup",cancel),body=scanBody(root);add(body,centered("Rechecking video safety",20,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(42),dp(42));scanButton(root,"Cancel",false,cancel);setContentView(root);
        new Thread(()->{try{
            SmartMediaScan.Result fresh=job.runVideos((stage,done,total)->{});java.util.List<android.net.Uri> approved=VideoApproval.verify(before,fresh,selected,keep);
            runOnUiThread(()->{if(runningScan!=job||isDestroyed()||isFinishing())return;runningScan=null;
                if(approved.isEmpty()){java.util.Arrays.fill(scanSelected,false);dupCache.evictAll();scanResult=fresh;realVideoSelected.clear();showRealVideos();folderToast("Files or protection changed. Review again.");return;}
                try{android.app.PendingIntent pending=android.provider.MediaStore.createTrashRequest(getContentResolver(),approved,true);cleanupReceipt=CleanupReceipt.capture(this,"Large videos",approved,fresh.groups.get(1));dupTrashPending=true;startIntentSenderForResult(pending.getIntentSender(),REQUEST_VIDEO_TRASH,null,0,0,0);}
                catch(Exception e){dupTrashPending=false;cleanupReceipt=null;showRealVideos();folderToast("Could not open Android trash approval");}
            });
        }catch(Exception e){runOnUiThread(()->{if(runningScan!=job||isDestroyed())return;runningScan=null;showRealVideos();folderToast("Safety recheck failed");});}},"Clenova-video-recheck").start();
    }

    private void openRealScreenshots(boolean fromResults){
        shotFromResults=fromResults;shotPage=0;shotFilter=0;shotSort=0;shotSelected.clear();shotKeep.clear();
        if(scanResult!=null&&scanResult.imagesScanned){showRealScreenshots();return;}
        shotAfterScan=true;dupAfterScan=false;similarAfterScan=false;videoAfterScan=false;requestSmartScan();
    }
    private void leaveRealScreenshots(){
        realShotVisible=false;realShotReview=false;realShotPreview=false;nextThumbGeneration();shotSelected.clear();shotKeep.clear();
        if(shotFromResults&&scanResult!=null)showScanResults();else if(agentTaskOrigin)showAgent();else showClean();
    }
    private long selectedShotBytes(){long bytes=0;if(scanResult!=null)for(SmartMediaScan.Item i:scanResult.groups.get(2))if(shotSelected.contains(i.uri.toString()))bytes+=i.bytes;return bytes;}
    private void shotFooter(TextView count,TextView amount,TextView action){
        count.setText(shotSelected.size()+" "+tr("screenshots selected"));amount.setText(scanBytes(selectedShotBytes()));
        action.setEnabled(!shotSelected.isEmpty());action.setAlpha(shotSelected.isEmpty()?.45f:1f);
    }
    private String shotMonth(SmartMediaScan.Item item){
        if(item.modified<=0)return tr("Unknown date");
        return new java.text.SimpleDateFormat("MMMM yyyy",java.util.Locale.forLanguageTag(prefs.language())).format(new java.util.Date(item.modified));
    }
    private void showRealScreenshots(){
        if(scanResult==null){showClean();return;}
        realShotVisible=true;realShotReview=false;realShotPreview=false;realVideoVisible=false;realSimilarVisible=false;realDupVisible=false;scanVisible=false;scanReviewVisible=false;
        final int generation=nextThumbGeneration();
        LinearLayout root=scanShell("Screenshots",this::leaveRealScreenshots),body=scanBody(root);
        LinearLayout hero=card(),summary=row(),labels=column();
        add(labels,txt("Screenshot storage",13,GRAY,400),-1,-2);space(labels,6);add(labels,txt(scanBytes(scanResult.bytes(2)),32,INK,700),-1,-2);space(labels,5);
        add(labels,txt(scanResult.groups.get(2).size()+" "+tr("screenshots available for review"),12,GRAY,400),-1,-2);weight(summary,labels);gap(summary,10);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,18));mark.addView(new Glyph(this,"screenshot",BLUE),new FrameLayout.LayoutParams(dp(33),dp(33),Gravity.CENTER));add(summary,mark,dp(64),dp(64));add(hero,summary,-1,-2);space(hero,12);thinRule(hero);space(hero,10);
        LinearLayout safe=row();add(safe,new Glyph(this,"shield",GREEN),dp(19),dp(19));gap(safe,8);weight(safe,txt("Protected items excluded",13,GREEN,600));add(hero,safe,-1,-2);add(body,hero,-1,-2);space(body,12);
        add(body,txt("Screenshots may contain important information. Review before selecting.",12,GRAY,400),-1,-2);space(body,12);
        LinearLayout filters=row();String[] names={"All","Older than 30d","Older than 90d"};
        for(int j=0;j<3;j++){final int index=j;if(j>0)gap(filters,5);TextView chip=centered(names[j],11,shotFilter==j?Color.WHITE:INK,600);chip.setPadding(dp(3),0,dp(3),0);
            tap(chip,shotFilter==j?BLUE:WHITE,18,()->{if(shotFilter!=index){shotFilter=index;shotPage=0;shotSelected.clear();showRealScreenshots();}});filters.addView(chip,new LinearLayout.LayoutParams(0,dp(42),1f));}
        add(body,filters,-1,-2);space(body,12);
        java.util.List<SmartMediaScan.Item> visible=new java.util.ArrayList<>();
        long threshold=System.currentTimeMillis()-(shotFilter==2?90L:30L)*86400000L;
        for(SmartMediaScan.Item item:scanResult.groups.get(2))if(shotFilter==0||(item.modified>0&&item.modified<threshold))visible.add(item);
        java.util.Comparator<SmartMediaScan.Item> cmp=shotSort==1?java.util.Comparator.comparingLong((SmartMediaScan.Item i)->i.modified<=0?Long.MAX_VALUE:i.modified):
            shotSort==2?java.util.Comparator.comparingLong((SmartMediaScan.Item i)->i.bytes).reversed():java.util.Comparator.comparingLong((SmartMediaScan.Item i)->i.modified).reversed();
        visible.sort(cmp.thenComparing(i->i.uri.toString()));
        LinearLayout heading=row();weight(heading,txt("Your screenshots",17,INK,600));
        int eligible=0;for(SmartMediaScan.Item i:visible)if(!shotKeep.contains(i.uri.toString()))eligible++;
        TextView select=txt(shotSelected.isEmpty()?(eligible>100?"Select 100":"Select all"):"Deselect all",12,BLUE,600);
        tap(select,BG,8,()->{if(!shotSelected.isEmpty())shotSelected.clear();else for(SmartMediaScan.Item i:visible){if(shotSelected.size()>=ScreenshotApproval.MAX_BATCH)break;if(!shotKeep.contains(i.uri.toString()))shotSelected.add(i.uri.toString());}showRealScreenshots();});add(heading,select,-2,dp(44));add(body,heading,-1,-2);
        String[] sorts={"Newest first","Oldest first","Largest first"};TextView sort=centered(tr(sorts[shotSort])+"  ▾",13,BLUE,600);
        tap(sort,WHITE,12,()->privacyDialog().setTitle(tr("Sort screenshots")).setSingleChoiceItems(new String[]{tr(sorts[0]),tr(sorts[1]),tr(sorts[2])},shotSort,(d,i)->{shotSort=i;shotPage=0;d.dismiss();showRealScreenshots();}).setNegativeButton(tr("Cancel"),null).show());add(body,sort,-1,dp(42));space(body,12);
        TextView count=txt("",12,GRAY,400),amount=txt("",22,INK,700),review=centered("Review & clean",14,Color.WHITE,600);
        int pages=Math.max(1,(visible.size()+23)/24);shotPage=Math.max(0,Math.min(shotPage,pages-1));
        if(visible.isEmpty()){LinearLayout empty=card();add(empty,centered(scanResult.imagesScanned?"No screenshots to review":"Photo access needed",17,INK,600),-1,-2);space(empty,8);add(empty,centered("Only accessible screenshot folders are checked. Protected photos and known duplicate groups are excluded.",13,GRAY,400),-1,-2);add(body,empty,-1,-2);}
        int start=shotPage*24,end=Math.min(visible.size(),start+24);
        while(start<end){
            String month=shotMonth(visible.get(start));int groupEnd=start+1;
            if(shotSort==2){groupEnd=end;}else while(groupEnd<end&&shotMonth(visible.get(groupEnd)).equals(month))groupEnd++;
            add(body,txt(shotSort==2?"Sorted by size":month,12,GRAY,600),-1,-2);space(body,8);
            for(int n=start;n<groupEnd;n+=3){LinearLayout row=row();row.setGravity(Gravity.TOP);
                for(int col=0;col<3;col++){if(col>0)gap(row,7);int i=n+col;
                    if(i>=groupEnd){row.addView(new View(this),new LinearLayout.LayoutParams(0,1,1f));continue;}
                    row.addView(shotTile(visible.get(i),generation,()->shotFooter(count,amount,review)),new LinearLayout.LayoutParams(0,-2,1f));}
                add(body,row,-1,-2);space(body,8);
            }start=groupEnd;space(body,8);
        }
        if(pages>1){LinearLayout pager=row();TextView prev=centered("Previous",13,BLUE,600),next=centered("Next",13,BLUE,600);tap(prev,WHITE,12,()->{shotPage--;showRealScreenshots();});prev.setEnabled(shotPage>0);prev.setAlpha(shotPage>0?1:.4f);tap(next,WHITE,12,()->{shotPage++;showRealScreenshots();});next.setEnabled(shotPage+1<pages);next.setAlpha(shotPage+1<pages?1:.4f);weight(pager,prev);weight(pager,centered((shotPage+1)+" / "+pages,13,GRAY,400));weight(pager,next);add(body,pager,-1,dp(48));}
        if(scanResult.partial||scanResult.limited){space(body,8);add(body,txt("Partial scan results",12,GRAY,500),-1,-2);}
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);tap(review,BLUE,24,this::showShotReview);review.setPadding(dp(13),0,dp(13),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);space(root,7);add(root,centered("Keep what's important. Clear the rest.",12,GRAY,400),-1,-2);shotFooter(count,amount,review);setContentView(root);
    }
    private LinearLayout shotTile(SmartMediaScan.Item item,int generation,Runnable changed){
        LinearLayout tile=column();tile.setBackground(round(WHITE,12));tile.setClipToOutline(true);String id=item.uri.toString();
        android.widget.ImageView image=new android.widget.ImageView(this);image.setBackgroundColor(PALE);image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);image.setContentDescription(item.name);image.setOnClickListener(v->showShotPreview(item));add(tile,image,-1,dp(128));loadDupThumb(image,item,384,generation);
        TextView select=centered("",11,GRAY,600);add(tile,select,-1,dp(44));boolean locked=shotKeep.contains(id);
        Runnable refresh=()->{boolean picked=shotSelected.contains(id);select.setText(locked?tr("Keep"):(picked?"✓ ":"○ ")+scanBytes(item.bytes));select.setTextColor(locked?GREEN:picked?Color.WHITE:INK);select.setBackground(round(locked?surface(0xffe8f6ec):picked?BLUE:WHITE,8));select.setContentDescription(item.name+", "+tr(locked?"Keep":picked?"Selected":"Not selected"));};
        select.setOnClickListener(v->{if(locked)return;if(!shotSelected.remove(id)){if(shotSelected.size()>=100){folderToast("Select up to 100 screenshots at a time");return;}shotSelected.add(id);}refresh.run();changed.run();});select.setEnabled(!locked);refresh.run();return tile;
    }
    private void showShotPreview(SmartMediaScan.Item item){
        if(scanResult==null){showClean();return;}realShotPreview=true;realShotReview=false;final int generation=nextThumbGeneration();
        LinearLayout root=scanShell("Screenshot preview",this::showRealScreenshots);
        FrameLayout viewer=new FrameLayout(this);viewer.setBackgroundColor(dark?0xff09090b:0xffe7e7ec);viewer.setClipChildren(true);
        android.widget.ImageView image=new android.widget.ImageView(this);image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);image.setContentDescription(item.name);
        viewer.addView(image,new FrameLayout.LayoutParams(-1,-1));root.addView(viewer,new LinearLayout.LayoutParams(-1,0,1f));loadDupThumb(image,item,1536,generation);
        final float[] zoom={1f},last={0,0};
        android.view.ScaleGestureDetector pinch=new android.view.ScaleGestureDetector(this,new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(android.view.ScaleGestureDetector d){zoom[0]=Math.max(1f,Math.min(4f,zoom[0]*d.getScaleFactor()));image.setScaleX(zoom[0]);image.setScaleY(zoom[0]);if(zoom[0]==1){image.setTranslationX(0);image.setTranslationY(0);}return true;}
        });
        viewer.setOnTouchListener((v,event)->{pinch.onTouchEvent(event);if(event.getPointerCount()==1){if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){last[0]=event.getX();last[1]=event.getY();}
            else if(event.getActionMasked()==android.view.MotionEvent.ACTION_MOVE&&!pinch.isInProgress()&&zoom[0]>1){float maxX=viewer.getWidth()*(zoom[0]-1)/2,maxY=viewer.getHeight()*(zoom[0]-1)/2;image.setTranslationX(Math.max(-maxX,Math.min(maxX,image.getTranslationX()+event.getX()-last[0])));image.setTranslationY(Math.max(-maxY,Math.min(maxY,image.getTranslationY()+event.getY()-last[1])));last[0]=event.getX();last[1]=event.getY();}}return true;});
        space(root,8);LinearLayout controls=row();for(String label:new String[]{"Zoom out","Reset zoom","Zoom in"}){TextView control=centered(label,12,BLUE,600);tap(control,WHITE,12,()->{zoom[0]=label.equals("Reset zoom")?1:Math.max(1f,Math.min(4f,zoom[0]+(label.equals("Zoom in")?.5f:-.5f)));image.setScaleX(zoom[0]);image.setScaleY(zoom[0]);image.setTranslationX(0);image.setTranslationY(0);});controls.addView(control,new LinearLayout.LayoutParams(0,dp(44),1f));}add(root,controls,-1,-2);
        space(root,8);TextView name=txt(item.name,14,INK,600);name.setMaxLines(2);add(root,name,-1,-2);space(root,5);
        add(root,txt(scanBytes(item.bytes)+" · "+shotMonth(item),12,GRAY,400),-1,-2);space(root,8);
        String id=item.uri.toString();scanButton(root,shotKeep.contains(id)?"Remove keep mark":"Keep screenshot",false,()->{if(!shotKeep.remove(id)){shotKeep.add(id);shotSelected.remove(id);}showRealScreenshots();});space(root,6);
        if(!shotKeep.contains(id))scanButton(root,shotSelected.contains(id)?"Deselect screenshot":"Select for cleanup",true,()->{if(!shotSelected.remove(id)){if(shotSelected.size()>=100){folderToast("Select up to 100 screenshots at a time");return;}shotSelected.add(id);}showRealScreenshots();});
        setContentView(root);
    }
    private void showShotReview(){
        if(scanResult==null||shotSelected.isEmpty()){showRealScreenshots();return;}realShotReview=true;realShotPreview=false;
        LinearLayout root=scanShell("Review cleanup",this::showRealScreenshots),body=scanBody(root);
        add(body,txt(shotSelected.size()+" "+tr("screenshots selected"),22,INK,700),-1,-2);space(body,6);add(body,txt(scanBytes(selectedShotBytes()),24,BLUE,600),-1,-2);space(body,12);
        add(body,txt("Check receipts, documents and other important information before moving screenshots to trash.",14,GRAY,400),-1,-2);space(body,12);
        LinearLayout items=card();int n=0;for(SmartMediaScan.Item i:scanResult.groups.get(2))if(shotSelected.contains(i.uri.toString())){
            if(n++>0)thinRule(items);LinearLayout line=row();line.setMinimumHeight(dp(64));android.widget.ImageView thumb=new android.widget.ImageView(this);thumb.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);loadDupThumb(thumb,i,128,dupThumbGeneration);add(line,thumb,dp(48),dp(56));gap(line,9);TextView name=txt(i.name,13,INK,500);name.setMaxLines(2);weight(line,name);gap(line,6);add(line,txt(scanBytes(i.bytes),12,GRAY,400),-2,-2);add(items,line,-1,-2);
        }add(body,items,-1,-2);space(body,12);add(body,txt("Space may not be freed until trash is emptied.",12,GRAY,400),-1,-2);
        if(android.os.Build.VERSION.SDK_INT>=30)scanButton(root,"Move to trash",true,this::recheckShotTrash);else{add(body,txt("System trash approval requires Android 11 or later. Review only on this device.",13,GRAY,400),-1,-2);scanButton(root,"Done",true,this::showRealScreenshots);}setContentView(root);
    }
    private void recheckShotTrash(){
        if(android.os.Build.VERSION.SDK_INT<30||runningScan!=null||scanResult==null||shotSelected.isEmpty())return;
        final SmartMediaScan.Result before=scanResult;final java.util.Set<String> selected=new java.util.HashSet<>(shotSelected),keep=new java.util.HashSet<>(shotKeep);
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);runningScan=job;
        Runnable cancel=()->{job.cancel();runningScan=null;showRealScreenshots();};LinearLayout root=scanShell("Review cleanup",cancel),body=scanBody(root);add(body,centered("Rechecking screenshot safety",20,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(42),dp(42));scanButton(root,"Cancel",false,cancel);setContentView(root);
        new Thread(()->{try{
            SmartMediaScan.Result fresh=job.runPhotos((stage,done,total)->{});java.util.List<android.net.Uri> approved=ScreenshotApproval.verify(before,fresh,selected,keep);
            runOnUiThread(()->{if(runningScan!=job||isDestroyed()||isFinishing())return;runningScan=null;
                if(approved.isEmpty()){java.util.Arrays.fill(scanSelected,false);dupCache.evictAll();scanResult=fresh;shotSelected.clear();showRealScreenshots();folderToast("Files or protection changed. Review again.");return;}
                try{android.app.PendingIntent pending=android.provider.MediaStore.createTrashRequest(getContentResolver(),approved,true);cleanupReceipt=CleanupReceipt.capture(this,"Screenshots",approved,fresh.groups.get(2));dupTrashPending=true;startIntentSenderForResult(pending.getIntentSender(),REQUEST_SCREENSHOT_TRASH,null,0,0,0);}
                catch(Exception e){dupTrashPending=false;cleanupReceipt=null;showRealScreenshots();folderToast("Could not open Android trash approval");}
            });
        }catch(Exception e){runOnUiThread(()->{if(runningScan!=job||isDestroyed())return;runningScan=null;showRealScreenshots();folderToast("Safety recheck failed");});}},"Clenova-screenshot-recheck").start();
    }

    private DownloadFiles downloadEngine(){return new DownloadFiles(this,prefs,protectedFiles,protectedFolders,largeFilesMode,clutterMode);}
    private void openLargeFiles(){openFileManager(true,false);}
    private void openDownloads(){openFileManager(false,false);}
    private void openClutter(){openFileManager(false,true);}
    private void openFileManager(boolean large,boolean clutter){
        clutterMode=clutter;largeFilesMode=large;largeFileThreshold=0;
        if(cleanScroll!=null){cleanScrollY=cleanScroll.getScrollY();cleanScroll=null;}
        downloadsVisible=true;downloadsReview=false;downloadsFilter=-1;downloadsSort=large?1:0;downloadsPage=0;downloadsQuery="";downloadSelected.clear();downloadKeep.clear();downloadItems.clear();scanResult=null;showDownloads();
    }
    private void pickDownloads(){
        if(downloadsBusy||downloadsPicking)return;
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("*/*");
        intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE,true);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(!largeFilesMode&&android.os.Build.VERSION.SDK_INT>=26)intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
        try{downloadsPicking=true;startActivityForResult(intent,PICK_DOWNLOAD_FILES);}catch(RuntimeException e){downloadsPicking=false;folderToast("File picker unavailable");}
    }
    private void pickLargeFolder(){
        if(downloadsBusy||downloadsPicking)return;
        android.net.Uri saved=new SetupFolderAccess(this).current();
        if(saved!=null){
            privacyDialog().setTitle("Scan a folder").setItems(new String[]{"Use saved folder","Choose another folder"},(dialog,which)->{
                if(which==0){android.net.Uri fresh=new SetupFolderAccess(this).current();if(fresh!=null)loadLargeFolder(fresh);else chooseLargeFolder();}
                else chooseLargeFolder();
            }).setNegativeButton("Cancel",null).show();return;
        }
        chooseLargeFolder();
    }
    private void chooseLargeFolder(){
        if(downloadsBusy||downloadsPicking)return;
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{downloadsPicking=true;startActivityForResult(intent,PICK_LARGE_FOLDER);}catch(RuntimeException e){downloadsPicking=false;folderToast("File picker unavailable");}
    }
    private void loadLargeFolder(android.net.Uri tree){
        if(downloadsSignal!=null)downloadsSignal.cancel();
        final android.os.CancellationSignal signal=new android.os.CancellationSignal();downloadsSignal=signal;
        final int generation=++downloadsGeneration;final DownloadFiles engine=downloadEngine();
        final java.util.List<android.net.Uri> existing=new java.util.ArrayList<>();for(DownloadFiles.Item i:downloadItems)existing.add(i.uri);
        LinearLayout root=scanShell(fileManagerTitle(),this::leaveAgentTool),body=scanBody(root);add(body,centered("Scanning selected folder",20,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(40),dp(40));setContentView(root);
        new Thread(()->{try{
            java.util.LinkedHashSet<android.net.Uri> uris=new java.util.LinkedHashSet<>(existing);for(android.net.Uri uri:engine.largeFolderUris(tree,signal)){if(uris.size()>=DownloadFiles.LIMIT)break;uris.add(uri);}
            java.util.List<DownloadFiles.Item> result=engine.readAll(uris,signal);
            runOnUiThread(()->{if(generation!=downloadsGeneration||isDestroyed())return;downloadsSignal=null;downloadItems.clear();downloadItems.addAll(result);downloadSelected.clear();downloadsPage=0;showDownloads();folderToast("Folder scan limited to accessible files");});
        }catch(Exception e){runOnUiThread(()->{if(generation!=downloadsGeneration||isDestroyed())return;downloadsSignal=null;showDownloads();folderToast("Could not read selected folder");});}},"Clenova-large-folder").start();
    }
    private void loadPickedDownloads(android.content.Intent data){
        java.util.LinkedHashSet<android.net.Uri> uris=new java.util.LinkedHashSet<>();
        for(DownloadFiles.Item item:downloadItems)uris.add(item.uri);
        if(data.getData()!=null)uris.add(data.getData());
        android.content.ClipData clip=data.getClipData();if(clip!=null)for(int i=0;i<clip.getItemCount()&&i<DownloadFiles.LIMIT;i++){android.net.Uri uri=clip.getItemAt(i).getUri();if(uri!=null)uris.add(uri);}
        readDownloads(uris);
    }
    private void refreshDownloads(){
        java.util.List<android.net.Uri> uris=new java.util.ArrayList<>();for(DownloadFiles.Item i:downloadItems)uris.add(i.uri);readDownloads(uris);
    }
    private void readDownloads(java.util.Collection<android.net.Uri> uris){
        if(downloadsSignal!=null)downloadsSignal.cancel();final android.os.CancellationSignal signal=new android.os.CancellationSignal();downloadsSignal=signal;
        final int generation=++downloadsGeneration;downloadsVisible=true;downloadsReview=false;downloadSelected.clear();downloadsPage=0;
        LinearLayout root=scanShell(fileManagerTitle(),this::leaveAgentTool),body=scanBody(root);add(body,centered("Reading selected files",20,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(40),dp(40));setContentView(root);
        final DownloadFiles engine=downloadEngine();
        new Thread(()->{try{
            java.util.List<DownloadFiles.Item> result=engine.readAll(uris,signal);
            runOnUiThread(()->{if(generation!=downloadsGeneration||isDestroyed())return;downloadsSignal=null;downloadItems.clear();downloadItems.addAll(result);showDownloads();if(result.size()<uris.size())folderToast(clutterMode?"Some selected files did not match review categories":largeFilesMode?"Some files were unavailable, too small or outside supported locations":"Some files were unavailable or outside supported Downloads locations");});
        }catch(Exception e){runOnUiThread(()->{if(generation!=downloadsGeneration||isDestroyed())return;downloadsSignal=null;showDownloads();folderToast("Could not read selected files");});}},"Clenova-downloads-read").start();
    }
    private long downloadBytes(int category){long bytes=0;for(DownloadFiles.Item i:downloadItems)if((category<0||i.type==category)&&i.bytes>0)bytes+=i.bytes;return bytes;}
    private String downloadSize(DownloadFiles.Item i){return i.bytes>=0?scanBytes(i.bytes):tr("Unknown");}
    private String downloadType(DownloadFiles.Item i){int dot=i.name.lastIndexOf('.');return dot>=0&&dot+1<i.name.length()?i.name.substring(dot+1).toUpperCase(java.util.Locale.ROOT):i.mime;}
    private String downloadDate(DownloadFiles.Item i){return i.modified>0?java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM,java.util.Locale.forLanguageTag(prefs.language())).format(new java.util.Date(i.modified)):tr("Unknown date");}
    private int downloadColor(int type){return type==0?GREEN:type==1?0xffff9500:type==2?BLUE:GRAY;}
    private String downloadGlyph(int type){return type==0?"apps":type==1?"folder":"document";}
    private boolean downloadEligible(DownloadFiles.Item i){return i.canDelete&&!downloadKeep.contains(i.uri.toString());}
    private void downloadFooter(TextView count,TextView amount,TextView action){
        long bytes=0;for(DownloadFiles.Item i:downloadItems)if(downloadSelected.contains(i.uri.toString())&&i.bytes>0)bytes+=i.bytes;
        count.setText(downloadSelected.size()+" "+tr("selected"));amount.setText(scanBytes(bytes));action.setEnabled(!downloadSelected.isEmpty());action.setAlpha(downloadSelected.isEmpty()?.45f:1f);
    }
    private void showDownloads(){
        downloadsVisible=true;downloadsReview=false;
        if(clutterMode){showClutterScreen();return;}
        LinearLayout root=scanShell(fileManagerTitle(),this::leaveAgentTool),body=scanBody(root);
        LinearLayout hero=card(),summary=row(),labels=column();add(labels,txt(largeFilesMode?"Storage occupied":"Accessible downloads storage",13,GRAY,400),-1,-2);space(labels,6);
        add(labels,txt(downloadItems.isEmpty()?"—":scanBytes(downloadBytes(-1)),32,INK,700),-1,-2);space(labels,5);add(labels,txt(downloadItems.size()+" "+tr("files available for review"),12,GRAY,400),-1,-2);weight(summary,labels);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,32));mark.addView(new Glyph(this,largeFilesMode?"document":"folder_down",BLUE),new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER));add(summary,mark,dp(76),dp(76));add(hero,summary,-1,-2);space(hero,12);
        scanButton(hero,largeFilesMode?"Choose files":downloadItems.isEmpty()?"Choose downloaded files":"Add downloaded files",true,this::pickDownloads);
        if(largeFilesMode){space(hero,7);scanButton(hero,"Scan selected folder",false,this::pickLargeFolder);}
        add(body,hero,-1,-2);space(body,10);
        add(body,txt(largeFilesMode?"Only selected files and accessible folders are checked. Folder scan results are read-only.":"Only files you choose in Android Files are listed. Some files may be read-only.",12,GRAY,400),-1,-2);space(body,13);
        if(largeFilesMode){
            LinearLayout filters=row();String[] sizes={"100 MiB+","500 MiB+","1 GiB+"};
            for(int j=0;j<3;j++){final int index=j;if(j>0)gap(filters,7);TextView chip=centered(sizes[j],12,largeFileThreshold==j?Color.WHITE:INK,600);
                tap(chip,largeFileThreshold==j?BLUE:WHITE,20,()->{if(largeFileThreshold!=index){largeFileThreshold=index;downloadSelected.clear();downloadsPage=0;showDownloads();}});filters.addView(chip,new LinearLayout.LayoutParams(0,dp(44),1f));}
            add(body,filters,-1,-2);space(body,12);
        }else{
        LinearLayout cats=row();String[] categories={"APK","Archives","Docs"};for(int i=0;i<3;i++){final int category=i;if(i>0)gap(cats,7);LinearLayout tile=column();pad(tile,10,12);add(tile,new Glyph(this,downloadGlyph(i),downloadColor(i)),dp(24),dp(24));space(tile,8);add(tile,txt(categories[i],13,INK,600),-1,-2);space(tile,4);add(tile,txt(scanBytes(downloadBytes(i)),11,GRAY,400),-1,-2);tap(tile,downloadsFilter==i?PALE:WHITE,15,()->{downloadsFilter=downloadsFilter==category?-1:category;downloadsPage=0;downloadSelected.clear();showDownloads();});cats.addView(tile,new LinearLayout.LayoutParams(0,-2,1f));}add(body,cats,-1,-2);space(body,13);}
        LinearLayout search=row();search.setBackground(round(surface(0xffe8e8ed),12));pad(search,10,3);add(search,new Glyph(this,"search",GRAY),dp(20),dp(20));gap(search,8);
        android.widget.EditText input=new android.widget.EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setTextColor(INK);input.setHintTextColor(GRAY);input.setHint(tr(largeFilesMode?"Search files":"Search downloads"));input.setBackgroundColor(Color.TRANSPARENT);input.setText(downloadsQuery);input.setPadding(0,0,0,0);search.addView(input,new LinearLayout.LayoutParams(0,dp(42),1f));add(body,search,-1,-2);space(body,12);
        LinearLayout listHost=column();add(body,listHost,-1,-2);
        TextView count=txt("",12,GRAY,400),amount=txt("",22,INK,700),review=centered("Review & clean",14,Color.WHITE,600);
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);tap(review,BLUE,24,this::showDownloadReview);review.setPadding(dp(13),0,dp(13),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);space(root,7);add(root,centered("Nothing removed without your approval.",12,GRAY,400),-1,-2);
        Runnable render=()->renderDownloadList(listHost,count,amount,review);
        input.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence q,int start,int n,int after){}public void onTextChanged(CharSequence q,int start,int before,int n){downloadsQuery=q.toString();downloadsPage=0;downloadSelected.clear();render.run();}public void afterTextChanged(android.text.Editable e){}});
        render.run();root.setFocusableInTouchMode(true);root.requestFocus();setContentView(root);
    }
    private void renderDownloadList(LinearLayout host,TextView count,TextView amount,TextView action){
        host.removeAllViews();java.util.List<DownloadFiles.Item> visible=new java.util.ArrayList<>();String query=downloadsQuery.trim().toLowerCase(java.util.Locale.ROOT);
        for(DownloadFiles.Item i:downloadItems)if((!largeFilesMode||i.bytes>=largeMinimum())&&(downloadsFilter<0||(clutterMode?ClutterRules.category(i.name,i.mime):i.type)==downloadsFilter)&&(i.name+" "+i.mime).toLowerCase(java.util.Locale.ROOT).contains(query))visible.add(i);
        java.util.Comparator<DownloadFiles.Item> cmp=downloadsSort==1?java.util.Comparator.comparingLong((DownloadFiles.Item i)->i.bytes).reversed():downloadsSort==2?java.util.Comparator.comparing((DownloadFiles.Item i)->i.name,String.CASE_INSENSITIVE_ORDER):java.util.Comparator.comparingLong((DownloadFiles.Item i)->i.modified).reversed();visible.sort(cmp.thenComparing(i->i.id));
        LinearLayout heading=row(),label=column();add(label,txt(clutterMode?(downloadsFilter<0?"All review files":ClutterRules.LABELS[downloadsFilter]):largeFilesMode?"Large files":downloadsFilter<0?"All files":new String[]{"APK","Archives","Docs"}[downloadsFilter],17,INK,600),-1,-2);space(label,4);long visibleBytes=0,maxBytes=1;for(DownloadFiles.Item file:visible){visibleBytes+=Math.max(0,file.bytes);maxBytes=Math.max(maxBytes,file.bytes);}final long barMax=maxBytes;
        add(label,txt(visible.size()+" "+tr("files")+(largeFilesMode?" · "+scanBytes(visibleBytes):""),12,GRAY,400),-1,-2);weight(heading,label);
        int eligible=0;for(DownloadFiles.Item i:visible)if(downloadEligible(i))eligible++;
        TextView select=txt(downloadSelected.isEmpty()?(eligible>DownloadFiles.DELETE_LIMIT?"Select 50":"Select all"):"Deselect all",12,BLUE,600);tap(select,BG,8,()->{if(!downloadSelected.isEmpty())downloadSelected.clear();else for(DownloadFiles.Item i:visible){if(downloadSelected.size()>=DownloadFiles.DELETE_LIMIT)break;if(downloadEligible(i))downloadSelected.add(i.uri.toString());}renderDownloadList(host,count,amount,action);});select.setEnabled(eligible>0||!downloadSelected.isEmpty());add(heading,select,-2,dp(44));gap(heading,8);
        FrameLayout sort=new FrameLayout(this);sort.addView(new Glyph(this,"more",BLUE),new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));sort.setContentDescription(tr("Sort files"));tap(sort,WHITE,18,()->privacyDialog().setTitle(tr("Sort files")).setSingleChoiceItems(new String[]{tr("Newest first"),tr("Largest first"),tr("Name")},downloadsSort,(d,i)->{downloadsSort=i;downloadsPage=0;d.dismiss();renderDownloadList(host,count,amount,action);}).setNegativeButton(tr("Cancel"),null).show());add(heading,sort,dp(38),dp(38));add(host,heading,-1,-2);space(host,7);
        LinearLayout list=card();pad(list,12,5);int pages=Math.max(1,(visible.size()+19)/20);downloadsPage=Math.max(0,Math.min(downloadsPage,pages-1));
        if(visible.isEmpty()){pad(list,14,20);add(list,centered(downloadItems.isEmpty()?"Choose files to get started":"No matching files",15,INK,600),-1,-2);space(list,6);add(list,centered(downloadItems.isEmpty()?((largeFilesMode||clutterMode)?"Choose files or scan an accessible folder.":"Choose files from Downloads in the system picker."):"Try a different search or filter.",12,GRAY,400),-1,-2);}
        for(int n=downloadsPage*20;n<Math.min(visible.size(),(downloadsPage+1)*20);n++){
            DownloadFiles.Item i=visible.get(n);if(n>downloadsPage*20)thinRule(list);LinearLayout line=row();line.setMinimumHeight(dp(66));line.setPadding(0,dp(8),0,dp(8));
            FrameLayout icon=new FrameLayout(this);icon.setBackground(round(BG,10));icon.addView(new Glyph(this,downloadGlyph(i.type),downloadColor(i.type)),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));add(line,icon,dp(40),dp(40));gap(line,9);
            LinearLayout details=column();TextView name=txt(i.name,13,INK,600);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(details,name,-1,-2);space(details,4);add(details,txt((clutterMode?tr(ClutterRules.REASONS[ClutterRules.category(i.name,i.mime)]):downloadType(i))+" · "+downloadDate(i),11,GRAY,400),-1,-2);
            if(!downloadEligible(i)){space(details,3);add(details,txt(i.protectedItem||downloadKeep.contains(i.uri.toString())?"Keep":"Read-only",10,GREEN,500),-1,-2);}
            tap(details,WHITE,8,()->showDownloadSheet(i));weight(line,details);gap(line,5);add(line,txt(downloadSize(i),11,INK,600),-2,-2);
            android.widget.CheckBox check=new android.widget.CheckBox(this);check.setButtonTintList(ColorStateList.valueOf(BLUE));check.setChecked(downloadSelected.contains(i.uri.toString()));check.setEnabled(downloadEligible(i));check.setContentDescription(i.name);check.setOnCheckedChangeListener((v,checked)->{String id=i.uri.toString();if(checked&&downloadSelected.size()>=DownloadFiles.DELETE_LIMIT&&!downloadSelected.contains(id)){v.setChecked(false);folderToast("Select up to 50 files at a time");return;}if(checked)downloadSelected.add(id);else downloadSelected.remove(id);downloadFooter(count,amount,action);});add(line,check,dp(42),dp(42));add(list,line,-1,-2);
            if(largeFilesMode){android.widget.ProgressBar progress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgress((int)Math.max(0,Math.min(1000,i.bytes*1000d/barMax)));progress.setProgressTintList(ColorStateList.valueOf(downloadColor(i.type)));progress.setProgressBackgroundTintList(ColorStateList.valueOf(surface(0xffededf1)));progress.setContentDescription(downloadSize(i));add(list,progress,-1,dp(5));space(list,8);}
        }add(host,list,-1,-2);
        if(pages>1){space(host,8);LinearLayout pager=row();TextView prev=centered("Previous",13,BLUE,600),next=centered("Next",13,BLUE,600);tap(prev,WHITE,12,()->{downloadsPage--;renderDownloadList(host,count,amount,action);});prev.setEnabled(downloadsPage>0);prev.setAlpha(downloadsPage>0?1:.4f);tap(next,WHITE,12,()->{downloadsPage++;renderDownloadList(host,count,amount,action);});next.setEnabled(downloadsPage+1<pages);next.setAlpha(downloadsPage+1<pages?1:.4f);weight(pager,prev);weight(pager,centered((downloadsPage+1)+" / "+pages,13,GRAY,400));weight(pager,next);add(host,pager,-1,dp(48));}
        downloadFooter(count,amount,action);
    }
    private void showDownloadSheet(DownloadFiles.Item item){
        if(downloadSheet!=null)downloadSheet.dismiss();android.app.Dialog dialog=new android.app.Dialog(this);downloadSheet=dialog;dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout sheet=column();GradientDrawable bg=new GradientDrawable();bg.setColor(WHITE);float r=dp(26);bg.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});sheet.setBackground(bg);pad(sheet,20,12);
        FrameLayout handle=new FrameLayout(this);View bar=new View(this);bar.setBackground(round(0xffc7c7cc,4));handle.addView(bar,new FrameLayout.LayoutParams(dp(36),dp(4),Gravity.CENTER));add(sheet,handle,-1,dp(22));space(sheet,7);
        LinearLayout title=row();FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,15));mark.addView(new Glyph(this,downloadGlyph(item.type),downloadColor(item.type)),new FrameLayout.LayoutParams(dp(28),dp(28),Gravity.CENTER));add(title,mark,dp(58),dp(58));gap(title,12);LinearLayout labels=column();TextView name=txt(item.name,17,INK,700);name.setMaxLines(3);add(labels,name,-1,-2);space(labels,5);add(labels,txt(downloadSize(item)+" · "+downloadType(item),13,GRAY,400),-1,-2);weight(title,labels);add(sheet,title,-1,-2);space(sheet,14);thinRule(sheet);
        for(String[] data:new String[][]{{"Location",item.verifiedLocation?((largeFilesMode||clutterMode)?item.id:tr("Downloads")):tr("Downloads provider · location unverified")},{"Modified",downloadDate(item)},{"Access",item.canDelete?tr("Read and remove"):tr("Read-only")}}){LinearLayout line=row();line.setMinimumHeight(dp(42));weight(line,txt(data[0],13,GRAY,400));TextView value=txt(data[1],12,INK,500);value.setMaxLines(2);line.addView(value,new LinearLayout.LayoutParams(0,-2,1f));add(sheet,line,-1,-2);}thinRule(sheet);space(sheet,10);
        if(clutterMode){space(sheet,8);add(sheet,txt(ClutterRules.REASONS[ClutterRules.category(item.name,item.mime)],13,GRAY,400),-1,-2);space(sheet,6);add(sheet,txt("Filename hints do not prove a file is unnecessary.",12,GRAY,400),-1,-2);space(sheet,8);}
        // Never launch APK installers from a cleanup manager.
        if(!clutterMode&&item.type!=DownloadFiles.APK)scanButton(sheet,"Open file",false,()->{dialog.dismiss();openDownloadFile(item);});
        String id=item.uri.toString();scanButton(sheet,downloadKeep.contains(id)?"Remove keep mark":"Keep this file",false,()->{if(!downloadKeep.remove(id)){downloadKeep.add(id);downloadSelected.remove(id);}dialog.dismiss();showDownloads();});space(sheet,6);
        if(downloadEligible(item))scanButton(sheet,downloadSelected.contains(id)?"Deselect file":"Select for cleanup",true,()->{if(!downloadSelected.remove(id)){if(downloadSelected.size()>=DownloadFiles.DELETE_LIMIT){folderToast("Select up to 50 files at a time");return;}downloadSelected.add(id);}dialog.dismiss();showDownloads();});
        else add(sheet,txt("Protected or read-only files cannot be removed here.",12,GRAY,400),-1,-2);
        space(sheet,7);scanButton(sheet,"Done",false,dialog::dismiss);dialog.setContentView(sheet);dialog.setOnDismissListener(d->{if(downloadSheet==dialog)downloadSheet=null;});
        android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.BOTTOM);window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
        dialog.show();if(window!=null)window.setLayout(-1,-2);
    }
    private void openDownloadFile(DownloadFiles.Item item){
        if(item.type==DownloadFiles.APK)return;
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_VIEW);intent.setDataAndType(item.uri,item.mime);intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{downloadsOpening=true;startActivity(intent);}catch(RuntimeException e){downloadsOpening=false;folderToast("No app available to open this file");}
    }
    private void showDownloadReview(){
        if(downloadSelected.isEmpty()){showDownloads();return;}downloadsReview=true;
        LinearLayout root=scanShell("Review cleanup",this::showDownloads),body=scanBody(root);add(body,txt(downloadSelected.size()+" "+tr("files selected"),22,INK,700),-1,-2);space(body,8);
        add(body,txt("These documents do not support Android media Trash. Removing them may be permanent. Review every file and keep a backup.",14,GRAY,400),-1,-2);space(body,12);
        LinearLayout list=card();int n=0;long size=0;for(DownloadFiles.Item item:downloadItems)if(downloadSelected.contains(item.uri.toString())&&downloadEligible(item)){if(n++>0)thinRule(list);LinearLayout line=row();line.setMinimumHeight(dp(52));TextView name=txt(item.name,13,INK,500);name.setMaxLines(2);weight(line,name);gap(line,7);add(line,txt(downloadSize(item),12,GRAY,500),-2,-2);add(list,line,-1,-2);size+=Math.max(0,item.bytes);}add(body,list,-1,-2);space(body,10);add(body,txt(scanBytes(size)+" · "+tr("Selected size"),15,INK,600),-1,-2);
        TextView remove=centered("Permanently remove files",15,Color.WHITE,600);tap(remove,0xffd92d20,25,()->privacyDialog().setTitle(tr("Permanently remove files?")).setMessage(tr("The selected files may not be recoverable. This does not uninstall any apps. Continue only after reviewing and backing up important files.")).setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Remove permanently"),(d,w)->removeDownloads()).show());add(root,remove,-1,dp(52));setContentView(root);
    }
    private void removeDownloads(){
        if(downloadsBusy||downloadSelected.isEmpty())return;
        java.util.List<DownloadFiles.Item> selected=new java.util.ArrayList<>();for(DownloadFiles.Item i:downloadItems)if(downloadSelected.contains(i.uri.toString())&&downloadEligible(i))selected.add(i);
        if(selected.size()!=downloadSelected.size()||selected.size()>DownloadFiles.DELETE_LIMIT){downloadSelected.clear();refreshDownloads();return;}
        final DownloadFiles engine=downloadEngine();final android.os.CancellationSignal removalSignal=new android.os.CancellationSignal();downloadsSignal=removalSignal;
        downloadsBusy=true;LinearLayout root=scanShell("Removing files",removalSignal::cancel),body=scanBody(root);add(body,centered("Checking and removing approved files",18,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(40),dp(40));space(body,12);scanButton(body,"Cancel remaining",false,removalSignal::cancel);setContentView(root);
        new Thread(()->{int[] result;try{result=engine.remove(selected,removalSignal);}catch(Exception e){result=new int[]{0,selected.size()};}final int removed=result[0],failed=result[1];
            final String detail=removed+" removals reported by the document provider; "+failed+" not removed. Recovered storage has not been measured.";
            final boolean logged=removed==0||history.add(null,"cleanup","Document removal reported",detail,0,removed,false);
            runOnUiThread(()->{if(isDestroyed())return;downloadsBusy=false;downloadsSignal=null;downloadSelected.clear();downloadItems.clear();deviceOverview=null;if(agentTaskOrigin&&activeChat!=null)appendAgent(false,detail,-1);showCleanupOutcome(removed==0?"No removals reported":failed==0?"File cleanup complete":"File cleanup partial",detail,removed,0,removed>0&&failed==0,logged);});
        },"Clenova-downloads-remove").start();
    }

    private void openUsageSettings(){
        if(unusedVisible)unusedExternal=true;
        try{startActivity(new android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));}
        catch(RuntimeException e){unusedExternal=false;folderToast("Usage access settings unavailable");}
    }
    private void openUnusedApps(){
        if(cleanScroll!=null){cleanScrollY=cleanScroll.getScrollY();cleanScroll=null;}
        scanResult=null;unusedVisible=true;unusedReview=false;unusedDays=30;unusedSort=0;unusedPage=0;unusedSelected.clear();unusedKeep.clear();refreshUnusedApps();
    }
    private void refreshUnusedApps(){
        if(unusedSignal!=null)unusedSignal.cancel();unusedSelected.clear();unusedReview=false;unusedPage=0;unusedResult=null;
        if(!UnusedAppScan.allowed(this)){showUnusedPermission();return;}
        final android.os.CancellationSignal signal=new android.os.CancellationSignal();unusedSignal=signal;final int generation=++unusedGeneration;
        LinearLayout root=scanShell("Unused apps",this::leaveAgentTool),body=scanBody(root);add(body,centered("Checking app usage",20,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(42),dp(42));space(body,12);add(body,centered("Your apps stay installed during scanning.",13,GRAY,400),-1,-2);setContentView(root);
        new Thread(()->{try{
            UnusedAppScan.Result result=new UnusedAppScan(this).run(signal);
            runOnUiThread(()->{if(generation!=unusedGeneration||isDestroyed()||!unusedVisible)return;unusedSignal=null;unusedResult=result;if(result.allowed)showUnusedApps();else showUnusedPermission();});
        }catch(Exception e){runOnUiThread(()->{if(generation!=unusedGeneration||isDestroyed()||!unusedVisible)return;unusedSignal=null;showUnusedPermission();folderToast("App usage could not be read");});}},"Clenova-app-usage").start();
    }
    private void showUnusedPermission(){
        unusedVisible=true;unusedReview=false;
        LinearLayout root=scanShell("Unused apps",this::leaveAgentTool),body=scanBody(root),hero=card();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,24));icon.addView(new Glyph(this,"apps",BLUE),new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.CENTER));add(hero,icon,dp(84),dp(84));space(hero,18);
        add(hero,centered("Usage access required",23,INK,700),-1,-2);space(hero,9);add(hero,centered("Android Usage Access lets Clenova review when apps were last used and their available storage statistics.",14,GRAY,400),-1,-2);space(hero,14);
        add(hero,centered("Only checked locally when you request a scan. No automatic uninstall.",13,GREEN,500),-1,-2);add(body,hero,-1,-2);space(body,15);
        scanButton(body,"Open Usage Access settings",true,this::openUsageSettings);space(body,8);scanButton(body,"Check again",false,this::refreshUnusedApps);setContentView(root);
    }
    private java.util.List<UnusedAppScan.Item> visibleUnused(){
        java.util.List<UnusedAppScan.Item> list=new java.util.ArrayList<>();if(unusedResult==null)return list;long now=System.currentTimeMillis();
        for(UnusedAppScan.Item item:unusedResult.items)if(item.days(now)>=unusedDays)list.add(item);
        java.util.Comparator<UnusedAppScan.Item> cmp=unusedSort==1?java.util.Comparator.comparingLong((UnusedAppScan.Item i)->i.lastUsed):unusedSort==2?java.util.Comparator.comparing((UnusedAppScan.Item i)->i.name,String.CASE_INSENSITIVE_ORDER):java.util.Comparator.comparingLong((UnusedAppScan.Item i)->i.bytes()).reversed();list.sort(cmp.thenComparing(i->i.pkg));return list;
    }
    private String appStorage(long bytes){return bytes<0?tr("Unknown"):scanBytes(bytes);}
    private String lastUsedLabel(UnusedAppScan.Item item){return Math.max(0,(System.currentTimeMillis()-item.lastUsed)/UnusedAppScan.DAY)+" "+tr("days since last observed use");}
    private View unusedIcon(UnusedAppScan.Item item,int size){
        FrameLayout frame=new FrameLayout(this);frame.setBackground(round(PALE,13));
        if(item.icon!=null){android.widget.ImageView image=new android.widget.ImageView(this);image.setImageBitmap(item.icon);image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);frame.addView(image,new FrameLayout.LayoutParams(dp(size),dp(size),Gravity.CENTER));}
        else frame.addView(new Glyph(this,"apps",BLUE),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
        return frame;
    }
    private void unusedFooter(TextView count,TextView amount,TextView action){
        long total=0;boolean unknown=false;if(unusedResult!=null)for(UnusedAppScan.Item i:unusedResult.items)if(unusedSelected.contains(i.pkg)){if(i.bytes()<0)unknown=true;else total+=i.bytes();}
        count.setText(unusedSelected.size()+" "+tr("apps selected"));amount.setText(scanBytes(total)+(unknown?" + "+tr("Unknown"):""));action.setEnabled(!unusedSelected.isEmpty());action.setAlpha(unusedSelected.isEmpty()?.45f:1f);
    }
    private void showUnusedApps(){
        if(!UnusedAppScan.allowed(this)||unusedResult==null){showUnusedPermission();return;}
        unusedVisible=true;unusedReview=false;java.util.List<UnusedAppScan.Item> list=visibleUnused();
        LinearLayout root=scanShell("Unused apps",this::leaveAgentTool),body=scanBody(root),hero=card(),summary=row(),labels=column();
        long total=0;int unknownSize=0;for(UnusedAppScan.Item i:list){if(i.bytes()<0)unknownSize++;else total+=i.bytes();}
        add(labels,txt("Known storage occupied",13,GRAY,400),-1,-2);space(labels,6);add(labels,txt(scanBytes(total),32,INK,700),-1,-2);space(labels,5);add(labels,txt(list.size()+" "+tr("rarely used apps"),12,GRAY,400),-1,-2);if(unknownSize>0){space(labels,4);add(labels,txt(unknownSize+" "+tr("sizes unavailable"),11,GRAY,400),-1,-2);}weight(summary,labels);gap(summary,8);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));mark.addView(new Glyph(this,"apps",BLUE),new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER));add(summary,mark,dp(70),dp(70));add(hero,summary,-1,-2);space(hero,12);thinRule(hero);space(hero,10);
        LinearLayout safe=row();add(safe,new Glyph(this,"shield",GREEN),dp(19),dp(19));gap(safe,7);weight(safe,txt("System apps protected",13,GREEN,600));add(hero,safe,-1,-2);add(body,hero,-1,-2);space(body,12);
        LinearLayout filters=row();for(int day:new int[]{30,60,90}){if(day!=30)gap(filters,7);TextView chip=centered(day+"+ "+tr("days"),12,unusedDays==day?Color.WHITE:INK,600);tap(chip,unusedDays==day?BLUE:WHITE,20,()->{unusedDays=day;unusedSelected.clear();unusedPage=0;showUnusedApps();});filters.addView(chip,new LinearLayout.LayoutParams(0,dp(44),1f));}add(body,filters,-1,-2);space(body,12);
        add(body,txt("Filters use observed use and last update. Apps with unknown usage are excluded.",12,GRAY,400),-1,-2);space(body,9);
        LinearLayout heading=row();weight(heading,txt("Rarely used apps",17,INK,600));int eligible=0;for(UnusedAppScan.Item i:list)if(!unusedKeep.contains(i.pkg))eligible++;
        TextView select=txt(unusedSelected.isEmpty()?"Select all":"Deselect all",12,BLUE,600);tap(select,BG,8,()->{if(!unusedSelected.isEmpty())unusedSelected.clear();else for(UnusedAppScan.Item i:list)if(!unusedKeep.contains(i.pkg))unusedSelected.add(i.pkg);showUnusedApps();});select.setEnabled(eligible>0||!unusedSelected.isEmpty());add(heading,select,-2,dp(44));gap(heading,7);
        FrameLayout sort=new FrameLayout(this);sort.addView(new Glyph(this,"more",BLUE),new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));sort.setContentDescription(tr("Sort apps"));tap(sort,WHITE,18,()->privacyDialog().setTitle(tr("Sort apps")).setSingleChoiceItems(new String[]{tr("Largest first"),tr("Least recently used"),tr("Name")},unusedSort,(d,i)->{unusedSort=i;unusedPage=0;d.dismiss();showUnusedApps();}).setNegativeButton(tr("Cancel"),null).show());add(heading,sort,dp(38),dp(38));add(body,heading,-1,-2);
        TextView count=txt("",12,GRAY,400),amount=txt("",18,INK,700),review=centered("Review & uninstall",13,Color.WHITE,600);
        int pages=Math.max(1,(list.size()+14)/15);unusedPage=Math.max(0,Math.min(unusedPage,pages-1));LinearLayout group=card();pad(group,12,6);
        if(list.isEmpty()){pad(group,16,22);add(group,centered("No rarely used apps found",17,INK,600),-1,-2);space(group,8);add(group,centered("Available Android usage history may be incomplete.",13,GRAY,400),-1,-2);}
        for(int n=unusedPage*15;n<Math.min(list.size(),(unusedPage+1)*15);n++){UnusedAppScan.Item item=list.get(n);if(n>unusedPage*15)thinRule(group);
            LinearLayout line=row();line.setMinimumHeight(dp(76));line.setPadding(0,dp(8),0,dp(8));View icon=unusedIcon(item,42);icon.setOnClickListener(v->showUnusedSheet(item));add(line,icon,dp(48),dp(48));gap(line,10);
            LinearLayout details=column();TextView name=txt(item.name,14,INK,600);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(details,name,-1,-2);space(details,4);add(details,txt(lastUsedLabel(item),11,GRAY,400),-1,-2);space(details,4);add(details,txt(appStorage(item.bytes()),14,INK,600),-1,-2);if(unusedKeep.contains(item.pkg)){space(details,3);add(details,txt("Keep",11,GREEN,500),-1,-2);}tap(details,WHITE,8,()->showUnusedSheet(item));weight(line,details);
            android.widget.CheckBox check=new android.widget.CheckBox(this);check.setButtonTintList(ColorStateList.valueOf(BLUE));check.setChecked(unusedSelected.contains(item.pkg));check.setEnabled(!unusedKeep.contains(item.pkg));check.setContentDescription(item.name);check.setOnCheckedChangeListener((v,checked)->{if(checked)unusedSelected.add(item.pkg);else unusedSelected.remove(item.pkg);unusedFooter(count,amount,review);});add(line,check,dp(44),dp(44));add(group,line,-1,-2);
        }add(body,group,-1,-2);space(body,10);
        if(pages>1){LinearLayout pager=row();TextView prev=centered("Previous",13,BLUE,600),next=centered("Next",13,BLUE,600);tap(prev,WHITE,12,()->{unusedPage--;showUnusedApps();});prev.setEnabled(unusedPage>0);prev.setAlpha(unusedPage>0?1:.4f);tap(next,WHITE,12,()->{unusedPage++;showUnusedApps();});next.setEnabled(unusedPage+1<pages);next.setAlpha(unusedPage+1<pages?1:.4f);weight(pager,prev);weight(pager,centered((unusedPage+1)+" / "+pages,13,GRAY,400));weight(pager,next);add(body,pager,-1,dp(48));}
        add(body,txt(unusedResult.unknown+" "+tr("apps with unknown usage excluded"),12,GRAY,400),-1,-2);
        if(unusedResult.partial){space(body,5);add(body,txt("Partial scan results",12,GRAY,500),-1,-2);}space(body,8);scanButton(body,"Refresh",false,this::refreshUnusedApps);
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);tap(review,BLUE,24,this::showUnusedReview);review.setPadding(dp(12),0,dp(12),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);space(root,7);add(root,centered("Nothing uninstalled without your approval.",11,GRAY,400),-1,-2);unusedFooter(count,amount,review);setContentView(root);
    }
    private void showUnusedSheet(UnusedAppScan.Item item){
        if(unusedSheet!=null)unusedSheet.dismiss();android.app.Dialog dialog=new android.app.Dialog(this);unusedSheet=dialog;dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout sheet=column();GradientDrawable bg=new GradientDrawable();bg.setColor(WHITE);float r=dp(26);bg.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});sheet.setBackground(bg);pad(sheet,20,12);
        FrameLayout handle=new FrameLayout(this);View bar=new View(this);bar.setBackground(round(0xffc7c7cc,4));handle.addView(bar,new FrameLayout.LayoutParams(dp(36),dp(4),Gravity.CENTER));add(sheet,handle,-1,dp(22));space(sheet,7);
        LinearLayout title=row();add(title,unusedIcon(item,54),dp(64),dp(64));gap(title,12);LinearLayout labels=column();add(labels,txt(item.name,19,INK,700),-1,-2);space(labels,5);add(labels,txt(appStorage(item.bytes())+" · "+lastUsedLabel(item),12,GRAY,400),-1,-2);weight(title,labels);add(sheet,title,-1,-2);space(sheet,12);thinRule(sheet);
        String dataExcludingCache=item.dataBytes>=0&&item.cacheBytes>=0?appStorage(Math.max(0,item.dataBytes-item.cacheBytes)):tr("Unknown");
        for(String[] pair:new String[][]{{"App size",appStorage(item.appBytes)},{"User data excluding cache",dataExcludingCache},{"Cache",appStorage(item.cacheBytes)},{"Last observed use",java.text.DateFormat.getDateInstance().format(new java.util.Date(item.lastUsed))},{"Last updated",java.text.DateFormat.getDateInstance().format(new java.util.Date(item.updated))}}){LinearLayout line=row();line.setMinimumHeight(dp(42));weight(line,txt(pair[0],13,GRAY,400));add(line,txt(pair[1],13,INK,600),-2,-2);add(sheet,line,-1,-2);}thinRule(sheet);space(sheet,10);
        scanButton(sheet,"Open app",false,()->{dialog.dismiss();openUnusedTarget(item,false);});space(sheet,5);scanButton(sheet,"App information",false,()->{dialog.dismiss();openUnusedTarget(item,true);});space(sheet,5);
        scanButton(sheet,unusedKeep.contains(item.pkg)?"Remove keep mark":"Keep this app",false,()->{if(!unusedKeep.remove(item.pkg)){unusedKeep.add(item.pkg);unusedSelected.remove(item.pkg);}dialog.dismiss();showUnusedApps();});space(sheet,5);
        if(!unusedKeep.contains(item.pkg))scanButton(sheet,unusedSelected.contains(item.pkg)?"Deselect app":"Select for uninstall",true,()->{if(!unusedSelected.remove(item.pkg))unusedSelected.add(item.pkg);dialog.dismiss();showUnusedApps();});
        space(sheet,6);scanButton(sheet,"Done",false,dialog::dismiss);
        ScrollView scroll=new ScrollView(this);scroll.addView(sheet,new ScrollView.LayoutParams(-1,-2));dialog.setContentView(scroll);dialog.setOnDismissListener(d->{if(unusedSheet==dialog)unusedSheet=null;});
        android.view.Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);}dialog.show();if(w!=null)w.setLayout(-1,Math.min(dp(620),getResources().getDisplayMetrics().heightPixels-dp(40)));
    }
    private void openUnusedTarget(UnusedAppScan.Item item,boolean settings){
        android.content.Intent intent=settings?new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.fromParts("package",item.pkg,null)):getPackageManager().getLaunchIntentForPackage(item.pkg);
        if(intent==null){folderToast("App unavailable");return;}try{unusedExternal=true;startActivity(intent);}catch(RuntimeException e){unusedExternal=false;folderToast("App unavailable");}
    }
    private void showUnusedReview(){
        if(unusedResult==null||unusedSelected.isEmpty()){showUnusedApps();return;}unusedReview=true;
        LinearLayout root=scanShell("Review uninstall",this::showUnusedApps),body=scanBody(root);
        add(body,txt("Review selected apps",22,INK,700),-1,-2);space(body,8);add(body,txt("Uninstalling can permanently remove app data. Review each app and back up important data. Android will ask for confirmation separately.",14,GRAY,400),-1,-2);space(body,12);
        LinearLayout list=card();int n=0;for(UnusedAppScan.Item item:visibleUnused())if(unusedSelected.contains(item.pkg)&&!unusedKeep.contains(item.pkg)){if(n++>0)thinRule(list);LinearLayout line=row();line.setMinimumHeight(dp(65));add(line,unusedIcon(item,36),dp(42),dp(42));gap(line,9);LinearLayout labels=column();add(labels,txt(item.name,14,INK,600),-1,-2);space(labels,4);add(labels,txt(appStorage(item.bytes()),12,GRAY,400),-1,-2);weight(line,labels);TextView action=centered("Uninstall",12,BLUE,600);tap(action,PALE,14,()->privacyDialog().setTitle(tr("Uninstall app?")).setMessage(item.name+"\n"+tr("App data may be permanently lost. Android will ask you to confirm.")).setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Continue"),(d,w)->verifyUnusedUninstall(item)).show());add(line,action,dp(88),dp(42));add(list,line,-1,-2);}add(body,list,-1,-2);space(body,12);scanButton(root,"Done",false,this::showUnusedApps);setContentView(root);
    }
    private void verifyUnusedUninstall(UnusedAppScan.Item item){
        if(unusedSignal!=null||unusedUninstallPending||!unusedSelected.contains(item.pkg)||unusedKeep.contains(item.pkg))return;
        final android.os.CancellationSignal signal=new android.os.CancellationSignal();unusedSignal=signal;final int generation=++unusedGeneration,days=unusedDays;
        LinearLayout root=scanShell("Review uninstall",()->{signal.cancel();unusedSignal=null;unusedGeneration++;showUnusedReview();}),body=scanBody(root);add(body,centered("Rechecking app eligibility",18,INK,600),-1,-2);space(body,12);add(body,new android.widget.ProgressBar(this),dp(42),dp(42));setContentView(root);
        new Thread(()->{boolean valid=new UnusedAppScan(this).stillEligible(item,days);
            runOnUiThread(()->{if(signal.isCanceled()||generation!=unusedGeneration||!unusedVisible||isDestroyed()||isFinishing())return;unusedSignal=null;
                if(!valid||!unusedSelected.contains(item.pkg)||unusedKeep.contains(item.pkg)){folderToast("App or usage changed. Review again.");refreshUnusedApps();return;}
                android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_DELETE,android.net.Uri.fromParts("package",item.pkg,null));intent.putExtra(android.content.Intent.EXTRA_RETURN_RESULT,true);
                try{unusedUninstallPending=true;pendingUninstallPackage=item.pkg;pendingUninstallName=item.name;pendingUninstallId=java.util.UUID.randomUUID().toString();startActivityForResult(intent,REQUEST_APP_UNINSTALL);}catch(RuntimeException e){unusedUninstallPending=false;pendingUninstallPackage="";pendingUninstallName="";pendingUninstallId="";folderToast("Android uninstall unavailable");showUnusedReview();}
            });},"Clenova-app-recheck").start();
    }

    private int clutterColor(int i){return new int[]{0xffff9500,BLUE,GREEN,0xffaf52de}[i];}
    private String clutterIcon(int i){return new String[]{"activity","folder_down","apps","document"}[i];}
    private void showClutterScreen(){
        LinearLayout root=scanShell("Unnecessary files",this::leaveAgentTool),body=scanBody(root);
        LinearLayout hero=card(),summary=row(),labels=column();long candidateBytes=0;int eligibleCount=0;
        for(DownloadFiles.Item item:downloadItems)if(downloadEligible(item)){candidateBytes+=Math.max(0,item.bytes);eligibleCount++;}
        add(labels,txt("Potential space to recover",13,GRAY,400),-1,-2);space(labels,6);
        add(labels,txt(downloadItems.isEmpty()?"—":scanBytes(candidateBytes),32,INK,700),-1,-2);space(labels,5);
        add(labels,txt(downloadItems.size()+" "+tr("files to review")+" · "+eligibleCount+" "+tr("eligible"),12,GRAY,400),-1,-2);weight(summary,labels);gap(summary,8);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,23));mark.addView(new Glyph(this,"file_x",BLUE),new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER));add(summary,mark,dp(70),dp(70));add(hero,summary,-1,-2);space(hero,12);thinRule(hero);space(hero,10);
        LinearLayout safety=row();add(safety,new Glyph(this,"shield",GREEN),dp(19),dp(19));gap(safety,7);weight(safety,txt("Protected files excluded from cleanup",12,GREEN,600));add(hero,safety,-1,-2);space(hero,12);
        scanButton(hero,"Choose files",true,this::pickDownloads);space(hero,7);scanButton(hero,"Scan selected folder",false,this::pickLargeFolder);add(body,hero,-1,-2);space(body,12);
        add(body,txt("Only selected files and folders are checked. Folder results, incomplete downloads and logs are read-only.",12,GRAY,400),-1,-2);space(body,14);
        LinearLayout heading=row();weight(heading,txt("Cleanup opportunities",18,INK,600));
        if(downloadsFilter>=0){TextView all=txt("All files",12,BLUE,600);tap(all,BG,8,()->{downloadsFilter=-1;downloadSelected.clear();downloadsPage=0;showDownloads();});add(heading,all,-2,dp(44));}add(body,heading,-1,-2);space(body,8);
        LinearLayout categories=card();pad(categories,12,5);
        for(int j=0;j<4;j++){final int category=j;if(j>0)thinRule(categories);int count=0,removable=0;long bytes=0;
            for(DownloadFiles.Item i:downloadItems)if(ClutterRules.category(i.name,i.mime)==j){count++;bytes+=Math.max(0,i.bytes);if(downloadEligible(i))removable++;}
            LinearLayout line=row();line.setMinimumHeight(dp(72));line.setPadding(0,dp(8),0,dp(8));
            FrameLayout icon=new FrameLayout(this);icon.setBackground(round(BG,12));icon.addView(new Glyph(this,clutterIcon(j),clutterColor(j)),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));add(line,icon,dp(44),dp(44));gap(line,11);
            LinearLayout label=column();add(label,txt(ClutterRules.LABELS[j],14,INK,600),-1,-2);space(label,4);
            add(label,txt(count+" "+tr("files")+" · "+scanBytes(bytes),12,GRAY,400),-1,-2);space(label,4);
            add(label,txt(removable>0?removable+" "+tr("eligible"):tr("Review only"),11,BLUE,500),-1,-2);weight(line,label);
            add(line,new Glyph(this,downloadsFilter==j?"selected":"chevron",downloadsFilter==j?BLUE:GRAY),dp(20),dp(20));
            tap(line,downloadsFilter==j?PALE:WHITE,10,()->{downloadsFilter=downloadsFilter==category?-1:category;downloadsPage=0;downloadSelected.clear();showDownloads();});add(categories,line,-1,-2);
        }add(body,categories,-1,-2);space(body,12);
        LinearLayout notice=row();notice.setBackground(round(PALE,15));pad(notice,13,13);add(notice,new Glyph(this,"shield",BLUE),dp(24),dp(24));gap(notice,9);
        LinearLayout copy=column();add(copy,txt("Important files stay protected.",14,INK,600),-1,-2);space(copy,5);add(copy,txt("Filename hints are not proof of junk. Review every file before removal.",12,GRAY,400),-1,-2);weight(notice,copy);add(body,notice,-1,-2);space(body,13);
        LinearLayout host=column();add(body,host,-1,-2);
        TextView count=txt("",12,GRAY,400),amount=txt("",22,INK,700),review=centered("Review & clean",14,Color.WHITE,600);
        LinearLayout footer=row();footer.setBackground(round(WHITE,18));pad(footer,13,10);LinearLayout selected=column();add(selected,count,-1,-2);space(selected,4);add(selected,amount,-1,-2);weight(footer,selected);gap(footer,8);tap(review,BLUE,24,this::showDownloadReview);review.setPadding(dp(13),0,dp(13),0);add(footer,review,-2,dp(46));add(root,footer,-1,-2);space(root,7);add(root,centered("Nothing removed without your approval.",12,GRAY,400),-1,-2);
        renderDownloadList(host,count,amount,review);setContentView(root);
    }

    private void cleanPending(String label){
        // Main dashboard is implemented first. Never route manual actions to simulated Home cleanup.
        privacyDialog().setTitle(tr(label)).setMessage(tr("This cleaning tool is not available in this preview yet. No files have been scanned or changed.")).setPositiveButton(tr("Done"),null).show();
    }
    private final class CleanScanMark extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        CleanScanMark(Context context){super(context);setContentDescription(tr("Not scanned yet"));}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float scale=Math.min(getWidth(),getHeight())/100f;canvas.save();
            canvas.translate((getWidth()-100*scale)/2,(getHeight()-100*scale)/2);canvas.scale(scale,scale);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(7);paint.setColor(PALE);canvas.drawCircle(50,50,42,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(PALE);canvas.drawCircle(50,50,31,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setColor(BLUE);paint.setStrokeWidth(2.7f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            Path path=new Path();path.moveTo(37,41);path.lineTo(37,34);path.lineTo(44,34);path.moveTo(56,34);path.lineTo(63,34);path.lineTo(63,41);
            path.moveTo(63,59);path.lineTo(63,66);path.lineTo(56,66);path.moveTo(44,66);path.lineTo(37,66);path.lineTo(37,59);path.moveTo(37,50);path.lineTo(63,50);
            canvas.drawPath(path,paint);canvas.restore();
        }
    }

    private LinearLayout header() {
        LinearLayout l=row(); add(l,txt("Clenova",29,INK,700),-2,-2); stretch(l);
        FrameLayout settings=new FrameLayout(this);
        settings.addView(new Glyph(this,"settings",INK),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        settings.setContentDescription("Settings");
        tap(settings,WHITE,24,this::openSettings);
        add(l,settings,dp(44),dp(44)); return l;
    }
    private void openSettings(){
        restoreAgentWindow();
        if(agentVisible&&agentShortcut){agentShortcut=false;scanResult=null;agentLiveResult=null;agentResultChat="";showAgent();return;}
        scanResult=null; // protection rules may change in settings; re-scan before recommending again.
        if(toolsScroll!=null){toolsScrollY=toolsScroll.getScrollY();toolsScroll=null;}
        if(cleanScroll!=null){cleanScrollY=cleanScroll.getScrollY();cleanScroll=null;}
        aboutVisible=false;aboutScroll=null;policyVisible=false;termsVisible=false;policyScroll=null;feedbackVisible=false;helpVisible=false;hideHelpKeyboard();permissionsVisible=false;privacyVisible=false;
        historyDetailId=null;historyScroll=null;historyScrollY=0;historyVisible=false;filesVisible=false;foldersVisible=false;protectionVisible=false;agentPrefsVisible=false;settingsVisible=true;setContentView(settingsScreen());
    }
    private LinearLayout settingsScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Settings",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr(toolsVisible?"Back to Tools":agentVisible?"Back to Agent":cleanVisible?"Back to Clean":"Back to Home"));tap(back,WHITE,22,this::returnToDashboard);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(15),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        addSettingsGroup(body,"Preferences",new String[][]{
            {"moon","Appearance",prefs.appearance()},{"language","Language",prefs.languageName()},{"bell","Notifications",""}
        });
        addSettingsGroup(body,"AI Agent",new String[][]{
            {"bot","Agent preferences",""},{"shield","Cleanup protection",""},{"history","Activity history",""}
        });
        addSettingsGroup(body,"Privacy & security",new String[][]{
            {"lock","App permissions",""},{"shield","Privacy & data",""}
        });
        addSettingsGroup(body,"Support",new String[][]{
            {"help","Help & support",""},{"message","Send feedback",""},{"star","Rate Clenova",""}
        });
        addSettingsGroup(body,"About",new String[][]{
            {"document","Privacy policy",""},{"document_check","Terms of use",""},{"info","About Clenova",""}
        });
        space(body,8);
        add(body,centered("Clenova",19,INK,700),-1,-2);space(body,5);
        add(body,centered(tr("Version")+" "+supportAppVersion(),12,GRAY,400),-1,-2);space(body,5);
        add(body,centered("By Skyorigin",12,GRAY,500),-1,-2);space(body,5);
        add(body,centered("Your phone. On autopilot.",12,GRAY,400),-1,-2);
        return root;
    }
    private void addSettingsGroup(LinearLayout body,String title,String[][] items){
        add(body,txt(title,14,GRAY,500),-1,-2);space(body,8);
        LinearLayout group=column();group.setBackground(round(WHITE,16));group.setClipToOutline(true);
        for(int i=0;i<items.length;i++){
            String[] item=items[i];LinearLayout line=row();
            line.setPadding(dp(13),dp(8),dp(13),dp(8));line.setMinimumHeight(dp(52));
            add(line,new Glyph(this,item[0],BLUE),dp(22),dp(22));gap(line,12);
            weight(line,txt(item[1],15,INK,500));
            if(!item[2].isEmpty()){gap(line,6);add(line,txt(item[2],12,GRAY,400),-2,-2);}
            boolean preference=title.equals("Preferences");
            gap(line,9);add(line,new Glyph(this,preference?"select_arrows":"chevron",GRAY),dp(14),dp(14));
            line.setContentDescription(tr(item[1])+(item[2].isEmpty()?"":", "+tr(item[2])));
            if(preference)tap(line,WHITE,0,()->showPreferencePopup(line,item[1]));
            else if(item[1].equals("Agent preferences"))tap(line,WHITE,0,this::openAgentPreferences);
            else if(item[1].equals("Cleanup protection"))tap(line,WHITE,0,this::openProtection);
            else if(item[1].equals("Activity history"))tap(line,WHITE,0,()->{historyFilter=0;historyScrollY=0;openHistory();});
            else if(item[1].equals("App permissions"))tap(line,WHITE,0,this::openAppPermissions);
            else if(item[1].equals("Privacy & data"))tap(line,WHITE,0,this::openPrivacyData);
            else if(item[1].equals("Help & support"))tap(line,WHITE,0,()->{helpQuery="";expandedFaq=-1;openHelpSupport();});
            else if(item[1].equals("Send feedback"))tap(line,WHITE,0,this::openFeedback);
            else if(item[1].equals("Rate Clenova"))tap(line,WHITE,0,()->{ratingStars=0;showRatingSheet();});
            else if(item[1].equals("Privacy policy"))tap(line,WHITE,0,()->{policyScrollY=0;openPrivacyPolicy();});
            else if(item[1].equals("Terms of use"))tap(line,WHITE,0,()->{policyScrollY=0;openTerms();});
            else if(item[1].equals("About Clenova"))tap(line,WHITE,0,()->{aboutScrollY=0;openAbout();});
            add(group,line,-1,-2);
            if(i<items.length-1){
                View divider=new View(this);divider.setBackgroundColor(surface(0xffeeeef0));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));
                lp.leftMargin=dp(47);group.addView(divider,lp);
            }
        }
        add(body,group,-1,-2);space(body,18);
    }

    private void showPreferencePopup(View anchor,String kind){
        if(preferencePopup!=null)preferencePopup.dismiss();
        LinearLayout menu=column();menu.setBackground(round(WHITE,16));
        menu.setPadding(dp(8),dp(6),dp(8),dp(6));
        int width=Math.min(dp(kind.equals("Notifications")?280:205),getResources().getDisplayMetrics().widthPixels-dp(32));
        android.widget.PopupWindow popup=new android.widget.PopupWindow(menu,width,-2,true);
        preferencePopup=popup;
        popup.setBackgroundDrawable(round(WHITE,16));
        popup.setElevation(dp(10));popup.setOutsideTouchable(true);popup.setClippingEnabled(true);
        popup.setInputMethodMode(android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setOnDismissListener(()->{if(preferencePopup==popup)preferencePopup=null;});
        if(kind.equals("Appearance")){
            String[] options={"Light","Dark","System"},icons={"sun","moon","phone"};
            for(int i=0;i<options.length;i++){
                final String value=options[i];LinearLayout option=preferenceOption(icons[i],value,prefs.appearance().equals(value));
                tap(option,WHITE,10,()->{prefs.setAppearance(value);popup.dismiss();applyPalette();openSettings();});
                add(menu,option,-1,dp(46));
            }
        }else if(kind.equals("Language")){
            ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
            LinearLayout options=column();
            for(int i=0;i<AppPreferences.LANG_CODES.length;i++){
                final String code=AppPreferences.LANG_CODES[i];
                LinearLayout option=preferenceOption(null,AppPreferences.LANG_NAMES[i],prefs.language().equals(code));
                tap(option,WHITE,10,()->{prefs.setLanguage(code);popup.dismiss();openSettings();});
                add(options,option,-1,dp(46));
            }
            scroll.addView(options,new ScrollView.LayoutParams(-1,-2));add(menu,scroll,-1,dp(230));
        }else{
            // Only Agent updates currently have a real delivery mechanism.
            for(int i=0;i<1;i++){
                final int index=i;LinearLayout option=row();option.setPadding(dp(8),0,dp(6),0);
                weight(option,txt(AppPreferences.NOTIFICATION_LABELS[i],14,INK,500));gap(option,8);
                android.widget.Switch toggle=new android.widget.Switch(this);
                toggle.setShowText(false);toggle.setChecked(prefs.notificationEnabled(i));
                toggle.setContentDescription(tr(AppPreferences.NOTIFICATION_LABELS[i]));
                toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
                toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{BLUE,GRAY}));
                toggle.setOnCheckedChangeListener((button,checked)->{
                    prefs.setNotification(index,checked);
                    if(checked && android.os.Build.VERSION.SDK_INT>=33 &&
                        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                        popup.dismiss();requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},91);
                    }
                });
                add(option,toggle,-2,dp(44));add(menu,option,-1,dp(48));
            }
            add(menu,txt("Agent updates notify you about scheduled media review candidates.",12,GRAY,400),-1,-2);
            if(!prefs.systemNotificationsAllowed()){
                TextView settings=centered("Enable in Android settings",12,BLUE,600);
                tap(settings,WHITE,8,()->{popup.dismiss();android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(intent);});
                add(menu,settings,-1,dp(42));
            }
        }
        // Overlay the anchor row; do not resize or expand Settings.
        popup.showAsDropDown(anchor,0,-anchor.getHeight(),Gravity.END);
        animatePreferenceMenu(menu);
    }
    private void animatePreferenceMenu(View menu){if(!ValueAnimator.areAnimatorsEnabled())return;menu.setAlpha(0f);menu.setScaleX(.97f);menu.setScaleY(.97f);menu.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(UiPolish.FAST_MOTION_MS).start();}
    private LinearLayout preferenceOption(String icon,String value,boolean selected){
        LinearLayout line=row();line.setPadding(dp(9),0,dp(9),0);
        if(icon!=null){add(line,new Glyph(this,icon,INK),dp(20),dp(20));gap(line,10);}
        weight(line,txt(value,15,INK,500));
        if(selected)add(line,new Glyph(this,"check",BLUE),dp(19),dp(19));
        line.setSelected(selected);line.setContentDescription(tr(value)+(selected?", selected":""));
        return line;
    }

    private LinearLayout inactiveAgentCard(){
        LinearLayout card=card();LinearLayout top=row();
        add(top,new Glyph(this,"bot",BLUE),dp(30),dp(30));gap(top,12);
        LinearLayout info=column();
        add(info,txt(prefs.autopilot()?AutopilotScheduler.status(this,prefs):"Autopilot paused",16,INK,600),-1,-2);space(info,5);
        add(info,txt(prefs.autopilot()?"Manage discovery preferences":"You can resume anytime.",13,GRAY,400),-1,-2);weight(top,info);
        add(card,top,-1,-2);space(card,14);
        TextView manage=centered("Agent preferences",15,BLUE,600);
        tap(manage,PALE,24,this::openAgentPreferences);add(card,manage,-1,dp(48));return card;
    }
    private void openAgentPreferences(){
        settingsVisible=true;agentPrefsVisible=true;setContentView(agentPreferencesScreen());
    }
    @Override protected void onPause(){splashHandler.removeCallbacks(finishSplash);if(onboardingIntro!=null)onboardingIntro.setActive(false);if(onboardingUnified!=null)onboardingUnified.setActive(false);if(onboardingAutopilot!=null)onboardingAutopilot.setActive(false);if(onboardingAgent!=null)onboardingAgent.setActive(false);
        overviewForeground=false;stopOverviewObserver();overviewHandler.removeCallbacks(finishPreparation);cancelOverview();deviceOverview=null;preparationReady=false;super.onPause();}
    @Override protected void onResume(){
        super.onResume();AutopilotScheduler.reconcile(this,prefs);overviewForeground=true;observeOverview();
        if(onboardingIntro!=null)onboardingIntro.setActive(true);
        if(onboardingUnified!=null)onboardingUnified.setActive(true);
        if(onboardingAutopilot!=null)onboardingAutopilot.setActive(true);
        if(onboardingAccess!=null)onboardingAccess.refresh();
        if(onboardingAgent!=null)onboardingAgent.setActive(true);
        if(preparingPhone!=null&&!preparationReady&&overviewJob==null)showPreparingPhone();
        else if(dashboardView!=null&&dashboardView.isAttachedToWindow()&&deviceOverview==null){renderOverviewDashboard();beginOverview(false);}
        if(launchSplash!=null){
            if(splashDeadline==0){splashDeadline=android.os.SystemClock.elapsedRealtime()+(android.animation.ValueAnimator.areAnimatorsEnabled()?OnboardingPolicy.SPLASH_MS:0);launchSplash.reveal();}
            splashHandler.removeCallbacks(finishSplash);splashHandler.postDelayed(finishSplash,Math.max(0,splashDeadline-android.os.SystemClock.elapsedRealtime()));
        }
        agentForeground=true;boolean expired=expireAgentSession();scheduleAgentExpiry();if(agentVisible&&!settingsVisible&&!agentPermissionPending&&!agentTaskOrigin&&!verifyingCleanup&&!completionVisible&&(expired||activeChat!=null)&&!agentCloudBusy&&agentScan==null)showAgent();
        if(unusedExternal){unusedExternal=false;if(unusedVisible&&!unusedUninstallPending)refreshUnusedApps();}
        if(downloadsOpening){downloadsOpening=false;if(downloadsVisible)refreshDownloads();}
        if(permissionsVisible&&devicePermissions!=null)openAppPermissions();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(onboardingAccess!=null&&onboardingAccess.permissionResult(requestCode))return;
        if(handleToolPermission(requestCode))return;
        if(requestCode==REQUEST_NOTIFICATION_ACCESS&&permissionsVisible)openAppPermissions();
    }
    private static final String[] RATING_LABELS={"Tap to rate your experience","Very poor","Could be better","It's okay","Pretty good","Love it!"};
    private void showRatingSheet(){
        if(ratingDialog!=null&&ratingDialog.isShowing())return;
        android.app.Dialog dialog=new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        ratingDialog=dialog;dialog.setCancelable(true);dialog.setCanceledOnTouchOutside(true);
        LinearLayout sheet=column();sheet.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable bg=new GradientDrawable();bg.setColor(WHITE);
        float radius=dp(28);bg.setCornerRadii(new float[]{radius,radius,radius,radius,0,0,0,0});
        sheet.setBackground(bg);sheet.setPadding(dp(22),dp(10),dp(22),dp(19));
        FrameLayout handle=new FrameLayout(this);
        View bar=new View(this);bar.setBackground(round(surface(0xffc7c7cc),4));
        handle.addView(bar,new FrameLayout.LayoutParams(dp(36),dp(4),Gravity.CENTER));
        add(sheet,handle,-1,dp(28));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        add(sheet,scroll,-1,-2);
        space(body,5);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,22));
        mark.addView(new Glyph(this,"star",BLUE),new FrameLayout.LayoutParams(dp(39),dp(39),Gravity.CENTER));
        add(body,mark,dp(76),dp(76));space(body,17);
        add(body,centered("Enjoying Clenova?",25,INK,700),-1,-2);space(body,8);
        add(body,centered("Your feedback helps us make Clenova even better.",14,GRAY,400),-1,-2);space(body,21);
        LinearLayout stars=row();stars.setGravity(Gravity.CENTER);
        RatingStar[] starViews=new RatingStar[5];
        TextView description=centered(RATING_LABELS[ratingStars],13,GRAY,500);
        for(int i=0;i<5;i++){
            final int selected=i+1;RatingStar star=new RatingStar(this,selected);starViews[i]=star;
            star.setFocusable(true);star.setClickable(true);
            star.setOnClickListener(view->{
                ratingStars=selected;view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                for(RatingStar item:starViews)if(item!=null)item.refresh();
                description.setText(tr(RATING_LABELS[ratingStars]));
                description.announceForAccessibility(tr(RATING_LABELS[ratingStars]));
            });
            stars.addView(star,new LinearLayout.LayoutParams(0,dp(52),1f));
        }
        add(body,stars,-1,dp(52));space(body,9);add(body,description,-1,-2);space(body,23);
        TextView play=centered("Rate on Google Play",16,Color.WHITE,600);
        // A rating is not required, and all star values follow exactly the same public-store route.
        tap(play,BLUE,26,()->openPlayRating(dialog));add(body,play,-1,dp(52));space(body,9);
        TextView later=centered("Maybe later",14,GRAY,500);
        tap(later,WHITE,20,dialog::dismiss);add(body,later,-1,dp(44));
        // Only the dedicated drag handle intercepts gestures, so star selection and scrolling remain reliable.
        handle.setContentDescription(tr("Dismiss"));handle.setFocusable(true);handle.setOnClickListener(v->dialog.dismiss());
        handle.setOnTouchListener(new View.OnTouchListener(){
            float start;boolean dragging;
            public boolean onTouch(View view,android.view.MotionEvent event){
                switch(event.getActionMasked()){
                    case android.view.MotionEvent.ACTION_DOWN:
                        sheet.animate().cancel();start=event.getRawY();dragging=true;return true;
                    case android.view.MotionEvent.ACTION_MOVE:
                        if(dragging)sheet.setTranslationY(Math.max(0,event.getRawY()-start));return true;
                    case android.view.MotionEvent.ACTION_UP:
                        dragging=false;
                        if(sheet.getTranslationY()>dp(72)){
                            sheet.animate().translationY(sheet.getHeight()).setDuration(150).withEndAction(dialog::dismiss).start();
                        }else if(Math.abs(event.getRawY()-start)<dp(8)){view.performClick();}else sheet.animate().translationY(0).setDuration(160).start();
                        return true;
                    case android.view.MotionEvent.ACTION_CANCEL:
                        dragging=false;sheet.animate().translationY(0).setDuration(160).start();return true;
                    default:return true;
                }
            }
        });
        dialog.setContentView(sheet);
        dialog.setOnDismissListener(d->{sheet.animate().cancel();if(ratingDialog==dialog)ratingDialog=null;});
        dialog.show();
        android.view.Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.44f);window.setGravity(Gravity.BOTTOM);
            window.setLayout(-1,-2);
            window.setNavigationBarColor(WHITE);
            window.getDecorView().setSystemUiVisibility(dark||android.os.Build.VERSION.SDK_INT<27?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            // Constrain tall sheets on short/landscape displays while keeping content scrollable.
            int maxHeight=Math.max(dp(160),getResources().getDisplayMetrics().heightPixels-dp(100));
            scroll.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,getResources().getDisplayMetrics().widthPixels-dp(44)),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            if(scroll.getMeasuredHeight()>maxHeight)scroll.getLayoutParams().height=maxHeight;
            scroll.requestLayout();
        }
        if(!ValueAnimator.areAnimatorsEnabled())return;
        sheet.setAlpha(0f);sheet.post(()->{
            if(!dialog.isShowing())return;
            sheet.setTranslationY(Math.min(sheet.getHeight(),dp(240)));
            sheet.animate().alpha(1f).translationY(0).setDuration(240).setInterpolator(new DecelerateInterpolator()).start();
        });
    }
    private final class RatingStar extends View{
        private final int value;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        RatingStar(Context context,int value){super(context);this.value=value;refresh();}
        void refresh(){
            setSelected(value<=ratingStars);setContentDescription(value+" "+tr(value==1?"star":"stars")+(value==ratingStars?", "+tr("Selected"):""));invalidate();
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float size=Math.min(dp(39),Math.min(getWidth()-dp(5),getHeight()-dp(5)));
            if(size<=0)return;
            canvas.save();canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/24f,size/24f);
            Path star=new Path();
            for(int i=0;i<10;i++){double a=Math.PI*i/5-Math.PI/2;float r=i%2==0?10:4.8f;float x=12+r*(float)Math.cos(a),y=12+r*(float)Math.sin(a);if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();
            boolean active=value<=ratingStars;paint.setStyle(Paint.Style.FILL);paint.setColor(active?0xffffb340:BG);canvas.drawPath(star,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeWidth(1.4f);paint.setColor(active?0xffffb340:surface(0xffc7c7cc));canvas.drawPath(star,paint);canvas.restore();
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){
            super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.RadioButton");info.setCheckable(true);info.setChecked(value==ratingStars);
        }
    }
    private void openPlayRating(android.app.Dialog dialog){
        String id=getPackageName();
        android.content.Intent market=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("market://details?id="+id));
        market.setPackage("com.android.vending");
        try{startActivity(market);dialog.dismiss();return;}
        catch(android.content.ActivityNotFoundException|SecurityException ignored){}
        try{
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://play.google.com/store/apps/details?id="+id)));
            dialog.dismiss();
        }catch(android.content.ActivityNotFoundException|SecurityException e){
            folderToast("Google Play is unavailable");
        }
    }

    private void openAbout(){
        aboutVisible=true;settingsVisible=true;setContentView(aboutScreen());
        final ScrollView scroll=aboutScroll;
        if(scroll!=null)scroll.post(()->scroll.scrollTo(0,aboutScrollY));
    }
    private LinearLayout aboutScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("About Clenova",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);aboutScroll=scroll;scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(20),0,v(16));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        android.widget.ImageView mark=new android.widget.ImageView(this);mark.setImageResource(R.drawable.clenova_logo);mark.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        mark.setContentDescription("Clenova");add(hero,mark,dp(92),dp(92));space(hero,16);
        add(hero,centered("Clenova",30,INK,700),-1,-2);space(hero,6);
        add(hero,centered("Your phone. On autopilot.",14,GRAY,400),-1,-2);space(hero,8);
        add(hero,centered("By Skyorigin",13,BLUE,600),-1,-2);space(hero,13);
        String version=supportAppVersion();
        TextView chip=centered(tr("Version")+" "+version,12,BLUE,600);
        chip.setBackground(round(PALE,18));chip.setPadding(dp(14),dp(8),dp(14),dp(8));add(hero,chip,-2,-2);
        add(body,hero,-1,-2);space(body,24);
        LinearLayout intro=card();add(intro,txt("A smarter phone experience.",19,INK,700),-1,-2);space(intro,9);
        TextView message=txt("Clenova is built to make phone management effortless, intelligent and personal. Less clutter, more space, and complete control over what matters to you.",14,GRAY,400);
        message.setLineSpacing(dp(4),1f);add(intro,message,-1,-2);
        if(PUBLIC_LISTING_URL.isEmpty()){space(intro,9);add(intro,txt("Development build · Media scans and cleanup reviews use accessible device data. You approve every removal.",12,GRAY,400),-1,-2);}
        add(body,intro,-1,-2);space(body,15);
        LinearLayout principles=card();pad(principles,14,5);
        add(principles,aboutPrinciple("shield","Privacy by design"),-1,-2);thinRule(principles);
        add(principles,aboutPrinciple("bot","Intelligent assistance"),-1,-2);thinRule(principles);
        add(principles,aboutPrinciple("fingerprint","Always in your control"),-1,-2);
        add(body,principles,-1,-2);space(body,18);
        LinearLayout actions=card();pad(actions,13,4);
        add(actions,aboutAction("refresh","Check for updates",PUBLIC_LISTING_URL.isEmpty()?"Preview build":null,this::checkClenovaUpdates),-1,-2);thinRule(actions);
        add(actions,aboutAction("share","Share Clenova",null,this::shareClenova),-1,-2);thinRule(actions);
        add(actions,aboutAction("mail","Contact us",null,this::openSupportEmail),-1,-2);
        add(body,actions,-1,-2);space(body,24);
        add(body,centered("Designed for a better digital life.",12,GRAY,400),-1,-2);space(body,7);
        add(body,centered("© 2026 Skyorigin",13,INK,500),-1,-2);
        return root;
    }
    private LinearLayout aboutPrinciple(String icon,String label){
        LinearLayout line=row();line.setMinimumHeight(dp(51));line.setPadding(0,dp(8),0,dp(8));
        add(line,new Glyph(this,icon,BLUE),dp(23),dp(23));gap(line,12);weight(line,txt(label,15,INK,500));return line;
    }
    private LinearLayout aboutAction(String icon,String label,String subtitle,Runnable action){
        LinearLayout line=row();line.setMinimumHeight(dp(55));line.setPadding(0,dp(8),0,dp(8));
        add(line,new Glyph(this,icon,BLUE),dp(23),dp(23));gap(line,12);
        LinearLayout labels=column();add(labels,txt(label,15,INK,500),-1,-2);
        if(subtitle!=null){space(labels,4);add(labels,txt(subtitle,12,GRAY,400),-1,-2);}
        weight(line,labels);gap(line,8);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));
        tap(line,WHITE,10,action);line.setContentDescription(tr(label)+(subtitle==null?"":", "+tr(subtitle)));return line;
    }
    private void checkClenovaUpdates(){
        if(PUBLIC_LISTING_URL.isEmpty()){
            privacyDialog().setTitle(tr("Check for updates"))
                .setMessage(tr("Clenova is currently a preview. Public updates will be available after launch.")+"\n\n"+tr("Version")+" "+supportAppVersion())
                .setPositiveButton(tr("Done"),null).show();return;
        }
        try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(PUBLIC_LISTING_URL)));}
        catch(android.content.ActivityNotFoundException|SecurityException e){folderToast("Google Play is unavailable");}
    }
    private void shareClenova(){
        String message="Clenova — Your phone. On autopilot.\nBy Skyorigin.";
        if(PUBLIC_LISTING_URL.isEmpty())message+="\n"+tr("Currently in preview. Public download is not available yet.");
        else message+="\n"+PUBLIC_LISTING_URL;
        android.content.Intent share=new android.content.Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");share.putExtra(android.content.Intent.EXTRA_TEXT,message);
        try{startActivity(android.content.Intent.createChooser(share,tr("Share Clenova")));}
        catch(android.content.ActivityNotFoundException|SecurityException e){folderToast("Sharing unavailable");}
    }

    private void openPrivacyPolicy(){
        policyVisible=true;termsVisible=false;settingsVisible=true;setContentView(legalScreen(false));
        final ScrollView scroll=policyScroll;
        if(scroll!=null)scroll.post(()->scroll.scrollTo(0,policyScrollY));
    }
    private void openTerms(){
        termsVisible=true;policyVisible=false;settingsVisible=true;setContentView(legalScreen(true));
        final ScrollView scroll=policyScroll;
        if(scroll!=null)scroll.post(()->scroll.scrollTo(0,policyScrollY));
    }
    private LinearLayout legalScreen(boolean terms){
        String title=terms?"Terms of use":"Privacy policy";
        String[][] sections=terms?TermsOfUse.SECTIONS:PrivacyPolicy.SECTIONS;
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered(title,17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);policyScroll=scroll;scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,terms?"document_check":"shield",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered(terms?"Clear from the start.":"Your privacy matters.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered(terms?"Everything you need to know about using Clenova.":"Transparency in everything we do.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,21);
        LinearLayout summary=card();pad(summary,15,7);
        String[] labels=terms?new String[]{"You stay in control","Your files remain yours","Fair and transparent terms"}:new String[]{"Personal files are not uploaded automatically","Local data controls","Approval first"};
        for(String label:labels){
            LinearLayout line=row();line.setMinimumHeight(dp(43));line.setPadding(0,dp(8),0,dp(8));
            add(line,new Glyph(this,"check",terms?BLUE:GREEN),dp(22),dp(22));gap(line,11);weight(line,txt(label,14,INK,500));add(summary,line,-1,-2);
        }
        add(body,summary,-1,-2);space(body,20);
        add(body,txt(title,15,INK,600),-1,-2);space(body,6);
        java.time.LocalDate revision=java.time.LocalDate.parse(terms?TermsOfUse.REVISION_DATE:PrivacyPolicy.REVISION_DATE);
        String date=revision.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(java.util.Locale.forLanguageTag(prefs.language())));
        add(body,txt(tr(terms?"Preview terms":"Preview policy")+" · "+tr("Version")+" "+(terms?TermsOfUse.VERSION:PrivacyPolicy.VERSION)+"\n"+tr("Last revised")+": "+date,12,GRAY,400),-1,-2);space(body,10);
        LinearLayout document=card();pad(document,16,15);
        for(int i=0;i<sections.length;i++){
            if(i>0){space(document,17);thinRule(document);space(document,17);}
            LinearLayout heading=row();add(heading,txt(String.format(java.util.Locale.ROOT,"%02d",i+1),13,BLUE,600),-2,-2);gap(heading,10);
            weight(heading,txt(sections[i][0],16,INK,600));add(document,heading,-1,-2);space(document,9);
            TextView paragraph=txt(sections[i][1],14,INK,400);paragraph.setLineSpacing(dp(5),1f);
            paragraph.setTextIsSelectable(true);add(document,paragraph,-1,-2);
        }
        space(document,13);TextView contact=txt(SUPPORT_EMAIL,14,BLUE,600);
        contact.setPaintFlags(contact.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);contact.setMinHeight(dp(44));contact.setGravity(Gravity.CENTER_VERTICAL);
        tap(contact,WHITE,10,()->openLegalContact(terms));add(document,contact,-1,-2);
        add(body,document,-1,-2);space(body,21);
        LinearLayout footer=row();footer.setGravity(Gravity.CENTER);add(footer,new Glyph(this,"lock",GRAY),dp(16),dp(16));gap(footer,7);
        add(footer,txt(terms?"Built on trust.":"Built with privacy in mind.",12,GRAY,400),-2,-2);add(body,footer,-1,-2);
        // Online policy intentionally omitted until an actual published URL is supplied.
        return root;
    }
    private void openLegalContact(boolean terms){
        android.net.Uri uri=android.net.Uri.parse("mailto:"+SUPPORT_EMAIL+"?subject="+android.net.Uri.encode(terms?"Clenova terms inquiry":"Clenova privacy inquiry"));
        try{startActivity(new android.content.Intent(android.content.Intent.ACTION_SENDTO,uri));}
        catch(android.content.ActivityNotFoundException|SecurityException e){
            privacyDialog().setTitle(tr("No email app available")).setMessage(SUPPORT_EMAIL)
                .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Copy email"),(d,w)->{
                    android.content.ClipboardManager clipboard=getSystemService(android.content.ClipboardManager.class);
                    if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText(terms?"Terms contact":"Privacy contact",SUPPORT_EMAIL));folderToast("Email copied");}
                }).show();
        }
    }

    private void openFeedback(){
        feedbackVisible=true;settingsVisible=true;setContentView(feedbackScreen());
    }
    private void leaveFeedback(){
        // Keep unsent draft in memory for this session, not in persistent preferences.
        hideHelpKeyboard();openSettings();
    }
    private LinearLayout feedbackScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));root.setFocusableInTouchMode(true);
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Send feedback",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::leaveFeedback);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setFillViewport(false);
        LinearLayout body=column();body.setPadding(0,v(14),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"feedback",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,12);
        add(hero,centered("Make us better.",25,INK,700),-1,-2);space(hero,6);
        add(hero,centered("Help us build a better Clenova for you.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,20);
        add(body,txt("What's it about?",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout categories=column();add(body,categories,-1,-2);renderFeedbackCategories(categories);
        space(body,18);add(body,txt("Your feedback",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout field=card();pad(field,14,10);
        android.widget.EditText input=new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setGravity(Gravity.TOP|Gravity.START);input.setTextColor(INK);input.setHintTextColor(GRAY);input.setTextSize(15);
        input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(0,dp(4),0,dp(4));input.setMinLines(4);input.setMaxLines(6);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(FeedbackDraft.MAX_LENGTH)});
        input.setHint(tr("Tell us what's on your mind..."));input.setContentDescription(tr("Your feedback"));
        input.setText(feedbackText);input.setSelection(input.length());add(field,input,-1,-2);
        TextView counter=txt(feedbackText.length()+"/"+FeedbackDraft.MAX_LENGTH,12,GRAY,400);counter.setGravity(Gravity.END);
        space(field,6);add(field,counter,-1,-2);add(body,field,-1,-2);space(body,14);
        LinearLayout optional=card();pad(optional,13,4);
        add(optional,agentToggle("info","Include app information","App version & Android version only",feedbackAppInfo,value->feedbackAppInfo=value),-1,-2);add(body,optional,-1,-2);
        space(body,13);
        LinearLayout note=row();note.setGravity(Gravity.CENTER);add(note,new Glyph(this,"lock",GRAY),dp(15),dp(15));gap(note,6);
        add(note,txt("No personal files are attached.",12,GRAY,400),-2,-2);add(body,note,-1,-2);space(body,5);
        LinearLayout send=row();send.setGravity(Gravity.CENTER);add(send,new Glyph(this,"send",Color.WHITE),dp(21),dp(21));gap(send,8);
        add(send,txt("Send feedback",16,Color.WHITE,600),-2,-2);
        tap(send,BLUE,26,this::sendFeedbackDraft);send.setContentDescription(tr("Send feedback"));
        updateFeedbackSend(send);space(root,6);add(root,send,-1,dp(52));
        space(root,7);add(root,centered("Opens your email app. You send the message.",11,GRAY,400),-1,-2);
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){
                feedbackText=text.toString();counter.setText(feedbackText.length()+"/"+FeedbackDraft.MAX_LENGTH);updateFeedbackSend(send);
            }
            public void afterTextChanged(android.text.Editable text){}
        });
        root.requestFocus();return root;
    }
    private void updateFeedbackSend(View send){
        boolean ready=FeedbackDraft.valid(feedbackText);
        send.setEnabled(ready);send.setAlpha(ready?1f:.48f);
    }
    private void renderFeedbackCategories(LinearLayout target){
        target.removeAllViews();
        String[] icons={"lightbulb","bug","sparkles","message"};
        for(int pair=0;pair<2;pair++){
            if(pair>0)space(target,9);
            LinearLayout line=row();
            for(int j=0;j<2;j++){
                if(j>0)gap(line,9);
                final int index=pair*2+j;boolean selected=feedbackCategory==index;
                LinearLayout tile=column();tile.setMinimumHeight(dp(77));pad(tile,13,11);
                LinearLayout top=row();add(top,new Glyph(this,icons[index],selected?BLUE:GRAY),dp(23),dp(23));stretch(top);
                if(selected)add(top,new Glyph(this,"check",BLUE),dp(16),dp(16));add(tile,top,-1,-2);space(tile,9);
                add(tile,txt(FeedbackDraft.CATEGORIES[index],14,selected?BLUE:INK,500),-1,-2);
                tap(tile,selected?PALE:WHITE,15,()->{feedbackCategory=index;renderFeedbackCategories(target);});
                GradientDrawable bg=round(selected?PALE:WHITE,15);
                if(selected)bg.setStroke(dp(1),BLUE);
                tile.setBackground(new RippleDrawable(ColorStateList.valueOf(0x19000000),bg,round(WHITE,15)));
                tile.setSelected(selected);tile.setContentDescription(tr(FeedbackDraft.CATEGORIES[index])+(selected?", "+tr("Selected"):""));
                weight(line,tile);
            }
            add(target,line,-1,-2);
        }
    }
    private void sendFeedbackDraft(){
        if(!FeedbackDraft.valid(feedbackText))return;
        hideHelpKeyboard();
        String subject=FeedbackDraft.subject(feedbackCategory);
        String body=FeedbackDraft.body(feedbackCategory,feedbackText,feedbackAppInfo,feedbackAppInfo?supportAppVersion():"",feedbackAppInfo?android.os.Build.VERSION.RELEASE:"");
        android.net.Uri mail=android.net.Uri.parse("mailto:"+SUPPORT_EMAIL+"?subject="+android.net.Uri.encode(subject)+"&body="+android.net.Uri.encode(body));
        try{startActivity(new android.content.Intent(android.content.Intent.ACTION_SENDTO,mail));}
        catch(android.content.ActivityNotFoundException|SecurityException e){
            privacyDialog().setTitle(tr("No email app available"))
                .setMessage(tr("Copy the draft to send it manually.")+"\n\n"+SUPPORT_EMAIL)
                .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Copy feedback"),(d,w)->{
                    android.content.ClipboardManager clipboard=getSystemService(android.content.ClipboardManager.class);
                    if(clipboard!=null){
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Clenova feedback","To: "+SUPPORT_EMAIL+"\nSubject: "+subject+"\n\n"+body));
                        folderToast("Feedback copied");
                    }
                }).show();
        }
        // Do not clear the draft or claim the email was sent: external mail delivery is unobservable.
    }

    private static final String[][] HELP_FAQS={{"How does the AI Agent work?","Home reads available device storage. Smart Scan analyzes accessible media on request. When enabled, Autopilot checks accessible media while charging when Android permits. You approve any removal."},
        {"Can Clenova delete files automatically?","No. Autopilot only recommends. Media removal requires your review and Android Trash approval on Android 11 or later. Document removal may be permanent and requires separate confirmation."},
        {"How do I protect important files?","Open Settings > Cleanup protection > Protected files or Protected folders. Select items with the Android picker. Exclusions are saved locally and checked during supported cleanup reviews. They are not an Android filesystem lock."},
        {"Why does Clenova need permissions?","Permissions support specific features. Open Settings > App permissions to manage access. Smart Scan requests optional photo and video access. Notification access is separate; file selection uses the Android picker."},
        {"Is my personal data safe?","Preferences, exclusions and activity history are stored locally. Personal files are not automatically uploaded. Optional private AI relay sends typed messages after consent. You can export or clear activity history in Privacy & data."},
        {"How do I clear my activity history?","Open Settings > Privacy & data > Clear activity history. Confirm to remove local activity records without changing your protection settings."},
        {"How do I change the app language or theme?","Open Settings and select Language or Appearance. Your choice is saved on your device."}};
    private void hideHelpKeyboard(){
        View focused=getCurrentFocus();
        android.view.inputmethod.InputMethodManager manager=getSystemService(android.view.inputmethod.InputMethodManager.class);
        if(focused!=null&&manager!=null)manager.hideSoftInputFromWindow(focused.getWindowToken(),0);
    }
    private void openHelpSupport(){
        helpVisible=true;settingsVisible=true;setContentView(helpSupportScreen());
    }
    private LinearLayout helpSupportScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        root.setFocusableInTouchMode(true);
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Help & support",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"headphones",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered("We're here for you.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Find answers. Get back to what matters.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,22);
        LinearLayout search=row();search.setBackground(round(WHITE,16));search.setPadding(dp(13),0,dp(8),0);
        add(search,new Glyph(this,"search",GRAY),dp(21),dp(21));gap(search,10);
        android.widget.EditText input=new android.widget.EditText(this);
        input.setSingleLine(true);input.setTextColor(INK);input.setHintTextColor(GRAY);input.setTextSize(15);
        input.setBackgroundColor(Color.TRANSPARENT);input.setPadding(0,0,0,0);
        input.setHint(tr("Search for answers"));input.setContentDescription(tr("Search for answers"));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        input.setText(helpQuery);input.setSelection(input.length());weight(search,input);
        FrameLayout clear=new FrameLayout(this);clear.addView(new Glyph(this,"close",GRAY),new FrameLayout.LayoutParams(dp(17),dp(17),Gravity.CENTER));
        clear.setContentDescription(tr("Clear search"));tap(clear,WHITE,18,()->input.setText(""));
        clear.setVisibility(helpQuery.isEmpty()?View.INVISIBLE:View.VISIBLE);add(search,clear,dp(36),dp(44));
        add(body,search,-1,dp(52));space(body,22);
        TextView heading=txt(helpQuery.trim().isEmpty()?"Popular questions":"Search results",14,GRAY,500);
        add(body,heading,-1,-2);space(body,9);
        LinearLayout results=card();pad(results,13,4);add(body,results,-1,-2);
        renderFaqs(results,false);
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){
                helpQuery=text.toString();expandedFaq=-1;
                heading.setText(tr(helpQuery.trim().isEmpty()?"Popular questions":"Search results"));
                clear.setVisibility(helpQuery.isEmpty()?View.INVISIBLE:View.VISIBLE);renderFaqs(results,false);
            }
            public void afterTextChanged(android.text.Editable text){}
        });
        input.setOnEditorActionListener((view,action,event)->{
            if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){hideHelpKeyboard();input.clearFocus();return true;}return false;
        });
        space(body,23);add(body,txt("Need more help?",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout contact=card(),line=row();pad(contact,13,13);
        FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,13));
        icon.addView(new Glyph(this,"message",BLUE),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
        add(line,icon,dp(44),dp(44));gap(line,12);
        LinearLayout copy=column();add(copy,txt("Contact support",15,INK,600),-1,-2);space(copy,5);
        add(copy,txt("Send us an email",12,GRAY,400),-1,-2);weight(line,copy);gap(line,7);
        add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));add(contact,line,-1,-2);
        tap(contact,WHITE,22,this::openSupportEmail);add(body,contact,-1,-2);space(body,20);
        add(body,centered("Clenova · Always here to help.",12,GRAY,400),-1,-2);
        root.requestFocus();return root;
    }
    private void renderFaqs(LinearLayout results,boolean animate){
        if(animate){
            android.transition.AutoTransition transition=new android.transition.AutoTransition();
            transition.setDuration(180);android.transition.TransitionManager.beginDelayedTransition(results,transition);
        }
        results.removeAllViews();
        String query=helpQuery.trim();int matches=0;
        for(int i=0;i<HELP_FAQS.length;i++){
            String question=HELP_FAQS[i][0],answer=HELP_FAQS[i][1];
            if(!FaqSearch.matches(query,question+" "+answer+" "+tr(question)+" "+tr(answer)))continue;
            if(matches++>0)thinRule(results);
            final int index=i;boolean expanded=expandedFaq==i;
            LinearLayout item=column();LinearLayout line=row();line.setMinimumHeight(dp(56));line.setPadding(0,dp(10),0,dp(10));
            weight(line,txt(question,15,INK,500));gap(line,10);
            add(line,new Glyph(this,expanded?"chevron_up":"chevron_down",GRAY),dp(16),dp(16));
            line.setContentDescription(tr(question)+", "+tr(expanded?"Expanded":"Collapsed"));
            tap(line,WHITE,10,()->{expandedFaq=expandedFaq==index?-1:index;renderFaqs(results,true);});
            add(item,line,-1,-2);
            if(expanded){TextView explanation=txt(answer,13,GRAY,400);explanation.setLineSpacing(dp(3),1f);
                explanation.setPadding(0,0,dp(8),dp(13));add(item,explanation,-1,-2);}
            add(results,item,-1,-2);
        }
        if(matches==0){
            LinearLayout empty=column();empty.setGravity(Gravity.CENTER_HORIZONTAL);pad(empty,12,24);
            add(empty,new Glyph(this,"search",GRAY),dp(29),dp(29));space(empty,10);
            add(empty,centered("No answers found",15,INK,600),-1,-2);space(empty,6);
            add(empty,centered("Try another keyword or contact support.",13,GRAY,400),-1,-2);add(results,empty,-1,-2);
        }
    }
    private String supportAppVersion(){
        try{String version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;return version==null?tr("Unavailable"):version;}
        catch(android.content.pm.PackageManager.NameNotFoundException e){return tr("Unavailable");}
    }
    private void openSupportEmail(){
        hideHelpKeyboard();
        String subject="Clenova support";
        String body="\n\n"+tr("App version")+": "+supportAppVersion()+"\nAndroid: "+android.os.Build.VERSION.RELEASE;
        // SENDTO confines handling to compatible email apps; no files, logs or identifiers attached.
        android.net.Uri mail=android.net.Uri.parse("mailto:"+SUPPORT_EMAIL+"?subject="+android.net.Uri.encode(subject)+"&body="+android.net.Uri.encode(body));
        try{startActivity(new android.content.Intent(android.content.Intent.ACTION_SENDTO,mail));}
        catch(android.content.ActivityNotFoundException e){
            privacyDialog().setTitle(tr("No email app available")).setMessage(SUPPORT_EMAIL)
                .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Copy email"),(d,w)->{
                    android.content.ClipboardManager clipboard=getSystemService(android.content.ClipboardManager.class);
                    if(clipboard!=null){clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Support email",SUPPORT_EMAIL));folderToast("Email copied");}
                }).show();
        }
    }

    private AlertDialog.Builder privacyDialog(){
        return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
    }
    private void openPrivacyData(){
        privacyVisible=true;permissionsVisible=false;settingsVisible=true;setContentView(privacyDataScreen());
    }
    private LinearLayout privacyDataScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Privacy & data",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"lock",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered("Private by design.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Your device. Your data. Your control.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,24);
        LinearLayout principles=card();pad(principles,13,5);
        add(principles,privacyPrinciple("phone","On-device storage","Preferences and history stored locally"),-1,-2);thinRule(principles);
        add(principles,privacyPrinciple("cloud_off","No automatic cloud uploads","Clenova does not upload personal files"),-1,-2);thinRule(principles);
        add(principles,privacyPrinciple("shield","Approval first","No cleanup without your permission"),-1,-2);
        add(body,principles,-1,-2);space(body,23);
        add(body,txt("Manage your data",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout manage=card();pad(manage,13,4);
        add(manage,privacyAction("database","Local app data",false,this::showLocalData),-1,-2);thinRule(manage);
        add(manage,privacyAction("download","Export activity history",false,this::confirmHistoryExport),-1,-2);thinRule(manage);
        add(manage,privacyAction("history","Clear activity history",false,this::confirmClearHistory),-1,-2);thinRule(manage);
        add(manage,privacyAction("trash","Reset Clenova data",true,this::confirmResetData),-1,-2);
        add(body,manage,-1,-2);space(body,21);
        LinearLayout footer=row();footer.setGravity(Gravity.CENTER);
        add(footer,new Glyph(this,"lock",GRAY),dp(16),dp(16));gap(footer,7);
        add(footer,txt("Privacy isn't optional.",12,GRAY,400),-2,-2);add(body,footer,-1,-2);
        return root;
    }
    private LinearLayout privacyPrinciple(String icon,String name,String description){
        LinearLayout line=row();line.setMinimumHeight(dp(64));line.setPadding(0,dp(10),0,dp(10));
        add(line,new Glyph(this,icon,GREEN),dp(24),dp(24));gap(line,12);
        LinearLayout copy=column();add(copy,txt(name,15,INK,600),-1,-2);space(copy,5);
        add(copy,txt(description,12,GRAY,400),-1,-2);weight(line,copy);return line;
    }
    private LinearLayout privacyAction(String icon,String name,boolean destructive,Runnable action){
        int tint=destructive?(dark?0xffff6961:0xffff3b30):BLUE;
        LinearLayout line=row();line.setMinimumHeight(dp(55));line.setPadding(0,dp(8),0,dp(8));
        add(line,new Glyph(this,icon,tint),dp(23),dp(23));gap(line,12);
        weight(line,txt(name,15,destructive?tint:INK,500));gap(line,9);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));
        line.setContentDescription(tr(name));tap(line,WHITE,10,action);return line;
    }
    private void showLocalData(){
        String message;
        LocalAppData.Size size=LocalAppData.measure(this);
        if(size.bytes<0)message=tr("Storage size unavailable");
        else message=tr(size.complete?"App data including cache":"Saved settings and history on disk")+": "+android.text.format.Formatter.formatFileSize(this,size.bytes);
        message+="\n\n"+tr("Activity records")+": "+history.entries().size()+"\n"+tr("Protected folders")+": "+protectedFolders.entries().size()+"\n"+tr("Protected files")+": "+protectedFiles.entries().size();
        privacyDialog().setTitle(tr("Local app data")).setMessage(message).setPositiveButton(tr("Done"),null).show();
    }
    private void confirmHistoryExport(){
        privacyDialog().setTitle(tr("Export activity history"))
            .setMessage(tr("Export a JSON copy of your history, including recorded file and folder names. Choose where to save it."))
            .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Export"),(d,w)->pickHistoryExport()).show();
    }
    private void pickHistoryExport(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.setType("application/json");
        intent.putExtra(android.content.Intent.EXTRA_TITLE,"clenova-activity-"+java.time.LocalDate.now()+".json");
        try{startActivityForResult(intent,EXPORT_ACTIVITY_HISTORY);}
        catch(android.content.ActivityNotFoundException e){folderToast("Export picker unavailable");}
    }
    private void writeHistoryExport(android.net.Uri uri){
        try{
            byte[] payload=history.exportJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                if(out==null)throw new java.io.IOException("No output stream");out.write(payload);
            }
            folderToast("Activity history exported");
        }catch(java.io.IOException|org.json.JSONException|RuntimeException e){folderToast("Could not export activity history");}
    }
    private void confirmClearHistory(){
        privacyDialog().setTitle(tr("Clear activity history"))
            .setMessage(tr("Permanently remove local activity records? Preferences and protection settings will stay unchanged. Exported copies are not affected."))
            .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Clear"),(d,w)->{
                if(history.clear()){historyDetailId=null;historyScrollY=0;historyScroll=null;folderToast("Activity history cleared");}
                else folderToast("Could not clear activity history");openPrivacyData();
            }).show();
    }
    private void confirmResetData(){
        privacyDialog().setTitle(tr("Reset Clenova data"))
            .setMessage(tr("Reset preferences, protection exclusions and activity history? Personal files, Android permissions and previously exported copies will not be removed."))
            .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Reset"),(d,w)->resetClenovaData()).show();
    }
    private void resetClenovaData(){
        cancelOverview();deviceOverview=null;agentSessionHandler.removeCallbacks(agentExpiry);cancelRelayRequest();agentCloudBusy=false;boolean ok=LocalAppData.reset(this);agentChats=new AgentChatStore(this);activeChat=null;agentLiveResult=null;agentResultChat="";relayEndpoint="";relayToken="";agentGeneration++;
        completionVisible=false;historyDetailId=null;historyScroll=null;historyScrollY=0;historyFilter=0;
        prefs=new AppPreferences(this);protectedFolders=new ProtectedFolders(this);protectedFiles=new ProtectedFiles(this);history=new ActivityHistory(this);
        applyPalette();openPrivacyData();folderToast(ok?"Clenova data reset":"Some app data could not be reset");
    }

    private void openAppPermissions(){
        permissionsVisible=true;settingsVisible=true;setContentView(appPermissionsScreen());
    }
    private LinearLayout appPermissionsScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("App permissions",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"shield",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered("You're in control.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Manage what Clenova can access.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,22);
        add(body,txt("Device access",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout access=card();pad(access,13,4);
        DevicePermissions.Snapshot snapshot=devicePermissions.snapshot();
        add(access,permissionRow("images","Photos & videos","Access for photo and video cleaning",snapshot.media,this::openAndroidAppSettings),-1,-2);
        thinRule(access);
        add(access,permissionRow("folder","Files & storage","Select files and folders using Android",snapshot.storage,this::openStoragePermissionInfo),-1,-2);
        thinRule(access);
        add(access,permissionRow("apps","Usage access","Optional access for Unused apps",UnusedAppScan.allowed(this)?"Allowed":"Not allowed",this::openUsageSettings),-1,-2);
        thinRule(access);
        add(access,permissionRow("bell","Notifications","Agent updates and reminders",snapshot.notifications,this::manageNotificationAccess),-1,-2);
        thinRule(access);
        add(access,permissionRow("users","Contacts","Requested when using Contacts Cleaner",snapshot.contacts,this::openAndroidAppSettings),-1,-2);
        add(body,access,-1,-2);space(body,22);
        add(body,txt("Agent access",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout agent=card();pad(agent,13,4);
        add(agent,permissionRow("activity","Background activity","Manage background restrictions",snapshot.background,this::openBackgroundInfo),-1,-2);
        add(body,agent,-1,-2);space(body,19);
        LinearLayout privacy=row();privacy.setBackground(round(PALE,16));pad(privacy,14,14);
        add(privacy,new Glyph(this,"lock",BLUE),dp(24),dp(24));gap(privacy,12);
        LinearLayout copy=column();add(copy,txt("Your privacy comes first.",15,INK,600),-1,-2);space(copy,6);
        add(copy,txt("Access never means permission to delete. Every cleanup requires your approval.",12,GRAY,400),-1,-2);
        weight(privacy,copy);add(body,privacy,-1,-2);space(body,16);
        add(body,centered("No background scanning or automatic deletion is active.",11,GRAY,400),-1,-2);
        space(root,6);
        LinearLayout button=row();button.setGravity(Gravity.CENTER);
        add(button,new Glyph(this,"settings",Color.WHITE),dp(20),dp(20));gap(button,8);
        add(button,txt("Open Android settings",15,Color.WHITE,600),-2,-2);
        tap(button,BLUE,27,this::openAndroidAppSettings);add(root,button,-1,dp(52));
        return root;
    }
    private LinearLayout permissionRow(String icon,String name,String description,String status,Runnable action){
        LinearLayout line=row();line.setMinimumHeight(dp(66));line.setPadding(0,dp(10),0,dp(10));
        add(line,new Glyph(this,icon,BLUE),dp(23),dp(23));gap(line,10);
        LinearLayout copy=column();add(copy,txt(name,15,INK,600),-1,-2);space(copy,5);
        add(copy,txt(description,12,GRAY,400),-1,-2);weight(line,copy);gap(line,8);
        int tint=status.equals("Allowed")||status.equals("Not restricted")?GREEN:status.equals("Not allowed")||status.equals("Limited")||status.equals("Restricted")?BLUE:GRAY;
        TextView label=txt(status,11,tint,600);label.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);label.setMaxWidth(dp(92));
        add(line,label,-2,-2);gap(line,5);
        add(line,new Glyph(this,"chevron",GRAY),dp(13),dp(13));
        line.setContentDescription(tr(name)+", "+tr(status));tap(line,WHITE,10,action);
        return line;
    }
    private void openAndroidAppSettings(){
        android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package",getPackageName(),null));
        try{startActivity(intent);}catch(android.content.ActivityNotFoundException e){folderToast("Android settings unavailable");}
    }
    private void openNotificationSettings(){
        android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());
        try{startActivity(intent);}catch(android.content.ActivityNotFoundException e){openAndroidAppSettings();}
    }
    private void manageNotificationAccess(){
        if(android.os.Build.VERSION.SDK_INT>=33&&!devicePermissions.granted(android.Manifest.permission.POST_NOTIFICATIONS)){
            android.content.SharedPreferences flags=getSharedPreferences("clenova_permission_requests",MODE_PRIVATE);
            boolean asked=flags.getBoolean("notifications_requested",false);
            if(!asked||shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)){
                flags.edit().putBoolean("notifications_requested",true).apply();
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},REQUEST_NOTIFICATION_ACCESS);return;
            }
        }
        openNotificationSettings();
    }
    private void openStoragePermissionInfo(){
        new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle(tr("Files & storage"))
            .setMessage(tr("Clenova uses the Android picker for files and folders you choose. It does not request all-files access."))
            .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Open Android settings"),(d,w)->openAndroidAppSettings()).show();
    }
    private void openBackgroundInfo(){
        new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle(tr("Background activity"))
            .setMessage(tr("Android controls background restrictions. Automatic scans are not active in this build."))
            .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Open Android settings"),(d,w)->openAndroidAppSettings()).show();
    }

    private void recordHistory(String id,String category,String title,String detail,double mb,int count,boolean preview){
        if(!history.add(id,category,title,detail,mb,count,preview))folderToast("Could not save activity");
    }
    private void openHistory(){
        historyDetailId=null;settingsVisible=true;historyVisible=true;setContentView(historyScreen());
        if(historyScroll!=null){final ScrollView scroll=historyScroll;scroll.post(()->scroll.scrollTo(0,historyScrollY));}
    }
    private static final String[] HISTORY_FILTERS={"All activity","Scans","Cleanups","Protection","Tools"};
    private static final String[] HISTORY_CATEGORIES={"","scan","cleanup","protection","tools"};
    private LinearLayout historyScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Activity history",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);historyScroll=scroll;scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"history",BLUE),new FrameLayout.LayoutParams(dp(32),dp(32),Gravity.CENTER));
        add(hero,mark,dp(64),dp(64));space(hero,13);
        add(hero,centered("Always in the loop.",24,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Your cleaning and tool activity, in one place.",14,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,22);
        java.util.List<ActivityHistory.Entry> all=history.entries();
        long items=0,events=0;
        for(ActivityHistory.Entry entry:all)if(!entry.preview){events++;if(entry.category.equals("cleanup")||entry.category.equals("app_manager"))items+=entry.count;}
        LinearLayout stats=card(),metrics=row(),left=column(),right=column();
        add(left,txt("Recorded activity",12,GRAY,400),-1,-2);space(left,6);
        add(left,txt(Long.toString(events),25,INK,700),-1,-2);weight(metrics,left);
        View rule=new View(this);rule.setBackgroundColor(surface(0xffe5e5ea));add(metrics,rule,dp(1),dp(44));gap(metrics,12);
        add(right,txt("Cleanup items",12,GRAY,400),-1,-2);space(right,6);
        add(right,txt(Long.toString(items),25,INK,700),-1,-2);weight(metrics,right);add(stats,metrics,-1,-2);
        space(stats,10);add(stats,txt("Recorded outcomes · Not reclaimed storage",11,GRAY,400),-1,-2);
        add(body,stats,-1,-2);space(body,20);
        if(historyFilter<0||historyFilter>=HISTORY_FILTERS.length)historyFilter=0;
        LinearLayout filters=row();weight(filters,txt("Activity",14,GRAY,500));
        LinearLayout selection=row();pad(selection,8,6);
        add(selection,txt(HISTORY_FILTERS[historyFilter],13,GRAY,500),-2,-2);gap(selection,5);
        add(selection,new Glyph(this,"select_arrows",GRAY),dp(14),dp(14));
        tap(selection,BG,8,()->showHistoryFilter(selection));add(filters,selection,-2,dp(40));add(body,filters,-1,-2);
        java.time.LocalDate today=java.time.LocalDate.now(),previous=null;
        java.time.format.DateTimeFormatter dateFormat=java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(java.util.Locale.forLanguageTag(prefs.language()));
        LinearLayout group=null;
        for(ActivityHistory.Entry entry:all){
            if(entry.preview)continue;
            if(historyFilter==4){if(entry.category.equals("scan")||entry.category.equals("cleanup")||entry.category.equals("protection"))continue;}
            else if(historyFilter!=0&&!HISTORY_CATEGORIES[historyFilter].equals(entry.category))continue;
            java.time.LocalDate date=java.time.Instant.ofEpochMilli(entry.timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if(!date.equals(previous)){
                if(group!=null){add(body,group,-1,-2);space(body,18);}
                add(body,txt(date.equals(today)?"Today":date.equals(today.minusDays(1))?"Yesterday":date.format(dateFormat),14,GRAY,500),-1,-2);space(body,9);
                group=card();pad(group,13,5);previous=date;
            }else thinRule(group);
            add(group,historyRow(entry),-1,-2);
        }
        if(group!=null)add(body,group,-1,-2);
        else{
            LinearLayout empty=card();empty.setGravity(Gravity.CENTER_HORIZONTAL);pad(empty,12,28);
            add(empty,new Glyph(this,"history",GRAY),dp(34),dp(34));space(empty,12);
            add(empty,centered("No activity yet",16,INK,600),-1,-2);space(empty,6);
            add(empty,centered(historyFilter==0?"Your actions will appear here.":"No activity for this filter.",13,GRAY,400),-1,-2);
            add(body,empty,-1,-2);
        }
        space(body,20);LinearLayout privacy=row();privacy.setGravity(Gravity.CENTER);
        add(privacy,new Glyph(this,"lock",GRAY),dp(15),dp(15));gap(privacy,6);
        add(privacy,txt("Your activity stays on your device.",12,GRAY,400),-2,-2);add(body,privacy,-1,-2);
        return root;
    }
    private LinearLayout historyRow(ActivityHistory.Entry entry){
        LinearLayout line=row();line.setPadding(0,dp(12),0,dp(12));
        boolean done=!entry.preview&&entry.category.equals("cleanup")&&entry.count>0;
        String icon=done?"check":entry.category.equals("scan")?"sparkles":entry.category.equals("photo_compression")?"images":entry.category.equals("compression")?"video":entry.category.equals("contacts")?"users":entry.category.equals("app_manager")?"apps":"shield";
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(done?0xffe8f6ec:PALE,12));
        mark.addView(new Glyph(this,icon,done?GREEN:BLUE),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        add(line,mark,dp(42),dp(42));gap(line,12);
        LinearLayout details=column();add(details,txt(entry.title,15,INK,600),-1,-2);space(details,5);
        TextView sub=txt("",12,GRAY,400);
        String description=entry.category.equals("protection")?entry.detail:tr(entry.detail);
        if(entry.mb>0)description+=" · "+ReviewPlan.gb(entry.mb);
        if(entry.count>0)description+=" · "+entry.count+" "+tr("items");
        sub.setText(description);sub.setMaxLines(3);add(details,sub,-1,-2);space(details,5);
        String time=android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(entry.timestamp));
        add(details,txt(time+(entry.preview?" · "+tr("Preview"):""),11,GRAY,400),-1,-2);
        weight(line,details);
        gap(line,8);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));
        tap(line,WHITE,10,()->{historyScrollY=historyScroll==null?0:historyScroll.getScrollY();openHistoryDetails(entry.id);});
        return line;
    }
    private void openHistoryDetails(String id){
        ActivityHistory.Entry entry=history.find(id);
        if(entry==null){folderToast("Activity unavailable");openHistory();return;}
        settingsVisible=true;historyVisible=true;historyDetailId=id;historyScroll=null;
        setContentView(historyDetailsScreen(entry));
    }
    private LinearLayout historyDetailsScreen(ActivityHistory.Entry entry){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Activity details",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Activity history"));tap(back,WHITE,22,this::openHistory);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(20),0,v(12));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        boolean cleanupActivity=entry.category.equals("cleanup"),scan=entry.category.equals("scan");
        boolean done=entry.title.equals("Cleanup preview completed");
        boolean approved=entry.title.equals("Cleanup preview approved");
        boolean protection=entry.category.equals("protection");
        boolean compression=entry.category.equals("photo_compression")||entry.category.equals("compression");
        boolean contacts=entry.category.equals("contacts");
        boolean appManager=entry.category.equals("app_manager");
        boolean removed=entry.title.endsWith("removed")||entry.title.endsWith("disabled");
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(done?0xffe8f6ec:PALE,38));
        mark.addView(new Glyph(this,done?"check":scan?"sparkles":compression?(entry.category.equals("photo_compression")?"images":"video"):contacts?"users":appManager?"apps":removed?"lock":"shield",done?GREEN:BLUE),new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER));
        add(hero,mark,dp(76),dp(76));space(hero,15);add(hero,centered(entry.title,24,INK,700),-1,-2);space(hero,8);
        java.util.Date date=new java.util.Date(entry.timestamp);
        String timestamp=android.text.format.DateFormat.getMediumDateFormat(this).format(date)+" · "+android.text.format.DateFormat.getTimeFormat(this).format(date);
        add(hero,centered(timestamp,13,GRAY,400),-1,-2);space(hero,10);
        String status=entry.preview?"Preview":compression?"Copy saved":protection?(removed?"Protection removed":"Protection added"):"Recorded";
        TextView pill=centered(status,12,entry.preview?BLUE:removed?GRAY:GREEN,600);
        pill.setBackground(round(entry.preview||removed?PALE:0xffe8f6ec,18));pill.setPadding(dp(14),dp(7),dp(14),dp(7));
        add(hero,pill,-2,-2);add(body,hero,-1,-2);space(body,24);
        if(!entry.preview&&(cleanupActivity||scan)){
            LinearLayout actual=card();add(actual,txt(scan?"Media items examined":"Recorded cleanup items",13,GRAY,500),-1,-2);space(actual,8);add(actual,txt(Integer.toString(entry.count),34,INK,700),-1,-2);space(actual,10);add(actual,txt(entry.detail,14,GRAY,400),-1,-2);space(actual,8);add(actual,txt("No amount of recovered storage is assumed.",12,GRAY,400),-1,-2);add(body,actual,-1,-2);
        }else if(cleanupActivity||scan){
            LinearLayout summary=card();summary.setGravity(Gravity.CENTER_HORIZONTAL);pad(summary,16,20);
            add(summary,centered(done?"Preview recovery":approved?"Approved preview size":"Sample opportunities",13,GRAY,400),-1,-2);space(summary,9);
            add(summary,centered(entry.mb==0?"0 GB":ReviewPlan.gb(entry.mb),36,INK,700),-1,-2);space(summary,7);
            if(entry.count>0)add(summary,centered(entry.count+" "+tr(done?"Preview items":approved?"Approved items":"Sample items"),14,GRAY,400),-1,-2);
            add(body,summary,-1,-2);space(body,20);
            add(body,txt(scan?"Sample breakdown":"Cleanup breakdown",14,GRAY,500),-1,-2);space(body,9);
            LinearLayout breakdown=card();pad(breakdown,13,6);
            if(entry.categoryMb!=null&&entry.categoryCounts!=null){
                boolean first=true;
                for(int i=0;i<3;i++){
                    if(entry.categoryMb[i]<=0)continue;
                    if(!first)thinRule(breakdown);first=false;
                    LinearLayout line=row();line.setPadding(0,dp(11),0,dp(11));
                    add(line,new Glyph(this,ReviewPlan.ICONS[i],BLUE),dp(23),dp(23));gap(line,12);
                    LinearLayout labels=column();add(labels,txt(ReviewPlan.NAMES[i],14,INK,600),-1,-2);space(labels,5);
                    add(labels,txt(entry.categoryCounts[i]+" "+tr("items"),12,GRAY,400),-1,-2);weight(line,labels);gap(line,8);
                    add(line,txt(ReviewPlan.gb(entry.categoryMb[i]),14,INK,600),-2,-2);add(breakdown,line,-1,-2);
                }
            }else{
                add(breakdown,centered("Breakdown was not recorded for this activity.",13,GRAY,400),-1,dp(66));
            }
            add(body,breakdown,-1,-2);space(body,19);
            LinearLayout note=card(),line=row();
            add(line,new Glyph(this,"shield",BLUE),dp(25),dp(25));gap(line,12);
            LinearLayout labels=column();
            add(labels,txt(cleanupActivity?"Approved by you":"Sample recommendations viewed",15,INK,600),-1,-2);space(labels,5);
            add(labels,txt(cleanupActivity?"Simulation · No files were deleted.":"Preview only · No real scan performed",12,GRAY,400),-1,-2);
            weight(line,labels);add(note,line,-1,-2);add(body,note,-1,-2);
        }else{
            add(body,txt(compression?"Saved copies":contacts?"Contact activity":appManager?"App activity":"Recorded change",14,GRAY,500),-1,-2);space(body,9);
            LinearLayout info=card();pad(info,16,15);
            add(info,txt(entry.title,15,INK,600),-1,-2);space(info,9);
            TextView target=txt("",16,INK,500);
            target.setText(entry.title.startsWith("Smart protection")?tr(entry.detail):entry.detail);
            target.setTextIsSelectable(true);add(info,target,-1,-2);space(info,10);
            add(info,txt("Status at the time of activity",12,GRAY,400),-1,-2);space(info,5);
            add(info,txt(status,14,removed?GRAY:GREEN,600),-1,-2);
            add(body,info,-1,-2);space(body,19);
            add(body,centered(compression?"This records a saved compressed copy, not reclaimed storage or original deletion.":contacts?"This records verified contact activity, not storage recovery.":appManager?"Android confirmed uninstall and the app is no longer visible; storage recovery is not measured.":"This records a setting change, not a file deletion.",12,GRAY,400),-1,-2);
        }
        space(body,22);LinearLayout footer=row();footer.setGravity(Gravity.CENTER);
        add(footer,new Glyph(this,"lock",GRAY),dp(16),dp(16));gap(footer,6);
        add(footer,txt("Your data. Your decisions.",12,GRAY,400),-2,-2);add(body,footer,-1,-2);
        return root;
    }

    private void showHistoryFilter(View anchor){
        if(preferencePopup!=null)preferencePopup.dismiss();
        LinearLayout menu=column();menu.setPadding(dp(8),dp(6),dp(8),dp(6));menu.setBackground(round(WHITE,16));
        android.widget.PopupWindow popup=new android.widget.PopupWindow(menu,Math.min(dp(205),getResources().getDisplayMetrics().widthPixels-dp(32)),-2,true);
        preferencePopup=popup;popup.setBackgroundDrawable(round(WHITE,16));popup.setElevation(dp(10));popup.setOutsideTouchable(true);popup.setClippingEnabled(true);
        popup.setOnDismissListener(()->{if(preferencePopup==popup)preferencePopup=null;});
        for(int i=0;i<HISTORY_FILTERS.length;i++){
            final int index=i;LinearLayout option=preferenceOption(null,HISTORY_FILTERS[i],historyFilter==i);
            tap(option,WHITE,10,()->{historyFilter=index;historyScrollY=0;popup.dismiss();openHistory();});add(menu,option,-1,dp(46));
        }
        popup.showAsDropDown(anchor,0,-anchor.getHeight(),Gravity.END);
        animatePreferenceMenu(menu);
    }

    private void openProtection(){
        filesVisible=false;foldersVisible=false;settingsVisible=true;agentPrefsVisible=false;protectionVisible=true;setContentView(protectionScreen());
    }
    private LinearLayout protectionScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Cleanup protection",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Back to Settings"));tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"shield",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered("Keep what matters.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Choose what your Agent should never remove.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,24);
        add(body,txt("Smart protection",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout smart=card();pad(smart,13,4);
        String[] names={"Favorite photos","Recent files","Important documents","Best shots"};
        String[] icons={"heart","clock","document","images"};
        for(int i=0;i<names.length;i++){
            final int index=i;
            add(smart,agentToggle(icons[i],names[i],null,prefs.protection(i),value->{prefs.setProtection(index,value);recordHistory(null,"protection",value?"Smart protection enabled":"Smart protection disabled",names[index],0,0,false);}),-1,-2);
            if(i<names.length-1)thinRule(smart);
        }
        add(body,smart,-1,-2);space(body,22);
        add(body,txt("Personal protection",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout personal=card();pad(personal,13,5);
        add(personal,protectionLink("folder_lock","Protected folders","Exclude from cleanup"),-1,-2);
        thinRule(personal);
        add(personal,protectionLink("file_lock","Protected files","Always keep selected files"),-1,-2);
        add(body,personal,-1,-2);space(body,20);
        LinearLayout safety=row();safety.setGravity(Gravity.CENTER);
        add(safety,new Glyph(this,"lock",GREEN),dp(17),dp(17));gap(safety,7);
        add(safety,txt("Your approval is always required.",12,GRAY,400),-2,-2);add(body,safety,-1,-2);
        space(body,10);
        add(body,centered("Preview mode · Protection preferences saved",11,GRAY,400),-1,-2);
        return root;
    }
    private LinearLayout protectionLink(String icon,String name,String description){
        LinearLayout line=row();line.setMinimumHeight(dp(62));line.setPadding(0,dp(10),0,dp(10));
        add(line,new Glyph(this,icon,BLUE),dp(24),dp(24));gap(line,12);
        LinearLayout labels=column();add(labels,txt(name,15,INK,600),-1,-2);space(labels,5);
        add(labels,txt(description,12,GRAY,400),-1,-2);weight(line,labels);gap(line,8);
        add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));
        if(name.equals("Protected folders"))tap(line,WHITE,8,this::openProtectedFolders);
        else if(name.equals("Protected files"))tap(line,WHITE,8,this::openProtectedFiles);
        line.setContentDescription(tr(name)+", "+tr(description));
        return line;
    }

    private void openProtectedFolders(){
        filesVisible=false;settingsVisible=true;agentPrefsVisible=false;protectionVisible=true;foldersVisible=true;
        setContentView(protectedFoldersScreen());
    }
    private LinearLayout protectedFoldersScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        TextView title=centered("Protected folders",17,INK,600);title.setPadding(dp(46),0,dp(46),0);
        header.addView(title,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this),plus=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Cleanup protection"));tap(back,WHITE,22,this::openProtection);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        plus.addView(new Glyph(this,"plus",BLUE),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        plus.setContentDescription(tr("Add folder"));tap(plus,WHITE,22,this::pickProtectedFolder);
        header.addView(plus,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.END|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"folder_lock",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered("Off limits.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Exclude these folders and their subfolders from future cleanup.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,24);
        java.util.List<ProtectedFolders.Entry> entries=protectedFolders.entries();
        LinearLayout section=row();weight(section,txt("Protected folders",14,GRAY,500));
        add(section,txt(Integer.toString(entries.size()),13,GRAY,500),-2,-2);add(body,section,-1,-2);space(body,10);
        LinearLayout list=card();pad(list,13,6);
        if(entries.isEmpty()){
            LinearLayout empty=column();empty.setGravity(Gravity.CENTER_HORIZONTAL);pad(empty,8,24);
            add(empty,new Glyph(this,"folder",GRAY),dp(35),dp(35));space(empty,12);
            add(empty,centered("No protected folders yet",16,INK,600),-1,-2);space(empty,6);
            add(empty,centered("Add a folder you want to keep.",13,GRAY,400),-1,-2);add(list,empty,-1,-2);
        }else for(int i=0;i<entries.size();i++){
            ProtectedFolders.Entry entry=entries.get(i);LinearLayout line=row();line.setPadding(0,dp(10),0,dp(10));
            FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,12));
            icon.addView(new Glyph(this,"folder",BLUE),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
            add(line,icon,dp(44),dp(44));gap(line,10);
            LinearLayout details=column();TextView name=txt(entry.name,15,INK,600);name.setText(entry.name);
            name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(details,name,-1,-2);space(details,5);
            TextView path=txt("",12,GRAY,400);path.setText(entry.location);path.setMaxLines(2);path.setEllipsize(android.text.TextUtils.TruncateAt.END);
            add(details,path,-1,-2);weight(line,details);gap(line,6);
            add(line,new Glyph(this,"shield",GREEN),dp(20),dp(20));
            FrameLayout more=new FrameLayout(this);more.addView(new Glyph(this,"more",GRAY),new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));
            more.setContentDescription(tr("Folder options")+", "+entry.name);tap(more,WHITE,20,()->showFolderMenu(more,entry));
            add(line,more,dp(40),dp(44));add(list,line,-1,-2);
            if(i<entries.size()-1)thinRule(list);
        }
        add(body,list,-1,-2);space(body,19);
        LinearLayout addButton=row();addButton.setGravity(Gravity.CENTER);
        add(addButton,new Glyph(this,"plus",Color.WHITE),dp(21),dp(21));gap(addButton,8);
        add(addButton,txt("Add folder",16,Color.WHITE,600),-2,-2);
        tap(addButton,BLUE,27,this::pickProtectedFolder);add(body,addButton,-1,dp(52));space(body,17);
        add(body,centered("Subfolders are included in each exclusion.",12,GRAY,400),-1,-2);space(body,8);
        add(body,centered("Saved exclusions · Cleanup engine not active",11,GRAY,400),-1,-2);
        return root;
    }
    private void openProtectedFiles(){
        foldersVisible=false;settingsVisible=true;agentPrefsVisible=false;protectionVisible=true;filesVisible=true;
        setContentView(protectedFilesScreen());
    }
    private LinearLayout protectedFilesScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        TextView title=centered("Protected files",17,INK,600);title.setPadding(dp(46),0,dp(46),0);
        header.addView(title,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this),plus=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr("Cleanup protection"));tap(back,WHITE,22,this::openProtection);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        plus.addView(new Glyph(this,"plus",BLUE),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        plus.setContentDescription(tr("Add files"));tap(plus,WHITE,22,this::pickProtectedFiles);
        header.addView(plus,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.END|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"file_lock",BLUE),new FrameLayout.LayoutParams(dp(35),dp(35),Gravity.CENTER));
        add(hero,mark,dp(68),dp(68));space(hero,13);
        add(hero,centered("Too important to lose.",25,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Keep your important files out of cleanup.",14,GRAY,400),-1,-2);
        add(body,hero,-1,-2);space(body,24);
        java.util.List<ProtectedFiles.Entry> entries=protectedFiles.entries();
        LinearLayout section=row();weight(section,txt("Protected files",14,GRAY,500));
        add(section,txt(Integer.toString(entries.size()),13,GRAY,500),-2,-2);add(body,section,-1,-2);space(body,10);
        LinearLayout list=card();pad(list,13,6);
        if(entries.isEmpty()){
            LinearLayout empty=column();empty.setGravity(Gravity.CENTER_HORIZONTAL);pad(empty,8,24);
            add(empty,new Glyph(this,"document",GRAY),dp(35),dp(35));space(empty,12);
            add(empty,centered("No protected files yet",16,INK,600),-1,-2);space(empty,6);
            add(empty,centered("Add files you want to keep.",13,GRAY,400),-1,-2);add(list,empty,-1,-2);
        }else for(int i=0;i<entries.size();i++){
            ProtectedFiles.Entry entry=entries.get(i);LinearLayout line=row();line.setPadding(0,dp(10),0,dp(10));
            FrameLayout icon=new FrameLayout(this);icon.setBackground(round(PALE,12));
            icon.addView(new Glyph(this,entry.icon(),BLUE),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
            add(line,icon,dp(44),dp(44));gap(line,10);
            LinearLayout details=column();TextView name=txt(entry.name,15,INK,600);name.setText(entry.name);
            name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(details,name,-1,-2);space(details,5);
            TextView path=txt("",12,GRAY,400);path.setText(tr(entry.typeLabel())+(entry.size>=0?" · "+android.text.format.Formatter.formatShortFileSize(this,entry.size):""));path.setMaxLines(2);path.setEllipsize(android.text.TextUtils.TruncateAt.END);
            add(details,path,-1,-2);weight(line,details);gap(line,6);
            add(line,new Glyph(this,"shield",GREEN),dp(20),dp(20));
            FrameLayout more=new FrameLayout(this);more.addView(new Glyph(this,"more",GRAY),new FrameLayout.LayoutParams(dp(20),dp(20),Gravity.CENTER));
            more.setContentDescription(tr("File options")+", "+entry.name);tap(more,WHITE,20,()->showFileMenu(more,entry));
            add(line,more,dp(40),dp(44));add(list,line,-1,-2);
            if(i<entries.size()-1)thinRule(list);
        }
        add(body,list,-1,-2);space(body,19);
        LinearLayout addButton=row();addButton.setGravity(Gravity.CENTER);
        add(addButton,new Glyph(this,"plus",Color.WHITE),dp(21),dp(21));gap(addButton,8);
        add(addButton,txt("Add files",16,Color.WHITE,600),-2,-2);
        tap(addButton,BLUE,27,this::pickProtectedFiles);add(body,addButton,-1,dp(52));space(body,17);
        add(body,centered("Excluded from supported cleanup reviews.",12,GRAY,400),-1,-2);space(body,8);
        add(body,centered("Saved exclusions · Cleanup engine not active",11,GRAY,400),-1,-2);
        return root;
    }
    private void pickProtectedFiles(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE,true);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{startActivityForResult(intent,PICK_PROTECTED_FILES);}
        catch(android.content.ActivityNotFoundException e){folderToast("File picker unavailable");}
    }
    private void handleProtectedFiles(android.content.Intent data){
        java.util.LinkedHashSet<android.net.Uri> uris=new java.util.LinkedHashSet<>();
        if(data.getData()!=null)uris.add(data.getData());
        android.content.ClipData clip=data.getClipData();
        if(clip!=null)for(int i=0;i<clip.getItemCount();i++){
            android.net.Uri uri=clip.getItemAt(i).getUri();if(uri!=null)uris.add(uri);
        }
        if(uris.isEmpty())return;
        int added=0,failed=0;
        java.util.Set<String> existing=new java.util.HashSet<>();for(ProtectedFiles.Entry e:protectedFiles.entries())existing.add(e.key);
        for(android.net.Uri uri:uris){
            try{if(protectedFiles.add(uri))added++;}catch(RuntimeException e){failed++;}
        }
        if(added>0)for(ProtectedFiles.Entry e:protectedFiles.entries())if(!existing.contains(e.key))recordHistory(null,"protection","File protection added",e.name,0,0,false);
        folderToast(failed>0?"Some files could not be saved":added>0?"Files added":"Files already protected");
        openProtectedFiles();
    }
    private void showFileMenu(View anchor,ProtectedFiles.Entry entry){
        if(preferencePopup!=null)preferencePopup.dismiss();
        LinearLayout menu=column();menu.setPadding(dp(7),dp(6),dp(7),dp(6));
        android.widget.PopupWindow popup=new android.widget.PopupWindow(menu,Math.min(dp(225),getResources().getDisplayMetrics().widthPixels-dp(32)),-2,true);
        preferencePopup=popup;popup.setBackgroundDrawable(round(WHITE,16));popup.setElevation(dp(10));popup.setOutsideTouchable(true);
        popup.setOnDismissListener(()->{if(preferencePopup==popup)preferencePopup=null;});
        TextView remove=txt("Remove protection",14,0xffe05252,600);remove.setGravity(Gravity.CENTER_VERTICAL);pad(remove,12,8);
        tap(remove,WHITE,10,()->{popup.dismiss();
            new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(tr("Remove protection")).setMessage(tr("This removes the exclusion, not the file."))
                .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Remove"),(d,w)->{
                    if(!protectedFiles.remove(entry.key))folderToast("Could not update protection");
                    else recordHistory(null,"protection","File protection removed",entry.name,0,0,false);
                    openProtectedFiles();
                }).show();
        });
        add(menu,remove,-1,dp(46));popup.showAsDropDown(anchor,0,-anchor.getHeight(),Gravity.END);
    }

    private void pickProtectedFolder(){
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|android.content.Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try{startActivityForResult(intent,PICK_PROTECTED_FOLDER);}
        catch(android.content.ActivityNotFoundException e){folderToast("Folder picker unavailable");}
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(onboardingAccess!=null&&onboardingAccess.activityResult(requestCode,resultCode,data))return;
        if(handleToolPermission(requestCode))return;
        if(requestCode==REQUEST_APP_UNINSTALL){finishUnusedUninstall(resultCode==RESULT_OK);return;}
        if(requestCode==PICK_LARGE_FOLDER){downloadsPicking=false;if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&(largeFilesMode||clutterMode))loadLargeFolder(data.getData());else if(downloadsVisible)showDownloads();return;}
        if(requestCode==PICK_DOWNLOAD_FILES){downloadsPicking=false;if(resultCode==RESULT_OK&&data!=null)loadPickedDownloads(data);else if(downloadsVisible)showDownloads();return;}
        if(requestCode==REQUEST_DUP_TRASH||requestCode==REQUEST_SIMILAR_TRASH||requestCode==REQUEST_VIDEO_TRASH||requestCode==REQUEST_SCREENSHOT_TRASH){finishMediaCleanup(resultCode==RESULT_OK);return;}
        if(requestCode==EXPORT_ACTIVITY_HISTORY){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)writeHistoryExport(data.getData());return;}
        if(requestCode==PICK_PROTECTED_FILES){if(resultCode==RESULT_OK&&data!=null)handleProtectedFiles(data);return;}
        if(requestCode!=PICK_PROTECTED_FOLDER||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            java.util.Set<String> existing=new java.util.HashSet<>();for(ProtectedFolders.Entry e:protectedFolders.entries())existing.add(e.uri);
            boolean added=protectedFolders.add(data.getData());
            if(added)for(ProtectedFolders.Entry e:protectedFolders.entries())if(!existing.contains(e.uri))recordHistory(null,"protection","Folder protection added",e.name,0,0,false);
            folderToast(added?"Folder added":"Folder already protected");
        }catch(RuntimeException e){folderToast("Could not save this folder");}
        openProtectedFolders();
    }
    private void folderToast(String message){android.widget.Toast.makeText(this,tr(message),android.widget.Toast.LENGTH_SHORT).show();}
    private void showFolderMenu(View anchor,ProtectedFolders.Entry entry){
        if(preferencePopup!=null)preferencePopup.dismiss();
        LinearLayout menu=column();menu.setPadding(dp(7),dp(6),dp(7),dp(6));
        android.widget.PopupWindow popup=new android.widget.PopupWindow(menu,Math.min(dp(225),getResources().getDisplayMetrics().widthPixels-dp(32)),-2,true);
        preferencePopup=popup;popup.setBackgroundDrawable(round(WHITE,16));popup.setElevation(dp(10));popup.setOutsideTouchable(true);
        popup.setOnDismissListener(()->{if(preferencePopup==popup)preferencePopup=null;});
        TextView remove=txt("Remove protection",14,0xffe05252,600);remove.setGravity(Gravity.CENTER_VERTICAL);pad(remove,12,8);
        tap(remove,WHITE,10,()->{popup.dismiss();
            new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(tr("Remove protection")).setMessage(tr("This removes the exclusion, not the folder or its files."))
                .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Remove"),(d,w)->{
                    if(!protectedFolders.remove(entry.uri))folderToast("Could not update protection");
                    else recordHistory(null,"protection","Folder protection removed",entry.name,0,0,false);
                    openProtectedFolders();
                }).show();
        });
        add(menu,remove,-1,dp(46));popup.showAsDropDown(anchor,0,-anchor.getHeight(),Gravity.END);
    }

    private LinearLayout agentPreferencesScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(UiKit.gutter(this)),v(12),dp(UiKit.gutter(this)),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Agent preferences",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Settings");tap(back,WHITE,22,this::openSettings);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(18),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(PALE,20));
        mark.addView(new Glyph(this,"bot",BLUE),new FrameLayout.LayoutParams(dp(32),dp(32),Gravity.CENTER));
        add(hero,mark,dp(64),dp(64));space(hero,13);
        add(hero,centered("Your Agent. Your rules.",24,INK,700),-1,-2);space(hero,7);
        add(hero,centered("Choose how Clenova works for you.",14,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,22);
        add(body,txt("Autopilot",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout auto=card();pad(auto,13,5);
        LinearLayout enabled=agentToggle("sparkles","AI Autopilot","Read-only scheduled media discovery",prefs.autopilot(),prefs::setAutopilot);
        add(auto,enabled,-1,-2);thinRule(auto);
        LinearLayout frequency=row();frequency.setMinimumHeight(dp(54));frequency.setPadding(0,dp(7),0,dp(7));
        add(frequency,new Glyph(this,"clock",BLUE),dp(23),dp(23));gap(frequency,12);
        weight(frequency,txt("Scan frequency",15,INK,500));gap(frequency,5);
        TextView current=txt(scanLabel(prefs.scanDays()),12,GRAY,400);add(frequency,current,-2,-2);gap(frequency,6);
        add(frequency,new Glyph(this,"select_arrows",GRAY),dp(14),dp(14));
        frequency.setContentDescription(tr("Scan frequency")+", "+tr(scanLabel(prefs.scanDays())));
        tap(frequency,WHITE,8,()->showScanFrequency(frequency,current));
        add(auto,frequency,-1,-2);add(body,auto,-1,-2);space(body,20);
        add(body,txt("Smart discovery",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout discover=card();pad(discover,13,4);
        String[] names={"Duplicate photos","Similar photos","Large videos","Unnecessary files"};
        String[] icons={"copy","images","video","document"};
        for(int i=0;i<names.length;i++){
            final int index=i;
            add(discover,agentToggle(icons[i],names[i],null,prefs.discovery(i),value->prefs.setDiscovery(index,value)),-1,-2);
            if(i<names.length-1)thinRule(discover);
        }
        add(body,discover,-1,-2);space(body,20);
        add(body,txt("Safety",14,GRAY,500),-1,-2);space(body,9);
        LinearLayout safety=card(),line=row();add(line,new Glyph(this,"shield",GREEN),dp(24),dp(24));gap(line,12);
        LinearLayout text=column();add(text,txt("Approval required",15,INK,600),-1,-2);space(text,5);
        add(text,txt("Nothing deleted without your permission.",12,GRAY,400),-1,-2);weight(line,text);gap(line,8);
        add(line,new Glyph(this,"lock",GREEN),dp(20),dp(20));add(safety,line,-1,-2);
        safety.setContentDescription(tr("Approval required")+", "+tr("Nothing deleted without your permission."));
        add(body,safety,-1,-2);space(body,10);
        add(body,centered("Checks run while charging when Android permits. Unnecessary files require manual review.",11,GRAY,400),-1,-2);
        return root;
    }
    private LinearLayout agentToggle(String icon,String name,String subtitle,boolean initial,java.util.function.Consumer<Boolean> change){
        LinearLayout line=row();line.setMinimumHeight(dp(54));line.setPadding(0,dp(8),0,dp(8));
        add(line,new Glyph(this,icon,BLUE),dp(23),dp(23));gap(line,12);
        LinearLayout labels=column();add(labels,txt(name,15,INK,500),-1,-2);
        if(subtitle!=null){space(labels,4);add(labels,txt(subtitle,12,GRAY,400),-1,-2);}
        weight(line,labels);gap(line,8);
        android.widget.Switch toggle=new android.widget.Switch(this);
        toggle.setShowText(false);toggle.setChecked(initial);toggle.setContentDescription(tr(name));
        toggle.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{BLUE,GRAY}));
        toggle.setOnCheckedChangeListener((button,checked)->change.accept(checked));add(line,toggle,-2,dp(44));
        line.setOnClickListener(view->toggle.setChecked(!toggle.isChecked()));return line;
    }
    private String scanLabel(int days){return days==3?"Every 3 days":days==7?"Weekly":"Daily";}
    private void showScanFrequency(View anchor,TextView current){
        if(preferencePopup!=null)preferencePopup.dismiss();
        LinearLayout menu=column();menu.setPadding(dp(8),dp(6),dp(8),dp(6));menu.setBackground(round(WHITE,16));
        android.widget.PopupWindow popup=new android.widget.PopupWindow(menu,Math.min(dp(205),getResources().getDisplayMetrics().widthPixels-dp(32)),-2,true);
        preferencePopup=popup;popup.setBackgroundDrawable(round(WHITE,16));popup.setElevation(dp(10));popup.setOutsideTouchable(true);popup.setClippingEnabled(true);
        popup.setOnDismissListener(()->{if(preferencePopup==popup)preferencePopup=null;});
        for(int days:new int[]{1,3,7}){
            LinearLayout option=preferenceOption("clock",scanLabel(days),prefs.scanDays()==days);
            tap(option,WHITE,10,()->{prefs.setScanDays(days);current.setText(tr(scanLabel(days)));anchor.setContentDescription(tr("Scan frequency")+", "+tr(scanLabel(days)));popup.dismiss();});
            add(menu,option,-1,dp(46));
        }
        popup.showAsDropDown(anchor,0,-anchor.getHeight(),Gravity.END);
        animatePreferenceMenu(menu);
    }

    private void openStorage(){storageVisible=true;setContentView(storageScreen());}
    private LinearLayout storageScreen(){
        DeviceOverview.Snapshot data=overview();LinearLayout root=scanShell("Storage",this::showHome),body=scanBody(root);
        add(body,txt("Available storage",14,GRAY,400),-1,-2);space(body,6);
        add(body,txt(data.storageKnown()?DeviceOverview.format(data.free):data.finished?"Unavailable":"Checking…",38,INK,700),-1,-2);space(body,14);
        LinearLayout captions=row();weight(captions,txt(data.storageKnown()?DeviceOverview.format(data.used())+" used":data.finished?"Storage unavailable":"Reading storage",13,GRAY,400));
        add(captions,txt(data.storageKnown()?DeviceOverview.format(data.total)+" total":"",13,GRAY,400),-2,-2);add(body,captions,-1,-2);space(body,9);
        if(data.storageKnown()){android.widget.ProgressBar bar=storageProgress();bar.setProgress(HomePresentation.progress(data.total,data.free));add(body,bar,-1,dp(10));space(body,8);add(body,txt(data.usedPercent()+"% used",13,GRAY,500),-1,-2);}space(body,20);
        add(body,txt("Storage overview",18,INK,600),-1,-2);space(body,10);
        LinearLayout breakdown=card();pad(breakdown,14,4);
        overviewRow(breakdown,"images","Photos",data.photos.size(),data.photos.items(),()->{showClean();openRealDuplicates(false);});
        thinRule(breakdown);overviewRow(breakdown,"video","Videos",data.videos.size(),data.videos.items(),()->{showClean();openRealVideos(false);});
        thinRule(breakdown);overviewRow(breakdown,"apps","Visible apps",data.apps<0?"Unavailable":data.apps+" apps",data.usage?"Usage access enabled":"Usage access optional",()->startActivity(new android.content.Intent(this,AppManagerActivity.class)));
        thinRule(breakdown);overviewRow(breakdown,"document","Other used storage",DeviceOverview.format(data.other()),"Includes apps, system and unclassified data",null);
        thinRule(breakdown);overviewRow(breakdown,"folder","File access",data.folder?"Selected folder":"Picker only","Review files when you choose",()->{showClean();openLargeFiles();});
        add(body,breakdown,-1,-2);space(body,16);
        add(body,txt("Internal data volume. Photo and video sizes include accessible primary-volume items only. Other used includes everything not measured separately. App count is launcher-visible; it is not an app storage total.",12,GRAY,400),-1,-2);space(body,9);
        add(body,txt(!data.finished?"Refreshing device overview…":data.partial()?"Limited overview · Some data is unavailable or not fully counted.":"Read from your device · Nothing changed.",12,GRAY,400),-1,-2);
        space(body,16);scanButton(body,"Smart Scan",true,()->{showClean();requestSmartScan();});attachOverviewDashboard(root,true);return root;
    }
    private void overviewRow(LinearLayout parent,String glyph,String name,String value,String detail,Runnable action){
        LinearLayout row=row();row.setMinimumHeight(dp(64));pad(row,0,9);add(row,new Glyph(this,glyph,BLUE),dp(24),dp(24));gap(row,11);
        LinearLayout copy=column();add(copy,txt(name,14,INK,600),-1,-2);space(copy,4);add(copy,txt(detail,11,GRAY,400),-1,-2);weight(row,copy);gap(row,8);add(row,txt(value,13,INK,500),-2,-2);
        if(action!=null){gap(row,5);add(row,new Glyph(this,"chevron",GRAY),dp(14),dp(14));tap(row,WHITE,10,action);}add(parent,row,-1,-2);
    }
    private void finishUnusedUninstall(boolean approved){
        final String pkg=pendingUninstallPackage,name=pendingUninstallName,id=pendingUninstallId;pendingUninstallPackage="";pendingUninstallName="";pendingUninstallId="";
        unusedUninstallPending=false;unusedExternal=false;unusedSelected.clear();verifyingCleanup=true;unusedVisible=false;unusedReview=false;unusedResult=null;
        LinearLayout root=scanShell("Uninstall result",()->{}),body=scanBody(root);add(body,centered("Checking uninstall result",20,INK,600),-1,-2);setContentView(root);
        final ActivityHistory journal=history;
        new Thread(()->{
            boolean absent=false;if(approved&&!pkg.isEmpty())for(int i=0;i<5;i++){if(new AppManagerEngine(this).confirmedAbsent(pkg)){absent=true;break;}try{Thread.sleep(250);}catch(InterruptedException e){break;}}
            final boolean confirmed=absent;final String detail=confirmed?name+" · Android approved and app no longer visible. Storage recovery not measured.":approved?"Android reported success but removal could not be verified.":"Uninstall not confirmed.";
            final boolean logged=!confirmed||journal.add(id,"app_manager","App uninstalled",detail,0,1,false);
            runOnUiThread(()->{if(isDestroyed()||isFinishing())return;verifyingCleanup=false;deviceOverview=null;if(agentTaskOrigin&&activeChat!=null)appendAgent(false,detail,-1);showCleanupOutcome(confirmed?"Uninstall complete":"Uninstall not verified",detail,confirmed?1:0,0,confirmed,logged);});
        },"Clenova-uninstall-receipt").start();
    }
    private void finishMediaCleanup(boolean approved){
        final CleanupReceipt receipt=cleanupReceipt;dupTrashPending=false;verifyingCleanup=true;mediaVerificationPending=true;mediaVerificationApproved=approved;
        scanResult=null;agentLiveResult=null;realDupSelected.clear();realSimilarSelected.clear();realVideoSelected.clear();shotSelected.clear();dupCache.evictAll();
        LinearLayout root=scanShell("Cleanup result",()->{}),body=scanBody(root);add(body,centered("Checking cleanup result",22,INK,600),-1,-2);space(body,14);add(body,new android.widget.ProgressBar(this),dp(44),dp(44));setContentView(root);
        CleanupReceipt.verifyAsync(this,receipt,approved,result->{
            mediaVerificationPending=false;cleanupReceipt=null;verifyingCleanup=false;deviceOverview=null;
            if(agentTaskOrigin&&activeChat!=null)appendAgent(false,result.detail(),-1);
            showCleanupOutcome(result.title(),result.detail(),result.count(),result.bytes,result.complete(),result.logged);
        });
    }
    private void showCleanupOutcome(String title,String detail,int count,long bytes,boolean complete,boolean logged){
        completionVisible=true;scanVisible=false;scanReviewVisible=false;realDupVisible=false;realSimilarVisible=false;realVideoVisible=false;realShotVisible=false;
        realDupReview=false;realSimilarReview=false;realVideoReview=false;realShotReview=false;downloadsVisible=false;downloadsReview=false;
        LinearLayout root=scanShell("Cleanup result",this::leaveCleanupOutcome),body=scanBody(root);
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);FrameLayout mark=new FrameLayout(this);mark.setBackground(round(complete?surface(0xffe8f6ec):PALE,42));
        mark.addView(new Glyph(this,complete?"selected":"shield",complete?GREEN:BLUE),new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.CENTER));add(hero,mark,dp(84),dp(84));space(hero,18);
        add(hero,centered(title,27,INK,700),-1,-2);space(hero,10);add(hero,centered(detail,14,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,22);
        LinearLayout result=card();add(result,txt("Recorded items",13,GRAY,500),-1,-2);space(result,6);add(result,txt(Integer.toString(count),34,INK,700),-1,-2);
        if(bytes>0){space(result,10);add(result,txt("Original file size · "+scanBytes(bytes),14,INK,500),-1,-2);}
        space(result,8);add(result,txt("Actual available storage is refreshed on Home. No recovery amount is assumed.",12,GRAY,400),-1,-2);add(body,result,-1,-2);
        if(!logged){space(body,12);add(body,txt("Activity could not be saved.",12,GRAY,400),-1,-2);}
        scanButton(root,"Back to Home",true,this::showHome);space(root,8);scanButton(root,agentTaskOrigin?"Back to Agent":"Continue cleaning",false,this::leaveCleanupOutcome);setContentView(root);
    }
    private void leaveCleanupOutcome(){completionVisible=false;if(agentTaskOrigin)showAgent();else showClean();}
    private void showHome() {
        toolsVisible=false;toolsScroll=null;toolsScrollY=0;
        if(agentBusy())cancelAgentWork();agentVisible=false;agentTaskVisible=false;agentTaskOrigin=false;agentShortcut=false;restoreAgentWindow();
        scanResult=null;scanVisible=false;scanReviewVisible=false;
        cleanVisible=false;cleanScroll=null;cleanScrollY=0;
        aboutVisible=false;aboutScroll=null;policyVisible=false;termsVisible=false;policyScroll=null;feedbackVisible=false;helpVisible=false;permissionsVisible=false;privacyVisible=false;
        storageVisible=false;
        settingsVisible=false;agentPrefsVisible=false;protectionVisible=false;foldersVisible=false;filesVisible=false;historyVisible=false;
        completionVisible=false;
        setContentView(home());
    }
    private void openReview() {
        showClean();requestSmartScan();
    }
    @Override public void onBackPressed() {
        if(launchSplash!=null){splashHandler.removeCallbacks(finishSplash);launchSplash=null;finish();return;}
        if(preparingPhone!=null){overviewHandler.removeCallbacks(finishPreparation);cancelOverview();preparingPhone=null;preparationReady=false;deviceOverview=null;showAgentSetup(false,prefs.autopilot());return;}
        if(onboardingAgent!=null){onboardingAgent.setActive(false);onboardingAgent=null;if(agentSetupFromTab){agentSetupFromTab=false;applyPalette();showAgent();}else showOnboardingAccess(null);return;}
        if(onboardingAccess!=null){if(onboardingAccess.busy())return;onboardingAccess=null;showOnboardingAutopilot();return;}
        if(onboardingAutopilot!=null){onboardingAutopilot.setActive(false);onboardingAutopilot=null;showOnboardingUnified();return;}
        if(onboardingUnified!=null){onboardingUnified.setActive(false);onboardingUnified=null;showOnboardingIntro();return;}
        if(onboardingIntro!=null){onboardingIntro.setActive(false);onboardingIntro=null;finish();return;}
        if(requestingScan||agentPermissionPending)return;
        if(downloadsBusy){if(downloadsSignal!=null)downloadsSignal.cancel();return;}
        if(unusedUninstallPending||dupTrashPending||verifyingCleanup)return;
        if(unusedReview){if(unusedSignal!=null){unusedSignal.cancel();unusedSignal=null;unusedGeneration++;}showUnusedApps();return;}
        if(unusedVisible){if(agentTaskOrigin)showAgent();else showClean();return;}
        if(downloadsReview){showDownloads();return;}
        if(downloadsVisible){if(agentTaskOrigin)showAgent();else showClean();return;}
        if(realShotPreview){showRealScreenshots();return;}
        if(realShotReview){if(runningScan!=null){runningScan.cancel();runningScan=null;}showRealScreenshots();return;}
        if(realShotVisible){leaveRealScreenshots();return;}
        if(realVideoPreview){showRealVideos();return;}
        if(realVideoReview){if(runningScan!=null){runningScan.cancel();runningScan=null;}showRealVideos();return;}
        if(realVideoVisible){leaveRealVideos();return;}
        if(realSimilarReview){if(runningScan!=null){runningScan.cancel();runningScan=null;}showRealSimilar();return;}
        if(realSimilarVisible){leaveRealSimilar();return;}
        if(realDupReview){if(runningScan!=null){runningScan.cancel();runningScan=null;}showRealDuplicates();return;}
        if(realDupVisible){leaveRealDuplicates();return;}
        if(scanReviewVisible){showScanResults();return;}
        if(scanVisible){cancelSmartScan();return;}
        if(aboutVisible||policyVisible||termsVisible){openSettings();return;}
        if(feedbackVisible){leaveFeedback();return;}
        if(helpVisible||privacyVisible||permissionsVisible){openSettings();return;}
        if(historyVisible&&historyDetailId!=null){openHistory();return;}
        if(historyVisible){openSettings();return;}
        if(filesVisible || foldersVisible){openProtection();return;}
        if(protectionVisible || agentPrefsVisible){openSettings();return;}
        if(settingsVisible || storageVisible){returnToDashboard();return;}
        if(completionVisible){leaveCleanupOutcome();return;}
        if(toolsVisible){showHome();return;}
        if(agentVisible&&agentBusy()){cancelAgentWork();return;}
        if(agentTaskVisible||agentVisible&&activeChat!=null){showHome();return;}
        if(agentVisible){showHome();return;}
        if(cleanVisible){if(agentTaskOrigin)showAgent();else showHome();return;}
        super.onBackPressed();
    }
    private void thinRule(LinearLayout parent) {
        View rule=new View(this);rule.setBackgroundColor(UiPolish.palette(dark).divider);add(parent,rule,-1,dp(1));
    }

    private LinearLayout navigation() {
        LinearLayout outer=row(); LinearLayout pill=row(); pill.setBackground(round(WHITE,34));
        pad(pill,4,4); outer.addView(pill,new LinearLayout.LayoutParams(0,dp(64),1f));
        String[] labels={"Home","Clean","Agent"}; String[] icons={"home","sparkles","bot"};
        for(int i=0;i<labels.length;i++){
            final String label=labels[i]; LinearLayout item=navItem(label,icons[i],!toolsVisible&&(agentVisible?i==2:cleanVisible?i==1:i==0));
            pill.addView(item,new LinearLayout.LayoutParams(0,-1,1f));
        }
        gap(outer,8); LinearLayout tools=navItem("Tools","tools",toolsVisible);
        add(outer,tools,dp(64),dp(64)); return outer;
    }
    private LinearLayout navItem(String label,String icon,boolean active) {
        LinearLayout l=column(); l.setGravity(Gravity.CENTER);
        int tint=active?BLUE:GRAY;
        add(l,new Glyph(this,icon,tint),dp(24),dp(24)); space(l,4);
        add(l,centered(label,11,tint,600),-1,-2);
        tap(l,active?PALE:WHITE,32,()->{
            if(active)return;
            if(label.equals("Home"))showHome();
            else if(label.equals("Clean")){agentTaskOrigin=false;showClean();}
            else if(label.equals("Agent"))showAgent();
            else if(label.equals("Tools"))showTools();
        });
        l.setSelected(active);l.setContentDescription(tr(label)+(active?", selected":"")); return l;
    }

    private static final class Glyph extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String type; private final int tint;
        Glyph(Context c,String type,int tint){super(c);this.type=type;this.tint=tint;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private void path(Canvas c,float... xy){Path a=new Path();a.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)a.lineTo(xy[i],xy[i+1]);c.drawPath(a,p);}
        @Override protected void onDraw(Canvas c){
            float s=Math.min(getWidth(),getHeight())/24f; c.save();
            c.translate((getWidth()-s*24)/2,(getHeight()-s*24)/2);c.scale(s,s);
            p.setColor(tint);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.9f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(type){
                case "home": path(c,3,11,12,3,21,11,21,21,3,21,3,11);path(c,9,21,9,14,15,14,15,21);break;
                case "sparkles":{
                    Path a=new Path();a.moveTo(11,2);a.quadTo(13,9,20,11);a.quadTo(13,13,11,20);a.quadTo(9,13,2,11);a.quadTo(9,9,11,2);c.drawPath(a,p);c.drawCircle(20,4,2,p);break;}
                case "bot": c.drawRoundRect(new RectF(4,7,20,19),4,4,p);path(c,12,7,12,3);
                    p.setStyle(Paint.Style.FILL);c.drawCircle(9,13,1.3f,p);c.drawCircle(15,13,1.3f,p);break;
                case "tools": for(int i=0;i<2;i++)for(int j=0;j<2;j++)c.drawCircle(6+i*12,6+j*12,3.4f,p);break;
                case "tool_swipe": c.drawRoundRect(new RectF(7,4,21,20),3,3,p);path(c,4,7,3,7,3,21,17,21);path(c,10,15,13,12,16,15,18,13);c.drawCircle(11,9,1,p);break;
                case "tool_speaker": path(c,3,9,7,9,12,5,12,19,7,15,3,15,3,9);c.drawArc(new RectF(8,7,19,17),-55,110,false,p);c.drawArc(new RectF(5,3,23,21),-55,110,false,p);break;
                case "tool_speed": c.drawArc(new RectF(3,3,21,21),145,250,false,p);path(c,12,12,17,7);c.drawCircle(12,12,1.3f,p);path(c,8,18,16,18);break;
                case "crown":path(c,2,6,7,11,12,4,17,11,22,6,19,17,5,17,2,6);path(c,5,21,19,21);break;
                case "back":path(c,15,5,8,12,15,19);break;
                case "copy": c.drawRoundRect(new RectF(7,7,21,21),3,3,p);path(c,17,3,3,3,3,17);break;
                case "images": c.drawRoundRect(new RectF(3,4,21,20),3,3,p);c.drawCircle(8,9,1.6f,p);path(c,4,17,10,12,14,16,17,13,21,17);break;
                case "apps": for(int i=0;i<2;i++)for(int j=0;j<2;j++)c.drawRoundRect(new RectF(3+i*10,3+j*10,10+i*10,10+j*10),2,2,p);break;
                case "document":path(c,5,2,14,2,20,8,20,22,5,22,5,2);path(c,14,2,14,8,20,8);path(c,8,13,17,13);path(c,8,17,17,17);break;
                case "phone":c.drawRoundRect(new RectF(6,2,18,22),3,3,p);path(c,10,18,14,18);break;
                case "heart":{
                    Path a=new Path();a.moveTo(12,21);a.cubicTo(8,17,2,13,2,8);a.cubicTo(2,2,9,1,12,6);
                    a.cubicTo(15,1,22,2,22,8);a.cubicTo(22,13,16,17,12,21);a.close();c.drawPath(a,p);break;}
                case "plus":path(c,12,4,12,20);path(c,4,12,20,12);break;
                case "more":p.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++)c.drawCircle(12,5+i*7,1.6f,p);break;
                case "users":c.drawCircle(9,7,3.4f,p);c.drawArc(new RectF(2,12,16,25),180,180,false,p);c.drawArc(new RectF(14,3,21,10),-90,180,false,p);c.drawArc(new RectF(14,12,23,24),-90,90,false,p);break;
                case "activity":path(c,2,12,6,12,9,5,13,19,16,12,22,12);break;
                case "cloud_off":path(c,3,3,21,21);c.drawArc(new RectF(2,9,12,19),80,200,false,p);c.drawArc(new RectF(7,3,19,15),180,160,false,p);path(c,18,10,21,12,22,15,20,18);path(c,6,19,15,19);break;
                case "database":c.drawOval(new RectF(4,2,20,9),p);path(c,4,5,4,19);path(c,20,5,20,19);c.drawArc(new RectF(4,9,20,16),0,180,false,p);c.drawArc(new RectF(4,15,20,22),0,180,false,p);break;
                case "download":path(c,12,3,12,15,7,10);path(c,12,15,17,10);path(c,3,16,3,21,21,21,21,16);break;
                case "trash":path(c,3,6,21,6);path(c,9,6,9,3,15,3,15,6);path(c,5,6,6,21,18,21,19,6);path(c,10,10,10,17);path(c,14,10,14,17);break;
                case "headphones":c.drawArc(new RectF(3,2,21,20),180,180,false,p);c.drawRoundRect(new RectF(3,11,8,20),2,2,p);c.drawRoundRect(new RectF(16,11,21,20),2,2,p);break;
                case "search":c.drawCircle(10,10,6.5f,p);path(c,15,15,21,21);break;
                case "close":path(c,6,6,18,18);path(c,18,6,6,18);break;
                case "chevron_up":path(c,5,15,12,8,19,15);break;
                case "chevron_down":path(c,5,9,12,16,19,9);break;
                case "feedback":path(c,3,3,21,3,21,17,9,17,3,21,3,3);path(c,7,8,17,8);path(c,7,12,14,12);break;
                case "lightbulb":c.drawArc(new RectF(5,2,19,16),155,230,false,p);path(c,6,11,9,16,9,18,15,18,15,16,18,11);path(c,10,21,14,21);break;
                case "bug":c.drawRoundRect(new RectF(7,7,17,21),5,5,p);c.drawArc(new RectF(9,2,15,9),180,180,false,p);path(c,12,9,12,21);path(c,3,7,7,11);path(c,17,11,21,7);path(c,3,14,7,14);path(c,17,14,21,14);path(c,3,21,7,17);path(c,17,17,21,21);break;
                case "send":path(c,3,3,22,12,3,21,7,12,3,3);path(c,7,12,22,12);break;
                case "refresh":c.drawArc(new RectF(3,3,21,21),-75,290,false,p);path(c,21,4,21,10,15,10);break;
                case "share":c.drawCircle(5,12,2.5f,p);c.drawCircle(19,5,2.5f,p);c.drawCircle(19,19,2.5f,p);path(c,7.5f,10.8f,16.5f,6.2f);path(c,7.5f,13.2f,16.5f,17.8f);break;
                case "mail":c.drawRoundRect(new RectF(3,5,21,19),2,2,p);path(c,3,7,12,13,21,7);break;
                case "fingerprint":c.drawArc(new RectF(3,2,21,20),190,205,false,p);c.drawArc(new RectF(6,5,18,17),180,190,false,p);c.drawArc(new RectF(9,8,15,14),180,180,false,p);path(c,12,11,12,16,10,21);path(c,16,13,16,18,14,22);path(c,6,12,6,16,4,19);break;
                case "scan":path(c,3,8,3,3,8,3);path(c,16,3,21,3,21,8);path(c,21,16,21,21,16,21);path(c,8,21,3,21,3,16);path(c,5,12,19,12);break;
                case "screenshot":c.drawRoundRect(new RectF(5,2,19,22),3,3,p);path(c,8,10,8,7,11,7);path(c,13,7,16,7,16,10);path(c,16,14,16,17,13,17);path(c,11,17,8,17,8,14);break;
                case "folder_down":path(c,3,5,10,5,12,8,21,8,21,21,3,21,3,5);path(c,12,10,12,17,8,13);path(c,12,17,16,13);break;
                case "file_x":path(c,5,2,14,2,20,8,20,22,5,22,5,2);path(c,14,2,14,8,20,8);path(c,9,12,15,18);path(c,15,12,9,18);break;
                case "folder":path(c,3,5,10,5,12,8,21,8,21,20,3,20,3,5);break;
                case "folder_lock":path(c,3,18,3,5,10,5,12,8,21,8,21,12);
                    path(c,3,18,10,18);c.drawRoundRect(new RectF(13,15,22,22),2,2,p);c.drawArc(new RectF(15,10,20,18),180,180,false,p);break;
                case "file_lock":path(c,5,2,14,2,20,8,20,11);path(c,14,2,14,8,20,8);path(c,5,2,5,22,10,22);
                    c.drawRoundRect(new RectF(13,15,22,22),2,2,p);c.drawArc(new RectF(15,10,20,18),180,180,false,p);break;
                case "clock":c.drawCircle(12,12,9,p);path(c,12,6,12,12,16,14);break;
                case "chevron":path(c,9,5,16,12,9,19);break;
                case "select_arrows":path(c,8,9,12,5,16,9);path(c,8,15,12,19,16,15);break;
                case "check":path(c,5,12,10,17,20,7);break;
                case "sun":c.drawCircle(12,12,4.5f,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=(float)Math.cos(a),y=(float)Math.sin(a);c.drawLine(12+8*x,12+8*y,12+10*x,12+10*y,p);}break;
                case "moon":{
                    Path a=new Path();a.moveTo(13,2);a.cubicTo(3,1,0,14,7,19);a.cubicTo(14,25,22,18,22,12);
                    a.cubicTo(15,16,8,9,13,2);a.close();c.drawPath(a,p);break;}
                case "language":path(c,2,5,13,5);path(c,7,2,7,5);path(c,4,8,10,14);path(c,11,5,10,10,3,16);
                    path(c,12,21,17,9,22,21);path(c,14,17,20,17);break;
                case "bell":{
                    Path a=new Path();a.moveTo(5,17);a.lineTo(6,14);a.lineTo(6,9);a.cubicTo(6,1,18,1,18,9);
                    a.lineTo(18,14);a.lineTo(20,17);a.close();c.drawPath(a,p);path(c,10,20,12,22,14,20);break;}
                case "history":c.drawArc(new RectF(3,3,21,21),-135,290,false,p);path(c,3,3,3,9,9,9);path(c,12,6,12,12,16,14);break;
                case "lock":c.drawRoundRect(new RectF(4,10,20,22),3,3,p);c.drawArc(new RectF(7,2,17,15),180,180,false,p);path(c,12,15,12,18);break;
                case "help":c.drawCircle(12,12,10,p);c.drawArc(new RectF(8,5,16,12),180,235,false,p);path(c,12,12,12,14);p.setStyle(Paint.Style.FILL);c.drawCircle(12,17,1,p);break;
                case "message":path(c,3,3,21,3,21,17,9,17,3,21,3,3);path(c,7,8,17,8);path(c,7,12,14,12);break;
                case "star":{
                    Path a=new Path();for(int i=0;i<10;i++){double ang=Math.PI*i/5-Math.PI/2;float r=i%2==0?10:4.8f;float x=12+r*(float)Math.cos(ang),y=12+r*(float)Math.sin(ang);if(i==0)a.moveTo(x,y);else a.lineTo(x,y);}a.close();c.drawPath(a,p);break;}
                case "document_check":path(c,5,2,14,2,20,8,20,22,5,22,5,2);path(c,14,2,14,8,20,8);path(c,8,14,11,17,17,11);break;
                case "info":c.drawCircle(12,12,10,p);path(c,12,11,12,17);p.setStyle(Paint.Style.FILL);c.drawCircle(12,7,1.2f,p);break;
                case "play":path(c,8,4,20,12,8,20,8,4);break;
                case "video": c.drawRoundRect(new RectF(3,6,16,18),3,3,p);path(c,16,10,21,7,21,17,16,14);break;
                case "selected": p.setStyle(Paint.Style.FILL);c.drawCircle(12,12,11,p);p.setStyle(Paint.Style.STROKE);p.setColor(Color.WHITE);path(c,7,12,10.5f,15.5f,17,9);break;
                case "unselected": c.drawCircle(12,12,10,p);break;
                case "right":path(c,4,12,20,12);path(c,14,6,20,12,14,18);break;
                case "agent_menu":path(c,3,8,21,8);path(c,3,15,15,15);break;
                case "new_chat":path(c,14,4,4,4,4,20,20,20,20,11);path(c,11,13,13,8,19,2,22,5,16,11,11,13);break;
                case "arrow_up":path(c,12,20,12,4);path(c,5,11,12,4,19,11);break;
                case "up":path(c,6,18,18,6);path(c,10,6,18,6,18,14);break;
                case "shield":path(c,12,2,21,6,20,14,17,19,12,22,7,19,4,14,3,6,12,2);path(c,8,12,11,15,16,9);break;
                case "settings":{
                    Path a=new Path();for(int i=0;i<32;i++){double ang=Math.PI*2*i/32;float r=(i%4==0||i%4==3)?8:10;float x=12+r*(float)Math.cos(ang),y=12+r*(float)Math.sin(ang);if(i==0)a.moveTo(x,y);else a.lineTo(x,y);}a.close();c.drawPath(a,p);c.drawCircle(12,12,3.4f,p);break;}
                default:break;
            }
            c.restore();
        }
    }
}

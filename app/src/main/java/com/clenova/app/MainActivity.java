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
    private boolean reviewing;
    private boolean cleanVisible;
    private static final int REQUEST_SCAN_MEDIA=96;
    private SmartMediaScan runningScan;
    private SmartMediaScan.Result scanResult;
    private boolean scanVisible, scanReviewVisible, requestingScan;
    private final boolean[] scanSelected=new boolean[3];
    private TextView scanStatus,scanProgress;
    private android.widget.ProgressBar scanBar;
    private ScrollView cleanScroll;
    private int cleanScrollY;
    private ReviewPlan reviewPlan;
    private boolean duplicateViewing;
    private int duplicateTab;
    private boolean[] duplicateDraft;
    private boolean similarViewing;
    private int similarTab;
    private boolean[] similarDraft;
    private boolean videoViewing;
    private int videoTab;
    private boolean[] videoDraft;
    private CleanupPreview cleanup;
    private boolean cleaning, completed, completionVisible;
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
    private String cleanupEventId, historyDetailId;
    private int[] cleanupCategoryCounts;
    private ScrollView historyScroll;
    private int historyScrollY;
    private ProtectedFiles protectedFiles;
    private static final int PICK_PROTECTED_FILES=93;
    private ProtectedFolders protectedFolders;
    private static final int PICK_PROTECTED_FOLDER=92;
    private ValueAnimator cleanupAnimation;
    private AppPreferences prefs;
    private android.widget.PopupWindow preferencePopup;
    private boolean dark;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=new AppPreferences(this);
        protectedFolders=new ProtectedFolders(this);
        protectedFiles=new ProtectedFiles(this);
        history=new ActivityHistory(this);
        devicePermissions=new DevicePermissions(this);
        if(state!=null){cleanVisible=state.getBoolean("clean_visible");cleanScrollY=state.getInt("clean_scroll_y");historyFilter=state.getInt("history_filter");cleanupEventId=state.getString("cleanup_event_id");cleanupCategoryCounts=state.getIntArray("cleanup_category_counts");historyDetailId=state.getString("history_detail_id");historyScrollY=state.getInt("history_scroll_y");}
        applyPalette();
        density = getResources().getDisplayMetrics().density;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        int height = getResources().getConfiguration().screenHeightDp;
        compact = height < 740 ? .88f : 1f;
        if(state!=null && state.containsKey("cleanup_amounts")){
            cleanup=new CleanupPreview(state.getDoubleArray("cleanup_amounts"),state.getInt("cleanup_count"));
            completed=state.getBoolean("cleanup_completed");
            if(state.getBoolean("cleanup_running")){startCleanupAnimation();return;}
            if(completed && state.getBoolean("cleanup_visible")){showCompletion(false);return;}
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
        if(cleanVisible){showClean();return;}
        setContentView(home());
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putBoolean("storage_visible",storageVisible);
        state.putBoolean("clean_visible",cleanVisible);
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
        state.putString("cleanup_event_id",cleanupEventId);
        state.putIntArray("cleanup_category_counts",cleanupCategoryCounts);
        state.putString("history_detail_id",historyDetailId);
        state.putInt("history_scroll_y",historyDetailId==null&&historyScroll!=null?historyScroll.getScrollY():historyScrollY);
        if(cleanup!=null){
            state.putDoubleArray("cleanup_amounts",cleanup.amounts());
            state.putInt("cleanup_count",cleanup.count);
            state.putBoolean("cleanup_completed",completed);
            state.putBoolean("cleanup_running",cleaning);
            state.putBoolean("cleanup_visible",completionVisible);
        }
    }
    @Override protected void onDestroy(){
        if(cleanupAnimation!=null){
            cleanupAnimation.removeAllListeners();cleanupAnimation.removeAllUpdateListeners();cleanupAnimation.cancel();
        }
        if(runningScan!=null){runningScan.cancel();runningScan=null;}
        if(preferencePopup!=null)preferencePopup.dismiss();
        if(ratingDialog!=null)ratingDialog.dismiss();
        super.onDestroy();
    }
    private void applyPalette(){
        dark=prefs.appearance().equals("Dark") || (prefs.appearance().equals("System") &&
            (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);
        BG=dark?0xff141416:0xfff5f5f7;
        WHITE=dark?0xff242426:Color.WHITE;
        INK=dark?0xfff4f4f6:0xff111111;
        GRAY=dark?0xffa2a2aa:0xff86868b;
        BLUE=dark?0xff409cff:0xff007aff;
        PALE=dark?0xff19334c:0xffe6f0ff;
        GREEN=dark?0xff61d883:0xff27984d;
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
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
    private StoragePreview storageData(){return new StoragePreview(completed?cleanup:null);}
    private double previewAvailable(){return storageData().available();}
    private double previewRecovered(){return storageData().recovered;}
    private double previewUsed(){return storageData().used();}
    private String gbNumber(double mb){return ReviewPlan.gb(mb).replace(" GB","");}

    private int dp(float n) { return Math.round(n * density); }
    private int v(float n) { return dp(n * compact); }
    private GradientDrawable round(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(surface(color)); shape.setCornerRadius(dp(radius)); return shape;
    }
    private void tap(View view, int color, float radius, Runnable action) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x19000000),
                round(color,radius), round(WHITE,radius)));
        view.setClickable(true); view.setFocusable(true);
        view.setOnClickListener(w -> {
            w.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            action.run();
        });
    }
    private TextView txt(String s, float size, int color, int weight) {
        TextView t = new TextView(this);
        t.setText(tr(s)); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create(weight >= 600 ? "sans-serif-medium" :
                "sans-serif", weight >= 700 ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false); return t;
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
        LinearLayout l=column(); l.setBackground(round(WHITE,22)); pad(l,16,14); return l;
    }
    private void demo(String title, String message) {
        new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert).setTitle(tr(title)).setMessage(tr(message))
                .setPositiveButton(tr("OK"),null).show();
    }
    private LinearLayout home() {
        LinearLayout root=column(); root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(10));
        add(root,header(),-1,dp(44)); space(root,8);

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(false); scroller.setVerticalScrollBarEnabled(false);
        scroller.setClipToPadding(false); scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();
        body.setPadding(0,v(4),0,v(8));
        scroller.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroller,new LinearLayout.LayoutParams(-1,0,1f));
        add(body,hero(),-1,-2); space(body,16);
        add(body,storage(),-1,-2); space(body,17);
        LinearLayout section=row();
        add(section,txt("Agent update",23,INK,700),-2,-2);
        stretch(section); add(section,txt("Just now",12,GRAY,400),-2,-2);
        add(body,section,-1,-2); space(body,10);
        add(body,agentCard(),-1,-2); space(body,12);
        LinearLayout privacy=row(); privacy.setGravity(Gravity.CENTER);
        add(privacy,new Glyph(this,"shield",GRAY),dp(15),dp(15)); gap(privacy,5);
        add(privacy,txt("Always in your control.",12,GRAY,400),-2,-2);
        add(body,privacy,-1,-2);
        space(root,6); add(root,navigation(),-1,dp(64));
        return root;
    }
    private void returnToDashboard(){
        if(cleanVisible)showClean();else showHome();
    }
    private void showClean(){
        scanVisible=false;scanReviewVisible=false;cleanVisible=true;settingsVisible=false;storageVisible=false;reviewing=false;
        aboutVisible=false;policyVisible=false;termsVisible=false;feedbackVisible=false;helpVisible=false;
        permissionsVisible=false;privacyVisible=false;historyVisible=false;agentPrefsVisible=false;protectionVisible=false;foldersVisible=false;filesVisible=false;
        setContentView(cleanScreen());
        final ScrollView scroll=cleanScroll;if(scroll!=null)scroll.post(()->scroll.scrollTo(0,cleanScrollY));
    }
    private LinearLayout cleanScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(10));
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
        space(copy,5);add(copy,txt(scanResult==null?"Not scanned yet":"Last scan · Review candidates",12,GRAY,500),-1,-2);weight(top,copy);gap(top,10);
        FrameLayout ring=new FrameLayout(this);ring.addView(new CleanScanMark(this),new FrameLayout.LayoutParams(-1,-1));add(top,ring,dp(92),dp(92));add(hero,top,-1,-2);space(hero,17);
        LinearLayout scan=row();scan.setGravity(Gravity.CENTER);add(scan,new Glyph(this,"scan",Color.WHITE),dp(21),dp(21));gap(scan,8);add(scan,txt("Smart Scan",16,Color.WHITE,600),-2,-2);
        tap(scan,BLUE,26,this::requestSmartScan);add(hero,scan,-1,dp(50));
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
                add(tile,txt("—",23,INK,700),-1,-2);space(tile,5);add(tile,txt(item[1],14,INK,600),-1,-2);space(tile,4);
                add(tile,txt("Scan to find",12,GRAY,400),-1,-2);
                tap(tile,WHITE,18,()->cleanPending(item[1]));tile.setContentDescription(tr(item[1])+", "+tr("Not scanned yet"));
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
            weight(line,labels);gap(line,8);add(line,new Glyph(this,"chevron",GRAY),dp(14),dp(14));tap(line,WHITE,10,()->cleanPending(item[1]));add(files,line,-1,-2);
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
    private void requestSmartScan(){
        if(requestingScan||runningScan!=null)return;
        if(SmartMediaScan.hasAccess(this)){startSmartScan();return;}
        privacyDialog().setTitle(tr("Media access needed"))
            .setMessage(tr("Smart Scan reads only the photos and videos you allow. Nothing is deleted."))
            .setNegativeButton(tr("Cancel"),null).setPositiveButton(tr("Continue"),(d,w)->{
                requestingScan=true;
                if(android.os.Build.VERSION.SDK_INT>=34)requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES,android.Manifest.permission.READ_MEDIA_VIDEO,android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED},REQUEST_SCAN_MEDIA);
                else if(android.os.Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES,android.Manifest.permission.READ_MEDIA_VIDEO},REQUEST_SCAN_MEDIA);
                else requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_SCAN_MEDIA);
            }).show();
    }
    private LinearLayout scanShell(String title,Runnable back){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        final SmartMediaScan job=new SmartMediaScan(this,prefs,protectedFiles,protectedFolders);runningScan=job;
        LinearLayout root=scanShell("Smart Scan",this::cancelSmartScan),body=scanBody(root);
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout ring=new FrameLayout(this);ring.addView(new CleanScanMark(this),new FrameLayout.LayoutParams(-1,-1));add(hero,ring,dp(164),dp(164));space(hero,18);
        scanProgress=centered("Scanning",23,INK,700);add(hero,scanProgress,-1,-2);space(hero,8);
        add(hero,centered("Finding what you don't need.",18,INK,600),-1,-2);space(hero,6);
        add(hero,centered("Analyzing accessible photos and videos.",13,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,20);
        LinearLayout status=card();scanStatus=txt("Reading media library",15,INK,600);add(status,scanStatus,-1,-2);space(status,13);
        scanBar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        scanBar.setIndeterminate(true);scanBar.setIndeterminateTintList(ColorStateList.valueOf(BLUE));scanBar.setProgressTintList(ColorStateList.valueOf(BLUE));add(status,scanBar,-1,dp(8));space(status,12);
        add(status,txt("Duplicate photos · Large videos · Screenshots",12,GRAY,400),-1,-2);add(body,status,-1,-2);space(body,16);
        add(body,centered("Your files are safe during scanning.",12,GREEN,500),-1,-2);
        scanButton(root,"Cancel scan",false,this::cancelSmartScan);setContentView(root);
        new Thread(()->{
            try{
                SmartMediaScan.Result result=job.run((stage,done,total)->{
                    if(done!=total&&done%16!=0)return;
                    runOnUiThread(()->{if(runningScan!=job||isFinishing()||isDestroyed())return;
                        scanStatus.setText(tr(new String[]{"Reading media library","Checking exact duplicates","Classifying media"}[stage]));
                        scanBar.setIndeterminate(total<=0);if(total>0){scanBar.setMax(total);scanBar.setProgress(done);scanProgress.setText(done+" / "+total);}else scanProgress.setText(tr("Scanning"));
                    });
                });
                runOnUiThread(()->{if(runningScan!=job||isFinishing()||isDestroyed())return;runningScan=null;scanResult=result;
                    java.util.Arrays.fill(scanSelected,false);showScanResults();});
            }catch(Exception e){runOnUiThread(()->{if(runningScan!=job||isFinishing()||isDestroyed())return;runningScan=null;showClean();folderToast("Scan could not finish");});}
        },"Clenova-media-scan").start();
    }
    private void cancelSmartScan(){
        if(runningScan!=null){runningScan.cancel();runningScan=null;}
        scanVisible=false;scanReviewVisible=false;showClean();
    }
    private void showScanResults(){
        if(scanResult==null){showClean();return;}
        scanVisible=true;scanReviewVisible=false;
        LinearLayout root=scanShell("Scan results",this::cancelSmartScan),body=scanBody(root);
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);add(hero,new Glyph(this,"selected",BLUE),dp(62),dp(62));space(hero,12);
        add(hero,centered(scanResult.partial?"Partial scan results":"Scan complete",15,BLUE,600),-1,-2);space(hero,8);
        add(hero,centered(scanBytes(scanResult.total()),35,INK,700),-1,-2);space(hero,5);add(hero,centered("Potential space to recover",14,GRAY,400),-1,-2);
        space(hero,7);add(hero,centered(scanResult.examined+" "+tr("media items checked"),12,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,20);
        add(body,txt("Cleanup opportunities",18,INK,700),-1,-2);space(body,10);
        LinearLayout list=card();pad(list,13,4);TextView review=centered("Review cleanup",16,Color.WHITE,600);
        for(int i=0;i<3;i++){
            if(i>0)thinRule(list);final int index=i;LinearLayout line=row();line.setMinimumHeight(dp(65));line.setPadding(0,dp(9),0,dp(9));
            add(line,new Glyph(this,new String[]{"copy","video","screenshot"}[i],BLUE),dp(25),dp(25));gap(line,12);
            LinearLayout labels=column();add(labels,txt(SmartMediaScan.LABELS[i],15,INK,600),-1,-2);space(labels,5);
            boolean scanned=i==1?scanResult.videosScanned:scanResult.imagesScanned;
            add(labels,txt(scanned?scanResult.groups.get(i).size()+" · "+scanBytes(scanResult.bytes(i)):tr("Not scanned"),12,GRAY,400),-1,-2);weight(line,labels);
            android.widget.CheckBox check=new android.widget.CheckBox(this);check.setButtonTintList(ColorStateList.valueOf(BLUE));check.setChecked(scanSelected[i]);check.setEnabled(!scanResult.groups.get(i).isEmpty());check.setContentDescription(tr(SmartMediaScan.LABELS[i]));check.setOnCheckedChangeListener((v,checked)->{scanSelected[index]=checked;updateScanReview(review);});add(line,check,dp(44),dp(44));add(list,line,-1,-2);
        }
        add(body,list,-1,-2);space(body,14);
        LinearLayout scope=card();add(scope,txt("Scan coverage",15,INK,600),-1,-2);space(scope,8);
        add(scope,txt("Similar photos, downloads, other files and unused apps are not scanned in this build.",13,GRAY,400),-1,-2);
        if(scanResult.limited){space(scope,7);add(scope,txt("Limited media access. Only accessible items were checked.",13,GRAY,400),-1,-2);}
        if(scanResult.partial){space(scope,7);add(scope,txt("Some media could not be checked or the scan limit was reached.",13,GRAY,400),-1,-2);}
        space(scope,7);add(scope,txt("Large videos: 100 MiB or more. Screenshots: folder-based detection.",12,GRAY,400),-1,-2);
        add(body,scope,-1,-2);space(body,12);
        LinearLayout protection=card();add(protection,txt("Protection enabled",15,GREEN,600),-1,-2);space(protection,6);add(protection,txt("Protected or uncertain items are excluded. At least one exact duplicate is retained.",13,GRAY,400),-1,-2);add(body,protection,-1,-2);
        tap(review,BLUE,26,this::showScanReview);updateScanReview(review);add(root,review,-1,dp(52));space(root,7);
        add(root,centered("Nothing is deleted without your approval.",12,GRAY,400),-1,-2);setContentView(root);
    }
    private void updateScanReview(TextView button){
        long bytes=0;for(int i=0;i<3;i++)if(scanSelected[i])bytes+=scanResult.bytes(i);
        button.setEnabled(bytes>0);button.setAlpha(bytes>0?1f:.45f);button.setText(tr("Review cleanup")+" · "+scanBytes(bytes));
    }
    private void showScanReview(){
        if(scanResult==null){showClean();return;}
        scanReviewVisible=true;scanVisible=true;LinearLayout root=scanShell("Review cleanup",this::showScanResults),body=scanBody(root);
        add(body,txt("Review selected media",21,INK,700),-1,-2);space(body,8);
        add(body,txt("Read-only review. File deletion will be enabled in a later phase.",13,GRAY,400),-1,-2);space(body,14);
        for(int i=0;i<3;i++)if(scanSelected[i]){
            add(body,txt(SmartMediaScan.LABELS[i],15,INK,600),-1,-2);space(body,8);
            LinearLayout group=card();int shown=0;
            for(SmartMediaScan.Item item:scanResult.groups.get(i)){
                if(shown==100)break;if(shown++>0)thinRule(group);LinearLayout line=row();line.setMinimumHeight(dp(48));
                TextView name=txt(item.name,13,INK,500);name.setMaxLines(2);weight(line,name);gap(line,8);add(line,txt(scanBytes(item.bytes),12,GRAY,400),-2,-2);add(group,line,-1,-2);
            }
            int remaining=scanResult.groups.get(i).size()-shown;if(remaining>0){space(group,8);add(group,txt(remaining+" "+tr("more items not shown"),12,GRAY,400),-1,-2);}
            add(body,group,-1,-2);space(body,15);
        }
        scanButton(root,"Done",true,this::showScanResults);setContentView(root);
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
        LinearLayout pro=row(); pro.setGravity(Gravity.CENTER); pad(pro,11,5);
        add(pro,new Glyph(this,"crown",BLUE),dp(18),dp(18)); gap(pro,4);
        add(pro,txt("Pro",14,BLUE,600),-2,-2);
        tap(pro,PALE,24,()->demo("Clenova Pro","Preview only. Subscription is not implemented."));
        add(l,pro,-2,dp(38)); gap(l,8);
        FrameLayout settings=new FrameLayout(this);
        settings.addView(new Glyph(this,"settings",INK),new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER));
        settings.setContentDescription("Settings");
        tap(settings,WHITE,24,this::openSettings);
        add(l,settings,dp(44),dp(44)); return l;
    }
    private void openSettings(){
        scanResult=null; // protection rules may change in settings; re-scan before recommending again.
        if(cleanScroll!=null){cleanScrollY=cleanScroll.getScrollY();cleanScroll=null;}
        aboutVisible=false;aboutScroll=null;policyVisible=false;termsVisible=false;policyScroll=null;feedbackVisible=false;helpVisible=false;hideHelpKeyboard();permissionsVisible=false;privacyVisible=false;
        historyDetailId=null;historyScroll=null;historyScrollY=0;historyVisible=false;filesVisible=false;foldersVisible=false;protectionVisible=false;agentPrefsVisible=false;settingsVisible=true;setContentView(settingsScreen());
    }
    private LinearLayout settingsScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Settings",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription(tr(cleanVisible?"Back to Clean":"Back to Home"));tap(back,WHITE,22,this::returnToDashboard);
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
            for(int i=0;i<AppPreferences.NOTIFICATION_KEYS.length;i++){
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
            if(!prefs.systemNotificationsAllowed()){
                TextView settings=centered("Enable in Android settings",12,BLUE,600);
                tap(settings,WHITE,8,()->{popup.dismiss();android.content.Intent intent=new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(intent);});
                add(menu,settings,-1,dp(42));
            }
        }
        // Overlay the anchor row; do not resize or expand Settings.
        popup.showAsDropDown(anchor,0,-anchor.getHeight(),Gravity.END);
        menu.setAlpha(0f);menu.setScaleX(.97f);menu.setScaleY(.97f);
        menu.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start();
    }
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
        add(info,txt(prefs.autopilot()?"No sample recommendations":"Autopilot paused",16,INK,600),-1,-2);space(info,5);
        add(info,txt(prefs.autopilot()?"Manage discovery preferences":"You can resume anytime.",13,GRAY,400),-1,-2);weight(top,info);
        add(card,top,-1,-2);space(card,14);
        TextView manage=centered("Agent preferences",15,BLUE,600);
        tap(manage,PALE,24,this::openAgentPreferences);add(card,manage,-1,dp(48));return card;
    }
    private void openAgentPreferences(){
        settingsVisible=true;agentPrefsVisible=true;setContentView(agentPreferencesScreen());
    }
    @Override protected void onResume(){
        super.onResume();
        if(permissionsVisible&&devicePermissions!=null)openAppPermissions();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_SCAN_MEDIA){requestingScan=false;if(SmartMediaScan.hasAccess(this))startSmartScan();else privacyDialog().setTitle(tr("Media access needed")).setMessage(tr("Allow access to photos or videos to scan them. No files were scanned.")).setPositiveButton(tr("Done"),null).show();return;}
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
        handle.setContentDescription(tr("Swipe down to dismiss"));
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
                        }else sheet.animate().translationY(0).setDuration(160).start();
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
            window.getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            // Constrain tall sheets on short/landscape displays while keeping content scrollable.
            int maxHeight=Math.max(dp(160),getResources().getDisplayMetrics().heightPixels-dp(100));
            scroll.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,getResources().getDisplayMetrics().widthPixels-dp(44)),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            if(scroll.getMeasuredHeight()>maxHeight)scroll.getLayoutParams().height=maxHeight;
            scroll.requestLayout();
        }
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(BLUE,24));
        // Use the existing in-app brand glyph until the final logo asset is added to the project.
        mark.addView(new Glyph(this,"sparkles",Color.WHITE),new FrameLayout.LayoutParams(dp(50),dp(50),Gravity.CENTER));
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
        if(PUBLIC_LISTING_URL.isEmpty()){space(intro,9);add(intro,txt("Preview build · Manual media scan is read-only. Home cleanup is simulated.",12,GRAY,400),-1,-2);}
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));root.setFocusableInTouchMode(true);
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

    private static final String[][] HELP_FAQS={{"How does the AI Agent work?","The Agent is designed to find cleanup opportunities for your approval. Home shows sample recommendations; Smart Scan reads accessible media on request. Automatic scanning is not active yet."},
        {"Can Clenova delete files automatically?","No. Cleanup requires your approval. In this preview, cleanup is simulated and no personal files are deleted."},
        {"How do I protect important files?","Open Settings > Cleanup protection > Protected files or Protected folders. Select items with the Android picker. Exclusions are saved locally; the real cleanup engine is not active yet."},
        {"Why does Clenova need permissions?","Permissions support specific features. Open Settings > App permissions to manage access. Smart Scan requests optional photo and video access. Notification access is separate; file selection uses the Android picker."},
        {"Is my personal data safe?","Preferences, exclusions and activity history are stored locally. Clenova does not automatically upload personal files. You can export or clear your history in Privacy & data."},
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        boolean ok=LocalAppData.reset(this);
        if(cleanupAnimation!=null){cleanupAnimation.removeAllListeners();cleanupAnimation.removeAllUpdateListeners();cleanupAnimation.cancel();cleanupAnimation=null;}
        cleanup=null;cleanupCategoryCounts=null;cleanupEventId=null;reviewPlan=null;
        duplicateDraft=null;similarDraft=null;videoDraft=null;
        duplicateViewing=false;similarViewing=false;videoViewing=false;reviewing=false;
        cleaning=false;completed=false;completionVisible=false;historyDetailId=null;historyScroll=null;historyScrollY=0;historyFilter=0;
        prefs=new AppPreferences(this);protectedFolders=new ProtectedFolders(this);protectedFiles=new ProtectedFiles(this);history=new ActivityHistory(this);
        applyPalette();openPrivacyData();folderToast(ok?"Clenova data reset":"Some app data could not be reset");
    }

    private void openAppPermissions(){
        permissionsVisible=true;settingsVisible=true;setContentView(appPermissionsScreen());
    }
    private LinearLayout appPermissionsScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        add(access,permissionRow("images","Photos & videos","Media scanning is not active yet",snapshot.media,this::openAndroidAppSettings),-1,-2);
        thinRule(access);
        add(access,permissionRow("folder","Files & storage","Select files and folders using Android",snapshot.storage,this::openStoragePermissionInfo),-1,-2);
        thinRule(access);
        add(access,permissionRow("bell","Notifications","Agent updates and reminders",snapshot.notifications,this::manageNotificationAccess),-1,-2);
        thinRule(access);
        add(access,permissionRow("users","Contacts","Contact cleanup is not active yet",snapshot.contacts,this::openAndroidAppSettings),-1,-2);
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
    private static final String[] HISTORY_FILTERS={"All activity","Scans","Cleanups","Protection"};
    private static final String[] HISTORY_CATEGORIES={"","scan","cleanup","protection"};
    private LinearLayout historyScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        add(hero,centered("See what your Agent has been up to.",14,GRAY,400),-1,-2);add(body,hero,-1,-2);space(body,22);
        java.util.List<ActivityHistory.Entry> all=history.entries();
        double mb=0;long items=0;
        for(ActivityHistory.Entry entry:all)if(entry.preview&&entry.title.equals("Cleanup preview completed")){mb+=entry.mb;items+=entry.count;}
        LinearLayout stats=card(),metrics=row(),left=column(),right=column();
        add(left,txt("Preview recovery",12,GRAY,400),-1,-2);space(left,6);
        add(left,txt(mb==0?"0 GB":ReviewPlan.gb(mb),25,INK,700),-1,-2);weight(metrics,left);
        View rule=new View(this);rule.setBackgroundColor(surface(0xffe5e5ea));add(metrics,rule,dp(1),dp(44));gap(metrics,12);
        add(right,txt("Preview items",12,GRAY,400),-1,-2);space(right,6);
        add(right,txt(Long.toString(items),25,INK,700),-1,-2);weight(metrics,right);add(stats,metrics,-1,-2);
        space(stats,10);add(stats,txt("Simulation totals · No real files deleted",11,GRAY,400),-1,-2);
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
            if(historyFilter!=0&&!HISTORY_CATEGORIES[historyFilter].equals(entry.category))continue;
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
        boolean done=entry.title.equals("Cleanup preview completed");
        String icon=done?"check":entry.category.equals("scan")?"sparkles":entry.category.equals("protection")?"shield":"shield";
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
    private void recordCleanupHistory(boolean complete){
        if(!history.add(cleanupEventId+(complete?":complete":":approved"),"cleanup",
            complete?"Cleanup preview completed":"Cleanup preview approved",complete?"No files deleted":"Approved by you",
            cleanup.total,cleanup.count,true,cleanup.amounts(),cleanupCategoryCounts))folderToast("Could not save activity");
    }
    private void openHistoryDetails(String id){
        ActivityHistory.Entry entry=history.find(id);
        if(entry==null){folderToast("Activity unavailable");openHistory();return;}
        settingsVisible=true;historyVisible=true;historyDetailId=id;historyScroll=null;
        setContentView(historyDetailsScreen(entry));
    }
    private LinearLayout historyDetailsScreen(ActivityHistory.Entry entry){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        boolean removed=entry.title.endsWith("removed")||entry.title.endsWith("disabled");
        LinearLayout hero=column();hero.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(done?0xffe8f6ec:PALE,38));
        mark.addView(new Glyph(this,done?"check":scan?"sparkles":removed?"lock":"shield",done?GREEN:BLUE),new FrameLayout.LayoutParams(dp(36),dp(36),Gravity.CENTER));
        add(hero,mark,dp(76),dp(76));space(hero,15);add(hero,centered(entry.title,24,INK,700),-1,-2);space(hero,8);
        java.util.Date date=new java.util.Date(entry.timestamp);
        String timestamp=android.text.format.DateFormat.getMediumDateFormat(this).format(date)+" · "+android.text.format.DateFormat.getTimeFormat(this).format(date);
        add(hero,centered(timestamp,13,GRAY,400),-1,-2);space(hero,10);
        String status=entry.preview?"Preview":protection?(removed?"Protection removed":"Protection added"):"Recorded";
        TextView pill=centered(status,12,entry.preview?BLUE:removed?GRAY:GREEN,600);
        pill.setBackground(round(entry.preview||removed?PALE:0xffe8f6ec,18));pill.setPadding(dp(14),dp(7),dp(14),dp(7));
        add(hero,pill,-2,-2);add(body,hero,-1,-2);space(body,24);
        if(cleanupActivity||scan){
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
            add(body,txt("Recorded change",14,GRAY,500),-1,-2);space(body,9);
            LinearLayout info=card();pad(info,16,15);
            add(info,txt(entry.title,15,INK,600),-1,-2);space(info,9);
            TextView target=txt("",16,INK,500);
            target.setText(entry.title.startsWith("Smart protection")?tr(entry.detail):entry.detail);
            target.setTextIsSelectable(true);add(info,target,-1,-2);space(info,10);
            add(info,txt("Status at the time of activity",12,GRAY,400),-1,-2);space(info,5);
            add(info,txt(status,14,removed?GRAY:GREEN,600),-1,-2);
            add(body,info,-1,-2);space(body,19);
            add(body,centered("This records a setting change, not a file deletion.",12,GRAY,400),-1,-2);
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
        menu.setAlpha(0f);menu.setScaleX(.97f);menu.setScaleY(.97f);menu.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start();
    }

    private void openProtection(){
        filesVisible=false;foldersVisible=false;settingsVisible=true;agentPrefsVisible=false;protectionVisible=true;setContentView(protectionScreen());
    }
    private LinearLayout protectionScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        add(body,centered("Saved for exclusion from future cleanup.",12,GRAY,400),-1,-2);space(body,8);
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
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
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
        LinearLayout enabled=agentToggle("sparkles","AI Autopilot","Find cleanup opportunities automatically",prefs.autopilot(),prefs::setAutopilot);
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
        add(body,centered("Preview mode · No automatic scans or deletion",11,GRAY,400),-1,-2);
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
        menu.setAlpha(0f);menu.setScaleX(.97f);menu.setScaleY(.97f);menu.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start();
    }

    private LinearLayout hero() {
        LinearLayout l=column(); l.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout state=row(); state.setGravity(Gravity.CENTER);
        View dot=new View(this); dot.setBackground(round(prefs.autopilot()?0xff34c759:GRAY,8));
        add(state,dot,dp(7),dp(7)); gap(state,6);
        add(state,txt(prefs.autopilot()?"Autopilot active":"Autopilot paused",15,prefs.autopilot()?GREEN:GRAY,600),-2,-2); add(l,state,-1,-2);
        space(l,5);
        Gauge gauge=new Gauge(this);
        add(l,gauge,-1,v(207));
        space(l,4);
        add(l,centered("All under control.",27,INK,700),-1,-2);
        space(l,5);
        add(l,centered(prefs.autopilot()?"Your AI Agent is taking care of things.":"Your Agent is paused.",14,GRAY,400),-1,-2);
        return l;
    }
    private LinearLayout storage() {
        LinearLayout card=card(); LinearLayout metrics=row();
        weight(metrics,metric("Available storage",gbNumber(previewAvailable()),false));
        View rule=new View(this); rule.setBackgroundColor(surface(0xffe6e6eb));
        add(metrics,rule,dp(1),v(42)); gap(metrics,15);
        weight(metrics,metric("Space recovered",gbNumber(previewRecovered()),true));
        add(card,metrics,-1,-2); space(card,12);
        add(card,new StorageBar(this),-1,dp(10)); space(card,9);
        LinearLayout labels=row(); add(labels,txt(ReviewPlan.gb(previewUsed())+" used",12,GRAY,400),-2,-2);
        stretch(labels); add(labels,txt(HomePreviewData.TOTAL,12,GRAY,400),-2,-2);
        add(card,labels,-1,-2);
        card.setContentDescription("Storage details. "+ReviewPlan.gb(previewAvailable())+" available, "+ReviewPlan.gb(previewRecovered())+" recovered.");
        tap(card,WHITE,22,this::openStorage);return card;
    }
    private static final int[] STORAGE_COLORS={0xff007aff,0xff65a4ff,0xff99c6ff,0xffb8bfc9,0xffd0d4da,0xffe5e5ea};
    private void openStorage(){
        storageVisible=true;setContentView(storageScreen());
    }
    private LinearLayout storageScreen(){
        StoragePreview data=storageData();
        LinearLayout root=column();root.setBackgroundColor(BG);root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Storage",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Home");tap(back,WHITE,22,this::showHome);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,v(16),0,v(10));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        add(body,txt("Available storage",14,GRAY,400),-1,-2);space(body,6);
        LinearLayout big=row();big.setBaselineAligned(true);
        add(big,txt(gbNumber(data.available()),42,INK,700),-2,-2);gap(big,6);
        add(big,txt("GB",21,GRAY,500),-2,-2);add(body,big,-1,-2);space(body,14);
        LinearLayout captions=row();add(captions,txt(ReviewPlan.gb(data.used())+" used",13,GRAY,400),-2,-2);
        stretch(captions);add(captions,txt("128 GB total",13,GRAY,400),-2,-2);add(body,captions,-1,-2);space(body,9);
        add(body,new BreakdownBar(this,data),-1,dp(13));space(body,20);
        LinearLayout recovered=card(),line=row();
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(0xffe8f6ec,13));
        mark.addView(new Glyph(this,"up",GREEN),new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.CENTER));
        add(line,mark,dp(46),dp(46));gap(line,12);
        LinearLayout values=column();add(values,txt("Recovered by Clenova",12,GRAY,400),-1,-2);space(values,5);
        add(values,txt(ReviewPlan.gb(data.recovered),26,INK,700),-1,-2);weight(line,values);
        add(line,new Glyph(this,"sparkles",BLUE),dp(23),dp(23));add(recovered,line,-1,-2);
        add(body,recovered,-1,-2);space(body,20);
        add(body,txt("Storage breakdown",14,GRAY,500),-1,-2);space(body,10);
        LinearLayout breakdown=card();pad(breakdown,14,4);
        for(int i=0;i<StoragePreview.LABELS.length;i++){
            LinearLayout entry=row();entry.setPadding(0,v(11),0,v(11));
            FrameLayout icon=new FrameLayout(this);icon.setBackground(round(0xfff0f4fa,11));
            icon.addView(new Glyph(this,StoragePreview.ICONS[i],BLUE),new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
            add(entry,icon,dp(40),dp(40));gap(entry,12);
            LinearLayout details=column(),labels=row();
            TextView title=txt(StoragePreview.LABELS[i],14,INK,600);weight(labels,title);gap(labels,6);
            add(labels,txt(ReviewPlan.gb(data.amount(i)),14,INK,500),-2,-2);
            add(details,labels,-1,-2);space(details,8);
            add(details,new CategoryBar(this,data.amount(i),STORAGE_COLORS[i]),-1,dp(4));
            weight(entry,details);add(breakdown,entry,-1,-2);
            if(i<StoragePreview.LABELS.length-1)thinRule(breakdown);
        }
        add(body,breakdown,-1,-2);space(body,16);
        LinearLayout privacy=row();privacy.setGravity(Gravity.CENTER);
        add(privacy,new Glyph(this,"shield",GREEN),dp(16),dp(16));gap(privacy,6);
        add(privacy,txt("Your Agent is keeping things in check.",12,GRAY,400),-2,-2);add(body,privacy,-1,-2);
        space(body,8);add(body,centered("Sample storage · No device data accessed",11,GRAY,400),-1,-2);
        return root;
    }
    private final class BreakdownBar extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final StoragePreview data;
        BreakdownBar(Context context,StoragePreview data){
            super(context);this.data=data;
            setContentDescription(ReviewPlan.gb(data.used())+" used, "+ReviewPlan.gb(data.available())+" available");
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float gap=dp(3),usable=Math.max(0,getWidth()-gap*5),x=0;
            for(int i=0;i<6;i++){
                double amount=i<5?data.amount(i):data.available();
                float width=(float)(usable*amount/StoragePreview.CAPACITY);
                paint.setColor(surface(STORAGE_COLORS[i]));
                canvas.drawRoundRect(new RectF(x,0,x+width,getHeight()),dp(4),dp(4),paint);
                x+=width+gap;
            }
        }
    }
    private final class CategoryBar extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final double amount;private final int color;
        CategoryBar(Context context,double amount,int color){
            super(context);this.amount=amount;this.color=color;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float radius=getHeight()/2f;
            paint.setColor(surface(0xfff0f0f3));canvas.drawRoundRect(new RectF(0,0,getWidth(),getHeight()),radius,radius,paint);
            paint.setColor(color);canvas.drawRoundRect(new RectF(0,0,(float)(getWidth()*amount/StoragePreview.CAPACITY),getHeight()),radius,radius,paint);
        }
    }

    private LinearLayout metric(String label,String number,boolean arrow) {
        LinearLayout l=column(); add(l,txt(label,12,GRAY,400),-1,-2); space(l,5);
        LinearLayout value=row(); value.setBaselineAligned(true);
        add(value,txt(number,27,INK,700),-2,-2); gap(value,4);
        if(arrow) add(value,new Glyph(this,"up",GREEN),dp(13),dp(13));
        add(l,value,-1,-2); space(l,1); add(l,txt("GB",14,GRAY,400),-2,-2); return l;
    }
    private LinearLayout agentCard() {
        if(completed)return completedAgentCard();
        if(prefs.recommendedMb()==0)return inactiveAgentCard();
        LinearLayout c=card(); LinearLayout top=row();
        FrameLayout mark=new FrameLayout(this); mark.setBackground(round(PALE,13));
        mark.addView(new Glyph(this,"sparkles",BLUE),new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.CENTER));
        add(top,mark,dp(43),dp(43)); gap(top,10);
        LinearLayout message=column();
        add(message,txt("I've prepared your cleanup.",16,INK,600),-1,-2); space(message,4);
        add(message,txt("Awaiting your approval",12,GRAY,400),-1,-2); weight(top,message);
        add(c,top,-1,-2); space(c,12);
        LinearLayout amount=row(); amount.setBaselineAligned(true);
        add(amount,txt(ReviewPlan.gb(prefs.recommendedMb()),32,INK,700),-2,-2); gap(amount,7);
        weight(amount,txt("ready to recover",13,GRAY,400)); add(c,amount,-1,-2);
        space(c,12);
        LinearLayout approve=row(); approve.setGravity(Gravity.CENTER);
        add(approve,txt("Review & Approve",17,Color.WHITE,600),-2,-2); gap(approve,8);
        add(approve,new Glyph(this,"right",Color.WHITE),dp(17),dp(17));
        tap(approve,BLUE,26,this::openReview);
        add(c,approve,-1,dp(51)); return c;
    }
    private void showHome() {
        scanResult=null;scanVisible=false;scanReviewVisible=false;
        cleanVisible=false;cleanScroll=null;cleanScrollY=0;
        aboutVisible=false;aboutScroll=null;policyVisible=false;termsVisible=false;policyScroll=null;feedbackVisible=false;helpVisible=false;permissionsVisible=false;privacyVisible=false;
        storageVisible=false;
        settingsVisible=false;agentPrefsVisible=false;protectionVisible=false;foldersVisible=false;filesVisible=false;historyVisible=false;
        completionVisible=false;
        reviewing = false;
        setContentView(home());
    }
    private void openReview() {
        if(completed||prefs.recommendedMb()==0)return;
        reviewPlan = new ReviewPlan();
        if(!prefs.discovery(0))reviewPlan.setSelected(0,false);
        reviewing = true;
        double[] offeredMb=new double[3];int[] offeredCounts=new int[3];int offeredTotal=0;
        for(int i=0;i<3;i++)if(prefs.discovery(i)){offeredMb[i]=ReviewPlan.MB[i];offeredCounts[i]=ReviewPlan.COUNTS[i];offeredTotal+=offeredCounts[i];}
        if(!history.add(null,"scan","Sample recommendations viewed","Preview only",prefs.recommendedMb(),offeredTotal,true,offeredMb,offeredCounts))folderToast("Could not save activity");
        setContentView(reviewScreen());
    }
    @Override public void onBackPressed() {
        if(cleaning)return;
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
        if(completionVisible){showHome();return;}
        if(cleanVisible){showHome();return;}
        if (videoViewing) leaveVideos(false); else if (similarViewing) leaveSimilar(false); else if (duplicateViewing) leaveDuplicates(false); else if (reviewing) showHome(); else super.onBackPressed();
    }
    private LinearLayout reviewScreen() {
        LinearLayout root = column(); root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header = new FrameLayout(this);
        TextView title = centered("Review cleanup",17,INK,600);
        header.addView(title,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back = new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Home");
        tap(back,WHITE,22,this::showHome);
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body = column();
        body.setPadding(0,v(12),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout hero = column(); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        pad(hero,0,16);
        add(hero,new Glyph(this,"sparkles",BLUE),dp(48),dp(48));
        space(hero,14);
        add(hero,centered("I've found "+ReviewPlan.gb(prefs.recommendedMb()),33,INK,700),-1,-2);
        space(hero,6);
        add(hero,centered("Your cleanup is ready.",15,GRAY,400),-1,-2);
        add(body,hero,-1,-2); space(body,16);
        add(body,txt("Cleanup plan",14,GRAY,600),-1,-2); space(body,10);
        LinearLayout plan = card(); pad(plan,16,4);
        for(int i=0;i<ReviewPlan.NAMES.length;i++){
            if(!prefs.discovery(i))continue;
            final int index=i;
            LinearLayout category=row();
            category.setPadding(0,v(15),0,v(15));
            FrameLayout mark=new FrameLayout(this); mark.setBackground(round(PALE,12));
            mark.addView(new Glyph(this,ReviewPlan.ICONS[i],BLUE),
                    new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
            add(category,mark,dp(44),dp(44)); gap(category,12);
            LinearLayout info=column();
            add(info,txt(ReviewPlan.NAMES[i],16,INK,600),-1,-2); space(info,4);
            add(info,txt(i==0 && reviewPlan.isSelected(0)?ReviewPlan.selectedCount(reviewPlan.draftCopies())+" extra copies selected":i==1 && reviewPlan.isSelected(1)?ReviewPlan.selectedCount(reviewPlan.draftSimilar())+" photos selected":i==2 && reviewPlan.isSelected(2)?ReviewPlan.selectedCount(reviewPlan.draftVideos())+" videos selected":ReviewPlan.DETAILS[i],13,GRAY,400),-1,-2); space(info,5);
            TextView review=txt("Review items",13,BLUE,600);
            review.setMinHeight(dp(32)); review.setGravity(Gravity.CENTER_VERTICAL);
            review.setContentDescription("Review items: "+ReviewPlan.NAMES[i]);
            tap(review,WHITE,8,()->reviewCategory(index));
            add(info,review,-1,-2); weight(category,info); gap(category,8);
            LinearLayout right=column(); right.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            add(right,txt(ReviewPlan.gb(reviewPlan.isSelected(i)?reviewPlan.categoryMb(i):ReviewPlan.MB[i]),15,INK,600),-2,-2);
            FrameLayout choice=new FrameLayout(this);
            choice.addView(new Glyph(this,reviewPlan.isSelected(i)?"selected":"unselected",
                    reviewPlan.isSelected(i)?BLUE:0xffc7c7cc),
                    new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
            choice.setContentDescription(ReviewPlan.NAMES[i]+(reviewPlan.isSelected(i)?", selected":", not selected"));
            choice.setSelected(reviewPlan.isSelected(i));
            tap(choice,WHITE,22,()->{
                if(index!=0 && !reviewPlan.isSelected(index)) reviewCategory(index);
                else {reviewPlan.setSelected(index,!reviewPlan.isSelected(index));setContentView(reviewScreen());}
            });
            add(right,choice,dp(44),dp(44)); add(category,right,-2,-2);
            add(plan,category,-1,-2);
            boolean more=false;for(int next=i+1;next<ReviewPlan.NAMES.length;next++)if(prefs.discovery(next))more=true;
            if(more){
                View divider=new View(this);divider.setBackgroundColor(surface(0xffeeeef0));add(plan,divider,-1,dp(1));
            }
        }
        add(body,plan,-1,-2); space(body,14);
        LinearLayout privacy=row();privacy.setGravity(Gravity.CENTER);
        add(privacy,new Glyph(this,"shield",GREEN),dp(17),dp(17)); gap(privacy,6);
        add(privacy,txt("Nothing deleted without your approval.",12,GRAY,400),-2,-2);
        add(body,privacy,-1,-2);

        LinearLayout footer=card(); pad(footer,16,14);
        add(footer,txt("You'll free up",14,GRAY,400),-1,-2);space(footer,5);
        add(footer,txt(ReviewPlan.gb(reviewPlan.total()),34,INK,700),-1,-2);space(footer,13);
        LinearLayout approve=row();approve.setGravity(Gravity.CENTER);
        add(approve,txt("Approve "+ReviewPlan.gb(reviewPlan.total())+" Cleanup",16,Color.WHITE,600),-2,-2);
        gap(approve,8);add(approve,new Glyph(this,"right",Color.WHITE),dp(17),dp(17));
        if(reviewPlan.total()>0) tap(approve,BLUE,26,()->confirmPreview());
        else {approve.setBackground(round(0xffc7c7cc,26));approve.setEnabled(false);}
        add(footer,approve,-1,dp(52));space(footer,5);
        TextView later=centered("Not now",14,GRAY,500);
        tap(later,WHITE,20,this::showHome);add(footer,later,-1,dp(44));
        space(root,6);add(root,footer,-1,-2);return root;
    }
    private void reviewCategory(int index) {
        if(index==0)openDuplicates();
        else if(index==1)openSimilar();
        else if(index==2)openVideos();
    }
    private void openVideos(){
        videoDraft=reviewPlan.draftVideos();videoViewing=true;videoTab=0;
        setContentView(videoScreen(0));
    }
    private void leaveVideos(boolean confirm){
        if(confirm)reviewPlan.confirmVideos(videoDraft);
        videoViewing=false;videoDraft=null;setContentView(reviewScreen());
    }
    private LinearLayout videoScreen(int offset){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Large videos",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Review cleanup");
        tap(back,WHITE,22,()->leaveVideos(false));
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));space(root,14);
        int count=ReviewPlan.selectedCount(videoDraft);
        String amount=ReviewPlan.gb(ReviewPlan.videoMb(videoDraft));
        add(root,txt(amount,36,INK,700),-1,-2);space(root,5);
        LinearLayout status=row();add(status,new Glyph(this,"sparkles",BLUE),dp(16),dp(16));gap(status,5);
        add(status,txt("12 videos recommended",14,BLUE,500),-2,-2);add(root,status,-1,-2);space(root,16);
        LinearLayout tabs=row();tabs.setBackground(round(0xffe7e7ec,14));
        tabs.setPadding(dp(3),dp(3),dp(3),dp(3));
        String[] labels={"Agent Picks","View All"};
        for(int i=0;i<2;i++){
            final int tab=i;
            TextView label=centered(labels[i],14,videoTab==i?INK:GRAY,600);
            tap(label,videoTab==i?WHITE:0xffe7e7ec,11,()->{videoTab=tab;setContentView(videoScreen(0));});
            label.setSelected(videoTab==i);tabs.addView(label,new LinearLayout.LayoutParams(0,dp(39),1f));
        }
        add(root,tabs,-1,-2);space(root,12);
        LinearLayout options=row();
        add(options,txt(videoTab==0?"Largest videos":"All 12 videos",14,GRAY,500),-2,-2);stretch(options);
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        TextView all=txt(videoTab==0?"View all":count==12?"Deselect all":"Select all",14,BLUE,600);
        all.setGravity(Gravity.CENTER_VERTICAL);
        tap(all,BG,8,()->{
            if(videoTab==0){videoTab=1;setContentView(videoScreen(0));}
            else{java.util.Arrays.fill(videoDraft,count!=12);setContentView(videoScreen(scroll.getScrollY()));}
        });
        add(options,all,-2,dp(40));add(root,options,-1,-2);
        LinearLayout body=column();body.setPadding(0,0,0,v(10));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        int limit=videoTab==0?4:ReviewPlan.VIDEO_MB.length;
        for(int first=0;first<limit;first+=2){
            LinearLayout gallery=row();gallery.setGravity(Gravity.TOP);
            for(int i=first;i<Math.min(first+2,limit);i++){
                LinearLayout tile=videoTile(i,scroll);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
                if(i>first)lp.leftMargin=dp(10);
                gallery.addView(tile,lp);
            }
            add(body,gallery,-1,-2);if(first+2<limit)space(body,14);
        }
        if(offset>0)scroll.post(()->scroll.scrollTo(0,offset));
        space(root,8);thinRule(root);space(root,12);
        LinearLayout total=row();add(total,txt(count+" videos selected",14,GRAY,500),-2,-2);stretch(total);
        add(total,txt(amount,21,INK,700),-2,-2);add(root,total,-1,-2);space(root,12);
        TextView confirm=centered("Confirm selection",17,Color.WHITE,600);
        tap(confirm,BLUE,26,()->leaveVideos(true));add(root,confirm,-1,dp(52));return root;
    }
    private LinearLayout videoTile(int index,ScrollView scroll){
        LinearLayout item=column();
        FrameLayout tile=new FrameLayout(this);tile.setBackground(round(0xffe4e8ee,10));tile.setClipToOutline(true);
        VideoSample image=new VideoSample(this,index);
        tile.addView(image,new FrameLayout.LayoutParams(-1,-1));
        image.setContentDescription("Preview video "+(index+1)+", "+ReviewPlan.VIDEO_DURATION[index]+", "+ReviewPlan.VIDEO_MB[index]+" MB");
        image.setOnClickListener(w->showVideoPreview(index,scroll));
        TextView play=centered("▶",21,Color.WHITE,600);play.setBackground(round(0x66000000,24));
        FrameLayout.LayoutParams playLp=new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.CENTER);
        tile.addView(play,playLp);play.setContentDescription("Open video preview");
        play.setOnClickListener(w->showVideoPreview(index,scroll));
        TextView duration=centered(ReviewPlan.VIDEO_DURATION[index],11,Color.WHITE,600);
        duration.setBackground(round(0xaa000000,5));duration.setPadding(dp(6),dp(4),dp(6),dp(4));
        FrameLayout.LayoutParams durLp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);
        durLp.setMargins(0,0,dp(6),dp(6));tile.addView(duration,durLp);
        FrameLayout choice=new FrameLayout(this);
        choice.addView(new Glyph(this,videoDraft[index]?"selected":"unselected",videoDraft[index]?BLUE:Color.WHITE),
                new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
        choice.setContentDescription("Video "+(index+1)+(videoDraft[index]?", selected":", not selected"));
        choice.setSelected(videoDraft[index]);
        tap(choice,0x55000000,22,()->{videoDraft[index]=!videoDraft[index];setContentView(videoScreen(scroll.getScrollY()));});
        tile.addView(choice,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.END));
        add(item,tile,-1,v(128));space(item,7);
        LinearLayout detail=row();add(detail,txt(ReviewPlan.gb(ReviewPlan.VIDEO_MB[index]),15,INK,600),-2,-2);
        stretch(detail);add(detail,txt("MP4",11,GRAY,400),-2,-2);add(item,detail,-1,-2);
        return item;
    }
    private void showVideoPreview(int index,ScrollView scroll){
        android.app.Dialog dialog=new android.app.Dialog(this,dark?android.R.style.Theme_Material_NoActionBar_Fullscreen:android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout layout=column();layout.setBackgroundColor(BG);layout.setPadding(dp(18),v(12),dp(18),v(16));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Video preview",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout close=new FrameLayout(this);
        close.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        close.setContentDescription("Close video preview");tap(close,WHITE,22,dialog::dismiss);
        header.addView(close,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(layout,header,-1,dp(44));
        FrameLayout canvas=new FrameLayout(this);canvas.setBackgroundColor(INK);
        VideoSample image=new VideoSample(this,index);
        canvas.addView(image,new FrameLayout.LayoutParams(1,1,Gravity.CENTER));
        canvas.addOnLayoutChangeListener((view,left,top,right,bottom,ol,ot,or,ob)->{
            int width=Math.min(right-left,Math.round((bottom-top)*160f/120f));
            if(width<=0)return;
            int height=Math.round(width*120f/160f);
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)image.getLayoutParams();
            if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;image.setLayoutParams(lp);}
        });
        layout.addView(canvas,new LinearLayout.LayoutParams(-1,0,1f));space(layout,12);
        add(layout,centered("Illustrated sample preview · no actual video file",12,GRAY,400),-1,-2);space(layout,8);
        add(layout,centered(ReviewPlan.gb(ReviewPlan.VIDEO_MB[index])+" · "+ReviewPlan.VIDEO_DURATION[index]+" · MP4",14,INK,500),-1,-2);space(layout,12);
        TextView toggle=centered(videoDraft[index]?"Keep this video":"Select video",17,Color.WHITE,600);
        tap(toggle,BLUE,26,()->{videoDraft[index]=!videoDraft[index];int offset=scroll.getScrollY();dialog.dismiss();setContentView(videoScreen(offset));});
        add(layout,toggle,-1,dp(52));dialog.setContentView(layout);dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setLayout(-1,-1);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setNavigationBarColor(BG);
            dialog.getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }
    private static final class VideoSample extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int index;
        VideoSample(Context c,int index){super(c);this.index=index;}
        private void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);}
        private void shape(Canvas c,int color,float... xy){
            fill(color);Path path=new Path();path.moveTo(xy[0],xy[1]);
            for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();c.drawPath(path,p);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.save();c.scale(getWidth()/160f,getHeight()/120f);
            int mode=index%4;int[] sky={0xffb3d1e4,0xffe8c1a6,0xffb0cbc9,0xffa6b2cb};
            fill(sky[mode]);c.drawRect(0,0,160,120,p);
            if(mode==0){
                shape(c,0xff718993,0,90,42+index,18,112,90,138,40,160,75,160,120,0,120);
                shape(c,0xff557b65,0,103,50,87,112,103,160,92,160,120,0,120);
            }else if(mode==1){
                fill(0xffffe6af);c.drawCircle(118-index,37,20,p);
                shape(c,0xffa98b7a,0,80,35,54,85,80,160,75,160,120,0,120);
                shape(c,0xff726f80,0,94,80,80,160,95,160,120,0,120);
            }else if(mode==2){
                shape(c,0xff6b9384,0,78,48,38,95,72,160,36,160,120,0,120);
                shape(c,0xffdddacb,62,120,80,75,92,75,125,120);
            }else{
                for(int b=0;b<5;b++){fill(b%2==0?0xff65778d:0xff7b899b);c.drawRect(b*34,30+(b%3)*12+index,b*34+29,120,p);}
            }
            c.restore();
        }
    }

    private void openSimilar() {
        similarDraft=reviewPlan.draftSimilar();similarViewing=true;similarTab=0;
        setContentView(similarScreen(0));
    }
    private void leaveSimilar(boolean confirm) {
        if(confirm)reviewPlan.confirmSimilar(similarDraft);
        similarViewing=false;similarDraft=null;setContentView(reviewScreen());
    }
    private LinearLayout similarScreen(int offset) {
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Similar photos",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Review cleanup");
        tap(back,WHITE,22,()->leaveSimilar(false));
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));space(root,14);
        int count=ReviewPlan.selectedCount(similarDraft);
        String amount=ReviewPlan.gb(count*ReviewPlan.SIMILAR_MB);
        add(root,txt(amount,36,INK,700),-1,-2);space(root,5);
        LinearLayout status=row();
        add(status,new Glyph(this,"sparkles",BLUE),dp(16),dp(16));gap(status,5);
        add(status,txt("80 photos recommended",14,BLUE,500),-2,-2);
        add(root,status,-1,-2);space(root,16);
        LinearLayout tabs=row();tabs.setBackground(round(0xffe7e7ec,14));
        tabs.setPadding(dp(3),dp(3),dp(3),dp(3));
        String[] labels={"Agent Picks","View All"};
        for(int i=0;i<2;i++){
            final int tab=i;
            TextView label=centered(labels[i],14,similarTab==i?INK:GRAY,600);
            tap(label,similarTab==i?WHITE:0xffe7e7ec,11,()->{similarTab=tab;setContentView(similarScreen(0));});
            label.setSelected(similarTab==i);tabs.addView(label,new LinearLayout.LayoutParams(0,dp(39),1f));
        }
        add(root,tabs,-1,-2);space(root,12);
        ScrollView scroll=new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,0,0,v(10));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        if(similarTab==0){
            LinearLayout stats=row();stats.setPadding(0,v(6),0,v(6));
            LinearLayout left=column();add(left,txt("80",25,INK,700),-1,-2);space(left,5);
            add(left,txt("Suggested to remove",12,GRAY,400),-1,-2);weight(stats,left);
            View divider=new View(this);divider.setBackgroundColor(surface(0xffdcdce1));add(stats,divider,dp(1),dp(36));gap(stats,15);
            LinearLayout right=column();add(right,txt("32",25,GREEN,700),-1,-2);space(right,5);
            add(right,txt("Best shots to keep",12,GRAY,400),-1,-2);weight(stats,right);
            add(body,stats,-1,-2);space(body,12);thinRule(body);space(body,6);
        }
        LinearLayout options=row();
        add(options,txt(similarTab==0?"Preview":"32 similar groups",14,GRAY,500),-2,-2);stretch(options);
        TextView all=txt(similarTab==0?"View all":count==80?"Deselect all":"Select all",14,BLUE,600);
        all.setGravity(Gravity.CENTER_VERTICAL);
        tap(all,BG,8,()->{
            if(similarTab==0){similarTab=1;setContentView(similarScreen(0));}
            else{java.util.Arrays.fill(similarDraft,count!=80);setContentView(similarScreen(scroll.getScrollY()));}
        });
        add(options,all,-2,dp(40));add(body,options,-1,-2);
        int limit=similarTab==0?1:ReviewPlan.SIMILAR_GROUPS;
        for(int group=0;group<limit;group++){
            addSimilarGroup(body,group,scroll);
            if(group<limit-1){space(body,14);thinRule(body);space(body,14);}
        }
        if(offset>0)scroll.post(()->scroll.scrollTo(0,offset));
        space(root,8);thinRule(root);space(root,12);
        LinearLayout total=row();add(total,txt(count+" photos selected",14,GRAY,500),-2,-2);stretch(total);
        add(total,txt(amount,21,INK,700),-2,-2);add(root,total,-1,-2);space(root,12);
        TextView confirm=centered("Confirm selection",17,Color.WHITE,600);
        tap(confirm,BLUE,26,()->leaveSimilar(true));add(root,confirm,-1,dp(52));return root;
    }
    private void addSimilarGroup(LinearLayout body,int group,ScrollView scroll){
        int size=group<16?4:3, columns=size==4?2:3;
        add(body,txt(size+" similar photos",14,INK,600),-1,-2);space(body,9);
        for(int first=0;first<size;first+=columns){
            LinearLayout gallery=row();gallery.setGravity(Gravity.TOP);
            for(int p=first;p<Math.min(first+columns,size);p++){
                final int photo=p;
                FrameLayout tile=new FrameLayout(this);tile.setBackground(round(0xffe4e8ee,10));tile.setClipToOutline(true);
                SamplePhoto image=new SamplePhoto(this,group,photo);
                tile.addView(image,new FrameLayout.LayoutParams(-1,-1));
                image.setContentDescription("Preview similar group "+(group+1)+", photo "+(photo+1));
                image.setOnClickListener(w->showPhotoPreview(group,photo,scroll,true));
                if(photo==0){
                    TextView keep=centered("Best shot",11,GREEN,600);keep.setBackground(round(WHITE,6));
                    keep.setPadding(dp(7),dp(4),dp(7),dp(4));
                    FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);
                    lp.setMargins(dp(7),dp(7),0,0);tile.addView(keep,lp);
                }else{
                    int index=ReviewPlan.similarIndex(group,photo);
                    FrameLayout choice=new FrameLayout(this);
                    choice.addView(new Glyph(this,similarDraft[index]?"selected":"unselected",similarDraft[index]?BLUE:Color.WHITE),
                            new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
                    choice.setContentDescription("Group "+(group+1)+" photo "+(photo+1)+(similarDraft[index]?", selected":", not selected"));
                    choice.setSelected(similarDraft[index]);
                    tap(choice,0x55000000,22,()->{similarDraft[index]=!similarDraft[index];setContentView(similarScreen(scroll.getScrollY()));});
                    tile.addView(choice,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.END));
                }
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(columns==2?132:110),1f);
                if(p>first)lp.leftMargin=dp(7);gallery.addView(tile,lp);
            }
            add(body,gallery,-1,-2);if(first+columns<size)space(body,7);
        }
    }

    private void openDuplicates() {
        duplicateDraft = reviewPlan.draftCopies();
        duplicateViewing = true;
        duplicateTab = 0;
        setContentView(duplicateScreen(0));
    }
    private void leaveDuplicates(boolean confirm) {
        if(confirm) reviewPlan.confirmCopies(duplicateDraft);
        duplicateViewing=false; duplicateDraft=null;
        setContentView(reviewScreen());
    }
    private LinearLayout duplicateScreen(int offset) {
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Duplicate photos",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Review cleanup");
        tap(back,WHITE,22,()->leaveDuplicates(false));
        header.addView(back,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(root,header,-1,dp(44));space(root,16);
        int selectedCount=ReviewPlan.selectedCount(duplicateDraft);
        String selectedSize=ReviewPlan.gb(selectedCount*ReviewPlan.COPY_MB);
        add(root,txt(selectedSize,36,INK,700),-1,-2);space(root,6);
        LinearLayout status=row();
        add(status,new Glyph(this,"sparkles",BLUE),dp(16),dp(16));gap(status,6);
        add(status,txt(selectedCount==48?"48 copies selected by Agent":selectedCount+" copies selected",14,BLUE,500),-2,-2);
        add(root,status,-1,-2);space(root,18);
        LinearLayout tabs=row();tabs.setBackground(round(0xffe7e7ec,14));
        tabs.setPadding(dp(3),dp(3),dp(3),dp(3));
        String[] labels={"Agent Picks","View All"};
        for(int i=0;i<2;i++){
            final int tab=i;
            TextView label=centered(labels[i],14,duplicateTab==i?INK:GRAY,600);
            tap(label,duplicateTab==i?WHITE:0xffe7e7ec,11,()->{duplicateTab=tab;setContentView(duplicateScreen(0));});
            label.setSelected(duplicateTab==i);
            tabs.addView(label,new LinearLayout.LayoutParams(0,dp(39),1f));
        }
        add(root,tabs,-1,-2);space(root,14);
        ScrollView scroll=new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setPadding(0,0,0,v(10));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        if(duplicateTab==0){
            LinearLayout summary=card();
            add(summary,duplicateSummary("images",BLUE,PALE,"Extra copies","Selected for removal",Integer.toString(selectedCount)), -1,-2);
            space(summary,12);thinRule(summary);space(summary,12);
            add(summary,duplicateSummary("shield",GREEN,0xffeaf7ee,"Photos to keep","Protected by Agent","24"),-1,-2);
            add(body,summary,-1,-2);space(body,20);
            LinearLayout preview=row();
            add(preview,txt("Preview",14,GRAY,500),-2,-2);stretch(preview);
            TextView all=txt("View all",14,BLUE,600);all.setGravity(Gravity.CENTER_VERTICAL);
            tap(all,BG,8,()->{duplicateTab=1;setContentView(duplicateScreen(0));});
            add(preview,all,-2,dp(40));add(body,preview,-1,-2);
            addDuplicateGroup(body,0,scroll);
        }else{
            LinearLayout options=row();
            add(options,txt("24 duplicate groups",13,GRAY,500),-2,-2);stretch(options);
            TextView all=txt(selectedCount==48?"Deselect all":"Select all",14,BLUE,600);
            all.setGravity(Gravity.CENTER_VERTICAL);
            tap(all,BG,8,()->{java.util.Arrays.fill(duplicateDraft,selectedCount!=48);setContentView(duplicateScreen(scroll.getScrollY()));});
            add(options,all,-2,dp(40));add(body,options,-1,-2);
            for(int group=0;group<ReviewPlan.GROUPS;group++){
                addDuplicateGroup(body,group,scroll);
                if(group<ReviewPlan.GROUPS-1){space(body,16);thinRule(body);space(body,16);}
            }
        }
        if(offset>0)scroll.post(()->scroll.scrollTo(0,offset));
        space(root,8);thinRule(root);space(root,12);
        LinearLayout total=row();
        add(total,txt(selectedCount+" photos selected",14,GRAY,500),-2,-2);stretch(total);
        add(total,txt(selectedSize,21,INK,700),-2,-2);
        add(root,total,-1,-2);space(root,12);
        TextView confirm=centered("Confirm selection",17,Color.WHITE,600);
        tap(confirm,BLUE,26,()->leaveDuplicates(true));
        add(root,confirm,-1,dp(52));
        return root;
    }
    private void thinRule(LinearLayout parent) {
        View rule=new View(this);rule.setBackgroundColor(surface(0xffe5e5ea));add(parent,rule,-1,dp(1));
    }
    private LinearLayout duplicateSummary(String icon,int tint,int background,String title,String subtitle,String count){
        LinearLayout l=row();
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(background,12));
        mark.addView(new Glyph(this,icon,tint),new FrameLayout.LayoutParams(dp(25),dp(25),Gravity.CENTER));
        add(l,mark,dp(44),dp(44));gap(l,12);
        LinearLayout text=column();
        add(text,txt(title,16,INK,600),-1,-2);space(text,4);
        add(text,txt(subtitle,13,GRAY,400),-1,-2);weight(l,text);gap(l,8);
        add(l,txt(count,24,tint==GREEN?GREEN:INK,700),-2,-2);return l;
    }
    private void addDuplicateGroup(LinearLayout body,int group,ScrollView scroll) {
        LinearLayout heading=row();
        add(heading,txt("3 identical photos",14,INK,600),-2,-2);stretch(heading);
        add(heading,txt("50 MB",12,GRAY,400),-2,-2);add(body,heading,-1,-2);space(body,9);
        LinearLayout gallery=row();gallery.setGravity(Gravity.TOP);
        for(int photo=0;photo<3;photo++){
            final int p=photo;
            FrameLayout tile=new FrameLayout(this);
            tile.setBackground(round(0xffe4e8ee,10));tile.setClipToOutline(true);
            SamplePhoto image=new SamplePhoto(this,group);
            tile.addView(image,new FrameLayout.LayoutParams(-1,-1));
            image.setContentDescription("Preview group "+(group+1)+", photo "+(photo+1));
            image.setOnClickListener(w->showSamplePhoto(group,p,scroll));
            if(photo==0){
                TextView keep=centered("Keep",11,GREEN,600);
                keep.setBackground(round(WHITE,6));keep.setPadding(dp(7),dp(4),dp(7),dp(4));
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);
                lp.setMargins(dp(7),dp(7),0,0);tile.addView(keep,lp);
            }else{
                int index=group*2+photo-1;
                FrameLayout choice=new FrameLayout(this);
                choice.addView(new Glyph(this,duplicateDraft[index]?"selected":"unselected",duplicateDraft[index]?BLUE:Color.WHITE),
                        new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
                choice.setContentDescription("Group "+(group+1)+" copy "+photo+(duplicateDraft[index]?", selected":", not selected"));
                choice.setSelected(duplicateDraft[index]);
                tap(choice,0x55000000,22,()->{duplicateDraft[index]=!duplicateDraft[index];setContentView(duplicateScreen(scroll.getScrollY()));});
                FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.END);
                tile.addView(choice,cp);
            }
            LinearLayout.LayoutParams tileLp=new LinearLayout.LayoutParams(0,dp(122),1f);
            if(photo>0)tileLp.leftMargin=dp(7);
            gallery.addView(tile,tileLp);
        }
        add(body,gallery,-1,-2);
    }
    private void showSamplePhoto(int group,int photo,ScrollView scroll) {
        showPhotoPreview(group,photo,scroll,false);
    }
    private void showPhotoPreview(int group,int photo,ScrollView scroll,boolean similar) {
        android.app.Dialog dialog=new android.app.Dialog(this,dark?android.R.style.Theme_Material_NoActionBar_Fullscreen:android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout layout=column();layout.setBackgroundColor(BG);
        layout.setPadding(dp(18),v(12),dp(18),v(16));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered(photo==0?(similar?"Best shot":"Protected original"):(similar?"Similar photo":"Duplicate copy"),17,INK,600),
                new FrameLayout.LayoutParams(-1,-1));
        FrameLayout close=new FrameLayout(this);
        close.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        close.setContentDescription("Close photo preview");
        tap(close,WHITE,22,dialog::dismiss);
        header.addView(close,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(layout,header,-1,dp(44));
        FrameLayout preview=new FrameLayout(this);
        SamplePhoto image=new SamplePhoto(this,group,similar?photo:0);
        image.setContentDescription("Sample photo, group "+(group+1));
        preview.addView(image,new FrameLayout.LayoutParams(1,1,Gravity.CENTER));
        preview.addOnLayoutChangeListener((view,left,top,right,bottom,ol,ot,or,ob)->{
            int width=Math.min(right-left,Math.round((bottom-top)*110f/125f));
            if(width<=0)return;
            int height=Math.round(width*125f/110f);
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)image.getLayoutParams();
            if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;image.setLayoutParams(lp);}
        });
        layout.addView(preview,new LinearLayout.LayoutParams(-1,0,1f));
        if(photo==0){
            add(layout,centered(similar?"Best shot · Recommended to keep":"Keep · Protected by Agent",15,GREEN,600),-1,dp(52));
        }else{
            int index=similar?ReviewPlan.similarIndex(group,photo):group*2+photo-1;
            boolean[] draft=similar?similarDraft:duplicateDraft;
            TextView toggle=centered(draft[index]?"Keep this copy":"Select copy",17,Color.WHITE,600);
            tap(toggle,BLUE,26,()->{
                draft[index]=!draft[index];
                int offset=scroll.getScrollY();dialog.dismiss();setContentView(similar?similarScreen(offset):duplicateScreen(offset));
            });
            add(layout,toggle,-1,dp(52));
        }
        dialog.setContentView(layout);dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setLayout(-1,-1);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setNavigationBarColor(BG);
            dialog.getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    /** Procedural sample thumbnails: each group repeats the exact same illustration. */
    private static final class SamplePhoto extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int group,variant;
        SamplePhoto(Context context,int group){this(context,group,0);}
        SamplePhoto(Context context,int group,int variant){super(context);this.group=group;this.variant=variant;}
        private void color(int c){paint.setColor(c);paint.setStyle(Paint.Style.FILL);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.save();c.scale(getWidth()/110f,getHeight()/125f);
            int[] skies={0xffbcd9e7,0xffe7d4bf,0xffc3d6ca,0xffd5c9e3,0xffead2ce,0xffc9ddea};
            int[] hills={0xff819ca7,0xffa99079,0xff789c89,0xff9292ae,0xffa67e78,0xff758fa8};
            color(skies[group%6]);c.drawRect(0,0,110,125,paint);
            color(0xffffefca);c.drawCircle(72+(group%4)*5+variant*3,21+(group%3)*4,12,paint);
            color(hills[group%6]);
            Path mountain=new Path();mountain.moveTo(-10,95);mountain.lineTo(26+group%12+variant*4,30+group%9+variant*3);
            mountain.lineTo(74,85);mountain.lineTo(96,48);mountain.lineTo(120,90);mountain.lineTo(120,125);
            mountain.lineTo(-10,125);mountain.close();c.drawPath(mountain,paint);
            color(0xff60877b);Path foreground=new Path();foreground.moveTo(0,102);
            foreground.quadTo(30,87,60,101);foreground.quadTo(88,111,110,92);
            foreground.lineTo(110,125);foreground.lineTo(0,125);foreground.close();c.drawPath(foreground,paint);
            c.restore();
        }
    }

    private void confirmPreview() {
        if(reviewPlan.total()<=0 || cleaning || completed)return;
        new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert).setTitle(tr("Approve cleanup?"))
                .setMessage("Preview: "+reviewPlan.count()+" sample items · "+ReviewPlan.gb(reviewPlan.total())+
                        ". No real files will be deleted.")
                .setNegativeButton(tr("Cancel"),null)
                .setPositiveButton(tr("Approve preview"),(d,w)->{
                    cleanup=new CleanupPreview(reviewPlan);cleanupEventId=java.util.UUID.randomUUID().toString();
                    cleanupCategoryCounts=new int[]{ReviewPlan.selectedCount(reviewPlan.draftCopies()),
                        reviewPlan.isSelected(1)?ReviewPlan.selectedCount(reviewPlan.draftSimilar()):0,
                        reviewPlan.isSelected(2)?ReviewPlan.selectedCount(reviewPlan.draftVideos()):0};
                    recordCleanupHistory(false);
                    startCleanupAnimation();
                }).show();
    }
    private LinearLayout completedAgentCard(){
        LinearLayout c=card(),top=row();
        FrameLayout mark=new FrameLayout(this);mark.setBackground(round(0xffe5f5e9,13));
        mark.addView(new Glyph(this,"shield",GREEN),new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.CENTER));
        add(top,mark,dp(43),dp(43));gap(top,10);
        LinearLayout message=column();
        add(message,txt("Cleanup complete.",16,INK,600),-1,-2);space(message,4);
        add(message,txt("Simulated cleanup · Just now",12,GRAY,400),-1,-2);weight(top,message);
        add(c,top,-1,-2);space(c,12);
        LinearLayout amount=row();add(amount,txt(ReviewPlan.gb(cleanup.total),32,INK,700),-2,-2);gap(amount,7);
        weight(amount,txt("recovered",13,GRAY,400));add(c,amount,-1,-2);space(c,12);
        TextView receipt=centered("View summary",16,BLUE,600);
        tap(receipt,PALE,26,()->showCompletion(false));add(c,receipt,-1,dp(51));return c;
    }
    private LinearLayout cleanupRoot(String heading){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        add(root,centered(heading,17,INK,600),-1,dp(44));
        return root;
    }
    private LinearLayout cleanupBody(LinearLayout root){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body=column();body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(0,v(14),0,v(12));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        return body;
    }
    private void startCleanupAnimation(){
        cleaning=true;completionVisible=false;reviewing=false;
        LinearLayout root=cleanupRoot("Cleaning up");
        LinearLayout body=cleanupBody(root);
        space(body,20);
        CleanupRing ring=new CleanupRing(this,false);
        add(body,ring,-1,v(238));space(body,12);
        add(body,centered("Making more room.",26,INK,700),-1,-2);space(body,7);
        add(body,centered("Your Agent is handling the cleanup.",14,GRAY,400),-1,-2);space(body,25);
        LinearLayout stages=card();
        TextView[] markers=new TextView[3], labels=new TextView[3];
        boolean first=true;
        for(int i=0;i<3;i++){
            if(cleanup.amount(i)<=0)continue;
            if(!first){space(stages,14);thinRule(stages);space(stages,14);}first=false;
            LinearLayout line=row();
            markers[i]=centered("○",23,GRAY,600);add(line,markers[i],dp(25),dp(30));gap(line,10);
            weight(line,txt(ReviewPlan.NAMES[i],14,INK,600));gap(line,6);
            labels[i]=txt("Waiting",12,GRAY,400);add(line,labels[i],-2,-2);
            add(stages,line,-1,-2);
        }
        add(body,stages,-1,-2);
        add(root,centered("Only your approved items are processed.",12,GRAY,400),-1,-2);space(root,6);
        add(root,centered("Simulation · No files are deleted.",12,GRAY,400),-1,dp(22));
        setContentView(root);
        cleanupAnimation=ValueAnimator.ofFloat(0f,1f);
        cleanupAnimation.setDuration(2800);
        cleanupAnimation.setInterpolator(new android.view.animation.LinearInterpolator());
        cleanupAnimation.addUpdateListener(a->{
            float f=(float)a.getAnimatedValue();double recovered=cleanup.recovered(f);
            ring.setFraction(f);
            for(int i=0;i<3;i++){
                if(markers[i]==null)continue;
                int status=cleanup.status(i,recovered);
                markers[i].setText(status==2?"✓":status==1?"◌":"○");
                markers[i].setTextColor(status==2?GREEN:status==1?BLUE:GRAY);
                labels[i].setText(tr(status==2?"Completed":status==1?"Cleaning…":"Waiting"));
                labels[i].setTextColor(status==1?BLUE:GRAY);
            }
        });
        cleanupAnimation.addListener(new android.animation.AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(android.animation.Animator animation){
                if(isFinishing()||isDestroyed())return;
                cleaning=false;completed=true;
                if(cleanupEventId!=null)recordCleanupHistory(true);
                showCompletion(true);
            }
        });
        cleanupAnimation.start();
    }
    private void showCompletion(boolean animate){
        completionVisible=true;cleaning=false;reviewing=false;
        LinearLayout root=cleanupRoot("Cleanup complete"),body=cleanupBody(root);
        space(body,12);
        CleanupRing ring=new CleanupRing(this,true);add(body,ring,-1,v(210));space(body,8);
        add(body,centered(ReviewPlan.gb(cleanup.total),38,INK,700),-1,-2);space(body,5);
        add(body,centered("Successfully recovered",15,GREEN,600),-1,-2);space(body,13);
        add(body,centered("All clean. All yours.",25,INK,700),-1,-2);space(body,6);
        add(body,centered("Your Agent took care of it.",14,GRAY,400),-1,-2);space(body,20);
        LinearLayout metrics=card(),line=row();
        LinearLayout left=column();add(left,txt("Items cleaned",12,GRAY,400),-1,-2);space(left,6);
        add(left,txt(Integer.toString(cleanup.count),25,INK,700),-1,-2);weight(line,left);
        View divider=new View(this);divider.setBackgroundColor(surface(0xffe5e5ea));add(line,divider,dp(1),dp(44));gap(line,14);
        LinearLayout right=column();add(right,txt("Available storage",12,GRAY,400),-1,-2);space(right,6);
        add(right,txt(ReviewPlan.gb(cleanup.available()),22,INK,700),-1,-2);weight(line,right);
        add(metrics,line,-1,-2);add(body,metrics,-1,-2);space(body,15);
        LinearLayout shield=row();shield.setGravity(Gravity.CENTER);
        add(shield,new Glyph(this,"shield",GREEN),dp(17),dp(17));gap(shield,6);
        add(shield,txt("Your protected photos are safe.",12,GRAY,400),-2,-2);add(body,shield,-1,-2);
        add(root,centered("Simulation · No files were deleted.",12,GRAY,400),-1,dp(26));space(root,6);
        TextView home=centered("Back to Home",17,Color.WHITE,600);
        tap(home,BLUE,26,this::showHome);add(root,home,-1,dp(52));setContentView(root);
        if(animate){
            ring.setFraction(0f);
            ring.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            cleanupAnimation=ValueAnimator.ofFloat(0f,1f);cleanupAnimation.setDuration(480);
            cleanupAnimation.setInterpolator(new DecelerateInterpolator());
            cleanupAnimation.addUpdateListener(a->ring.setFraction((float)a.getAnimatedValue()));
            cleanupAnimation.start();
        }
    }
    private final class CleanupRing extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean success;
        private float fraction;
        CleanupRing(Context c,boolean success){super(c);this.success=success;fraction=success?1f:0f;}
        void setFraction(float f){fraction=f;invalidate();}
        private void label(Canvas c,String label,float y,int size,int color,boolean bold){
            paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
            paint.setTextSize(size);c.drawText(tr(label),130,y,paint);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float scale=Math.min(getWidth(),getHeight())/260f;
            c.save();c.translate((getWidth()-260*scale)/2,(getHeight()-260*scale)/2);c.scale(scale,scale);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3.5f);paint.setStrokeCap(Paint.Cap.ROUND);
            for(int i=0;i<80;i++){
                double a=Math.toRadians(i*4.5-90);float x=(float)Math.cos(a),y=(float)Math.sin(a);
                paint.setColor(success?0xff34c759:i<Math.round(80*fraction)?BLUE:surface(0xffdfe2e8));
                c.drawLine(130+x*104,130+y*104,130+x*117,130+y*117,paint);
            }
            if(success){
                paint.setStyle(Paint.Style.FILL);paint.setColor(surface(0xffe5f5e9));c.drawCircle(130,130,70,paint);
                c.save();c.translate(130,130);float tick=.75f+.25f*fraction;c.scale(tick,tick);
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(9);paint.setColor(GREEN);
                paint.setAlpha(Math.round(fraction*255));paint.setStrokeJoin(Paint.Join.ROUND);
                Path path=new Path();path.moveTo(-33,0);path.lineTo(-10,23);path.lineTo(36,-25);c.drawPath(path,paint);
                paint.setAlpha(255);c.restore();
            }else{
                label(c,"Cleaning",100,14,GRAY,false);
                label(c,Math.min(100,(int)(fraction*100))+"%",151,53,INK,true);
                label(c,ReviewPlan.gb(cleanup.recovered(fraction))+" recovered",180,13,BLUE,false);
            }
            c.restore();
        }
    }

    private LinearLayout navigation() {
        LinearLayout outer=row(); LinearLayout pill=row(); pill.setBackground(round(WHITE,34));
        pad(pill,4,4); outer.addView(pill,new LinearLayout.LayoutParams(0,dp(64),1f));
        String[] labels={"Home","Clean","AI Agent"}; String[] icons={"home","sparkles","bot"};
        for(int i=0;i<labels.length;i++){
            final String label=labels[i]; LinearLayout item=navItem(label,icons[i],cleanVisible?i==1:i==0);
            pill.addView(item,new LinearLayout.LayoutParams(0,-1,1f));
        }
        gap(outer,8); LinearLayout tools=navItem("Tools","tools",false);
        add(outer,tools,dp(64),dp(64)); return outer;
    }
    private LinearLayout navItem(String label,String icon,boolean active) {
        LinearLayout l=column(); l.setGravity(Gravity.CENTER);
        int tint=active?BLUE:INK;
        add(l,new Glyph(this,icon,tint),dp(24),dp(24)); space(l,4);
        add(l,centered(label,11,tint,600),-1,-2);
        tap(l,active?PALE:WHITE,32,()->{
            if(active)return;
            if(label.equals("Home"))showHome();
            else if(label.equals("Clean"))showClean();
            else demo(label,label+" is reserved for the next design phase.");
        });
        l.setContentDescription(label+(active?", selected":"")); return l;
    }

    private final class Gauge extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress=0f; private ValueAnimator animation;
        Gauge(Context c){ super(c); setContentDescription("Storage health 92, Excellent. Last checked 8 minutes ago."); }
        @Override protected void onAttachedToWindow(){
            super.onAttachedToWindow();
            animation=ValueAnimator.ofFloat(0f,HomePreviewData.HEALTH/100f);
            animation.setDuration(850); animation.setInterpolator(new DecelerateInterpolator());
            animation.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();}); animation.start();
        }
        @Override protected void onDetachedFromWindow(){ if(animation!=null)animation.cancel(); super.onDetachedFromWindow(); }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float scale=Math.min(getWidth()/280f,getHeight()/218f);
            c.save(); c.translate((getWidth()-280*scale)/2,(getHeight()-218*scale)/2); c.scale(scale,scale);
            p.setStrokeWidth(3.6f); p.setStrokeCap(Paint.Cap.ROUND);
            for(int i=0;i<81;i++){
                double a=Math.toRadians(130+i*3.5);
                p.setColor(i<Math.round(progress*81)?BLUE:surface(0xffdfe2e8));
                c.drawLine(140+89*(float)Math.cos(a),111+89*(float)Math.sin(a),
                    140+102*(float)Math.cos(a),111+102*(float)Math.sin(a),p);
            }
            label(c,"Storage health",140,77,12,GRAY,false);
            label(c,Integer.toString(HomePreviewData.HEALTH),140,138,70,INK,true);
            label(c,HomePreviewData.HEALTH_LABEL,140,159,15,GREEN,true);
            label(c,HomePreviewData.LAST_CHECK,140,211,10,GRAY,false);
            c.restore();
        }
        private void label(Canvas c,String text,float x,float y,float size,int color,boolean strong){
            p.setColor(color); p.setStyle(Paint.Style.FILL); p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create(strong?"sans-serif-medium":"sans-serif",strong?Typeface.BOLD:Typeface.NORMAL));
            p.setTextSize(size); c.drawText(tr(text),x,y,p);
        }
    }
    private final class StorageBar extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        StorageBar(Context c){super(c);setContentDescription(ReviewPlan.gb(previewUsed())+" used of 128 GB, "+ReviewPlan.gb(previewAvailable())+" available");}
        @Override protected void onDraw(Canvas c){
            float[] parts={.45f,(float)(previewUsed()/128000.0-.45),(float)(previewAvailable()/128000.0)}; int[] colors={BLUE,0xff91bdff,surface(0xffe5e5ea)};
            float available=getWidth()-dp(4), x=0;
            for(int i=0;i<3;i++){
                float w=available*parts[i]; p.setColor(colors[i]);
                c.drawRoundRect(new RectF(x,0,x+w,getHeight()),getHeight()/2f,getHeight()/2f,p);
                x+=w+dp(2);
            }
        }
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
                case "video": c.drawRoundRect(new RectF(3,6,16,18),3,3,p);path(c,16,10,21,7,21,17,16,14);break;
                case "selected": p.setStyle(Paint.Style.FILL);c.drawCircle(12,12,11,p);p.setStyle(Paint.Style.STROKE);p.setColor(Color.WHITE);path(c,7,12,10.5f,15.5f,17,9);break;
                case "unselected": c.drawCircle(12,12,10,p);break;
                case "right":path(c,4,12,20,12);path(c,14,6,20,12,14,18);break;
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

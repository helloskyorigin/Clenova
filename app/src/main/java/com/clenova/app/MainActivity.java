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
    private boolean storageVisible, settingsVisible;
    private ValueAnimator cleanupAnimation;
    private AppPreferences prefs;
    private android.widget.PopupWindow preferencePopup;
    private boolean dark;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=new AppPreferences(this);
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
        if(state!=null && state.getBoolean("settings_visible")){openSettings();return;}
        if(state!=null && state.getBoolean("storage_visible")){openStorage();return;}
        setContentView(home());
    }
    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putBoolean("storage_visible",storageVisible);
        state.putBoolean("settings_visible",settingsVisible);
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
        if(preferencePopup!=null)preferencePopup.dismiss();
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
        settingsVisible=true;setContentView(settingsScreen());
    }
    private LinearLayout settingsScreen(){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(18),v(12),dp(18),v(12));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered("Settings",17,INK,600),new FrameLayout.LayoutParams(-1,-1));
        FrameLayout back=new FrameLayout(this);
        back.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        back.setContentDescription("Back to Home");tap(back,WHITE,22,this::showHome);
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
        add(body,centered("Version 1.0.0",12,GRAY,400),-1,-2);space(body,5);
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

    private LinearLayout hero() {
        LinearLayout l=column(); l.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout state=row(); state.setGravity(Gravity.CENTER);
        View dot=new View(this); dot.setBackground(round(0xff34c759,8));
        add(state,dot,dp(7),dp(7)); gap(state,6);
        add(state,txt("Autopilot active",15,GREEN,600),-2,-2); add(l,state,-1,-2);
        space(l,5);
        Gauge gauge=new Gauge(this);
        add(l,gauge,-1,v(207));
        space(l,4);
        add(l,centered("All under control.",27,INK,700),-1,-2);
        space(l,5);
        add(l,centered("Your AI Agent is taking care of things.",14,GRAY,400),-1,-2);
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
        LinearLayout c=card(); LinearLayout top=row();
        FrameLayout mark=new FrameLayout(this); mark.setBackground(round(PALE,13));
        mark.addView(new Glyph(this,"sparkles",BLUE),new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.CENTER));
        add(top,mark,dp(43),dp(43)); gap(top,10);
        LinearLayout message=column();
        add(message,txt("I've prepared your cleanup.",16,INK,600),-1,-2); space(message,4);
        add(message,txt("Awaiting your approval",12,GRAY,400),-1,-2); weight(top,message);
        add(c,top,-1,-2); space(c,12);
        LinearLayout amount=row(); amount.setBaselineAligned(true);
        add(amount,txt(HomePreviewData.RECOVERABLE,32,INK,700),-2,-2); gap(amount,7);
        weight(amount,txt("ready to recover",13,GRAY,400)); add(c,amount,-1,-2);
        space(c,12);
        LinearLayout approve=row(); approve.setGravity(Gravity.CENTER);
        add(approve,txt("Review & Approve",17,Color.WHITE,600),-2,-2); gap(approve,8);
        add(approve,new Glyph(this,"right",Color.WHITE),dp(17),dp(17));
        tap(approve,BLUE,26,this::openReview);
        add(c,approve,-1,dp(51)); return c;
    }
    private void showHome() {
        storageVisible=false;
        settingsVisible=false;
        completionVisible=false;
        reviewing = false;
        setContentView(home());
    }
    private void openReview() {
        if(completed)return;
        reviewPlan = new ReviewPlan();
        reviewing = true;
        setContentView(reviewScreen());
    }
    @Override public void onBackPressed() {
        if(cleaning)return;
        if(settingsVisible || storageVisible){showHome();return;}
        if(completionVisible){showHome();return;}
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
        add(hero,centered("I've found 4.8 GB",33,INK,700),-1,-2);
        space(hero,6);
        add(hero,centered("Your cleanup is ready.",15,GRAY,400),-1,-2);
        add(body,hero,-1,-2); space(body,16);
        add(body,txt("Cleanup plan",14,GRAY,600),-1,-2); space(body,10);
        LinearLayout plan = card(); pad(plan,16,4);
        for(int i=0;i<ReviewPlan.NAMES.length;i++){
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
            if(i<ReviewPlan.NAMES.length-1){
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
                    cleanup=new CleanupPreview(reviewPlan);startCleanupAnimation();
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
                cleaning=false;completed=true;showCompletion(true);
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
            final String label=labels[i]; LinearLayout item=navItem(label,icons[i],i==0);
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
            if(!active) demo(label,label+" is reserved for the next design phase.");
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

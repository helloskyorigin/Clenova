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
    private static final int BG = Color.rgb(245,245,247);
    private static final int WHITE = Color.WHITE;
    private static final int INK = Color.rgb(17,17,17);
    private static final int GRAY = Color.rgb(134,134,139);
    private static final int BLUE = Color.rgb(0,122,255);
    private static final int PALE = Color.rgb(230,240,255);
    private static final int GREEN = Color.rgb(39,152,77);
    private float density;
    private float compact = 1f;
    private boolean reviewing;
    private ReviewPlan reviewPlan;
    private boolean duplicateViewing;
    private int duplicateTab;
    private boolean[] duplicateDraft;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        density = getResources().getDisplayMetrics().density;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        int height = getResources().getConfiguration().screenHeightDp;
        compact = height < 740 ? .88f : 1f;
        setContentView(home());
    }

    private int dp(float n) { return Math.round(n * density); }
    private int v(float n) { return dp(n * compact); }
    private GradientDrawable round(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(radius)); return shape;
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
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
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
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK",null).show();
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
        tap(settings,WHITE,24,()->demo("Settings","Settings will be implemented in a later screen."));
        add(l,settings,dp(44),dp(44)); return l;
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
        weight(metrics,metric("Available storage",HomePreviewData.AVAILABLE,false));
        View rule=new View(this); rule.setBackgroundColor(0xffe6e6eb);
        add(metrics,rule,dp(1),v(42)); gap(metrics,15);
        weight(metrics,metric("Space recovered",HomePreviewData.RECOVERED,true));
        add(card,metrics,-1,-2); space(card,12);
        add(card,new StorageBar(this),-1,dp(10)); space(card,9);
        LinearLayout labels=row(); add(labels,txt(HomePreviewData.USED,12,GRAY,400),-2,-2);
        stretch(labels); add(labels,txt(HomePreviewData.TOTAL,12,GRAY,400),-2,-2);
        add(card,labels,-1,-2); return card;
    }
    private LinearLayout metric(String label,String number,boolean arrow) {
        LinearLayout l=column(); add(l,txt(label,12,GRAY,400),-1,-2); space(l,5);
        LinearLayout value=row(); value.setBaselineAligned(true);
        add(value,txt(number,27,INK,700),-2,-2); gap(value,4);
        if(arrow) add(value,new Glyph(this,"up",GREEN),dp(13),dp(13));
        add(l,value,-1,-2); space(l,1); add(l,txt("GB",14,GRAY,400),-2,-2); return l;
    }
    private LinearLayout agentCard() {
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
        add(approve,txt("Review & Approve",17,WHITE,600),-2,-2); gap(approve,8);
        add(approve,new Glyph(this,"right",WHITE),dp(17),dp(17));
        tap(approve,BLUE,26,this::openReview);
        add(c,approve,-1,dp(51)); return c;
    }
    private void showHome() {
        reviewing = false;
        setContentView(home());
    }
    private void openReview() {
        reviewPlan = new ReviewPlan();
        reviewing = true;
        setContentView(reviewScreen());
    }
    @Override public void onBackPressed() {
        if (duplicateViewing) leaveDuplicates(false); else if (reviewing) showHome(); else super.onBackPressed();
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
            add(info,txt(i==0 && reviewPlan.isSelected(0)?ReviewPlan.selectedCount(reviewPlan.draftCopies())+" extra copies selected":ReviewPlan.DETAILS[i],13,GRAY,400),-1,-2); space(info,5);
            TextView review=txt("Review items",13,BLUE,600);
            review.setMinHeight(dp(32)); review.setGravity(Gravity.CENTER_VERTICAL);
            review.setContentDescription("Review items: "+ReviewPlan.NAMES[i]);
            tap(review,WHITE,8,()->reviewCategory(index));
            add(info,review,-1,-2); weight(category,info); gap(category,8);
            LinearLayout right=column(); right.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            add(right,txt(ReviewPlan.gb(i==0 && reviewPlan.isSelected(0)?reviewPlan.categoryMb(0):ReviewPlan.MB[i]),15,INK,600),-2,-2);
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
                View divider=new View(this);divider.setBackgroundColor(0xffeeeef0);add(plan,divider,-1,dp(1));
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
        add(approve,txt("Approve "+ReviewPlan.gb(reviewPlan.total())+" Cleanup",16,WHITE,600),-2,-2);
        gap(approve,8);add(approve,new Glyph(this,"right",WHITE),dp(17),dp(17));
        if(reviewPlan.total()>0) tap(approve,BLUE,26,()->confirmPreview());
        else {approve.setBackground(round(0xffc7c7cc,26));approve.setEnabled(false);}
        add(footer,approve,-1,dp(52));space(footer,5);
        TextView later=centered("Not now",14,GRAY,500);
        tap(later,WHITE,20,this::showHome);add(footer,later,-1,dp(44));
        space(root,6);add(root,footer,-1,-2);return root;
    }
    private void reviewCategory(int index) {
        if(index==0){openDuplicates();return;}
        new AlertDialog.Builder(this).setTitle(ReviewPlan.NAMES[index])
                .setMessage("Sample category: "+ReviewPlan.DETAILS[index]+" · "+
                        ReviewPlan.gb(ReviewPlan.MB[index])+
                        ". Individual file review will be built next. No device files are accessed in this preview.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton(reviewPlan.isSelected(index)?"Deselect":"Select for preview",(d,w)->{
                    reviewPlan.setSelected(index,!reviewPlan.isSelected(index));
                    setContentView(reviewScreen());
                }).show();
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
        TextView confirm=centered("Confirm selection",17,WHITE,600);
        tap(confirm,BLUE,26,()->leaveDuplicates(true));
        add(root,confirm,-1,dp(52));
        return root;
    }
    private void thinRule(LinearLayout parent) {
        View rule=new View(this);rule.setBackgroundColor(0xffe5e5ea);add(parent,rule,-1,dp(1));
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
                choice.addView(new Glyph(this,duplicateDraft[index]?"selected":"unselected",duplicateDraft[index]?BLUE:WHITE),
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
        android.app.Dialog dialog=new android.app.Dialog(this,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout layout=column();layout.setBackgroundColor(BG);
        layout.setPadding(dp(18),v(12),dp(18),v(16));
        FrameLayout header=new FrameLayout(this);
        header.addView(centered(photo==0?"Protected original":"Duplicate copy",17,INK,600),
                new FrameLayout.LayoutParams(-1,-1));
        FrameLayout close=new FrameLayout(this);
        close.addView(new Glyph(this,"back",INK),new FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER));
        close.setContentDescription("Close photo preview");
        tap(close,WHITE,22,dialog::dismiss);
        header.addView(close,new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.START|Gravity.CENTER_VERTICAL));
        add(layout,header,-1,dp(44));
        FrameLayout preview=new FrameLayout(this);
        SamplePhoto image=new SamplePhoto(this,group);
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
            add(layout,centered("Keep · Protected by Agent",15,GREEN,600),-1,dp(52));
        }else{
            int index=group*2+photo-1;
            TextView toggle=centered(duplicateDraft[index]?"Keep this copy":"Select copy",17,WHITE,600);
            tap(toggle,BLUE,26,()->{
                duplicateDraft[index]=!duplicateDraft[index];
                int offset=scroll.getScrollY();dialog.dismiss();setContentView(duplicateScreen(offset));
            });
            add(layout,toggle,-1,dp(52));
        }
        dialog.setContentView(layout);dialog.show();
        if(dialog.getWindow()!=null){
            dialog.getWindow().setLayout(-1,-1);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setNavigationBarColor(BG);
            dialog.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    /** Procedural sample thumbnails: each group repeats the exact same illustration. */
    private static final class SamplePhoto extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int group;
        SamplePhoto(Context context,int group){super(context);this.group=group;}
        private void color(int c){paint.setColor(c);paint.setStyle(Paint.Style.FILL);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);c.save();c.scale(getWidth()/110f,getHeight()/125f);
            int[] skies={0xffbcd9e7,0xffe7d4bf,0xffc3d6ca,0xffd5c9e3,0xffead2ce,0xffc9ddea};
            int[] hills={0xff819ca7,0xffa99079,0xff789c89,0xff9292ae,0xffa67e78,0xff758fa8};
            color(skies[group%6]);c.drawRect(0,0,110,125,paint);
            color(0xffffefca);c.drawCircle(72+(group%4)*5,21+(group%3)*4,12,paint);
            color(hills[group%6]);
            Path mountain=new Path();mountain.moveTo(-10,95);mountain.lineTo(26+group%12,30+group%9);
            mountain.lineTo(74,85);mountain.lineTo(96,48);mountain.lineTo(120,90);mountain.lineTo(120,125);
            mountain.lineTo(-10,125);mountain.close();c.drawPath(mountain,paint);
            color(0xff60877b);Path foreground=new Path();foreground.moveTo(0,102);
            foreground.quadTo(30,87,60,101);foreground.quadTo(88,111,110,92);
            foreground.lineTo(110,125);foreground.lineTo(0,125);foreground.close();c.drawPath(foreground,paint);
            c.restore();
        }
    }

    private void confirmPreview() {
        new AlertDialog.Builder(this).setTitle("Approve cleanup?")
                .setMessage("Preview: "+reviewPlan.count()+" sample items · "+ReviewPlan.gb(reviewPlan.total())+
                        ". No real files will be deleted.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Approve preview",(d,w)->demo("Approval preview","Selection confirmed. No files were changed."))
                .show();
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
                p.setColor(i<Math.round(progress*81)?BLUE:0xffdfe2e8);
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
            p.setTextSize(size); c.drawText(text,x,y,p);
        }
    }
    private final class StorageBar extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        StorageBar(Context c){super(c);setContentDescription("85.2 of 128 gigabytes used, 42.8 gigabytes available");}
        @Override protected void onDraw(Canvas c){
            float[] parts={.45f,.215625f,.334375f}; int[] colors={BLUE,0xff91bdff,0xffe5e5ea};
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

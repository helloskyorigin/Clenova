package com.clenova.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Foreground, local-only manual review. Swipes never directly delete media. */
public final class SwipePhotosActivity extends Activity {
    private static final int PHOTO_ACCESS=201, TRASH=202, MAX_SELECTION=100;
    private static final int REMOVE=1, KEEP=2, SKIP=3;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService previews=Executors.newSingleThreadExecutor();
    private final Map<String,Integer> decisions=new LinkedHashMap<>();
    private final ArrayDeque<SmartMediaScan.Item> undo=new ArrayDeque<>();
    private final List<SmartMediaScan.Item> items=new ArrayList<>();
    private final Map<String,String> versions=new HashMap<>(), monthsByUri=new HashMap<>();
    private SmartMediaScan job;
    private Future<?> previewTask;
    private int epoch, imageEpoch;
    private boolean dark, partial, loading, reviewing, approvedPending, permissionPending, reload, destroyed, previewReady, animating;
    private String month="All photos";
    private int bg,card,ink,muted,blue,pale,red,green;
    private AppPreferences prefs;
    private ImageView photo;
    private TextView photoStatus, removeButton;
    private LinearLayout controls;
    private SmartMediaScan.Item shown;
    private float downX,downY;
    private boolean dragging, resumed;
    private TextView swipeHint;
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=new AppPreferences(this);
        dark=prefs.appearance().equals("Dark")||(prefs.appearance().equals("System")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=dark?0xff141416:0xfff5f5f7;card=dark?0xff242426:0xffffffff;ink=dark?0xfff4f4f6:0xff111111;
        muted=dark?0xffa2a2aa:0xff86868b;blue=dark?0xff409cff:0xff007aff;pale=dark?0xff19334c:0xffe6f0ff;red=dark?0xffff6961:0xffff453a;green=dark?0xff61d883:0xff249d42;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27&&!dark)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        approvedPending=state!=null&&state.getBoolean("trash_pending");
        if(approvedPending){status("Waiting for Android approval",null);return;}
        open();
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("trash_pending",approvedPending);}
    private void open(){if(SmartMediaScan.hasPhotoAccess(this))load();else accessScreen();}
    private String tr(String s){return AppStrings.translate(prefs.language(),s);}
    private GradientDrawable rounded(int c,int radius){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout p,View v,int w,int h){p.addView(v,new LinearLayout.LayoutParams(w,h));}
    private void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private void gap(LinearLayout p,int n){add(p,new View(this),dp(n),dp(n));}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(tr(s));t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private TextView button(String s,int color,int fill,Runnable action){
        TextView t=label(s,15,color,true);t.setGravity(Gravity.CENTER);t.setBackground(rounded(fill,26));t.setPadding(dp(12),dp(8),dp(12),dp(8));
        t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->action.run());return t;
    }
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(16),dp(10),dp(16),dp(10));return l;}
    private void header(LinearLayout root,String title,String detail,boolean canUndo){
        LinearLayout h=row();TextView back=button("‹",ink,card,this::onBackPressed);back.setTextSize(27);back.setContentDescription("Back");add(h,back,dp(44),dp(44));gap(h,8);
        LinearLayout center=col();center.setGravity(Gravity.CENTER);TextView heading=label(title,20,ink,true);heading.setGravity(Gravity.CENTER);add(center,heading,-1,-2);
        if(!detail.isEmpty()){gap(center,3);TextView sub=label(detail,12,muted,false);sub.setGravity(Gravity.CENTER);add(center,sub,-1,-2);}weight(h,center);gap(h,8);
        if(canUndo){TextView b=button("↶",ink,card,this::undoLast);b.setTextSize(24);b.setContentDescription("Undo last swipe");b.setEnabled(!undo.isEmpty());b.setAlpha(undo.isEmpty()?.35f:1f);add(h,b,dp(44),dp(44));}else add(h,new View(this),dp(44),dp(44));
        add(root,h,-1,dp(52));gap(root,8);
    }
    private void status(String text,Runnable retry){
        LinearLayout l=root();header(l,"Swipe Photos","",false);LinearLayout body=col();body.setGravity(Gravity.CENTER);
        TextView t=label(text,16,ink,false);t.setGravity(Gravity.CENTER);add(body,t,-1,-2);
        if(retry!=null){gap(body,18);add(body,button("Try again",0xffffffff,blue,retry),-1,dp(50));}
        l.addView(body,new LinearLayout.LayoutParams(-1,0,1));setContentView(l);
    }
    private void accessScreen(){
        LinearLayout l=root();header(l,"Swipe Photos","",false);LinearLayout body=col();body.setGravity(Gravity.CENTER);
        TextView title=label("Review your photos",24,ink,true);title.setGravity(Gravity.CENTER);add(body,title,-1,-2);gap(body,10);
        TextView detail=label("Allow photo access to review your gallery. Nothing is uploaded or removed automatically.",15,muted,false);detail.setGravity(Gravity.CENTER);add(body,detail,-1,-2);gap(body,22);
        add(body,button("Allow photo access",0xffffffff,blue,this::askPermission),-1,dp(50));gap(body,10);
        add(body,button("Open Android settings",blue,card,()->{if(permissionPending)return;permissionPending=true;if(!ToolAccess.openSettings(this,PHOTO_ACCESS)){permissionPending=false;toast("Android settings unavailable");}}),-1,dp(48));
        l.addView(body,new LinearLayout.LayoutParams(-1,0,1));setContentView(l);
    }
    private void askPermission(){
        if(permissionPending)return;if(ToolAccess.has(this,ToolAccess.PHOTOS)){load();return;}
        permissionPending=true;if(!ToolAccess.launch(this,ToolAccess.PHOTOS,PHOTO_ACCESS)){permissionPending=false;toast("Could not open photo access");}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);if(request!=PHOTO_ACCESS)return;
        permissionPending=false;if(SmartMediaScan.hasPhotoAccess(this))load();else accessScreen();
    }
    private SmartMediaScan scan(){return new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this));}
    private void cancelWork(){epoch++;imageEpoch++;if(job!=null){job.cancel();job=null;}if(previewTask!=null){previewTask.cancel(true);previewTask=null;}loading=false;animating=false;previewReady=false;}
    private void clear(){cancelWork();items.clear();decisions.clear();undo.clear();versions.clear();monthsByUri.clear();shown=null;reviewing=false;month="All photos";}
    private void load(){
        clear();loading=true;status("Loading your photos…",null);final int token=epoch;final SmartMediaScan current=scan();job=current;
        worker.execute(()->{try{
            Map<String,String> snapshot=new HashMap<>();
            if(Build.VERSION.SDK_INT>=30)for(String volume:MediaStore.getExternalVolumeNames(this)){try{snapshot.put(volume,MediaStore.getVersion(this,volume));}catch(RuntimeException e){snapshot.put(volume,"");}}
            SmartMediaScan.Gallery result=current.gallery();
            if(Build.VERSION.SDK_INT>=30)for(String volume:new ArrayList<>(snapshot.keySet())){try{if(!snapshot.get(volume).equals(MediaStore.getVersion(this,volume)))snapshot.put(volume,"");}catch(RuntimeException e){snapshot.put(volume,"");}}
            runOnUiThread(()->{if(destroyed||token!=epoch)return;job=null;loading=false;items.addAll(result.items);versions.putAll(snapshot);partial=result.partial;render();});
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;job=null;loading=false;if(!SmartMediaScan.hasPhotoAccess(this))accessScreen();else status("Could not read your photos.",this::load);});}});
    }
    private String key(SmartMediaScan.Item i){return i.uri.toString();}
    private String monthOf(SmartMediaScan.Item i){return monthsByUri.computeIfAbsent(key(i),k->i.modified<=0?"Unknown date":new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(new Date(i.modified)));}
    private String date(SmartMediaScan.Item i){return i.modified<=0?"Unknown date":new SimpleDateFormat("d MMMM · h:mm a",Locale.getDefault()).format(new Date(i.modified));}
    private String size(long bytes){return bytes>=1073741824L?String.format(Locale.getDefault(),"%.2f GB",bytes/1073741824d):bytes>=1048576?String.format(Locale.getDefault(),"%.1f MB",bytes/1048576d):String.format(Locale.getDefault(),"%.0f KB",bytes/1024d);}
    private List<SmartMediaScan.Item> group(){List<SmartMediaScan.Item> out=new ArrayList<>();for(SmartMediaScan.Item i:items)if(month.equals("All photos")||month.equals(monthOf(i)))out.add(i);return out;}
    private List<SmartMediaScan.Item> selected(){List<SmartMediaScan.Item> out=new ArrayList<>();for(SmartMediaScan.Item i:items)if(Integer.valueOf(REMOVE).equals(decisions.get(key(i))))out.add(i);return out;}
    private long total(List<SmartMediaScan.Item> list){long sum=0;for(SmartMediaScan.Item i:list)sum+=i.bytes;return sum;}
    private boolean eligible(SmartMediaScan.Item i){return Build.VERSION.SDK_INT>=30&&!i.protectedItem&&i.generation>=0&&i.volume!=null&&i.path!=null&&!versions.getOrDefault(i.volume,"").isEmpty();}
    private void pickMonth(){
        if(animating)return;
        LinkedHashSet<String> set=new LinkedHashSet<>();set.add("All photos");for(SmartMediaScan.Item i:items)set.add(monthOf(i));
        String[] months=set.toArray(new String[0]);new AlertDialog.Builder(this,dialogTheme()).setTitle("Choose month").setSingleChoiceItems(months,Arrays.asList(months).indexOf(month),(d,w)->{month=months[w];d.dismiss();render();}).setNegativeButton("Cancel",null).show();
    }
    private int dialogTheme(){return dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert;}
    private void render(){
        if(destroyed||loading||approvedPending)return;reviewing=false;imageEpoch++;if(previewTask!=null)previewTask.cancel(true);previewReady=false;animating=false;shown=null;
        List<SmartMediaScan.Item> group=group();int done=0;for(SmartMediaScan.Item i:group){if(decisions.containsKey(key(i)))done++;else if(shown==null)shown=i;}
        LinearLayout l=root();header(l,"Swipe Photos",done+" of "+group.size()+" reviewed",true);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Math.max(1,group.size()));progress.setProgress(done);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(blue));add(l,progress,-1,dp(5));gap(l,9);
        LinearLayout filter=row();TextView m=button(month+"  ▾",ink,card,this::pickMonth);weight(filter,m);gap(filter,8);if(shown!=null)add(filter,label(size(shown.bytes),13,muted,false),-2,-2);add(l,filter,-1,dp(43));gap(l,8);
        if(partial){add(l,label("Limited photo access or scan coverage",11,muted,false),-1,-2);gap(l,5);}
        if(Build.VERSION.SDK_INT<30){add(l,label("Review only on this Android version",12,muted,false),-1,-2);gap(l,5);}
        if(shown==null){
            LinearLayout empty=col();empty.setGravity(Gravity.CENTER);TextView t=label(group.isEmpty()?"No photos available":"All caught up",23,ink,true);t.setGravity(Gravity.CENTER);add(empty,t,-1,-2);gap(empty,10);
            TextView detail=label(group.isEmpty()?"Try another month or change photo access.":"Review another month or check your removal list.",14,muted,false);detail.setGravity(Gravity.CENTER);add(empty,detail,-1,-2);
            gap(empty,12);add(empty,button("Choose month",blue,card,this::pickMonth),-1,dp(45));gap(empty,8);add(empty,button("Refresh gallery",blue,card,()->{if(decisions.isEmpty())load();else confirmReset();}),-1,dp(45));
            l.addView(empty,new LinearLayout.LayoutParams(-1,0,1));
        }else{
            final SmartMediaScan.Item current=shown;
            FrameLayout frame=new FrameLayout(this);frame.setBackground(rounded(card,24));frame.setClipToOutline(true);
            photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.FIT_CENTER);photo.setContentDescription("Current photo");frame.addView(photo,new FrameLayout.LayoutParams(-1,-1));
            photoStatus=label("Loading photo…",15,muted,true);photoStatus.setGravity(Gravity.CENTER);frame.addView(photoStatus,new FrameLayout.LayoutParams(-1,-1));
            swipeHint=label("",17,0xffffffff,true);swipeHint.setGravity(Gravity.CENTER);swipeHint.setPadding(dp(16),dp(8),dp(16),dp(8));swipeHint.setVisibility(View.GONE);
            FrameLayout.LayoutParams hintParams=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);hintParams.topMargin=dp(18);frame.addView(swipeHint,hintParams);
            l.addView(frame,new LinearLayout.LayoutParams(-1,0,1));gap(l,6);
            LinearLayout info=row();weight(info,label("Modified "+date(current)+(eligible(current)?"":" · Protected / read-only"),12,muted,false));TextView zoom=button("⤢",blue,card,()->showPreview(current));zoom.setContentDescription("Expand photo");add(info,zoom,dp(44),dp(44));add(l,info,-1,dp(44));
            controls=row();controls.setGravity(Gravity.CENTER);removeButton=decisionControl(controls,"×","Remove",red,card,REMOVE);gap(controls,24);decisionControl(controls,"»","Skip",muted,card,SKIP);gap(controls,24);decisionControl(controls,"✓","Keep",0xffffffff,0xff34c759,KEEP);add(l,controls,-1,-2);
            removeButton.setEnabled(false);removeButton.setAlpha(.35f);
            photo.setOnTouchListener((v,event)->gesture(event,current));
        }
        gap(l,10);List<SmartMediaScan.Item> selected=selected();LinearLayout bottom=row();bottom.setPadding(dp(12),dp(9),dp(12),dp(9));bottom.setBackground(rounded(card,20));
        LinearLayout count=col();add(count,label(selected.size()+" photos selected",14,ink,true),-1,-2);gap(count,3);add(count,label(size(total(selected))+" to review",12,muted,false),-1,-2);weight(bottom,count);gap(bottom,8);
        TextView review=button("Review",0xffffffff,blue,this::review);review.setEnabled(!selected.isEmpty());review.setAlpha(selected.isEmpty()?.45f:1f);add(bottom,review,-2,dp(44));add(l,bottom,-1,-2);gap(l,7);
        TextView foot=label("Your photos. Your decision.",11,muted,false);foot.setGravity(Gravity.CENTER);add(l,foot,-1,-2);
        setContentView(l);if(shown!=null)loadPhoto(shown);
    }
    private TextView decisionControl(LinearLayout parent,String glyph,String title,int tint,int fill,int action){
        LinearLayout c=col();c.setGravity(Gravity.CENTER);TextView b=button(glyph,tint,fill,()->decide(action));b.setTextSize(action==SKIP?22:32);b.setContentDescription(title);int size=action==SKIP?46:62;add(c,b,dp(size),dp(size));gap(c,4);
        add(c,label(title,12,action==KEEP?green:tint,true),-2,-2);add(parent,c,-2,-2);return b;
    }
    private Bitmap decode(SmartMediaScan.Item item,int max)throws Exception{
        if(Build.VERSION.SDK_INT>=28)return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(),item.uri),(decoder,info,source)->{
            int w=info.getSize().getWidth(),h=info.getSize().getHeight();float scale=Math.min(1f,max/(float)Math.max(w,h));decoder.setTargetSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(item.uri)){BitmapFactory.decodeStream(in,null,o);}
        if(o.outWidth<=0||o.outHeight<=0)return null;o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>max)o.inSampleSize*=2;o.inJustDecodeBounds=false;
        try(InputStream in=getContentResolver().openInputStream(item.uri)){return BitmapFactory.decodeStream(in,null,o);}
    }
    private void loadPhoto(SmartMediaScan.Item current){
        final int token=++imageEpoch;
        previewTask=previews.submit(()->{Bitmap b=null;try{b=decode(current,1400);}catch(Exception ignored){}final Bitmap bitmap=b;
            runOnUiThread(()->{if(destroyed||token!=imageEpoch||shown!=current||reviewing){if(bitmap!=null)bitmap.recycle();return;}
                previewReady=bitmap!=null;if(bitmap!=null){photo.setImageBitmap(bitmap);photoStatus.setVisibility(View.GONE);}else photoStatus.setText("Photo unavailable · Skip to continue");
                boolean enabled=previewReady&&eligible(current);removeButton.setEnabled(enabled);removeButton.setAlpha(enabled?1f:.35f);
            });});
    }
    private boolean gesture(MotionEvent e,SmartMediaScan.Item current){
        if(animating||shown!=current||!previewReady)return false;
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();dragging=false;return true;
            case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-downX;if(Math.abs(dx)>dp(12)&&Math.abs(dx)>Math.abs(e.getRawY()-downY))dragging=true;if(dragging){photo.setTranslationX(dx*.75f);photo.setRotation(dx/dp(18));photo.setAlpha(Math.max(.5f,1-Math.abs(dx)/(float)Math.max(1,photo.getWidth())));swipeHint.setText(dx>0?"KEEP":eligible(current)?"REMOVE":"PROTECTED");swipeHint.setBackground(rounded(dx>0?0xff249d42:eligible(current)?0xffe23c32:0xff777777,10));swipeHint.setVisibility(View.VISIBLE);}return true;
            case MotionEvent.ACTION_UP:float delta=e.getRawX()-downX;if(dragging&&Math.abs(delta)>dp(82)){if(delta>0){decide(KEEP);return true;}if(eligible(current)){decide(REMOVE);return true;}toast("This photo is protected or read-only");}else if(!dragging)showPreview(current);
                resetDrag();return true;
            case MotionEvent.ACTION_CANCEL:resetDrag();return true;
            default:return true;
        }
    }
    private void resetDrag(){if(swipeHint!=null)swipeHint.setVisibility(View.GONE);photo.animate().translationX(0).rotation(0).alpha(1).setDuration(150).start();dragging=false;}
    private void decide(int action){
        if(animating||reviewing||shown==null||loading)return;SmartMediaScan.Item current=shown;
        if(action==REMOVE&&(!previewReady||!eligible(current))){toast("Review an eligible photo first");return;}
        if(action==REMOVE&&selected().size()>=MAX_SELECTION){toast("Review up to 100 selected photos first");resetDrag();return;}
        decisions.put(key(current),action);undo.addLast(current);animating=true;if(swipeHint!=null&&action!=SKIP){swipeHint.setText(action==KEEP?"KEEP":"REMOVE");swipeHint.setBackground(rounded(action==KEEP?0xff249d42:0xffe23c32,10));swipeHint.setVisibility(View.VISIBLE);}photo.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        final int token=imageEpoch;float direction=action==REMOVE?-1:action==KEEP?1:0;
        photo.animate().translationX(direction*photo.getWidth()).rotation(direction*12).alpha(0).setDuration(160).withEndAction(()->{if(!destroyed&&token==imageEpoch)render();}).start();
    }
    private void undoLast(){if(animating||undo.isEmpty())return;SmartMediaScan.Item last=undo.removeLast();decisions.remove(key(last));month=monthOf(last);render();}
    private void confirmReset(){new AlertDialog.Builder(this,dialogTheme()).setTitle("Restart review?").setMessage("Your current choices will be cleared. No files will be changed.").setNegativeButton("Cancel",null).setPositiveButton("Restart",(d,w)->load()).show();}
    private void showPreview(SmartMediaScan.Item i){
        if(destroyed||loading||approvedPending)return;
        LinearLayout box=col();box.setPadding(dp(12),dp(12),dp(12),dp(12));ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);add(box,image,-1,dp(Math.max(100,Math.min(420,getResources().getConfiguration().screenHeightDp-200))));
        TextView message=label(date(i)+" · "+size(i.bytes),13,muted,false);add(box,message,-1,-2);
        AlertDialog dialog=new AlertDialog.Builder(this,dialogTheme()).setTitle("Photo preview").setView(box).setPositiveButton("Done",null).create();dialog.show();
        previews.submit(()->{Bitmap b=null;try{b=decode(i,1800);}catch(Exception ignored){}final Bitmap bitmap=b;runOnUiThread(()->{if(!dialog.isShowing()||destroyed){if(bitmap!=null)bitmap.recycle();return;}if(bitmap!=null)image.setImageBitmap(bitmap);else message.setText("Photo unavailable");});});
    }
    private void review(){
        if(animating||selected().isEmpty()||loading||approvedPending)return;reviewing=true;imageEpoch++;if(previewTask!=null)previewTask.cancel(true);
        LinearLayout l=root();List<SmartMediaScan.Item> list=selected();header(l,"Review photos",list.size()+" selected · "+size(total(list)),false);
        TextView notice=label("Check every selection. Android will ask for permission to move these photos to Trash.",13,muted,false);add(l,notice,-1,-2);gap(l,12);
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);LinearLayout body=col();scroll.addView(body);l.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(SmartMediaScan.Item i:list){
            LinearLayout r=row();r.setBackground(rounded(card,15));r.setPadding(dp(12),dp(8),dp(12),dp(8));
            LinearLayout info=col();add(info,label(i.name,14,ink,true),-1,-2);gap(info,3);add(info,label(date(i)+" · "+size(i.bytes),12,muted,false),-1,-2);info.setOnClickListener(v->showPreview(i));weight(r,info);gap(r,6);
            TextView view=button("View",blue,pale,()->showPreview(i));add(r,view,-2,dp(42));gap(r,4);
            TextView keep=button("Keep",green,bg,()->{decisions.put(key(i),KEEP);if(selected().isEmpty())render();else review();});add(r,keep,-2,dp(42));add(body,r,-1,-2);gap(body,8);
        }
        gap(l,8);add(l,button("Move "+list.size()+" photos to Trash",0xffffffff,blue,()->new AlertDialog.Builder(this,dialogTheme()).setTitle("Confirm removal").setMessage("Move only the selected photos to Android Trash? Nothing is permanently deleted by Clenova.").setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->approve()).show()),-1,dp(52));setContentView(l);
    }
    private void approve(){
        if(Build.VERSION.SDK_INT<30||loading||approvedPending||!SmartMediaScan.hasPhotoAccess(this)){toast("Trash approval is unavailable");return;}
        List<SmartMediaScan.Item> request=selected();if(request.isEmpty()||request.size()>MAX_SELECTION)return;
        loading=true;status("Checking selections and protection…",null);final int token=++epoch;final SmartMediaScan current=scan();job=current;
        worker.execute(()->{try{
            SmartMediaScan.Gallery fresh=current.gallery();Map<String,SmartMediaScan.Item> map=new HashMap<>();for(SmartMediaScan.Item i:fresh.items)map.put(key(i),i);
            ArrayList<Uri> uris=new ArrayList<>();
            for(SmartMediaScan.Item i:request){String version=i.volume==null?"":versions.getOrDefault(i.volume,"");if(version.isEmpty()||!version.equals(MediaStore.getVersion(this,i.volume))||!SmartMediaScan.sameGalleryItem(i,map.get(key(i))))throw new IllegalStateException("Selection changed");uris.add(i.uri);}
            runOnUiThread(()->{if(destroyed||token!=epoch)return;job=null;loading=false;if(!resumed){clear();reload=true;return;}if(!SmartMediaScan.hasPhotoAccess(this)){clear();accessScreen();return;}
                try{PendingIntent intent=MediaStore.createTrashRequest(getContentResolver(),uris,true);approvedPending=true;startIntentSenderForResult(intent.getIntentSender(),TRASH,null,0,0,0);}
                catch(Exception e){approvedPending=false;toast("Android could not open trash approval");review();}
            });
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;job=null;loading=false;toast("Photos or protection changed. Review again.");load();});}});
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request==PHOTO_ACCESS){permissionPending=false;reload=false;open();return;}if(request!=TRASH)return;approvedPending=false;reload=false;
        toast(result==RESULT_OK?"Android trash request approved. Refreshing gallery.":"Trash request cancelled. No completion claimed.");load();
    }
    private void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_SHORT).show();}
    @Override public void onBackPressed(){
        if(approvedPending)return;if(loading){clear();finish();return;}if(animating)return;
        if(reviewing){render();return;}if(decisions.isEmpty()){finish();return;}
        new AlertDialog.Builder(this,dialogTheme()).setTitle("Leave photo review?").setMessage("Your current choices will be discarded. Nothing will be removed.").setNegativeButton("Stay",null).setPositiveButton("Leave",(d,w)->finish()).show();
    }
    @Override protected void onStop(){super.onStop();if(!permissionPending&&!approvedPending&&!isFinishing()){clear();reload=true;}}
    @Override protected void onPause(){resumed=false;super.onPause();}
    @Override protected void onResume(){super.onResume();resumed=true;if(reload&&!approvedPending&&!permissionPending){reload=false;open();}}
    @Override protected void onDestroy(){destroyed=true;cancelWork();worker.shutdownNow();previews.shutdownNow();super.onDestroy();}
}

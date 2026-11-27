package com.clenova.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.transformer.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** The four approved compressor screens and their real local workflows. */
@androidx.annotation.OptIn(markerClass = UnstableApi.class)
public final class VideoCompressorActivity extends Activity {
    private static final int ACCESS=211, PICK=212, SAVE=213, TRASH=214, PAGE=24;
    private static final int GALLERY=0, SETTINGS=1, ENCODING=2, RESULT=3, HISTORY=4;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final java.util.concurrent.ThreadPoolExecutor thumbs=PreviewWork.pool(1);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final List<SmartMediaScan.Item> videos=new ArrayList<>();
    private final List<Future<?>> thumbnailJobs=new ArrayList<>();
    private final Map<String,String> galleryVersions=new HashMap<>();
    private AppPreferences prefs;private ActivityHistory history;private CleanupReceipt cleanupReceipt;
    private int bg,card,ink,gray,blue,pale,green,red,phase=GALLERY,page,sort,epoch,thumbEpoch;
    private boolean dark,busy,limited,encoding,destroyed,foreground,trashPending,sourceRemoved,loadingGallery,accessPending,refreshGallery;
    private String error="",sourceVersion="";
    private SmartMediaScan galleryJob;
    private SmartMediaScan.Item original;
    private VideoCompressionEngine.Source source;
    private VideoCompressionEngine.Preset preset=VideoCompressionEngine.Preset.BALANCED;
    private VideoCompressionEngine.Output output;
    private Uri saved;
    private boolean savedLocal, outputReviewed;
    private File temp;
    private Transformer transformer;
    private Future<?> work;
    private ProgressRing ring;
    private TextView progressLabel;
    private Dialog previewDialog;
    private VideoView player;
    private final ProgressHolder progressHolder=new ProgressHolder();
    private final Runnable progressTick=new Runnable(){public void run(){
        if(!encoding||transformer==null||destroyed)return;
        int value=transformer.getProgress(progressHolder)==Transformer.PROGRESS_STATE_AVAILABLE?Math.max(0,Math.min(99,progressHolder.progress)):-1;
        if(ring!=null)ring.setProgress(value);
        if(progressLabel!=null)progressLabel.setText(value<0?"Preparing video…":"Encoding video · "+value+"%");
        handler.postDelayed(this,250);
    }};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=new AppPreferences(this);history=new ActivityHistory(this);
        dark=prefs.appearance().equals("Dark")||(prefs.appearance().equals("System")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=dark?0xff141416:0xfff5f5f7;card=dark?0xff242426:0xffffffff;ink=dark?0xfff4f4f6:0xff111111;gray=dark?0xffa2a2aa:UiPolish.LIGHT_MUTED;
        blue=dark?0xff409cff:0xff007aff;pale=dark?0xff19334c:0xffe6f0ff;green=dark?0xff61d883:0xff249d42;red=dark?0xffff6961:0xffe43e35;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        trashPending=state!=null&&state.getBoolean("trash_pending");cleanupReceipt=CleanupReceipt.restore(state==null?null:state.getBundle("cleanup_receipt"));
        // Only remove stale compressor-owned cache files, never personal files.
        File[] stale=getCacheDir().listFiles((dir,name)->name.startsWith("clenova-compress-")&&name.endsWith(".mp4"));
        if(stale!=null)for(File f:stale)if(System.currentTimeMillis()-f.lastModified()>24L*60*60*1000)f.delete();
        if(trashPending){status("Waiting for Android approval");return;}
        loadGallery();
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("trash_pending",trashPending);if(cleanupReceipt!=null)b.putBundle("cleanup_receipt",cleanupReceipt.save());}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String tr(String s){return AppStrings.translate(prefs.language(),s);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout p,View v,int w,int h){p.addView(v,new LinearLayout.LayoutParams(w,h));}
    private void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private void gap(LinearLayout p,int n){add(p,new View(this),dp(n),dp(n));}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(tr(s));t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private TextView centered(String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView button(String s,int color,int fill,Runnable r){TextView t=centered(s,15,color,true);t.setBackground(shape(UiPolish.buttonFill(fill,color),26));t.setPadding(dp(12),dp(6),dp(12),dp(6));t.setFocusable(true);t.setClickable(true);t.setOnClickListener(v->{if(!destroyed)r.run();});return t;}
    private LinearLayout block(){LinearLayout l=col();l.setBackground(shape(card,22));l.setPadding(dp(15),dp(14),dp(15),dp(14));return l;}
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(18),dp(10),dp(18),dp(10));return l;}
    private LinearLayout body(LinearLayout root){ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout b=col();scroll.addView(b,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return b;}
    private void header(LinearLayout root,String title,boolean historyButton){
        LinearLayout h=row();TextView back=button("‹",ink,card,this::onBackPressed);back.setTextSize(26);back.setContentDescription("Back");add(h,back,dp(44),dp(44));gap(h,8);
        weight(h,centered(title,19,ink,true));gap(h,8);
        if(historyButton){TextView past=button("◷",ink,card,this::showHistory);past.setTextSize(25);past.setContentDescription("Compression history");add(h,past,dp(44),dp(44));}else add(h,new View(this),dp(44),dp(44));
        add(root,h,-1,dp(52));gap(root,12);
    }
    private void foot(LinearLayout l,String s){gap(l,9);add(l,centered(s,11,gray,false),-1,-2);}
    private void primary(LinearLayout l,String title,Runnable action){gap(l,10);add(l,button(title,0xffffffff,blue,action),-1,dp(52));}
    private void rule(LinearLayout l){View line=new View(this);line.setBackgroundColor(dark?0xff38383c:0xffeaeaec);add(l,line,-1,dp(1));}
    private int theme(){return dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert;}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(this,theme());}
    private void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_SHORT).show();}
    private String size(long bytes){if(bytes<0)return "Unknown";return bytes>=1073741824L?String.format(Locale.getDefault(),"%.2f GB",bytes/1073741824d):bytes>=1048576?String.format(Locale.getDefault(),"%.1f MB",bytes/1048576d):String.format(Locale.getDefault(),"%.0f KB",bytes/1024d);}
    private String duration(long ms){if(ms<=0)return "Unknown duration";long s=ms/1000;return s>=3600?String.format(Locale.getDefault(),"%d:%02d:%02d",s/3600,s/60%60,s%60):String.format(Locale.getDefault(),"%02d:%02d",s/60,s%60);}
    private void status(String message){LinearLayout l=root();header(l,"Video Compressor",false);LinearLayout b=col();b.setGravity(Gravity.CENTER);ProgressBar p=new ProgressBar(this);add(b,p,dp(44),dp(44));gap(b,16);add(b,centered(message,15,gray,false),-1,-2);l.addView(b,new LinearLayout.LayoutParams(-1,0,1));setContentView(l);}
    private void cancelThumbs(){thumbEpoch++;for(Future<?> f:thumbnailJobs)f.cancel(true);thumbnailJobs.clear();thumbs.purge();}
    private void thumb(ImageView view,Uri uri,int max){
        final int token=thumbEpoch;
        thumbnailJobs.add(thumbs.submit(()->{Bitmap b=null;try{if(Build.VERSION.SDK_INT>=29)b=getContentResolver().loadThumbnail(uri,new android.util.Size(max,max),null);else b=VideoCompressionEngine.thumb(this,uri,max).bitmap;}catch(Exception ignored){}
            final Bitmap result=b;runOnUiThread(()->{if(destroyed||token!=thumbEpoch||!view.isAttachedToWindow()){if(result!=null)result.recycle();return;}if(result!=null)view.setImageBitmap(result);});}));
    }
    private void loadGallery(){
        if(destroyed||busy)return;epoch++;cancelThumbs();if(galleryJob!=null)galleryJob.cancel();videos.clear();galleryVersions.clear();page=0;phase=GALLERY;error="";
        if(!SmartMediaScan.hasVideoAccess(this)){limited=false;loadingGallery=false;showGallery();return;}
        loadingGallery=true;showGallery();final int token=++epoch;galleryJob=new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this));final SmartMediaScan scan=galleryJob;
        work=io.submit(()->{try{
            Map<String,String> versions=new HashMap<>();if(Build.VERSION.SDK_INT>=30)for(String volume:MediaStore.getExternalVolumeNames(this))try{versions.put(volume,MediaStore.getVersion(this,volume));}catch(RuntimeException ignored){}
            SmartMediaScan.Gallery result=scan.videoGallery();
            if(Build.VERSION.SDK_INT>=30)for(String volume:new ArrayList<>(versions.keySet()))if(!versions.get(volume).equals(MediaStore.getVersion(this,volume)))versions.remove(volume);
            runOnUiThread(()->{if(destroyed||token!=epoch)return;galleryJob=null;loadingGallery=false;videos.addAll(result.items);galleryVersions.putAll(versions);limited=result.partial;sortVideos();if(phase==GALLERY)showGallery();});
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;galleryJob=null;loadingGallery=false;error="Could not load videos. Browse files or refresh.";if(phase==GALLERY)showGallery();});}});
    }
    private void sortVideos(){Comparator<SmartMediaScan.Item> comparator=sort==0?Comparator.comparingLong((SmartMediaScan.Item i)->i.bytes).reversed():sort==1?Comparator.comparingLong((SmartMediaScan.Item i)->i.modified).reversed():Comparator.comparingLong((SmartMediaScan.Item i)->i.duration).reversed();videos.sort(comparator.thenComparing(i->i.uri.toString()));}
    private void requestAccess(){
        if(accessPending||busy)return;if(ToolAccess.has(this,ToolAccess.VIDEOS)){loadGallery();return;}
        accessPending=true;if(!ToolAccess.launch(this,ToolAccess.VIDEOS,ACCESS)){accessPending=false;toast("Could not open access settings");}
    }
    private void accessReturned(){accessPending=false;refreshGallery=false;loadGallery();if(!ToolAccess.has(this,ToolAccess.VIDEOS))toast("Access not granted. Browse files instead.");}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==ACCESS)accessReturned();}
    private void browse(){if(busy)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){toast("No file picker available");}}
    private void showGallery(){
        if(destroyed)return;phase=GALLERY;cancelThumbs();LinearLayout l=root();header(l,"Video Compressor",true);LinearLayout b=body(l);
        add(b,text("Save more space.",28,ink,true),-1,-2);gap(b,5);add(b,text("Smaller videos. Same memories.",13,gray,false),-1,-2);gap(b,22);
        LinearLayout section=row();weight(section,text("Your videos",19,ink,true));TextView sorting=button(new String[]{"Largest first","Newest first","Longest first"}[sort]+" ▾",blue,card,()->dialog().setTitle("Sort videos").setSingleChoiceItems(new String[]{"Largest first","Newest first","Longest first"},sort,(d,w)->{sort=w;page=0;sortVideos();d.dismiss();showGallery();}).setNegativeButton("Cancel",null).show());add(section,sorting,-2,dp(40));add(b,section,-1,-2);gap(b,10);
        if(!SmartMediaScan.hasVideoAccess(this)){LinearLayout notice=block();add(notice,text("Allow video access",17,ink,true),-1,-2);gap(notice,7);add(notice,text("Choose from your gallery or browse a single file without gallery access.",13,gray,false),-1,-2);primary(notice,ToolAccess.settingsNeeded(this,ToolAccess.VIDEOS)?"Open Android settings":"Allow video access",this::requestAccess);add(b,notice,-1,-2);}
        else if(loadingGallery){LinearLayout c=block();add(c,centered("Loading your videos…",15,gray,false),-1,dp(100));add(b,c,-1,-2);}
        else if(videos.isEmpty()){LinearLayout c=block();add(c,centered(error.isEmpty()?"No accessible videos":error,15,gray,false),-1,dp(75));add(c,button("Refresh gallery",blue,pale,this::loadGallery),-1,dp(44));add(b,c,-1,-2);}
        else{
            if(limited){add(b,text("Limited video access or gallery coverage",12,gray,false),-1,-2);gap(b,8);if(Build.VERSION.SDK_INT>=34){add(b,button("Manage video access",blue,pale,this::requestAccess),-1,dp(42));gap(b,8);}}
            LinearLayout list=block();list.setPadding(dp(12),dp(4),dp(12),dp(4));int start=page*PAGE,end=Math.min(videos.size(),start+PAGE);
            for(int n=start;n<end;n++){SmartMediaScan.Item item=videos.get(n);if(n>start)rule(list);LinearLayout line=row();line.setPadding(0,dp(10),0,dp(10));
                ImageView image=new ImageView(this);image.setBackground(shape(pale,12));image.setClipToOutline(true);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setContentDescription(null);add(line,image,dp(86),dp(65));gap(line,12);
                LinearLayout info=col();TextView name=text(item.name,14,ink,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(info,name,-1,-2);gap(info,5);add(info,text((item.height>0?item.height+"p · ":"")+duration(item.duration),12,gray,false),-1,-2);gap(info,4);add(info,text(size(item.bytes),14,blue,true),-1,-2);weight(line,info);gap(line,8);add(line,text("›",23,gray,false),-2,-2);
                line.setFocusable(true);line.setClickable(true);line.setOnClickListener(v->select(item.uri,item));add(list,line,-1,-2);thumb(image,item.uri,240);
            }add(b,list,-1,-2);
            if(videos.size()>PAGE){gap(b,12);LinearLayout pages=row();TextView prev=button("Previous",blue,card,()->{if(page>0){page--;showGallery();}});prev.setEnabled(page>0);add(pages,prev,-2,dp(44));weight(pages,centered((page+1)+" / "+((videos.size()+PAGE-1)/PAGE),13,gray,false));TextView next=button("Next",blue,card,()->{if((page+1)*PAGE<videos.size()){page++;showGallery();}});next.setEnabled((page+1)*PAGE<videos.size());add(pages,next,-2,dp(44));add(b,pages,-1,-2);}
            gap(b,10);add(b,button("Refresh gallery",blue,card,this::loadGallery),-1,dp(44));
        }
        gap(l,8);add(l,button("Browse files",blue,pale,this::browse),-1,dp(50));foot(l,"Processed privately on your device.");setContentView(l);
    }
    private void select(Uri uri,SmartMediaScan.Item item){
        if(busy||destroyed)return;cancelThumbs();if(galleryJob!=null){galleryJob.cancel();galleryJob=null;loadingGallery=false;}
        clearOutput();source=null;original=item;sourceVersion=item==null||item.volume==null?"":galleryVersions.getOrDefault(item.volume,"");preset=VideoCompressionEngine.Preset.BALANCED;busy=true;status("Reading video…");final int token=++epoch;
        work=io.submit(()->{try{VideoCompressionEngine.Source value=VideoCompressionEngine.inspect(this,uri);
            runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;source=value;showSettings();});
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;source=null;toast("Unable to read this video. Choose another file.");loadGallery();});}});
    }
    private void showSettings(){
        if(source==null){loadGallery();return;}phase=SETTINGS;cancelThumbs();LinearLayout l=root();header(l,"Compress video",false);LinearLayout b=body(l);
        LinearLayout video=block();video.setPadding(dp(12),dp(12),dp(12),dp(12));FrameLayout preview=new FrameLayout(this);preview.setBackground(shape(pale,16));preview.setClipToOutline(true);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);preview.addView(image,new FrameLayout.LayoutParams(-1,-1));TextView play=button("▶",0xffffffff,0xcc000000,()->play(source.uri,false));FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(52),dp(52),Gravity.CENTER);preview.addView(play,pp);add(video,preview,-1,dp(168));thumb(image,source.uri,640);gap(video,12);
        add(video,text(source.name,15,ink,true),-1,-2);gap(video,5);add(video,text(source.width+" × "+source.height+" · "+duration(source.duration),12,gray,false),-1,-2);add(b,video,-1,-2);gap(b,12);
        LinearLayout estimate=block();LinearLayout sizes=row();LinearLayout before=col();add(before,text("Original",12,gray,false),-1,-2);gap(before,6);add(before,text(size(source.bytes),23,ink,true),-1,-2);weight(sizes,before);add(sizes,text("→",22,gray,false),-2,-2);gap(sizes,12);LinearLayout after=col();after.setGravity(Gravity.END);add(after,text("Estimated size",12,gray,false),-2,-2);gap(after,6);add(after,text("~"+size(preset.estimate(source)),23,blue,true),-2,-2);weight(sizes,after);add(estimate,sizes,-1,-2);gap(estimate,14);bar(estimate,preset.estimate(source),source.bytes,blue);gap(estimate,8);
        long potential=source.bytes-preset.estimate(source);add(estimate,centered(potential>0?"Estimated reduction: "+size(potential):"This preset may not reduce file size",12,potential>0?green:gray,true),-1,-2);gap(estimate,5);add(estimate,centered("Estimate only · actual size may vary",11,gray,false),-1,-2);add(b,estimate,-1,-2);gap(b,18);
        add(b,text("Compression quality",19,ink,true),-1,-2);gap(b,8);LinearLayout presets=block();presets.setPadding(dp(8),dp(8),dp(8),dp(8));
        for(VideoCompressionEngine.Preset p:VideoCompressionEngine.Preset.values()){LinearLayout option=row();option.setPadding(dp(12),dp(10),dp(12),dp(10));option.setBackground(shape(p==preset?pale:card,14));
            LinearLayout copy=col();add(copy,text(p.label,15,ink,true),-1,-2);gap(copy,4);add(copy,text(p.detail+" · up to "+p.targetHeight(source)+"p",12,gray,false),-1,-2);weight(option,copy);add(option,text(p==preset?"●":"○",22,p==preset?blue:gray,true),-2,-2);option.setFocusable(true);option.setClickable(true);option.setOnClickListener(v->{preset=p;showSettings();});add(presets,option,-1,-2);gap(presets,3);
        }add(b,presets,-1,-2);primary(l,"Compress video",this::startCompression);foot(l,"Your original video stays safe.");setContentView(l);
    }
    private void bar(LinearLayout parent,long value,long total,int color){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(1000);p.setProgress((int)Math.min(1000,Math.max(0,1000d*value/Math.max(1,total))));p.setProgressTintList(ColorStateList.valueOf(color));add(parent,p,-1,dp(8));}
    private void startCompression(){
        if(busy||source==null||destroyed)return;cancelThumbs();clearOutput();
        long estimate=preset.estimate(source),available=new StatFs(getCacheDir().getAbsolutePath()).getAvailableBytes();
        if(available<Math.max(32L*1024*1024,estimate+16L*1024*1024)){dialog().setTitle("Not enough free space").setMessage("Free up some storage or choose a smaller preset. The original is safe.").setPositiveButton("Done",null).show();return;}
        busy=true;encoding=true;phase=ENCODING;temp=new File(getCacheDir(),"clenova-compress-"+UUID.randomUUID()+".mp4");final int token=++epoch;final File target=temp;final VideoCompressionEngine.Source input=source;showProgress();
        try{
            transformer=VideoCompressionEngine.transformer(this,input,preset,new Transformer.Listener(){
                @Override public void onCompleted(Composition composition,ExportResult result){if(destroyed||token!=epoch)return;encoding=false;transformer=null;handler.removeCallbacks(progressTick);if(ring!=null)ring.setProgress(100);if(progressLabel!=null)progressLabel.setText("Checking compressed video…");
                    work=io.submit(()->{try{VideoCompressionEngine.Output verified=VideoCompressionEngine.validate(VideoCompressorActivity.this,input,target);runOnUiThread(()->{if(destroyed||token!=epoch){target.delete();return;}busy=false;output=verified;showResult();});}catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;failCompression("Output could not be verified. Try another quality.");});}});
                }
                @Override public void onError(Composition composition,ExportResult result,ExportException e){if(!destroyed&&token==epoch)failCompression("This video could not be compressed. Try a different quality or video.");}
            });
            transformer.start(VideoCompressionEngine.edited(input,preset),target.getAbsolutePath());handler.post(progressTick);
        }catch(Exception e){failCompression("Could not start video compression on this device.");}
    }
    private void showProgress(){
        LinearLayout l=root();header(l,"Compressing video",false);LinearLayout b=body(l);LinearLayout center=col();center.setGravity(Gravity.CENTER);gap(center,20);
        ring=new ProgressRing();add(center,ring,-1,dp(250));gap(center,12);add(center,centered("Making room.",26,ink,true),-1,-2);gap(center,8);progressLabel=centered("Preparing video…",14,gray,false);add(center,progressLabel,-1,-2);gap(center,7);add(center,centered("Your video is being compressed locally.",13,gray,false),-1,-2);add(b,center,-1,-2);gap(b,24);
        LinearLayout info=block();add(info,text(source.name,15,ink,true),-1,-2);gap(info,6);add(info,text(preset.label+" · "+size(source.bytes),13,gray,false),-1,-2);add(b,info,-1,-2);
        gap(l,10);add(l,button("Cancel",ink,card,this::confirmCancel),-1,dp(52));setContentView(l);
    }
    private final class ProgressRing extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private int progress=-1;
        ProgressRing(){super(VideoCompressorActivity.this);setContentDescription("Compression progress");}
        void setProgress(int v){progress=v;setContentDescription(v<0?"Preparing video":v+" percent");invalidate();}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.max(1,Math.min(cx,cy)-dp(18));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(10));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(dark?0xff38383c:0xffe2e5eb);canvas.drawCircle(cx,cy,r,p);p.setColor(blue);if(progress>=0)canvas.drawArc(cx-r,cy-r,cx+r,cy+r,-90,360f*progress/100f,false,p);else canvas.drawArc(cx-r,cy-r,cx+r,cy+r,-90,36,false,p);
            p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setColor(ink);p.setTextSize(dp(44));canvas.drawText(progress<0?"…":progress+"%",cx,cy+dp(15),p);p.setColor(gray);p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(13));canvas.drawText("Compressing",cx,cy-dp(35),p);}
    }
    private void confirmCancel(){if(!busy)return;dialog().setTitle("Cancel compression?").setMessage("The unfinished copy will be discarded. Your original stays safe.").setNegativeButton("Continue",null).setPositiveButton("Cancel compression",(d,w)->{cancelCompression();showSettings();}).show();}
    private void cancelCompression(){epoch++;handler.removeCallbacks(progressTick);if(transformer!=null){try{transformer.cancel();}catch(RuntimeException ignored){}transformer=null;}if(work!=null)work.cancel(true);busy=false;encoding=false;clearOutput();}
    private void failCompression(String message){cancelCompression();showSettings();dialog().setTitle("Compression unavailable").setMessage(message).setPositiveButton("Done",null).show();}
    private void showResult(){
        if(output==null){showSettings();return;}phase=RESULT;LinearLayout l=root();header(l,"Compression complete",false);LinearLayout b=body(l);LinearLayout hero=col();hero.setGravity(Gravity.CENTER);gap(hero,8);
        TextView mark=centered("✓",45,0xffffffff,true);mark.setBackground(shape(0xff34c759,52));add(hero,mark,dp(88),dp(88));gap(hero,15);add(hero,centered(saved==null?"All compressed!":"Compressed video saved",25,ink,true),-1,-2);gap(hero,6);add(hero,centered(saved==null?"Your video is ready to review.":"Your copy is available to open or share.",13,gray,false),-1,-2);add(b,hero,-1,-2);gap(b,20);
        long delta=source.bytes-output.meta.bytes;LinearLayout stats=block();add(stats,centered(delta>0?"Reduced file size by":"Output file size",13,gray,false),-1,-2);gap(stats,9);add(stats,centered(size(delta>0?delta:output.meta.bytes),36,delta>0?green:ink,true),-1,-2);gap(stats,5);add(stats,centered(delta>0?Math.round(100d*delta/source.bytes)+"% smaller":"Not smaller · try a different preset",13,delta>0?green:gray,false),-1,-2);gap(stats,14);rule(stats);gap(stats,12);sizeRow(stats,"Before",source.bytes,source.bytes,gray);gap(stats,12);sizeRow(stats,"After",output.meta.bytes,Math.max(source.bytes,output.meta.bytes),blue);gap(stats,9);add(stats,text(output.meta.width+" × "+output.meta.height+" · "+duration(output.meta.duration)+(output.meta.audio?" · Audio included":""),12,gray,false),-1,-2);add(b,stats,-1,-2);gap(b,14);
        if(saved==null){add(b,button("Preview compressed video",blue,card,()->play(Uri.fromFile(output.file),true)),-1,dp(48));gap(b,8);add(b,button("Try another quality",blue,card,()->{clearOutput();showSettings();}),-1,dp(48));primary(l,"Save compressed video",this::save);}
        else{LinearLayout actions=row();TextView preview=button("Preview",blue,card,()->play(saved,true));actions.addView(preview,new LinearLayout.LayoutParams(0,-1,1));gap(actions,8);actions.addView(button("Share",blue,card,this::share),new LinearLayout.LayoutParams(0,-1,1));add(b,actions,-1,dp(48));gap(b,12);if(savedLocal&&!sourceRemoved&&delta>0&&original!=null&&Build.VERSION.SDK_INT>=30&&!sourceVersion.isEmpty()&&!original.protectedItem){add(b,button("Remove original video",red,card,this::confirmOriginalRemoval),-1,dp(48));foot(b,"Only after reviewing the saved copy and Android approval.");}else foot(b,sourceRemoved?"Android approved the original trash request.":"Original kept. Removal may be unavailable for protected or picker-selected videos.");primary(l,"Done",()->{clearOutput();source=null;original=null;loadGallery();});}
        foot(l,saved==null?"Saving a separate copy uses additional space.":sourceRemoved?"Android Trash may still occupy storage.":"No storage recovery is claimed while the original remains.");setContentView(l);
    }
    private void sizeRow(LinearLayout l,String name,long bytes,long max,int tint){LinearLayout r=row();weight(r,text(name,13,gray,false));add(r,text(size(bytes),15,ink,true),-2,-2);add(l,r,-1,-2);gap(l,7);bar(l,bytes,max,tint);}
    private void save(){
        if(busy||output==null||saved!=null)return;
        if(Build.VERSION.SDK_INT>=29){saveTo(null);return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("video/mp4");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,VideoCompressionEngine.filename(source));try{startActivityForResult(i,SAVE);}catch(ActivityNotFoundException e){toast("No save destination available");}
    }
    private void saveTo(Uri destination){
        if(busy||output==null||saved!=null)return;busy=true;status("Saving and verifying compressed copy…");final int token=++epoch;final VideoCompressionEngine.Output result=output;final String filename=VideoCompressionEngine.filename(source);
        work=io.submit(()->{try{Uri uri;if(destination==null)uri=VideoCompressionEngine.saveGallery(this,result,filename);else{VideoCompressionEngine.verifyNewDestination(this,source.uri,destination);VideoCompressionEngine.copy(this,result,destination);uri=destination;}
            long before=source.bytes;boolean logged=history.add(null,"compression","Video compressed","Saved copy · "+size(before)+" → "+size(result.meta.bytes),0,1,false);
            runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;saved=uri;savedLocal=destination==null;outputReviewed=false;showResult();if(!logged)toast("Video saved, but history could not be recorded");});
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;showResult();dialog().setTitle("Could not save video").setMessage(destination==null?"The copy could not be published. Your original is safe.":"The selected provider could not verify the copy. It may contain an incomplete file; check the destination. Your original is safe.").setPositiveButton("Done",null).show();});}});
    }
    private void play(Uri uri,boolean compressed){
        stopPlayer();LinearLayout box=col();box.setPadding(dp(10),dp(8),dp(10),dp(8));VideoView v=new VideoView(this);player=v;add(box,v,-1,dp(Math.max(140,Math.min(330,getResources().getConfiguration().screenHeightDp-200))));
        TextView message=text("Loading video…",12,gray,false);add(box,message,-1,-2);MediaController controller=new MediaController(this);controller.setAnchorView(v);v.setMediaController(controller);
        previewDialog=dialog().setTitle(compressed?"Compressed preview":"Original preview").setView(box).setPositiveButton("Done",null).create();previewDialog.setOnDismissListener(d->{v.stopPlayback();if(player==v)player=null;previewDialog=null;});
        previewDialog.show();v.setOnPreparedListener(mp->{message.setText("Use playback controls to review");v.start();});v.setOnCompletionListener(mp->{if(compressed)outputReviewed=true;});v.setOnErrorListener((mp,a,b)->{message.setText("Playback unavailable on this device");return true;});
        if("file".equals(uri.getScheme()))v.setVideoPath(uri.getPath());else v.setVideoURI(uri);
    }
    private void stopPlayer(){if(previewDialog!=null){Dialog d=previewDialog;previewDialog=null;d.dismiss();}if(player!=null){player.stopPlayback();player=null;}}
    private void share(){if(saved==null){toast("Save the video first");return;}Intent i=new Intent(Intent.ACTION_SEND);i.setType("video/mp4");i.putExtra(Intent.EXTRA_STREAM,saved);i.setClipData(ClipData.newRawUri("Compressed video",saved));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(Intent.createChooser(i,"Share compressed video"));}catch(ActivityNotFoundException e){toast("No sharing app available");}}
    private void confirmOriginalRemoval(){
        if(busy||saved==null||!savedLocal||original==null||output==null||output.meta.bytes>=source.bytes||sourceRemoved||Build.VERSION.SDK_INT<30)return;
        if(!outputReviewed){dialog().setTitle("Review the saved video first").setMessage("Play the compressed video through to the end before removing its original.").setNegativeButton("Keep original",null).setPositiveButton("Preview", (d,w)->play(saved,true)).show();return;}
        dialog().setTitle("Move original to Trash?").setMessage("Make sure you are happy with the compressed copy. Protected or changed originals cannot be removed. Android will ask for separate approval.").setNegativeButton("Keep original",null).setPositiveButton("Continue",(d,w)->checkOriginal()).show();
    }
    private void checkOriginal(){
        if(Build.VERSION.SDK_INT<30){toast("Android Trash requires Android 11 or later. Original kept.");return;}
        busy=true;status("Checking saved copy and original…");final int token=++epoch;final SmartMediaScan.Item old=original;final VideoCompressionEngine.Output result=output;final Uri copy=saved;final String version=sourceVersion;
        work=io.submit(()->{try{
            if(Build.VERSION.SDK_INT<30||!VideoCompressionEngine.matches(this,copy,result)||!SmartMediaScan.hasVideoAccess(this)||old==null||old.volume==null||version.isEmpty()||!version.equals(MediaStore.getVersion(this,old.volume)))throw new IOException("Copy or source changed");
            SmartMediaScan fresh=new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this));SmartMediaScan.Item match=null;for(SmartMediaScan.Item item:fresh.videoGallery().items)if(item.uri.equals(old.uri)){match=item;break;}
            if(!SmartMediaScan.sameGalleryItem(old,match)||!version.equals(MediaStore.getVersion(this,old.volume))||old.bytes!=source.bytes)throw new IOException("Original changed");
            runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;if(Build.VERSION.SDK_INT<30||!foreground||!SmartMediaScan.hasVideoAccess(this)){showResult();toast("Original kept");return;}try{PendingIntent intent=MediaStore.createTrashRequest(getContentResolver(),Collections.singletonList(old.uri),true);cleanupReceipt=CleanupReceipt.capture(this,"Compressed video original",Collections.singletonList(old.uri),Collections.singletonList(old));trashPending=true;startIntentSenderForResult(intent.getIntentSender(),TRASH,null,0,0,0);}catch(Exception e){trashPending=false;cleanupReceipt=null;showResult();toast("Could not request Android Trash approval");}});
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;showResult();toast("Original kept: copy, access or protection changed.");});}});
    }
    private void showHistory(){
        if(busy)return;phase=HISTORY;cancelThumbs();LinearLayout l=root();header(l,"Compression history",false);LinearLayout b=body(l);int count=0;
        for(ActivityHistory.Entry entry:history.entries())if("compression".equals(entry.category)){LinearLayout r=block();add(r,text(entry.title,16,ink,true),-1,-2);gap(r,6);add(r,text(entry.detail,13,gray,false),-1,-2);gap(r,5);add(r,text(android.text.format.DateFormat.getMediumDateFormat(this).format(new Date(entry.timestamp)),12,gray,false),-1,-2);add(b,r,-1,-2);gap(b,10);count++;}
        if(count==0){LinearLayout empty=block();add(empty,centered("No compressed videos yet",18,ink,true),-1,-2);gap(empty,8);add(empty,centered("Saved copies will appear in your history.",13,gray,false),-1,-2);add(b,empty,-1,-2);}foot(l,"History records saved copies, not reclaimed storage.");setContentView(l);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==ACCESS){accessReturned();return;}
        if(request==PICK&&result==RESULT_OK&&data!=null&&data.getData()!=null){select(data.getData(),null);return;}
        if(request==SAVE&&result==RESULT_OK&&data!=null&&data.getData()!=null){saveTo(data.getData());return;}
        if(request==TRASH){final CleanupReceipt receipt=cleanupReceipt;cleanupReceipt=null;trashPending=false;busy=true;status("Checking cleanup result…");
            CleanupReceipt.verifyAsync(this,receipt,result==RESULT_OK,verified->{busy=false;sourceRemoved=verified.complete();toast(verified.detail()+(verified.logged?"":" History could not be saved."));if(output!=null)showResult();else loadGallery();});}
    }
    private void clearOutput(){stopPlayer();if(temp!=null){temp.delete();temp=null;}output=null;saved=null;savedLocal=false;sourceRemoved=false;outputReviewed=false;}
    @Override public void onBackPressed(){
        if(trashPending)return;if(busy){if(phase==ENCODING)confirmCancel();else toast("Current operation is finishing");return;}
        if(phase==HISTORY){showGallery();return;}if(phase==SETTINGS){source=null;original=null;clearOutput();loadGallery();return;}
        if(phase==RESULT){if(saved!=null){clearOutput();source=null;loadGallery();}else dialog().setTitle("Discard unsaved copy?").setMessage("The original video is unchanged.").setNegativeButton("Stay",null).setPositiveButton("Discard",(d,w)->{clearOutput();showSettings();}).show();return;}finish();
    }
    @Override protected void onResume(){super.onResume();foreground=true;
        if(refreshGallery&&!accessPending&&!busy&&!trashPending&&phase==GALLERY){refreshGallery=false;loadGallery();}
    }
    @Override protected void onPause(){foreground=false;super.onPause();}
    @Override protected void onStop(){super.onStop();stopPlayer();if(phase==ENCODING&&busy){cancelCompression();if(!isFinishing()){showSettings();toast("Compression cancelled when the app left the foreground");}}if(phase==GALLERY&&!busy&&!accessPending&&!trashPending){cancelThumbs();if(galleryJob!=null){galleryJob.cancel();galleryJob=null;}epoch++;loadingGallery=false;videos.clear();galleryVersions.clear();refreshGallery=true;}}
    @Override protected void onDestroy(){destroyed=true;epoch++;cancelThumbs();handler.removeCallbacks(progressTick);if(galleryJob!=null)galleryJob.cancel();if(transformer!=null)try{transformer.cancel();}catch(RuntimeException ignored){}if(work!=null)work.cancel(true);io.shutdownNow();thumbs.shutdownNow();clearOutput();super.onDestroy();}
}

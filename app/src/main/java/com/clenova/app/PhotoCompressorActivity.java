package com.clenova.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Four-screen photo compressor. Selection is explicit and limited to 1–100. */
public final class PhotoCompressorActivity extends Activity {
    private static final int ACCESS=221,PICK=222,FOLDER=223,TRASH=224,PAGE=60,MAX=PhotoCompressionEngine.MAX_SELECTION;
    private static final int GALLERY=0,SETTINGS=1,ENCODING=2,RESULT=3,HISTORY=4;
    private final ExecutorService io=Executors.newSingleThreadExecutor(), thumbnails=Executors.newSingleThreadExecutor();
    private final List<Future<?>> thumbJobs=new ArrayList<>();
    private final List<PhotoCompressionEngine.Source> gallery=new ArrayList<>();
    private final LinkedHashMap<String,PhotoCompressionEngine.Source> selected=new LinkedHashMap<>();
    private final List<PhotoCompressionEngine.Output> outputs=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private final List<Uri> pendingTrash=new ArrayList<>();
    private final Map<String,TextView> marks=new HashMap<>();
    private final Map<String,String> months=new HashMap<>();
    private List<PhotoCompressionEngine.Source> currentFilter=Collections.emptyList();
    private AppPreferences prefs;private ActivityHistory history;private CleanupReceipt cleanupReceipt;
    private PhotoCompressionEngine.Preset preset=PhotoCompressionEngine.Preset.BALANCED;
    private PhotoCompressionEngine.Format format=PhotoCompressionEngine.Format.ORIGINAL;
    private SmartMediaScan scanner;private Future<?> task;
    private int bg,card,ink,gray,blue,pale,green,red,phase=GALLERY,page,sort,epoch,thumbEpoch,batchCount,completedCount;
    private boolean dark,destroyed,foreground,busy,loading,limited,trashPending,accessPending,refreshGallery;
    private String month="All photos",error="";
    private TextView countLabel,sizeLabel,nextButton,progressTitle,progressDetail,selectAllButton;
    private LinearLayout progressRows;
    private ProgressBar progressBar;
    private Ring ring;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=new AppPreferences(this);history=new ActivityHistory(this);
        dark=prefs.appearance().equals("Dark")||(prefs.appearance().equals("System")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=dark?0xff141416:0xfff5f5f7;card=dark?0xff242426:0xffffffff;ink=dark?0xfff4f4f6:0xff111111;gray=dark?0xffa2a2aa:0xff86868b;blue=dark?0xff409cff:0xff007aff;pale=dark?0xff19334c:0xffe6f0ff;green=dark?0xff61d883:0xff249d42;red=dark?0xffff6961:0xffe43e35;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27&&!dark)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        File[] stale=getCacheDir().listFiles((d,n)->n.startsWith("clenova-photo-"));
        if(stale!=null)for(File f:stale)if(System.currentTimeMillis()-f.lastModified()>24L*60*60*1000)f.delete();
        trashPending=state!=null&&state.getBoolean("trash_pending");cleanupReceipt=CleanupReceipt.restore(state==null?null:state.getBundle("cleanup_receipt"));
        if(trashPending){status("Waiting for Android approval");return;}loadGallery();
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
    private TextView text(String s,int size,int tint,boolean bold){TextView t=new TextView(this);t.setText(tr(s));t.setTextSize(size);t.setTextColor(tint);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private TextView center(String s,int size,int tint,boolean bold){TextView t=text(s,size,tint,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView button(String s,int tint,int fill,Runnable action){TextView t=center(s,14,tint,true);t.setBackground(shape(fill,26));t.setPadding(dp(10),dp(7),dp(10),dp(7));t.setFocusable(true);t.setClickable(true);t.setOnClickListener(v->{if(!destroyed)action.run();});return t;}
    private LinearLayout block(){LinearLayout l=col();l.setBackground(shape(card,21));l.setPadding(dp(14),dp(13),dp(14),dp(13));return l;}
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(16),dp(10),dp(16),dp(10));return l;}
    private LinearLayout body(LinearLayout l){ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout b=col();scroll.addView(b,new ScrollView.LayoutParams(-1,-2));l.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return b;}
    private void header(LinearLayout l,String title,boolean past){LinearLayout h=row();TextView back=button("‹",ink,card,this::onBackPressed);back.setTextSize(27);back.setContentDescription("Back");add(h,back,dp(44),dp(44));gap(h,8);weight(h,center(title,19,ink,true));gap(h,8);
        if(past){TextView b=button("◷",ink,card,this::showHistory);b.setTextSize(24);b.setContentDescription("Photo compression history");add(h,b,dp(44),dp(44));}else add(h,new View(this),dp(44),dp(44));add(l,h,-1,dp(52));gap(l,10);}
    private void primary(LinearLayout l,String s,Runnable r){gap(l,10);add(l,button(s,0xffffffff,blue,r),-1,dp(51));}
    private void foot(LinearLayout l,String s){gap(l,8);add(l,center(s,11,gray,false),-1,-2);}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
    private void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_SHORT).show();}
    private String size(long b){return b>=1073741824L?String.format(Locale.getDefault(),"%.2f GB",b/1073741824d):b>=1048576?String.format(Locale.getDefault(),"%.1f MB",b/1048576d):b>=1024?String.format(Locale.getDefault(),"%.0f KB",b/1024d):b+" B";}
    private long total(){long sum=0;for(PhotoCompressionEngine.Source s:selected.values())sum+=s.bytes;return sum;}
    private String key(PhotoCompressionEngine.Source s){return s.uri.toString();}
    private String monthOf(PhotoCompressionEngine.Source s){return months.computeIfAbsent(key(s),k->s.original==null?"Picked files":s.original.modified<=0?"Unknown date":new java.text.SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(new Date(s.original.modified)));}
    private void status(String msg){LinearLayout l=root();header(l,"Photo Compressor",false);LinearLayout c=col();c.setGravity(Gravity.CENTER);ProgressBar p=new ProgressBar(this);add(c,p,dp(42),dp(42));gap(c,15);add(c,center(msg,14,gray,false),-1,-2);l.addView(c,new LinearLayout.LayoutParams(-1,0,1));setContentView(l);}
    private void cancelThumbs(){thumbEpoch++;for(Future<?> f:thumbJobs)f.cancel(true);thumbJobs.clear();}
    private void thumbnail(ImageView view,Uri uri,int max){final int token=thumbEpoch;
        thumbJobs.add(thumbnails.submit(()->{Bitmap b=null;try{
            if(Build.VERSION.SDK_INT>=29)b=getContentResolver().loadThumbnail(uri,new android.util.Size(max,max),null);
            else {BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>max*2)o.inSampleSize*=2;o.inJustDecodeBounds=false;try(InputStream in=getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(in,null,o);}}
        }catch(Exception|OutOfMemoryError ignored){}final Bitmap result=b;runOnUiThread(()->{if(destroyed||token!=thumbEpoch||!view.isAttachedToWindow()){if(result!=null)result.recycle();return;}if(result!=null)view.setImageBitmap(result);});}));
    }
    private void loadGallery(){
        if(destroyed||busy)return;epoch++;cancelThumbs();if(scanner!=null)scanner.cancel();gallery.clear();months.clear();page=0;phase=GALLERY;error="";
        if(!SmartMediaScan.hasPhotoAccess(this)){selected.entrySet().removeIf(e->e.getValue().original!=null);loading=false;limited=false;showGallery();return;}
        loading=true;showGallery();final int token=++epoch;scanner=new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this));final SmartMediaScan scan=scanner;
        task=io.submit(()->{try{
            Map<String,String> versions=new HashMap<>();if(Build.VERSION.SDK_INT>=30)for(String volume:MediaStore.getExternalVolumeNames(this))try{versions.put(volume,MediaStore.getVersion(this,volume));}catch(RuntimeException ignored){}
            SmartMediaScan.Gallery found=scan.gallery();if(Build.VERSION.SDK_INT>=30)for(String v:new ArrayList<>(versions.keySet()))if(!versions.get(v).equals(MediaStore.getVersion(this,v)))versions.remove(v);
            List<PhotoCompressionEngine.Source> result=new ArrayList<>();for(SmartMediaScan.Item i:found.items)result.add(new PhotoCompressionEngine.Source(i.uri,i.name,i.mime,i.bytes,i,i.volume==null?"":versions.getOrDefault(i.volume,"")));
            runOnUiThread(()->{if(destroyed||token!=epoch)return;scanner=null;loading=false;gallery.addAll(result);limited=found.partial;
                Map<String,PhotoCompressionEngine.Source> current=new HashMap<>();for(PhotoCompressionEngine.Source s:gallery)current.put(key(s),s);
                // Refresh removes stale gallery selections; explicit picker grants remain separate.
                selected.entrySet().removeIf(e->e.getValue().original!=null&&!current.containsKey(e.getKey()));
                for(String k:new ArrayList<>(selected.keySet()))if(selected.get(k).original!=null){PhotoCompressionEngine.Source old=selected.get(k),now=current.get(k);if(now==null||old.bytes!=now.bytes||old.original.modified!=now.original.modified||old.original.generation!=now.original.generation||!old.version.equals(now.version))selected.remove(k);else selected.put(k,now);}
                if(phase==GALLERY)showGallery();});
        }catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;scanner=null;loading=false;error="Could not load photos. Browse files or refresh.";if(phase==GALLERY)showGallery();});}});
    }
    private List<PhotoCompressionEngine.Source> visible(){
        LinkedHashMap<String,PhotoCompressionEngine.Source> map=new LinkedHashMap<>();for(PhotoCompressionEngine.Source s:gallery)map.put(key(s),s);for(PhotoCompressionEngine.Source s:selected.values())map.put(key(s),s);
        List<PhotoCompressionEngine.Source> list=new ArrayList<>();for(PhotoCompressionEngine.Source s:map.values())if(month.equals("All photos")||month.equals(monthOf(s)))list.add(s);
        Comparator<PhotoCompressionEngine.Source> c=sort==0?Comparator.comparingLong((PhotoCompressionEngine.Source s)->s.original==null?Long.MAX_VALUE:s.original.modified).reversed():Comparator.comparingLong((PhotoCompressionEngine.Source s)->s.bytes).reversed();
        list.sort(c.thenComparing(this::key));return list;
    }
    private void access(){
        if(accessPending||busy)return;if(ToolAccess.has(this,ToolAccess.PHOTOS)){loadGallery();return;}
        accessPending=true;if(!ToolAccess.launch(this,ToolAccess.PHOTOS,ACCESS)){accessPending=false;toast("Could not open access settings");}
    }
    private void accessReturned(){accessPending=false;refreshGallery=false;loadGallery();if(!ToolAccess.has(this,ToolAccess.PHOTOS))toast("Access not granted. Browse files instead.");}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==ACCESS)accessReturned();}
    private void pick(){if(busy)return;if(selected.size()>=MAX){toast("Maximum 100 photos");return;}Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){toast("No file picker available");}}
    private void importPhotos(Intent data){
        LinkedHashSet<Uri> uris=new LinkedHashSet<>();if(data.getData()!=null)uris.add(data.getData());ClipData clips=data.getClipData();if(clips!=null)for(int i=0;i<clips.getItemCount();i++){Uri u=clips.getItemAt(i).getUri();if(u!=null)uris.add(u);}
        if(uris.isEmpty())return;if(scanner!=null){scanner.cancel();scanner=null;loading=false;}busy=true;status("Reading selected photos…");final int token=++epoch;final int room=MAX-selected.size();final Set<String> existing=new HashSet<>(selected.keySet());
        task=io.submit(()->{List<PhotoCompressionEngine.Source> added=new ArrayList<>();int failed=0,overflow=0;for(Uri u:uris){if(Thread.currentThread().isInterrupted())return;if(existing.contains(u.toString()))continue;if(added.size()>=room){overflow++;continue;}try{added.add(PhotoCompressionEngine.inspect(this,u));}catch(Exception e){failed++;}}
            final int bad=failed,extra=overflow;runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;for(PhotoCompressionEngine.Source s:added)selected.put(key(s),s);month="All photos";page=0;showGallery();if(bad>0||extra>0)toast((bad>0?bad+" unreadable. ":"")+(extra>0?"Maximum 100 photos; "+extra+" not added.":""));});});
    }
    private void showGallery(){
        if(destroyed)return;phase=GALLERY;cancelThumbs();marks.clear();LinearLayout l=root();header(l,"Photo Compressor",true);LinearLayout b=body(l);
        add(b,text("Less space. More memories.",25,ink,true),-1,-2);gap(b,5);add(b,text("Choose 1 to 100 photos to compress.",13,gray,false),-1,-2);gap(b,16);
        if(!SmartMediaScan.hasPhotoAccess(this)){LinearLayout notice=block();add(notice,text("Allow photo access",16,ink,true),-1,-2);gap(notice,6);add(notice,text("Choose from your gallery or browse selected files.",13,gray,false),-1,-2);primary(notice,ToolAccess.settingsNeeded(this,ToolAccess.PHOTOS)?"Open Android settings":"Allow photo access",this::access);add(b,notice,-1,-2);gap(b,10);}
        if(loading){add(b,center("Loading your photos…",13,gray,false),-1,dp(42));}
        if(limited){add(b,text("Limited photo access or gallery coverage",12,gray,false),-1,-2);if(Build.VERSION.SDK_INT>=34){gap(b,5);add(b,button("Manage photo access",blue,pale,this::access),-1,dp(40));}gap(b,8);}
        if(!error.isEmpty()){add(b,text(error,12,red,false),-1,-2);gap(b,8);}
        LinearLayout filters=row();TextView monthButton=button(month+" ▾",ink,card,this::chooseMonth);weight(filters,monthButton);gap(filters,6);TextView sortButton=button(sort==0?"Newest":"Largest",blue,card,()->{sort=1-sort;page=0;showGallery();});add(filters,sortButton,-2,dp(42));add(b,filters,-1,dp(42));gap(b,8);
        List<PhotoCompressionEngine.Source> list=visible();currentFilter=list;page=Math.min(page,Math.max(0,(list.size()-1)/PAGE));int start=page*PAGE,end=Math.min(list.size(),start+PAGE);
        LinearLayout section=row();weight(section,text(list.size()+" photos",13,gray,false));selectAllButton=button("Select all",blue,card,()->selectVisible(list));add(section,selectAllButton,-2,dp(38));add(b,section,-1,-2);gap(b,8);
        if(list.isEmpty()&&!loading){LinearLayout empty=block();add(empty,center("No photos available",16,ink,true),-1,dp(42));add(empty,center("Browse files or refresh your gallery.",12,gray,false),-1,dp(34));add(b,empty,-1,-2);}
        int tileHeight=Math.max(76,Math.min(145,(getResources().getConfiguration().screenWidthDp-52)/3));
        for(int n=start;n<end;n+=3){LinearLayout line=row();line.setGravity(Gravity.TOP);for(int k=0;k<3;k++){if(k>0)gap(line,7);int index=n+k;
            if(index>=end){line.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));continue;}
            PhotoCompressionEngine.Source s=list.get(index);LinearLayout tile=col();tile.setBackground(shape(card,12));tile.setClipToOutline(true);
            FrameLayout imageBox=new FrameLayout(this);imageBox.setBackground(shape(pale,10));ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);imageBox.addView(image,new FrameLayout.LayoutParams(-1,-1));
            TextView check=center("",18,0xffffffff,true);FrameLayout.LayoutParams checkParams=new FrameLayout.LayoutParams(dp(27),dp(27),Gravity.TOP|Gravity.END);checkParams.topMargin=dp(6);checkParams.rightMargin=dp(6);imageBox.addView(check,checkParams);marks.put(key(s),check);updateMark(s,check);
            add(tile,imageBox,-1,dp(tileHeight));add(tile,center(size(s.bytes),11,ink,true),-1,dp(28));tile.setContentDescription(s.name+", "+size(s.bytes));tile.setClickable(true);tile.setFocusable(true);tile.setOnClickListener(v->toggle(s));line.addView(tile,new LinearLayout.LayoutParams(0,-2,1));thumbnail(image,s.uri,300);
        }add(b,line,-1,-2);gap(b,7);}
        if(list.size()>PAGE){LinearLayout p=row();TextView prev=button("Previous",blue,card,()->{if(page>0){page--;showGallery();}});prev.setEnabled(page>0);add(p,prev,-2,dp(42));weight(p,center((page+1)+" / "+((list.size()+PAGE-1)/PAGE),12,gray,false));TextView next=button("Next",blue,card,()->{if((page+1)*PAGE<list.size()){page++;showGallery();}});next.setEnabled((page+1)*PAGE<list.size());add(p,next,-2,dp(42));add(b,p,-1,-2);gap(b,8);}
        LinearLayout links=row();weight(links,button("Browse files",blue,pale,this::pick));gap(links,8);weight(links,button("Refresh",blue,card,this::loadGallery));add(b,links,-1,-2);gap(b,10);
        LinearLayout bottom=block();LinearLayout summary=row();LinearLayout counts=col();countLabel=text("",15,ink,true);sizeLabel=text("",12,gray,false);add(counts,countLabel,-1,-2);gap(counts,4);add(counts,sizeLabel,-1,-2);weight(summary,counts);add(summary,button("Clear",blue,card,()->{selected.clear();showGallery();}),-2,dp(38));add(bottom,summary,-1,-2);gap(bottom,9);nextButton=button("Continue →",0xffffffff,blue,this::showSettings);add(bottom,nextButton,-1,dp(49));add(l,bottom,-1,-2);foot(l,"Photos stay on your device during compression.");setContentView(l);refreshSelection();
    }
    private void chooseMonth(){LinkedHashSet<String> all=new LinkedHashSet<>();all.add("All photos");for(PhotoCompressionEngine.Source s:gallery)all.add(monthOf(s));for(PhotoCompressionEngine.Source s:selected.values())all.add(monthOf(s));String[] a=all.toArray(new String[0]);dialog().setTitle("Choose album or month").setSingleChoiceItems(a,Arrays.asList(a).indexOf(month),(d,w)->{month=a[w];page=0;d.dismiss();showGallery();}).setNegativeButton("Cancel",null).show();}
    private void updateMark(PhotoCompressionEngine.Source s,TextView t){boolean on=selected.containsKey(key(s));t.setText(on?"✓":"○");t.setBackground(shape(on?blue:0x88000000,15));t.setContentDescription(on?"Selected":"Not selected");if(t.getParent() instanceof View){View container=(View)t.getParent();if(container.getParent() instanceof View)((View)container.getParent()).setSelected(on);}}
    private void toggle(PhotoCompressionEngine.Source s){String k=key(s);if(selected.containsKey(k))selected.remove(k);else{if(selected.size()>=MAX){toast("Maximum 100 photos");return;}selected.put(k,s);}TextView t=marks.get(k);if(t!=null){updateMark(s,t);t.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}refreshSelection();}
    private void selectVisible(List<PhotoCompressionEngine.Source> list){if(list.isEmpty())return;boolean all=true;for(PhotoCompressionEngine.Source s:list)if(!selected.containsKey(key(s))){all=false;break;}if(all){for(PhotoCompressionEngine.Source s:list)selected.remove(key(s));}else{for(PhotoCompressionEngine.Source s:list){if(selected.size()>=MAX)break;selected.putIfAbsent(key(s),s);}if(list.size()>MAX||selected.size()==MAX)toast("Up to 100 photos selected");}showGallery();}
    private void refreshSelection(){int n=selected.size();countLabel.setText(n+" / "+MAX+" photos selected");sizeLabel.setText(size(total())+" total");nextButton.setEnabled(n>=1&&n<=MAX&&!busy);nextButton.setAlpha(n>=1&&n<=MAX?1f:.45f);if(selectAllButton!=null){boolean all=!currentFilter.isEmpty();for(PhotoCompressionEngine.Source item:currentFilter)if(!selected.containsKey(key(item))){all=false;break;}selectAllButton.setText(all?"Deselect all":"Select all");selectAllButton.setEnabled(!currentFilter.isEmpty());}}
    private long estimated(){double ratio=preset==PhotoCompressionEngine.Preset.HIGH?.70:preset==PhotoCompressionEngine.Preset.BALANCED?.42:.23;return Math.max(1,Math.round(total()*ratio));}
    private void bar(LinearLayout l,long value,long total,int tint){ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(1000);p.setProgress((int)Math.min(1000,Math.max(0,1000d*value/Math.max(1,total))));p.setProgressTintList(ColorStateList.valueOf(tint));add(l,p,-1,dp(8));}
    private void showSettings(){
        if(busy||selected.isEmpty()){if(!busy)showGallery();return;}phase=SETTINGS;cancelThumbs();LinearLayout l=root();header(l,"Compress photos",false);LinearLayout b=body(l);
        LinearLayout preview=block();LinearLayout strip=row();int i=0;for(PhotoCompressionEngine.Source s:selected.values()){if(i++==3)break;if(i>1)gap(strip,6);ImageView im=new ImageView(this);im.setBackground(shape(pale,12));im.setClipToOutline(true);im.setScaleType(ImageView.ScaleType.CENTER_CROP);strip.addView(im,new LinearLayout.LayoutParams(0,dp(88),1));thumbnail(im,s.uri,300);}add(preview,strip,-1,-2);gap(preview,10);LinearLayout label=row();weight(label,text(selected.size()+" photos selected",14,ink,true));add(label,button("Edit",blue,card,this::showGallery),-2,dp(36));add(preview,label,-1,-2);add(b,preview,-1,-2);gap(b,11);
        LinearLayout estimate=block();LinearLayout sizes=row();LinearLayout left=col();add(left,text("Original",12,gray,false),-1,-2);gap(left,5);add(left,text(size(total()),23,ink,true),-1,-2);weight(sizes,left);add(sizes,text("→",22,gray,false),-2,-2);gap(sizes,9);LinearLayout right=col();right.setGravity(Gravity.END);add(right,text("Estimated size",12,gray,false),-2,-2);gap(right,5);add(right,text("~"+size(estimated()),23,blue,true),-2,-2);weight(sizes,right);add(estimate,sizes,-1,-2);gap(estimate,12);bar(estimate,estimated(),total(),blue);gap(estimate,8);add(estimate,center("Approximate · actual size depends on format and content",11,gray,false),-1,-2);add(b,estimate,-1,-2);gap(b,16);
        add(b,text("Compression quality",18,ink,true),-1,-2);gap(b,8);LinearLayout options=block();options.setPadding(dp(7),dp(7),dp(7),dp(7));
        for(PhotoCompressionEngine.Preset p:PhotoCompressionEngine.Preset.values()){LinearLayout option=row();option.setBackground(shape(preset==p?pale:card,14));option.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout copy=col();add(copy,text(p.label,15,ink,true),-1,-2);gap(copy,4);add(copy,text(p.detail+" · up to "+p.edge+" px",12,gray,false),-1,-2);weight(option,copy);add(option,text(preset==p?"●":"○",22,preset==p?blue:gray,true),-2,-2);option.setClickable(true);option.setFocusable(true);option.setOnClickListener(v->{preset=p;showSettings();});add(options,option,-1,-2);gap(options,3);}add(b,options,-1,-2);gap(b,11);
        LinearLayout formats=block();add(formats,text("Output format",14,ink,true),-1,-2);gap(formats,9);LinearLayout tabs=row();for(PhotoCompressionEngine.Format f:PhotoCompressionEngine.Format.values()){TextView tab=button(f.label,format==f?blue:gray,format==f?pale:bg,()->{format=f;showSettings();});tabs.addView(tab,new LinearLayout.LayoutParams(0,dp(42),1));gap(tabs,3);}add(formats,tabs,-1,-2);gap(formats,7);
        add(formats,text(format==PhotoCompressionEngine.Format.ORIGINAL?"JPEG, PNG and WebP retained. PNG uses lossless encoding.":format==PhotoCompressionEngine.Format.JPEG?"Transparent areas become white.":"WebP can retain transparency.",11,gray,false),-1,-2);gap(formats,5);add(formats,text("Copies are re-encoded; original metadata is not retained. Animated images are unsupported.",11,gray,false),-1,-2);add(b,formats,-1,-2);
        primary(l,"Compress "+selected.size()+" photos",this::startCompression);foot(l,"Original photos stay safe.");setContentView(l);
    }
    private void startCompression(){
        if(busy||selected.size()<1||selected.size()>MAX)return;cancelThumbs();clearOutputs();batchCount=selected.size();completedCount=0;busy=true;phase=ENCODING;final int token=++epoch;List<PhotoCompressionEngine.Source> input=new ArrayList<>(selected.values());PhotoCompressionEngine.Preset q=preset;PhotoCompressionEngine.Format f=format;showProgress();
        task=io.submit(()->{
            for(int index=0;index<input.size();index++){if(Thread.currentThread().isInterrupted())return;PhotoCompressionEngine.Source s=input.get(index);final int pos=index;
                runOnUiThread(()->{if(!destroyed&&token==epoch)updateProgress(pos,"Compressing "+s.name,null);});
                PhotoCompressionEngine.Output out=null;String errorText=null;
                try{if(new StatFs(getCacheDir().getAbsolutePath()).getAvailableBytes()<16L*1024*1024)throw new IOException("Not enough free space");out=PhotoCompressionEngine.compress(this,s,q,f);}
                catch(Exception|OutOfMemoryError e){errorText=e instanceof IOException&&e.getMessage()!=null?e.getMessage():"Unsupported or unreadable photo";}
                final PhotoCompressionEngine.Output result=out;final String failure=errorText;
                if(Thread.currentThread().isInterrupted()){if(result!=null)result.file.delete();return;}
                runOnUiThread(()->{if(destroyed||token!=epoch){if(result!=null)result.file.delete();return;}if(result!=null)outputs.add(result);else failures.add(s.name+" — "+failure);completedCount=pos+1;updateProgress(completedCount,s.name,result==null?"Skipped · "+failure:size(s.bytes)+" → "+size(result.bytes));});
            }
            runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;showResult();});
        });
    }
    private void showProgress(){LinearLayout l=root();header(l,"Compressing photos",false);LinearLayout b=body(l);ring=new Ring();add(b,ring,-1,dp(220));gap(b,9);add(b,center("Making memories lighter.",24,ink,true),-1,-2);gap(b,7);progressTitle=center("Preparing photos…",14,gray,false);progressDetail=center("0 of "+batchCount+" photos",12,gray,false);add(b,progressTitle,-1,-2);gap(b,4);add(b,progressDetail,-1,-2);gap(b,12);
        progressBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progressBar.setMax(batchCount);progressBar.setProgressTintList(ColorStateList.valueOf(blue));add(b,progressBar,-1,dp(6));gap(b,14);progressRows=block();add(b,progressRows,-1,-2);gap(l,8);add(l,button("Cancel",ink,card,this::confirmCancel),-1,dp(50));foot(l,"Processing privately on your device.");setContentView(l);}
    private void updateProgress(int done,String title,String detail){if(phase!=ENCODING)return;progressTitle.setText(detail==null?title:"Processed "+done+" of "+batchCount);progressDetail.setText(done+" of "+batchCount+" photos");progressBar.setProgress(done);ring.set(done,batchCount);
        if(detail!=null){LinearLayout line=col();add(line,text(title,13,ink,true),-1,-2);gap(line,4);add(line,text(detail,12,detail.startsWith("Skipped")?red:green,false),-1,-2);gap(line,10);progressRows.addView(line,0);while(progressRows.getChildCount()>5)progressRows.removeViewAt(progressRows.getChildCount()-1);}}
    private final class Ring extends View{private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int done=0,total=1;Ring(){super(PhotoCompressorActivity.this);}void set(int d,int t){done=d;total=Math.max(1,t);setContentDescription(done+" of "+total+" processed");invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.max(1,Math.min(x,y)-dp(15));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(10));p.setStrokeCap(Paint.Cap.ROUND);p.setColor(dark?0xff38383c:0xffe2e5eb);c.drawCircle(x,y,r,p);p.setColor(blue);c.drawArc(x-r,y-r,x+r,y+r,-90,360f*done/total,false,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(43));p.setColor(ink);c.drawText(Math.round(100f*done/total)+"%",x,y+dp(14),p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(dp(12));p.setColor(gray);c.drawText("Optimizing",x,y-dp(32),p);}}
    private void confirmCancel(){dialog().setTitle("Cancel compression?").setMessage("Unsaved copies will be discarded. Original photos stay unchanged.").setNegativeButton("Continue",null).setPositiveButton("Cancel compression",(d,w)->{cancelBatch();showSettings();}).show();}
    private void cancelBatch(){epoch++;if(task!=null)task.cancel(true);busy=false;clearOutputs();}
    private void clearOutputs(){for(PhotoCompressionEngine.Output o:outputs)o.file.delete();outputs.clear();failures.clear();pendingTrash.clear();batchCount=0;completedCount=0;}
    private List<PhotoCompressionEngine.Output> unsaved(){List<PhotoCompressionEngine.Output> list=new ArrayList<>();for(PhotoCompressionEngine.Output o:outputs)if(o.saved==null)list.add(o);return list;}
    private int savedCount(){int n=0;for(PhotoCompressionEngine.Output o:outputs)if(o.saved!=null)n++;return n;}
    private void showResult(){
        phase=RESULT;cancelThumbs();LinearLayout l=root();header(l,"Compression complete",false);LinearLayout b=body(l);int saved=savedCount(),successful=outputs.size();long before=0,after=0;for(PhotoCompressionEngine.Output o:outputs){before+=o.source.bytes;after+=o.bytes;}long reduction=before-after;
        LinearLayout hero=col();hero.setGravity(Gravity.CENTER);TextView mark=center(successful==0?"!":"✓",43,0xffffffff,true);mark.setBackground(shape(successful==0?gray:0xff34c759,50));add(hero,mark,dp(85),dp(85));gap(hero,13);add(hero,center(successful==0?"No photos compressed":saved==successful?"Compressed photos saved":failures.isEmpty()?"All compressed!":"Compression finished",24,ink,true),-1,-2);gap(hero,6);add(hero,center(successful+" of "+batchCount+" photos compressed"+(saved>0?" · "+saved+" saved":""),13,gray,false),-1,-2);add(b,hero,-1,-2);gap(b,17);
        if(successful>0){LinearLayout stats=block();add(stats,center(reduction>0?"Reduced file size by":"Actual output size",13,gray,false),-1,-2);gap(stats,8);add(stats,center(size(reduction>0?reduction:after),33,reduction>0?green:ink,true),-1,-2);gap(stats,5);add(stats,center(reduction>0?Math.round(100d*reduction/Math.max(1,before))+"% smaller":"Output is not smaller for this batch",12,reduction>0?green:gray,false),-1,-2);gap(stats,15);sizeRow(stats,"Before",before,Math.max(before,after),gray);gap(stats,12);sizeRow(stats,"After",after,Math.max(before,after),blue);add(b,stats,-1,-2);gap(b,12);
            LinearLayout details=block();add(details,text("Photo results",16,ink,true),-1,-2);gap(details,9);for(PhotoCompressionEngine.Output o:outputs){LinearLayout line=col();add(line,text(o.source.name,13,ink,true),-1,-2);gap(line,4);add(line,text(size(o.source.bytes)+" → "+size(o.bytes)+(o.saved!=null?" · Saved":"")+(o.trashed?" · Trash approved":""),12,o.bytes<o.source.bytes?green:gray,false),-1,-2);gap(line,9);add(details,line,-1,-2);}add(b,details,-1,-2);gap(b,10);}
        if(!failures.isEmpty()){LinearLayout failed=block();add(failed,text(failures.size()+" photos not compressed",16,red,true),-1,-2);gap(failed,8);for(String error:failures){add(failed,text(error,12,gray,false),-1,-2);gap(failed,6);}add(b,failed,-1,-2);gap(b,10);}
        if(saved>0){add(b,button("Share saved photos",blue,card,this::share),-1,dp(47));gap(b,8);int removable=eligibleOutputs().size();if(removable>0){add(b,button("Remove "+removable+" original photos",red,card,this::confirmRemoval),-1,dp(47));foot(b,"Only saved, verified, smaller copies can qualify. Android approval required.");}}
        if(saved<successful){primary(l,"Save "+(successful-saved)+" compressed photos",this::save);}
        else primary(l,successful==0?"Change settings":"Compress more",successful==0?this::showSettings:this::newBatch);
        foot(l,"Saving copies uses additional space. Original photos remain until separately approved.");setContentView(l);
    }
    private void sizeRow(LinearLayout l,String s,long value,long max,int tint){LinearLayout r=row();weight(r,text(s,13,gray,false));add(r,text(size(value),15,ink,true),-2,-2);add(l,r,-1,-2);gap(l,6);bar(l,value,max,tint);}
    private void save(){if(busy||unsaved().isEmpty())return;if(Build.VERSION.SDK_INT>=29){saveTo(null);return;}Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);try{startActivityForResult(i,FOLDER);}catch(ActivityNotFoundException e){toast("No save folder available");}}
    private void saveTo(Uri tree){
        if(busy)return;List<PhotoCompressionEngine.Output> batch=unsaved();if(batch.isEmpty())return;busy=true;status("Saving and verifying "+batch.size()+" photos…");final int token=++epoch;
        task=io.submit(()->{int success=0,failed=0;long before=0,after=0;
            for(PhotoCompressionEngine.Output o:batch){if(Thread.currentThread().isInterrupted())break;try{Uri uri=PhotoCompressionEngine.save(this,o,tree);o.saved=uri;o.gallerySaved=tree==null;success++;before+=o.source.bytes;after+=o.bytes;}catch(Exception e){failed++;}}
            boolean logged=success==0||history.add(null,"photo_compression","Photos compressed","Saved "+success+" copies · "+size(before)+" → "+size(after),0,success,false);final int ok=success,bad=failed;runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;showResult();if(bad>0)dialog().setTitle("Some photos could not be saved").setMessage(ok+" saved, "+bad+" not saved. Retry the remaining copies. Originals were not changed.").setPositiveButton("Done",null).show();else if(!logged)toast("Saved, but history could not be recorded");else toast(ok+" photos saved");});
        });
    }
    private List<PhotoCompressionEngine.Output> eligibleOutputs(){List<PhotoCompressionEngine.Output> list=new ArrayList<>();if(Build.VERSION.SDK_INT<30)return list;for(PhotoCompressionEngine.Output o:outputs){SmartMediaScan.Item i=o.source.original;if(o.saved!=null&&o.gallerySaved&&!o.trashed&&o.bytes<o.source.bytes&&i!=null&&!i.protectedItem&&!o.source.version.isEmpty())list.add(o);}return list;}
    private void confirmRemoval(){
        if(busy)return;List<PhotoCompressionEngine.Output> list=eligibleOutputs();if(list.isEmpty())return;
        String[] names=new String[list.size()];boolean[] checked=new boolean[list.size()];for(int i=0;i<list.size();i++){PhotoCompressionEngine.Output o=list.get(i);names[i]=o.source.name+" · "+size(o.source.bytes)+" → "+size(o.bytes);}
        dialog().setTitle("Select originals to move to Trash").setMultiChoiceItems(names,checked,(d,w,c)->checked[w]=c).setNegativeButton("Keep originals",null).setPositiveButton("Continue",(d,w)->{List<PhotoCompressionEngine.Output> chosen=new ArrayList<>();for(int i=0;i<list.size();i++)if(checked[i])chosen.add(list.get(i));if(chosen.isEmpty()){toast("No originals selected");return;}dialog().setTitle("Keep your compressed copies?").setMessage("You are choosing to move "+chosen.size()+" original photos to Android Trash. Compressed copies may have less detail or metadata. Android will ask for separate approval.").setNegativeButton("Cancel",null).setPositiveButton("Verify and continue",(a,b)->verifyRemoval(chosen)).show();}).show();
    }
    private void verifyRemoval(List<PhotoCompressionEngine.Output> chosen){
        if(busy||Build.VERSION.SDK_INT<30)return;busy=true;status("Verifying saved copies and protection…");final int token=++epoch;
        task=io.submit(()->{List<Uri> approved=new ArrayList<>();try{
            if(!SmartMediaScan.hasPhotoAccess(this))throw new IOException("Photo access changed");SmartMediaScan.Gallery fresh=new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this)).gallery();Map<String,SmartMediaScan.Item> current=new HashMap<>();for(SmartMediaScan.Item i:fresh.items)current.put(i.uri.toString(),i);
            for(PhotoCompressionEngine.Output o:chosen){SmartMediaScan.Item old=o.source.original,now=current.get(o.source.uri.toString());if(old==null||old.volume==null||!o.source.version.equals(MediaStore.getVersion(this,old.volume))||!SmartMediaScan.sameGalleryItem(old,now)||!PhotoCompressionEngine.matches(this,o)||!MessageDigest.isEqual(o.inputHash,PhotoCompressionEngine.hash(this,o.source.uri)))continue;approved.add(old.uri);}
        }catch(Exception e){approved.clear();}
        if(approved.size()==chosen.size())try{SmartMediaScan.Gallery last=new SmartMediaScan(this,prefs,new ProtectedFiles(this),new ProtectedFolders(this)).gallery();Map<String,SmartMediaScan.Item> finalItems=new HashMap<>();for(SmartMediaScan.Item i:last.items)finalItems.put(i.uri.toString(),i);for(PhotoCompressionEngine.Output o:chosen)if(!SmartMediaScan.sameGalleryItem(o.source.original,finalItems.get(o.source.uri.toString()))||!o.source.version.equals(MediaStore.getVersion(this,o.source.original.volume)))throw new IOException("Original changed");}catch(Exception e){approved.clear();}
        final List<Uri> verified=approved;runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;if(!foreground||verified.size()!=chosen.size()||!SmartMediaScan.hasPhotoAccess(this)){showResult();toast("Originals kept: a copy, source or protection changed.");return;}try{PendingIntent request=MediaStore.createTrashRequest(getContentResolver(),verified,true);pendingTrash.clear();pendingTrash.addAll(verified);List<SmartMediaScan.Item> originals=new ArrayList<>();for(PhotoCompressionEngine.Output o:chosen)originals.add(o.source.original);cleanupReceipt=CleanupReceipt.capture(this,"Compressed photo originals",verified,originals);trashPending=true;startIntentSenderForResult(request.getIntentSender(),TRASH,null,0,0,0);}catch(Exception e){trashPending=false;cleanupReceipt=null;pendingTrash.clear();showResult();toast("Android Trash approval unavailable");}});
        });
    }
    private void share(){ArrayList<Uri> uris=new ArrayList<>();for(PhotoCompressionEngine.Output o:outputs)if(o.saved!=null)uris.add(o.saved);if(uris.isEmpty())return;Intent i=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE);i.setType("image/*");if(uris.size()==1)i.putExtra(Intent.EXTRA_STREAM,uris.get(0));else i.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);ClipData clip=ClipData.newRawUri("Compressed photo",uris.get(0));for(int n=1;n<uris.size();n++)clip.addItem(new ClipData.Item(uris.get(n)));i.setClipData(clip);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(Intent.createChooser(i,"Share compressed photos"));}catch(ActivityNotFoundException e){toast("No sharing app available");}}
    private void showHistory(){if(busy)return;phase=HISTORY;cancelThumbs();LinearLayout l=root();header(l,"Compression history",false);LinearLayout b=body(l);int n=0;for(ActivityHistory.Entry e:history.entries())if("photo_compression".equals(e.category)){LinearLayout box=block();add(box,text(e.title,15,ink,true),-1,-2);gap(box,6);add(box,text(e.detail,12,gray,false),-1,-2);gap(box,5);add(box,text(android.text.format.DateFormat.getMediumDateFormat(this).format(new Date(e.timestamp)),12,gray,false),-1,-2);add(b,box,-1,-2);gap(b,9);n++;}if(n==0){LinearLayout box=block();add(box,center("No compressed photos yet",17,ink,true),-1,dp(46));add(box,center("Successful saves appear here.",13,gray,false),-1,dp(40));add(b,box,-1,-2);}foot(l,"History records saved copies, not reclaimed storage.");setContentView(l);}
    private void newBatch(){clearOutputs();selected.clear();month="All photos";preset=PhotoCompressionEngine.Preset.BALANCED;format=PhotoCompressionEngine.Format.ORIGINAL;loadGallery();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==ACCESS){accessReturned();return;}
        if(request==PICK&&result==RESULT_OK&&data!=null){importPhotos(data);return;}
        if(request==FOLDER&&result==RESULT_OK&&data!=null&&data.getData()!=null){saveTo(data.getData());return;}
        if(request==TRASH){final CleanupReceipt receipt=cleanupReceipt;cleanupReceipt=null;trashPending=false;busy=true;status("Checking cleanup result…");
            CleanupReceipt.verifyAsync(this,receipt,result==RESULT_OK,verified->{busy=false;for(PhotoCompressionEngine.Output o:outputs)if(verified.confirmed.contains(o.source.uri.toString()))o.trashed=true;pendingTrash.clear();toast(verified.detail()+(verified.logged?"":" History could not be saved."));if(outputs.isEmpty())loadGallery();else showResult();});}
    }
    @Override public void onBackPressed(){if(trashPending)return;if(busy){if(phase==ENCODING)confirmCancel();else toast("Current operation is finishing");return;}if(phase==HISTORY||phase==SETTINGS){showGallery();return;}if(phase==RESULT){if(unsaved().isEmpty()){newBatch();return;}dialog().setTitle("Discard unsaved copies?").setMessage("Saved copies and originals stay unchanged.").setNegativeButton("Stay",null).setPositiveButton("Discard",(d,w)->{clearOutputs();showSettings();}).show();return;}finish();}
    @Override protected void onResume(){super.onResume();foreground=true;
        if(refreshGallery&&!accessPending&&!busy&&!trashPending&&phase==GALLERY){refreshGallery=false;loadGallery();}
    }
    @Override protected void onPause(){foreground=false;super.onPause();}
    @Override protected void onStop(){super.onStop();if(phase==ENCODING&&busy){cancelBatch();if(!isFinishing()){showSettings();toast("Compression cancelled when the app left the foreground");}}if(phase==GALLERY&&!busy&&!accessPending&&!trashPending){cancelThumbs();if(scanner!=null){scanner.cancel();scanner=null;}epoch++;loading=false;gallery.clear();months.clear();currentFilter=Collections.emptyList();selected.entrySet().removeIf(e->e.getValue().original!=null);refreshGallery=true;}}
    @Override protected void onDestroy(){destroyed=true;epoch++;cancelThumbs();if(scanner!=null)scanner.cancel();if(task!=null)task.cancel(true);io.shutdownNow();thumbnails.shutdownNow();clearOutputs();super.onDestroy();}
}

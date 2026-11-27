package com.clenova.app;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** App Manager: real inventory, optional metrics, native settings, reviewed uninstall. */
public final class AppManagerActivity extends Activity {
    private static final int MANAGE=241,USAGE=242,UNINSTALL=243,OPEN=244,PAGE=24;
    private static final int HOME=0,DETAIL=1,REVIEW=2,PERMISSIONS=3,INFO=4,HISTORY=5;
    private final ExecutorService io=Executors.newSingleThreadExecutor(),icons=Executors.newSingleThreadExecutor();
    private final List<Future<?>> iconJobs=new ArrayList<>();
    private final android.util.LruCache<String,Bitmap> iconCache=new android.util.LruCache<String,Bitmap>(96);
    private AppManagerEngine engine;private AppManagerEngine.Result result;
    private ActivityHistory history;private AppPreferences prefs;
    private CancellationSignal signal;private Future<?> task;
    private int bg,card,ink,gray,blue,pale,green,red,phase=HOME,filter,sort,page,epoch,iconEpoch;
    private boolean dark,busy,destroyed,foreground,pending,initial=true;
    private String query="",selectedPkg="",pendingPkg="",pendingName="",notice="",operationId="";
    private LinearLayout listHost;
    private EditText search;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=new AppPreferences(this);history=new ActivityHistory(this);engine=new AppManagerEngine(this);
        dark=prefs.appearance().equals("Dark")||(prefs.appearance().equals("System")&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=dark?0xff141416:0xfff5f5f7;card=dark?0xff242426:0xffffffff;ink=dark?0xfff4f4f6:0xff111111;gray=dark?0xffa2a2aa:0xff86868b;blue=dark?0xff409cff:0xff007aff;pale=dark?0xff19334c:0xffe6f0ff;green=dark?0xff61d883:0xff249d42;red=dark?0xffff6961:0xffe43e35;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27&&!dark)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(state!=null){pending=state.getBoolean("pending");pendingPkg=state.getString("pending_pkg","");pendingName=state.getString("pending_name","");operationId=state.getString("operation_id","");query=state.getString("query","");filter=state.getInt("filter");sort=state.getInt("sort");}
        if(pending)status("Waiting for Android uninstall confirmation…");else refresh(false);
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("pending",pending);state.putString("pending_pkg",pendingPkg);state.putString("pending_name",pendingName);state.putString("operation_id",operationId);state.putString("query",query);state.putInt("filter",filter);state.putInt("sort",sort);}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String tr(String s){return AppStrings.translate(prefs.language(),s);}
    private GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout p,View v,int w,int h){p.addView(v,new LinearLayout.LayoutParams(w,h));}
    private void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private void gap(LinearLayout p,int n){add(p,new View(this),dp(n),dp(n));}
    private TextView text(String s,int size,int tint,boolean bold){TextView t=new TextView(this);t.setText(tr(s));t.setTextSize(size);t.setTextColor(tint);t.setIncludeFontPadding(false);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private TextView center(String s,int size,int tint,boolean bold){TextView t=text(s,size,tint,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView button(String s,int tint,int fill,Runnable r){TextView t=center(s,14,tint,true);t.setBackground(shape(fill,22));t.setPadding(dp(10),dp(7),dp(10),dp(7));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->{if(!destroyed&&!busy&&!pending)r.run();});return t;}
    private LinearLayout block(){LinearLayout l=col();l.setBackground(shape(card,22));l.setPadding(dp(14),dp(13),dp(14),dp(13));return l;}
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(18),dp(10),dp(18),dp(10));l.setFocusableInTouchMode(true);return l;}
    private LinearLayout body(LinearLayout root){ScrollView s=new ScrollView(this);s.setVerticalScrollBarEnabled(false);s.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout b=col();s.addView(b,new ScrollView.LayoutParams(-1,-2));root.addView(s,new LinearLayout.LayoutParams(-1,0,1));return b;}
    private void header(LinearLayout l,String title,boolean refresh){LinearLayout h=row();TextView back=button("‹",ink,card,this::onBackPressed);back.setTextSize(27);back.setContentDescription("Back");add(h,back,dp(44),dp(44));gap(h,8);weight(h,center(title,19,ink,true));gap(h,8);if(refresh){TextView b=button("↻",ink,card,()->refresh(false));b.setTextSize(25);b.setContentDescription("Refresh apps");add(h,b,dp(44),dp(44));}else add(h,new View(this),dp(44),dp(44));add(l,h,-1,dp(52));gap(l,10);}
    private void primary(LinearLayout l,String title,Runnable action){gap(l,10);add(l,button(title,0xffffffff,blue,action),-1,dp(51));}
    private void foot(LinearLayout l,String s){gap(l,8);add(l,center(s,11,gray,false),-1,-2);}
    private void rule(LinearLayout l){View v=new View(this);v.setBackgroundColor(dark?0xff38383c:0xffededf0);add(l,v,-1,dp(1));}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
    private void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_SHORT).show();}
    private String size(long b){if(b<0)return "Unavailable";return b>=1073741824L?String.format(Locale.getDefault(),"%.2f GB",b/1073741824d):b>=1048576?String.format(Locale.getDefault(),"%.1f MB",b/1048576d):b>=1024?String.format(Locale.getDefault(),"%.0f KB",b/1024d):b+" B";}
    private String date(long t){return t<=0?"Unavailable":android.text.format.DateFormat.getMediumDateFormat(this).format(new Date(t));}
    private String used(AppManagerEngine.Item i){if(i.lastUsed<=0)return result!=null&&result.usage?"Usage not observed":"Usage access off";long d=Math.max(0,(System.currentTimeMillis()-i.lastUsed)/AppManagerEngine.DAY);return d==0?"Used within 24 hours":d==1?"Last used 1 day ago":"Last used "+d+" days ago";}
    private void status(String msg){LinearLayout l=root();header(l,"App Manager",false);LinearLayout c=col();c.setGravity(Gravity.CENTER);ProgressBar p=new ProgressBar(this);add(c,p,dp(44),dp(44));gap(c,15);add(c,center(msg,14,gray,false),-1,-2);l.addView(c,new LinearLayout.LayoutParams(-1,0,1));setContentView(l);}
    private void cancelIcons(){iconEpoch++;for(Future<?> f:iconJobs)f.cancel(true);iconJobs.clear();}
    private void icon(ImageView view,AppManagerEngine.Item item){String key=item.pkg+":"+item.updated;Bitmap cached=iconCache.get(key);if(cached!=null){view.setImageBitmap(cached);return;}final int token=iconEpoch;iconJobs.add(icons.submit(()->{Bitmap b=engine.icon(item.pkg);runOnUiThread(()->{if(b==null)return;if(destroyed||token!=iconEpoch||!view.isAttachedToWindow()){b.recycle();return;}iconCache.put(key,b);view.setImageBitmap(b);});}));}
    private ImageView appIcon(AppManagerEngine.Item i){ImageView v=new ImageView(this);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setBackground(shape(pale,12));v.setContentDescription(null);icon(v,i);return v;}
    private AppManagerEngine.Item current(){if(result!=null)for(AppManagerEngine.Item i:result.items)if(i.pkg.equals(selectedPkg))return i;return null;}
    private void refresh(boolean keepDetail){
        if(destroyed||pending)return;cancelIcons();if(signal!=null)signal.cancel();if(task!=null)task.cancel(true);busy=true;status("Checking installed apps…");final int token=++epoch;final CancellationSignal cancel=new CancellationSignal();signal=cancel;
        task=io.submit(()->{try{AppManagerEngine.Result data=engine.scan(cancel);runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;signal=null;result=data;initial=false;if(keepDetail&&current()!=null)showDetail();else showHome();});}
        catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;signal=null;result=null;initial=false;notice="App list unavailable. Refresh to try again.";showHome();});}});
    }
    private void showHome(){
        phase=HOME;cancelIcons();LinearLayout l=root();header(l,"App Manager",true);LinearLayout b=body(l);
        LinearLayout hero=block();LinearLayout summary=row();LinearLayout words=col();add(words,text("App storage",13,gray,false),-1,-2);gap(words,7);
        long apps=result==null?0:result.appBytes(),data=result==null?0:result.dataBytes();int measured=result==null?0:result.measured(),total=result==null?0:result.items.size();
        add(words,text(measured==0?"—":size(apps+data),34,ink,true),-1,-2);gap(words,5);add(words,text(result==null?"App inventory unavailable":total+" visible apps",13,gray,false),-1,-2);weight(summary,words);add(summary,new StorageRing(apps,data,measured>0),dp(82),dp(82));add(hero,summary,-1,-2);gap(hero,13);rule(hero);gap(hero,12);
        LinearLayout split=row();LinearLayout a=col(),d=col();add(a,text(measured==0?"—":size(data),20,ink,true),-1,-2);gap(a,4);add(a,text("App data incl. cache",11,gray,false),-1,-2);add(d,text(measured==0?"—":size(apps),20,ink,true),-1,-2);gap(d,4);add(d,text("App size",11,gray,false),-1,-2);weight(split,a);weight(split,d);add(hero,split,-1,-2);
        if(measured>0){gap(hero,8);add(hero,text("Measured "+measured+" of "+total+" visible apps · current profile",11,gray,false),-1,-2);}add(b,hero,-1,-2);gap(b,12);
        if(result==null||!result.usage){LinearLayout access=block();add(access,text("Unlock app insights",16,blue,true),-1,-2);gap(access,5);add(access,text("Optional Usage Access enables available storage and last-used information. Apps can still be managed without it.",12,gray,false),-1,-2);gap(access,8);add(access,button("Enable Usage Access",blue,pale,this::openUsage),-1,dp(44));add(b,access,-1,-2);gap(b,10);}
        else if(result.partial||measured<total){add(b,text("Some apps or storage figures are unavailable; totals cover measured apps only.",12,gray,false),-1,-2);gap(b,8);}
        if(!notice.isEmpty()){LinearLayout message=block();add(message,text(notice,13,gray,false),-1,-2);add(b,message,-1,-2);gap(b,8);}
        search=new EditText(this);search.setSingleLine(true);search.setHint("Search apps");search.setText(query);search.setTextColor(ink);search.setHintTextColor(gray);search.setTextSize(14);search.setPadding(dp(14),dp(8),dp(14),dp(8));search.setBackground(shape(dark?0xff29292d:0xffe9e9ee,13));search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);add(b,search,-1,dp(46));gap(b,9);
        LinearLayout filters=row();String[] labels={"All apps","Large","Unused"};for(int n=0;n<labels.length;n++){final int choice=n;if(n>0)gap(filters,6);TextView t=button(labels[n],filter==n?0xffffffff:ink,filter==n?blue:card,()->{filter=choice;page=0;showHome();});filters.addView(t,new LinearLayout.LayoutParams(0,dp(39),1));}add(b,filters,-1,-2);gap(b,11);
        LinearLayout head=row();weight(head,text("Installed apps",17,ink,true));add(head,button(new String[]{"Largest first","Name","Recently used","Least recently used"}[sort]+" ▾",blue,card,this::chooseSort),-2,dp(39));add(b,head,-1,-2);gap(b,8);
        listHost=col();add(b,listHost,-1,-2);gap(b,10);add(b,button("App activity history",blue,card,this::showHistory),-1,dp(45));foot(l,"System and essential apps are protected from in-app uninstall.");setContentView(l);l.requestFocus();renderList();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();page=0;renderList();}public void afterTextChanged(Editable e){}});
    }
    private final class StorageRing extends View{private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final long a,d;private final boolean known;StorageRing(long a,long d,boolean k){super(AppManagerActivity.this);this.a=a;this.d=d;known=k;setContentDescription(k?"Measured app and data storage":"Storage not measured");}
        @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.max(1,Math.min(x,y)-dp(8));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(8));p.setColor(dark?0xff38383c:0xffe6e6eb);c.drawCircle(x,y,r,p);if(known&&a+d>0){p.setColor(0xff85b8ff);c.drawCircle(x,y,r,p);p.setColor(blue);c.drawArc(x-r,y-r,x+r,y+r,-90,360f*a/(a+d),false,p);}p.setStyle(Paint.Style.FILL);p.setColor(blue);float w=dp(8);for(int i=0;i<2;i++)for(int j=0;j<2;j++){float left=x-dp(10)+i*dp(12),top=y-dp(10)+j*dp(12);c.drawRoundRect(left,top,left+w,top+w,dp(2),dp(2),p);}}}
    private void chooseSort(){dialog().setTitle("Sort apps").setSingleChoiceItems(new String[]{"Largest first","Name","Recently used","Least recently used"},sort,(d,n)->{sort=n;page=0;d.dismiss();showHome();}).setNegativeButton("Cancel",null).show();}
    private List<AppManagerEngine.Item> visible(){
        List<AppManagerEngine.Item> list=new ArrayList<>();if(result==null)return list;String q=query.trim().toLowerCase(Locale.ROOT);long now=System.currentTimeMillis();
        for(AppManagerEngine.Item i:result.items){if(!i.name.toLowerCase(Locale.ROOT).contains(q)&&!i.pkg.toLowerCase(Locale.ROOT).contains(q))continue;if(filter==1&&i.bytes()<AppManagerEngine.LARGE)continue;if(filter==2&&!i.unused(now))continue;list.add(i);}
        Comparator<AppManagerEngine.Item> byName=Comparator.comparing((AppManagerEngine.Item i)->i.name,String.CASE_INSENSITIVE_ORDER).thenComparing(i->i.pkg);
        Comparator<AppManagerEngine.Item> c=sort==0?Comparator.comparingLong(AppManagerEngine.Item::bytes).reversed():sort==2?Comparator.comparingLong((AppManagerEngine.Item i)->i.lastUsed).reversed():sort==3?Comparator.comparingLong((AppManagerEngine.Item i)->i.lastUsed<0?Long.MAX_VALUE:i.lastUsed):byName;list.sort(c.thenComparing(byName));return list;
    }
    private void renderList(){
        if(listHost==null||phase!=HOME)return;cancelIcons();listHost.removeAllViews();List<AppManagerEngine.Item> list=visible();
        if(list.isEmpty()){LinearLayout empty=block();String msg=(filter>0&&(result==null||!result.usage))?"Enable Usage Access to filter by storage or usage.":filter==1?"No measured apps at least 500 MB":filter==2?"No eligible apps with observed inactivity of 30+ days":"No matching apps";add(empty,center(msg,14,gray,false),-1,dp(78));add(listHost,empty,-1,-2);return;}
        page=Math.min(page,Math.max(0,(list.size()-1)/PAGE));LinearLayout group=block();group.setPadding(dp(12),dp(3),dp(12),dp(3));int end=Math.min(list.size(),(page+1)*PAGE);
        for(int n=page*PAGE;n<end;n++){AppManagerEngine.Item i=list.get(n);if(n>page*PAGE)rule(group);LinearLayout r=row();r.setPadding(0,dp(11),0,dp(11));add(r,appIcon(i),dp(44),dp(44));gap(r,10);LinearLayout copy=col();TextView title=text(i.name,15,ink,true);title.setMaxLines(2);add(copy,title,-1,-2);gap(copy,4);add(copy,text(used(i)+(i.protectedApp?" · Protected":""),11,gray,false),-1,-2);weight(r,copy);gap(r,6);add(r,text(i.bytes()<0?"—":size(i.bytes()),13,ink,true),-2,-2);gap(r,6);add(r,text("›",21,gray,false),-2,-2);r.setFocusable(true);r.setClickable(true);r.setContentDescription(i.name+", "+size(i.bytes())+(i.protectedApp?", Protected":""));r.setOnClickListener(v->{selectedPkg=i.pkg;hideKeyboard();showDetail();});add(group,r,-1,-2);}
        add(listHost,group,-1,-2);if(list.size()>PAGE){gap(listHost,10);LinearLayout nav=row();TextView prev=button("Previous",blue,card,()->{page--;renderList();});prev.setEnabled(page>0);add(nav,prev,-2,dp(42));weight(nav,center((page+1)+" / "+((list.size()+PAGE-1)/PAGE),12,gray,false));TextView next=button("Next",blue,card,()->{page++;renderList();});next.setEnabled((page+1)*PAGE<list.size());add(nav,next,-2,dp(42));add(listHost,nav,-1,-2);}
        foot(listHost,filter==1?"Large apps: measured size of 500 MB or more.":filter==2?"Unused: observed inactivity for at least 30 days; unknown usage excluded.":"Only launcher-visible apps in this Android profile are listed.");
    }
    private void hideKeyboard(){if(search!=null){android.view.inputmethod.InputMethodManager m=getSystemService(android.view.inputmethod.InputMethodManager.class);if(m!=null)m.hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();}}
    private void openUsage(){hideKeyboard();try{startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),USAGE);}catch(ActivityNotFoundException e){toast("Usage Access settings unavailable");}}
    private void showDetail(){
        AppManagerEngine.Item i=current();if(i==null){showHome();return;}phase=DETAIL;cancelIcons();LinearLayout l=root();header(l,"App details",false);LinearLayout b=body(l);LinearLayout hero=col();hero.setGravity(Gravity.CENTER);gap(hero,5);add(hero,appIcon(i),dp(78),dp(78));gap(hero,12);add(hero,center(i.name,25,ink,true),-1,-2);gap(hero,5);add(hero,center(i.system?"System app":i.protectedApp?"Protected app":"User-installed app",12,gray,false),-1,-2);gap(hero,8);TextView use=center(used(i),12,blue,true);use.setBackground(shape(pale,20));use.setPadding(dp(12),dp(7),dp(12),dp(7));add(hero,use,-2,-2);add(b,hero,-1,-2);gap(b,16);
        LinearLayout storage=block();add(storage,text("Total storage",13,gray,false),-1,-2);gap(storage,7);add(storage,text(size(i.bytes()),32,ink,true),-1,-2);gap(storage,14);
        if(i.bytes()>0){LinearLayout bar=row();bar.setGravity(Gravity.CENTER_VERTICAL);long[] values=i.userData()<0?new long[]{i.app,i.data}:new long[]{i.app,i.userData(),i.cache};int[] colors={blue,0xff85b8ff,0xffd9e8ff};for(int n=0;n<values.length;n++)if(values[n]>0){View segment=new View(this);segment.setBackground(shape(colors[n],4));bar.addView(segment,new LinearLayout.LayoutParams(0,dp(9),(float)values[n]/i.bytes()));}add(storage,bar,-1,dp(9));gap(storage,13);}
        metric(storage,"App size",i.app,blue);gap(storage,9);metric(storage,i.userData()<0?"App data incl. cache":"User data excluding cache",i.userData()<0?i.data:i.userData(),0xff85b8ff);gap(storage,9);metric(storage,"Cache",i.cache,0xffd9e8ff);
        if(i.userData()<0)foot(storage,"Cache is included in data when a separate breakdown is unavailable.");add(b,storage,-1,-2);gap(b,13);
        LinearLayout actions=block();action(actions,"↗","Open app",()->openApp(i));rule(actions);action(actions,"▣","Manage storage",()->nativeSettings(i));rule(actions);action(actions,"✓","App permissions",()->showPermissions(i));rule(actions);action(actions,"ⓘ","App information",this::showInfo);rule(actions);action(actions,"↗", "com.android.vending".equals(i.installer)?"Share app link":"Share app information",()->share(i));add(b,actions,-1,-2);
        if(!i.protectedApp){gap(l,8);add(l,button("Uninstall app",red,dark?0xff47282b:0xfffce9e9,this::showReview),-1,dp(49));foot(l,"Android confirmation required. App data may be lost.");}else foot(l,"Protected app · in-app uninstall disabled.");setContentView(l);
    }
    private void metric(LinearLayout l,String s,long bytes,int tint){LinearLayout r=row();View dot=new View(this);dot.setBackground(shape(tint,8));add(r,dot,dp(9),dp(9));gap(r,8);weight(r,text(s,13,gray,false));add(r,text(size(bytes),13,ink,true),-2,-2);add(l,r,-1,-2);}
    private void action(LinearLayout l,String glyph,String label,Runnable run){LinearLayout r=row();r.setMinimumHeight(dp(49));add(r,center(glyph,20,blue,true),dp(29),dp(40));gap(r,8);weight(r,text(label,14,ink,true));add(r,text("›",21,gray,false),-2,-2);r.setClickable(true);r.setFocusable(true);r.setContentDescription(label);r.setOnClickListener(v->run.run());add(l,r,-1,-2);}
    private void openApp(AppManagerEngine.Item i){try{Intent launch=getPackageManager().getLaunchIntentForPackage(i.pkg);if(launch==null){toast("App cannot be opened");return;}startActivityForResult(launch,OPEN);}catch(RuntimeException e){toast("App cannot be opened");}}
    private void nativeSettings(AppManagerEngine.Item i){try{startActivityForResult(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",i.pkg,null)),MANAGE);}catch(RuntimeException e){toast("Android app settings unavailable");}}
    private void share(AppManagerEngine.Item i){String content="com.android.vending".equals(i.installer)?i.name+"\nhttps://play.google.com/store/apps/details?id="+i.pkg:i.name+"\nPackage: "+i.pkg;Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,content);try{startActivity(Intent.createChooser(send,"Share app"));}catch(RuntimeException e){toast("No sharing app available");}}
    private void showInfo(){AppManagerEngine.Item i=current();if(i==null){showHome();return;}phase=INFO;cancelIcons();LinearLayout l=root();header(l,"App information",false);LinearLayout b=body(l);LinearLayout c=block();add(c,text(i.name,21,ink,true),-1,-2);gap(c,14);info(c,"Package",i.pkg);info(c,"Version",i.version==null?"Unavailable":i.version);info(c,"Version code",Long.toString(i.versionCode));info(c,"Installed",date(i.installed));info(c,"Last updated",date(i.updated));info(c,"Install source",i.installer==null?"Unavailable":i.installer);info(c,"Type",i.system?"System":i.protectedApp?"Protected":"User-installed");add(b,c,-1,-2);primary(l,"Open Android app settings",()->nativeSettings(i));setContentView(l);}
    private void info(LinearLayout c,String title,String value){add(c,text(title,12,gray,false),-1,-2);gap(c,4);TextView t=text(value,14,ink,true);t.setTextIsSelectable(true);add(c,t,-1,-2);gap(c,11);}
    private void showPermissions(AppManagerEngine.Item item){
        phase=PERMISSIONS;cancelIcons();busy=true;status("Reading requested permissions…");final int token=++epoch;task=io.submit(()->{List<AppManagerEngine.Permission> data=null;try{data=engine.permissions(item);}catch(Exception ignored){}final List<AppManagerEngine.Permission> permissions=data;runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;LinearLayout l=root();header(l,"App permissions",false);LinearLayout b=body(l);add(b,text(item.name,22,ink,true),-1,-2);gap(b,7);add(b,text("Declared permissions and Android-reported grants. Not a record of actual access.",12,gray,false),-1,-2);gap(b,13);LinearLayout c=block();if(permissions==null)add(c,text("Permission information unavailable.",14,gray,false),-1,-2);else if(permissions.isEmpty())add(c,text("No requested permissions reported.",14,gray,false),-1,-2);else for(AppManagerEngine.Permission p:permissions){LinearLayout r=row();LinearLayout copy=col();add(copy,text(p.label,14,ink,true),-1,-2);gap(copy,3);add(copy,text(p.name,10,gray,false),-1,-2);weight(r,copy);gap(r,5);add(r,text(p.granted?"Granted":"Not granted",11,p.granted?green:gray,true),-2,-2);add(c,r,-1,-2);gap(c,9);rule(c);gap(c,9);}add(b,c,-1,-2);primary(l,"Manage in Android settings",()->nativeSettings(item));setContentView(l);});});
    }
    private void showReview(){
        AppManagerEngine.Item i=current();if(i==null||i.protectedApp){toast("App is protected");return;}phase=REVIEW;cancelIcons();LinearLayout l=root();header(l,"Review uninstall",false);LinearLayout b=body(l);LinearLayout hero=col();hero.setGravity(Gravity.CENTER);gap(hero,15);add(hero,appIcon(i),dp(78),dp(78));gap(hero,13);add(hero,center(i.name,25,ink,true),-1,-2);gap(hero,6);add(hero,center(used(i),13,gray,false),-1,-2);add(b,hero,-1,-2);gap(b,19);LinearLayout c=block();add(c,center("Measured app storage",13,gray,false),-1,-2);gap(c,9);add(c,center(size(i.bytes()),33,blue,true),-1,-2);gap(c,13);rule(c);gap(c,13);add(c,text("Uninstalling may permanently remove local app data. Actual recovered space can differ from the measured size.",13,gray,false),-1,-2);add(b,c,-1,-2);gap(l,8);add(l,button("Continue to uninstall",0xffffffff,red,()->verifyUninstall(i)),-1,dp(51));gap(l,9);add(l,button("Keep app",ink,card,this::showDetail),-1,dp(49));setContentView(l);
    }
    private void verifyUninstall(AppManagerEngine.Item item){
        if(busy||pending||!foreground)return;busy=true;status("Checking app identity and protection…");final int token=++epoch;task=io.submit(()->{boolean eligible=engine.eligible(item);runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;if(!foreground||!eligible){notice="App changed or is protected. Nothing was uninstalled.";refresh(false);return;}
            Intent intent=new Intent(Intent.ACTION_DELETE,Uri.fromParts("package",item.pkg,null));intent.putExtra(Intent.EXTRA_RETURN_RESULT,true);
            pending=true;pendingPkg=item.pkg;pendingName=item.name;operationId=UUID.randomUUID().toString();try{startActivityForResult(intent,UNINSTALL);}catch(RuntimeException e){pending=false;pendingPkg="";pendingName="";operationId="";toast("Android uninstall unavailable");showReview();}
        });});
    }
    private void verifyResult(int code){
        final String pkg=pendingPkg,name=pendingName,id=operationId;pending=false;pendingPkg="";pendingName="";operationId="";busy=true;status("Checking uninstall result…");final int token=++epoch;
        task=io.submit(()->{boolean absent=false;if(code==RESULT_OK&&!pkg.isEmpty())for(int n=0;n<5;n++){if(engine.confirmedAbsent(pkg)){absent=true;break;}try{Thread.sleep(300);}catch(InterruptedException e){return;}}
            final boolean confirmed=absent;boolean logged=!confirmed||history.add(id,"app_manager","App uninstalled",name+" · Android confirmed and no longer visible",0,1,false);
            runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;notice=confirmed?name+" uninstalled"+(logged?".":"; history could not be saved."):code==RESULT_OK?"Android reported success, but removal could not be verified.":"Uninstall not confirmed.";selectedPkg="";refresh(false);});});
    }
    private void showHistory(){phase=HISTORY;cancelIcons();LinearLayout l=root();header(l,"App activity history",false);LinearLayout b=body(l);int count=0;for(ActivityHistory.Entry e:history.entries())if("app_manager".equals(e.category)){LinearLayout c=block();add(c,text(e.title,16,ink,true),-1,-2);gap(c,5);add(c,text(e.detail,13,gray,false),-1,-2);gap(c,5);add(c,text(date(e.timestamp),12,gray,false),-1,-2);add(b,c,-1,-2);gap(b,9);count++;}if(count==0){LinearLayout c=block();add(c,center("No verified uninstalls yet",17,ink,true),-1,dp(65));add(b,c,-1,-2);}foot(l,"Only confirmed removals are logged. No storage recovery is assumed.");setContentView(l);}
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);if(request==UNINSTALL){verifyResult(code);return;}if(request==USAGE){notice="";refresh(false);return;}if(request==MANAGE||request==OPEN){refresh(true);}}
    @Override protected void onBackPressed(){if(pending)return;if(busy){toast("Current operation is finishing");return;}if(phase==REVIEW||phase==PERMISSIONS||phase==INFO){showDetail();return;}if(phase!=HOME){showHome();return;}finish();}
    @Override protected void onResume(){super.onResume();foreground=true;if(!initial&&!busy&&!pending&&result!=null&&result.usage!=AppManagerEngine.usageAllowed(this))refresh(phase!=HOME);}
    @Override protected void onPause(){foreground=false;super.onPause();}
    @Override protected void onDestroy(){destroyed=true;epoch++;cancelIcons();if(signal!=null)signal.cancel();if(task!=null)task.cancel(true);io.shutdownNow();icons.shutdownNow();iconCache.evictAll();super.onDestroy();}
}

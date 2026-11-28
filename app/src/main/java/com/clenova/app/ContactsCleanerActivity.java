package com.clenova.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Contacts Cleaner: local discovery, explicit review and native contact operations. */
public final class ContactsCleanerActivity extends Activity {
    private static final int READ=231,WRITE=232,BACKUP=233,EDIT=234,PAGE=20;
    private static final int HOME=0,GROUPS=1,MERGE=2,INCOMPLETE=3,REMOVE=4,COMPLETE=5,HISTORY=6;
    private static final int MAX_GROUPS=20;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private AppPreferences prefs;private ActivityHistory history;
    private ContactsCleanerEngine engine;
    private ContactsCleanerEngine.Result result;
    private Future<?> work;
    private int bg,card,ink,gray,blue,pale,green,red,phase=HOME,category,page,epoch,linked,removed,unverified;
    private boolean dark,busy,destroyed,foreground,accessPending;
    private long scanTime;
    private String notice="";
    private final LinkedHashMap<String,ContactsCleanerEngine.Group> selectedGroups=new LinkedHashMap<>();
    private final LinkedHashMap<Long,ContactsCleanerEngine.Contact> selectedContacts=new LinkedHashMap<>();
    private TextView selectedLabel,continueButton;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=new AppPreferences(this);history=new ActivityHistory(this);
        dark=UiKit.dark(this,prefs.appearance());
        UiPolish.Palette palette=UiPolish.palette(dark);bg=palette.background;card=palette.card;ink=palette.ink;gray=palette.muted;
        blue=palette.accent;pale=palette.tint;green=palette.success;red=palette.danger;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);getWindow().setNavigationBarColor(bg);
        if(Build.VERSION.SDK_INT>=27)getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(canRead())scan(false);else showHome();
    }
    private boolean canRead(){return checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED;}
    private boolean canWrite(){return checkSelfPermission(Manifest.permission.WRITE_CONTACTS)==PackageManager.PERMISSION_GRANTED;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String tr(String s){return AppStrings.translate(prefs.language(),s);}
    private GradientDrawable shape(int c,int r){return UiKit.shape(this,c,r);}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout p,View v,int w,int h){p.addView(v,new LinearLayout.LayoutParams(w,h));}
    private void weight(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    private void gap(LinearLayout p,int n){add(p,new View(this),dp(n),dp(n));}
    private TextView text(String s,int size,int tint,boolean bold){TextView t=new TextView(this);t.setText(tr(s));UiKit.typography(t,size,tint,bold?600:400);return t;}
    private TextView center(String s,int size,int tint,boolean bold){TextView t=text(s,size,tint,bold);t.setGravity(Gravity.CENTER);return t;}
    private TextView button(String s,int tint,int fill,Runnable r){TextView t=center(s,14,tint,true);UiKit.buttonSurface(t,fill,tint,dark);t.setPadding(dp(10),dp(8),dp(10),dp(8));t.setFocusable(true);t.setClickable(true);t.setOnClickListener(v->{if(!destroyed&&!busy)r.run();});return t;}
    private LinearLayout block(){LinearLayout l=col();l.setBackground(UiKit.card(this,dark));l.setPadding(dp(UiPolish.CARD_PAD_X),dp(UiPolish.CARD_PAD_Y),dp(UiPolish.CARD_PAD_X),dp(UiPolish.CARD_PAD_Y));return l;}
    private LinearLayout root(){LinearLayout l=col();l.setBackgroundColor(bg);l.setPadding(dp(UiKit.gutter(this)),dp(10),dp(UiKit.gutter(this)),dp(10));return l;}
    private LinearLayout body(LinearLayout l){ScrollView s=new ScrollView(this);s.setVerticalScrollBarEnabled(false);s.setOverScrollMode(View.OVER_SCROLL_NEVER);LinearLayout b=col();s.addView(b,new ScrollView.LayoutParams(-1,-2));l.addView(s,new LinearLayout.LayoutParams(-1,0,1));return b;}
    private void header(LinearLayout l,String title,boolean past){LinearLayout h=row();android.widget.ImageButton back=UiKit.headerIcon(this,"back",tr("Back"),ink,dark,() -> {if(!destroyed&&!busy)onBackPressed();});add(h,back,dp(UiPolish.ICON_TARGET),dp(UiPolish.ICON_TARGET));gap(h,8);weight(h,center(title,18,ink,true));gap(h,8);
        if(past){android.widget.ImageButton b=UiKit.headerIcon(this,"history",tr("Contacts history"),ink,dark,() -> {if(!destroyed&&!busy)showHistory();});add(h,b,dp(UiPolish.ICON_TARGET),dp(UiPolish.ICON_TARGET));}else add(h,new View(this),dp(UiPolish.ICON_TARGET),dp(UiPolish.ICON_TARGET));add(l,h,-1,dp(UiPolish.HEADER_HEIGHT));gap(l,10);}
    private void primary(LinearLayout l,String s,Runnable r){gap(l,10);add(l,button(s,0xffffffff,blue,r),-1,dp(51));}
    private void foot(LinearLayout l,String s){gap(l,9);add(l,center(s,11,gray,false),-1,-2);}
    private void rule(LinearLayout l){View v=new View(this);v.setBackgroundColor(UiPolish.palette(dark).divider);add(l,v,-1,dp(1));}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);}
    private void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_SHORT).show();}
    private void status(String msg){LinearLayout l=root();header(l,"Contacts Cleaner",false);LinearLayout c=col();c.setGravity(Gravity.CENTER);ProgressBar p=new ProgressBar(this);add(c,p,dp(44),dp(44));gap(c,16);add(c,center(msg,15,gray,false),-1,-2);l.addView(c,new LinearLayout.LayoutParams(-1,0,1));setContentView(l);}
    private void askRead(){if(accessPending||busy)return;if(canRead()){scan(false);return;}accessPending=true;if(!ToolAccess.launch(this,ToolAccess.CONTACTS,READ)){accessPending=false;toast("Could not open contact access");}}
    private boolean requireWrite(){if(canWrite())return true;if(accessPending)return false;dialog().setTitle("Allow contact changes").setMessage("Android needs permission to link or remove only contacts you explicitly approve. After granting access, review and confirm again.").setNegativeButton("Cancel",null).setPositiveButton(ToolAccess.action(this,ToolAccess.CONTACT_WRITE),(d,w)->{accessPending=true;if(!ToolAccess.launch(this,ToolAccess.CONTACT_WRITE,WRITE)){accessPending=false;toast("Could not open contact access");}}).show();return false;}
    private void accessReturned(int code){accessPending=false;if(code==READ){if(canRead())scan(false);else{result=null;showHome();toast("Contact access not granted");}}else if(code==WRITE){toast(canWrite()?"Permission granted. Review and confirm again.":"No contacts changed. Permission not granted.");}}
    @Override public void onRequestPermissionsResult(int code,String[] p,int[] results){super.onRequestPermissionsResult(code,p,results);if(code==READ||code==WRITE)accessReturned(code);}
    private void clearSelections(){selectedGroups.clear();selectedContacts.clear();page=0;}
    private void scan(boolean completion){
        if(destroyed||busy)return;clearSelections();result=null;
        if(!canRead()){showHome();return;}busy=true;status("Scanning your contacts…");int token=++epoch;ContactsCleanerEngine job=new ContactsCleanerEngine(this);engine=job;
        work=io.submit(()->{try{ContactsCleanerEngine.Result next=job.scan();runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;engine=null;if(!canRead()){result=null;clearSelections();showHome();return;}result=next;scanTime=System.currentTimeMillis();if(completion)showComplete();else showHome();});}
            catch(Exception e){runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;engine=null;result=null;notice="Could not read contacts. Check access and try again.";if(completion)showComplete();else showHome();});}});
    }
    private void showHome(){
        phase=HOME;LinearLayout l=root();header(l,"Contacts Cleaner",true);LinearLayout b=body(l);
        if(!canRead()){result=null;LinearLayout empty=block();add(empty,center("Organize your contacts",24,ink,true),-1,-2);gap(empty,10);add(empty,center("Allow contact access to find matching entries and missing details. Nothing is changed or uploaded during scanning.",14,gray,false),-1,-2);primary(empty,"Allow contact access",this::askRead);gap(empty,8);add(empty,button("Open Android settings",blue,card,()->{if(accessPending)return;accessPending=true;if(!ToolAccess.openSettings(this,READ)){accessPending=false;toast("Android settings unavailable");}}),-1,dp(44));add(b,empty,-1,-2);}
        else{
            LinearLayout overview=block();LinearLayout top=row();LinearLayout copy=col();add(copy,text("Your contacts",13,gray,false),-1,-2);gap(copy,8);add(copy,text(result==null?"—":String.format(Locale.getDefault(),"%,d",result.contacts.size()),38,ink,true),-1,-2);gap(copy,7);add(copy,text(result==null?"Not scanned":result.attention()+" contacts need review",13,blue,true),-1,-2);weight(top,copy);TextView mark=center("◎",43,blue,true);mark.setBackground(shape(pale,42));add(top,mark,dp(82),dp(82));add(overview,top,-1,-2);gap(overview,14);rule(overview);gap(overview,8);
            LinearLayout bottom=row();weight(bottom,text(result==null?"No scan results":"Scanned "+android.text.format.DateFormat.getTimeFormat(this).format(new Date(scanTime)),12,gray,false));add(bottom,button("Rescan",blue,pale,()->{notice="";scan(false);}),-2,dp(38));add(overview,bottom,-1,-2);add(b,overview,-1,-2);gap(b,17);
            if(result!=null&&result.partial){add(b,text("Partial scan · review only. Changes and backup are disabled.",12,red,false),-1,-2);gap(b,10);}
            if(!notice.isEmpty()){add(b,text(notice,12,gray,false),-1,-2);gap(b,10);}
            add(b,text("Needs attention",20,ink,true),-1,-2);gap(b,9);
            LinearLayout list=block();String[] icons={"◎","☎","Aa","!"};int[] colors={blue,dark?0xffb6a0ff:0xff7657db,dark?0xffffc36b:0xffb77917,red};
            String[] desc={"Review same name and number","Same number, separate entries","Review matching names carefully","Missing names or phone numbers"};
            for(int n=0;n<4;n++){final int c=n;if(n>0)rule(list);LinearLayout r=row();r.setPadding(0,dp(11),0,dp(11));TextView symbol=center(icons[n],22,colors[n],true);symbol.setBackground(shape(pale,12));add(r,symbol,dp(43),dp(43));gap(r,11);LinearLayout labels=col();add(labels,text(ContactsCleanerEngine.LABELS[n],14,ink,true),-1,-2);gap(labels,4);add(labels,text(desc[n],11,gray,false),-1,-2);weight(r,labels);gap(r,6);add(r,text(result==null?"—":Integer.toString(n==3?result.incomplete.size():result.groups.get(n).size()),17,ink,true),-2,-2);gap(r,6);add(r,text("›",22,gray,false),-2,-2);
                r.setFocusable(true);r.setClickable(true);r.setOnClickListener(v->{if(result==null){scan(false);return;}clearSelections();category=c;if(c==3)showIncomplete();else showGroups();});add(list,r,-1,-2);}
            add(b,list,-1,-2);gap(b,13);LinearLayout backup=block();add(backup,text("Backup contacts",16,blue,true),-1,-2);gap(backup,6);add(backup,text("Export standard vCards to a location you choose.",12,gray,false),-1,-2);gap(backup,9);TextView export=button("Export VCF",blue,pale,this::chooseBackup);export.setEnabled(result!=null&&!result.partial&&!result.contacts.isEmpty());export.setAlpha(export.isEnabled()?1f:.4f);add(backup,export,-1,dp(44));add(b,backup,-1,-2);
            foot(b,"Duplicate categories can overlap. Linking does not delete raw contact records.");
        }foot(l,"Private. Secure. Always in your control.");setContentView(l);
    }
    private String initials(String name){String[] parts=name.trim().split("\\s+");if(parts.length==0||parts[0].isEmpty())return "?";String first=new String(Character.toChars(parts[0].codePointAt(0)));String last=parts.length>1?new String(Character.toChars(parts[parts.length-1].codePointAt(0))):"";return (first+last).toUpperCase(Locale.getDefault());}
    private void contactRow(LinearLayout box,ContactsCleanerEngine.Contact c,boolean edit){LinearLayout r=row();TextView avatar=center(initials(c.label()),16,blue,true);avatar.setBackground(shape(pale,24));add(r,avatar,dp(43),dp(43));gap(r,10);LinearLayout copy=col();add(copy,text(c.label(),14,ink,true),-1,-2);gap(copy,4);add(copy,text(c.phones.isEmpty()?(c.emails.isEmpty()?"No phone number":c.emails.iterator().next()):String.join(" · ",c.phones),12,gray,false),-1,-2);gap(copy,3);add(copy,text(c.accounts()+(c.starred?" · Favorite":""),11,gray,false),-1,-2);weight(r,copy);if(edit){gap(r,6);add(r,button("Edit",blue,pale,()->editContact(c)),-2,dp(40));}add(box,r,-1,-2);}
    private void showGroups(){
        if(result==null){showHome();return;}phase=GROUPS;LinearLayout l=root();header(l,ContactsCleanerEngine.LABELS[category],false);LinearLayout b=body(l);List<ContactsCleanerEngine.Group> groups=result.groups.get(category);
        add(b,text(groups.size()+" groups",25,ink,true),-1,-2);gap(b,6);add(b,text(category==ContactsCleanerEngine.NAMES?"Same name is not proof of the same person. Review each entry.":"Matching entries can be linked while keeping account records.",13,gray,false),-1,-2);gap(b,12);
        if(groups.isEmpty()){LinearLayout e=block();add(e,center("No matching groups",18,ink,true),-1,dp(65));add(b,e,-1,-2);}
        else{LinearLayout options=row();weight(options,text("Select groups to review",13,gray,false));add(options,button("Clear",blue,card,()->{selectedGroups.clear();showGroups();}),-2,dp(40));add(b,options,-1,-2);gap(b,8);}
        page=Math.min(page,Math.max(0,(groups.size()-1)/PAGE));int end=Math.min(groups.size(),(page+1)*PAGE);
        for(int n=page*PAGE;n<end;n++){ContactsCleanerEngine.Group g=groups.get(n);LinearLayout tile=block();LinearLayout heading=row();weight(heading,text("GROUP "+(n+1)+" · "+g.reason,11,gray,true));CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(blue));check.setChecked(selectedGroups.containsKey(g.key()));check.setEnabled(!result.partial&&g.linkable());check.setContentDescription("Select group "+(n+1));check.setOnCheckedChangeListener((v,on)->{if(on&&!selectGroup(g)){v.setChecked(false);return;}if(!on)selectedGroups.remove(g.key());refreshCount();});add(heading,check,dp(44),dp(44));add(tile,heading,-1,-2);
            int shown=0;for(ContactsCleanerEngine.Contact c:g.members){if(shown++==4)break;if(shown>1){gap(tile,8);rule(tile);gap(tile,8);}contactRow(tile,c,false);}if(g.members.size()>4){gap(tile,7);add(tile,text("+"+(g.members.size()-4)+" more entries",12,gray,false),-1,-2);}
            gap(tile,10);add(tile,button("Review contact details",blue,pale,()->details(g)),-1,dp(42));if(!g.linkable())foot(tile,"Read-only or large group · review in Contacts");add(b,tile,-1,-2);gap(b,10);}
        pages(b,groups.size(),this::showGroups);if(category!=ContactsCleanerEngine.NAMES&&!result.partial&&!groups.isEmpty()){gap(b,8);add(b,button("Select eligible groups",blue,card,()->{for(ContactsCleanerEngine.Group g:groups){if(selectedGroups.size()>=MAX_GROUPS)break;if(!selectedGroups.containsKey(g.key())&&g.linkable()&&!overlap(g))selectedGroups.put(g.key(),g);}showGroups();}),-1,dp(44));}
        selectionFooter(l,"groups selected","Review & merge",this::showMerge);setContentView(l);refreshCount();
    }
    private boolean overlap(ContactsCleanerEngine.Group g){Set<Long> ids=new HashSet<>();for(ContactsCleanerEngine.Group x:selectedGroups.values())for(ContactsCleanerEngine.Contact c:x.members)ids.add(c.id);for(ContactsCleanerEngine.Contact c:g.members)if(ids.contains(c.id))return true;return false;}
    private boolean selectGroup(ContactsCleanerEngine.Group g){if(result.partial||!g.linkable())return false;if(selectedGroups.containsKey(g.key()))return true;if(selectedGroups.size()>=MAX_GROUPS){toast("Review up to 20 groups at a time");return false;}if(overlap(g)){toast("This group overlaps another selection. Review it separately.");return false;}selectedGroups.put(g.key(),g);return true;}
    private void details(ContactsCleanerEngine.Group g){LinearLayout box=col();box.setPadding(dp(14),dp(8),dp(14),dp(8));int visible=0;for(ContactsCleanerEngine.Contact c:g.members){if(visible++>=ContactsCleanerEngine.MAX_GROUP)break;contactRow(box,c,true);for(Map.Entry<String,Set<String>> e:c.details.entrySet()){gap(box,5);add(box,text(e.getKey()+": "+String.join(" · ",e.getValue()),12,ink,false),-1,-2);}gap(box,12);rule(box);gap(box,10);}if(g.members.size()>ContactsCleanerEngine.MAX_GROUP)add(box,text("Large group: remaining entries must be reviewed in Android Contacts.",13,gray,false),-1,-2);ScrollView scroll=new ScrollView(this);scroll.addView(box);dialog().setTitle(g.reason).setView(scroll).setPositiveButton("Done",null).show();}
    private void pages(LinearLayout b,int total,Runnable render){if(total<=PAGE)return;LinearLayout r=row();TextView prev=button("Previous",blue,card,()->{if(page>0){page--;render.run();}});prev.setEnabled(page>0);add(r,prev,-2,dp(42));weight(r,center((page+1)+" / "+((total+PAGE-1)/PAGE),12,gray,false));TextView next=button("Next",blue,card,()->{if((page+1)*PAGE<total){page++;render.run();}});next.setEnabled((page+1)*PAGE<total);add(r,next,-2,dp(42));add(b,r,-1,-2);}
    private void selectionFooter(LinearLayout l,String unit,String action,Runnable r){gap(l,8);LinearLayout box=block();selectedLabel=text("0 "+unit,14,ink,true);add(box,selectedLabel,-1,-2);gap(box,9);continueButton=button(action,0xffffffff,blue,r);add(box,continueButton,-1,dp(48));add(l,box,-1,-2);}
    private void refreshCount(){int n=phase==GROUPS?selectedGroups.size():selectedContacts.size();selectedLabel.setText(n+(phase==GROUPS?" groups selected":" contacts selected"));continueButton.setEnabled(n>0);continueButton.setAlpha(n>0?1f:.45f);}
    private void showMerge(){
        if(selectedGroups.isEmpty()||result==null||result.partial)return;phase=MERGE;LinearLayout l=root();header(l,"Review merge",false);LinearLayout b=body(l);
        add(b,center(selectedGroups.size()+" groups selected",24,ink,true),-1,-2);gap(b,7);add(b,center("Review the combined details before linking.",13,gray,false),-1,-2);gap(b,15);
        for(ContactsCleanerEngine.Group g:selectedGroups.values()){LinearLayout tile=block();add(tile,text(g.members.get(0).label(),19,ink,true),-1,-2);gap(tile,5);add(tile,text(g.reason+" · "+g.members.size()+" entries",12,gray,false),-1,-2);gap(tile,10);Map<String,Set<String>> combined=new LinkedHashMap<>();Set<String> accounts=new LinkedHashSet<>();
            for(ContactsCleanerEngine.Contact c:g.members){accounts.add(c.accounts());for(Map.Entry<String,Set<String>> e:c.details.entrySet())combined.computeIfAbsent(e.getKey(),k->new LinkedHashSet<>()).addAll(e.getValue());}
            for(Map.Entry<String,Set<String>> e:combined.entrySet()){rule(tile);gap(tile,8);add(tile,text(e.getKey(),11,gray,false),-1,-2);gap(tile,4);for(String value:e.getValue()){add(tile,text(value,14,ink,false),-1,-2);gap(tile,4);}gap(tile,5);}
            add(tile,text("Accounts: "+String.join(" / ",accounts),12,gray,false),-1,-2);gap(tile,8);add(tile,button("Review individual entries",blue,pale,()->details(g)),-1,dp(42));add(b,tile,-1,-2);gap(b,10);}
        LinearLayout safe=block();add(safe,text("Keep original contact records",15,blue,true),-1,-2);gap(safe,7);add(safe,text("Android links matching records into one visible contact. Raw details and account ownership are retained. Names and numbers can belong to different people; only link people you know are the same.",13,gray,false),-1,-2);add(b,safe,-1,-2);
        primary(l,"Confirm merge",()->{if(!requireWrite())return;dialog().setTitle("Link "+selectedGroups.size()+" groups?").setMessage("This changes Android's contact grouping. It does not erase original account records. Confirm that every selected group belongs to the same person.").setNegativeButton("Cancel",null).setPositiveButton("Link contacts",(d,w)->applyLinks()).show();});foot(l,"Only explicitly approved groups will be linked.");setContentView(l);
    }
    private void showIncomplete(){
        if(result==null){showHome();return;}phase=INCOMPLETE;LinearLayout l=root();header(l,"Incomplete contacts",false);LinearLayout b=body(l);add(b,text("Organize your contacts.",24,ink,true),-1,-2);gap(b,6);add(b,text(result.incomplete.size()+" entries with missing names or phone numbers.",13,gray,false),-1,-2);gap(b,12);
        if(result.incomplete.isEmpty()){LinearLayout e=block();add(e,center("No incomplete contacts found",17,ink,true),-1,dp(65));add(b,e,-1,-2);}
        page=Math.min(page,Math.max(0,(result.incomplete.size()-1)/PAGE));int end=Math.min(result.incomplete.size(),(page+1)*PAGE);
        for(int n=page*PAGE;n<end;n++){ContactsCleanerEngine.Contact c=result.incomplete.get(n);LinearLayout tile=block();contactRow(tile,c,true);gap(tile,9);LinearLayout flags=row();weight(flags,text(c.issue(),12,dark?0xffffc36b:0xffad7117,true));CheckBox check=new CheckBox(this);check.setButtonTintList(android.content.res.ColorStateList.valueOf(blue));check.setChecked(selectedContacts.containsKey(c.id));check.setEnabled(c.deletable()&&!result.partial);check.setContentDescription("Select "+c.label()+" for removal");check.setOnCheckedChangeListener((v,on)->{if(on){if(selectedContacts.size()>=ContactsCleanerEngine.MAX_REMOVALS&&!selectedContacts.containsKey(c.id)){toast("Review up to 50 contacts at a time");v.setChecked(false);return;}selectedContacts.put(c.id,c);}else selectedContacts.remove(c.id);refreshCount();});add(flags,check,dp(44),dp(44));add(tile,flags,-1,-2);if(!c.deletable())foot(tile,"Protected, linked or account contact · edit in Contacts");add(b,tile,-1,-2);gap(b,9);}
        pages(b,result.incomplete.size(),this::showIncomplete);selectionFooter(l,"contacts selected","Review selected for removal",this::showRemoval);foot(l,"Missing details do not mean a contact is unnecessary.");setContentView(l);refreshCount();
    }
    private void editContact(ContactsCleanerEngine.Contact c){if(!canRead()||c.uri()==null){toast("Contact no longer available");return;}Intent i=new Intent(Intent.ACTION_EDIT,c.uri());try{startActivityForResult(i,EDIT);}catch(ActivityNotFoundException e){toast("No contact editor available");}}
    private void showRemoval(){
        if(result==null||result.partial||selectedContacts.isEmpty())return;phase=REMOVE;LinearLayout l=root();header(l,"Review removal",false);LinearLayout b=body(l);
        add(b,text(selectedContacts.size()+" selected contacts",23,ink,true),-1,-2);gap(b,7);add(b,text("Only unstarred, single-record phone contacts qualify. Deletion may be permanent. Back up contacts first if needed.",13,gray,false),-1,-2);gap(b,14);
        for(ContactsCleanerEngine.Contact c:new ArrayList<>(selectedContacts.values())){LinearLayout tile=block();contactRow(tile,c,false);for(Map.Entry<String,Set<String>> e:c.details.entrySet()){gap(tile,5);add(tile,text(e.getKey()+": "+String.join(" · ",e.getValue()),12,ink,false),-1,-2);}gap(tile,8);add(tile,button("Keep contact",blue,pale,()->{selectedContacts.remove(c.id);if(selectedContacts.isEmpty())showIncomplete();else showRemoval();}),-1,dp(42));add(b,tile,-1,-2);gap(b,9);}
        gap(l,8);add(l,button("Remove "+selectedContacts.size()+" contacts",0xffffffff,red,()->{if(!requireWrite())return;dialog().setTitle("Permanently remove selected contacts?").setMessage("This removes "+selectedContacts.size()+" phone contacts and their stored details. It cannot be undone inside Clenova.").setNegativeButton("Keep contacts",null).setPositiveButton("Remove",(d,w)->applyRemoval()).show();}),-1,dp(51));foot(l,"Nothing is removed without your confirmation.");setContentView(l);
    }
    private void applyLinks(){
        if(busy||!foreground||!canRead()||!canWrite()||result==null||result.partial||selectedGroups.isEmpty())return;
        List<ContactsCleanerEngine.Group> groups=new ArrayList<>(selectedGroups.values());busy=true;status("Linking approved contacts…");final int token=++epoch;ContactsCleanerEngine job=new ContactsCleanerEngine(this);engine=job;
        work=io.submit(()->{int ok=0,bad=0;for(ContactsCleanerEngine.Group g:groups){if(Thread.currentThread().isInterrupted())break;try{if(job.link(g))ok++;else bad++;}catch(Exception e){bad++;}}final int success=ok,unknown=bad;if(success>0)history.add(null,"contacts","Contacts linked",success+" verified linked groups",0,success,false);runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;engine=null;linked=success;removed=0;unverified=unknown;notice="";scan(true);});});
    }
    private void applyRemoval(){
        if(busy||!foreground||!canRead()||!canWrite()||result==null||result.partial||selectedContacts.isEmpty())return;
        List<ContactsCleanerEngine.Contact> contacts=new ArrayList<>(selectedContacts.values());busy=true;status("Removing approved phone contacts…");final int token=++epoch;ContactsCleanerEngine job=new ContactsCleanerEngine(this);engine=job;
        work=io.submit(()->{int n=0;try{n=job.remove(contacts);}catch(Exception ignored){}final int verified=n;if(verified>0)history.add(null,"contacts","Phone contacts removed",verified+" verified removed records",0,verified,false);runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;engine=null;linked=0;removed=verified;unverified=contacts.size()-verified;notice="";scan(true);});});
    }
    private void showComplete(){
        phase=COMPLETE;LinearLayout l=root();header(l,"Contact review complete",false);LinearLayout b=body(l);LinearLayout hero=col();hero.setGravity(Gravity.CENTER);gap(hero,10);TextView mark=center(unverified==0?"✓":"!",43,0xffffffff,true);mark.setBackground(shape(unverified==0?0xff34c759:gray,50));add(hero,mark,dp(86),dp(86));gap(hero,14);add(hero,center(unverified==0?"Changes verified!":"Review finished",25,ink,true),-1,-2);gap(hero,6);add(hero,center(unverified==0?"Your approved changes have been checked.":"Some changes could not be verified. Rescan before retrying.",13,gray,false),-1,-2);add(b,hero,-1,-2);gap(b,20);
        LinearLayout stats=block();LinearLayout top=row();LinearLayout a=col(),c=col();add(a,center(Integer.toString(linked),31,blue,true),-1,-2);gap(a,5);add(a,center("Groups linked",12,gray,false),-1,-2);add(c,center(Integer.toString(removed),31,ink,true),-1,-2);gap(c,5);add(c,center("Records removed",12,gray,false),-1,-2);weight(top,a);weight(top,c);add(stats,top,-1,-2);gap(stats,15);rule(stats);gap(stats,14);LinearLayout remain=row();weight(remain,text("Contacts remaining",13,gray,false));add(remain,text(result==null||result.partial?"Unknown":Integer.toString(result.contacts.size()),17,ink,true),-2,-2);add(stats,remain,-1,-2);if(unverified>0){gap(stats,10);add(stats,text(unverified+" changes unverified or unsuccessful",12,red,false),-1,-2);}add(b,stats,-1,-2);primary(l,"Done",this::showHome);setContentView(l);
    }
    private void chooseBackup(){
        if(result==null||result.partial||result.contacts.isEmpty()||!canRead())return;
        dialog().setTitle("Export contacts?").setMessage("The VCF contains personal contact information. Choose a trusted location. Standard vCards may not preserve every account-specific field or linking rule.").setNegativeButton("Cancel",null).setPositiveButton("Choose location",(d,w)->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/x-vcard");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"Clenova_Contacts_"+System.currentTimeMillis()+".vcf");try{startActivityForResult(i,BACKUP);}catch(ActivityNotFoundException e){toast("No document picker available");}}).show();
    }
    private void backup(Uri uri){if(busy||!canRead())return;busy=true;status("Exporting contacts…");final int token=++epoch;ContactsCleanerEngine job=new ContactsCleanerEngine(this);engine=job;
        work=io.submit(()->{int count=0;boolean ok=false;try{ContactsCleanerEngine.Result fresh=job.scan();count=job.backup(fresh,uri);ok=true;}catch(Exception ignored){}final int n=count;final boolean success=ok;if(success)history.add(null,"contacts","Contact backup exported",n+" vCards exported",0,n,false);runOnUiThread(()->{if(destroyed||token!=epoch)return;busy=false;engine=null;showHome();dialog().setTitle(success?"Backup exported":"Backup could not finish").setMessage(success?n+" contacts exported to your chosen VCF file. Keep it private.":"The chosen file may contain an incomplete export. Check or remove it in your file manager. No contacts were changed.").setPositiveButton("Done",null).show();});});
    }
    private void showHistory(){if(busy)return;phase=HISTORY;LinearLayout l=root();header(l,"Contacts history",false);LinearLayout b=body(l);int count=0;for(ActivityHistory.Entry e:history.entries())if("contacts".equals(e.category)){LinearLayout tile=block();add(tile,text(e.title,16,ink,true),-1,-2);gap(tile,6);add(tile,text(e.detail,13,gray,false),-1,-2);gap(tile,5);add(tile,text(android.text.format.DateFormat.getMediumDateFormat(this).format(new Date(e.timestamp)),12,gray,false),-1,-2);add(b,tile,-1,-2);gap(b,9);count++;}if(count==0){LinearLayout e=block();add(e,center("No contact activity yet",18,ink,true),-1,dp(48));add(e,center("Approved changes and completed exports appear here.",12,gray,false),-1,-2);add(b,e,-1,-2);}foot(l,"History stores counts, not contact names or numbers.");setContentView(l);}
    @Override protected void onActivityResult(int code,int status,Intent data){super.onActivityResult(code,status,data);if(code==READ||code==WRITE){accessReturned(code);return;}if(code==BACKUP&&status==RESULT_OK&&data!=null&&data.getData()!=null)backup(data.getData());if(code==EDIT){notice="";scan(false);}}
    @Override public void onBackPressed(){if(busy){toast("Current operation is finishing");return;}if(phase==MERGE){showGroups();return;}if(phase==REMOVE){showIncomplete();return;}if(phase!=HOME){clearSelections();showHome();return;}finish();}
    @Override protected void onResume(){super.onResume();foreground=true;if(accessPending)return;if(!busy&&!canRead()&&result!=null){result=null;clearSelections();showHome();}else if(!busy&&canRead()&&result==null&&phase==HOME&&notice.isEmpty())scan(false);}
    @Override protected void onPause(){foreground=false;super.onPause();}
    @Override protected void onDestroy(){destroyed=true;epoch++;if(engine!=null)engine.cancel();if(work!=null)work.cancel(true);io.shutdownNow();super.onDestroy();}
}

package com.clenova.app;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.provider.ContactsContract.CommonDataKinds.*;
import android.provider.OpenableColumns;
import android.telephony.PhoneNumberUtils;
import java.io.*;
import java.util.*;

/** Read-only discovery and explicitly approved, version-checked native contact operations. */
final class ContactsCleanerEngine {
    static final int MAX_CONTACTS=10000,MAX_ROWS=150000,MAX_GROUP=20,MAX_REMOVALS=50;
    static final int DUPLICATES=0,NUMBERS=1,NAMES=2,INCOMPLETE=3;
    static final String[] LABELS={"Duplicate contacts","Duplicate numbers","Duplicate names","Incomplete contacts"};
    static final class Raw {
        final long id,contact,version;final String account,type,dataset;final boolean readonly;
        Raw(long i,long c,long v,String a,String t,String d,boolean ro){id=i;contact=c;version=v;account=a;type=t;dataset=d;readonly=ro;}
        boolean local(){return !readonly&&account==null&&type==null&&dataset==null;}
        String label(){return account==null&&type==null?"Phone":(type==null?"Account":type)+(account==null?"":" · "+account);}
    }
    static final class Contact {
        final long id;final String lookup,name;final boolean starred;
        final List<Raw> raw=new ArrayList<>();
        final Set<String> phones=new LinkedHashSet<>(),numbers=new LinkedHashSet<>(),emails=new LinkedHashSet<>(),names=new LinkedHashSet<>(),companies=new LinkedHashSet<>();
        final Map<String,Set<String>> details=new LinkedHashMap<>();
        boolean nonNameData;
        Contact(long i,String l,String n,boolean s){id=i;lookup=l;name=n==null?"":n;starred=s;}
        String label(){return name.trim().isEmpty()?"Unknown contact":name;}
        String issue(){return names.isEmpty()&&phones.isEmpty()&&emails.isEmpty()?"Missing name and number":names.isEmpty()?"Missing name":phones.isEmpty()?"Missing phone number":"";}
        boolean deletable(){return !starred&&raw.size()==1&&raw.get(0).local();}
        String accounts(){LinkedHashSet<String> a=new LinkedHashSet<>();for(Raw r:raw)a.add(r.label());return String.join(" / ",a);}
        Uri uri(){return Contacts.getLookupUri(id,lookup);}
        void detail(String label,String value){if(value!=null&&!value.trim().isEmpty())details.computeIfAbsent(label,k->new LinkedHashSet<>()).add(value);}
    }
    static final class Group {
        final String reason;final List<Contact> members;
        Group(String r,List<Contact> m){reason=r;members=new ArrayList<>(m);}
        String key(){StringBuilder b=new StringBuilder();for(Contact c:members)b.append(c.id).append(':');return b.toString();}
        boolean linkable(){if(members.size()<2||members.size()>MAX_GROUP)return false;int raws=0;for(Contact c:members){if(c.raw.isEmpty())return false;for(Raw r:c.raw){if(r.readonly)return false;raws++;}}return raws<=MAX_GROUP;}
    }
    static final class Result {
        final Map<Long,Contact> contacts=new LinkedHashMap<>();
        final List<List<Group>> groups=Arrays.asList(new ArrayList<>(),new ArrayList<>(),new ArrayList<>());
        final List<Contact> incomplete=new ArrayList<>();boolean partial;
        int attention(){Set<Long> ids=new HashSet<>();for(List<Group> groupsOfType:groups)for(Group g:groupsOfType)for(Contact c:g.members)ids.add(c.id);for(Contact c:incomplete)ids.add(c.id);return ids.size();}
    }
    private final ContentResolver resolver;private final CancellationSignal cancel=new CancellationSignal();
    ContactsCleanerEngine(Context c){resolver=c.getContentResolver();}
    void cancel(){cancel.cancel();}
    private void check(){cancel.throwIfCanceled();if(Thread.currentThread().isInterrupted())throw new android.os.OperationCanceledException();}
    private static String normName(String name){return name==null?"":name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
    static String numberKey(String value,String normalized){
        String candidate=normalized==null||normalized.isEmpty()?value:normalized;
        if(candidate==null||candidate.matches(".*[,;*#].*"))return "";
        // Never compare only a suffix or assume a country code for a local number.
        String key=PhoneNumberUtils.normalizeNumber(candidate);int digits=key.startsWith("+")?key.length()-1:key.length();
        return digits>=7?key:"";
    }
    Result scan(){
        Result out=new Result();
        try(Cursor c=resolver.query(Contacts.CONTENT_URI,new String[]{Contacts._ID,Contacts.LOOKUP_KEY,Contacts.DISPLAY_NAME_PRIMARY,Contacts.STARRED},null,null,Contacts._ID+" ASC",cancel)){
            if(c==null)throw new IllegalStateException("Contacts unavailable");
            while(c.moveToNext()){check();if(out.contacts.size()>=MAX_CONTACTS){out.partial=true;break;}long id=c.getLong(0);out.contacts.put(id,new Contact(id,c.getString(1),c.getString(2),c.getInt(3)!=0));}
        }
        try(Cursor c=resolver.query(RawContacts.CONTENT_URI,new String[]{RawContacts._ID,RawContacts.CONTACT_ID,RawContacts.VERSION,RawContacts.ACCOUNT_NAME,RawContacts.ACCOUNT_TYPE,RawContacts.DATA_SET,RawContacts.RAW_CONTACT_IS_READ_ONLY},RawContacts.DELETED+"=0",null,RawContacts._ID+" ASC",cancel)){
            if(c==null)throw new IllegalStateException("Accounts unavailable");int rows=0;
            while(c.moveToNext()){check();if(++rows>MAX_ROWS){out.partial=true;break;}Contact x=out.contacts.get(c.getLong(1));if(x!=null)x.raw.add(new Raw(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getString(4),c.getString(5),c.getInt(6)!=0));}
        }
        try(Cursor c=resolver.query(Data.CONTENT_URI,new String[]{Data.CONTACT_ID,Data.MIMETYPE,Data.DATA1,Data.DATA2,Data.DATA3,Data.DATA4,Data.DATA5,Data.DATA6},null,null,Data._ID+" ASC",cancel)){
            if(c==null)throw new IllegalStateException("Contact details unavailable");int rows=0;
            while(c.moveToNext()){check();if(++rows>MAX_ROWS){out.partial=true;break;}Contact x=out.contacts.get(c.getLong(0));if(x==null)continue;String mime=c.getString(1),value=c.getString(2);
                if(StructuredName.CONTENT_ITEM_TYPE.equals(mime)){if(value!=null&&!value.trim().isEmpty()){x.names.add(value);x.detail("Name",value);}continue;}
                x.nonNameData=true;
                if(Phone.CONTENT_ITEM_TYPE.equals(mime)&&value!=null&&!value.trim().isEmpty()){x.phones.add(value);String key=numberKey(value,c.getString(5));if(!key.isEmpty())x.numbers.add(key);x.detail("Phone",value);}
                else if(Email.CONTENT_ITEM_TYPE.equals(mime)&&value!=null&&!value.trim().isEmpty()){x.emails.add(value);x.detail("Email",value);}
                else if(Organization.CONTENT_ITEM_TYPE.equals(mime)){if(value!=null&&!value.isEmpty())x.companies.add(value);x.detail("Company",value);x.detail("Job title",c.getString(5));}
                else if(StructuredPostal.CONTENT_ITEM_TYPE.equals(mime))x.detail("Address",value);
                else if(Website.CONTENT_ITEM_TYPE.equals(mime))x.detail("Website",value);
                else if(Note.CONTENT_ITEM_TYPE.equals(mime))x.detail("Note",value);
                else if(Event.CONTENT_ITEM_TYPE.equals(mime))x.detail("Date",value);
                else if(Nickname.CONTENT_ITEM_TYPE.equals(mime))x.detail("Nickname",value);
            }
        }
        Map<String,List<Contact>> exact=new LinkedHashMap<>(),numbers=new LinkedHashMap<>(),names=new LinkedHashMap<>();
        for(Contact c:out.contacts.values()){check();String name=normName(c.names.isEmpty()?"":c.name);
            for(String n:c.numbers){numbers.computeIfAbsent(n,k->new ArrayList<>()).add(c);if(!name.isEmpty())exact.computeIfAbsent(name+"\u001f"+n,k->new ArrayList<>()).add(c);}
            if(!name.isEmpty())names.computeIfAbsent(name,k->new ArrayList<>()).add(c);
            if(!c.issue().isEmpty())out.incomplete.add(c);
        }
        groups(exact,"Same name and number",out.groups.get(DUPLICATES));groups(numbers,"Same phone number",out.groups.get(NUMBERS));groups(names,"Same name only",out.groups.get(NAMES));
        out.incomplete.sort(Comparator.comparing(Contact::label,String.CASE_INSENSITIVE_ORDER).thenComparingLong(c->c.id));return out;
    }
    private static void groups(Map<String,List<Contact>> source,String reason,List<Group> target){
        Set<String> seen=new HashSet<>();for(List<Contact> list:source.values())if(list.size()>1){Group g=new Group(reason,list);if(seen.add(g.key()))target.add(g);}
        target.sort(Comparator.comparing((Group g)->g.members.get(0).label(),String.CASE_INSENSITIVE_ORDER).thenComparing(Group::key));
    }
    private static String ids(List<Raw> raw){StringBuilder b=new StringBuilder();for(Raw r:raw){if(b.length()>0)b.append(',');b.append(r.id);}return b.toString();}
    private void assertContact(ArrayList<ContentProviderOperation> ops,Contact c){
        ops.add(ContentProviderOperation.newAssertQuery(RawContacts.CONTENT_URI).withSelection(RawContacts.CONTACT_ID+"=? AND "+RawContacts.DELETED+"=0",new String[]{Long.toString(c.id)}).withExpectedCount(c.raw.size()).build());
        for(Raw r:c.raw)ops.add(ContentProviderOperation.newAssertQuery(ContentUris.withAppendedId(RawContacts.CONTENT_URI,r.id))
            .withValue(RawContacts.VERSION,r.version).withValue(RawContacts.CONTACT_ID,r.contact).withValue(RawContacts.DELETED,0).withValue(RawContacts.RAW_CONTACT_IS_READ_ONLY,0).withExpectedCount(1).build());
        ops.add(ContentProviderOperation.newAssertQuery(ContentUris.withAppendedId(Contacts.CONTENT_URI,c.id)).withValue(Contacts.STARRED,c.starred?1:0).withExpectedCount(1).build());
    }
    /** Non-destructive Android aggregation: preserve each raw contact and its account. */
    boolean link(Group group)throws Exception{
        if(!group.linkable())throw new IllegalArgumentException("Group is read-only");
        check();ArrayList<ContentProviderOperation> ops=new ArrayList<>();List<Raw> all=new ArrayList<>();
        for(Contact c:group.members){assertContact(ops,c);all.addAll(c.raw);}
        // All pairs maintain explicit links even where accounts or names differ.
        for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++){
            if(all.get(i).contact==all.get(j).contact)continue;
            ops.add(ContentProviderOperation.newUpdate(AggregationExceptions.CONTENT_URI)
                .withValue(AggregationExceptions.TYPE,AggregationExceptions.TYPE_KEEP_TOGETHER)
                .withValue(AggregationExceptions.RAW_CONTACT_ID1,all.get(i).id)
                .withValue(AggregationExceptions.RAW_CONTACT_ID2,all.get(j).id).build());
        }
        check();resolver.applyBatch(ContactsContract.AUTHORITY,ops);
        for(int attempt=0;attempt<5;attempt++){check();Set<Long> contactIds=new HashSet<>();int count=0;
            try(Cursor c=resolver.query(RawContacts.CONTENT_URI,new String[]{RawContacts.CONTACT_ID},RawContacts._ID+" IN ("+ids(all)+") AND "+RawContacts.DELETED+"=0",null,null,cancel)){
                if(c==null)throw new IOException("Could not verify link");while(c.moveToNext()){count++;contactIds.add(c.getLong(0));}}
            if(count==all.size()&&contactIds.size()==1&&!contactIds.contains(0L))return true;
            Thread.sleep(200);
        }return false;
    }
    /** Direct deletion only for unstarred single-record local contacts, never synced records. */
    int remove(List<Contact> contacts)throws Exception{
        if(contacts.isEmpty()||contacts.size()>MAX_REMOVALS)throw new IllegalArgumentException("Invalid selection");
        ArrayList<ContentProviderOperation> ops=new ArrayList<>();Set<Long> ids=new HashSet<>();
        for(Contact c:contacts){if(!c.deletable()||!ids.add(c.id))throw new IllegalArgumentException("Contact cannot be safely removed");assertContact(ops,c);}
        for(Contact c:contacts)ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI,c.raw.get(0).id)).withExpectedCount(1).build());
        check();resolver.applyBatch(ContactsContract.AUTHORITY,ops);
        int removed=0;for(Contact c:contacts){try(Cursor cur=resolver.query(RawContacts.CONTENT_URI,new String[]{RawContacts._ID},RawContacts._ID+"=? AND "+RawContacts.DELETED+"=0",new String[]{Long.toString(c.raw.get(0).id)},null,cancel)){if(cur!=null&&!cur.moveToFirst())removed++;}}
        return removed;
    }
    /** Write provider-generated vCards to a new document. Never overwrite an existing backup. */
    int backup(Result snapshot,Uri destination)throws Exception{
        if(snapshot.partial||snapshot.contacts.isEmpty()||destination==null||!"content".equals(destination.getScheme()))throw new IOException("Complete contact scan and new backup file required");
        long size=-1;try(Cursor c=resolver.query(destination,new String[]{OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst()&&!c.isNull(0))size=c.getLong(0);}
        if(size!=0)throw new IOException("Choose a new empty backup file");
        int count=0;long total=0;
        try(OutputStream stream=resolver.openOutputStream(destination,"w")){
            if(stream==null)throw new IOException("Cannot open backup");
            byte[] buffer=new byte[65536];
            for(Contact x:snapshot.contacts.values()){check();if(x.lookup==null||x.lookup.isEmpty())throw new IOException("Missing contact key");
                Uri vcard=Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI,Uri.encode(x.lookup));
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=resolver.openInputStream(vcard)){if(in==null)throw new IOException("Cannot export contact");int n;while((n=in.read(buffer))!=-1){check();if(bytes.size()+n>8*1024*1024)throw new IOException("Contact backup too large");bytes.write(buffer,0,n);}}
                byte[] data=bytes.toByteArray();String value=new String(data,java.nio.charset.StandardCharsets.UTF_8).trim();if(!value.startsWith("BEGIN:VCARD")||!value.endsWith("END:VCARD"))throw new IOException("Invalid provider backup");
                total+=data.length+2;if(total>256L*1024*1024)throw new IOException("Backup exceeds 256 MB");
                stream.write(data);stream.write('\r');stream.write('\n');count++;
            }stream.flush();
        }return count;
    }
}

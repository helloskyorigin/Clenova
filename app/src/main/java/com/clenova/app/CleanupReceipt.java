package com.clenova.app;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import java.util.*;

/** Post-approval observation, never equates approval or inaccessible rows with success. */
final class CleanupReceipt {
    final String id,label;
    private final ArrayList<String> uris,versions;
    private final long[] sizes,generations;
    private CleanupReceipt(String id,String label,ArrayList<String> uris,ArrayList<String> versions,long[] sizes,long[] generations){
        this.id=id;this.label=label;this.uris=uris;this.versions=versions;this.sizes=sizes;this.generations=generations;
    }
    static CleanupReceipt capture(Context c,String label,List<Uri> approved,List<SmartMediaScan.Item> items){
        if(Build.VERSION.SDK_INT<30||approved.isEmpty()||approved.size()>100)throw new IllegalArgumentException("Invalid batch");
        Map<String,SmartMediaScan.Item> map=new HashMap<>();for(SmartMediaScan.Item item:items)map.put(item.uri.toString(),item);
        ArrayList<String> uris=new ArrayList<>(),versions=new ArrayList<>();long[] sizes=new long[approved.size()],generations=new long[approved.size()];Map<String,String> volumeVersions=new HashMap<>();
        for(int i=0;i<approved.size();i++){
            String key=approved.get(i).toString();SmartMediaScan.Item item=map.get(key);
            if(item==null||item.volume==null||item.generation<=0||item.bytes<=0||uris.contains(key))throw new IllegalStateException("Approval identity unavailable");
            String version=volumeVersions.get(item.volume);
            if(version==null){version=MediaStore.getVersion(c,item.volume);volumeVersions.put(item.volume,version);}
            if(version==null||version.isEmpty())throw new IllegalStateException("Media version unavailable");
            uris.add(key);versions.add(version);sizes[i]=item.bytes;generations[i]=item.generation;
        }
        return new CleanupReceipt(UUID.randomUUID().toString(),label,uris,versions,sizes,generations);
    }
    Bundle save(){Bundle b=new Bundle();b.putString("id",id);b.putString("label",label);b.putStringArrayList("uris",uris);b.putStringArrayList("versions",versions);b.putLongArray("sizes",sizes);b.putLongArray("generations",generations);return b;}
    static CleanupReceipt restore(Bundle state){
        if(state==null)return null;ArrayList<String> u=state.getStringArrayList("uris"),v=state.getStringArrayList("versions");long[] s=state.getLongArray("sizes"),g=state.getLongArray("generations");
        String id=state.getString("id"),label=state.getString("label");
        if(id==null||label==null||u==null||v==null||s==null||g==null||u.isEmpty()||u.size()>100||u.size()!=v.size()||u.size()!=s.length||u.size()!=g.length)return null;
        return new CleanupReceipt(id,label,u,v,s,g);
    }
    static final class Result{
        final int requested;final boolean approved;final Set<String> confirmed=new HashSet<>();long bytes;boolean logged=true;
        Result(int requested,boolean approved){this.requested=requested;this.approved=approved;}
        int count(){return confirmed.size();}
        boolean complete(){return approved&&requested>0&&count()==requested;}
        String title(){return !approved?"Cleanup cancelled":complete()?"Cleanup complete":count()>0?"Cleanup partially verified":"Cleanup not verified";}
        String detail(){return !approved?"Android approval was not completed. No cleanup is claimed.":requested==0?"Approval details unavailable. No cleanup is claimed.":count()+" of "+requested+" items confirmed in Android Trash. "+(count()<requested?"Remaining items could not be verified. ":"")+"Storage recovery is not measured.";}
    }
    interface Done{void finish(Result result);}
    static void verifyAsync(Activity host,CleanupReceipt receipt,boolean approved,Done done){
        Context context=host.getApplicationContext();
        new Thread(()->{
            Result result=receipt==null?new Result(0,approved):receipt.verify(context,approved);
            if(receipt!=null&&result.count()>0)result.logged=new ActivityHistory(context).add(receipt.id,"cleanup",receipt.label+" moved to Trash",result.detail(),0,result.count(),false);
            host.runOnUiThread(()->{if(!host.isDestroyed()&&!host.isFinishing())done.finish(result);});
        },"Clenova-cleanup-receipt").start();
    }
    private Result verify(Context c,boolean approved){
        Result out=new Result(uris.size(),approved);if(!approved||Build.VERSION.SDK_INT<30)return out;
        Bundle args=new Bundle();args.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED,MediaStore.MATCH_INCLUDE);
        for(int i=0;i<uris.size();i++){
            Uri uri=Uri.parse(uris.get(i));try{
                List<String> parts=uri.getPathSegments();if(!"media".equals(uri.getAuthority())||parts.size()<3||!versions.get(i).equals(MediaStore.getVersion(c,parts.get(0))))continue;
                try(Cursor cursor=c.getContentResolver().query(uri,new String[]{"_id","_size","is_trashed","generation_modified"},args,null)){
                    if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0)&&!cursor.isNull(1)&&!cursor.isNull(2)&&!cursor.isNull(3)
                        &&Long.toString(cursor.getLong(0)).equals(uri.getLastPathSegment())&&cursor.getLong(1)==sizes[i]&&cursor.getInt(2)==1
                        &&cursor.getLong(3)>=generations[i]&&versions.get(i).equals(MediaStore.getVersion(c,parts.get(0)))){
                        out.confirmed.add(uris.get(i));out.bytes+=sizes[i];
                    }
                }
            }catch(RuntimeException ignored){}
        }
        return out;
    }
}

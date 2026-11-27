package com.clenova.app;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import java.io.InputStream;
import java.util.*;

/** Conservative local visual similarity heuristic, not a semantic AI model. */
final class SimilarPhotoScan {
    static final int LIMIT=600, MAX_GROUP=6;
    static final long WINDOW=120000L;
    static final class Signature {
        final long hash;final int red,green,blue,width,height;final double quality;final int pixelFingerprint;
        Signature(long h,int r,int g,int b,int w,int he,double q,int fp){hash=h;red=r;green=g;blue=b;width=w;height=he;quality=q;pixelFingerprint=fp;}
    }
    static final class Group {
        final List<SmartMediaScan.Item> items=new ArrayList<>();
        final Map<String,Signature> signatures=new HashMap<>();
        SmartMediaScan.Item best;
    }
    static final class Match {
        final SmartMediaScan.Item item;final Signature signature;
        Match(SmartMediaScan.Item i,Signature s){item=i;signature=s;}
    }
    static boolean close(Signature a,Signature b){
        return Long.bitCount(a.hash^b.hash)<=5
            && Math.abs(a.red-b.red)+Math.abs(a.green-b.green)+Math.abs(a.blue-b.blue)<=36
            && Math.abs(Math.log((a.width/(double)a.height)/(b.width/(double)b.height)))<=0.10;
    }
    static Signature signature(ContentResolver resolver,SmartMediaScan.Item item,CancellationSignal signal)throws Exception{
        signal.throwIfCanceled();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
        try(InputStream in=resolver.openInputStream(item.uri)){if(in==null)return null;BitmapFactory.decodeStream(in,null,opts);}
        int width=opts.outWidth,height=opts.outHeight;if(width<=0||height<=0)return null;
        int factor=1;while(Math.max(width,height)/factor>256)factor*=2;
        opts.inSampleSize=factor;opts.inJustDecodeBounds=false;
        Bitmap image;try(InputStream in=resolver.openInputStream(item.uri)){if(in==null)return null;image=BitmapFactory.decodeStream(in,null,opts);}
        if(image==null)return null;
        Bitmap sample=null;try{
            signal.throwIfCanceled();sample=Bitmap.createScaledBitmap(image,32,32,true);
            int[] pixels=new int[1024];sample.getPixels(pixels,0,32,0,0,32,32);
            int[] gray=new int[1024];int r=0,g=0,b=0,l=0;
            for(int i=0;i<pixels.length;i++){int c=pixels[i],rr=(c>>16)&255,gg=(c>>8)&255,bb=c&255;r+=rr;g+=gg;b+=bb;gray[i]=(77*rr+150*gg+29*bb)>>8;l+=gray[i];}
            long hash=0;for(int y=0;y<8;y++)for(int x=0;x<8;x++){
                int left=gray[(y*4+1)*32+x*3],right=gray[(y*4+1)*32+x*3+3];
                if(left>right)hash|=1L<<(y*8+x);
            }
            double edge=0;for(int y=1;y<31;y++)for(int x=1;x<31;x++){int i=y*32+x;edge+=Math.abs(4*gray[i]-gray[i-1]-gray[i+1]-gray[i-32]-gray[i+32]);}
            if(edge/900d<4)return null; // Flat images do not provide a reliable visual signature.
            double mean=l/1024d;double quality=edge/900d-0.18*Math.abs(mean-128)+2*Math.log1p((double)width*height/1000000);
            return new Signature(hash,r/1024,g/1024,b/1024,width,height,quality,Arrays.hashCode(pixels));
        }finally{if(sample!=null&&sample!=image)sample.recycle();image.recycle();}
    }
    static List<Group> run(ContentResolver resolver,List<SmartMediaScan.Item> input,CancellationSignal cancel,SmartMediaScan.Result result,SmartMediaScan.Progress progress){
        List<SmartMediaScan.Item> sorted=new ArrayList<>(input);
        sorted.sort(Comparator.comparingLong((SmartMediaScan.Item i)->i.modified).reversed().thenComparing(i->i.uri.toString()));
        if(sorted.size()>LIMIT){result.similarPartial=true;sorted=new ArrayList<>(sorted.subList(0,LIMIT));}
        List<Match> matches=new ArrayList<>();int done=0;progress.update(3,0,sorted.size());
        for(SmartMediaScan.Item item:sorted){cancel.throwIfCanceled();try{
            Signature sig=signature(resolver,item,cancel);if(sig!=null)matches.add(new Match(item,sig));else result.similarPartial=true;
        }catch(OperationCanceledException e){throw e;}catch(Exception e){result.similarPartial=true;}progress.update(3,++done,sorted.size());}
        List<Group> groups=new ArrayList<>();Set<String> used=new HashSet<>();
        for(int i=0;i<matches.size();i++){
            cancel.throwIfCanceled();Match anchor=matches.get(i);if(used.contains(anchor.item.uri.toString())||anchor.item.modified<=0)continue;
            List<Match> cluster=new ArrayList<>();cluster.add(anchor);
            for(int j=i+1;j<matches.size()&&cluster.size()<MAX_GROUP;j++){
                Match candidate=matches.get(j);if(anchor.item.modified-candidate.item.modified>WINDOW)break;
                if(used.contains(candidate.item.uri.toString())||candidate.item.modified<=0||!Objects.equals(anchor.item.path,candidate.item.path)||anchor.item.path==null||!Objects.equals(anchor.item.volume,candidate.item.volume))continue;
                boolean fits=true;for(Match existing:cluster)if(!close(existing.signature,candidate.signature)){fits=false;break;}
                if(fits)cluster.add(candidate);
            }
            if(cluster.size()<2)continue;
            // Always keep the strongest quality heuristic; existing exclusions remain locked.
            cluster.sort(Comparator.comparingDouble((Match m)->m.signature.quality).reversed().thenComparing(m->m.item.uri.toString()));
            Group group=new Group();group.best=cluster.get(0).item;
            for(Match m:cluster){used.add(m.item.uri.toString());group.items.add(m.item);group.signatures.put(m.item.uri.toString(),m.signature);}
            groups.add(group);
        }
        return groups;
    }
    static boolean candidate(Group g,SmartMediaScan.Item i){return i!=g.best&&!i.protectedItem;}
    private static boolean sameSignature(Signature a,Signature b){return a!=null&&b!=null&&a.hash==b.hash&&a.pixelFingerprint==b.pixelFingerprint&&a.width==b.width&&a.height==b.height&&a.red==b.red&&a.green==b.green&&a.blue==b.blue;}
    static List<android.net.Uri> verify(SmartMediaScan.Result original,SmartMediaScan.Result fresh,Set<String> selected,Set<String> keep){
        if(selected.isEmpty()||selected.size()>100)return Collections.emptyList();
        List<android.net.Uri> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Group g:original.similarGroups)for(SmartMediaScan.Item item:g.items)if(selected.contains(item.uri.toString())){
            String id=item.uri.toString();if(!candidate(g,item)||keep.contains(id))return Collections.emptyList();
            boolean valid=false;
            for(Group current:fresh.similarGroups){
                SmartMediaScan.Item target=null,retained=null;
                for(SmartMediaScan.Item i:current.items){if(i.uri.equals(item.uri))target=i;if(i.uri.equals(g.best.uri))retained=i;}
                if(target!=null&&retained!=null&&candidate(current,target)&&!selected.contains(retained.uri.toString())
                    &&target.bytes==item.bytes&&target.modified==item.modified&&item.generation>0&&target.generation==item.generation&&retained.bytes==g.best.bytes&&retained.modified==g.best.modified&&g.best.generation>0&&retained.generation==g.best.generation
                    &&sameSignature(g.signatures.get(id),current.signatures.get(id))
                    &&sameSignature(g.signatures.get(g.best.uri.toString()),current.signatures.get(g.best.uri.toString()))){valid=true;break;}
            }
            if(!valid||!seen.add(id))return Collections.emptyList();out.add(item.uri);
        }
        return out.size()==selected.size()?out:Collections.emptyList();
    }
}

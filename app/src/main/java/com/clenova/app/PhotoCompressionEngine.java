package com.clenova.app;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Bounded, sequential, local image compression. Sources are always opened read-only. */
final class PhotoCompressionEngine {
    static final int MIN_SELECTION=1,MAX_SELECTION=100;
    enum Preset {
        HIGH("High quality","Preserve finer details",3200,90),
        BALANCED("Balanced","Recommended",2048,78),
        SMALL("Maximum compression","Prioritize smaller files",1280,60);
        final String label,detail;final int edge,quality;
        Preset(String l,String d,int e,int q){label=l;detail=d;edge=e;quality=q;}
    }
    enum Format { ORIGINAL("Original"),JPEG("JPEG"),WEBP("WebP"); final String label;Format(String s){label=s;} }
    static final class Source {
        final Uri uri;final String name,mime;final long bytes;final SmartMediaScan.Item original;final String version;
        Source(Uri u,String n,String m,long b,SmartMediaScan.Item i,String v){uri=u;name=n;mime=m;bytes=b;original=i;version=v;}
    }
    static final class Output {
        final Source source;final File file;final String mime;final long bytes;final int width,height;final byte[] inputHash,hash;
        Uri saved;boolean gallerySaved,trashed;
        Output(Source s,File f,String m,int w,int h,byte[] input,byte[] output){source=s;file=f;mime=m;bytes=f.length();width=w;height=h;inputHash=input;hash=output;}
    }
    static Source inspect(Context c,Uri uri)throws IOException {
        if(!"content".equals(uri.getScheme()))throw new IOException("Unsupported photo URI");
        ContentResolver r=c.getContentResolver();String name="Photo",mime=r.getType(uri);long bytes=-1;
        try(Cursor cur=r.query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
            if(cur!=null&&cur.moveToFirst()){if(!cur.isNull(0))name=cur.getString(0);if(!cur.isNull(1))bytes=cur.getLong(1);}
        }catch(RuntimeException ignored){}
        if(bytes<=0)try(android.os.ParcelFileDescriptor fd=r.openFileDescriptor(uri,"r")){if(fd!=null)bytes=fd.getStatSize();}
        if(bytes<=0||mime==null||!mime.startsWith("image/"))throw new IOException("Unknown photo size or type");
        return new Source(uri,name,mime,bytes,null,"");
    }
    private static void check()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");}
    private static InputStream open(Context c,Uri u)throws IOException{InputStream in=c.getContentResolver().openInputStream(u);if(in==null)throw new IOException("Cannot read photo");return in;}
    static byte[] hash(Context c,Uri u)throws Exception{try(InputStream in=open(c,u)){return digest(in);}}
    private static byte[] digest(InputStream in)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){check();d.update(b,0,n);}return d.digest();}
    private static String targetMime(String actual,Format format)throws IOException{
        if(format==Format.JPEG)return "image/jpeg";if(format==Format.WEBP)return "image/webp";
        if("image/jpeg".equals(actual)||"image/png".equals(actual)||"image/webp".equals(actual))return actual;
        throw new IOException("Original format unsupported; choose JPEG or WebP");
    }
    // Reject animated GIF and animated WebP on all supported releases. Other potentially
    // animated formats (AVIF/HEIF, etc.) are checked by ImageDecoder where available.
    private static void checkAnimation(Context c,Source s,String actual)throws IOException{
        if("image/gif".equals(actual))throw new IOException("GIF is not supported");
        if("image/webp".equals(actual)){
            byte[] h=new byte[21];int n=0;try(InputStream in=open(c,s.uri)){while(n<h.length){int k=in.read(h,n,h.length-n);if(k<0)break;n+=k;}}
            if(n>=21&&h[12]=='V'&&h[13]=='P'&&h[14]=='8'&&h[15]=='X'&&(h[20]&2)!=0)throw new IOException("Animated WebP is not supported");
        }
        // APNG may be identified as PNG: inspect chunk headers until IDAT.
        if("image/png".equals(actual))try(DataInputStream in=new DataInputStream(new BufferedInputStream(open(c,s.uri)))){
            byte[] signature=new byte[8];in.readFully(signature);
            while(true){check();long length=Integer.toUnsignedLong(in.readInt());int type=in.readInt();
                if(type==0x6163544c)throw new IOException("Animated PNG is not supported");
                if(type==0x49444154||type==0x49454e44)break;
                if(length>64L*1024*1024)throw new IOException("Invalid PNG chunk");
                long left=length+4;while(left>0){long k=in.skip(left);if(k==0){if(in.read()<0)throw new EOFException();k=1;}left-=k;}
            }
        }
        if(Build.VERSION.SDK_INT<28&&!Arrays.asList("image/jpeg","image/png","image/webp").contains(actual))throw new IOException("Format unsupported on this Android version");
    }
    private static Bitmap decode(Context c,Source s,Preset preset)throws Exception{
        if(Build.VERSION.SDK_INT>=28){
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.getContentResolver(),s.uri),(decoder,info,src)->{
                if(info.isAnimated())throw new IllegalArgumentException("Animated images are not supported");
                int w=info.getSize().getWidth(),h=info.getSize().getHeight();float ratio=Math.min(1f,preset.edge/(float)Math.max(w,h));
                decoder.setTargetSize(Math.max(1,Math.round(w*ratio)),Math.max(1,Math.round(h*ratio)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
            });
        }
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try(InputStream in=open(c,s.uri)){BitmapFactory.decodeStream(in,null,options);}
        if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Unreadable photo");
        options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>preset.edge*2)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);
        Bitmap b;try(InputStream in=open(c,s.uri)){b=BitmapFactory.decodeStream(in,null,options);}
        if(b==null)throw new IOException("Unreadable photo");
        try{
            int orientation;try(InputStream in=open(c,s.uri)){orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(IOException e){if("image/jpeg".equals(options.outMimeType))throw e;orientation=ExifInterface.ORIENTATION_NORMAL;}
            Matrix m=new Matrix();switch(orientation){
                case 2:m.setScale(-1,1);break;case 3:m.setRotate(180);break;case 4:m.setScale(1,-1);break;
                case 5:m.setRotate(90);m.postScale(-1,1);break;case 6:m.setRotate(90);break;case 7:m.setRotate(-90);m.postScale(-1,1);break;case 8:m.setRotate(-90);break;
            }
            float ratio=Math.min(1f,preset.edge/(float)Math.max(b.getWidth(),b.getHeight()));m.postScale(ratio,ratio);
            if(!m.isIdentity()){Bitmap next=Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),m,true);if(next!=b){b.recycle();b=next;}}
            return b;
        }catch(Exception|OutOfMemoryError e){b.recycle();throw e;}
    }
    static Output compress(Context c,Source s,Preset preset,Format format)throws Exception{
        check();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
        try(InputStream in=open(c,s.uri)){BitmapFactory.decodeStream(in,null,bounds);}
        String actual=bounds.outMimeType;
        if(actual==null||bounds.outWidth<1||bounds.outHeight<1)throw new IOException("Unsupported or damaged photo");
        checkAnimation(c,s,actual);String mime=targetMime(actual,format);byte[] inputHash=hash(c,s.uri);
        File file=File.createTempFile("clenova-photo-",extension(mime),c.getCacheDir());boolean ok=false;Bitmap b=null,flat=null;
        try{
            check();b=decode(c,s,preset);if(b==null)throw new IOException("Could not decode photo");
            if("image/jpeg".equals(mime)&&b.hasAlpha()){flat=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(flat);canvas.drawColor(Color.WHITE);canvas.drawBitmap(b,0,0,null);}
            Bitmap.CompressFormat encoder="image/jpeg".equals(mime)?Bitmap.CompressFormat.JPEG:"image/png".equals(mime)?Bitmap.CompressFormat.PNG:Build.VERSION.SDK_INT>=30?Bitmap.CompressFormat.WEBP_LOSSY:Bitmap.CompressFormat.WEBP;
            try(OutputStream out=new BufferedOutputStream(new FileOutputStream(file))){if(!(flat==null?b:flat).compress(encoder,preset.quality,out))throw new IOException("Encoding failed");}
            check();if(file.length()<=0)throw new IOException("Empty output");
            BitmapFactory.Options verified=new BitmapFactory.Options();verified.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getAbsolutePath(),verified);
            if(verified.outWidth!=b.getWidth()||verified.outHeight!=b.getHeight()||!mime.equals(verified.outMimeType))throw new IOException("Invalid output dimensions or format");
            BitmapFactory.Options small=new BitmapFactory.Options();small.inSampleSize=1;while(Math.max(verified.outWidth,verified.outHeight)/small.inSampleSize>512)small.inSampleSize*=2;
            Bitmap sample=BitmapFactory.decodeFile(file.getAbsolutePath(),small);if(sample==null)throw new IOException("Output could not be decoded");sample.recycle();
            if(!MessageDigest.isEqual(inputHash,hash(c,s.uri)))throw new IOException("Source changed during compression");
            byte[] outputHash;try(InputStream in=new FileInputStream(file)){outputHash=digest(in);}
            Output result=new Output(s,file,mime,b.getWidth(),b.getHeight(),inputHash,outputHash);ok=true;return result;
        }finally{if(flat!=null)flat.recycle();if(b!=null)b.recycle();if(!ok)file.delete();}
    }
    static String extension(String mime){return "image/png".equals(mime)?".png":"image/webp".equals(mime)?".webp":".jpg";}
    static String name(Output out){String n=out.source.name.replaceFirst("(?i)\\.[a-z0-9]{1,8}$","").replaceAll("[^a-zA-Z0-9_-]","_");if(n.length()>40)n=n.substring(0,40);return "Clenova_"+n+"_"+UUID.randomUUID().toString().substring(0,8)+extension(out.mime);}
    static boolean matches(Context c,Output out)throws Exception{return out.saved!=null&&MessageDigest.isEqual(out.hash,hash(c,out.saved));}
    private static void copy(Context c,Output out,Uri uri)throws Exception{
        if(!out.file.isFile()||out.file.length()!=out.bytes)throw new IOException("Temporary photo missing");
        long copied=0;try(InputStream in=new FileInputStream(out.file);OutputStream dst=c.getContentResolver().openOutputStream(uri,"w")){
            if(dst==null)throw new IOException("Cannot open destination");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){check();dst.write(b,0,n);copied+=n;}dst.flush();
        }
        if(copied!=out.bytes||!MessageDigest.isEqual(out.hash,hash(c,uri)))throw new IOException("Saved copy did not verify");
    }
    static Uri save(Context c,Output out,Uri tree)throws Exception{
        check();ContentResolver r=c.getContentResolver();Uri uri=null;boolean published=false,created=false;
        try{
            if(tree==null){
                if(Build.VERSION.SDK_INT<29)throw new IOException("Choose a folder");
                ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name(out));values.put(MediaStore.MediaColumns.MIME_TYPE,out.mime);values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/Clenova");values.put(MediaStore.MediaColumns.IS_PENDING,1);
                uri=r.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),values);
            }else{
                Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));uri=DocumentsContract.createDocument(r,parent,out.mime,name(out));
            }
            if(uri==null||uri.equals(out.source.uri))throw new IOException("Cannot create new photo");
            if(tree==null)created=true;
            if(tree!=null){long bytes=-1;try(Cursor cur=r.query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(cur!=null&&cur.moveToFirst()&&!cur.isNull(0))bytes=cur.getLong(0);}if(bytes!=0)throw new IOException("Destination must be a new empty document");created=true;}
            copy(c,out,uri);check();
            if(tree==null){ContentValues done=new ContentValues();done.put(MediaStore.MediaColumns.IS_PENDING,0);if(r.update(uri,done,null,null)!=1)throw new IOException("Cannot publish photo");}
            published=true;return uri;
        }finally{if(!published&&created&&uri!=null&&!uri.equals(out.source.uri))try{if(tree==null)r.delete(uri,null,null);else DocumentsContract.deleteDocument(r,uri);}catch(Exception ignored){}}
    }
    private PhotoCompressionEngine(){}
}

package com.clenova.app;
import java.util.concurrent.*;
public final class PreviewWorkTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        check(PreviewWork.cacheBytes(32L*1024*1024)==2*1024*1024);
        check(PreviewWork.cacheBytes(512L*1024*1024)==12*1024*1024);
        check(PreviewWork.cacheBytes(0)==1024*1024);
        ThreadPoolExecutor p=PreviewWork.pool(1);
        CountDownLatch release=new CountDownLatch(1), started=new CountDownLatch(1), newest=new CountDownLatch(1);
        try{
            p.execute(()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            check(started.await(2,TimeUnit.SECONDS));
            Future<?> oldest=p.submit(()->{});
            for(int i=0;i<600;i++)p.execute(()->{});
            check(oldest.isCancelled());
            p.execute(newest::countDown);
            check(p.getQueue().size()==PreviewWork.MAX_PENDING);
            check(p.allowsCoreThreadTimeOut());
            release.countDown();check(newest.await(2,TimeUnit.SECONDS));
        }finally{release.countDown();p.shutdownNow();}
        System.out.println("PreviewWork: 8 assertions passed");
    }
}

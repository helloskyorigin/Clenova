package com.clenova.app;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bound pending preview work and cache usage; never execute bitmap decoding on the UI thread. */
final class PreviewWork {
    static final int MAX_PENDING=256;
    static ThreadPoolExecutor pool(int workers){
        ThreadPoolExecutor pool=new ThreadPoolExecutor(workers,workers,15,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING),new ThreadPoolExecutor.DiscardOldestPolicy());
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }
    static int cacheBytes(long heapBytes){
        return (int)Math.max(1024*1024,Math.min(12L*1024*1024,heapBytes/16));
    }
    private PreviewWork(){}
}

package com.clenova.app;

import java.util.Arrays;

/** Decimal megabits/sec and explicitly defined HTTP latency statistics. */
final class SpeedTestMath {
    static double mbps(long bytes,long nanos){if(bytes<0||nanos<=0)throw new IllegalArgumentException("Invalid transfer");return bytes*8000d/nanos;}
    static double median(double[] samples){if(samples.length==0)throw new IllegalArgumentException("No samples");double[] a=samples.clone();for(double v:a)if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("Invalid latency");Arrays.sort(a);return a.length%2==1?a[a.length/2]:(a[a.length/2-1]+a[a.length/2])/2;}
    static double jitter(double[] samples){if(samples.length<2)throw new IllegalArgumentException("Need two samples");double sum=0;for(int i=1;i<samples.length;i++)sum+=Math.abs(samples[i]-samples[i-1]);return sum/(samples.length-1);}
    static int nextSize(double speed,int remaining,int max){long target=Math.round(Math.max(0,speed)*1000000d/8*1.5);return (int)Math.min(remaining,Math.min(max,Math.max(128000,target)));}
    private SpeedTestMath(){}
}

package com.clenova.app;

public final class SpeedTestMathTest {
    private static void eq(double actual,double expected){if(Math.abs(actual-expected)>0.000001)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        eq(SpeedTestMath.mbps(1000000,1000000000L),8);
        eq(SpeedTestMath.mbps(12500000,1000000000L),100);
        eq(SpeedTestMath.median(new double[]{40,20,30,10,50}),30);
        eq(SpeedTestMath.median(new double[]{40,20}),30);
        eq(SpeedTestMath.jitter(new double[]{20,30,25}),7.5);
        eq(SpeedTestMath.nextSize(0,5000000,4000000),128000);
        eq(SpeedTestMath.nextSize(1000,5000000,4000000),4000000);
        eq(SpeedTestMath.nextSize(1000,42000,4000000),42000);
        boolean bad=false;try{SpeedTestMath.mbps(1,0);}catch(IllegalArgumentException e){bad=true;}
        if(!bad)throw new AssertionError("Invalid timing must fail");
        System.out.println("SpeedTestMathTest passed");
    }
}

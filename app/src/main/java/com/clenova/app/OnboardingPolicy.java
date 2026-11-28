package com.clenova.app;

/** Introduction timing is presentation only, never device-operation progress. */
final class OnboardingPolicy {
    static final long SPLASH_MS=420, ENTER_MS=180;
    static final int STEPS=3;
    static long duration(int page){if(page<0||page>=STEPS)throw new IllegalArgumentException("page");return page==2?6000:5000;}
    static float position(float p){return Float.isFinite(p)?Math.max(0f,Math.min(1f,p)):0f;}
    static int emphasis(float fraction,int rows){if(rows<1)throw new IllegalArgumentException("rows");return Math.min(rows-1,(int)(position(fraction)*rows));}
    static boolean automatic(boolean reducedMotion,float fontScale,boolean fits){return !reducedMotion&&fontScale<=1.15f&&fits;}
    static boolean compact(int width,int height){return width<360||height<660;}
    private OnboardingPolicy(){}
}

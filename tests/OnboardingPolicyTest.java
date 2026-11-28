package com.clenova.app;
public final class OnboardingPolicyTest {
    static int count;
    static void check(boolean b){count++;if(!b)throw new AssertionError("Assertion "+count);}
    public static void main(String[] args){
        check(OnboardingPolicy.duration(0)==5000);check(OnboardingPolicy.duration(1)==5000);check(OnboardingPolicy.duration(2)==6000);
        check(OnboardingPolicy.duration(0)+OnboardingPolicy.duration(1)+OnboardingPolicy.duration(2)==16000);
        check(OnboardingPolicy.SPLASH_MS<500);check(OnboardingPolicy.ENTER_MS<250);
        check(OnboardingPolicy.position(-1)==0);check(OnboardingPolicy.position(2)==1);check(OnboardingPolicy.position(Float.NaN)==0);
        check(OnboardingPolicy.emphasis(0,4)==0);check(OnboardingPolicy.emphasis(.25f,4)==1);check(OnboardingPolicy.emphasis(1,4)==3);
        check(OnboardingPolicy.automatic(false,1,true));check(!OnboardingPolicy.automatic(true,1,true));
        check(!OnboardingPolicy.automatic(false,1.3f,true));check(!OnboardingPolicy.automatic(false,1,false));
        check(OnboardingPolicy.compact(320,740));check(OnboardingPolicy.compact(393,620));check(!OnboardingPolicy.compact(393,740));
        System.out.println("Onboarding policy: "+count+" assertions passed");
    }
}

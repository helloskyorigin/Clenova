package com.clenova.app;
public final class ProtectionPolicyTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        long now=2_000_000_000_000L,old=now-ProtectionPolicy.RECENT_WINDOW_MS-1;
        for(int mask=0;mask<16;mask++){
            boolean f=(mask&1)!=0,r=(mask&2)!=0,d=(mask&4)!=0,b=(mask&8)!=0;
            ProtectionPolicy policy=new ProtectionPolicy(f,r,d,b);
            check(policy.approvalRequired());
            check(policy.isProtected(true,false,false,old,false,false,now));
            check(policy.isProtected(false,true,false,old,false,false,now));
            check(!policy.isProtected(false,false,false,old,false,false,now));
            check(policy.isProtected(false,false,true,old,false,false,now)==f);
            check(policy.isProtected(false,false,false,now-ProtectionPolicy.RECENT_WINDOW_MS,false,false,now)==r);
            check(policy.isProtected(false,false,false,old,true,false,now)==d);
            check(policy.isProtected(false,false,false,old,false,true,now)==b);
            check(policy.isProtected(false,false,null,old,false,false,now)==f);
            check(policy.isProtected(false,false,false,null,false,false,now)==r);
        }
        System.out.println("Protection policy: all 16 switch configurations passed");
    }
}

package com.clenova.app;
public final class AutopilotPolicyTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        check(AutopilotPolicy.eligible(7,0)==0);
        check(AutopilotPolicy.eligible(7,4)==0);
        check(AutopilotPolicy.eligible(7,5)==3);
        check(AutopilotPolicy.eligible(7,6)==4);
        check(AutopilotPolicy.eligible(7,3)==7);
        check(AutopilotPolicy.eligible(4,1)==0);
        check(AutopilotPolicy.eligible(1,3)==1);
        check(AutopilotPolicy.interval(3)==259200000L);
        check(AutopilotPolicy.interval(7)==604800000L);
        check(AutopilotPolicy.interval(0)==86400000L);
        check(!AutopilotPolicy.fresh(10,9,1));
        check(!AutopilotPolicy.fresh(0,100,1));
        check(AutopilotPolicy.fresh(10,10+172800000L,1));
        check(!AutopilotPolicy.fresh(10,11+172800000L,1));
        System.out.println("Autopilot policy: 14 assertions passed");
    }
}

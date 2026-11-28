package com.clenova.app;
public final class PreparationPolicyTest {
    static int count;
    static void check(boolean ok){count++;if(!ok)throw new AssertionError("Assertion "+count);}
    public static void main(String[] args){
        check(PreparationPolicy.stage(-1)==0);check(PreparationPolicy.stage(99)==5);
        for(int i=0;i<=5;i++){check(PreparationPolicy.percent(i)==i*20);for(int j=0;j<5;j++)check(PreparationPolicy.rowState(i,j,false)==(j<i?2:j==i?1:0));}
        check(PreparationPolicy.rowState(2,2,true)==0);check(PreparationPolicy.rowState(2,1,true)==2);
        check(PreparationPolicy.COMPLETE_DWELL_MS<500);
        System.out.println("Preparation policy: "+count+" assertions passed");
    }
}

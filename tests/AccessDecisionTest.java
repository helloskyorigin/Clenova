package com.clenova.app;
public final class AccessDecisionTest {
    private static void check(boolean expected,boolean[] g,boolean[] a,boolean[] r){if(AccessDecision.settings(g,a,r)!=expected)throw new AssertionError("Unexpected permission route");}
    public static void main(String[] args){
        check(false,new boolean[]{false},new boolean[]{false},new boolean[]{false});
        check(false,new boolean[]{false},new boolean[]{true},new boolean[]{true});
        check(true,new boolean[]{false},new boolean[]{true},new boolean[]{false});
        check(false,new boolean[]{true},new boolean[]{true},new boolean[]{false});
        check(false,new boolean[]{false,false},new boolean[]{true,false},new boolean[]{false,false});
        check(true,new boolean[]{true,false},new boolean[]{true,true},new boolean[]{false,false});
        check(false,new boolean[]{false,false},new boolean[]{true,true},new boolean[]{false,true});
        System.out.println("AccessDecision: 7 cases passed");
    }
}

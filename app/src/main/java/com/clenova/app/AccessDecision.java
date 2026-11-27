package com.clenova.app;
final class AccessDecision {
    static boolean settings(boolean[] granted,boolean[] asked,boolean[] rationale){
        if(granted.length!=asked.length||asked.length!=rationale.length)throw new IllegalArgumentException();
        boolean missing=false;
        for(int i=0;i<granted.length;i++)if(!granted[i]){
            missing=true;if(!asked[i]||rationale[i])return false;
        }
        return missing;
    }
    private AccessDecision(){}
}

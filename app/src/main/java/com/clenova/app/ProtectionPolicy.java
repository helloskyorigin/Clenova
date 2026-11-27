package com.clenova.app;

/** Rule evaluation for a future scanner. No file access or deletion. */
final class ProtectionPolicy {
    static final long RECENT_WINDOW_MS=7L*24*60*60*1000;
    private final boolean favorites,recent,documents,bestShots;
    ProtectionPolicy(boolean favorites,boolean recent,boolean documents,boolean bestShots){
        this.favorites=favorites;this.recent=recent;this.documents=documents;this.bestShots=bestShots;
    }
    /** Mandatory keep (e.g. retained original) and explicit exclusions always win.
     * Null metadata is conservatively treated as protected when its rule is enabled.
     */
    boolean isProtected(boolean mandatoryKeep,boolean personalExclusion,Boolean favorite,Long modifiedAt,
                        Boolean importantDocument,Boolean bestShot,long now){
        if(mandatoryKeep||personalExclusion)return true;
        if(favorites&&!Boolean.FALSE.equals(favorite))return true;
        if(documents&&!Boolean.FALSE.equals(importantDocument))return true;
        if(bestShots&&!Boolean.FALSE.equals(bestShot))return true;
        if(recent&&(modifiedAt==null||modifiedAt<=0||modifiedAt>=now-RECENT_WINDOW_MS))return true;
        return false;
    }
    boolean approvalRequired(){return true;}
}

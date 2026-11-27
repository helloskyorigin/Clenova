package com.clenova.app;

/** Boundary-safe comparison of Android external-storage tree document IDs. */
final class FolderExclusion {
    static boolean matches(String rootAuthority,String rootId,String candidateAuthority,String candidateId){
        if(rootAuthority==null||!rootAuthority.equals(candidateAuthority)||rootId==null||candidateId==null)return false;
        if(rootId.equals(candidateId))return true;
        if(!"com.android.externalstorage.documents".equals(rootAuthority))return false;
        int a=rootId.indexOf(':'),b=candidateId.indexOf(':');
        if(a<=0||b<=0||!rootId.substring(0,a).equalsIgnoreCase(candidateId.substring(0,b)))return false;
        String root=normalize(rootId.substring(a+1)),candidate=normalize(candidateId.substring(b+1));
        if(root==null||candidate==null)return false;
        return root.isEmpty()||candidate.equalsIgnoreCase(root)||candidate.regionMatches(true,0,root+"/",0,root.length()+1);
    }
    private static String normalize(String path){
        StringBuilder out=new StringBuilder();
        for(String part:path.split("/")){
            if(part.isEmpty()||part.equals("."))continue;
            if(part.equals(".."))return null;
            if(out.length()>0)out.append('/');
            out.append(part);
        }
        return out.toString();
    }
}

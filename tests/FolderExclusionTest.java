package com.clenova.app;
public final class FolderExclusionTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        String a="com.android.externalstorage.documents",r="primary:Pictures/Family";
        check(FolderExclusion.matches(a,r,a,r));
        check(FolderExclusion.matches(a,r,a,r+"/2026/photo.jpg"));
        check(!FolderExclusion.matches(a,r,a,r+"Backup/photo.jpg"));
        check(!FolderExclusion.matches(a,r,a,"1234:Pictures/Family/photo.jpg"));
        check(!FolderExclusion.matches(a,r,"other",r));
        check(!FolderExclusion.matches(a,r,a,r+"/../Other"));
        check(FolderExclusion.matches(a,r,a,"primary:pictures/family/photo.jpg"));
        check(!FolderExclusion.matches("opaque","123","opaque","123/child"));
        check(FolderExclusion.matches("opaque","123","opaque","123"));
        System.out.println("Folder exclusion boundary checks passed");
    }
}

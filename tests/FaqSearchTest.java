package com.clenova.app;
public final class FaqSearchTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        check(FaqSearch.matches("","anything"));
        check(FaqSearch.matches("  ","anything"));
        check(FaqSearch.matches("CLEANUP permission","Permission for cleanup"));
        check(FaqSearch.matches("proteccion","Protección de archivos"));
        check(FaqSearch.matches("फ़ाइल","फ़ाइल सुरक्षा"));
        check(!FaqSearch.matches("cleanup unrelated","Cleanup permissions"));
        check(!FaqSearch.matches("xyz","Nothing"));
        System.out.println("FAQ search checks passed");
    }
}

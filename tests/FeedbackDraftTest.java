package com.clenova.app;
public final class FeedbackDraftTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        check(!FeedbackDraft.valid(null));check(!FeedbackDraft.valid(" \n\t"));
        check(FeedbackDraft.valid("x".repeat(1000)));check(!FeedbackDraft.valid("x".repeat(1001)));
        check(FeedbackDraft.category(-1)==0&&FeedbackDraft.category(4)==0);
        for(int i=0;i<4;i++)check(FeedbackDraft.subject(i).contains(FeedbackDraft.CATEGORIES[i]));
        String off=FeedbackDraft.body(1,"  Fix this  ",false,"secret-version","secret-android");
        check(off.equals("Category: Report a bug\n\nFix this"));
        check(!off.contains("secret"));
        String on=FeedbackDraft.body(2,"New feature",true,"1.2","14");
        check(on.contains("App version: 1.2\nAndroid: 14"));
        System.out.println("Feedback draft checks passed");
    }
}

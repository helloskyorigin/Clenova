package com.clenova.app;

/** Builds a user-controlled feedback email; no network transport or personal metadata. */
final class FeedbackDraft {
    static final int MAX_LENGTH=1000;
    static final String[] CATEGORIES={"Suggestion","Report a bug","Feature request","Something else"};
    static int category(int index){return index>=0&&index<CATEGORIES.length?index:0;}
    static boolean valid(String text){return text!=null&&!text.trim().isEmpty()&&text.length()<=MAX_LENGTH;}
    static String subject(int index){return "Clenova feedback — "+CATEGORIES[category(index)];}
    static String body(int index,String text,boolean includeInfo,String version,String android){
        if(!valid(text))throw new IllegalArgumentException("Invalid feedback");
        String result="Category: "+CATEGORIES[category(index)]+"\n\n"+text.trim();
        if(includeInfo)result+="\n\nApp version: "+version+"\nAndroid: "+android;
        return result;
    }
}

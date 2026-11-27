package com.clenova.app;
public final class AgentActionsTest{
    private static void expect(String q,int n){if(AgentActions.route(q)!=n)throw new AssertionError(q);}
    public static void main(String[] args){
        expect("Help me free up 4.8 GB",0);expect("Review my duplicate photos",1);expect("Find apps I rarely use",2);expect("Protect my files",3);
        expect("organize my downloads",4);expect("find large videos",5);expect("review screenshots",6);expect("find similar photos",7);
        expect("show large files",8);expect("review unnecessary files",9);expect("delete everything", -1);expect("review screenshots and delete photos",-1);
        System.out.println("Agent action routing: 12 cases passed");
    }
}

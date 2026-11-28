package com.clenova.app;
public final class HomePresentationTest {
    static int count;
    static void check(boolean ok){count++;if(!ok)throw new AssertionError("Assertion "+count);}
    public static void main(String[] args){
        check(HomePresentation.progress(100,100)==0);
        check(HomePresentation.progress(100,0)==1000);
        check(HomePresentation.progress(100,25)==750);
        check(HomePresentation.progress(Long.MAX_VALUE,0)==1000);
        check(HomePresentation.progress(Long.MAX_VALUE,Long.MAX_VALUE)==0);
        check(HomePresentation.progress(0,0)==-1);
        check(HomePresentation.progress(100,-1)==-1);
        check(HomePresentation.progress(100,101)==-1);
        check(HomePresentation.columns(320,1f)==1);
        check(HomePresentation.columns(360,1f)==2);
        check(HomePresentation.columns(393,1.3f)==1);
        check(HomePresentation.status(false,false,true).equals("Reading device data"));
        check(HomePresentation.status(false,true,true).startsWith("Storage unavailable"));
        check(HomePresentation.status(true,true,true).startsWith("Limited overview"));
        check(HomePresentation.status(true,true,false).equals("Read from your device"));
        System.out.println("Home presentation: "+count+" assertions passed");
    }
}

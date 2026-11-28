package com.clenova.app;
public final class NavigationPresentationTest {
    static int count;
    static void check(boolean ok){count++;if(!ok)throw new AssertionError("Assertion "+count);}
    public static void main(String[] args){
        check(NavigationPresentation.selected(false,false,false)==0);
        check(NavigationPresentation.selected(false,false,true)==1);
        check(NavigationPresentation.selected(false,true,false)==2);
        check(NavigationPresentation.selected(true,true,true)==3);
        check(NavigationPresentation.height(1f)==64);
        check(NavigationPresentation.height(1.3f)==76);
        check(NavigationPresentation.height(2f)==92);
        check(NavigationPresentation.toolsWidth(320)>=48);
        check(NavigationPresentation.toolsWidth(393)==64);
        check(NavigationPresentation.motion(true,false));
        check(!NavigationPresentation.motion(false,false));
        check(!NavigationPresentation.motion(true,true));
        check(NavigationPresentation.scroll(-1)==0);
        check(NavigationPresentation.scroll(80)==80);
        System.out.println("Navigation presentation: "+count+" assertions passed");
    }
}

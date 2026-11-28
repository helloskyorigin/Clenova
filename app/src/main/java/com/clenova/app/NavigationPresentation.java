package com.clenova.app;

/** Dashboard geometry and selected state; never owns a feature or permission route. */
final class NavigationPresentation {
    static final int HOME=0,CLEAN=1,AGENT=2,TOOLS=3;
    static final int TOUCH_TARGET=48;
    static int selected(boolean tools,boolean agent,boolean clean){return tools?TOOLS:agent?AGENT:clean?CLEAN:HOME;}
    static int height(float fontScale){return fontScale>1.6f?92:fontScale>1.15f?76:64;}
    static int toolsWidth(int widthDp){return widthDp<360?56:64;}
    static boolean motion(boolean animations,boolean touchExploration){return animations&&!touchExploration;}
    static int scroll(int value){return Math.max(0,value);}
    private NavigationPresentation(){}
}

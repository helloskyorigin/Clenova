package com.clenova.app;

import android.content.Context;

/** Introduction 1: native story, no device work or fake results. */
final class AgentOnboardingView extends OnboardingStoryView {
    static final int BACKGROUND=UiPolish.LIGHT.background;
    AgentOnboardingView(Context context,boolean dark,Runnable next){
        super(context);buildStory(0,dark,"Meet your personal\nphone agent.","More space. Less clutter. Always in your control.","Get Started",next);
    }
}

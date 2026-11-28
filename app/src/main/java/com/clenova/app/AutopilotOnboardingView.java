package com.clenova.app;

import android.content.Context;

/** Introduction 3: native story, no device work or fake results. */
final class AutopilotOnboardingView extends OnboardingStoryView {
    AutopilotOnboardingView(Context context,boolean dark,Runnable next){
        super(context);buildStory(2,dark,"On autopilot.\nIn your control.","Let your Agent find opportunities. You approve every removal.","Continue",next);
    }
}

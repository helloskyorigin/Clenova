package com.clenova.app;

import android.content.Context;

/** Introduction 2: native story, no device work or fake results. */
final class UnifiedOnboardingView extends OnboardingStoryView {
    UnifiedOnboardingView(Context context,boolean dark,Runnable next){
        super(context);buildStory(1,dark,"One Agent.\nYour entire phone.","Review photos, videos, apps and files from one place.","Continue",next);
    }
}

package com.clenova.app;

/** Consistent decimal-MB sample storage breakdown, before or after approved preview. */
final class StoragePreview {
    static final double CAPACITY=128000;
    static final String[] LABELS={"Photos","Videos","Apps","Documents","System & other"};
    static final String[] ICONS={"images","video","apps","document","phone"};
    private final double[] categories={27400,21000,13600,3200,20000};
    final double recovered;
    StoragePreview(CleanupPreview cleanup){
        recovered=12400+(cleanup==null?0:cleanup.total);
        if(cleanup!=null){
            categories[0]-=cleanup.amount(0)+cleanup.amount(1);
            categories[1]-=cleanup.amount(2);
        }
    }
    double amount(int i){return categories[i];}
    double used(){double sum=0;for(double amount:categories)sum+=amount;return sum;}
    double available(){return CAPACITY-used();}
}

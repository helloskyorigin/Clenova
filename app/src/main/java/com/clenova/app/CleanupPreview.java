package com.clenova.app;

/** Immutable snapshot of an explicitly approved, simulated cleanup. */
final class CleanupPreview {
    private final double[] amounts;
    final int count;
    final double total;
    CleanupPreview(ReviewPlan plan) {
        this(new double[]{plan.categoryMb(0),plan.categoryMb(1),plan.categoryMb(2)},plan.count());
    }
    CleanupPreview(double[] amounts,int count) {
        if(amounts.length!=3 || count<=0)throw new IllegalArgumentException("Empty cleanup");
        this.amounts=amounts.clone();this.count=count;
        double sum=0;
        for(double amount:this.amounts){
            if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid size");
            sum+=amount;
        }
        if(sum<=0)throw new IllegalArgumentException("Empty cleanup");
        total=sum;
    }
    double[] amounts(){return amounts.clone();}
    double amount(int i){return amounts[i];}
    double before(int i){double sum=0;for(int j=0;j<i;j++)sum+=amounts[j];return sum;}
    double recovered(float fraction){return Math.min(total,Math.max(0,Math.round(total*Math.max(0f,Math.min(1f,fraction))*2.0)/2.0));}
    int status(int i,double recovered){
        if(amounts[i]==0)return -1;
        if(recovered>=before(i)+amounts[i])return 2;
        return recovered>=before(i)?1:0;
    }
    double available(){return 42800+total;}
    double lifetimeRecovered(){return 12400+total;}
    double used(){return 85200-total;}
}

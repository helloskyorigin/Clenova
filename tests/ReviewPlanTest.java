package com.clenova.app;
import java.util.Arrays;
public final class ReviewPlanTest {
 private static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  ReviewPlan p=new ReviewPlan();
  check(p.total()==1200 && p.count()==48 && !p.isSelected(1));
  boolean[] draft=p.draftCopies();draft[0]=false;check(p.total()==1200);
  p.confirmCopies(draft);check(p.total()==1175 && p.count()==47);
  check(ReviewPlan.gb(p.total()).equals("1.175 GB"));
  boolean[] sim=p.draftSimilar();check(sim.length==80 && ReviewPlan.selectedCount(sim)==80);
  check(p.total()==1175);boolean[] seen=new boolean[80];
  for(int g=0;g<32;g++)for(int i=1;i<(g<16?4:3);i++){
    int j=ReviewPlan.similarIndex(g,i);check(!seen[j]);seen[j]=true;
  }
  check(ReviewPlan.selectedCount(seen)==80);
  sim[0]=false;p.confirmSimilar(sim);check(p.total()==2162.5 && p.count()==126);
  check(ReviewPlan.gb(p.categoryMb(1)).equals("987.5 MB"));
  sim[1]=false;check(p.total()==2162.5);
  p.setSelected(2,true);check(p.total()==4762.5);
  Arrays.fill(sim,false);p.confirmSimilar(sim);check(!p.isSelected(1));
  check(ReviewPlan.selectedCount(p.draftSimilar())==0);
  p.setSelected(0,false);p.setSelected(2,false);check(p.total()==0);
  p.setSelected(0,true);Arrays.fill(sim,true);p.confirmSimilar(sim);p.setSelected(2,true);
  check(p.total()==4800 && p.count()==140 && ReviewPlan.gb(p.total()).equals("4.8 GB"));
  System.out.println("Duplicate and similar selection checks passed");
 }
}

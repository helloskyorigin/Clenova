package com.clenova.app;
import java.util.Arrays;
public final class ReviewPlanTest {
 private static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  ReviewPlan p=new ReviewPlan();check(p.total()==1200&&p.count()==48&&!p.isSelected(1)&&!p.isSelected(2));
  boolean[] d=p.draftCopies();d[0]=false;check(p.total()==1200);p.confirmCopies(d);check(p.total()==1175&&p.count()==47);
  boolean[] sim=p.draftSimilar();check(sim.length==80&&ReviewPlan.selectedCount(sim)==80);boolean[] seen=new boolean[80];
  for(int g=0;g<32;g++)for(int i=1;i<(g<16?4:3);i++){int j=ReviewPlan.similarIndex(g,i);check(!seen[j]);seen[j]=true;}
  check(ReviewPlan.selectedCount(seen)==80);sim[0]=false;p.confirmSimilar(sim);check(p.total()==2162.5&&p.count()==126);
  check(ReviewPlan.gb(p.categoryMb(1)).equals("987.5 MB"));sim[1]=false;check(p.total()==2162.5);
  boolean[] vid=p.draftVideos();check(vid.length==12&&ReviewPlan.videoMb(vid)==2600&&p.total()==2162.5);
  vid[0]=false;p.confirmVideos(vid);check(p.total()==4282.5&&p.count()==137);
  vid[1]=false;check(p.total()==4282.5);
  Arrays.fill(vid,false);p.confirmVideos(vid);check(!p.isSelected(2)&&ReviewPlan.selectedCount(p.draftVideos())==0);
  Arrays.fill(sim,false);p.confirmSimilar(sim);p.setSelected(0,false);check(p.total()==0&&p.count()==0);
  p.setSelected(0,true);Arrays.fill(sim,true);p.confirmSimilar(sim);Arrays.fill(vid,true);p.confirmVideos(vid);
  check(p.total()==4800&&p.count()==140&&ReviewPlan.gb(p.total()).equals("4.8 GB"));
  for(int mask=0;mask<(1<<12);mask++){int expected=0,n=0;for(int i=0;i<12;i++){vid[i]=(mask&(1<<i))!=0;if(vid[i]){expected+=ReviewPlan.VIDEO_MB[i];n++;}}p.confirmVideos(vid);check(p.categoryMb(2)==expected&&p.total()==2200+expected&&p.count()==128+n);}
  System.out.println("All 4096 video selection masks and photo checks passed");
 }
}

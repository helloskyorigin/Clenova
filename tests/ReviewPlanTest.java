package com.clenova.app;
import java.util.Arrays;
public final class ReviewPlanTest {
  private static void check(boolean b){if(!b)throw new AssertionError();}
  public static void main(String[] args){
    ReviewPlan p=new ReviewPlan();
    check(p.total()==1200 && p.count()==48 && p.isSelected(0));
    boolean[] draft=p.draftCopies();
    check(draft.length==48);
    draft[0]=false;
    check(p.total()==1200);
    p.confirmCopies(draft);
    check(p.total()==1175 && p.count()==47);
    check(ReviewPlan.gb(p.total()).equals("1.175 GB"));
    draft[1]=false;
    check(p.total()==1175);
    p.setSelected(1,true);p.setSelected(2,true);
    check(p.total()==4775 && p.count()==139);
    Arrays.fill(draft,false);p.confirmCopies(draft);
    check(!p.isSelected(0) && p.total()==3600);
    p.setSelected(1,false);p.setSelected(2,false);
    check(p.total()==0 && p.count()==0);
    p.setSelected(0,true);p.setSelected(1,true);p.setSelected(2,true);
    check(p.total()==4800 && p.count()==140 && ReviewPlan.gb(p.total()).equals("4.8 GB"));
    System.out.println("ReviewPlan selection checks passed");
  }
}

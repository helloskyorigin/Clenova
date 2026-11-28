package com.clenova.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native, fully readable story examples. Animation highlights steps; it never represents a scan. */
final class OnboardingPanel extends LinearLayout {
    private final UiPolish.Palette colors;
    private final boolean compact,dark;
    private final LinearLayout[] rows;
    private final String[] steps;
    private int current=-1;
    OnboardingPanel(Context c,int page,boolean dark,boolean compact){
        super(c);this.dark=dark;this.compact=compact;colors=UiPolish.palette(dark);
        setOrientation(VERTICAL);setPadding(dp(14),dp(compact?12:16),dp(14),dp(compact?12:16));
        setBackground(UiKit.card(c,dark));
        String[] headers={"A conversation, not a chore","A plan for your phone","You make the final call"};
        String[] tags={"EXAMPLE CONVERSATION","REVIEW PLAN","APPROVAL FIRST"};
        String[][] titles={{"Photos & videos","Apps & files"},{"Photos","Videos","Unused apps","Files & downloads"},{"Discover","Review","Approve"}};
        String[][] details={{"Review clutter and duplicates","Find what is taking up space"},{"Find duplicate and similar photos","Review large videos","See available usage information","Review files in chosen folders"},{"Find opportunities locally","Choose what stays and what goes","Confirm before anything is removed"}};
        String[][] kinds={{"photos","files"},{"photos","video","apps","files"},{"scan","review","shield"}};
        TextView tag=label(tags[page],10,colors.accent,600);tag.setLetterSpacing(.08f);append(this,tag,0);
        TextView head=label(headers[page],compact?17:19,colors.ink,700);append(this,head,5);
        if(page==0){
            TextView user=label("Take care of my phone.",14,0xffffffff,600);user.setPadding(dp(12),dp(10),dp(12),dp(10));user.setBackground(UiKit.shape(c,UiPolish.CTA_BLUE,16));
            LayoutParams up=new LayoutParams(-2,-2);up.gravity=Gravity.END;up.topMargin=dp(12);addView(user,up);
            TextView reply=label("I can help you find what to review.",13,colors.ink,500);reply.setPadding(dp(12),dp(10),dp(12),dp(10));reply.setBackground(UiKit.shape(c,colors.tint,16));append(this,reply,7);
        }
        steps=titles[page];rows=new LinearLayout[steps.length];
        for(int i=0;i<steps.length;i++){
            LinearLayout row=new LinearLayout(c);row.setOrientation(HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(9),dp(compact?7:9),dp(9),dp(compact?7:9));
            View icon=new StoryIcon(c,kinds[page][i],colors.accent);
            icon.setBackground(UiKit.shape(c,colors.tint,10));row.addView(icon,new LayoutParams(dp(32),dp(32)));
            LinearLayout copy=new LinearLayout(c);copy.setOrientation(VERTICAL);LayoutParams cp=new LayoutParams(0,-2,1);cp.leftMargin=dp(10);row.addView(copy,cp);
            append(copy,label(steps[i],14,colors.ink,600),0);
            append(copy,label(details[page][i],12,colors.muted,400),3);
            rows[i]=row;append(this,row,i==0?12:4);
        }
        if(page==2){
            TextView note=label("No automatic deletion.",12,colors.success,600);append(this,note,10);
        }
        setFraction(0);
    }
    private int dp(float n){return UiKit.dp(getContext(),n);}
    private TextView label(String value,int size,int color,int weight){TextView t=new TextView(getContext());t.setText(value);UiKit.typography(t,size,color,weight);t.setLineSpacing(dp(2),1f);return t;}
    private void append(LinearLayout host,View v,int margin){LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(margin);host.addView(v,lp);}
    void setFraction(float value){
        int index=OnboardingPolicy.emphasis(value,rows.length);if(index==current)return;current=index;
        for(int i=0;i<rows.length;i++){
            android.graphics.drawable.GradientDrawable surface=UiKit.shape(getContext(),i==index?colors.tint:colors.card,12);
            surface.setStroke(dp(1),i==index?colors.accent:colors.border);rows[i].setBackground(surface);
        }
    }
    private static final class StoryIcon extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path path=new Path();private final String kind;
        StoryIcon(Context c,String kind,int tint){super(c);this.kind=kind;p.setColor(tint);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.65f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private void line(Canvas c,float... xy){path.reset();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float s=Math.min(getWidth(),getHeight())/32f;c.translate((getWidth()-24*s)/2,(getHeight()-24*s)/2);c.scale(s,s);
            switch(kind){
                case "photos":c.drawRoundRect(3,4,21,20,3,3,p);c.drawCircle(8,9,1.7f,p);line(c,4,17,10,12,14,16,17,13,20,16);break;
                case "video":c.drawRoundRect(3,5,21,19,3,3,p);line(c,10,8,16,12,10,16,10,8);break;
                case "apps":for(int i=0;i<2;i++)for(int j=0;j<2;j++)c.drawRoundRect(4+i*10,4+j*10,10+i*10,10+j*10,1.5f,1.5f,p);break;
                case "files":line(c,3,19,3,6,9,6,11,9,21,9,21,19,3,19);break;
                case "scan":line(c,8,4,4,4,4,8);line(c,16,4,20,4,20,8);line(c,4,16,4,20,8,20);line(c,16,20,20,20,20,16);line(c,7,12,17,12);break;
                case "review":c.drawRoundRect(5,3,19,21,2,2,p);line(c,8,8,16,8);line(c,8,12,16,12);line(c,8,16,13,16);break;
                default:line(c,12,3,20,6,20,12,18,16,12,21,6,16,4,12,4,6,12,3);line(c,8,12,11,15,16,9);break;
            }c.restore();}
    }
}

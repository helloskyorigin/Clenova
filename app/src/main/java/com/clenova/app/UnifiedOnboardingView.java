package com.clenova.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native introduction: an animated explanation of review planning, never a device scan. */
final class UnifiedOnboardingView extends OnboardingStoryView {
    private static final int BG=AgentOnboardingView.BACKGROUND, BLUE=0xff007aff;
    private final MotionStage stage;

    UnifiedOnboardingView(Context context,Runnable next){
        super(context);
        setBackgroundColor(BG);setFillViewport(true);setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setFocusableInTouchMode(true);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0,dp(12),0,dp(28));addView(root,new ScrollView.LayoutParams(-1,-2));
        addStoryProgress(root,1);
        stage=new MotionStage(context);stage.setMinimumHeight(dp(400));
        root.addView(stage,new LinearLayout.LayoutParams(-1,dp(408),1f));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(24),0,dp(24),0);root.addView(copy,new LinearLayout.LayoutParams(-1,-2));
        TextView title=label("One Agent.\nYour entire phone.",29,0xff111111,650);
        title.setLetterSpacing(-.037f);title.setLineSpacing(dp(1),1);append(copy,title,20);
        TextView description=label("Everything connected.\nIntelligently managed.",13,0xff777780,400);
        description.setLineSpacing(dp(3),1);append(copy,description,19);
        Button button=new Button(context);button.setText("Continue");button.setAllCaps(false);
        button.setTextSize(16);button.setTextColor(Color.WHITE);button.setTypeface(font(600));
        button.setLetterSpacing(0);button.setGravity(Gravity.CENTER);button.setMinimumHeight(dp(54));
        button.setMinHeight(dp(54));button.setMinimumWidth(0);button.setMinWidth(0);
        button.setPadding(dp(16),dp(15),dp(16),dp(15));button.setStateListAnimator(null);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),shape(BLUE,100),shape(Color.WHITE,100)));
        button.setOnClickListener(v->advanceStory());append(copy,button,30);
        configureStory(10000L,.90f,true,stage::setTime,next);
    }
    private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private static Typeface font(int weight){return Build.VERSION.SDK_INT>=28?Typeface.create(Typeface.DEFAULT,weight,false):Typeface.create(weight>=500?"sans-serif-medium":"sans-serif",Typeface.NORMAL);}
    private TextView label(String value,int sp,int color,int weight){
        TextView t=new TextView(getContext());t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        t.setTypeface(font(weight));t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;
    }
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void append(LinearLayout parent,View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);parent.addView(v,p);}
    private static final class MotionStage extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path=new Path(),clip=new Path();
        private final RectF rect=new RectF();
        private final Typeface medium=font(500),semibold=font(600);
        private final Shader[] gradients=new Shader[4];
        private final Shader gloss=new LinearGradient(0,0,0,1,0x55ffffff,0x00ffffff,Shader.TileMode.CLAMP);
        private final Shader glow=new RadialGradient(180,190,180,new int[]{0xbfd2e5ff,0x00f5f5f7},null,Shader.TileMode.CLAMP);
        private final Shader beam=new LinearGradient(-55,0,0,0,new int[]{0x00ffffff,0x99ffffff,0xeeffffff},new float[]{0,.75f,1},Shader.TileMode.CLAMP);
        private static final String[] NAMES={"Photos","Videos","Apps","Files"};
        private static final String[] DETAILS={"Organize memories","Optimize storage","Keep what you use","Reduce clutter"};
        private static final float[][] BOX={{30,105,150,130},{188,105,142,88},{188,201,142,88},{30,243,150,46}};
        private static final int[] TOP={0xff67b9ff,0xffb5d6ff,0xffdbeaff,0xffe7edf5};
        private static final int[] BOTTOM={0xff0063dd,0xff70a8ef,0xffa5c3ed,0xffc7d5e7};
        private float t=.04f;
        MotionStage(Context c){
            super(c);
            for(int i=0;i<4;i++)gradients[i]=new LinearGradient(0,0,1,1,TOP[i],BOTTOM[i],Shader.TileMode.CLAMP);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setContentDescription("The Agent can connect photos, videos, apps and files into a plan for your review, within the access you grant.");
        }
        void setTime(float value){t=value;invalidate();}
        private static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
        private float phase(float a,float b){return smooth((t-a)/(b-a));}
        private static float mix(float a,float b,float f){return a+(b-a)*f;}
        private void color(int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);}
        private void round(Canvas c,float x,float y,float w,float h,float r,int col){color(col);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
        private void text(Canvas c,String value,float x,float y,float size,int col,Typeface font,Paint.Align align){
            color(col);p.setTypeface(font);p.setTextSize(size);p.setTextAlign(align);c.drawText(value,x,y,p);
        }
        private void left(Canvas c,String value,float x,float y,float size,int col,Typeface font){text(c,value,x,y,size,col,font,Paint.Align.LEFT);}
        private void centered(Canvas c,String value,float x,float y,float size,int col,Typeface font){text(c,value,x,y,size,col,font,Paint.Align.CENTER);}
        private void stroke(int color,float width){color(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);}
        private void spark(Canvas c,float x,float y,float r,int col){
            color(col);path.reset();path.moveTo(x,y-r);path.quadTo(x+r*.22f,y-r*.22f,x+r,y);
            path.quadTo(x+r*.22f,y+r*.22f,x,y+r);path.quadTo(x-r*.22f,y+r*.22f,x-r,y);
            path.quadTo(x-r*.22f,y-r*.22f,x,y-r);path.close();c.drawPath(path,p);
        }
        private void icon(Canvas c,int kind,float x,float y,float size,int col){
            c.save();c.translate(x-size/2,y-size/2);c.scale(size/24,size/24);
            if(kind==0){
                stroke(col,1.7f);c.drawRoundRect(2,3,22,21,5,5,p);c.drawCircle(8,9,2,p);
                path.reset();path.moveTo(3,18);path.lineTo(9,12);path.lineTo(14,16);path.lineTo(18,11);path.lineTo(21,15);c.drawPath(path,p);
            }else if(kind==1){
                stroke(col,1.7f);c.drawRoundRect(2,4,22,20,5,5,p);color(col);path.reset();path.moveTo(10,8);path.lineTo(16,12);path.lineTo(10,16);path.close();c.drawPath(path,p);
            }else if(kind==2){
                for(int j=0;j<4;j++)round(c,2+(j%2)*11,2+(j/2)*11,9,9,2.5f,col);
            }else{
                color(col);path.reset();path.moveTo(2,5);path.lineTo(10,5);path.lineTo(13,8);path.lineTo(22,8);path.lineTo(22,20);path.lineTo(2,20);path.close();c.drawPath(path,p);
            }c.restore();
        }
        private void block(Canvas c,int i){
            float[] b=BOX[i];
            // All tiles contract within their original bounds before moving to separate rows.
            float shrink=phase(.27f+i*.013f,.40f+i*.013f);
            float move=phase(.43f+i*.025f,.54f+i*.025f);
            float w=mix(b[2],40,shrink),h=mix(b[3],40,shrink);
            float x=mix(b[0]+(b[2]-w)/2,30,move);
            float y=mix(b[1]+(b[3]-h)/2,85+65*i,move);
            float radius=mix(i==3?15:22,12,shrink);
            round(c,x,y+4,w,h,radius,0x0923528a);round(c,x,y+2,w,h,radius,0x0c23528a);
            c.save();c.translate(x,y);c.scale(w,h);
            color(Color.WHITE);p.setShader(gradients[i]);c.drawRoundRect(0,0,1,1,radius/w,radius/h,p);
            p.setShader(gloss);c.drawRoundRect(0,0,1,1,radius/w,radius/h,p);c.restore();p.setShader(null);
            stroke(0x55ffffff,.8f);c.drawRoundRect(x+.5f,y+.5f,x+w-.5f,y+h-.5f,radius,radius,p);
            float initial=1-phase(.25f,.36f);
            int glyph=i==3?0xff7495bb:Color.WHITE;
            float iconX=i==3?mix(x+53,x+w/2,shrink):x+w/2;
            float iconY=i==3?y+h/2:mix(y+(i==0?48:29),y+h/2,shrink);
            icon(c,i,iconX,iconY,mix(i==0?44: i==3?22:34,23,shrink),glyph);
            if(initial>.001f){
                c.saveLayerAlpha(x,y,x+w,y+h,Math.round(initial*255));
                if(i==3)left(c,NAMES[i],x+78,y+28,12,0xff4c6384,medium);
                else centered(c,NAMES[i],x+w/2,y+(i==0?101:66),i==0?13:12,i==2?0xff4b6d9d:Color.WHITE,medium);
                c.restore();
            }
        }
        private void row(Canvas c,int i){
            float appear=phase(.54f+i*.025f,.61f+i*.025f);
            if(appear<=0)return;
            float y=85+65*i;
            c.saveLayerAlpha(77,y-4,336,y+56,Math.round(appear*255));
            c.translate(7*(1-appear),0);
            left(c,NAMES[i],84,y+16,14,0xff182639,semibold);
            left(c,DETAILS[i],84,y+32,10,0xff7d8592,medium);
            round(c,274,y+9,55,22,11,0xffe4efff);
            centered(c,"Review",301.5f,y+23,10,BLUE,medium);
            if(i<3){stroke(0xffdfe3e9,1);c.drawLine(84,y+50,330,y+50,p);}
            c.restore();
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float scale=Math.min(getWidth()/360f,getHeight()/408f);
            if(scale<=0)return;
            canvas.save();canvas.translate((getWidth()-360*scale)/2,(getHeight()-408*scale)/2);canvas.scale(scale,scale);
            float opacity=t<.03f?mix(.65f,1,phase(0,.03f)):1-phase(.95f,1);
            canvas.saveLayerAlpha(0,0,360,408,Math.round(opacity*255));
            color(Color.WHITE);p.setShader(glow);p.setAlpha(Math.round(255*(.5f+.5f*(1-phase(.25f,.5f)))));canvas.drawOval(0,9,360,371,p);p.setShader(null);p.setAlpha(255);
            float intro=1-phase(.27f,.36f);
            if(intro>0){
                canvas.saveLayerAlpha(20,10,340,100,Math.round(intro*255));
                spark(canvas,180,45,16+2*phase(.04f,.20f),BLUE);spark(canvas,199,35,9,0xff82bbff);
                left(canvas,"Your phone",30,85,15,0xff24334b,semibold);
                color(BLUE);canvas.drawCircle(269,77,3,p);left(canvas,"Analyzing",280,81,11,BLUE,medium);
                canvas.restore();
            }
            for(int i=0;i<4;i++)block(canvas,i);
            if(t>.04f&&t<.28f){
                float sweep=mix(0,355,Math.max(0,Math.min(1,(t-.04f)/.23f)));
                canvas.save();clip.reset();
                for(float[] b:BOX){rect.set(b[0],b[1],b[0]+b[2],b[1]+b[3]);clip.addRoundRect(rect,b[3]<50?15:22,b[3]<50?15:22,Path.Direction.CW);}
                canvas.clipPath(clip);canvas.translate(sweep,0);color(Color.WHITE);p.setShader(beam);canvas.drawRect(-55,105,0,289,p);stroke(0xeeffffff,2);canvas.drawLine(0,105,0,289,p);canvas.restore();
            }
            float plan=phase(.43f,.51f);
            if(plan>0){
                canvas.saveLayerAlpha(20,20,340,75,Math.round(plan*255));canvas.translate(0,7*(1-plan));
                spark(canvas,40,49,10,BLUE);left(canvas,"Your Agent's plan",62,55,19,0xff152236,semibold);canvas.restore();
            }
            for(int i=0;i<4;i++)row(canvas,i);
            float done=phase(.69f,.79f);
            if(done>0){
                canvas.saveLayerAlpha(55,340,305,405,Math.round(done*255));canvas.translate(0,7*(1-done));
                round(canvas,63,351,234,41,21,0xffe2f1e9);color(0xff29a96a);canvas.drawCircle(87,371.5f,11,p);
                float check=phase(.76f,.85f);stroke(Color.WHITE,2);
                path.reset();path.moveTo(82,371);if(check<.4f)path.lineTo(mix(82,86,check/.4f),mix(371,375,check/.4f));
                else{path.lineTo(86,375);path.lineTo(mix(86,93,(check-.4f)/.6f),mix(375,367,(check-.4f)/.6f));}
                canvas.drawPath(path,p);left(canvas,"Ready for your approval",106,376,12,0xff258553,semibold);canvas.restore();
            }
            canvas.restore();canvas.restore();
        }
    }
}

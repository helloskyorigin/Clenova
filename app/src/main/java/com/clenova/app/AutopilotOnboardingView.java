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

/** Native introduction: an animated explanation of approved actions, never an actual cleanup. */
final class AutopilotOnboardingView extends OnboardingStoryView {
    private static final int BG=AgentOnboardingView.BACKGROUND, BLUE=0xff007aff;
    private final MotionStage stage;

    AutopilotOnboardingView(Context context,Runnable next){
        super(context);
        setBackgroundColor(BG);setFillViewport(true);setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setFocusableInTouchMode(true);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0,dp(12),0,dp(28));addView(root,new ScrollView.LayoutParams(-1,-2));
        addStoryProgress(root,2);
        stage=new MotionStage(context);stage.setMinimumHeight(dp(400));
        root.addView(stage,new LinearLayout.LayoutParams(-1,dp(408),1f));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(24),0,dp(24),0);root.addView(copy,new LinearLayout.LayoutParams(-1,-2));
        TextView title=label("On autopilot.\nIn your control.",29,0xff111111,650);
        title.setLetterSpacing(-.037f);title.setLineSpacing(dp(1),1);append(copy,title,20);
        TextView description=label("Your Agent handles the work.\nYou decide what happens.",13,0xff777780,400);
        description.setLineSpacing(dp(3),1);append(copy,description,19);
        Button button=new Button(context);button.setText("Continue");button.setAllCaps(false);
        button.setTextSize(16);button.setTextColor(Color.WHITE);button.setTypeface(font(600));
        button.setLetterSpacing(0);button.setGravity(Gravity.CENTER);button.setMinimumHeight(dp(54));
        button.setMinHeight(dp(54));button.setMinimumWidth(0);button.setMinWidth(0);
        button.setPadding(dp(16),dp(15),dp(16),dp(15));button.setStateListAnimator(null);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),shape(BLUE,100),shape(Color.WHITE,100)));
        button.setOnClickListener(v->advanceStory());append(copy,button,30);
        configureStory(14000L,.96f,false,stage::setTime,next);
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
        private final Typeface medium=font(500),semibold=font(600),headline=font(650);
        private final Shader[] gradients=new Shader[4];
        private final Shader gloss=new LinearGradient(0,0,0,1,0x55ffffff,0x00ffffff,Shader.TileMode.CLAMP);
        private final Shader glow=new RadialGradient(180,115,142,new int[]{0xe6b3e7ce,0x00f5f5f7},null,Shader.TileMode.CLAMP);
        private final Shader beam=new LinearGradient(-50,0,0,0,new int[]{0x00ffffff,0xbfffffff,0x00ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP);
        private static final String[] NAMES={"Photos","Videos","Apps","Files"};
        private static final String[] DETAILS={"Organize memories","Optimize storage","Keep what you use","Reduce clutter"};
        private static final String[] ACTIONS={"Organizing photos","Optimizing videos","Managing apps","Organizing files"};
        private static final float[] FINISH={.32f,.40f,.48f,.55f};
        private static final float[] SAVINGS={.10f,.11f,.07f,.08f};
        private final Shader green=new LinearGradient(0,0,1,1,0xff91e6b9,0xff178d51,Shader.TileMode.CLAMP);
        private final Shader ring=new LinearGradient(118,53,242,177,0xff91e6b9,0xff178d51,Shader.TileMode.CLAMP);
        private final Shader medal=new RadialGradient(166,97,85,new int[]{0xffffffff,0xffedf9f2,0xffcce9d9},new float[]{0,.55f,1},Shader.TileMode.CLAMP);
        private final Shader paper=new LinearGradient(0,251,0,389,0xffffffff,0xfff4f8f6,Shader.TileMode.CLAMP);
        private static final int[] TOP={0xff67b9ff,0xffb5d6ff,0xffdbeaff,0xffe7edf5};
        private static final int[] BOTTOM={0xff0063dd,0xff70a8ef,0xffa5c3ed,0xffc7d5e7};
        private float t=0f;
        MotionStage(Context c){
            super(c);
            for(int i=0;i<4;i++)gradients[i]=new LinearGradient(0,0,1,1,TOP[i],BOTTOM[i],Shader.TileMode.CLAMP);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setContentDescription("See how approving an Agent plan can lead to completed actions and more free space. Actual changes require your permissions and confirmation.");
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
        private static final int INK=0xff182639,MUTED=0xff7d8592,GREEN=0xff258553;
        private void gradient(Canvas c,float x,float y,float w,float h,float radius,Shader shader,boolean glass){
            if(w<=0||h<=0)return;
            c.save();c.translate(x,y);c.scale(w,h);
            color(Color.WHITE);p.setShader(shader);c.drawRoundRect(0,0,1,1,radius/w,radius/h,p);
            if(glass){p.setShader(gloss);c.drawRoundRect(0,0,1,1,radius/w,radius/h,p);}
            c.restore();p.setShader(null);
        }
        private void tile(Canvas c,int i,float x,float y,float size){
            float radius=size*.30f;
            round(c,x,y+size*.10f,size,size,radius,0x0923528a);
            gradient(c,x,y,size,size,radius,gradients[i],true);
            icon(c,i,x+size/2,y+size/2,size*.60f,i==3?0xff7495bb:Color.WHITE);
        }
        private void line(Canvas c,float x1,float y1,float x2,float y2,int col,float width){
            stroke(col,width);c.drawLine(x1,y1,x2,y2,p);
        }
        private void check(Canvas c,float x,float y,float scale,int col,float progress){
            if(progress<=0)return;
            c.save();c.translate(x,y);c.scale(scale,scale);stroke(col,1.9f);
            path.reset();path.moveTo(-4.5f,0);
            if(progress<.36f){float f=progress/.36f;path.lineTo(mix(-4.5f,-1,f),3.5f*f);}
            else{path.lineTo(-1,3.5f);float f=(progress-.36f)/.64f;path.lineTo(mix(-1,5,f),mix(3.5f,-4,f));}
            c.drawPath(path,p);c.restore();
        }
        private void status(Canvas c,int i,float y,boolean plan){
            float x=314,cy=y+20;
            if(plan){color(BLUE);c.drawCircle(x,cy,10,p);check(c,x,cy,1,Color.WHITE,1);return;}
            float end=FINISH[i],done=phase(end-.025f,end);
            if(done>0){
                c.saveLayerAlpha(x-13,cy-13,x+13,cy+13,Math.round(255*done));
                color(0xffdef2e6);c.drawCircle(x,cy,10,p);check(c,x,cy,1,0xff269e60,done);c.restore();
            }
            if(done<1){
                c.saveLayerAlpha(x-13,cy-13,x+13,cy+13,Math.round(255*(1-done)));
                stroke(0xffd4e3f6,2);c.drawCircle(x,cy,9,p);
                float start=i==0?.25f:FINISH[i-1];
                if(t>=start){stroke(BLUE,2);c.drawArc(x-9,cy-9,x+9,cy+9,(t-start)*5040-90,100,false,p);}
                c.restore();
            }
        }
        private void header(Canvas c,String title,float y,int size){
            spark(c,40,y-6,11,BLUE);left(c,title,62,y,size,0xff152236,semibold);
        }
        private void scenePlan(Canvas c){
            float a=1-phase(.15f,.20f);if(a<=0)return;
            c.saveLayerAlpha(0,0,360,432,Math.round(a*255));c.translate(0,-8*(1-a));
            header(c,"Your Agent's plan",51,19);
            for(int i=0;i<4;i++){
                float y=81+i*59;
                tile(c,i,30,y,40);left(c,NAMES[i],84,y+17,14,INK,semibold);
                left(c,DETAILS[i],84,y+33,11,MUTED,medium);status(c,i,y,true);
                if(i<3)line(c,84,y+49,330,y+49,0xffdfe3e9,1);
            }
            float press=phase(.10f,.13f)*(1-phase(.13f,.17f));
            c.save();c.scale(1-.04f*press,1-.04f*press,180,340);
            gradient(c,30,316,300,48,24,gradients[0],false);
            centered(c,"Approve plan",180,345,14,Color.WHITE,semibold);c.restore();
            float ripple=phase(.09f,.13f)*(1-phase(.13f,.19f));
            if(ripple>0){
                color((Math.round(150*ripple)<<24)|0x98cbff);c.drawCircle(180,340,mix(10,45,phase(.09f,.19f)),p);
            }
            centered(c,"Nothing changes without your approval.",180,390,11,MUTED,medium);
            c.restore();
        }
        private float storageFraction(){
            float used=1;
            for(int i=0;i<4;i++)used-=SAVINGS[i]*phase(FINISH[i]-.07f,FINISH[i]);
            return used;
        }
        private void workingStorage(Canvas c){
            left(c,"Storage used",30,79,11,0xff747f8d,medium);
            round(c,30,90,300,27,9,0xffe0e5ed);
            float used=storageFraction();
            c.save();clip.reset();rect.set(30,90,30+285*used,117);
            clip.addRoundRect(rect,8,8,Path.Direction.CW);c.clipPath(clip);
            float[] widths={108,73,57,38};float x=30;
            for(int i=0;i<4;i++){gradient(c,x,90,widths[i]*used,27,5,gradients[i],true);x+=(widths[i]+3)*used;}
            c.save();c.translate(mix(0,360,phase(.23f,.55f)),90);color(Color.WHITE);p.setShader(beam);c.drawRect(-50,0,0,27,p);p.setShader(null);c.restore();c.restore();
        }
        private void sceneWork(Canvas c){
            float appear=phase(.19f,.25f),a=appear*(1-phase(.55f,.62f));if(a<=0)return;
            c.saveLayerAlpha(0,0,360,432,Math.round(a*255));c.translate(0,8*(1-appear));
            header(c,"Agent at work",48,18);workingStorage(c);
            float fade=1-phase(.55f,.61f);
            c.saveLayerAlpha(22,137,338,390,Math.round(fade*255));
            c.translate(0,-5*(1-fade));
            for(int i=0;i<4;i++){
                float y=146+i*63;
                // The same four tiles are handed to the merge sequence rather than duplicated.
                float tileFade=1-phase(.55f,.57f);
                if(tileFade>0){c.saveLayerAlpha(27,y-3,75,y+48,Math.round(tileFade*255));tile(c,i,30,y,40);c.restore();}
                left(c,ACTIONS[i],84,y+24,14,INK,medium);status(c,i,y,false);
                if(i<3)line(c,84,y+50,330,y+50,0xffdfe3e9,1);
            }
            c.restore();c.restore();
        }
        private void merge(Canvas c){
            float a=phase(.55f,.57f)*(1-phase(.64f,.67f));if(a<=0)return;
            c.saveLayerAlpha(0,0,360,432,Math.round(a*255));
            for(int i=0;i<4;i++){
                float f=phase(.55f+i*.005f,.65f+i*.003f);
                float x=mix(30,163+(i%2)*20,f),y=mix(146+i*63,98+(i/2)*20,f);
                tile(c,i,x,y,mix(40,14,f));
            }
            c.restore();
        }
        private void shield(Canvas c,float x,float y){
            stroke(0xff269e60,1.5f);path.reset();path.moveTo(x,y-9);path.lineTo(x+8,y-6);
            path.lineTo(x+8,y);path.quadTo(x+8,y+6,x,y+10);path.quadTo(x-8,y+6,x-8,y);
            path.lineTo(x-8,y-6);path.close();c.drawPath(path,p);check(c,x,y,.67f,0xff269e60,1);
        }
        private void finalCard(Canvas c){
            float a=phase(.75f,.82f);if(a<=0)return;
            c.saveLayerAlpha(13,239,347,405,Math.round(a*255));c.translate(0,12*(1-a));
            round(c,23,256,314,138,23,0x09245284);round(c,23,254,314,138,23,0x0b245284);
            color(Color.WHITE);p.setShader(paper);c.drawRoundRect(23,251,337,389,23,23,p);p.setShader(null);
            stroke(Color.WHITE,1);c.drawRoundRect(23,251,337,389,23,23,p);
            left(c,"Storage",39,277,13,0xff26374c,semibold);
            left(c,"Before",39,309,11,0xff818995,medium);
            round(c,96,296,224,17,8.5f,0xffe8edf2);round(c,96,296,209,17,8.5f,0xffb3c3d6);
            left(c,"After",39,345,11,GREEN,semibold);
            round(c,96,332,224,17,8.5f,0xffe4eee8);
            // A conceptual comparison uses the same proportional reduction as the earlier bar.
            float after=209*mix(1,.64f,phase(.81f,.89f));
            gradient(c,96,332,after,17,8.5f,green,true);
            float free=phase(.86f,.91f);
            if(free>0){
                c.saveLayerAlpha(215,258,328,384,Math.round(free*255));
                round(c,228,262,92,22,11,0xffe4f4ea);centered(c,"More free space",274,277,10,0xff268b55,semibold);
                stroke(0xff63ba8b,1.3f);path.reset();path.moveTo(96+after,356);path.lineTo(96+after,361);path.lineTo(305,361);path.lineTo(305,356);c.drawPath(path,p);
                centered(c,"Freed space",(96+after+305)/2,375,10,GREEN,medium);c.restore();
            }
            c.restore();
        }
        private void sceneFinal(Canvas c){
            float a=phase(.62f,.67f);if(a<=0)return;
            c.saveLayerAlpha(0,0,360,432,Math.round(a*255));
            float halo=phase(.63f,.76f);
            c.save();c.scale(mix(.5f,1.05f,halo),mix(.5f,1.05f,halo),180,115);
            color(Color.WHITE);p.setShader(glow);p.setAlpha(Math.round(255*halo));c.drawCircle(180,115,142,p);p.setShader(null);p.setAlpha(255);c.restore();
            float wave=phase(.73f,.76f)*(1-phase(.76f,.84f));
            if(wave>0){stroke((Math.round(150*wave)<<24)|0x7dd3a4,1.5f);c.drawCircle(180,115,mix(58,108,phase(.73f,.84f)),p);}
            stroke(0xffdcebe3,4.5f);c.drawCircle(180,115,62,p);
            stroke(Color.WHITE,4.5f);p.setShader(ring);c.drawArc(118,53,242,177,-90,360*phase(.63f,.73f),false,p);p.setShader(null);
            float medalSize=mix(.65f,1.06f,phase(.64f,.74f))-.06f*phase(.74f,.78f);
            c.save();c.scale(medalSize,medalSize,180,115);
            color(0x12248754);c.drawCircle(180,121,51,p);color(0x17248754);c.drawCircle(180,118,50,p);
            color(Color.WHITE);p.setShader(medal);c.drawCircle(180,115,49,p);p.setShader(null);
            stroke(Color.WHITE,2);c.drawCircle(180,115,49,p);
            float tick=phase(.69f,.77f);
            if(tick>0){
                stroke(0xff269c60,6);path.reset();path.moveTo(158,115);
                if(tick<.34f){float f=tick/.34f;path.lineTo(mix(158,173,f),mix(115,130,f));}
                else{float f=(tick-.34f)/.66f;path.lineTo(173,130);path.lineTo(mix(173,202,f),mix(130,100,f));}
                c.drawPath(path,p);
            }
            c.restore();
            float title=phase(.72f,.79f);
            if(title>0){
                c.saveLayerAlpha(15,181,345,245,Math.round(title*255));c.translate(0,7*(1-title));
                centered(c,"All taken care of.",180,208,27,0xff152638,headline);
                centered(c,"More space for what matters.",180,231,12,MUTED,medium);c.restore();
            }
            finalCard(c);
            float safe=phase(.89f,.94f);
            if(safe>0){c.saveLayerAlpha(73,395,310,432,Math.round(safe*255));c.translate(0,5*(1-safe));
                shield(c,91,409);left(c,"Only approved actions",108,414,11,GREEN,medium);c.restore();}
            c.restore();
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float scale=Math.min(getWidth()/360f,getHeight()/432f);if(scale<=0)return;
            c.save();c.translate((getWidth()-360*scale)/2,(getHeight()-432*scale)/2);c.scale(scale,scale);
            float alpha=1-phase(.97f,1);
            c.saveLayerAlpha(0,0,360,432,Math.round(alpha*255));
            scenePlan(c);sceneWork(c);merge(c);sceneFinal(c);
            c.restore();c.restore();
        }
    }
}

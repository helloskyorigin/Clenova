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

/** Native introduction: an animated welcome and conversation; never a real chat or device operation. */
final class AgentOnboardingView extends OnboardingStoryView {
    static final int BACKGROUND=0xfff5f5f7;
    private static final int BLUE=0xff007aff;
    private final MotionStage stage;

    AgentOnboardingView(Context context,Runnable next){
        super(context);
        setBackgroundColor(BACKGROUND);setFillViewport(true);setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setFocusableInTouchMode(true);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0,dp(12),0,dp(28));addView(root,new ScrollView.LayoutParams(-1,-2));
        addStoryProgress(root,0);
        stage=new MotionStage(context);stage.setMinimumHeight(dp(400));
        root.addView(stage,new LinearLayout.LayoutParams(-1,dp(408),1f));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(24),0,dp(24),0);root.addView(copy,new LinearLayout.LayoutParams(-1,-2));
        TextView title=label("Meet your personal\nphone agent.",29,0xff111111,650);
        title.setLetterSpacing(-.037f);title.setLineSpacing(dp(1),1);append(copy,title,20);
        TextView description=label("More space. Less clutter.\nA smarter phone, always in your control.",13,0xff777780,400);
        description.setLineSpacing(dp(3),1);append(copy,description,19);
        Button button=new Button(context);button.setText("Get Started");button.setAllCaps(false);
        button.setTextSize(16);button.setTextColor(Color.WHITE);button.setTypeface(font(600));
        button.setLetterSpacing(0);button.setGravity(Gravity.CENTER);button.setMinimumHeight(dp(54));
        button.setMinHeight(dp(54));button.setMinimumWidth(0);button.setMinWidth(0);
        button.setPadding(dp(16),dp(15),dp(16),dp(15));button.setStateListAnimator(null);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),shape(BLUE,100),shape(Color.WHITE,100)));
        button.setOnClickListener(v->advanceStory());append(copy,button,30);
        configureStory(12000L,.95f,true,stage::setTime,next);
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
        private final Shader gloss=new LinearGradient(0,0,0,1,new int[]{0x94ffffff,0x10ffffff,0x00ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP);
        private final Shader glow=new RadialGradient(180,112,132,new int[]{0xe6b7d8ff,0x00f5f5f7},null,Shader.TileMode.CLAMP);
        private final Shader beam=new LinearGradient(-48,0,0,0,new int[]{0x00ffffff,0xccffffff,0x00ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP);
        private final Shader paper=new LinearGradient(0,229,0,359,0xffffffff,0xfff0f5fb,Shader.TileMode.CLAMP);
        private final Shader brand=new LinearGradient(0,0,1,1,new int[]{0xff83c9ff,0xff218dff,0xff0057ce},new float[]{0,.42f,1},Shader.TileMode.CLAMP);
        private static final String[] NAMES={"Photos","Videos","Apps","Files"};
        private static final String COMMAND="Take care of my phone.";
        private static final int[] TOP={0xff83c9ff,0xffb5d6ff,0xffdbeaff,0xffe7edf5};
        private static final int[] BOTTOM={0xff0057ce,0xff70a8ef,0xffa5c3ed,0xffc7d5e7};
        private float t=0f;
        MotionStage(Context c){
            super(c);
            for(int i=0;i<4;i++)gradients[i]=new LinearGradient(0,0,1,1,TOP[i],BOTTOM[i],Shader.TileMode.CLAMP);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            setContentDescription("Meet your personal phone agent. Ask it to take care of your phone, then review its plan for photos, videos, apps and files. You stay in control.");
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
        private void gradient(Canvas c,float x,float y,float w,float h,float radius,Shader shader,boolean glass){
            if(w<=0||h<=0)return;
            c.save();c.translate(x,y);c.scale(w,h);
            color(Color.WHITE);p.setShader(shader);c.drawRoundRect(0,0,1,1,radius/w,radius/h,p);
            if(glass){p.setShader(gloss);c.drawRoundRect(0,0,1,1,radius/w,radius/h,p);}
            c.restore();p.setShader(null);
        }
        private static final int INK=0xff152638,MUTED=0xff7d8592,GREEN=0xff258553;
        private void line(Canvas c,float x1,float y1,float x2,float y2,int col,float width){
            stroke(col,width);c.drawLine(x1,y1,x2,y2,p);
        }
        private void shadow(Canvas c,float x,float y,float w,float h,float radius){
            round(c,x,y+6,w,h,radius,0x092356a0);
            round(c,x,y+3,w,h,radius,0x0c2356a0);
        }
        private void brandMark(Canvas c){
            float m=phase(.18f,.28f),pulse=phase(.14f,.18f)*(1-m);
            float size=96*mix(1,.46f,m)*(1+.03f*pulse);
            float x=mix(132,27,m),y=mix(64,30,m);
            c.save();c.translate(x,y);c.scale(size/96,size/96);
            round(c,0,5,96,96,29,0xff075bc5);
            round(c,-2,8,100,99,30,0x120069ef);
            gradient(c,0,0,96,96,29,brand,true);
            stroke(0x90ffffff,1);c.drawRoundRect(1,1,95,95,28,28,p);
            spark(c,48,48,27,Color.WHITE);spark(c,72,29,12,0xffd9efff);
            stroke(0x90ffffff,2);path.reset();path.moveTo(17,18);path.quadTo(23,9,39,9);c.drawPath(path,p);
            c.restore();
        }
        private void welcome(Canvas c){
            float a=1-phase(.15f,.23f);
            if(a>0){
                c.saveLayerAlpha(0,0,360,310,Math.round(a*255));c.translate(0,-12*(1-a));
                centered(c,"Hi, I'm your Agent.",180,215,30,0xff142337,headline);
                centered(c,"What can I take care of?",180,245,15,MUTED,medium);
                stroke(BLUE,2.3f);path.reset();path.moveTo(162,284);
                for(int i=0;i<4;i++){float x=162+i*10;float h=(float)(7+5*Math.sin(t*34+i*.8));
                    path.quadTo(x+5,284+(i%2==0?-h:h),x+10,284);}
                c.drawPath(path,p);c.restore();
            }
        }
        private void chatHeader(Canvas c){
            float a=phase(.23f,.31f);if(a<=0)return;
            c.saveLayerAlpha(24,24,340,98,Math.round(a*255));c.translate(9*(1-a),0);
            left(c,"Your Agent",86,48,17,INK,headline);
            color(0xff2ca86b);p.setAlpha((int)Math.round(190+65*Math.sin(Math.PI*t*8)*Math.sin(Math.PI*t*8)));
            c.drawCircle(90,65,3,p);p.setAlpha(255);
            left(c,"Here to help",100,69,11,MUTED,medium);line(c,28,91,332,91,0xffdfe4ec,1);
            c.restore();
        }
        private void request(Canvas c){
            float a=phase(.29f,.35f);if(a<=0)return;
            c.saveLayerAlpha(65,96,342,174,Math.round(a*255));c.translate(0,9*(1-a));
            c.scale(mix(.98f,1,a),mix(.98f,1,a),202,131);
            shadow(c,72,105,260,53,23);gradient(c,72,105,260,53,23,brand,true);
            stroke(0x55ffffff,.8f);c.drawRoundRect(73,106,331,157,22,22,p);
            float typed=phase(.33f,.43f);
            int count=Math.min(COMMAND.length(),(int)Math.floor(typed*COMMAND.length()));
            color(Color.WHITE);p.setTypeface(medium);p.setTextSize(14);p.setTextAlign(Paint.Align.LEFT);
            c.drawText(COMMAND,0,count,90,137,p);
            float send=phase(.41f,.44f);
            if(t>=.33f&&t<.43f&&((int)(t*24)%2==0)){
                float cursor=90+p.measureText(COMMAND,0,count)+1;
                line(c,cursor,125,cursor,140,0xccffffff,1.3f);
            }
            c.saveLayerAlpha(296,116,324,146,Math.round(255*mix(.45f,1,send)));
            color(0x36ffffff);c.drawCircle(310,131,12,p);stroke(Color.WHITE,1.7f);
            path.reset();path.moveTo(306,131);path.lineTo(310,127);path.lineTo(314,131);
            path.moveTo(310,127);path.lineTo(310,136);c.drawPath(path,p);c.restore();c.restore();
        }
        private void thinking(Canvas c){
            float in=phase(.43f,.47f),a=in*(1-phase(.52f,.55f));
            if(a<=0)return;
            c.saveLayerAlpha(20,168,138,228,Math.round(a*255));c.translate(0,5*(1-in));
            round(c,28,178,100,39,19.5f,Color.WHITE);
            stroke(0xffe7ecf3,1);c.drawRoundRect(28,178,128,217,19.5f,19.5f,p);
            for(int i=0;i<3;i++){
                float wave=(float)(.5+.5*Math.sin((t-.43f)*116-i*1.35f));
                color(BLUE);p.setAlpha(Math.round(mix(77,255,wave)));
                c.drawCircle(61+16*i,197.5f-3*wave,3.5f,p);
            }
            p.setAlpha(255);c.restore();
        }
        private void reply(Canvas c){
            float a=phase(.53f,.59f);if(a<=0)return;
            c.saveLayerAlpha(20,166,342,224,Math.round(a*255));c.translate(0,8*(1-a));
            spark(c,37,187,10,BLUE);left(c,"Of course. I've got you.",58,187,17,0xff162638,semibold);
            left(c,"Let's make your phone work better.",58,208,12,MUTED,medium);
            c.restore();
        }
        private void plan(Canvas c){
            float a=phase(.57f,.64f);if(a<=0)return;
            c.saveLayerAlpha(14,222,346,375,Math.round(a*255));c.translate(0,10*(1-a));
            shadow(c,25,229,310,130,24);
            color(Color.WHITE);p.setShader(paper);c.drawRoundRect(25,229,335,359,24,24,p);p.setShader(null);
            stroke(Color.WHITE,1);c.drawRoundRect(25,229,335,359,24,24,p);
            left(c,"I'll prepare your plan",41,253,13,0xff182639,semibold);
            stroke(BLUE,1.7f);path.reset();path.moveTo(306,244);path.lineTo(314,252);path.lineTo(306,260);c.drawPath(path,p);
            for(int i=0;i<4;i++){
                float f=phase(.60f+i*.03f,.66f+i*.03f);if(f<=0)continue;
                float x=41+72*i;
                c.saveLayerAlpha(x-2,267,x+66,347,Math.round(f*255));c.translate(0,7*(1-f));
                gradient(c,x,270,64,66,15,gradients[i],true);
                int glyph=i==3?0xff7495bb:Color.WHITE;
                icon(c,i,x+32,292,28,glyph);
                centered(c,NAMES[i],x+32,324,10.5f,i<2?Color.WHITE:0xff45648e,medium);
                c.restore();
            }
            float shine=phase(.70f,.73f)*(1-phase(.82f,.85f));
            if(shine>0){
                c.save();clip.reset();for(int i=0;i<4;i++){float x=41+72*i;rect.set(x,270,x+64,336);clip.addRoundRect(rect,15,15,Path.Direction.CW);}
                c.clipPath(clip);c.translate(13+340*phase(.70f,.82f),270);
                color(Color.WHITE);p.setShader(beam);p.setAlpha(Math.round(180*shine));c.drawRect(-48,0,0,66,p);p.setShader(null);p.setAlpha(255);c.restore();
            }
            c.restore();
        }
        private void control(Canvas c){
            float a=phase(.77f,.85f);if(a<=0)return;
            c.saveLayerAlpha(57,374,303,431,Math.round(a*255));c.translate(0,9*(1-a));
            round(c,65,380,230,38,19,0xffe0f1e7);
            color(0xff269e60);path.reset();path.moveTo(87,388);path.lineTo(97,392);path.lineTo(97,399);
            path.quadTo(97,406,87,411);path.quadTo(77,406,77,399);path.lineTo(77,392);path.close();c.drawPath(path,p);
            float f=phase(.80f,.89f);if(f>0){
                stroke(Color.WHITE,1.9f);path.reset();path.moveTo(82,399);
                if(f<.4f)path.lineTo(mix(82,86,f/.4f),mix(399,403,f/.4f));
                else{path.lineTo(86,403);path.lineTo(mix(86,92,(f-.4f)/.6f),mix(403,395,(f-.4f)/.6f));}
                c.drawPath(path,p);
            }
            left(c,"You ask. You're in control.",108,403,12,GREEN,semibold);c.restore();
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float scale=Math.min(getWidth()/360f,getHeight()/432f);if(scale<=0)return;
            c.save();c.translate((getWidth()-360*scale)/2,(getHeight()-432*scale)/2);c.scale(scale,scale);
            c.saveLayerAlpha(0,0,360,432,Math.round((1-phase(.96f,1))*255));
            float halo=(.65f+.35f*phase(0,.12f))*(1-phase(.21f,.29f));
            if(halo>0){
                c.save();float z=mix(.85f,1.08f,phase(0,.12f));c.scale(z,z,180,112);
                color(Color.WHITE);p.setShader(glow);p.setAlpha(Math.round(255*halo));c.drawCircle(180,112,132,p);p.setShader(null);p.setAlpha(255);c.restore();
            }
            welcome(c);brandMark(c);chatHeader(c);request(c);thinking(c);reply(c);plan(c);control(c);
            float focus=phase(.45f,.50f)*(1-phase(.53f,.57f));
            if(focus>0){stroke((Math.round(205*focus)<<24)|0x83c5ff,2);c.drawRoundRect(25,27,73,76,15,15,p);}
            c.restore();c.restore();
        }
    }
}

package com.clenova.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.widget.ImageButton;
import android.widget.TextView;

/** Native shared primitives. No feature state, navigation or device operations. */
final class UiKit {
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static boolean dark(Context c,String preference){
        return UiPolish.dark(preference,(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
    }
    static int gutter(Context c){return UiPolish.gutter(c.getResources().getConfiguration().screenWidthDp);}
    static GradientDrawable shape(Context c,int color,float radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
    }
    static GradientDrawable card(Context c,boolean dark){
        UiPolish.Palette p=UiPolish.palette(dark);
        GradientDrawable d=shape(c,p.card,UiPolish.CARD_RADIUS);d.setStroke(dp(c,1),p.border);return d;
    }
    static RippleDrawable ripple(Context c,int fill,float radius,boolean dark){
        return new RippleDrawable(ColorStateList.valueOf(UiPolish.palette(dark).ripple),shape(c,fill,radius),shape(c,0xffffffff,radius));
    }
    static Typeface font(int weight){
        int w=UiPolish.fontWeight(weight);
        if(Build.VERSION.SDK_INT>=28)return Typeface.create(Typeface.DEFAULT,w,false);
        return Typeface.create(w>=600?"sans-serif-medium":w>=500?"sans-serif-medium":"sans-serif",w>=700?Typeface.BOLD:Typeface.NORMAL);
    }
    static void typography(TextView t,float size,int color,int weight){
        t.setTextSize(size);t.setTextColor(color);t.setTypeface(font(weight));t.setIncludeFontPadding(false);
    }
    static void buttonSurface(TextView t,int fill,int textColor,boolean dark){
        t.setBackground(ripple(t.getContext(),UiPolish.buttonFill(fill,textColor),UiPolish.BUTTON_RADIUS,dark));
        t.setClickable(true);t.setFocusable(true);
    }
    static ImageButton headerIcon(Context c,String kind,String description,int tint,boolean dark,Runnable action){
        ImageButton v=new ImageButton(c);v.setImageDrawable(new LineIcon(c,kind,tint));
        int pad=dp(c,(UiPolish.ICON_TARGET-UiPolish.ICON_SIZE)/2f);
        v.setPadding(pad,pad,pad,pad);v.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        v.setBackground(ripple(c,UiPolish.palette(dark).card,UiPolish.ICON_TARGET/2f,dark));
        v.setContentDescription(description);v.setClickable(true);v.setFocusable(true);v.setOnClickListener(w->action.run());return v;
    }
    private static final class LineIcon extends Drawable{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final String kind;private final int size;
        LineIcon(Context c,String kind,int tint){this.kind=kind;size=dp(c,UiPolish.ICON_SIZE);paint.setColor(tint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.85f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        @Override public int getIntrinsicWidth(){return size;}
        @Override public int getIntrinsicHeight(){return size;}
        private void line(Canvas c,float... pts){Path p=new Path();p.moveTo(pts[0],pts[1]);for(int i=2;i<pts.length;i+=2)p.lineTo(pts[i],pts[i+1]);c.drawPath(p,paint);}
        @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
            switch(kind){
                case "back":line(c,14.5f,5.5f,8,12,14.5f,18.5f);break;
                case "history":c.drawCircle(12,12,8.5f,paint);line(c,12,7,12,12,15.5f,14);break;
                case "refresh":c.drawArc(4,4,20,20,0,310,false,paint);line(c,20,4,20,12,12,12);break;
                case "undo":line(c,8,4,3.5f,9,8,14);c.drawArc(4,9,21,21,-90,180,false,paint);break;
                default:c.drawCircle(12,12,8.5f,paint);line(c,12,11,12,16.5f);c.drawPoint(12,7.5f,paint);break;
            }c.restore();
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private UiKit(){}
}

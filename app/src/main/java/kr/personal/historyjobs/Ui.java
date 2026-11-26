package kr.personal.historyjobs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Native, dependency-free presentation primitives inspired by Montage. */
final class Ui {
    static final int WHITE=0xffffffff, INK=0xff171719, SECONDARY=0xff515157,
            MUTED=0xff74747c, LINE=0xffebebef, FILL=0xfff7f7f8,
            BROWN=0xff7b533e, TINT=0xfff6f0ec, NEGATIVE=0xffb5473c;
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable shape(Context c,int color,int border,float radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));
        if(border!=0)d.setStroke(dp(c,1),border);return d;
    }
    static Drawable touch(Context c,int fill,int border,float radius){
        return new RippleDrawable(ColorStateList.valueOf(0x167b533e),shape(c,fill,border,radius),shape(c,WHITE,0,radius));
    }
    static TextView text(Context c,String s,int size,int color,boolean strong){
        TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create(strong?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setIncludeFontPadding(false);t.setLineSpacing(dp(c,4),1);return t;
    }
    static Button button(Context c,String label,boolean solid,Runnable action){
        Button b=new Button(c);b.setText(label);b.setTextSize(14);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setTextColor(solid?WHITE:INK);b.setBackground(touch(c,solid?BROWN:WHITE,solid?0:LINE,12));
        b.setStateListAnimator(null);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));b.setOnClickListener(v->action.run());return b;
    }
    static ImageButton iconButton(Context c,String icon,String label,int color,Runnable action){
        ImageButton b=new ImageButton(c);b.setImageDrawable(new Symbol(c,icon,color));b.setContentDescription(label);
        b.setBackground(touch(c,Color.TRANSPARENT,0,12));b.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c,48),dp(c,48)));b.setOnClickListener(v->action.run());return b;
    }
    static ImageView icon(Context c,String name,int color){ImageView v=new ImageView(c);v.setImageDrawable(new Symbol(c,name,color));return v;}
    static void divider(Context c,LinearLayout parent){View d=new View(c);d.setBackgroundColor(LINE);parent.addView(d,new LinearLayout.LayoutParams(-1,dp(c,1)));}
    static void tint(CompoundButton b){
        ColorStateList colors=new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{BROWN,0xffb9b9bf});
        if(b instanceof Switch){((Switch)b).setThumbTintList(ColorStateList.valueOf(WHITE));((Switch)b).setTrackTintList(colors);}
        else b.setButtonTintList(colors);
    }
    static final class Symbol extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final String name;private final int size;
        Symbol(Context c,String n,int color){name=n;size=dp(c,24);p.setColor(color);p.setStrokeWidth(1.8f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
        private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
        private void path(Canvas c,float... v){Path t=new Path();t.moveTo(v[0],v[1]);for(int i=2;i<v.length;i+=2)t.lineTo(v[i],v[i+1]);c.drawPath(t,p);}
        @Override public void draw(Canvas c){c.save();Rect r=getBounds();c.translate(r.left,r.top);c.scale(r.width()/24f,r.height()/24f);
            switch(name){
                case "search":c.drawCircle(10.5f,10.5f,6.5f,p);line(c,15.5f,15.5f,21,21);break;
                case "close":path(c,6,6,18,18);path(c,18,6,6,18);break;
                case "bookmark":case "bookmark-fill":
                    if(name.endsWith("fill"))p.setStyle(Paint.Style.FILL);
                    path(c,6,21,6,4,18,4,18,21,12,17,6,21);p.setStyle(Paint.Style.STROKE);break;
                case "list":c.drawRoundRect(4,3,20,21,3,3,p);line(c,8,8,16,8);line(c,8,12,16,12);line(c,8,16,13,16);break;
                case "boards":c.drawRoundRect(3,3,10,10,1.5f,1.5f,p);c.drawRoundRect(14,3,21,10,1.5f,1.5f,p);c.drawRoundRect(3,14,10,21,1.5f,1.5f,p);c.drawRoundRect(14,14,21,21,1.5f,1.5f,p);break;
                case "settings":c.drawCircle(12,12,7,p);c.drawCircle(12,12,2.5f,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(c,12+7*(float)Math.cos(a),12+7*(float)Math.sin(a),12+9.5f*(float)Math.cos(a),12+9.5f*(float)Math.sin(a));}break;
                case "plus":line(c,12,5,12,19);line(c,5,12,19,12);break;
                case "arrow":path(c,9,6,15,12,9,18);break;
                case "back":path(c,14,5,7,12,14,19);break;
                case "external":path(c,14,4,20,4,20,10);line(c,20,4,11,13);path(c,10,5,5,5,5,20,19,20,19,14);break;
                case "clock":c.drawCircle(12,12,9,p);path(c,12,7,12,12,16,14);break;
                case "bell":path(c,5,17,7,14,7,9,8,6,12,4,16,6,17,9,17,14,19,17,5,17);c.drawArc(10,18,14,22,0,180,false,p);break;
                case "download":path(c,12,3,12,15);path(c,7,10,12,15,17,10);path(c,4,16,4,21,20,21,20,16);break;
                case "upload":path(c,12,16,12,4);path(c,7,9,12,4,17,9);path(c,4,16,4,21,20,21,20,16);break;
                case "refresh":c.drawArc(4,4,20,20,35,285,false,p);path(c,20,4,20,10,14,10);break;
                case "check":path(c,5,12,10,17,19,7);break;
                case "school":path(c,3,10,12,4,21,10);path(c,5,9,5,21,19,21,19,9);path(c,10,21,10,15,14,15,14,21);c.drawCircle(12,10,1,p);break;
                default:c.drawCircle(12,12,9,p);line(c,12,10,12,16);c.drawPoint(12,7,p);
            }c.restore();
        }
        @Override public int getIntrinsicWidth(){return size;}@Override public int getIntrinsicHeight(){return size;}
        @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}

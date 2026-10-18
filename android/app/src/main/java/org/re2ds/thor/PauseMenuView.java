package org.re2ds.thor;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

final class PauseMenuView extends View {
    interface Action {void choose(int index);}
    private final Action action;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String[] entries=new String[0];
    private int selected,pressed=-1;
    private float scale,left,top;
    PauseMenuView(Context c,Action a){super(c);action=a;setClickable(true);setFocusable(false);setContentDescription("Pause menu");}
    void update(String[] values,int selection){entries=values;selected=selection;invalidate();}
    private void text(Canvas c,String value,float x,float y,int size,int color){paint.setTypeface(Typeface.create("serif",Typeface.BOLD));paint.setTextSize(size);paint.setColor(color);c.drawText(value,x,y,paint);}
    @Override protected void onDraw(Canvas c){
        c.drawColor(0xb0000000);float height=140+entries.length*48;
        scale=Math.min(getWidth()/720f,getHeight()/(height+48));left=(getWidth()-560*scale)/2;top=(getHeight()-height*scale)/2;
        c.save();c.translate(left,top);c.scale(scale,scale);
        paint.setColor(0xff8d969d);c.drawRect(0,0,560,height,paint);paint.setColor(0xff0b0e13);c.drawRect(3,3,557,height-3,paint);
        text(c,"PAUSE",28,43,28,0xffd5d6c8);
        for(int i=0;i<entries.length;i++){
            if(i==selected){paint.setColor(0xff28303b);c.drawRect(16,63+i*48,544,107+i*48,paint);}
            text(c,(i==selected?">  ":"   ")+entries[i],28,94+i*48,23,i==selected?0xffc6b868:0xffd5d6c8);
        }
        text(c,"A: select     B / START: resume",28,height-42,17,0xffa4aaa7);
        text(c,"Exploration preview · story, combat and audio unfinished",28,height-18,14,0xffa4aaa7);
        c.restore();
    }
    private int hit(float x,float y){if(x<16||x>544||y<63)return -1;int i=(int)(y-63)/48;return i<entries.length?i:-1;}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(scale<=0)return true;int i=hit((e.getX()-left)/scale,(e.getY()-top)/scale);
        if(e.getAction()==MotionEvent.ACTION_DOWN)pressed=i;
        else if(e.getAction()==MotionEvent.ACTION_UP){if(i>=0&&i==pressed){performClick();action.choose(i);}pressed=-1;}
        else if(e.getAction()==MotionEvent.ACTION_CANCEL)pressed=-1;
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}

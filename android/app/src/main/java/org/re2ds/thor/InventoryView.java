package org.re2ds.thor;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/** PS1-inspired status panel, using the user's original portrait and item art. */
final class InventoryView extends View {
    interface Actions {void select(int slot);void equip();void combine();void check();void exit();void answer(boolean yes);}
    private final Actions actions;
    private final RoomData.Menu menu;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap portrait;
    private final Bitmap[] icons,pictures;
    private int[] inventory=new int[17];
    private int selected,combine=-1,checking,pickupItem;
    private boolean pickupYes=true;
    private boolean paused,notification;
    private String message="";
    private float scale,left,top;
    private int pressed=-1;
    private static final int WHITE=0xffd5d6c8,GOLD=0xffc6b868,DIM=0xff8e938c;
    InventoryView(Context c,RoomData.Menu m,Actions a){
        super(c);menu=m;actions=a;portrait=RoomRenderer.bitmap(m.portrait);
        icons=new Bitmap[m.icons.length];pictures=new Bitmap[m.pictures.length];
        for(int i=0;i<icons.length;i++)icons[i]=RoomRenderer.bitmap(m.icons[i]);
        setFocusable(false);setClickable(true);setContentDescription("Leon inventory. Eight slots in two columns. Equip, combine, check and exit.");
    }
    void update(int[] inv,int slot,int source,boolean paused,boolean notification,String text){
        inventory=inv;selected=slot;combine=source;this.paused=paused;this.notification=notification;message=text;invalidate();
    }
    void pickup(int item,boolean yes){pickupItem=item;pickupYes=yes;if(item>0&&item<pictures.length&&pictures[item]==null)pictures[item]=RoomRenderer.bitmap(menu.pictures[item]);}
    void check(){int item=inventory[selected*2];if(item>0&&item<pictures.length){if(pictures[item]==null)pictures[item]=RoomRenderer.bitmap(menu.pictures[item]);checking=item;invalidate();}}
    boolean closeCheck(){if(checking==0)return false;checking=0;invalidate();return true;}
    private void rect(Canvas c,float x,float y,float w,float h,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,paint);}
    private void panel(Canvas c,float x,float y,float w,float h){
        rect(c,x,y,w,h,0xff090c10);rect(c,x,y,w,2,0xffb2b4ac);rect(c,x,y,2,h,0xff949a97);
        rect(c,x+w-3,y+2,3,h-2,0xff272b30);rect(c,x+2,y+h-3,w-2,3,0xff262a2c);
        rect(c,x+3,y+3,w-6,1,0xff505963);rect(c,x+3,y+3,1,h-6,0xff505963);
    }
    private void text(Canvas c,String text,float x,float y,int size,int color){
        paint.setTypeface(Typeface.create("serif",Typeface.BOLD));paint.setTextSize(size);paint.setColor(0xff000000);c.drawText(text,x+1,y+1,paint);paint.setColor(color);c.drawText(text,x,y,paint);
    }
    private void art(Canvas c,Bitmap b,float x,float y,float w,float h){paint.setFilterBitmap(false);paint.setColor(Color.WHITE);c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),paint);}
    private void icon(Canvas c,int item,float x,float y,float w,float h){if(item>0&&item<icons.length)art(c,icons[item],x,y,w,h);}
    @Override protected void onDraw(Canvas c){
        c.drawColor(0xff080a0d);scale=Math.min(getWidth()/640f,getHeight()/520f);left=(getWidth()-640*scale)/2;top=(getHeight()-520*scale)/2;
        c.save();c.translate(left,top);c.scale(scale,scale);
        rect(c,0,0,640,520,0xff353c42);
        // Narrow alternating strips give the frame the worn, brushed-metal look.
        for(int y=0;y<520;y+=3)rect(c,0,y,640,1,(y%9==0)?0xff454d53:0xff30373c);
        panel(c,12,12,616,44);text(c,"STATUS",28,42,24,WHITE);
        text(c,"LEON S. KENNEDY",215,41,20,WHITE);text(c,paused?"PAUSE":"",538,40,16,GOLD);
        panel(c,12,68,242,282);
        text(c,"LEON",28,96,20,WHITE);
        art(c,portrait,72,108,122,138);
        text(c,"EQUIPPED",28,273,17,DIM);
        int equipped=inventory[16],item=equipped<0?0:inventory[equipped*2];
        icon(c,item,30,285,80,60);text(c,item==0?"None":menu.name(item),120,313,15,WHITE);
        if(item>=2&&item<=0x1e)text(c,String.valueOf(inventory[equipped*2+1]),211,338,20,WHITE);
        panel(c,266,68,362,282);
        for(int i=0;i<8;i++){
            int x=276+(i%2)*172,y=78+(i/2)*65,id=inventory[i*2],amount=inventory[i*2+1];
            rect(c,x,y,166,59,0xff080b20);rect(c,x+2,y+2,162,55,0xff11132e);
            if(id>0){icon(c,id,x+37,y+2,80,54);if(id>=2&&id<=0x1e)text(c,Integer.toString(amount),x+134,y+52,20,WHITE);}
            if(equipped==i)text(c,"E",x+9,y+20,18,0xff80b88a);
            if(i==selected||i==combine){int color=i==combine?0xffbd725b:GOLD;rect(c,x,y,166,2,color);rect(c,x,y+57,166,2,color);rect(c,x,y,2,59,color);rect(c,x+164,y,2,59,color);}
        }
        String[] commands={"EQUIP","COMBINE","CHECK","EXIT"};
        for(int i=0;i<4;i++){panel(c,12+i*156,362,148,44);text(c,commands[i],28+i*156,391,19,i==3?GOLD:WHITE);}
        panel(c,12,418,616,90);
        if(checking!=0){
            panel(c,266,68,362,282);Bitmap b=pictures[checking];float w=Math.min(320,b.getWidth()*2),h=w*b.getHeight()/b.getWidth();if(h>235){h=235;w=h*b.getWidth()/b.getHeight();}
            art(c,b,447-w/2,203-h/2,w,h);text(c,"A / B: close",370,337,16,DIM);
        }
        String title=notification?"ITEM / MESSAGE":menu.name(inventory[selected*2]);
        text(c,title,28,443,19,WHITE);
        String detail=checking!=0?"Inspecting "+menu.name(checking):message;
        wrap(c,detail,28,461,580,16);
        text(c,notification?"A / B / START: continue":paused?"A: equip   X: combine   Y: check   START / B: exit":"START: pause menu     Touch an item to select",28,500,14,DIM);
        if(pickupItem>0){
            rect(c,0,60,640,460,0xc0000000);panel(c,40,84,560,392);
            text(c,"Will you take the "+menu.name(pickupItem)+"?",64,117,19,WHITE);
            if(pickupItem<pictures.length){Bitmap b=pictures[pickupItem];float h=180,w=h*b.getWidth()/b.getHeight();if(w>400){w=400;h=w*b.getHeight()/b.getWidth();}art(c,b,320-w/2,220-h/2,w,h);}
            panel(c,130,336,160,48);panel(c,350,336,160,48);
            text(c,(pickupYes?"> ":"  ")+"YES",167,368,23,pickupYes?GOLD:WHITE);
            text(c,(!pickupYes?"> ":"  ")+"NO",387,368,23,!pickupYes?GOLD:WHITE);
            text(c,"Choose with left/right. A: confirm   B: cancel",82,445,18,WHITE);
        }
        c.restore();
    }
    private void wrap(Canvas c,String value,int x,int y,int width,int size){
        paint.setTextSize(size);paint.setTypeface(Typeface.create("serif",Typeface.BOLD));
        String line="";int lines=0;
        for(String word:value.replace('\n',' ').split(" ")){
            String next=line.isEmpty()?word:line+" "+word;
            if(paint.measureText(next)>width&&!line.isEmpty()){text(c,line,x,y,size,DIM);line=word;y+=17;if(++lines==2)return;}else line=next;
        }
        if(lines<2)text(c,line,x,y,size,DIM);
    }
    private int hit(float x,float y){
        if(pickupItem>0){if(y>=336&&y<384){if(x>=130&&x<290)return 12;if(x>=350&&x<510)return 13;}return -1;}
        if(x>=276&&x<614&&y>=78&&y<332){int col=(int)(x-276)/172,row=(int)(y-78)/65;if(col<2&&row<4)return row*2+col;}
        if(y>=362&&y<406&&x>=12&&x<628)return 8+(int)(x-12)/156;
        if(y>=418&&y<508&&notification)return 11;
        return -1;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(scale<=0)return true;int hit=hit((e.getX()-left)/scale,(e.getY()-top)/scale);
        if(e.getAction()==MotionEvent.ACTION_DOWN){pressed=hit;return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            if(pickupItem==0&&closeCheck()){performClick();return true;}
            if(hit>=0&&hit==pressed){performClick();if(hit>=12)actions.answer(hit==12);else if(hit<8)actions.select(hit);else if(hit==8)actions.equip();else if(hit==9)actions.combine();else if(hit==10)actions.check();else actions.exit();}
            pressed=-1;
        }else if(e.getAction()==MotionEvent.ACTION_CANCEL)pressed=-1;
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}

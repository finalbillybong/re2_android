package org.re2ds.thor;

/** Lower-screen condition, synchronized with native combat health each game tick. */
final class PlayerHealth {
    static final int MAX_HP=200;
    private int hp=MAX_HP;
    int hp(){return hp;}
    void restore(int value){hp=Math.max(0,Math.min(MAX_HP,value));}
    void reset(){hp=MAX_HP;}
    String condition(){return hp==0?"Dead":hp<=20?"Danger":hp<=100?"Caution":"Fine";}
    int color(){return hp<=20?0xffd85b50:hp<=100?0xffd8b54f:0xff65bb81;}
}

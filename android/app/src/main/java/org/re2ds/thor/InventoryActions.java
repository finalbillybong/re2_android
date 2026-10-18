package org.re2ds.thor;

/** Item-grid confirmation opens this menu without changing equipment. */
final class InventoryActions {
    static final int PRIMARY=0,COMBINE=1,CHECK=2;
    boolean open;
    int selected;
    boolean open(int[] inventory,int slot){
        if(slot<0||slot>=8||inventory[slot*2]==0){close();return false;}
        open=true;selected=0;return true;
    }
    void close(){open=false;selected=0;}
    void move(int direction){if(open)selected=Math.floorMod(selected+direction,3);}
    static boolean weapon(int item){return item>=1&&item<=0x13;}
    static String primary(int[] inventory,int slot){
        return weapon(inventory[slot*2])?(inventory[16]==slot?"UNEQUIP":"EQUIP"):"USE";
    }
}

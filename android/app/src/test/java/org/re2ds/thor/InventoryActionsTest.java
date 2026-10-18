package org.re2ds.thor;
import org.junit.Test;
import static org.junit.Assert.*;
public class InventoryActionsTest {
    @Test public void openingAndCancellingNeverChangesEquipment(){
        int[] inventory=new int[17];inventory[0]=1;inventory[2]=2;inventory[16]=1;
        int[] original=inventory.clone();InventoryActions actions=new InventoryActions();
        assertTrue(actions.open(inventory,0));assertTrue(actions.open);assertEquals(0,actions.selected);
        actions.move(1);assertEquals(InventoryActions.COMBINE,actions.selected);
        actions.move(1);assertEquals(InventoryActions.CHECK,actions.selected);
        actions.move(1);assertEquals(InventoryActions.PRIMARY,actions.selected);
        actions.move(-1);assertEquals(InventoryActions.CHECK,actions.selected);
        actions.close();assertFalse(actions.open);assertArrayEquals(original,inventory);
    }
    @Test public void primaryCommandReflectsItemAndEquippedSlot(){
        int[] inventory=new int[17];inventory[0]=1;inventory[2]=2;inventory[4]=0x14;inventory[16]=1;
        assertEquals("EQUIP",InventoryActions.primary(inventory,0));
        assertEquals("UNEQUIP",InventoryActions.primary(inventory,1));
        assertEquals("USE",InventoryActions.primary(inventory,2));
        inventory[16]=-1;assertEquals("EQUIP",InventoryActions.primary(inventory,1));
    }
    @Test public void emptySlotsDoNotOpenAnActionMenu(){
        InventoryActions actions=new InventoryActions();int[] inventory=new int[17];inventory[0]=2;
        assertTrue(actions.open(inventory,0));assertFalse(actions.open(inventory,1));assertFalse(actions.open);
    }
}

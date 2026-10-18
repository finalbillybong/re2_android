package org.re2ds.thor;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoomInteractionTest {
 private int[] trigger(int type,int floor){return new int[]{3,type,0x31,floor,500,1000,1000,500,-200,-200,200,200,0x14,0};}
 @Test public void facingAndReachAreRequiredOutsideTrigger(){int[] t=trigger(1,0);assertEquals(0,RoomInteraction.nearest(t,0,0,0,0));assertEquals(-1,RoomInteraction.nearest(t,0,0,16384,0));assertEquals(-1,RoomInteraction.nearest(t,-101,0,0,0));}
 @Test public void floorMismatchDoesNotActivateDoor(){assertEquals(-1,RoomInteraction.nearest(trigger(1,1),600,0,0,0));assertEquals(0,RoomInteraction.nearest(trigger(1,255),600,0,0,0));}
 @Test public void pickupWinsOverOverlappingText(){int[] t=new int[28];System.arraycopy(trigger(4,0),0,t,0,14);System.arraycopy(trigger(2,0),0,t,14,14);assertEquals(14,RoomInteraction.nearest(t,600,0,0,0));}
}

package org.re2ds.thor;
import org.junit.Test;
import static org.junit.Assert.*;
public class CombatStateTest {
    @Test public void grabbedPoseAndHealthAreCopiedTogether(){
        int[] raw=new int[113];raw[0]=80;raw[1]=1|2|16;raw[2]=123;raw[5]=16384;raw[11]=77;raw[17]=4096;
        CombatState state=new CombatState(raw);raw[11]=0;
        assertEquals(80,state.hp);assertTrue(state.locked);assertTrue(state.grabbed);assertFalse(state.dead);
        assertEquals(77,state.frame[0]);assertEquals(4096,state.frame[6]);
        PlayerHealth health=new PlayerHealth();health.restore(state.hp);assertEquals("Caution",health.condition());
    }
    @Test public void finishedDeathIsDistinctFromAnInProgressDeath(){
        int[] raw=new int[113];raw[1]=1|2|4|16;
        CombatState dying=new CombatState(raw);assertTrue(dying.dead);assertFalse(dying.finished);
        raw[1]|=8;assertTrue(new CombatState(raw).finished);
        assertThrows(IllegalArgumentException.class,()->new CombatState(new int[112]));
    }
    @Test public void pauseAndFocusLossReleaseAllCombatInputs(){
        ProbeState state=new ProbeState();state.aimBumper=state.aimButton=state.aimTrigger=state.fire=state.pressed=true;
        state.releaseInput();assertFalse(state.aiming());assertFalse(state.fire);assertFalse(state.pressed);
        state.aimTrigger=true;assertTrue(state.aiming());state.aimBumper=true;state.aimTrigger=false;assertTrue(state.aiming());
    }
}

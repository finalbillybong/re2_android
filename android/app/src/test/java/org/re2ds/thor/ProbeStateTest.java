package org.re2ds.thor;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProbeStateTest {
 @Test public void touchPausesAndLifecycleClearsHeldInput() {
  ProbeState s=new ProbeState();s.axisX=1;s.axisY=-1;s.select(5);
  assertTrue(s.paused);assertEquals(5,s.selected);
  s.releaseInput();assertEquals(0,s.axisX,0);assertEquals(0,s.axisY,0);
 }
}

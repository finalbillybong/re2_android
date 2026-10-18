package org.re2ds.thor;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlayerHealthTest {
    @Test public void conditionMatchesNativeHealthThresholds(){
        PlayerHealth health=new PlayerHealth();assertEquals(200,health.hp());assertEquals("Fine",health.condition());
        health.restore(101);assertEquals("Fine",health.condition());
        health.restore(100);assertEquals("Caution",health.condition());
        health.restore(21);assertEquals("Caution",health.condition());
        health.restore(20);assertEquals("Danger",health.condition());
        health.restore(1);assertEquals("Danger",health.condition());
        health.restore(0);assertEquals("Dead",health.condition());
    }
    @Test public void restorePreservesHealthAndBoundsAndResetStartsFull(){
        PlayerHealth health=new PlayerHealth();health.restore(37);
        PlayerHealth restored=new PlayerHealth();restored.restore(health.hp());assertEquals(37,restored.hp());
        restored.restore(-5);assertEquals(0,restored.hp());
        restored.restore(201);assertEquals(200,restored.hp());
        restored.restore(1);restored.reset();assertEquals(200,restored.hp());
    }
}

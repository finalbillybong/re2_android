package org.re2ds.thor;
import java.io.FileInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
public class StoryStateTest {
    private RoomSession room()throws Exception {
        return new RoomSession(new RoomCatalog(p->{try(FileInputStream in=new FileInputStream("../../build/android-assets/re2/"+p)){return in.readAllBytes();}}).load(1,1));
    }
    private int[] state(){int[] p=new int[143];p[2]=-1;p[3]=1;for(int i=0;i<32;i++)p[111+i]=i;return p;}
    @Test public void cinematicPoseAndCameraOverrideWalkingZones()throws Exception {
        RoomSession s=room();int[] p=state();p[0]=p[1]=p[8]=1;p[2]=10;p[3]=0;
        p[4]=-16011;p[6]=-13702;p[7]=14464;p[9]=123;
        StoryState story=new StoryState(p);story.apply(s);s.switchCamera();
        assertTrue(story.locked);assertEquals(-16011,s.x,0);assertEquals(10,s.camera);assertEquals(123,s.combatFrame[0]);
    }
    @Test public void returningControlRestoresAutomaticCameraPolicy()throws Exception {
        RoomSession s=room();s.autoCamera=false;int[] p=state();p[111]=7;p[112]=8;
        new StoryState(p).apply(s);assertTrue(s.autoCamera);assertEquals(7,s.mapped(0));assertEquals(8,s.mapped(1));
        assertThrows(IllegalArgumentException.class,()->new StoryState(new int[142]));
    }
}

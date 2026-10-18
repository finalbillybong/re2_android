package org.re2ds.thor;
import java.io.FileInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoomSessionTest {
    static RoomData room;
    @BeforeClass public static void assets() throws Exception {
        room=new RoomData(p->{try(FileInputStream in=new FileInputStream("../../build/android-assets/re2/"+p)){return in.readAllBytes();}});
    }
    @Test public void openingRouteAssetsDecode() throws Exception {
        for(int id=0;id<3;id++){
            RoomData d=new RoomData(p->{try(FileInputStream in=new FileInputStream("../../build/android-assets/re2/"+p)){return in.readAllBytes();}},id);
            assertEquals(id,d.roomId);assertTrue(d.cameras.length>0);assertTrue(d.boxes.length>0);
            if(id==0)assertEquals(0,d.objects.length);
            else {
                assertTrue(d.objects.length>1);assertNotNull(d.objects[1]);
                for(RoomData.ObjectMesh m:d.objects)if(m!=null){
                    assertTrue(m.triangles.length>0);assertEquals(0,m.triangles.length%15);
                    assertEquals(m.texture.width*m.texture.height,m.texture.argb.length);
                    for(int j=3;j<m.triangles.length;j+=5){assertTrue(m.triangles[j]>=0&&m.triangles[j]<=1);assertTrue(m.triangles[j+1]>=0&&m.triangles[j+1]<=1);}
                }
            }
        }
    }
    @Test public void disabledBusCameraZonesCannotSwitchUntilEnabled()throws Exception {
        RoomData d=new RoomData(p->{try(FileInputStream in=new FileInputStream("../../build/android-assets/re2/"+p)){return in.readAllBytes();}},0x19);
        assertEquals(2,d.zones[8].role&2); // authored alternate switch starts disabled
        for(RoomData.Zone z:d.zones)z.role|=2;
        RoomData.Zone target=d.zones[6];RoomSession s=new RoomSession(d);s.camera=2;
        s.x=(target.x[0]+target.x[2])/2f;s.z=(target.z[0]+target.z[2])/2f;s.y=0;
        s.switchCamera();assertEquals(2,s.camera);
        target.role&=~2;s.switchCamera();assertEquals(target.to,s.camera);
    }
    @Test public void realAssetsDecodeAndAllAnimationReferencesAreValid() {
        assertEquals(9,room.cameras.length);assertEquals("Hand Gun",room.menu.name(2));
        for(RoomData.Model m:room.models){assertEquals(15,m.bones);assertFalse(m.meshes.isEmpty());for(int[] sequence:m.animations)for(int f:sequence)assertTrue(f<m.frames.length);}
        assertNotNull(room.cameras[0].mask);assertNull(room.cameras[7].mask);
        assertEquals(320*240,room.cameras[0].background.argb.length);
    }
    @Test public void pauseAndWakeDoNotAdvanceRoom() {
        RoomSession s=new RoomSession(room);s.step(10,1,-1,true,true,2);
        assertEquals(19800,s.x,0);assertEquals(16384,s.direction,0);
        s.step(10,0,0,false,false,2);assertEquals(19800,s.x,0);
        assertTrue(s.key<=1);
    }
    @Test public void rootMotionMovesLeonAndTurnsInUpstreamDirection() {
        RoomSession s=new RoomSession(room);
        for(int i=0;i<20;i++)s.step(1f/30,0,-1,false,false,2);
        assertTrue(s.x<19800);assertEquals(300,s.z,.1);
        float x=s.x;s.step(1f/30,1,0,false,false,2);
        assertTrue(s.direction>16384);assertEquals(x,s.x,0);
    }
    @Test public void footstepsFollowOriginalContactFramesAndStopWhenPausedOrIdle() {
        for(int weapon:new int[]{0,1,2})for(int mode=0;mode<4;mode++){
            RoomSession s=new RoomSession(room);int contacts=0;
            float turn=mode==3?1:0,forward=mode==3?0:mode==2?1:-1;boolean run=mode==1;
            for(int tick=0;tick<90;tick++){
                s.step(1f/30,turn,forward,run,false,weapon);
                int expected=(room.models[weapon].animationFlags[s.animation][s.key]&0x4000)!=0?(run?2:1):0;
                assertEquals(expected,s.footstep);if(s.footstep!=0)contacts++;
            }
            assertTrue(contacts>0);
            s.step(1f/30,turn,forward,run,true,weapon);assertEquals(0,s.footstep);
            for(int tick=0;tick<60;tick++){s.step(1f/30,0,0,false,false,weapon);assertEquals(0,s.footstep);}
        }
    }
    @Test public void obstaclesPermitEscapeButBlockDeeperEntry() {
        RoomData.Box b=new RoomData.Box();b.x=0;b.z=0;b.w=1000;b.d=1000;
        assertEquals(0,RoomSession.penetration(b,-401,500),0);
        assertEquals(400,RoomSession.penetration(b,0,500),0);
        assertTrue(RoomSession.penetration(b,500,500)>RoomSession.penetration(b,0,500));
        b.shape=6;assertEquals(0,RoomSession.penetration(b,-401,500),0);
    }
    @Test(expected=IllegalArgumentException.class) public void corruptCompressionIsRejected(){RoomData.decompress(new byte[]{0x10,3,0,0,(byte)128,0,0},0);}
}

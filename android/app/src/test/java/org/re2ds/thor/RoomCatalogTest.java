package org.re2ds.thor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoomCatalogTest {
    private RoomCatalog catalog()throws Exception {
        return new RoomCatalog(p->{try(java.io.FileInputStream in=new java.io.FileInputStream("../../build/android-assets/re2/"+p)){return in.readAllBytes();}});
    }
    @Test public void everyRoomAndItsOriginalArtworkDecode()throws Exception {
        RoomCatalog catalog=catalog();assertEquals(125,catalog.keys.length);
        RoomData.Model player=null;int cameras=0;
        for(int key:catalog.keys){
            RoomData room=catalog.load(key/256,key%256);
            assertEquals(key,room.stage*256+room.roomId);
            if(player==null)player=room.models[0];else assertSame(player,room.models[0]);
            for(RoomData.Camera camera:room.cameras){assertEquals(320,camera.background.width);assertEquals(240,camera.background.height);cameras++;}
        }
        assertEquals(1253,cameras);
        assertFalse(catalog.contains(0,0));assertFalse(catalog.contains(1,256));
    }
    @Test public void stairsChangeHeightAndFloorAndPausedMovementDoesNot()throws Exception {
        RoomData room=catalog().load(1,3);RoomSession s=new RoomSession(room);
        RoomData.Box stair=null;
        for(RoomData.Box b:room.boxes)if((b.shape==11||b.shape==12)&&((b.type>>6)&31)>0){stair=b;break;}
        assertNotNull(stair);
        RoomData.Box b=stair;
        s.x=b.x;s.z=b.z;s.y=0;
        int dir=(b.type>>4)&3;
        if(dir==1)s.x+=b.w;if(dir==3)s.z+=b.d;
        s.updateHeight();assertEquals(0,s.y,.01);
        if(dir<2)s.x=b.x+b.w*.5f;else s.z=b.z+b.d*.5f;
        s.updateHeight();assertEquals(((b.type>>6)&31)*-900,s.y,.01);
        float y=s.y;s.step(1,0,-1,true,true,2);assertEquals(y,s.y,0);
        s.y=-3600;assertEquals(2,s.floor());assertEquals(-3600,s.pose().y,0);
    }
}

package org.re2ds.thor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;
public class DoorAnimationTest {
    private DoorAnimation.Catalog load() throws Exception {
        return new DoorAnimation.Catalog(p->Files.readAllBytes(Path.of("../../build/android-assets/re2",p)));
    }
    @Test public void originalDoorsCoverTheFullCatalogAndAnimate() throws Exception {
        DoorAnimation.Catalog doors=load();assertEquals(279,doors.entries.size());
        // The gun-shop door is DOOR0B in opposite variants; it is not DOOR00/01.
        assertEquals(Integer.valueOf(0x0b00),doors.entries.get(0x10000));
        assertEquals(Integer.valueOf(0x0b01),doors.entries.get(0x10100));
        HashSet<Integer> checked=new HashSet<>();
        for(int key:doors.entries.keySet()){
            if(!checked.add(doors.entries.get(key)))continue;
            DoorAnimation d=doors.get(key/65536,(key/256)%256,key%256);
            if(doors.entries.get(key)/256!=0x28)assertFalse("Door "+doors.entries.get(key),d.model.meshes.isEmpty());assertEquals(60,d.fps);
            assertTrue(d.frames>0&&d.frames<1500);
            boolean visible=false;
            for(int[] pose:d.poses)for(int i=0;i<8;i++)visible|=pose[1+i*8]>=0;
            int kind=doors.entries.get(key)/256;
            if(kind!=0x28&&kind!=0x32&&kind!=0x34&&kind!=0x36)assertTrue("Door "+doors.entries.get(key),visible);
        }
        assertEquals(106,checked.size());
    }
    @Test public void rejectsTruncatedAnimation() throws Exception {
        DoorAnimation d=load().get(1,0,0);
        byte[] bytes=Files.readAllBytes(Path.of("../../build/android-assets/re2/door/door0b_00.bin"));
        assertThrows(IllegalArgumentException.class,()->new DoorAnimation(d.model,Arrays.copyOf(bytes,bytes.length-4)));
    }
}

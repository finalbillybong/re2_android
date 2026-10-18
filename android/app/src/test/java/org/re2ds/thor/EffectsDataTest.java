package org.re2ds.thor;
import java.io.FileInputStream;
import org.junit.Test;
import static org.junit.Assert.*;
public class EffectsDataTest {
    private byte[] read(String path)throws java.io.IOException {
        try(FileInputStream in=new FileInputStream("../../build/android-assets/re2/"+path)){return in.readAllBytes();}
    }
    @Test public void originalBloodFlashCasingAndSmokePagesDecode()throws Exception {
        EffectsData effects=new EffectsData(this::read);
        for(int id=0;id<=3;id++){
            RoomData.Pixels page=effects.texture(id==0?0:1,id,0);
            assertEquals(256,page.width);assertEquals(page.width*page.height,page.argb.length);
            int opaque=0,transparent=0;for(int color:page.argb){if(color==0)transparent++;else{assertEquals(255,color>>>24);opaque++;}}
            assertTrue(opaque>0);assertTrue(transparent>0);
            assertSame(page,effects.texture(id==0?0:1,id,0));
        }
        assertNotSame(effects.texture(1,3,0),effects.texture(1,3,1));
        assertThrows(IllegalArgumentException.class,()->effects.texture(1,255,0));
    }
    @Test public void originalGunShopGlassPagesDecode()throws Exception {
        EffectsData effects=new EffectsData(this::read).forRoom(1,1);
        for(int id=0x11;id<=0x13;id++)assertEquals(256,effects.texture(1,id,0).width);
    }
    @Test public void routeEffectPagesDecode()throws Exception {
        for(int room:new int[]{0,2,3,0x18,0x19,0x1a,0x1b}){
            EffectsData effects=new EffectsData(this::read).forRoom(1,room);
            java.nio.ByteBuffer b=RoomData.bytes(read(String.format(java.util.Locale.US,"room/room1%02x0_esp.bin",room)));
            for(int i=0;i<RoomData.u16(b,4);i++){
                int o=8+i*12,id=RoomData.u8(b,o),rows=RoomData.u8(b,o+1);
                for(int row=0;row<rows;row++)assertEquals(256,effects.texture(1,id,row).width);
            }
        }
    }
    @Test public void truncatedOriginalPagesAreRejected(){
        assertThrows(RuntimeException.class,()->new EffectsData(path->new byte[8]));
    }
}

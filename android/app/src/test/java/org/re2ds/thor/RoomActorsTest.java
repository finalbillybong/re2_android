package org.re2ds.thor;
import java.io.FileInputStream;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;
public class RoomActorsTest {
    private byte[] read(String path)throws java.io.IOException {
        try(FileInputStream in=new FileInputStream("../../build/android-assets/re2/"+path)){return in.readAllBytes();}
    }
    @Test public void originalOpeningSkeletonsAndSnapshotsDecode()throws Exception {
        RoomActors actors=new RoomActors(this::read);
        for(int type:new int[]{0x10,0x11,0x12,0x13,0x48}){
            String path=String.format(Locale.US,"model/em%02x",type);
            RoomData.Model original=RoomData.model(read(path+".bin"),read(path+"_tex.bin"));
            assertFalse(original.meshes.isEmpty());
            int[] record=new int[RoomActors.STRIDE];record[1]=type;record[2]=1234;record[7]=original.bones;record[8]=789;record[11]=4096;
            RoomActors.Actor a=actors.decode(record)[0];
            assertEquals(1234,a.x);assertEquals(789,a.frame[0]);assertEquals(4096,a.frame[6]);
            assertSame(a.model,actors.decode(record)[0].model);
            record[8]=0;assertEquals(789,a.frame[0]); // GL snapshot cannot change underneath a frame.
            actors.clear();assertNotSame(a.model,actors.decode(record)[0].model);
        }
    }
    @Test public void zombieVariantsUseOriginalBodyTexturesAndKeepHeadAndHands()throws Exception {
        RoomActors actors=new RoomActors(this::read);
        RoomData.Model base=RoomData.model(read("model/em1f.bin"),read("model/em1f_tex.bin"));
        RoomData.Model previous=null;
        for(int variant=1;variant<=3;variant++){
            int[] record=new int[RoomActors.STRIDE];record[1]=0x1f;record[7]=15;record[107]=variant;
            RoomData.Model model=actors.decode(record)[0].model;
            assertSame(model,actors.decode(record)[0].model);assertNotSame(previous,model);
            for(int i=0;i<model.meshes.size();i++){
                RoomData.Mesh mesh=model.meshes.get(i),original=base.meshes.get(i);
                assertArrayEquals(original.triangles,mesh.triangles,0);
                assertEquals(mesh.bone==8||mesh.bone==11||mesh.bone==14?original.page:variant,mesh.page);
            }
            if(previous!=null)assertFalse(java.util.Arrays.equals(previous.textures[variant-1].argb,model.textures[variant].argb));
            previous=model;
        }
    }
    @Test public void invalidNativeSkeletonIsRejected()throws Exception {
        RoomActors actors=new RoomActors(this::read);
        int[] record=new int[RoomActors.STRIDE];record[1]=0x48;record[7]=33;
        assertThrows(IllegalArgumentException.class,()->actors.decode(record));
        assertThrows(IllegalArgumentException.class,()->actors.decode(new int[1]));
    }
}

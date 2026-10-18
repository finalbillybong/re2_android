package org.re2ds.thor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Main-thread asset cache and immutable poses handed to the GL thread. */
final class RoomActors {
    static final int STRIDE=108;
    static final Actor[] EMPTY=new Actor[0];
    private final RoomData.Reader reader;
    private final Map<Integer,RoomData.Model> models=new HashMap<>();
    RoomActors(RoomData.Reader reader){this.reader=reader;}
    void clear(){models.clear();}
    Actor[] decode(int[] records) throws IOException {
        RoomData.require(records.length%STRIDE==0,"Invalid native actor snapshot");
        Actor[] result=new Actor[records.length/STRIDE];
        for(int i=0;i<result.length;i++){
            int o=i*STRIDE,type=records[o+1];
            int appearance=type==0x1f?records[o+107]:0;
            RoomData.require(appearance>=0&&appearance<=3,"Invalid zombie appearance");
            int cacheKey=type|(appearance<<8);
            RoomData.Model model=models.get(cacheKey);
            if(model==null){
                String path=String.format(Locale.US,"model/em%02x",type);
                model=RoomData.model(reader.read(path+".bin"),reader.read(path+"_tex.bin"));
                if(type==0x1f&&appearance!=0){
                    // Original zombie init changes body pages; head (8) and
                    // hands (11,14) retain their authored textures.
                    for(int j=0;j<model.meshes.size();j++){
                        RoomData.Mesh mesh=model.meshes.get(j);
                        if(mesh.bone!=8&&mesh.bone!=11&&mesh.bone!=14)
                            model.meshes.set(j,new RoomData.Mesh(mesh.bone,appearance,mesh.triangles));
                    }
                }
                models.put(cacheKey,model);
            }
            RoomData.require(model.bones==records[o+7]&&model.bones<=32,"Actor skeleton mismatch");
            result[i]=new Actor(model,records,o);
        }
        return result;
    }
    static final class Actor {
        final RoomData.Model model;
        final int slot,type,x,y,z,direction,flinch;
        final short[] frame;
        Actor(RoomData.Model model,int[] p,int o){
            this.model=model;slot=p[o];type=p[o+1];x=p[o+2];y=p[o+3];z=p[o+4];direction=p[o+5];flinch=p[o+6];
            frame=new short[6+3*model.bones];
            for(int k=0;k<3;k++)frame[k]=(short)p[o+8+k];
            for(int k=0;k<3*model.bones;k++)frame[6+k]=(short)p[o+11+k];
        }
    }
}

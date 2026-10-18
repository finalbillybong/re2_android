package org.re2ds.thor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Original door-program transforms, evaluated at build time by source/door.c. */
final class DoorAnimation {
    static final int PARTS=8, STRIDE=1+PARTS*8;
    final RoomData.Model model;
    final int fps,frames;
    final int[][] poses;
    DoorAnimation(RoomData.Model mesh,byte[] bytes){
        model=mesh;ByteBuffer b=RoomData.bytes(bytes);
        RoomData.require(bytes.length>=12&&b.getInt(0)==0x31414452,"Invalid door animation");
        fps=b.getInt(4);frames=b.getInt(8);
        RoomData.require(fps==60&&frames>0&&frames<=1800&&bytes.length==12+frames*STRIDE*4,"Invalid door timing");
        poses=new int[frames][STRIDE];
        for(int f=0;f<frames;f++){
            for(int i=0;i<STRIDE;i++)poses[f][i]=b.getInt(12+(f*STRIDE+i)*4);
            RoomData.require(poses[f][0]>=0&&poses[f][0]<=32767,"Invalid door fade");
            for(int i=0;i<PARTS;i++)RoomData.require(poses[f][1+i*8]>=-1&&poses[f][1+i*8]<=255,"Invalid door part");
        }
    }
    static final class Catalog {
        private final RoomData.Reader reader;
        final Map<Integer,Integer> entries=new HashMap<>();
        private final java.util.LinkedHashMap<Integer,DoorAnimation> cache=new java.util.LinkedHashMap<>(4,.75f,true);
        Catalog(RoomData.Reader reader)throws IOException{
            this.reader=reader;
            ByteBuffer index=RoomData.bytes(reader.read("door/index.bin"));int n=index.getInt(0);
            RoomData.require(n>=0&&n<=2048&&index.capacity()==4+n*5,"Invalid door index");
            for(int i=0;i<n;i++){
                int o=4+i*5,stage=RoomData.u8(index,o),room=RoomData.u8(index,o+1),slot=RoomData.u8(index,o+2),kind=RoomData.u8(index,o+3),variant=RoomData.u8(index,o+4);
                entries.put((stage*256+room)*256+slot,kind*256+variant);
            }
        }
        DoorAnimation get(int stage,int room,int slot)throws IOException{
            Integer key=entries.get((stage*256+room)*256+slot);if(key==null)return null;
            DoorAnimation animation=cache.get(key);
            if(animation==null){
                String prefix=String.format(Locale.US,"door/door%02x",key/256);
                RoomData.Model model=RoomData.model(reader.read(prefix+"_model.bin"),reader.read(prefix+"_tex.bin"));
                animation=new DoorAnimation(model,reader.read(prefix+String.format(Locale.US,"_%02x.bin",key%256)));
                cache.put(key,animation);
                if(cache.size()>2)cache.remove(cache.keySet().iterator().next());
            }
            return animation;
        }
    }
}

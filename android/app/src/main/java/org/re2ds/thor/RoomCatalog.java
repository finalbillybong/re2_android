package org.re2ds.thor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Lightweight index; only the current room's backgrounds and geometry are retained. */
final class RoomCatalog {
    final int[] keys;
    private final RoomData.Reader reader;
    private final RoomData.Shared shared;
    RoomCatalog(RoomData.Reader reader)throws IOException {
        this.reader=reader;ByteBuffer b=RoomData.bytes(reader.read("room/index.bin"));
        int count=RoomData.u16(b,0);RoomData.require(count>0&&count<=2048&&b.capacity()==2+count*2,"Invalid room catalog");
        keys=new int[count];
        for(int i=0;i<count;i++){
            int stage=RoomData.u8(b,2+i*2),room=RoomData.u8(b,3+i*2);
            RoomData.require(stage>=1&&stage<=7,"Invalid stage");keys[i]=stage*256+room;
        }
        Arrays.sort(keys);for(int i=1;i<count;i++)RoomData.require(keys[i]!=keys[i-1],"Duplicate room");
        shared=new RoomData.Shared(reader);
    }
    boolean contains(int stage,int room){return stage>=1&&stage<=7&&room>=0&&room<=255&&Arrays.binarySearch(keys,stage*256+room)>=0;}
    RoomData load(int stage,int room)throws IOException {
        if(!contains(stage,room))throw new IOException("Room is not in the converted disc");
        return new RoomData(reader,stage,room,shared);
    }
}

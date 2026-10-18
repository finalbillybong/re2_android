package org.re2ds.thor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
/** Original RES1 blood and RRE1 CORE00 effect pages; palette index zero is transparent. */
final class EffectsData {
    static final int STRIDE=19;
    private final RoomData.Reader reader;
    private final Map<Integer,RoomData.Pixels> textures=new HashMap<>();
    EffectsData(RoomData.Reader reader)throws IOException {
        this.reader=reader;
        ByteBuffer b=RoomData.bytes(reader.read("effect/core.bin"));
        RoomData.require(b.getInt(0)==0x31534552,"Invalid blood effects");
        int palette=8+RoomData.u16(b,4)*8+RoomData.u16(b,6)*8;
        textures.put(0,page(b,palette,palette+32,256));
        loadPages(RoomData.bytes(reader.read("effect/core_fx.bin")));
    }
    EffectsData forRoom(int stage,int room)throws IOException {
        EffectsData result=new EffectsData(reader);
        if(stage==1&&(room<=3||(room>=0x18&&room<=0x1b)))
            result.loadPages(RoomData.bytes(reader.read(String.format(java.util.Locale.US,"room/room1%02x0_esp.bin",room))));
        return result;
    }
    private void loadPages(ByteBuffer b){
        RoomData.require(b.getInt(0)==0x31455252,"Invalid core effects");
        for(int i=0;i<RoomData.u16(b,4);i++){
            int o=8+i*12,id=RoomData.u8(b,o),rows=RoomData.u8(b,o+1),frames=RoomData.u8(b,o+2),sprites=RoomData.u8(b,o+3),subs=RoomData.u8(b,o+4);
            int log2=RoomData.u8(b,o+5),offset=b.getInt(o+8);
            RoomData.require(rows>0&&rows<=4&&log2<=8,"Invalid core effect page");
            int relative=(rows*32+frames*4+sprites*4+subs*8+3)&~3;
            for(int row=0;row<rows;row++)textures.put(65536+id*256+row,page(b,offset+row*32,offset+relative,1<<log2));
        }
    }
    RoomData.Pixels texture(int bank,int id,int row){
        RoomData.Pixels p=textures.get(bank==0?0:65536+id*256+row);
        RoomData.require(p!=null,"Missing original effect palette");return p;
    }
    private static RoomData.Pixels page(ByteBuffer b,int palette,int texels,int height){
        RoomData.require(palette>=0&&palette+32<=b.limit()&&texels>=0&&texels+128*height<=b.limit(),"Truncated effect page");
        int[] colors=new int[16],pixels=new int[256*height];
        for(int i=1;i<16;i++){
            int rgb=RoomData.u16(b,palette+i*2),r=rgb&31,g=rgb>>5&31,blue=rgb>>10&31;
            colors[i]=0xff000000|((r<<3|r>>2)<<16)|((g<<3|g>>2)<<8)|(blue<<3|blue>>2);
        }
        for(int i=0;i<pixels.length;i++)pixels[i]=colors[(RoomData.u8(b,texels+i/2)>>(4*(i&1)))&15];
        return new RoomData.Pixels(256,height,pixels);
    }
}

package org.re2ds.thor;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Readers for source/formats.h. Pure Java so real asset decoding is host-testable. */
final class RoomData {
    interface Reader { byte[] read(String path) throws IOException; }
    static ByteBuffer bytes(byte[] data) { return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN); }
    static int u16(ByteBuffer b, int o) { return b.getShort(o) & 65535; }
    static int u8(ByteBuffer b, int o) { return b.get(o) & 255; }
    static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }

    static final class Pixels {
        final int width, height;
        final int[] argb;
        Pixels(int w, int h, int[] p) { width = w; height = h; argb = p; }
    }
    static final class Camera {
        final float[] from = new float[3], to = new float[3];
        int screen;
        Pixels background;
        Mask mask;
    }
    static final class Mask { Pixels atlas; float[] triangles; }
    static final class Box { int x, z, w, d, shape, flags, type, floors; }
    static final class Zone { int from, to, floor, role; final int[] x = new int[4], z = new int[4]; }
    static final class Mesh {
        final int bone, page;
        final float[] triangles;
        Mesh(int b, int p, float[] v) { bone = b; page = p; triangles = v; }
    }
    static final class Model {
        int bones, pages;
        int[] parents;
        float[][] positions;
        short[][] frames;
        int[][] animations, animationFlags;
        final List<Mesh> meshes = new ArrayList<>();
        Pixels[] textures;
    }
    final byte[] raw;
    final int roomId,stage;
    final float spawnX,spawnY,spawnZ,spawnDirection;
    final int spawnCamera;
    final Camera[] cameras;
    final Box[] boxes;
    final Zone[] zones;
    final Model[] models = new Model[3]; // unarmed, knife, handgun
    static final class ObjectMesh {Pixels texture;float[] triangles;}
    final ObjectMesh[] objects;
    final Menu menu;

    RoomData(Reader reader) throws IOException {this(reader,0);}
    RoomData(Reader reader,int id) throws IOException {this(reader,1,id,new Shared(reader));}
    RoomData(Reader reader,int stage,int id,Shared shared) throws IOException {
        roomId=id;this.stage=stage;
        String prefix=String.format(java.util.Locale.US,"room%d%02x",stage,id);
        raw=reader.read("room/"+prefix+"0.bin");
        ByteBuffer b = bytes(raw);
        require(b.getInt(0) == 0x314d5252 && u16(b, 4) == 4, "Unsupported room format");
        int nc = u8(b, 6), ns = u8(b, 7), nb = u16(b, 8);
        spawnX=b.getInt(12);spawnY=b.getInt(16);spawnZ=b.getInt(20);spawnDirection=u16(b,24);spawnCamera=u8(b,10);
        require(nc > 0 && nc <= 32, "Invalid camera count");
        cameras = new Camera[nc]; zones = new Zone[ns]; boxes = new Box[nb];
        int o = 60;
        for (int i = 0; i < nc; i++, o += 28) {
            Camera c = cameras[i] = new Camera();
            for (int j = 0; j < 3; j++) { c.from[j] = b.getInt(o + 4*j); c.to[j] = b.getInt(o + 12 + 4*j); }
            c.screen = u16(b, o + 24);
            require(c.screen > 0, "Invalid camera projection");
            String name = String.format(java.util.Locale.US, prefix+"_cam%02d.bin", i);
            c.background = background(reader.read("bg/" + name.replace(".bin",".rgb")));
            try { c.mask = mask(reader.read("mask/" + name)); }
            catch (FileNotFoundException ignored) { /* Not every camera has foreground geometry. */ }
        }
        for (int i = 0; i < ns; i++, o += 20) {
            Zone z = zones[i] = new Zone();
            z.from = u8(b, o); z.to = u8(b, o+1); z.floor = u8(b, o+2); z.role = u8(b, o+3);
            for (int j = 0; j < 4; j++) { z.x[j] = b.getShort(o+4+j*2); z.z[j] = b.getShort(o+12+j*2); }
        }
        for (int i = 0; i < nb; i++, o += 20) {
            Box box = boxes[i] = new Box();
            box.x=b.getShort(o); box.z=b.getShort(o+2); box.w=u16(b,o+4); box.d=u16(b,o+6);
            box.shape=u16(b,o+8); box.flags=u16(b,o+10); box.type=u16(b,o+12); box.floors=b.getInt(o+16);
        }
        System.arraycopy(shared.models,0,models,0,models.length);
        ObjectMesh[] placed;
        try {placed=objects(reader.read("room/"+prefix+"0_obj.bin"));}
        catch(FileNotFoundException ignored){placed=new ObjectMesh[0];}
        objects=placed;
        menu = shared.menu;
    }

    /** Shared immutable player and menu assets; rooms can be released independently. */
    static final class Shared {
        final Model[] models=new Model[3];
        final Menu menu;
        Shared(Reader reader)throws IOException {
        byte[] baseTexture = reader.read("model/leon_tex.bin");
        models[0] = model(reader.read("model/leon.bin"), baseTexture);
        for (int weapon = 1; weapon <= 2; weapon++) {
            String name = "model/leon_w0" + weapon;
            byte[] patched = patchTexture(baseTexture, reader.read(name + "_tex.bin"));
            models[weapon] = model(reader.read(name + ".bin"), patched);
        }
            menu=new Menu(reader.read("menu/items.bin"),reader.read("menu/pictures.bin"));
        }
    }

    static byte[] decompress(byte[] data, int offset) {
        ByteBuffer b = bytes(data);
        int header = b.getInt(offset), size = header >>> 8;
        require((header & 255) == 0x10 && size > 0 && size <= 4*1024*1024, "Invalid LZ77 header");
        byte[] result = new byte[size]; int src = offset + 4, out = 0;
        while (out < size) {
            int flags = data[src++] & 255;
            for (int bit = 7; bit >= 0 && out < size; bit--) {
                if ((flags & (1 << bit)) == 0) result[out++] = data[src++];
                else {
                    int a = data[src++] & 255, c = data[src++] & 255;
                    int length = (a >>> 4) + 3, distance = ((a & 15) << 8 | c) + 1;
                    require(distance <= out && out + length <= size, "Invalid LZ77 reference");
                    for (int i = 0; i < length; i++) { result[out] = result[out-distance]; out++; }
                }
            }
        }
        return result;
    }

    static Pixels indexed(ByteBuffer data, int palette, byte[] indices, int width, int height, boolean transparent) {
        require(indices.length == width * height, "Invalid indexed image dimensions");
        int[] colors = new int[256];
        for (int i = 0; i < 256; i++) {
            int c = u16(data, palette + i*2);
            colors[i] = 0xff000000 | (c & 31)*255/31 << 16 | ((c >>> 5) & 31)*255/31 << 8 | ((c >>> 10) & 31)*255/31;
        }
        if (transparent) colors[0] = 0;
        int[] pixels = new int[indices.length];
        for (int i = 0; i < pixels.length; i++) pixels[i] = colors[indices[i] & 255];
        return new Pixels(width, height, pixels);
    }

    static Pixels background(byte[] data) {
        ByteBuffer b = bytes(data);
        if(b.getInt(0)==0x31414252){
            int w=u16(b,4),h=u16(b,6);require(w==320&&h==240&&data.length==8+w*h*3,"Invalid original background");
            int[] pixels=new int[w*h];for(int i=0;i<pixels.length;i++)pixels[i]=0xff000000|(data[8+i*3]&255)<<16|(data[9+i*3]&255)<<8|(data[10+i*3]&255);
            return new Pixels(w,h,pixels);
        }
        require(b.getInt(0) == 0x31474252, "Invalid background");
        return indexed(b, 4, decompress(data, 516), 256, 192, false);
    }

    static Mask mask(byte[] packed) {
        byte[] data = decompress(packed, 0); ByteBuffer b = bytes(data);
        require(b.getInt(0) == 0x314b4d52, "Invalid foreground mask");
        int n = u16(b, 4), height = u16(b, 6), pixelsEnd = 524 + 256 * height;
        Mask mask = new Mask();
        mask.atlas = indexed(b, 12, java.util.Arrays.copyOfRange(data, 524, pixelsEnd), 256, height, true);
        mask.triangles = new float[n*6*5];
        int[] order = {0,1,2,0,2,3}; int dest = 0;
        for (int q = 0; q < n; q++) {
            int o = pixelsEnd + q*56, u = u8(b,o), v=u8(b,o+1), w=u16(b,o+2), h=u16(b,o+4);
            for (int corner : order) {
                for (int axis=0; axis<3; axis++) mask.triangles[dest++] = b.getInt(o+8+corner*12+axis*4);
                mask.triangles[dest++] = (u + ((corner==1 || corner==2) ? w : 0)) / 256f;
                mask.triangles[dest++] = (v + (corner>=2 ? h : 0)) / (float)height;
            }
        }
        return mask;
    }

    static byte[] patchTexture(byte[] original, byte[] patch) {
        byte[] result = original.clone(); ByteBuffer p=bytes(patch), b=bytes(result);
        int page=u16(p,0), x=u16(p,2), y=u16(p,4), w=u16(p,6), h=u16(p,8), first=u16(p,10), count=u16(p,12);
        require(page < result.length/33280 && x+w<=128 && y+h<=256 && first+count<=256, "Invalid weapon texture patch");
        for (int i=0;i<count;i++) b.putShort(page*33280+(first+i)*2,p.getShort(16+i*2));
        for (int row=0;row<h;row++) System.arraycopy(patch,16+count*2+row*w,result,page*33280+512+(y+row)*128+x,w);
        return result;
    }

    static Model model(byte[] data, byte[] texture) {
        ByteBuffer b=bytes(data); require(b.getInt(0)==0x31444d52,"Invalid model");
        Model m=new Model(); m.bones=u16(b,4); m.pages=u16(b,6);
        int frames=u16(b,8), anims=u16(b,10), ob=b.getInt(12), ol=b.getInt(16), of=b.getInt(20), oa=b.getInt(24);
        require(m.bones>0 && m.bones<=64 && m.pages>0 && m.pages<=8,"Invalid skeleton");
        m.parents=new int[m.bones]; m.positions=new float[m.bones][3];
        for(int bone=0;bone<m.bones;bone++) {
            m.parents[bone]=b.get(ob+bone*8);
            require(m.parents[bone]<bone,"Skeleton must have parents before children");
            for(int axis=0;axis<3;axis++) m.positions[bone][axis]=b.getShort(ob+bone*8+2+axis*2);
            for(int page=0;page<m.pages;page++) {
                int offset=b.getInt(ol+(bone*m.pages+page)*4);
                if(offset!=0) m.meshes.add(new Mesh(bone,page,decodeGx(b,offset)));
            }
        }
        m.frames=new short[frames][6+3*m.bones];
        for(int f=0;f<frames;f++) for(int j=0;j<m.frames[f].length;j++) m.frames[f][j]=b.getShort(of+f*m.frames[f].length*2+j*2);
        m.animations=new int[anims][];m.animationFlags=new int[anims][];
        for(int a=0;a<anims;a++) {
            int first=u16(b,oa+a*4), count=u16(b,oa+a*4+2); m.animations[a]=new int[count];m.animationFlags[a]=new int[count];
            for(int k=0;k<count;k++) {
                int entry=u16(b,oa+anims*4+(first+k)*2);
                int index=entry&4095;m.animationFlags[a][k]=entry&0xf000;
                require(index<frames,"Animation references missing frame");m.animations[a][k]=index;
            }
        }
        m.textures=new Pixels[m.pages];
        for(int page=0;page<m.pages;page++) m.textures[page]=indexed(bytes(texture),page*33280,
                java.util.Arrays.copyOfRange(texture,page*33280+512,(page+1)*33280),128,256,false);
        return m;
    }

    static ObjectMesh[] objects(byte[] data){
        ByteBuffer b=bytes(data);require(b.getInt(0)==0x31424f52,"Invalid room objects");
        int count=u16(b,4),nt=u16(b,6);require(count<=32&&nt<=32,"Too many room objects");
        Pixels[] textures=new Pixels[nt];
        for(int i=0;i<nt;i++){
            int w=u16(b,8+i*8),h=u16(b,10+i*8),offset=b.getInt(12+i*8);
            require(w>0&&w<=256&&h>0&&h<=256&&offset>=0&&offset+512+w*h<=data.length,"Invalid object texture");
            textures[i]=indexed(b,offset,java.util.Arrays.copyOfRange(data,offset+512,offset+512+w*h),w,h,u16(b,offset)==0);
        }
        ObjectMesh[] result=new ObjectMesh[count];
        for(int i=0;i<count;i++){
            int o=8+nt*8+i*8,texture=u16(b,o),list=b.getInt(o+4);if(list==0)continue;
            require(texture<nt,"Invalid object texture index");ObjectMesh m=result[i]=new ObjectMesh();m.texture=textures[texture];m.triangles=decodeGx(b,list,m.texture.width,m.texture.height);
        }
        return result;
    }
    static float[] decodeGx(ByteBuffer b,int offset) {return decodeGx(b,offset,128,256);}
    static float[] decodeGx(ByteBuffer b,int offset,int width,int height) {
        int end=offset+4+b.getInt(offset)*4, p=offset+4, mode=0, vertex=0;
        float u=0,v=0;float[][] poly=new float[4][5];List<Float> out=new ArrayList<>();
        while(p<end) {
            int commands=b.getInt(p);p+=4;
            for(int k=0;k<4;k++) {
                int cmd=(commands >>> (8*k))&255;
                switch(cmd) {
                    case 0: case 0x41: break;
                    case 0x20: p+=4;break;
                    case 0x40: mode=b.getInt(p);p+=4;vertex=0;require(mode==0||mode==1,"Unsupported GX primitive");break;
                    case 0x22: u=b.getShort(p)/16f/width;v=b.getShort(p+2)/16f/height;p+=4;break;
                    case 0x23:
                        poly[vertex]=new float[]{b.getShort(p),b.getShort(p+2),b.getShort(p+4),u,v};p+=8;vertex++;
                        if(vertex==(mode==0?3:4)) {
                            for(int index:new int[]{0,1,2}) for(float f:poly[index]) out.add(f);
                            if(mode==1) for(int index:new int[]{0,2,3}) for(float f:poly[index]) out.add(f);
                            vertex=0;
                        }
                        break;
                    default: throw new IllegalArgumentException("Unsupported GX command "+cmd);
                }
                require(p<=end,"Truncated GX list");
            }
        }
        float[] result=new float[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);return result;
    }

    static final class Menu {
        final Pixels[] icons, pictures;
        final Pixels portrait;
        final String[] names;
        Menu(byte[] items,byte[] images) {
            ByteBuffer b=bytes(items),p=bytes(images);
            require(b.getInt(0)==0x31544952 && p.getInt(0)==0x31504952,"Invalid menu assets");
            int count=u16(b,4), namesCount=u16(b,6), portrait=532+count*1200;
            int namesOffset=portrait+4+u16(b,portrait)*u16(b,portrait+2), strings=namesOffset+namesCount*2;
            this.portrait=indexed(b,20,java.util.Arrays.copyOfRange(items,portrait+4,namesOffset),u16(b,portrait),u16(b,portrait+2),false);
            icons=new Pixels[count];names=new String[namesCount+1];names[0]="Empty";
            for(int i=0;i<count;i++) icons[i]=indexed(b,20,java.util.Arrays.copyOfRange(items,532+i*1200,532+(i+1)*1200),40,30,false);
            for(int i=0;i<namesCount;i++) {
                int start=strings+u16(b,namesOffset+i*2),end=start;
                while(end<items.length && items[end]!=0)end++;
                names[i+1]=new String(items,start,end-start,StandardCharsets.US_ASCII);
            }
            int n=u16(p,4),w=u16(p,6),h=u16(p,8);pictures=new Pixels[n];
            for(int i=0;i<n;i++)pictures[i]=indexed(b,20,java.util.Arrays.copyOfRange(images,12+i*w*h,12+(i+1)*w*h),w,h,false);
        }
        String name(int item) { return item>=0 && item<names.length?names[item]:"Unknown item"; }
    }
}

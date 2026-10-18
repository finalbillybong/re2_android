package org.re2ds.thor;

/** First-room movement using the upstream root motion, collision and camera zones. */
final class RoomSession {
    final RoomData data;
    float x=19800, y, z=300, direction=16384, accumulator;
    short[] combatFrame;
    int slope=-1;
    int footstep; // 0 none, 1 walk/turn/back, 2 run; consumed each simulation tick
    float slopeBase;
    boolean autoCamera=true;
    final int[] cameraMap=new int[32];
    int camera, animation=12, key, weapon=2;
    RoomSession(RoomData d) { data=d;for(int i=0;i<32;i++)cameraMap[i]=i; }
    void reset() { x=19800;z=300;direction=16384;y=0;slope=-1;camera=0;animation=12;key=0;accumulator=0; }
    void step(float seconds,float turn,float forward,boolean run,boolean paused,int equipped) {
        footstep=0;
        int w=equipped==1?1:equipped==2?2:0;
        if(w!=weapon) { weapon=w;key=0; }
        if(paused) { accumulator=0;return; }
        accumulator+=Math.max(0,Math.min(seconds,.05f));
        while(accumulator>=1f/30) {
            accumulator-=1f/30;
            boolean moving=Math.abs(forward)>=.15f;
            int a=!moving?(Math.abs(turn)>.15f?10:12):run&&forward<0?11:10;
            float turnRate=!moving?640:a==11?448:320;
            direction=(direction+turn*turnRate+32768)%32768;
            if(a!=animation) { animation=a;key=0; }
            RoomData.Model m=data.models[weapon];int[] sequence=m.animations[animation];
            int prev=key;boolean back=forward>0;
            key=Math.floorMod(key+(back?-1:1),sequence.length);
            if(a!=12&&m.animationFlags!=null&&(m.animationFlags[a][key]&0x4000)!=0)footstep=a==11?2:1;
            if(moving) {
                int k0=back?key:prev,k1=back?prev:key;
                int s0=k1==0?0:m.frames[sequence[k0]][3],s1=m.frames[sequence[k1]][3];
                float distance=s1-s0;if(distance<0)distance=s1;if(back)distance=-distance;
                double angle=direction*Math.PI/16384;
                float nx=x+(float)Math.cos(angle)*distance,nz=z-(float)Math.sin(angle)*distance;
                if(!blocked(x,z,nx,nz)) { x=nx;z=nz; }
                else if(!blocked(x,z,nx,z))x=nx;
                else if(!blocked(x,z,x,nz))z=nz;
                updateHeight();switchCamera();
            }
        }
    }
    static float penetration(RoomData.Box b,float x,float z) {
        if(b.shape>=6&&b.shape<=8) {
            float ra=b.w*.5f,rb=(b.shape==6?b.w:b.d)*.5f;
            float dx=x-b.x-ra,dz=z-b.z-rb;ra+=400;rb+=400;
            float q=dx*dx/(ra*ra)+dz*dz/(rb*rb);
            return q>=1?0:Math.max(1,(1-(float)Math.sqrt(q))*Math.min(ra,rb));
        }
        return Math.max(0,Math.min(Math.min(x-b.x+400,b.x+b.w+400-x),Math.min(z-b.z+400,b.z+b.d+400-z)));
    }
    boolean blocked(float ox,float oz,float nx,float nz) {
        for(RoomData.Box b:data.boxes) if((b.flags&0x8000)!=0&&(b.floors&(1<<floor()))!=0&&b.shape!=11&&b.shape!=12) {
            float p=penetration(b,nx,nz);if(p>0&&p>=penetration(b,ox,oz))return true;
        }
        return false;
    }
    int floor(){return Math.max(0,Math.min(31,(int)((-y+900)/1800)));}
    void updateHeight(){
        for(int i=0;i<data.boxes.length;i++){
            RoomData.Box b=data.boxes[i];
            if((b.shape!=11&&b.shape!=12)||b.w==0||b.d==0||x<b.x||x>b.x+b.w||z<b.z||z>b.z+b.d)continue;
            float fraction;
            switch((b.type>>4)&3){
                case 0:fraction=(x-b.x)/b.w;break;
                case 1:fraction=(b.x+b.w-x)/b.w;break;
                case 2:fraction=(z-b.z)/b.d;break;
                default:fraction=(b.z+b.d-z)/b.d;break;
            }
            float rise=((b.type>>6)&31)*-1800;
            if(slope!=i){slope=i;slopeBase=y-rise*fraction;}
            y=slopeBase+rise*fraction;return;
        }
        slope=-1;
    }
    static boolean inside(RoomData.Zone q,float x,float z) {
        boolean positive=false,negative=false;
        for(int i=0;i<4;i++) {int j=(i+1)%4;float c=(q.x[j]-q.x[i])*(z-q.z[i])-(q.z[j]-q.z[i])*(x-q.x[i]);positive|=c>0;negative|=c<0;}
        return !(positive&&negative);
    }
    int mapped(int c){return c>=0&&c<32?cameraMap[c]:c;}
    void switchCamera() {
        if(!autoCamera)return;
        for(RoomData.Zone q:data.zones) if((q.role&3)==0&&mapped(q.from)==camera&&(q.floor==255||q.floor==floor())&&inside(q,x,z)) {
            if(mapped(q.to)<data.cameras.length)camera=mapped(q.to);return;
        }
        boolean inView=false;int other=-1;
        for(RoomData.Zone q:data.zones) if((q.role&3)==1&&(q.floor==255||q.floor==floor())&&inside(q,x,z)) {
            if(mapped(q.from)==camera)inView=true;
            else if(other<0)for(RoomData.Zone t:data.zones)if((t.role&3)==0&&t.from!=t.to&&(t.from==q.from||t.to==q.from)){other=mapped(q.from);break;}
        }
        if(!inView&&other>=0&&other<data.cameras.length)camera=other;
    }
    Pose pose() {return new Pose(x,y,z,direction,camera,weapon,animation,key,combatFrame);}
    static final class Pose {
        final short[] frame;
        final float x,y,z,direction;final int camera,weapon,animation,key;
        Pose(float x,float y,float z,float d,int c,int w,int a,int k,short[] frame){this.frame=frame;this.x=x;this.y=y;this.z=z;direction=d;camera=c;weapon=w;animation=a;key=k;}
    }
}

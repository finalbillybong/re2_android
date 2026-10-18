package org.re2ds.thor;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class RoomRenderer implements GLSurfaceView.Renderer {
    final RoomData data;
    EffectsData effectData;
    volatile int[] effects=new int[0];
    private final float[] effectVertices=new float[30];
    private final FloatBuffer effectBuffer=ByteBuffer.allocateDirect(120).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final int[] effectOrder=new int[512];
    volatile RoomSession.Pose pose;
    volatile DoorAnimation door;
    volatile int doorFrame;
    volatile int[] objects=new int[0];
    volatile RoomActors.Actor[] actors=RoomActors.EMPTY;
    private final IdentityHashMap<RoomData.Pixels,Integer> textures=new IdentityHashMap<>();
    private final IdentityHashMap<float[],FloatBuffer> buffers=new IdentityHashMap<>();
    private int program,position,uv,mvp,brightness,tint,viewportW,viewportH,viewportX,viewportY;
    private final float[] identity=new float[16],view=new float[16],projection=new float[16],pv=new float[16],world=new float[16],temp=new float[16];
    private final float[][] bones=new float[64][16];
    private final float[] background={-1,1,0,0,0, -1,-1,0,0,1, 1,-1,0,1,1, -1,1,0,0,0, 1,-1,0,1,1, 1,1,0,1,0};
    RoomRenderer(RoomData d,RoomSession.Pose p){data=d;pose=p;Matrix.setIdentityM(identity,0);}
    static Bitmap bitmap(RoomData.Pixels p){return Bitmap.createBitmap(p.argb,p.width,p.height,Bitmap.Config.ARGB_8888);}
    private int shader(int type,String source){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        textures.clear();buffers.clear();
        int v=shader(GLES20.GL_VERTEX_SHADER,"attribute vec3 position; attribute vec2 uv; uniform mat4 mvp; varying vec2 tex; void main(){tex=uv;gl_Position=mvp*vec4(position,1.0);}");
        int f=shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float; varying vec2 tex; uniform sampler2D image; uniform float brightness; uniform vec4 tint; void main(){vec4 c=texture2D(image,tex);if(c.a<0.5)discard;gl_FragColor=vec4(c.rgb*brightness,c.a)*tint;}");
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);
        int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);
        tint=GLES20.glGetUniformLocation(program,"tint");
        brightness=GLES20.glGetUniformLocation(program,"brightness");
        position=GLES20.glGetAttribLocation(program,"position");uv=GLES20.glGetAttribLocation(program,"uv");mvp=GLES20.glGetUniformLocation(program,"mvp");
        GLES20.glUseProgram(program);GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"image"),0);
        GLES20.glDisable(GLES20.GL_CULL_FACE);GLES20.glClearColor(0,0,0,1);
        for(RoomData.Camera c:data.cameras){texture(c.background);if(c.mask!=null)texture(c.mask.atlas);}
        for(RoomData.Model m:data.models)for(RoomData.Pixels p:m.textures)texture(p);
    }
    private int texture(RoomData.Pixels p){
        Integer found=textures.get(p);if(found!=null)return found;
        int[] id=new int[1];GLES20.glGenTextures(1,id,0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,id[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        Bitmap b=bitmap(p);GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,b,0);b.recycle();textures.put(p,id[0]);return id[0];
    }
    @Override public void onSurfaceChanged(GL10 gl,int width,int height){viewportW=Math.min(width,height*4/3);viewportH=viewportW*3/4;viewportX=(width-viewportW)/2;viewportY=(height-viewportH)/2;}
    private void draw(float[] vertices,RoomData.Pixels image,float[] matrix){
        FloatBuffer b=buffers.get(vertices);if(b==null){b=ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(vertices).position(0);buffers.put(vertices,b);}
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture(image));GLES20.glUniformMatrix4fv(mvp,1,false,matrix,0);
        b.position(0);GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,20,b);GLES20.glEnableVertexAttribArray(position);
        b.position(3);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,20,b);GLES20.glEnableVertexAttribArray(uv);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertices.length/5);
    }

    private void doorTransform(int[] pose,int part,float[] matrix,int depth){
        int o=1+part*8,flags=pose[o+1],parent=flags&15;
        if((flags&16)!=0&&parent<DoorAnimation.PARTS&&depth<4)doorTransform(pose,parent,matrix,depth+1);
        Matrix.translateM(matrix,0,pose[o+2],pose[o+3],pose[o+4]);
        Matrix.rotateM(matrix,0,pose[o+5]*360f/4096,1,0,0);
        Matrix.rotateM(matrix,0,pose[o+6]*360f/4096,0,1,0);
        Matrix.rotateM(matrix,0,pose[o+7]*360f/4096,0,0,1);
    }
    private void drawDoor(DoorAnimation animation,int frame){
        int[] pose=animation.poses[frame];
        GLES20.glUniform1f(brightness,1-pose[0]/32767f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        Matrix.setLookAtM(view,0,10000,0,0,0,0,0,0,-1,0);
        float near=204.8f,w=near*160/290,h=near*120/290;
        Matrix.frustumM(projection,0,-w,w,-h,h,near,65536);Matrix.multiplyMM(pv,0,projection,0,view,0);
        for(int i=0;i<DoorAnimation.PARTS;i++){
            int part=pose[1+i*8];if(part<0)continue;
            Matrix.setIdentityM(world,0);doorTransform(pose,i,world,0);
            Matrix.multiplyMM(temp,0,pv,0,world,0);
            for(RoomData.Mesh mesh:animation.model.meshes)if(mesh.bone==part)draw(mesh.triangles,animation.model.textures[mesh.page],temp);
        }
    }
    private void drawModel(RoomData.Model m,short[] frame){
        for(int i=0;i<m.bones;i++){
            float[] parent=m.parents[i]<0?world:bones[m.parents[i]];System.arraycopy(parent,0,bones[i],0,16);
            if(m.parents[i]<0)Matrix.translateM(bones[i],0,frame[0],frame[1],frame[2]);else Matrix.translateM(bones[i],0,m.positions[i][0],m.positions[i][1],m.positions[i][2]);
            Matrix.rotateM(bones[i],0,frame[6+i*3]*360f/32768,1,0,0);Matrix.rotateM(bones[i],0,frame[7+i*3]*360f/32768,0,1,0);Matrix.rotateM(bones[i],0,frame[8+i*3]*360f/32768,0,0,1);
        }
        for(RoomData.Mesh mesh:m.meshes){Matrix.multiplyMM(temp,0,pv,0,bones[mesh.bone],0);draw(mesh.triangles,m.textures[mesh.page],temp);}
    }
    private void drawEffects(RoomData.Camera camera){
        int[] fx=effects;if(effectData==null||fx.length==0)return;
        float fxDir=camera.to[0]-camera.from[0],fy=camera.to[1]-camera.from[1],fz=camera.to[2]-camera.from[2];
        float length=(float)Math.sqrt(fxDir*fxDir+fy*fy+fz*fz);if(length<1)return;
        fxDir/=length;fy/=length;fz/=length;
        float rightLength=(float)Math.sqrt(fxDir*fxDir+fz*fz);if(rightLength<.0001f)return;
        float rx=fz/rightLength,rz=-fxDir/rightLength;
        float dx=-rz*fy,dy=rz*fxDir-rx*fz,dz=rx*fy;
        if(dy<0){dx=-dx;dy=-dy;dz=-dz;}
        int count=Math.min(effectOrder.length,fx.length/EffectsData.STRIDE);
        for(int i=0;i<count;i++){
            int o=i*EffectsData.STRIDE,j=i;
            float depth=fx[o+3]*fxDir+fx[o+4]*fy+fx[o+5]*fz;
            while(j>0){int prior=effectOrder[j-1];float d=fx[prior+3]*fxDir+fx[prior+4]*fy+fx[prior+5]*fz;if(d>=depth)break;effectOrder[j]=prior;j--;}
            effectOrder[j]=o;
        }
        GLES20.glEnable(GLES20.GL_BLEND);GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);GLES20.glDepthMask(false);
        GLES20.glUniformMatrix4fv(mvp,1,false,pv,0);
        for(int i=0;i<count;i++){
            int o=effectOrder[i];RoomData.Pixels page=effectData.texture(fx[o],fx[o+1],fx[o+2]);
            float x=fx[o+3]-fxDir*fx[o+18],y=fx[o+4]-fy*fx[o+18],z=fx[o+5]-fz*fx[o+18];
            for(int v=0;v<6;v++){
                int corner=v<3?v:v==3?0:v==4?2:3;
                boolean right=corner==1||corner==2,bottom=corner>=2;
                float sx=fx[o+(right?8:6)],sy=fx[o+(bottom?9:7)];int at=v*5;
                effectVertices[at]=x+rx*sx+dx*sy;effectVertices[at+1]=y+dy*sy;effectVertices[at+2]=z+rz*sx+dz*sy;
                effectVertices[at+3]=fx[o+(right?12:10)]/256f;effectVertices[at+4]=fx[o+(bottom?13:11)]/(float)page.height;
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture(page));
            GLES20.glUniform4f(tint,fx[o+14]/255f,fx[o+15]/255f,fx[o+16]/255f,fx[o+17]/255f);
            effectBuffer.clear();effectBuffer.put(effectVertices).position(0);
            GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,20,effectBuffer);
            effectBuffer.position(3);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,20,effectBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,6);
        }
        GLES20.glDepthMask(true);GLES20.glDisable(GLES20.GL_BLEND);GLES20.glUniform4f(tint,1,1,1,1);
    }
    @Override public void onDrawFrame(GL10 gl){
        RoomSession.Pose p=pose;RoomData.Camera c=data.cameras[p.camera];
        GLES20.glViewport(viewportX,viewportY,viewportW,viewportH);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);
        GLES20.glUniform1f(brightness,1);GLES20.glUniform4f(tint,1,1,1,1);
        DoorAnimation active=door;
        if(active!=null){drawDoor(active,Math.min(doorFrame,active.frames-1));return;}
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);draw(background,c.background,identity);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        Matrix.setLookAtM(view,0,c.from[0],c.from[1],c.from[2],c.to[0],c.to[1],c.to[2],0,-1,0);
        float near=204.8f,w=near*160/c.screen,h=near*120/c.screen;
        Matrix.frustumM(projection,0,-w,w,-h,h,near,65536);Matrix.multiplyMM(pv,0,projection,0,view,0);
        Matrix.setIdentityM(world,0);Matrix.translateM(world,0,p.x,p.y,p.z);Matrix.rotateM(world,0,p.direction*360/32768,0,1,0);
        RoomData.Model m=data.models[p.weapon];int[] seq=m.animations[p.animation];short[] frame=p.frame!=null?p.frame:m.frames[seq[p.key%seq.length]];
        drawModel(m,frame);
        for(RoomActors.Actor actor:actors){
            Matrix.setIdentityM(world,0);Matrix.translateM(world,0,actor.x,actor.y,actor.z);
            Matrix.rotateM(world,0,actor.direction*360f/32768,0,1,0);
            if(actor.flinch!=0)Matrix.rotateM(world,0,-actor.flinch*220*360f/32768,0,0,1);
            drawModel(actor.model,actor.frame);
        }
        int[] placed=objects;
        for(int i=0;i<placed.length;i+=9){
            int index=placed[i];if(index<0||index>=data.objects.length)continue;
            RoomData.ObjectMesh mesh=data.objects[index];if(mesh==null)continue;
            boolean visible=true;
            if(placed[i+1]!=1&&(placed[i+2]&0x10)==0){
                for(RoomData.Zone zone:data.zones)if(zone.from==p.camera&&(zone.role&1)!=0){visible=RoomSession.inside(zone,placed[i+3],placed[i+5]);break;}
            }
            if(!visible)continue;
            Matrix.setIdentityM(world,0);Matrix.translateM(world,0,placed[i+3],placed[i+4],placed[i+5]);
            Matrix.rotateM(world,0,placed[i+6]*360f/4096,1,0,0);Matrix.rotateM(world,0,placed[i+7]*360f/4096,0,1,0);Matrix.rotateM(world,0,placed[i+8]*360f/4096,0,0,1);
            Matrix.multiplyMM(temp,0,pv,0,world,0);draw(mesh.triangles,mesh.texture,temp);
        }
        if(c.mask!=null)draw(c.mask.triangles,c.mask.atlas,pv);
        drawEffects(c);
    }
}

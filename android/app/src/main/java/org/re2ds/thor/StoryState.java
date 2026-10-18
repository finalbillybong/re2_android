package org.re2ds.thor;

/** Script-controlled player pose and camera policy, sampled after a native tick. */
final class StoryState {
    final boolean locked;
    private final int[] data;
    StoryState(int[] data){RoomData.require(data.length==143,"Invalid story snapshot");this.data=data;locked=data[0]!=0;}
    void apply(RoomSession s){
        s.autoCamera=data[3]!=0;
        for(int i=0;i<32;i++)s.cameraMap[i]=data[111+i];
        if(data[1]!=0){s.x=data[4];s.y=data[5];s.z=data[6];s.direction=data[7];s.slope=-1;}
        if(data[8]!=0){s.combatFrame=new short[102];for(int i=0;i<102;i++)s.combatFrame[i]=(short)data[9+i];}
        if(data[2]>=0&&data[2]<s.data.cameras.length)s.camera=data[2];
        else if(s.autoCamera)s.switchCamera();
    }
}

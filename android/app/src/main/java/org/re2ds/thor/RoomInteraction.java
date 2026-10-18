package org.re2ds.thor;

/** Reach checks mirror the engine's manual AOT checks, in world coordinates. */
final class RoomInteraction {
 static int nearest(int[] triggers,float x,float z,float direction,int floor) {
  double angle=direction*Math.PI/16384;
  float px=x+(float)Math.cos(angle)*600,pz=z-(float)Math.sin(angle)*600;
  // Items take priority over overlapping descriptive text.
  for(int type:new int[]{2,1,4})for(int i=0;i<triggers.length;i+=14){
   if(triggers[i+1]!=type||(triggers[i+2]&1)==0||(triggers[i+3]!=255&&triggers[i+3]!=floor))continue;
   if(inside(triggers,i,x,z)||inside(triggers,i,px,pz))return i;
  }
  return -1;
 }
 static boolean inside(int[] t,int o,float x,float z){
  boolean positive=false,negative=false;
  for(int i=0;i<4;i++){int j=(i+1)%4;float cross=(t[o+4+j]-t[o+4+i])*(z-t[o+8+i])-(t[o+8+j]-t[o+8+i])*(x-t[o+4+i]);positive|=cross>0;negative|=cross<0;}
  return !(positive&&negative);
 }
}

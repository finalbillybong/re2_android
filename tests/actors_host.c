/* Real opening-room scripts + production Android actor bridge, without JNI/GL.
 * tools/test_android_world.sh builds this with ASan/UBSan. */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "enemy.h"
#include "script.h"
#include "world.h"
static void enter(int id){
 char path[80];snprintf(path,sizeof path,"nitrofiles/room/room1%02x0.bin",id);
 FILE *f=fopen(path,"rb");assert(f);fseek(f,0,SEEK_END);size_t size=ftell(f);rewind(f);
 uint8_t *bytes=malloc(size);assert(fread(bytes,1,size,f)==size);fclose(f);
 assert(world_enter(bytes,size,1,id));free(bytes);
}
int main(void){
 int before[MAX_ENEMIES*ENEMY_POSE_SIZE],after[MAX_ENEMIES*ENEMY_POSE_SIZE];
 world_reset();inventory_reset();inventory_add(2,18);enter(0);
 world_player(19800,0,300,16384,0);
 for(int i=0;i<5;i++)world_tick();
 int n=enemy_poses(before);printf("Street: %d visible actors, error %d\n",n,enemy_last_error);
 assert(n>0&&enemy_last_error==0);
 for(int i=0;i<120;i++)world_tick();
 int m=enemy_poses(after);assert(m==n);
 assert(memcmp(before,after,n*ENEMY_POSE_SIZE*sizeof(int))!=0);
 // The combat bridge now allows zombies to grab Leon.
 // Scripted visibility must reach the renderer, and not stop simulation of other slots.
 int slot=after[0],flags;assert(enemy_member_get(slot,7,&flags));
 enemy_member_set(slot,7,flags|0x8000);assert(enemy_poses(before)==n-1);
 enemy_member_set(slot,7,flags);assert(enemy_poses(before)==n);
 enter(1);for(int i=0;i<5;i++)world_tick();
 n=enemy_poses(before);printf("Gun shop: %d visible actors, error %d\n",n,enemy_last_error);
 assert(n>0&&enemy_last_error==0);
 int kendo=0;for(int i=0;i<n;i++)if(before[i*ENEMY_POSE_SIZE+1]==0x48)kendo++;
 assert(kendo==1);
 // Gun-shop spawn model selectors are 1,2,0,0: preserve explicit appearances
 // and resolve unspecified ones once at spawn, never on a render snapshot.
 for(int i=0;i<n;i++)if(before[i*ENEMY_POSE_SIZE+1]==0x1f){
  int slot=before[i*ENEMY_POSE_SIZE],appearance=before[i*ENEMY_POSE_SIZE+107];
  assert(appearance>=1&&appearance<=3);
  if(slot==1||slot==2)assert(appearance==slot);
 }

 // Before the conversation Kendo holds his original neutral standing pose.
 // The old default looped main-set animation 2 (a non-cyclic weapon motion).
 Model own={0};assert(model_load(&own,"nitro:/model/em48.bin"));
 int idle_frame=ANIM_FRAME(own.anim_pool[own.anims[3].first]);
 const ModelFrame *idle=model_frame(&own,idle_frame);
 const int16_t *idle_rot=model_frame_rot(&own,idle_frame);
 for(int tick=0;tick<90;tick++){
  n=enemy_poses(before);int *pose=NULL;
  for(int i=0;i<n;i++)if(before[i*ENEMY_POSE_SIZE+1]==0x48)pose=before+i*ENEMY_POSE_SIZE;
  assert(pose);
  for(int i=0;i<3;i++)assert(pose[8+i]==idle->offset[i]);
  for(int i=0;i<pose[7]*3;i++)assert(pose[11+i]==(idle_rot[i]&32767));
  world_tick();
 }
 asset_free(own.data);

 // Original NPC PLC destination: actor walks and signals arrival.
 const uint8_t walk[]={0x40,0,4,1,0x80,0x3e,0x70,0x17};
 int kendo_slot=-1;for(int i=0;i<n;i++)if(before[i*ENEMY_POSE_SIZE+1]==0x48)kendo_slot=before[i*ENEMY_POSE_SIZE];
 int oldx,oldz;assert(enemy_member_get(kendo_slot,11,&oldx));assert(enemy_member_get(kendo_slot,13,&oldz));
 enemy_plc(kendo_slot,walk);
 for(int i=0;i<60;i++)world_tick();
 int newx,newz;assert(enemy_member_get(kendo_slot,11,&newx));assert(enemy_member_get(kendo_slot,13,&newz));
 assert(newx!=oldx||newz!=oldz);
 m=enemy_poses(after);assert(m>0);
 assert(memcmp(before,after,(m<n?m:n)*ENEMY_POSE_SIZE*sizeof(int))!=0);
 // A cinematic command must use the RBJ pose, not silently retain an idle pose.
 Model rbj={0};assert(model_load(&rbj,"nitro:/room/room1010_rbj_e0.bin"));
 const uint8_t motion[]={0x3f,0,9,0x40};enemy_plc(kendo_slot,motion);world_tick();
 n=enemy_poses(after);int *kp=NULL;for(int i=0;i<n;i++)if(after[i*ENEMY_POSE_SIZE]==kendo_slot)kp=after+i*ENEMY_POSE_SIZE;
 assert(kp);int frame=ANIM_FRAME(rbj.anim_pool[rbj.anims[9].first]);
 const ModelFrame *expected=model_frame(&rbj,frame);const int16_t *rot=model_frame_rot(&rbj,frame);
 for(int i=0;i<3;i++)assert(kp[8+i]==expected->offset[i]);
 for(int i=0;i<kp[7]*3;i++)assert(kp[11+i]==(rot[i]&32767));
 asset_free(rbj.data);
 enter(2);for(int i=0;i<5;i++)world_tick();
 n=enemy_poses(before);printf("Alley: %d visible actors, error %d\n",n,enemy_last_error);assert(n>0);
 enter(4);assert(enemy_count()==0&&enemy_poses(after)==0); // exploration outside supported opening
 enter(0);for(int i=0;i<5;i++)world_tick();assert(enemy_poses(after)>0);
 puts("Opening actor loading, animated poses, visibility, NPC commands and room cleanup passed");
}

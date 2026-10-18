/* Real NTSC-U gun-shop encounter, using production scripts, actors and player PLC. */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "world.h"
#include "story.h"
#include "script.h"
#include "combat.h"
#include "enemy.h"
#include "combat_audio.h"
#include "effect_snapshot.h"
static int x=-15312,y,z=-13362,d=14464,cam=5;
static void enter(int room){
 char name[80];snprintf(name,sizeof name,"nitrofiles/room/room1%02x0.bin",room);
 FILE *f=fopen(name,"rb");assert(f);fseek(f,0,SEEK_END);long n=ftell(f);rewind(f);
 uint8_t *p=malloc(n);assert(fread(p,1,n,f)==(size_t)n);fclose(f);assert(world_enter(p,n,1,room));free(p);
}
static int voices,cuts,poses,room_sounds,glass,lock_cues;
static void tick(void){
 world_player(x,y,z,d,cam);world_tick();
 int p[STORY_STATE_SIZE];story_state(p);
 if(p[1]){x=p[4];y=p[5];z=p[6];d=p[7];}
 if(p[2]>=0){cam=p[2];cuts++;}poses+=p[8]!=0;
 int audio[COMBAT_AUDIO_MAX*4],n=combat_audio_drain(audio);for(int i=0;i<n;i++){
  room_sounds+=audio[i*4]==1001;
  if(audio[i*4]==1001&&audio[i*4+1]==10){
   lock_cues++;
   int kx,kz;assert(enemy_member_get(0,11,&kx)&&enemy_member_get(0,13,&kz));
   printf("Lock sound: Kendo at %d,%d; door waypoint -15372,-14079\n",kx,kz);
   assert(abs(kx-(-15372))<100&&abs(kz-(-14079))<100);
  }
 }
 int fx[FX_MAX_QUADS*FX_RECORD_SIZE];n=roomfx_snapshot(fx,FX_MAX_QUADS);for(int i=0;i<n;i++)glass+=fx[i*FX_RECORD_SIZE+1]==0x13;
 int v=story_voice_event();if(v>=0){printf("Voice %d at %d,%d cam %d\n",v,x,z,cam);voices++;assert(story_voice_playing());}
 if(*world_message()){printf("Message: %s\n",world_message());world_close_message();}
}
int main(void){
 Model room_anim={0},npc_anim={0};
 assert(model_load(&room_anim,"nitro:/room/room1010_rbj.bin"));
 assert(model_load(&npc_anim,"nitro:/room/room1010_rbj_e0.bin"));
 asset_free(room_anim.data);asset_free(npc_anim.data);
 // The original scheduler clears only transient arrival bits, not story bits.
 world_reset();flag_set(5,0,1);flag_set(5,32,1);flag_set(5,33,1);script_tick();
 assert(flag_get(5,0)&&!flag_get(5,32)&&!flag_get(5,33));
 world_reset();inventory_reset();inventory_add(2,18);flag_set(5,33,1);enter(1);assert(!flag_get(5,33));
 for(int i=0;i<5;i++)tick();
 assert(flag_get(4,26));assert(aots[0].type==SCE_EVENT);
 // Walking into the original welcome AOT starts the scene without an A press.
 x=(aots[5].x[0]+aots[5].x[2])/2;z=(aots[5].z[0]+aots[5].z[2])/2;
 tick();tick();assert(story_locked());
 int result[8];assert(world_trigger(1,result)==0);
 for(int i=0;i<6000&&story_locked();i++)tick();
 printf("Welcome complete: lock %d, voices %d cuts %d poses %d flags %d/%d xyz %d,%d\n",story_locked(),voices,cuts,poses,flag_get(5,0),flag_get(5,1),x,z);
 assert(!story_locked());assert(voices==8&&lock_cues==1);assert(cuts>=6&&poses>100);assert(script_unknown_ops==0);
 // Test the actual Android render bridge, not only the uncropped actor list.
 int poses[MAX_ENEMIES*ENEMY_POSE_SIZE],rendered[MAX_ENEMIES*ENEMY_POSE_SIZE];
 for(int idle=0;idle<120;idle++){
  tick();int n=enemy_poses(poses);
  for(int camera=0;camera<13;camera++){
   world_player(x,y,z,d,camera);assert(world_actors(rendered)==n);
  }
 }
 // Crossing the rear trigger arms the window attack, then entering its AOT fires it.
 x=(aots[3].x[0]+aots[3].x[2])/2;z=(aots[3].z[0]+aots[3].z[2])/2;tick();tick();
 assert(aots[2].type==SCE_EVENT);
 x=(aots[2].x[0]+aots[2].x[2])/2;z=(aots[2].z[0]+aots[2].z[2])/2;tick();tick();assert(story_locked());
 for(int i=0;i<3000&&story_locked();i++)tick();
 assert(!story_locked());assert(flag_get(4,27));assert(voices==10);assert(aots[0].type==SCE_MESSAGE);
 // Original victim death animation must reach its final pose on Kendo's mesh.
 Model victim={0};assert(model_load(&victim,"nitro:/model/em1f.bin"));
 uint32_t sz;uint8_t *sets=asset_load("nitro:/model/em1f_sets.bin",&sz);assert(sets&&sz>=7);
 int anim=(sets[5]|sets[6]<<8)+0x0d;asset_free(sets);
 const ModelAnim *death=&victim.anims[anim];int frame=ANIM_FRAME(victim.anim_pool[death->first+death->count-1]);
 const int16_t *rot=model_frame_rot(&victim,frame);
 for(int i=0;i<180;i++)tick();
 int count=enemy_poses(poses),*kendo=NULL;
 for(int i=0;i<count;i++)if(poses[i*ENEMY_POSE_SIZE+1]==0x48)kendo=poses+i*ENEMY_POSE_SIZE;
 assert(kendo);for(int i=0;i<kendo[7]*3;i++)assert(kendo[11+i]==(rot[i]&32767));
 asset_free(victim.data);
 // Approaching the body and changing cameras must not drop it or the zombies.
 for(int camera=0;camera<13;camera++)for(int dx=-2000;dx<=2000;dx+=500){
  world_player(kendo[2]+dx,0,kendo[4]+1000,d,camera);
  assert(world_actors(rendered)==count);
 }
 world_player(x,y,z,d,cam);
 assert(world_trigger(0,result)==3);world_close_message();assert(world_trigger(1,result)==2&&result[1]==2);
 uint8_t save[SCRIPT_STATE_SIZE+128];int size=world_save(save);
 enter(2);world_restore(save,size);enter(1);for(int i=0;i<5;i++)tick();
 assert(flag_get(4,27));assert(!story_locked());assert(aots[0].type==SCE_MESSAGE);assert(cut_replace(0)==7);
 // On re-entry the original spawn tail selects Kendo's room body animation.
 for(int i=0;i<120;i++)tick();
 Model own={0};assert(model_load(&own,"nitro:/room/room1010_rbj_e0.bin"));
 assert(own.header->num_anims>7);
 const ModelAnim *body=&own.anims[7];int matching=0;
 count=world_actors(rendered);kendo=NULL;
 for(int i=0;i<count;i++)if(rendered[i*ENEMY_POSE_SIZE+1]==0x48)kendo=rendered+i*ENEMY_POSE_SIZE;
 assert(kendo);
 for(int k=0;k<body->count;k++){
  const int16_t *r=model_frame_rot(&own,ANIM_FRAME(own.anim_pool[body->first+k]));int same=1;
  for(int b=0;b<kendo[7]*3;b++)if(kendo[11+b]!=(r[b]&32767))same=0;
  matching|=same;
 }
 assert(matching);asset_free(own.data);
 assert(voices==10);assert(room_sounds>0&&glass>0);puts("Kendo welcome, ambush, control return, exit and story-state re-entry passed");
}

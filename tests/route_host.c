/* Original Leon A route: script events, enemies, pickups and door destinations. */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include "world.h"
#include "story.h"
#include "script.h"
#include "combat.h"
#include "enemy.h"
#include "combat_audio.h"
static int x,y,z,dir,cam;
static void enter(int stage,int id){
 char path[80];snprintf(path,sizeof path,"nitrofiles/room/room%d%02x0.bin",stage,id);
 FILE *f=fopen(path,"rb");assert(f);fseek(f,0,SEEK_END);size_t n=ftell(f);rewind(f);
 uint8_t *data=malloc(n);assert(fread(data,1,n,f)==n);fclose(f);
 assert(world_enter(data,n,stage,id));free(data);
}
static void tick(void){
 world_player(x,y,z,dir,cam);world_tick();
 int s[STORY_STATE_SIZE];story_state(s);if(s[1]){x=s[4];y=s[5];z=s[6];dir=s[7];}if(s[2]>=0)cam=s[2];
 assert(!script_unknown_ops);if(enemy_last_error)fprintf(stderr,"route enemy error %d after %d spawns\n",enemy_last_error,enemy_spawn_calls);assert(!enemy_last_error);
 if(*world_message())world_close_message();
}
static void at(int slot){x=(aots[slot].x[0]+aots[slot].x[2])/2;z=(aots[slot].z[0]+aots[slot].z[2])/2;}
int main(void){
 world_reset();inventory_reset();inventory_add(2,18);flag_set(4,26,1); // Kendo welcome completed; leaving before the window attack.
 enter(1,2);x=-23300;y=0;z=20900;cam=0;for(int i=0;i<5;i++)tick();
 assert(aots[0].type==SCE_EVENT);at(6);tick();tick();assert(story_locked());
 int voice=0,sound=0;
 for(int i=0;i<500;i++){
  tick();if(story_voice_event()==1011)voice++;
  int s[COMBAT_AUDIO_MAX*4],n=combat_audio_drain(s);for(int k=0;k<n;k++)if(s[k*4]==1002&&s[k*4+1]==0x21)sound++;
 }
 assert(!story_locked()&&voice==1&&sound==1&&flag_get(4,27));assert(aots[0].type==SCE_DOOR);
 // Crossing the rear event, viewed from its scripted approach camera, drops the gate.
 at(3);cam=4;tick();for(int i=0;i<180;i++)tick();
 assert(flag_get(4,135)&&!story_locked());
 int flags;assert(enemy_member_get(2,7,&flags)&&!(flags&0x8000));assert(enemy_member_get(3,7,&flags)&&!(flags&0x8000));
 const int route[]={0x18,0x19,0x1a,0x1b,3,0x200};
 for(unsigned r=0;r<sizeof route/sizeof *route;r++){
  int t[NUM_AOTS*14],n=world_triggers(t),slot=-1;
  int target=route[r]==0x200?0x200:0x100+route[r];
  for(int k=0;k<n;k++)if(t[k*14+1]==SCE_DOOR&&t[k*14+13]==target)slot=t[k*14];
  assert(slot>=0);int dest[7];assert(world_trigger(slot,dest)==2);
  enter(dest[0],dest[1]);x=dest[2];y=dest[3];z=dest[4];dir=dest[5];cam=dest[6];
  for(int i=0;i<5;i++)tick();
  if(target!=0x200){assert(enemy_count()>0);printf("Route %03x: %d actors\n",target,enemy_count());}
  if(target==0x119){
   int roles[256];assert(world_camera_zones(roles)==15);assert((roles[6]&2)&&!(roles[8]&2));
   cam=4;for(int i=0;i<240;i++)tick();assert(flag_get(4,152)); // recurring camera check activates bus approach
   for(int slot=1;slot<=4;slot++){int state;assert(enemy_member_get(slot,3,&state)&&state!=8);}
   cam=1;tick();world_camera_zones(roles);assert(!(roles[6]&2)&&(roles[8]&2));
  }
  if(target==0x11a){
   int out[7];assert(world_trigger(2,out)==5);assert(world_confirm(0,out)==0);assert(world_trigger(2,out)==5);
   assert(world_confirm(1,out)==1&&inventory_has(0x14));
   enter(1,0x1a);for(int i=0;i<5;i++)tick();assert(world_trigger(2,out)==0);
  }
 }
 puts("Leon A route: alley scream/gate/reinforcements, camera-triggered bus approach, bus pickup/re-entry, original enemies and all doors to RPD passed");
}

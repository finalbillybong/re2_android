#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include "combat.h"
#include "combat_audio.h"
#include "effect.h"
#include "effect_snapshot.h"
#include "roomfx.h"
#include "world.h"
#include "world_actors.h"
#include "inventory.h"
void game_set_box_flag(int,int);
static int state[COMBAT_STATE_SIZE],fx[FX_MAX_QUADS*FX_RECORD_SIZE];
static void tick(int fire){combat_input(1,fire,0,0,0,0);world_actors_tick(10000,0,10000,0);combat_state(state);}
static int has(int bank,int id){int n=effect_snapshot(fx,FX_MAX_QUADS);n+=roomfx_snapshot(fx+n*FX_RECORD_SIZE,FX_MAX_QUADS-n);for(int i=0;i<n;i++)if(fx[i*FX_RECORD_SIZE]==bank&&fx[i*FX_RECORD_SIZE+1]==id)return 1;return 0;}
int main(void){
 FILE *f=fopen("nitrofiles/room/room1000.bin","rb");assert(f);fseek(f,0,SEEK_END);size_t size=ftell(f);rewind(f);uint8_t *bytes=malloc(size);assert(fread(bytes,1,size,f)==size);fclose(f);
 assert(world_enter(bytes,size,1,0));int flags[65536],n=world_boxes(flags);for(int i=0;i<n;i++)game_set_box_flag(i,0);
 enemy_room_reset();combat_reset(200);inventory_reset();inventory_add(2,18);inventory_equipped=0;
 enemy_spawn(0,0x10,0,1,15000,0,10000,2048);combat_audio_clear();
 for(int i=0;i<100&&!state[9];i++)tick(1);
 assert(state[9]==1&&state[10]==0&&has(0,0)); // blood only after a real hit
 int audio[COMBAT_AUDIO_MAX*4],sounds=combat_audio_drain(audio),shot=0;
 for(int i=0;i<sounds;i++)if(audio[i*4]==2&&audio[i*4+1]==0)shot++;
 assert(shot==1&&combat_audio_drain(audio)==0);
 tick(0);assert(has(1,1)&&has(1,3)); // key 1: muzzle flash/fire
 tick(0);tick(0);assert(has(1,2)); // key 3: casing and smoke
 int quads=roomfx_snapshot(fx,FX_MAX_QUADS);assert(quads>0);
 for(int i=0;i<quads;i++){int *p=fx+i*FX_RECORD_SIZE;assert(abs(p[3]-10000)<4000&&abs(p[5]-10000)<4000);assert(p[8]>p[6]&&p[9]>p[7]);}
 assert(effect_snapshot(fx,0)==0&&roomfx_snapshot(fx,0)==0);
 // No more emissions: all temporary effects, including grounded casings, expire.
 for(int i=0;i<600;i++){effect_tick();roomfx_tick();}
 assert(effect_count()==0&&roomfx_count()==0);
 // Knife and dry fire cannot create muzzle sprites; missing enemies cannot create blood.
 enemy_room_reset();effect_clear();roomfx_load(0,0);combat_reset(200);
 inventory[0].count=0;for(int i=0;i<60;i++)tick(1);assert(state[9]==0&&!has(0,0)&&!has(1,1));
 tick(0);tick(1);sounds=combat_audio_drain(audio);int click=0;for(int i=0;i<sounds;i++)if(audio[i*4]==2&&audio[i*4+1]==1)click++;assert(click==1);
 inventory_reset();inventory_add(1,0);inventory_equipped=0;combat_reset(200);for(int i=0;i<60;i++)tick(1);assert(state[9]>0&&!has(1,1)&&!has(1,2)&&!has(0,0));
 effect_blood(0,0,-1000,0,0,20,0);assert(effect_count()>0);
 assert(world_enter(bytes,size,1,0));assert(effect_count()==0&&roomfx_count()==0&&combat_audio_drain(audio)==0);free(bytes);
 puts("Effects: real hit blood, gunshot event, keyframe flash/smoke/casing anchors, expiry, bounded snapshots, dry fire, knife and room cleanup passed");
}

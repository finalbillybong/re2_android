// cc -Wall -Wextra -Werror -fsanitize=address,undefined -Isource -Iandroid/app/src/main/cpp tests/world_host.c android/app/src/main/cpp/world.c source/script.c source/inventory.c -o build/world_host
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "script.h"
#include "world.h"
static void enter_stage(int stage,int id){
 char path[128];snprintf(path,sizeof path,"nitrofiles/room/room%d%02x0.bin",stage,id);
 FILE *f=fopen(path,"rb");assert(f);fseek(f,0,SEEK_END);size_t n=ftell(f);rewind(f);
 uint8_t *p=malloc(n);assert(fread(p,1,n,f)==n);fclose(f);
 assert(world_enter(p,n,stage,id));free(p);
 for(int i=0;i<300;i++){world_player(0,0,0,0,0);world_tick();world_close_message();}
 assert(script_unknown_ops==0);
}
static void enter(int id){enter_stage(1,id);}
static int visible(int model){int data[32*9],n=world_objects(data);for(int i=0;i<n;i++)if(data[i*9]==model)return 1;return 0;}
static int find(int type,int destination){int t[32*14],n=world_triggers(t);for(int i=0;i<n;i++)if(t[i*14+1]==type&&(destination<0||t[i*14+13]==destination))return t[i*14];return -1;}
int main(void){
 int result[7]={0};world_reset();inventory_reset();inventory_add(2,18);enter(0);
 int door=find(SCE_DOOR,0x101);assert(door>=0&&world_trigger(door,result)==2&&result[1]==1);
 enter(1);int item=find(SCE_ITEM,-1);assert(item>=0);
 int model=aots[item].data[6];assert(visible(model));
 assert(world_trigger(item,result)==5);assert(!inventory_has(0x14));assert(visible(model));
 assert(world_confirm(0,result)==0);assert(!inventory_has(0x14));assert(visible(model));
 for(int i=1;i<8;i++)assert(inventory_add(1,0));
 assert(world_trigger(item,result)==5);assert(world_confirm(1,result)==3);assert(visible(model));assert(!inventory_has(0x14));world_close_message();
 inventory[7].item=0;assert(world_trigger(item,result)==5);assert(world_confirm(1,result)==1);assert(!visible(model));assert(result[0]==0x14&&result[1]==15);
 assert(inventory_has(0x14));assert(world_trigger(item,result)==0);
 uint8_t saved[SCRIPT_STATE_SIZE+128];assert(world_save(saved)==sizeof saved);
 door=find(SCE_DOOR,0x100);assert(door>=0&&world_trigger(door,result)==2&&result[1]==0);
 enter(0);enter(1);assert(!visible(model));assert(world_trigger(item,result)==0); // no duplicate after re-entry
 world_reset();assert(world_restore(saved,sizeof saved));enter(1);assert(world_trigger(item,result)==0);
 int another=find(SCE_ITEM,-1);assert(another>=0);assert(world_trigger(another,result)==5);
 enter(0);assert(world_confirm(1,result)==0);enter(1); // pending choices cannot cross rooms
 assert(!world_restore(saved,12));assert(!world_enter(saved,12,1,0));
 door=find(SCE_DOOR,0x102);assert(door>=0&&world_trigger(door,result)==2&&result[1]==2);
 enter(2);door=find(SCE_DOOR,0x118);assert(door>=0&&world_trigger(door,result)==2);assert(result[1]==0x18);
 door=find(SCE_DOOR,0x101);assert(door>=0&&world_trigger(door,result)==2&&result[1]==1);
 FILE *index=fopen("nitrofiles/room/index.bin","rb");assert(index);
 int count=fgetc(index);count|=fgetc(index)<<8;assert(count==125);
 for(int i=0;i<count;i++){int stage=fgetc(index),id=fgetc(index);world_reset();enter_stage(stage,id);}
 fclose(index);
 puts("World: route 100↔101↔102, confirmation/cancel, visible items, full inventory, real pickup, no duplicates, flags restore and all 125 room scripts passed");
}

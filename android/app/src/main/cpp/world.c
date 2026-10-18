/* Exploration bridge. Real room scripts register triggers and item flags.
 * Cinematic actor/position/camera hooks are intentionally not applied yet.
 * Converted door definitions remain usable when a cutscene replaces an AOT.
 */
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "script.h"
#include "world.h"
static Room room;
static size_t room_size;
static uint8_t locks[128];
static char message[1024];
static int actors[32][32], objects[32][32];
static int player[32];
static int pending=-1;
static uint8_t pending_data[8];
static int u16(const uint8_t *p){return p[0]|p[1]<<8;}
static int range(size_t start,size_t count,size_t size){return start<=size&&count<=size-start;}
const char *room_message(const Room *r,int n){
    if(n<0||n>=r->header->num_msgs)return NULL;
    size_t start=r->header->off_msgs;
    if(!range(start,(size_t)r->header->num_msgs*2,room_size))return NULL;
    const uint8_t *p=(const uint8_t *)r->data+start;
    size_t at=start+u16(p+n*2);
    if(at>=room_size||!memchr((const uint8_t *)r->data+at,0,room_size-at))return NULL;
    return (const char *)r->data+at;
}
void game_show_message(const char *text){snprintf(message,sizeof message,"%s",text?text:"");}
int game_message_open(void){return message[0]!=0;}
const char *world_message(void){return message;}
void world_close_message(void){message[0]=0;}
/* These hooks are deliberately deferred in the exploration build. */
void game_cut_change(int n){(void)n;}
void game_set_player_pos(int32_t x,int32_t y,int32_t z){(void)x;(void)y;(void)z;}
void game_set_player_dir(int16_t d){(void)d;}
void game_set_box_flag(int n,int f){if(room.data&&n>=0&&n<room.header->num_boxes)((RoomBox *)room.boxes)[n].flag=(uint16_t)f;}
void game_play_sound(int b,int s){(void)b;(void)s;}
void game_bgm_control(int i,int o,int t,int l,int r){(void)i;(void)o;(void)t;(void)l;(void)r;}
void game_bgmtbl_set(int s,int r,int m,int u){(void)s;(void)r;(void)m;(void)u;}
void game_voice(int k,int n){(void)k;(void)n;}
int game_voice_playing(void){return 0;}
void game_espr_on(const uint8_t *p){(void)p;}
void game_espr_kill(int i,int t){(void)i;(void)t;}
void game_floor_sound_set(int n,int on){(void)n;(void)on;}
void game_object_hide(int n){if(n>=0&&n<32)objects[n][1]=1;}
void game_object_set(const uint8_t *p){
 int n=p[1];if(n>=32)return;objects[n][0]=1;objects[n][2]=p[2];objects[n][3]=u16(p+10)|1;
 for(int k=0;k<3;k++){objects[n][11+k]=(int16_t)u16(p+14+k*2);objects[n][14+k]=(int16_t)u16(p+20+k*2);}
}
int world_objects(int *out){
 int count=0;
 for(int n=0;n<32;n++)if(objects[n][0]&&!objects[n][1]){
  int *p=out+count++*9;p[0]=n;p[1]=objects[n][2];p[2]=objects[n][3];
  for(int k=0;k<6;k++)p[3+k]=objects[n][11+k];
 }
 return count;
}
void game_spawn_enemy(int s,int t,int f,int v,int x,int y,int z,int d){
    (void)v;if(s<0||s>=32)return;actors[s][0]=t;actors[s][7]=f;actors[s][11]=x;actors[s][12]=y;actors[s][13]=z;actors[s][15]=d;
}
int game_enemy_member(int s,int m,int *v){if(s<0||s>=32||m<0||m>=32)return 0;*v=actors[s][m];return 1;}
void game_enemy_member_set(int s,int m,int v){if(s>=0&&s<32&&m>=0&&m<32)actors[s][m]=v;}
int game_object_member(int s,int m,int *v){if(s<0||s>=32||m<0||m>=32)return 0;*v=objects[s][m];return 1;}
void game_object_member_set(int s,int m,int v){if(s>=0&&s<32&&m>=0&&m<32)objects[s][m]=v;}
int game_player_member(int m,int *v){if(m<0||m>=32)return 0;*v=player[m];return 1;}
void game_player_member_set(int m,int v){if(m>=0&&m<32)player[m]=v;}
void game_plc(int w,int i,const uint8_t *p){(void)w;(void)i;(void)p;}
void world_reset(void){pending=-1;script_state_reset();memset(locks,0,sizeof locks);message[0]=0;}
int world_enter(const uint8_t *data,size_t size,int stage,int id){
    if(stage<1||stage>7||id<0||id>255||size<sizeof(RoomHeader))return 0;
    const RoomHeader *h=(const RoomHeader *)data;
    size_t arrays=sizeof(*h)+(size_t)h->num_cameras*sizeof(RoomCamera)+(size_t)h->num_switches*sizeof(RoomSwitch)+(size_t)h->num_boxes*sizeof(RoomBox)+(size_t)h->num_doors*sizeof(RoomDoor);
    if(h->magic!=ROOM_MAGIC||h->version!=ROOM_VERSION||arrays>size||!range(h->off_init,h->size_init,size)||!range(h->off_exec,h->size_exec,size))return 0;
    void *copy=malloc(size);if(!copy)return 0;memcpy(copy,data,size);
    free(room.data);memset(&room,0,sizeof room);room.data=copy;room.header=copy;room_size=size;h=room.header;
    room.cameras=(const RoomCamera *)(h+1);room.switches=(const RoomSwitch *)(room.cameras+h->num_cameras);
    room.boxes=(const RoomBox *)(room.switches+h->num_switches);room.doors=(const RoomDoor *)(room.boxes+h->num_boxes);
    room.init_script=h->size_init?(const uint8_t *)copy+h->off_init:NULL;
    room.exec_script=h->size_exec?(const uint8_t *)copy+h->off_exec:NULL;
    pending=-1;message[0]=0;memset(actors,0,sizeof actors);memset(objects,0,sizeof objects);
    script_room_enter(&room);return 1;
}
void world_player(int x,int y,int z,int direction,int camera){
    player[11]=x;player[12]=y;player[13]=z;player[15]=direction/8;
    static int previous;script_camera_changed(previous,camera);previous=camera;
}
int world_boxes(int *out){if(!room.data)return 0;int n=room.header->num_boxes;for(int i=0;i<n;i++)out[i]=room.boxes[i].flag;return n;}
void world_tick(void){if(room.data&&!message[0]&&pending<0)script_tick();}
static const RoomDoor *door_at(int slot){
    if(!room.data)return NULL;
    for(int i=0;i<room.header->num_doors;i++)if(room.doors[i].aot==slot)return &room.doors[i];
    return NULL;
}
static int supported(const Aot *a){return a->type==SCE_ITEM||a->type==SCE_MESSAGE;}
int world_triggers(int *out){
    int count=0;
    for(int i=0;i<NUM_AOTS;i++){
        Aot *a=&aots[i];const RoomDoor *d=door_at(i);
        if(!d&&(!a->active||!supported(a)))continue;
        int *p=out+count++*14;p[0]=i;p[1]=d?SCE_DOOR:a->type;p[2]=d?d->sat:a->sat;p[3]=d?d->floor:a->floor;
        for(int k=0;k<4;k++){p[4+k]=d?d->x[k]:a->x[k];p[8+k]=d?d->z[k]:a->z[k];}
        p[12]=d?0:u16(a->data);p[13]=d?d->next_stage*256+d->next_room:0;
    }
    return count;
}
int world_trigger(int slot,int *out){
    if(slot<0||slot>=NUM_AOTS)return 0;
    Aot *a=&aots[slot];const RoomDoor *d=door_at(slot);
    if(d){
        if(d->next_stage<1||d->next_stage>7){game_show_message("The destination stage is not available.");return 3;}
        if(d->key_id&&!locks[d->key_id&127]){
            if(d->key_type==0xff){game_show_message("It's locked from the other side.");return 3;}
            if(d->key_type!=0xfe&&!inventory_has(d->key_type)){game_show_message("It's locked.");return 3;}
            locks[d->key_id&127]=1;game_show_message("You unlocked the door.");return 3;
        }
        out[0]=d->next_stage;out[1]=d->next_room;out[2]=d->next[0];out[3]=d->next[1];out[4]=d->next[2];out[5]=d->next_dir;out[6]=d->next_camera;return 2;
    }
    if(!a->active)return 0;
    if(a->type==SCE_ITEM){
        int picked=u16(a->data+4);
        if(flag_get(FLAGS_ITEM_PICKED,picked)){a->active=0;return 0;}
        pending=slot;memcpy(pending_data,a->data,sizeof pending_data);
        out[0]=u16(a->data);out[1]=u16(a->data+2);return 5;

    }
    if(a->type==SCE_MESSAGE){game_show_message(room_message(&room,a->data[0]));return 3;}
    return 4;
}
int world_confirm(int yes,int *out){
 int slot=pending;pending=-1;if(!yes||slot<0||slot>=NUM_AOTS)return 0;
 Aot *a=&aots[slot];
 if(!a->active||a->type!=SCE_ITEM||memcmp(a->data,pending_data,sizeof pending_data))return 0;
 int item=u16(a->data),amount=u16(a->data+2),picked=u16(a->data+4);
 if(flag_get(FLAGS_ITEM_PICKED,picked))return 0;
 if(!inventory_add(item,amount)){game_show_message("You cannot carry any more.");return 3;}
 flag_set(FLAGS_ITEM_PICKED,picked,1);a->active=0;game_object_hide(a->data[6]);
 out[0]=item;out[1]=amount;return 1;
}
int world_save(uint8_t *out){script_state_get(out);memcpy(out+SCRIPT_STATE_SIZE,locks,sizeof locks);return SCRIPT_STATE_SIZE+sizeof locks;}
int world_restore(const uint8_t *in,size_t size){if(size!=SCRIPT_STATE_SIZE+sizeof locks)return 0;script_state_set(in);memcpy(locks,in+SCRIPT_STATE_SIZE,sizeof locks);return 1;}

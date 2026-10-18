/* Original room-script player controller. Render snapshots never advance time. */
#include <stdio.h>
#include "route.h"
#include <string.h>
#include "story.h"
#include "script.h"
#include "plc.h"
#include "world.h"
static Model own,room_set;
static Plc plc;
static PlcBody body;
static int enabled,stage_id,py,dirty,camera=-1,voice_ticks,voice_event=-1;
void story_enter(int stage,int room){
    enabled=opening_route(stage,room)&&room!=0;stage_id=stage;
    memset(&plc,0,sizeof plc);memset(&body,0,sizeof body);
    asset_free(room_set.data);memset(&room_set,0,sizeof room_set);
    dirty=0;camera=-1;voice_ticks=0;voice_event=-1;py=0;
    if(!enabled)return;
    if(!own.data)model_load(&own,"nitro:/model/leon_w02.bin");
    char path[80];snprintf(path,sizeof path,"nitro:/room/room%d%02x0_rbj.bin",stage,room);
    model_load(&room_set,path);
    body=(PlcBody){.set=&own,.own_set=&own,.room_set=room_set.data?&room_set:NULL,
        .anim=12,.own_first=10,.walk_anim=10,.idle_anim=12,.table=16};
}
int story_enabled(void){return enabled;}
int story_locked(void){return enabled&&(plc.active||flag_get(2,7)||flag_get(1,27));}
void story_player(int x,int y,int z,int dir,int cam){
    if(!enabled)return;
    if(!dirty&&!plc.active){body.x=x;py=y;body.z=z;body.dir=dir;}
    (void)cam;
}
void story_position(int x,int y,int z){if(enabled){body.x=x;py=y;body.z=z;dirty=1;}}
void story_direction(int dir){if(enabled){body.dir=(dir*8)&32767;dirty=1;}}
void story_cut(int cam){if(enabled)camera=cam;}
int story_member(int member,int *v){
    if(!enabled)return 0;
    switch(member){case 11:*v=body.x;break;case 12:*v=py;break;case 13:*v=body.z;break;case 15:*v=(body.dir/8)&4095;break;default:return 0;}return 1;
}
void story_member_set(int member,int v){
    if(!enabled)return;
    switch(member){case 11:body.x=(int16_t)v;break;case 12:py=(int16_t)v;break;case 13:body.z=(int16_t)v;break;case 15:body.dir=((int16_t)v*8)&32767;break;default:return;}dirty=1;
}
void story_plc(const uint8_t *ins){if(enabled&&own.data){plc_command(&plc,&body,ins);dirty=1;}}
void story_tick(void){
    if(voice_ticks>0)voice_ticks--;
    if(enabled&&plc.active){plc_tick(&plc,&body);if(body.footstep)world_footstep(body.x,py,body.z,plc.mode==PLC_RUN);dirty=1;}
}
void story_state(int *out){
    memset(out,0,STORY_STATE_SIZE*sizeof(int));out[2]=-1;out[3]=1;
    for(int i=0;i<32;i++)out[111+i]=i;
    if(!enabled)return;
    out[0]=story_locked();out[1]=dirty;out[2]=camera;out[3]=cut_auto_enabled();
    out[4]=body.x;out[5]=py;out[6]=body.z;out[7]=body.dir;
    const Model *m=body.set;
    if(plc.active&&m&&m->data&&body.anim>=0&&body.anim<m->header->num_anims){
        const ModelAnim *a=&m->anims[body.anim];
        if(body.key>=0&&body.key<a->count){
            int index=ANIM_FRAME(m->anim_pool[a->first+body.key]);
            const ModelFrame *f=model_frame(m,index);const int16_t *rot=model_frame_rot(m,index);
            out[8]=1;for(int i=0;i<3;i++){out[9+i]=f->offset[i];out[12+i]=f->speed[i];}
            for(int i=0;i<m->header->num_bones*3&&i<96;i++)out[15+i]=rot[i];
        }
    }
    for(int i=0;i<32;i++)out[111+i]=cut_replace(i);
    dirty=0;camera=-1;
}
void story_voice(int kind,int clip){
    if(!enabled)return;
    int bank=kind?0:stage_id;char path[80];snprintf(path,sizeof path,"nitro:/voice/v%d_%02d.wav",bank,clip);
    uint32_t size=0;uint8_t *p=asset_load(path,&size);voice_ticks=0;
    if(p&&size>=44){uint32_t rate,bytes;memcpy(&rate,p+28,4);memcpy(&bytes,p+40,4);
        if(rate&&bytes<=size-44){voice_ticks=(int)(((uint64_t)bytes*30+rate-1)/rate);voice_event=bank*1000+clip;}}
    asset_free(p);
}
int story_voice_playing(void){return voice_ticks>0;}
int story_voice_event(void){int result=voice_event;voice_event=-1;return result;}

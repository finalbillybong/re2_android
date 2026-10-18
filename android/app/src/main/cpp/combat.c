/* Android combat: original weapon moves, cadence/damage and enemy grab callbacks. */
#include <math.h>
#include <string.h>
#include "combat.h"
#include "story.h"
#include "inventory.h"
#include "roomfx.h"
#include "portable_angles.h"
#include "combat_audio.h"
static Model weapons[2];
static int hp=200,grabbed,dead,finished,pose_valid;
static int x,y,z,dir,mode,anim,key,weapon,shots,hit=-1;
static int aim,fire,pressed,turn,posture,noise,fire_down;
static int target_x,target_z,target;
static int pose[102];
enum {IDLE,RAISE,HOLD,FIRE,RELOAD};
void combat_input(int a,int f,int p,int t,int posture_in,int n){
    aim=a;fire_down=f&&!fire;fire=f;pressed=p;turn=t;
    posture=posture_in<0?1:posture_in>0?2:0;noise=n;
}
void combat_room_enter(void){
    mode=IDLE;grabbed=pose_valid=0;aim=fire=pressed=fire_down=0;
    if(dead)finished=1;
}
void combat_reset(int health){
    hp=health<0?0:health>200?200:health;dead=finished=hp==0;shots=0;hit=-1;
    combat_room_enter();
}
int combat_heal(int amount){
    if(amount<=0||dead||grabbed||story_locked())return 0;
    if(hp>=200)return -1;
    hp+=amount;if(hp>200)hp=200;
    return 1;
}
static const Model *model(void){
    if(weapon<1||weapon>2)return NULL;
    Model *m=&weapons[weapon-1];
    if(!m->data)model_load(m,weapon==1?"nitro:/model/leon_w01.bin":"nitro:/model/leon_w02.bin");
    return m->data?m:NULL;
}
static int length(void){
    const Model *m=model();
    return m&&anim<m->header->num_anims?m->anims[anim].count:1;
}
// Same skeleton/hand anchor and PS1 effect parameters as source/main.c:shot_effects.
static void hand_point(const int local[3],int out[3]){
    const Model *m=model();
    int index=ANIM_FRAME(m->anim_pool[m->anims[anim].first+key]);
    const ModelFrame *frame=model_frame(m,index);const int16_t *rot=model_frame_rot(m,index);
    float p[3]={(float)local[0],(float)local[1],(float)local[2]};
    for(int b=11;b>=0&&b<m->header->num_bones;b=m->bones[b].parent){
        const int16_t *r=rot+b*3;float a,c,s,t;
        a=r[2]*6.283185307f/32768;c=cosf(a);s=sinf(a);t=c*p[0]-s*p[1];p[1]=s*p[0]+c*p[1];p[0]=t;
        a=r[1]*6.283185307f/32768;c=cosf(a);s=sinf(a);t=c*p[0]+s*p[2];p[2]=-s*p[0]+c*p[2];p[0]=t;
        a=r[0]*6.283185307f/32768;c=cosf(a);s=sinf(a);t=c*p[1]-s*p[2];p[2]=s*p[1]+c*p[2];p[1]=t;
        for(int k=0;k<3;k++)p[k]+=m->bones[b].parent<0?frame->offset[k]:m->bones[b].pos[k];
    }
    float a=dir*6.283185307f/32768,c=cosf(a),s=sinf(a);
    out[0]=x+(int)(c*p[0]+s*p[2]);out[1]=y+(int)p[1];out[2]=z+(int)(-s*p[0]+c*p[2]);
}
static void shot_effects(void){
    int point[3];
    if(key==1){
        const int muzzle[3]={138,615,8};hand_point(muzzle,point);
        roomfx_spawn(1,0,point[0],point[1],point[2],16,0);
        roomfx_spawn(3,0x0a,point[0],point[1],point[2],16,0);
    }else if(key==3){
        const int port[3]={350,40,-15};hand_point(port,point);
        roomfx_spawn(3,0x08,point[0],point[1],point[2],16,0);
        int angle=dir+1300*8;
        roomfx_throw(2,0,0,point[0],point[1],point[2],30*cosLerp(angle)>>12,-45,-(30*sinLerp(angle)>>12),y,4096*3/10);
    }
}
static void move(int next,int animation){mode=next;anim=animation;key=0;}
void combat_tick(int px,int py,int pz,int direction,int enabled){
    if(!grabbed&&!dead){x=px;y=py;z=pz;dir=direction;pose_valid=0;}
    if(grabbed||dead)return;
    int item=inventory_equipped<0?0:inventory[inventory_equipped].item;
    if(item!=weapon){weapon=item;mode=IDLE;}
    if(!enabled||story_locked()||item<1||item>2||!model()){mode=IDLE;return;}
    if(mode==RELOAD){
        if(key==7)combat_audio_play(weapon,4,127,64);
        if(key==22){combat_audio_play(weapon,3,127,64);inventory_reload();}
        if(++key>=length())move(HOLD,21+posture*2);
        return;
    }
    if(!aim&&!(mode==FIRE&&key<=(weapon==1?50:8))){mode=IDLE;return;}
    if(mode==IDLE){
        move(RAISE,19);
        target=enemy_aim_target(x,y,z,weapon==1?4500:0,&target_x,&target_z);
    }
    if(mode==RAISE&&target>=0){
        int want=(int)(atan2(-(double)(target_z-z),(double)(target_x-x))*32768/6.283185307179586)&32767;
        int diff=((want-dir+16384)&32767)-16384;
        dir=(dir+(diff>1728?1728:diff< -1728?-1728:diff))&32767;
    }else dir=(dir+turn*(mode==FIRE?64:512))&32767;
    if(mode==HOLD){
        anim=21+posture*2;key=0;
        if(aim&&fire){
            InvSlot *slot=&inventory[inventory_equipped];
            if(weapon==1||slot->count){
                static const uint8_t knife[]={3,0,0},gun[]={16,15,14};
                if(weapon!=1)slot->count--;
                move(FIRE,20+posture*2);shots++;noise|=NOISE_SHOT;
                combat_audio_play(weapon,0,127,64);
                hit=enemy_hit(x,y,z,dir,weapon==1?1500:0,weapon,weapon==1?knife:gun);
                return;
            }
            if(fire_down){if(inventory_spare_ammo())move(RELOAD,26);else combat_audio_play(weapon,1,127,64);}
        }
    }else if(++key>=length())move(HOLD,21+posture*2);
    else if(mode==FIRE&&weapon==2)shot_effects();
}
void combat_target(EnemyTarget *t){
    t->x=x;t->y=y;t->z=z;t->dir=dir;t->noise=noise;t->pressed=pressed;
    t->can_grab=!grabbed&&!dead&&!story_locked();t->dead=dead;
}
void combat_state(int *out){
    // hp, flags (movement locked, grabbed, dead, death finished, external pose),
    // x,y,z,dir, animation,key, weapon, shot count,last hit, then original frame.
    out[0]=hp;out[1]=(mode!=IDLE||grabbed||dead?1:0)|(grabbed?2:0)|(dead?4:0)|(finished?8:0)|(pose_valid?16:0);
    out[2]=x;out[3]=y;out[4]=z;out[5]=dir;out[6]=anim;out[7]=key;out[8]=weapon;out[9]=shots;out[10]=hit;
    memcpy(out+11,pose,sizeof pose);
}
int game_player_damage(int damage){
    if(dead)return 2;
    if(damage<41)damage+=damage>>1;
    hp-=damage;
    if(hp<=0){hp=0;dead=1;return 2;}
    return 0;
}
void game_player_grabbed(int on){grabbed=on;mode=IDLE;if(!on)pose_valid=0;}
void game_player_pose(const Model *set,int animation,int frame,int32_t px,int32_t pz,int16_t direction){
    const ModelAnim *a=&set->anims[animation];
    int index=ANIM_FRAME(set->anim_pool[a->first+frame]);
    const ModelFrame *f=model_frame(set,index);const int16_t *rot=model_frame_rot(set,index);
    memset(pose,0,sizeof pose);
    for(int i=0;i<3;i++)pose[i]=f->offset[i];
    for(int i=0;i<set->header->num_bones*3&&i<96;i++)pose[6+i]=rot[i];
    x=px;z=pz;dir=direction;pose_valid=1;
}
void game_player_died(void){finished=1;}

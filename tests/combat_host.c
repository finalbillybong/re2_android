#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include "combat.h"
#include "inventory.h"
#include "world.h"
void game_set_box_flag(int,int);
static int state[COMBAT_STATE_SIZE];
static void tick(int aim,int fire,int pressed){
    combat_input(aim,fire,pressed,0,0,0);
    combat_tick(10000,0,10000,0,1);
    EnemyTarget target;combat_target(&target);enemy_tick(&target);
    combat_state(state);
}
static void setup(int ammo,int hp,int distance){
    enemy_room_reset();combat_reset(hp);inventory_reset();inventory_add(2,ammo);inventory_equipped=0;
    enemy_spawn(0,0x10,0,0,10000+distance,0,10000,2048);
}
int main(void){
    FILE *f=fopen("nitrofiles/room/room1000.bin","rb");assert(f);fseek(f,0,SEEK_END);size_t size=ftell(f);rewind(f);
    unsigned char *data=malloc(size);assert(fread(data,1,size,f)==size);fclose(f);
    assert(world_enter(data,size,1,0));free(data);
    int flags[65536],boxes=world_boxes(flags);for(int i=0;i<boxes;i++)game_set_box_flag(i,0);
    enemy_room_reset();combat_reset(200);
    enemy_spawn(0,0x10,6,0,10000,0,10000,2048);
    enemy_spawn(1,0x10,6,0,11000,0,10000,2048);
    EnemyTarget crowd_target={.x=0,.y=0,.z=10000,.can_grab=0};
    for(int i=0;i<180;i++){
        enemy_tick(&crowd_target);int ax,az,bx,bz;
        assert(enemy_member_get(0,11,&ax)&&enemy_member_get(0,13,&az));
        assert(enemy_member_get(1,11,&bx)&&enemy_member_get(1,13,&bz));
        assert((int64_t)(ax-bx)*(ax-bx)+(int64_t)(az-bz)*(az-bz)>=800*800);
    }
    setup(18,200,5000);
    for(int i=0;i<100&&state[9]==0;i++)tick(1,1,0);
    assert(state[9]==1&&inventory[0].count==17&&state[10]==0); // real hit, one spent round
    int poses[MAX_ENEMIES*ENEMY_POSE_SIZE];assert(enemy_poses(poses)==1&&poses[6]>0); // recoil
    for(int i=0;i<8;i++)tick(1,1,0);
    assert(inventory[0].count==17); // original 23-key firing move limits cadence
    for(int i=0;i<500&&inventory[0].count;i++)tick(1,1,0);
    int enemy_state;assert(enemy_member_get(0,3,&enemy_state));assert(enemy_state==20||enemy_state==21);
    assert(inventory[0].count==0);
    int shots=state[9];for(int i=0;i<50;i++)tick(1,1,0);assert(state[9]==shots); // empty cannot fire
    inventory_add(0x14,30);tick(1,0,0);tick(1,1,1); // new press starts reload
    for(int i=0;i<22;i++)tick(1,0,0);
    assert(inventory[0].count==0);
    tick(1,0,0);assert(inventory[0].count==18&&inventory[1].count==12);
    // Release aim, then knife: no ammunition is spent.
    setup(18,200,1200);inventory_reset();inventory_add(1,0);inventory_equipped=0;
    for(int i=0;i<120;i++)tick(1,1,1);
    assert(inventory[0].item==1&&inventory[0].count==0);
    // A real near zombie grabs Leon; its victim pose is exported and damage is applied.
    setup(18,200,600);int saw_pose=0,saw_damage=0,saw_release=0,saw_grab=0;
    for(int i=0;i<1200;i++){
        tick(0,0,1);
        if(state[1]&2){assert(combat_heal(50)==0);saw_grab=1;if(state[1]&16)saw_pose=1;}
        else if(saw_grab){saw_release=1;break;}
        if(state[0]<200)saw_damage=1;
    }
    assert(saw_grab&&saw_pose&&saw_release); // button presses let Leon struggle free
    // Struggling free pushes a zombie into the one behind it. NPCs and enemies
    // on another floor must not be affected by this contact.
    setup(18,200,600);int placed=0,shoved=0;
    for(int i=0;i<1500&&!shoved;i++){
        tick(0,0,1);int sub;
        if((state[1]&2)&&!placed){
            int x,z;assert(enemy_member_get(0,11,&x)&&enemy_member_get(0,13,&z));
            enemy_spawn(1,0x10,0,0,x+900,0,z,2048);
            enemy_spawn(2,0x10,0,0,x+900,-1800,z,2048);
            enemy_spawn(3,0x48,0,0,x+900,0,z,2048);placed=1;
        }
        if(placed&&enemy_member_get(1,3,&sub)&&sub==9)shoved=1;
    }
    assert(placed&&shoved);int other;assert(enemy_member_get(2,3,&other)&&other!=9);
    assert(enemy_member_get(3,3,&other)&&other!=9);
    setup(18,20,600);int finished=0;
    for(int i=0;i<2000;i++){
        tick(0,0,0);if(state[0]<20)saw_damage=1;
        if(state[1]&8){finished=1;break;}
    }
    assert(saw_damage&&finished&&state[0]==0&&(state[1]&4));
    struct {RoomHeader header;RoomCamera camera;RoomBox box;} wall={0};
    wall.header.magic=ROOM_MAGIC;wall.header.version=ROOM_VERSION;wall.header.num_cameras=1;wall.header.num_boxes=1;
    wall.box.x=11000;wall.box.z=9000;wall.box.w=1000;wall.box.d=2000;wall.box.flag=0xfc00;wall.box.floors=1;wall.box.type=2<<6;
    assert(world_enter((const uint8_t *)&wall,sizeof wall,1,0));setup(18,200,5000);
    state[9]=0;for(int i=0;i<100&&state[9]==0;i++)tick(1,1,0);
    assert(state[9]==1&&state[10]==-1&&inventory[0].count==17);
    combat_reset(200);combat_state(state);assert(state[0]==200&&state[1]==0);
    puts("Combat: aiming, original shot cadence, ammo, recoil/death, reload timing, knife, grab poses, struggle, damage, death and reset passed");
}

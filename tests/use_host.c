// Production item-use integration, run by tools/test_android_world.sh.
#include <assert.h>
#include <stdio.h>
#include "world.h"
#include "combat.h"
#include "script.h"
static int health(void){int s[COMBAT_STATE_SIZE];combat_state(s);return s[0];}
int main(void){
 struct {RoomHeader h;RoomDoor d;} r={0};
 r.h.magic=ROOM_MAGIC;r.h.version=ROOM_VERSION;r.h.num_doors=1;
 r.d=(RoomDoor){.x={1000,1400,1400,1000},.z={-200,-200,200,200},.next_stage=1,.next_room=2,.sat=0x11,.key_id=0x81,.key_type=0x59,.aot=0};
 world_reset();assert(world_enter((const uint8_t *)&r,sizeof r,1,0));
 const int items[]={0x23,0x26,0x29,0x2a,0x2b,0x2c,0x2d,0x2e};
 const int amounts[]={200,50,100,200,50,200,100,200};
 for(unsigned i=0;i<sizeof items/sizeof *items;i++){
  inventory_reset();inventory_add(items[i],0);combat_reset(10);
  assert(world_use_item(0)==1);assert(health()==(amounts[i]+10>200?200:amounts[i]+10));assert(!inventory[0].item);
  inventory_add(items[i],0);combat_reset(190);assert(world_use_item(0)==1&&health()==200);
  inventory_add(items[i],0);assert(world_use_item(0)==-1&&inventory[0].item==items[i]);
  combat_reset(0);assert(world_use_item(0)==0&&inventory[0].item==items[i]&&health()==0);
 }
 inventory_reset();combat_reset(50);inventory_add(0x27,0);inventory_add(0x28,0);inventory_add(0x14,20);
 for(int i=0;i<3;i++)assert(!world_use_item(i)&&inventory[i].item);
 assert(!world_use_item(-1)&&!world_use_item(8)&&!world_use_item(7)&&health()==50);
 inventory_reset();inventory_add(0x59,0);inventory_add(0x5a,0);
 world_player(0,0,0,0,0);assert(!world_use_item(0)); // too far
 world_player(500,-1800,0,0,0);assert(!world_use_item(0)); // wrong floor
 world_player(500,0,0,16384,0);assert(!world_use_item(0)); // facing away
 world_player(500,0,0,0,0);assert(!world_use_item(1)); // wrong key
 int out[7];assert(world_trigger(0,out)==3);world_close_message(); // automatic carried-key unlock
 world_reset();assert(world_use_item(0)==2&&inventory[0].item==0x59);assert(world_trigger(0,out)==2);
 assert(!world_use_item(0)); // already unlocked
 uint8_t saved[SCRIPT_STATE_SIZE+128];int n=world_save(saved);
 world_reset();assert(world_restore(saved,n));assert(world_enter((const uint8_t *)&r,sizeof r,1,0));assert(world_trigger(0,out)==2);
 for(int type=0xfe;type<=0xff;type++){
  world_reset();r.d.key_type=type;assert(world_enter((const uint8_t *)&r,sizeof r,1,0));
  world_player(500,0,0,0,0);assert(!world_use_item(0));assert(world_trigger(0,out)==3);world_close_message();
  assert(world_trigger(0,out)==(type==0xfe?2:3));world_close_message();
 }
 puts("Item use: healing amounts/cap/full/dead, non-healing items, reach/floor/facing, matching keys, reusable keys, one-way locks and persistence passed");
}

#pragma once
#include <stddef.h>
#include <stdint.h>
int world_enter(const uint8_t *data,size_t size,int stage,int room);
void world_reset(void);
void world_tick(void);
void world_player(int x,int y,int z,int direction,int camera);
int world_boxes(int *out);
int world_trigger(int slot,int *result); // 2 door, 3 message, 4 unsupported, 5 confirmation
int world_confirm(int yes,int *result); // 1 collected, 3 full inventory, 0 cancelled/stale
int world_objects(int *out); // up to 32 records: model,type,flags,x,y,z,rx,ry,rz
int world_triggers(int *out); // 32 records, 14 ints: slot,type,sat,floor,quad x/z,item,destination
const char *world_message(void);
void world_close_message(void);
int world_save(uint8_t *out);
int world_restore(const uint8_t *in,size_t size);

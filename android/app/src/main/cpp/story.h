#pragma once
#include "assets.h"
#define STORY_STATE_SIZE 143
void story_enter(int stage,int room);
int story_enabled(void);
int story_locked(void);
void story_player(int x,int y,int z,int dir,int camera);
void story_tick(void);
void story_state(int *out);
void story_position(int x,int y,int z);
void story_direction(int dir);
void story_cut(int camera);
int story_member(int member,int *value);
void story_member_set(int member,int value);
void story_plc(const uint8_t *ins);
void story_voice(int kind,int clip);
int story_voice_playing(void);
int story_voice_event(void);

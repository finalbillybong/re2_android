#pragma once
#include "enemy.h"
#define COMBAT_STATE_SIZE 113
void combat_reset(int hp);
void combat_room_enter(void);
void combat_input(int aim,int fire,int pressed,int turn,int posture,int noise);
void combat_tick(int x,int y,int z,int direction,int enabled);
void combat_target(EnemyTarget *target);
void combat_state(int *out);
// Returns 1 healed, -1 already healthy, 0 unavailable. Does not reset combat.
int combat_heal(int amount);

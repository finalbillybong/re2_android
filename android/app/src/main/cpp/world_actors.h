#pragma once
#include "assets.h"
void world_actors_enter(const Room *room,int stage,int id);
int world_actors_enabled(void);
void world_actors_tick(int x,int y,int z,int direction);
int world_actors_visible(int *out,int x,int y,int z,int camera);

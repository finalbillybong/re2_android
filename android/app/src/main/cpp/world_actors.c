/* Opening actors, room collision and occlusion; combat owns the player callbacks. */
#include <math.h>
#include <stdio.h>
#include "route.h"
#include <string.h>
#include "enemy.h"
#include "combat.h"
#include "effect.h"
#include "roomfx.h"
#include "combat_audio.h"
#include "world_actors.h"
static Room room;
static int enabled;
static Model sets[MAX_ENEMIES];
static void *zones;
int world_actors_enabled(void){return enabled;}
void world_actors_enter(const Room *current,int stage,int id){
    combat_room_enter();combat_audio_clear();effect_load();effect_clear();
    roomfx_load(opening_route(stage,id)?stage:0,opening_route(stage,id)?id:0);
    enemy_room_reset();enemy_set_zones(NULL);
    asset_free(zones);zones=NULL;
    for(int i=0;i<MAX_ENEMIES;i++){enemy_set_room_set(i,NULL);asset_free(sets[i].data);memset(&sets[i],0,sizeof sets[i]);}
    room=*current;enabled=opening_route(stage,id);
    if(!enabled)return;
    char path[80];
    snprintf(path,sizeof path,"nitro:/room/room%d%02x0_blk.bin",stage,id);
    zones=asset_load(path,NULL);enemy_set_zones(zones);
    for(int i=0;i<MAX_ENEMIES;i++){
        snprintf(path,sizeof path,"nitro:/room/room%d%02x0_rbj_e%d.bin",stage,id,i);
        if(model_load(&sets[i],path))enemy_set_room_set(i,&sets[i]);
    }
}
void world_actors_tick(int x,int y,int z,int direction){
    effect_tick();roomfx_tick();
    combat_tick(x,y,z,direction,enabled);
    if(!enabled)return;
    EnemyTarget target;combat_target(&target);
    enemy_tick(&target);
}
#define PLAYER_RADIUS 400
static int32_t penetration(const RoomBox *b, int32_t x, int32_t z)
{
    if (b->shape == 6 || b->shape == 7 || b->shape == 8) {
        float ra = (b->shape == 6 ? b->w : b->w) * 0.5f, rb = (b->shape == 6 ? b->w : b->d) * 0.5f;
        float dx = x - (b->x + ra), dz = z - (b->z + rb);
        ra += PLAYER_RADIUS;
        rb += PLAYER_RADIUS;
        float q = dx * dx / (ra * ra) + dz * dz / (rb * rb);
        if (q >= 1.0f)
            return 0;
        int32_t depth = (int32_t)((1.0f - sqrtf(q)) * (ra < rb ? ra : rb));
        return depth > 0 ? depth : 1;
    }
    int32_t in_x = x - (b->x - PLAYER_RADIUS), out_x = b->x + b->w + PLAYER_RADIUS - x;
    int32_t in_z = z - (b->z - PLAYER_RADIUS), out_z = b->z + b->d + PLAYER_RADIUS - z;
    if (in_x <= 0 || out_x <= 0 || in_z <= 0 || out_z <= 0)
        return 0;
    int32_t d = in_x < out_x ? in_x : out_x;
    if (in_z < d)
        d = in_z;
    if (out_z < d)
        d = out_z;
    return d;
}


int game_blocked(int32_t from_x,int32_t from_z,int32_t x,int32_t z,int32_t y){
    int floor=(-y+900)/1800;floor=floor<0?0:floor>31?31:floor;
    for(int i=0;i<room.header->num_boxes;i++){
        const RoomBox *b=&room.boxes[i];
        if(!(b->floors&(1u<<floor))||b->shape==SHAPE_SLOPE||b->shape==SHAPE_STAIRS||!(b->flag&BOX_ENEMY))continue;
        int depth=penetration(b,x,z);
        if(depth>0&&depth>=penetration(b,from_x,from_z))return 1;
    }
    return 0;
}
static int segment_hits_box(const float p0[3], const float p1[3], const float lo[3], const float hi[3])
{
    float t0 = 0.0f, t1 = 1.0f;
    for (int k = 0; k < 3; k++) {
        float d = p1[k] - p0[k];
        if (fabsf(d) < 1e-3f) {
            if (p0[k] < lo[k] || p0[k] > hi[k])
                return 0;
            continue;
        }
        float a = (lo[k] - p0[k]) / d, b = (hi[k] - p0[k]) / d;
        if (a > b) {
            float tmp = a;
            a = b;
            b = tmp;
        }
        if (a > t0)
            t0 = a;
        if (b < t1)
            t1 = b;
        if (t0 > t1)
            return 0;
    }
    return 1;
}

// Line of sight for enemies (0x8004d000): room boxes with flag 0x2000 block
// it, except shapes 1-5 and 13 (table 0x8009f480); the others count as their
// rectangle. A box reaches from its lowest floor up to the height in its type
// field, so low ones like cars do not hide Leon from a zombie's eyes.
// Does box i (with all bits of need in its flag) block the line p0-p1?
static int sight_box_hit_flags(int i, const float p0[3], const float p1[3], uint16_t need)
{
    static const uint8_t skip_shape[16] = { 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 };
    const RoomBox *b = &room.boxes[i];
    if (!(b->flag & 0x2000) || (b->flag & need) != need || skip_shape[b->flag & 15] || !b->floors)
        return 0;
    int32_t top = -((b->type >> 11) * 100 + ((b->type >> 6) & 31) * 1800);
    int32_t bottom = 0;
    for (uint32_t f = b->floors; !(f & 1); f >>= 1)
        bottom += FLOOR_HEIGHT;
    const float lo[3] = { (float)b->x, (float)top, (float)b->z };
    const float hi[3] = { (float)(b->x + b->w), (float)bottom, (float)(b->z + b->d) };
    return segment_hits_box(p0, p1, lo, hi);
}

static int sight_box_hit(int i, const float p0[3], const float p1[3])
{
    return sight_box_hit_flags(i, p0, p1, 0);
}

int game_sight_blocked(int32_t x0, int32_t y0, int32_t z0, int32_t x1, int32_t y1, int32_t z1)
{
    const float p0[3] = { (float)x0, (float)y0, (float)z0 }, p1[3] = { (float)x1, (float)y1, (float)z1 };
    for (int i = 0; i < room.header->num_boxes; i++)
        if (sight_box_hit(i, p0, p1))
            return 1;
    return 0;
}


void game_enemy_sound(int sound,int32_t x,int32_t z){
    EnemyTarget player;combat_target(&player);
    float dx=x-player.x,dz=z-player.z;int d=(int)sqrtf(dx*dx+dz*dz);
    combat_audio_play(combat_audio_enemy_bank(),sound,d>=20000?20:127-d*107/20000,64);
}
void game_player_voice(int sound){combat_audio_play(0,sound,127,64);}
void game_blood(int kind,int32_t x,int32_t y,int32_t z,int16_t dir,int speed,int32_t floor_y){
    effect_blood(kind,x,y,z,dir,speed,floor_y);
}

// Foreground masks and depth testing perform camera occlusion in the renderer.
// Collision boxes are not render geometry: filtering whole actors by Leon's
// position made Kendo and nearby zombies vanish as the player moved.
int world_actors_visible(int *out,int x,int y,int z,int camera){
    (void)x;(void)y;(void)z;
    if(!enabled||camera<0||camera>=room.header->num_cameras)return 0;
    return enemy_poses(out);
}

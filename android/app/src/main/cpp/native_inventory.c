#include <jni.h>
#include "inventory.h"

JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeInventory_reset(JNIEnv *env, jclass cls, jboolean test)
{
    (void)env; (void)cls;
    inventory_reset();
    // Same starting loadout as source/main.c:start_game.
    inventory_add(1, 0);
    inventory_add(2, test ? 10 : 18);
    inventory_equipped = 1;
    if (test) {
        inventory_add(0x14, 30); inventory_add(0x26, 0);
        inventory_add(0x27, 0); inventory_add(0x28, 0);
    }
}

JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeInventory_snapshot(JNIEnv *env, jclass cls)
{
    (void)cls;
    jint values[17];
    for (int i = 0; i < INV_SLOTS; i++) { values[2*i] = inventory[i].item; values[2*i+1] = inventory[i].count; }
    values[16] = inventory_equipped;
    jintArray result = (*env)->NewIntArray(env, 17);
    if (result) (*env)->SetIntArrayRegion(env, result, 0, 17, values);
    return result;
}

JNIEXPORT jint JNICALL Java_org_re2ds_thor_NativeInventory_equip(JNIEnv *env, jclass cls, jint slot)
{
    (void)env; (void)cls; return inventory_equip(slot);
}

JNIEXPORT jint JNICALL Java_org_re2ds_thor_NativeInventory_combine(JNIEnv *env, jclass cls, jint a, jint b)
{
    (void)env; (void)cls; return inventory_combine(a, b);
}

JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeInventory_restore(JNIEnv *env, jclass cls, jintArray saved)
{
    (void)cls;
    if (!saved || (*env)->GetArrayLength(env, saved) != 17) return;
    jint data[17]; (*env)->GetIntArrayRegion(env, saved, 0, 17, data);
    if (data[16] < -1 || data[16] >= INV_SLOTS) return;
    for (int i = 0; i < 16; i++) if (data[i] < 0 || data[i] > 255) return;
    for (int i = 0; i < INV_SLOTS; i++) { inventory[i].item = data[2*i]; inventory[i].count = data[2*i+1]; }
    inventory_equipped = data[16];
}

#include "world.h"
#include "script.h"
JNIEXPORT jboolean JNICALL Java_org_re2ds_thor_NativeWorld_enter(JNIEnv *e,jclass c,jbyteArray bytes,jint stage,jint room){
    (void)c;jsize n=(*e)->GetArrayLength(e,bytes);jbyte *p=(*e)->GetByteArrayElements(e,bytes,NULL);if(!p)return JNI_FALSE;
    int ok=world_enter((const uint8_t *)p,n,stage,room);(*e)->ReleaseByteArrayElements(e,bytes,p,JNI_ABORT);return ok?JNI_TRUE:JNI_FALSE;
}
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeWorld_reset(JNIEnv *e,jclass c){(void)e;(void)c;world_reset();}
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeWorld_tick(JNIEnv *e,jclass c,jint x,jint y,jint z,jint direction,jint camera){(void)e;(void)c;world_player(x,y,z,direction,camera);world_tick();}
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_triggers(JNIEnv *e,jclass c){
    (void)c;int data[NUM_AOTS*14];int count=world_triggers(data);jintArray a=(*e)->NewIntArray(e,count*14);if(a)(*e)->SetIntArrayRegion(e,a,0,count*14,data);return a;
}
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_trigger(JNIEnv *e,jclass c,jint slot){
    (void)c;jint data[8]={0};data[0]=world_trigger(slot,data+1);jintArray a=(*e)->NewIntArray(e,8);if(a)(*e)->SetIntArrayRegion(e,a,0,8,data);return a;
}
JNIEXPORT jstring JNICALL Java_org_re2ds_thor_NativeWorld_message(JNIEnv *e,jclass c){(void)c;return (*e)->NewStringUTF(e,world_message());}
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeWorld_closeMessage(JNIEnv *e,jclass c){(void)e;(void)c;world_close_message();}
JNIEXPORT jbyteArray JNICALL Java_org_re2ds_thor_NativeWorld_save(JNIEnv *e,jclass c){
    (void)c;uint8_t data[SCRIPT_STATE_SIZE+128];int n=world_save(data);jbyteArray a=(*e)->NewByteArray(e,n);if(a)(*e)->SetByteArrayRegion(e,a,0,n,(const jbyte *)data);return a;
}
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeWorld_restore(JNIEnv *e,jclass c,jbyteArray bytes){
    (void)c;jsize n=(*e)->GetArrayLength(e,bytes);jbyte *p=(*e)->GetByteArrayElements(e,bytes,NULL);if(!p)return;world_restore((const uint8_t *)p,n);(*e)->ReleaseByteArrayElements(e,bytes,p,JNI_ABORT);
}

JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_boxes(JNIEnv *e,jclass c){
 (void)c;int data[65536];int count=world_boxes(data);jintArray a=(*e)->NewIntArray(e,count);if(a)(*e)->SetIntArrayRegion(e,a,0,count,data);return a;
}

JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_objects(JNIEnv *e,jclass c){
 (void)c;jint data[32*9];int n=world_objects(data);jintArray a=(*e)->NewIntArray(e,n*9);if(a)(*e)->SetIntArrayRegion(e,a,0,n*9,data);return a;
}
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_confirm(JNIEnv *e,jclass c,jboolean yes){
 (void)c;jint data[8]={0};data[0]=world_confirm(yes,data+1);jintArray a=(*e)->NewIntArray(e,8);if(a)(*e)->SetIntArrayRegion(e,a,0,8,data);return a;
}

#include "enemy.h"
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_actors(JNIEnv *e,jclass c){
    (void)c;int data[MAX_ENEMIES*ENEMY_POSE_SIZE];int size=world_actors(data)*ENEMY_POSE_SIZE;
    jintArray a=(*e)->NewIntArray(e,size);if(a)(*e)->SetIntArrayRegion(e,a,0,size,data);return a;
}

#include "combat.h"
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeCombat_input(JNIEnv *e,jclass c,jboolean aim,jboolean fire,jboolean pressed,jint turn,jint posture,jint noise){
 (void)e;(void)c;combat_input(aim,fire,pressed,turn,posture,noise);
}
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeCombat_snapshot(JNIEnv *e,jclass c){
 (void)c;int data[COMBAT_STATE_SIZE];combat_state(data);jintArray a=(*e)->NewIntArray(e,COMBAT_STATE_SIZE);
 if(a)(*e)->SetIntArrayRegion(e,a,0,COMBAT_STATE_SIZE,data);return a;
}
JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeCombat_restore(JNIEnv *e,jclass c,jint hp){(void)e;(void)c;combat_reset(hp);}
JNIEXPORT jboolean JNICALL Java_org_re2ds_thor_NativeCombat_blocked(JNIEnv *e,jclass c,jint ox,jint oz,jint x,jint z){
 (void)e;(void)c;return enemy_blocks_player(ox,oz,x,z)?JNI_TRUE:JNI_FALSE;
}

#include "effect_snapshot.h"
#include "combat_audio.h"
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeEffects_snapshot(JNIEnv *e,jclass c){
 (void)c;int data[FX_MAX_QUADS*FX_RECORD_SIZE];int n=effect_snapshot(data,FX_MAX_QUADS);
 n+=roomfx_snapshot(data+n*FX_RECORD_SIZE,FX_MAX_QUADS-n);
 jintArray a=(*e)->NewIntArray(e,n*FX_RECORD_SIZE);if(a)(*e)->SetIntArrayRegion(e,a,0,n*FX_RECORD_SIZE,data);return a;
}
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeEffects_sounds(JNIEnv *e,jclass c){
 (void)c;int data[COMBAT_AUDIO_MAX*4];int n=combat_audio_drain(data);
 jintArray a=(*e)->NewIntArray(e,n*4);if(a)(*e)->SetIntArrayRegion(e,a,0,n*4,data);return a;
}
JNIEXPORT jint JNICALL Java_org_re2ds_thor_NativeEffects_enemyBank(JNIEnv *e,jclass c){(void)e;(void)c;return combat_audio_enemy_bank();}

#include "story.h"
JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_story(JNIEnv *e,jclass c){
 (void)c;int data[STORY_STATE_SIZE];story_state(data);jintArray a=(*e)->NewIntArray(e,STORY_STATE_SIZE);
 if(a)(*e)->SetIntArrayRegion(e,a,0,STORY_STATE_SIZE,data);return a;
}
JNIEXPORT jint JNICALL Java_org_re2ds_thor_NativeWorld_voice(JNIEnv *e,jclass c){(void)e;(void)c;return story_voice_event();}

JNIEXPORT void JNICALL Java_org_re2ds_thor_NativeWorld_footstep(JNIEnv *e,jclass c,jint x,jint y,jint z,jboolean run){(void)e;(void)c;world_footstep(x,y,z,run);}

JNIEXPORT jint JNICALL Java_org_re2ds_thor_NativeWorld_useItem(JNIEnv *e,jclass c,jint slot,jint x,jint y,jint z,jint direction,jint camera){
    (void)e;(void)c;world_player(x,y,z,direction,camera);return world_use_item(slot);
}

JNIEXPORT jintArray JNICALL Java_org_re2ds_thor_NativeWorld_cameraZones(JNIEnv *e,jclass c){
    (void)c;int data[256],n=world_camera_zones(data);jintArray a=(*e)->NewIntArray(e,n);
    if(a)(*e)->SetIntArrayRegion(e,a,0,n,data);return a;
}

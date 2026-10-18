#include <string.h>
#include "assets.h"
#include "enemy.h"
#include "combat_audio.h"
static int events[COMBAT_AUDIO_MAX*4],count;
static unsigned char *pairs;
void combat_audio_clear(void){count=0;}
void combat_audio_play(int bank,int note,int volume,int pan){
    if(bank<0||count>=COMBAT_AUDIO_MAX)return;
    int *p=events+4*count++;p[0]=bank;p[1]=note;p[2]=volume;p[3]=pan;
}
int combat_audio_drain(int *out){int n=count;memcpy(out,events,n*4*sizeof(int));count=0;return n;}
int combat_audio_enemy_bank(void){
    if(!pairs){
        uint32_t size;pairs=asset_load("nitro:/sound/enem_pairs.bin",&size);
        if(pairs&&(size<1||size<1u+2u*pairs[0])){asset_free(pairs);pairs=NULL;}
    }
    int bank=enemy_sound_set(pairs);
    return bank<0?-1:100+bank;
}

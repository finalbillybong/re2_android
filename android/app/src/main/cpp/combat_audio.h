#pragma once
#define COMBAT_AUDIO_MAX 64
void combat_audio_clear(void);
void combat_audio_play(int bank,int note,int volume,int pan);
int combat_audio_drain(int *out); // bank,note,volume,pan records
int combat_audio_enemy_bank(void);

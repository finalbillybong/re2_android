#pragma once
// Android billboard record: bank (0 RES1 blood, 1 RRE1 core), effect id, palette,
// world xyz, local left/top/right/bottom, UV rect in texels, RGBA (0..255), depth bias.
#define FX_RECORD_SIZE 19
#define FX_MAX_QUADS 512
int effect_snapshot(int *out,int capacity);
int roomfx_snapshot(int *out,int capacity);

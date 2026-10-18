// RE2/DS turns: 32768 units per revolution; trigonometry in 20.12 fixed point.
#pragma once
#include <math.h>
static inline int cosLerp(int a) { return (int)(4096 * cos(a * 6.283185307179586 / 32768)); }
static inline int sinLerp(int a) { return (int)(4096 * sin(a * 6.283185307179586 / 32768)); }

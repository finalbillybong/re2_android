// Shared DS/Android inventory state and rules. No platform dependencies.
#pragma once
#include <stdint.h>

// Leon's inventory: 8 slots, item 0 = empty; count = quantity, or the
// rounds in a weapon (0 = empty gun, still there)
#define INV_SLOTS 8
typedef struct {
    uint8_t item, count;
} InvSlot;
extern InvSlot inventory[INV_SLOTS];
extern int inventory_equipped;      // slot of the weapon in hand, -1 = none
int inventory_has(int item);
int inventory_add(int item, int amount); // 0 if there is no room
void inventory_reset(void);

int inventory_spare_ammo(void);
void inventory_reload(void);
// Returns 1 loaded, 2 mixed, -1 full weapon, 0 invalid combination.
int inventory_combine(int a, int b);
int inventory_equip(int slot);

// Extracted from script.c and menu.c; shared by DS and Android.
#include <string.h>
#include "inventory.h"

InvSlot inventory[INV_SLOTS];
int inventory_equipped = -1;

int inventory_has(int item)
{
    for (int i = 0; i < INV_SLOTS; i++)
        if (inventory[i].item && inventory[i].item == item)
            return 1;
    return 0;
}

// Ammunition (ids 0x14-0x1D) and ink ribbons stack; everything else takes a slot.
static int stacks(int item) { return item >= 0x14 && item <= 0x1E; }

int inventory_add(int item, int amount)
{
    if (stacks(item))
        for (int i = 0; i < INV_SLOTS; i++)
            if (inventory[i].item == item) {
                inventory[i].count += amount;
                return 1;
            }
    for (int i = 0; i < INV_SLOTS; i++)
        if (!inventory[i].item) {
            inventory[i].item = item;
            inventory[i].count = amount;
            return 1;
        }
    return 0; // full
}

void inventory_reset(void)
{
    memset(inventory, 0, sizeof(inventory));
    inventory_equipped = -1;
}

#define ITEM_GREEN 0x26
#define ITEM_RED 0x27
#define ITEM_BLUE 0x28
static const struct { uint8_t weapon, ammo, capacity; } loads[] = {
    { 0x02, 0x14, 18 }, { 0x03, 0x14, 13 }, { 0x04, 0x14, 18 },   // hand guns
    { 0x05, 0x16, 8 }, { 0x06, 0x16, 8 },                         // magnums
    { 0x07, 0x15, 5 }, { 0x08, 0x15, 7 },                         // shotguns
    { 0x09, 0x18, 6 }, { 0x0A, 0x19, 6 }, { 0x0B, 0x1A, 6 },      // grenade launcher
    { 0x0C, 0x1D, 18 }, { 0x0E, 0x1C, 100 }, { 0x0F, 0x1B, 100 }, { 0x10, 0x17, 100 },
};
// mixed herbs 0x29.. : G+G, G+R, G+B, G+G+G, G+G+B, G+R+B
static const uint8_t mixes[][3] = {
    { ITEM_GREEN, ITEM_GREEN, 0x29 }, { ITEM_GREEN, ITEM_RED, 0x2A }, { ITEM_GREEN, ITEM_BLUE, 0x2B },
    { 0x29, ITEM_GREEN, 0x2C }, { 0x29, ITEM_BLUE, 0x2D }, { 0x2B, ITEM_GREEN, 0x2D },
    { 0x2A, ITEM_BLUE, 0x2E }, { 0x2B, ITEM_RED, 0x2E },
};

// the ammunition slot for the weapon in hand, -1 if none (FUN_80065708)
static int spare_ammo(int *capacity)
{
    if (inventory_equipped < 0)
        return -1;
    int weapon = inventory[inventory_equipped].item;
    for (unsigned i = 0; i < sizeof(loads) / sizeof(loads[0]); i++) {
        if (loads[i].weapon != weapon)
            continue;
        for (int s = 0; s < INV_SLOTS; s++)
            if (inventory[s].item == loads[i].ammo && inventory[s].count) {
                *capacity = loads[i].capacity;
                return s;
            }
    }
    return -1;
}

int inventory_spare_ammo(void)
{
    int capacity;
    return spare_ammo(&capacity) >= 0;
}

// FUN_80065780: fills the weapon in hand from its ammunition
void inventory_reload(void)
{
    int capacity, s = spare_ammo(&capacity);
    if (s < 0)
        return;
    InvSlot *w = &inventory[inventory_equipped], *m = &inventory[s];
    int take = capacity - w->count;
    if (take > m->count)
        take = m->count;
    if (take <= 0)
        return;
    w->count += take;
    m->count -= take;
    if (!m->count)
        m->item = 0;
}

int inventory_combine(int a, int b)
{
    if (a < 0 || a >= INV_SLOTS || b < 0 || b >= INV_SLOTS || a == b) return 0;
    InvSlot *x = &inventory[a], *y = &inventory[b];
    if (!x->item || !y->item) return 0;
    for (unsigned i = 0; i < sizeof(loads) / sizeof(loads[0]); i++) {
        InvSlot *w = x->item == loads[i].weapon ? x : y->item == loads[i].weapon ? y : NULL;
        InvSlot *ammo = w == x ? y : x;
        if (!w || ammo->item != loads[i].ammo) continue;
        int take = loads[i].capacity - w->count;
        if (take <= 0) return -1;
        if (take > ammo->count) take = ammo->count;
        if (!take) return 0;
        w->count += take; ammo->count -= take;
        if (!ammo->count) ammo->item = 0;
        return 1;
    }
    for (unsigned i = 0; i < sizeof(mixes) / sizeof(mixes[0]); i++) {
        if (!((x->item == mixes[i][0] && y->item == mixes[i][1]) ||
              (y->item == mixes[i][0] && x->item == mixes[i][1]))) continue;
        x->item = mixes[i][2]; x->count = 0;
        y->item = y->count = 0;
        if (inventory_equipped == b) inventory_equipped = -1;
        return 2;
    }
    return 0;
}

int inventory_equip(int slot)
{
    if (slot < 0 || slot >= INV_SLOTS || inventory[slot].item < 1 || inventory[slot].item > 0x13) return 0;
    inventory_equipped = inventory_equipped == slot ? -1 : slot;
    return 1;
}

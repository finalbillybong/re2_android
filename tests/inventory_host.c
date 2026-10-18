// cc -Wall -Wextra -Werror -fsanitize=undefined -Isource tests/inventory_host.c source/inventory.c -o build/inventory_host
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include "inventory.h"
int main(void) {
    inventory_reset();
    assert(inventory_equipped == -1);
    assert(inventory_add(2,10));assert(inventory_add(0x14,30));
    assert(inventory_equip(0));assert(inventory_equipped==0);
    assert(inventory_combine(1,0)==1);
    assert(inventory[0].count==18&&inventory[1].count==22);
    assert(inventory_combine(0,1)==-1);
    assert(inventory_combine(-1,8)==0);
    InvSlot before[8];memcpy(before,inventory,sizeof before);
    assert(inventory_combine(0,0)==0);assert(!memcmp(before,inventory,sizeof before));
    inventory[0].count=0;inventory[1].count=3;inventory_reload();
    assert(inventory[0].count==3&&inventory[1].item==0&&!inventory_spare_ammo());
    assert(inventory_add(0x27,0));assert(inventory_add(0x26,0));
    assert(!inventory_equip(1));assert(inventory_combine(1,2)==2);
    assert(inventory[1].item==0x2a&&inventory[2].item==0);
    assert(inventory_add(0x28,0));assert(inventory_combine(2,1)==2);
    assert(inventory[2].item==0x2e&&inventory[1].item==0&&inventory_equipped==0);
    for(int i=0;i<6;i++)assert(inventory_add(1,0));
    assert(!inventory_add(1,0));
    inventory_reset();assert(!inventory_has(2));assert(inventory_equipped==-1);
    puts("Inventory: reload, capacity, consumption, herb mixing, invalid operations, full slots passed");
}

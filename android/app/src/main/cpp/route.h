#pragma once
// Leon A opening route, ending at the RPD courtyard (room 103).
static inline int opening_route(int stage,int room){
    return stage==1&&(room<=3||(room>=0x18&&room<=0x1b));
}

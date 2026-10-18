package org.re2ds.thor;
/** Immutable native combat result; also carries Leon's original zombie-victim pose. */
final class CombatState {
    final int hp,x,y,z,direction,animation,key,shots,hit;
    final boolean locked,grabbed,dead,finished;
    final short[] frame;
    CombatState(int[] p){
        RoomData.require(p.length==113,"Invalid combat snapshot");
        hp=p[0];locked=(p[1]&1)!=0;grabbed=(p[1]&2)!=0;dead=(p[1]&4)!=0;finished=(p[1]&8)!=0;
        x=p[2];y=p[3];z=p[4];direction=p[5];animation=p[6];key=p[7];shots=p[9];hit=p[10];
        frame=(p[1]&16)==0?null:new short[102];
        if(frame!=null)for(int i=0;i<frame.length;i++)frame[i]=(short)p[11+i];
    }
    void apply(RoomSession session){
        session.combatFrame=frame;
        if(locked){
            session.x=x;session.y=y;session.z=z;session.direction=direction;
            if(frame==null&&!dead){session.animation=animation;session.key=key;}
            session.switchCamera();
        }
    }
}

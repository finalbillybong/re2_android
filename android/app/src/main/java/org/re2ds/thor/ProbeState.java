package org.re2ds.thor;

/** Shared inventory focus and controller input for both displays. */
final class ProbeState {
    final PlayerHealth health=new PlayerHealth();
    int selected, touches, actions, resumes;
    boolean paused, swapped;
    boolean aimBumper,aimButton,aimTrigger,fire,pressed;
    boolean aiming(){return aimBumper||aimButton||aimTrigger;}
    float axisX, axisY;
    String lastInput = "Waiting for touch or controller input";

    void select(int slot) {
        selected = Math.floorMod(slot, 8);
        paused = true;
        touches++;
        lastInput = "Touch: slot " + (selected + 1);
    }

    void releaseInput() { axisX = axisY = 0;aimBumper=aimButton=aimTrigger=fire=pressed=false; }
}

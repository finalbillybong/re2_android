package org.re2ds.thor;

/** Shared inventory focus and controller input for both displays. */
final class ProbeState {
    int selected, touches, actions, resumes;
    boolean paused, swapped;
    float axisX, axisY;
    String lastInput = "Waiting for touch or controller input";

    void select(int slot) {
        selected = Math.floorMod(slot, 8);
        paused = true;
        touches++;
        lastInput = "Touch: slot " + (selected + 1);
    }

    void releaseInput() { axisX = axisY = 0; }
}

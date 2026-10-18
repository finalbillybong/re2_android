package org.re2ds.thor;
final class NativeCombat {
    static {System.loadLibrary("re2thor");}
    static native void input(boolean aim,boolean fire,boolean pressed,int turn,int posture,int noise);
    static native int[] snapshot();
    static native void restore(int hp);
    static native boolean blocked(int oldX,int oldZ,int x,int z);
}

package org.re2ds.thor;
final class NativeEffects {
    static {System.loadLibrary("re2thor");}
    static native int[] snapshot();
    static native int[] sounds();
    static native int enemyBank();
}

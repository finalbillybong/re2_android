package org.re2ds.thor;

/** Called on the main/UI thread only; both displays use the same C inventory. */
final class NativeInventory {
    static { System.loadLibrary("re2thor"); }
    static native void reset(boolean testSupplies);
    static native int[] snapshot();
    static native int equip(int slot);
    static native int combine(int first, int second);
    static native void restore(int[] state);
}

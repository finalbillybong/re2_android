package org.re2ds.thor;
/** Main-thread bridge to room scripts and exploration interactions. */
final class NativeWorld {
 static {System.loadLibrary("re2thor");}
 static native void configure(android.content.res.AssetManager assets);
 static native int[] actors();
 static native int[] story();
 static native int voice();
 static native boolean enter(byte[] room,int stage,int id);
 static native void reset();
 static native void footstep(int x,int y,int z,boolean run);
 static native void tick(int x,int y,int z,int direction,int camera);
 static native int[] boxes();
 static native int[] cameraZones();
 static native int[] objects();
 static native int[] confirm(boolean yes);
 static native int[] triggers();
 static native int[] trigger(int slot);
 static native int useItem(int slot,int x,int y,int z,int direction,int camera);
 static native String message();
 static native void closeMessage();
 static native byte[] save();
 static native void restore(byte[] state);
}

package org.re2ds.thor;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.app.AlertDialog;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity implements DisplayManager.DisplayListener {
    private static final int BG = Color.BLACK;
    private final ProbeState state = new ProbeState();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ProbePanel> panels = new ArrayList<>();
    private RoomData room;
    private RoomCatalog rooms;
    private RoomActors actorAssets;
    private RoomActors.Actor[] worldActors=RoomActors.EMPTY;
    private EffectsData effectData;
    private CombatAudio combatAudio;
    private StoryVoice storyVoice;
    private StoryState story;
    private Bundle roomCheckpoint;
    private int[] worldEffects=new int[0];
    private int[] triggers=new int[0];
    private float scriptTime;
    private boolean worldMessage;
    private int pickupItem;
    private boolean pickupYes=true;
    private int[] worldObjects=new int[0];
    private String actionHint="Explore the street; A interacts with nearby doors and items";
    private RoomSession session;
    private CombatState combat;
    private int combineFrom = -1;
    private final InventoryActions itemActions=new InventoryActions();
    private boolean running;
    private boolean pauseMenu;
    private boolean quitPrompt,quitYes,quitWasPaused;
    private boolean doorAnimations=true;
    private DoorAnimation.Catalog doors;
    private DoorAnimation activeDoor;
    private float doorTime;
    private int pausePage,pauseSelection,roomPage;
    private static final String[] PAUSE_ITEMS={"Resume","Inventory","Interact","Explore rooms","Swap screens","Test supplies","Reset exploration","Door animations: On","Quit game"};
    private static final String[] ROOM_ITEMS={"Opening street","Gun shop","Alley","All rooms","Back"};
    private String message = "Select an item to pause and manage inventory";
    private DisplayManager manager;
    private ProbePresentation secondary;
    private int chosenDisplay = -1;
    private int hatX, hatY;
    private boolean resumed;
    private long previousFrame, previousText;
    private String routing = "Looking for a second display";

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            long now = SystemClock.uptimeMillis();
            boolean displaysAwake=screensOn();
            if(combatAudio!=null)combatAudio.setMenuEnabled(displaysAwake);
            if(combatAudio!=null)combatAudio.setRunning(displaysAwake&&!state.paused);
            if(storyVoice!=null)storyVoice.setRunning(displaysAwake&&!state.paused);
            if (displaysAwake && session != null) {
                int[] inventory = NativeInventory.snapshot();
                int equipped = inventory[16] < 0 ? 0 : inventory[inventory[16]*2];
                float seconds=(now-previousFrame)/1000f;
                if(activeDoor!=null){
                    doorTime+=Math.min(.05f,Math.max(0,seconds));
                    if(doorTime*activeDoor.fps>=activeDoor.frames){activeDoor=null;resumeGameplay();}
                }
                if(!state.paused) {
                    scriptTime+=Math.min(.05f,Math.max(0,seconds));
                    while(scriptTime>=1f/30){
                        scriptTime-=1f/30;
                        boolean locked=(story!=null&&story.locked)||state.aiming()||(combat!=null&&combat.locked);
                        float ox=session.x,oz=session.z;
                        session.step(1f/30,state.axisX,state.axisY,running,locked,equipped);
                        if(NativeCombat.blocked((int)ox,(int)oz,(int)session.x,(int)session.z)){
                            session.x=ox;session.z=oz;session.switchCamera();
                        }
                        int noise=session.x!=ox||session.z!=oz?(running?4:2):0;
                        NativeCombat.input((story==null||!story.locked)&&state.aiming(),(story==null||!story.locked)&&state.fire,state.pressed,
                            state.axisX>.15f?1:state.axisX<-.15f?-1:0,
                            state.axisY>.15f?1:state.axisY<-.15f?-1:0,noise);
                        state.pressed=false;
                        NativeWorld.tick((int)session.x,(int)session.y,(int)session.z,(int)session.direction,session.camera);
                        if(session.footstep!=0)NativeWorld.footstep((int)session.x,(int)session.y,(int)session.z,session.footstep==2);
                        try{combatAudio.play(NativeEffects.sounds());}catch(java.io.IOException ex){roomLoadFailed(ex);break;}
                        combat=new CombatState(NativeCombat.snapshot());combat.apply(session);state.health.restore(combat.hp);
                        int[] zones=NativeWorld.cameraZones();for(int i=0;i<zones.length;i++)room.zones[i].role=zones[i];
                        story=new StoryState(NativeWorld.story());story.apply(session);
                        try{storyVoice.play(NativeWorld.voice());}catch(java.io.IOException ex){roomLoadFailed(ex);break;}
                        if(combat.finished){openPauseMenu();break;}
                    }
                    String text=NativeWorld.message();
                    if(!text.isEmpty()){message=text;worldMessage=true;state.paused=true;state.releaseInput();}
                } else scriptTime=0;
                triggers=NativeWorld.triggers();worldObjects=NativeWorld.objects();worldEffects=NativeEffects.snapshot();updateActors();updateActionHint();
                int[] boxes=NativeWorld.boxes();for(int i=0;i<boxes.length;i++)room.boxes[i].flags=boxes[i];
            }
            previousFrame = now;
            for (ProbePanel panel : panels) {
                if(displaysAwake&&panel.inventoryView!=null)panel.inventoryView.animateHeartbeat(now);
                if (panel.scene != null) panel.scene.update();
                if (now - previousText >= 150) panel.refresh();
            }
            if (now - previousText >= 150) previousText = now;
            getWindow().getDecorView().postOnAnimation(this);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        manager = getSystemService(DisplayManager.class);
        doorAnimations=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("doorAnimations",true);
        if (saved == null) saved = getIntent().getBundleExtra("probeState");
        if (saved != null) {
            state.selected = saved.getInt("slot"); state.touches = saved.getInt("touches");
            state.actions = saved.getInt("actions"); state.resumes = saved.getInt("resumes");
            state.paused = saved.getBoolean("paused"); state.swapped = saved.getBoolean("swapped");
            chosenDisplay = saved.getInt("display", -1);
            state.health.restore(saved.getInt("hp",PlayerHealth.MAX_HP));
            quitPrompt=saved.getBoolean("quitPrompt");quitWasPaused=saved.getBoolean("quitWasPaused");
            quitYes=false; // Recreated confirmations always default to keeping the app open.
        }
        try {
            RoomData.Reader reader = path -> {
                try (InputStream in = getAssets().open("re2/" + path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
                    return out.toByteArray();
                }
            };
            NativeWorld.configure(getAssets());
            actorAssets=new RoomActors(reader);
            effectData=new EffectsData(reader);
            combatAudio=new CombatAudio(getAssets(),reader);
            storyVoice=new StoryVoice(getAssets());
            doors=new DoorAnimation.Catalog(reader);
            rooms=new RoomCatalog(reader);
            int stage=saved==null?1:saved.getInt("stage",1),id=saved==null?0:saved.getInt("roomId",0);
            room=rooms.load(stage,id);effectData=effectData.forRoom(stage,id);
            NativeInventory.reset(false);
            if(saved!=null&&saved.getIntArray("inventory")!=null)NativeInventory.restore(saved.getIntArray("inventory"));
            NativeWorld.reset();
            if(saved!=null&&saved.getByteArray("worldState")!=null)NativeWorld.restore(saved.getByteArray("worldState"));
            roomCheckpoint=saved==null?checkpoint(room.stage,room.roomId,19800,0,300,16384,0):saved.getBundle("roomCheckpoint");
            if(roomCheckpoint==null)roomCheckpoint=checkpoint(room.stage,room.roomId,saved.getFloat("playerX",19800),saved.getFloat("playerY"),saved.getFloat("playerZ",300),saved.getFloat("direction",16384),saved.getInt("camera"));
            if(!NativeWorld.enter(room.raw,room.stage,room.roomId))throw new IllegalStateException("Room script load failed");
            combatAudio.room(NativeEffects.enemyBank(),1000+(room.stage-1)*256+room.roomId);
            NativeCombat.restore(state.health.hp());
            combat=new CombatState(NativeCombat.snapshot());
            session = new RoomSession(room);
            if (saved != null) {
                int[] inventory=saved.getIntArray("inventory");
                if(inventory!=null) NativeInventory.restore(inventory);
                session.y=saved.getFloat("playerY",0);session.x=saved.getFloat("playerX",19800);session.z=saved.getFloat("playerZ",300);
                session.direction=saved.getFloat("direction",16384);session.camera=saved.getInt("camera",0);
            }
            if(combat.dead){state.paused=true;pauseMenu=true;}
        } catch (Exception ex) {
            Log.e("Re2Thor","Could not load room assets",ex);
            new AlertDialog.Builder(this).setTitle("Room could not load").setMessage(ex.toString()).setPositiveButton("Close",(d,w)->finish()).show();
            return;
        }
        configureWindow(getWindow());
        moveToPrimaryDisplay();
    }

    @Override protected void onStart() {
        super.onStart();
        if (isFinishing() || session == null || moveToPrimaryDisplay()) return;
        manager.registerDisplayListener(this, handler);
        rebuild();
    }

    @Override protected void onResume() {
        super.onResume();
        if (isFinishing() || session == null || moveToPrimaryDisplay()) return;
        resumed = true; for(ProbePanel p:panels) if(p.scene!=null)p.scene.onResume(); state.resumes++; state.releaseInput(); hatX = hatY = 0;
        previousFrame = SystemClock.uptimeMillis();
        getWindow().getDecorView().removeCallbacks(frame);
        getWindow().getDecorView().postOnAnimation(frame);
    }

    @Override protected void onPause() {
        if(combatAudio!=null)combatAudio.setMenuEnabled(false);
        if(combatAudio!=null)combatAudio.setRunning(false);
        if(storyVoice!=null)storyVoice.setRunning(false);
        resumed = false; state.releaseInput(); running=false;
        for(ProbePanel p:panels) if(p.scene!=null)p.scene.onPause();
        getWindow().getDecorView().removeCallbacks(frame);
        super.onPause();
    }

    @Override protected void onDestroy(){if(combatAudio!=null)combatAudio.close();if(storyVoice!=null)storyVoice.close();super.onDestroy();}

    @Override protected void onStop() {
        manager.unregisterDisplayListener(this);
        dismissSecondary(); panels.clear();
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        saveProbeState(out);
    }

    private Bundle checkpoint(int stage,int id,float x,float y,float z,float direction,int camera){
        Bundle b=new Bundle();b.putInt("stage",stage);b.putInt("roomId",id);b.putByteArray("worldState",NativeWorld.save());
        b.putIntArray("inventory",NativeInventory.snapshot());b.putInt("hp",state.health.hp());
        b.putFloat("playerX",x);b.putFloat("playerY",y);b.putFloat("playerZ",z);b.putFloat("direction",direction);b.putInt("camera",camera);return b;
    }
    private void saveProbeState(Bundle out) {
        out.putInt("slot", state.selected); out.putInt("touches", state.touches);
        out.putInt("actions", state.actions); out.putInt("resumes", state.resumes);
        out.putBoolean("paused", activeDoor==null&&state.paused); out.putBoolean("swapped", state.swapped);
        out.putInt("display", chosenDisplay);
        out.putInt("hp",state.health.hp());out.putBoolean("quitPrompt",quitPrompt);out.putBoolean("quitWasPaused",quitWasPaused);
        if(session!=null) {
            out.putIntArray("inventory",NativeInventory.snapshot());
            out.putInt("stage",room.stage);out.putInt("roomId",room.roomId);out.putByteArray("worldState",NativeWorld.save());
            out.putFloat("playerY",session.y);out.putFloat("playerX",session.x);out.putFloat("playerZ",session.z);
            out.putFloat("direction",session.direction);out.putInt("camera",session.camera);
            out.putBundle("roomCheckpoint",roomCheckpoint);
            if(story!=null&&story.locked&&roomCheckpoint!=null){out.putAll(roomCheckpoint);out.putBoolean("paused",true);}
        }
    }

    private boolean moveToPrimaryDisplay() {
        // Presentation cannot target the default display. The Thor's lower
        // launcher can place this activity on display 4, including on reopen.
        // Keep the activity on the default display and present on the other one.
        if (hostId() == Display.DEFAULT_DISPLAY) {
            getIntent().removeExtra("routingRetry");
            return false;
        }
        if (getIntent().getBooleanExtra("routingRetry", false)
                || manager.getDisplay(Display.DEFAULT_DISPLAY) == null) return false;
        Bundle snapshot = new Bundle();
        saveProbeState(snapshot);
        Intent redirect = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                .putExtra("probeState", snapshot).putExtra("routingRetry", true);
        try {
            Log.i("Re2Thor", "Moving host from display " + hostId() + " to default display");
            startActivity(redirect, ActivityOptions.makeBasic()
                    .setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle());
            finishAndRemoveTask();
            return true;
        } catch (SecurityException ex) {
            // An OEM may prohibit activity placement. Keep a usable fallback.
            getIntent().putExtra("routingRetry", true);
            Log.w("Re2Thor", "Primary-display launch denied", ex);
            return false;
        }
    }

    private void configureWindow(Window window) {
        if (window == null) return;
        window.setStatusBarColor(BG); window.setNavigationBarColor(BG);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private int hostId() { return getWindowManager().getDefaultDisplay().getDisplayId(); }

    private List<Display> candidates() {
        List<Display> result = new ArrayList<>();
        // Some built-in secondary screens do not advertise CATEGORY_PRESENTATION.
        for (Display d : manager.getDisplays())
            if (d.getDisplayId() != hostId() && d.isValid()) result.add(d);
        return result;
    }

    private void dismissSecondary() {
        ProbePresentation old = secondary;
        secondary = null;
        if (old != null) old.dismiss();
    }

    private void rebuild() {
        if(session==null)return;
        for(ProbePanel p:panels)if(p.scene!=null)p.scene.onPause();
        state.releaseInput(); running=false; hatX = hatY = 0; dismissSecondary(); panels.clear();
        List<Display> choices = candidates();
        // Prefer the user's selection, but recover when its display disappears.
        choices.sort((a, b) -> Integer.compare(a.getDisplayId() == chosenDisplay ? 0 : 1,
                                               b.getDisplayId() == chosenDisplay ? 0 : 1));
        String failure = "";
        for (Display d : choices) {
            ProbePresentation attempt = new ProbePresentation(this, d);
            try {
                attempt.show(); secondary = attempt; chosenDisplay = d.getDisplayId();
                attempt.setOnDismissListener(dialog -> {
                    if (resumed && secondary == attempt) handler.post(() -> {
                        if (resumed && secondary == attempt) rebuild();
                    });
                });
                routing = "Displays " + hostId() + " + " + chosenDisplay + " connected";
                break;
            } catch (WindowManager.InvalidDisplayException | SecurityException ex) {
                attempt.dismiss(); panels.clear();
                failure = " · Display " + d.getDisplayId() + " unavailable";
            }
        }
        if (secondary == null) routing = "Single-display preview" + failure;
        ProbePanel main = new ProbePanel(this, hostId(), secondary != null && state.swapped);
        if (secondary == null) {
            LinearLayout split = new LinearLayout(this);
            split.setOrientation(LinearLayout.VERTICAL);
            split.addView(main, new LinearLayout.LayoutParams(-1, 0, 1));
            split.addView(new ProbePanel(this, hostId(), true), new LinearLayout.LayoutParams(-1, 0, 1.3f));
            setContentView(split);
        } else setContentView(main);
        if(resumed)for(ProbePanel p:panels)if(p.scene!=null)p.scene.onResume();
        refresh();
    }

    private boolean screensOn() {
        Display host = manager.getDisplay(hostId());
        Display remote = secondary == null ? null : secondary.getDisplay();
        return host != null && host.getState() == Display.STATE_ON
                && (remote == null || remote.getState() == Display.STATE_ON);
    }

    private void refresh() {
        for (ProbePanel p : panels) p.refresh();
        Log.i("Re2Thor", routing + " slot=" + (state.selected + 1) + " paused=" + state.paused
                + " touches=" + state.touches + " actions=" + state.actions + " input=" + state.lastInput);
    }
    private void cycleDisplay() {
        List<Display> choices = candidates();
        for (int i = 0; i < choices.size(); i++)
            if (choices.get(i).getDisplayId() == chosenDisplay) {
                chosenDisplay = choices.get((i + 1) % choices.size()).getDisplayId(); break;
            }
        rebuild();
    }

    @Override public void onDisplayAdded(int id) { rebuild(); }
    @Override public void onDisplayRemoved(int id) { rebuild(); }
    @Override public void onDisplayChanged(int id) {
        state.releaseInput(); running=false;
        if (secondary == null && !candidates().isEmpty()) rebuild();
        else refresh();
    }

    private boolean controller(KeyEvent event) {
        int key = event.getKeyCode();
        boolean direction = key >= KeyEvent.KEYCODE_DPAD_UP && key <= KeyEvent.KEYCODE_DPAD_RIGHT;
        boolean action = key == KeyEvent.KEYCODE_BUTTON_A || key == KeyEvent.KEYCODE_DPAD_CENTER;
        boolean start = key == KeyEvent.KEYCODE_BUTTON_START;
        boolean back = key == KeyEvent.KEYCODE_BUTTON_B;
        if (!direction && !action && !start && !back && !KeyEvent.isGamepadButton(key)) return false;
        if(activeDoor!=null)return true;
        state.lastInput = KeyEvent.keyCodeToString(key) + (event.getAction() == KeyEvent.ACTION_UP ? " released" : " pressed");
        if(quitPrompt){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                if(back||start)answerQuit(false);
                else if(action)answerQuit(quitYes);
                else if(direction)quitYes=key==KeyEvent.KEYCODE_DPAD_LEFT?true:key==KeyEvent.KEYCODE_DPAD_RIGHT?false:!quitYes;
            }
            refresh();return true;
        }
        if(pauseMenu){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                if(start||back)resumeGameplay();
                else if(action)pauseAction(pauseSelection);
                else if(direction){int move=(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_LEFT)?-1:1;pauseSelection=Math.floorMod(pauseSelection+move,pauseEntries().length);}
            }
            refresh();return true;
        }
        if(pickupItem>0){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                if(direction)pickupYes=key==KeyEvent.KEYCODE_DPAD_LEFT?true:key==KeyEvent.KEYCODE_DPAD_RIGHT?false:!pickupYes;
                else if(action)answerPickup(pickupYes);
                else if(back||start)answerPickup(false);
            }
            refresh();return true;
        }
        if(state.paused&&itemActions.open){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                if(start)resumeGameplay();
                else if(itemChecking()){if(action||back)closeItemCheck();}
                else if(back){itemActions.close();menuSound(6);}
                else if(direction){itemActions.move(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);menuSound(4);}
                else if(action)itemCommand(itemActions.selected);
            }
            refresh();return true;
        }
        if(!state.paused){
            boolean down=event.getAction()==KeyEvent.ACTION_DOWN;
            if(down&&event.getRepeatCount()==0)state.pressed=true;
            if(key==KeyEvent.KEYCODE_BUTTON_R1){state.aimBumper=down;return true;}
            if(key==KeyEvent.KEYCODE_BUTTON_R2){state.aimButton=down;return true;}
            if(action&&(state.aiming()||(combat!=null&&(combat.grabbed||combat.dead)))){
                state.fire=down;return true;
            }
        }
        if(action&&event.getAction()==KeyEvent.ACTION_UP)state.fire=false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (direction) {
                int dx = key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 0;
                int dy = key == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 0;
                if (state.paused) {state.selected = Math.floorMod(state.selected + dx + dy * 2, 8);menuSound(4);}
                else { if(dx!=0)state.axisX=dx; if(dy!=0)state.axisY=dy; }
            } else if (event.getRepeatCount() == 0) {
                if (start) { togglePause(); }
                if (back) { if(closeItemCheck()){} else if(combineFrom>=0) {state.selected=combineFrom;combineFrom=-1;message="Combine cancelled";itemActions.open(NativeInventory.snapshot(),state.selected);menuSound(6);} else if(state.paused){resumeGameplay();} else running=true; }

                if(action) {
                    if(closeItemCheck()){}
                    else if(worldMessage){resumeGameplay();}
                    else if(state.paused){if(combineFrom>=0)selectSlot(state.selected);else openItemActions();}
                    else interact();
                }
            }
        }
        if(event.getAction()==KeyEvent.ACTION_UP) {
            if(back)running=false;
            if(direction) { if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)state.axisX=0;else state.axisY=0; }
        }
        refresh(); return true;
    }

    private void requestQuit(){
        if(activeDoor!=null||quitPrompt)return;
        menuSound(5);quitWasPaused=state.paused;quitPrompt=true;quitYes=false;state.paused=true;
        state.releaseInput();running=false;hatX=hatY=0;refresh();
    }
    private void answerQuit(boolean yes){
        if(!quitPrompt)return;
        if(yes){dismissSecondary();finishAndRemoveTask();return;}
        menuSound(6);quitPrompt=false;quitYes=false;state.paused=quitWasPaused;state.releaseInput();running=false;hatX=hatY=0;
        getWindow().getDecorView().setFocusableInTouchMode(true);getWindow().getDecorView().requestFocus();refresh();
    }
    @Override public void onBackPressed(){if(quitPrompt)answerQuit(false);else requestQuit();}

    private void closeMessage(){NativeWorld.closeMessage();worldMessage=false;message="Select an item to manage inventory";}
    private boolean itemChecking(){for(ProbePanel p:panels)if(p.inventoryView!=null&&p.inventoryView.isChecking())return true;return false;}
    private boolean closeItemCheck(){boolean closed=false;for(ProbePanel p:panels)if(p.inventoryView!=null)closed|=p.inventoryView.closeCheck();if(closed)menuSound(6);return closed;}
    private void resumeGameplay(){
        if(state.paused)menuSound(6);
        if(combat!=null&&combat.dead){openPauseMenu();return;}
        if(activeDoor!=null||quitPrompt)return;
        pauseMenu=false;itemActions.close();
        if(pickupItem>0){NativeWorld.confirm(false);pickupItem=0;}
        closeItemCheck();closeMessage();state.paused=false;combineFrom=-1;running=false;state.releaseInput();hatX=hatY=0;
        // The activity remains the controller input owner after lower-screen touches.
        getWindow().getDecorView().setFocusableInTouchMode(true);
        getWindow().getDecorView().requestFocus();
        refresh();
    }
    private String[] pauseEntries(){
        if(combat!=null&&combat.dead)return new String[]{"Restart exploration","Quit game"};
        if(pausePage==1)return ROOM_ITEMS;
        if(pausePage==2){
            int start=roomPage*8,count=Math.min(8,rooms.keys.length-start);
            String[] entries=new String[count+3];
            for(int i=0;i<count;i++)entries[i]=String.format(Locale.US,"Room %03X",rooms.keys[start+i]);
            entries[count]="Previous page";entries[count+1]="Next page";entries[count+2]="Back";return entries;
        }
        String[] items=PAUSE_ITEMS.clone();items[7]="Door animations: "+(doorAnimations?"On":"Off");return items;
    }
    private void openPauseMenu(){
        if(activeDoor!=null||quitPrompt||pickupItem>0||worldMessage)return;
        if(combat!=null&&combat.dead)message="You died. Choose restart or quit.";
        itemActions.close();closeItemCheck();menuSound(5);pauseMenu=true;pausePage=0;pauseSelection=0;state.paused=true;combineFrom=-1;running=false;state.releaseInput();hatX=hatY=0;refresh();
    }
    private void togglePause(){if(state.paused)resumeGameplay();else openPauseMenu();}
    private void pauseAction(int choice){
        if(combat!=null&&combat.dead){if(choice==0){pauseMenu=false;resetGame();}else requestQuit();return;}
        if(quitPrompt)return;
        if(pausePage==2){
            int count=Math.min(8,rooms.keys.length-roomPage*8);
            if(choice<count){int key=rooms.keys[roomPage*8+choice];exploreRoom(key/256,key%256);return;}
            if(choice==count)roomPage=Math.floorMod(roomPage-1,(rooms.keys.length+7)/8);
            else if(choice==count+1)roomPage=(roomPage+1)%((rooms.keys.length+7)/8);
            else {pausePage=1;pauseSelection=3;refresh();return;}
            pauseSelection=0;refresh();return;
        }
        if(pausePage==1){
            if(choice==4){pausePage=0;pauseSelection=3;refresh();return;}
            if(choice==3){pausePage=2;roomPage=0;pauseSelection=0;refresh();return;}
            pauseMenu=false;
            if(choice==0)enterRoom(0,19800,300,16384,0);
            else if(choice==1)enterRoom(1,-15332,-11799,14656,0);
            else enterRoom(2,-23240,20939,0,0);
            return;
        }
        switch(choice){
            case 0:resumeGameplay();break;
            case 1:menuSound(5);pauseMenu=false;state.paused=true;message="Select an item to manage inventory";refresh();break;
            case 2:resumeGameplay();interact();break;
            case 3:if(story!=null&&story.locked){message="Finish the scene before changing rooms";refresh();break;}pausePage=1;pauseSelection=0;refresh();break;
            case 4:state.swapped=!state.swapped;rebuild();break;
            case 5:pauseMenu=false;NativeInventory.reset(true);worldMessage=false;NativeWorld.closeMessage();message="Test supplies added";refresh();break;
            case 6:pauseMenu=false;resetGame();break;
            case 7:pauseSelection=7;doorAnimations=!doorAnimations;getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("doorAnimations",doorAnimations).apply();refresh();break;
            case 8:requestQuit();break;
            default:break;
        }
    }
    private void beginCombine(){
        if(activeDoor!=null||quitPrompt)return;pauseMenu=false;state.paused=true;itemActions.close();menuSound(5);combineFrom=state.selected;message="Select the second item to combine";refresh();}
    private void checkItem(){
        if(activeDoor!=null||quitPrompt)return;pauseMenu=false;state.paused=true;menuSound(5);for(ProbePanel p:panels)if(p.inventoryView!=null)p.inventoryView.check();refresh();}
    private void updateActionHint(){
        if(story!=null&&story.locked){actionHint="Story sequence · Start: pause";return;}
        if(combat!=null&&combat.dead){actionHint="You died";return;}
        if(combat!=null&&combat.grabbed){actionHint="Tap A repeatedly to break free";return;}
        if(state.aiming()){actionHint="A: fire / stab · Empty handgun: press A again to reload";return;}
        int i=RoomInteraction.nearest(triggers,session.x,session.z,session.direction,session.floor());
        actionHint=i<0?"R1 / RT: aim · A: interact":triggers[i+1]==1?"A: open door":triggers[i+1]==2?"A: pick up "+room.menu.name(triggers[i+12]):"A: examine";
    }
    private void enterRoom(int id,float x,float z,float direction,int camera){enterRoom(1,id,x,0,z,direction,camera);}
    private void exploreRoom(int stage,int id){
        try {RoomData next=rooms.load(stage,id);enterLoadedRoom(next,next.spawnX,next.spawnY,next.spawnZ,next.spawnDirection,next.spawnCamera);}
        catch(Exception ex){roomLoadFailed(ex);}
    }
    private void roomLoadFailed(Exception ex){
        Log.e("Re2Thor","Room load failed",ex);pauseMenu=false;message="Could not load that room. "+ex.getMessage();worldMessage=true;state.paused=true;refresh();
    }
    private void enterRoom(int stage,int id,float x,float y,float z,float direction,int camera){
        try {enterLoadedRoom(rooms.load(stage,id),x,y,z,direction,camera);}
        catch(Exception ex){roomLoadFailed(ex);}
    }
    private void enterLoadedRoom(RoomData next,float x,float y,float z,float direction,int camera){
        story=null;storyVoice.stop();
        try{effectData=effectData.forRoom(next.stage,next.roomId);}catch(java.io.IOException ex){throw new IllegalStateException("Room effects load failed",ex);}
        actorAssets.clear();worldActors=RoomActors.EMPTY;worldEffects=new int[0];
        roomCheckpoint=checkpoint(next.stage,next.roomId,x,y,z,direction,camera);
        if(!NativeWorld.enter(next.raw,next.stage,next.roomId))throw new IllegalStateException("Room script load failed");
        try{combatAudio.room(NativeEffects.enemyBank(),1000+(next.stage-1)*256+next.roomId);}catch(java.io.IOException ex){throw new IllegalStateException("Combat sound load failed",ex);}
        combat=new CombatState(NativeCombat.snapshot());
        itemActions.close();pauseMenu=false;room=next;session=new RoomSession(room);session.x=x;session.y=y;session.z=z;session.direction=direction;session.camera=Math.max(0,Math.min(camera,room.cameras.length-1));
        state.releaseInput();running=false;scriptTime=0;combineFrom=-1;worldMessage=false;pickupItem=0;worldObjects=NativeWorld.objects();state.paused=false;
        triggers=NativeWorld.triggers();message=String.format(Locale.US,"Entered room %d%02X",room.stage,room.roomId);rebuild();
    }
    private boolean actorError;
    private void updateActors(){
        if(actorError)return;
        try {worldActors=actorAssets.decode(NativeWorld.actors());}
        catch(java.io.IOException|IllegalArgumentException ex){
            actorError=true;state.paused=true;worldActors=RoomActors.EMPTY;
            Log.e("Re2Thor","Actor assets failed",ex);
            new AlertDialog.Builder(this).setTitle("Actor could not load").setMessage(ex.toString())
                .setPositiveButton("Close",(d,w)->finishAndRemoveTask()).show();
        }
    }
    private void resetGame(){state.health.reset();NativeWorld.reset();NativeInventory.reset(false);enterRoom(0,19800,300,16384,0);}
    private void interact(){
        if(story!=null&&story.locked)return;
        if(combat!=null&&(combat.grabbed||combat.dead))return;
        if(activeDoor!=null||quitPrompt)return;
        if(pickupItem>0){answerPickup(pickupYes);return;}
        int i=RoomInteraction.nearest(triggers,session.x,session.z,session.direction,session.floor());
        if(i<0){message="Move closer to a door or item and face it";return;}
        int[] result=NativeWorld.trigger(triggers[i]);
        if(result[0]==2){
            DoorAnimation animation=null;
            if(doorAnimations)try {animation=doors.get(room.stage,room.roomId,triggers[i]);}
            catch(Exception ex){Log.w("Re2Thor","Door animation unavailable",ex);}
            enterRoom(result[1],result[2],result[3],result[4],result[5],result[6],result[7]);
            if(animation!=null&&!state.paused){activeDoor=animation;doorTime=0;state.paused=true;message="Opening door…";refresh();}
            return;
        }
        if(result[0]==5){
            closeItemCheck();pickupItem=result[1];pickupYes=true;worldMessage=false;state.paused=true;running=false;state.releaseInput();
            message="Will you take the "+room.menu.name(pickupItem)+"?";
        } else if(result[0]==3){message=NativeWorld.message();worldMessage=true;state.paused=true;state.releaseInput();}
        triggers=NativeWorld.triggers();refresh();
    }
    private void answerPickup(boolean yes){
        if(pickupItem==0)return;
        int[] result=NativeWorld.confirm(yes);pickupItem=0;
        worldObjects=NativeWorld.objects();triggers=NativeWorld.triggers();
        if(result[0]==3){message=NativeWorld.message();worldMessage=true;state.paused=true;state.releaseInput();}
        else {resumeGameplay();if(result[0]==1)message="You took the "+room.menu.name(result[1])+".";}
        refresh();
    }
    private void menuSound(int note){if(combatAudio!=null&&resumed&&screensOn()){combatAudio.setMenuEnabled(true);combatAudio.menu(note);}}
    private void selectSlot(int slot) {
        if(activeDoor!=null||quitPrompt)return;
        pauseMenu=false;
        if(worldMessage){resumeGameplay();return;}
        closeItemCheck();state.select(slot);state.releaseInput();itemActions.close();
        if(combineFrom>=0) {
            int result=NativeInventory.combine(combineFrom,slot);combineFrom=-1;
            menuSound(result==1?8:result==2?5:7);
            message=result==1?"Ammunition loaded":result==2?"Herbs combined":result==-1?"Weapon is already full":"These items cannot be combined";
        } else {message=room.menu.name(NativeInventory.snapshot()[slot*2]);openItemActions();}
        refresh();
    }
    private void openItemActions(){
        if(worldMessage||pickupItem>0||quitPrompt||activeDoor!=null)return;
        pauseMenu=false;state.paused=true;state.releaseInput();hatX=hatY=0;
        menuSound(itemActions.open(NativeInventory.snapshot(),state.selected)?5:7);refresh();
    }
    private void itemCommand(int command){
        if(!itemActions.open||itemChecking())return;
        if(command==InventoryActions.CHECK){checkItem();return;}
        itemActions.close();
        if(command==InventoryActions.COMBINE){beginCombine();return;}
        if(command==InventoryActions.PRIMARY){
            int item=NativeInventory.snapshot()[state.selected*2];
            if(InventoryActions.weapon(item))equip();
            else {
                int result=NativeWorld.useItem(state.selected,(int)session.x,(int)session.y,(int)session.z,(int)session.direction,session.camera);
                combat=new CombatState(NativeCombat.snapshot());state.health.restore(combat.hp);
                menuSound(result>0?5:7);
                message=result==1?"Health restored.":result==2?"You unlocked the door.":result==-1?"You are already in Fine condition at full health.":"You can't use this here.";
                refresh();
            }
        }
    }
    private void inventoryBack(){if(!closeItemCheck()){itemActions.close();menuSound(6);}refresh();}
    private void equip() {
        if(story!=null&&story.locked){message="Equipment is unavailable during this scene";refresh();return;}
        if(combat!=null&&(combat.grabbed||combat.dead)){message="Cannot change equipment while grabbed";refresh();return;}
        if(activeDoor!=null||quitPrompt)return;
        pauseMenu=false;
        if(worldMessage){resumeGameplay();return;}
        state.paused=true;
        boolean ok=NativeInventory.equip(state.selected)!=0;menuSound(ok?5:7);
        message=ok?(NativeInventory.snapshot()[16]==state.selected?"Weapon equipped":"Weapon unequipped"):"Select a weapon to equip";
        combineFrom=-1;refresh();
    }
    private float axis(MotionEvent event, int axis) {
        InputDevice device = event.getDevice();
        InputDevice.MotionRange range = device == null ? null : device.getMotionRange(axis, event.getSource());
        float value = event.getAxisValue(axis);
        return Math.abs(value) > Math.max(.15f, range == null ? 0 : range.getFlat()) ? value : 0;
    }

    private boolean joystick(MotionEvent event) {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.getAction() != MotionEvent.ACTION_MOVE) return false;
        if(activeDoor!=null){state.releaseInput();return true;}
        if(!state.paused)state.aimTrigger=event.getAxisValue(MotionEvent.AXIS_RTRIGGER)>.5f||event.getAxisValue(MotionEvent.AXIS_GAS)>.5f;
        state.axisX = axis(event, MotionEvent.AXIS_X); state.axisY = axis(event, MotionEvent.AXIS_Y);
        int hx = Math.round(event.getAxisValue(MotionEvent.AXIS_HAT_X));
        int hy = Math.round(event.getAxisValue(MotionEvent.AXIS_HAT_Y));
        if(quitPrompt){int nav=hx!=0?hx:state.axisX>.5f?1:state.axisX<-.5f?-1:0;if(nav!=0&&nav!=hatX)quitYes=nav<0;hatX=nav;state.releaseInput();refresh();return true;}
        if(pauseMenu){int nav=hy!=0?hy:state.axisY>.5f?1:state.axisY<-.5f?-1:0;if(nav!=0&&nav!=hatY)pauseSelection=Math.floorMod(pauseSelection+nav,pauseEntries().length);hatY=nav;state.releaseInput();refresh();return true;}
        if(pickupItem>0){int nav=hx!=0?hx:state.axisX>.5f?1:state.axisX<-.5f?-1:0;if(nav!=0&&nav!=hatX)pickupYes=nav<0;hatX=nav;state.releaseInput();refresh();return true;}
        if(state.paused&&itemActions.open){
            int nav=hy!=0?hy:state.axisY>.5f?1:state.axisY<-.5f?-1:0;
            if(!itemChecking()&&nav!=0&&nav!=hatY){itemActions.move(nav);menuSound(4);}
            hatY=nav;state.releaseInput();refresh();return true;
        }
        if (state.paused && (hx != 0 || hy != 0) && (hx != hatX || hy != hatY)) {
            state.selected = Math.floorMod(state.selected + hx + hy * 2, 8);menuSound(4);
        }
        if (state.axisX == 0) state.axisX = hx;
        if (state.axisY == 0) state.axisY = hy;
        hatX = hx; hatY = hy;
        state.lastInput = String.format(Locale.US, "Stick %.2f, %.2f · triggers %.2f / %.2f",
                state.axisX, state.axisY, event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_RTRIGGER));
        return true;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) { return controller(e) || super.dispatchKeyEvent(e); }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e) { return joystick(e) || super.dispatchGenericMotionEvent(e); }

    private final class ProbePresentation extends Presentation {
        ProbePresentation(Context context, Display display) { super(context, display); }
        @Override protected void onCreate(Bundle saved) {
            super.onCreate(saved);
            // Touch still reaches this window; gamepad keys/axes stay with the Activity.
            // This also holds when the screen roles are swapped.
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            setContentView(new ProbePanel(getContext(), getDisplay().getDisplayId(), !state.swapped));
            configureWindow(getWindow()); setCancelable(false);
        }
        @Override public boolean dispatchKeyEvent(KeyEvent e) { return controller(e) || super.dispatchKeyEvent(e); }
        @Override public boolean dispatchGenericMotionEvent(MotionEvent e) { return joystick(e) || super.dispatchGenericMotionEvent(e); }
    }

    private final class ProbePanel extends LinearLayout {
        final int displayId;
        InventoryView inventoryView;
        Scene scene;
        PauseMenuView menu;
        ProbePanel(Context c,int id,boolean inventory){
            super(c);displayId=id;setOrientation(VERTICAL);setBackgroundColor(Color.BLACK);
            if(inventory){
                inventoryView=new InventoryView(c,room.menu,new InventoryView.Actions(){
                    public void select(int slot){selectSlot(slot);}
                    public void command(int index){itemCommand(index);}
                    public void back(){inventoryBack();}
                    public void equip(){MainActivity.this.equip();}
                    public void combine(){if(worldMessage)resumeGameplay();else beginCombine();}
                    public void check(){if(worldMessage)resumeGameplay();else checkItem();}
                    public void exit(){requestQuit();}
                    public void resume(){resumeGameplay();}
                    public void quit(boolean yes){answerQuit(yes);}
                    public void answer(boolean yes){answerPickup(yes);}
                });
                addView(inventoryView,new LayoutParams(-1,-1));
            }else{
                FrameLayout frame=new FrameLayout(c);
                scene=new Scene(c);frame.addView(scene,new FrameLayout.LayoutParams(-1,-1));
                scene.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();if(worldMessage)resumeGameplay();else openPauseMenu();}return true;});
                menu=new PauseMenuView(c,index->pauseAction(index));frame.addView(menu,new FrameLayout.LayoutParams(-1,-1));menu.setVisibility(View.GONE);
                addView(frame,new LayoutParams(-1,-1));
            }
            panels.add(this);
        }
        void refresh(){
            if(inventoryView!=null){inventoryView.commands(itemActions.open,itemActions.selected);inventoryView.status(state.health.hp(),quitPrompt,quitYes);inventoryView.pickup(pickupItem,pickupYes);inventoryView.update(NativeInventory.snapshot(),state.selected,combineFrom,state.paused,worldMessage,state.paused?message:actionHint);}
            if(menu!=null){menu.update(pauseEntries(),pauseSelection,combat!=null&&combat.dead);menu.setVisibility(pauseMenu?View.VISIBLE:View.GONE);}
            if(scene!=null)scene.update();
        }
    }

    private final class Scene extends GLSurfaceView {
        final RoomRenderer renderer;
        Scene(Context c) {
            super(c);setEGLContextClientVersion(2);setEGLConfigChooser(8,8,8,8,24,0);
            renderer=new RoomRenderer(room,session.pose());renderer.effectData=effectData;setRenderer(renderer);setRenderMode(RENDERMODE_WHEN_DIRTY);
            setContentDescription("Resident Evil 2 gameplay. Tap to pause.");
        }
        void update(){renderer.pose=session.pose();renderer.objects=worldObjects;renderer.actors=worldActors;renderer.effects=worldEffects;
            renderer.doorFrame=activeDoor==null?0:Math.min(activeDoor.frames-1,(int)(doorTime*activeDoor.fps));
            renderer.door=activeDoor;requestRender();}
    }
}

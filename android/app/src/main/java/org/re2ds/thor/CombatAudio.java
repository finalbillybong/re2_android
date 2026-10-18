package org.re2ds.thor;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Original short PCM effects. Pause/focus loss stops sounds instead of replaying stale shots. */
final class CombatAudio implements AutoCloseable {
    private final SoundPool pool=new SoundPool.Builder().setMaxStreams(16).setAudioAttributes(
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
    private final AssetManager assets;
    private final RoomData.Reader reader;
    private final Map<Integer,Sample[]> banks=new HashMap<>();
    private final Map<Integer,Sample> ids=new HashMap<>();
    private final ArrayList<Pending> pending=new ArrayList<>();
    private final int[] streams=new int[16];
    private int streamIndex,menuStream;
    private boolean menuEnabled;
    private boolean running,closed;
    private static final class Sample {int low,high,id;boolean ready,failed;}
    private static final class Pending {Sample sample;int volume,pan;long time;boolean menu;}
    CombatAudio(AssetManager assets,RoomData.Reader reader)throws IOException {
        this.assets=assets;this.reader=reader;
        pool.setOnLoadCompleteListener((p,id,status)->{
            if(closed)return;
            Sample sample=ids.get(id);
            if(sample!=null){sample.ready=status==0;sample.failed=status!=0;}
            if(status!=0)Log.e("Re2Thor","Sound sample load failed: "+id+" status="+status);
            flush();
        });
        try {load(0);load(1);load(2);}catch(IOException|RuntimeException error){close();throw error;}
    }
    void room(int bank,int roomBank)throws IOException {
        setRunning(false);
        // Only keep the current enemy bank; weapon and player banks remain shared.
        Iterator<Map.Entry<Integer,Sample[]>> it=banks.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer,Sample[]> entry=it.next();
            if(entry.getKey()>=100&&entry.getKey()!=roomBank&&entry.getKey()!=bank){for(Sample sample:entry.getValue()){pool.unload(sample.id);ids.remove(sample.id);}it.remove();}
        }
        if(bank>=0)load(bank);
        load(roomBank);
    }
    private void load(int bank)throws IOException {
        if(banks.containsKey(bank))return;
        ByteBuffer bytes=RoomData.bytes(reader.read("audio/b"+bank+".bin"));
        int count=RoomData.u16(bytes,0);RoomData.require(bytes.limit()==2+count*4,"Invalid sound index");
        Sample[] samples=new Sample[count];
        for(int i=0;i<count;i++){
            Sample sample=samples[i]=new Sample();int o=2+i*4;
            sample.low=RoomData.u8(bytes,o);sample.high=RoomData.u8(bytes,o+1);int file=RoomData.u16(bytes,o+2);
            try(AssetFileDescriptor fd=assets.openFd("re2/audio/b"+bank+"_"+file+".wav")){
                sample.id=pool.load(fd,1);if(sample.id==0)throw new IOException("Could not load sound bank "+bank);
                ids.put(sample.id,sample);
            }
        }
        banks.put(bank,samples);
    }
    void setRunning(boolean value){
        if(closed||value==running)return;
        running=value;
        if(!running){for(int stream:streams)if(stream!=0)pool.stop(stream);java.util.Arrays.fill(streams,0);pending.removeIf(p->!p.menu);}
    }
    void setMenuEnabled(boolean value){
        menuEnabled=value;
        if(!value){if(menuStream!=0)pool.stop(menuStream);menuStream=0;pending.removeIf(p->p.menu);}
    }
    void menu(int note){
        if(closed||!menuEnabled)return;
        Sample[] samples=banks.get(0);if(samples==null)return;
        for(Sample sample:samples)if(note>=sample.low&&note<=sample.high){
            if(sample.failed)return;
            Pending p=new Pending();p.sample=sample;p.volume=127;p.pan=64;p.menu=true;p.time=SystemClock.uptimeMillis();
            if(sample.ready)play(p);else if(pending.size()<64)pending.add(p);return;
        }
    }
    void play(int[] events)throws IOException {
        if(!running||closed)return;
        for(int i=0;i+3<events.length;i+=4){
            // Some rooms spawn their enemies in the first script tick, after room().
            if(!banks.containsKey(events[i]))load(events[i]);
            Sample[] samples=banks.get(events[i]);
            for(Sample sample:samples)if(events[i+1]>=sample.low&&events[i+1]<=sample.high){
                if(sample.failed)break;
                Pending p=new Pending();p.sample=sample;p.volume=events[i+2];p.pan=events[i+3];p.time=SystemClock.uptimeMillis();
                if(sample.ready)play(p);else if(pending.size()<64)pending.add(p);break;
            }
        }
        flush();
    }
    private void flush(){
        long now=SystemClock.uptimeMillis();
        Iterator<Pending> it=pending.iterator();
        while(it.hasNext()){
            Pending p=it.next();
            if(!(p.menu?menuEnabled:running)||now-p.time>200||p.sample.failed){it.remove();continue;}
            if(p.sample.ready){play(p);it.remove();}
        }
    }
    private void play(Pending p){
        float volume=Math.max(0,Math.min(127,p.volume))/127f,pan=Math.max(0,Math.min(127,p.pan))/127f;
        if(p.menu){if(menuStream!=0)pool.stop(menuStream);menuStream=pool.play(p.sample.id,volume,volume,1,0,1);return;}
        int i=streamIndex++%streams.length;if(streams[i]!=0)pool.stop(streams[i]);
        streams[i]=pool.play(p.sample.id,volume*Math.min(1,2*(1-pan)),volume*Math.min(1,2*pan),1,0,1);
    }
    @Override public void close(){if(closed)return;setRunning(false);setMenuEnabled(false);closed=true;pool.release();banks.clear();ids.clear();}
}

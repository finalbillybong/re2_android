package org.re2ds.thor;

import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import java.io.IOException;

/** Stream original dialogue; pauses with the simulation, including lid/background. */
final class StoryVoice implements AutoCloseable {
    private final AssetManager assets;
    private MediaPlayer player;
    private boolean running;
    StoryVoice(AssetManager assets){this.assets=assets;}
    void play(int event) throws IOException {
        if(event<0)return;
        stop();
        MediaPlayer next=new MediaPlayer();
        try(AssetFileDescriptor file=assets.openFd(String.format(java.util.Locale.US,"re2/voice/v%d_%02d.wav",event/1000,event%1000))){
            next.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            next.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            next.setOnCompletionListener(p->{if(player==p)stop();});
            next.prepare();player=next;if(running)player.start();
        }catch(IOException|RuntimeException ex){next.release();player=null;throw ex;}
    }
    void setRunning(boolean value){
        if(running==value)return;running=value;
        if(player!=null){if(value)player.start();else if(player.isPlaying())player.pause();}
    }
    void stop(){if(player!=null){player.release();player=null;}}
    @Override public void close(){stop();}
}

package com.clenova.app;

import android.content.Context;
import android.media.*;
import android.os.*;
import java.util.concurrent.*;

/** Foreground-only AudioTrack controller. All audio state transitions happen on the main thread. */
final class SpeakerPlayback {
    interface Listener {
        void preparing();
        void progress(long playedMs,long totalMs);
        void finished(SpeakerTone.Mode mode);
        void stopped(String reason);
    }
    private final AudioManager audio;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Listener listener;
    private final AudioAttributes attributes=new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private AudioTrack track;
    private AudioFocusRequest focus;
    private AudioDeviceCallback devices;
    private AudioRouting.OnRoutingChangedListener routing;
    private Future<?> synthesis;
    private SpeakerTone.Mode mode;
    private int epoch,frames;
    private long started,lastMovement;
    private int previousHead;
    private boolean active,audible,released;
    private float gain;
    SpeakerPlayback(Context context,Listener listener){
        audio=context.getSystemService(AudioManager.class);this.listener=listener;
    }
    boolean active(){return active;}
    int mediaVolume(){return audio==null?0:audio.getStreamVolume(AudioManager.STREAM_MUSIC);}
    int maxVolume(){return audio==null?0:audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);}
    private boolean externalConnected(){
        if(audio==null)return true;
        for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int type=d.getType();
            // Fail closed on external, remote, or unrecognized output devices.
            if(type!=AudioDeviceInfo.TYPE_BUILTIN_SPEAKER&&type!=AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                &&type!=AudioDeviceInfo.TYPE_TELEPHONY&&type!=AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)return true;
        }return false;
    }
    private boolean speakerRoute(){
        AudioDeviceInfo d=track==null?null:track.getRoutedDevice();
        return d!=null&&(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER||d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE);
    }
    String readyReason(){
        try{
            if(audio==null)return "Audio playback unavailable.";
            if(audio.getMode()!=AudioManager.MODE_NORMAL)return "End the active call or audio communication first.";
            if(externalConnected())return "Disconnect headphones, Bluetooth audio and other external audio devices.";
            if(audio.isStreamMute(AudioManager.STREAM_MUSIC)||mediaVolume()<=0)return "Set a low, comfortable media volume first.";
            if(mediaVolume()>Math.max(1,(int)Math.floor(maxVolume()*.75)))return "Lower media volume before starting. Do not use maximum volume.";
            return "";
        }catch(RuntimeException e){return "Audio route could not be verified.";}
    }
    void start(SpeakerTone.Mode requested,float requestedGain){
        if(released||active)return;
        String reason=readyReason();if(!reason.isEmpty()){listener.stopped(reason);return;}
        mode=requested;gain=Math.max(.15f,Math.min(.85f,requestedGain));active=true;audible=false;final int token=++epoch;listener.preparing();
        synthesis=worker.submit(()->{try{short[] pcm=SpeakerTone.samples(requested);main.post(()->{if(active&&!released&&token==epoch)play(pcm,token);});}
            catch(RuntimeException|OutOfMemoryError e){main.post(()->{if(active&&token==epoch)stop("Could not prepare cleaning sound.");});}});
    }
    private void play(short[] pcm,int token){
        String reason=readyReason();if(!reason.isEmpty()){stop(reason);return;}
        try{
            focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(change->{if(active&&epoch==token&&change!=AudioManager.AUDIOFOCUS_GAIN)stop("Audio interrupted. Start again when ready.");},main).build();
            if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stop("Another app is using audio. Try again later.");return;}
            track=new AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(SpeakerTone.RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.length*2).setTransferMode(AudioTrack.MODE_STATIC).build();
            if(track.getState()!=AudioTrack.STATE_NO_STATIC_DATA&&track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IllegalStateException("AudioTrack unavailable");
            if(track.write(pcm,0,pcm.length,AudioTrack.WRITE_BLOCKING)!=pcm.length)throw new IllegalStateException("Audio buffer incomplete");
            frames=pcm.length;
            // Start muted; only enable sound once the actual built-in speaker route is confirmed.
            if(track.setVolume(0f)!=AudioTrack.SUCCESS)throw new IllegalStateException("Could not mute initial playback");
            routing=router->{if(!active||epoch!=token)return;if(audible&&!speakerRoute())stop("Audio output changed. Playback stopped.");};
            track.addOnRoutingChangedListener(routing,main);
            devices=new AudioDeviceCallback(){
                @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added){checkDevices(token);}
                @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){checkDevices(token);}
            };
            audio.registerAudioDeviceCallback(devices,main);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener(){
                @Override public void onMarkerReached(AudioTrack t){if(active&&epoch==token&&t==track)completeIfValid();}
                @Override public void onPeriodicNotification(AudioTrack t){}
            },main);
            if(track.setNotificationMarkerPosition(frames-1)!=AudioTrack.SUCCESS)throw new IllegalStateException("Audio marker unavailable");
            reason=readyReason();if(!reason.isEmpty()){stop(reason);return;}
            started=lastMovement=SystemClock.elapsedRealtime();previousHead=0;
            track.play();main.post(tick);
        }catch(RuntimeException e){stop("Could not play through the built-in speaker.");}
    }
    private void checkDevices(int token){
        if(!active||epoch!=token)return;String reason=readyReason();
        if(!reason.isEmpty())stop(reason);else if(audible&&!speakerRoute())stop("Audio output changed. Playback stopped.");
    }
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!active||track==null)return;
        try{
            String reason=readyReason();if(!reason.isEmpty()){stop(reason);return;}
            long now=SystemClock.elapsedRealtime();int head=track.getPlaybackHeadPosition();
            if(head>previousHead){lastMovement=now;previousHead=head;}
            if(!audible){
                if(speakerRoute()&&head<SpeakerTone.leadFrames()){
                    if(track.setVolume(gain)!=AudioTrack.SUCCESS){stop("Could not set sound level.");return;}audible=true;
                }else if(head>=SpeakerTone.leadFrames()||now-started>1000){stop("Built-in speaker route could not be confirmed.");return;}
            }else if(!speakerRoute()){stop("Audio output changed. Playback stopped.");return;}
            if(audible&&head>=frames-1){completeIfValid();return;}
            if(now-lastMovement>1800||now-started>(mode.seconds+4L)*1000){stop("Audio playback stalled. Try again.");return;}
            long played=Math.max(0,Math.min(mode.seconds*1000L,(head-SpeakerTone.leadFrames())*1000L/SpeakerTone.RATE));
            listener.progress(played,mode.seconds*1000L);main.postDelayed(this,40);
        }catch(RuntimeException e){stop("Audio playback interrupted.");}
    }};
    private void completeIfValid(){
        if(!active)return;
        try{if(!audible||!speakerRoute()||!readyReason().isEmpty()){stop("Audio route changed before completion.");return;}
            SpeakerTone.Mode finished=mode;clear();listener.finished(finished);
        }catch(RuntimeException e){stop("Audio completion could not be verified.");}
    }
    void stop(String reason){if(!active)return;clear();listener.stopped(reason);}
    private void clear(){
        active=false;audible=false;epoch++;main.removeCallbacks(tick);
        if(synthesis!=null){synthesis.cancel(true);synthesis=null;}
        AudioTrack old=track;track=null;
        if(old!=null){
            try{old.setVolume(0f);}catch(RuntimeException ignored){}
            if(routing!=null)try{old.removeOnRoutingChangedListener(routing);}catch(RuntimeException ignored){}
            try{old.stop();}catch(RuntimeException ignored){}
            try{old.release();}catch(RuntimeException ignored){}
        }
        routing=null;
        if(devices!=null){try{audio.unregisterAudioDeviceCallback(devices);}catch(RuntimeException ignored){}devices=null;}
        if(focus!=null){try{audio.abandonAudioFocusRequest(focus);}catch(RuntimeException ignored){}focus=null;}
    }
    void release(){if(released)return;released=true;clear();worker.shutdownNow();}
}

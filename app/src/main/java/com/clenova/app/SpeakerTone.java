package com.clenova.app;

/** Pure PCM synthesis, no microphone, network, or high-volume system controls. */
final class SpeakerTone {
    static final int RATE=48000, LEAD_MS=650;
    static final double PEAK=0.60;
    enum Mode {
        QUICK(15,"Quick clean"), DEEP(30,"Deep clean"), TEST(3,"Speaker test");
        final int seconds;final String label;
        Mode(int s,String l){seconds=s;label=l;}
    }
    static int leadFrames(){return RATE*LEAD_MS/1000;}
    static short[] samples(Mode mode){
        int count=RATE*mode.seconds,lead=leadFrames();
        short[] pcm=new short[lead+count];double phase=0;
        for(int i=0;i<count;i++){
            if((i&8191)==0&&Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            double t=i/(double)RATE;
            // Smooth, moderate low-frequency sweeps; no claim of a universal resonance.
            double frequency=mode==Mode.TEST?440:mode==Mode.QUICK?180+40*Math.sin(2*Math.PI*t/3):185+55*Math.sin(2*Math.PI*t/5);
            phase+=2*Math.PI*frequency/RATE;
            double fade=Math.min(1,Math.min(i,count-1-i)/(RATE*0.12));
            double envelope=mode==Mode.TEST?0.55:0.85+0.15*Math.sin(2*Math.PI*t/2);
            pcm[lead+i]=(short)Math.round(32767*PEAK*fade*envelope*Math.sin(phase));
        }
        return pcm;
    }
    private SpeakerTone(){}
}

package com.clenova.app;

public final class SpeakerToneTest {
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    public static void main(String[] args){
        for(SpeakerTone.Mode mode:SpeakerTone.Mode.values()){
            short[] pcm=SpeakerTone.samples(mode);
            int lead=SpeakerTone.leadFrames();
            check(pcm.length==lead+SpeakerTone.RATE*mode.seconds,"Exact cycle duration");
            for(int i=0;i<lead;i++)check(pcm[i]==0,"Silent route confirmation lead");
            check(pcm[lead]==0&&pcm[pcm.length-1]==0,"Fade endpoints");
            int peak=0,zeros=0;
            for(int i=lead;i<pcm.length;i++){peak=Math.max(peak,Math.abs((int)pcm[i]));if(pcm[i]==0)zeros++;}
            check(peak<=Math.ceil(32767*SpeakerTone.PEAK),"No clipping");
            check(peak>500&&zeros<(pcm.length-lead)/10,"Real non-silent output");
            int start=lead+SpeakerTone.RATE/2,end=start+SpeakerTone.RATE;
            int crossings=0;for(int i=start+1;i<end;i++)if(pcm[i-1]<=0&&pcm[i]>0)crossings++;
            check(mode==SpeakerTone.Mode.TEST?crossings>=438&&crossings<=442:crossings>=130&&crossings<=240,"Tone frequency range");
        }
        Thread.currentThread().interrupt();
        boolean cancelled=false;try{SpeakerTone.samples(SpeakerTone.Mode.DEEP);}catch(java.util.concurrent.CancellationException e){cancelled=true;}finally{Thread.interrupted();}
        check(cancelled,"Synthesis responds to cancellation");
        System.out.println("SpeakerToneTest passed");
    }
}

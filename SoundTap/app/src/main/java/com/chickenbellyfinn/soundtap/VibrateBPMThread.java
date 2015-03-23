package com.chickenbellyfinn.soundtap;

import android.content.Context;
import android.os.Vibrator;

/**
 * Thread that vibrates the phone at a given bpm
 * Useless (and possibly annoying) gimmick that I decided to keep
 */
public class VibrateBPMThread extends Thread {

    private static VibrateBPMThread instance = null;

    private int bpm;
    private Vibrator vibrator;

    private boolean isRunning = false;

    static void startTapping(Context c, int bpm){
        instance = new VibrateBPMThread(c, bpm);
        instance.start();
    }

    static void stopTapping(){
        if(instance != null){
            instance.stopVibrating();
        }
    }

    private VibrateBPMThread(){}

    private VibrateBPMThread(Context context, int bpm){
        this.bpm = bpm;
        vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void run(){
        isRunning = true;

        while(isRunning){
            vibrator.vibrate(25); //pulse
            try {
                Thread.sleep((60000)/bpm); //sleep for whatever amount of time makes the bpm work
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void stopVibrating(){
        isRunning = false;
    }
}

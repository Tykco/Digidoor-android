package com.example.android.digidoor_gate;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

// Monitor for changes to the state of the phone
public class StatePhoneReceiver extends PhoneStateListener {
    TelephonyManager manager;
    StatePhoneReceiver myPhoneStateListener;
    public static boolean callFromApp=false; // To control the call has been made from the application
    public static boolean callFromOffHook=false; // To control the change to idle state is from the app call
    Context context;
    public StatePhoneReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        switch (state) {

            case TelephonyManager.CALL_STATE_OFFHOOK: //Call is established
                if (callFromApp) {
                    callFromApp=false;
                    callFromOffHook=true;

                    Toast.makeText(context,
                            "CALL ESTABLISHED HELLOOEOEOEOEOEOEOEOEOEOEOEOEOEOEOEO", Toast.LENGTH_LONG).show();

                    try {
                        Thread.sleep(500); // Delay 0,5 seconds to handle better turning on loudspeaker
                    } catch (InterruptedException e) {
                    }

                    //Activate loudspeaker
                    AudioManager audioManager = (AudioManager)
                            context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                    audioManager.setSpeakerphoneOn(true);
                }
                break;

            case TelephonyManager.CALL_STATE_IDLE: //Call is finished
                if (callFromOffHook) {
                    callFromOffHook=false;
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setMode(AudioManager.MODE_NORMAL); //Deactivate loudspeaker
                    /*manager.listen(myPhoneStateListener, // Remove listener
                            PhoneStateListener.LISTEN_NONE);*/
                }
                break;
        }
    }
}
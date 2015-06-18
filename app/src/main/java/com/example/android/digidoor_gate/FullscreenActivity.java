package com.example.android.digidoor_gate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.android.digidoor_gate.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {

    public UsbManager usbManager;
    public UsbDevice deviceFound;
    public UsbDeviceConnection usbDeviceConnection;
    public UsbInterface usbInterfaceFound = null;
    public UsbEndpoint endpointOut = null;
    public UsbEndpoint endpointIn = null;

    private static final int LOCK = 2;
    private static final int UNLOCK = 1;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 1500;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen2);

        //final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Hides the Action Bar.
        getActionBar().hide();
        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);


        Button button = (Button) findViewById(R.id.button_lock);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendCommand(LOCK);

                Toast.makeText(getApplicationContext(),
                        "Gate is now locked.", Toast.LENGTH_SHORT).show();

                //Re-invoke an instance of the NumbPad.
                setupNumbpad();
            }
        });


        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        setupNumbpad();
    }


    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String action = intent.getAction();

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (deviceFound != null && deviceFound.equals(device)) {
                setDevice(null);
            }
        }
    }

    private void setDevice(UsbDevice device) {
        usbInterfaceFound = null;
        endpointOut = null;
        endpointIn = null;

        for (int i = 0; i < (device.getInterfaceCount()-2); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            //TextInfo.append(usbInterface.toString() + "\n");
            //TextInfo.append("\n");
            UsbEndpoint tOut = null;
            UsbEndpoint tIn = null;

            int tEndpointCnt = usbInterface.getEndpointCount();
            if (tEndpointCnt >= 2) {
                for (int j = 0; j < tEndpointCnt; j++) {
                    if (usbInterface.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (usbInterface.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT) {
                            tOut = usbInterface.getEndpoint(j);
                        } else if (usbInterface.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN) {
                            tIn = usbInterface.getEndpoint(j);
                        }
                    }
                }

                if (tOut != null && tIn != null) {
                    // This interface have both USB_DIR_OUT
                    // and USB_DIR_IN of USB_ENDPOINT_XFER_BULK
                    usbInterfaceFound = usbInterface;
                    endpointOut = tOut;
                    endpointIn = tIn;
                }
            }

        }

        if (usbInterfaceFound == null) {
            return;
        }

        deviceFound = device;

        if (device != null) {
            UsbDeviceConnection connection =
                    usbManager.openDevice(device);
            if (connection != null &&
                    connection.claimInterface(usbInterfaceFound, true)) {
                usbDeviceConnection = connection;
                Thread thread = new Thread();
                thread.start();

            } else {
                usbDeviceConnection = null;
            }
        }
    }

    private void sendCommand(int control) {
        synchronized (this) {

            if (usbDeviceConnection != null) {
                byte[] message = new byte[1];
                message[0] = (byte)control;
				/*ByteBuffer messagebuf = ByteBuffer.wrap(message);
				UsbRequest requestRec = new UsbRequest();
				requestRec.initialize(usbDeviceConnection, endpointOut);
				requestRec.queue(messagebuf, 1);
				while (true) {

					if (usbDeviceConnection.requestWait() == requestRec) {
						break;
					}
				}*/


                usbDeviceConnection.bulkTransfer(endpointOut,message, message.length, 500);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };*/

   /* *//**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     *//*
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }*/

    private void setupNumbpad() {

        // create an instance of NumbPad
        NumbPad np = new NumbPad();
        // optionally set additional title
        np.setAdditionalText("Please Enter Pin:");
        // show the NumbPad to capture input.
        np.show(this, "SAMSUNG DIGIDOOR", NumbPad.HIDE_INPUT,
                new NumbPad.numbPadInterface() {
                    // This is called when the user click the 'Ok' button on the dialog
                    // value is the captured input from the dialog.
                    public String numPadInputValue(String value) {
                        if (value.equals("1234")) {
                            // do something here
                            Toast.makeText(getApplicationContext(),
                                    "Pin is correct, please enter.", Toast.LENGTH_SHORT).show();
                            sendCommand(UNLOCK);
                            /*Intent intent = new Intent(getApplicationContext(), UsbUnlock.class);
                            startActivity(intent);
                            finish();*/
                        } else {
                            // generate a toast message to inform the user that
                            // the captured input is not valid
                            Toast.makeText(getApplicationContext(),
                                    "Pin is incorrect, please try again.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), FullscreenActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        return null;
                    }

                    // This is called when the user clicks the 'Cancel' button on the dialog
                    public String numPadCanceled() {
                        // generate a toast message to inform the user that the pin
                        // capture was canceled
                        Toast.makeText(getApplicationContext(),
                                "Pin capture canceled!", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                });
    }
}

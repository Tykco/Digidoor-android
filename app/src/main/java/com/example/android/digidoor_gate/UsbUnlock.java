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
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class UsbUnlock extends Activity implements Runnable{

    private static final int LOCK = 2;
    private static final int UNLOCK = 1;

    /*SeekBar bar;
    ToggleButton buttonLed;
    TextView TextInfo;*/



    private UsbManager usbManager;
    private UsbDevice deviceFound;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbInterface usbInterfaceFound = null;
    private UsbEndpoint endpointOut = null;
    private UsbEndpoint endpointIn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_unlock);

        // Hides the Action Bar.
        getActionBar().hide();

        //TextInfo = (TextView) findViewById(R.id.info);
        //bar = (SeekBar)findViewById(R.id.seekbar);
        //buttonLed = (ToggleButton)findViewById(R.id.arduinoled);
        //buttonLed.setOnCheckedChangeListener(new OnCheckedChangeListener(){

            /*@Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sendCommand(CMD_LED_ON);
                }else{
                    sendCommand(CMD_LED_OFF);
                }
            }});*/

        //send command to unlock upon entering this intent.
        sendCommand(UNLOCK);

        Button button = (Button) findViewById(R.id.button_lock);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendCommand(LOCK);

                Toast.makeText(getApplicationContext(),
                        "Gate is now locked.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), FullscreenActivity.class);
                startActivity(intent);
                finish();
            }
        });

        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
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
                Thread thread = new Thread(this);
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
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        UsbRequest request = new UsbRequest();
        request.initialize(usbDeviceConnection, endpointIn);
        while (true) {
            request.queue(buffer, 1);
            if (usbDeviceConnection.requestWait() == request) {
                //byte rxCmd = buffer.get(0);
                /*if(rxCmd!=0){
                    bar.setProgress((int)rxCmd);
                }*/

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            } else {
                break;
            }
        }

    }

}

package com.example.android.digidoor_gate;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.android.digidoor_gate.util.SystemUiHider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ViewFlipper viewFlipper;

    private static final int LOCK = 2;
    private static final int UNLOCK = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static int REQUEST_CODE = 301;
    public static boolean END_CALL = false;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000;

    //GET Request URL for status of remote unlock.
    private String urlRemoteUnlock ="http://digidoor.herokuapp.com/api/v1/unlocks.json";
    //GET Request URL which defines the pin.
    private String urlOwners ="http://digidoor.herokuapp.com/api/v1/owners.json";
    public static List<String> pinList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(getApplicationContext(),
                "Booted Up", Toast.LENGTH_SHORT).show();

        setContentView(R.layout.activity_fullscreen2);

        viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);

        // Hides the Action Bar.
        getActionBar().hide();
        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);


        requestPin(urlOwners);

        //Invoke NumbPad fragment to prompt for pin.
        setupNumbpad();
        //setupScheduledAccess();

        Button button = (Button) findViewById(R.id.button_lock);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendCommand(LOCK);
                viewFlipper.showPrevious();

                Toast.makeText(getApplicationContext(),
                        "Gate is now locked.", Toast.LENGTH_SHORT).show();

                Intent mStartActivity = new Intent(getApplicationContext(), FullscreenActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(
                        getApplicationContext(), mPendingIntentId,
                        mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(
                        getApplicationContext().ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            }
        });

        //Initialize thread to query lock status for remote unlocking.
        remoteUnlockThread();

        //*********** BLUETOOTH onCreate start*****************************************************

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        //*********** BLUETOOTH onCreate end*******************************************************

    }

    @Override
    public void onResume() {
        super.onResume();

        if(END_CALL){
            Toast.makeText(getApplicationContext(),
                    "END CALL LALALALALALALALALALLALALALALALALA", Toast.LENGTH_SHORT).show();

            END_CALL = false;
            Intent mStartActivity = new Intent(this, FullscreenActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId,
                    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager)this.getSystemService(this.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }

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
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        scanLeDevice(true);
    }


    //*********** BLUETOOTH implementation start***************************************************
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast.makeText(getApplicationContext(),
                "Request Code: " + Integer.toString(requestCode), Toast.LENGTH_LONG).show();
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        //mLeDeviceListAdapter.clear();
    }


    Runnable runnableStop = new Runnable() {
        @Override
        public void run() {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            invalidateOptionsMenu();
            mHandler.postDelayed(runnableStart, 500);
        }
    };

    Runnable runnableStart = new Runnable() {
        @Override
        public void run() {
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            invalidateOptionsMenu();
            mHandler.postDelayed(runnableStop, SCAN_PERIOD);
        }
    };

    private void scanLeDevice(final boolean enable) {

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(runnableStart, SCAN_PERIOD);
            mScanning = true;
            //mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (scanRecord[28] == (byte) 0xAF) {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                if (rssi > -65&&rssi != 0) {
                                    sendCommand(UNLOCK);
                                } else sendCommand(LOCK);

                            }
                        }
                    });
                }
            };


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

                usbDeviceConnection.bulkTransfer(endpointOut,message, message.length, 500);
            }
        }
    }
    //*********** BLUETOOTH implementation end*****************************************************


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    /***
     * This method invokes the Numbpad Dialog on top of the main activity.
     */
    private void setupNumbpad() {

        //creates a delay before toasting the pin out to compensate for GET request time delay.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(pinList.size() > 0){
                    Toast.makeText(getApplicationContext(),
                            "Pin of 1st Owner: " + pinList.get(0), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            "Pin of 1st Owner: pinList empty", Toast.LENGTH_SHORT).show();
                }

            }
        }, 2000);

        // create an instance of NumbPad
        NumbPad np = new NumbPad();

        // optionally set additional title
        np.setAdditionalText("Please Enter Pin:");

        // show the NumbPad to capture input.
        np.show(this, "SAMSUNG DIGIDOOR", NumbPad.HIDE_INPUT,
                new NumbPad.numbPadInterface() {

                    // This is called when the user click the 'unlock' button on the dialog
                    // value is the captured input from the dialog.
                    public String numPadInputValue(String value) {

                        boolean pinValid = false;
                        String usedPin = "";

                        for (String pin : pinList) {
                            if (value.equals(pin)) {
                                pinValid = true;
                                usedPin = value;
                            }
                        }

                        if (pinValid) {
                            viewFlipper.showNext();

                            Toast.makeText(getApplicationContext(),
                                    "Pin: " + usedPin + ". Pin is correct, please enter.", Toast.LENGTH_SHORT).show();

                            //Sends signal through USB to Arduino to unlock gate
                            sendCommand(UNLOCK);
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


    private void remoteUnlockThread(){
        Runnable remoteUnlockRunnable = new Runnable() {
            public void run() {
                requestRemoteStatus(urlRemoteUnlock);

            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(remoteUnlockRunnable, 0, 10, TimeUnit.SECONDS);
    }



    /***
     * This method takes in the HTTP address and performs a GET request
     * to retrieve the pin from the server database.
     * @param uri
     * GET Request URL which defines the pin.
     */
    private void requestPin(String uri){

        JsonArrayRequest request = new JsonArrayRequest(uri,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        /*Toast.makeText(getApplicationContext(),
                                "Response is: "+ response, Toast.LENGTH_LONG).show();*/

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject object = response.getJSONObject(i);
                                pinList.add(Integer.toString(object.getInt("pin")));

                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(),
                                        "JSONException", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        Toast.makeText(getApplicationContext(),
                                "Volley Error!", Toast.LENGTH_SHORT).show();
                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    /***
     * This method takes in the HTTP address and performs a GET request
     * to retrieve the status of the remote unlocking faeture
     * @param uri
     * GET Request URL which defines the pin.
     */
    private void requestRemoteStatus(String uri){

        JsonArrayRequest request = new JsonArrayRequest(uri,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject object = response.getJSONObject(i);

                                /*Toast.makeText(getApplicationContext(),
                                        "Status of remote unlock: " + Boolean.toString(
                                                object.getBoolean("status")),
                                        Toast.LENGTH_SHORT).show();*/

                                if (object.getBoolean("status") == false){
                                    break;
                                }else{
                                    sendCommand(UNLOCK);
                                }

                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(),
                                        "JSONException", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError ex) {
                Toast.makeText(getApplicationContext(),
                        "Volley Error!", Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}


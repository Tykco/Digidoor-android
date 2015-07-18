package com.example.android.digidoor_gate;

import android.app.Activity;
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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.android.digidoor_gate.util.SystemUiHider;

import org.json.*;

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

    private static final int LOCK = 2;
    private static final int UNLOCK = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000;
    public static String pinStr = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen2);

        //final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Hides the Action Bar.
        getActionBar().hide();
        usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        String url ="http://digidoor.herokuapp.com/api/v1/owners.json";
        requestData(url);

        //Invoke NumbPad.
        setupNumbpad();


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
            return;
        }
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
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        //mLeDeviceListAdapter = new LeDeviceListAdapter();
        //setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

    //public String pin = "";
    private void setupNumbpad() {

        //new HttpGet().execute("http://digidoor.herokuapp.com/api/v1/owners");

        //final String pinStr = getPin();
        Toast.makeText(getApplicationContext(),
                "Print final pinStr: " + pinStr, Toast.LENGTH_SHORT).show();

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
                        if (value.equals(pinStr)) {
                            // do something here
                            Toast.makeText(getApplicationContext(),
                                    "Pin: " + pinStr + ". Pin is correct, please enter.", Toast.LENGTH_SHORT).show();
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

    /*public String getPin() {
        String pinStr = "";
        String str = callURL("http://digidoor.herokuapp.com/api/v1/owners");
        JSONArray aryJSONStrings;
        try {
            aryJSONStrings = new JSONArray(str);
            for (int i = 0; i < aryJSONStrings.length(); i++) {
                int pin = aryJSONStrings.getJSONObject(i).getInt("pin");
                pinStr = Integer.toString(pin);
                Toast.makeText(getApplicationContext(),
                        "getPin: " + pinStr, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pinStr;
    }

    public static String callURL(String myURL) {
        //System.out.println("Requested URL:" + myURL);
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = url.openConnection();
            if (urlConn != null)
                urlConn.setReadTimeout(60 * 1000);
            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader != null) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
            }
            in.close();
        } catch (Exception e) {
            throw new RuntimeException("Exception while calling URL:"+ myURL, e);
        }
        return sb.toString();
    }*/

    /***
     * This method takes in the HTTP address and performs a GET request
     * to retrieve the pin from the server database.
     * @param uri
     */
    private void requestData(String uri){

        StringRequest request = new StringRequest(uri,

                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        /*Toast.makeText(getApplicationContext(),
                                "Response is: "+ response, Toast.LENGTH_LONG).show();*/

                        try {
                            JSONArray array = new JSONArray(response);
                            for (int i = 0; i < array.length(); i++) {
                                int pinNumber = array.getJSONObject(i).getInt("pin");
                                pinStr = Integer.toString(pinNumber);
                            }
                            /*Toast.makeText(getApplicationContext(),
                                    "pinStr is: "+ pinStr, Toast.LENGTH_LONG).show();*/
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(),
                                    "JSONException", Toast.LENGTH_SHORT).show();
                        }
                    }
                },

                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        Toast.makeText(getApplicationContext(),
                                "Volley Error!", Toast.LENGTH_SHORT).show();
                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
;    }

}

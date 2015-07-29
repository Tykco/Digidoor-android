package com.example.android.digidoor_gate;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CallOwnersDialog {

    static TextView callOwnerPrompt;
    static Button btnBack;
    private String additional_text = "";
    private String ownerPhoneNumber = "";

    // Scheduled users json url
    private static final String urlOwners ="http://digidoor.herokuapp.com/api/v1/owners.json";

    private ProgressDialog pDialog;
    private List<User> ownersList = new ArrayList<>();
    private OwnersListAdapter adapter;
    TelephonyManager manager;
    StatePhoneReceiver myPhoneStateListener;

    public void setAdditionalText(String inTxt) {
        additional_text = inTxt;
    }

    public void showDialog(final Activity activity) {

        final Dialog saDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        // Inflate the dialog layout
        LayoutInflater inflater = activity.getLayoutInflater();
        final View saView = inflater.inflate(R.layout.list_view_dialog, null, false);

        // create code to handle the change tender
        callOwnerPrompt = (TextView) saView.findViewById(R.id.promptSAText);
        callOwnerPrompt.setText(additional_text);
        if (additional_text.equals("")) {
            callOwnerPrompt.setVisibility(View.GONE);
        }
        Typeface typeface = Typeface.createFromAsset(activity.getAssets(), "fonts/Gotham-light.ttf");

        btnBack = (Button) saView.findViewById(R.id.buttonBack);

        btnBack.setTypeface(typeface);

        btnBack.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                saDialog.dismiss();
            }
        });


        saDialog.setContentView(saView);

        ListView listView = (ListView) saDialog.findViewById(R.id.listView);
        adapter = new OwnersListAdapter(activity, ownersList);
        listView.setAdapter(adapter);

        /***
         * Call specific owner.
         */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //established the specific owner clicked on in the list
                Object object = adapter.getItem(position);
                User owner = (User) object;
                ownerPhoneNumber = "+65" + Integer.toString(owner.getPhoneNumber());

                //To be notified of changes of the phone state create an instance
                //of the TelephonyManager class and the StatePhoneReceiver class
                myPhoneStateListener = new StatePhoneReceiver(activity);
                manager = ((TelephonyManager) activity.getSystemService(activity.TELEPHONY_SERVICE));

                manager.listen(myPhoneStateListener,
                        PhoneStateListener.LISTEN_CALL_STATE); // start listening to the phone changes
                StatePhoneReceiver.callFromApp=true;

                Intent callIntent = new Intent(android.content.Intent.ACTION_CALL,
                        Uri.parse("tel:" + ownerPhoneNumber)); // Make the call
                activity.startActivity(callIntent);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FullscreenActivity.END_CALL = true;
                    }
                }, 10000);
            }
        });

        pDialog = new ProgressDialog(activity);
        // Showing progress dialog before making http request
        pDialog.setMessage("Loading...");



        // Creating volley request obj
        JsonArrayRequest ownersReq = new JsonArrayRequest(urlOwners,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        hidePDialog();

                        // Parsing json
                        for (int i = 0; i < response.length(); i++) {
                            try {

                                JSONObject obj = response.getJSONObject(i);
                                User ownersUser = new User();

                                ownersUser.setName(obj.getString("name"));
                                ownersUser.setPhoneNumber(obj.getInt("phoneno"));

                                // adding owners to owners array
                                ownersList.add(ownersUser);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        // notifying list adapter about data changes
                        // so that it renders the list view with updated data
                        adapter.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hidePDialog();
            }
        });

        // Adding request to request queue
        RequestQueue queue = Volley.newRequestQueue(activity);
        queue.add(ownersReq);


        WindowManager wm = (WindowManager) activity.getSystemService(activity.WINDOW_SERVICE); // for activity use context instead of getActivity()
        Display display = wm.getDefaultDisplay(); // getting the screen size of device
        Point size = new Point();
        display.getSize(size);
        int width = size.x;  // Set your heights
        int height = size.y; // set your widths

        WindowManager.LayoutParams saLp = new WindowManager.LayoutParams();
        saLp.copyFrom(saDialog.getWindow().getAttributes());
        saLp.width = width-20;
        saLp.height = height-200;

        saDialog.getWindow().setAttributes(saLp);
        saDialog.show();
        pDialog.show();
    }

    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }
}

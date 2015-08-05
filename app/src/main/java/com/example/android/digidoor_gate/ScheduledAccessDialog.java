package com.example.android.digidoor_gate;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class ScheduledAccessDialog {

    static TextView saPrompt;
    static Button btnBack;
    private String additional_text = "";


    // Scheduled users json url
    private static final String url = "http://digidoor.herokuapp.com/api/v1/scheduled_accesses.json";

    private ProgressDialog pDialog;
    private List<User> userList = new ArrayList<>();
    private ScheduledListAdapter adapter;
    private String numberToSmsUser;
    private String numberToSmsOwner = "+6583102429";
    private String messageToSmsUser = ", welcome! Please use this one-time pin to enter: ";
    private String messageToSmsOwner = "Hi! The scheduled user: ";

    public void setAdditionalText(String inTxt) {
        additional_text = inTxt;
    }

    public void showDialog(final Activity activity) {

        final Dialog saDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        // Inflate the dialog layout
        LayoutInflater inflater = activity.getLayoutInflater();
        final View saView = inflater.inflate(R.layout.list_view_dialog, null, false);

        // create code to handle the change tender
        saPrompt = (TextView) saView.findViewById(R.id.promptSAText);
        saPrompt.setText(additional_text);
        if (additional_text.equals("")) {
            saPrompt.setVisibility(View.GONE);
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
        adapter = new ScheduledListAdapter(activity, userList);
        listView.setAdapter(adapter);

        /***
         * Send scheduled user his/her temp pin via SMS.
         */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Object object = adapter.getItem(position);
                User scheduledUser = (User) object;
                Toast.makeText(activity,
                        "Pressed on: " + scheduledUser.getName(), Toast.LENGTH_LONG).show();
                numberToSmsUser = "+65" + Integer.toString(scheduledUser.getPhoneNumber());

                //send pin to scheduled user
                messageToSmsUser = "Hi " + scheduledUser.getName() + messageToSmsUser + Integer.toString(scheduledUser.getPin());
                sendSmsScheduledUser(activity, numberToSmsUser, messageToSmsUser, scheduledUser.getName());

                //notify owner of scheduled user entering home
                messageToSmsOwner = messageToSmsOwner + scheduledUser.getName()
                        + " has just unlocked and entered your home. Have a nice day! :)";
                sendSmsOwner(activity, numberToSmsOwner, messageToSmsOwner);

                FullscreenActivity.pinList.add(Integer.toString(scheduledUser.getPin()));

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        saDialog.dismiss();
                    }
                }, 6000);
            }
        });

        pDialog = new ProgressDialog(activity);
        // Showing progress dialog before making http request
        pDialog.setMessage("Loading...");



        // Creating volley request obj
        JsonArrayRequest userReq = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        hidePDialog();

                        // Parsing json
                        for (int i = 0; i < response.length(); i++) {
                            try {

                                JSONObject obj = response.getJSONObject(i);
                                User scheduledUser = new User();

                                scheduledUser.setName(obj.getString("name"));
                                scheduledUser.setPhoneNumber(obj.getInt("phoneno"));
                                scheduledUser.setPin(obj.getInt("pin"));
                                //scheduledUser.setThumbnailUrl(obj.getString("image"));

                                // adding scheduled users to scheduled users array
                                userList.add(scheduledUser);

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
        //JsonController.getInstance().addToRequestQueue(userReq);

        RequestQueue queue = Volley.newRequestQueue(activity);
        queue.add(userReq);


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

    /***
     * Sends SMS to scheduled user with specific message and pin.
     * @param activity
     * @param numberToSms
     * @param messageToSms
     * @param user
     */
    private void sendSmsScheduledUser(Activity activity, String numberToSms, String messageToSms, String user){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(numberToSms, null, messageToSms, null, null);
        Toast.makeText(activity,
                "Pin sent to: " + user + ".", Toast.LENGTH_LONG).show();
    }

    /***
     * Sends SMS to owner notifying one-time-pin sent to scheduled user.
     * @param activity
     * @param numberToSms
     * @param messageToSms
     */
    private void sendSmsOwner(Activity activity, String numberToSms, String messageToSms){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(numberToSms, null, messageToSms, null, null);

    }
}

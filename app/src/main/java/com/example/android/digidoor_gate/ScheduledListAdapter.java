package com.example.android.digidoor_gate;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ScheduledListAdapter extends BaseAdapter {

    private Activity activity;
    private LayoutInflater inflater;
    private List<User> scheduledUserItems;

    public ScheduledListAdapter(Activity activity, List<User> scheduledUserItems){
        this.activity = activity;
        this.scheduledUserItems = scheduledUserItems;
    }


    @Override
    public int getCount() {
        return scheduledUserItems.size();
    }

    @Override
    public Object getItem(int location) {
        return scheduledUserItems.get(location);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (inflater == null)
            inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.list_row, null);

        TextView name = (TextView) convertView.findViewById(R.id.name);
        //TextView phoneNumber = (TextView) convertView.findViewById(R.id.phone_number);

        // getting user data for the row
        User su = scheduledUserItems.get(position);

        // name
        name.setText("I'm " + su.getName() + ".");

        // phone number
        //phoneNumber.setText("Phone No.: " + String.valueOf(su.getPhoneNumber()));


        return convertView;
    }
}

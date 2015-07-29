package com.example.android.digidoor_gate;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class OwnersListAdapter extends BaseAdapter {

    private Activity activity;
    private LayoutInflater inflater;
    private List<User> ownersUserItems;

    public OwnersListAdapter(Activity activity, List<User> ownersUserItems){
        this.activity = activity;
        this.ownersUserItems = ownersUserItems;
    }


    @Override
    public int getCount() {
        return ownersUserItems.size();
    }

    @Override
    public Object getItem(int location) {
        return ownersUserItems.get(location);
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
        User owner = ownersUserItems.get(position);

        // name
        name.setText(owner.getName());

        // phone number
        //phoneNumber.setText("Phone No.: " + String.valueOf(owner.getPhoneNumber()));


        return convertView;
    }
}

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
    private List<ScheduledUser> userItems;

    public ScheduledListAdapter(Activity activity, List<ScheduledUser> userItems){
        this.activity = activity;
        this.userItems = userItems;
    }


    @Override
    public int getCount() {
        return userItems.size();
    }

    @Override
    public Object getItem(int location) {
        return userItems.get(location);
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
            convertView = inflater.inflate(R.layout.scheduled_row, null);

        /*if (imageLoader == null)
            imageLoader = AppController.getInstance().getImageLoader();
        NetworkImageView thumbNail = (NetworkImageView) convertView
                .findViewById(R.id.thumbnail);*/
        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView phoneNumber = (TextView) convertView.findViewById(R.id.phone_number);

        // getting user data for the row
        ScheduledUser su = userItems.get(position);

        /*// thumbnail image
        thumbNail.setImageUrl(m.getThumbnailUrl(), imageLoader);*/

        // name
        name.setText("I'm " + su.getName() + ".");

        // phone number
        phoneNumber.setText("Phone No.: " + String.valueOf(su.getPhoneNumber()));

        /*// genre
        String genreStr = "";
        for (String str : m.getGenre()) {
            genreStr += str + ", ";
        }
        genreStr = genreStr.length() > 0 ? genreStr.substring(0,
                genreStr.length() - 2) : genreStr;
        genre.setText(genreStr);

        // release year
        year.setText(String.valueOf(m.getYear()));*/

        return convertView;
    }
}

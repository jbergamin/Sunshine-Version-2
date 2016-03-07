package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * Created by jake on 3/6/16.
 * For use with the drawer in MainActivity.  The drawer displays locations.
 */
public class DrawerLocationsAdapter extends CursorAdapter {

    public DrawerLocationsAdapter(Context context, Cursor cursor, int flags){
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_location, parent, false);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor){
        TextView cityTextView = (TextView) view.findViewById(R.id.city_name_textview);
        String name = cursor.getString(MainActivity.COL_LOCATION_CITY_NAME);
        cityTextView.setText(name);
    }
}

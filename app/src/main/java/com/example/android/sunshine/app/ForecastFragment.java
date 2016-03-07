package com.example.android.sunshine.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.sunshine.app.data.WeatherContract;

import java.util.List;

/**
 * A fragment to display the weather forecast.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private ForecastAdapter mForecastAdapter;
    private ListView mForecastListView;

    // Id for cursor loader.  This can be any number, doesn't matter.
    private final int CURSOR_LOADER_ID = 48;
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    // Key for saving the position in savedInstanceState.
    private final String STATE_POSITION = "position";

    private Callback mCallback;
    private int mPosition = -1;

    private boolean mUseTodayView = true;

    public interface Callback {
        void onItemSelected(Uri dateUri);
    }

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    public ForecastFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        setHasOptionsMenu(true);

    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        Activity activity = getActivity();
        try {
            mCallback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        if(mPosition != ListView.INVALID_POSITION) {
            outState.putInt(STATE_POSITION, mPosition);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle action bar item clicks here
        int id = item.getItemId();
        if(id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ForecastAdapter to populate listView with data from a cursor
        mForecastAdapter = new ForecastAdapter(getContext(), null, 0);
        mForecastAdapter.setUseTodayView(mUseTodayView);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // get reference to ListView
        mForecastListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        // set adapter to one we just made
        mForecastListView.setAdapter(mForecastAdapter);

        mForecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                String locationSetting = Utility.getPreferredLocation(getActivity());
                if (cursor != null) {
                    Uri dateUri = WeatherContract.WeatherEntry
                            .buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE));
                    mCallback.onItemSelected(dateUri);
                }

                // store position in member variable
                mPosition = position;
            }
        });


        // restore scroll position if necessary
        if(savedInstanceState != null && savedInstanceState.containsKey(STATE_POSITION)) {
            mPosition = savedInstanceState.getInt(STATE_POSITION);
        }

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getContext());
        Uri uri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithStartDate(locationSetting,
                        System.currentTimeMillis());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        return new CursorLoader(getContext(), uri, FORECAST_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        mForecastAdapter.swapCursor(data);

        // restore selection if in two-pane mode
        if(mPosition != ListView.INVALID_POSITION && !mUseTodayView){
            // restore scroll position
            mForecastListView.setItemChecked(mPosition, true);
            mForecastListView.setSelection(mPosition);
        } else {
            // select the first item if in two-pane mode.
            // we know we are in two-pane mode if we are not using the special today view
            if(!mUseTodayView){
                mForecastListView.post(new Runnable() {
                    @Override
                    public void run() {
                        mForecastListView.setItemChecked(0, true);
                        mForecastListView.setSelection(0);
                        // call performItemClick() so the details fragment will be created
                        int position = mForecastListView.getCheckedItemPosition();
                        mForecastListView.performItemClick(mForecastListView.getAdapter().getView(position, null, null), 0, 0);
                    }
                });

            }

        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mForecastAdapter.swapCursor(null);
    }

    /**
     * Helper method that updates ListView with weather data from query
     */
    private void updateWeather(){
        // execute FetchWeatherTask
        String location = Utility.getPreferredLocation(getContext());
        new FetchWeatherTask(getContext()).execute(location);
        Log.v(LOG_TAG, "updateWeather() called");
    }

    public void onLocationChanged(){
        updateWeather();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void setUseTodayView(boolean useTodayView){
        mUseTodayView = useTodayView;

        if(mForecastAdapter != null){
            mForecastAdapter.setUseTodayView(useTodayView);
        }
    }
}

package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = DetailFragment.class.getSimpleName();
    private final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
    private ShareActionProvider mShareActionProvider;

    private final int DETAIL_LOADER_ID = 0;

    public static final String DETAIL_URI = "uriStr";

    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // these constants correspond to the projection defined above, and must change if the
    // projection changes
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    private static final int COL_WEATHER_HUMIDITY = 5;
    private static final int COL_WEATHER_WIND_SPEED = 6;
    private static final int COL_WEATHER_DEGREES = 7;
    private static final int COL_WEATHER_PRESSURE = 8;
    private static final int COL_WEATHER_CONDITION_ID = 9;

    // children views
    TextView mDayView;
    TextView mDateView;
    TextView mHighView;
    TextView mLowView;
    ImageView mIconView;
    TextView mDescriptionView;
    TextView mHumidityView;
    TextView mWindView;
    TextView mPressureView;

    private Uri mUri;


    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle args = getArguments();
        if (args != null){
            mUri = (Uri) args.getParcelable(DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        // children views
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDayView = (TextView) rootView.findViewById(R.id.detail_day_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);
        // get share menu item and share action provider
        MenuItem shareItem = menu.findItem(R.id.action_share);
        // get the ShareActionProvider so we can set share intent
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        // TODO
        // set intent
        if(true) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());

        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        if(mUri == null){
            return null;
        }

        CursorLoader loader = new CursorLoader(getContext(), mUri, FORECAST_COLUMNS,
                null, null, null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(!data.moveToFirst()){
            return;
        }

        // get data from cursor
        int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);  // so we know which icon to display
        long date = data.getLong(COL_WEATHER_DATE);
        String dayStr = Utility.getDayName(getContext(), date);
        String dateStr = Utility.getFormattedMonthDay(getContext(), date);
        String weatherDescription = data.getString(COL_WEATHER_DESC);
        boolean isMetric = Utility.isMetric(this.getContext());
        String high = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
        String low = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
        float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
        float direction = data.getFloat(COL_WEATHER_DEGREES);
        float pressure = data.getFloat(COL_WEATHER_PRESSURE);

        // insert data into views
        mDayView.setText(dayStr);
        mDateView.setText(dateStr);
        mHighView.setText(high);
        mLowView.setText(low);

        mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
        mDescriptionView.setText(weatherDescription);
        mHumidityView.setText(getString(R.string.format_humidity, humidity));
        mWindView.setText(Utility.getFormattedWind(getContext(), windSpeed, direction));
        mPressureView.setText(getString(R.string.format_pressure, pressure));


        // update the Share Intent
        if(mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void onLocationChanged(String newLocation){
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
        }

    }

    /**
     * Helper method that creates the share intent for the action provider
     * @return Share intent for ShareActionProvider
     */
    private Intent createShareForecastIntent(){
        // TODO
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }
}

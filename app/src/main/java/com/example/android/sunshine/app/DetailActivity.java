package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.ShareActionProvider;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

public class DetailActivity extends ActionBarActivity {

    private final String LOG_TAG = DetailActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private final String LOG_TAG = DetailFragment.class.getSimpleName();
        private final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
        private String mForecastStr;
        private ShareActionProvider mShareActionProvider;

        private final int DETAIL_LOADER_ID = 0;

        private static final String[] FORECAST_COLUMNS = {
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATE,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
        };

        // these constants correspond to the projection defined above, and must change if the
        // projection changes
        private static final int COL_WEATHER_ID = 0;
        private static final int COL_WEATHER_DATE = 1;
        private static final int COL_WEATHER_DESC = 2;
        private static final int COL_WEATHER_MAX_TEMP = 3;
        private static final int COL_WEATHER_MIN_TEMP = 4;

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

            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.detailfragment, menu);
            // get share menu item and share action provider
            MenuItem shareItem = menu.findItem(R.id.action_share);
            // get the ShareActionProvider so we can set share intent
            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
            // set intent
            if(mForecastStr != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
                Log.v(LOG_TAG, "Set share intent");
            }
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args){
            Intent intent = getActivity().getIntent();
            if(intent == null){
                return null;
            }

            CursorLoader loader = new CursorLoader(getContext(), intent.getData(), FORECAST_COLUMNS,
                    null, null, null);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if(!data.moveToFirst()){
                return;
            }

            String dateString = Utility.formatDate(data.getLong(COL_WEATHER_DATE));
            String weatherDescription = data.getString(COL_WEATHER_DESC);
            boolean isMetric = Utility.isMetric(this.getContext());
            String high = Utility.formatTemperature(data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            String low = Utility.formatTemperature(data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
            mForecastStr = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);

            TextView textView = (TextView) getView().findViewById(R.id.detail_text);
            textView.setText(mForecastStr);

            // update the Share Intent
            if(mShareActionProvider != null){
                mShareActionProvider.setShareIntent(createShareForecastIntent());
                Log.v(LOG_TAG, "Set share intent");
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }

        /**
         * Helper method that creates the share intent for the action provider
         * @return Share intent for ShareActionProvider
         */
        private Intent createShareForecastIntent(){
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
            return shareIntent;
        }
    }

}

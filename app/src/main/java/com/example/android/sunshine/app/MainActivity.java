package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private String mLocation;
    final String DETAILFRAGMENT_TAG = "DFTAG";
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(findViewById(R.id.weather_detail_container) != null){
            // The detail container will only be present in large-screen layouts.
            // If this view is present then we are in two-pane mode.
            mTwoPane = true;

            // Show the detail view in this activity by adding or replacing the detail fragment
            // using a fragment transaction.
            if(savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            // The weather detail container is not present
            // so we are not in two-pane mode.
            mTwoPane = false;
        }

        mLocation = Utility.getPreferredLocation(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        } else if (id == R.id.action_view_location) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        if(!Utility.getPreferredLocation(this).equals(mLocation)){
            mLocation = Utility.getPreferredLocation(this);

            ForecastFragment ff =
                    (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            ff.onLocationChanged();

            DetailFragment df =
                    (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if(df != null){
                df.onLocationChanged(mLocation);
            }
            Log.v(LOG_TAG, "Updating location");
        }

        super.onResume();
    }

    public void onItemSelected(Uri dateUri){
        if(mTwoPane){
            // In two-pane mode.  Show the detail view in this
            // activity by adding or replacing the detail fragment.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, dateUri);

            DetailFragment f = new DetailFragment();
            f.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, f, DETAILFRAGMENT_TAG)
                    .commit();

        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.setData(dateUri);
            startActivity(intent);
        }
    }

    private void openPreferredLocationInMap(){
        // create implicit intent to open map app to display the current user's
        // preferred location on map

        // get location from preferences
        String locationStr = Utility.getPreferredLocation(this);
        Uri locationUri = Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q", locationStr)
                .build();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(locationUri);

        if(intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        } else{
            Log.d(LOG_TAG, "Couldn't call " + locationStr + ", no application installed to open and display location.");
        }
    }

}

package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private String mLocation;
    final String FORECASTFRAGMENT_TAG = "forecastfragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment(), FORECASTFRAGMENT_TAG)
                    .commit();
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
            ForecastFragment ff =
                    (ForecastFragment) getSupportFragmentManager().findFragmentByTag(FORECASTFRAGMENT_TAG);
            ff.onLocationChanged();
            mLocation = Utility.getPreferredLocation(this);
            Log.v(LOG_TAG, "Updating location");
        }

        super.onResume();
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

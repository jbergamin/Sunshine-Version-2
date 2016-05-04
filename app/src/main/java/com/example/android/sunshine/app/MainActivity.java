package com.example.android.sunshine.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.sunshine.app.data.WeatherContract;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback,
        LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private String mLocation;
    final String DETAILFRAGMENT_TAG = "DFTAG";
    private boolean mTwoPane;

    // drawer layout stuff
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private int LOCATION_LOADER_ID = 48;

    private DrawerLocationsAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    // columns for the location loader
    private static final String[] LOCATION_COLUMNS = {
            WeatherContract.LocationEntry.TABLE_NAME + "." + WeatherContract.LocationEntry._ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.LocationEntry.COLUMN_CITY_NAME
    };
    // these indices are tied to the LOCATION_COLUMNS
    static final int COL_LOCATION_ID = 0;
    static final int COL_LOCATION_SETTING = 1;
    static final int COL_LOCATION_CITY_NAME = 2;


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
            getSupportActionBar().setElevation(0f);
        }

        // tell the adapter contained within the forecast fragment whether or
        // not we want to display the special today view
        ForecastFragment forecastFragment = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        forecastFragment.setUseTodayView(!mTwoPane);


        // set up navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerAdapter = new DrawerLocationsAdapter(this, null, 0);
        // add footer view that gives the option to add a location
        View footerView = LayoutInflater.from(this).inflate(R.layout.add_new_item, null);
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.add_location);

                // edit text view for zip code input
                final EditText editText = new EditText(v.getContext());
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setHint(R.string.zip_code);

                builder.setView(editText);
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // validate zip code
                        String zipCode = editText.getText().toString();
                        if(zipCode.length() == 5){
                            // we have a valid zip code
                            // Hooray! The user isn't an idiot
                            // now let's add that new location to the db and update everything
                            Utility.setPreferredLocation(getApplicationContext(), zipCode);
                            updateLocation();
                            restartLoader();
                        } else {
                            // the user doesn't know what a zip code is
                            // let's tell them they are stupid
                            Toast.makeText(getApplicationContext(), R.string.invalid_zip_message, Toast.LENGTH_SHORT)
                                    .show();
                        }

                    }
                });

                builder.show();

            }
        });
        mDrawerList.setFooterDividersEnabled(true);
        mDrawerList.addFooterView(footerView);

        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                String newLocationSetting = cursor.getString(COL_LOCATION_SETTING);
                Utility.setPreferredLocation(getApplicationContext(), newLocationSetting);
                updateLocation();
                restartLoader();

                // Highlight the selected item and close the drawer
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });

        // set click listener for long holds on location items
        // this gives the user the option to delete the location
        mDrawerList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, long id) {
                // dialog to delete location
                AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
                builder.setMessage(R.string.delete_message)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDrawerLayout.openDrawer(mDrawerList);

                                // delete location

                                // if there is only one location in db do not delete it
                                if (parent.getCount() <= 2) {
                                    dialog.cancel();
                                    dialog.dismiss();
                                    // notify the user
                                    Toast.makeText(getApplicationContext(),
                                            R.string.cannot_delete_last_message, Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // delete the selected location
                                String selectedLocation = ((Cursor) parent.getItemAtPosition(position)).getString(COL_LOCATION_SETTING);
                                String[] args = {selectedLocation};
                                getApplicationContext().getContentResolver().delete(WeatherContract.LocationEntry.CONTENT_URI,
                                        WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?", args);

                                restartLoader();


                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                dialog.dismiss();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

                return false;
            }
        });


        // code to display hamburger icon in corner
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // set up loader to load locations from db
        getSupportLoaderManager().initLoader(LOCATION_LOADER_ID, null, this);

        mLocation = Utility.getPreferredLocation(this);

        // TODO this is only temporary
        // fetch weather from internet and update everything
        updateLocation();
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

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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
            updateLocation();
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle){
        Uri uri = WeatherContract.LocationEntry.CONTENT_URI;

        return new CursorLoader(this, uri, LOCATION_COLUMNS, null, null, null);

    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data){
        mDrawerAdapter.swapCursor(data);

        // select current location
        if(!data.moveToFirst()){
            Log.e(LOG_TAG, "no locations in database");
            return;
        }

        do {
            String locCol = data.getString(COL_LOCATION_SETTING);
            if(locCol.equals(mLocation)){
                int position = data.getPosition();
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                return;
            }
        } while (data.moveToNext());

        // if we get to here without returning...
        // location setting doesn't match any location in db
        // so we set the location setting to the first location in the db
        Log.w(LOG_TAG, "location setting does not match any location in db");
        mDrawerList.clearChoices();

//        data.moveToFirst();
//        String newLocation = data.getString(COL_LOCATION_SETTING);
//        Utility.setPreferredLocation(this, newLocation);
//        // restart loader and update location
//        restartLoader();
//        updateLocation();
    }

    @Override
    public void onLoaderReset(Loader loader){
        mDrawerAdapter.swapCursor(null);
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

    private void updateLocation(){
        mLocation = Utility.getPreferredLocation(this);
        Log.v(LOG_TAG, "update location: " + mLocation);

        ForecastFragment ff =
                (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        ff.onLocationChanged();

        DetailFragment df =
                (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
        if(df != null){
            df.onLocationChanged(mLocation);
        }
    }

    private void restartLoader(){
        getSupportLoaderManager().restartLoader(LOCATION_LOADER_ID, null, this);
    }

}

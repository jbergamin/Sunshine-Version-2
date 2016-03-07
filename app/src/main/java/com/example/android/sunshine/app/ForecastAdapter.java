package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    private final String LOG_TAG = ForecastAdapter.class.getSimpleName();
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;

    private boolean mUseTodayView = true;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(mContext, high, isMetric) + "/" + Utility.formatTemperature(mContext, low, isMetric);

        return highLowStr;
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
     */
    private String convertCursorRowToUXFormat(Cursor cursor) {
        String highAndLow = formatHighLows(
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(ForecastFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }

    public void setUseTodayView(boolean useTodayView){
        mUseTodayView = useTodayView;
    }

    @Override
    public int getItemViewType(int position){
        return (position == 0 && mUseTodayView) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        if(viewType == VIEW_TYPE_TODAY){
            layoutId = R.layout.list_item_forecast_today;
        } else if(viewType == VIEW_TYPE_FUTURE_DAY) {
            layoutId = R.layout.list_item_forecast;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        // add a view holder to improve performance
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    /*low + ""
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // get view holder
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // read weather id from cursor
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        // set icon based on item type and weather ID
        int drawableId = -1;
        if(getItemViewType(cursor.getPosition()) == VIEW_TYPE_TODAY){
            drawableId = Utility.getArtResourceForWeatherCondition(weatherId);
        } else if (getItemViewType(cursor.getPosition()) == VIEW_TYPE_FUTURE_DAY){
            drawableId = Utility.getIconResourceForWeatherCondition(weatherId);
        }

        if(drawableId == -1){
            Log.e(LOG_TAG, "Cannot match weather ID to icon.  ID: " + weatherId + ".  Using ic_launcher instead");
            drawableId = R.mipmap.ic_launcher;
        }
        viewHolder.iconView.setImageResource(drawableId);

        // read date from cursor and get friendly day string
        // if in two-pane mode then only display the 'Today' for the today item, not the full date
        long date = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        String day = Utility.getDayName(context, date);
        if(!day.equals(Utility.getDayName(context, System.currentTimeMillis()))
                || getItemViewType(cursor.getPosition()) == VIEW_TYPE_TODAY){
            day = Utility.getFriendlyDayString(context, date);
        }
        viewHolder.dateView.setText(day);

        // read forecast and set forecast description in UI
        String forecaseStr = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(forecaseStr);

        // read high and low from cursor and set appropriate textViews
        float high = cursor.getFloat(ForecastFragment.COL_WEATHER_MAX_TEMP);
        float low = cursor.getFloat(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(mContext, high, Utility.isMetric(context)) + "");
        viewHolder.lowTempView.setText(Utility.formatTemperature(mContext, low, Utility.isMetric(context)) + "");


    }

    /**
     * A cache of children views.  We use this to remove costly findFiewById() calls.
     */
    static class ViewHolder {
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view){
            iconView = (ImageView) view.findViewById(R.id.detail_icon);
            dateView = (TextView) view.findViewById(R.id.detail_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.detail_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.detail_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.detail_low_textview);
        }
    }
}
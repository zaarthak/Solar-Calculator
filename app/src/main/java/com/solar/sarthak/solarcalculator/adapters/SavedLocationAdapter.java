package com.solar.sarthak.solarcalculator.adapters;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.solar.sarthak.solarcalculator.database.DatabaseHelper;
import com.solar.sarthak.solarcalculator.R;
import com.solar.sarthak.solarcalculator.models.Place;
import com.solar.sarthak.solarcalculator.utils.AlarmReceiver;
import com.solar.sarthak.solarcalculator.utils.SolarEventCalculator;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Array adapter for the saved location items to be displayed in alert dialog.
 */
public class SavedLocationAdapter extends ArrayAdapter<Place> {

    private Place place;
    private List<Place> placeList;

    private DatabaseHelper db;

    Context mContext;

    public SavedLocationAdapter(@NonNull Context context, int resource, List<Place> list) {
        super(context, resource, list);

        mContext = context;
        placeList = list;

        db = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {

            place = placeList.get(position);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_saved_location, null);

            TextView mLocationNameTv = convertView.findViewById(R.id.location_name_tv);
            final ImageView mFavoriteBtn = convertView.findViewById(R.id.favorite_iv);

            // set favorites icon
            if (Integer.parseInt(place.getFavorite()) == 0) {

                mFavoriteBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_empty_star));
            } else {

                mFavoriteBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_star));
            }

            mLocationNameTv.setText(place.getName());

            mFavoriteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    place = db.getPlace(Double.parseDouble(place.getLatitude()),
                            Double.parseDouble(place.getLongitude()));

                    // if 'favorite' in database is 0, add location to favorites.
                    if (Integer.parseInt(place.getFavorite()) == 0) {

                        db.updatePlace(Double.parseDouble(place.getLatitude()),
                                Double.parseDouble(place.getLongitude()), 1);
                        createNotification();
                        mFavoriteBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_star));
                        Toast.makeText(mContext, "Added to favorites.", Toast.LENGTH_SHORT).show();
                    }
                    // if 'favorite' in database is 1, remove location from favorites.
                    else {

                        db.updatePlace(Double.parseDouble(place.getLatitude()),
                                Double.parseDouble(place.getLongitude()), 0);
                        deleteNotification();
                        mFavoriteBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_empty_star));
                        Toast.makeText(mContext, "Removed from favorites.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        return convertView;
    }

    /**
     * Set alarm manager to create notification at the golden hour.
     */
    private void createNotification() {

        // for sunrise, time is 0.
        SolarEventCalculator sunriseCalculator = new SolarEventCalculator(mContext,
                0,
                new Date(),
                new LatLng(Double.parseDouble(place.getLatitude()),
                        Double.parseDouble(place.getLongitude())));

        double sunriseIst = sunriseCalculator.getIstTime();

        String[] sunriseArr = String.valueOf(sunriseIst).split("\\.");

        // for sunset, time is set to 1.
        SolarEventCalculator sunsetCalculator = new SolarEventCalculator(mContext,
                0,
                new Date(),
                new LatLng(Double.parseDouble(place.getLatitude()),
                        Double.parseDouble(place.getLongitude())));

        double sunsetIst = sunsetCalculator.getIstTime();

        String[] sunsetArr = String.valueOf(sunsetIst).split("\\.");

        Intent intent = new Intent(mContext, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        Calendar sunriseTime = Calendar.getInstance();
        sunriseTime.setTimeInMillis(System.currentTimeMillis());
        sunriseTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sunriseArr[0]) % 12);
        sunriseTime.set(Calendar.MINUTE, Integer.parseInt(sunriseArr[1].substring(0, 2)) % 60);
        sunriseTime.set(Calendar.SECOND, 0);

        // create alarm manager for sunrise
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                sunriseTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);

        Calendar sunsetTime = Calendar.getInstance();
        sunriseTime.setTimeInMillis(System.currentTimeMillis());
        sunriseTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sunsetArr[0]) % 12);
        sunriseTime.set(Calendar.MINUTE, Integer.parseInt(sunsetArr[1].substring(0, 2)) % 60);
        sunriseTime.set(Calendar.SECOND, 0);

        // create alarm manager for sunset
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                sunsetTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    /**
     * Cancel alarm manager if place is removed from favorites.
     */
    private void deleteNotification() {

        Intent intent = new Intent(mContext, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(pendingIntent);
    }
}

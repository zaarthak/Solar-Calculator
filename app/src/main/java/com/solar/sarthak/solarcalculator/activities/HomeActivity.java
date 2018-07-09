package com.solar.sarthak.solarcalculator.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.solar.sarthak.solarcalculator.database.DatabaseHelper;
import com.solar.sarthak.solarcalculator.R;
import com.solar.sarthak.solarcalculator.adapters.SavedLocationAdapter;
import com.solar.sarthak.solarcalculator.utils.SolarEventCalculator;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private Date selectedDate;
    private LatLng placeLatLng;

    private String locationName = "";

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int REQUEST_CHECK_SETTINGS = 100;
    private GoogleMap mMap;

    // The entry point to the Fused Place Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Place Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    PlaceAutocompleteFragment placeAutoComplete;

    TextView mDateTv;
    TextView mSunriseTv, mSunsetTv;

    ImageButton mPreviousMonthBtn, mPreviousDayBtn;
    ImageButton mTodayBtn;
    ImageButton mNextMonthBtn, mNextDayBtn;
    ImageButton mPinLocationBtn, mShowLocationBtn;

    ImageView mCloseLayoutBtn;

    LinearLayout mTimeLayout;

    private SolarEventCalculator sunriseCalculator;
    private SolarEventCalculator sunsetCalculator;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(HomeActivity.this);

        checkGPSConfig();

        selectedDate = new Date();

        sunriseCalculator = new SolarEventCalculator();
        sunsetCalculator = new SolarEventCalculator();

        db= new DatabaseHelper(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        placeAutoComplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete);
        placeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .title(place.getName().toString())
                        .position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                locationName = (String) place.getName();
                placeLatLng = place.getLatLng();

                // show time layout
                mTimeLayout.setVisibility(View.VISIBLE);

                // set saved location icon
                if (db.getPlace(placeLatLng.latitude, placeLatLng.longitude) != null) {

                    mPinLocationBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_star));
                } else {

                    mPinLocationBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_empty_star));
                }

                // get sunrise time
                sunriseCalculator = new SolarEventCalculator(HomeActivity.this, 0, selectedDate, placeLatLng);
                getSunriseTimeFromIst(sunriseCalculator.getIstTime());

                // get sunset time
                sunsetCalculator = new SolarEventCalculator(HomeActivity.this, 1, selectedDate, placeLatLng);
                getSunsetTimeFromIst(sunsetCalculator.getIstTime());

                mMap.animateCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
            }

            @Override
            public void onError(Status status) {
                Log.d("Maps", "An error occurred: " + status);
            }
        });

        mDateTv = findViewById(R.id.date_tv);
        mSunriseTv = findViewById(R.id.sunrise_tv);
        mSunsetTv = findViewById(R.id.sunset_tv);

        mPreviousMonthBtn = findViewById(R.id.previous_month_ib);
        mPreviousDayBtn = findViewById(R.id.previous_day_ib);
        mNextDayBtn = findViewById(R.id.next_day_ib);
        mTodayBtn = findViewById(R.id.today_ib);
        mNextMonthBtn = findViewById(R.id.next_month_ib);
        mPinLocationBtn = findViewById(R.id.pin_location_btn);
        mShowLocationBtn = findViewById(R.id.show_location_btn);
        mCloseLayoutBtn = findViewById(R.id.close_layout_btn);

        mTimeLayout = findViewById(R.id.time_layout);

        mDateTv.setOnClickListener(this);
        mPreviousMonthBtn.setOnClickListener(this);
        mPreviousDayBtn.setOnClickListener(this);
        mTodayBtn.setOnClickListener(this);
        mNextDayBtn.setOnClickListener(this);
        mNextMonthBtn.setOnClickListener(this);
        mPinLocationBtn.setOnClickListener(this);
        mShowLocationBtn.setOnClickListener(this);
        mCloseLayoutBtn.setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mMap != null) {

            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Place layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:

                mLocationPermissionGranted = grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED;

                getDeviceLocation();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.date_tv:

                showDatePickerDialog();
                break;

            case R.id.previous_month_ib:

                updateMonthToPrevious();
                break;

            case R.id.previous_day_ib:

                updateDayToPrevious();
                break;

            case R.id.today_ib:

                updateToToday();
                break;

            case R.id.next_day_ib:

                updateDayToNext();
                break;

            case R.id.next_month_ib:

                updateMonthToNext();
                break;

            case R.id.pin_location_btn:

                if (placeLatLng != null) {

                    // if location is not saved in database, save in database
                    if (db.getPlace(placeLatLng.latitude, placeLatLng.longitude) == null) {

                        saveLocationToDb();
                        mPinLocationBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_star));
                    }
                    // if location is saved in database, delete it from database
                    else {

                        db.deletePlace(placeLatLng.latitude, placeLatLng.longitude);
                        mPinLocationBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_empty_star));
                        Log.d("oil", db.getPlaceCount() + "");
                    }
                }
                break;

            case R.id.show_location_btn:

                showAllSavedLocation();
                break;

            case R.id.close_layout_btn:

                closeLayout();
                break;
        }
    }

    private void checkGPSConfig() {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(HomeActivity.this)
                    .addApi(LocationServices.API).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(10000 / 2);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            Log.i(TAG, "All location settings are satisfied.");
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i(TAG, "Place settings are not satisfied. Show the user a dialog to upgrade location settings ");

                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the result
                                // in onActivityResult().
                                status.startResolutionForResult(HomeActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            Log.i(TAG, "Place settings are inadequate, and cannot be fixed here. Dialog not created.");
                            break;
                    }
                }
            });
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    public void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {

            if (mLocationPermissionGranted) {

                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(HomeActivity.this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {

                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {

                                mMap.clear();
                                mMap.addMarker(new MarkerOptions()
                                        .title("Your location")
                                        .position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            } else {

                                getDeviceLocation();
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(HomeActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(HomeActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Show all user saved location in alert dialog
     */
    private void showAllSavedLocation() {

        final List<com.solar.sarthak.solarcalculator.models.Place> placeList;

        placeList = db.getAllPlace();

        // create alert dialog
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(HomeActivity.this);
        alertBuilder.setIcon(R.drawable.ic_bookmark);
        alertBuilder.setTitle("Saved locations");

        // create custom adapter for items in alert dialog
        SavedLocationAdapter arrayAdapter = new SavedLocationAdapter(HomeActivity.this,
                R.layout.item_saved_location, placeList);

        alertBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // move to the desired location when user clicks on the item in alert dialog
        alertBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {

                dialog.dismiss();

                locationName = placeList.get(position).getName();

                // get location latlng
                placeLatLng = new LatLng(Double.parseDouble(placeList.get(position).getLatitude()),
                        Double.parseDouble(placeList.get(position).getLongitude()));

                mMap.clear();

                mMap.addMarker(new MarkerOptions()
                        .title(placeList.get(position).getName())
                        .position(placeLatLng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                mMap.animateCamera(CameraUpdateFactory.newLatLng(placeLatLng));

                // show time layout
                mTimeLayout.setVisibility(View.VISIBLE);

                // get sunrise time
                sunriseCalculator = new SolarEventCalculator(HomeActivity.this, 0, selectedDate, placeLatLng);
                getSunriseTimeFromIst(sunriseCalculator.getIstTime());

                // get sunset time
                sunsetCalculator = new SolarEventCalculator(HomeActivity.this, 1, selectedDate, placeLatLng);
                getSunsetTimeFromIst(sunsetCalculator.getIstTime());
            }
        });

        alertBuilder.show();
    }

    /**
     * Save location to database
     */
    private void saveLocationToDb() {

        if (placeLatLng != null) {

            com.solar.sarthak.solarcalculator.models.Place newPlace =
                    new com.solar.sarthak.solarcalculator.models.Place(locationName,
                    String.valueOf(placeLatLng.latitude),
                    String.valueOf(placeLatLng.longitude),
                    String.valueOf(0));

            db.addPlace(newPlace);

            Toast.makeText(this, "Location saved.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show date picker dialog when click on date textView.
     */
    private void showDatePickerDialog() {

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        selectedDate = cal.getTime();

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(HomeActivity.this, date, year, month, day).show();
    }

    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {

            Calendar.getInstance().set(Calendar.YEAR, year);
            Calendar.getInstance().set(Calendar.MONTH, monthOfYear);
            Calendar.getInstance().set(Calendar.DAY_OF_MONTH, dayOfMonth);

            Calendar c = Calendar.getInstance();
            c.set(year, monthOfYear, dayOfMonth);
            selectedDate = c.getTime();

            updateLabel(year, monthOfYear, dayOfMonth);
        }
    };

    /**
     * Update date in textView.
     */
    private void updateLabel(int year, int monthOfYear, int dayOfMonth) {

        if (placeLatLng != null) {

            mTimeLayout.setVisibility(View.VISIBLE);

            sunriseCalculator = new SolarEventCalculator(HomeActivity.this, 0, selectedDate, placeLatLng);
            getSunriseTimeFromIst(sunriseCalculator.getIstTime());

            sunsetCalculator = new SolarEventCalculator(HomeActivity.this, 1, selectedDate, placeLatLng);
            getSunsetTimeFromIst(sunsetCalculator.getIstTime());
        }

        String monthName = getMonthName(monthOfYear);

        String date = String.format("%02d", dayOfMonth) + " "
                + monthName + " "
                + year;

        mDateTv.setText(date);
    }

    //-------------------------------------------------------------------------
    // Methods to increment/decrement date
    //-------------------------------------------------------------------------
    private void updateDayToPrevious() {

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.add(Calendar.DATE, -1);
        selectedDate = cal.getTime();

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        updateLabel(year, month, day);
    }

    private void updateDayToNext() {

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.add(Calendar.DATE, 1);
        selectedDate = cal.getTime();

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        updateLabel(year, month, day);
    }

    private void updateMonthToPrevious() {

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.add(Calendar.MONTH, -1);
        selectedDate = cal.getTime();

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        updateLabel(year, month, day);
    }

    private void updateMonthToNext() {

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.add(Calendar.MONTH, 1);
        selectedDate = cal.getTime();

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        updateLabel(year, month, day);
    }

    private void updateToToday() {

        selectedDate = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        updateLabel(year, month, day);
    }

    /**
     * Hide time layout.
     */
    private void closeLayout() {

        if (mTimeLayout.getVisibility() == View.VISIBLE) {

            mTimeLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Show sunrise time in textView.
     */
    private void getSunriseTimeFromIst(double ist) {

        String[] arr = String.valueOf(ist).split("\\.");

        Log.d("IST Time", arr[0] + " : "  + arr[1]);

        mSunriseTv.setText(String.format("%02d", Integer.parseInt(arr[0]) % 24)
                + " : "
                + String.format("%02d", Integer.parseInt(arr[1].substring(0, 2)) % 60));
    }

    /**
     * Show sunset time in textView.
     */
    private void getSunsetTimeFromIst(double ist) {

        String[] arr = String.valueOf(ist).split("\\.");

        Log.d("IST Time", arr[0] + " : "  + arr[1]);

        mSunsetTv.setText(String.format("%02d", Integer.parseInt(arr[0]) % 24)
                + " : "
                + String.format("%02d", Integer.parseInt(arr[1].substring(0, 2)) % 60));
    }

    public static String getMonthName(int month){

        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }
}


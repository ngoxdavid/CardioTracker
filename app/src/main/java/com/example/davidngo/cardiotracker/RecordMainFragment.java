package com.example.davidngo.cardiotracker;


/**
 * Created By: David Ngo
 */

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/* RecordMainFragment
    - Portrait Main Activity
    - Contains a button to the profile activity
    - Displays Real-time Calories Burnt, and Duration
    - Displays a GoogleMap that will trace a path of their workout (run)
    - Start/Stop Button
 */
public class RecordMainFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, LocationListener {

    private GoogleMap mMap;
    private Location mLastKnownLocation;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    boolean mLocationPermissionGranted;
    final public static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 18;
    int secs, mins, milliSecs;
    TextView durationTextView, distanceTextView;
    Button startButton;
    private DbHelper db_Helper;
    private boolean workoutStatus = false;
    private SharedPreferences mPrefs;
    private ArrayList<LatLng> points;
    private Polyline line;
    private static float stepCount = 0;
    private Context mContext;
    private LocationRequest locationRequest;
    private long FASTEST_INTERVAL = 2000;
    private long INTERVAL = 10000;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLocationPermission();

        //Initialize points array list that will keep track of our new locations during a workout
        points = new ArrayList<LatLng>();

        db_Helper = new DbHelper(getActivity());

        //Initialize total stats table for the first time (insert one row of 0's)
        if(db_Helper.getTotalStatsTblCount() == 0){
            db_Helper.initializeTotalStats();
        }

        // Insert Dummy Data into Database to show that calculating weekly averages is correct
        // This data will be shown in all-time stats but not in the past week (since it's from a year ago)
        if(db_Helper.getTotalSessionsCount() == 0){
            db_Helper.addSession("20160101", (long)10000, 2.12f, 212, 4000);
            db_Helper.updateTotalStats((long)10000, 2.12f, 212);

            db_Helper.addSession("20160101", (long)3124, 5.71f, 571, 10000);
            db_Helper.updateTotalStats((long)3124, 5.71f, 571);

            db_Helper.addSession("20160101", (long)156163, 10.15f, 1215, 12372);
            db_Helper.updateTotalStats((long)156163, 10.15f, 1215);

            db_Helper.addSession("20160101", (long)13030, 8f, 352, 3216);
            db_Helper.updateTotalStats((long)13030, 8f, 352);

            db_Helper.addSession("20160101", (long)256163, 21f, 2100, 7134);
            db_Helper.updateTotalStats((long)256163, 21f, 2100);
        }

        //Retain Instance for fragment lifecycle in orientation changes
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Register the Broadcast Receiver
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(MyFitnessService.BROADCAST_ACTION));

        //Check if the the user is still working out
        mPrefs = getActivity().getSharedPreferences("myPreferences", 0);
        workoutStatus = mPrefs.getBoolean("workingOutCheck", false);

        //Set Distance
        distanceTextView.setText(Float.toString(mPrefs.getFloat("lastRecentDistance", 0)));

        mContext = getActivity().getApplicationContext();
        // If the user is still working out, make sure the button is set to STOP
        if (workoutStatus) {
            startButton.setText("STOP WORKOUT");
            startButton.setBackgroundColor(Color.RED);
        } else {
            //If the user is not working out make sure the button is set to START
            startButton.setText("START WORKOUT");
            startButton.setBackgroundColor(Color.parseColor("#0057E7"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //Unregister Receiver to avoid leak
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //Create and inflate portrait fragment
        View view = inflater.inflate(R.layout.record_workout_main_layout, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        //Initialize Google Map Fragment
        final SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //I am using the FusedLocationProviderClient for this app to get location data
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        durationTextView = (TextView) getView().findViewById(R.id.durationValueTextView);
        distanceTextView = (TextView) getView().findViewById(R.id.distanceValueTextView);

        //Set Tag so that we can transform button from stop to start
        startButton = (Button) getView().findViewById(R.id.startButton);
        startButton.setTag(1);


        startLocationUpdates();

        // Start/Stop Button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int isWorkingOut = (Integer) view.getTag();

                //If the user is workingout, start the remote fitness service
                // set button to red and show stop working out button
                // reset distance to 0
                if (isWorkingOut == 1) {

                    mPrefs = getActivity().getSharedPreferences("myPreferences", 0);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putBoolean("workingOutCheck", true);
                    editor.commit();

                    distanceTextView.setText("0.0");

                    mPrefs = getActivity().getSharedPreferences("myPreferences", 0);

                    float totalSteps = mPrefs.getFloat("totalSteps", 0);
                    editor.putFloat("lastRecentSteps", totalSteps);
                    editor.commit();


                    startButton.setText("STOP WORKOUT");
                    startButton.setBackgroundColor(Color.RED);

                    //Reset Map and Points Array
                    mMap.clear();
                    points.clear();

                    getActivity().startService(new Intent(getActivity(), MyFitnessService.class));
                    view.setTag(0);
                } else {
                    //If the user is stops working out, save the session into the database
                    db_Helper = new DbHelper(getActivity());

                    startButton.setText("START WORKOUT");
                    startButton.setBackgroundColor(Color.parseColor("#0057E7"));

                    mPrefs = getActivity().getSharedPreferences("myPreferences", 0);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putBoolean("workingOutCheck", false);
                    editor.commit();

                    //Since the step counter cannot reset its total steps until the device reboots
                    //I must keep track of the new steps and total steps to get the current step count
                    float totalSteps = mPrefs.getFloat("totalSteps", 0);
                    float newSteps = mPrefs.getFloat("lastRecentSteps", 0);


                    newSteps = totalSteps - newSteps;

                    editor.putFloat("lastRecentSteps", totalSteps);
                    editor.commit();


                    db_Helper = new DbHelper(getActivity());

                    float calories;
                    //Calculate Calories with # of steps and the user's weight
                    calories = calculateCalories(newSteps, db_Helper.getWeight());

                    long lastRecentDuration = mPrefs.getLong("lastRecentDuration", 0);
                    float lastRecentDistance = mPrefs.getFloat("lastRecentDistance", 0);

                    //Use yyyyMMDdd date format so it is easy for the app to check the last seven days sessions
                    String currentDate = new java.text.SimpleDateFormat("yyyyMMdd").format(java.util.Calendar.getInstance().getTime());
                    DateFormat dateFormat = new java.text.SimpleDateFormat("yyyyMMdd");


                    Calendar calendar = Calendar.getInstance().getInstance();
                    //Set calendar to 7 days ago
                    calendar.add(Calendar.DATE, -7);

                    //Get current date from a week ago
                    Date dateFromWeekAgo = calendar.getTime();

                    String dateFromWeekAgoStr = dateFormat.format(dateFromWeekAgo);

                    editor.putString("pastDate", dateFromWeekAgoStr);
                    editor.commit();
                    editor.putString("currentDate", currentDate);

                    //Insert session data into database
                    db_Helper.addSession(currentDate, lastRecentDuration, lastRecentDistance, calories, newSteps);

                    //Add and update session to the all-time stats table
                    if(db_Helper.getTotalStatsTblCount() > 0){
                        db_Helper.updateTotalStats(lastRecentDuration, lastRecentDistance, calories);
                    }

                    //Set tag back to start tag
                    view.setTag(1);
                    //Stop Remote Service
                    getActivity().stopService(new Intent(getActivity(), MyFitnessService.class));

                    //Reset Some Preferences
                    editor.putFloat("totalCalories", 0);
                    editor.putFloat("totalCaloriesWeek", 0);
                    editor.putLong("totalTime", 0);
                    editor.putLong("totalTimeWeek", 0);
                    editor.commit();


                    db_Helper.close();
                }


            }
        });
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        //LocationRequest is created to help us receive updates
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        //Build a LocationSettingsRequest to help us get location
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        //Check location settings
        SettingsClient settingsClient = LocationServices.getSettingsClient(getActivity());
        settingsClient.checkLocationSettings(locationSettingsRequest);


        //Required to check permission before requestion location updates
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    //Calculate Calories from Steps and Weight
    public static float calculateCalories(float steps, int weight) {

        int weightRounded;
        float calories;
        float caloriesPerTwoK = 55;

        //Round weight to the nearest whole tens, ex. 155 -> 160, 92 -> 90
        if (weight <= 100) {
            weightRounded = 100;
        } else {
            weightRounded = ((weight + 5) / 10) * 10;

        }

        /* Calorie Conversion
           Calorie Count per 2000 miles taken from:
           https://www.verywell.com/pedometer-steps-to-calories-converter-3882595
           The brackets will be different than the websites (ex. 100, 120, 140 .. etc) because
           I am essentially rounding the weights to a certain bracket.
           For example, if my weight is 110 lbs I am estimating that my calories burnt per 2000 miles will
           be the same as someone who weights 100 lbs.
         */
        if (weightRounded <= 110) {
            caloriesPerTwoK = 55;
        } else if (weightRounded > 110 && weightRounded <= 130) {
            caloriesPerTwoK = 66;
        } else if (weightRounded > 130 && weightRounded <= 150) {
            caloriesPerTwoK = 76;
        } else if (weightRounded > 150 && weightRounded <= 170) {
            caloriesPerTwoK = 87;
        } else if (weightRounded > 170 && weightRounded <= 190) {
            caloriesPerTwoK = 98;
        } else if (weightRounded > 190 && weightRounded <= 210) {
            caloriesPerTwoK = 109;
        } else if (weightRounded > 210 && weightRounded <= 235) {
            caloriesPerTwoK = 120;
        } else if (weightRounded > 235 && weightRounded <= 263) {
            caloriesPerTwoK = 137;
        } else if (weightRounded > 263 && weightRounded <= 288) {
            caloriesPerTwoK = 150;
        } else if (weightRounded > 288) {
            caloriesPerTwoK = 164;
        }

        //Calculate Calories
        calories = (caloriesPerTwoK / 2000f) * steps;
        //Round Calories to the nearest hundredth place
        calories = Math.round(calories * 100.0f) / 100.0f;

        return calories;
    }


    //BroadcastReceiver - when a step is detected and counted
    //Distance will be calculated and updated
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            updateDuration(intent);

            long lastRecentDuration = intent.getLongExtra("duration", 0);

            mPrefs = context.getApplicationContext().getSharedPreferences("myPreferences", 0);
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putLong("lastRecentDuration", lastRecentDuration);
            editor.commit();


            float steps = intent.getFloatExtra("steps", 0);


            if (steps > 0.0) {

                editor.putFloat("totalSteps", steps);
                editor.commit();
                stepCount = mPrefs.getFloat("lastRecentSteps", 0);



                steps = steps - stepCount;

                double stepDouble = Double.parseDouble(Float.toString(steps));

                //Distance in Meters per Step
                double distance = stepDouble * 0.762;

                //Convert Distance from Meters to Miles
                distance = distance / 1609.34;

                //Round Distance to the nearest Hundredth
                distance = Math.round(distance * 100.0) / 100.0;

                editor.putFloat("lastRecentDistance", (float) distance);
                editor.commit();

                distanceTextView.setText(Double.toString(distance));
            }

        }
    };

    //Updates the Stopwatch TextView on the Portrait Activity
    public void updateDuration(Intent intent) {

        mins = intent.getIntExtra("mins", 0);
        secs = intent.getIntExtra("secs", 0);
        milliSecs = intent.getIntExtra("ms", 0);

        durationTextView.setText("" + String.format("%02d", mins) + ":"
                + String.format("%02d", secs) + ":" + String.format("%02d", milliSecs));
    }


    // onMapReady initiates the Google Map Fragment
    // it will request location permission if necessary, and set the map to the user's location
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(this);

                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d("test", "Current location is null. Using defaults.");
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    //Requests Location Permission from the User
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    //Depending on the user's permission request, mLocationPermissionGranted will be set
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }


    //onMyLocationButtonClick - if the user moves the map around to look at different places,
    // they focus the map back onto them by clicking the location button
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(mContext, "Focusing On Your Location", Toast.LENGTH_SHORT).show();

        return false;
    }

    //onLocationChanged - whenever the user is at a new location (while their workout is in session
    //  their path will be updated and the map will refocus onto the user
    @Override
    public void onLocationChanged(Location location){

        //Log.d("myLocation", "Location Changed: " + Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude()));

        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        LatLng latlong = new LatLng(latitude, longitude);

        //Add new location to points arraylist
        points.add(latlong);
        //update the line on the map
        updateLine();
        //Refocus camera onto the new user's location (CENTER MAP)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlong, DEFAULT_ZOOM));
    }

    //updateLine - updates the polyline that is drawing the user's path
    public void updateLine(){

        //Clear the map to draw a new line
        mMap.clear();
        PolylineOptions op = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

        //get latitude/longitude point and add to the polyline options
        for(int i = 0; i < points.size(); i++){
            LatLng point = points.get(i);
            op.add(point);
        }

        //Add line to the map
        line = mMap.addPolyline(op);
    }

}

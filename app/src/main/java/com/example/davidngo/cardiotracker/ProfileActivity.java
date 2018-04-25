package com.example.davidngo.cardiotracker;


/**
 * Created By: David Ngo
 */


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;


/* ProfileActivity
    - The profile screen
    - Displays a user's name, weight, and gender
    - Allows the user to edit their information
    - Shows the user their weekly average for calories burnt, time, distance, and # of workouts
    - Shows the user their total statistics of workouts
 */
public class ProfileActivity extends AppCompatActivity {

    private PopupWindow popUpInfo;
    private EditText newName, newWeight;
    private RadioButton radioMale, radioFemale;
    private TextView name, gender, weight;
    private TextView distanceWeekly, timeWeekly, workoutsWeekly, caloriesWeekly;
    private TextView distanceAll, timeAll, workoutsAll, caloriesAll;
    private String nTemp = "", gTemp = "", wTemp = "";
    private DbHelper db_Helper;
    private SharedPreferences mPrefs;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Set All TextViews
        name = (TextView) findViewById(R.id.nameTextView);
        gender = (TextView) findViewById(R.id.genderTextView);
        weight = (TextView) findViewById(R.id.weightTextView);

        distanceWeekly = (TextView) findViewById(R.id.distanceValueWeekly);
        timeWeekly = (TextView) findViewById(R.id.timeValueWeekly);
        workoutsWeekly = (TextView) findViewById(R.id.workoutsValueWeekly);
        caloriesWeekly = (TextView) findViewById(R.id.caloriesValueWeekly);

        distanceAll = (TextView) findViewById(R.id.distanceValueAllTime);
        timeAll = (TextView) findViewById(R.id.timeValueAllTime);
        workoutsAll = (TextView) findViewById(R.id.workoutsValueAllTime);
        caloriesAll = (TextView) findViewById(R.id.caloriesValueAllTime);


        db_Helper = new DbHelper(this);

        //Preference to see if this is the user's first time to the profile activity
        mPrefs = getSharedPreferences("myPreferences", 0);

        if (!mPrefs.contains("check")) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean("check", true);
            editor.commit();
        }
        Cursor data = db_Helper.getProfileInfo();

        while (data.moveToNext()) {
            nTemp = data.getString(1);
            gTemp = data.getString(2);
            wTemp = data.getString(3);
        }

        SharedPreferences.Editor editor = mPrefs.edit();

        //If this is the user's first time, set default Name, Gender, and Weight
        if (nTemp.isEmpty() || gTemp.isEmpty() || wTemp.isEmpty()) {
            name.setText("Your Name");
            gender.setText("Gender: N/A");
            weight.setText("Weight: 0 lbs");

            editor.putInt("weight", 0);
            editor.commit();

        } else {
            //Otherwise, set it to their info from the Database
            name.setText(nTemp);
            gender.setText("Gender: " + gTemp);
            weight.setText("Weight: " + wTemp + " lbs");

            editor.putInt("weight", Integer.parseInt(wTemp));
            editor.commit();
        }

        //Set Weekly Workout Statistics View
        setWeeklyTextViews();
        //Set All Time Workout Statistics View
        setAllTimeTextViews();


        data.close();
        db_Helper.close();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Register a Broadcast Receiver
        registerReceiver(broadcastReceiver, new IntentFilter(MyFitnessService.BROADCAST_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();

        //Unregister a Broadcast Receiver to avoid Leaking
        unregisterReceiver(broadcastReceiver);
    }

    //Broadcast Receiver to update statistics in real-time
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            float currentDistance, currentCalories;
            float currentDistance2, currentCalories2;
            float calories;
            long currentTime, currentTime2;
            long days, hours, minutes, seconds;

            mPrefs = getSharedPreferences("myPreferences", 0);


            db_Helper = new DbHelper(context);

            float steps = intent.getFloatExtra("steps", 0);
            long time = intent.getLongExtra("currentTime", 0);


            float lastRecentSteps = mPrefs.getFloat("lastRecentSteps", 0);

            //There are many times a broadcast can be sent if no steps have been taken
            // Must check that steps is > 0
            if (steps > 0.0) {

                /*
                    Calculate and Update ALL TIME WORKOUT STATS in Real-Time
                 */
                //Get Steps from Current Workout
                steps = steps - lastRecentSteps;

                //Calculate and Update All Time in Real-Time
                calories = RecordMainFragment.calculateCalories(steps, mPrefs.getInt("weight", 0));
                String str = distanceAll.getText().toString();

                //Since Distance will be in "100.0 Mile" form we just need the 100.0
                String[] splitHolder = str.split("\\s+");


                currentTime = mPrefs.getLong("totalTime", 0) + time;

                //Calculate Current Distance
                currentDistance = Float.parseFloat(splitHolder[0]) + (steps * 0.762f/ 1609.34f);
                //currentDistance = Float.parseFloat(splitHolder[0]) + (steps * 0.762f);
                currentDistance = Math.round(currentDistance * 100.0f) / 100.0f;

                distanceAll.setText(Float.toString(currentDistance) + " miles");


                //Convert Time to "X days X hr X min X sec" format
                seconds = (currentTime / 1000) % 60;
                minutes =  (currentTime/ (1000*60)) % 60;
                hours = ((currentTime / (1000*60*60)) % 24);
                days = (currentTime / (1000*60*60*24));
                String tempString = "";

                if(days > 0){
                    tempString = Long.toString(days) + " day ";
                }
                if (hours > 0) {
                    tempString = tempString + Long.toString(hours) + " hr ";
                }
                if(minutes > 0 ){
                    tempString = tempString + Long.toString(minutes) + " min ";
                }
                if (seconds > 0){
                    tempString = tempString + Long.toString(seconds) + " sec ";
                }
                if(seconds == 0){
                    tempString = "0 sec";
                }

                timeAll.setText(tempString);

                //Calculate Current Calories
                currentCalories = calories + mPrefs.getFloat("totalCalories", 0);
                currentCalories = Math.round(currentCalories * 100.0f) / 100.0f;
                caloriesAll.setText(Float.toString(currentCalories) + " Cal");


                /*
                    Calculate and Update WEEKLY WORKOUT STATS in Real-Time
                 */

                str = distanceWeekly.getText().toString();

                String[] splitHolder2 = str.split("\\s+");


                currentTime2 = mPrefs.getLong("totalTimeWeek", 0) + time;

                //Calculate Weekly Distance in Real-Time
                currentDistance2 = Float.parseFloat(splitHolder2[0]) + (steps * 0.762f / 1609.34f);
                currentDistance2 = Math.round(currentDistance2 * 100.0f) / 100.0f;

                distanceWeekly.setText(Float.toString(currentDistance2) + " miles");

                //timeWeekly.setText(Long.toString(currentTime));

                //Convert Time to "X days X hr X min X sec" format
                seconds = (currentTime2 / 1000) % 60;
                minutes =  (currentTime2/ (1000*60)) % 60;
                hours = ((currentTime2 / (1000*60*60)) % 24);
                days = (currentTime2 / (1000*60*60*24));
                tempString = "";

                if(days > 0){
                    tempString = Long.toString(days) + " day ";
                }
                if (hours > 0) {
                    tempString = tempString + Long.toString(hours) + " hr ";
                }
                if(minutes > 0 ){
                    tempString = tempString + Long.toString(minutes) + " min ";
                }
                if (seconds > 0){
                    tempString = tempString + Long.toString(seconds) + " sec ";
                }
                if(seconds == 0){
                    tempString = "0 sec";
                }

                timeWeekly.setText(tempString);

                //Calculate All-Time Calories in Real-Time
                currentCalories2 = calories + mPrefs.getFloat("totalCaloriesWeek", 0);
                currentCalories2 = Math.round(currentCalories2 * 100.0f) / 100.0f;
                caloriesWeekly.setText(Float.toString(currentCalories2) + " Cal");
            }


        }
    };

    //Setting All-Time TextViews to Actual Stats from Database
    public void setAllTimeTextViews() {

        Cursor data = db_Helper.getTotalSessionInfo();

        long t = 0;
        float d = 0;
        float c = 0;
        int w = 0;

        while (data.moveToNext()) {
            t = data.getLong(1);
            d = data.getFloat(2);
            c = data.getFloat(3);
            w = data.getInt(4);
        }

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong("totalTime", t);
        editor.commit();
        editor.putFloat("totalCalories", c);
        editor.commit();

        long seconds, hours, minutes, days;
        seconds = (t / 1000) % 60;
        minutes =  (t/ (1000*60)) % 60;
        hours = ((t / (1000*60*60)) % 24);
        days = (t / (1000*60*60*24));
        String tempString = "";

        if(days > 0){
            tempString = Long.toString(days) + " day ";
        }
        if (hours > 0) {
            tempString = tempString + Long.toString(hours) + " hr ";
        }
        if(minutes > 0 ){
            tempString = tempString + Long.toString(minutes) + " min ";
        }
        if (seconds > 0){
            tempString = tempString + Long.toString(seconds) + " sec ";
        }
        if(seconds == 0){
            tempString = "0 sec";
        }

        timeAll.setText(tempString);
        distanceAll.setText(Float.toString(d) + " miles");
        caloriesAll.setText(Float.toString(c) + " Cal");
        workoutsAll.setText(Integer.toString(w) + " times");

        data.close();
    }

    //Setting Weekly Average TextViews to Actual Stats from Database
    public void setWeeklyTextViews() {

        Cursor data = db_Helper.getRecordsFromPastWeek(mPrefs.getString("currentDate", ""), mPrefs.getString("pastDate", ""));

        long t = 0;
        float d = 0;
        float c = 0;
        int w = 0;

        while (data.moveToNext()) {
            t = data.getLong(0);
            d = data.getFloat(1);
            c = data.getFloat(2);
            w = data.getInt(3);
        }

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong("totalTimeWeek", t);
        editor.commit();
        editor.putFloat("totalCaloriesWeek", c);
        editor.commit();

        long seconds, hours, minutes, days;
        seconds = (t / 1000) % 60;
        minutes =  (t/ (1000*60)) % 60;
        hours = ((t / (1000*60*60)) % 24);
        days = (t / (1000*60*60*24));
        String tempString = "";

        if(days > 0){
            tempString = Long.toString(days) + " day ";
        }
        if (hours > 0) {
            tempString = tempString + Long.toString(hours) + " hr ";
        }
        if(minutes > 0 ){
            tempString = tempString + Long.toString(minutes) + " min ";
        }
        if (seconds > 0){
            tempString = tempString + Long.toString(seconds) + " sec ";
        }
        if(seconds == 0){
            tempString = "0 sec";
        }


        c = Math.round(c * 100.0f) / 100.0f;
        timeWeekly.setText(tempString);

        d = Math.round(d * 100.0f) / 100.0f;

        distanceWeekly.setText(Float.toString(d) + " miles");
        caloriesWeekly.setText(Float.toString(c) + " Cal");
        workoutsWeekly.setText(Integer.toString(w) + " times");

        data.close();
    }


    // PopUp window will appear if a user decides to click and edit their profile information
    // It will allow them to edit their name, gender, and weight
    public void popUp(View view) {
        LayoutInflater inflater2 = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        ViewGroup container = (ViewGroup) inflater2.inflate(R.layout.edit_profile_layout, null);
        LinearLayout linearL = (LinearLayout) findViewById(R.id.profileLinearL);
        popUpInfo = new PopupWindow(container, 900, 700, true);
        popUpInfo.showAtLocation(linearL, Gravity.CENTER, 0, 0);

        String nHint = "Name", gHint = "", wHint = "Weight";

        Button cancelButton = (Button) popUpInfo.getContentView().findViewById(R.id.cancelButton);
        Button saveInfoButton = (Button) popUpInfo.getContentView().findViewById(R.id.saveInfoButton);
        newName = (EditText) popUpInfo.getContentView().findViewById(R.id.newName);
        radioMale = (RadioButton) popUpInfo.getContentView().findViewById(R.id.radio_male);
        radioFemale = (RadioButton) popUpInfo.getContentView().findViewById(R.id.radio_female);
        newWeight = (EditText) popUpInfo.getContentView().findViewById(R.id.newWeight);

        db_Helper = new DbHelper(this);

        SharedPreferences mPrefs = getSharedPreferences("myPreferences", 0);
        boolean check = mPrefs.getBoolean("check", true);

        if (!check) {
            Cursor data = db_Helper.getProfileInfo();

            while (data.moveToNext()) {
                nHint = data.getString(1);
                gHint = data.getString(2);
                wHint = data.getString(3);
            }

            newName.setText(nHint);
            if (gHint.equalsIgnoreCase("Female")) {
                radioFemale.setChecked(true);
            } else {
                radioMale.setChecked(true);
            }
            newWeight.setText(wHint);


        }

        //saves profile info to profile database
        saveInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                db_Helper = new DbHelper(getApplicationContext());
                nTemp = newName.getText().toString().trim();
                if (radioMale.isChecked()) {
                    gTemp = "Male";
                } else if (radioFemale.isChecked()) {
                    gTemp = "Female";
                }
                wTemp = newWeight.getText().toString().trim();

                SharedPreferences mPrefs = getSharedPreferences("myPreferences", 0);
                boolean check = mPrefs.getBoolean("check", true);

                //If this is the first time the user adds their Profile info, insert into database
                if (check) {
                    db_Helper.addInfo(nTemp, gTemp, wTemp);
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putBoolean("check", false);
                    editor.commit();
                } else {
                    //Else we will update the current info (there will only be one record)
                    Log.d("TESTER", "Update Info");
                    db_Helper.updateInfo(nTemp, gTemp, wTemp);
                }


                name.setText(nTemp);
                gender.setText("Gender: " + gTemp);
                weight.setText("Weight: " + wTemp + " lbs");


                db_Helper.close();
                popUpInfo.dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popUpInfo.dismiss();
            }
        });
    }


}

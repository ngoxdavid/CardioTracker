package com.example.davidngo.cardiotracker;

/**
 * Created By: David Ngo
 */



import android.content.Intent;
import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/* RecordWorkout
    - The base activity of the main screen
    - It contains two different fragments that will appear depending on screen orientation (landscape or portrait)
    - Fragment Switches are handled by Android (using layout-land and etc in res folders)
    - Helps retain fragment instance
 */
public class RecordWorkout extends AppCompatActivity  {

    private static final String TAG_RETAINED_FRAGMENT = "RecordMainFragment";

    private RecordMainFragment mRetainedFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_workout);


        FragmentManager fm = getSupportFragmentManager();

        //Checks for a retained fragment to get back data
        //This means that switching from the workoutgraph back to the main page will
        //continue to show the correct real-time data for the workout
        if (mRetainedFragment == null) {
            // add the fragment
            mRetainedFragment = new RecordMainFragment();
            fm.beginTransaction().add(mRetainedFragment, TAG_RETAINED_FRAGMENT).commit();
        }

    }

    // Function to start the Profile Activity (once the profile button is pressed)
    public void goToProfile(View view){
        Intent myIntent = new Intent(RecordWorkout.this, ProfileActivity.class);
        startActivity(myIntent);
    }
}
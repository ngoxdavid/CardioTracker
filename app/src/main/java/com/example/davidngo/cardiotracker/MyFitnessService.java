package com.example.davidngo.cardiotracker;


/**
 * Created By: David Ngo
 */

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Time;
import com.example.davidngo.cardiotracker.R;


/* MyFitness Service
    - a remote service that will continue to track the user's steps and duration for their current workout
    - sends a broadcast out to receivers in various activities
 */
public class MyFitnessService extends Service implements SensorEventListener {

    long TimeInMS, startTime ;
    Handler handler;
    public static int secs, mins, milliSecs ;
    SensorManager sensorManager;
    public static final String BROADCAST_ACTION = "com.example.davidngo.cardiotracker";


    @Override
    public void onCreate(){
        super.onCreate();


        handler = new Handler();

        //Get the system time when this process starts (so we can calculate time later)
        startTime = SystemClock.uptimeMillis();

        //Post the runnable to the handler
        handler.postDelayed(runnable, 0);

        //Create a Sensor Manager
        sensorManager = (SensorManager) getSystemService(getApplicationContext().SENSOR_SERVICE);

        //Register the Step Counter Sensor to a variable
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if(stepSensor != null){
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
        else{
            Log.d("tester", "Step Counter Sensor Not Found.");
        }

    }

    //Remove the task and unregister the Sensor Listener when this service is destroyed
    @Override
    public void onDestroy(){
        super.onDestroy();

        sensorManager.unregisterListener(this);
        handler.removeCallbacks(runnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //Runnable that will run on another thread
    // This will calculate the time and mimic a stopwatch
    public Runnable runnable = new Runnable(){
        public void run() {

            //Current Time - Starting time = total time of the workout
            TimeInMS = SystemClock.uptimeMillis() - startTime;

            //Miliseconds/1000 = seconds
            secs = (int) (TimeInMS / 1000);
            //Seconds / 60 = minutes
            mins = secs / 60;
            //Seconds % 60 to get the remaining seconds to help with 00:00 format time
            secs = secs % 60;

            //Miliseconds for 00:00:00 format time
            milliSecs = (int) (TimeInMS % 1000);
            milliSecs = milliSecs/10;

            //Send Broadcast with intent that contains secs, mins, miliseconds, total duration
            Intent intent = new Intent(BROADCAST_ACTION);
            intent.putExtra("secs", secs);
            intent.putExtra("mins", mins);
            intent.putExtra("ms", milliSecs);
            intent.putExtra("duration", TimeInMS);
            sendBroadcast(intent);

            handler.postDelayed(this, 0);
        }
    };

    //onSensorChanged - when the device senses a step
    // it will send the sensorvalue (number of steps) and current time to another activity
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

            float sensorValue;


            Intent intent = new Intent(BROADCAST_ACTION);
            sensorValue = sensorEvent.values[0];
            if(sensorValue > 0) {
                intent.putExtra("steps", sensorValue);
                intent.putExtra("currentTime", TimeInMS);
                sendBroadcast(intent);
            }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
            //Do nothing
    }
}

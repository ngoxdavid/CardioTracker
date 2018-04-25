package com.example.davidngo.cardiotracker;

/**
 * Created By: David Ngo
 */

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import java.util.ArrayList;
import java.util.List;
import static com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER;


/* WorkoutGraphFragment
    - Displays Two Graphs:
        1. BarChart - Calories Burnt Per 5 Minutes
        2. LineChart - Steps Per 5 Minutes
    - Graphs are displayed in Realtime
    - Also the user's minutes per mile will be displayed.
    - It will show their average min/mile, fastest min/mile, and slowest min/mile
 */
public class WorkoutGraphFragment extends Fragment {

    private SharedPreferences mPrefs;
    private LineChart stepsChart;
    private BarChart caloriesChart;
    private List<Entry> stepValues;
    private List<BarEntry> caloriesValues;
    private DbHelper db_Helper;
    private TextView avgTextView, minTextView, maxTextView;
    private String minMileTemp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.record_workout_landscape_layout, container, false);

        //Set TextViews
        avgTextView = view.findViewById(R.id.minMileAvg);
        minTextView = view.findViewById(R.id.minMileMin);
        maxTextView = view.findViewById(R.id.minMileMax);

        //Set SharedPreferences
        mPrefs = view.getContext().getSharedPreferences("myPreferences", 0);

        //Reset minMile Stats on Create
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putFloat("minMileMax", 0f);
        editor.commit();

        editor.putFloat("minMileMin", 0f);
        editor.commit();

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();

        //Register Broadcast Receiver to receive data from remote service
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(MyFitnessService.BROADCAST_ACTION));
    }

    @Override
    public void onPause(){
        super.onPause();

        //Unregister Broadcast Receiver to avoid leak
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        //Initialize LineChart and BarChart
        stepsChart = (LineChart) getActivity().findViewById(R.id.stepsChart);
        caloriesChart = (BarChart) getActivity().findViewById(R.id.caloriesChart);

        //Hide Description on Chart
        Description blankDescription = new Description();
        blankDescription.setText("");
        stepsChart.setDescription(blankDescription);
        caloriesChart.setDescription(blankDescription);

        LineData stepData = new LineData();
        BarData caloriesData = new BarData();


        //Set Some Features for the StepChart
        stepsChart.setDragEnabled(true);
        stepsChart.setScaleEnabled(true);
        stepsChart.setDrawGridBackground(false);
        stepsChart.setPinchZoom(true);

        //Attach Data to StepChart
        stepsChart.setData(stepData);

        stepValues = new ArrayList<Entry>();
        caloriesValues = new ArrayList<BarEntry>();

        //Starting Data Entries to set Graph Scale
        stepValues.add(new Entry(0f, 100f));
        caloriesValues.add(new BarEntry(0f, 5f));

        //Set Step DataSet
        LineDataSet stepValueSet = new LineDataSet(stepValues, "Steps Per 5 Mins");
        //Setting More Details for the Step Chart
        stepValueSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        stepValueSet.setColor(Color.RED);
        stepValueSet.setFillColor(Color.RED);
        stepValueSet.setCubicIntensity(0.15f);
        stepValueSet.setMode(CUBIC_BEZIER);
        stepValueSet.setDrawFilled(true);

        //Set Calorie DataSet
        BarDataSet caloriesValueSet = new BarDataSet(caloriesValues, "Calories Burned Per 5 Mins");

        //Calorie Chart Details
        caloriesValueSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        caloriesValueSet.setColor(Color.parseColor("#5FCDEE"));

        //Step Data Set List
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(stepValueSet);

        //Axis Options for Both Step and Calorie Chart
        stepsChart.getXAxis().setDrawGridLines(false);
        stepsChart.getAxisLeft().setDrawGridLines(false);

        caloriesChart.getXAxis().setDrawGridLines(false);
        caloriesChart.getAxisLeft().setDrawGridLines(false);

        caloriesChart.getAxisRight().setDrawLabels(false);
        stepsChart.getAxisRight().setDrawLabels(false);

        stepsChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        caloriesChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        //Finalize Attaching Data to Step Chart and Drawing the Chart
        stepData = new LineData(dataSets);
        stepsChart.setData(stepData);
        stepsChart.invalidate();

        //Finalize Attaching Data to Calorie Chart and Drawing the Chart
        caloriesData = new BarData(caloriesValueSet);

        caloriesData.setBarWidth(0.9f);
        caloriesChart.setData(caloriesData);
        caloriesChart.setFitBars(true);
        caloriesChart.invalidate();
    }

    //broadcastReceiver - When the user makes a step, the graph will change in real-time
    //  minMile calculations will be updated as well
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long time = intent.getLongExtra("currentTime", 0);
            float steps = intent.getFloatExtra("steps", 0f);

            float stepsPerFiveMin;
            float caloriesPerFiveMin;

            mPrefs = context.getApplicationContext().getSharedPreferences("myPreferences", 0);
            SharedPreferences.Editor editor = mPrefs.edit();


            float lastRecentSteps = mPrefs.getFloat("lastRecentSteps", 0);

            //Make sure the step counter is not reading 0
            if(steps > 0f){

                db_Helper = new DbHelper(getActivity());

                //Get current # of steps during current workout
                steps = steps - lastRecentSteps;

                Log.d("graph", "Graph Current Steps: " + Float.toString(steps));
                Log.d("graph", "Graph Current Time: " + Float.toString(time));

                //Steps * 1000 / miliseconds = steps per second
                stepsPerFiveMin = (steps*1000f)/(float)time;
                //Steps Per Second * 300 seconds = steps per 5 minutes
                stepsPerFiveMin = stepsPerFiveMin * 300;

                //Calculate Calories burnt per five minutes
                caloriesPerFiveMin = RecordMainFragment.calculateCalories(stepsPerFiveMin, db_Helper.getWeight());


                LineData data = stepsChart.getData();
                ILineDataSet set = data.getDataSetByIndex(0);

                BarData bdata = caloriesChart.getBarData();
                IBarDataSet bset = bdata.getDataSetByIndex(0);

                //Add new point to the graph for calories per five minutes and notify that the data has changed
                bdata.addEntry(new BarEntry(bset.getEntryCount(), caloriesPerFiveMin), 0);
                bdata.notifyDataChanged();

                //Add new point to the graph for steps per five minutes and notify that the data has changed
                data.addEntry(new Entry(set.getEntryCount(), stepsPerFiveMin),0);
                data.notifyDataChanged();

                //Notify the graphs that the data has changed
                stepsChart.notifyDataSetChanged();

                //Move the view of the graph to accommodate the new step data
                stepsChart.setVisibleXRangeMaximum(10);
                stepsChart.moveViewToX(data.getEntryCount());

                //Move the view of the graph to accommodate the new calories data
                caloriesChart.notifyDataSetChanged();
                caloriesChart.setVisibleXRangeMaximum(10);
                caloriesChart.moveViewToX(bdata.getEntryCount());

                //Calculate Avg Time per Mile (avg # of steps in a mile is 2000)
                float stepsPerMile = steps % 2000;

                stepsPerMile = (stepsPerMile*1000f)/(float)time;

                //Steps per minute
                stepsPerMile = stepsPerMile * 60;

                //minsPerMile = 2000Steps Per Mile / Steps per minute
                float minsPerMile = 2000f/stepsPerMile;

                //Calculate both minutes and seconds to get "00:00 mins/Mile" format
                float seconds = (minsPerMile * 100f) % 100f;
                seconds = (seconds / 100f) * 60f;

                avgTextView.setText(String.format("%02d:%02d", (int)minsPerMile, (int)seconds));


                //If this is the first time calculating min/max, set min and max to the avg
                if(mPrefs.getFloat("minMileMax", 0) == 0f && mPrefs.getFloat("minMileMin", 0) == 0f) {

                    editor.putFloat("minMileMax", minsPerMile);
                    editor.commit();
                    editor.putFloat("minMileMin", minsPerMile);
                    editor.commit();
                    minMileTemp = avgTextView.getText().toString().trim();

                    minTextView.setText(minMileTemp);
                    maxTextView.setText(minMileTemp);

                } else{
                    //If current avg is > max, set max
                    minMileTemp = avgTextView.getText().toString().trim();

                    if(minsPerMile < mPrefs.getFloat("minMileMax", 0)){
                        //set textview
                        editor.putFloat("minMileMax", minsPerMile);
                        editor.commit();

                        maxTextView.setText(minMileTemp);

                    }
                    //if current avg is < min, set min
                    else if(minsPerMile > mPrefs.getFloat("minMileMin", 0)){
                        //set min textview
                        editor.putFloat("minMileMin", minsPerMile);
                        editor.commit();

                        minTextView.setText(minMileTemp);
                    }
                }

                db_Helper.close();
            }
        }
    };
}

package com.example.davidngo.cardiotracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created By: David Ngo
 */

/* DbHelper
    - Class to help with database management
    - Creates different tables
    - Will insert, update, delete data from the database
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String PROFILE_TBL_NAME = "profile";
    private static final String SESSION_TBL_NAME = "session";
    private static final String TOTAL_STATS_TBL_NAME = "totalstats";


    public DbHelper(Context context){
        super(context, PROFILE_TBL_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        String createProfileTBL = "CREATE TABLE " + PROFILE_TBL_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "gender TEXT, " +
                "weight INTEGER DEFAULT 0);";

        String createSessionTBL = "CREATE TABLE " + SESSION_TBL_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date INT, " +
                "duration REAL, " +
                "distance REAL, " +
                "calories REAL, " +
                "steps INT);";

        String createTotalStatsTBL = "CREATE TABLE " + TOTAL_STATS_TBL_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "duration REAL, " +
                "distance REAL, " +
                "calories REAL, " +
                "amount INT);";
        //Might need to add a steps column

        //Create Profile Table - Contains Personal Information (Name, Gender, Weight)
        database.execSQL(createProfileTBL);
        //Create Stats Table - Contains stats and sessions of workouts
        database.execSQL(createSessionTBL);
        //Create Total Stats Table - Contains Total Stats and Sessions of Workouts
        database.execSQL(createTotalStatsTBL);
    }

    //Update Profile Information
    public void updateInfo(String name, String gender, String weight){

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues newInfo = new ContentValues();

        newInfo.put("name", name);
        newInfo.put("gender", gender);
        newInfo.put("weight", weight);

        database.update(PROFILE_TBL_NAME, newInfo, "id = 1", null);

    }

    //Update All Time Stats Table
    public void updateTotalStats(long duration, float distance, float calories){

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues newInfo = new ContentValues();
        String updateQuery;

        updateQuery = "UPDATE " + TOTAL_STATS_TBL_NAME + " SET duration = duration + " + Long.toString(duration);
        database.execSQL(updateQuery);
        updateQuery = "UPDATE " + TOTAL_STATS_TBL_NAME + " SET distance = distance + " + Float.toString(distance);
        database.execSQL(updateQuery);
        updateQuery = "UPDATE " + TOTAL_STATS_TBL_NAME + " SET calories = calories + " + Float.toString(calories);
        database.execSQL(updateQuery);
        updateQuery = "UPDATE " + TOTAL_STATS_TBL_NAME + " SET amount = amount + 1";
        database.execSQL(updateQuery);
    }

    //Initialize Stats Table to Have One Row
    public void initializeTotalStats(){

        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues info = new ContentValues();

        info.put("duration", 0.0);
        info.put("distance", 0.0);
        info.put("calories", 0.0);
        info.put("amount", 0);

        database.insert(TOTAL_STATS_TBL_NAME, null, info);
    }

    //Get total number of rows in stats table
    //This is to check if it is empty or full
    public int getTotalStatsTblCount(){

        SQLiteDatabase database = this.getWritableDatabase();

        String query = "SELECT COUNT(*) FROM "  + TOTAL_STATS_TBL_NAME;

        Cursor data = database.rawQuery(query, null);

        int count = -1;

        while(data.moveToNext()){
            count = data.getInt(0);
        }

        return count;
    }

    //Get total number of workouts
    public int getTotalSessionsCount(){

        SQLiteDatabase database = this.getWritableDatabase();

        String query = "SELECT COUNT(*) FROM "  + SESSION_TBL_NAME;

        Cursor data = database.rawQuery(query, null);

        int count = -1;

        while(data.moveToNext()){
            count = data.getInt(0);
        }

        data.close();
        return count;
    }

    //Add Profile Info to Database
    public boolean addInfo(String name, String gender, String weight){
        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues info = new ContentValues();

        info.put("name", name);
        info.put("gender", gender);
        info.put("weight", weight);

        long result = database.insert(PROFILE_TBL_NAME, null, info);

        if(result == -1){
            return false;
        }
        else{
            return true;
        }
    }

    //Return the User's Weight
    public int getWeight(){

        SQLiteDatabase database = this.getWritableDatabase();

        String query = "SELECT weight FROM "  + PROFILE_TBL_NAME;

        Cursor data = database.rawQuery(query, null);

        int weight = 0;

        while(data.moveToNext()){
            weight = data.getInt(0);
        }

        data.close();

        return weight;
    }

    //Insert Session Data into Database
    public boolean addSession(String date, Long duration,  float distance, float calories, float steps){
        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues info = new ContentValues();
        info.put("date", date);
        info.put("duration", duration);
        info.put("distance", distance);
        info.put("calories", calories);
        info.put("steps", steps);


        long result = database.insert(SESSION_TBL_NAME, null, info);

        if(result == -1){
            return false;
        }
        else{
            return true;
        }
    }

    //Get Records from the past 7 days (to calculate weekly averages)
    public Cursor getRecordsFromPastWeek(String currentDate, String pastDate){
        SQLiteDatabase database = this.getWritableDatabase();

        String query = "SELECT sum(duration), sum(distance), sum(calories), count(*) FROM " + SESSION_TBL_NAME + " WHERE date >= '" + pastDate + "' AND date <= '" + currentDate +"'" ;

        Cursor data = database.rawQuery(query, null);

        return data;
    }

    //Return Profile Info
    public Cursor getProfileInfo(){
        SQLiteDatabase database = this.getWritableDatabase();
        String query = "SELECT * FROM " + PROFILE_TBL_NAME + " WHERE id = 1";

        Cursor data = database.rawQuery(query, null);

        return data;
    }

    //Return All Time Session Info
    public Cursor getTotalSessionInfo(){

        SQLiteDatabase database = this.getWritableDatabase();
        String query = "SELECT * FROM " + TOTAL_STATS_TBL_NAME + " WHERE id = 1";

        Cursor data = database.rawQuery(query, null);

        return data;
    }


    @Override
    public void onUpgrade(SQLiteDatabase database, int i, int i1) {
        database.execSQL("DROP IF TABLE EXISTS " + PROFILE_TBL_NAME);
        database.execSQL("DROP IF TABLE EXISTS " + TOTAL_STATS_TBL_NAME);
        database.execSQL("DROP IF TABLE EXISTS " + SESSION_TBL_NAME);
        onCreate(database);
    }

}


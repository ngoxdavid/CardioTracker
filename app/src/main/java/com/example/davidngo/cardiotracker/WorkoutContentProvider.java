package com.example.davidngo.cardiotracker;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import java.util.HashMap;


/**
 * Created By: David Ngo
 */


public class WorkoutContentProvider extends ContentProvider{

    private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private DbHelper db_Helper;
    private SQLiteDatabase database;
    private static final String PROVIDER = "WorkoutContentProvider";
    private static final String URL = "content://" + PROVIDER + "/";
    private static final String DB_NAME = "mydb";
    private static final String PROFILE_TBL_NAME = "profile";
    private static final String SESSION_TBL_NAME = "session";
    private static final String TOTAL_STATS_TBL_NAME = "totalstats";
    private static HashMap<String, String> PROJECTION_MAP;
    private static final int PROFILE_ID = 1, SESSION_ID = 2, TOTAL_STATS_ID = 3;


    static{
        mUriMatcher.addURI(PROVIDER , PROFILE_TBL_NAME , PROFILE_ID);
        mUriMatcher.addURI(PROVIDER , PROFILE_TBL_NAME +"/" , PROFILE_ID);
        mUriMatcher.addURI(PROVIDER , SESSION_TBL_NAME  , SESSION_ID);
        mUriMatcher.addURI(PROVIDER , SESSION_TBL_NAME +"/" , SESSION_ID);
        mUriMatcher.addURI(PROVIDER , TOTAL_STATS_TBL_NAME , TOTAL_STATS_ID);
        mUriMatcher.addURI(PROVIDER , TOTAL_STATS_TBL_NAME +"/" , TOTAL_STATS_ID);
    }

    public boolean onCreate(){

        db_Helper = new DbHelper(getContext());

        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values){

        db_Helper = new DbHelper(getContext());
        database = db_Helper.getWritableDatabase();

        long count = 0;
        Uri _uri = null;

        switch(mUriMatcher.match(uri)){
            case PROFILE_ID:
                count = database.insert(PROFILE_TBL_NAME, "", values);
                _uri = ContentUris.withAppendedId(Uri.parse(URL + PROFILE_TBL_NAME), count);

                break;
            case SESSION_ID:
                count = database.insert(SESSION_TBL_NAME, "", values);
                _uri = ContentUris.withAppendedId(Uri.parse(URL + SESSION_TBL_NAME), count);

                break;
            case TOTAL_STATS_ID:
                count = database.insert(TOTAL_STATS_TBL_NAME, "", values);
                _uri = ContentUris.withAppendedId(Uri.parse(URL + TOTAL_STATS_TBL_NAME), count);

                break;
            default:
                count = 0;
        }


        database.close();

        return _uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionsArgs){
        //Not deleting anything in this app yet
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,String[] selectionArgs, String sort) {

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        database = db_Helper.getWritableDatabase();
        switch (mUriMatcher.match(uri)) {
            case PROFILE_ID:
                queryBuilder.setTables(PROFILE_TBL_NAME);
                queryBuilder.setProjectionMap(PROJECTION_MAP);
                break;

            case SESSION_ID:
                queryBuilder.setTables(SESSION_TBL_NAME);
                queryBuilder.setProjectionMap(PROJECTION_MAP);
                break;
            case TOTAL_STATS_ID:
                queryBuilder.setTables(TOTAL_STATS_TBL_NAME);
                queryBuilder.setProjectionMap(PROJECTION_MAP);
                break;
            default:
                throw new IllegalArgumentException("URI Not Supported: " + uri);
        }



        Cursor data = queryBuilder.query(database,	projection,	selection, selectionArgs,null, null, sort);
        data.setNotificationUri(getContext().getContentResolver(), uri);

        db_Helper.close();
        return data;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs){
        int count = 0;
        db_Helper = new DbHelper(getContext());
        database = db_Helper.getWritableDatabase();

        switch(mUriMatcher.match(uri)){
            case PROFILE_ID:
                count = database.update(PROFILE_TBL_NAME, values, selection, selectionArgs);
                break;
            case SESSION_ID:
                count = database.update(SESSION_TBL_NAME, values, selection, selectionArgs);

                break;
            case TOTAL_STATS_ID:
                count = database.update(TOTAL_STATS_TBL_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("URI Not Supported: " + uri);
        }

        return count;
    }

    @Override
    public String getType(Uri uri){
        switch (mUriMatcher.match(uri)){
            case PROFILE_ID:
                return "vnd.android.cursor.dir/vnd.com.example.davidngo.cardiotracker.profile";
            case SESSION_ID:
                return "vnd.android.cursor.dir/vnd.com.example.davidngo.cardiotracker.session";
            case TOTAL_STATS_ID:
                return "vnd.android.cursor.dir/vnd.com.example.davidngo.cardiotracker.totalstats";
            default:
                throw new IllegalArgumentException("URI Not Supported: " + uri);
        }
    }



}

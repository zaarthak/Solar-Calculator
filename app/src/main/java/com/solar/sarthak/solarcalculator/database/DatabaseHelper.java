package com.solar.sarthak.solarcalculator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.solar.sarthak.solarcalculator.models.Place;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // database version
    private static final int DATABASE_VERSION = 1;

    // database name
    private static final String DATABASE_NAME = "solar_calculator_db";

    // table name
    private static final String TABLE_NAME = "category";

    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_FAVORITE = "favorite";

    // create table query
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
            + COLUMN_NAME + " TEXT, "
            + COLUMN_LATITUDE + " REAL, "
            + COLUMN_LONGITUDE + " REAL, "
            + COLUMN_FAVORITE + " INTEGER, "
            + "PRIMARY KEY (" + COLUMN_LATITUDE + ", "
            + COLUMN_LONGITUDE + ")" + ")";

    Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    /**
     * Add new Place in database.
     *
     * @param place is the object of new place
     */
    public void addPlace(Place place) {

        // get writable database as we want to write data
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_NAME, COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_FAVORITE},
                COLUMN_LATITUDE + "=? AND " + COLUMN_LONGITUDE + "=?",
                new String[]{String.valueOf(place.getLatitude()), String.valueOf(place.getLongitude())},
                null, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {

            Toast.makeText(mContext, "Already added in favorites.", Toast.LENGTH_SHORT).show();
        } else {

            ContentValues values = new ContentValues();

            values.put(COLUMN_NAME, place.getName());
            values.put(COLUMN_LATITUDE, place.getLatitude());
            values.put(COLUMN_LONGITUDE, place.getLongitude());
            values.put(COLUMN_FAVORITE, Integer.parseInt(place.getFavorite()));

            // insert row
            db.insert(TABLE_NAME, null, values);

            // close db connection
            db.close();
        }

        cursor.close();
    }

    /**
     * Update new place with favorite status.
     */
    public void updatePlace(double lat, double lon, int favorite) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(COLUMN_FAVORITE, favorite);

        db.update(TABLE_NAME,
                values,
                COLUMN_LATITUDE + "=? AND " + COLUMN_LONGITUDE + "=?",
                new String[] {String.valueOf(lat), String.valueOf(lon)} );

        db.close();
    }

    /**
     * Get place by latitude and longitude.
     * Since, latitude and longitude together form the primary key of the table.
     */
    public Place getPlace(double lat, double lon) {

        Place place = null;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " +
                COLUMN_LATITUDE + "=? AND " + COLUMN_LONGITUDE + "=?",
                new String[] {String.valueOf(lat), String.valueOf(lon)});

        if (cursor.moveToNext()) {

            place = new Place(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                    String.valueOf(cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE))),
                    String.valueOf(cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE))),
                    String.valueOf(cursor.getInt(cursor.getColumnIndex(COLUMN_FAVORITE))));
        }

        cursor.close();
        db.close();

        return place;
    }

    /**
     * Get all saved places.
     */
    public List<Place> getAllPlace() {
        List<Place> placeList = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Place place = new Place(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                        String.valueOf(cursor.getDouble(cursor.getColumnIndex(COLUMN_LATITUDE))),
                        String.valueOf(cursor.getDouble(cursor.getColumnIndex(COLUMN_LONGITUDE))),
                        String.valueOf(cursor.getInt(cursor.getColumnIndex(COLUMN_FAVORITE))));

                placeList.add(place);
            } while (cursor.moveToNext());
        }

        // close db connection
        db.close();

        // return location list
        return placeList;
    }

    /**
     * Delete a place from database.
     */
    public void deletePlace(double lat, double lon) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_NAME,
                COLUMN_LATITUDE + "=? AND " + COLUMN_LONGITUDE + "=?",
                new String[] {String.valueOf(lat), String.valueOf(lon)});
        db.close();
    }

    /**
     * Get total count of places in database.
     */
    public int getPlaceCount() {

        String countQuery = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();

        return count;
    }
}

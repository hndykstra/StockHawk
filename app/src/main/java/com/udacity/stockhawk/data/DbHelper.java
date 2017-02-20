package com.udacity.stockhawk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.udacity.stockhawk.data.Contract.Quote;


class DbHelper extends SQLiteOpenHelper {


    private static final String NAME = "StockHawk.db";
    private static final int VERSION = 3;


    DbHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String builder = "CREATE TABLE " + Quote.TABLE_NAME + " ("
                + Quote._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Quote.COLUMN_SYMBOL + " TEXT NOT NULL, "
                + Quote.COLUMN_PRICE + " REAL NOT NULL, "
                + Quote.COLUMN_ABSOLUTE_CHANGE + " REAL NOT NULL, "
                + Quote.COLUMN_PERCENTAGE_CHANGE + " REAL NOT NULL, "
                + Quote.COLUMN_NAME + " TEXT NOT NULL, "
                + Quote.COLUMN_HISTORY + " TEXT NOT NULL, "
                + "UNIQUE (" + Quote.COLUMN_SYMBOL + ") ON CONFLICT REPLACE);";

        db.execSQL(builder);

        String updBuilder = "CREATE TABLE " + Contract.StockUpdated.TABLE_NAME + " ("
                + Contract.StockUpdated._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Contract.StockUpdated.COLUMN_LAST_UPDATED + " INTEGER NOT NULL"
                + ");";
        db.execSQL(updBuilder);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL(" DROP TABLE IF EXISTS " + Quote.TABLE_NAME);
        db.execSQL(" DROP TABLE IF EXISTS " + Contract.StockUpdated.TABLE_NAME);

        onCreate(db);
    }
}

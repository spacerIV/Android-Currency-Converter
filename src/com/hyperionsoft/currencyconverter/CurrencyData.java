package com.hyperionsoft.currencyconverter;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class CurrencyData extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "currencys.db";
	private static final int DATABASE_VERSION = 1;
	
	public CurrencyData(Context ctx) {
		super (ctx, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE currencydata (" +
			  	   " fromCurrency TEXT NOT NULL,  " + 
				   " toCurrency TEXT NOT NULL,  " +
				   " amount TEXT NOT NULL,  " +
				   " source TEXT NOT NULL,  " +
				   " dateTime TEXT NOT NULL);");			
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IS EXISTS currencydata");
		onCreate(db);
	}

}

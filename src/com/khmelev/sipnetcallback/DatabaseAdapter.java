package com.khmelev.sipnetcallback;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseAdapter {
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	private final Context mCtx;

	private static final String DATABASE_TABLE = "last10calls";
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (_id integer primary key autoincrement, "
			+ "row_time NOT NULL DEFAULT CURRENT_TIMESTAMP, number text not null, name text);";

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, Constants.DBNAME, null, Constants.DBVERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("DatabaseAdapter", "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE);
			onCreate(db);
		}
	}

	public DatabaseAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	public DatabaseAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public Cursor fetchLast10Calls() {
		Cursor cursor = mDb.rawQuery("SELECT * FROM "+DATABASE_TABLE+" ORDER BY row_time DESC LIMIT 10", null);
		return cursor;
	}

	public long addRec(String number, String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("number", number);
		initialValues.put("name", (name==null)?"":name);

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	public void dbRecreate() {
		mDbHelper.onUpgrade(mDb, 1, 1);
		open();
	}
	public void verifyTable() {
		try {
			mDb.rawQuery("SELECT row_time,number,name FROM "+DATABASE_TABLE+" LIMIT 1", null);
		} catch (SQLException ex) {
			dbRecreate();
		}
	}
	public void truncateCallTable() {
		try {
			mDb.delete(DATABASE_TABLE, null, null);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	public String[] getNameAndNumber(String id) {
		String[] result = null;
		
		Cursor cursor = mDb.rawQuery("SELECT number,name FROM "+DATABASE_TABLE+" WHERE _id=?", new String[] { id });
		if (cursor.moveToNext()) {
			result = new String[] {cursor.getString(0),cursor.getString(1)};
		}
		cursor.close();
		
		return result;
	}
}

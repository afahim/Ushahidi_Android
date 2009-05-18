package org.addhen.ushahidi.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.addhen.ushahidi.net.Incidents;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UshahidiDatabase {
	private static final String TAG = "UshahidiDatabase";

	public static final String INCIDENT_ID = "_id";
	public static final String INCIDENT_TITLE = "incident_title";
	public static final String INCIDENT_DESC = "incident_desc";
	public static final String INCIDENT_DATE = "incident_date";
	public static final String INCIDENT_MODE = "incident_mode";
	public static final String INCIDENT_VERIFIED = "incident_verified";
	public static final String INCIDENT_LOC_NAME = "incident_loc_name";
	public static final String INCIDENT_LOC_LATITUDE = "incident_loc_latitude";
	public static final String INCIDENT_LOC_LONGITUDE = "incident_loc_longitude";
	public static final String INCIDENT_CATEGORIES = "incident_categories";
	public static final String INCIDENT_MEDIA = "incident_media";
	public static final String INCIDENT_IS_UNREAD = "is_unread";
	
	
	public static final String CATEGORY_ID = "_id";
	public static final String CATEGORY_TITLE = "category_title";
	public static final String CATEGORY_DESC = "category_desc";
	public static final String CATEGORY_COLOR = "category_color";
	public static final String CATEGORY_IS_UNREAD = "is_unread";
	
	public static final String[] INCIDENTS_COLUMNS = new String[] {	INCIDENT_ID,
		INCIDENT_TITLE, INCIDENT_DESC, INCIDENT_DATE, INCIDENT_MODE, INCIDENT_VERIFIED,
		INCIDENT_LOC_NAME,INCIDENT_LOC_LATITUDE,INCIDENT_LOC_LONGITUDE,INCIDENT_CATEGORIES,
		INCIDENT_MEDIA,INCIDENT_IS_UNREAD
	};
	
	public static final String[] CATEGORIES_COLUMNS = new String[] { CATEGORY_ID,
		CATEGORY_TITLE,CATEGORY_DESC,CATEGORY_COLOR, CATEGORY_IS_UNREAD
	};
	
  
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_NAME = "ushahidi_db";

	private static final String INCIDENTS_TABLE = "incidents";
	private static final String CATEGORIES_TABLE = "categories";

	private static final int DATABASE_VERSION = 9;

  // NOTE: the incident ID is used as the row ID.
  // Furthermore, if a row already exists, an insert will replace
  // the old row upon conflict.
	
	private static final String INCIDENTS_TABLE_CREATE = "CREATE TABLE " + INCIDENTS_TABLE + " ("
		+ INCIDENT_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, "  
		+ INCIDENT_TITLE + " TEXT NOT NULL, "
		+ INCIDENT_DESC + " TEXT, "
		+ INCIDENT_DATE + " DATE NOT NULL, "
		+ INCIDENT_MODE + " INTEGER, "
		+ INCIDENT_VERIFIED + " INTEGER, "
		+ INCIDENT_LOC_NAME + " TEXT NOT NULL, "
		+ INCIDENT_LOC_LATITUDE + " TEXT NOT NULL, "
		+ INCIDENT_LOC_LONGITUDE + " TEXT NOT NULL, "
		+ INCIDENT_CATEGORIES + " TEXT NOT NULL, "
		+ INCIDENT_MEDIA + " TEXT, "
		+ INCIDENT_IS_UNREAD + " BOOLEAN NOT NULL "
		+ ")";
	
	private static final String CATEGORIES_TABLE_CREATE = "CREATE TABLE " + CATEGORIES_TABLE + " ("
		+ CATEGORY_ID + " INTEGER PRIMARY KEY ON CONFLICT REPLACE, "
		+ CATEGORY_TITLE + " TEXT NOT NULL, " 
		+ CATEGORY_DESC + " TEXT, " 
		+ CATEGORY_COLOR + " TEXT, "
		+ CATEGORY_IS_UNREAD + " BOOLEAN NOT NULL "
		+ ")";
	

	private final Context mContext;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(INCIDENTS_TABLE_CREATE);
    		db.execSQL(CATEGORIES_TABLE_CREATE);
		}

    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
    				+ newVersion + " which destroys all old data");
    		db.execSQL("DROP TABLE IF EXISTS " + INCIDENTS_TABLE);
      		db.execSQL("DROP TABLE IF EXISTS " + CATEGORIES_TABLE);
      		onCreate(db);
    	}
	}

	public UshahidiDatabase(Context context) {
		this.mContext = context;
	}

  	public UshahidiDatabase open() throws SQLException {
  		mDbHelper = new DatabaseHelper(mContext);
	  	mDb = mDbHelper.getWritableDatabase();

	  	return this;
  	}

  	public void close() {
  		mDbHelper.close();
  	}

  	public long createIncidents(IncidentsData incidents, boolean isUnread) {
  		ContentValues initialValues = new ContentValues();
  		
    	initialValues.put(INCIDENT_ID, incidents.getIncidentId());
    	initialValues.put(INCIDENT_TITLE, incidents.getIncidentTitle());
    	initialValues.put(INCIDENT_DESC, incidents.getIncidentDesc());
    	initialValues.put(INCIDENT_DATE, incidents.getIncidentDate());
    	initialValues.put(INCIDENT_MODE, incidents.getIncidentMode());
    	initialValues.put(INCIDENT_VERIFIED, incidents.getIncidentVerified());
    	initialValues.put(INCIDENT_LOC_NAME, incidents.getIncidentLocation());
    	initialValues
        	.put(INCIDENT_LOC_LATITUDE, incidents.getIncidentLocLatitude());
    	initialValues.put(INCIDENT_LOC_LONGITUDE, incidents.getIncidentLocLongitude());
    	initialValues.put(INCIDENT_CATEGORIES, incidents.getIncidentCategories());
    	initialValues.put(INCIDENT_MEDIA, incidents.getIncidentMedia());
    	initialValues.put(INCIDENT_IS_UNREAD, isUnread);

    	return mDb.insert(INCIDENTS_TABLE, null, initialValues);
  	}

  	public long createCategories(CategoriesData categories, boolean isUnread) {
  		ContentValues initialValues = new ContentValues();
  		initialValues.put(CATEGORY_ID, categories.getCategoryId());
  		initialValues.put(CATEGORY_TITLE, categories.getCategoryTitle());
  		initialValues.put(CATEGORY_DESC, categories.getCategoryDescription());
  		initialValues.put(CATEGORY_COLOR, categories.getCategoryColor());
  		initialValues.put(CATEGORY_IS_UNREAD, isUnread);
  		
  		return mDb.insert(CATEGORIES_TABLE, null, initialValues);
  	}

  	public int addNewIncidentsAndCountUnread(ArrayList<IncidentsData> newIncidents) {
  		addIncidents(newIncidents, true);
  		return fetchUnreadCount();
  	}

  public Cursor fetchAllIncidents() {
    return mDb.query(INCIDENTS_TABLE, INCIDENTS_COLUMNS, null, null, null, null, INCIDENT_ID
        + " DESC");
  }

  
  public Cursor fetchAllCategories() {
    return mDb.query(CATEGORIES_TABLE, CATEGORIES_COLUMNS, null, null, null, null, CATEGORY_ID
        + " DESC");
  }

  	public Cursor fetchIncidentsByCategories( String filter ) {
  		String likeFilter = '%' + filter + '%';
  		String sql = "SELECT * FROM "+INCIDENTS_TABLE+" WHERE "+INCIDENT_CATEGORIES+" LIKE ? ORDER BY "
  			+INCIDENT_TITLE+" COLLATE NOCASE";
  		return mDb.rawQuery(sql, new String[] { likeFilter } );
  	}
  
  	public Cursor fetchIncidentsById( String id ) {
  		  		String sql = "SELECT * FROM "+INCIDENTS_TABLE+" WHERE "+INCIDENT_ID+" = ? ORDER BY "
  			+INCIDENT_TITLE+" COLLATE NOCASE";
  		return mDb.rawQuery(sql, new String[] { id } );
  	}

  	public void clearData() {
  		// TODO: just wipe the database.
  		deleteAllIncidents();
    	deleteAllCategories();
  	}

  	public boolean deleteAllIncidents() {
	  return mDb.delete(INCIDENTS_TABLE, null, null) > 0;
  	}

  	public boolean deleteAllCategories() {
	  return mDb.delete(CATEGORIES_TABLE, null, null) > 0;
  	}

  	public boolean deleteCategory(int id) {
	  return mDb.delete(CATEGORIES_TABLE, CATEGORY_ID + "=" + id, null) > 0;
  	}

  	public void markAllIncidentssRead() {
	  ContentValues values = new ContentValues();
    	values.put(INCIDENT_IS_UNREAD, 0);
    	mDb.update(INCIDENTS_TABLE, values, null, null);
  	}

  	public void markAllCategoriesRead() {
	  ContentValues values = new ContentValues();
    	values.put(CATEGORY_IS_UNREAD, 0);
    	mDb.update(CATEGORIES_TABLE, values, null, null);
  	}

  	public int fetchMaxId() {
  		Cursor mCursor = mDb.rawQuery("SELECT MAX(" + INCIDENT_ID + ") FROM "
        + INCIDENTS_TABLE, null);

  		int result = 0;

  		if (mCursor == null) {
  			return result;
  		}

  		mCursor.moveToFirst();
  		result = mCursor.getInt(0);
  		mCursor.close();

  		return result;
  	}

  	public int fetchUnreadCount() {
  		Cursor mCursor = mDb.rawQuery("SELECT COUNT(" + INCIDENT_ID + ") FROM "
  				+ INCIDENTS_TABLE + " WHERE " + INCIDENT_IS_UNREAD + " = 1", null);

  		int result = 0;

  		if (mCursor == null) {
  			return result;
  		}

  		mCursor.moveToFirst();
  		result = mCursor.getInt(0);
  		mCursor.close();

  		return result;
  	}

  	/*public int fetchMaxCategoryId(boolean isSent) {
  		Cursor mCursor = mDb.rawQuery("SELECT MAX(" + CATEGORY_ID + ") FROM " + CATEGORIES_TABLE
  				+ " WHERE " + CATEGORIES_IS_SENT + " = ?", new String[] { isSent ? "1" : "0" });

  		int result = 0;

  		if (mCursor == null) {
  			return result;
  		}

  		mCursor.moveToFirst();
  		result = mCursor.getInt(0);
  		mCursor.close();

  		return result;
  	}*/

  	public int addNewCategoryAndCountUnread(List<CategoriesData> categories) {
  		addCategories(categories, true);

  		return fetchUnreadCategoriesCount();
  	}

  	public int fetchCategoriesCount() {
  		Cursor mCursor = mDb.rawQuery("SELECT COUNT(" + CATEGORY_ID + ") FROM "
  				+ CATEGORIES_TABLE, null);

  		int result = 0;

  		if (mCursor == null) {
  			return result;
  		}

    mCursor.moveToFirst();
    result = mCursor.getInt(0);
    mCursor.close();

    return result;
  }
  
  	private int fetchUnreadCategoriesCount() {
  		Cursor mCursor = mDb.rawQuery("SELECT COUNT(" + CATEGORY_ID + ") FROM "
  				+ CATEGORIES_TABLE + " WHERE " + CATEGORY_IS_UNREAD + " = 1", null);

  		int result = 0;

  		if (mCursor == null) {
  			return result;
  		}

  		mCursor.moveToFirst();
  		result = mCursor.getInt(0);
  		mCursor.close();

  		return result;
  	}

  	public void addIncidents(List<IncidentsData> incidents, boolean isUnread) {
  		try {
  			mDb.beginTransaction();

  			for (IncidentsData incident : incidents) {
  				createIncidents(incident, isUnread);
  			}

  			limitRows(INCIDENTS_TABLE, 20, INCIDENT_ID);
  			mDb.setTransactionSuccessful();
  		} finally {
  			mDb.endTransaction();
  		}
  	}

  	public void addCategories(List<CategoriesData> categories, boolean isUnread) {
  		try {
  			mDb.beginTransaction();

  			for (CategoriesData category : categories) {
  				createCategories(category, isUnread);
  			}

  			limitRows(CATEGORIES_TABLE, 20, CATEGORY_ID);
  			mDb.setTransactionSuccessful();
  		} finally {
  			mDb.endTransaction();
  		}
  	}

  	public int limitRows(String tablename, int limit, String KEY_ID) {
  		Cursor cursor = mDb.rawQuery("SELECT " + KEY_ID + " FROM " + tablename
  				+ " ORDER BY " + KEY_ID + " DESC LIMIT 1 OFFSET ?",
  				new String[] { limit - 1 + "" });

  		int deleted = 0;

  		if (cursor != null && cursor.moveToFirst()) {
  			int limitId = cursor.getInt(0);
  			deleted = mDb.delete(tablename, KEY_ID + "<" + limitId, null);
  		}

  		cursor.close();

  		return deleted;
  	}

}

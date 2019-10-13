package com.lowenhardt.pickup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.lowenhardt.pickup.models.ContactConfig;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Database extends SQLiteOpenHelper {
    private static final String TAG = Database.class.getName();

    static private final String DATABASE_NAME = "MyDB";

    static private final int DATABASE_VERSION = 1;

    private static final int TRUE_VALUE = 1;
    private static final int FALSE_VALUE = 0;

    // contacts config info
    private static final String CONTACTS_CONFIG_TABLE_NAME = "contacts_config_table";
    private static final String CONTACTS_CONFIG_COLUMN_ID = "_id";
    private static final String CONTACTS_CONFIG_COLUMN_NAME = "contact_config_name";
    private static final String CONTACTS_CONFIG_COLUMN_PHONE_NUMBER = "contact_config_number";
    private static final String CONTACTS_CONFIG_COLUMN_INTERVAL_SECONDS = "contact_config_interval";
    private static final String CONTACTS_CONFIG_COLUMN_NUM_CALLS_TO_CHANGE_MODE = "num_calls_to_change_mode";
    private static final String CONTACTS_CONFIG_COLUMN_VOLUME_WHEN_UNMUTED = "volume_when_unmuted";
    private static final String CONTACTS_CONFIG_COLUMN_RECENT_CALLS = "contact_config_calls";

    private static final String CREATE_TABLE_CONTACTS_CONFIG =
            "CREATE TABLE " + CONTACTS_CONFIG_TABLE_NAME + "("
                    + CONTACTS_CONFIG_COLUMN_ID +             " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + CONTACTS_CONFIG_COLUMN_NAME             + " TEXT, "
                    + CONTACTS_CONFIG_COLUMN_PHONE_NUMBER     + " TEXT, "
                    + CONTACTS_CONFIG_COLUMN_INTERVAL_SECONDS + " INTEGER, "
                    + CONTACTS_CONFIG_COLUMN_NUM_CALLS_TO_CHANGE_MODE + " INTEGER, "
                    + CONTACTS_CONFIG_COLUMN_VOLUME_WHEN_UNMUTED + " INTEGER, "
                    + CONTACTS_CONFIG_COLUMN_RECENT_CALLS + " TEXT" + ")";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
        if (!isTableExists(db, CONTACTS_CONFIG_TABLE_NAME)) {
            Crashlytics.log(Log.INFO, TAG, CONTACTS_CONFIG_TABLE_NAME+" table doesn't exist, creating...");
            db.execSQL(CREATE_TABLE_CONTACTS_CONFIG);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String logText = "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data";
        Crashlytics.log(Log.WARN, TAG, logText);
        db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_CONFIG_TABLE_NAME);
        createTables(db);
        Crashlytics.log(Log.WARN, TAG, "Finished upgrading DB, newVersion: "+newVersion);
    }

    public ContactConfig addOrUpdate(ContactConfig contactConfig, Context context) {
        if (contactConfig.getId() == -1) {
            return addContactConfig(contactConfig, context);
        }

        SQLiteDatabase db = null;
        try {
            ContentValues values = contactConfigToContentValues(contactConfig);
            db = this.getWritableDatabase();
            String where = CONTACTS_CONFIG_COLUMN_ID+"=?";
            String[] whereArgs = new String[] {String.valueOf(contactConfig.getId())};
            long id = db.update(CONTACTS_CONFIG_TABLE_NAME, values, where, whereArgs);
            Crashlytics.log(Log.INFO, TAG, "Updated contactConfig in DB, reminder: "+contactConfig);
            contactConfig.setId(id);
            return contactConfig;
        } catch (SQLiteException e) {
            ErrorLogger.log(context, TAG, "Exception while adding ContactConfig", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return null;
    }

    // This will insert the reminder as a new row, even if it already exists with ID!
    private ContactConfig addContactConfig(ContactConfig contactConfig, Context context) {
        if (contactConfig == null) {
            ErrorLogger.log(context, TAG, "Tried to add null contactConfig", true);
            return null;
        }

        SQLiteDatabase db = null;
        try {
            ContentValues values = contactConfigToContentValues(contactConfig);
            db = this.getWritableDatabase();
            long id = db.insertOrThrow(CONTACTS_CONFIG_TABLE_NAME, null, values);
            contactConfig.setId(id);
            Crashlytics.log(Log.INFO, TAG, "Added new contactConfig to DB, reminder: "+contactConfig);
            return contactConfig;
        } catch (SQLiteException e) {
            ErrorLogger.log(context, TAG, "Exception while adding contactConfig", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return null;
    }

    private ContentValues contactConfigToContentValues(ContactConfig contactConfig) {
        ContentValues values = new ContentValues();
        if (contactConfig.getId() != -1) {
            values.put(CONTACTS_CONFIG_COLUMN_ID, contactConfig.getId());
        }
        values.put(CONTACTS_CONFIG_COLUMN_NAME, contactConfig.getName());
        values.put(CONTACTS_CONFIG_COLUMN_PHONE_NUMBER, contactConfig.getPhoneNumber());
        values.put(CONTACTS_CONFIG_COLUMN_INTERVAL_SECONDS, contactConfig.getCallIntervalSeconds());
        values.put(CONTACTS_CONFIG_COLUMN_NUM_CALLS_TO_CHANGE_MODE, contactConfig.getNumCallsToChangeMode());
        values.put(CONTACTS_CONFIG_COLUMN_VOLUME_WHEN_UNMUTED, contactConfig.getVolumeWhenUnmuted());
        values.put(CONTACTS_CONFIG_COLUMN_RECENT_CALLS, ContactConfig.serializeCalls(contactConfig.getCalls()));
        return values;
    }

    /**
     * Removes contact config from the database.
     *
     * @param id the id of the contact config
     * @param context Context
     */
    void removeContactConfig(long id, Context context) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String[] args = {Long.toString(id)};
            long removed = db.delete(CONTACTS_CONFIG_TABLE_NAME, CONTACTS_CONFIG_COLUMN_ID+"=?", args);
            Crashlytics.log(Log.INFO, TAG, "Removed contact config from DB, id: "+id+", numRemoved: "+removed);
        } catch (SQLiteException e) {
            if (context != null) {
                ErrorLogger.log(context, TAG, "Exception while trying to remove scheduled reminder, id: " + id, e);
            }
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    void removeContactConfigs(List<Long> ids, Context context) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String[] args = {TextUtils.join(",", ids)};
            long deleted = db.delete(CONTACTS_CONFIG_TABLE_NAME, CONTACTS_CONFIG_COLUMN_ID+ " IN (?)", args);
            Crashlytics.log(Log.INFO, TAG, "Removed contact configs, ids: "+args[0]+", deleted: "+deleted);
        } catch (SQLiteException e) {
            String idsStr = TextUtils.join(",", ids);
            ErrorLogger.log(context, TAG, "Exception while trying to remove scheduled reminders, ids: "+idsStr, e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public @Nullable ContactConfig getContactConfig(long id, Context context) {
        ContactConfig contactConfig = null;
        SQLiteDatabase db = null;

        try {
            db = getReadableDatabase();
            if (!isTableExists(db, CONTACTS_CONFIG_TABLE_NAME)) {
                Crashlytics.log(Log.WARN, TAG, CONTACTS_CONFIG_TABLE_NAME+" table doesn't exist, can't get item");
                return null;
            }

            String selection = CONTACTS_CONFIG_COLUMN_ID+"=?";
            String[] args = {Long.toString(id)};
            Cursor cursor = db.query(CONTACTS_CONFIG_TABLE_NAME, null, selection, args, null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                contactConfig = cursorToContactConfig(cursor);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (SQLiteException e) {
            ErrorLogger.log(context, TAG, "Exception while trying to get reminders", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return contactConfig;
    }

    @NonNull List<ContactConfig> getAllContactConfigs(Context context) {
        List<ContactConfig> configs = new ArrayList<>();
        SQLiteDatabase db = null;

        try {
            db = getReadableDatabase();
            if (!isTableExists(db, CONTACTS_CONFIG_TABLE_NAME)) {
                Crashlytics.log(Log.WARN, TAG, CONTACTS_CONFIG_TABLE_NAME+" table doesn't exist, nothing to get");
                return configs;
            }

            Cursor cursor = db.query(CONTACTS_CONFIG_TABLE_NAME, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    configs.add(cursorToContactConfig(cursor));
                } while (cursor.moveToNext());
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (SQLiteException e) {
            ErrorLogger.log(context, TAG, "Exception while trying to get reminders", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }

        return configs;
    }

    public void removeAll(Context context) {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            if (isTableExists(db, CONTACTS_CONFIG_TABLE_NAME)) {
                long deleted = db.delete(CONTACTS_CONFIG_TABLE_NAME, "1", null);
                Crashlytics.log(Log.INFO, TAG, "Deleted all reminders, num deleted: "+deleted);
            }
        } catch (SQLiteException e) {
            ErrorLogger.log(context, TAG, "Exception while trying to remove all reminders", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private ContactConfig cursorToContactConfig(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_NAME));
        String phoneNumber = cursor.getString(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_PHONE_NUMBER));
        int intervalSeconds = cursor.getInt(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_INTERVAL_SECONDS));
        int numCallsToChangeMode = cursor.getInt(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_NUM_CALLS_TO_CHANGE_MODE));
        int volumeWhenUnmuted = cursor.getInt(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_VOLUME_WHEN_UNMUTED));
        ArrayList<Date> recentCalls = ContactConfig.deserializeCalls(cursor.getString(cursor.getColumnIndex(CONTACTS_CONFIG_COLUMN_RECENT_CALLS)));

        return new ContactConfig(id,
                name,
                phoneNumber,
                intervalSeconds,
                numCallsToChangeMode,
                volumeWhenUnmuted,
                recentCalls);
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean isTableExists(SQLiteDatabase db, String tableName) {
        if (tableName == null || db == null || !db.isOpen()) {
            return false;
        }
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = ? AND name = ?", new String[] {"table", tableName});
        if (!cursor.moveToFirst()) {
            return false;
        }
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }
}
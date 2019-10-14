package com.lowenhardt.pickup;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.lowenhardt.pickup.models.Contact;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class AsyncQueryContacts extends AsyncTask<Void, Void, ArrayList<Contact>> {

    private static final String TAG = AsyncQueryContacts.class.getSimpleName();

    private WeakReference<Context> weakContext;
    private QueryCB cb;

    interface QueryCB {
        void onSuccess(ArrayList<Contact> contacts);
        void onFailure();
    }

    AsyncQueryContacts(Context context, QueryCB cb) {
        this.weakContext = new WeakReference<>(context);
        this.cb = cb;
    }

    @Override
    protected ArrayList<Contact> doInBackground(Void... voids) {
        if (ContactsCache.isCacheSet()) {
            Crashlytics.log(Log.INFO, TAG, "Contacts cache is set, not re-querying");
            return ContactsCache.getContacts();
        }

        Crashlytics.log(Log.INFO, TAG, "Loading contacts asynchronously");

        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };

        String selection = "(" + ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1' " +
                "AND (" + ContactsContract.Contacts.HAS_PHONE_NUMBER + " != 0 ))";

        ArrayList<Contact> list = new ArrayList<>();
        Context context = this.weakContext.get();
        if (context == null) {
            Crashlytics.log(Log.INFO, TAG, "Context has been cleared, not getting contacts");
            return null;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);
        if (cursor == null) {
            Crashlytics.log(Log.INFO, TAG, "null cursor, no contacts loaded");
            cancel(true);
            return null;
        }

        // no contacts is still success
        if (cursor.getCount() == 0) {
            Crashlytics.log(Log.INFO, TAG, "No contacts to load");
            cursor.close();
            return list;
        }

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Cursor cursorInfo = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id},
                    null);

            if (cursorInfo == null) {
                continue;
            }

            while (cursorInfo.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String number = cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                list.add(new Contact(name, number));
            }

            cursorInfo.close();
        }

        cursor.close();

        Crashlytics.log(Log.INFO, TAG, "Loaded "+list.size()+" contacts");

        return list;
    }

    @Override
    protected void onPostExecute(ArrayList<Contact> contacts) {
        super.onPostExecute(contacts);

        ContactsCache.setContacts(contacts);

        if (this.cb == null) {
            return;
        }

        if (contacts == null) {
            cb.onFailure();
            return;
        }

        cb.onSuccess(contacts);
    }

    @Override
    protected void onCancelled(ArrayList<Contact> contacts) {
        super.onCancelled(contacts);

        ContactsCache.setContacts(null);

        if (cb == null) {
            return;
        }

        cb.onFailure();
    }
}
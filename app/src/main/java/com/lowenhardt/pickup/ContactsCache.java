package com.lowenhardt.pickup;

import android.util.Log;

import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.lowenhardt.pickup.models.Contact;

import java.util.ArrayList;

class ContactsCache {

    static private final String TAG = ContactsCache.class.getSimpleName();

    static private boolean cacheSet = false;
    static private ArrayList<Contact> contacts;

    static void setContacts(@Nullable ArrayList<Contact> newContacts) {
        cacheSet = true;
        contacts = newContacts;

        Crashlytics.log(Log.INFO, TAG, "Contacts set, count: " +
                (newContacts != null ? newContacts.size() : "null"));
    }

    static boolean isCacheSet() {
        return cacheSet;
    }

    static ArrayList<Contact> getContacts() {
        return contacts;
    }

}

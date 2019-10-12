package com.lowenhardt.pickup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI;

public class ContactConfig implements Comparable<ContactConfig> {

    private static final String TAG = ContactConfig.class.getSimpleName();

    public static final long INVALID_ID = -1;

    private long _id; // database id, save for updating
    private String name;
    private String phoneNumber;
    private int callIntervalSeconds;
    private int numCallsToChangeMode;
    private int volumeWhenUnmuted;
    private ArrayList<Date> calls;

    /**
     * Configuration per contact.
     *
     * @param _id the id of the contact config in DB
     * @param name the nameET of the config
     * @param phoneNumber the contact phone number
     * @param callIntervalSeconds how much time back to look for calls from this number
     * @param numCallsToChangeMode num calls from this number before changing ringer mode
     * @param volumeWhenUnmuted volume for when device is unmuted
     * @param calls a list of the recent calls
     */
    ContactConfig(long _id,
                         String name,
                         String phoneNumber,
                         int callIntervalSeconds,
                         int numCallsToChangeMode,
                         int volumeWhenUnmuted,
                         ArrayList<Date> calls) {
        this._id = _id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.callIntervalSeconds = callIntervalSeconds;
        this.numCallsToChangeMode = numCallsToChangeMode;
        this.volumeWhenUnmuted = volumeWhenUnmuted;
        this.calls = calls;
    }

    public long getId() {
        return _id;
    }

    public void setId(long id) {
        this._id = id;
    }

    public String getName() {
        return name;
    }

    @NonNull String getPhoneNumber() {
        return phoneNumber;
    }

    Uri getPhoneNumberUri() {
        return Uri.withAppendedPath(CONTENT_FILTER_URI, Uri.encode(getPhoneNumber()));
    }

    int getCallIntervalSeconds() {
        return callIntervalSeconds;
    }

    int getCallIntervalMinutes() {
        return getCallIntervalSeconds() / 60;
    }

    int getNumCallsToChangeMode() {
        return numCallsToChangeMode;
    }


    int getVolumeWhenUnmuted() {
        return volumeWhenUnmuted;
    }

    ArrayList<Date> getCalls() {
        return calls;
    }

    void addCall(Date date) {
        if (calls == null) {
            calls = new ArrayList<>();
        }

        calls.add(date);
    }

    boolean shouldChangeMode() {
        if (calls == null) {
            return false;
        }

        if (numCallsToChangeMode > calls.size()) {
            Crashlytics.log(Log.INFO, TAG, "There are still no "+numCallsToChangeMode+" calls from this number, not changing");
            return false;
        }

        Date lastCallDate = calls.get(numCallsToChangeMode - 1);
        if (lastCallDate == null) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -1 * callIntervalSeconds);
        if (lastCallDate.before(calendar.getTime())) {
            Crashlytics.log(Log.INFO, TAG, "Call #"+numCallsToChangeMode+" isn't in interval, date: "+lastCallDate);
            return false;
        }

        Crashlytics.log(Log.INFO, TAG, "Ringer mode should be changed, call #"+numCallsToChangeMode+" was at: "+lastCallDate);
        return true;
    }

    // returns how many calls the contact called in the defined interval
    int getNumCallsInInterval() {
        if (calls == null || calls.isEmpty()) {
            return 0;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -1 * getCallIntervalSeconds());

        int numCalls = 0;
        for (Date call : calls) {
            if (call.after(calendar.getTime())) {
                numCalls += 1;
            }
        }

        return numCalls;
    }

    @Override
    public @NonNull String toString() {
        return "ContactConfig | name: "+name+", id: "+_id;
    }

    void fillIntent(Intent intent) {
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_ID, getId());
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_NAME, getName());
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_PHONE_NUMBER, getPhoneNumber());
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_INTERVAL_SECONDS, getCallIntervalSeconds());
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_INTERVAL_NUM_CALLS_BEFORE_MODE_CHANGE, getNumCallsToChangeMode());
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_VOLUME_WHEN_UNMUTED, getVolumeWhenUnmuted());
        intent.putExtra(Constants.EXTRA_CONTACT_CONFIG_INTERVAL_RECENT_CALLS, serializeCalls(calls));
    }

    @Nullable
    static ContactConfig fromIntent(Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        long id = bundle.getLong(Constants.EXTRA_CONTACT_CONFIG_ID, -1);
        if (id == -1) {
            return null;
        }

        String name = bundle.getString(Constants.EXTRA_CONTACT_CONFIG_NAME);
        String phoneNumber = bundle.getString(Constants.EXTRA_CONTACT_CONFIG_PHONE_NUMBER);
        int intervalSeconds = bundle.getInt(Constants.EXTRA_CONTACT_CONFIG_INTERVAL_SECONDS);
        int numCallsBeforeModeChange = bundle.getInt(Constants.EXTRA_CONTACT_CONFIG_INTERVAL_NUM_CALLS_BEFORE_MODE_CHANGE);
        int volumeWhenUnmuted = bundle.getInt(Constants.EXTRA_CONTACT_CONFIG_VOLUME_WHEN_UNMUTED);
        ArrayList<Date> recentCalls = deserializeCalls(bundle.getString(Constants.EXTRA_CONTACT_CONFIG_INTERVAL_RECENT_CALLS));

        return new ContactConfig(id,
                name,
                phoneNumber,
                intervalSeconds,
                numCallsBeforeModeChange,
                volumeWhenUnmuted,
                recentCalls);
    }


    static @Nullable String serializeCalls(ArrayList<Date> calls) {
        if (calls == null) {
            return null;
        }

        if (calls.size() > Constants.MAX_NUM_OF_RECENT_CALLS) {
            calls = new ArrayList<>(calls.subList(0, Constants.MAX_NUM_OF_RECENT_CALLS));
        }

        return new Gson().toJson(calls);
    }

    static @Nullable ArrayList<Date> deserializeCalls(String calls) {
        if (calls == null) {
            return null;
        }

        return new Gson().fromJson(calls, new TypeToken<ArrayList<Date>>(){}.getType());
    }

    @Override
    public int compareTo(ContactConfig o) {
        return getName().compareTo(o.getName());
    }

}

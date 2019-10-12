package com.lowenhardt.pickup;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

public class MyPreferenceManager {

    private static final String TAG = MyPreferenceManager.class.getSimpleName();

    private Context context;
    private SharedPreferences sp;

    private static final String TRACKED_NUMBER = "0747112660";
    private static final int CALL_INTERVAL_SECONDS = 60*10; // 10 minutes
//    private static final String CALL_INFO_1_NUMBER = "call_info_1_number";
    private static final String CALL_INFO_1_DATE = "call_info_1_date";
//    private static final String CALL_INFO_2_NUMBER = "call_info_2_number";
    private static final String CALL_INFO_2_DATE = "call_info_2_date";
//    private static final String CALL_INFO_3_NUMBER = "call_info_3_number";
    private static final String CALL_INFO_3_DATE = "call_info_3_date";
//    private static final String CALL_INFO_4_NUMBER = "call_info_4_number";
    private static final String CALL_INFO_4_DATE = "call_info_4_date";
//    private static final String CALL_INFO_5_NUMBER = "call_info_5_number";
    private static final String CALL_INFO_5_DATE = "call_info_5_date";


    public MyPreferenceManager(@NonNull Context context) {
        this.context = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static @NonNull String getTrackedNumber() {
        return TRACKED_NUMBER;
    }

    public static int getCallIntervalSeconds() {
        return CALL_INTERVAL_SECONDS;
    }

    public void setIncomingCall(Date time) {
        long date1 = sp.getLong(CALL_INFO_1_DATE, -1);
        long date2 = sp.getLong(CALL_INFO_2_DATE, -1);
        long date3 = sp.getLong(CALL_INFO_3_DATE, -1);
        long date4 = sp.getLong(CALL_INFO_4_DATE, -1);

        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(CALL_INFO_5_DATE, date4);
        editor.putLong(CALL_INFO_4_DATE, date3);
        editor.putLong(CALL_INFO_3_DATE, date2);
        editor.putLong(CALL_INFO_2_DATE, date1);
        editor.putLong(CALL_INFO_1_DATE, time.getTime());

        editor.apply();
    }

    public @Nullable Date getCallDate(int index) {
        long date = -1;
        switch (index) {
            case 1:
                date = sp.getLong(CALL_INFO_1_DATE, -1);
                break;
            case 2:
                date = sp.getLong(CALL_INFO_2_DATE, -1);
                break;
            case 3:
                date = sp.getLong(CALL_INFO_3_DATE, -1);
                break;
            case 4:
                date = sp.getLong(CALL_INFO_4_DATE, -1);
                break;
            case 5:
                date = sp.getLong(CALL_INFO_5_DATE, -1);
                break;
            default: return null;
        }
        return date == -1 ? null : new Date(date);
    }

}
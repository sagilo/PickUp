package com.lowenhardt.pickup;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;

import java.util.Calendar;

public class ErrorLogger {

    static private String error_1_key;
    static private String error_2_key;
    static private String error_3_key;
    static private String error_4_key;
    static private String error_5_key;

    static void init(Context context) {
        error_1_key = context.getString(R.string.error_latest_1);
        error_2_key = context.getString(R.string.error_latest_2);
        error_3_key = context.getString(R.string.error_latest_3);
        error_4_key = context.getString(R.string.error_latest_4);
        error_5_key = context.getString(R.string.error_latest_5);
    }

    static private String toString(Exception e) {
        if (e == null) {
            return "";
        }

        String str = " | exception: "+ e.toString();
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length == 0) {
            return str + " | no stack trace";
        }
        StackTraceElement element = stackTrace[0];
        str += " | " +element.getMethodName()+"("+element.getFileName()+":"+element.getLineNumber()+")";
        return str;
    }

    // don't do anything that requires main thread
    static public void log(Context context, String tag, String msg, Exception e) {
        String exceptionStr = toString(e);
        if (context != null) {
            addError(context, tag + " | " + msg + exceptionStr);
        } else {
            Crashlytics.log(Log.ERROR, tag, "Can't log error to persistent, null context");
        }

        Crashlytics.log(Log.ERROR, tag, msg+exceptionStr);
        if (e != null) {
            Crashlytics.logException(e);
        }
    }

    static public void log(Context context, String tag, String msg) {
        log(context, tag, msg, false);
    }

    static public void log(Context context, String tag, String msg, boolean logAsCrashlyticsException) {
        if (context != null) {
            addError(context, tag + " | " + msg);
        } else {
            Crashlytics.log(Log.ERROR, tag, "Can't log error to persistent, null context");
        }

        if (logAsCrashlyticsException) {
            Crashlytics.logException(new Exception(tag+": "+msg));
        } else {
            Crashlytics.log(Log.ERROR, tag, msg);
        }
    }

    static private void addError(Context context, String error) {
        String line = Calendar.getInstance().getTime().toString();
        line += " | " + error;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(error_5_key, sp.getString(error_4_key, null));
        editor.putString(error_4_key, sp.getString(error_3_key, null));
        editor.putString(error_3_key, sp.getString(error_2_key, null));
        editor.putString(error_2_key, sp.getString(error_1_key, null));
        editor.putString(error_1_key, line);
        editor.apply();
    }
}

package com.lowenhardt.pickup;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.crashlytics.android.Crashlytics;

import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_PHONE_STATE;

class Utils {

    @NonNull
    static String getString(EditText editText) {
        return editText.getText().toString().trim();
    }

    static Integer getInt(EditText editText) {
        try {
            return Integer.parseInt(getString(editText));
        } catch (Exception e) {
            return null;
        }
    }

    static boolean checkReadPhoneStatePermission(Context context) {
        return checkPermission(context, READ_PHONE_STATE);
    }

    static boolean checkReadCallLogPermission(Context context) {
        return checkPermission(context, READ_CALL_LOG);
    }

    static boolean checkReadContactsPermission(Context context) {
        return checkPermission(context, READ_CONTACTS);
    }

    private static boolean checkPermission(Context context, String permission) {
        if (context == null) {
            Crashlytics.log(Log.WARN, "checkPermission", "null context, can't check for permission");
            return false;
        }

        int result = ContextCompat.checkSelfPermission(context, permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    static boolean isMarshmallowOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    static boolean isOreoOrHigher() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }
}
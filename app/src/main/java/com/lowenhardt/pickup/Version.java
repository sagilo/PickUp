package com.lowenhardt.pickup;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

class Version {

    static String appVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            if (pInfo != null) {
                return pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    static int codeVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            if (pInfo != null) {
                return pInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

}

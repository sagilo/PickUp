package com.lowenhardt.pickup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class ContactUs {
    protected static String TAG = ContactUs.class.getName();

    private final static String CONTACT_DIR_NAME = "contact_us_files"; // same as in paths.xml
    private final static String REPORT_INFO_FILE_NAME = "info.txt";
    private static final String AUTHORITY = "com.lowenhardt.pickup.fileprovider";

    static void contact(Activity activity) {
        contact(activity, null);
    }

    static void contact(Activity activity, String message) {

        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");

        String version = Version.appVersion(activity);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{activity.getString(R.string.contact_email)});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.app_name)+" ("+version+")");
        if (message != null) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);
        }

        addInfoAttachment(emailIntent, activity);

        String title = activity.getString(R.string.string_select_email_app);
        activity.startActivity(Intent.createChooser(emailIntent, title));
    }

    private static void addInfoAttachment(Intent emailIntent, Context context) {
        File infoFile = createInfoFile(context);
        if (infoFile == null) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(context, AUTHORITY, infoFile);
        ArrayList<Uri> uris = new ArrayList<>(1);
        uris.add(uri);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Workaround for Android bug.
        // grantUriPermission also needed for KITKAT
        // see https://code.google.com/p/android/issues/detail?id=76683
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(
                    emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }

    private static String getInstaller(final Context context) {
        return context.getPackageManager().getInstallerPackageName(context.getPackageName());
    }

    private static File createInfoFile(Context context) {
        File dirpath = new File(context.getFilesDir(), CONTACT_DIR_NAME);
        if (!dirpath.mkdirs() && (!dirpath.isDirectory())) {
            ErrorLogger.log(context, TAG, "Failed to create info file path: " + dirpath.getAbsolutePath(), true);
            return null;
        }

        File file = new File(dirpath, REPORT_INFO_FILE_NAME);
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        try {
            if (!file.createNewFile() && !file.isFile()) {
                Exception e = new Exception("Failed to create info file: " + file.getAbsolutePath());
                ErrorLogger.log(context, TAG,
                        "Failed to create info file" + file.getAbsolutePath(), e);
                return null;
            }
        } catch (IOException e) {
            ErrorLogger.log(context, TAG,
                    "Failed to create info file" + file.getAbsolutePath(), e);
            return null;
        }

        try {
            FileOutputStream fos = new FileOutputStream(file);
            String data = getInfo(context);
            fos.write(data.getBytes());
            fos.close();
            return file;
        } catch (IOException exception) {
            ErrorLogger.log(context, TAG, "Failed to write cached file: " + REPORT_INFO_FILE_NAME, exception);
        }
        return null;
    }

    private static String getInfo(Context context) {
        MyPreferenceManager mpm = new MyPreferenceManager(context);
        String data ="";
        data += "Android SDK version: " +android.os.Build.VERSION.SDK_INT + "\n";
        data += "Android version: " + android.os.Build.VERSION.RELEASE + "\n";
        data += "Google Play Services version: " + getGooglePlayServicesVersion(context) + "\n";
        data += "Device: " + android.os.Build.MODEL + "\n";
        data += "App Version: " + (Version.appVersion(context) + "\n");
        data += "Code Version: " + (Version.codeVersion(context) + "\n");
        data += "Installer: " + getInstaller(context) + "\n";
        data += "Timestamp: " + Calendar.getInstance().getTime() + "\n";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        data += "Error 1:" + sp.getString(context.getString(R.string.error_latest_1), "none") + "\n";
        data += "Error 2:" + sp.getString(context.getString(R.string.error_latest_2), "none") + "\n";
        data += "Error 3:" + sp.getString(context.getString(R.string.error_latest_3), "none") + "\n";
        data += "Error 4:" + sp.getString(context.getString(R.string.error_latest_4), "none") + "\n";
        data += "Error 5:" + sp.getString(context.getString(R.string.error_latest_5), "none") + "\n";
        return data;
    }

    static private int getGooglePlayServicesVersion(Context c) {
        try {
            return c.getPackageManager().getPackageInfo("com.google.android.gms", 0 ).versionCode;
        } catch (NameNotFoundException e) {
            ErrorLogger.log(c, TAG, "Exception while trying to get play services version", e);
        }
        return -1;
    }
}
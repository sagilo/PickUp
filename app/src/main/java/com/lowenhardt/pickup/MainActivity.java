package com.lowenhardt.pickup;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.crashlytics.android.Crashlytics;

import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_PHONE_STATE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_ACCESS_NOTIFICATION_POLICY_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkAllPermissionsGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            Crashlytics.log(Log.WARN, TAG, "reqPermissionResult grantResult len is 0");
            return;
        }

        switch (requestCode) {
            case Constants.REQUEST_PERMISSION_READ_PHONE_STATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Crashlytics.log(Log.INFO, TAG, "Read phone state permission granted");
                }
                break;
            case Constants.REQUEST_PERMISSION_READ_CALL_LOG:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Crashlytics.log(Log.INFO, TAG, "Read call log permission granted");
                }
                break;
            default:
                Crashlytics.log(Log.WARN, TAG, "Unexpected requestCode in onRequestPermissionsResult, " +
                        "reqCode="+requestCode);
        }

        checkAllPermissionsGranted();
    }

    private void checkAllPermissionsGranted() {
        if (!checkReadPhoneStatePermission()) {
            Crashlytics.log(Log.WARN, TAG, "read phone state permission isn't granted, requesting");
            return;
        }

        if (!checkReadCallLogPermissionGranted()) {
            Crashlytics.log(Log.WARN, TAG, "read call log permission isn't granted, requesting");
            return;
        }

        if (!checkReadContactsPermissionGranted()) {
            Crashlytics.log(Log.WARN, TAG, "read contacts permission isn't granted, requesting");
            return;
        }

        if (!checkAccessNotificationPolicyPermissionGranted()) {
            Crashlytics.log(Log.WARN, TAG, "access notification policy (DND) permissions isn't granted, requesting");
            return;
        }
    }

    private boolean checkReadPhoneStatePermission() {
        if (Utils.checkReadPhoneStatePermission(this)) {
            return true;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{READ_PHONE_STATE},
                Constants.REQUEST_PERMISSION_READ_PHONE_STATE);

        return false;
    }

    private boolean checkReadCallLogPermissionGranted() {
        if (Utils.checkReadCallLogPermission(this)) {
            return true;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{READ_CALL_LOG},
                Constants.REQUEST_PERMISSION_READ_CALL_LOG);

        return false;
    }

    private boolean checkReadContactsPermissionGranted() {
        if (Utils.checkReadContactsPermission(this)) {
            return true;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{READ_CONTACTS},
                Constants.REQUEST_PERMISSION_READ_CONTACTS);

        return false;
    }

    private boolean checkAccessNotificationPolicyPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        // check if the user granted notification policy access to change DND mode
        NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        if (!n.isNotificationPolicyAccessGranted()) {
            // Ask the user to grant access
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_ACCESS_NOTIFICATION_POLICY_PERMISSION);
            return false;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_contact:
                ContactUs.contact(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

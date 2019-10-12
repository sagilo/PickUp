package com.lowenhardt.pickup;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.BuildConfig;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.FirebaseApp;

import io.fabric.sdk.android.Fabric;

public class PickUpApplication extends MultiDexApplication {

    private static final String TAG = PickUpApplication.class.getSimpleName();

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initFabric();

        Crashlytics.log(Log.INFO, TAG, "Starting onCreate()");

        initFeatureFlights();
        ErrorLogger.init(this);
        MyPreferenceManager mpm = new MyPreferenceManager(this);
        if (Utils.isOreoOrHigher()) {
            createGeneralNotificationChannel();
        }
        Crashlytics.log(Log.DEBUG, TAG, "Finished onCreate()");
    }

    private void initFeatureFlights() {
        FirebaseApp.initializeApp(this);
//        final FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
//        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
//                .setMinimumFetchIntervalInSeconds(3600)
//                .build();
//
//        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
//        firebaseRemoteConfig.setDefaults(R.xml.feature_flights_defaults);
//        firebaseRemoteConfig.fetchAndActivate()
//                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Boolean> task) {
//                        if (task.isSuccessful()) {
//                            Boolean isFetched = task.getResult();
//                            Log.d(TAG, "Fetch Succeeded, isUpdated: " + isFetched);
//                        } else {
//                            ErrorLogger.log(PickUpApplication.this, TAG, "Failed to fetch RemoteConfig");
//                        }
//                        FeatureFlights.setScheduledReminders(firebaseRemoteConfig.getBoolean("scheduled_reminders"));
//                        FeatureFlights.setWakeFromDeepDoze(firebaseRemoteConfig.getBoolean("wake_from_deep_doze"));
//                        FeatureFlights.setIsRemindersDeletionEnabled(firebaseRemoteConfig.getBoolean("reminder_deletion_enabled"));
//                        FeatureFlights.setReminderDeletionUndoTimeout(firebaseRemoteConfig.getLong("reminder_deletion_undo_timeout_ms"));
//                    }
//                });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void initFabric() {
        final boolean disableOnDebug = BuildConfig.DEBUG;
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(disableOnDebug).build())
                .build();
        final Fabric fabric = new Fabric.Builder(this)
                .kits(crashlyticsKit)
                .debuggable(true) // getting more analytics info
                .build();
        Fabric.with(fabric);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createGeneralNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_GENERAL_ID,
                Constants.NOTIFICATION_CHANNEL_GENERAL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(notificationChannel);
    }
}


package com.lowenhardt.pickup;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.crashlytics.android.Crashlytics;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CallReceiver extends PhoneCallReceiver {
    private static final String TAG = CallReceiver.class.getSimpleName();

    @Override
    protected void onIncomingCallReceived(Context c, Uri number, Date start) {
        Database db = new Database(c);
        List<ContactConfig> contactConfigs = db.getAllContactConfigs(c);
        ContactConfig matchingContact = null;
        for (ContactConfig contactConfig : contactConfigs) {
            Uri contactNumber = contactConfig.getPhoneNumberUri();
            if (contactNumber.equals(number)) {
                matchingContact = contactConfig;
            }
        }

        if (matchingContact == null) {
            Crashlytics.log(Log.INFO, TAG, "No contact config matching incoming call number: "+number);
            return;
        }

        Crashlytics.log(Log.INFO, TAG, "Incoming phone number matches contact config: "+matchingContact);

        matchingContact.addCall(start);
        if (!matchingContact.shouldChangeMode()) {
            return;
        }

        if (!isDeviceMuted(c)) {
            return;
        }

        unmute(c, matchingContact);
    }

    private void unmute(Context c, ContactConfig matchingContact) {
        try {
            changeRingerMode(c);
            changeVolume(c, matchingContact);
            postUnmuteNotification(c, matchingContact);
        } catch (Exception e) {
            Crashlytics.log(Log.ERROR, TAG, "Failed to change ringer/volumes");
            Crashlytics.logException(e);
        }
    }

    private void postUnmuteNotification(Context c, ContactConfig matchingContact) {
        String notificationSummary = c.getString(R.string.unmute_notification_summary,
                matchingContact.getName(),
                matchingContact.getNumCallsInInterval(),
                matchingContact.getCallIntervalMinutes());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, Constants.NOTIFICATION_CHANNEL_GENERAL_ID)
                .setSmallIcon(R.drawable.ic_contact)
                .setContentTitle(c.getString(R.string.device_was_unmuted))
                .setContentText(notificationSummary)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Long notificationId = Calendar.getInstance().getTimeInMillis();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(c);
        notificationManager.notify(notificationId.intValue(), builder.build());
        Crashlytics.log(Log.INFO, TAG, "Posted unmute notification, id: "+notificationId.intValue()+
                " summary: "+notificationSummary);
    }

    private void changeVolume(Context c, ContactConfig matchingContact) throws NullPointerException {
        float configMaxVolume = c.getResources().getInteger(R.integer.volume_when_unmuted_max);
        float volumeWhenUnmuted = matchingContact.getVolumeWhenUnmuted();

        AudioManager am = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_RING);

        float relativeMaxVolume = maxVolume / configMaxVolume;
        double relativeVolume = volumeWhenUnmuted * relativeMaxVolume;
        int selectedVolume = (int) Math.ceil(relativeVolume);

        Crashlytics.log(Log.INFO, TAG, "Setting volume to: "+selectedVolume+", contact volume: "+volumeWhenUnmuted);
        am.setStreamVolume(AudioManager.STREAM_RING, selectedVolume,0);
    }

    private void changeRingerMode(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                Crashlytics.log(Log.INFO, TAG,"Ringer mode is silent, changing to normal");
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                Crashlytics.log(Log.INFO, TAG,"Ringer mode is vibrate, changing to normal");
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                Crashlytics.log(Log.INFO, TAG,"Ringer mode is normal, nothing to do");
                break;
        }
    }

    private boolean isDeviceMuted(Context c) {
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        // Android M has notion of 'isMuted'
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            boolean isMuted = am.isStreamMute(AudioManager.STREAM_RING);
            if (isMuted) {
                Crashlytics.log(Log.INFO, TAG,"Ring stream is muted");
                return true;
            }
        }

        int ringerMode = am.getRingerMode();
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_SILENT:
            case AudioManager.RINGER_MODE_VIBRATE:
                Crashlytics.log(Log.INFO, TAG,"Device is not muted ("+ringerMode+")");
                return true;
            case AudioManager.RINGER_MODE_NORMAL:
                int volume = am.getStreamVolume(AudioManager.STREAM_RING);
                if (volume == 0) {
                    Crashlytics.log(Log.INFO, TAG,"Ringer volume is 0, device muted");
                    return true;
                } else {
                    Crashlytics.log(Log.INFO, TAG,"Ringer volume is "+volume+", device not muted");
                    return false;
                }
        }
        return false;
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, Uri number, Date start) {
        //
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, Uri number, Date start, Date end) {
        //
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, Uri number, Date start) {
        //
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, Uri number, Date start, Date end) {
        //
    }

    @Override
    protected void onMissedCall(Context ctx, Uri number, Date start) {
        //
    }

}
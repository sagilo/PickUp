package com.lowenhardt.pickup;

@SuppressWarnings("WeakerAccess")
public class Constants {

    public static final int REQUEST_PERMISSION_READ_PHONE_STATE = 1003;
    public static final int REQUEST_PERMISSION_READ_CALL_LOG = 1004;
    public static final int REQUEST_PERMISSION_READ_CONTACTS = 1005;

    static public String NOTIFICATION_CHANNEL_GENERAL_ID = "general";
    static public String NOTIFICATION_CHANNEL_GENERAL_NAME = "General";

    static public String EXTRA_CONTACT_CONFIG_NAME = "extra_reminder_subject";
    static public String EXTRA_CONTACT_CONFIG_PHONE_NUMBER = "extra_phone_number";
    static public String EXTRA_CONTACT_CONFIG_INTERVAL_SECONDS = "extra_interval_seconds";
    static public String EXTRA_CONTACT_CONFIG_INTERVAL_NUM_CALLS_BEFORE_MODE_CHANGE = "extra_num_calls_before_mode_change";
    static public String EXTRA_CONTACT_CONFIG_VOLUME_WHEN_UNMUTED = "extra_volume_when_unmuted";
    static public String EXTRA_CONTACT_CONFIG_INTERVAL_RECENT_CALLS = "extra_recent_calls";
    static public String EXTRA_CONTACT_CONFIG_ID = "contact_config_id";

    static public final int INVALID_START_ID = -1;

    public static final int MAX_NUM_OF_RECENT_CALLS = 50;
}

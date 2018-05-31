package com.atenishev.storagestat;

public class Constants {
    public static final String JOB_TAG = "scan_files";
    public static final String JOB_DATA = "scan_data";

    // required parameters limitations
    public static final int BIGGEST_FILES_NUM = 10;
    public static final int FREQ_EXTS_NUM = 5;
    // parameters bundle keys
    public static final String PREF_BIG_FILES_NAME = "BF_N_";
    public static final String PREF_BIG_FILES_SIZE = "BF_S_";
    public static final String PREF_FREQ_EXT_NAME = "FR_N_";
    public static final String PREF_FREQ_EXT_FREQ = "FR_F_";



    public static final String EXTENDED_DATA_STATUS =
            "com.atenishev.storagestat.STATUS";
    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "com.atenishev.storagestat.BROADCAST";
    public static final String AVERAGE_FSIZE = "AVERAGEFS";


    public static final String CHANNEL_ID = "VERBOSE_NOTIFICATION";
    public static final int NOTIFICATION_ID = 1;
    // Name of Notification Channel for verbose notifications of background work
    public static final CharSequence VERBOSE_NOTIFICATION_CHANNEL_NAME = "Scan Stat Notifications";
    public static String VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows current scan status whenever work starts";
}

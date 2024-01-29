package com.simbaquartz.xapi.connect.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommunicationUtils {
    private final static String module = CommunicationUtils.class.getName();

    /**
     * Calculates call duration.
     *
     * @param startTime
     * @param endTime
     * @return
     * @throws Exception
     */
    public static String getCallDuration(String startTime, String endTime) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        Date date1 = format.parse(startTime);
        Date date2 = format.parse(endTime);
        long diff = date2.getTime() - date1.getTime();
        long hours = (long) Math.floor(diff / (60 * 60 * 1000));
        if (hours < 1.00) {
            // get min
            diff = diff % (60 * 60 * 1000);
            long minutes = (long) Math.floor(diff / (60 * 1000));
            if (minutes >= 1.00) {
                int minutesDuration = (int) (long) minutes;
                return minutesDuration + " Min(s)";
            } else {
                // get seconds
                diff = diff % (60 * 1000);
                long seconds = diff / 1000;
                int secondsDuration = (int) (long) seconds;
                return secondsDuration + " Sec(s)";
            }
        } else {
            //  get hrs
            int hoursDuration = (int) (long) hours;
            return hoursDuration + " Hour(s)";
        }
    }

}

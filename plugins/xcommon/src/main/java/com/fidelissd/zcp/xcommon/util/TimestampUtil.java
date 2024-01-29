package com.fidelissd.zcp.xcommon.util;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Date;

public class TimestampUtil {

    /**
     * Convert from ISO 8601 format string to timestamp
     *
     * @param dateStr date string in ISO 8601 format yyyy-MM-ddTHH:mm:ssZ  (example: 2022-08-01T00:00:00+00:00)
     *                Refer https://en.wikipedia.org/wiki/ISO_8601
     * @return timestamp object
     */
    public static Timestamp fromISO8601(String dateStr) {
        if (dateStr == null) return null;
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateStr);
            return Timestamp.from(offsetDateTime.toInstant());
        } catch (Exception ex) {
            Debug.logError("There was a problem parsing date :" + dateStr, TimestampUtil.class.getName());
        }
        return null;
    }

    /**
     * @param ts input timestamp
     * @return string representation of timestamp, in ISO 8601 format yyyy-MM-ddTHH:mm:ssZ  (example: 2022-08-01T00:00:00+00:00)
     * Refer https://en.wikipedia.org/wiki/ISO_8601
     */
    public static String toISO8601(Timestamp ts) {
        try {
            return UtilDateTime.toDateString(new Date(ts.getTime()), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        } catch (Exception ex) {
            Debug.logError("There was a problem formatting given timestamp to string :" + ts, TimestampUtil.class.getName());
        }
        return null;
    }

    /**
     * To calculate milliseconds between two Timestamps.
     *
     * @param from start timestamp
     * @param to   end timestamp
     * @return difference in milliseconds
     */
    public static long difference(Timestamp from, Timestamp to) {
        if (UtilValidate.isEmpty(from) || UtilValidate.isEmpty(to)) return 0L;
        return to.getTime() - from.getTime();
    }

    /**
     * Return String formatted time for given time in ms
     * @param ms time in ms
     * @return string formatted time
     */
    public static String msToString(long ms) {
        long totalSecs = ms / 1000;
        long hours = (totalSecs / 3600);
        long mins = (totalSecs / 60) % 60;
        long secs = totalSecs % 60;
        String minsString = (mins == 0) ? "0" : ((mins < 10) ? "0" + mins : "" + mins);
        String secsString = (secs == 0) ? "0" : ((secs < 10) ? "0" + secs : "" + secs);
        if (hours > 0)
            if (mins == 0) {
                return hours + " Hrs";
            } else {
                return hours + "." + minsString + " Hrs";
            }
        else if (mins > 0) return mins + " Min";
        else return ":" + secsString;
    }

}

package org.apache.ofbiz.base.util;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UtilDateTimeRuntime {

    private static final DecimalFormat DF = new DecimalFormat("0.00;-0.00");

    private static double getInterval(Date from, Date thru) {
        return thru != null ? thru.getTime() - from.getTime() : 0;
    }

    public static String formatInterval(Date from, Date thru, int count, Locale locale) {
        return formatInterval(getInterval(from, thru), count, locale);
    }

    public static String formatInterval(Date from, Date thru, Locale locale) {
        return formatInterval(from, thru, 2, locale);
    }

    public static String formatInterval(Timestamp from, Timestamp thru, int count, Locale locale) {
        return formatInterval(UtilDateTime.getInterval(from, thru), count, locale);
    }

    public static String formatInterval(Timestamp from, Timestamp thru, Locale locale) {
        return formatInterval(from, thru, 2, locale);
    }

    public static String formatInterval(long interval, int count, Locale locale) {
        return formatInterval((double) interval, count, locale);
    }

    public static String formatInterval(long interval, Locale locale) {
        return formatInterval(interval, 2, locale);
    }

    public static String formatInterval(double interval, Locale locale) {
        return formatInterval(interval, 2, locale);
    }

    public static String formatInterval(double interval, int count, Locale locale) {
        List<Double> parts = new ArrayList<>(UtilDateTime.TIMEVALS.length);
        for (String[] timeval: UtilDateTime.TIMEVALS) {
            int value = Integer.parseInt(timeval[0]);
            double remainder = interval % value;
            interval = interval / value;
            parts.add(remainder);
        }

        Map<String, Object> uiDateTimeMap = UtilPropertiesRuntime.getResourceBundleMap("DateTimeLabels", locale);

        StringBuilder sb = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0 && count > 0; i--) {
            Double doub = parts.get(i);
            double d = doub;
            if (d < 1) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            count--;
            sb.append(count == 0 ? DF.format(d) : Integer.toString(doub.intValue()));
            sb.append(' ');
            Object label;
            if (doub.intValue() == 1) {
                label = uiDateTimeMap.get(UtilDateTime.TIMEVALS[i][1] + ".singular");
            } else {
                label = uiDateTimeMap.get(UtilDateTime.TIMEVALS[i][1] + ".plural");
            }
            sb.append(label);
        }
        return sb.toString();
    }
}

/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written by Forrest Rae <forrest.rae@fidelissd.com>, December, 2016                       *
 *  *****************************************************************************************
 */

package com.simbaquartz.xsolr.util;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by mande on 12/15/2016.
 */
public class SolrDateTimeUtils {
    public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String toSolrFormattedDateString(Timestamp toFormat){

        if(UtilValidate.isEmpty(toFormat)){
            return "";
        }

        SimpleDateFormat solrDateFormat = new SimpleDateFormat(SOLR_DATE_FORMAT);
        solrDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return solrDateFormat.format(toFormat);
    }

    /**
     * Prepares date range filter for given timestamps, uses * for null values.
     * @param fromDate date range from, returns * in case of null value
     * @param thruDate date range thru, returns * in case of null value
     * @return
     */
    public static String prepareDateRangeFilter(Timestamp fromDate, Timestamp thruDate){

        String fromDateFilterQuery = "*";
        String thruDateFilterQuery = "*";

        if(UtilValidate.isNotEmpty(fromDate)){
            fromDateFilterQuery = SolrDateTimeUtils.toSolrFormattedDateString(fromDate);
        }
        if(UtilValidate.isNotEmpty(thruDate)){
            thruDateFilterQuery =SolrDateTimeUtils.toSolrFormattedDateString(thruDate);
        }

        return "[ " + fromDateFilterQuery + " TO " + thruDateFilterQuery + " ]";
    }


    /** Given a Date, gets its Start and End time in Solr format to be used in Solr Query
     * Output: fromTime and thruTime
    **/
    public static Map<String, String> getStartAndEndTimeInSolrFormat(Date date) {
        Instant instant = date.toInstant();
        ZoneId defaultZoneId = ZoneId.systemDefault();
        LocalDate localDate = instant.atZone(defaultZoneId).toLocalDate();

        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime tomorrow = startOfDay.plusDays(1);
        tomorrow = tomorrow.minusSeconds(1); // gets 23:59:59

        Timestamp fromTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp toTimestamp = Timestamp.valueOf(tomorrow);
        String fromTime = SolrDateTimeUtils.toSolrFormattedDateString(fromTimestamp);
        String thruTime = SolrDateTimeUtils.toSolrFormattedDateString(toTimestamp);
        Map<String,String> output = FastMap.newInstance();
        output.put("fromTime", fromTime);
        output.put("thruTime", thruTime);
        return output;
    }

    public static Map<String, String> getStartAndEndTimeInSolrFormatForProfileTz(Date date,Map<String,Object> context) {
        Instant instant = date.toInstant();
        TimeZone timeZone=null;
        TimeZone oldTimeZone=null;

        if (timeZone == null && context.containsKey("userLogin")) {
            Map<String, String> userLogin = UtilGenerics.cast(context.get("userLogin"));
            timeZone = UtilDateTime.toTimeZone(userLogin.get("lastTimeZone"));
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        oldTimeZone=TimeZone.getDefault();
        TimeZone.setDefault(timeZone);
        ZoneId defaultZoneId = ZoneId.systemDefault();
      //  ZoneId defaultZoneId = TimeZone.getTimeZone("IST").toZoneId();
        LocalDate localDate = instant.atZone(defaultZoneId).toLocalDate();

        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime tomorrow = startOfDay.plusDays(1);
        tomorrow = tomorrow.minusSeconds(1); // gets 23:59:59


        Timestamp fromTimestamp = Timestamp.valueOf(startOfDay);
        Timestamp toTimestamp = Timestamp.valueOf(tomorrow);
        String fromTime =fromTimestamp.toInstant().toString();
        String thruTime =toTimestamp.toInstant().toString();
        Map<String,String> output = FastMap.newInstance();
        output.put("fromTime", fromTime);
        output.put("thruTime", thruTime);
        TimeZone.setDefault(oldTimeZone);
        return output;
    }
}

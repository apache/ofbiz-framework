/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.common.uom;

import org.ofbiz.entity.GenericDelegator;

import com.ibm.icu.util.Calendar;
import java.sql.Timestamp;

/**
 * UomWorker
 */
public class UomWorker {

    public static final String module = UomWorker.class.getName();

    public static int[] uomTimeToCalTime(String uomId) {
        if ("TF_ms".equals(uomId)) {
            return new int[] { Calendar.MILLISECOND, 1 };
        } else if ("TF_s".equals(uomId)) {
            return new int[] { Calendar.SECOND, 1 };
        } else if ("TF_min".equals(uomId)) {
            return new int[] { Calendar.MINUTE, 1 };
        } else if ("TF_hr".equals(uomId)) {
            return new int[] { Calendar.HOUR, 1 };
        } else if ("TF_day".equals(uomId)) {
            return new int[] { Calendar.DAY_OF_YEAR, 1 };
        } else if ("TF_wk".equals(uomId)) {
            return new int[] { Calendar.WEEK_OF_YEAR, 1 };
        } else if ("TF_mon".equals(uomId)) {
            return new int[] { Calendar.MONTH, 1 };
        } else if ("TF_yr".equals(uomId)) {
            return new int[] { Calendar.YEAR, 1 };
        } else if ("TF_decade".equals(uomId)) {
            return new int[] { Calendar.YEAR, 10 };
        } else if ("TF_score".equals(uomId)) {
            return new int[] { Calendar.YEAR, 20 };
        } else if ("TF_century".equals(uomId)) {
            return new int[] { Calendar.YEAR, 100 };
        } else if ("TF_millenium".equals(uomId)) {
            return new int[] { Calendar.YEAR, 1000 };
        }

        return null;
    }

    public static Calendar addUomTime(Calendar cal, Timestamp startTime, String uomId, int value) {
        if (cal == null) {
            cal = Calendar.getInstance();
        }
        if (startTime != null) {
            cal.setTimeInMillis(startTime.getTime());
        }
        int[] conv = uomTimeToCalTime(uomId);

        // conversion multiplier * value by type
        cal.add(conv[0], (value * conv[1]));
        return cal;
    }

    public static Calendar addUomTime(Calendar cal, String uomId, int value) {
        return addUomTime(cal, null, uomId, value);
    }

    public static Calendar addUomTime(Timestamp startTime, String uomId, int value) {
        return addUomTime(null, startTime, uomId, value);
    }
}

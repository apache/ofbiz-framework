/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.common.geo;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.DispatchContext;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GeoServices {

    private static final double RADIUS_OF_EARTH = 6371; // Radius of the earth in km
    private static final double MILES_PER_KILOMETER = 0.621371192; // TODO: Think upon using convertUom service

    public static Map<String, Object> getDistanceBetweenGeoPoints(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> serviceResponse = new HashMap<>();

        double fromLatitude = UtilMisc.toDouble(context.get("fromLatitude"));
        double fromLongitude = UtilMisc.toDouble(context.get("fromLongitude"));
        double toLatitude = UtilMisc.toDouble(context.get("toLatitude"));
        double toLongitude = UtilMisc.toDouble(context.get("toLongitude"));

        double dLatitude = Math.toRadians(toLatitude - fromLatitude);
        double dLongitude = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.sin(dLatitude / 2) * Math.sin(dLatitude / 2)
                + Math.cos(Math.toRadians(fromLatitude)) * Math.cos(Math.toRadians(toLatitude))
                * Math.sin(dLongitude / 2) * Math.sin(dLongitude / 2);
        double c = 2 * Math.atan(Math.sqrt(a) / Math.sqrt(1 - a));
        double distance = c * RADIUS_OF_EARTH; // Distance in Kilometers
        Locale locale = (Locale) context.get("locale");
        if ("IMPERIAL".equals(GeoWorker.getMeasurementSystem(locale))) {
            distance = distance * MILES_PER_KILOMETER; // Distance in Miles
        }
        serviceResponse.put("distance", distance);
        return serviceResponse;
    }
}

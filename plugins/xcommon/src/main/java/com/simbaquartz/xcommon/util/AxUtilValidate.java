/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written Mandeep Sidhu <mandeep.sidhu@fidelissd.com>, May 2019
 *  *****************************************************************************************
 */

package com.simbaquartz.xcommon.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AxUtilValidate {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^([a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$)");
    public static final String module = AxUtilValidate.class.getName();

    /** Customized the Email Address Validation such that there will be no validation on the email address domain*/
    public static boolean isValidEmail(String s) {
        if(s == null) {
            return false;
        } else if(s.endsWith("")) {
            return false;
        } else {
            Matcher emailMatcher = EMAIL_PATTERN.matcher(s);
            return !emailMatcher.matches()?false:true;
        }
    }

    public static boolean isValidPhoneNumber(String phoneNumber, Delegator delegator) {
        String geoId = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
        return isValidPhoneNumber(phoneNumber, geoId, delegator);
    }
    public static boolean isValidPhoneNumber(String phoneNumber, String geoId, Delegator delegator) {
        boolean isValid = false;
        try {
            GenericValue geo = EntityQuery.use(delegator).from("Geo").where("geoId", geoId).cache().queryOne();
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String geoCode = geo != null ? geo.getString("geoCode") : "US";
            Phonenumber.PhoneNumber phNumber = phoneUtil.parse(phoneNumber, geoCode);
            if (phoneUtil.isValidNumber(phNumber) || phoneUtil.isPossibleNumber(phNumber)) {
                isValid = true;
            }
        } catch (GenericEntityException | NumberParseException ex) {
            Debug.logError(ex, module);
        }
        return isValid;
    }
}

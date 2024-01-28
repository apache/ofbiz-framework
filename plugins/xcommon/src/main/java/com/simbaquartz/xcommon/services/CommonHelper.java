/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written by Sunaina Kapoor <sunaina.kapoor@simbaquartz.com>, July, 2019                 *
 *  *****************************************************************************************
 */

package com.simbaquartz.xcommon.services;

import java.util.Map;
import java.util.TreeMap;

import com.simbaquartz.xcommon.models.Language;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.base.util.Debug;

public class CommonHelper {
    private static final String module = CommonHelper.class.getName();

    public static String getEnumTypeDesc(Delegator delegator, String enumId) throws GenericServiceException {
        GenericValue enumDetails;
        String enumDesc = "";
        String enumerationId = enumId;

        try {
            enumDetails = EntityQuery.use(delegator).from("Enumeration").where("enumId", enumId).queryOne();
            if (UtilValidate.isNotEmpty(enumDetails)) {
                enumDesc = (String) enumDetails.get("description");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return enumDesc;
    }

    public static String getContentName(Delegator delegator, String contentId) throws GenericEntityException {
        String contentName = "";
        GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
        if(UtilValidate.isNotEmpty(content)) {
            contentName = content.getString("contentName");
        }
        return contentName;
    }

    public static String getStatusItemDesc(Delegator delegator, String statusId) throws GenericServiceException {
        GenericValue statusGv;
        String desc = "";

        try {
            statusGv = EntityQuery.use(delegator).from("StatusItem").where("statusId", statusId).queryOne();
            if (UtilValidate.isNotEmpty(statusGv)) {
                desc = (String) statusGv.get("description");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return desc;
    }

    public static String getRoleTypeDesc(Delegator delegator, String roleTypeId) throws GenericServiceException {
        GenericValue roleType;
        String desc = "";

        try {
            roleType = EntityQuery.use(delegator).from("RoleType").where("roleTypeId", roleTypeId).queryOne();
            if (UtilValidate.isNotEmpty(roleType)) {
                desc = (String) roleType.get("description");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return desc;
    }

    /**
     * Converts a map to query String
     *
     * @param queryParamMap key value pair
     */
    public static String queryStringFromMap(Map<String, String> queryParamMap) {
        StringBuilder querySt = new StringBuilder();
        if (UtilValidate.isNotEmpty(queryParamMap)) {
            int paramCount = 0;
            querySt.append("?");
            for (Map.Entry<String, String> entry : queryParamMap.entrySet()) {

                if (paramCount++ > 0) {
                    querySt.append("&");
                }
                querySt.append(entry.getKey()).append("=").append(entry.getValue());

            }

        }
        return querySt.toString();
    }

    /**
     * sorts a query String
     *
     * @param queryStr key value pair
     */
    public static String getSortedQueryString(String queryStr) {

        Map sortedQueryMap = getSortedQueryMap(queryStr);

        //Getting query string from the sorted map
        queryStr = CommonHelper.queryStringFromMap(sortedQueryMap);

        return queryStr;
    }

    /**
     * sorts a query String
     *
     * @param queryStr key value pair
     */
    public static Map getSortedQueryMap(String queryStr) {

        //removing ? from the query string before storing
        if (queryStr.contains("?")) {
            queryStr = queryStr.split("\\?")[1];
        }
        // extract into Json and order them alphabetically.
        Map queryMap = UtilHttp.getQueryStringOnlyParameterMap(queryStr);

        return new TreeMap(queryMap);
    }

    public static boolean isValidLanguageProficiency(Language language) {
        boolean isValidPreferenceLevel = true;
        if(UtilValidate.isNotEmpty(language.getFluency())
                && !((language.getFluency() >= 1) && (language.getFluency() <= 10))) {
            return false;
        }
        if(UtilValidate.isNotEmpty(language.getRead())
                && !((language.getRead() >= 1) && (language.getRead() <= 10))) {
            return false;
        }
        if(UtilValidate.isNotEmpty(language.getWrite())
                && !((language.getWrite() >= 1) && (language.getWrite() <= 10))) {
            return false;
        }
        if(UtilValidate.isNotEmpty(language.getSpoken())
                && !((language.getSpoken() >= 1) && (language.getSpoken() <= 10))) {
            return false;
        }
        return isValidPreferenceLevel;
    }

    public static Boolean isExistingLanguageId(Delegator delegator, String languageId) {
        GenericValue languageRecord = null;
        try {
            languageRecord = EntityQuery.use(delegator).from("StandardLanguage").where("standardLanguageId", languageId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(languageRecord)) {
            return true;
        }
        return false;
    }

    public static GenericValue getLanguage(Delegator delegator, String languageId) {
        GenericValue languageRecord = null;
        try {
            languageRecord = EntityQuery.use(delegator).from("StandardLanguage").where("standardLanguageId", languageId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return languageRecord;
    }
}
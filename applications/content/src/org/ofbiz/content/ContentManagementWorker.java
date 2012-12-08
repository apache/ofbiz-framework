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
package org.ofbiz.content;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.LifoSet;
import org.ofbiz.content.content.ContentServicesComplex;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entityext.permission.EntityPermissionChecker;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.security.Security;


/**
 * ContentManagementWorker Class
 */
public class ContentManagementWorker {

    public static final String module = ContentManagementWorker.class.getName();
    public static Map<String, GenericValue> cachedWebSitePublishPoints = FastMap.newInstance();
    public static Map<String, Map<String, Object>> cachedStaticValues = FastMap.newInstance();

    public static void mruAdd(HttpServletRequest request, GenericEntity pk, String suffix) {
        HttpSession session = request.getSession();
        mruAdd(session, pk);
    }

    public static void mruAdd(HttpServletRequest request, GenericEntity pk) {
        HttpSession session = request.getSession();
        mruAdd(session, pk);
    }

    public static void mruAdd(HttpSession session, GenericEntity pk) {
        if (pk == null) {
            return;
        }

        Map<String, LifoSet<Object>> lookupCaches = UtilGenerics.checkMap(session.getAttribute("lookupCaches"));
        if (lookupCaches == null) {
            lookupCaches = FastMap.newInstance();
            session.setAttribute("lookupCaches", lookupCaches);
        }
        String entityName = pk.getEntityName();
        mruAddByEntityName(entityName, pk, lookupCaches);
    }

   /**
    * Makes an entry in the "most recently used" cache. It picks the cache
    * by the entity name and builds a signature from the primary key values.
    *
    * @param entityName
    * @param lookupCaches
    * @param pk either a GenericValue or GenericPK - populated
    */
    public static void mruAddByEntityName(String entityName, GenericEntity pk, Map<String, LifoSet<Object>> lookupCaches) {
        String cacheEntityName = entityName;
        LifoSet<Object> lkupCache = lookupCaches.get(cacheEntityName);
        if (lkupCache == null) {
            lkupCache = new LifoSet<Object>();
            lookupCaches.put(cacheEntityName, lkupCache);
        }
        lkupCache.add(pk.getPrimaryKey());
        if (Debug.infoOn()) Debug.logInfo("in mruAddByEntityName, entityName:" + entityName + " lifoSet.size()" + lkupCache.size(), module);
    }

    public static Iterator<Object> mostRecentlyViewedIterator(String entityName, Map<String, LifoSet<Object>> lookupCaches) {
        String cacheEntityName = entityName;
        LifoSet<Object> lkupCache = lookupCaches.get(cacheEntityName);
        if (lkupCache == null) {
            lkupCache = new LifoSet<Object>();
            lookupCaches.put(cacheEntityName, lkupCache);
        }

        Iterator<Object> mrvIterator = lkupCache.iterator();
        return mrvIterator;
    }


   /**
    * Builds a string signature from a GenericValue or GenericPK.
    *
    * @param pk either a populated GenericValue or GenericPK.
    * @param suffix a string that can be used to distinguish the signature (probably not used).
    */
    public static String buildPKSig(GenericEntity pk, String suffix) {
        StringBuilder sig = new StringBuilder("");
        Collection<String> keyColl = pk.getPrimaryKey().getAllKeys();
        List<String> keyList = UtilMisc.makeListWritable(keyColl);
        Collections.sort(keyList);
        for(String ky : keyList) {
            String val = (String)pk.get(ky);
            if (UtilValidate.isNotEmpty(val)) {
                if (sig.length() > 0) sig.append("_");
                sig.append(val);
            }
        }
        if (UtilValidate.isNotEmpty(suffix)) {
            if (sig.length() > 0) sig.append("_");
            sig.append(suffix);
        }
        return sig.toString();
    }

    public static void setCurrentEntityMap(HttpServletRequest request, GenericEntity ent) {
        String entityName = ent.getEntityName();
        setCurrentEntityMap(request, entityName, ent);
    }

    public static void setCurrentEntityMap(HttpServletRequest request, String entityName, GenericEntity ent) {
        HttpSession session = request.getSession();
        Map<String, GenericEntity> currentEntityMap = UtilGenerics.checkMap(session.getAttribute("currentEntityMap"));
        if (currentEntityMap == null) {
            currentEntityMap = FastMap.newInstance();
            session.setAttribute("currentEntityMap", currentEntityMap);
        }
        currentEntityMap.put(entityName, ent);
    }

    public static String getFromSomewhere(String name, Map<String, Object> paramMap, HttpServletRequest request, Map<String, Object> context) {
        String ret = null;
        if (paramMap != null) {
            ret = (String)paramMap.get(name);
        }
        if (UtilValidate.isEmpty(ret)) {
            Object obj = request.getAttribute(name);
            if (obj != null) {
                ret = obj.toString();
            } else {
                obj = context.get(name);
                if (obj != null) {
                    ret = obj.toString();
                }
            }
        }
        return ret;
    }

    //public static String getFromSomewhere(String name, LinkedHashMap paramMap, HttpServletRequest request, org.jpublish.JPublishContext context) {
    /* This method should no longer be in use; the JPublish library was removed by default from OFBiz
    public static String getFromSomewhere(String name, Map paramMap, HttpServletRequest request, org.jpublish.JPublishContext context) {

        String ret = null;
        if (paramMap != null)
            ret = (String)paramMap.get(name);

        if (UtilValidate.isEmpty(ret)) {
            Object obj = request.getAttribute(name);
            if (obj != null) {
                ret = obj.toString();
            } else {
                obj = context.get(name);
                if (obj != null) {
                    ret = obj.toString();
                }
            }
        }
        return ret;
    } */

    public static void getCurrentValue(HttpServletRequest request, Delegator delegator) {
        HttpSession session = request.getSession();
        Map<String, GenericPK> currentEntityMap = UtilGenerics.checkMap(session.getAttribute("currentEntityMap"));
        if (currentEntityMap == null) {
            currentEntityMap     = FastMap.newInstance();
            session.setAttribute("currentEntityMap", currentEntityMap);
        }
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        String entityName = (String)paramMap.get("entityName");
        if (UtilValidate.isEmpty(entityName)) {
            entityName = (String)request.getAttribute("entityName");
        }
        GenericPK cachedPK = null;
        if (UtilValidate.isNotEmpty(entityName)) {
            cachedPK = currentEntityMap.get(entityName);
        } 
        getCurrentValueWithCachedPK(request, delegator, cachedPK, entityName);
        GenericPK currentPK = (GenericPK)request.getAttribute("currentPK");
        currentEntityMap.put(entityName, currentPK);
    }

    public static void getCurrentValueWithCachedPK(HttpServletRequest request, Delegator delegator, GenericPK cachedPK, String entityName) {
        Map<String, Object> paramMap = UtilHttp.getParameterMap(request);
        // Build the primary key that may have been passed in as key values
        GenericValue v = delegator.makeValue(entityName);
        GenericPK passedPK = v.getPrimaryKey();
        Collection<String> keyColl = passedPK.getAllKeys();
        for(String attrName : keyColl) {
            String attrVal = (String)request.getAttribute(attrName);
            if (UtilValidate.isEmpty(attrVal)) {
                attrVal = (String)paramMap.get(attrName);
            }
            if (UtilValidate.isNotEmpty(attrVal)) {
                passedPK.put(attrName,attrVal);
            }
        }

        // If a full passed primary key exists, it takes precedence over a cached key
        // I cannot determine if the key testing utils of GenericEntity take into account
        // whether or not a field is populated.
        boolean useCached = false;
        boolean usePassed = true;
        if (cachedPK != null) {
            useCached = true;
            keyColl = cachedPK.getPrimaryKey().getAllKeys();
            for(String ky : keyColl) {
                String sCached = null;
                String sPassed = null;
                Object oPassed = null;
                Object oCached = null;
                oPassed = passedPK.get(ky);
                if (oPassed != null) {
                    sPassed = oPassed.toString();
                    if (UtilValidate.isEmpty(sPassed)) {
                        // If any part of passed key is not available, it can't be used
                        usePassed = false;
                    } else {
                        oCached = cachedPK.get(ky);
                        if (oCached != null) {
                            sCached = oCached.toString();
                            if (UtilValidate.isEmpty(sCached)) {
                                useCached = false;
                            } else {
                            }
                        } else {
                            useCached = false;
                        }
                    }
                } else {
                    //useCached = false;
                    usePassed = false;
                }
            }
        }

        GenericPK currentPK = null;
        if (usePassed && useCached) {
            currentPK = passedPK;
        } else if (usePassed && !useCached) {
            currentPK = passedPK;
        } else if (!usePassed && useCached) {
            currentPK = cachedPK;
        }

        if (currentPK != null) {
            request.setAttribute("currentPK", currentPK);
            GenericValue currentValue = null;
            try {
                currentValue = delegator.findOne(currentPK.getEntityName(), currentPK, false);
            } catch (GenericEntityException e) {
            }
            request.setAttribute("currentValue", currentValue);
        }

    }

    public static List<String []> getPermittedPublishPoints(Delegator delegator, List<GenericValue> allPublishPoints, GenericValue userLogin, Security security, String permittedAction, String permittedOperations, String passedRoles) throws GeneralException {
        List<String []> permittedPublishPointList = FastList.newInstance();

        // Check that user has permission to admin sites
        for(GenericValue webSitePP : allPublishPoints) {
            String contentId = (String)webSitePP.get("contentId");
            String templateTitle = (String)webSitePP.get("templateTitle");
            GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", contentId));
            String statusId = null;
            String entityAction = permittedAction;
            if (entityAction == null) {
                entityAction = "_ADMIN";
            }
            List<String> passedPurposes = UtilMisc.toList("ARTICLE");
            List<String> roles = StringUtil.split(passedRoles, "|");
            List<String> targetOperationList = FastList.newInstance();
            if (UtilValidate.isEmpty(permittedOperations)) {
                 targetOperationList.add("CONTENT" + entityAction);
            } else {
                 targetOperationList = StringUtil.split(permittedOperations, "|");
            }
            Map<String, Object> results = null;
            //if (Debug.infoOn()) Debug.logInfo("in getPermittedPublishPoints, content:" + content, module);
            results = EntityPermissionChecker.checkPermission(content, statusId, userLogin, passedPurposes, targetOperationList, roles, delegator, security, entityAction);
            String permissionStatus = (String)results.get("permissionStatus");
            if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
                String [] arr = {contentId,templateTitle};
                permittedPublishPointList.add(arr);
            }
        }
        return permittedPublishPointList;
    }

    /**
     Returns a list of WebSitePublishPoint entities that are children of parentPubPt
     The name should be "getAllTopLevelPublishPoints" or "getAllChildPublishPoints"

     @param parentPubPt The parent publish point.
     */
    public static List<GenericValue> getAllPublishPoints(Delegator delegator, String parentPubPt) throws GeneralException {
        List<GenericValue> relatedPubPts = null;
        try {
            relatedPubPts = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentIdTo", parentPubPt, "contentAssocTypeId", "SUBSITE"), null, true);
        } catch (GenericEntityException e) {
            throw new GeneralException(e.getMessage());
        }
        List<GenericValue> allPublishPoints = FastList.newInstance();
        GenericValue webSitePublishPoint = null;
        for(GenericValue contentAssoc : relatedPubPts) {
           String pub = (String)contentAssoc.get("contentId");
           //webSitePublishPoint = delegator.findOne("WebSitePublishPoint", UtilMisc.toMap("contentId", pub), true);
           webSitePublishPoint = getWebSitePublishPoint(delegator, pub, false);
           allPublishPoints.add(webSitePublishPoint);
        }
        return allPublishPoints;
    }

    public static Map<String, GenericValue> getPublishPointMap(Delegator delegator, String pubPtId) throws GeneralException {
        List<GenericValue> publishPointList = getAllPublishPoints(delegator, pubPtId);
        Map<String, GenericValue> publishPointMap = FastMap.newInstance();
        for(GenericValue webSitePublishPoint : publishPointList) {
           String pub = (String)webSitePublishPoint.get("contentId");
           publishPointMap.put(pub, webSitePublishPoint);
        }
        return publishPointMap;
    }


    public static void getAllPublishPointMap(Delegator delegator, String pubPtId, Map<String, GenericValue> publishPointMap) throws GeneralException {
        List<GenericValue> publishPointList = getAllPublishPoints(delegator, pubPtId);
        for(GenericValue webSitePublishPoint : publishPointList) {
           String pub = (String)webSitePublishPoint.get("contentId");
           publishPointMap.put(pub, webSitePublishPoint);
           getAllPublishPointMap(delegator, pub, publishPointMap);
        }
    }

    public static Map<String, GenericValue> getPublishPointMap(Delegator delegator, List<GenericValue> publishPointList) {
        Map<String, GenericValue> publishPointMap = FastMap.newInstance();
        for(GenericValue webSitePublishPoint : publishPointList) {
           String pub = (String)webSitePublishPoint.get("contentId");
           publishPointMap.put(pub, webSitePublishPoint);
        }
        return publishPointMap;
    }

    public static List<Map<String, Object>> getStaticValues(Delegator delegator,  String parentPlaceholderId, List<String []> permittedPublishPointList) throws GeneralException {
        List<GenericValue> assocValueList = null;
        try {
            assocValueList = delegator.findByAnd("Content", UtilMisc.toMap("contentTypeId", parentPlaceholderId), null, true);
        } catch (GenericEntityException e) {
            throw new GeneralException(e.getMessage());
        }

        List<Map<String, Object>> staticValueList = FastList.newInstance();
        int counter = 0;
        for(GenericValue content : assocValueList) {
            String contentId = (String)content.get("contentId");
            String contentName = (String)content.get("contentName");
            String description = (String)content.get("description");
            Map<String, Object> map = FastMap.newInstance();
            map.put("contentId", contentId);
            map.put("contentName", contentName);
            map.put("description", description);
            for(String [] publishPointArray : permittedPublishPointList) {
                String publishPointId = publishPointArray[0];
                List<GenericValue> contentAssocList = content.getRelated("ToContentAssoc", UtilMisc.toMap("contentId", publishPointId), null, false);
                List<GenericValue> filteredList = EntityUtil.filterByDate(contentAssocList);
                if (filteredList.size() > 0) {
                    map.put(publishPointId, "Y");
                    GenericValue assoc = filteredList.get(0);
                    Timestamp fromDate = (Timestamp)assoc.get("fromDate");
                    map.put(publishPointId + "FromDate", fromDate);
                } else {
                    map.put(publishPointId, "N");
                }
            }
            staticValueList.add(map);
            counter++;
        }
        return staticValueList;
    }

    public static GenericValue getWebSitePublishPoint(Delegator delegator, String contentId) throws GenericEntityException {
           return getWebSitePublishPoint(delegator, contentId, false);
    }

    public static GenericValue getWebSitePublishPoint(Delegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException {
        GenericValue webSitePublishPoint = null;
        if (!ignoreCache)
            webSitePublishPoint = cachedWebSitePublishPoints.get(contentId);

        if (webSitePublishPoint == null) {
            webSitePublishPoint = delegator.findOne("WebSitePublishPoint", UtilMisc.toMap("contentId", contentId), false);
            // If no webSitePublishPoint exists, still try to look for parent by making a dummy value
            if (webSitePublishPoint == null) {
                webSitePublishPoint = delegator.makeValue("WebSitePublishPoint", UtilMisc.toMap("contentId", contentId));
            }
            //if (Debug.infoOn()) Debug.logInfo("in getWebSitePublishPoint, contentId:" + contentId, module);
            webSitePublishPoint = overrideWebSitePublishPoint(delegator, webSitePublishPoint);
            cachedWebSitePublishPoints.put(contentId, webSitePublishPoint);
        }
        return webSitePublishPoint;
    }

    public static GenericValue overrideWebSitePublishPoint(Delegator delegator, GenericValue passedValue) throws GenericEntityException {
        String contentId = passedValue.getString("contentId");
        GenericValue webSitePublishPoint = passedValue;
        String contentIdTo = getParentWebSitePublishPointId(delegator, contentId);
            //if (Debug.infoOn()) Debug.logInfo("in overrideWebSitePublishPoint, contentIdTo:" + contentIdTo, module);
        if (contentIdTo != null) {
            //webSitePublishPoint = getWebSitePublishPoint(delegator, contentIdTo, false);
            webSitePublishPoint = delegator.findOne("WebSitePublishPoint", UtilMisc.toMap("contentId", contentIdTo), true);
            if (webSitePublishPoint != null) {
                webSitePublishPoint = GenericValue.create(webSitePublishPoint);
                webSitePublishPoint = overrideWebSitePublishPoint(delegator, webSitePublishPoint);
                webSitePublishPoint.setNonPKFields(passedValue, false);
                webSitePublishPoint.setPKFields(passedValue, false);
                passedValue.setNonPKFields(webSitePublishPoint);
            }
        }
        return webSitePublishPoint;
    }

    public static GenericValue getParentWebSitePublishPointValue(Delegator delegator, String  contentId) throws GenericEntityException {

        String contentIdTo = getParentWebSitePublishPointId(delegator, contentId);
        GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentIdTo), true);
        return content;
    }

    public static String getParentWebSitePublishPointId(Delegator delegator, String  contentId) throws GenericEntityException {
        String contentIdTo = null;
        List<GenericValue> contentAssocList = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", contentId, "contentAssocTypeId", "SUBSITE"), null, true);
        List<GenericValue> filteredContentAssocList = EntityUtil.filterByDate(contentAssocList);
        if (filteredContentAssocList.size() > 0) {
            GenericValue contentAssoc = filteredContentAssocList.get(0);
            if (contentAssoc != null)
                contentIdTo = contentAssoc.getString("contentIdTo");
        }
        return contentIdTo;
    }

    public static GenericValue getStaticValue(Delegator delegator, String parentPlaceholderId, String webSitePublishPointId, boolean ignoreCache) throws GenericEntityException {
        GenericValue webSitePublishPoint = null;
        // GenericValue staticValue = null;
        if (!ignoreCache) {
            Map<String, Object> subStaticValueMap = cachedStaticValues.get(parentPlaceholderId);
            if (subStaticValueMap == null) {
                subStaticValueMap = FastMap.newInstance();
                cachedStaticValues.put(parentPlaceholderId, subStaticValueMap);
            }
            //Map staticValueMap = (GenericValue)cachedStaticValues.get(web);
        }

/*
        if (webSitePublishPoint == null) {
            webSitePublishPoint = delegator.findOne("WebSitePublishPoint", UtilMisc.toMap("contentId", contentId), false);
            // If no webSitePublishPoint exists, still try to look for parent by making a dummy value
            if (webSitePublishPoint == null) {
                webSitePublishPoint = delegator.makeValue("WebSitePublishPoint", UtilMisc.toMap("contentId", contentId));
            }
            webSitePublishPoint = overrideStaticValues(delegator, webSitePublishPoint);
            cachedWebSitePublishPoints.put(contentId, webSitePublishPoint);
        }
*/
        return webSitePublishPoint;
    }

    public static List<Object []> getPublishedLinks(Delegator delegator,  String targContentId, String rootPubId, GenericValue userLogin, Security security, String permittedAction, String permittedOperations , String passedRoles) throws GeneralException {
        // Set up one map with all the top-level publish points (to which only one sub point can be attached to)
        // and another map (publishPointMapAll) that points to one of the top-level points.
        List<GenericValue> allPublishPointList = getAllPublishPoints(delegator, rootPubId);
        //if (Debug.infoOn()) Debug.logInfo("in getPublishLinks, allPublishPointList:" + allPublishPointList, module);
        List<String []> publishPointList = getPermittedPublishPoints(delegator, allPublishPointList, userLogin, security , permittedAction, permittedOperations, passedRoles);
        Map<String, Object> publishPointMap = FastMap.newInstance();
        Map<String, Object> publishPointMapAll = FastMap.newInstance();
        for(String [] arr : publishPointList) {
            //GenericValue webSitePublishPoint = (GenericValue)it.next();
            //String contentId = (String)webSitePublishPoint.get("contentId");
            //String description = (String)webSitePublishPoint.get("description");
            String contentId = arr[0];
            String description = arr[1];
            List<Object []> subPointList = FastList.newInstance();
            Object nullObj = null;
            Object [] subArr = {contentId, subPointList, description, nullObj};
            publishPointMap.put(contentId, subArr);
            publishPointMapAll.put(contentId, contentId);
            List<GenericValue> subPublishPointList = getAllPublishPoints(delegator, contentId);
            for(GenericValue webSitePublishPoint2 : subPublishPointList) {
                //String [] arr2 = (String [])it2.next();
                //String contentId2 = (String)arr2[0];
                //String description2 = (String)arr2[1];
                String contentId2 = (String)webSitePublishPoint2.get("contentId");
                String description2 = (String)webSitePublishPoint2.get("templateTitle");
                publishPointMapAll.put(contentId2, contentId);
                Timestamp obj = null;
                Object [] subArr2 = {contentId2, description2, obj};
                subPointList.add(subArr2);
            }
        }
/* */
        List<GenericValue> assocValueList = null;
        try {
            List<GenericValue> rawAssocValueList = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentId", targContentId, "contentAssocTypeId", "PUBLISH_LINK"), null, true);
            assocValueList = EntityUtil.filterByDate(rawAssocValueList);
        } catch (GenericEntityException e) {
            throw new GeneralException(e.getMessage());
        }
        for(GenericValue contentAssoc : assocValueList) {
            String contentIdTo = contentAssoc.getString("contentIdTo");
            String topContentId = (String)publishPointMapAll.get(contentIdTo);
            Object [] subArr = (Object [])publishPointMap.get(topContentId);
                //if (Debug.infoOn()) Debug.logInfo("in getPublishLinks, subArr:" + Arrays.asList(subArr) , module);
            if (contentIdTo.equals(topContentId)) {
                subArr[3] =  contentAssoc.get("fromDate");
            } else {
                if (subArr != null) {
                    List<Object []> subPointList = UtilGenerics.checkList(subArr[1]);
                    Iterator<Object []> it5 = subPointList.iterator();
                    Object [] subArr2 = null;
                    while (it5.hasNext()) {
                        subArr2 = it5.next();
                        String contentId5 = (String)subArr2[0];
                        if (contentId5.equals(contentIdTo))
                            break;
                    }
                    subArr2[2] =  contentAssoc.get("fromDate");
                }
            }
        }

        List<Object []> publishedLinkList = FastList.newInstance();
        for(String contentId : publishPointMap.keySet()) {
            Object [] subPointArr = (Object [])publishPointMap.get(contentId);
            publishedLinkList.add(subPointArr);
        }
        return publishedLinkList;
    }

    public static GenericValue getAuthorContent(Delegator delegator, String contentId) {
        GenericValue authorContent = null;
        try {
            List<String> assocTypes = UtilMisc.toList("AUTHOR");
            List<String> contentTypes = null;
            // String fromDate = null;
            // String thruDate = null;
            Map<String, Object> results =  ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, null, "To", null, null, assocTypes, contentTypes, Boolean.TRUE, null);
            List<GenericValue> valueList = UtilGenerics.checkList(results.get("entityList"));
            if (valueList.size() > 0) {
                GenericValue value = valueList.get(0);
                authorContent = delegator.makeValue("Content");
                authorContent.setPKFields(value);
                authorContent.setNonPKFields(value);
            //if (Debug.infoOn()) Debug.logInfo("in getAuthorContent, authorContent:" + authorContent, module);
            }
        } catch (GenericEntityException e) {
        } catch (MiniLangException e2) {
        }

        return authorContent;
    }

    public static List<String []> getPermittedDepartmentPoints(Delegator delegator, List<GenericValue> allDepartmentPoints, GenericValue userLogin, Security security, String permittedAction, String permittedOperations, String passedRoles) throws GeneralException {
        List<String []> permittedDepartmentPointList = FastList.newInstance();

        // Check that user has permission to admin sites
        for(GenericValue content : allDepartmentPoints) {
            String contentId = (String)content.get("contentId");
            String contentName = (String)content.get("contentName");
            String statusId = null;
            String entityAction = permittedAction;
            if (entityAction == null)
                entityAction = "_ADMIN";
            List<String> passedPurposes = UtilMisc.<String>toList("ARTICLE");
            List<String> roles = StringUtil.split(passedRoles, "|");
            List<String> targetOperationList = FastList.newInstance();
            if (UtilValidate.isEmpty(permittedOperations)) {
                 targetOperationList.add("CONTENT" + entityAction);
            } else {
                 targetOperationList = StringUtil.split(permittedOperations, "|");
            }
            Map<String, Object> results = null;
            //if (Debug.infoOn()) Debug.logInfo("in getPermittedDepartmentPoints, content:" + content, module);
            results = EntityPermissionChecker.checkPermission(content, statusId, userLogin, passedPurposes, targetOperationList, roles, delegator, security, entityAction);
            String permissionStatus = (String)results.get("permissionStatus");
            if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
                String [] arr = {contentId,contentName};
                permittedDepartmentPointList.add(arr);
            }
        }
        return permittedDepartmentPointList;
    }

    /**
     Returns a list of "department" (having ContentAssoc of type "DEPARTMENT")
     Content entities that are children of parentPubPt

     @param parentPubPt The parent publish point.
     */
    public static List<GenericValue> getAllDepartmentContent(Delegator delegator, String parentPubPt) throws GeneralException {
        List<GenericValue> relatedPubPts = null;
        try {
            relatedPubPts = delegator.findByAnd("ContentAssoc", UtilMisc.toMap("contentIdTo", parentPubPt, "contentAssocTypeId", "DEPARTMENT"), null, true);

        } catch (GenericEntityException e) {
            throw new GeneralException(e.getMessage());
        }
        List<GenericValue> allDepartmentPoints = FastList.newInstance();
        GenericValue departmentContent = null;
        for(GenericValue contentAssoc : relatedPubPts) {
           String pub = (String)contentAssoc.get("contentId");
           departmentContent = delegator.findOne("Content", UtilMisc.toMap("contentId", pub), true);
           allDepartmentPoints.add(departmentContent);
        }
        return allDepartmentPoints;
    }

    public static String getUserName(HttpServletRequest request, String userLoginId) throws GenericEntityException {
        String userName = null;
        Delegator delegator = (Delegator)request.getAttribute("delegator");
        GenericValue userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), true);
        GenericValue person = userLogin.getRelatedOne("Person", true);
        userName = person.getString("firstName") + " " + person.getString("lastName");
        return userName;
    }

    public static int updateStatsTopDown(Delegator delegator, String contentId, List<String> typeList) throws GenericEntityException {
        int subLeafCount = 0;
        GenericValue thisContent = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
        if (thisContent == null)
            throw new RuntimeException("No entity found for id=" + contentId);

       EntityCondition conditionMain = null;
       if (typeList.size() > 0) {
           EntityCondition conditionType = EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, typeList);
           conditionMain = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentId), conditionType), EntityOperator.AND);
       } else {
           conditionMain = EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentId);
       }
        List<GenericValue> listAll = delegator.findList("ContentAssoc", conditionMain, null, null, null, true);
        List<GenericValue> listFiltered = EntityUtil.filterByDate(listAll);
        for(GenericValue contentAssoc : listFiltered) {
            String subContentId = contentAssoc.getString("contentId");
            subLeafCount += updateStatsTopDown(delegator, subContentId, typeList);
        }

        // If no children, count this as a leaf
        if (subLeafCount == 0)
            subLeafCount = 1;
        thisContent.put("childBranchCount", Long.valueOf(listFiltered.size()));
        thisContent.put("childLeafCount", Long.valueOf(subLeafCount));
        thisContent.store();

        return subLeafCount;
    }

    public static void updateStatsBottomUp(Delegator delegator, String contentId, List<String> typeList, int branchChangeAmount, int leafChangeAmount) throws GenericEntityException {
        GenericValue thisContent = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
        if (thisContent == null)
            throw new RuntimeException("No entity found for id=" + contentId);

        EntityCondition conditionType = EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, typeList);
        EntityCondition conditionMain = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId), conditionType), EntityOperator.AND);
        List<GenericValue> listAll = delegator.findList("ContentAssoc", conditionMain, null, null, null, true);
        List<GenericValue> listFiltered = EntityUtil.filterByDate(listAll);
        for(GenericValue contentAssoc : listFiltered) {
            String contentIdTo = contentAssoc.getString("contentIdTo");
            GenericValue contentTo = delegator.findOne("Content", UtilMisc.toMap("contentId", contentIdTo), false);
            int intLeafCount = 0;
            Long leafCount = (Long)contentTo.get("childLeafCount");
            if (leafCount != null) {
                intLeafCount = leafCount.intValue();
            }
            contentTo.set("childLeafCount", Long.valueOf(intLeafCount + leafChangeAmount));

            if (branchChangeAmount != 0) {
                int intBranchCount = 0;
                Long branchCount = (Long)contentTo.get("childBranchCount");
                if (branchCount != null) {
                    intBranchCount = branchCount.intValue();
                }
                contentTo.set("childBranchCount", Long.valueOf(intBranchCount + branchChangeAmount));
            }
            contentTo.store();
            updateStatsBottomUp(delegator, contentIdTo, typeList, 0, leafChangeAmount);
        }
    }
}

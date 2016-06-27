/*
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
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.security.*;
import org.ofbiz.service.*;
import org.ofbiz.entity.model.*;
import org.ofbiz.content.ContentManagementWorker;

import javax.servlet.*;
import javax.servlet.http.*;

currentEntityMap = session.getAttribute("currentEntityMap");
if (!currentEntityMap) {
    currentEntityMap = [:];
    session.setAttribute("currentEntityMap", currentEntityMap);
}

entityName = context.entityName;
currentEntityPropertyName = parameters.currentEntityPropertyName;
if (!currentEntityPropertyName) {
    currentEntityName = entityName;
} else {
    currentEntityName = parameters[currentEntityPropertyName];
}

//Debug.logInfo("in currentvalprep, currentEntityName:" + currentEntityName,"");
cachedPK = currentEntityMap[currentEntityName];
//Debug.logInfo("in currentvalprep, cachedPK:" + cachedPK,"");

// Build a key from param or attribute values.
paramMap = UtilHttp.getParameterMap(request);
//Debug.logInfo("paramMap:" + paramMap, null);
v = delegator.makeValue(currentEntityName);
passedPK = v.getPrimaryKey();
keyColl = passedPK.getAllKeys();
keyIt = keyColl.iterator();
while (keyIt.hasNext()) {
    attrName = keyIt.next();
    attrVal = parameters[attrName];
    //Debug.logInfo("in currentvalprep, attrName:" + attrName,"");
    //Debug.logInfo("in currentvalprep, attrVal:" + attrVal,"");
    if (attrVal) {
        passedPK[attrName] = attrVal;
    }
}

//Debug.logInfo("in currentvalprep, passedPK:" + passedPK,"");
// messed up code to determine whether or not the cached or passed keys have missing fields,
// in which case, the valid one is used to retrieve the current value
pksEqual = true;
if (cachedPK) {
    keyColl = cachedPK.getPrimaryKey().getAllKeys();
    keyIt = keyColl.iterator();
    while (keyIt.hasNext()) {
        sCached = null;
        sPassed = null;
        oCached = null;
        oCached = null;
        ky = keyIt.next();
        oPassed = passedPK[ky];
        if (oPassed) {
            sPassed = oPassed;
            if (!sPassed) {
                pksEqual = false;
            } else {
                oCached = cachedPK[ky];
                if (oCached) {
                    sCached = oCached;
                    if (!sPassed) {
                        pksEqual = false;
                    } else {
                        if (!sPassed.equals(sCached)) {
                            //pksEqual = true;
                        }
                    }
                }
            }
        } else {
            pksEqual = false;
        }
    }
}

currentPK = passedPK;
if (!pksEqual) {
    currentPK = cachedPK;
   // all other condition result in currentPK = passedPK
}
//Debug.logInfo("in currentvalprep, currentPK:" + currentPK,"");

currentEntityMap[currentEntityName] = currentPK;
request.setAttribute("currentPK", currentPK);
context.currentPK = currentPK;
currentValue = from(currentPK.getPrimaryKey().getEntityName()).where(currentPK.getPrimaryKey()).queryOne();
context.currentValue = currentValue;
request.setAttribute("currentValue", currentValue);

// Debug.logInfo("===========in currentvalprep, currentValue:" + request.getAttribute("currentValue"),"");
if (currentValue) {
    if (currentEntityName.indexOf("DataResource") >= 0) {
        suffix = "";
        s = currentValue.dataResourceTypeId ?: currentValue.drDataResourceTypeId;
        if (s) suffix = "_" + s;

        if ("_ELECTRONIC_TEXT".equals(suffix)) {
            s = currentValue.mimeTypeId ?: currentValue.drMimeTypeId;
            if (s) suffix += "_" + s;
        }

        //Debug.logInfo("in currentvalprep, suffix:" + suffix,"");
//        if (suffix) {
//            ContentManagementWorker.mruAdd(session, currentPK, suffix);
//        } else {
        ContentManagementWorker.mruAdd(session, currentPK);
//        }
    } else {
        ContentManagementWorker.mruAdd(session, currentPK);
    }
}
//Debug.logInfo("in currentvalprep, contentId:" + request.getAttribute("contentId"),"");

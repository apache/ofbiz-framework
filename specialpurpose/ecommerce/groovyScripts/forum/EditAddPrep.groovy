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

import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.Iterator
import java.util.LinkedList
import java.util.List
import java.util.Map
import java.util.Set
import java.util.TreeSet
import org.apache.ofbiz.base.util.*
import org.apache.ofbiz.entity.*
import org.apache.ofbiz.security.*
import org.apache.ofbiz.service.*
import org.apache.ofbiz.entity.model.*
import org.apache.ofbiz.securityext.login.*
import org.apache.ofbiz.common.*
import org.apache.ofbiz.entity.model.*
import org.apache.ofbiz.content.ContentManagementWorker
import org.apache.ofbiz.content.content.ContentWorker

import freemarker.template.SimpleHash
import freemarker.template.SimpleSequence

import javax.servlet.*
import javax.servlet.http.*

singleWrapper = context.singleWrapper
contentIdTo = parameters.contentIdTo
singleWrapper.putInContext("contentIdTo", contentIdTo)
mapKey = parameters.mapKey
singleWrapper.putInContext("mapKey", mapKey)

if ("SUMMARY".equals(mapKey)) {
    singleWrapper.putInContext("textSource", "summaryData")
} else {
    singleWrapper.putInContext("textSource", "textData")
}

singleWrapper.putInContext("dataResourceTypeId", "ELECTRONIC_TEXT")
singleWrapper.putInContext("contentAssocTypeId", "SUB_CONTENT")
//Debug.logInfo("in editaddprep, contentIdTo:" + contentIdTo,"")
//Debug.logInfo("in editaddprep, mapKey:" + mapKey,"")
//currentValue = request.getAttribute("currentValue")
//currentValue = request.getAttribute("currentValue")

currentValue = ContentWorker.getSubContentCache(delegator, contentIdTo, mapKey, null, userLogin, null, null, false, null)
//Debug.logInfo("in editaddprep, currentValue:" + currentValue,"")

if (!currentValue) {
    parentValue = from("Content").where("contentId", contentIdTo).cache(true).queryOne()
    currentValue = delegator.makeValue("Content")
    subject =  parentValue.contentName
    if ("SUMMARY".equals(mapKey)) {
        subject = "Short " + subject
    }
    currentValue.contentName = subject
    currentValue.description = subject
    singleWrapper.putInContext("contentTypeId", "DOCUMENT")
} else {
    singleWrapper.putInContext("contentTypeId", null)
    //Debug.logInfo("in editaddprep, currentValue:" + currentValue,"")
}
singleWrapper.putInContext("currentValue", currentValue)
context.currentValue = currentValue
request.setAttribute("currentValue", currentValue)
persistAction = parameters.persistAction ?: "persistContent"

singleWrapper.putInContext("persistAction", persistAction)
//Debug.logInfo("in editaddprep, currentValue:" + currentValue,"")

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
import org.apache.ofbiz.content.content.ContentWorker
import org.apache.ofbiz.content.ContentManagementWorker

import javax.servlet.*
import javax.servlet.http.*

nodeTrailCsv = ContentManagementWorker.getFromSomewhere("nodeTrailCsv", parameters, request, context)
passedParams = null

if (!nodeTrailCsv) {
    // this only happens in UploadContentAndImage
    passedParams = request.getAttribute("passedParams")
    if (passedParams) {
        nodeTrailCsv = passedParams.nodeTrailCsv
    }
}

if (nodeTrailCsv) {
    nodeTrail = ContentWorker.csvToTrail(nodeTrailCsv, delegator)
    context.globalNodeTrail = nodeTrail
    singleWrapper = context.singleWrapper
    if (singleWrapper) {
        singleWrapper.putInContext("nodeTrailCsv",nodeTrailCsv)
        // there might be another way to do this, but if the widget form def already has a default-map
        // (such as "currentValue"), then I don't know how to reference another map (defined in the
        //  field def via "map-name", except to do this.
        // What I want to do is specify 'map-name=""' and have it use the context main
        Map dummy = singleWrapper.getFromContext("dummy")
        if (!dummy) {
           dummy = [:]
        }
        dummy.nodeTrailCsv = nodeTrailCsv
        //Debug.logInfo("in nodetrailprep, dummy:" + dummy,"")
        singleWrapper.putInContext("dummy",dummy)
    }
    context.nodeTrailCsv = nodeTrailCsv

    //Debug.logInfo("in nodetrailprep, nodeTrailCsv:" + nodeTrailCsv,"")
    trailContentList = ContentWorker.csvToContentList(nodeTrailCsv, delegator)
    //Debug.logInfo("in nodetrailprep, trailContentList:" + trailContentList,"")
    context.ancestorList = trailContentList
    //Debug.logInfo("in vewprep, siteAncestorList:" + siteAncestorList,"")
}

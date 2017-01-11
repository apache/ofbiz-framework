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
import org.apache.ofbiz.webapp.website.WebSiteWorker
import org.apache.ofbiz.securityext.login.*
import org.apache.ofbiz.common.*
import org.apache.ofbiz.entity.model.*
import org.apache.ofbiz.content.ContentManagementWorker

import javax.servlet.*
import javax.servlet.http.*

paramMap = UtilHttp.getParameterMap(request)
forumId = null
servletContext = session.getServletContext()
rootForumId = WebSiteWorker.getWebSiteId(request)
context.rootPubId = rootForumId
session.setAttribute("rootPubId", rootForumId)
request.setAttribute("rootPubId", rootForumId)
forumId = ContentManagementWorker.getFromSomewhere("forumId", paramMap, request, context)
if (forumId) {
    forumId = rootForumId
}
context.forumId = forumId
session.setAttribute("forumId", forumId)
request.setAttribute("forumId", forumId)

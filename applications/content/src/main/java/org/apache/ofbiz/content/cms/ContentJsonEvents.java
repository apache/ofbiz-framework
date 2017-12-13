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
package org.apache.ofbiz.content.cms;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericEntityNotFoundException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

public class ContentJsonEvents {

    public static final int CONTENT_NAME_MAX_LENGTH = 27;

    public static String getContentAssocs(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException, IOException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String contentId = request.getParameter("contentId");

        EntityCondition condition = EntityCondition.makeCondition(
            EntityCondition.makeCondition(UtilMisc.toMap("contentId", contentId)),
            EntityUtil.getFilterByDateExpr()
            );
        List<GenericValue> assocs = delegator.findList("ContentAssoc", condition, null, null, null, false);

        List<Map<String, Object>> nodes = new LinkedList<Map<String, Object>>();
        for (GenericValue assoc : assocs) {
            nodes.add(getTreeNode(assoc));
        }

        Collections.sort(nodes, new Comparator<Map<String, Object>>() {

            @Override
            public int compare(Map<String, Object> node1, Map<String, Object> node2) {
                Map<String, Object> data1 = UtilGenerics.cast(node1.get("data"));
                Map<String, Object> data2 = UtilGenerics.cast(node2.get("data"));
                if (data1 == null || data2 == null) {
                    return 0;
                }

                String title1 = (String) data1.get("title");
                String title2 = (String) data2.get("title");
                if (title1 == null || title2 == null) {
                    return 0;
                }

                return title1.toLowerCase(Locale.getDefault()).compareTo(title2.toLowerCase(Locale.getDefault()));
            }

        });
        IOUtils.write(JSON.from(nodes).toString(), response.getOutputStream());

        return "success";
    }

    public static String moveContent(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException, IOException {
        final Delegator delegator = (Delegator) request.getAttribute("delegator");

        final String contentIdTo = request.getParameter("contentIdTo");
        final String contentIdFrom = request.getParameter("contentIdFrom");
        final String contentIdFromNew = request.getParameter("contentIdFromNew");
        final String contentAssocTypeId = request.getParameter("contentAssocTypeId");
        final Timestamp fromDate = Timestamp.valueOf(request.getParameter("fromDate"));

        final Timestamp now = UtilDateTime.nowTimestamp();
        GenericValue assoc = TransactionUtil.inTransaction(new Callable<GenericValue>() {
            @Override
            public GenericValue call() throws Exception {
                GenericValue oldAssoc = EntityQuery.use(delegator).from("ContentAssoc").where("contentIdTo", contentIdTo, "contentId", contentIdFrom, "contentAssocTypeId", contentAssocTypeId, "fromDate", fromDate).queryOne();
                if (oldAssoc == null) {
                    throw new GenericEntityNotFoundException("Could not find ContentAssoc by primary key [contentIdTo: $contentIdTo, contentId: $contentIdFrom, contentAssocTypeId: $contentAssocTypeId, fromDate: $fromDate]");
                }
                GenericValue newAssoc = (GenericValue) oldAssoc.clone();

                oldAssoc.set("thruDate", now);
                oldAssoc.store();

                newAssoc.set("contentId", contentIdFromNew);
                newAssoc.set("fromDate", now);
                newAssoc.set("thruDate", null);
                delegator.clearCacheLine(delegator.create(newAssoc));

                return newAssoc;
            }
        }, String.format("move content [%s] from [%s] to [%s]", contentIdTo, contentIdFrom, contentIdFromNew), 0, true).call();

        IOUtils.write(JSON.from(getTreeNode(assoc)).toString(), response.getOutputStream());

        return "success";
    }

    public static String deleteContent(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String contentId = request.getParameter("contentId");

        deleteContent(delegator, contentId);

        return "success";
    }

    public static void deleteContent(Delegator delegator, String contentId) throws GenericEntityException {
        Timestamp now = UtilDateTime.nowTimestamp();
        EntityCondition condition = EntityCondition.makeCondition(
            EntityCondition.makeCondition(UtilMisc.toMap("contentIdTo", contentId)),
            EntityUtil.getFilterByDateExpr()
        );
        List<GenericValue> assocs = delegator.findList("ContentAssoc", condition, null, null, null, true);
        for (GenericValue assoc : assocs) {
            assoc.set("thruDate", now);
            delegator.store(assoc);
        }
        deleteWebPathAliases(delegator, contentId);
    }

    private static void deleteWebPathAliases(Delegator delegator, String contentId) throws GenericEntityException {
        Timestamp now = UtilDateTime.nowTimestamp();
        EntityCondition condition = EntityCondition.makeCondition(
            EntityCondition.makeCondition(UtilMisc.toMap("contentId", contentId)),
            EntityUtil.getFilterByDateExpr()
        );
        List<GenericValue> pathAliases = delegator.findList("WebSitePathAlias", condition, null, null, null, true);
        for (GenericValue alias : pathAliases) {
            alias.set("thruDate", now);
            delegator.store(alias);
        }
        List<GenericValue> subContents = delegator.findList("ContentAssoc", condition, null, null, null, true);
        for (GenericValue subContentAssoc : subContents) {
            deleteWebPathAliases(delegator, subContentAssoc.getString("contentIdTo"));
        }
    }

    private static Map<String, Object> getTreeNode(GenericValue assoc) throws GenericEntityException {
        GenericValue content = assoc.getRelatedOne("ToContent", true);
        String contentName = assoc.getString("contentIdTo");
        if (content != null && content.getString("contentName") != null) {
            contentName = content.getString("contentName");
            if (contentName.length() > CONTENT_NAME_MAX_LENGTH) {
                contentName = contentName.substring(0, CONTENT_NAME_MAX_LENGTH);
            }
        }

        Map<String, Object> data = UtilMisc.toMap(
            "title", (Object) contentName
        );

        Map<String, Object> attr = UtilMisc.toMap(
            "id", assoc.get("contentIdTo"),
            "contentId", assoc.get("contentId"),
            "fromDate", assoc.getTimestamp("fromDate").toString(),
            "contentAssocTypeId", assoc.get("contentAssocTypeId")
        );

        Map<String, Object> node = UtilMisc.toMap("data", (Object) data, "attr", (Object) attr);

        List<GenericValue> assocChildren  = content != null ? content.getRelated("FromContentAssoc", null, null, true) : null;
        assocChildren = EntityUtil.filterByDate(assocChildren);
        if (!CollectionUtils.isEmpty(assocChildren)) {
            node.put("state", "closed");
        }
        return node;
    }
}
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
package org.apache.ofbiz.content;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class ConvertTree {
    private static final String MODULE = ConvertTree.class.getName();

/*

This program will convert the output of the DOS 'tree' command into a contantAssoc tree.
the leaves in the tree will point to filenames on the local disk.

With this program and the content navigation a office file server can be replaced with a
document tree in OFBiz. From that point on the documents can be connected to the cutomers,
orders, invoices etc..

In order to make this service active add the following to the service definition file:

<service name="convertTree"  auth="true" engine="java" invoke="convertTree" transaction-timeout="3600"
                 location="org.apache.ofbiz.content.tree.ConvertTree">
    <description>Convert DOS tree output to ContentAssoc tree.</description>
    <attribute name="file" type="String" mode="IN" optional="false"/>
</service>


*/

    public static Map<String, Object> convertTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String file = (String) context.get("file");
        Map<String, Object> result = new HashMap<>();
        String errMsg = "";
        String sucMsg = "";
        GenericValue entity = null;
        if (UtilValidate.isNotEmpty(file)) {
            try (BufferedReader input = new BufferedReader(new FileReader(file))) {
                String line = null;
                int size = 0;
                int counterLine = 0;
                entity = delegator.makeValue("Content");
                entity.set("contentId", "ROOT");
                entity.set("contentName", "ROOT");
                entity.set("contentTypeId", "DOCUMENT");
                entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                entity.set("createdDate", UtilDateTime.nowTimestamp());
                entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                entity.set("createdStamp", UtilDateTime.nowTimestamp());
                entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                delegator.create(entity);

                entity = delegator.makeValue("Content");
                entity.set("contentId", "HOME_DOCUMENT");
                entity.set("contentName", "Home");
                entity.set("contentTypeId", "DOCUMENT");
                entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                entity.set("createdDate", UtilDateTime.nowTimestamp());
                entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                entity.set("createdStamp", UtilDateTime.nowTimestamp());
                entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                delegator.create(entity);

                Map<String, Object> contentAssoc = new HashMap<>();
                contentAssoc.put("contentId", "HOME_DOCUMENT");
                contentAssoc.put("contentAssocTypeId", "TREE_CHILD");
                contentAssoc.put("contentIdTo", "ROOT");
                contentAssoc.put("userLogin", userLogin);
                result = dispatcher.runSync("createContentAssoc", contentAssoc);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
                while ((line = input.readLine()) != null) { //start line
                    boolean hasFolder = true;
                    String rootContent = null;
                    String contentId = null; counterLine++;
                    if (counterLine > 1) {
                        size = line.length();
                        String check = "\\";
                        String checkSubContent = ",";
                        String contentName = "";
                        String contentNameInprogress = "";
                        String data = line.substring(3, size);
                        size = data.length();

                        for (int index = 0; index < size; index++) { //start character in line
                            boolean contentNameMatch = false;
                            int contentAssocSize = 0;
                            List<GenericValue> contentAssocs = null;
                            if (data.charAt(index) == check.charAt(0) || data.charAt(index) == checkSubContent.charAt(0)) { //store data
                                contentName = contentName + contentNameInprogress;
                                if (contentName.length() > 100) {
                                    contentName = contentName.substring(0, 100);
                                }
                                //check duplicate folder
                                GenericValue content = EntityQuery.use(delegator).from("Content").where("contentName", contentName).queryFirst();
                                if (content != null) {
                                    contentId = content.getString("contentId");
                                }
                                if (content != null && hasFolder) {
                                    if (rootContent != null) {
                                        contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                                                .where("contentId", contentId, "contentIdTo", rootContent)
                                                .queryList();
                                        List<GenericValue> contentAssocCheck = EntityQuery.use(delegator).from("ContentAssoc")
                                                .where("contentIdTo", rootContent).queryList();

                                        Iterator<GenericValue> contentAssChecks = contentAssocCheck.iterator();
                                        while (contentAssChecks.hasNext() && !contentNameMatch) {
                                            GenericValue contentAss = contentAssChecks.next();
                                            GenericValue contentcheck = EntityQuery.use(delegator).from("Content").where("contentId",
                                                    contentAss.get("contentId")).queryOne();
                                            if (contentcheck != null) {
                                                if (contentcheck.get("contentName").equals(contentName) && !contentNameMatch) {
                                                    contentNameMatch = true;
                                                    contentId = contentcheck.get("contentId").toString();
                                                }
                                            }
                                        }
                                    } else {
                                        rootContent = "HOME_DOCUMENT";
                                        contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                                                .where("contentId", contentId, "contentIdTo", rootContent)
                                                .queryList();
                                    }
                                    contentAssocSize = contentAssocs.size();
                                }

                                if (contentAssocSize == 0 && !contentNameMatch) { //New Root Content
                                    contentId = delegator.getNextSeqId("Content");
                                    entity = delegator.makeValue("Content");
                                    entity.set("contentId", contentId);
                                    entity.set("contentName", contentName);
                                    entity.set("contentTypeId", "DOCUMENT");
                                    entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                                    entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                                    entity.set("createdDate", UtilDateTime.nowTimestamp());
                                    delegator.create(entity);
                                    hasFolder = false;
                                } else {
                                    hasFolder = true;
                                }
                                //Relation Content
                                if (rootContent == null) {
                                    rootContent = "HOME_DOCUMENT";
                                }
                                contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                                        .where("contentId", contentId,
                                                "contentIdTo", rootContent,
                                                "contentAssocTypeId", "TREE_CHILD")
                                        .queryList();

                                if (contentAssocs.isEmpty()) {
                                    contentAssoc = new HashMap<>();
                                    contentAssoc.put("contentId", contentId);
                                    contentAssoc.put("contentAssocTypeId", "TREE_CHILD");
                                    contentAssoc.put("contentIdTo", rootContent);
                                    contentAssoc.put("userLogin", userLogin);
                                    result = dispatcher.runSync("createContentAssoc", contentAssoc);
                                    if (ServiceUtil.isError(result)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                                    }
                                    rootContent = contentId;
                                } else {
                                    //Debug.logInfo("ContentAssoc [contentId= " + contentId + ", contentIdTo=" + rootContent
                                    // + "] already exist.");//ShoW log file
                                    rootContent = contentId;
                                }
                                contentName = "";
                                contentNameInprogress = "";
                            }
                            if (data.charAt(index) == checkSubContent.charAt(0)) {
                                createSubContent(index, data, rootContent, context, dctx);
                                index = size;
                                continue;
                            }
                            if ((data.charAt(index)) != check.charAt(0)) {
                                contentNameInprogress = contentNameInprogress.concat(Character.toString(data.charAt(index)));
                                if (contentNameInprogress.length() > 99) {
                                    contentName = contentName + contentNameInprogress;
                                    contentNameInprogress = "";
                                }
                            }
                        } //end character in line
                    }
                } //end line
                sucMsg = UtilProperties.getMessage("ContentUiLabels", "ContentConvertDocumentsTreeSuccessful",
                        UtilMisc.toMap("counterLine", counterLine), locale);
            } catch (IOException | GenericServiceException | GenericEntityException e) {
                errMsg = "Exception " + UtilMisc.toMap("errMessage", e.toString());
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }
        }
        return ServiceUtil.returnSuccess(sucMsg);
    }

    public static Map<String, Object> createSubContent(int index, String line, String rootContent,
                                                       Map<String, ? extends Object> context, DispatchContext dctx) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = new HashMap<>();
        String subContents = null;
        String check = ",";
        String oldChar = "\"";
        String newChar = "";
        String contentNameInprogress = "";
        String contentName = "";
        String contentId = null;
        GenericValue entity = null;
        String errMsg = "";
        String sucMsg = "";
        subContents = line.substring(index + 1, line.length());
        subContents = subContents.replace(oldChar, newChar);
        int size = subContents.length();
        try {
            for (index = 0; index < size; index++) { //start character in line
                boolean contentNameMatch = false;
                if (subContents.charAt(index) == check.charAt(0)) { //store data
                    contentName = contentName + contentNameInprogress;
                    if (contentName.length() > 100) {
                        contentName = contentName.substring(0, 100);
                    }
                    List<GenericValue> contents = EntityQuery.use(delegator).from("Content").where("contentName",
                            contentName).orderBy("-contentId").queryList();
                    if (contents != null) {
                        Iterator<GenericValue> contentCheck = contents.iterator();
                        while (contentCheck.hasNext() && !contentNameMatch) {
                            GenericValue contentch = contentCheck.next();
                            if (contentch != null) {
                                List<GenericValue> contentAssocsChecks = EntityQuery.use(delegator).from("ContentAssoc")
                                        .where("contentId", contentch.get("contentId"), "contentIdTo", rootContent)
                                        .queryList();
                                if (!contentAssocsChecks.isEmpty()) {
                                    contentNameMatch = true;
                                }
                            }
                        }
                    }
                    if (!contentNameMatch) {
                        //create DataResource
                        Map<String, Object> data = new HashMap<>();
                        data.put("userLogin", userLogin);
                        result = dispatcher.runSync("createDataResource", data);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                        String dataResourceId = (String) result.get("dataResourceId");
                        //create Content
                        contentId = delegator.getNextSeqId("Content");
                        entity = delegator.makeValue("Content");
                        entity.set("contentId", contentId);
                        entity.set("contentName", contentName);
                        entity.set("contentTypeId", "DOCUMENT");
                        entity.set("dataResourceId", dataResourceId);
                        entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                        entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                        entity.set("createdDate", UtilDateTime.nowTimestamp());
                        entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                        entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                        entity.set("createdStamp", UtilDateTime.nowTimestamp());
                        entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                        delegator.create(entity);

                        //Relation Content
                        Map<String, Object> contentAssoc = new HashMap<>();
                        contentAssoc.put("contentId", contentId);
                        contentAssoc.put("contentAssocTypeId", "SUB_CONTENT");
                        contentAssoc.put("contentIdTo", rootContent);
                        contentAssoc.put("userLogin", userLogin);
                        result = dispatcher.runSync("createContentAssoc", contentAssoc);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                    }
                    contentName = "";
                    contentNameInprogress = "";
                }

                if ((subContents.charAt(index)) != check.charAt(0)) {
                    contentNameInprogress = contentNameInprogress.concat(Character.toString(subContents.charAt(index)));
                    if (contentNameInprogress.length() > 99) {
                        contentName = contentName + contentNameInprogress;
                        contentNameInprogress = "";
                    }
                }
                //lastItem
                if (index == size - 1) {
                    contentNameMatch = false;
                    List<GenericValue> contents = EntityQuery.use(delegator).from("Content").where("contentName", contentName).queryList();
                    if (contents != null) {
                        Iterator<GenericValue> contentCheck = contents.iterator();
                        while (contentCheck.hasNext() && !contentNameMatch) {
                            GenericValue contentch = contentCheck.next();
                            if (contentch != null) {
                                long contentAssocCount = EntityQuery.use(delegator).from("ContentAssoc")
                                        .where("contentId", contentch.get("contentId"), "contentIdTo", rootContent)
                                        .queryCount();
                                if (contentAssocCount > 0) {
                                    contentNameMatch = true;
                                }
                            }
                        }
                    }
                    if (!contentNameMatch) {
                        //create DataResource
                        Map<String, Object> data = new HashMap<>();
                        data.put("userLogin", userLogin);
                        result = dispatcher.runSync("createDataResource", data);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                        String dataResourceId = (String) result.get("dataResourceId");
                        //create Content
                        contentId = delegator.getNextSeqId("Content");
                        entity = delegator.makeValue("Content");
                        entity.set("contentId", contentId);
                        entity.set("contentName", contentName);
                        entity.set("contentTypeId", "DOCUMENT");
                        entity.set("dataResourceId", dataResourceId);
                        entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                        entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                        entity.set("createdDate", UtilDateTime.nowTimestamp());
                        entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                        entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                        entity.set("createdStamp", UtilDateTime.nowTimestamp());
                        entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                        delegator.create(entity);

                        //create ContentAssoc
                        Map<String, Object> contentAssoc = new HashMap<>();
                        contentAssoc.put("contentId", contentId);
                        contentAssoc.put("contentAssocTypeId", "SUB_CONTENT");
                        contentAssoc.put("contentIdTo", rootContent);
                        contentAssoc.put("userLogin", userLogin);
                        result = dispatcher.runSync("createContentAssoc", contentAssoc);
                        if (ServiceUtil.isError(result)) {
                            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                        }
                    }
                }
            }
            return ServiceUtil.returnSuccess(sucMsg);
        } catch (GenericEntityException e) {
            errMsg = "GenericEntityException " + UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericServiceException e) {
            errMsg = "GenericServiceException" + UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }
    }
}

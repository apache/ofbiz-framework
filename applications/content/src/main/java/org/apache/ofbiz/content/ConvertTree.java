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
import java.util.Locale;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class ConvertTree{
    public static final String module = ConvertTree.class.getName();

/*

This program will convert the output of the DOS 'tree' command into a contantAssoc tree.
the leaves in the tree will point to filenames on the local disk.

With this program and the content navigation a office file server can be replaced with a
document tree in OFBiz. From that point on the documents can be connected to the cutomers,
orders, invoices etc..

In order ta make this service active add the following to the service definition file:

<service name="convertTree"  auth="true" engine="java" invoke="convertTree" transaction-timeout="3600"
                 location="org.apache.ofbiz.content.tree.ConvertTree">
    <description>Convert DOS tree output to ContentAssoc tree.</description>
    <attribute name="file" type="String" mode="IN" optional="false"/>
</service>


*/


    public static  Map<String, Object> convertTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String file = (String) context.get("file");
        String errMsg = "", sucMsg= "";
        GenericValue Entity = null;
        try {
            BufferedReader input = null;
            try {
                if (UtilValidate.isNotEmpty(file)) {
                    input = new BufferedReader(new FileReader(file));
                    String line = null;
                    int size = 0;
                    if (file != null) {
                        int counterLine = 0;
                        //Home Document
                        Entity = null;
                        Entity = delegator.makeValue("Content");
                        Entity.set("contentId", "ROOT");
                        Entity.set("contentName", "ROOT");
                        Entity.set("contentTypeId", "DOCUMENT");
                        Entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("createdDate", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                        delegator.create(Entity);

                        Entity = null;
                        Entity = delegator.makeValue("Content");
                        Entity.set("contentId", "HOME_DUCUMENT");
                        Entity.set("contentName", "Home");
                        Entity.set("contentTypeId", "DOCUMENT");
                        Entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("createdDate", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                        delegator.create(Entity);

                        Map<String, Object> contentAssoc = new HashMap<String, Object>();
                        contentAssoc.put("contentId", "HOME_DUCUMENT");
                        contentAssoc.put("contentAssocTypeId", "TREE_CHILD");
                        contentAssoc.put("contentIdTo", "ROOT");
                        contentAssoc.put("userLogin", userLogin);
                        dispatcher.runSync("createContentAssoc", contentAssoc);
                        int recordCount = 0;
                        while ((line = input.readLine()) != null) {//start line
                             boolean hasFolder = true;
                             String rootContent = null, contentId = null; counterLine++;
                             if (counterLine > 1) {
                                size = line.length();
                                String check = "\\", checkSubContent = ",", contentName = "", contentNameInprogress = "", data = line.substring(3, size);
                                size = data.length();

                                for (int index = 0; index< size; index++) {//start character in line
                                    boolean contentNameMatch = false;
                                    int contentAssocSize = 0;
                                    List<GenericValue> contentAssocs = null;
                                    if (data.charAt(index) == check.charAt(0) || data.charAt(index) == checkSubContent.charAt(0)) {//store data
                                        contentName = contentName + contentNameInprogress;
                                        if (contentName.length() > 100) {
                                            contentName = contentName.substring(0, 100);
                                        }
                                        //check duplicate folder
                                        GenericValue content = EntityQuery.use(delegator).from("Content").where("contentName", contentName).queryFirst();
                                        if (content != null) {
                                            contentId = content.getString("contentId");
                                        }
                                        if (content != null && hasFolder==true) {
                                            if (rootContent != null) {
                                                contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                                                        .where("contentId", contentId, "contentIdTo", rootContent)
                                                        .queryList();
                                                List<GenericValue> contentAssocCheck = EntityQuery.use(delegator).from("ContentAssoc").where("contentIdTo", rootContent).queryList();

                                                Iterator<GenericValue> contentAssChecks = contentAssocCheck.iterator();
                                                while (contentAssChecks.hasNext() && contentNameMatch == false) {
                                                    GenericValue contentAss = contentAssChecks.next();
                                                    GenericValue contentcheck = EntityQuery.use(delegator).from("Content").where("contentId", contentAss.get("contentId")).queryOne();
                                                    if (contentcheck!=null) {
                                                        if (contentcheck.get("contentName").equals(contentName) && contentNameMatch == false) {
                                                            contentNameMatch = true;
                                                            contentId = contentcheck.get("contentId").toString();
                                                        }
                                                    }
                                                }
                                            } else {
                                                rootContent = "HOME_DUCUMENT";
                                                contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                                                        .where("contentId", contentId, "contentIdTo", rootContent)
                                                        .queryList();
                                            }
                                            contentAssocSize = contentAssocs.size();
                                        }

                                        if (contentAssocSize == 0 && contentNameMatch == false) {//New Root Content
                                            Entity = null;
                                            contentId = delegator.getNextSeqId("Content");
                                            Entity = delegator.makeValue("Content");
                                            Entity.set("contentId", contentId);
                                            Entity.set("contentName", contentName);
                                            Entity.set("contentTypeId", "DOCUMENT");
                                            Entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                                            Entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                                            Entity.set("createdDate", UtilDateTime.nowTimestamp());
                                            delegator.create(Entity);
                                            hasFolder = false;
                                        } else {
                                            hasFolder = true;
                                        }
                                        //Relation Content
                                        if (rootContent == null) {
                                            rootContent = "HOME_DUCUMENT";
                                        }
                                        contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                                                .where("contentId", contentId, 
                                                        "contentIdTo", rootContent,
                                                        "contentAssocTypeId", "TREE_CHILD")
                                                .queryList();

                                        if (contentAssocs.size() == 0) {
                                            contentAssoc = new HashMap<String, Object>();
                                            contentAssoc.put("contentId", contentId);
                                            contentAssoc.put("contentAssocTypeId", "TREE_CHILD");
                                            contentAssoc.put("contentIdTo", rootContent);
                                            contentAssoc.put("userLogin", userLogin);
                                            dispatcher.runSync("createContentAssoc", contentAssoc);
                                            rootContent = contentId;
                                        } else {
                                            //Debug.logInfo("ContentAssoc [contentId= " + contentId + ", contentIdTo=" + rootContent + "] already exist.");//ShoW log file
                                            rootContent=contentId;
                                        }
                                        contentName = "";
                                        contentNameInprogress ="";
                                    }
                                    if (data.charAt(index)== checkSubContent.charAt(0)) {//Have sub content
                                        createSubContent(index, data, rootContent, context, dctx);
                                        index = size;
                                        continue;
                                    }
                                    if ((data.charAt(index)) != check.charAt(0)) {
                                        contentNameInprogress = contentNameInprogress.concat(Character.toString(data.charAt(index)));
                                        if (contentNameInprogress.length() > 99) {
                                            contentName = contentName + contentNameInprogress;
                                            contentNameInprogress ="";
                                        }
                                    }
                                }//end character in line
                                recordCount++;
                            }
                        }//end line
                        sucMsg = UtilProperties.getMessage("ContentUiLabels", "ContentConvertDocumentsTreeSuccessful", UtilMisc.toMap("counterLine", counterLine), locale);
                    }
                }
             }
             finally {
                 input.close();
             }
             return ServiceUtil.returnSuccess(sucMsg);
        } catch (IOException e) {
            errMsg = "IOException "+ UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericServiceException e) {
            errMsg = "GenericServiceException "+ UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericEntityException e) {
            errMsg = "GenericEntityException "+ UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, module);
            e.printStackTrace();
            return ServiceUtil.returnError(errMsg);
        }
    }

    public static Map<String,Object> createSubContent(int index, String line, String rootContent, Map<String, ? extends Object> context, DispatchContext dctx) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String subContents = null, check = ",", oldChar = "\"", newChar = "", contentNameInprogress = "", contentName = "", contentId = null;
        GenericValue Entity = null;
        String errMsg = "", sucMsg= "";
        subContents = line.substring(index + 1, line.length());
        subContents = subContents.replace(oldChar, newChar);
        int size = subContents.length();
        try {
            for (index = 0; index < size; index++) {//start character in line
                boolean contentNameMatch = false;
                if (subContents.charAt(index) == check.charAt(0)) {//store data
                    contentName = contentName + contentNameInprogress;
                    if (contentName.length()>100) {
                        contentName = contentName.substring(0,100);
                    }
                    List<GenericValue> contents = EntityQuery.use(delegator).from("Content").where("contentName", contentName).orderBy("-contentId").queryList();
                    if (contents != null) {
                        Iterator<GenericValue> contentCheck = contents.iterator();
                        while (contentCheck.hasNext() && contentNameMatch == false) {
                            GenericValue contentch = contentCheck.next();
                            if (contentch != null) {
                                List<GenericValue> contentAssocsChecks = EntityQuery.use(delegator).from("ContentAssoc")
                                        .where("contentId", contentch.get("contentId"), "contentIdTo", rootContent)
                                        .queryList();
                                if (contentAssocsChecks.size() > 0) {
                                    contentNameMatch = true;
                                }
                            }
                        }
                    }
                    contentId = null;
                    if (contentNameMatch == false) {
                        //create DataResource
                        Map<String,Object> data = new HashMap<String, Object>();
                        data.put("userLogin", userLogin);
                        String dataResourceId = dispatcher.runSync("createDataResource", data).get("dataResourceId").toString();

                        //create Content
                        contentId = delegator.getNextSeqId("Content");
                        Entity = null;
                        Entity = delegator.makeValue("Content");
                        Entity.set("contentId", contentId);
                        Entity.set("contentName", contentName);
                        Entity.set("contentTypeId", "DOCUMENT");
                        Entity.set("dataResourceId", dataResourceId);
                        Entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("createdDate", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                        delegator.create(Entity);

                        //Relation Content
                        Map<String,Object> contentAssoc = new HashMap<String, Object>();
                        contentAssoc.put("contentId", contentId);
                        contentAssoc.put("contentAssocTypeId", "SUB_CONTENT");
                        contentAssoc.put("contentIdTo", rootContent);
                        contentAssoc.put("userLogin", userLogin);
                        dispatcher.runSync("createContentAssoc", contentAssoc);
                    }
                    contentName ="";
                    contentNameInprogress="";
                }

                if ((subContents.charAt(index) )!= check.charAt(0)) {
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
                        while (contentCheck.hasNext() && contentNameMatch == false) {
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
                    contentId = null;
                    if (contentNameMatch == false) {
                        //create DataResource
                        Map<String,Object> data = new HashMap<String, Object>();
                        data.put("userLogin", userLogin);
                        String dataResourceId = dispatcher.runSync("createDataResource",data).get("dataResourceId").toString();

                        //create Content
                        contentId = delegator.getNextSeqId("Content");
                        Entity = null;
                        Entity = delegator.makeValue("Content");
                        Entity.set("contentId", contentId);
                        Entity.set("contentName", contentName);
                        Entity.set("contentTypeId", "DOCUMENT");
                        Entity.set("dataResourceId", dataResourceId);
                        Entity.set("createdByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("lastModifiedByUserLogin", userLogin.get("userLoginId"));
                        Entity.set("createdDate", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
                        Entity.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdStamp", UtilDateTime.nowTimestamp());
                        Entity.set("createdTxStamp", UtilDateTime.nowTimestamp());
                        delegator.create(Entity);

                        //create ContentAssoc
                        Map<String,Object> contentAssoc = new HashMap<String, Object>();
                        contentAssoc.put("contentId", contentId);
                        contentAssoc.put("contentAssocTypeId", "SUB_CONTENT");
                        contentAssoc.put("contentIdTo", rootContent);
                        contentAssoc.put("userLogin", userLogin);
                        dispatcher.runSync("createContentAssoc", contentAssoc);
                    }
                }
            }
            return ServiceUtil.returnSuccess(sucMsg);
        } catch (GenericEntityException e) {
            errMsg = "GenericEntityException "+ UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, module);
            e.printStackTrace();
            return ServiceUtil.returnError(errMsg);
        } catch (GenericServiceException e) {
            errMsg = "GenericServiceException"+ UtilMisc.toMap("errMessage", e.toString());
            Debug.logError(e, errMsg, module);
            e.printStackTrace();
            return ServiceUtil.returnError(errMsg);
        }
    }
}

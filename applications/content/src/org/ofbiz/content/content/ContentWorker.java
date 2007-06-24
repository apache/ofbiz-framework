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
package org.ofbiz.content.content;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.content.ContentManagementWorker;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMapProcessor;
import org.ofbiz.service.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import bsh.EvalError;
import freemarker.ext.dom.NodeModel;
import javolution.util.FastMap;

/**
 * ContentWorker Class
 */
public class ContentWorker implements org.ofbiz.widget.ContentWorkerInterface {

    public static final String module = ContentWorker.class.getName();

    public ContentWorker() { }

    public GenericValue getWebSitePublishPointExt(GenericDelegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException {
        return ContentManagementWorker.getWebSitePublishPoint(delegator, contentId, ignoreCache);
    }

    public GenericValue getCurrentContentExt(GenericDelegator delegator, List trail, GenericValue userLogin, Map ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GeneralException {
        return getCurrentContent(delegator, trail, userLogin, ctx, nullThruDatesOnly, contentAssocPredicateId);
    }

    public String getMimeTypeIdExt(GenericDelegator delegator, GenericValue view, Map ctx) {
        return getMimeTypeId(delegator, view, ctx);
    }

    // new rendering methods
    public void renderContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Writer out, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        renderContentAsText(dispatcher, delegator, contentId, out, templateContext, locale, mimeTypeId, cache);
    }

    public void renderSubContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Writer out, String mapKey, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        renderSubContentAsText(dispatcher, delegator, contentId, out, mapKey, templateContext, locale, mimeTypeId, cache);
    }

    public String renderSubContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, String mapKey, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderSubContentAsText(dispatcher, delegator, contentId, mapKey, templateContext, locale, mimeTypeId, cache);
    }

    public String renderContentAsTextExt(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderContentAsText(dispatcher, delegator, contentId, templateContext, locale, mimeTypeId, cache);
    }

    // -------------------------------------
    // Content rendering methods
    // -------------------------------------

    public static String renderContentAsText(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Map templateContext,
            Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderContentAsText(dispatcher, delegator, contentId, writer, templateContext, locale, mimeTypeId, cache);
        return writer.toString();
    }

    public static void renderContentAsText(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Writer out,
            Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        GenericValue content;
        if (cache) {
            content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
        } else {
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
        }
        if (content == null) {
            throw new GeneralException("No content found for content ID [" + contentId + "]");
        }

        // if the content is a PUBLISH_POINT and the data resource is not defined; get the related content
        if (content.get("contentTypeId").equals("WEB_SITE_PUB_PT") && content.get("dataResourceId") == null) {
            List relContentIds = delegator.findByAnd("ContentAssocDataResourceViewTo",
                    UtilMisc.toMap("contentIdStart", content.get("contentId"),"statusId","CTNT_PUBLISHED",
                    "caContentAssocTypeId", "PUBLISH_LINK"), UtilMisc.toList("caFromDate"));

            relContentIds = EntityUtil.filterByDate(relContentIds, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
            if (relContentIds != null && relContentIds.size() > 0) {
                content = EntityUtil.getFirst(relContentIds);
            }

            if (content == null) {
                throw new GeneralException("No related content found for publish point [" + contentId + "]");
            }
        }

        // check for alternate content per locale
        if (locale != null) {
            String thisLocaleString = (String) content.get("localeString");
            String targetLocaleString = locale.toString();

            thisLocaleString = (thisLocaleString != null) ? thisLocaleString : "";
            if (targetLocaleString != null && !targetLocaleString.equalsIgnoreCase(thisLocaleString)) {
                GenericValue altContent = ContentWorker.findAlternateLocaleContent(delegator, content, locale);
                if (altContent != null) {
                    content = altContent;
                }
            }
        }

        // if the content has a service attached run the service
        String serviceName = content.getString("serviceName");
        if (dispatcher != null && UtilValidate.isNotEmpty(serviceName)) {
            DispatchContext dctx = dispatcher.getDispatchContext();
            ModelService service = dctx.getModelService(serviceName);
            if (service != null) {
                Map serviceCtx = service.makeValid(templateContext, ModelService.IN_PARAM);
                Map serviceRes;
                try {
                    serviceRes = dispatcher.runSync(serviceName, serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    throw e;
                }
                if (ServiceUtil.isError(serviceRes)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(serviceRes));
                } else {
                    templateContext.putAll(serviceRes);
                }
            }
        }

        // get the data resource info
        String templateDataResourceId = content.getString("templateDataResourceId");
        String dataResourceId = content.getString("dataResourceId");
        contentId = content.getString("contentId");

        if (templateContext == null) {
            templateContext = FastMap.newInstance();
        }

        // create the content facade
        ContentMapFacade facade = new ContentMapFacade(dispatcher, content, templateContext, locale, mimeTypeId, cache);

        // look for a content decorator
        String contentDecoratorId = content.getString("decoratorContentId");
        // check to see if the decorator has already been run
        boolean isDecorated = Boolean.TRUE.equals(templateContext.get("_IS_DECORATED_"));
        if (!isDecorated && UtilValidate.isNotEmpty(contentDecoratorId)) {
            // if there is a decorator content; do not render this content;
            // instead render the decorator
            GenericValue decorator;
            if (cache) {
                decorator = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentDecoratorId));
            } else {
                decorator = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentDecoratorId));
            }
            if (decorator == null) {
                throw new GeneralException("No decorator content found for decorator contentId [" + contentDecoratorId + "]");
            }

            // render the decorator
            ContentMapFacade decFacade = new ContentMapFacade(dispatcher, decorator, templateContext, locale, mimeTypeId, cache);
            facade.setIsDecorated(true);
            templateContext.put("decoratedContent", facade); // decorated content
            templateContext.put("thisContent", decFacade); // decorator content
            ContentWorker.renderContentAsText(dispatcher, delegator, contentDecoratorId, out, templateContext, locale, mimeTypeId, cache);
        } else {
            // set this content facade in the context
            templateContext.put("thisContent", facade);
            templateContext.put("contentId", contentId);

            // now if no template; just render the data
            if (UtilValidate.isEmpty(templateDataResourceId) || templateContext.containsKey("ignoreTemplate")) {
                DataResourceWorker.renderDataResourceAsText(delegator, dataResourceId, out, templateContext, locale, mimeTypeId, cache);

            // there is a template; render the data and then the template
            } else {
                Writer dataWriter = new StringWriter();
                DataResourceWorker.renderDataResourceAsText(delegator, dataResourceId, dataWriter,
                        templateContext, locale, mimeTypeId, cache);

                String textData = dataWriter.toString();
                if (textData != null) {
                    textData = textData.trim();
                }

                String mimeType;
                try {
                    mimeType = DataResourceWorker.getDataResourceMimeType(delegator, dataResourceId, null);
                } catch (GenericEntityException e) {
                    throw new GeneralException(e.getMessage());
                }

                // using FTL to handle XML? not really sure what this is doing...
                if (UtilValidate.isNotEmpty(mimeType)) {
                    if (mimeType.toLowerCase().indexOf("xml") >= 0) {
                        StringReader sr = new StringReader(textData);
                        try {
                            NodeModel nodeModel = NodeModel.parse(new InputSource(sr));
                            templateContext.put("doc", nodeModel);
                        } catch (SAXException e) {
                            throw new GeneralException(e.getMessage());
                        } catch (ParserConfigurationException e2) {
                            throw new GeneralException(e2.getMessage());
                        }
                    } else {
                        // must be text
                        templateContext.put("textData", textData);
                    }
                } else {
                    templateContext.put("textData", textData);
                }

                // render the template
                DataResourceWorker.renderDataResourceAsText(delegator, templateDataResourceId, out, templateContext, locale, mimeTypeId, cache);
            }
        }
    }

    public static String renderSubContentAsText(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, String mapKey, Map templateContext,
            Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderSubContentAsText(dispatcher, delegator, contentId, writer, mapKey, templateContext, locale, mimeTypeId, cache);
        return writer.toString();
    }

    public static void renderSubContentAsText(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, Writer out, String mapKey,
            Map templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {

        // find the sub-content with matching mapKey
        List orderBy = UtilMisc.toList("-fromDate");
        List exprs = UtilMisc.toList(new EntityExpr("contentId", EntityOperator.EQUALS, contentId),
                //new EntityExpr("contentAssocTypeId", EntityOperator.EQUALS, "SUB_CONTENT"),
                new EntityExpr("mapKey", EntityOperator.EQUALS, mapKey));

        List assocs;
        if (cache) {
            assocs = delegator.findByConditionCache("ContentAssoc", new EntityConditionList(exprs, EntityOperator.AND), null, orderBy);
        } else {
            assocs = delegator.findByCondition("ContentAssoc", new EntityConditionList(exprs, EntityOperator.AND), null, orderBy);
        }
        assocs = EntityUtil.filterByDate(assocs);
        GenericValue subContent = EntityUtil.getFirst(assocs);

        if (subContent == null) {
            //throw new GeneralException("No sub-content found with map-key [" + mapKey + "] for content [" + contentId + "]");
            out.write("<!-- no sub-content found with map-key [" + mapKey + "] for content [" + contentId + "] -->");
        } else {
            String subContentId = subContent.getString("contentIdTo");
            templateContext.put("mapKey", mapKey);
            renderContentAsText(dispatcher, delegator, subContentId, out, templateContext, locale, mimeTypeId, cache);
        }
    }

    public static GenericValue findAlternateLocaleContent(GenericDelegator delegator, GenericValue view, Locale locale) {
        GenericValue contentAssocDataResourceViewFrom = view;
        if (locale == null) {
            return contentAssocDataResourceViewFrom;
        }
        
        String localeStr = locale.toString();
        boolean isTwoLetterLocale = localeStr.length() == 2;

        List alternateViews = null;
        try {
            alternateViews = view.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding alternate locale content: " + e.toString(), module);
            return contentAssocDataResourceViewFrom;
        }
        
        alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
        Iterator alternateViewIter = alternateViews.iterator();
        while (alternateViewIter.hasNext()) {
            GenericValue thisView = (GenericValue) alternateViewIter.next();
            String currentLocaleString = thisView.getString("localeString");
            if (UtilValidate.isEmpty(currentLocaleString)) {
                continue;
            }
            
            int currentLocaleLength = currentLocaleString.length();
            
            // could be a 2 letter or 5 letter code
            if (isTwoLetterLocale) {
                if (currentLocaleLength == 2) {
                    // if the currentLocaleString is only a two letter code and the current one is a two and it matches, we are done
                    if (localeStr.equals(currentLocaleString)) {
                        contentAssocDataResourceViewFrom = thisView;
                        break;
                    }
                } else if (currentLocaleLength == 5) {
                    // if the currentLocaleString is only a two letter code and the current one is a five, match up but keep going
                    if (localeStr.equals(currentLocaleString.substring(0, 2))) {
                        contentAssocDataResourceViewFrom = thisView;
                    }
                }
            } else {
                if (currentLocaleLength == 2) {
                    // if the currentLocaleString is a five letter code and the current one is a two and it matches, keep going
                    if (localeStr.substring(0, 2).equals(currentLocaleString)) {
                        contentAssocDataResourceViewFrom = thisView;
                    }
                } else if (currentLocaleLength == 5) {
                    // if the currentLocaleString is a five letter code and the current one is a five, if it matches we are done
                    if (localeStr.equals(currentLocaleString)) {
                        contentAssocDataResourceViewFrom = thisView;
                        break;
                    }
                }
            }
        }
        
        return contentAssocDataResourceViewFrom;
    }

    public static void traverse(GenericDelegator delegator, GenericValue content, Timestamp fromDate, Timestamp thruDate, Map whenMap, int depthIdx, Map masterNode, String contentAssocTypeId, List pickList, String direction) {
        
        //String startContentAssocTypeId = null;
        String contentTypeId = null;
        String contentId = null;
        try {
            if (contentAssocTypeId == null) {
                contentAssocTypeId = "";
            }
            contentId = (String) content.get("contentId");
            contentTypeId = (String) content.get("contentTypeId");
            List topicList = content.getRelatedByAnd("ToContentAssoc", UtilMisc.toMap("contentAssocTypeId", "TOPIC"));
            List topics = new ArrayList();
            for (int i = 0; i < topicList.size(); i++) {
                GenericValue assoc = (GenericValue) topicList.get(i);
                topics.add(assoc.get("contentId"));
            }
            List keywordList = content.getRelatedByAnd("ToContentAssoc", UtilMisc.toMap("contentAssocTypeId", "KEYWORD"));
            List keywords = new ArrayList();
            for (int i = 0; i < keywordList.size(); i++) {
                GenericValue assoc = (GenericValue) keywordList.get(i);
                keywords.add(assoc.get("contentId"));
            }
            List purposeValueList = content.getRelatedCache("ContentPurpose");
            List purposes = new ArrayList();
            for (int i = 0; i < purposeValueList.size(); i++) {
                GenericValue purposeValue = (GenericValue) purposeValueList.get(i);
                purposes.add(purposeValue.get("contentPurposeTypeId"));
            }
            List contentTypeAncestry = new ArrayList();
            getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);

            Map context = new HashMap();
            context.put("content", content);
            context.put("contentAssocTypeId", contentAssocTypeId);
            //context.put("related", related);
            context.put("purposes", purposes);
            context.put("topics", topics);
            context.put("keywords", keywords);
            context.put("typeAncestry", contentTypeAncestry);
            boolean isPick = checkWhen(context, (String) whenMap.get("pickWhen"));
            boolean isReturnBefore = checkReturnWhen(context, (String) whenMap.get("returnBeforePickWhen"));
            Map thisNode = null;
            if (isPick || !isReturnBefore) {
                thisNode = new HashMap();
                thisNode.put("contentId", contentId);
                thisNode.put("contentTypeId", contentTypeId);
                thisNode.put("contentAssocTypeId", contentAssocTypeId);
                List kids = (List) masterNode.get("kids");
                if (kids == null) {
                    kids = new ArrayList();
                    masterNode.put("kids", kids);
                }
                kids.add(thisNode);
            }
            if (isPick) {
                pickList.add(content);
                thisNode.put("value", content);
            }
            boolean isReturnAfter = checkReturnWhen(context, (String) whenMap.get("returnAfterPickWhen"));
            if (!isReturnAfter) {

                List relatedAssocs = getContentAssocsWithId(delegator, contentId, fromDate, thruDate, direction, new ArrayList());
                Iterator it = relatedAssocs.iterator();
                Map assocContext = new HashMap();
                assocContext.put("related", relatedAssocs);
                while (it.hasNext()) {
                    GenericValue assocValue = (GenericValue) it.next();
                    contentAssocTypeId = (String) assocValue.get("contentAssocTypeId");
                    assocContext.put("contentAssocTypeId", contentAssocTypeId);
                    //assocContext.put("contentTypeId", assocValue.get("contentTypeId") );
                    assocContext.put("parentContent", content);
                    String assocRelation = null;
                    // This needs to be the opposite
                    String relatedDirection = null;
                    if (direction != null && direction.equalsIgnoreCase("From")) {
                        assocContext.put("contentIdFrom", assocValue.get("contentId"));
                        assocRelation = "ToContent";
                        relatedDirection = "From";
                    } else {
                        assocContext.put("contentIdTo", assocValue.get("contentId"));
                        assocRelation = "FromContent";
                        relatedDirection = "To";
                    }

                    boolean isFollow = checkWhen(assocContext, (String) whenMap.get("followWhen"));
                    if (isFollow) {
                        GenericValue thisContent = assocValue.getRelatedOne(assocRelation);
                        traverse(delegator, thisContent, fromDate, thruDate, whenMap, depthIdx + 1, thisNode, contentAssocTypeId, pickList, relatedDirection);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
    }

    public static boolean traverseSubContent(Map ctx) {

        boolean inProgress = false;
        List nodeTrail = (List)ctx.get("nodeTrail");
        ContentWorker.traceNodeTrail("11",nodeTrail);
        int sz = nodeTrail.size();
        if (sz == 0) { 
            return false;
        }

        Map currentNode = (Map)nodeTrail.get(sz - 1);
        Boolean isReturnAfter = (Boolean)currentNode.get("isReturnAfter");
        if (isReturnAfter != null && isReturnAfter.booleanValue()) {
            return false;
        }

        List kids = (List)currentNode.get("kids");
        if (kids != null && kids.size() > 0) {
            int idx = 0;
            while (idx < kids.size()) {
                currentNode = (Map)kids.get(idx);
                ContentWorker.traceNodeTrail("12",nodeTrail);
                Boolean isPick = (Boolean)currentNode.get("isPick");
               
                if (isPick != null && isPick.booleanValue()) {
                    nodeTrail.add(currentNode);
                    inProgress = true;
                    selectKids(currentNode, ctx);
                    ContentWorker.traceNodeTrail("14",nodeTrail);
                    break;
                } else {
                    Boolean isFollow = (Boolean)currentNode.get("isFollow");
                    if (isFollow != null && isFollow.booleanValue()) {
                        nodeTrail.add(currentNode);
                        boolean foundPick = traverseSubContent(ctx);
                        if (foundPick) {
                            inProgress = true;
                            break;
                        }
                    }
                }
                idx++;
            }
        } 

        if (!inProgress) {
            // look for next sibling
            while (sz > 1) {
                currentNode = (Map)nodeTrail.remove(--sz);
                ContentWorker.traceNodeTrail("15",nodeTrail);
                Map parentNode = (Map)nodeTrail.get(sz - 1);
                kids = (List)parentNode.get("kids");
                if (kids == null) {
                    continue;
                }

                int idx = kids.indexOf(currentNode);
                while (idx < (kids.size() - 1)) {
                    currentNode = (Map)kids.get(idx + 1);
                    Boolean isFollow = (Boolean)currentNode.get("isFollow");
                    if (isFollow == null || !isFollow.booleanValue()) {
                        idx++;
                        continue;
                    }
                    String contentAssocTypeId = (String)currentNode.get("contentAssocTypeId");
                    nodeTrail.add(currentNode);
                    ContentWorker.traceNodeTrail("16",nodeTrail);
                    Boolean isPick = (Boolean)currentNode.get("isPick");
                    if (isPick == null || !isPick.booleanValue()) {
                        // If not a "pick" node, look at kids
                        inProgress = traverseSubContent(ctx);
                        ContentWorker.traceNodeTrail("17",nodeTrail);
                        if (inProgress) {
                            break;
                        }
                    } else {
                        inProgress = true;
                        break;
                    }
                    idx++;
                }
                if (inProgress) {
                    break;
                }
            }
        }
        return inProgress;
    }

    public static List getPurposes(GenericValue content) {
        List purposes = new ArrayList();
        try {
            List purposeValueList = content.getRelatedCache("ContentPurpose");
            for (int i = 0; i < purposeValueList.size(); i++) {
                GenericValue purposeValue = (GenericValue) purposeValueList.get(i);
                purposes.add(purposeValue.get("contentPurposeTypeId"));
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
        return purposes;
    }

    public static List getSections(GenericValue content) {
        List sections = new ArrayList();
        try {
            List sectionValueList = content.getRelatedCache("FromContentAssoc");
            for (int i = 0; i < sectionValueList.size(); i++) {
                GenericValue sectionValue = (GenericValue) sectionValueList.get(i);
                String contentAssocPredicateId = (String)sectionValue.get("contentAssocPredicateId");
                if (contentAssocPredicateId != null && contentAssocPredicateId.equals("categorizes")) {
                    sections.add(sectionValue.get("contentIdTo"));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
        return sections;
    }

    public static List getTopics(GenericValue content) {
        List topics = new ArrayList();
        try {
            List topicValueList = content.getRelatedCache("FromContentAssoc");
            for (int i = 0; i < topicValueList.size(); i++) {
                GenericValue topicValue = (GenericValue) topicValueList.get(i);
                String contentAssocPredicateId = (String)topicValue.get("contentAssocPredicateId");
                if (contentAssocPredicateId != null && contentAssocPredicateId.equals("topifies"))
                    topics.add(topicValue.get("contentIdTo"));
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
        return topics;
    }

    public static void selectKids(Map currentNode, Map ctx) {
        
        GenericDelegator delegator = (GenericDelegator) ctx.get("delegator");
        GenericValue parentContent = (GenericValue) currentNode.get("value");
        String contentAssocTypeId = (String) ctx.get("contentAssocTypeId");
        String contentTypeId = (String) ctx.get("contentTypeId");
        String mapKey = (String) ctx.get("mapKey");
        String parentContentId = (String) parentContent.get("contentId");
        //if (Debug.infoOn()) Debug.logInfo("traverse, contentAssocTypeId:" + contentAssocTypeId,null);
        Map whenMap = (Map) ctx.get("whenMap");
        List kids = new ArrayList();
        currentNode.put("kids", kids);
        String direction = (String) ctx.get("direction");
        if (UtilValidate.isEmpty(direction))
            direction = "From";
        Timestamp fromDate = (Timestamp) ctx.get("fromDate");
        Timestamp thruDate = (Timestamp) ctx.get("thruDate");
        
        List assocTypeList = StringUtil.split(contentAssocTypeId, " ");
        List contentTypeList = StringUtil.split(contentTypeId, " ");
        String contentAssocPredicateId = null;
        Boolean nullThruDatesOnly = Boolean.TRUE;
        Map results = null;
        try {
            results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, parentContentId, mapKey, direction, null, null, assocTypeList, contentTypeList, nullThruDatesOnly, contentAssocPredicateId);
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        } catch (MiniLangException e2) {
            throw new RuntimeException(e2.getMessage());
        }
        List relatedViews = (List) results.get("entityList");
        //if (Debug.infoOn()) Debug.logInfo("traverse, relatedViews:" + relatedViews,null);
        Iterator it = relatedViews.iterator();
        while (it.hasNext()) {
            GenericValue assocValue = (GenericValue) it.next();
            Map thisNode = ContentWorker.makeNode(assocValue);
            checkConditions(delegator, thisNode, null, whenMap);
            boolean isReturnBeforePick = booleanDataType(thisNode.get("isReturnBeforePick"));
            boolean isReturnAfterPick = booleanDataType(thisNode.get("isReturnAfterPick"));
            boolean isPick = booleanDataType(thisNode.get("isPick"));
            boolean isFollow = booleanDataType(thisNode.get("isFollow"));
            kids.add(thisNode);
            if (isPick) {
                    Integer count = (Integer) currentNode.get("count");
                    if (count == null) {
                        count = new Integer(1);
                    } else {
                        count = new Integer(count.intValue() + 1);
                    }
                    currentNode.put("count", count);
            }
        }
    }

    public static boolean checkWhen(Map context, String whenStr) {
   
        boolean isWhen = true; //opposite default from checkReturnWhen
        if (whenStr != null && whenStr.length() > 0) {
            FlexibleStringExpander fse = new FlexibleStringExpander(whenStr);
            String newWhen = fse.expandString(context);
            //if (Debug.infoOn()) Debug.logInfo("newWhen:" + newWhen,null);
            //if (Debug.infoOn()) Debug.logInfo("context:" + context,null);
            try {
                Boolean isWhenObj = (Boolean) BshUtil.eval(newWhen, context);
                isWhen = isWhenObj.booleanValue();
            } catch (EvalError e) {
                Debug.logError("Error in evaluating :" + whenStr + " : " + e.getMessage(), null);
                throw new RuntimeException(e.getMessage());

            }
        }
        //if (Debug.infoOn()) Debug.logInfo("isWhen:" + isWhen,null);
        return isWhen;
    }

    public static boolean checkReturnWhen(Map context, String whenStr) {
        boolean isWhen = false; //opposite default from checkWhen
        if (whenStr != null && whenStr.length() > 0) {
            FlexibleStringExpander fse = new FlexibleStringExpander(whenStr);
            String newWhen = fse.expandString(context);
            try {
                Boolean isWhenObj = (Boolean) BshUtil.eval(newWhen, context);
                isWhen = isWhenObj.booleanValue();
            } catch (EvalError e) {
                Debug.logError("Error in evaluating :" + whenStr + " : " + e.getMessage(), null);
                throw new RuntimeException(e.getMessage());
            }
        }
        return isWhen;
    }

    public static List getAssociatedContent(GenericValue currentContent, String linkDir, List assocTypes, List contentTypes, String fromDate, String thruDate)
        throws GenericEntityException {

        GenericDelegator delegator = currentContent.getDelegator();
        List assocList = getAssociations(currentContent, linkDir, assocTypes, fromDate, thruDate);
        if (UtilValidate.isEmpty(assocList)) { 
            return assocList;
        }
        if (Debug.infoOn()) Debug.logInfo("assocList:" + assocList.size() + " contentId:" + currentContent.getString("contentId"), "");

        List contentList = new ArrayList();
        String contentIdName = "contentId";
        if (linkDir != null && linkDir.equalsIgnoreCase("TO")) {
            contentIdName = contentIdName.concat("To");
        }
        GenericValue assoc = null;
        GenericValue content = null;
        String contentTypeId = null;
        Iterator assocIt = assocList.iterator();
        while (assocIt.hasNext()) {
            assoc = (GenericValue) assocIt.next();
            String contentId = (String) assoc.get(contentIdName);
            if (Debug.infoOn()) Debug.logInfo("contentId:" + contentId, "");
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            if (contentTypes != null && contentTypes.size() > 0) {
                contentTypeId = (String) content.get("contentTypeId");
                if (contentTypes.contains(contentTypeId)) {
                    contentList.add(content);
                }
            } else {
                contentList.add(content);
            }

        }
        if (Debug.infoOn()) Debug.logInfo("contentList:" + contentList.size() , "");
        return contentList;

    }

    public static List getAssociatedContentView(GenericValue currentContent, String linkDir, List assocTypes, List contentTypes, String fromDate, String thruDate) throws GenericEntityException {
        List contentList = new ArrayList();
        List exprListAnd = new ArrayList();

        String origContentId = (String) currentContent.get("contentId");
        String contentIdName = "contentId";
        String contentAssocViewName = "contentAssocView";
        if (linkDir != null && linkDir.equalsIgnoreCase("TO")) {
            contentIdName = contentIdName.concat("To");
            contentAssocViewName = contentAssocViewName.concat("To");
        }
        EntityExpr expr = new EntityExpr(contentIdName, EntityOperator.EQUALS, origContentId);
        exprListAnd.add(expr);

        if (contentTypes.size() > 0) {
            List exprListOr = new ArrayList();
            Iterator it = contentTypes.iterator();
            while (it.hasNext()) {
                String contentType = (String) it.next();
                expr = new EntityExpr("contentTypeId", EntityOperator.EQUALS, contentType);
                exprListOr.add(expr);
            }
            EntityConditionList contentExprList = new EntityConditionList(exprListOr, EntityOperator.OR);
            exprListAnd.add(contentExprList);
        }
        if (assocTypes.size() > 0) {
            List exprListOr = new ArrayList();
            Iterator it = assocTypes.iterator();
            while (it.hasNext()) {
                String assocType = (String) it.next();
                expr = new EntityExpr("contentAssocTypeId", EntityOperator.EQUALS, assocType);
                exprListOr.add(expr);
            }
            EntityConditionList assocExprList = new EntityConditionList(exprListOr, EntityOperator.OR);
            exprListAnd.add(assocExprList);
        }

        if (fromDate != null) {
            Timestamp tsFrom = UtilDateTime.toTimestamp(fromDate);
            expr = new EntityExpr("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, tsFrom);
            exprListAnd.add(expr);
        }

        if (thruDate != null) {
            Timestamp tsThru = UtilDateTime.toTimestamp(thruDate);
            expr = new EntityExpr("thruDate", EntityOperator.LESS_THAN, tsThru);
            exprListAnd.add(expr);
        }
        EntityConditionList contentCondList = new EntityConditionList(exprListAnd, EntityOperator.AND);
        GenericDelegator delegator = currentContent.getDelegator();
        contentList = delegator.findByCondition(contentAssocViewName, contentCondList, null, null);
        return contentList;
    }

    public static List getAssociations(GenericValue currentContent, String linkDir, List assocTypes, String strFromDate, String strThruDate) throws GenericEntityException {
        GenericDelegator delegator = currentContent.getDelegator();
        String origContentId = (String) currentContent.get("contentId");
        Timestamp fromDate = null;
        if (strFromDate != null) {
            fromDate = UtilDateTime.toTimestamp(strFromDate);
        }
        Timestamp thruDate = null;
        if (strThruDate != null) {
            thruDate = UtilDateTime.toTimestamp(strThruDate);
        }
        List assocs = getContentAssocsWithId(delegator, origContentId, fromDate, thruDate, linkDir, assocTypes);
        //if (Debug.infoOn()) Debug.logInfo(" origContentId:" + origContentId + " linkDir:" + linkDir + " assocTypes:" + assocTypes, "");
        return assocs;
    }

    public static List getContentAssocsWithId(GenericDelegator delegator, String contentId, Timestamp fromDate, Timestamp thruDate, String direction, List assocTypes) throws GenericEntityException {
        List exprList = new ArrayList();
        EntityExpr joinExpr = null;
        EntityExpr expr = null;
        if (direction != null && direction.equalsIgnoreCase("From")) {
            joinExpr = new EntityExpr("contentIdTo", EntityOperator.EQUALS, contentId);
        } else {
            joinExpr = new EntityExpr("contentId", EntityOperator.EQUALS, contentId);
        }
        exprList.add(joinExpr);
        if (assocTypes != null && assocTypes.size() > 0) {
            List exprListOr = new ArrayList();
            Iterator it = assocTypes.iterator();
            while (it.hasNext()) {
                String assocType = (String) it.next();
                expr = new EntityExpr("contentAssocTypeId", EntityOperator.EQUALS, assocType);
                exprListOr.add(expr);
            }
            EntityConditionList assocExprList = new EntityConditionList(exprListOr, EntityOperator.OR);
            exprList.add(assocExprList);
        }
        if (fromDate != null) {
            EntityExpr fromExpr = new EntityExpr("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate);
            exprList.add(fromExpr);
        }
        if (thruDate != null) {
            List thruList = new ArrayList();
            //thruDate = UtilDateTime.getDayStart(thruDate, daysLater);

            EntityExpr thruExpr = new EntityExpr("thruDate", EntityOperator.LESS_THAN, thruDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = new EntityExpr("thruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList thruExprList = new EntityConditionList(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else if (fromDate != null) {
            List thruList = new ArrayList();

            EntityExpr thruExpr = new EntityExpr("thruDate", EntityOperator.GREATER_THAN, fromDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = new EntityExpr("thruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList thruExprList = new EntityConditionList(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else {
            EntityExpr thruExpr2 = new EntityExpr("thruDate", EntityOperator.EQUALS, null);
            exprList.add(thruExpr2);
        }
        EntityConditionList assocExprList = new EntityConditionList(exprList, EntityOperator.AND);
        //if (Debug.infoOn()) Debug.logInfo(" assocExprList:" + assocExprList , "");
        List relatedAssocs = delegator.findByCondition("ContentAssoc", assocExprList, new ArrayList(), UtilMisc.toList("-fromDate"));
        //if (Debug.infoOn()) Debug.logInfo(" relatedAssoc:" + relatedAssocs.size() , "");
        //for (int i = 0; i < relatedAssocs.size(); i++) {
            //GenericValue a = (GenericValue) relatedAssocs.get(i);
        //}
        return relatedAssocs;
    }

    public static void getContentTypeAncestry(GenericDelegator delegator, String contentTypeId, List contentTypes) throws GenericEntityException {
        contentTypes.add(contentTypeId);
        GenericValue contentTypeValue = delegator.findByPrimaryKey("ContentType", UtilMisc.toMap("contentTypeId", contentTypeId));
        if (contentTypeValue == null)
            return;
        String parentTypeId = (String) contentTypeValue.get("parentTypeId");
        if (parentTypeId != null) {
            getContentTypeAncestry(delegator, parentTypeId, contentTypes);
        }
    }

    public static void getContentAncestry(GenericDelegator delegator, String contentId, String contentAssocTypeId, String direction, List contentAncestorList) throws GenericEntityException {
        String contentIdField = null;
        String contentIdOtherField = null;
        if (direction != null && direction.equalsIgnoreCase("to")) {
            contentIdField = "contentId";
            contentIdOtherField = "contentIdTo";
        } else {
            contentIdField = "contentIdTo";
            contentIdOtherField = "contentId";
        }
        
        if (Debug.infoOn()) Debug.logInfo("getContentAncestry, contentId:" + contentId, "");
        if (Debug.infoOn()) Debug.logInfo("getContentAncestry, contentAssocTypeId:" + contentAssocTypeId, "");
        Map andMap = null;
        if (UtilValidate.isEmpty(contentAssocTypeId)) {
            andMap = UtilMisc.toMap(contentIdField, contentId);
        } else {
            andMap = UtilMisc.toMap(contentIdField, contentId, "contentAssocTypeId", contentAssocTypeId);
        }
        try {
            List lst = delegator.findByAndCache("ContentAssoc", andMap);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst:" + lst, "");
            List lst2 = EntityUtil.filterByDate(lst);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst2:" + lst2, "");
            if (lst2.size() > 0) {
                GenericValue contentAssoc = (GenericValue)lst2.get(0);
                getContentAncestry(delegator, contentAssoc.getString(contentIdOtherField), contentAssocTypeId, direction, contentAncestorList);
                contentAncestorList.add(contentAssoc.getString(contentIdOtherField));
            }
        } catch(GenericEntityException e) {
            Debug.logError(e,module); 
            return;
        }
    }
    
    public static void getContentAncestryAll(GenericDelegator delegator, String contentId, String passedContentTypeId, String direction, List contentAncestorList) {
        String contentIdField = null;
        String contentIdOtherField = null;
        if (direction != null && direction.equalsIgnoreCase("to")) {
            contentIdField = "contentId";
            contentIdOtherField = "contentIdTo";
        } else {
            contentIdField = "contentIdTo";
            contentIdOtherField = "contentId";
        }
        
        if (Debug.infoOn()) Debug.logInfo("getContentAncestry, contentId:" + contentId, "");
        Map andMap = UtilMisc.toMap(contentIdField, contentId);
        try {
            List lst = delegator.findByAndCache("ContentAssoc", andMap);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst:" + lst, "");
            List lst2 = EntityUtil.filterByDate(lst);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst2:" + lst2, "");
            Iterator iter = lst2.iterator();
            while (iter.hasNext()) {
                GenericValue contentAssoc = (GenericValue)iter.next();
                String contentIdOther = contentAssoc.getString(contentIdOtherField);
                if (!contentAncestorList.contains(contentIdOther)) {
                    getContentAncestryAll(delegator, contentIdOther, passedContentTypeId, direction, contentAncestorList);
                    if (!contentAncestorList.contains(contentIdOther)) {
                        GenericValue contentTo = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentIdOther));
                        String contentTypeId = contentTo.getString("contentTypeId");
                        if (contentTypeId != null && contentTypeId.equals(passedContentTypeId))
                            contentAncestorList.add(contentIdOther );
                    }
                }
            }
        } catch(GenericEntityException e) {
            Debug.logError(e,module); 
            return;
        }
    }

    public static List getContentAncestryNodeTrail(GenericDelegator delegator, String contentId, String contentAssocTypeId, String direction) throws GenericEntityException {

         List contentAncestorList = new ArrayList();
         List nodeTrail = new ArrayList();
         getContentAncestry(delegator, contentId, contentAssocTypeId, direction, contentAncestorList);
         Iterator contentAncestorListIter = contentAncestorList.iterator(); 
         while (contentAncestorListIter.hasNext()) {
             GenericValue value = (GenericValue) contentAncestorListIter.next();
             Map thisNode = ContentWorker.makeNode(value);
             nodeTrail.add(thisNode);
         }
         return nodeTrail;
    }

    public static String getContentAncestryNodeTrailCsv(GenericDelegator delegator, String contentId, String contentAssocTypeId, String direction) throws GenericEntityException {

         List contentAncestorList = new ArrayList();
         getContentAncestry(delegator, contentId, contentAssocTypeId, direction, contentAncestorList);
         String csv = StringUtil.join(contentAncestorList, ",");
         return csv;
    }

    public static void getContentAncestryValues(GenericDelegator delegator, String contentId, String contentAssocTypeId, String direction, List contentAncestorList) throws GenericEntityException {
        String contentIdField = null;
        String contentIdOtherField = null;
        if (direction != null && direction.equalsIgnoreCase("to")) {
            contentIdField = "contentId";
            contentIdOtherField = "contentIdTo";
        } else {
            contentIdField = "contentIdTo";
            contentIdOtherField = "contentId";
        }
        
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, contentId:" + contentId, "");
        try {
            List lst = delegator.findByAndCache("ContentAssoc", UtilMisc.toMap(contentIdField, contentId, "contentAssocTypeId", contentAssocTypeId));
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst:" + lst, "");
            List lst2 = EntityUtil.filterByDate(lst);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst2:" + lst2, "");
            if (lst2.size() > 0) {
                GenericValue contentAssoc = (GenericValue)lst2.get(0);
                getContentAncestryValues(delegator, contentAssoc.getString(contentIdOtherField), contentAssocTypeId, direction, contentAncestorList);
                GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentAssoc.getString(contentIdOtherField)));
                
                contentAncestorList.add(content);
            }
        } catch(GenericEntityException e) {
            Debug.logError(e,module); 
            return;
        }
    }

    public static Map pullEntityValues(GenericDelegator delegator, String entityName, Map context) {
        GenericValue entOut = delegator.makeValue(entityName, null);
        entOut.setPKFields(context);
        entOut.setNonPKFields(context);
        return (Map) entOut;
    }

    /**
     * callContentPermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static String callContentPermissionCheck(GenericDelegator delegator, LocalDispatcher dispatcher, Map context) {
        Map permResults = callContentPermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        return permissionStatus;
    }

    public static Map callContentPermissionCheckResult(GenericDelegator delegator, LocalDispatcher dispatcher, Map context) {
        
        Map permResults = new HashMap();
        String skipPermissionCheck = (String) context.get("skipPermissionCheck");

        if (skipPermissionCheck == null
            || skipPermissionCheck.length() == 0
            || (!skipPermissionCheck.equalsIgnoreCase("true") && !skipPermissionCheck.equalsIgnoreCase("granted"))) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Map serviceInMap = new HashMap();
            serviceInMap.put("userLogin", userLogin);
            serviceInMap.put("targetOperationList", context.get("targetOperationList"));
            serviceInMap.put("contentPurposeList", context.get("contentPurposeList"));
            serviceInMap.put("targetOperationString", context.get("targetOperationString"));
            serviceInMap.put("contentPurposeString", context.get("contentPurposeString"));
            serviceInMap.put("entityOperation", context.get("entityOperation"));
            serviceInMap.put("currentContent", context.get("currentContent"));
            serviceInMap.put("displayFailCond", context.get("displayFailCond"));

            try {
                permResults = dispatcher.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
            }
        } else {
            permResults.put("permissionStatus", "granted");   
        }
        return permResults;
    }

    public static GenericValue getSubContent(GenericDelegator delegator, String contentId, String mapKey, String subContentId, GenericValue userLogin, List assocTypes, Timestamp fromDate) throws IOException {
        //GenericValue content = null;
        GenericValue view = null;
        try {
            if (subContentId == null) {
                if (contentId == null) {
                    throw new GenericEntityException("contentId and subContentId are null.");
                }
                Map results = null;
                results = ContentServicesComplex.getAssocAndContentAndDataResourceMethod(delegator, contentId, mapKey, "To", fromDate, null, null, null, assocTypes, null);
                List entityList = (List) results.get("entityList");
                if (UtilValidate.isEmpty(entityList)) {
                    //throw new IOException("No subcontent found.");
                } else {
                    view = (GenericValue) entityList.get(0);
                }
            } else {
                List lst = delegator.findByAnd("ContentDataResourceView", UtilMisc.toMap("contentId", subContentId));
                if (UtilValidate.isEmpty(lst)) {
                    throw new IOException("No subContent found for subContentId=." + subContentId);
                }
                view = (GenericValue) lst.get(0);
            }
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }
        return view;
    }

    public static GenericValue getSubContentCache(GenericDelegator delegator, String contentId, String mapKey, String subContentId, GenericValue userLogin, List assocTypes, Timestamp fromDate, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException {
        //GenericValue content = null;
        GenericValue view = null;
        if (UtilValidate.isEmpty(subContentId)) {
            view = getSubContentCache(delegator, contentId, mapKey, userLogin, assocTypes, fromDate, nullThruDatesOnly, contentAssocPredicateId);
        } else {
            view = getContentCache(delegator, subContentId);
        }
        return view;
    }

    public static GenericValue getSubContentCache(GenericDelegator delegator, String contentId, String mapKey, GenericValue userLogin, List assocTypes, Timestamp fromDate, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException {
        //GenericValue content = null;
        GenericValue view = null;
        if (contentId == null) {
            Debug.logError("ContentId is null", module);
            return view;
        }
        Map results = null;
        List contentTypes = null;
        try {
            // NOTE DEJ20060610: Changed "From" to "To" because it makes the most sense for sub-content renderings using a root-contentId and mapKey to determine the sub-contentId to have the ContentAssoc go from the root to the sub, ie try to determine the contentIdTo from the contentId and mapKey
            // This shouldn't be changed from "To" to "From", but if desired could be parameterized to make this selectable in higher up calling methods
            results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, mapKey, "To", fromDate, null, assocTypes, contentTypes, nullThruDatesOnly, contentAssocPredicateId);
        } catch (MiniLangException e) {
            throw new RuntimeException(e.getMessage());
        }
        List entityList = (List) results.get("entityList");
        if (UtilValidate.isEmpty(entityList)) {
            //throw new IOException("No subcontent found.");
        } else {
            view = (GenericValue) entityList.get(0);
        }
        return view;
    }

    public static GenericValue getContentCache(GenericDelegator delegator, String contentId) throws GenericEntityException {

        GenericValue view = null;
        List lst = delegator.findByAndCache("ContentDataResourceView", UtilMisc.toMap("contentId", contentId));
            //if (Debug.infoOn()) Debug.logInfo("getContentCache, lst(2):" + lst, "");
        if (lst != null && lst.size() > 0) {
            view = (GenericValue) lst.get(0);
        }
        return view;
    }

    public static GenericValue getCurrentContent(GenericDelegator delegator, List trail, GenericValue userLogin, Map ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId)  throws GeneralException {

        String contentId = (String)ctx.get("contentId");
        String subContentId = (String)ctx.get("subContentId");
        String mapKey = (String)ctx.get("mapKey");
        Timestamp fromDate = UtilDateTime.nowTimestamp();
        List assocTypes = null;
        List passedGlobalNodeTrail = null;
        GenericValue currentContent = null;
        String viewContentId = null;
        if (trail != null && trail.size() > 0) { 
            passedGlobalNodeTrail = new ArrayList(trail);
        } else {
            passedGlobalNodeTrail = new ArrayList();
        }
        //if (Debug.infoOn()) Debug.logInfo("in getCurrentContent, passedGlobalNodeTrail(3):" + passedGlobalNodeTrail , module);
        int sz = passedGlobalNodeTrail.size();
        if (sz > 0) {
            Map nd = (Map)passedGlobalNodeTrail.get(sz - 1);
            if (nd != null)
                currentContent = (GenericValue)nd.get("value");
            if (currentContent != null) 
                viewContentId = (String)currentContent.get("contentId");
        }

        //if (Debug.infoOn()) Debug.logInfo("in getCurrentContent, currentContent(3):" + currentContent , module);
        //if (Debug.infoOn()) Debug.logInfo("getCurrentContent, contentId:" + contentId, "");
        //if (Debug.infoOn()) Debug.logInfo("getCurrentContent, subContentId:" + subContentId, "");
        //if (Debug.infoOn()) Debug.logInfo("getCurrentContent, viewContentId:" + viewContentId, "");
        if (UtilValidate.isNotEmpty(subContentId)) {
            ctx.put("subContentId", subContentId);
            ctx.put("contentId", null);
            if (viewContentId != null && viewContentId.equals(subContentId) ) {
                return currentContent;
            }
        } else {
            ctx.put("contentId", contentId);
            ctx.put("subContentId", null);
            if (viewContentId != null && viewContentId.equals(contentId) ) {
                return currentContent;
            }
        }
        //if (Debug.infoOn()) Debug.logInfo("getCurrentContent(2), contentId:" + contentId + " viewContentId:" + viewContentId + " subContentId:" + subContentId, "");
        if (UtilValidate.isNotEmpty(contentId) || UtilValidate.isNotEmpty(subContentId)) {
            try {
                currentContent = ContentWorker.getSubContentCache(delegator, contentId, mapKey, subContentId, userLogin, assocTypes, fromDate, nullThruDatesOnly, contentAssocPredicateId);
                Map node = ContentWorker.makeNode(currentContent);
                passedGlobalNodeTrail.add(node);
            } catch (GenericEntityException e) {
                throw new GeneralException(e.getMessage());
            }
        }
        ctx.put("globalNodeTrail", passedGlobalNodeTrail);
        ctx.put("indent", new Integer(sz));
        //if (Debug.infoOn()) Debug.logInfo("getCurrentContent, currentContent:" + currentContent, "");
        return currentContent;
    }

    public static GenericValue getContentFromView(GenericValue view) {
        GenericValue content = null;
        if (view == null) {
            return content;
        }
        GenericDelegator delegator = view.getDelegator();
        content = delegator.makeValue("Content", null);
        content.setPKFields(view);
        content.setNonPKFields(view);
        String dataResourceId = null;
        try {
            dataResourceId = (String) view.get("drDataResourceId");
        } catch (Exception e) {
            dataResourceId = (String) view.get("dataResourceId");
        }
        content.set("dataResourceId", dataResourceId);
        return content;
    }

    public static Map buildPickContext(GenericDelegator delegator, String contentAssocTypeId, String assocContentId, String direction, GenericValue thisContent) throws GenericEntityException {

        Map ctx = new HashMap();
        ctx.put("contentAssocTypeId", contentAssocTypeId);
        ctx.put("contentId", assocContentId);
        String assocRelation = null;
        // This needs to be the opposite
        if (direction != null && direction.equalsIgnoreCase("From")) {
            ctx.put("contentIdFrom", assocContentId);
            assocRelation = "FromContent";
        } else {
            ctx.put("contentIdTo", assocContentId);
            assocRelation = "ToContent";
        }
        if (thisContent == null)
            thisContent = delegator.findByPrimaryKeyCache("Content",
                                   UtilMisc.toMap("contentId", assocContentId));
        ctx.put("content", thisContent);
        List purposes = getPurposes(thisContent);
        ctx.put("purposes", purposes);
        List contentTypeAncestry = new ArrayList();
        String contentTypeId = (String)thisContent.get("contentTypeId");
        getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
        ctx.put("typeAncestry", contentTypeAncestry);
        List sections = getSections(thisContent);
        ctx.put("sections", sections);
        List topics = getTopics(thisContent);
        ctx.put("topics", topics);
        //Debug.logInfo("buildPickContext, ctx:" + ctx, "");
        return ctx;
    }

    public static void checkConditions(GenericDelegator delegator, Map trailNode, Map contentAssoc, Map whenMap) {

        Map context = new HashMap();
        GenericValue content = (GenericValue)trailNode.get("value");
        String contentId = (String)trailNode.get("contentId");
        if (contentAssoc == null && content != null && (content.getEntityName().indexOf("Assoc") >= 0)) {
            contentAssoc = delegator.makeValue("ContentAssoc", null);
            try {
                // TODO: locale needs to be gotten correctly
                SimpleMapProcessor.runSimpleMapProcessor("org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocIn", content, contentAssoc, new ArrayList(), Locale.getDefault());
                context.put("contentAssocTypeId", contentAssoc.get("contentAssocTypeId"));
                context.put("contentAssocPredicateId", contentAssoc.get("contentAssocPredicateId"));
                context.put("mapKey", contentAssoc.get("mapKey"));
            } catch (MiniLangException e) {
                Debug.logError(e.getMessage(), module);
                //throw new GeneralException(e.getMessage());
            }
        } else {
                context.put("contentAssocTypeId", null);
                context.put("contentAssocPredicateId", null);
                context.put("mapKey", null);
        }
        context.put("content", content);
        List purposes = getPurposes(content);
        context.put("purposes", purposes);
        List sections = getSections(content);
        context.put("sections", sections);
        List topics = getTopics(content);
        context.put("topics", topics);
        String contentTypeId = (String)content.get("contentTypeId");
        List contentTypeAncestry = new ArrayList();
        try {
            getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
        } catch(GenericEntityException e) {
        }
        context.put("typeAncestry", contentTypeAncestry);
        boolean isReturnBefore = checkReturnWhen(context, (String)whenMap.get("returnBeforePickWhen"));
        trailNode.put("isReturnBefore", new Boolean(isReturnBefore));
        boolean isPick = checkWhen(context, (String)whenMap.get("pickWhen"));
        trailNode.put("isPick", new Boolean(isPick));
        boolean isFollow = checkWhen(context, (String)whenMap.get("followWhen"));
        trailNode.put("isFollow", new Boolean(isFollow));
        boolean isReturnAfter = checkReturnWhen(context, (String)whenMap.get("returnAfterPickWhen"));
        trailNode.put("isReturnAfter", new Boolean(isReturnAfter));
        trailNode.put("checked", Boolean.TRUE);

    }

    public static boolean booleanDataType(Object boolObj) {
        boolean bool = false;
        if (boolObj != null && ((Boolean)boolObj).booleanValue()) {
            bool = true;
        }
        return bool;
    }
        

    public static List prepTargetOperationList(Map context, String md) {

        List targetOperationList = (List)context.get("targetOperationList");
        String targetOperationString = (String)context.get("targetOperationString");
        if (Debug.infoOn()) Debug.logInfo("in prepTargetOperationList, targetOperationString(0):" + targetOperationString, "");
        if (UtilValidate.isNotEmpty(targetOperationString) ) {
            List opsFromString = StringUtil.split(targetOperationString, "|");
            if (UtilValidate.isEmpty(targetOperationList)) {
                targetOperationList = new ArrayList();
            }
            targetOperationList.addAll(opsFromString);
        }
        if (UtilValidate.isEmpty(targetOperationList)) {
            targetOperationList = new ArrayList();
            if (UtilValidate.isEmpty(md))
                md ="_CREATE";
            targetOperationList.add("CONTENT" + md);
        }
        if (Debug.infoOn()) Debug.logInfo("in prepTargetOperationList, targetOperationList(0):" + targetOperationList, "");
        return targetOperationList;
    }

    /**
     * Checks to see if there is a purpose string (delimited by pipes) and 
     * turns it into a list and concants to any existing purpose list.
     * @param context
     * @return
     */
    public static List prepContentPurposeList(Map context) {

        List contentPurposeList = (List)context.get("contentPurposeList");
        String contentPurposeString = (String)context.get("contentPurposeString");
        if (Debug.infoOn()) Debug.logInfo("in prepContentPurposeList, contentPurposeString(0):" + contentPurposeString, "");
        if (UtilValidate.isNotEmpty(contentPurposeString) ) {
            List purposesFromString = StringUtil.split(contentPurposeString, "|");
            if (UtilValidate.isEmpty(contentPurposeList)) {
                contentPurposeList = new ArrayList();
            }
            contentPurposeList.addAll(purposesFromString);
        }
        if (UtilValidate.isEmpty(contentPurposeList)) {
            contentPurposeList = new ArrayList();
        }
        if (Debug.infoOn()) Debug.logInfo("in prepContentPurposeList, contentPurposeList(0):" + contentPurposeList, "");
        return contentPurposeList;
    }

    public static String prepPermissionErrorMsg(Map permResults) {

        String permissionStatus = (String)permResults.get("permissionStatus");
        String errorMessage = "Permission is denied." + permissionStatus; 
        errorMessage += ServiceUtil.getErrorMessage(permResults);
        PermissionRecorder recorder = (PermissionRecorder)permResults.get("permissionRecorder");
            Debug.logInfo("recorder(0):" + recorder, "");
        if (recorder != null && recorder.isOn()) {
            String permissionMessage = recorder.toHtml();
            //Debug.logInfo("permissionMessage(0):" + permissionMessage, "");
            errorMessage += " \n " + permissionMessage;
        }
        return errorMessage;
    }

    public static List getContentAssocViewList(GenericDelegator delegator, String contentIdTo, String contentId, String contentAssocTypeId, String statusId, String privilegeEnumId) throws GenericEntityException {

        List exprListAnd = new ArrayList();

        if (UtilValidate.isNotEmpty(contentIdTo)) {
            EntityExpr expr = new EntityExpr("caContentIdTo", EntityOperator.EQUALS, contentIdTo);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(contentId)) {
            EntityExpr expr = new EntityExpr("contentId", EntityOperator.EQUALS, contentId);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            EntityExpr expr = new EntityExpr("caContentAssocTypeId", EntityOperator.EQUALS, contentAssocTypeId);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(statusId)) {
            EntityExpr expr = new EntityExpr("statusId", EntityOperator.EQUALS, statusId);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(privilegeEnumId)) {
            EntityExpr expr = new EntityExpr("privilegeEnumId", EntityOperator.EQUALS, privilegeEnumId);
            exprListAnd.add(expr);
        }

        EntityConditionList contentCondList = new EntityConditionList(exprListAnd, EntityOperator.AND);
        List contentList = delegator.findByCondition("ContentAssocDataResourceViewFrom", contentCondList, null, null);
        List filteredList = EntityUtil.filterByDate(contentList, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
        return filteredList;
    }

    public static GenericValue getContentAssocViewFrom(GenericDelegator delegator, String contentIdTo, String contentId, String contentAssocTypeId, String statusId, String privilegeEnumId) throws GenericEntityException {

        List filteredList = getContentAssocViewList(delegator, contentIdTo, contentId, contentAssocTypeId, statusId, privilegeEnumId);

        GenericValue val = null;
        if (filteredList.size() > 0 ) {
            val = (GenericValue)filteredList.get(0);
        }
        return val;
    }

    public static Map makeNode(GenericValue thisContent) {
        Map thisNode = null;
        if (thisContent == null) 
            return thisNode;

        thisNode = new HashMap();
        thisNode.put("value", thisContent);
        String contentId = (String)thisContent.get("contentId");
        thisNode.put("contentId", contentId);
        thisNode.put("contentTypeId", thisContent.get("contentTypeId"));
        thisNode.put("isReturnBeforePick", Boolean.FALSE);
        thisNode.put("isReturnAfterPick", Boolean.FALSE);
        thisNode.put("isPick", Boolean.TRUE);
        thisNode.put("isFollow", Boolean.TRUE);
        try {
            thisNode.put("contentAssocTypeId", thisContent.get("caContentAssocTypeId"));
            thisNode.put("mapKey", thisContent.get("caMapKey"));
            thisNode.put("fromDate", thisContent.get("caFromDate"));
            thisNode.put("contentAssocTypeId", thisContent.get("caContentAssocTypeId"));
        } catch(Exception e) {
            // This ignores the case when thisContent does not have ContentAssoc values
        }
        return thisNode;
    }


    public static String nodeTrailToCsv(List nodeTrail) {
        
        if (nodeTrail == null)
            return "";
        StringBuffer csv = new StringBuffer();
        Iterator it = nodeTrail.iterator();
        while (it.hasNext()) {
            if (csv.length() > 0)
                csv.append(",");
            Map node = (Map)it.next();
            if (node == null)
                break;

            String contentId = (String)node.get("contentId");
            csv.append(contentId);
        }
        return csv.toString();
    }

    public static List csvToList(String csv, GenericDelegator delegator) {
        
        ArrayList outList = new ArrayList();
        List contentIdList = StringUtil.split(csv, ",");
        GenericValue content = null;
        String contentId = null;
        String contentName = null;
        ArrayList values = null;
        Iterator it = contentIdList.iterator();
        while (it.hasNext()) {
            contentId = (String)it.next();
            try {
                content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            } catch(GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                return new ArrayList();
            }
            contentName = (String)content.get("contentName");
            values = new ArrayList();
            values.add(contentId);
            values.add(contentName);
            outList.add(values);    
        }
        return outList;
    }

    public static List csvToContentList(String csv, GenericDelegator delegator) {

        List trail = new ArrayList();
        if (csv == null)
            return trail;

        ArrayList outList = new ArrayList();
        List contentIdList = StringUtil.split(csv, ",");
        GenericValue content = null;
        String contentId = null;
        Iterator it = contentIdList.iterator();
        while (it.hasNext()) {
            contentId = (String)it.next();
            try {
                content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            } catch(GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                return new ArrayList();
            }
            trail.add(content);
        }
        return trail;
    }

    public static List csvToTrail(String csv, GenericDelegator delegator) {

        ArrayList trail = new ArrayList();
        if (csv == null)
            return trail;

        List contentList = csvToContentList(csv, delegator);
        GenericValue content = null;
        Iterator it = contentList.iterator();
        while (it.hasNext()) {
            content = (GenericValue)it.next();
            Map node = makeNode(content);
            trail.add(node);
        }
        return trail;
    }

    public static String getMimeTypeId(GenericDelegator delegator, GenericValue view, Map ctx) {
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = (String)ctx.get("mimeTypeId");
        if (UtilValidate.isEmpty(mimeTypeId) && view != null) {
            mimeTypeId = (String) view.get("mimeTypeId");
            String parentContentId = (String)ctx.get("contentId");
            if (UtilValidate.isEmpty(mimeTypeId) && UtilValidate.isNotEmpty(parentContentId)) { // will need these below
                try {
                    GenericValue parentContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", parentContentId));
                    if (parentContent != null) {
                        mimeTypeId = (String) parentContent.get("mimeTypeId");
                        ctx.put("parentContent", parentContent);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
                    //throw new GeneralException(e.getMessage());
                }
            }

        }
        return mimeTypeId;
    }

    /*
     * Tries to find the mime type of the associated content and parent content.
     *
     * @param delegator 
     * @param view SubContentDataResourceView
     * @param parentContent Content entity
     * @param contentId part of primary key of view. To be used if view is null.
     * @param dataResourceId part of primary key of view. To be used if view is null.
     * @param parentContentId primary key of parent content. To be used if parentContent is null;
     */
    public static String determineMimeType(GenericDelegator delegator,
            GenericValue view, GenericValue parentContent, String contentId,
            String dataResourceId, String parentContentId)
            throws GenericEntityException {
        String mimeTypeId = null;

        if (view != null) {
            mimeTypeId = (String) view.get("mimeTypeId");
            String drMimeTypeId = (String) view.get("drMimeTypeId");
            if (UtilValidate.isNotEmpty(drMimeTypeId)) {
                mimeTypeId = drMimeTypeId;
            }
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            if (UtilValidate.isNotEmpty(contentId)
                    && UtilValidate.isNotEmpty(dataResourceId)) {
                view = delegator.findByPrimaryKey("SubContentDataResourceView",
                        UtilMisc.toMap("contentId", contentId,
                                "drDataResourceId", dataResourceId));
                if (view != null) {
                    mimeTypeId = (String) view.get("mimeTypeId");
                    String drMimeTypeId = (String) view.get("drMimeTypeId");
                    if (UtilValidate.isNotEmpty(drMimeTypeId)) {
                        mimeTypeId = drMimeTypeId;
                    }
                }
            }
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            if (parentContent != null) {
                mimeTypeId = (String) parentContent.get("mimeTypeId");
            }
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            if (UtilValidate.isNotEmpty(parentContentId)) {
                parentContent = delegator.findByPrimaryKey("Content", UtilMisc
                        .toMap("contentId", contentId));
                if (parentContent != null) {
                    mimeTypeId = (String) parentContent.get("mimeTypeId");
                }
            }
        }

        return mimeTypeId;
    }

    public static String logMap(String lbl, Map map, int indent) {
        String sep = ":";
        String eol = "\n";
        String spc = "";
        for (int i=0; i<indent; i++) { 
            spc += "  ";
        }
        String s = (lbl != null) ? lbl : "";
        s += "=" + indent + "==>" + eol;
        Set keySet = map.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            if ("request response session".indexOf(key) < 0) {
                Object obj = map.get(key);
                s += spc + key + sep;;
                if (obj instanceof GenericValue) {
                    GenericValue gv = (GenericValue)obj;
                    GenericPK pk = gv.getPrimaryKey();
                    s += logMap("GMAP[" + key + " name:" + pk.getEntityName()+ "]", pk, indent + 1);
                } else if (obj instanceof List) {
                    s += logList("LIST[" + ((List)obj).size() + "]", (List)obj, indent + 1);
                } else if (obj instanceof Map) {
                    s += logMap("MAP[" + key + "]", (Map)obj, indent + 1);
                } else if (obj != null) {
                    s += obj + sep + obj.getClass() + eol;
                } else {
                    s += eol;
                }
            }
        }
        return s + eol + eol;
    }

    public static String logList(String lbl, List lst, int indent) {
   
        String sep = ":";
        String eol = "\n";
        String spc = "";
        if (lst == null)
            return "";
        int sz = lst.size();
        for (int i=0; i<indent; i++) 
            spc += "  ";
        String s = (lbl != null) ? lbl : "";
        s += "=" + indent + "==> sz:" + sz + eol;
        Iterator it = lst.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
                s += spc;
                if (obj instanceof GenericValue) {
                    GenericValue gv = (GenericValue)obj;
                    GenericPK pk = gv.getPrimaryKey();
                    s += logMap("MAP[name:" + pk.getEntityName() + "]", pk, indent + 1);
                } else if (obj instanceof List) {
                    s += logList("LIST[" + ((List)obj).size() + "]", (List)obj, indent + 1);
                } else if (obj instanceof Map) {
                    s += logMap("MAP[]", (Map)obj, indent + 1);
                } else if (obj != null) {
                    s += obj + sep + obj.getClass() + eol;
                } else {
                    s += eol;
                }
        }
        return s + eol + eol;
    }
    
    public static void traceNodeTrail(String lbl, List nodeTrail) {
        /*
                if (!Debug.verboseOn()) {
                    return;
                }
                if (nodeTrail == null) {
                    return;
                }
                String s = "";
                int sz = nodeTrail.size();
                s = "nTsz:" + sz;
                if (sz > 0) {
                    Map cN = (Map)nodeTrail.get(sz - 1);
                    if (cN != null) {
                        String cid = (String)cN.get("contentId");
                        s += " cN[" + cid + "]";
                        List kids = (List)cN.get("kids");
                        int kSz = (kids == null) ? 0 : kids.size();
                        s += " kSz:" + kSz;
                        Boolean isPick = (Boolean)cN.get("isPick");
                        s += " isPick:" + isPick;
                        Boolean isFollow = (Boolean)cN.get("isFollow");
                        s += " isFollow:" + isFollow;
                        Boolean isReturnAfterPick = (Boolean)cN.get("isReturnAfterPick");
                        s += " isReturnAfterPick:" + isReturnAfterPick;
                    }
                }
        */
    }
}

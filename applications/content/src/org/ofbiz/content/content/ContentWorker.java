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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastList;
import javolution.util.FastMap;

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
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMapProcessor;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import bsh.EvalError;
import freemarker.ext.dom.NodeModel;

/**
 * ContentWorker Class
 */
public class ContentWorker implements org.ofbiz.widget.ContentWorkerInterface {

    public static final String module = ContentWorker.class.getName();

    public ContentWorker() { }

    public GenericValue getWebSitePublishPointExt(Delegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException {
        return ContentManagementWorker.getWebSitePublishPoint(delegator, contentId, ignoreCache);
    }

    public GenericValue getCurrentContentExt(Delegator delegator, List trail, GenericValue userLogin, Map ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GeneralException {
        return getCurrentContent(delegator, trail, userLogin, ctx, nullThruDatesOnly, contentAssocPredicateId);
    }

    public String getMimeTypeIdExt(Delegator delegator, GenericValue view, Map<String, Object> ctx) {
        return getMimeTypeId(delegator, view, ctx);
    }

    // new rendering methods
    public void renderContentAsTextExt(LocalDispatcher dispatcher, Delegator delegator, String contentId, Appendable out, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        renderContentAsText(dispatcher, delegator, contentId, out, templateContext, locale, mimeTypeId, null, null, cache);
    }

    public void renderSubContentAsTextExt(LocalDispatcher dispatcher, Delegator delegator, String contentId, Appendable out, String mapKey, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        renderSubContentAsText(dispatcher, delegator, contentId, out, mapKey, templateContext, locale, mimeTypeId, cache);
    }

    public String renderSubContentAsTextExt(LocalDispatcher dispatcher, Delegator delegator, String contentId, String mapKey, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderSubContentAsText(dispatcher, delegator, contentId, mapKey, templateContext, locale, mimeTypeId, cache);
    }

    public String renderContentAsTextExt(LocalDispatcher dispatcher, Delegator delegator, String contentId, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderContentAsText(dispatcher, delegator, contentId, templateContext, locale, mimeTypeId, cache);
    }

    // -------------------------------------
    // Content rendering methods
    // -------------------------------------
    public static GenericValue findContentForRendering(Delegator delegator, String contentId, Locale locale, String partyId, String roleTypeId, boolean cache) throws GeneralException, IOException {
        GenericValue content;
        if (UtilValidate.isEmpty(contentId)) {
            Debug.logError("No content ID found.", module);
            return null;
        }
        if (cache) {
            content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
        } else {
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
        }
        if (content == null) {
            throw new GeneralException("No content found for content ID [" + contentId + "]");
        }

        // if the content is a PUBLISH_POINT and the data resource is not defined; get the related content
        if ("WEB_SITE_PUB_PT".equals(content.get("contentTypeId")) && content.get("dataResourceId") == null) {
            List<GenericValue> relContentIds = delegator.findByAnd("ContentAssocDataResourceViewTo",
                    UtilMisc.toMap("contentIdStart", content.get("contentId"),"statusId","CTNT_PUBLISHED",
                    "caContentAssocTypeId", "PUBLISH_LINK"), UtilMisc.toList("caFromDate"));

            relContentIds = EntityUtil.filterByDate(relContentIds, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
            if (UtilValidate.isNotEmpty(relContentIds)) {
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

        // check for alternate content per party
        if (partyId != null && roleTypeId != null) {
            List<GenericValue> alternateViews = null;
            try {
                alternateViews = content.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_ROLE"), UtilMisc.toList("-caFromDate"));
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding alternate content: " + e.toString(), module);
            }

            alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
            Iterator<GenericValue> alternateViewIter = alternateViews.iterator();
            while (alternateViewIter.hasNext()) {
                GenericValue thisView = alternateViewIter.next();
                GenericValue altContentRole = EntityUtil.getFirst(EntityUtil.filterByDate(thisView.getRelatedByAnd("ContentRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId))));
                GenericValue altContent = null;
                if (UtilValidate.isNotEmpty(altContentRole)) {
                    altContent = altContentRole.getRelatedOne("Content");
                    if (altContent != null) {
                        content = altContent;
                    }
                }
            }
        }
        return content;
    }

    public static void renderContentAsText(LocalDispatcher dispatcher, Delegator delegator, GenericValue content, Appendable out,
            Map<String,Object>templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        // if the content has a service attached run the service

        String serviceName = content.getString("serviceName");
        if (dispatcher != null && UtilValidate.isNotEmpty(serviceName)) {
            DispatchContext dctx = dispatcher.getDispatchContext();
            ModelService service = dctx.getModelService(serviceName);
            if (service != null) {
                Map<String,Object> serviceCtx = service.makeValid(templateContext, ModelService.IN_PARAM);
                Map<String,Object> serviceRes;
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
        String contentId = content.getString("contentId");
        if (UtilValidate.isEmpty(dataResourceId)) {
            Debug.logError("No dataResourceId found for contentId: " + content.getString("contentId"), module);
            return;
        }

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
            ContentWorker.renderContentAsText(dispatcher, delegator, contentDecoratorId, out, templateContext, locale, mimeTypeId, null, null, cache);
        } else {
            // set this content facade in the context
            templateContext.put("thisContent", facade);
            templateContext.put("contentId", contentId);

            // now if no template; just render the data
            if (UtilValidate.isEmpty(templateDataResourceId) || templateContext.containsKey("ignoreTemplate")) {
                if (UtilValidate.isEmpty(contentId)) {
                    Debug.logError("No content ID found.", module);
                    return;
                }
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

                // This part is using an xml file as the input data and an ftl or xsl file to present it.
                if (UtilValidate.isNotEmpty(mimeType)) {
                    if (mimeType.toLowerCase().indexOf("xml") >= 0) {
                        GenericValue dataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
                        GenericValue templateDataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", templateDataResourceId));
                        if ("FTL".equals(templateDataResource.getString("dataTemplateTypeId"))) {
                            StringReader sr = new StringReader(textData);
                            try {
                                NodeModel nodeModel = NodeModel.parse(new InputSource(sr));
                                templateContext.put("doc", nodeModel) ;
                            } catch (SAXException e) {
                                throw new GeneralException(e.getMessage());
                            } catch (ParserConfigurationException e2) {
                                throw new GeneralException(e2.getMessage());
                            }
                        } else {
                            templateContext.put("docFile", DataResourceWorker.getContentFile(dataResource.getString("dataResourceTypeId"), dataResource.getString("objectInfo"), (String) templateContext.get("contextRoot")).getAbsoluteFile().toString());
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

    public static String renderContentAsText(LocalDispatcher dispatcher, Delegator delegator, String contentId, Map<String, Object> templateContext,
            Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderContentAsText(dispatcher, delegator, contentId, writer, templateContext, locale, mimeTypeId, null, null, cache);
        return writer.toString();
    }

    public static void renderContentAsText(LocalDispatcher dispatcher, Delegator delegator, String contentId, Appendable out,
            Map<String, Object> templateContext, Locale locale, String mimeTypeId, String partyId, String roleTypeId, boolean cache) throws GeneralException, IOException {
        GenericValue content = ContentWorker.findContentForRendering(delegator, contentId, locale, partyId, roleTypeId, cache);
        ContentWorker.renderContentAsText(dispatcher, delegator, content, out, templateContext, locale, mimeTypeId, cache);
    }

    public static String renderSubContentAsText(LocalDispatcher dispatcher, Delegator delegator, String contentId, String mapKey, Map<String, Object> templateContext,
            Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderSubContentAsText(dispatcher, delegator, contentId, writer, mapKey, templateContext, locale, mimeTypeId, cache);
        return writer.toString();
    }

    public static void renderSubContentAsText(LocalDispatcher dispatcher, Delegator delegator, String contentId, Appendable out, String mapKey,
            Map<String,Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {

        // find the sub-content with matching mapKey
        List<String> orderBy = UtilMisc.toList("-fromDate");
        List<EntityExpr> exprs = UtilMisc.toList(EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId));
        if (UtilValidate.isNotEmpty(mapKey)) {
                exprs.add(EntityCondition.makeCondition("mapKey", EntityOperator.EQUALS, mapKey));
        }

        List<GenericValue> assocs;
        assocs = delegator.findList("ContentAssoc", EntityCondition.makeCondition(exprs, EntityOperator.AND), null, orderBy, null, cache);
        assocs = EntityUtil.filterByDate(assocs);
        GenericValue subContent = EntityUtil.getFirst(assocs);

        if (subContent == null) {
            //throw new GeneralException("No sub-content found with map-key [" + mapKey + "] for content [" + contentId + "]");
            out.append("<!-- no sub-content found with map-key [" + mapKey + "] for content [" + contentId + "] -->");
        } else {
            String subContentId = subContent.getString("contentIdTo");
            templateContext.put("mapKey", mapKey);
            renderContentAsText(dispatcher, delegator, subContentId, out, templateContext, locale, mimeTypeId, null, null, cache);
        }
    }

    public static GenericValue findAlternateLocaleContent(Delegator delegator, GenericValue view, Locale locale) {
        GenericValue contentAssocDataResourceViewFrom = view;
        if (locale == null) {
            return contentAssocDataResourceViewFrom;
        }

        String localeStr = locale.toString();
        boolean isTwoLetterLocale = localeStr.length() == 2;

        List<GenericValue> alternateViews = null;
        try {
            alternateViews = view.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding alternate locale content: " + e.toString(), module);
            return contentAssocDataResourceViewFrom;
        }

        alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
        Iterator<GenericValue> alternateViewIter = alternateViews.iterator();
        while (alternateViewIter.hasNext()) {
            GenericValue thisView = alternateViewIter.next();
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

    public static void traverse(Delegator delegator, GenericValue content, Timestamp fromDate, Timestamp thruDate, Map<String, Object> whenMap, int depthIdx, Map<String, Object> masterNode, String contentAssocTypeId, List<GenericValue> pickList, String direction) {

        //String startContentAssocTypeId = null;
        String contentTypeId = null;
        String contentId = null;
        try {
            if (contentAssocTypeId == null) {
                contentAssocTypeId = "";
            }
            contentId = (String) content.get("contentId");
            contentTypeId = (String) content.get("contentTypeId");
            List<GenericValue> topicList = content.getRelatedByAnd("ToContentAssoc", UtilMisc.toMap("contentAssocTypeId", "TOPIC"));
            List<String> topics = FastList.newInstance();
            for (int i = 0; i < topicList.size(); i++) {
                GenericValue assoc = (GenericValue) topicList.get(i);
                topics.add(assoc.getString("contentId"));
            }
            List<GenericValue> keywordList = content.getRelatedByAnd("ToContentAssoc", UtilMisc.toMap("contentAssocTypeId", "KEYWORD"));
            List<String> keywords = FastList.newInstance();
            for (int i = 0; i < keywordList.size(); i++) {
                GenericValue assoc = (GenericValue) keywordList.get(i);
                keywords.add(assoc.getString("contentId"));
            }
            List<GenericValue> purposeValueList = content.getRelatedCache("ContentPurpose");
            List<String> purposes = FastList.newInstance();
            for (int i = 0; i < purposeValueList.size(); i++) {
                GenericValue purposeValue = (GenericValue) purposeValueList.get(i);
                purposes.add(purposeValue.getString("contentPurposeTypeId"));
            }
            List<GenericValue> contentTypeAncestry = FastList.newInstance();
            getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);

            Map<String, Object> context = FastMap.newInstance();
            context.put("content", content);
            context.put("contentAssocTypeId", contentAssocTypeId);
            //context.put("related", related);
            context.put("purposes", purposes);
            context.put("topics", topics);
            context.put("keywords", keywords);
            context.put("typeAncestry", contentTypeAncestry);
            boolean isPick = checkWhen(context, (String) whenMap.get("pickWhen"));
            boolean isReturnBefore = checkReturnWhen(context, (String) whenMap.get("returnBeforePickWhen"));
            Map<String, Object> thisNode = null;
            if (isPick || !isReturnBefore) {
                thisNode = FastMap.newInstance();
                thisNode.put("contentId", contentId);
                thisNode.put("contentTypeId", contentTypeId);
                thisNode.put("contentAssocTypeId", contentAssocTypeId);
                List<Map<String, Object>> kids = (List) masterNode.get("kids");
                if (kids == null) {
                    kids = FastList.newInstance();
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

                List<GenericValue> relatedAssocs = getContentAssocsWithId(delegator, contentId, fromDate, thruDate, direction, FastList.newInstance());
                Iterator<GenericValue> it = relatedAssocs.iterator();
                Map<String, Object> assocContext = FastMap.newInstance();
                assocContext.put("related", relatedAssocs);
                while (it.hasNext()) {
                    GenericValue assocValue = (GenericValue) it.next();
                    contentAssocTypeId = (String) assocValue.get("contentAssocTypeId");
                    assocContext.put("contentAssocTypeId", contentAssocTypeId);
                    //assocContext.put("contentTypeId", assocValue.get("contentTypeId"));
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

    public static boolean traverseSubContent(Map<String, Object> ctx) {

        boolean inProgress = false;
        List<Map <String, Object>> nodeTrail = (List<Map <String, Object>>)ctx.get("nodeTrail");
        ContentWorker.traceNodeTrail("11",nodeTrail);
        int sz = nodeTrail.size();
        if (sz == 0) {
            return false;
        }

        Map<String, Object> currentNode = (Map<String, Object>)nodeTrail.get(sz - 1);
        Boolean isReturnAfter = (Boolean)currentNode.get("isReturnAfter");
        if (isReturnAfter != null && isReturnAfter.booleanValue()) {
            return false;
        }

        List<Map <String, Object>> kids = (List<Map <String, Object>>)currentNode.get("kids");
        if (UtilValidate.isNotEmpty(kids)) {
            int idx = 0;
            while (idx < kids.size()) {
                currentNode = (Map<String, Object>)kids.get(idx);
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
                currentNode = (Map<String, Object>)nodeTrail.remove(--sz);
                ContentWorker.traceNodeTrail("15",nodeTrail);
                Map<String, Object> parentNode = (Map<String, Object>) nodeTrail.get(sz - 1);
                kids = (List<Map <String, Object>>)parentNode.get("kids");
                if (kids == null) {
                    continue;
                }

                int idx = kids.indexOf(currentNode);
                while (idx < (kids.size() - 1)) {
                    currentNode = (Map<String, Object>)kids.get(idx + 1);
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
        List purposes = FastList.newInstance();
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
        List sections = FastList.newInstance();
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
        List topics = FastList.newInstance();
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

        Delegator delegator = (Delegator) ctx.get("delegator");
        GenericValue parentContent = (GenericValue) currentNode.get("value");
        String contentAssocTypeId = (String) ctx.get("contentAssocTypeId");
        String contentTypeId = (String) ctx.get("contentTypeId");
        String mapKey = (String) ctx.get("mapKey");
        String parentContentId = (String) parentContent.get("contentId");
        //if (Debug.infoOn()) Debug.logInfo("traverse, contentAssocTypeId:" + contentAssocTypeId,null);
        Map whenMap = (Map) ctx.get("whenMap");
        List kids = FastList.newInstance();
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
                        count = Integer.valueOf(1);
                    } else {
                        count = Integer.valueOf(count.intValue() + 1);
                    }
                    currentNode.put("count", count);
            }
        }
    }

    public static boolean checkWhen(Map context, String whenStr) {

        boolean isWhen = true; //opposite default from checkReturnWhen
        if (UtilValidate.isNotEmpty(whenStr)) {
            FlexibleStringExpander fse = FlexibleStringExpander.getInstance(whenStr);
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

    public static boolean checkReturnWhen(Map<String, Object> context, String whenStr) {
        boolean isWhen = false; //opposite default from checkWhen
        if (UtilValidate.isNotEmpty(whenStr)) {
            FlexibleStringExpander fse = FlexibleStringExpander.getInstance(whenStr);
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

        Delegator delegator = currentContent.getDelegator();
        List<GenericValue> assocList = getAssociations(currentContent, linkDir, assocTypes, fromDate, thruDate);
        if (UtilValidate.isEmpty(assocList)) {
            return assocList;
        }
        if (Debug.infoOn()) Debug.logInfo("assocList:" + assocList.size() + " contentId:" + currentContent.getString("contentId"), "");

        List<GenericValue> contentList = FastList.newInstance();
        String contentIdName = "contentId";
        if (linkDir != null && linkDir.equalsIgnoreCase("TO")) {
            contentIdName = contentIdName.concat("To");
        }
        GenericValue assoc = null;
        GenericValue content = null;
        String contentTypeId = null;
        Iterator<GenericValue> assocIt = assocList.iterator();
        while (assocIt.hasNext()) {
            assoc = assocIt.next();
            String contentId = (String) assoc.get(contentIdName);
            if (Debug.infoOn()) Debug.logInfo("contentId:" + contentId, "");
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            if (UtilValidate.isNotEmpty(contentTypes)) {
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
        List contentList = FastList.newInstance();
        List exprListAnd = FastList.newInstance();

        String origContentId = (String) currentContent.get("contentId");
        String contentIdName = "contentId";
        String contentAssocViewName = "contentAssocView";
        if (linkDir != null && linkDir.equalsIgnoreCase("TO")) {
            contentIdName = contentIdName.concat("To");
            contentAssocViewName = contentAssocViewName.concat("To");
        }
        EntityExpr expr = EntityCondition.makeCondition(contentIdName, EntityOperator.EQUALS, origContentId);
        exprListAnd.add(expr);

        if (contentTypes.size() > 0) {
            exprListAnd.add(EntityCondition.makeCondition("contentTypeId", EntityOperator.IN, contentTypes));
        }
        if (assocTypes.size() > 0) {
            exprListAnd.add(EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, assocTypes));
        }

        if (fromDate != null) {
            Timestamp tsFrom = UtilDateTime.toTimestamp(fromDate);
            expr = EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, tsFrom);
            exprListAnd.add(expr);
        }

        if (thruDate != null) {
            Timestamp tsThru = UtilDateTime.toTimestamp(thruDate);
            expr = EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN, tsThru);
            exprListAnd.add(expr);
        }
        EntityConditionList contentCondList = EntityCondition.makeCondition(exprListAnd, EntityOperator.AND);
        Delegator delegator = currentContent.getDelegator();
        contentList = delegator.findList(contentAssocViewName, contentCondList, null, null, null, false);
        return contentList;
    }

    public static List<GenericValue> getAssociations(GenericValue currentContent, String linkDir, List<GenericValue> assocTypes, String strFromDate, String strThruDate) throws GenericEntityException {
        Delegator delegator = currentContent.getDelegator();
        String origContentId = (String) currentContent.get("contentId");
        Timestamp fromDate = null;
        if (strFromDate != null) {
            fromDate = UtilDateTime.toTimestamp(strFromDate);
        }
        Timestamp thruDate = null;
        if (strThruDate != null) {
            thruDate = UtilDateTime.toTimestamp(strThruDate);
        }
        List<GenericValue> assocs = getContentAssocsWithId(delegator, origContentId, fromDate, thruDate, linkDir, assocTypes);
        //if (Debug.infoOn()) Debug.logInfo(" origContentId:" + origContentId + " linkDir:" + linkDir + " assocTypes:" + assocTypes, "");
        return assocs;
    }

    public static List<GenericValue> getContentAssocsWithId(Delegator delegator, String contentId, Timestamp fromDate, Timestamp thruDate, String direction, List assocTypes) throws GenericEntityException {
        List exprList = FastList.newInstance();
        EntityExpr joinExpr = null;
        EntityExpr expr = null;
        if (direction != null && direction.equalsIgnoreCase("From")) {
            joinExpr = EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentId);
        } else {
            joinExpr = EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId);
        }
        exprList.add(joinExpr);
        if (UtilValidate.isNotEmpty(assocTypes)) {
            List exprListOr = FastList.newInstance();
            Iterator it = assocTypes.iterator();
            while (it.hasNext()) {
                String assocType = (String) it.next();
                expr = EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.EQUALS, assocType);
                exprListOr.add(expr);
            }
            EntityConditionList assocExprList = EntityCondition.makeCondition(exprListOr, EntityOperator.OR);
            exprList.add(assocExprList);
        }
        if (fromDate != null) {
            EntityExpr fromExpr = EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate);
            exprList.add(fromExpr);
        }
        if (thruDate != null) {
            List thruList = FastList.newInstance();
            //thruDate = UtilDateTime.getDayStart(thruDate, daysLater);

            EntityExpr thruExpr = EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN, thruDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList thruExprList = EntityCondition.makeCondition(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else if (fromDate != null) {
            List thruList = FastList.newInstance();

            EntityExpr thruExpr = EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, fromDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList thruExprList = EntityCondition.makeCondition(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else {
            EntityExpr thruExpr2 = EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null);
            exprList.add(thruExpr2);
        }
        EntityConditionList assocExprList = EntityCondition.makeCondition(exprList, EntityOperator.AND);
        //if (Debug.infoOn()) Debug.logInfo(" assocExprList:" + assocExprList , "");
        List relatedAssocs = delegator.findList("ContentAssoc", assocExprList, null, UtilMisc.toList("-fromDate"), null, false);
        //if (Debug.infoOn()) Debug.logInfo(" relatedAssoc:" + relatedAssocs.size() , "");
        //for (int i = 0; i < relatedAssocs.size(); i++) {
            //GenericValue a = (GenericValue) relatedAssocs.get(i);
        //}
        return relatedAssocs;
    }

    public static void getContentTypeAncestry(Delegator delegator, String contentTypeId, List contentTypes) throws GenericEntityException {
        contentTypes.add(contentTypeId);
        GenericValue contentTypeValue = delegator.findByPrimaryKey("ContentType", UtilMisc.toMap("contentTypeId", contentTypeId));
        if (contentTypeValue == null)
            return;
        String parentTypeId = (String) contentTypeValue.get("parentTypeId");
        if (parentTypeId != null) {
            getContentTypeAncestry(delegator, parentTypeId, contentTypes);
        }
    }

    public static void getContentAncestry(Delegator delegator, String contentId, String contentAssocTypeId, String direction, List contentAncestorList) throws GenericEntityException {
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
            List<GenericValue> lst = delegator.findByAndCache("ContentAssoc", andMap);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst:" + lst, "");
            List<GenericValue> lst2 = EntityUtil.filterByDate(lst);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst2:" + lst2, "");
            if (lst2.size() > 0) {
                GenericValue contentAssoc = (GenericValue)lst2.get(0);
                getContentAncestry(delegator, contentAssoc.getString(contentIdOtherField), contentAssocTypeId, direction, contentAncestorList);
                contentAncestorList.add(contentAssoc.getString(contentIdOtherField));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e,module);
            return;
        }
    }

    public static void getContentAncestryAll(Delegator delegator, String contentId, String passedContentTypeId, String direction, List contentAncestorList) {
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
            List<GenericValue> lst = delegator.findByAndCache("ContentAssoc", andMap);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst:" + lst, "");
            List<GenericValue> lst2 = EntityUtil.filterByDate(lst);
            //if (Debug.infoOn()) Debug.logInfo("getContentAncestry, lst2:" + lst2, "");
            Iterator<GenericValue> iter = lst2.iterator();
            while (iter.hasNext()) {
                GenericValue contentAssoc = (GenericValue)iter.next();
                String contentIdOther = contentAssoc.getString(contentIdOtherField);
                if (!contentAncestorList.contains(contentIdOther)) {
                    getContentAncestryAll(delegator, contentIdOther, passedContentTypeId, direction, contentAncestorList);
                    if (!contentAncestorList.contains(contentIdOther)) {
                        GenericValue contentTo = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentIdOther));
                        String contentTypeId = contentTo.getString("contentTypeId");
                        if (contentTypeId != null && contentTypeId.equals(passedContentTypeId))
                            contentAncestorList.add(contentIdOther);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e,module);
            return;
        }
    }

    public static List<Map<String, Object>> getContentAncestryNodeTrail(Delegator delegator, String contentId, String contentAssocTypeId, String direction) throws GenericEntityException {

         List contentAncestorList = FastList.newInstance();
         List<Map<String, Object>> nodeTrail = FastList.newInstance();
         getContentAncestry(delegator, contentId, contentAssocTypeId, direction, contentAncestorList);
         Iterator<GenericValue> contentAncestorListIter = contentAncestorList.iterator();
         while (contentAncestorListIter.hasNext()) {
             GenericValue value = contentAncestorListIter.next();
             Map<String, Object> thisNode = ContentWorker.makeNode(value);
             nodeTrail.add(thisNode);
         }
         return nodeTrail;
    }

    public static String getContentAncestryNodeTrailCsv(Delegator delegator, String contentId, String contentAssocTypeId, String direction) throws GenericEntityException {

         List contentAncestorList = FastList.newInstance();
         getContentAncestry(delegator, contentId, contentAssocTypeId, direction, contentAncestorList);
         String csv = StringUtil.join(contentAncestorList, ",");
         return csv;
    }

    public static void getContentAncestryValues(Delegator delegator, String contentId, String contentAssocTypeId, String direction, List contentAncestorList) throws GenericEntityException {
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
        } catch (GenericEntityException e) {
            Debug.logError(e,module);
            return;
        }
    }

    public static Map pullEntityValues(Delegator delegator, String entityName, Map context) {
        GenericValue entOut = delegator.makeValue(entityName);
        entOut.setPKFields(context);
        entOut.setNonPKFields(context);
        return (Map) entOut;
    }

    /**
     * callContentPermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static String callContentPermissionCheck(Delegator delegator, LocalDispatcher dispatcher, Map context) {
        Map permResults = callContentPermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        return permissionStatus;
    }

    public static Map callContentPermissionCheckResult(Delegator delegator, LocalDispatcher dispatcher, Map context) {

        Map permResults = FastMap.newInstance();
        String skipPermissionCheck = (String) context.get("skipPermissionCheck");

        if (UtilValidate.isEmpty(skipPermissionCheck)
            || (!skipPermissionCheck.equalsIgnoreCase("true") && !skipPermissionCheck.equalsIgnoreCase("granted"))) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Map serviceInMap = FastMap.newInstance();
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

    public static GenericValue getSubContent(Delegator delegator, String contentId, String mapKey, String subContentId, GenericValue userLogin, List assocTypes, Timestamp fromDate) throws IOException {
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

    public static GenericValue getSubContentCache(Delegator delegator, String contentId, String mapKey, String subContentId, GenericValue userLogin, List assocTypes, Timestamp fromDate, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException {
        //GenericValue content = null;
        GenericValue view = null;
        if (UtilValidate.isEmpty(subContentId)) {
            view = getSubContentCache(delegator, contentId, mapKey, userLogin, assocTypes, fromDate, nullThruDatesOnly, contentAssocPredicateId);
        } else {
            view = getContentCache(delegator, subContentId);
        }
        return view;
    }

    public static GenericValue getSubContentCache(Delegator delegator, String contentId, String mapKey, GenericValue userLogin, List assocTypes, Timestamp fromDate, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException {
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

    public static GenericValue getContentCache(Delegator delegator, String contentId) throws GenericEntityException {

        GenericValue view = null;
        List lst = delegator.findByAndCache("ContentDataResourceView", UtilMisc.toMap("contentId", contentId));
            //if (Debug.infoOn()) Debug.logInfo("getContentCache, lst(2):" + lst, "");
        if (UtilValidate.isNotEmpty(lst)) {
            view = (GenericValue) lst.get(0);
        }
        return view;
    }

    public static GenericValue getCurrentContent(Delegator delegator, List trail, GenericValue userLogin, Map ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId)  throws GeneralException {

        String contentId = (String)ctx.get("contentId");
        String subContentId = (String)ctx.get("subContentId");
        String mapKey = (String)ctx.get("mapKey");
        Timestamp fromDate = UtilDateTime.nowTimestamp();
        List assocTypes = null;
        List passedGlobalNodeTrail = null;
        GenericValue currentContent = null;
        String viewContentId = null;
        if (UtilValidate.isNotEmpty(trail)) {
            passedGlobalNodeTrail = UtilMisc.makeListWritable(trail);
        } else {
            passedGlobalNodeTrail = FastList.newInstance();
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
            if (viewContentId != null && viewContentId.equals(subContentId)) {
                return currentContent;
            }
        } else {
            ctx.put("contentId", contentId);
            ctx.put("subContentId", null);
            if (viewContentId != null && viewContentId.equals(contentId)) {
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
        ctx.put("indent", Integer.valueOf(sz));
        //if (Debug.infoOn()) Debug.logInfo("getCurrentContent, currentContent:" + currentContent, "");
        return currentContent;
    }

    public static GenericValue getContentFromView(GenericValue view) {
        GenericValue content = null;
        if (view == null) {
            return content;
        }
        Delegator delegator = view.getDelegator();
        content = delegator.makeValue("Content");
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

    public static Map buildPickContext(Delegator delegator, String contentAssocTypeId, String assocContentId, String direction, GenericValue thisContent) throws GenericEntityException {

        Map ctx = FastMap.newInstance();
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
        List contentTypeAncestry = FastList.newInstance();
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

    public static void checkConditions(Delegator delegator, Map trailNode, Map contentAssoc, Map whenMap) {

        Map context = FastMap.newInstance();
        GenericValue content = (GenericValue)trailNode.get("value");
        String contentId = (String)trailNode.get("contentId");
        if (contentAssoc == null && content != null && (content.getEntityName().indexOf("Assoc") >= 0)) {
            contentAssoc = delegator.makeValue("ContentAssoc");
            try {
                // TODO: locale needs to be gotten correctly
                SimpleMapProcessor.runSimpleMapProcessor("component://content/script/org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocIn", content, contentAssoc, FastList.newInstance(), Locale.getDefault());
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
        List contentTypeAncestry = FastList.newInstance();
        try {
            getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
        } catch (GenericEntityException e) {
        }
        context.put("typeAncestry", contentTypeAncestry);
        boolean isReturnBefore = checkReturnWhen(context, (String)whenMap.get("returnBeforePickWhen"));
        trailNode.put("isReturnBefore", Boolean.valueOf(isReturnBefore));
        boolean isPick = checkWhen(context, (String)whenMap.get("pickWhen"));
        trailNode.put("isPick", Boolean.valueOf(isPick));
        boolean isFollow = checkWhen(context, (String)whenMap.get("followWhen"));
        trailNode.put("isFollow", Boolean.valueOf(isFollow));
        boolean isReturnAfter = checkReturnWhen(context, (String)whenMap.get("returnAfterPickWhen"));
        trailNode.put("isReturnAfter", Boolean.valueOf(isReturnAfter));
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
        if (UtilValidate.isNotEmpty(targetOperationString)) {
            List opsFromString = StringUtil.split(targetOperationString, "|");
            if (UtilValidate.isEmpty(targetOperationList)) {
                targetOperationList = FastList.newInstance();
            }
            targetOperationList.addAll(opsFromString);
        }
        if (UtilValidate.isEmpty(targetOperationList)) {
            targetOperationList = FastList.newInstance();
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
        if (UtilValidate.isNotEmpty(contentPurposeString)) {
            List purposesFromString = StringUtil.split(contentPurposeString, "|");
            if (UtilValidate.isEmpty(contentPurposeList)) {
                contentPurposeList = FastList.newInstance();
            }
            contentPurposeList.addAll(purposesFromString);
        }
        if (UtilValidate.isEmpty(contentPurposeList)) {
            contentPurposeList = FastList.newInstance();
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

    public static List getContentAssocViewList(Delegator delegator, String contentIdTo, String contentId, String contentAssocTypeId, String statusId, String privilegeEnumId) throws GenericEntityException {

        List exprListAnd = FastList.newInstance();

        if (UtilValidate.isNotEmpty(contentIdTo)) {
            EntityExpr expr = EntityCondition.makeCondition("caContentIdTo", EntityOperator.EQUALS, contentIdTo);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(contentId)) {
            EntityExpr expr = EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            EntityExpr expr = EntityCondition.makeCondition("caContentAssocTypeId", EntityOperator.EQUALS, contentAssocTypeId);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(statusId)) {
            EntityExpr expr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId);
            exprListAnd.add(expr);
        }

        if (UtilValidate.isNotEmpty(privilegeEnumId)) {
            EntityExpr expr = EntityCondition.makeCondition("privilegeEnumId", EntityOperator.EQUALS, privilegeEnumId);
            exprListAnd.add(expr);
        }

        EntityConditionList contentCondList = EntityCondition.makeCondition(exprListAnd, EntityOperator.AND);
        List contentList = delegator.findList("ContentAssocDataResourceViewFrom", contentCondList, null, null, null, false);
        List filteredList = EntityUtil.filterByDate(contentList, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
        return filteredList;
    }

    public static GenericValue getContentAssocViewFrom(Delegator delegator, String contentIdTo, String contentId, String contentAssocTypeId, String statusId, String privilegeEnumId) throws GenericEntityException {

        List filteredList = getContentAssocViewList(delegator, contentIdTo, contentId, contentAssocTypeId, statusId, privilegeEnumId);

        GenericValue val = null;
        if (filteredList.size() > 0) {
            val = (GenericValue)filteredList.get(0);
        }
        return val;
    }

    public static Map<String, Object> makeNode(GenericValue thisContent) {
        Map thisNode = null;
        if (thisContent == null)
            return thisNode;

        thisNode = FastMap.newInstance();
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
        } catch (Exception e) {
            // This ignores the case when thisContent does not have ContentAssoc values
        }
        return thisNode;
    }


    public static String nodeTrailToCsv(List nodeTrail) {

        if (nodeTrail == null)
            return "";
        StringBuilder csv = new StringBuilder();
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

    public static List csvToList(String csv, Delegator delegator) {

        List outList = FastList.newInstance();
        List contentIdList = StringUtil.split(csv, ",");
        GenericValue content = null;
        String contentId = null;
        String contentName = null;
        List values = null;
        Iterator it = contentIdList.iterator();
        while (it.hasNext()) {
            contentId = (String)it.next();
            try {
                content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                return FastList.newInstance();
            }
            contentName = (String)content.get("contentName");
            values = FastList.newInstance();
            values.add(contentId);
            values.add(contentName);
            outList.add(values);
        }
        return outList;
    }

    public static List csvToContentList(String csv, Delegator delegator) {

        List trail = FastList.newInstance();
        if (csv == null)
            return trail;

        List outList = FastList.newInstance();
        List contentIdList = StringUtil.split(csv, ",");
        GenericValue content = null;
        String contentId = null;
        Iterator it = contentIdList.iterator();
        while (it.hasNext()) {
            contentId = (String)it.next();
            try {
                content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                return FastList.newInstance();
            }
            trail.add(content);
        }
        return trail;
    }

    public static List csvToTrail(String csv, Delegator delegator) {

        List trail = FastList.newInstance();
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

    public static String getMimeTypeId(Delegator delegator, GenericValue view, Map ctx) {
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
    public static String determineMimeType(Delegator delegator,
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

    public static String logMap(String lbl, Map map, int indentLevel) {
        StringBuilder indent = new StringBuilder();
        for (int i=0; i<indentLevel; i++) {
            indent.append(' ');
        }
        StringBuilder sb = new StringBuilder();
        return logMap(new StringBuilder(), lbl, map, indent).toString();
    }

    public static StringBuilder logMap(StringBuilder s, String lbl, Map map, StringBuilder indent) {
        String sep = ":";
        String eol = "\n";
        String spc = "";
        if (lbl != null) s.append(lbl);
        s.append("=").append(indent).append("==>").append(eol);
        Set keySet = map.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            if ("request response session".indexOf(key) < 0) {
                Object obj = map.get(key);
                s.append(spc).append(key).append(sep);
                if (obj instanceof GenericValue) {
                    GenericValue gv = (GenericValue)obj;
                    GenericPK pk = gv.getPrimaryKey();
                    indent.append(' ');
                    logMap(s, "GMAP[" + key + " name:" + pk.getEntityName()+ "]", pk, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj instanceof List) {
                    indent.append(' ');
                    logList(s, "LIST[" + ((List)obj).size() + "]", (List)obj, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj instanceof Map) {
                    indent.append(' ');
                    logMap(s, "MAP[" + key + "]", (Map)obj, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj != null) {
                    s.append(obj).append(sep).append(obj.getClass()).append(eol);
                } else {
                    s.append(eol);
                }
            }
        }
        return s.append(eol).append(eol);
    }

    public static String logList(String lbl, List lst, int indentLevel) {
        StringBuilder indent = new StringBuilder();
        for (int i=0; i<indentLevel; i++) {
            indent.append(' ');
        }
        StringBuilder sb = new StringBuilder();
        return logList(new StringBuilder(), lbl, lst, indent).toString();
    }

    public static StringBuilder logList(StringBuilder s, String lbl, List lst, StringBuilder indent) {

        String sep = ":";
        String eol = "\n";
        String spc = "";
        if (lst == null)
            return s;
        int sz = lst.size();
        if (lbl != null) s.append(lbl);
        s.append("=").append(indent).append("==> sz:").append(sz).append(eol);
        Iterator it = lst.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
                s.append(spc);
                if (obj instanceof GenericValue) {
                    GenericValue gv = (GenericValue)obj;
                    GenericPK pk = gv.getPrimaryKey();
                    indent.append(' ');
                    logMap(s, "MAP[name:" + pk.getEntityName() + "]", pk, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj instanceof List) {
                    indent.append(' ');
                    logList(s, "LIST[" + ((List)obj).size() + "]", (List)obj, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj instanceof Map) {
                    indent.append(' ');
                    logMap(s, "MAP[]", (Map)obj, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj != null) {
                    s.append(obj).append(sep).append(obj.getClass()).append(eol);
                } else {
                    s.append(eol);
                }
        }
        return s.append(eol).append(eol);
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

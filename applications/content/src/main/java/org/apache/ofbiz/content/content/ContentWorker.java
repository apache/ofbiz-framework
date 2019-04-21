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
package org.apache.ofbiz.content.content;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GroovyUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.content.ContentManagementWorker;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMapProcessor;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.codehaus.groovy.control.CompilationFailedException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import freemarker.ext.dom.NodeModel;

/**
 * ContentWorker Class
 */
public class ContentWorker implements org.apache.ofbiz.widget.content.ContentWorkerInterface {

    public static final String module = ContentWorker.class.getName();
    static final UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder("html");

    public ContentWorker() { }

    @Override
    public GenericValue getWebSitePublishPointExt(Delegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException {
        return ContentManagementWorker.getWebSitePublishPoint(delegator, contentId, ignoreCache);
    }

    @Override
    public GenericValue getCurrentContentExt(Delegator delegator, List<Map<String, ? extends Object>> trail, GenericValue userLogin, Map<String, Object> ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GeneralException {
        return getCurrentContent(delegator, trail, userLogin, ctx, nullThruDatesOnly, contentAssocPredicateId);
    }

    @Override
    public String getMimeTypeIdExt(Delegator delegator, GenericValue view, Map<String, Object> ctx) {
        return getMimeTypeId(delegator, view, ctx);
    }

    // new rendering methods
    @Override
    public void renderContentAsTextExt(LocalDispatcher dispatcher, String contentId, Appendable out, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        renderContentAsText(dispatcher, contentId, out, templateContext, locale, mimeTypeId, null, null, cache);
    }

    @Override
    public void renderSubContentAsTextExt(LocalDispatcher dispatcher, String contentId, Appendable out, String mapKey, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        renderSubContentAsText(dispatcher, contentId, out, mapKey, templateContext, locale, mimeTypeId, cache);
    }

    @Override
    public String renderSubContentAsTextExt(LocalDispatcher dispatcher, String contentId, String mapKey, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderSubContentAsText(dispatcher, contentId, mapKey, templateContext, locale, mimeTypeId, cache);
    }

    @Override
    public String renderContentAsTextExt(LocalDispatcher dispatcher, String contentId, Map<String, Object> templateContext, Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderContentAsText(dispatcher, contentId, templateContext, locale, mimeTypeId, cache);
    }

    // -------------------------------------
    // Content rendering methods
    // -------------------------------------
    public static GenericValue findContentForRendering(Delegator delegator, String contentId, Locale locale, String partyId, String roleTypeId, boolean cache) throws GeneralException, IOException {
        if (UtilValidate.isEmpty(contentId)) {
            Debug.logError("No content ID found.", module);
            return null;
        }
        GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache(cache).queryOne();
        if (content == null) {
            throw new GeneralException("No content found for content ID [" + contentId + "]");
        }

        // if the content is a PUBLISH_POINT and the data resource is not defined; get the related content
        if ("WEB_SITE_PUB_PT".equals(content.get("contentTypeId")) && content.get("dataResourceId") == null) {
            GenericValue relContent = EntityQuery.use(delegator)
                    .from("ContentAssocDataResourceViewTo")
                    .where("contentIdStart", content.get("contentId"),
                            "statusId","CTNT_PUBLISHED",
                            "caContentAssocTypeId", "PUBLISH_LINK")
                    .orderBy("caFromDate")
                    .filterByDate("caFromDate", "caThruDate")
                    .cache().queryFirst();

            if (relContent != null) {
                content = relContent;
            }

            if (relContent == null) {
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
                alternateViews = content.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_ROLE"), UtilMisc.toList("-caFromDate"), true);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding alternate content: " + e.toString(), module);
            }

            alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
            for (GenericValue thisView : alternateViews) {
                GenericValue altContentRole = EntityUtil.getFirst(EntityUtil.filterByDate(thisView.getRelated("ContentRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId), null, true)));
                GenericValue altContent = null;
                if (UtilValidate.isNotEmpty(altContentRole)) {
                    altContent = altContentRole.getRelatedOne("Content", true);
                    if (altContent != null) {
                        content = altContent;
                    }
                }
            }
        }
        return content;
    }

    public static void renderContentAsText(LocalDispatcher dispatcher, GenericValue content, Appendable out, Map<String,Object>templateContext,
            Locale locale, String mimeTypeId, boolean cache, List<GenericValue> webAnalytics) throws GeneralException, IOException {
        // if the content has a service attached run the service

        Delegator delegator = dispatcher.getDelegator();
        String serviceName = content.getString("serviceName"); //Kept for backward compatibility
        GenericValue custMethod = null;
        if (UtilValidate.isNotEmpty(content.getString("customMethodId"))) {
            custMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", content.get("customMethodId")).cache().queryOne();
        }
        if (custMethod != null) serviceName = custMethod.getString("customMethodName");
        if (UtilValidate.isNotEmpty(serviceName)) {
            DispatchContext dctx = dispatcher.getDispatchContext();
            ModelService service = dctx.getModelService(serviceName);
            if (service != null) {
                //put all requestParameters into templateContext to use them as IN service parameters
                Map<String,Object> tempTemplateContext = new HashMap<>();
                tempTemplateContext.putAll(UtilGenerics.<String,Object>checkMap(templateContext.get("requestParameters")));
                tempTemplateContext.putAll(templateContext);
                Map<String,Object> serviceCtx = service.makeValid(tempTemplateContext, ModelService.IN_PARAM);
                Map<String,Object> serviceRes;
                try {
                    serviceRes = dispatcher.runSync(serviceName, serviceCtx);
                    if (ServiceUtil.isError(serviceRes)) {
                        String errorMessage = ServiceUtil.getErrorMessage(serviceRes);
                        Debug.logError(errorMessage, module);
                        throw new GeneralException(errorMessage);
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    throw e;
                }
                templateContext.putAll(serviceRes);
            }
        }

        String contentId = content.getString("contentId");

        if (templateContext == null) {
            templateContext = new HashMap<>();
        }

        // create the content facade
        ContentMapFacade facade = new ContentMapFacade(dispatcher, content, templateContext, locale, mimeTypeId, cache);
        // If this content is decorating something then tell the facade about it in order to maintain the chain of decoration
        ContentMapFacade decoratedContent = (ContentMapFacade) templateContext.get("decoratedContent");
        if (decoratedContent != null) {
            facade.setDecoratedContent(decoratedContent);
        }

        // look for a content decorator
        String contentDecoratorId = content.getString("decoratorContentId");
        // Check that the decoratorContent is not the same as the current content
        if (contentId.equals(contentDecoratorId)) {
            Debug.logError("[" + contentId + "] decoratorContentId is the same as contentId, ignoring.", module);
            contentDecoratorId = null;
        }
        // check to see if the decorator has already been run
        boolean isDecorated = Boolean.TRUE.equals(templateContext.get("_IS_DECORATED_"));
        if (!isDecorated && UtilValidate.isNotEmpty(contentDecoratorId)) {
            // if there is a decorator content; do not render this content;
            // instead render the decorator
            GenericValue decorator = EntityQuery.use(delegator).from("Content").where("contentId", contentDecoratorId).cache(cache).queryOne();
            if (decorator == null) {
                throw new GeneralException("No decorator content found for decorator contentId [" + contentDecoratorId + "]");
            }

            // render the decorator
            ContentMapFacade decFacade = new ContentMapFacade(dispatcher, decorator, templateContext, locale, mimeTypeId, cache);
            decFacade.setDecoratedContent(facade);
            facade.setIsDecorated(true);
            templateContext.put("decoratedContent", facade); // decorated content
            templateContext.put("thisContent", decFacade); // decorator content
            ContentWorker.renderContentAsText(dispatcher, contentDecoratorId, out, templateContext, locale, mimeTypeId, null, null, cache);
        } else {
            // get the data resource info
            String templateDataResourceId = content.getString("templateDataResourceId");
            String dataResourceId = content.getString("dataResourceId");
            if (UtilValidate.isEmpty(dataResourceId)) {
                Debug.logError("No dataResourceId found for contentId: " + content.getString("contentId"), module);
                return;
            }

            // set this content facade in the context
            templateContext.put("thisContent", facade);
            templateContext.put("contentId", contentId);

            // now if no template; just render the data
            if (UtilValidate.isEmpty(templateDataResourceId) || templateContext.containsKey("ignoreTemplate")) {
                if (UtilValidate.isEmpty(contentId)) {
                    Debug.logError("No content ID found.", module);
                    return;
                }
                
                if (UtilValidate.isNotEmpty(webAnalytics)) {
                    DataResourceWorker.renderDataResourceAsText(dispatcher, delegator, dataResourceId, out, templateContext, locale, mimeTypeId, cache, webAnalytics);
                } else {
                    DataResourceWorker.renderDataResourceAsText(dispatcher, dataResourceId, out, templateContext, locale, mimeTypeId, cache);
                }

            // there is a template; render the data and then the template
            } else {
                Writer dataWriter = new StringWriter();
                DataResourceWorker.renderDataResourceAsText(dispatcher, dataResourceId, dataWriter, templateContext, locale, mimeTypeId, cache);

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
                        GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
                        GenericValue templateDataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", templateDataResourceId).cache().queryOne();
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
                DataResourceWorker.renderDataResourceAsText(dispatcher, templateDataResourceId, out, templateContext, locale, mimeTypeId, cache);
            }
        }
    }

    public static String renderContentAsText(LocalDispatcher dispatcher, String contentId, Map<String, Object> templateContext, Locale locale,
            String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderContentAsText(dispatcher, contentId, writer, templateContext, locale, mimeTypeId, null, null, cache);
        GenericValue content = EntityQuery.use(dispatcher.getDelegator()).from("Content").where("contentId", contentId).queryOne();
        String contentTypeId = content.getString("contentTypeId");
        String rendered = writer.toString();
        // According to https://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet#XSS_Prevention_Rules_Summary,
        // normally head is protected by X-XSS-Protection Response Header by default.
        if (rendered.contains("<script>")
                || rendered.contains("<!--")
                || rendered.contains("<div")
                || rendered.contains("<style>")
                || rendered.contains("<span")
                || rendered.contains("<input")
                || rendered.contains("<iframe")
                || rendered.contains("<a")) {
            rendered = encoder.sanitize(rendered, contentTypeId);
        }
        return rendered; 
    }

    public static String renderContentAsText(LocalDispatcher dispatcher, String contentId, Appendable out, Map<String, Object> templateContext,
            Locale locale, String mimeTypeId, String partyId, String roleTypeId, boolean cache, List<GenericValue> webAnalytics) throws GeneralException, IOException {
        Delegator delegator = dispatcher.getDelegator();
        GenericValue content = ContentWorker.findContentForRendering(delegator, contentId, locale, partyId, roleTypeId, cache);
        ContentWorker.renderContentAsText(dispatcher, content, out, templateContext, locale, mimeTypeId, cache, webAnalytics);
        return out.toString();
    }

    public static void renderContentAsText(LocalDispatcher dispatcher, String contentId, Appendable out, Map<String, Object> templateContext,
            Locale locale, String mimeTypeId, String partyId, String roleTypeId, boolean cache) throws GeneralException, IOException {
        Delegator delegator = dispatcher.getDelegator();
        GenericValue content = ContentWorker.findContentForRendering(delegator, contentId, locale, partyId, roleTypeId, cache);
        ContentWorker.renderContentAsText(dispatcher, content, out, templateContext, locale, mimeTypeId, cache, null);
    }

    public static String renderSubContentAsText(LocalDispatcher dispatcher, String contentId, String mapKey, Map<String, Object> templateContext, Locale locale,
            String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderSubContentAsText(dispatcher, contentId, writer, mapKey, templateContext, locale, mimeTypeId, cache);
        return writer.toString();
    }

    public static void renderSubContentAsText(LocalDispatcher dispatcher, String contentId, Appendable out, String mapKey, Map<String,Object> templateContext,
            Locale locale, String mimeTypeId, boolean cache) throws GeneralException, IOException {
        Delegator delegator = dispatcher.getDelegator();

        // find the sub-content with matching mapKey
        List<EntityCondition> exprs = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId));
        if (UtilValidate.isNotEmpty(mapKey)) {
                exprs.add(EntityCondition.makeCondition("mapKey", EntityOperator.EQUALS, mapKey));
        }

        GenericValue subContent = EntityQuery.use(delegator).from("ContentAssoc")
                .where(exprs)
                .orderBy("-fromDate").cache(cache).filterByDate().queryFirst();

        if (subContent == null) {
            Debug.logWarning("No sub-content found with map-key [" + mapKey + "] for content [" + contentId + "]", module);
        } else {
            String subContentId = subContent.getString("contentIdTo");
            templateContext.put("mapKey", mapKey);
            renderContentAsText(dispatcher, subContentId, out, templateContext, locale, mimeTypeId, null, null, cache);
        }
    }

    public static GenericValue findAlternateLocaleContent(Delegator delegator, GenericValue view, Locale locale) {
        GenericValue contentAssocDataResourceViewFrom = null;
        if (locale == null) {
            return view;
        }

        String localeStr = locale.toString();
        boolean isTwoLetterLocale = localeStr.length() == 2;

        List<GenericValue> alternateViews = null;
        try {
            alternateViews = view.getRelated("ContentAssocDataResourceViewTo", UtilMisc.toMap("caContentAssocTypeId", "ALTERNATE_LOCALE"), UtilMisc.toList("-caFromDate"), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding alternate locale content: " + e.toString(), module);
            return view;
        }

        alternateViews = EntityUtil.filterByDate(alternateViews, UtilDateTime.nowTimestamp(), "caFromDate", "caThruDate", true);
        // also check the given view for a matching locale
        alternateViews.add(0, view);

        for (GenericValue thisView : alternateViews) {
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

        if (contentAssocDataResourceViewFrom == null) {
            // no content matching the given locale found.
            Locale fallbackLocale = UtilProperties.getFallbackLocale();
            contentAssocDataResourceViewFrom = locale.equals(fallbackLocale) ? view
                    // only search for a content with the fallbackLocale if it is different to the given locale
                    : findAlternateLocaleContent(delegator, view, fallbackLocale);
        }

        return contentAssocDataResourceViewFrom;
    }

    public static void traverse(Delegator delegator, GenericValue content, Timestamp fromDate, Timestamp thruDate, Map<String, Object> whenMap, int depthIdx, Map<String, Object> masterNode, String contentAssocTypeId, List<GenericValue> pickList, String direction) {
        String contentTypeId = null;
        String contentId = null;
        try {
            if (contentAssocTypeId == null) {
                contentAssocTypeId = "";
            }
            contentId = (String) content.get("contentId");
            contentTypeId = (String) content.get("contentTypeId");
            List<GenericValue> topicList = content.getRelated("ToContentAssoc", UtilMisc.toMap("contentAssocTypeId", "TOPIC"), null, false);
            List<String> topics = new LinkedList<>();
            for (int i = 0; i < topicList.size(); i++) {
                GenericValue assoc = topicList.get(i);
                topics.add(assoc.getString("contentId"));
            }
            List<GenericValue> keywordList = content.getRelated("ToContentAssoc", UtilMisc.toMap("contentAssocTypeId", "KEYWORD"), null, false);
            List<String> keywords = new LinkedList<>();
            for (int i = 0; i < keywordList.size(); i++) {
                GenericValue assoc = keywordList.get(i);
                keywords.add(assoc.getString("contentId"));
            }
            List<GenericValue> purposeValueList = content.getRelated("ContentPurpose", null, null, true);
            List<String> purposes = new LinkedList<>();
            for (int i = 0; i < purposeValueList.size(); i++) {
                GenericValue purposeValue = purposeValueList.get(i);
                purposes.add(purposeValue.getString("contentPurposeTypeId"));
            }
            List<String> contentTypeAncestry = new LinkedList<>();
            getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);

            Map<String, Object> context = new HashMap<>();
            context.put("content", content);
            context.put("contentAssocTypeId", contentAssocTypeId);
            context.put("purposes", purposes);
            context.put("topics", topics);
            context.put("keywords", keywords);
            context.put("typeAncestry", contentTypeAncestry);
            boolean isPick = checkWhen(context, (String) whenMap.get("pickWhen"), true);
            boolean isReturnBefore = checkWhen(context, (String) whenMap.get("returnBeforePickWhen"), false);
            Map<String, Object> thisNode = null;
            if (isPick || !isReturnBefore) {
                thisNode = new HashMap<>();
                thisNode.put("contentId", contentId);
                thisNode.put("contentTypeId", contentTypeId);
                thisNode.put("contentAssocTypeId", contentAssocTypeId);
                List<Map<String, Object>> kids = UtilGenerics.checkList(masterNode.get("kids"));
                if (kids == null) {
                    kids = new LinkedList<>();
                    masterNode.put("kids", kids);
                }
                kids.add(thisNode);
            }
            if (isPick) {
                pickList.add(content);
                thisNode.put("value", content);
            }
            boolean isReturnAfter = checkWhen(context, (String) whenMap.get("returnAfterPickWhen"), false);
            if (!isReturnAfter) {
                List<String> assocTypes = new LinkedList<>();
                List<GenericValue> relatedAssocs = getContentAssocsWithId(delegator, contentId, fromDate, thruDate, direction, assocTypes);
                Map<String, Object> assocContext = new HashMap<>();
                assocContext.put("related", relatedAssocs);
                for (GenericValue assocValue : relatedAssocs) {
                    contentAssocTypeId = (String) assocValue.get("contentAssocTypeId");
                    assocContext.put("contentAssocTypeId", contentAssocTypeId);
                    assocContext.put("parentContent", content);
                    String assocRelation = null;
                    // This needs to be the opposite
                    String relatedDirection = null;
                    if (direction != null && "From".equalsIgnoreCase(direction)) {
                        assocContext.put("contentIdFrom", assocValue.get("contentId"));
                        assocRelation = "ToContent";
                        relatedDirection = "From";
                    } else {
                        assocContext.put("contentIdTo", assocValue.get("contentId"));
                        assocRelation = "FromContent";
                        relatedDirection = "To";
                    }

                    boolean isFollow = checkWhen(assocContext, (String) whenMap.get("followWhen"), true);
                    if (isFollow) {
                        GenericValue thisContent = assocValue.getRelatedOne(assocRelation, false);
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
        List<Map <String, Object>> nodeTrail = UtilGenerics.checkList(ctx.get("nodeTrail"));
        ContentWorker.traceNodeTrail("11",nodeTrail);
        int sz = nodeTrail.size();
        if (sz == 0) {
            return false;
        }

        Map<String, Object> currentNode = nodeTrail.get(sz - 1);
        Boolean isReturnAfter = (Boolean)currentNode.get("isReturnAfter");
        if (isReturnAfter != null && isReturnAfter) {
            return false;
        }

        List<Map <String, Object>> kids = UtilGenerics.checkList(currentNode.get("kids"));
        if (UtilValidate.isNotEmpty(kids)) {
            int idx = 0;
            while (idx < kids.size()) {
                currentNode = kids.get(idx);
                ContentWorker.traceNodeTrail("12",nodeTrail);
                Boolean isPick = (Boolean)currentNode.get("isPick");

                if (isPick != null && isPick) {
                    nodeTrail.add(currentNode);
                    inProgress = true;
                    selectKids(currentNode, ctx);
                    ContentWorker.traceNodeTrail("14",nodeTrail);
                    break;
                } else {
                    Boolean isFollow = (Boolean)currentNode.get("isFollow");
                    if (isFollow != null && isFollow) {
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
                currentNode = nodeTrail.remove(--sz);
                ContentWorker.traceNodeTrail("15",nodeTrail);
                Map<String, Object> parentNode = nodeTrail.get(sz - 1);
                kids = UtilGenerics.checkList(parentNode.get("kids"));
                if (kids == null) {
                    continue;
                }

                int idx = kids.indexOf(currentNode);
                while (idx < (kids.size() - 1)) {
                    currentNode = kids.get(idx + 1);
                    Boolean isFollow = (Boolean)currentNode.get("isFollow");
                    if (isFollow == null || !isFollow) {
                        idx++;
                        continue;
                    }
                    nodeTrail.add(currentNode);
                    ContentWorker.traceNodeTrail("16",nodeTrail);
                    Boolean isPick = (Boolean)currentNode.get("isPick");
                    if (isPick == null || !isPick) {
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

    public static List<Object> getPurposes(GenericValue content) {
        List<Object> purposes = new LinkedList<>();
        try {
            List<GenericValue> purposeValueList = content.getRelated("ContentPurpose", null, null, true);
            for (int i = 0; i < purposeValueList.size(); i++) {
                GenericValue purposeValue = purposeValueList.get(i);
                purposes.add(purposeValue.get("contentPurposeTypeId"));
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
        return purposes;
    }

    public static List<Object> getSections(GenericValue content) {
        List<Object> sections = new LinkedList<>();
        try {
            List<GenericValue> sectionValueList = content.getRelated("FromContentAssoc", null, null, true);
            for (int i = 0; i < sectionValueList.size(); i++) {
                GenericValue sectionValue = sectionValueList.get(i);
                String contentAssocPredicateId = (String)sectionValue.get("contentAssocPredicateId");
                if (contentAssocPredicateId != null && "categorizes".equals(contentAssocPredicateId)) {
                    sections.add(sectionValue.get("contentIdTo"));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
        return sections;
    }

    public static List<Object> getTopics(GenericValue content) {
        List<Object> topics = new LinkedList<>();
        try {
            List<GenericValue> topicValueList = content.getRelated("FromContentAssoc", null, null, true);
            for (int i = 0; i < topicValueList.size(); i++) {
                GenericValue topicValue = topicValueList.get(i);
                String contentAssocPredicateId = (String)topicValue.get("contentAssocPredicateId");
                if (contentAssocPredicateId != null && "topifies".equals(contentAssocPredicateId))
                    topics.add(topicValue.get("contentIdTo"));
            }
        } catch (GenericEntityException e) {
            Debug.logError("Entity Error:" + e.getMessage(), null);
        }
        return topics;
    }

    public static void selectKids(Map<String, Object> currentNode, Map<String, Object> ctx) {
        Delegator delegator = (Delegator) ctx.get("delegator");
        GenericValue parentContent = (GenericValue) currentNode.get("value");
        String contentAssocTypeId = (String) ctx.get("contentAssocTypeId");
        String contentTypeId = (String) ctx.get("contentTypeId");
        String mapKey = (String) ctx.get("mapKey");
        String parentContentId = (String) parentContent.get("contentId");
        Map<String, Object> whenMap = UtilGenerics.checkMap(ctx.get("whenMap"));
        List<Map<String, Object>> kids = new LinkedList<>();
        currentNode.put("kids", kids);
        String direction = (String) ctx.get("direction");
        if (UtilValidate.isEmpty(direction)) {
            direction = "From";
        }

        List<String> assocTypeList = StringUtil.split(contentAssocTypeId, " ");
        List<String> contentTypeList = StringUtil.split(contentTypeId, " ");
        String contentAssocPredicateId = null;
        Boolean nullThruDatesOnly = Boolean.TRUE;
        Map<String, Object> results = null;
        try {
            results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, parentContentId, mapKey, direction, null, null, assocTypeList, contentTypeList, nullThruDatesOnly, contentAssocPredicateId, null);
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        } catch (MiniLangException e2) {
            throw new RuntimeException(e2.getMessage());
        }
        List<GenericValue> relatedViews = UtilGenerics.checkList(results.get("entityList"));
        for (GenericValue assocValue : relatedViews) {
            Map<String, Object> thisNode = ContentWorker.makeNode(assocValue);
            checkConditions(delegator, thisNode, null, whenMap);
            boolean isPick = booleanDataType(thisNode.get("isPick"));
            kids.add(thisNode);
            if (isPick) {
                    Integer count = (Integer) currentNode.get("count");
                    if (count == null) {
                        count = 1;
                    } else {
                        count = count + 1;
                    }
                    currentNode.put("count", count);
            }
        }
    }

    /** Returns a boolean, result of whenStr evaluation with context.
     * If whenStr is empty return defaultReturn.
     * @param context A <code>Map</code> containing initial variables
     * @param whenStr A <code>String</code> condition expression
     * @param defaultReturn A <code>boolean</code> default return value
     * @return A <code>boolan</code> result of evaluation
     */
    public static boolean checkWhen(Map<String, Object> context, String whenStr, boolean defaultReturn) {
        boolean isWhen = defaultReturn;
        if (UtilValidate.isNotEmpty(whenStr)) {
            FlexibleStringExpander fse = FlexibleStringExpander.getInstance(whenStr);
            String newWhen = fse.expandString(context);
            try {
                Object retVal = GroovyUtil.eval(newWhen,context);
                // retVal should be a Boolean, if not something weird is up...
                if (retVal instanceof Boolean) {
                    Boolean boolVal = (Boolean) retVal;
                    isWhen = boolVal;
                } else {
                    throw new IllegalArgumentException("Return value from use-when condition eval was not a Boolean: "
                            + (retVal != null ? retVal.getClass().getName() : "null") + " [" + retVal + "]");
                }
            } catch (CompilationFailedException e) {
                Debug.logError("Error in evaluating :" + whenStr + " : " + e.getMessage(), null);
                throw new RuntimeException(e.getMessage());
            }
        }
        return isWhen;
    }

    public static List<GenericValue> getAssociatedContent(GenericValue currentContent, String linkDir, List<String> assocTypes, List<String> contentTypes, String fromDate, String thruDate) throws GenericEntityException {
        Delegator delegator = currentContent.getDelegator();
        List<GenericValue> assocList = getAssociations(currentContent, linkDir, assocTypes, fromDate, thruDate);
        if (UtilValidate.isEmpty(assocList)) {
            return assocList;
        }
        if (Debug.infoOn()) {
            Debug.logInfo("assocList:" + assocList.size() + " contentId:" + currentContent.getString("contentId"), "");
        }

        List<GenericValue> contentList = new LinkedList<>();
        String contentIdName = "contentId";
        if (linkDir != null && "TO".equalsIgnoreCase(linkDir)) {
            contentIdName = contentIdName.concat("To");
        }
        GenericValue content = null;
        String contentTypeId = null;
        for (GenericValue assoc : assocList) {
            String contentId = (String) assoc.get(contentIdName);
            if (Debug.infoOn()) Debug.logInfo("contentId:" + contentId, "");
            content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            if (UtilValidate.isNotEmpty(contentTypes)) {
                contentTypeId = content.getString("contentTypeId");
                if (contentTypes.contains(contentTypeId)) {
                    contentList.add(content);
                }
            } else {
                contentList.add(content);
            }
        }
        if (Debug.infoOn()) {
            Debug.logInfo("contentList:" + contentList.size() , "");
        }
        return contentList;
    }

    public static List<GenericValue> getAssociatedContentView(GenericValue currentContent, String linkDir, List<String> assocTypes, List<String> contentTypes, String fromDate, String thruDate) throws GenericEntityException {
        List<EntityExpr> exprListAnd = new LinkedList<>();

        String origContentId = (String) currentContent.get("contentId");
        String contentIdName = "contentId";
        String contentAssocViewName = "contentAssocView";
        if (linkDir != null && "TO".equalsIgnoreCase(linkDir)) {
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
        Delegator delegator = currentContent.getDelegator();
        return EntityQuery.use(delegator).from(contentAssocViewName).where(exprListAnd).queryList();
    }

    public static List<GenericValue> getAssociations(GenericValue currentContent, String linkDir, List<String> assocTypes, String strFromDate, String strThruDate) throws GenericEntityException {
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
        return assocs;
    }

    public static List<GenericValue> getContentAssocsWithId(Delegator delegator, String contentId, Timestamp fromDate, Timestamp thruDate, String direction, List<String> assocTypes) throws GenericEntityException {
        List<EntityCondition> exprList = new LinkedList<>();
        EntityExpr joinExpr = null;
        if (direction != null && "From".equalsIgnoreCase(direction)) {
            joinExpr = EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentId);
        } else {
            joinExpr = EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId);
        }
        exprList.add(joinExpr);
        if (UtilValidate.isNotEmpty(assocTypes)) {
            exprList.add(EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, assocTypes));
        }
        if (fromDate != null) {
            EntityExpr fromExpr = EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate);
            exprList.add(fromExpr);
        }
        if (thruDate != null) {
            List<EntityExpr> thruList = new LinkedList<>();

            EntityExpr thruExpr = EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN, thruDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList<EntityExpr> thruExprList = EntityCondition.makeCondition(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else if (fromDate != null) {
            List<EntityExpr> thruList = new LinkedList<>();

            EntityExpr thruExpr = EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, fromDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList<EntityExpr> thruExprList = EntityCondition.makeCondition(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else {
            EntityExpr thruExpr2 = EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null);
            exprList.add(thruExpr2);
        }
        
        return EntityQuery.use(delegator).from("ContentAssoc").where(exprList).orderBy("-fromDate").queryList();
    }

    public static void getContentTypeAncestry(Delegator delegator, String contentTypeId, List<String> contentTypes) throws GenericEntityException {
        contentTypes.add(contentTypeId);
        GenericValue contentTypeValue = EntityQuery.use(delegator).from("ContentType").where("contentTypeId", contentTypeId).queryOne();
        if (contentTypeValue == null)
            return;
        String parentTypeId = (String) contentTypeValue.get("parentTypeId");
        if (parentTypeId != null) {
            getContentTypeAncestry(delegator, parentTypeId, contentTypes);
        }
    }

    public static void getContentAncestry(Delegator delegator, String contentId, String contentAssocTypeId, String direction, List<GenericValue> contentAncestorList) throws GenericEntityException {
        String contentIdField = null;
        String contentIdOtherField = null;
        if (direction != null && "to".equalsIgnoreCase(direction)) {
            contentIdField = "contentId";
            contentIdOtherField = "contentIdTo";
        } else {
            contentIdField = "contentIdTo";
            contentIdOtherField = "contentId";
        }

        if (Debug.infoOn()) {
            Debug.logInfo("getContentAncestry, contentId:" + contentId, "");
            Debug.logInfo("getContentAncestry, contentAssocTypeId:" + contentAssocTypeId, "");
        }
        Map<String, Object> andMap = null;
        if (UtilValidate.isEmpty(contentAssocTypeId)) {
            andMap = UtilMisc.<String, Object>toMap(contentIdField, contentId);
        } else {
            andMap = UtilMisc.<String, Object>toMap(contentIdField, contentId, "contentAssocTypeId", contentAssocTypeId);
        }
        try {
            GenericValue contentAssoc = EntityQuery.use(delegator).from("ContentAssoc").where(andMap).cache().filterByDate().queryFirst();
            if (contentAssoc != null) {
                getContentAncestry(delegator, contentAssoc.getString(contentIdOtherField), contentAssocTypeId, direction, contentAncestorList);
                contentAncestorList.add(contentAssoc);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e,module);
            return;
        }
    }

    public static void getContentAncestryAll(Delegator delegator, String contentId, String passedContentTypeId, String direction, List<String> contentAncestorList) {
        String contentIdField = null;
        String contentIdOtherField = null;
        if (direction != null && "to".equalsIgnoreCase(direction)) {
            contentIdField = "contentId";
            contentIdOtherField = "contentIdTo";
        } else {
            contentIdField = "contentIdTo";
            contentIdOtherField = "contentId";
        }

        if (Debug.infoOn()) Debug.logInfo("getContentAncestry, contentId:" + contentId, "");
        try {
            List<GenericValue> contentAssocs = EntityQuery.use(delegator).from("ContentAssoc")
                    .where(contentIdField, contentId)
                    .cache().filterByDate().queryList();
            for (GenericValue contentAssoc : contentAssocs) {
                String contentIdOther = contentAssoc.getString(contentIdOtherField);
                if (!contentAncestorList.contains(contentIdOther)) {
                    getContentAncestryAll(delegator, contentIdOther, passedContentTypeId, direction, contentAncestorList);
                    if (!contentAncestorList.contains(contentIdOther)) {
                        GenericValue contentTo = EntityQuery.use(delegator).from("Content").where("contentId", contentIdOther).cache().queryOne();

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
         List<GenericValue> contentAncestorList = new LinkedList<>();
         List<Map<String, Object>> nodeTrail = new LinkedList<>();
         getContentAncestry(delegator, contentId, contentAssocTypeId, direction, contentAncestorList);
         for (GenericValue value : contentAncestorList) {
             Map<String, Object> thisNode = ContentWorker.makeNode(value);
             nodeTrail.add(thisNode);
         }
         return nodeTrail;
    }

    public static String getContentAncestryNodeTrailCsv(Delegator delegator, String contentId, String contentAssocTypeId, String direction) throws GenericEntityException {
         List<GenericValue> contentAncestorList = new LinkedList<>();
         getContentAncestry(delegator, contentId, contentAssocTypeId, direction, contentAncestorList);
         String csv = StringUtil.join(contentAncestorList, ",");
         return csv;
    }

    public static void getContentAncestryValues(Delegator delegator, String contentId, String contentAssocTypeId, String direction, List<GenericValue> contentAncestorList) throws GenericEntityException {
        String contentIdField = null;
        String contentIdOtherField = null;
        if (direction != null && "to".equalsIgnoreCase(direction)) {
            contentIdField = "contentId";
            contentIdOtherField = "contentIdTo";
        } else {
            contentIdField = "contentIdTo";
            contentIdOtherField = "contentId";
        }

        try {
            GenericValue contentAssoc = EntityQuery.use(delegator).from("ContentAssoc")
                    .where(contentIdField, contentId, "contentAssocTypeId", contentAssocTypeId)
                    .cache().filterByDate().queryFirst();
            if (contentAssoc != null) {
                getContentAncestryValues(delegator, contentAssoc.getString(contentIdOtherField), contentAssocTypeId, direction, contentAncestorList);
                GenericValue content = EntityQuery.use(delegator).from("Content")
                        .where("contentId", contentAssoc.getString(contentIdOtherField))
                        .cache().queryOne();

                contentAncestorList.add(content);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e,module);
            return;
        }
    }

    public static GenericValue pullEntityValues(Delegator delegator, String entityName, Map<String, Object> context) {
        GenericValue entOut = delegator.makeValue(entityName);
        entOut.setPKFields(context);
        entOut.setNonPKFields(context);
        return entOut;
    }

    /**
     * callContentPermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static String callContentPermissionCheck(Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> context) {
        Map<String, Object> permResults = callContentPermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        return permissionStatus;
    }

    public static Map<String, Object> callContentPermissionCheckResult(Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> context) {
        Map<String, Object> permResults = new HashMap<>();
        String skipPermissionCheck = (String) context.get("skipPermissionCheck");

        if (UtilValidate.isEmpty(skipPermissionCheck) 
                || (!"true".equalsIgnoreCase(skipPermissionCheck) && !"granted".equalsIgnoreCase(skipPermissionCheck))) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Map<String, Object> serviceInMap = new HashMap<>();
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
                if (ServiceUtil.isError(permResults)) {
                    Debug.logError(ServiceUtil.getErrorMessage(permResults) + "Problem checking permissions", "ContentServices");
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
            }
        } else {
            permResults.put("permissionStatus", "granted");
        }
        return permResults;
    }

    public static GenericValue getSubContent(Delegator delegator, String contentId, String mapKey, String subContentId, GenericValue userLogin, List<String> assocTypes, Timestamp fromDate) throws IOException {
        GenericValue view = null;
        try {
            if (subContentId == null) {
                if (contentId == null) {
                    throw new GenericEntityException("contentId and subContentId are null.");
                }
                Map<String, Object> results = null;
                results = ContentServicesComplex.getAssocAndContentAndDataResourceMethod(delegator, contentId, mapKey, "To", fromDate, null, null, null, assocTypes, null);
                List<GenericValue> entityList = UtilGenerics.checkList(results.get("entityList"));
                if (UtilValidate.isEmpty(entityList)) {
                    //throw new IOException("No subcontent found.");
                } else {
                    view = entityList.get(0);
                }
            } else {
                view = EntityQuery.use(delegator).from("ContentDataResourceView")
                        .where("contentId", subContentId).queryFirst();
                if (view == null) {
                    throw new IOException("No subContent found for subContentId=." + subContentId);
                }
            }
        } catch (GenericEntityException e) {
            throw new IOException(e.getMessage());
        }
        return view;
    }

    public static GenericValue getSubContentCache(Delegator delegator, String contentId, String mapKey, String subContentId, GenericValue userLogin, List<String> assocTypes, Timestamp fromDate, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException {
        GenericValue view = null;
        if (UtilValidate.isEmpty(subContentId)) {
            view = getSubContentCache(delegator, contentId, mapKey, userLogin, assocTypes, fromDate, nullThruDatesOnly, contentAssocPredicateId);
        } else {
            view = getContentCache(delegator, subContentId);
        }
        return view;
    }

    public static GenericValue getSubContentCache(Delegator delegator, String contentId, String mapKey, GenericValue userLogin, List<String> assocTypes, Timestamp fromDate, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException {
        GenericValue view = null;
        if (contentId == null) {
            Debug.logError("ContentId is null", module);
            return view;
        }
        Map<String, Object> results = null;
        List<String> contentTypes = null;
        try {
            // NOTE DEJ20060610: Changed "From" to "To" because it makes the most sense for sub-content renderings using a root-contentId and mapKey to determine the sub-contentId to have the ContentAssoc go from the root to the sub, ie try to determine the contentIdTo from the contentId and mapKey
            // This shouldn't be changed from "To" to "From", but if desired could be parameterized to make this selectable in higher up calling methods
            results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, mapKey, "To", fromDate, null, assocTypes, contentTypes, nullThruDatesOnly, contentAssocPredicateId, null);
        } catch (MiniLangException e) {
            throw new RuntimeException(e.getMessage());
        }
        List<GenericValue> entityList = UtilGenerics.checkList(results.get("entityList"));
        if (UtilValidate.isEmpty(entityList)) {
            //throw new IOException("No subcontent found.");
        } else {
            view = entityList.get(0);
        }
        return view;
    }

    public static GenericValue getContentCache(Delegator delegator, String contentId) throws GenericEntityException {
        return EntityQuery.use(delegator).from("ContentDataResourceView")
                .where("contentId", contentId)
                .cache().queryFirst();
    }

    public static GenericValue getCurrentContent(Delegator delegator, List<Map<String, ? extends Object>> trail, GenericValue userLogin, Map<String, Object> ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId)  throws GeneralException {
        String contentId = (String)ctx.get("contentId");
        String subContentId = (String)ctx.get("subContentId");
        String mapKey = (String)ctx.get("mapKey");
        Timestamp fromDate = UtilDateTime.nowTimestamp();
        List<String> assocTypes = null;
        List<Map<String, Object>> passedGlobalNodeTrail = null;
        GenericValue currentContent = null;
        String viewContentId = null;
        if (UtilValidate.isNotEmpty(trail)) {
            passedGlobalNodeTrail = UtilGenerics.checkList(UtilMisc.makeListWritable(trail));
        } else {
            passedGlobalNodeTrail = new LinkedList<>();
        }
        int sz = passedGlobalNodeTrail.size();
        if (sz > 0) {
            Map<String, Object> nd = passedGlobalNodeTrail.get(sz - 1);
            if (nd != null)
                currentContent = (GenericValue)nd.get("value");
            if (currentContent != null)
                viewContentId = (String)currentContent.get("contentId");
        }

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
        if (UtilValidate.isNotEmpty(contentId) || UtilValidate.isNotEmpty(subContentId)) {
            try {
                currentContent = ContentWorker.getSubContentCache(delegator, contentId, mapKey, subContentId, userLogin, assocTypes, fromDate, nullThruDatesOnly, contentAssocPredicateId);
                Map<String, Object> node = ContentWorker.makeNode(currentContent);
                passedGlobalNodeTrail.add(node);
            } catch (GenericEntityException e) {
                throw new GeneralException(e.getMessage());
            }
        }
        ctx.put("globalNodeTrail", passedGlobalNodeTrail);
        ctx.put("indent", sz);
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
        } catch (IllegalArgumentException e) {
            dataResourceId = (String) view.get("dataResourceId");
        }
        content.set("dataResourceId", dataResourceId);
        return content;
    }

    public static Map<String, Object> buildPickContext(Delegator delegator, String contentAssocTypeId, String assocContentId, String direction, GenericValue thisContent) throws GenericEntityException {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("contentAssocTypeId", contentAssocTypeId);
        ctx.put("contentId", assocContentId);
        // This needs to be the opposite
        if (direction != null && "From".equalsIgnoreCase(direction)) {
            ctx.put("contentIdFrom", assocContentId);
        } else {
            ctx.put("contentIdTo", assocContentId);
        }
        if (thisContent == null)
            thisContent = EntityQuery.use(delegator).from("Content").where("contentId", assocContentId).cache().queryOne();
        ctx.put("content", thisContent);
        List<Object> purposes = getPurposes(thisContent);
        ctx.put("purposes", purposes);
        List<String> contentTypeAncestry = new LinkedList<>();
        String contentTypeId = thisContent.getString("contentTypeId");
        getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
        ctx.put("typeAncestry", contentTypeAncestry);
        List<Object> sections = getSections(thisContent);
        ctx.put("sections", sections);
        List<Object> topics = getTopics(thisContent);
        ctx.put("topics", topics);
        return ctx;
    }

    public static void checkConditions(Delegator delegator, Map<String, Object> trailNode, Map<String, Object> contentAssoc, Map<String, Object> whenMap) {
        Map<String, Object> context = new HashMap<>();
        GenericValue content = (GenericValue)trailNode.get("value");
        if (content != null) {
            context.put("content", content);
            List<Object> purposes = getPurposes(content);
            context.put("purposes", purposes);
            List<Object> sections = getSections(content);
            context.put("sections", sections);
            List<Object> topics = getTopics(content);
            context.put("topics", topics);
            String contentTypeId = (String)content.get("contentTypeId");
            List<String> contentTypeAncestry = new LinkedList<>();
            try {
                getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
            }
            context.put("typeAncestry", contentTypeAncestry);
            if (contentAssoc == null && (content.getEntityName().indexOf("Assoc") >= 0)) {
                contentAssoc = delegator.makeValue("ContentAssoc");
                try {
                    // TODO: locale needs to be gotten correctly
                    SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml", "contentAssocIn", content, contentAssoc, new LinkedList<>(), Locale.getDefault());
                    context.put("contentAssocTypeId", contentAssoc.get("contentAssocTypeId"));
                    context.put("contentAssocPredicateId", contentAssoc.get("contentAssocPredicateId"));
                    context.put("mapKey", contentAssoc.get("mapKey"));
                } catch (MiniLangException e) {
                    Debug.logError(e.getMessage(), module);
                }
            } else {
                context.put("contentAssocTypeId", null);
                context.put("contentAssocPredicateId", null);
                context.put("mapKey", null);
            }
        }
        boolean isReturnBefore = checkWhen(context, (String)whenMap.get("returnBeforePickWhen"), false);
        trailNode.put("isReturnBefore", isReturnBefore);
        boolean isPick = checkWhen(context, (String)whenMap.get("pickWhen"), true);
        trailNode.put("isPick", isPick);
        boolean isFollow = checkWhen(context, (String)whenMap.get("followWhen"), true);
        trailNode.put("isFollow", isFollow);
        boolean isReturnAfter = checkWhen(context, (String)whenMap.get("returnAfterPickWhen"), false);
        trailNode.put("isReturnAfter", isReturnAfter);
        trailNode.put("checked", Boolean.TRUE);
    }

    public static boolean booleanDataType(Object boolObj) {
        boolean bool = false;
        if (boolObj != null && (Boolean) boolObj) {
            bool = true;
        }
        return bool;
    }

    public static List<String> prepTargetOperationList(Map<String, ? extends Object> context, String md) {
        List<String> targetOperationList = UtilGenerics.checkList(context.get("targetOperationList"));
        String targetOperationString = (String)context.get("targetOperationString");
        if (Debug.infoOn()) {
            Debug.logInfo("in prepTargetOperationList, targetOperationString(0):" + targetOperationString, "");
        }
        if (UtilValidate.isNotEmpty(targetOperationString)) {
            List<String> opsFromString = StringUtil.split(targetOperationString, "|");
            if (UtilValidate.isEmpty(targetOperationList)) {
                targetOperationList = new LinkedList<>();
            }
            targetOperationList.addAll(opsFromString);
        }
        if (UtilValidate.isEmpty(targetOperationList)) {
            targetOperationList = new LinkedList<>();
            if (UtilValidate.isEmpty(md)) {
                md ="_CREATE";
            }
            targetOperationList.add("CONTENT" + md);
        }
        if (Debug.infoOn()) {
            Debug.logInfo("in prepTargetOperationList, targetOperationList(0):" + targetOperationList, "");
        }
        return targetOperationList;
    }

    /**
     * Checks to see if there is a purpose string (delimited by pipes) and
     * turns it into a list and concants to any existing purpose list.
     * @param context the context
     * @return the list of content purpose
     */
    public static List<String> prepContentPurposeList(Map<String, Object> context) {
        List<String> contentPurposeList = UtilGenerics.checkList(context.get("contentPurposeList"));
        String contentPurposeString = (String)context.get("contentPurposeString");
        if (Debug.infoOn()) {
            Debug.logInfo("in prepContentPurposeList, contentPurposeString(0):" + contentPurposeString, "");
        }
        if (UtilValidate.isNotEmpty(contentPurposeString)) {
            List<String> purposesFromString = StringUtil.split(contentPurposeString, "|");
            if (UtilValidate.isEmpty(contentPurposeList)) {
                contentPurposeList = new LinkedList<>();
            }
            contentPurposeList.addAll(purposesFromString);
        }
        if (UtilValidate.isEmpty(contentPurposeList)) {
            contentPurposeList = new LinkedList<>();
        }
        if (Debug.infoOn()) {
            Debug.logInfo("in prepContentPurposeList, contentPurposeList(0):" + contentPurposeList, "");
        }
        return contentPurposeList;
    }

    public static String prepPermissionErrorMsg(Map<String, Object> permResults) {
        String permissionStatus = (String)permResults.get("permissionStatus");
        String errorMessage = "Permission is denied." + permissionStatus;
        errorMessage += ServiceUtil.getErrorMessage(permResults);
        PermissionRecorder recorder = (PermissionRecorder)permResults.get("permissionRecorder");
        Debug.logInfo("recorder(0):" + recorder, "");
        if (recorder != null && recorder.isOn()) {
            String permissionMessage = recorder.toHtml();
            errorMessage += " \n " + permissionMessage;
        }
        return errorMessage;
    }

    public static List<GenericValue> getContentAssocViewList(Delegator delegator, String contentIdTo, String contentId, String contentAssocTypeId, String statusId, String privilegeEnumId) throws GenericEntityException {
        List<EntityExpr> exprListAnd = new LinkedList<>();

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

        return EntityQuery.use(delegator).from("ContentAssocDataResourceViewFrom")
                .where(exprListAnd)
                .filterByDate("caFromDate", "caThruDate")
                .queryList();
    }

    public static GenericValue getContentAssocViewFrom(Delegator delegator, String contentIdTo, String contentId, String contentAssocTypeId, String statusId, String privilegeEnumId) throws GenericEntityException {
        List<GenericValue> filteredList = getContentAssocViewList(delegator, contentIdTo, contentId, contentAssocTypeId, statusId, privilegeEnumId);

        GenericValue val = null;
        if (filteredList.size() > 0) {
            val = filteredList.get(0);
        }
        return val;
    }

    public static Map<String, Object> makeNode(GenericValue thisContent) {
        Map<String, Object> thisNode = null;
        if (thisContent == null) {
            return thisNode;
        }

        thisNode = new HashMap<>();
        thisNode.put("value", thisContent);
        String contentId = (String)thisContent.get("contentId");
        thisNode.put("contentId", contentId);
        thisNode.put("contentTypeId", thisContent.get("contentTypeId"));
        thisNode.put("isReturnBeforePick", Boolean.FALSE);
        thisNode.put("isReturnAfterPick", Boolean.FALSE);
        thisNode.put("isPick", Boolean.TRUE);
        thisNode.put("isFollow", Boolean.TRUE);
        if (thisContent.getModelEntity().getField("caContentAssocTypeId") != null) {
            thisNode.put("contentAssocTypeId", thisContent.get("caContentAssocTypeId"));
            thisNode.put("mapKey", thisContent.get("caMapKey"));
            thisNode.put("fromDate", thisContent.get("caFromDate"));
        }
        return thisNode;
    }

    public static String nodeTrailToCsv(List<Map<String, ? extends Object>> nodeTrail) {
        if (nodeTrail == null) {
            return "";
        }
        StringBuilder csv = new StringBuilder();
        for (Map<String, ? extends Object> node : nodeTrail) {
            if (csv.length() > 0) {
                csv.append(",");
            }
            if (node == null) {
                break;
            }
            String contentId = (String)node.get("contentId");
            csv.append(contentId);
        }
        return csv.toString();
    }

    public static List<List<String>> csvToList(String csv, Delegator delegator) {
        List<List<String>> outList = new LinkedList<>();
        List<String> contentIdList = StringUtil.split(csv, ",");
        GenericValue content = null;
        String contentName = null;
        for (String contentId : contentIdList) {
            try {
                content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                return new LinkedList<>();
            }
            contentName = (String)content.get("contentName");
            outList.add(UtilMisc.toList(contentId, contentName));
        }
        return outList;
    }

    public static List<GenericValue> csvToContentList(String csv, Delegator delegator) {
        List<GenericValue> trail = new LinkedList<>();
        if (csv == null) {
            return trail;
        }
        List<String> contentIdList = StringUtil.split(csv, ",");
        GenericValue content = null;
        for (String contentId : contentIdList) {
            try {
                content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), module);
                return new LinkedList<>();
            }
            trail.add(content);
        }
        return trail;
    }

    public static List<Map<String, Object>> csvToTrail(String csv, Delegator delegator) {
        List<Map<String, Object>> trail = new LinkedList<>();
        if (csv == null) {
            return trail;
        }
        List<GenericValue> contentList = csvToContentList(csv, delegator);
        for (GenericValue content : contentList) {
            Map<String, Object> node = makeNode(content);
            trail.add(node);
        }
        return trail;
    }

    public static String getMimeTypeId(Delegator delegator, GenericValue view, Map<String, Object> ctx) {
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = (String)ctx.get("mimeTypeId");
        if (UtilValidate.isEmpty(mimeTypeId) && view != null) {
            mimeTypeId = (String) view.get("mimeTypeId");
            String parentContentId = (String)ctx.get("contentId");
            if (UtilValidate.isEmpty(mimeTypeId) && UtilValidate.isNotEmpty(parentContentId)) { // will need these below
                try {
                    GenericValue parentContent = EntityQuery.use(delegator).from("Content").where("contentId", parentContentId).queryOne();
                    if (parentContent != null) {
                        mimeTypeId = (String) parentContent.get("mimeTypeId");
                        ctx.put("parentContent", parentContent);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(), module);
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
    public static String determineMimeType(Delegator delegator, GenericValue view, GenericValue parentContent, String contentId, String dataResourceId, String parentContentId) throws GenericEntityException {
        String mimeTypeId = null;

        if (view != null) {
            mimeTypeId = view.getString("mimeTypeId");
            String drMimeTypeId = view.getString("drMimeTypeId");
            if (UtilValidate.isNotEmpty(drMimeTypeId)) {
                mimeTypeId = drMimeTypeId;
            }
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            if (UtilValidate.isNotEmpty(contentId) && UtilValidate.isNotEmpty(dataResourceId)) {
                view = EntityQuery.use(delegator).from("SubContentDataResourceView").where("contentId", contentId, "drDataResourceId", dataResourceId).queryOne();
                if (view != null) {
                    mimeTypeId = view.getString("mimeTypeId");
                    String drMimeTypeId = view.getString("drMimeTypeId");
                    if (UtilValidate.isNotEmpty(drMimeTypeId)) {
                        mimeTypeId = drMimeTypeId;
                    }
                }
            }
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            if (parentContent != null) {
                mimeTypeId = parentContent.getString("mimeTypeId");
            }
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            if (UtilValidate.isNotEmpty(parentContentId)) {
                parentContent = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                if (parentContent != null) {
                    mimeTypeId = parentContent.getString("mimeTypeId");
                }
            }
        }
        return mimeTypeId;
    }

    public static String logMap(String lbl, Map<String, Object> map, int indentLevel) {
        StringBuilder indent = new StringBuilder();
        for (int i=0; i<indentLevel; i++) {
            indent.append(' ');
        }
        return logMap(new StringBuilder(), lbl, map, indent).toString();
    }

    public static StringBuilder logMap(StringBuilder s, String lbl, Map<String, Object> map, StringBuilder indent) {
        String sep = ":";
        String eol = "\n";
        String spc = "";
        if (lbl != null) {
            s.append(lbl);
        }
        s.append("=").append(indent).append("==>").append(eol);
        for (String key : map.keySet()) {
            if ("request response session".indexOf(key) < 0) {
                Object obj = map.get(key);
                s.append(spc).append(key).append(sep);
                if (obj instanceof GenericValue) {
                    GenericValue gv = (GenericValue)obj;
                    GenericPK pk = gv.getPrimaryKey();
                    indent.append(' ');
                    logMap(s, "GMAP[" + key + " name:" + pk.getEntityName()+ "]", pk, indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj instanceof List<?>) {
                    indent.append(' ');
                    logList(s, "LIST[" + ((List<?>)obj).size() + "]", UtilGenerics.checkList(obj), indent);
                    indent.setLength(indent.length() - 1);
                } else if (obj instanceof Map<?, ?>) {
                    indent.append(' ');
                    logMap(s, "MAP[" + key + "]", UtilGenerics.<String, Object>checkMap(obj), indent);
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

    public static String logList(String lbl, List<Object> lst, int indentLevel) {
        StringBuilder indent = new StringBuilder();
        for (int i=0; i<indentLevel; i++) {
            indent.append(' ');
        }
        return logList(new StringBuilder(), lbl, lst, indent).toString();
    }

    public static StringBuilder logList(StringBuilder s, String lbl, List<Object> lst, StringBuilder indent) {
        String sep = ":";
        String eol = "\n";
        String spc = "";
        if (lst == null) {
            return s;
        }
        int sz = lst.size();
        if (lbl != null) s.append(lbl);
        s.append("=").append(indent).append("==> sz:").append(sz).append(eol);
        for (Object obj : lst) {
            s.append(spc);
            if (obj instanceof GenericValue) {
                GenericValue gv = (GenericValue)obj;
                GenericPK pk = gv.getPrimaryKey();
                indent.append(' ');
                logMap(s, "MAP[name:" + pk.getEntityName() + "]", pk, indent);
                indent.setLength(indent.length() - 1);
            } else if (obj instanceof List<?>) {
                indent.append(' ');
                logList(s, "LIST[" + ((List<?>)obj).size() + "]", UtilGenerics.checkList(obj), indent);
                indent.setLength(indent.length() - 1);
            } else if (obj instanceof Map<?, ?>) {
                indent.append(' ');
                logMap(s, "MAP[]", UtilGenerics.<String, Object>checkMap(obj), indent);
                indent.setLength(indent.length() - 1);
            } else if (obj != null) {
                s.append(obj).append(sep).append(obj.getClass()).append(eol);
            } else {
                s.append(eol);
            }
        }
        return s.append(eol).append(eol);
    }

    public static void traceNodeTrail(String lbl, List<Map<String, Object>> nodeTrail) {

    }
}

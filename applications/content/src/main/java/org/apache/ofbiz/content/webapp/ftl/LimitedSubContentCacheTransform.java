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
package org.apache.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentServicesComplex;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * LimitedSubContentCacheTransform - Freemarker Transform for URLs (links)
 */
public class LimitedSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = LimitedSubContentCacheTransform.class.getName();

    static final String[] upSaveKeyNames = { "globalNodeTrail" };
    static final String[] saveKeyNames = { "contentId", "subContentId", "entityList", "entityIndex",
            "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale", "entityList", "viewSize", "viewIndex",
            "highIndex", "lowIndex", "listSize", "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly",
            "globalNodeTrail", "outputIndex" };

    /**
     * @deprecated use FreeMarkerWorker.getWrappedObject()
     * A wrapper for the FreeMarkerWorker version.
     */
    @Deprecated
    public static Object getWrappedObject(String varName, Environment env) {
        return FreeMarkerWorker.getWrappedObject(varName, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, ? extends Object> args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, ? extends Object> args, String key, Map<String, ? extends Object> ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        final Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        final Map<String, Object> savedValuesUp = new HashMap<>();
        FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        final Map<String, Object> savedValues = new HashMap<>();
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);

        String contentAssocTypeId = (String) templateRoot.get("contentAssocTypeId");
        if (UtilValidate.isEmpty(contentAssocTypeId)) {
            contentAssocTypeId = "SUB_CONTENT";
            templateRoot.put("contentAssocTypeId ", contentAssocTypeId);
        }

        final Map<String, GenericValue> pickedEntityIds = new HashMap<>();
        List<String> assocTypes = StringUtil.split(contentAssocTypeId, "|");

        String contentPurposeTypeId = (String) templateRoot.get("contentPurposeTypeId");
        List<String> purposeTypes = StringUtil.split(contentPurposeTypeId, "|");
        templateRoot.put("purposeTypes", purposeTypes);
        Locale locale = (Locale) templateRoot.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
            templateRoot.put("locale", locale);
        }

        Map<String, Object> whenMap = new HashMap<>();
        whenMap.put("followWhen", templateRoot.get("followWhen"));
        whenMap.put("pickWhen", templateRoot.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", templateRoot.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", templateRoot.get("returnAfterPickWhen"));
        templateRoot.put("whenMap", whenMap);

        String fromDateStr = (String) templateRoot.get("fromDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        if (fromDate == null) {
            fromDate = UtilDateTime.nowTimestamp();
        }

        String limitSize = (String) templateRoot.get("limitSize");
        final int returnLimit = Integer.parseInt(limitSize);
        String orderBy = (String) templateRoot.get("orderBy");

        // NOTE this was looking for subContentId, but that doesn't make ANY sense, so changed to contentId
        String contentId = (String) templateRoot.get("contentId");

        templateRoot.put("contentId", null);
        templateRoot.put("subContentId", null);

        Map<String, Object> results = null;
        String contentAssocPredicateId = (String) templateRoot.get("contentAssocPredicateId");
        try {
            results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, null, "From", fromDate, null, assocTypes, null, Boolean.TRUE, contentAssocPredicateId, orderBy);
        } catch (MiniLangException | GenericEntityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        List<GenericValue> longList = UtilGenerics.checkList(results.get("entityList"));
        templateRoot.put("entityList", longList);

        return new LoopWriter(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public int onStart() throws TemplateModelException, IOException {
                boolean inProgress = false;
                if (pickedEntityIds.size() < returnLimit) {
                    inProgress = getNextMatchingEntity(templateRoot, delegator, env);
                }
                FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                if (inProgress) {
                    return TransformControl.EVALUATE_BODY;
                }
                return TransformControl.SKIP_BODY;
            }

            @Override
            public int afterBody() throws TemplateModelException, IOException {
                FreeMarkerWorker.reloadValues(templateRoot, savedValues, env);
                List<Map<String, ? extends Object>> list = UtilGenerics.checkList(templateRoot.get("globalNodeTrail"));
                List<Map<String, ? extends Object>> subList = list.subList(0, list.size() - 1);
                templateRoot.put("globalNodeTrail", subList);
                env.setVariable("globalNodeTrail", FreeMarkerWorker.autoWrap(subList, env));

                boolean inProgress = false;
                if (pickedEntityIds.size() < returnLimit) {
                    inProgress = getNextMatchingEntity(templateRoot, delegator, env);
                }

                FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                if (inProgress) {
                    return TransformControl.REPEAT_EVALUATION;
                }
                return TransformControl.END_EVALUATION;
            }

            @Override
            public void close() throws IOException {
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
            }

            public boolean prepCtx(Delegator delegator, Map<String, Object> ctx, Environment env, GenericValue view) throws GeneralException {
                String subContentIdSub = (String) view.get("contentId");
                // This order is taken so that the dataResourceType can be overridden in the transform arguments.
                String subDataResourceTypeId = (String) ctx.get("subDataResourceTypeId");
                if (UtilValidate.isEmpty(subDataResourceTypeId)) {
                    subDataResourceTypeId = (String) view.get("drDataResourceTypeId");
                    // TODO: If this value is still empty then it is probably necessary to get a value from
                    // the parent context. But it will already have one and it is the same context that is
                    // being passed.
                }

                String mimeTypeId = ContentWorker.getMimeTypeId(delegator, view, ctx);
                Map<String, Object> trailNode = ContentWorker.makeNode(view);
                Map<String, Object> whenMap = UtilGenerics.checkMap(ctx.get("whenMap"));
                ContentWorker.checkConditions(delegator, trailNode, null, whenMap);
                Boolean isReturnBeforeObj = (Boolean) trailNode.get("isReturnBefore");
                Boolean isPickObj = (Boolean) trailNode.get("isPick");
                Boolean isFollowObj = (Boolean) trailNode.get("isFollow");
                if ((isReturnBeforeObj == null || !isReturnBeforeObj.booleanValue()) && ((isPickObj != null &&
                        isPickObj.booleanValue()) || (isFollowObj != null && isFollowObj.booleanValue()))) {
                    List<Map<String, ? extends Object>> globalNodeTrail = UtilGenerics.checkList(ctx.get("globalNodeTrail"));
                    if (globalNodeTrail == null) {
                        globalNodeTrail = new LinkedList<>();
                    }
                    globalNodeTrail.add(trailNode);
                    ctx.put("globalNodeTrail", globalNodeTrail);
                    String csvTrail = ContentWorker.nodeTrailToCsv(globalNodeTrail);
                    ctx.put("nodeTrailCsv", csvTrail);
                    int indentSz = globalNodeTrail.size();
                    ctx.put("indent", Integer.valueOf(indentSz));

                    ctx.put("subDataResourceTypeId", subDataResourceTypeId);
                    ctx.put("mimeTypeId", mimeTypeId);
                    ctx.put("subContentId", subContentIdSub);
                    ctx.put("content", view);

                    env.setVariable("subDataResourceTypeId", FreeMarkerWorker.autoWrap(subDataResourceTypeId, env));
                    env.setVariable("indent", FreeMarkerWorker.autoWrap(Integer.valueOf(indentSz), env));
                    env.setVariable("nodeTrailCsv", FreeMarkerWorker.autoWrap(csvTrail, env));
                    env.setVariable("globalNodeTrail", FreeMarkerWorker.autoWrap(globalNodeTrail, env));
                    env.setVariable("content", FreeMarkerWorker.autoWrap(view, env));
                    env.setVariable("mimeTypeId", FreeMarkerWorker.autoWrap(mimeTypeId, env));
                    env.setVariable("subContentId", FreeMarkerWorker.autoWrap(subContentIdSub, env));
                    return true;
                }
                return false;
            }

            public GenericValue getRandomEntity() {
                GenericValue pickEntity = null;
                List<GenericValue> lst = UtilGenerics.checkList(templateRoot.get("entityList"));
                if (Debug.verboseOn()) {
                    Debug.logVerbose("in limited, lst:" + lst, "");
                }

                while (pickEntity == null && lst.size() > 0) {
                    double randomValue = Math.random();
                    int idx = (int) (lst.size() * randomValue);
                    pickEntity = lst.get(idx);
                    String pickEntityId = pickEntity.getString("contentId");
                    if (pickedEntityIds.get(pickEntityId) == null) {
                        pickedEntityIds.put(pickEntityId, pickEntity);
                        lst.remove(idx);
                    } else {
                        pickEntity = null;
                    }
                }
                return pickEntity;
            }

            public boolean getNextMatchingEntity(Map<String, Object> templateRoot, Delegator delegator, Environment env) throws IOException {
                boolean matchFound = false;
                GenericValue pickEntity = getRandomEntity();

                while (pickEntity != null && !matchFound) {
                    try {
                        matchFound = prepCtx(delegator, templateRoot, env, pickEntity);
                    } catch (GeneralException e) {
                        throw new IOException(e.getMessage());
                    }
                    if (!matchFound) {
                        pickEntity = getRandomEntity();
                    }
                }
                return matchFound;
            }
        };
    }
}

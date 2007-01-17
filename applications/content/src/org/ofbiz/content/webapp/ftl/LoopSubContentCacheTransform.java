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
package org.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentServicesComplex;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * LoopSubContentCacheTransform - Freemarker Transform for URLs (links)
 */
public class LoopSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = LoopSubContentCacheTransform.class.getName();

    public static final String[] upSaveKeyNames = {"globalNodeTrail"};
    public static final String[] saveKeyNames = {"contentId", "subContentId", "entityList", "entityIndex", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale", "entityList", "viewSize", "viewIndex", "highIndex", "lowIndex", "listSize", "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail", "outputIndex"};

    /**
     * A wrapper for the FreeMarkerWorker version.
     */
    public static Object getWrappedObject(String varName, Environment env) {
        return FreeMarkerWorker.getWrappedObject(varName, env);
    }

    public static String getArg(Map args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    public static String getArg(Map args, String key, Map ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    public static boolean prepCtx(GenericDelegator delegator, Map ctx, Environment env) throws GeneralException {

        //String contentId = (String)ctx.get("contentId");
        //String mimeTypeId = (String)ctx.get("mimeTypeId");
        List lst = (List) ctx.get("entityList");
        int entityIndex = ((Integer) ctx.get("entityIndex")).intValue();
        //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache, prepCtx, entityIndex :" + entityIndex, module);
        if (entityIndex >= lst.size()) {
            return false;
        }
        GenericValue view = (GenericValue) lst.get(entityIndex);
        //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache, subContentDataResourceView contentId/drDataResourceId:" + view.get("contentId")  + " / " + view.get("drDataResourceId") + " entityIndex:" + entityIndex, module);

        String dataResourceId = (String) view.get("drDataResourceId");
        String subContentIdSub = (String) view.get("contentId");
        //String contentAssocTypeId = (String) view.get("caContentAssocTypeId");
        //String mapKey = (String) view.get("caMapKey");
        //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache(0), subContentIdSub ." + subContentIdSub, module);
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String) ctx.get("subDataResourceTypeId");
        if (UtilValidate.isEmpty(subDataResourceTypeId)) {
            subDataResourceTypeId = (String) view.get("drDataResourceTypeId");
            // TODO: If this value is still empty then it is probably necessary to get a value from
            // the parent context. But it will already have one and it is the same context that is
            // being passed.
        }

        String mimeTypeId = ContentWorker.getMimeTypeId(delegator, view, ctx);
        Map trailNode = ContentWorker.makeNode(view);
        Map whenMap = (Map) ctx.get("whenMap");
        Locale locale = (Locale) ctx.get("locale");
        if (locale == null)
            locale = Locale.getDefault();
        GenericValue assocContent = null;
        ContentWorker.checkConditions(delegator, trailNode, assocContent, whenMap);
        Boolean isReturnBeforeObj = (Boolean) trailNode.get("isReturnBefore");
        Boolean isReturnAfterObj = (Boolean) trailNode.get("isReturnAfter");
        Boolean isPickObj = (Boolean) trailNode.get("isPick");
        Boolean isFollowObj = (Boolean) trailNode.get("isFollow");
        //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache, isReturnBeforeObj" + isReturnBeforeObj + " isPickObj:" + isPickObj + " isFollowObj:" + isFollowObj + " isReturnAfterObj:" + isReturnAfterObj, module);
        if ((isReturnBeforeObj == null || !isReturnBeforeObj.booleanValue()) && ((isPickObj != null &&
                isPickObj.booleanValue()) || (isFollowObj != null && isFollowObj.booleanValue()))) {
            List globalNodeTrail = (List) ctx.get("globalNodeTrail");
            globalNodeTrail.add(trailNode);
            ctx.put("globalNodeTrail", globalNodeTrail);
            String csvTrail = ContentWorker.nodeTrailToCsv(globalNodeTrail);
            ctx.put("nodeTrailCsv", csvTrail);
            //if (Debug.infoOn()) Debug.logInfo("prepCtx, csvTrail(2):" + csvTrail, "");
            int indentSz = globalNodeTrail.size();
            ctx.put("indent", new Integer(indentSz));
            //if (Debug.infoOn()) Debug.logInfo("prepCtx, trail(2):" + trail, "");
            //if (Debug.infoOn()) Debug.logInfo("prepCtx, globalNodeTrail csv:" + FreeMarkerWorker.nodeTrailToCsv((List)trail), "");

            /*
            GenericValue electronicText = null;
            try {
                electronicText = view.getRelatedOneCache("ElectronicText");
            } catch (GenericEntityException e) {
                throw new GeneralException(e.getMessage());
            }
            if (electronicText != null)
                ctx.put("textData", electronicText.get("textData"));
            else
                ctx.put("textData", null);
            */

            ctx.put("subDataResourceTypeId", subDataResourceTypeId);
            ctx.put("mimeTypeId", mimeTypeId);
            ctx.put("subContentId", subContentIdSub);
            ctx.put("content", view);
            //ctx.put("drDataResourceId", dataResourceId);
            //ctx.put("dataResourceId", dataResourceId);
            //ctx.put("subContentIdSub", subContentIdSub);

            env.setVariable("subDataResourceTypeId", FreeMarkerWorker.autoWrap(subDataResourceTypeId, env));
            env.setVariable("indent", FreeMarkerWorker.autoWrap(new Integer(indentSz), env));
            env.setVariable("nodeTrailCsv", FreeMarkerWorker.autoWrap(csvTrail, env));
            env.setVariable("globalNodeTrail", FreeMarkerWorker.autoWrap(globalNodeTrail, env));
            env.setVariable("content", FreeMarkerWorker.autoWrap(view, env));
            env.setVariable("mimeTypeId", FreeMarkerWorker.autoWrap(mimeTypeId, env));
            env.setVariable("subContentId", FreeMarkerWorker.autoWrap(subContentIdSub, env));
            return true;
        } else {
            return false;
        }
    }

    public static boolean getNextMatchingEntity(Map templateRoot, GenericDelegator delegator, Environment env) throws IOException {
        int lowIndex = ((Integer) templateRoot.get("lowIndex")).intValue();
        int entityIndex = ((Integer) templateRoot.get("entityIndex")).intValue();
        int outputIndex = ((Integer) templateRoot.get("outputIndex")).intValue();
        int listSize = ((Integer) templateRoot.get("listSize")).intValue();
        boolean matchFound = false;

        while (!matchFound && entityIndex < listSize) {
            try {
                matchFound = prepCtx(delegator, templateRoot, env);
            } catch (GeneralException e) {
                throw new IOException(e.getMessage());
            }
            entityIndex++;
            templateRoot.put("entityIndex", new Integer(entityIndex));
            if (matchFound) {
                outputIndex++;
                if (outputIndex >= lowIndex) {
                    break;
                } else {
                    matchFound = false;
                }
            }
        }
        //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache, getNextMatchingEntity, outputIndex :" + outputIndex, module);
        templateRoot.put("outputIndex", new Integer(outputIndex));
        env.setVariable("outputIndex", FreeMarkerWorker.autoWrap(new Integer(outputIndex), env));
        env.setVariable("entityIndex", FreeMarkerWorker.autoWrap(new Integer(entityIndex), env));
        return matchFound;
    }

    public Writer getWriter(final Writer out, Map args) {
        //Profiler.begin("Loop");
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        //final Map templateRoot = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        final Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
        //FreeMarkerWorker.convertContext(templateRoot);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        //templateRoot.put("buf", buf);
        final Map savedValuesUp = new HashMap();
        FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        final Map savedValues = new HashMap();
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);
        String contentAssocTypeId = (String) templateRoot.get("contentAssocTypeId");
        //if (UtilValidate.isEmpty(contentAssocTypeId)) {
        //throw new RuntimeException("contentAssocTypeId is empty");
        //}
        List assocTypes = StringUtil.split(contentAssocTypeId, "|");

        String contentPurposeTypeId = (String) templateRoot.get("contentPurposeTypeId");
        List purposeTypes = StringUtil.split(contentPurposeTypeId, "|");
        templateRoot.put("purposeTypes", purposeTypes);
        Locale locale = (Locale) templateRoot.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
            templateRoot.put("locale", locale);
        }

        Map whenMap = new HashMap();
        whenMap.put("followWhen", (String) templateRoot.get("followWhen"));
        whenMap.put("pickWhen", (String) templateRoot.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", (String) templateRoot.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", (String) templateRoot.get("returnAfterPickWhen"));
        templateRoot.put("whenMap", whenMap);

        String fromDateStr = (String) templateRoot.get("fromDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        if (fromDate == null)
            fromDate = UtilDateTime.nowTimestamp();

        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        List globalNodeTrail = (List) templateRoot.get("globalNodeTrail");
        //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache(0), nodeTrailCsv ." + FreeMarkerWorker.nodeTrailToCsv(globalNodeTrail), module);
        String strNullThruDatesOnly = (String) templateRoot.get("nullThruDatesOnly");
        String orderBy = (String) templateRoot.get("orderBy");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE : Boolean.FALSE;
        GenericValue val = null;
        try {
            val = ContentWorker.getCurrentContent(delegator, globalNodeTrail, userLogin, templateRoot, nullThruDatesOnly, null);
        } catch (GeneralException e) {
            Debug.logError(e, module);
        }
        final GenericValue view = val;

        if (view != null) {
            templateRoot.put("contentId", null);
            templateRoot.put("subContentId", null);

            String contentId = (String) view.get("contentId");
            //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache(0), contentId ." + contentId, module);
            final String contentIdTo = contentId;

            String thisMapKey = (String) templateRoot.get("mapKey");
            //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache(0), thisMapKey ." + thisMapKey, module);
            Map results = null;
            //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache(0), assocTypes ." + assocTypes, module);
            String contentAssocPredicateId = (String) templateRoot.get("contentAssocPredicateId");
            try {
                results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, thisMapKey, "From", fromDate, null, assocTypes, null, Boolean.TRUE, contentAssocPredicateId, orderBy);
            } catch (MiniLangException e2) {
                throw new RuntimeException(e2.getMessage());
            } catch (GenericEntityException e) {
                throw new RuntimeException(e.getMessage());
            }
            List longList = (List) results.get("entityList");
            //if (Debug.infoOn()) Debug.logInfo("in LoopSubContentCache(0), longList ." + longList.size(), module);
            String viewSizeStr = (String) templateRoot.get("viewSize");
            if (UtilValidate.isEmpty(viewSizeStr))
                viewSizeStr = UtilProperties.getPropertyValue("content", "viewSize");
            if (UtilValidate.isEmpty(viewSizeStr))
                viewSizeStr = "10";
            int viewSize = Integer.parseInt(viewSizeStr);
            String viewIndexStr = (String) templateRoot.get("viewIndex");
            if (UtilValidate.isEmpty(viewIndexStr))
                viewIndexStr = "0";
            int viewIndex = Integer.parseInt(viewIndexStr);
            int lowIndex = viewIndex * viewSize;
            int listSize = longList.size();
            int highIndex = (viewIndex + 1) * viewSize;
            if (highIndex > listSize)
                highIndex = listSize;
            //if (Debug.infoOn()) Debug.logInfo("viewIndexStr(0):" + viewIndexStr + " viewIndex:" + viewIndex, "");
            //if (Debug.infoOn()) Debug.logInfo("viewSizeStr(0):" + viewSizeStr + " viewSize:" + viewSize, "");
            //if (Debug.infoOn()) Debug.logInfo("listSize(0):" + listSize , "");
            //if (Debug.infoOn()) Debug.logInfo("highIndex(0):" + highIndex , "");
            //if (Debug.infoOn()) Debug.logInfo("lowIndex(0):" + lowIndex , "");
            Iterator it = longList.iterator();
            //List entityList = longList.subList(lowIndex, highIndex);
            List entityList = longList;
            templateRoot.put("entityList", entityList);
            templateRoot.put("viewIndex", new Integer(viewIndex));
            templateRoot.put("viewSize", new Integer(viewSize));
            templateRoot.put("lowIndex", new Integer(lowIndex));
            templateRoot.put("highIndex", new Integer(highIndex));
            templateRoot.put("listSize", new Integer(listSize));

            env.setVariable("entityList", FreeMarkerWorker.autoWrap(entityList, env));
            env.setVariable("viewIndex", FreeMarkerWorker.autoWrap(new Integer(viewIndex), env));
            env.setVariable("viewSize", FreeMarkerWorker.autoWrap(new Integer(viewSize), env));
            env.setVariable("lowIndex", FreeMarkerWorker.autoWrap(new Integer(lowIndex), env));
            env.setVariable("highIndex", FreeMarkerWorker.autoWrap(new Integer(highIndex), env));
            env.setVariable("listSize", FreeMarkerWorker.autoWrap(new Integer(listSize), env));
        }

        return new LoopWriter(out) {

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
                //StringBuffer ctxBuf = (StringBuffer) templateRoot.get("buf");
                //ctxBuf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public int onStart() throws TemplateModelException, IOException {

                if (view == null) {
                    return TransformControl.SKIP_BODY;
                }

                List globalNodeTrail = (List) templateRoot.get("globalNodeTrail");
                String trailCsv = ContentWorker.nodeTrailToCsv(globalNodeTrail);
                //if (Debug.infoOn()) Debug.logInfo("in Loop, onStart, trailCsv:" + trailCsv, "");
                int viewIndex = ((Integer) templateRoot.get("viewIndex")).intValue();
                int viewSize = ((Integer) templateRoot.get("viewSize")).intValue();
                int listSize = ((Integer) templateRoot.get("listSize")).intValue();
                int lowIndex = viewIndex * viewSize;
                int highIndex = (viewIndex + 1) * viewSize;
                if (highIndex > listSize)
                    highIndex = listSize;
                int outputIndex = 0;
                Integer highIndexInteger = new Integer(highIndex);
                Integer lowIndexInteger = new Integer(lowIndex);
                Integer outputIndexInteger = new Integer(outputIndex);
                Integer entityIndexInteger = new Integer(0);
                templateRoot.put("lowIndex", lowIndexInteger);
                templateRoot.put("highIndex", highIndexInteger);
                templateRoot.put("outputIndex", outputIndexInteger);
                templateRoot.put("entityIndex", outputIndexInteger);

                env.setVariable("lowIndex", FreeMarkerWorker.autoWrap(lowIndexInteger, env));
                env.setVariable("highIndex", FreeMarkerWorker.autoWrap(highIndexInteger, env));
                env.setVariable("outputIndex", FreeMarkerWorker.autoWrap(outputIndexInteger, env));
                env.setVariable("entityIndex", FreeMarkerWorker.autoWrap(outputIndexInteger, env));
                //if (Debug.infoOn()) Debug.logInfo( " viewIndex:" + viewIndex, "");
                //if (Debug.infoOn()) Debug.logInfo( " viewSize:" + viewSize, "");
                //if (Debug.infoOn()) Debug.logInfo("listSize(1):" + listSize , "");
                //if (Debug.infoOn()) Debug.logInfo("highIndex(1):" + highIndexInteger , "");
                //if (Debug.infoOn()) Debug.logInfo("lowIndex(1):" + lowIndexInteger , "");
                boolean inProgress = false;
                if (outputIndex < highIndex) {
                    inProgress = getNextMatchingEntity(templateRoot, delegator, env);
                }
                FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                if (inProgress) {
                    return TransformControl.EVALUATE_BODY;
                } else {
                    return TransformControl.SKIP_BODY;
                }
            }

            public int afterBody() throws TemplateModelException, IOException {
                FreeMarkerWorker.reloadValues(templateRoot, savedValues, env);
                List list = (List) templateRoot.get("globalNodeTrail");
                List subList = list.subList(0, list.size() - 1);
                templateRoot.put("globalNodeTrail", subList);
                env.setVariable("globalNodeTrail", FreeMarkerWorker.autoWrap(subList, env));

                int outputIndex = ((Integer) templateRoot.get("outputIndex")).intValue();
                int highIndex = ((Integer) templateRoot.get("highIndex")).intValue();
                Integer highIndexInteger = new Integer(highIndex);
                env.setVariable("highIndex", FreeMarkerWorker.autoWrap(highIndexInteger, env));
                //if (Debug.infoOn()) Debug.logInfo("highIndex(2):" + highIndexInteger , "");
                boolean inProgress = false;
                if (outputIndex < highIndex) {
                    inProgress = getNextMatchingEntity(templateRoot, delegator, env);
                }

                FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                if (inProgress) {
                    return TransformControl.REPEAT_EVALUATION;
                } else {
                    return TransformControl.END_EVALUATION;
                }
            }

            public void close() throws IOException {
                if (view == null) {
                    return;
                }
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                int outputIndex = ((Integer) templateRoot.get("outputIndex")).intValue();
                //if (Debug.infoOn()) Debug.logInfo("outputIndex(3):" + outputIndex , "");
                int highIndex = ((Integer) templateRoot.get("highIndex")).intValue();
                Integer highIndexInteger = new Integer(highIndex);
                env.setVariable("highIndex", FreeMarkerWorker.autoWrap(highIndexInteger, env));
                //if (Debug.infoOn()) Debug.logInfo("highIndex(3):" + highIndexInteger , "");
                if (outputIndex < highIndex) {
                    templateRoot.put("highIndex", new Integer(outputIndex));
                    //if (Debug.infoOn()) Debug.logInfo("highIndex(4):" + highIndex , "");
                    templateRoot.put("listSize", new Integer(outputIndex));
                }
                Object highIndexObj = FreeMarkerWorker.getWrappedObject("highIndex", env);
                //if (Debug.infoOn()) Debug.logInfo("highIndex(3b):" + highIndexObj , "");
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
                //if (Debug.infoOn()) Debug.logInfo("in LoopSubContent, wrappedContent:" + wrappedContent, module);
                //try {
                //Profiler.end("Loop");
                //FileOutputStream fw = new FileOutputStream(new File("/usr/local/agi/ofbiz/hot-deploy/sfmp/misc/profile.data"));
                //Profiler.print(fw);
                //fw.close();
                //} catch (IOException e) {
                //Debug.logError("[PROFILER] " + e.getMessage(),"");
                //}
            }
        };
    }
}

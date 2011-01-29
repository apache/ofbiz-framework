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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

//import com.clarkware.profiler.Profiler;
/**
 * TraverseSubContentCacheTransform - Freemarker Transform for URLs (links)
 */
public class TraverseSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = TraverseSubContentCacheTransform.class.getName();
    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
    public static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail"};

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
    public static String getArg(Map<String, Object> args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, Object> args, String key, Map<String, Object> ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        //final Map templateRoot = FreeMarkerWorker.getWrappedObject("context", env);
        final Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
        //FreeMarkerWorker.convertContext(templateRoot);
        final Map<String, Object> savedValuesUp = FastMap.newInstance();
        FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        final Map<String, Object> savedValues = FastMap.newInstance();
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);
        String startContentAssocTypeId = (String)templateRoot.get("contentAssocTypeId");
        // if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, startContentAssocTypeId:" + startContentAssocTypeId, module);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        List<Map<String, ? extends Object>> globalNodeTrail = UtilGenerics.checkList(templateRoot.get("globalNodeTrail"));
        // List globalNodeTrail = (List)templateRoot.get("globalNodeTrail");
        // String csvTrail = ContentWorker.nodeTrailToCsv(globalNodeTrail);
        // if (Debug.infoOn()) Debug.logInfo("in Traverse(0), csvTrail:"+csvTrail,module);
        String strNullThruDatesOnly = (String)templateRoot.get("nullThruDatesOnly");
        String contentAssocPredicateId = (String)templateRoot.get("contentAssocPredicateId");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE :Boolean.FALSE;
        try {
            // getCurrentContent puts the "current" node on the end of globalNodeTrail.
            // It may have already been there, but getCurrentContent will compare its contentId
            // to values in templateRoot.
            ContentWorker.getCurrentContent(delegator, globalNodeTrail, userLogin, templateRoot, nullThruDatesOnly, contentAssocPredicateId);
        } catch (GeneralException e) {
            throw new RuntimeException("Error getting current content. " + e.toString());
        }
        // final GenericValue view = val;


        final Map<String, Object> traverseContext = FastMap.newInstance();
        traverseContext.put("delegator", delegator);
        Map<String, Object> whenMap = FastMap.newInstance();
        whenMap.put("followWhen", templateRoot.get("followWhen"));
        whenMap.put("pickWhen", templateRoot.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", templateRoot.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", templateRoot.get("returnAfterPickWhen"));
        traverseContext.put("whenMap", whenMap);
        env.setVariable("whenMap", FreeMarkerWorker.autoWrap(whenMap, env));
        String fromDateStr = (String)templateRoot.get("fromDateStr");
        String thruDateStr = (String)templateRoot.get("thruDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        traverseContext.put("fromDate", fromDate);
        Timestamp thruDate = null;
        if (UtilValidate.isNotEmpty(thruDateStr)) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }
        traverseContext.put("thruDate", thruDate);
        //if (UtilValidate.isEmpty(startContentAssocTypeId)) {
            //throw new RuntimeException("contentAssocTypeId is empty.");
        //}
        traverseContext.put("contentAssocTypeId", startContentAssocTypeId);
        String direction = (String)templateRoot.get("direction");
        if (UtilValidate.isEmpty(direction)) {
            direction = "From";
        }
        traverseContext.put("direction", direction);

        return new LoopWriter(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
                //StringBuilder ctxBuf = (StringBuilder) templateContext.get("buf");
                //ctxBuf.append(cbuf, off, len);
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public int onStart() throws TemplateModelException, IOException {
                //templateContext.put("buf", new StringBuilder());
                List<Map<String, ? extends Object>> nodeTrail = null;
                Map<String, Object> node = null;
                // GenericValue subContentDataResourceView = null;
                List<Map<String, ? extends Object>> globalNodeTrail = UtilGenerics.checkList(templateRoot.get("globalNodeTrail"));
                // String trailCsv = ContentWorker.nodeTrailToCsv(globalNodeTrail);
                // if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, onStart, trailCsv(1):" + trailCsv , module);
                if (globalNodeTrail.size() > 0) {
                    int sz = globalNodeTrail.size() ;
                    nodeTrail = FastList.newInstance();
                    //nodeTrail = passedGlobalNodeTrail.subList(sz - 1, sz);
                    node = UtilGenerics.checkMap(globalNodeTrail.get(sz - 1));
                    //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, onStart, node(1):" + node , module);
                    Boolean checkedObj = (Boolean)node.get("checked");
                    Map<String, Object> whenMap = UtilGenerics.checkMap(templateRoot.get("whenMap"));
                    //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, whenMap(2):" + whenMap , module);
                    if (checkedObj == null || !checkedObj.booleanValue()) {
                        ContentWorker.checkConditions(delegator, node, null, whenMap);
                    }
                } else {
                    throw new IOException("Empty node trail entries");
                }

                Boolean isReturnBeforePickBool = (Boolean)node.get("isReturnBeforePick");
                if (isReturnBeforePickBool != null && isReturnBeforePickBool.booleanValue()) {
                    return TransformControl.SKIP_BODY;
                }   

                // GenericValue content = null;
                ContentWorker.selectKids(node, traverseContext);
                //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, onStart, node(2):" + node , module);
                nodeTrail.add(node);
                traverseContext.put("nodeTrail", nodeTrail);
                Boolean isPickBool = (Boolean)node.get("isPick");
                Boolean isFollowBool = (Boolean)node.get("isFollow");
                //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, onStart, isPickBool(1):" + isPickBool + " isFollowBool:" + isFollowBool, module);
                boolean isPick = true;
                if ((isPickBool == null || !isPickBool.booleanValue())
                   && (isFollowBool != null && isFollowBool.booleanValue())) {
                    isPick = ContentWorker.traverseSubContent(traverseContext);
                    //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, onStart, isPick(2):" + isPick, module);
                }
                if (isPick) {
                    populateContext(traverseContext, templateRoot);
                    FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                    return TransformControl.EVALUATE_BODY;
                } else {
                    return TransformControl.SKIP_BODY;
                }
            }

            @Override
            public int afterBody() throws TemplateModelException, IOException {
                FreeMarkerWorker.reloadValues(templateRoot, savedValues, env);
                // List globalNodeTrail = (List)templateRoot.get("globalNodeTrail");
                // if (Debug.infoOn()) Debug.logInfo("populateContext, globalNodeTrail(2a):" + FreeMarkerWorker.nodeTrailToCsv(globalNodeTrail), "");
                // List<Map<String, ? extends Object>> nodeTrail = UtilGenerics.checkList(traverseContext.get("nodeTrail"));
                // List savedGlobalNodeTrail = (List)savedValues.get("globalNodeTrail");
                // templateRoot.put("globalNodeTrail", savedGlobalNodeTrail);
                // int sz = nodeTrail.size();
                boolean inProgress = ContentWorker.traverseSubContent(traverseContext);
                if (inProgress) {
                    populateContext(traverseContext, templateRoot);
                    FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                    //globalNodeTrail = (List)templateRoot.get("globalNodeTrail");
                    //globalNodeTrail.addAll(nodeTrail);
                    return TransformControl.REPEAT_EVALUATION;
                } else {
                    return TransformControl.END_EVALUATION;
                }
            }

            @Override
            public void close() throws IOException {
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
                //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContent, wrappedContent:" + wrappedContent, module);
            }

            public void populateContext(Map<String, Object> traverseContext, Map<String, Object> templateContext) {
                List<Map<String, ? extends Object>> nodeTrail = UtilGenerics.checkList(traverseContext.get("nodeTrail"));
                // if (Debug.infoOn()) Debug.logInfo("populateContext, nodeTrail csv(a):" + FreeMarkerWorker.nodeTrailToCsv((List)nodeTrail), "");
                int sz = nodeTrail.size();
                Map<String, ? extends Object> node = nodeTrail.get(sz - 1);
                GenericValue content = (GenericValue)node.get("value");
                String contentId = (String)node.get("contentId");
                // String subContentId = (String)node.get("subContentId");
                String contentAssocTypeId = (String)node.get("contentAssocTypeId");
                envWrap("contentAssocTypeId", contentAssocTypeId);
                envWrap("contentId", contentId);
                envWrap("content", content);
                String mapKey = (String)node.get("mapKey");
                envWrap("mapKey", mapKey);
                envWrap("subContentDataResourceView", null);
                List<Map<String, ? extends Object>> globalNodeTrail = UtilGenerics.checkList(templateContext.get("nodeTrail"));
                String contentIdEnd = null;
                String contentIdStart = null;
                if (globalNodeTrail != null) {
                    Map<String, ? extends Object> ndEnd = globalNodeTrail.get(globalNodeTrail.size() - 1);
                    contentIdEnd = (String)ndEnd.get("contentId");
                    Map<String, ? extends Object> ndStart = nodeTrail.get(0);
                    contentIdStart = (String)ndStart.get("contentId");
                } else {
                    globalNodeTrail = FastList.newInstance();
                }
                //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, populateContext, contentIdEnd(1):" + contentIdEnd + " contentIdStart:" + contentIdStart + " equals:" + (contentIdStart.equals(contentIdEnd)), module);
                boolean bIdEnd = UtilValidate.isNotEmpty(contentIdEnd);
                boolean bIdStart = UtilValidate.isNotEmpty(contentIdStart);
                boolean bEquals = contentIdStart.equals(contentIdEnd);
                // if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, populateContext, contentIdEnd(1):" + bIdEnd + " contentIdStart:" + bIdStart + " equals:" + bEquals, module);
                // if (Debug.infoOn()) Debug.logInfo("populateContext, globalNodeTrail(1a):" + FreeMarkerWorker.nodeTrailToCsv(globalNodeTrail), "");
                if (bIdEnd && bIdStart && bEquals) {
                    List<Map<String, ? extends Object>> subList = nodeTrail.subList(1, sz);
                    globalNodeTrail.addAll(subList);
                } else {
                    globalNodeTrail.addAll(nodeTrail);
                }
                // if (Debug.infoOn()) Debug.logInfo("populateContext, globalNodeTrail(1b):" + FreeMarkerWorker.nodeTrailToCsv(globalNodeTrail), "");
                int indentSz = globalNodeTrail.size();
                envWrap("indent", Integer.valueOf(indentSz));
                String trailCsv = ContentWorker.nodeTrailToCsv(globalNodeTrail);
                //if (Debug.infoOn()) Debug.logInfo("in TraverseSubContentCache, populateCtx, trailCsv(2):" + trailCsv , module);
                envWrap("nodeTrailCsv", trailCsv);
                envWrap("globalNodeTrail", globalNodeTrail);
            }

            public void envWrap(String varName, Object obj) {
                templateRoot.put(varName, obj);
                env.setVariable(varName, FreeMarkerWorker.autoWrap(obj, env));
            }
        };
    }
}

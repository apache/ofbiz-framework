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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
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
 * LimitedSubContentCacheTransform - Freemarker Transform for URLs (links)
 */
public class LimitedSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = LimitedSubContentCacheTransform.class.getName();

    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
    public static final String [] saveKeyNames = {"contentId", "subContentId", "entityList", "entityIndex", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "entityList", "viewSize", "viewIndex", "highIndex", "lowIndex", "listSize", "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail", "outputIndex"};

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


    public Writer getWriter(final Writer out, Map args) {
        //Profiler.begin("Limited");
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
        final Map pickedEntityIds = new HashMap();
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

        String limitSize = (String) templateRoot.get("limitSize");
        final int returnLimit = Integer.parseInt(limitSize); 
        // limitMode will be "random" to begin with
        String limitMode = (String) templateRoot.get("limitMode");
        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        List globalNodeTrail = (List) templateRoot.get("globalNodeTrail");
        String strNullThruDatesOnly = (String) templateRoot.get("nullThruDatesOnly");
        String orderBy = (String) templateRoot.get("orderBy");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE : Boolean.FALSE;
        String contentId = (String) templateRoot.get("subContentId");

        templateRoot.put("contentId", null);
        templateRoot.put("subContentId", null);

        final String contentIdTo = contentId;

        Map results = null;
        //if (Debug.infoOn()) Debug.logInfo("in LimitedSubContentCache(0), assocTypes ." + assocTypes, module);
        String contentAssocPredicateId = (String) templateRoot.get("contentAssocPredicateId");
        try {
            results = ContentServicesComplex.getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, null, "From", fromDate, null, assocTypes, null, Boolean.TRUE, contentAssocPredicateId, orderBy);
        } catch (MiniLangException e2) {
            throw new RuntimeException(e2.getMessage());
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        }
        List longList = (List) results.get("entityList");
        templateRoot.put("entityList", longList);
        //if (Debug.infoOn()) Debug.logInfo("in limited, longList:" + longList , "");

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

                List globalNodeTrail = (List) templateRoot.get("globalNodeTrail");
                String trailCsv = ContentWorker.nodeTrailToCsv(globalNodeTrail);
                boolean inProgress = false;
                //if (Debug.infoOn()) Debug.logInfo("in limited, returnLimit:" + returnLimit , "");
                //if (Debug.infoOn()) Debug.logInfo("in limited, pickedEntityIds:" + pickedEntityIds , "");
                if (pickedEntityIds.size() < returnLimit) {
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
                
                //if (Debug.infoOn()) Debug.logInfo("highIndex(2):" + highIndexInteger , "");
                //if (Debug.infoOn()) Debug.logInfo("in limited, returnLimit(2):" + returnLimit , "");
                //if (Debug.verboseOn()) Debug.logVerbose("in limited, pickedEntityIds(2):" + pickedEntityIds , "");
                boolean inProgress = false;
                if (pickedEntityIds.size() < returnLimit) {
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
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
                //try {
                //Profiler.end("Limited");
                //FileOutputStream fw = new FileOutputStream(new File("/usr/local/agi/ofbiz/hot-deploy/sfmp/misc/profile.data"));
                //Profiler.print(fw);
                //fw.close();
                //} catch (IOException e) {
                //Debug.logError("[PROFILER] " + e.getMessage(),"");
                //}
            }

            public boolean prepCtx(GenericDelegator delegator, Map ctx, Environment env, GenericValue view) throws GeneralException {

                String dataResourceId = (String) view.get("drDataResourceId");
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


                Map trailNode = ContentWorker.makeNode(view);
                Map whenMap = (Map) ctx.get("whenMap");
                Locale locale = (Locale) ctx.get("locale");
                if (locale == null) {
                    locale = Locale.getDefault();
                }
                GenericValue assocContent = null;
                ContentWorker.checkConditions(delegator, trailNode, assocContent, whenMap);
                Boolean isReturnBeforeObj = (Boolean) trailNode.get("isReturnBefore");
                Boolean isReturnAfterObj = (Boolean) trailNode.get("isReturnAfter");
                Boolean isPickObj = (Boolean) trailNode.get("isPick");
                Boolean isFollowObj = (Boolean) trailNode.get("isFollow");
                //if (Debug.infoOn()) Debug.logInfo("in LimitedSubContentCache, isReturnBeforeObj" + isReturnBeforeObj + " isPickObj:" + isPickObj + " isFollowObj:" + isFollowObj + " isReturnAfterObj:" + isReturnAfterObj, module);
                if ((isReturnBeforeObj == null || !isReturnBeforeObj.booleanValue()) && ((isPickObj != null &&
                        isPickObj.booleanValue()) || (isFollowObj != null && isFollowObj.booleanValue()))) {
                    List globalNodeTrail = (List) ctx.get("globalNodeTrail");
                    if (globalNodeTrail == null) {
                        globalNodeTrail = new ArrayList();
                    }
                    globalNodeTrail.add(trailNode);
                    ctx.put("globalNodeTrail", globalNodeTrail);
                    String csvTrail = ContentWorker.nodeTrailToCsv(globalNodeTrail);
                    ctx.put("nodeTrailCsv", csvTrail);
                    //if (Debug.infoOn()) Debug.logInfo("prepCtx, csvTrail(2):" + csvTrail, "");
                    int indentSz = globalNodeTrail.size();
                    ctx.put("indent", new Integer(indentSz));

                    ctx.put("subDataResourceTypeId", subDataResourceTypeId);
                    ctx.put("mimeTypeId", mimeTypeId);
                    ctx.put("subContentId", subContentIdSub);
                    ctx.put("content", view);

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

            public GenericValue getRandomEntity() {

                GenericValue pickEntity = null;
                List lst = (List) templateRoot.get("entityList");
                if (Debug.verboseOn()) Debug.logVerbose("in limited, lst:" + lst, "");

                while (pickEntity == null && lst.size() > 0) {
                    double randomValue = Math.random();
                    //if (Debug.infoOn()) Debug.logInfo("in limited, randomValue:" + randomValue , "");
                    int idx = (int) (lst.size() * randomValue);
                    //if (Debug.infoOn()) Debug.logInfo("in limited, idx:" + idx , "");
                    pickEntity = (GenericValue) lst.get(idx);
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

            public boolean getNextMatchingEntity(Map templateRoot, GenericDelegator delegator, Environment env) throws IOException {
                boolean matchFound = false;
                GenericValue pickEntity = getRandomEntity();

                while (pickEntity != null && !matchFound) {
                    try {
                        matchFound = prepCtx(delegator, templateRoot, env, pickEntity);
                    } catch (GeneralException e) {
                        throw new IOException(e.getMessage());
                    }
                    if (!matchFound)
                        pickEntity = getRandomEntity();
                }
                return matchFound;
            }
        };
    }
}

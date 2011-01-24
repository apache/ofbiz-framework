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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.content.PermissionRecorder;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entityext.permission.EntityPermissionChecker;
import org.ofbiz.security.Security;
import org.ofbiz.service.ModelService;
import org.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * CheckPermissionTransform - Freemarker Transform for URLs (links)
 */
public class CheckPermissionTransform implements TemplateTransformModel {

    public static final String module = CheckPermissionTransform.class.getName();

    public static final String [] saveKeyNames = {"globalNodeTrail", "nodeTrail", "mode", "purposeTypeId", "statusId", "entityOperation", "targetOperation" };
    public static final String [] removeKeyNames = {};

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


    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        final Map<String, Object> templateCtx = FreeMarkerWorker.createEnvironmentMap(env);
        //FreeMarkerWorker.convertContext(templateCtx);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        FreeMarkerWorker.getSiteParameters(request, templateCtx);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final String mode = (String)templateCtx.get("mode");
        final String quickCheckContentId = (String)templateCtx.get("quickCheckContentId");
        final Map<String, Object> savedValues = FastMap.newInstance();
        //Debug.logInfo("in CheckPermission, contentId(1):" + templateCtx.get("contentId"),"");
        //Debug.logInfo("in CheckPermission, subContentId(1):" + templateCtx.get("subContentId"),"");

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
                List<Map<String, ? extends Object>> trail = UtilGenerics.checkList(templateCtx.get("globalNodeTrail"));
                //String trailCsv = ContentWorker.nodeTrailToCsv(trail);
                //Debug.logInfo("in CheckPermission, trailCsv(2):" + trailCsv,"");
                //Debug.logInfo("in CheckPermission, contentId(2):" + templateCtx.get("contentId"),"");
                //Debug.logInfo("in CheckPermission, subContentId(2):" + templateCtx.get("subContentId"),"");

                GenericValue currentContent = null;
                String contentAssocPredicateId = (String)templateCtx.get("contentAssocPredicateId");
                String strNullThruDatesOnly = (String)templateCtx.get("nullThruDatesOnly");
                Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE : Boolean.FALSE;
                GenericValue val = null;
                try {
                    val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
                } catch (GeneralException e) {
                    throw new RuntimeException("Error getting current content. " + e.toString());
                }
                // final GenericValue view = val;
                currentContent = val;
                if (currentContent != null) {
                    //Debug.logInfo("in CheckPermission, currentContent(0):" + currentContent.get("contentId"),"");
                }

                if (currentContent == null) {
                    currentContent = delegator.makeValue("Content");
                    currentContent.put("ownerContentId", templateCtx.get("ownerContentId"));
                }
                //Debug.logInfo("in CheckPermission, currentContent(1):" + currentContent.get("contentId"),"");

                Security security = null;
                if (request != null) {
                    security = (Security) request.getAttribute("security");
                }

                String statusId = (String)currentContent.get("statusId");
                String passedStatusId = (String)templateCtx.get("statusId");
                List<String> statusList = StringUtil.split(passedStatusId, "|");
                if (statusList == null) {
                    statusList = FastList.newInstance();
                }
                if (UtilValidate.isNotEmpty(statusId) && !statusList.contains(statusId)) {
                    statusList.add(statusId);
                }
                String targetPurpose = (String)templateCtx.get("contentPurposeList");
                List<String> purposeList = StringUtil.split(targetPurpose, "|");
                String entityOperation = (String)templateCtx.get("entityOperation");
                String targetOperation = (String)templateCtx.get("targetOperation");
                if (UtilValidate.isEmpty(targetOperation)) {
                    if (UtilValidate.isNotEmpty(entityOperation)) {
                        targetOperation = "CONTENT" + entityOperation;
                    }
                }
                List<String> targetOperationList = StringUtil.split(targetOperation, "|");
                if (targetOperationList.size() == 0) {
                    //Debug.logInfo("in CheckPermission, entityOperation:" + entityOperation,"");
                    //Debug.logInfo("in CheckPermission, templateCtx:" + templateCtx,"");
                    throw new IOException("targetOperationList has zero size.");
                }
                List<String> roleList = FastList.newInstance();

                String privilegeEnumId = (String)currentContent.get("privilegeEnumId");
                Map<String, Object> results = EntityPermissionChecker.checkPermission(currentContent, statusList, userLogin, purposeList, targetOperationList, roleList, delegator, security, entityOperation, privilegeEnumId, quickCheckContentId);

                boolean isError = ModelService.RESPOND_ERROR.equals(results.get(ModelService.RESPONSE_MESSAGE));
                if (isError) {
                    throw new IOException(ModelService.RESPONSE_MESSAGE);
                }

                String permissionStatus = (String) results.get("permissionStatus");

                if (UtilValidate.isEmpty(permissionStatus) || !permissionStatus.equals("granted")) {
                    String errorMessage = "Permission to add response is denied (2)";
                    PermissionRecorder recorder = (PermissionRecorder)results.get("permissionRecorder");
                        //Debug.logInfo("recorder(0):" + recorder, "");
                    if (recorder != null) {
                        String permissionMessage = recorder.toHtml();
                        //Debug.logInfo("permissionMessage(0):" + permissionMessage, "");
                        errorMessage += " \n " + permissionMessage;
                    }
                    templateCtx.put("permissionErrorMsg", errorMessage);
                }

                if (permissionStatus != null && permissionStatus.equalsIgnoreCase("granted")) {
                    FreeMarkerWorker.saveContextValues(templateCtx, saveKeyNames, savedValues);
                    if (mode == null || !mode.equalsIgnoreCase("not-equals"))
                        return TransformControl.EVALUATE_BODY;
                    else
                        return TransformControl.SKIP_BODY;
                } else {
                    if (mode == null || !mode.equalsIgnoreCase("not-equals"))
                        return TransformControl.SKIP_BODY;
                    else
                        return TransformControl.EVALUATE_BODY;
                }
            }

            @Override
            public void close() throws IOException {
                FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
            }
        };
    }
}

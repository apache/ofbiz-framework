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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.content.PermissionRecorder;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entityext.permission.EntityPermissionChecker;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * CheckPermissionTransform - Freemarker Transform for URLs (links)
 */
public class CheckPermissionTransform implements TemplateTransformModel {

    private static final String MODULE = CheckPermissionTransform.class.getName();

    static final String[] SAVE_KEY_NAMES = {"globalNodeTrail", "nodeTrail", "mode", "purposeTypeId", "statusId", "entityOperation",
            "targetOperation" };

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
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        final Map<String, Object> templateCtx = FreeMarkerWorker.createEnvironmentMap(env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        FreeMarkerWorker.getSiteParameters(request, templateCtx);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final String mode = (String) templateCtx.get("mode");
        final String quickCheckContentId = (String) templateCtx.get("quickCheckContentId");
        final Map<String, Object> savedValues = new HashMap<>();

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
                List<Map<String, ? extends Object>> trail = UtilGenerics.cast(templateCtx.get("globalNodeTrail"));
                GenericValue currentContent = null;
                String contentAssocPredicateId = (String) templateCtx.get("contentAssocPredicateId");
                String strNullThruDatesOnly = (String) templateCtx.get("nullThruDatesOnly");
                Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && "true".equalsIgnoreCase(strNullThruDatesOnly))
                        ? Boolean.TRUE : Boolean.FALSE;
                GenericValue val = null;
                try {
                    val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
                } catch (GeneralException e) {
                    throw new RuntimeException("Error getting current content. " + e.toString());
                }
                currentContent = val;

                if (currentContent == null) {
                    currentContent = delegator.makeValue("Content");
                    currentContent.put("ownerContentId", templateCtx.get("ownerContentId"));
                }

                Security security = null;
                if (request != null) {
                    security = (Security) request.getAttribute("security");
                }

                String statusId = (String) currentContent.get("statusId");
                String passedStatusId = (String) templateCtx.get("statusId");
                List<String> statusList = StringUtil.split(passedStatusId, "|");
                if (statusList == null) {
                    statusList = new LinkedList<>();
                }
                if (UtilValidate.isNotEmpty(statusId) && !statusList.contains(statusId)) {
                    statusList.add(statusId);
                }
                String targetPurpose = (String) templateCtx.get("contentPurposeList");
                List<String> purposeList = StringUtil.split(targetPurpose, "|");
                String entityOperation = (String) templateCtx.get("entityOperation");
                String targetOperation = (String) templateCtx.get("targetOperation");
                if (UtilValidate.isEmpty(targetOperation)) {
                    if (UtilValidate.isNotEmpty(entityOperation)) {
                        targetOperation = "CONTENT" + entityOperation;
                    }
                }
                List<String> targetOperationList = StringUtil.split(targetOperation, "|");
                if (targetOperationList.isEmpty()) {
                    throw new IOException("targetOperationList has zero size.");
                }
                List<String> roleList = new LinkedList<>();

                String privilegeEnumId = (String) currentContent.get("privilegeEnumId");
                Map<String, Object> results = EntityPermissionChecker.checkPermission(currentContent, statusList, userLogin, purposeList,
                        targetOperationList, roleList, delegator, security, entityOperation, privilegeEnumId, quickCheckContentId);

                boolean isError = ModelService.RESPOND_ERROR.equals(results.get(ModelService.RESPONSE_MESSAGE));
                if (isError) {
                    throw new IOException(ModelService.RESPONSE_MESSAGE);
                }

                String permissionStatus = (String) results.get("permissionStatus");

                if (UtilValidate.isEmpty(permissionStatus) || !"granted".equals(permissionStatus)) {
                    String errorMessage = "Permission to add response is denied (2)";
                    PermissionRecorder recorder = (PermissionRecorder) results.get("permissionRecorder");
                    if (recorder != null) {
                        String permissionMessage = recorder.toHtml();
                        errorMessage += " \n " + permissionMessage;
                    }
                    templateCtx.put("permissionErrorMsg", errorMessage);
                }

                if (permissionStatus != null && "granted".equalsIgnoreCase(permissionStatus)) {
                    FreeMarkerWorker.saveContextValues(templateCtx, SAVE_KEY_NAMES, savedValues);
                    if (mode == null || !"not-equals".equalsIgnoreCase(mode)) {
                        return TransformControl.EVALUATE_BODY;
                    }
                    return TransformControl.SKIP_BODY;
                }
                if (mode == null || !"not-equals".equalsIgnoreCase(mode)) {
                    return TransformControl.SKIP_BODY;
                }
                return TransformControl.EVALUATE_BODY;
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

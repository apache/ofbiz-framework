/*
 * $Id: CheckPermissionTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 * 
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *  
 */
package org.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.content.PermissionRecorder;
import org.ofbiz.entity.GenericDelegator;
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
 * 
 * @author <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @version $Rev$
 * @since 3.0
 */
public class CheckPermissionTransform implements TemplateTransformModel {

    public static final String module = CheckPermissionTransform.class.getName();

    public static final String [] saveKeyNames = {"globalNodeTrail", "nodeTrail", "mode", "purposeTypeId", "statusId", "entityOperation", "targetOperation" };
    public static final String [] removeKeyNames = {};

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
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        final Map templateCtx = FreeMarkerWorker.createEnvironmentMap(env);
        //FreeMarkerWorker.convertContext(templateCtx);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        FreeMarkerWorker.getSiteParameters(request, templateCtx);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final String mode = (String)templateCtx.get("mode");
        final String quickCheckContentId = (String)templateCtx.get("quickCheckContentId");
        final Map savedValues = new HashMap();
                    //Debug.logInfo("in CheckPermission, contentId(1):" + templateCtx.get("contentId"),"");
                    //Debug.logInfo("in CheckPermission, subContentId(1):" + templateCtx.get("subContentId"),"");

        return new LoopWriter(out) {

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public int onStart() throws TemplateModelException, IOException {
                List trail = (List)templateCtx.get("globalNodeTrail");
                String trailCsv = ContentWorker.nodeTrailToCsv(trail);
                    //Debug.logInfo("in CheckPermission, trailCsv(2):" + trailCsv,"");
                    //Debug.logInfo("in CheckPermission, contentId(2):" + templateCtx.get("contentId"),"");
                    //Debug.logInfo("in CheckPermission, subContentId(2):" + templateCtx.get("subContentId"),"");
             
                GenericValue currentContent = null;
        String contentAssocPredicateId = (String)templateCtx.get("contentAssocPredicateId");
                String strNullThruDatesOnly = (String)templateCtx.get("nullThruDatesOnly");
                Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? new Boolean(true) :new Boolean(false);
                GenericValue val = null;
                try {
                    val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
                } catch(GeneralException e) {
                    throw new RuntimeException("Error getting current content. " + e.toString());
                }
                final GenericValue view = val;
                currentContent = val;
                if (currentContent != null) {
                    //Debug.logInfo("in CheckPermission, currentContent(0):" + currentContent.get("contentId"),"");
                }

                if (currentContent == null) {
                    currentContent = delegator.makeValue("Content", null);
                    currentContent.put("ownerContentId", templateCtx.get("ownerContentId"));
                }
                    //Debug.logInfo("in CheckPermission, currentContent(1):" + currentContent.get("contentId"),"");
        
                Security security = null;
                if (request != null) {
                    security = (Security) request.getAttribute("security");
                }
             
                String statusId = (String)currentContent.get("statusId");
                String passedStatusId = (String)templateCtx.get("statusId");
                List statusList = StringUtil.split(passedStatusId, "|");
                if (statusList == null)
                    statusList = new ArrayList();
                if (UtilValidate.isNotEmpty(statusId) && !statusList.contains(statusId)) {
                    statusList.add(statusId);
                } 
                String targetPurpose = (String)templateCtx.get("contentPurposeList");
                List purposeList = StringUtil.split(targetPurpose, "|");
                String entityOperation = (String)templateCtx.get("entityOperation");
                String targetOperation = (String)templateCtx.get("targetOperation");
                if (UtilValidate.isEmpty(targetOperation)) {
                    if (UtilValidate.isNotEmpty(entityOperation))
                        targetOperation = "CONTENT" + entityOperation;
                }
                List targetOperationList = StringUtil.split(targetOperation, "|");
                if (targetOperationList.size() == 0) {
                    //Debug.logInfo("in CheckPermission, entityOperation:" + entityOperation,"");
                    //Debug.logInfo("in CheckPermission, templateCtx:" + templateCtx,"");
                    throw new IOException("targetOperationList has zero size.");
                }
                List roleList = new ArrayList();
        
                String privilegeEnumId = (String)currentContent.get("privilegeEnumId");
                Map results = EntityPermissionChecker.checkPermission(currentContent, statusList, userLogin, purposeList, targetOperationList, roleList, delegator, security, entityOperation, privilegeEnumId, quickCheckContentId); 

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


            public void close() throws IOException {
                FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
            }
        };
    }
}

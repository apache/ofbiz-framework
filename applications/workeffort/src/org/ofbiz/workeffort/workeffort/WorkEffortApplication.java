/*
 * $Id: WorkEffortApplication.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.workeffort.workeffort;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfUtil;

import bsh.EvalError;

/**
 * WorkEffortWorker - Worker class to reduce code in JSPs & make it more reusable
 *
 * @author     Manuel Soto
 * @version    $Rev$
 * @since      2.0
 */
public final class WorkEffortApplication {

    public static final String module = WorkEffortApplication.class.getName();

    /** 
     * Determine if the workeffort is an activity and has an Application as the implementation
     * @param pageContext
     * @param workEffort The work effort to be examined
     * @return
     */
    public static boolean isApplication(GenericValue workEffort) {

        if (!workEffort.get("workEffortTypeId").equals("ACTIVITY"))
            return false;
        // check for the existence of applications
        try {
            if (getApplications(workEffort).isEmpty())
                return false;
        } catch (GenericEntityException ee) {
            Debug.logWarning(ee, module);
            return false;
        }

        return true;

    }

    /** 
     * Determine if the workeffort is an activity and has an Application as the implementation
     * @param workEffortAttrName The work effort to be examined. Located in the pageContext
     * @return
     */
    public static void isApplication(
        PageContext pageContext,
        String workEffortAttrName,
        String isApplicationAttrName) {

        pageContext.setAttribute(
            isApplicationAttrName,
            new Boolean(isApplication((GenericValue) pageContext.getAttribute(workEffortAttrName))));

    }

    public static String getApplicationId(PageContext pageContext, String partyAssignsAttrName) {
        Collection partyAssigns = (Collection) pageContext.getAttribute(partyAssignsAttrName);
        if (!partyAssigns.isEmpty()) {
            // look for acepted
            GenericValue partyAssignAcepted = null;
            for (Iterator partyAssignIt = partyAssigns.iterator(); partyAssignIt.hasNext();) {
                GenericValue partyAssign = (GenericValue) partyAssignIt.next();
                if (((String) partyAssign.get("statusId")).equals("CAL_ACCEPTED")) {
                    partyAssignAcepted = partyAssign;
                    break;
                }
            }
            if (partyAssignAcepted != null)
                try {
                    final Collection applicationSandboxs = partyAssignAcepted.getRelated("ApplicationSandbox");
                    GenericValue applicationSandbox = (GenericValue) applicationSandboxs.toArray()[0];
                    return (String) applicationSandbox.get("applicationId");
                } catch (GenericEntityException ee) {
                    Debug.logWarning(ee, module);
                }
        }
        Debug.logWarning("Can't find applicationId", module);
        return null;
    }

    /** 
     * Retrieve the information of the application in the implementation of the activity ans save in the page context
     * It asume that an activity has at least one application
     *
     * @param pageContext The page context where the information is to be saved
     * @param workEffortAttrName The attribute name to localte the workeffort in the pageCOntext
     * @param applicationAttrName The name of the attribute where the application's specification will be savd in the page context
     * @throws WfException
     * @see getApplication
     */
    public static void getApplication(PageContext pageContext, String workEffortAttrName, String applicationAttrName)
            throws WfException {
        getApplication(pageContext, workEffortAttrName, applicationAttrName, null, null, null);
    }

    /** 
     * Retrieve the information of the application in the implementation of the activity ans save in the page context
     * It asume that an activity has at least one application
     *
     * @param pageContext The page context where the information is to be saved
     * @param workEffortAttrName The attribute name to localte the workeffort in the pageContext
     * @param applicationAttrName The name of the attribute where the application's specification will be savd in the page context
     * @param applicationContextSignatureName The name of the attribute where the application's context Signature specification will be savd in the page context
     * @param applicationResultSignatureName The name of the attribute where the application's result Signature specification will be saved in the page context
     * @param applicationContextName The name of the attribute where the application's context  will be savd in the page context
     * @throws WfException
     */
    public static void getApplication(PageContext pageContext, String workEffortAttrName, String applicationAttrName,            
            String applicationContextSignatureName, String applicationResultSignatureName, String applicationContextName)
            throws WfException {
        GenericValue workEffort = (GenericValue) pageContext.getAttribute(workEffortAttrName);
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");
        try {
            Collection applications = getApplications(workEffort);

            if (applications.iterator().hasNext()) {
                GenericValue workflowActivityTool = (GenericValue) applications.iterator().next();
                if (applicationAttrName != null && applicationAttrName.length() != 0)
                    pageContext.setAttribute(applicationAttrName, workflowActivityTool);

                Map contextSignature = new HashMap();
                Map resultSignature = new HashMap();
                getApplicationSignatures(delegator, workflowActivityTool, contextSignature, resultSignature);

                Map context =
                    getApplicationContext(
                        delegator,
                        contextSignature,
                        (String) workflowActivityTool.get("actualParameters"),
                        (String) workflowActivityTool.get("extendedAttributes"),
                        getContext(workEffort),
                        (String) workEffort.get("workEffortId"));

                if (applicationContextSignatureName != null
                    && applicationContextSignatureName.length() != 0
                    && contextSignature != null)
                    pageContext.setAttribute(applicationContextSignatureName, contextSignature);

                if (applicationResultSignatureName != null
                    && applicationResultSignatureName.length() != 0
                    && resultSignature != null)
                    pageContext.setAttribute(applicationResultSignatureName, resultSignature);

                if (applicationContextName != null && applicationContextName.length() != 0)
                    pageContext.setAttribute(applicationContextName, context);
            }

        } catch (GenericEntityException gex) {
            throw new WfException(
                "Can't find Applications in Workeffort [" + workEffort.get("workEffortId") + "]",
                gex);
        }

    }

    private static Collection getApplications(GenericValue workEffort) throws GenericEntityException {

        final String packageId = (String) workEffort.get("workflowPackageId");
        final String packageVersion = (String) workEffort.get("workflowPackageVersion");
        final String processId = (String) workEffort.get("workflowProcessId");
        final String processVersion = (String) workEffort.get("workflowProcessVersion");
        final String activityId = (String) workEffort.get("workflowActivityId");
        final GenericDelegator delegator = workEffort.getDelegator();

        Map expresions = new HashMap();
        expresions.putAll(UtilMisc.toMap("packageId", packageId));
        expresions.putAll(UtilMisc.toMap("packageVersion", packageVersion));
        expresions.putAll(UtilMisc.toMap("processId", processId));
        expresions.putAll(UtilMisc.toMap("processVersion", processVersion));
        expresions.putAll(UtilMisc.toMap("activityId", activityId));
        expresions.putAll(UtilMisc.toMap("toolTypeEnumId", "WTT_APPLICATION"));

        return delegator.findByAnd("WorkflowActivityTool", expresions);

    }

    private static void getApplicationSignatures(
            GenericDelegator delegator,
            GenericValue application,
            Map contextSignature,
            Map resultSignature)
            throws GenericEntityException {
        final String packageId = (String) application.get("packageId");
        final String packageVersion = (String) application.get("packageVersion");
        final String processId = (String) application.get("processId");
        final String processVersion = (String) application.get("processVersion");
        final String applicationId = (String) application.get("toolId");

        Map expresions = new HashMap();
        expresions.putAll(UtilMisc.toMap("packageId", packageId));
        expresions.putAll(UtilMisc.toMap("packageVersion", packageVersion));
        expresions.putAll(UtilMisc.toMap("processId", processId));
        expresions.putAll(UtilMisc.toMap("processVersion", processVersion));
        expresions.putAll(UtilMisc.toMap("applicationId", applicationId));

        final Collection params = delegator.findByAnd("WorkflowFormalParam", expresions);

        Iterator i = params.iterator();
        while (i.hasNext()) {
            GenericValue param = (GenericValue) i.next();
            String name = param.getString("formalParamId");
            String mode = param.getString("modeEnumId");
            String type = param.getString("dataTypeEnumId");
            if (mode.equals("WPM_IN") || mode.equals("WPM_INOUT"))
                contextSignature.put(name, WfUtil.getJavaType(type));
            else if (mode.equals("WPM_OUT") || mode.equals("WPM_INOUT"))
                resultSignature.put(name, WfUtil.getJavaType(type));
        }
    }

    private static Map getApplicationContext(
            GenericDelegator delegator,
            Map contextSignature,
            String actualParameters,
            String extendedAttr,
            Map context,
            String workEffortId)
            throws WfException {
        List params = StringUtil.split(actualParameters, ",");
        Map actualContext = new HashMap();

        Map extendedAttributes = StringUtil.strToMap(extendedAttr);
        if (extendedAttributes != null && extendedAttributes.size() > 0)
            actualContext.putAll(extendedAttributes);

        // setup some internal buffer parameters
        // maintain synchronized w/ wfActivityImpl
        GenericValue userLogin = null;
        if (context.containsKey("runAsUser")) {
            userLogin = getUserLogin(delegator, (String) context.get("runAsUser"));
            actualContext.put("userLogin", userLogin);
        } else if (context.containsKey("workflowOwnerId")) {
            userLogin = getUserLogin(delegator, (String) context.get("workflowOwnerId"));
        }

        context.put("userLogin", userLogin);
        context.put("workEffortId", workEffortId);
        // /setup some internal bufer parameters

        Iterator i = params.iterator();
        while (i.hasNext()) {

            Object keyExpr = i.next();
            String keyExprStr = (String) keyExpr;
            if (keyExprStr != null && keyExprStr.trim().toLowerCase().startsWith("expr:"))
                try {
                    BshUtil.eval(keyExprStr.trim().substring(5).trim(), context);
                } catch (EvalError e) {
                    throw new WfException("Error evaluating actual parameter: " + keyExprStr, e);
                } else if (keyExprStr != null && keyExprStr.trim().toLowerCase().startsWith("name:")) {
                List couple = StringUtil.split(keyExprStr.trim().substring(5).trim(), "=");
                if (contextSignature.containsKey(((String) couple.get(0)).trim()))
                    actualContext.put(((String) couple.get(0)).trim(), context.get(couple.get(1)));
            } else if (context.containsKey(keyExprStr)) {
                if (contextSignature.containsKey(keyExprStr))
                    actualContext.put(keyExprStr, context.get(keyExprStr));
            } else if (!actualContext.containsKey(keyExprStr))
                throw new WfException("Context does not contain the key: '" + keyExprStr + "'");
        }

        return actualContext;

    }

    private static Map getContext(GenericValue dataObject) throws WfException {
        String contextXML = null;
        Map context = null;
        if (dataObject.get("runtimeDataId") == null)
            return context;
        try {
            GenericValue runtimeData = dataObject.getRelatedOne("RuntimeData");
            contextXML = runtimeData.getString("runtimeInfo");
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        // De-serialize the context
        if (contextXML != null) {
            try {
                context = (Map) XmlSerializer.deserialize(contextXML, dataObject.getDelegator());
            } catch (SerializeException e) {
                throw new WfException(e.getMessage(), e);
            } catch (IOException e) {
                throw new WfException(e.getMessage(), e);
            } catch (Exception e) {
                throw new WfException(e.getMessage(), e);
            }
        }
        return context;
    }
    // Gets a UserLogin object for service invocation
    // This allows a workflow to invoke a service as a specific user
    private static GenericValue getUserLogin(GenericDelegator delegator, String userId) throws WfException {
        GenericValue userLogin = null;
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userId));
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        return userLogin;
    }

}

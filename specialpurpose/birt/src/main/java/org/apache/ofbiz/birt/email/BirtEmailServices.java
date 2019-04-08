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
package org.apache.ofbiz.birt.email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.fop.apps.FOPException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.birt.BirtFactory;
import org.apache.ofbiz.birt.BirtWorker;
import org.apache.ofbiz.common.email.NotificationServices;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;

public class BirtEmailServices {

    public static final String module = BirtEmailServices.class.getName();
    public static final String resource = "BirtUiLabels";
    /**
     * send birt mail
     *
     * @param ctx the dispatch context
     * @param context the context
     * @return returns the result of the service execution
     */
    public static Map<String, Object> sendBirtMail(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> serviceContext = UtilMisc.makeMapWritable(context);
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Security security = ctx.getSecurity();
        
        String webSiteId = (String) serviceContext.remove("webSiteId");
        String bodyText = (String) serviceContext.remove("bodyText");
        String bodyScreenUri = (String) serviceContext.remove("bodyScreenUri");
        String birtReportLocation = (String) serviceContext.remove("birtReportLocation");
        String attachmentName = (String) serviceContext.remove("attachmentName");
        Locale locale = (Locale) serviceContext.get("locale");
        Map<String, Object> bodyParameters = UtilGenerics.cast(serviceContext.remove("bodyParameters"));
        Locale birtLocale = (Locale) serviceContext.remove(BirtWorker.getBirtLocale());
        Map<String, Object> birtParameters = UtilGenerics.cast(serviceContext.remove(BirtWorker.getBirtParameters()));
        String birtImageDirectory = (String) serviceContext.remove(BirtWorker.getBirtImageDirectory());
        String birtContentType = (String) serviceContext.remove(BirtWorker.getBirtContentType());
        if (bodyParameters == null) {
            bodyParameters = MapStack.create();
        }
        if (!bodyParameters.containsKey("locale")) {
            bodyParameters.put("locale", locale);
        } else {
            locale = (Locale) bodyParameters.get("locale");
        }
        String partyId = (String) bodyParameters.get("partyId");
        if (UtilValidate.isNotEmpty(webSiteId)) {
            NotificationServices.setBaseUrl(ctx.getDelegator(), webSiteId, bodyParameters);
        }
        String contentType = (String) serviceContext.remove("contentType");

        if (UtilValidate.isEmpty(attachmentName)) {
            attachmentName = "Details.pdf";
        }
        StringWriter bodyWriter = new StringWriter();

        MapStack<String> screenContext = MapStack.create();
        screenContext.put("locale", locale);
        ScreenStringRenderer screenStringRenderer = null;
        try {
            screenStringRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", "screen.name", delegator),
                    EntityUtilProperties.getPropertyValue("widget", "screen.screenrenderer", delegator));
        } catch (TemplateException e) {
            String errMsg =  UtilProperties.getMessage(resource, "BirtErrorRenderingScreenForEmail", UtilMisc.toMap("errorString", e.toString()), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (IOException e) {
            String errMsg = UtilProperties.getMessage(resource, "BirtErrorRenderingScreenForEmail", UtilMisc.toMap("errorString", e.toString()), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        ScreenRenderer screens = new ScreenRenderer(bodyWriter, screenContext, screenStringRenderer);
        screens.populateContextForService(ctx, bodyParameters);
        screenContext.putAll(bodyParameters);

        if (bodyScreenUri != null) {
            try {
                screens.render(bodyScreenUri);
            } catch (GeneralException e) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorRenderingScreenForEmail", UtilMisc.toMap("errorString", e.toString()), locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (IOException e) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorIORenderingScreenForEmail", UtilMisc.toMap("errorString", e.toString()), locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (SAXException e) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorSAXRenderingScreenForEmail", UtilMisc.toMap("errorString", e.toString()), locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (ParserConfigurationException e) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorParserConfigRenderingScreenForEmail", UtilMisc.toMap("errorString", e.toString()), locale);
                Debug.logError(e, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        }

        boolean isMultiPart = false;

        // check if attachment screen location passed in
        if (UtilValidate.isNotEmpty(birtReportLocation)) {
            isMultiPart = true;
            // start processing fo pdf attachment
            try {
                // create the output stream for the generation
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                Map<String, Object> birtContext = new HashMap<String, Object>();
                if (birtLocale == null) {
                    birtLocale = locale;
                }
                birtContext.put(BirtWorker.getBirtLocale(), birtLocale);
                if (birtParameters != null) {
                    birtContext.put(BirtWorker.getBirtParameters(), birtParameters);
                }
                if (birtImageDirectory != null) {
                    birtContext.put(BirtWorker.getBirtImageDirectory(), birtImageDirectory);
                }
                if (birtContentType == null) {
                    birtContentType = "application/pdf";
                }
                IReportEngine engine = BirtFactory.getReportEngine();
                Map<String, Object> appContext = UtilGenerics.cast(engine.getConfig().getAppContext());
                appContext.put("delegator", delegator);
                appContext.put("dispatcher", dispatcher);
                appContext.put("security", security);
                
                InputStream reportInputStream = BirtFactory.getReportInputStreamFromLocation(birtReportLocation);
                IReportRunnable design = engine.openReportDesign(reportInputStream);
                Debug.logInfo("Export report as content type:" + birtContentType, module);
                BirtWorker.exportReport(design, context, birtContentType, baos);
                baos.flush();
                baos.close();

                // store in the list of maps for sendmail....
                List<Map<String, ? extends Object>> bodyParts = new LinkedList<Map<String,? extends Object>>();
                if (bodyText != null) {
                    bodyText = FlexibleStringExpander.expandString(bodyText, screenContext,  locale);
                    bodyParts.add(UtilMisc.toMap("content", bodyText, "type", "text/html"));
                } else {
                    bodyParts.add(UtilMisc.toMap("content", bodyWriter.toString(), "type", "text/html"));
                }
                bodyParts.add(UtilMisc.toMap("content", baos.toByteArray(), "type", "application/pdf", "filename", attachmentName));
                serviceContext.put("bodyParts", bodyParts);
            } catch (GeneralException ge) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorRenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", ge.toString()), locale);
                Debug.logError(ge, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (IOException ie) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorIORenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", ie.toString()), locale);
                Debug.logError(ie, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (FOPException fe) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorFOPRenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", fe.toString()), locale);
                Debug.logError(fe, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (SAXException se) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorSAXRenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", se.toString()), locale);
                Debug.logError(se, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (ParserConfigurationException pe) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorParserRenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", pe.toString()), locale);
                Debug.logError(pe, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (EngineException ee) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorRenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", ee.toString()), locale);
                Debug.logError(ee, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } catch (SQLException se) {
                String errMsg = UtilProperties.getMessage(resource, "BirtErrorSQLRenderingAttachmentForEmail", UtilMisc.toMap("birtContentType", birtContentType, "errorString", se.toString()), locale);
                Debug.logError(se, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }
        } else {
            isMultiPart = false;
            // store body and type for single part message in the context.
            if (bodyText != null) {
                bodyText = FlexibleStringExpander.expandString(bodyText, screenContext,  locale);
                serviceContext.put("body", bodyText);
            } else {
                serviceContext.put("body", bodyWriter.toString());
            }

            // Only override the default contentType in case of plaintext, since other contentTypes may be multipart
            //    and would require specific handling.
            if (contentType != null && contentType.equalsIgnoreCase("text/plain")) {
                serviceContext.put("contentType", "text/plain");
            } else {
                serviceContext.put("contentType", "text/html");
            }
        }

        // also expand the subject at this point, just in case it has the FlexibleStringExpander syntax in it...
        String subject = (String) serviceContext.remove("subject");
        subject = FlexibleStringExpander.expandString(subject, screenContext, locale);
        serviceContext.put("subject", subject);
        serviceContext.put("partyId", partyId);

        if (Debug.verboseOn()) Debug.logVerbose("sendMailFromScreen sendMail context: " + serviceContext, module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            if (isMultiPart) {
                dispatcher.runSync("sendMailMultiPart", serviceContext);
            } else {
                dispatcher.runSync("sendMail", serviceContext);
            }
        } catch (GenericServiceException e) {
            String errMsg = UtilProperties.getMessage(resource, "BirtErrorInSendingEmail", UtilMisc.toMap("errorString", e.toString()), locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        result.put("body", bodyWriter.toString());
        return result;
    }
}

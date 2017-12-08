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
package org.apache.ofbiz.widget;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.ofbiz.webapp.taglib.ContentUrlTag;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.jsoup.parser.Parser;

public final class WidgetWorker {

    public static final String module = WidgetWorker.class.getName();

    private WidgetWorker () {}

    public static void buildHyperlinkUrl(Appendable externalWriter, String target, String targetType, Map<String, String> parameterMap,
            String prefix, boolean fullPath, boolean secure, boolean encode, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        // We may get an encoded request like: &#47;projectmgr&#47;control&#47;EditTaskContents&#63;workEffortId&#61;10003
        // Try to reducing a possibly encoded string down to its simplest form: /projectmgr/control/EditTaskContents?workEffortId=10003
        // This step make sure the following appending externalLoginKey operation to work correctly
        String localRequestName = Parser.unescapeEntities(target, true);
        localRequestName = UtilHttp.encodeAmpersands(localRequestName);

        Appendable localWriter = new StringWriter();

        if ("intra-app".equals(targetType)) {
            if (request != null && response != null) {
                ServletContext servletContext = request.getSession().getServletContext();
                RequestHandler rh = (RequestHandler) servletContext.getAttribute("_REQUEST_HANDLER_");
                externalWriter.append(rh.makeLink(request, response, "/" + localRequestName, fullPath, secure, encode));
            } else if (prefix != null) {
                externalWriter.append(prefix);
                externalWriter.append(localRequestName);
            } else {
                externalWriter.append(localRequestName);
            }
        } else if ("inter-app".equals(targetType)) {
            String fullTarget = localRequestName;
            localWriter.append(fullTarget);
            String externalLoginKey = (String) request.getAttribute("externalLoginKey");
            if (UtilValidate.isNotEmpty(externalLoginKey)) {
                if (fullTarget.indexOf('?') == -1) {
                    localWriter.append('?');
                } else {
                    localWriter.append("&amp;");
                }
                localWriter.append("externalLoginKey=");
                localWriter.append(externalLoginKey);
            }
        } else if ("content".equals(targetType)) {
            appendContentUrl(localWriter, localRequestName, request);
        } else {
            localWriter.append(localRequestName);
        }

        if (UtilValidate.isNotEmpty(parameterMap)) {
            String localUrl = localWriter.toString();
            externalWriter.append(localUrl);
            boolean needsAmp = true;
            if (localUrl.indexOf('?') == -1) {
                externalWriter.append('?');
                needsAmp = false;
            }

            for (Map.Entry<String, String> parameter: parameterMap.entrySet()) {
                String parameterValue = null;
                if (parameter.getValue() instanceof String) {
                    parameterValue = parameter.getValue();
                } else {
                    Object parameterObject = parameter.getValue();

                    // skip null values
                    if (parameterObject == null) continue;

                    if (parameterObject instanceof String[]) {
                        // it's probably a String[], just get the first value
                        String[] parameterArray = (String[]) parameterObject;
                        parameterValue = parameterArray[0];
                        Debug.logInfo("Found String array value for parameter [" + parameter.getKey() + "], using first value: " + parameterValue, module);
                    } else {
                        // not a String, and not a String[], just use toString
                        parameterValue = parameterObject.toString();
                    }
                }

                if (needsAmp) {
                    externalWriter.append("&amp;");
                } else {
                    needsAmp = true;
                }
                externalWriter.append(parameter.getKey());
                externalWriter.append('=');
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null && parameterValue != null) {
                    externalWriter.append(simpleEncoder.encode(URLEncoder.encode(parameterValue, "UTF-8")));
                } else {
                    externalWriter.append(parameterValue);
                }
            }
        } else {
            externalWriter.append(localWriter.toString());
        }
    }

    public static void appendContentUrl(Appendable writer, String location, HttpServletRequest request) throws IOException {
        StringBuilder buffer = new StringBuilder();
        ContentUrlTag.appendContentPrefix(request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }
    public static void makeHyperlinkByType(Appendable writer, String linkType, String linkStyle, String targetType, String target,
            Map<String, String> parameterMap, String description, String targetWindow, String confirmation, ModelFormField modelFormField,
            HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        if (modelFormField == null) {
            throw new IllegalArgumentException("modelFormField in WidgetWorker.makeHyperlinkByType has turned out to be null");
        }
        String realLinkType = WidgetWorker.determineAutoLinkType(linkType, target, targetType, request);
        if ("hidden-form".equals(realLinkType)) {
            if ("multi".equals(modelFormField.getModelForm().getType())) {
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, confirmation, modelFormField, request, response, context);

                // this is a bit trickier, since we can't do a nested form we'll have to put the link to submit the form in place, but put the actual form def elsewhere, ie after the big form is closed
                Map<String, Object> wholeFormContext = UtilGenerics.checkMap(context.get("wholeFormContext"));
                Appendable postMultiFormWriter = wholeFormContext != null ? (Appendable) wholeFormContext.get("postMultiFormWriter") : null;
                if (postMultiFormWriter == null) {
                    postMultiFormWriter = new StringWriter();
                }
                WidgetWorker.makeHiddenFormLinkForm(postMultiFormWriter, target, targetType, targetWindow, parameterMap, modelFormField, request, response, context);
            } else {
                WidgetWorker.makeHiddenFormLinkForm(writer, target, targetType, targetWindow, parameterMap, modelFormField, request, response, context);
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, confirmation, modelFormField, request, response, context);
            }
        } else {
            WidgetWorker.makeHyperlinkString(writer, linkStyle, targetType, target, parameterMap, description, confirmation, modelFormField, request, response, context, targetWindow);
        }

    }
    public static void makeHyperlinkString(Appendable writer, String linkStyle, String targetType, String target, Map<String, String> parameterMap,
            String description, String confirmation, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context, String targetWindow)
            throws IOException {
        if (UtilValidate.isNotEmpty(description) || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            writer.append("<a");

            if (UtilValidate.isNotEmpty(linkStyle)) {
                writer.append(" class=\"");
                writer.append(linkStyle);
                writer.append("\"");
            }

            writer.append(" href=\"");

            buildHyperlinkUrl(writer, target, targetType, parameterMap, null, false, false, true, request, response, context);

            writer.append("\"");

            if (UtilValidate.isNotEmpty(targetWindow)) {
                writer.append(" target=\"");
                writer.append(targetWindow);
                writer.append("\"");
            }

            if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                writer.append(" ");
                writer.append(modelFormField.getEvent());
                writer.append("=\"");
                writer.append(modelFormField.getAction(context));
                writer.append('"');
            }
            if (UtilValidate.isNotEmpty(confirmation)){
                writer.append(" onclick=\"return confirm('");
                writer.append(confirmation);
                writer.append("')\"");
            }
            writer.append('>');

            if (UtilValidate.isNotEmpty(request.getAttribute("image"))) {
                writer.append("<img src=\"");
                writer.append(request.getAttribute("image").toString());
                writer.append("\"/>");
            }

            writer.append(description);
            writer.append("</a>");
        }
    }

    public static void makeHiddenFormLinkAnchor(Appendable writer, String linkStyle, String description, String confirmation, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        if (UtilValidate.isNotEmpty(description) || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            writer.append("<a");

            if (UtilValidate.isNotEmpty(linkStyle)) {
                writer.append(" class=\"");
                writer.append(linkStyle);
                writer.append("\"");
            }

            writer.append(" href=\"javascript:document.");
            writer.append(makeLinkHiddenFormName(context, modelFormField));
            writer.append(".submit()\"");

            if (UtilValidate.isNotEmpty(modelFormField.getEvent()) && UtilValidate.isNotEmpty(modelFormField.getAction(context))) {
                writer.append(" ");
                writer.append(modelFormField.getEvent());
                writer.append("=\"");
                writer.append(modelFormField.getAction(context));
                writer.append('"');
            }

            if (UtilValidate.isNotEmpty(confirmation)){
                writer.append(" onclick=\"return confirm('");
                writer.append(confirmation);
                writer.append("')\"");
            }

            writer.append('>');

            if (UtilValidate.isNotEmpty(request.getAttribute("image"))) {
                writer.append("<img src=\"");
                writer.append(request.getAttribute("image").toString());
                writer.append("\"/>");
            }

            writer.append(description);
            writer.append("</a>");
        }
    }

    public static void makeHiddenFormLinkForm(Appendable writer, String target, String targetType, String targetWindow, Map<String, String> parameterMap, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        writer.append("<form method=\"post\"");
        writer.append(" action=\"");
        // note that this passes null for the parameterList on purpose so they won't be put into the URL
        WidgetWorker.buildHyperlinkUrl(writer, target, targetType, null, null, false, false, true, request, response, context);
        writer.append("\"");

        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.append(" target=\"");
            writer.append(targetWindow);
            writer.append("\"");
        }

        writer.append(" onsubmit=\"javascript:submitFormDisableSubmits(this)\"");

        writer.append(" name=\"");
        writer.append(makeLinkHiddenFormName(context, modelFormField));
        writer.append("\">");

        for (Map.Entry<String, String> parameter: parameterMap.entrySet()) {
            if (parameter.getValue() != null) {
                writer.append("<input name=\"");
                writer.append(parameter.getKey());
                writer.append("\" value=\"");
                writer.append(UtilCodec.getEncoder("html").encode(parameter.getValue()));
                writer.append("\" type=\"hidden\"/>");
            }
        }

        writer.append("</form>");
    }

    public static String makeLinkHiddenFormName(Map<String, Object> context, ModelFormField modelFormField) {
        ModelForm modelForm = modelFormField.getModelForm();
        Integer itemIndex = (Integer) context.get("itemIndex");
        String iterateId = "";
        String formUniqueId = "";
        String formName = (String) context.get("formName");
        if (UtilValidate.isEmpty(formName)) {
            formName = modelForm.getName();
        }
        if (UtilValidate.isNotEmpty(context.get("iterateId"))) {
            iterateId = (String) context.get("iterateId");
        }
        if (UtilValidate.isNotEmpty(context.get("formUniqueId"))) {
            formUniqueId = (String) context.get("formUniqueId");
        }
        if (itemIndex != null) {
            return formName + modelForm.getItemIndexSeparator() + itemIndex.intValue() + iterateId + formUniqueId + modelForm.getItemIndexSeparator() + modelFormField.getName();
        }
        return formName + modelForm.getItemIndexSeparator() + modelFormField.getName();
    }
    public static String determineAutoLinkType(String linkType, String target, String targetType, HttpServletRequest request) {
        if ("auto".equals(linkType)) {
            if ("intra-app".equals(targetType)) {
                String requestUri = (target.indexOf('?') > -1) ? target.substring(0, target.indexOf('?')) : target;
                ServletContext servletContext = request.getSession().getServletContext();
                RequestHandler rh = (RequestHandler) servletContext.getAttribute("_REQUEST_HANDLER_");
                ConfigXMLReader.RequestMap requestMap = null;
                try {
                    requestMap = rh.getControllerConfig().getRequestMapMap().get(requestUri);
                } catch (WebAppConfigurationException e) {
                    Debug.logError(e, "Exception thrown while parsing controller.xml file: ", module);
                }
                if (requestMap != null && requestMap.event != null) {
                    return "hidden-form";
                }
            }
            return "anchor";
        }
        return linkType;
    }

    /** Returns the script location based on a script combined name:
     * <code>location#methodName</code>.
     *
     * @param combinedName The combined location/method name
     * @return The script location
     */
    public static String getScriptLocation(String combinedName) {
        int pos = combinedName.lastIndexOf('#');
        if (pos == -1) {
            return combinedName;
        }
        return combinedName.substring(0, pos);
    }

    /** Returns the script method name based on a script combined name:
     * <code>location#methodName</code>. Returns <code>null</code> if
     * no method name is found.
     *
     * @param combinedName The combined location/method name
     * @return The method name or <code>null</code>
     */
    public static String getScriptMethodName(String combinedName) {
        int pos = combinedName.lastIndexOf('#');
        if (pos == -1) {
            return null;
        }
        return combinedName.substring(pos + 1);
    }

    public static int getPaginatorNumber(Map<String, Object> context) {
        int paginator_number = 0;
        if (context != null) {
            Integer paginateNumberInt= (Integer)context.get("PAGINATOR_NUMBER");
            if (paginateNumberInt == null) {
                paginateNumberInt = Integer.valueOf(0);
                context.put("PAGINATOR_NUMBER", paginateNumberInt);
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    globalCtx.put("PAGINATOR_NUMBER", paginateNumberInt);
                }
            }
            paginator_number = paginateNumberInt.intValue();
        }
        return paginator_number;
    }

    public static void incrementPaginatorNumber(Map<String, Object> context) {
        Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
        if (globalCtx != null) {
            Boolean NO_PAGINATOR = (Boolean) globalCtx.get("NO_PAGINATOR");
            if (UtilValidate.isNotEmpty(NO_PAGINATOR)) {
                globalCtx.remove("NO_PAGINATOR");
            } else {
                Integer paginateNumberInt= (Integer)globalCtx.get("PAGINATOR_NUMBER");
                if (paginateNumberInt == null) {
                    paginateNumberInt = Integer.valueOf(0);
                }
                paginateNumberInt = Integer.valueOf(paginateNumberInt.intValue() + 1);
                globalCtx.put("PAGINATOR_NUMBER", paginateNumberInt);
                context.put("PAGINATOR_NUMBER", paginateNumberInt);
            }
        }
    }

    public static LocalDispatcher getDispatcher(Map<String, Object> context) {
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        return dispatcher;
    }

    public static Delegator getDelegator(Map<String, Object> context) {
        Delegator delegator = (Delegator) context.get("delegator");
        return delegator;
    }
}

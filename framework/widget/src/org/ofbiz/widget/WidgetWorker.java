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
package org.ofbiz.widget;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.webapp.control.ConfigXMLReader;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.form.ModelFormField;
import org.w3c.dom.Element;

public class WidgetWorker {

    public static final String module = WidgetWorker.class.getName();

    public WidgetWorker () {}

    public static void buildHyperlinkUrl(Appendable externalWriter, String target, String targetType, List<WidgetWorker.Parameter> parameterList,
            String prefix, boolean fullPath, boolean secure, boolean encode, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        String localRequestName = UtilHttp.encodeAmpersands(target);
        Appendable localWriter = new StringWriter();

        if ("intra-app".equals(targetType)) {
            if (request != null && response != null) {
                ServletContext servletContext = (ServletContext) request.getSession().getServletContext();
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
        } else if ("plain".equals(targetType)) {
            localWriter.append(localRequestName);
        } else {
            localWriter.append(localRequestName);
        }

        if (UtilValidate.isNotEmpty(parameterList)) {
            String localUrl = localWriter.toString();
            externalWriter.append(localUrl);
            boolean needsAmp = true;
            if (localUrl.indexOf('?') == -1) {
                externalWriter.append('?');
                needsAmp = false;
            }

            for (WidgetWorker.Parameter parameter: parameterList) {
                if (needsAmp) {
                    externalWriter.append("&amp;");
                } else {
                    needsAmp = true;
                }
                externalWriter.append(parameter.getName());
                externalWriter.append('=');
                StringUtil.SimpleEncoder simpleEncoder = (StringUtil.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    externalWriter.append(simpleEncoder.encode(parameter.getValue(context)));
                } else {
                    externalWriter.append(parameter.getValue(context));
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
            List<WidgetWorker.Parameter> parameterList, String description, String targetWindow, String confirmation, ModelFormField modelFormField,
            HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        String realLinkType = WidgetWorker.determineAutoLinkType(linkType, target, targetType, request);
        if ("hidden-form".equals(realLinkType)) {
            if (modelFormField != null && "multi".equals(modelFormField.getModelForm().getType())) {
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, confirmation, modelFormField, request, response, context);

                // this is a bit trickier, since we can't do a nested form we'll have to put the link to submit the form in place, but put the actual form def elsewhere, ie after the big form is closed
                Map<String, Object> wholeFormContext = UtilGenerics.checkMap(context.get("wholeFormContext"));
                Appendable postMultiFormWriter = wholeFormContext != null ? (Appendable) wholeFormContext.get("postMultiFormWriter") : null;
                if (postMultiFormWriter == null) {
                    postMultiFormWriter = new StringWriter();
                    wholeFormContext.put("postMultiFormWriter", postMultiFormWriter);
                }
                WidgetWorker.makeHiddenFormLinkForm(postMultiFormWriter, target, targetType, targetWindow, parameterList, modelFormField, request, response, context);
            } else {
                WidgetWorker.makeHiddenFormLinkForm(writer, target, targetType, targetWindow, parameterList, modelFormField, request, response, context);
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, confirmation, modelFormField, request, response, context);
            }
        } else {
            WidgetWorker.makeHyperlinkString(writer, linkStyle, targetType, target, parameterList, description, confirmation, modelFormField, request, response, context, targetWindow);
        }

    }
    public static void makeHyperlinkString(Appendable writer, String linkStyle, String targetType, String target, List<WidgetWorker.Parameter> parameterList,
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

            buildHyperlinkUrl(writer, target, targetType, parameterList, null, false, false, true, request, response, context);

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

    public static void makeHiddenFormLinkForm(Appendable writer, String target, String targetType, String targetWindow, List<WidgetWorker.Parameter> parameterList, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
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

        for (WidgetWorker.Parameter parameter: parameterList) {
            writer.append("<input name=\"");
            writer.append(parameter.getName());
            writer.append("\" value=\"");
            writer.append(parameter.getValue(context));
            writer.append("\" type=\"hidden\"/>");
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
        } else {
            return formName + modelForm.getItemIndexSeparator() + modelFormField.getName();
        }
    }

    public static class Parameter {
        protected String name;
        protected FlexibleStringExpander value;
        protected FlexibleMapAccessor<Object> fromField;

        public Parameter(Element element) {
            this.name = element.getAttribute("param-name");
            this.value = UtilValidate.isNotEmpty(element.getAttribute("value")) ? FlexibleStringExpander.getInstance(element.getAttribute("value")) : null;
            this.fromField = UtilValidate.isNotEmpty(element.getAttribute("from-field")) ? FlexibleMapAccessor.getInstance(element.getAttribute("from-field")) : null;
        }

        public Parameter(String paramName, String paramValue, boolean isField) {
            this.name = paramName;
            if (isField) {
                this.fromField = FlexibleMapAccessor.getInstance(paramValue);
            } else {
                this.value = FlexibleStringExpander.getInstance(paramValue);
            }
        }

        public String getName() {
            return name;
        }

        public String getValue(Map<String, Object> context) {
            if (this.value != null) {
                return this.value.expandString(context);
            } else if (this.fromField != null && this.fromField.get(context) != null) {
                Object retVal = this.fromField.get(context);
                
                if (retVal != null) {
                    TimeZone timeZone = (TimeZone) context.get("timeZone");
                    if (timeZone == null) timeZone = TimeZone.getDefault();
                    
                    String returnValue = null;
                    // format string based on the user's time zone (not locale because these are parameters)
                    if (retVal instanceof Double || retVal instanceof Float || retVal instanceof BigDecimal) {
                        returnValue = retVal.toString();
                    } else if (retVal instanceof java.sql.Date) {
                        DateFormat df = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, null);
                        returnValue = df.format((java.util.Date) retVal);
                    } else if (retVal instanceof java.sql.Time) {
                        DateFormat df = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, null);
                        returnValue = df.format((java.util.Date) retVal);
                    } else if (retVal instanceof java.sql.Timestamp) {
                        DateFormat df = UtilDateTime.toDateTimeFormat(UtilDateTime.DATE_TIME_FORMAT, timeZone, null);
                        returnValue = df.format((java.util.Date) retVal);
                    } else if (retVal instanceof java.util.Date) {
                        DateFormat df = UtilDateTime.toDateTimeFormat("EEE MMM dd hh:mm:ss z yyyy", timeZone, null);
                        returnValue = df.format((java.util.Date) retVal);
                    } else {
                        returnValue = retVal.toString();
                    }
                    return returnValue;
                } else {
                    return null;
                }
            } else {
                // as a last chance try finding a context field with the key of the name field
                Object obj = context.get(this.name);
                if (obj != null) {
                    return obj.toString();
                } else {
                    return null;
                }
            }
        }
    }

    public static String determineAutoLinkType(String linkType, String target, String targetType, HttpServletRequest request) {
        if ("auto".equals(linkType)) {
            if ("intra-app".equals(targetType)) {
                String requestUri = (target.indexOf('?') > -1) ? target.substring(0, target.indexOf('?')) : target;
                ServletContext servletContext = (ServletContext) request.getSession().getServletContext();
                RequestHandler rh = (RequestHandler) servletContext.getAttribute("_REQUEST_HANDLER_");
                ConfigXMLReader.RequestMap requestMap = rh.getControllerConfig().getRequestMapMap().get(requestUri);
                if (requestMap != null && requestMap.event != null) {
                    return "hidden-form";
                } else {
                    return "anchor";
                }
            } else {
                return "anchor";
            }
        } else {
            return linkType;
        }
    }
}

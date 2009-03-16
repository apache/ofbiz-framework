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
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.form.ModelFormField;

public class WidgetWorker {

    public static final String module = WidgetWorker.class.getName();

    public WidgetWorker () {}

    public static void buildHyperlinkUrl(Appendable externalWriter, String target, String targetType, List<ModelFormField.Parameter> parameterList, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        String localRequestName = UtilHttp.encodeAmpersands(target);
        Appendable localWriter = new StringWriter();
        
        if ("intra-app".equals(targetType)) {
            appendOfbizUrl(localWriter, "/" + localRequestName, request, response);
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
        
        if (parameterList != null && parameterList.size() > 0) {
            String localUrl = localWriter.toString();
            externalWriter.append(localUrl);
            boolean needsAmp = true;
            if (localUrl.indexOf('?') == -1) {
                externalWriter.append('?');
                needsAmp = false;
            }
            
            for (ModelFormField.Parameter parameter: parameterList) {
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

    public static void appendOfbizUrl(Appendable writer, String location, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        writer.append(rh.makeLink(request, response, location));
    }

    public static void appendContentUrl(Appendable writer, String location, HttpServletRequest request) throws IOException {
        StringBuffer buffer = new StringBuffer();
        ContentUrlTag.appendContentPrefix(request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }

    public static void makeHyperlinkByType(Appendable writer, String linkType, String linkStyle, String targetType, String target, 
            List<ModelFormField.Parameter> parameterList, String description, String targetWindow, ModelFormField modelFormField, 
            HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        if ("hidden-form".equals(linkType)) {
            if ("multi".equals(modelFormField.getModelForm().getType())) {
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, modelFormField, request, response, context);

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
                WidgetWorker.makeHiddenFormLinkAnchor(writer, linkStyle, description, modelFormField, request, response, context);
            }
        } else {
            WidgetWorker.makeHyperlinkString(writer, linkStyle, targetType, target, parameterList, description, modelFormField, request, response, context, targetWindow);
        }
        
    }
    
    public static void makeHyperlinkString(Appendable writer, String linkStyle, String targetType, String target, List<ModelFormField.Parameter> parameterList, 
            String description, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context, String targetWindow) 
            throws IOException {
        if (UtilValidate.isNotEmpty(description) || UtilValidate.isNotEmpty(request.getAttribute("image"))) {
            writer.append("<a");

            if (UtilValidate.isNotEmpty(linkStyle)) {
                writer.append(" class=\"");
                writer.append(linkStyle);
                writer.append("\"");
            }

            writer.append(" href=\"");

            buildHyperlinkUrl(writer, target, targetType, parameterList, request, response, context);

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

            writer.append('>');
            
            if (UtilValidate.isNotEmpty(request.getAttribute("image"))){
            	writer.append("<img src=\"");
            	writer.append(request.getAttribute("image").toString());
            	writer.append("\"/>");
            }

            writer.append(description);
            writer.append("</a>");
        }
    }
    
    public static void makeHiddenFormLinkAnchor(Appendable writer, String linkStyle, String description, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
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

            writer.append('>');
            
            if (UtilValidate.isNotEmpty(request.getAttribute("image"))){
                writer.append("<img src=\"");
                writer.append(request.getAttribute("image").toString());
                writer.append("\"/>");
            }

            writer.append(description);
            writer.append("</a>");
        }
    }
    
    public static void makeHiddenFormLinkForm(Appendable writer, String target, String targetType, String targetWindow, List<ModelFormField.Parameter> parameterList, ModelFormField modelFormField, HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws IOException {
        writer.append("<form method=\"post\"");
        writer.append(" action=\"");
        WidgetWorker.buildHyperlinkUrl(writer, target, targetType, null, request, response, context);
        writer.append("\"");

        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.append(" target=\"");
            writer.append(targetWindow);
            writer.append("\"");
        }

        writer.append(" onSubmit=\"javascript:submitFormDisableSubmits(this)\"");

        writer.append(" name=\"");
        writer.append(makeLinkHiddenFormName(context, modelFormField));
        writer.append("\">");

        for (ModelFormField.Parameter parameter: parameterList) {
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
        String formName = (String) context.get("formName");
        if (UtilValidate.isEmpty(formName)) {
            formName = modelForm.getName();
        }

        if (itemIndex != null) {
            return formName + modelForm.getItemIndexSeparator() + itemIndex.intValue() + modelForm.getItemIndexSeparator() + modelFormField.getName();
        } else {
            return formName + modelForm.getItemIndexSeparator() + modelFormField.getName();
        }
    }
}

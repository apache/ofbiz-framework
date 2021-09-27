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
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.webapp.OfbizUrlBuilder;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;

public class OfbizContentAltUrlTransforms implements TemplateTransformModel {
    private static final String MODULE = OfbizContentAltUrlTransforms.class.getName();
    private static final String DEF_VIEW_REQUEST = "contentViewInfo";

    /**
     * Gets string arg.
     * @param args the args
     * @param key the key
     * @return the string arg
     */
    public String getStringArg(Map<String, Object> args, String key) {
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            return ((SimpleScalar) o).getAsString();
        } else if (o instanceof StringModel) {
            return ((StringModel) o).getAsString();
        } else if (o instanceof SimpleNumber) {
            return ((SimpleNumber) o).getAsNumber().toString();
        } else if (o instanceof NumberModel) {
            return ((NumberModel) o).getAsNumber().toString();
        }
        return null;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args)
            throws TemplateModelException, IOException {
        final StringBuilder buf = new StringBuilder();
        return new Writer(out) {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    BeanModel res = (BeanModel) env.getVariable("response");
                    if (req != null) {
                        Map<String, Object> arguments = UtilGenerics.cast(args);
                        String contentId = getStringArg(arguments, "contentId");
                        String viewContent = getStringArg(arguments, "viewContent");
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        HttpServletResponse response = null;
                        if (res != null) {
                            response = (HttpServletResponse) res.getWrappedObject();
                        }
                        String url = "";
                        if (UtilValidate.isNotEmpty(contentId)) {
                            url = makeContentAltUrl(request, response, contentId, viewContent);
                        }
                        out.write(url);
                    }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }

    private static String makeContentAltUrl(HttpServletRequest request, HttpServletResponse response, String contentId, String viewContent) {
        if (UtilValidate.isEmpty(contentId)) {
            return null;
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String url = null;
        try {
            List<GenericValue> contentAssocDataResources = EntityQuery.use(delegator)
                    .select("contentIdStart", "drObjectInfo", "dataResourceId", "caFromDate", "caThruDate", "caCreatedDate")
                    .from("ContentAssocDataResourceViewTo")
                    .where("caContentAssocTypeId", "ALTERNATIVE_URL",
                            "caThruDate", null,
                            "contentIdStart", contentId)
                    .orderBy("-caFromDate")
                    .cache()
                    .queryList();
            GenericValue contentAssocDataResource = EntityUtil.filterByCondition(contentAssocDataResources,
                    EntityCondition.makeCondition("localeString", request.getLocale().toString()));
            if (UtilValidate.isEmpty(contentAssocDataResource)) {
                contentAssocDataResource = EntityUtil.getFirst(contentAssocDataResources);
            }
            if (contentAssocDataResource != null) {
                url = contentAssocDataResource.getString("drObjectInfo");
                url = UtilCodec.getDecoder("url").decode(url);
            }
        } catch (GenericEntityException gee) {
            Debug.logWarning("[Exception] : " + gee.getMessage(), MODULE);
        }

        if (UtilValidate.isEmpty(url)) {
            url = makeContentUrl(request, response, contentId, viewContent);
        }
        try {
            OfbizUrlBuilder ofbizUrlBuilder = OfbizUrlBuilder.from(request);
            Writer writer = new StringWriter();
            ofbizUrlBuilder.buildFullUrl(writer, url, true);
            return writer.toString();
        } catch (GenericEntityException | WebAppConfigurationException | IOException e) {
            Debug.logError(e, "Failed to resolve the url", MODULE);
        }
        return url;
    }

    private static String makeContentUrl(HttpServletRequest request, HttpServletResponse response, String contentId, String viewContent) {
        if (UtilValidate.isEmpty(contentId)) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder();

        if (UtilValidate.isNotEmpty(viewContent)) {
            urlBuilder.append("/" + viewContent);
        } else {
            urlBuilder.append("/" + DEF_VIEW_REQUEST);
        }
        urlBuilder.append("?contentId=" + contentId);
        return urlBuilder.toString();
    }

}

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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.webapp.WebAppUtil;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

public class OfbizContentAltUrlTransforms implements TemplateTransformModel {
    public final static String module = OfbizContentAltUrlTransforms.class.getName();
    private static final String defaultViewRequest = "contentViewInfo";

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
            GenericValue contentAssocDataResource = EntityQuery.use(delegator)
                    .select("contentIdStart", "drObjectInfo", "dataResourceId", "caFromDate", "caThruDate", "caCreatedDate")
                    .from("ContentAssocDataResourceViewTo")
                    .where("caContentAssocTypeId", "ALTERNATIVE_URL",
                            "caThruDate", null,
                            "contentIdStart", contentId)
                    .orderBy("-caFromDate")
                    .queryFirst();
            if (contentAssocDataResource != null) {
                url = contentAssocDataResource.getString("drObjectInfo");
                url = UtilCodec.getDecoder("url").decode(url);
                String mountPoint = request.getContextPath();
                if (!("/".equals(mountPoint)) && !(mountPoint.equals(""))) {
                    url = mountPoint + url;
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logWarning("[Exception] : " + gee.getMessage(), module);
        }

        if (UtilValidate.isEmpty(url)) {
            if (UtilValidate.isEmpty(viewContent)) {
                viewContent = defaultViewRequest;
            }
            url = makeContentUrl(request, response, contentId, viewContent);
        }
        return url;
    }

    private static String makeContentUrl(HttpServletRequest request, HttpServletResponse response, String contentId, String viewContent) {
        if (UtilValidate.isEmpty(contentId)) {
            return null;
        }
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(request.getSession().getServletContext().getContextPath());
        if (urlBuilder.length() == 0 || urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        urlBuilder.append(WebAppUtil.CONTROL_MOUNT_POINT);

        if (UtilValidate.isNotEmpty(viewContent)) {
            urlBuilder.append("/" + viewContent);
        } else {
            urlBuilder.append("/" + defaultViewRequest);
        }
        urlBuilder.append("?contentId=" + contentId);
        return urlBuilder.toString();
    }

}

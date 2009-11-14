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
package org.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.webapp.taglib.ContentUrlTag;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * OfbizContentTransform - Freemarker Transform for content links
 */
public class OfbizContentTransform implements TemplateTransformModel {

    public final static String module = OfbizUrlTransform.class.getName();

    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        return new Writer(out) {
            @Override
            public void write(char cbuf[], int off, int len) {
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
                    BeanModel req = (BeanModel)env.getVariable("request");
                    HttpServletRequest request = req == null ? null : (HttpServletRequest) req.getWrappedObject();

                    String requestUrl = buf.toString();

                    // make the link
                    StringBuilder newURL = new StringBuilder();
                    ContentUrlTag.appendContentPrefix(request, newURL);
                    if (newURL.length() > 0 && newURL.charAt(newURL.length() - 1) != '/' && requestUrl.charAt(0) != '/') {
                        newURL.append('/');
                    }
                    newURL.append(requestUrl);
                    out.write(newURL.toString());
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}

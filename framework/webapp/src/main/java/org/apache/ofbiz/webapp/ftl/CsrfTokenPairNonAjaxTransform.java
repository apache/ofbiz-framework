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
package org.apache.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.security.CsrfUtil;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * CsrfTokenPairNonAjaxTransform - Freemarker Transform for csrf token in non-Ajax call
 */
public class CsrfTokenPairNonAjaxTransform implements TemplateTransformModel {

    private static final String MODULE = CsrfTokenPairNonAjaxTransform.class.getName();

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args)
            throws TemplateModelException, IOException {

        final StringBuffer buf = new StringBuffer();

        return new Writer(out) {

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        String tokenValue = CsrfUtil.generateTokenForNonAjax(request, buf.toString());
                        out.write(CsrfUtil.getTokenNameNonAjax() + "=" + tokenValue);
                    }
                    return;
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }
        };
    }
}

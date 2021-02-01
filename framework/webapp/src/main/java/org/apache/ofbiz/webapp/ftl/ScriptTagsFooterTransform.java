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
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.widget.model.ScriptLinkHelper;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

/**
 * Render the externalized script tags collected from the "html-template" tag where multi-block = true
 */
public class ScriptTagsFooterTransform implements TemplateTransformModel {

    private static final String MODULE = CsrfTokenAjaxTransform.class.getName();

    @Override
    public final Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args)
            throws TemplateModelException, IOException {

        return new Writer(out) {

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        Set<String> scriptSrcSet = ScriptLinkHelper.getScriptLinksForBodyEnd(request);
                        if (scriptSrcSet != null) {
                            String srcList = "";
                            for (String scriptSrc : scriptSrcSet) {
                                srcList += ("<script src=\"" + scriptSrc + "\" type=\"application/javascript\"></script>\n");
                            }
                            out.write(srcList);
                        }
                    }
                    return;
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void write(char[] cbuf, int off, int len) {
            }
        };

    }
}

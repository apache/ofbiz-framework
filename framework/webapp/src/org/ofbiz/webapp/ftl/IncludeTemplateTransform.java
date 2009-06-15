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

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

public class IncludeTemplateTransform implements TemplateTransformModel {

    public final static String module = IncludeTemplateTransform.class.getName();

    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final String templateLocation = this.getTemplateLocation(args);
        
        return new Writer(out) {
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    Template template = FreeMarkerWorker.getTemplate(templateLocation);
                    env.include(template);
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                } catch (TemplateException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
    
    private String getTemplateLocation(Map args) {
        Object templateLocationObj = args.get("location");
        if (templateLocationObj != null && templateLocationObj instanceof SimpleScalar) {
            return ((SimpleScalar) templateLocationObj).getAsString();
        }
        return null;
    }
}

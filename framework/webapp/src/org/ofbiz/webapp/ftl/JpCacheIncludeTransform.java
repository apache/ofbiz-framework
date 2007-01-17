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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.cache.UtilCache;

import org.jpublish.RepositoryWrapper;

public class JpCacheIncludeTransform implements TemplateTransformModel {

    public static final String module = JpCacheIncludeTransform.class.getName();
    protected static UtilCache pageCache = new UtilCache("webapp.JpInclude", 0, 0, 0, false, false);

    public Writer getWriter(final Writer writer, Map args) throws TemplateModelException, IOException {
        Environment env = Environment.getCurrentEnvironment();
        BeanModel req = (BeanModel) env.getVariable("request");
        BeanModel jpr = (BeanModel) env.getVariable("pages");

        final HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
        final ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        final RepositoryWrapper wrapper = (RepositoryWrapper) jpr.getWrappedObject();
        final String contextName = ctx.getServletContextName();
        final long expireTime = this.getExpireTime(args);
        final String include = this.getInclude(args);

        return new Writer(writer) {
            public void write(char cbuf[], int off, int len) throws IOException {
                writer.write(cbuf, off, len);
            }

            public void flush() throws IOException {
                writer.flush();
            }

            public void close() throws IOException {
                Debug.log("Checking for cached content (" + contextName + "." + include + ")", module);
                String content = (String) pageCache.get(contextName + "." + include);
                if (content == null) {
                    content =  wrapper.get(include);
                    pageCache.put(contextName + "." + include, content, expireTime);
                    Debug.log("No content found; cached result for - " + expireTime, module);
                }
                if (content != null) {
                    writer.write(content);
                }
            }
        };
    }

    public long getExpireTime(Map args) {
        Object o = args.get("expireTime");
        Debug.log("ExpireTime Object - " + o, module);
        long expireTime = 0;
        if (o != null) {
            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                String ets = s.getAsString();
                Debug.log("ExpireTime String - " + ets, module);
                try {
                    expireTime = Long.parseLong(ets);
                } catch (Exception e) {
                    Debug.logError(e, module);
                }
            }
        }
        return expireTime;
    }

    public String getInclude(Map args) {
        Object o = args.get("include");
        Debug.log("Include Object - " + o, module);
        String include = null;
        if (o != null) {
            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                include = s.getAsString();
                Debug.log("Include String - " + include, module);
            }
        }
        return include;
    }
}

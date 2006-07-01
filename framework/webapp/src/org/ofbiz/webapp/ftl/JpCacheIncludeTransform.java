/*
 * $Id: JpCacheIncludeTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.2
 */
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

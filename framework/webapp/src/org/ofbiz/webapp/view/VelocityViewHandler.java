/*
 * $Id: VelocityViewHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2002-2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.webapp.view;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleProperties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.io.VelocityWriter;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.util.SimplePool;

/**
 * VelocityViewHandler - Velocity Template Engine View Handler
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class VelocityViewHandler implements ViewHandler {

    public static final String module = VelocityViewHandler.class.getName();

    public static final String REQUEST = "req";
    public static final String RESPONSE = "res";       

    private static SimplePool writerPool = new SimplePool(40);
    private VelocityEngine ve = null;

    public void init(ServletContext context) throws ViewHandlerException {
        try {
            Debug.logInfo("[VelocityViewHandler.init] : Loading...", module);
            ve = new VelocityEngine();
            // set the properties
            // use log4j for logging
            // use classpath template loading (file loading will not work in WAR)
            ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.Log4JLogSystem");
            ve.setProperty("runtime.log.logsystem.log4j.category", module);

            Properties props = null;
            URL propsURL = null;

            try {
                propsURL = context.getResource("/WEB-INF/velocity.properties");
            } catch (MalformedURLException e) {
                Debug.logError(e, module);
            }

            if (propsURL != null) {
                props = new FlexibleProperties(propsURL);
                Debug.logWarning("[VelocityViewHandler.init] : Loaded /WEB-INF/velocity.properties", module);
            } else {
                props = new Properties();
                Debug.logWarning("[VelocityViewHandler.init] : Cannot load /WEB-INF/velocity.properties. " +
                    "Using default properties.", module);
            }

            // set the file loader path -- used to mount the webapp
            if (context.getRealPath("/") != null) {
                props.setProperty("file.resource.loader.path", context.getRealPath("/"));
                Debug.logInfo("[VelocityViewHandler.init] : Got true webapp path, mounting as template path.", module);
            }

            ve.init(props);
        } catch (Exception e) {
            throw new ViewHandlerException(e.getMessage(), e);
        }
    }

    public void render(String name, String page, String info, String contentType, String encoding, 
            HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        if (ve == null) {
            throw new ViewHandlerException("Velocity Template Engine has not been initialized");
        }

        if (page == null || page.length() == 0) {
            throw new ViewHandlerException("Invalid template source");
        }

        Context context = new VelocityContext();

        context.put(REQUEST, request);
        context.put(RESPONSE, response);

        Template template = null;

        try {
            template = ve.getTemplate(page);
        } catch (ResourceNotFoundException rne) {
            throw new ViewHandlerException("Invalid template source", rne);
        } catch (Exception e) {
            throw new ViewHandlerException(e.getMessage(), e);
        }

        ServletOutputStream out = null;
        VelocityWriter vw = null;

        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            throw new ViewHandlerException(e.getMessage(), e);
        }

        try {
            vw = (VelocityWriter) writerPool.get();
            if (vw == null)
                vw = new VelocityWriter(new OutputStreamWriter(out, encoding), 4 * 1024, true);
            else
                vw.recycle(new OutputStreamWriter(out, encoding));

            if (vw == null)
                Debug.logWarning("[VelocityViewHandler.eval] : VelocityWriter is NULL", module);

            template.merge(context, vw);
        } catch (Exception e) {
            throw new ViewHandlerException(e.getMessage(), e);
        } finally {
            try {
                if (vw != null) {
                    vw.flush();
                    writerPool.put(vw);
                }
            } catch (Exception e) {
                throw new ViewHandlerException(e.getMessage(), e);
            }
        }
    }
}

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

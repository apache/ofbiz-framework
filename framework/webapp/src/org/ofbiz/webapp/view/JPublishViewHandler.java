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
import java.io.Writer;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilJ2eeCompat;

/**
 * Handles JPublish type view rendering
 */
public class JPublishViewHandler implements ViewHandler {

    public static final String module = JPublishViewHandler.class.getName();
    
    protected ServletContext servletContext = null;
    protected JPublishWrapper wrapper = null;

    /**
     * @see org.ofbiz.webapp.view.ViewHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
        this.wrapper = (JPublishWrapper) context.getAttribute("jpublishWrapper");
        if (this.wrapper == null) {
            this.wrapper = new JPublishWrapper(servletContext);            
        }

        // make sure it loaded
        if (this.wrapper == null)
            throw new ViewHandlerException("JPublishWrapper not created");
    }

    /**
     * @see org.ofbiz.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        Writer writer = null;
        try {
        	// use UtilJ2eeCompat to get this setup properly
        	boolean useOutputStreamNotWriter = false;
        	if (this.servletContext != null) {
        		useOutputStreamNotWriter = UtilJ2eeCompat.useOutputStreamNotWriter(this.servletContext);
        	}
        	if (useOutputStreamNotWriter) {
        		ServletOutputStream ros = response.getOutputStream();
                writer = new OutputStreamWriter(ros, "UTF-8");
        	} else {
                writer = response.getWriter();
        	}
            wrapper.render(page, request, response, writer, null, true);
        } catch (IOException e) {
            throw new ViewHandlerException("Problems with the response writer/output stream", e);
        } catch (GeneralException e) {
            throw new ViewHandlerException("Cannot render page", e);
        }
    }
}


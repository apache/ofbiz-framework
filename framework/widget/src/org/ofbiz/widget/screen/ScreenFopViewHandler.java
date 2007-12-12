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
package org.ofbiz.widget.screen;

import java.io.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;

import org.ofbiz.base.util.Debug;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.fo.FoScreenRenderer;

/**
 * Uses XSL-FO formatted templates to generate PDF, PCL, POSTSCRIPT etc.  views
 * This handler will use JPublish to generate the XSL-FO
 */
public class ScreenFopViewHandler implements ViewHandler {
    public static final String module = ScreenFopViewHandler.class.getName();

    protected ServletContext servletContext = null;
    protected FoScreenRenderer foScreenRenderer = new FoScreenRenderer();

    /**
     * @see org.ofbiz.webapp.view.ViewHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }

    /**
     * @see org.ofbiz.content.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {

        // render and obtain the XSL-FO
        Writer writer = new StringWriter();
        try {
            ScreenRenderer screens = new ScreenRenderer(writer, null, foScreenRenderer);
            screens.populateContextForRequest(request, response, servletContext);

            // this is the object used to render forms from their definitions
            screens.getContext().put("formStringRenderer", new FoFormRenderer(request, response));
            screens.render(page);
        } catch (Throwable t) {
            throw new ViewHandlerException("Problems with the response writer/output stream", t);
        }

        // set the input source (XSL-FO) and generate the output stream of contentType
        Reader reader = new StringReader(writer.toString());
        StreamSource src = new StreamSource(reader);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Transforming the following xsl-fo template: " + writer.toString(), module);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Fop fop = ApacheFopWorker.createFopInstance(out, contentType);
            ApacheFopWorker.transform(src, null, fop);
        } catch (Exception e) {
            throw createException("Unable to transform FO file", e, response);
        }
        // set the content type and length
        response.setContentType(contentType);
        response.setContentLength(out.size());

        // write to the browser
        try {
            out.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw createException("Unable write to browser OutputStream", e, response);
        }
    }

    protected ViewHandlerException createException(String msg, Exception e, HttpServletResponse response) {
        Debug.logError(msg + ": " + e, module);
        String htmlString = "<html><head><title>FOP Rendering Error</title></head><body>" + msg + ": " + e.getMessage() + "</body></html>";
        response.setContentType("text/html");
        response.setContentLength(htmlString.length());
        try {
            response.getOutputStream().write(htmlString.getBytes());
        } catch (IOException i) {
            Debug.logError("Multiple errors rendering FOP", module);
        }
        return new ViewHandlerException(msg, e);
    }

}

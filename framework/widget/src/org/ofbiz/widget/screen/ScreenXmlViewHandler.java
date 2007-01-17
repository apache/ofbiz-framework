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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.xml.sax.SAXException;

import org.ofbiz.widget.screen.ScreenWidgetViewHandler;
import org.ofbiz.widget.xml.XmlFormRenderer;

public class ScreenXmlViewHandler extends ScreenWidgetViewHandler {
    
    public static final String module = ScreenXmlViewHandler.class.getName();
    
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

            ScreenRenderer screens = new ScreenRenderer(writer, null, htmlScreenRenderer);
            screens.populateContextForRequest(request, response, servletContext);
            // this is the object used to render forms from their definitions
            screens.getContext().put("formStringRenderer", new XmlFormRenderer(request, response));
            screens.render(page);
        } catch (IOException e) {
            throw new ViewHandlerException("Error in the response writer/output stream: " + e.toString(), e);
        } catch (SAXException e) {
            throw new ViewHandlerException("XML Error rendering page: " + e.toString(), e);
        } catch (ParserConfigurationException e) {
            throw new ViewHandlerException("XML Error rendering page: " + e.toString(), e);
        } catch (GeneralException e) {
            throw new ViewHandlerException("Lower level error rendering page: " + e.toString(), e);
        }
    }
}

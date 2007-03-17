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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.FOPException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.webapp.view.ApacheFopFactory;
import org.ofbiz.widget.fo.FoFormRenderer;

/**
 * Uses XSL-FO formatted templates to generate PDF, PCL, POSTSCRIPT etc.  views
 * This handler will use JPublish to generate the XSL-FO
 */
public class ScreenFopViewHandler extends ScreenWidgetViewHandler {
    public static final String module = ScreenFopViewHandler.class.getName();

    /**
     * @see org.ofbiz.content.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {

        if (UtilValidate.isEmpty(contentType)) {
            contentType = "application/pdf";
        }
        
        // render and obtain the XSL-FO
        Writer writer = new StringWriter();

        FopFactory fopFactory = ApacheFopFactory.instance();

        try {
            ScreenRenderer screens = new ScreenRenderer(writer, null, htmlScreenRenderer);
            screens.populateContextForRequest(request, response, servletContext);

            // this is the object used to render forms from their definitions
            screens.getContext().put("formStringRenderer", new FoFormRenderer(request, response));
            screens.render(page);
        } catch (Throwable t) {
            throw new ViewHandlerException("Problems with the response writer/output stream", t);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        TransformerFactory transFactory = TransformerFactory.newInstance();

        try {
            Fop fop = fopFactory.newFop(contentType, out);
            Transformer transformer = transFactory.newTransformer();

            // set the input source (XSL-FO) and generate the output stream of contentType
            Reader reader = new StringReader(writer.toString());
            Source src = new StreamSource(reader);

            /*
            try {
                String buf = writer.toString();
                java.io.FileWriter fw = new java.io.FileWriter(new java.io.File("/tmp/xslfo.out"));
                fw.write(buf.toString());
                fw.close();
            } catch (IOException e) {
                throw new ViewHandlerException("Unable write to browser OutputStream", e);            
            }
            */

            // Get handler that is used in the generation process
            Result res = new SAXResult(fop.getDefaultHandler());

            try {
                // Transform the FOP XML source
                transformer.transform(src, res);

                // We don't want to cache the images that get loaded by the FOP engine
                fopFactory.getImageFactory().clearCaches();

                // set the content type and length
                response.setContentType(contentType);
                response.setContentLength(out.size());

                // write to the browser
                try {
                    out.writeTo(response.getOutputStream());
                    response.getOutputStream().flush();
                } catch (IOException e) {
                    throw new ViewHandlerException("Unable write to browser OutputStream", e);
                }

            } catch (TransformerException e) {
                Debug.logError("FOP transform failed:" + e, module );
                throw new ViewHandlerException("Unable to transform FO to " + contentType, e);
            }

        } catch (TransformerConfigurationException e) {
            Debug.logError("FOP TransformerConfiguration Exception " + e, module);
            throw new ViewHandlerException("Transformer Configuration Error", e);
        } catch (FOPException e) {
            Debug.logError("FOP Exception " + e, module);
            throw new ViewHandlerException("FOP Error", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                Debug.logError("Unable to close output stream " + e, module);
            }
        }
    }
}

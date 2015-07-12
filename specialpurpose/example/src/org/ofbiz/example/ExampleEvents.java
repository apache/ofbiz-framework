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
package org.ofbiz.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.ofbiz.widget.renderer.FormStringRenderer;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.ofbiz.widget.renderer.macro.MacroFormRenderer;
import org.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;


public class ExampleEvents {

    public static final String module = ExampleEvents.class.getName();
    
    public static final String examplePdfProperties = "examplepdf.properties";
    
    public static final boolean useExampleDefaultOwnerPassword = UtilProperties.getPropertyValue(examplePdfProperties, "use.default.pdf.owner.password", "N").equalsIgnoreCase("Y");

    public static final String exampleDefaultOwnerPassword = UtilProperties.getPropertyValue(examplePdfProperties, "default.pdf.owner.password", "ofbiz");
    
    public static final String resourceExample = "ExampleUiLables";

    /** Set password to the specified example and output the generated PDF.
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String setExamplePdfPassword(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request);
        ServletContext servletContext = request.getSession().getServletContext();

        Map<String, Object> requestParams = UtilHttp.getParameterMap(request);
        String password = (String) requestParams.get("PASSWORD");
        String confirmPassword = (String) requestParams.get("CONFIRM_PASSWORD");

        if (UtilValidate.isEmpty(password) && UtilValidate.isEmpty(confirmPassword) && (UtilValidate.isEmpty(exampleDefaultOwnerPassword) || !useExampleDefaultOwnerPassword)) {
        	return "nopassword";
        }
        if (UtilValidate.isNotEmpty(password) && !password.equals(confirmPassword)) {
        	String errMsg = UtilProperties.getMessage(resourceExample, "password_not_equal_confirm_password", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        
        // get the original pdf
        String screenLocation = "component://example/widget/example/ExampleScreens.xml";
        String reportScreenName = "ExampleReport";

        // render a screen to get the XML document
        Writer reportWriter = new StringWriter();

        try {
            ScreenStringRenderer foScreenRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", "screenfop.name", delegator), EntityUtilProperties.getPropertyValue("widget", "screenfop.screenrenderer", delegator));
            FormStringRenderer formStringRenderer = new MacroFormRenderer(EntityUtilProperties.getPropertyValue("widget", "screenfop.formrenderer", delegator), request, response);
            ScreenRenderer screens = new ScreenRenderer(reportWriter, null, foScreenRenderer);
            screens.populateContextForRequest(request, response, servletContext);

            // this is the object used to render forms from their definitions
            screens.getContext().put("formStringRenderer", formStringRenderer);
            screens.getContext().put("simpleEncoder", UtilCodec.getEncoder(EntityUtilProperties.getPropertyValue("widget", "screenfop.encoder", delegator)));

            screens.render(screenLocation, reportScreenName);
        } catch (GeneralException e) {
            String errMsg = "General error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } catch (IOException e) {
            String errMsg = "IO error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } catch (SAXException e) {
            String errMsg = "SAX (XML parse) error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } catch (ParserConfigurationException e) {
            String errMsg = "Parser configuration error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        } catch (TemplateException e) {
            String errMsg = "Freemarker template error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
		}

        // set the input source (XSL-FO) and generate the PDF
        StreamSource src = new StreamSource(new StringReader(reportWriter.toString()));

        // create the output stream for the generation
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Fop fop = ApacheFopWorker.createFopInstance(out, MimeConstants.MIME_PDF);
            ApacheFopWorker.transform(src, null, fop);
            out.flush();
            out.close();
        } catch (FOPException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            Debug.logError(e, module);
            return "error";
        } catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            Debug.logError(e, module);
            return "error";
		}
        
        // parse the pdf with PDFBox
        ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());
        PDDocument document;
		try {
			document = PDDocument.load(is);
	        int keyLength = 40;
	        AccessPermission ap = new AccessPermission();
	        String ownerPassword = exampleDefaultOwnerPassword;
	        if (UtilValidate.isEmpty(ownerPassword) || !useExampleDefaultOwnerPassword) {
	        	ownerPassword = password;
	        }
	        StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, password, ap);
	        spp.setEncryptionKeyLength(keyLength);
	        document.protect(spp);
		} catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            Debug.logError(e, module);
            return "error";
		} catch (BadSecurityHandlerException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            Debug.logError(e, module);
            return "error";
		} finally {
            try {
            	if (is != null) {
    				is.close();
            	}
			} catch (IOException e) {
				// ignore
			}
		}

		out = new ByteArrayOutputStream();
		try {
			document.save(out);
	        // set the content type and length
	        response.setContentType(MimeConstants.MIME_PDF);
	        response.setContentLength(out.size());
			out.flush();
			out.close();
	        // write to the browser
        	response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();
		} catch (COSVisitorException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            Debug.logError(e, module);
        	return "error";
		} catch (IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            Debug.logError(e, module);
        	return "error";
		}

        return "success";
    }

}

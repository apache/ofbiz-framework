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
package org.apache.ofbiz.widget.renderer.fo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.pdf.PDFEncryptionParams;
import org.apache.fop.render.pdf.PDFEncryptionOption;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.view.AbstractViewHandler;
import org.apache.ofbiz.webapp.view.ApacheFopWorker;
import org.apache.ofbiz.webapp.view.ViewHandlerException;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.MacroFormRenderer;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;

/**
 * Uses XSL-FO formatted templates to generate PDF, PCL, POSTSCRIPT etc.  views
 * This handler will use JPublish to generate the XSL-FO
 */
public class ScreenFopViewHandler extends AbstractViewHandler {
    protected static final String DEFAULT_ERROR_TEMPLATE = "component://common/widget/CommonScreens.xml#FoError";
    private static final String MODULE = ScreenFopViewHandler.class.getName();
    private ServletContext servletContext = null;

    /**
     * @see org.apache.ofbiz.webapp.view.ViewHandler#init(javax.servlet.ServletContext)
     */
    @Override
    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }

    /**
     * @see org.apache.ofbiz.webapp.view.ViewHandler#render(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request,
                       HttpServletResponse response) throws ViewHandlerException {
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        ModelTheme modelTheme = visualTheme.getModelTheme();

        // render and obtain the XSL-FO
        Writer writer = new StringWriter();
        try {
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(modelTheme.getType(getName()),
                    modelTheme.getScreenRendererLocation(getName()));
            FormStringRenderer formStringRenderer = new MacroFormRenderer(modelTheme.getFormRendererLocation(getName()), request, response);
            // TODO: uncomment these lines when the renderers are implemented
            //TreeStringRenderer treeStringRenderer = new MacroTreeRenderer(modelTheme.getTreeRendererLocation(getName()), writer);
            //MenuStringRenderer menuStringRenderer = new MacroMenuRenderer(modelTheme.getMenuRendererLocation(getName()), writer);
            ScreenRenderer screens = new ScreenRenderer(writer, null, screenStringRenderer);
            screens.populateContextForRequest(request, response, servletContext);

            // this is the object used to render forms from their definitions
            screens.getContext().put("formStringRenderer", formStringRenderer);
            screens.getContext().put("simpleEncoder", UtilCodec.getEncoder(modelTheme.getEncoder(getName())));
            screens.render(page);
        } catch (IOException | GeneralException | SAXException | ParserConfigurationException | TemplateException e) {
            renderError("Problems with the response writer/output stream", e, "[Not Yet Rendered]", request, response);
            return;
        }

        // set the input source (XSL-FO) and generate the output stream of contentType
        String screenOutString = writer.toString();
        if (!screenOutString.startsWith("<?xml")) {
            screenOutString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + screenOutString;
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("XSL:FO Screen Output: " + screenOutString, MODULE);
        }

        if (UtilValidate.isEmpty(contentType)) {
            contentType = modelTheme.getContentType(getName());
        }
        // get encryption related parameters
        FOUserAgent foUserAgent = null;
        String userPassword = request.getParameter("userPassword");
        String ownerPassword = request.getParameter("ownerPassword");
        boolean allowPrint = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowPrint"))
                ? ApacheFopWorker.getAllowPrintDefault() : request.getParameter("allowPrint"));
        boolean allowCopyContent = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowCopyContent"))
                ? ApacheFopWorker.getAllowCopyContentDefault() : request.getParameter("allowCopyContent"));
        boolean allowEditContent = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowEditContent"))
                ? ApacheFopWorker.getAllowEditContentDefault() : request.getParameter("allowEditContent"));
        boolean allowEditAnnotations = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowEditAnnotations"))
                ? ApacheFopWorker.getAllowEditAnnotationsDefault() : request.getParameter("allowEditAnnotations"));
        if (UtilValidate.isNotEmpty(userPassword) || UtilValidate.isNotEmpty(ownerPassword) || !allowPrint || !allowCopyContent || allowEditContent
                || !allowEditAnnotations) {
            int encryptionLength = 128;
            try {
                encryptionLength = Integer.parseInt(request.getParameter("encryption-length"));
            } catch (NumberFormatException e) {
                try {
                    encryptionLength = Integer.parseInt(ApacheFopWorker.getEncryptionLengthDefault());
                } catch (NumberFormatException e1) {
                    // ignore
                }
            }

            boolean encryptMetadata = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("encrypt-metadata"))
                    ? ApacheFopWorker.getEncryptMetadataDefault() : request.getParameter("encrypt-metadata"));
            boolean allowFillInForms = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowFillInForms"))
                    ? ApacheFopWorker.getAllowFillInFormsDefault() : request.getParameter("allowFillInForms"));
            boolean allowAccessContent = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowAccessContent"))
                    ? ApacheFopWorker.getAllowAccessContentDefault() : request.getParameter("allowAccessContent"));
            boolean allowAssembleDocument = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowAssembleDocument"))
                    ? ApacheFopWorker.getAllowAssembleDocumentDefault() : request.getParameter("allowAssembleDocument"));
            boolean allowPrintHq = Boolean.parseBoolean(UtilValidate.isEmpty(request.getParameter("allowPrintHq"))
                    ? ApacheFopWorker.getAllowPrintHqDefault() : request.getParameter("allowPrintHq"));
            FopFactory fopFactory = ApacheFopWorker.getFactoryInstance();
            foUserAgent = fopFactory.newFOUserAgent();
            PDFEncryptionParams pdfEncryptionParams = new PDFEncryptionParams(userPassword, ownerPassword, allowPrint, allowCopyContent,
                    allowEditContent, allowEditAnnotations, encryptMetadata);
            pdfEncryptionParams.setAllowFillInForms(allowFillInForms);
            pdfEncryptionParams.setAllowAccessContent(allowAccessContent);
            pdfEncryptionParams.setAllowAssembleDocument(allowAssembleDocument);
            pdfEncryptionParams.setAllowPrintHq(allowPrintHq);
            pdfEncryptionParams.setEncryptionLengthInBits(encryptionLength);
            foUserAgent.getRendererOptions().put(PDFEncryptionOption.ENCRYPTION_PARAMS, pdfEncryptionParams);
        }

        Reader reader = new StringReader(screenOutString);
        StreamSource src = new StreamSource(reader);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        /* Debug area, uncomment this to view the xml file generate before analyse by fop
        try {
                java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(System.getProperty("ofbiz.home")+"/runtime/tempfiles/temp.xsl.fo"));
                fw.write(screenOutString);
                fw.close();
            } catch (IOException e) {
                Debug.logError(e, "Couldn't save xls debug file: " + e.toString(), MODULE);
            }
        */
        try {
            Fop fop = ApacheFopWorker.createFopInstance(out, contentType, foUserAgent);
            ApacheFopWorker.transform(src, null, fop);
        } catch (Exception e) {
            renderError("Unable to transform FO file", e, screenOutString, request, response);
            return;
        }
        // set the content type and length
        response.setContentType(contentType);
        response.setContentLength(out.size());

        // write to the browser
        try {
            out.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            renderError("Unable to write to OutputStream", e, screenOutString, request, response);
        }
    }

    /**
     * Render error.
     * @param msg             the msg
     * @param e               the e
     * @param screenOutString the screen out string
     * @param request         the request
     * @param response        the response
     * @throws ViewHandlerException the view handler exception
     */
    protected void renderError(String msg, Exception e, String screenOutString, HttpServletRequest request, HttpServletResponse response)
            throws ViewHandlerException {
        Debug.logError(msg + ": " + e + "; Screen XSL:FO text was:\n" + screenOutString, MODULE);
        try {
            Writer writer = new StringWriter();
            VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
            ModelTheme modelTheme = visualTheme.getModelTheme();
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(modelTheme.getType("screen"),
                    modelTheme.getScreenRendererLocation("screen"));

            ScreenRenderer screens = new ScreenRenderer(writer, null, screenStringRenderer);
            screens.populateContextForRequest(request, response, servletContext);
            screens.getContext().put("errorMessage", msg + ": " + e);
            screens.render(DEFAULT_ERROR_TEMPLATE);
            response.setContentType("text/html");
            response.getWriter().write(writer.toString());
            writer.close();
        } catch (IOException | GeneralException | SAXException | ParserConfigurationException | TemplateException x) {
            Debug.logError("Multiple errors rendering FOP", MODULE);
            throw new ViewHandlerException("Multiple errors rendering FOP", x);
        }
    }
}

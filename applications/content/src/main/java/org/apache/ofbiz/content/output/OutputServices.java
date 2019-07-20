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
package org.apache.ofbiz.content.output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.view.ApacheFopWorker;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.fo.FoFormRenderer;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;


/**
 * Output Services
 */
public class OutputServices {

    public final static String module = OutputServices.class.getName();

    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    public static final String resource = "ContentUiLabels";

    public static Map<String, Object> sendPrintFromScreen(DispatchContext dctx, Map<String, ? extends Object> serviceContext) {
        Locale locale = (Locale) serviceContext.get("locale");
        VisualTheme visualTheme = (VisualTheme) serviceContext.get("visualTheme");
        if (visualTheme == null) {
            visualTheme = ThemeFactory.resolveVisualTheme(null);
        }        
        String screenLocation = (String) serviceContext.remove("screenLocation");
        Map<String, Object> screenContext = UtilGenerics.cast(serviceContext.remove("screenContext"));
        String contentType = (String) serviceContext.remove("contentType");
        String printerContentType = (String) serviceContext.remove("printerContentType");

        if (UtilValidate.isEmpty(screenContext)) {
            screenContext = new HashMap<>();
        }
        screenContext.put("locale", locale);
        if (UtilValidate.isEmpty(contentType)) {
            contentType = "application/postscript";
        }
        if (UtilValidate.isEmpty(printerContentType)) {
            printerContentType = contentType;
        }

        try {

            MapStack<String> screenContextTmp = MapStack.create();
            screenContextTmp.put("locale", locale);

            Writer writer = new StringWriter();
            // substitute the freemarker variables...
            ScreenStringRenderer foScreenStringRenderer = new MacroScreenRenderer(visualTheme.getModelTheme().getType("screenfop"),
                            visualTheme.getModelTheme().getScreenRendererLocation("screenfop"));

            ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContextTmp, foScreenStringRenderer);
            screensAtt.populateContextForService(dctx, screenContext);
            screenContextTmp.putAll(screenContext);
            screensAtt.getContext().put("formStringRenderer", foFormRenderer);
            screensAtt.render(screenLocation);

            // create the input stream for the generation
            StreamSource src = new StreamSource(new StringReader(writer.toString()));

            // create the output stream for the generation
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Fop fop = ApacheFopWorker.createFopInstance(baos, MimeConstants.MIME_PDF);
            ApacheFopWorker.transform(src, null, fop);

            baos.flush();
            baos.close();

            // Print is sent
            DocFlavor psInFormat = new DocFlavor.INPUT_STREAM(printerContentType);
            InputStream bais = new ByteArrayInputStream(baos.toByteArray());

            DocAttributeSet docAttributeSet = new HashDocAttributeSet();
            List<Object> docAttributes = UtilGenerics.checkList(serviceContext.remove("docAttributes"));
            if (UtilValidate.isNotEmpty(docAttributes)) {
                for (Object da : docAttributes) {
                    Debug.logInfo("Adding DocAttribute: " + da, module);
                    docAttributeSet.add((DocAttribute) da);
                }
            }

            Doc myDoc = new SimpleDoc(bais, psInFormat, docAttributeSet);

            PrintService printer = null;

            // lookup the print service for the supplied printer name
            String printerName = (String) serviceContext.remove("printerName");
            if (UtilValidate.isNotEmpty(printerName)) {

                PrintServiceAttributeSet printServiceAttributes = new HashPrintServiceAttributeSet();
                printServiceAttributes.add(new PrinterName(printerName, locale));

                PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, printServiceAttributes);
                if (printServices.length > 0) {
                    printer = printServices[0];
                    Debug.logInfo("Using printer: " + printer.getName(), module);
                    if (!printer.isDocFlavorSupported(psInFormat)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPrinterNotSupportDocFlavorFormat", UtilMisc.toMap("psInFormat", psInFormat, "printerName", printer.getName()), locale));
                    }
                }
                if (printer == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPrinterNotFound", UtilMisc.toMap("printerName", printerName), locale));
                }

            } else {

                // if no printer name was supplied, try to get the default printer
                printer = PrintServiceLookup.lookupDefaultPrintService();
                if (printer != null) {
                    Debug.logInfo("No printer name supplied, using default printer: " + printer.getName(), module);
                }
            }

            if (printer == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPrinterNotAvailable", locale));
            }

            PrintRequestAttributeSet praset = new HashPrintRequestAttributeSet();
            List<Object> printRequestAttributes = UtilGenerics.checkList(serviceContext.remove("printRequestAttributes"));
            if (UtilValidate.isNotEmpty(printRequestAttributes)) {
                for (Object pra : printRequestAttributes) {
                    Debug.logInfo("Adding PrintRequestAttribute: " + pra, module);
                    praset.add((PrintRequestAttribute) pra);
                }
            }
            DocPrintJob job = printer.createPrintJob();
            job.print(myDoc, praset);
        } catch (PrintException | IOException | TemplateException | GeneralException | SAXException | ParserConfigurationException e) {
            Debug.logError(e, "Error rendering [" + contentType + "]: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentRenderingError", UtilMisc.toMap("contentType", contentType, "errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createFileFromScreen(DispatchContext dctx, Map<String, ? extends Object> serviceContext) {
        Locale locale = (Locale) serviceContext.get("locale");
        Delegator delegator = dctx.getDelegator();
        VisualTheme visualTheme = (VisualTheme) serviceContext.get("visualTheme");
        if (visualTheme == null) {
            visualTheme = ThemeFactory.resolveVisualTheme(null);
        }        
        String screenLocation = (String) serviceContext.remove("screenLocation");
        Map<String, Object> screenContext = UtilGenerics.cast(serviceContext.remove("screenContext"));
        String contentType = (String) serviceContext.remove("contentType");
        String filePath = (String) serviceContext.remove("filePath");
        String fileName = (String) serviceContext.remove("fileName");

        if (UtilValidate.isEmpty(screenContext)) {
            screenContext = new HashMap<>();
        }
        screenContext.put("locale", locale);
        if (UtilValidate.isEmpty(contentType)) {
            contentType = "application/pdf";
        }

        try {
            MapStack<String> screenContextTmp = MapStack.create();
            screenContextTmp.put("locale", locale);

            Writer writer = new StringWriter();
            // substitute the freemarker variables...
            ScreenStringRenderer foScreenStringRenderer = new MacroScreenRenderer(visualTheme.getModelTheme().getType("screenfop"),
                    visualTheme.getModelTheme().getScreenRendererLocation("screenfop"));
            ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContextTmp, foScreenStringRenderer);
            screensAtt.populateContextForService(dctx, screenContext);
            screenContextTmp.putAll(screenContext);
            screensAtt.getContext().put("formStringRenderer", foFormRenderer);
            screensAtt.render(screenLocation);

            // create the input stream for the generation
            StreamSource src = new StreamSource(new StringReader(writer.toString()));

            // create the output stream for the generation
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Fop fop = ApacheFopWorker.createFopInstance(baos, MimeConstants.MIME_PDF);
            ApacheFopWorker.transform(src, null, fop);

            baos.flush();
            baos.close();

            fileName += UtilDateTime.nowAsString();
            if ("application/pdf".equals(contentType)) {
                fileName += ".pdf";
            } else if ("application/postscript".equals(contentType)) {
                fileName += ".ps";
            } else if ("text/plain".equals(contentType)) {
                fileName += ".txt";
            }
            if (UtilValidate.isEmpty(filePath)) {
                filePath = EntityUtilProperties.getPropertyValue("content", "content.output.path", "/output", delegator);
            }
            File file = new File(filePath, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.close();

        } catch (IOException | TemplateException | GeneralException | SAXException | ParserConfigurationException e) {
            Debug.logError(e, "Error rendering [" + contentType + "]: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentRenderingError", UtilMisc.toMap("contentType", contentType, "errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

}

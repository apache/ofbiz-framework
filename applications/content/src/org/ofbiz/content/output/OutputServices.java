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
package org.ofbiz.content.output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.standard.PrinterName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.fo.FoScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;

import javolution.util.FastMap;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;


/**
 * Output Services
 */
public class OutputServices {

    public final static String module = OutputServices.class.getName();

    protected static final FoScreenRenderer foScreenRenderer = new FoScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();

    public static Map<String, Object> sendPrintFromScreen(DispatchContext dctx, Map<String, ? extends Object> serviceContext) {

        Locale locale = (Locale) serviceContext.get("locale");
        String screenLocation = (String) serviceContext.remove("screenLocation");
        Map screenContext = (Map) serviceContext.remove("screenContext");
        String contentType = (String) serviceContext.remove("contentType");
        String printerContentType = (String) serviceContext.remove("printerContentType");
        String printerName = (String) serviceContext.remove("printerName");

        if (UtilValidate.isEmpty(screenContext)) {
            screenContext = FastMap.newInstance();
        }
        screenContext.put("locale", locale);
        if (UtilValidate.isEmpty(contentType)) {
            contentType = "application/postscript";
        }
        if (UtilValidate.isEmpty(printerContentType)) {
            printerContentType = contentType;
        }

        try {

            MapStack screenContextTmp = MapStack.create();
            screenContextTmp.put("locale", locale);

            Writer writer = new StringWriter();
            // substitute the freemarker variables...
            ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContextTmp, foScreenRenderer);
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
            List docAttributes = (List) serviceContext.remove("docAttributes");
            if (UtilValidate.isNotEmpty(docAttributes)) {
                for (Object da : docAttributes) {
                    Debug.logInfo("Adding DocAttribute: " + da, module);
                    docAttributeSet.add((DocAttribute) da);
                }
            }

            Doc myDoc = new SimpleDoc(bais, psInFormat, docAttributeSet);

            PrintService[] services = PrintServiceLookup.lookupPrintServices(psInFormat, null);
            PrintService printer = null;
            if (services.length > 0) {
                if (UtilValidate.isNotEmpty(printerName)) {
                    String sPrinterName = null;
                    for (PrintService service : services) {
                        PrintServiceAttribute attr = service.getAttribute(PrinterName.class);
                        sPrinterName = ((PrinterName) attr).getValue();
                        if (sPrinterName.toLowerCase().indexOf(printerName.toLowerCase()) >= 0) {
                            printer = service;
                            Debug.logInfo("Printer with name [" + sPrinterName + "] selected", module);
                            break;
                        }
                    }
                }
                if (printer == null) {
                    printer = services[0];
                }
            }
            if (printer != null) {
                PrintRequestAttributeSet praset = new HashPrintRequestAttributeSet();
                List printRequestAttributes = (List) serviceContext.remove("printRequestAttributes");
                if (UtilValidate.isNotEmpty(printRequestAttributes)) {
                    for (Object pra : printRequestAttributes) {
                        Debug.logInfo("Adding PrintRequestAttribute: " + pra, module);
                        praset.add((PrintRequestAttribute) pra);
                    }
                }
                DocPrintJob job = printer.createPrintJob();
                job.print(myDoc, praset);
            } else {
                String errMsg = "No printer found with name: " + printerName;
                Debug.logError(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }

        } catch (PrintException pe) {
            String errMsg = "Error printing [" + contentType + "]: " + pe.toString();
            Debug.logError(pe, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GeneralException ge) {
            String errMsg = "Error rendering [" + contentType + "]: " + ge.toString();
            Debug.logError(ge, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (IOException ie) {
            String errMsg = "Error rendering [" + contentType + "]: " + ie.toString();
            Debug.logError(ie, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (FOPException fe) {
            String errMsg = "Error rendering [" + contentType + "]: " + fe.toString();
            Debug.logError(fe, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (SAXException se) {
            String errMsg = "Error rendering [" + contentType + "]: " + se.toString();
            Debug.logError(se, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (ParserConfigurationException pe) {
            String errMsg = "Error rendering [" + contentType + "]: " + pe.toString();
            Debug.logError(pe, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createFileFromScreen(DispatchContext dctx, Map<String, ? extends Object> serviceContext) {

        Locale locale = (Locale) serviceContext.get("locale");
        String screenLocation = (String) serviceContext.remove("screenLocation");
        Map screenContext = (Map) serviceContext.remove("screenContext");
        String contentType = (String) serviceContext.remove("contentType");
        String filePath = (String) serviceContext.remove("filePath");
        String fileName = (String) serviceContext.remove("fileName");

        if (UtilValidate.isEmpty(screenContext)) {
            screenContext = FastMap.newInstance();
        }
        screenContext.put("locale", locale);
        if (UtilValidate.isEmpty(contentType)) {
            contentType = "application/pdf";
        }

        try {
            MapStack screenContextTmp = MapStack.create();
            screenContextTmp.put("locale", locale);

            Writer writer = new StringWriter();
            // substitute the freemarker variables...
            ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContextTmp, foScreenRenderer);
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
                filePath = UtilProperties.getPropertyValue("content.properties", "content.output.path", "/output");
            }
            File file = new File(filePath, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.close();

        } catch (GeneralException ge) {
            String errMsg = "Error rendering [" + contentType + "]: " + ge.toString();
            Debug.logError(ge, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (IOException ie) {
            String errMsg = "Error rendering [" + contentType + "]: " + ie.toString();
            Debug.logError(ie, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (FOPException fe) {
            String errMsg = "Error rendering [" + contentType + "]: " + fe.toString();
            Debug.logError(fe, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (SAXException se) {
            String errMsg = "Error rendering [" + contentType + "]: " + se.toString();
            Debug.logError(se, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (ParserConfigurationException pe) {
            String errMsg = "Error rendering [" + contentType + "]: " + pe.toString();
            Debug.logError(pe, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

}

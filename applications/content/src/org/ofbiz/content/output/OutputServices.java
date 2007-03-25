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

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.view.ApacheFopFactory;
import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.SimpleDoc;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterURI;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * Email Services
 */
public class OutputServices {

    public final static String module = OutputServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    
    public static Map sendPrintFromScreen(DispatchContext dctx, Map serviceContext) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
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
            ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContextTmp, htmlScreenRenderer);
            screensAtt.populateContextForService(dctx, screenContext);
            screenContextTmp.putAll(screenContext);
            screensAtt.getContext().put("formStringRenderer", foFormRenderer);
            screensAtt.render(screenLocation);

            // create the in/output stream for the generation
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            FopFactory fopFactory = ApacheFopFactory.instance();
            Fop fop = fopFactory.newFop(contentType, baos);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();

            Reader reader = new StringReader(writer.toString());
            Source src = new StreamSource(reader);
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
            // and generate the stream
            baos.flush();
            baos.close();

            // We don't want to cache the images that get loaded by the FOP engine
            fopFactory.getImageFactory().clearCaches();

            // Print is sent
            DocFlavor psInFormat = new DocFlavor.INPUT_STREAM(printerContentType);
            InputStream bais = new ByteArrayInputStream(baos.toByteArray());
            
            Doc myDoc = new SimpleDoc(bais, psInFormat, null);
            PrintServiceAttributeSet psaset = new HashPrintServiceAttributeSet();
            if (UtilValidate.isNotEmpty(printerName)) {
                try {
                    URI printerUri = new URI(printerName);
                    PrinterURI printerUriObj = new PrinterURI(printerUri);
                    psaset.add(printerUriObj);
                } catch (URISyntaxException ue) {
                    Debug.logWarning(ue, "Invalid URI for printer [" + printerName + "]", module);
                }
            }
            //PrintService[] services = PrintServiceLookup.lookupPrintServices(psInFormat, psaset); // TODO: selecting the printer by URI seems to not work
            PrintService[] services = PrintServiceLookup.lookupPrintServices(psInFormat, null);
            PrintService printer = null;
            if (services.length > 0) {
                if (UtilValidate.isNotEmpty(printerName)) {
                    String sPrinterName = null;
                    for (int i = 0; i < services.length; i++) {
                        PrintServiceAttribute attr = services[i].getAttribute(PrinterName.class);
                        sPrinterName = ((PrinterName)attr).getValue();
                        if (sPrinterName.toLowerCase().indexOf(printerName.toLowerCase()) >= 0) {
                            printer = services[i];
                            Debug.logInfo("Printer with name [" + sPrinterName +"] selected", module);
                            break;
                        }
                    }
                }
                if (UtilValidate.isEmpty(printer)) {
                    printer = services[0];
                }
            }
            if (UtilValidate.isNotEmpty(printer)) {
                PrintRequestAttributeSet praset = new HashPrintRequestAttributeSet();
                praset.add(new Copies(1));
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
        } catch (TransformerConfigurationException tce) {
            String errMsg = "FOP TransformerConfiguration Exception: " + tce.toString();
            return ServiceUtil.returnError(errMsg);
        } catch (TransformerException te) {
            String errMsg = "FOP transform failed: " + te.toString();
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

        Map result = ServiceUtil.returnSuccess();
        return result;
    }

    public static Map createFileFromScreen(DispatchContext dctx, Map serviceContext) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
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
            ScreenRenderer screensAtt = new ScreenRenderer(writer, screenContextTmp, htmlScreenRenderer);
            screensAtt.populateContextForService(dctx, screenContext);
            screenContextTmp.putAll(screenContext);
            screensAtt.getContext().put("formStringRenderer", foFormRenderer);
            screensAtt.render(screenLocation);

            // create the in/output stream for the generation
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            FopFactory fopFactory = ApacheFopFactory.instance();
            Fop fop = fopFactory.newFop(contentType, baos);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();

            Reader reader = new StringReader(writer.toString());
            Source src = new StreamSource(reader);
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
            // and generate the stream
            baos.flush();
            baos.close();

            // We don't want to cache the images that get loaded by the FOP engine
            fopFactory.getImageFactory().clearCaches();

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
        } catch (TransformerConfigurationException tce) {
            String errMsg = "FOP TransformerConfiguration Exception: " + tce.toString();
            return ServiceUtil.returnError(errMsg);
        } catch (TransformerException te) {
            String errMsg = "FOP transform failed: " + te.toString();
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

        Map result = ServiceUtil.returnSuccess();
        return result;
    }

}

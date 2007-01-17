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

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.view.ApacheFopFactory;
import org.ofbiz.widget.html.HtmlScreenRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.Sides;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

public class ExamplePrintServices {
    public static final String module = ExamplePrintServices.class.getName();

    private static HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();

    public static Map printReportPdf(DispatchContext dctx, Map context) {
        String screenLocation = "component://example/widget/example/ExampleReportScreens.xml";
        String reportScreenName = "ExampleReport";
        
        // render a screen to get the XML document
        Writer reportWriter = new StringWriter();
        ScreenRenderer reportScreenRenderer = new ScreenRenderer(reportWriter, null, htmlScreenRenderer);
        reportScreenRenderer.populateContextForService(dctx, context);

        // put the exampleId in the screen context, is a parameter coming into the service
        //reportScreenRenderer.getContext().put("exampleId", context.get("exampleId"));
        
        try {
            reportScreenRenderer.render(screenLocation, reportScreenName);
        } catch (GeneralException e) {
            String errMsg = "General error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (IOException e) {
            String errMsg = "IO error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (SAXException e) {
            String errMsg = "SAX (XML parse) error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (ParserConfigurationException e) {
            String errMsg = "Parser configuration error rendering screen [" + screenLocation + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        String reportXmlDocument = reportWriter.toString();

        // create the in/output stream for the generation
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        
        FopFactory fopFactory;
        try {
            fopFactory = ApacheFopFactory.instance();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();

            // set the input source (XSL-FO) and generate the PDF
            Reader reader = new StringReader(reportXmlDocument);
            Source src = new StreamSource(reader);
            
            // load the FOP driver

            // Get handler that is used in the generation process
            Result res = new SAXResult(fop.getDefaultHandler());
            
            // read the XSL-FO XML into the W3 Document
            
            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
            // and generate the PDF
            // We don't want to cache the images that get loaded by the FOP engine
            fopFactory.getImageFactory().clearCaches();
            
        } catch (FOPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
        
        /*
        // set the content type and length                    
        response.setContentType("application/pdf");        
        response.setContentLength(out.size());
        
        // write to the browser
        try {
            out.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new ViewHandlerException("Unable write to browser OutputStream", e);            
        }
        */                             
        
        DocFlavor docFlavor = DocFlavor.BYTE_ARRAY.PDF;
        Doc myDoc = new SimpleDoc(out.toByteArray(), docFlavor, null);
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(new Copies(1));
        //aset.add(MediaSize.A4);
        aset.add(Sides.ONE_SIDED);
        
        PrintService[] services = PrintServiceLookup.lookupPrintServices(docFlavor, aset);
        if (services.length > 0) { 
        	DocPrintJob job = services[0].createPrintJob(); 
            try {
            	job.print(myDoc, aset); 
            } catch (PrintException pe) {
                String errMsg = "Unable to print PDF from XSL-FO: " + pe.toString();
                Debug.logError(pe, errMsg, module);
                return ServiceUtil.returnError(errMsg);
            } 
        } 

        return ServiceUtil.returnSuccess();
    }
}

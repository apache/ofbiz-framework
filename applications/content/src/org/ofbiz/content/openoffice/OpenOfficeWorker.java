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
package org.ofbiz.content.openoffice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uri.ExternalUriReferenceTranslator;

/**
 * OpenOfficeWorker Class
 * 
 * Note that for this to work you must start OpenOffice with a command such as the following: 
 *   <code>soffice -accept=socket,host=localhost,port=8100;urp;</code>
 */
public class OpenOfficeWorker{

    public static final String module = OpenOfficeWorker.class.getName();
    
    /**
     * Use OpenOffice to convert documents between types
     */
    public static XMultiComponentFactory getRemoteServer(String host, String port) throws IOException, Exception {
        
        if (UtilValidate.isEmpty(host)) host = UtilProperties.getPropertyValue("openoffice-uno", "oo.host", "localhost");
        if (UtilValidate.isEmpty(port)) port = UtilProperties.getPropertyValue("openoffice-uno", "oo.port", "8100");
        
        XMultiComponentFactory xmulticomponentfactory = null;
        XComponentContext xcomponentcontext = null;
        Object objectUrlResolver = null;
        XUnoUrlResolver xurlresolver = null;
        Object objectInitial = null;
        // Converting the document to the favoured type
        try {
            /* Bootstraps a component context with the jurt base components
            registered. Component context to be granted to a component for running.
            Arbitrary values can be retrieved from the context. */
            xcomponentcontext = com.sun.star.comp.helper.Bootstrap.createInitialComponentContext(null);
         
            /* Gets the service manager instance to be used (or null). This method has
            been added for convenience, because the service manager is a often used
            object. */
            xmulticomponentfactory = xcomponentcontext.getServiceManager();
         
            /* Creates an instance of the component UnoUrlResolver which
            supports the services specified by the factory. */
            objectUrlResolver = xmulticomponentfactory.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", xcomponentcontext);
         
            // Create a new url resolver
            xurlresolver = (XUnoUrlResolver) UnoRuntime.queryInterface(XUnoUrlResolver.class, objectUrlResolver);
         
            // Resolves an object that is specified as follow:
            // uno:<connection description>;<protocol description>;<initial object name>
            String url = "uno:socket,host=" + host + ",port=" + port + ";urp;StarOffice.ServiceManager";
            objectInitial = xurlresolver.resolve(url);
         
            // Create a service manager from the initial object
            xmulticomponentfactory = (XMultiComponentFactory) UnoRuntime.queryInterface(XMultiComponentFactory.class, objectInitial);
        } catch(Exception e) {
            // TODO: None of this works. Need a programmable start solution.
            //String ooxvfb = UtilProperties.getPropertyValue("openoffice-uno", "oo.start.xvfb");
            //String ooexport = UtilProperties.getPropertyValue("openoffice-uno", "oo.start.export");
           // String oosoffice = UtilProperties.getPropertyValue("openoffice-uno", "oo.start.soffice");
            //Process procXvfb = Runtime.getRuntime().exec(ooxvfb);
            //Process procExport = Runtime.getRuntime().exec(ooexport);
            /*
            Process procSoffice = Runtime.getRuntime().exec(oosoffice);
            Thread.sleep(3000);
            objectInitial = xurlresolver.resolve("uno:socket,host=" + host + ",port=" + port + ";urp;StarOffice.ServiceManager");
            xmulticomponentfactory = (XMultiComponentFactory) UnoRuntime.queryInterface(XMultiComponentFactory.class, objectInitial);
            Debug.logInfo("soffice started. " + procSoffice, module);
            */
            String errMsg = "Error connecting to OpenOffice with host [" + host + "] and port [" + port + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
            throw new IllegalArgumentException(errMsg);
        }
        
        return xmulticomponentfactory;
    }
 
    public static String listFilterNamesEvent(HttpServletRequest request, HttpServletResponse response) {
        XMultiComponentFactory factory = null;
        
        try {
            factory = getRemoteServer("localhost", "8100");
            List filterList = getFilterNames(factory);
            request.setAttribute("filterList", filterList);
        } catch(IOException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        } catch(Exception e) {
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return "error";
        }
        return "success";
    }    
    
    public static List getFilterNames(XMultiComponentFactory xmulticomponentfactory) throws Exception {
        XPropertySet xPropertySet = (XPropertySet)UnoRuntime.queryInterface(XPropertySet.class, xmulticomponentfactory);        
        Object oDefaultContext = xPropertySet.getPropertyValue("DefaultContext");
        XComponentContext xComponentContext = (XComponentContext) UnoRuntime.queryInterface(XComponentContext.class, oDefaultContext);


        Object filterFactory = xmulticomponentfactory.createInstanceWithContext("com.sun.star.document.FilterFactory", xComponentContext);
        XNameAccess xNameAccess = (XNameAccess)UnoRuntime.queryInterface(XNameAccess.class, filterFactory);
        String [] filterNames = xNameAccess.getElementNames();
        
        //String [] serviceNames = filterFactory.getAvailableServiceNames();
        for (int i=0; i < filterNames.length; i++) {
            String s = filterNames[i];
            Debug.logInfo(s, module);
            /*
            if (s.toLowerCase().indexOf("filter") >= 0) {
                Debug.logInfo("FILTER: " + s, module);
            }
            if (s.toLowerCase().indexOf("desktop") >= 0) {
                Debug.logInfo("DESKTOP: " + s, module);
            }
            */
        }

        List filterNameList = UtilMisc.toListArray(filterNames);
        return filterNameList;
    }
    
    public static void convertOODocToFile(XMultiComponentFactory xmulticomponentfactory, String fileInPath, String fileOutPath, String outputMimeType) throws FileNotFoundException, IOException, MalformedURLException, Exception {
        // Converting the document to the favoured type
        // Query for the XPropertySet interface.
        XPropertySet xpropertysetMultiComponentFactory = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xmulticomponentfactory);
        
        // Get the default context from the office server.
        Object objectDefaultContext = xpropertysetMultiComponentFactory.getPropertyValue("DefaultContext");
        
        // Query for the interface XComponentContext.
        XComponentContext xcomponentcontext = (XComponentContext) UnoRuntime.queryInterface(XComponentContext.class, objectDefaultContext);
        
        /* A desktop environment contains tasks with one or more
           frames in which components can be loaded. Desktop is the
           environment for components which can instanciate within
           frames. */
        
        Object desktopObj = xmulticomponentfactory.createInstanceWithContext("com.sun.star.frame.Desktop", xcomponentcontext);
        //XDesktop desktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktopObj);
        XComponentLoader xcomponentloader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, desktopObj);
       
        
        // Preparing properties for loading the document
        PropertyValue propertyvalue[] = new PropertyValue[ 2 ];
        // Setting the flag for hidding the open document
        propertyvalue[ 0 ] = new PropertyValue();
        propertyvalue[ 0 ].Name = "Hidden";
        propertyvalue[ 0 ].Value = new Boolean(false);

        propertyvalue[ 1 ] = new PropertyValue();
        propertyvalue[ 1 ].Name = "UpdateDocMode";
        propertyvalue[ 1 ].Value = "1";

        // Loading the wanted document
        String stringUrl = convertToUrl(fileInPath, xcomponentcontext);
        Debug.logInfo("stringUrl:" + stringUrl, module);
        Object objectDocumentToStore = xcomponentloader.loadComponentFromURL(stringUrl, "_blank", 0, propertyvalue);
        
        // Getting an object that will offer a simple way to store a document to a URL. 
        XStorable xstorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, objectDocumentToStore);
        
        // Preparing properties for converting the document
        propertyvalue = new PropertyValue[ 3 ];
        // Setting the flag for overwriting
        propertyvalue[ 0 ] = new PropertyValue();
        propertyvalue[ 0 ].Name = "Overwrite";
        propertyvalue[ 0 ].Value = new Boolean(true);
        // Setting the filter name
        // Preparing properties for converting the document
        String filterName = getFilterNameFromMimeType(outputMimeType);
        
        propertyvalue[ 1 ] = new PropertyValue();
        propertyvalue[ 1 ].Name = "FilterName";
        propertyvalue[ 1 ].Value = filterName;  
        
        propertyvalue[2] = new PropertyValue();
        propertyvalue[2].Name = "CompressionMode";
        propertyvalue[2].Value = "1";
        
        // Storing and converting the document
        //File newFile = new File(stringConvertedFile);
        //newFile.createNewFile();
        
        String stringConvertedFile = convertToUrl(fileOutPath, xcomponentcontext);
        Debug.logInfo("stringConvertedFile: "+stringConvertedFile, module);
        xstorable.storeToURL(stringConvertedFile, propertyvalue);
        
        // Getting the method dispose() for closing the document
        XComponent xcomponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, xstorable);
        
        // Closing the converted document
        xcomponent.dispose();
        return;
    }
    
    public static OpenOfficeByteArrayOutputStream convertOODocByteStreamToByteStream(XMultiComponentFactory xmulticomponentfactory, 
            OpenOfficeByteArrayInputStream is, String inputMimeType, String outputMimeType) throws Exception {
        
        // Query for the XPropertySet interface.
        XPropertySet xpropertysetMultiComponentFactory = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xmulticomponentfactory);
        
        // Get the default context from the office server.
        Object objectDefaultContext = xpropertysetMultiComponentFactory.getPropertyValue("DefaultContext");
        
        // Query for the interface XComponentContext.
        XComponentContext xcomponentcontext = (XComponentContext) UnoRuntime.queryInterface(XComponentContext.class, objectDefaultContext);
        
        /* A desktop environment contains tasks with one or more
           frames in which components can be loaded. Desktop is the
           environment for components which can instanciate within
           frames. */
        
        Object desktopObj = xmulticomponentfactory.createInstanceWithContext("com.sun.star.frame.Desktop", xcomponentcontext);
        //XDesktop desktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktopObj);
        XComponentLoader xcomponentloader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, desktopObj);
        
        // Preparing properties for loading the document
        PropertyValue propertyvalue[] = new PropertyValue[2];
        // Setting the flag for hidding the open document
        propertyvalue[0] = new PropertyValue();
        propertyvalue[0].Name = "Hidden";
        propertyvalue[0].Value = Boolean.TRUE;
        //
        propertyvalue[1] = new PropertyValue();
        propertyvalue[1].Name = "InputStream";
        propertyvalue[1].Value = is;
        
        // Loading the wanted document
        Object objectDocumentToStore = xcomponentloader.loadComponentFromURL("private:stream", "_blank", 0, propertyvalue);
        if (objectDocumentToStore == null) {
            Debug.logError("Could not get objectDocumentToStore object from xcomponentloader.loadComponentFromURL", module);
        }
        
        // Getting an object that will offer a simple way to store a document to a URL.
        XStorable xstorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, objectDocumentToStore);
        if (xstorable == null) {
            Debug.logError("Could not get XStorable object from UnoRuntime.queryInterface", module);
        }
        
        // Preparing properties for converting the document
        String filterName = getFilterNameFromMimeType(outputMimeType);
        propertyvalue = new PropertyValue[4];
        
        propertyvalue[0] = new PropertyValue();
        propertyvalue[0].Name = "OutputStream";
        OpenOfficeByteArrayOutputStream os = new OpenOfficeByteArrayOutputStream();
        propertyvalue[0].Value = os;
        // Setting the filter name
        propertyvalue[1] = new PropertyValue();
        propertyvalue[1].Name = "FilterName";
        propertyvalue[1].Value = filterName;
        // Setting the flag for overwriting
        propertyvalue[3] = new PropertyValue();
        propertyvalue[3].Name = "Overwrite";
        propertyvalue[3].Value = Boolean.TRUE;
        // For PDFs
        propertyvalue[2] = new PropertyValue();
        propertyvalue[2].Name = "CompressionMode";
        propertyvalue[2].Value = "1";
        
        xstorable.storeToURL("private:stream", propertyvalue);
        //xstorable.storeToURL("file:///home/byersa/testdoc1_file.pdf", propertyvalue);
        
        // Getting the method dispose() for closing the document
        XComponent xcomponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, xstorable);
        
        // Closing the converted document
        xcomponent.dispose();
      
        return os;
    }
    
    public static String getFilterNameFromMimeType(String mimeType) {
        String filterName = "";
        if (UtilValidate.isEmpty(mimeType)) {
            filterName = "HTML";
        } else if (mimeType.equalsIgnoreCase("application/pdf")) {
            filterName = "writer_pdf_Export";
        } else if (mimeType.equalsIgnoreCase("application/msword")) {
            filterName = "MS Word 97";
        } else if (mimeType.equalsIgnoreCase("text/html")) {
            filterName = "HTML (StarWriter)";
        } else {
            filterName = "HTML";
        }
        return filterName;

    }
    
    public static String getExtensionFromMimeType(String mimeType) {
        String extension = "";
        if (UtilValidate.isEmpty(mimeType)) {
            extension = "html";
        } else if (mimeType.equalsIgnoreCase("application/pdf")) {
            extension = "pdf";
        } else if (mimeType.equalsIgnoreCase("application/msword")) {
            extension = "doc";
        } else if (mimeType.equalsIgnoreCase("text/html")) {
            extension = "html";
        } else {
            extension = "html";
        }
        return extension;

    }
    public static String convertToUrl(String filePath, XComponentContext xComponentContext ) throws MalformedURLException {

    	String returnUrl = null;
    	File f = new File(filePath);
    	URL u = f.toURL();
        returnUrl =  ExternalUriReferenceTranslator.create(xComponentContext).translateToInternal(u.toExternalForm());

    	return returnUrl;
    }
    
}

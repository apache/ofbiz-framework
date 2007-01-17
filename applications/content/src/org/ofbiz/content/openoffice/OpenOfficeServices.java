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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Random;
import java.sql.Timestamp;
import java.lang.Math;
import java.net.MalformedURLException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * OpenOfficeServices Class
 */
public class OpenOfficeServices {
    public static final String module = OpenOfficeServices.class.getName();

    /**
     * Use OpenOffice to convert documents between types
     * This service requires that the "content.temp.dir" directory be set in the content.properties file.
     * This value should be operating system dependent with "\\" separators for Windows
     * and "/" for Linux/Unix.
     */
    public static Map convertDocumentByteWrapper(DispatchContext dctx, Map context) {
        
        Map results = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();
        XMultiComponentFactory xmulticomponentfactory = null;
        //String uniqueSeqNum = delegator.getNextSeqId("OOTempDir");
        Timestamp ts = UtilDateTime.nowTimestamp();
        Random random = new Random(ts.getTime());
        String uniqueSeqNum = Integer.toString(Math.abs(random.nextInt()));
        String fileInName = "OOIN_" + uniqueSeqNum;
        String fileOutName = "OOOUT_" + uniqueSeqNum;
        File fileIn = null;
        File fileOut = null;
        
        ByteWrapper inByteWrapper = (ByteWrapper) context.get("inByteWrapper");
        String inputMimeType = (String) context.get("inputMimeType");
        String outputMimeType = (String) context.get("outputMimeType");
        String extName = OpenOfficeWorker.getExtensionFromMimeType(outputMimeType);
        fileOutName += "." + extName;

        // if these are empty don't worry, the OpenOfficeWorker down below will take care of it
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        
        try {   
            xmulticomponentfactory = OpenOfficeWorker.getRemoteServer(oooHost, oooPort);
            byte[] inByteArray = inByteWrapper.getBytes();
            
            // The following line work in linux, but not Windows or Mac environment. It is preferred because it does not use temporary files
            //OpenOfficeByteArrayInputStream oobais = new OpenOfficeByteArrayInputStream(inByteArray);
            //Debug.logInfo("Doing convertDocumentByteWrapper, inBytes size is [" + inByteArray.length + "]", module);
             //OpenOfficeByteArrayOutputStream baos = OpenOfficeWorker.convertOODocByteStreamToByteStream(xmulticomponentfactory, oobais, inputMimeType, outputMimeType);
            
            
            String tempDir = UtilProperties.getPropertyValue("content", "content.temp.dir");
            fileIn = new File(tempDir + fileInName);
            FileOutputStream fos = new FileOutputStream(fileIn);
            fos.write(inByteArray);
            fos.close();
            Debug.logInfo("fileIn:" + tempDir + fileInName, module);
            OpenOfficeWorker.convertOODocToFile(xmulticomponentfactory, tempDir + fileInName, tempDir + fileOutName, outputMimeType);
            fileOut = new File(tempDir + fileOutName);
            FileInputStream fis = new FileInputStream(fileOut);
            int c;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((c=fis.read()) > -1) {
                baos.write(c);
            }
            fis.close();
            
            results.put("outByteWrapper", new ByteWrapper(baos.toByteArray()));
            baos.close();

        } catch (MalformedURLException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        } catch (FileNotFoundException e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } finally {
            if (fileIn != null) fileIn.delete();
            if (fileOut != null)  fileOut.delete();
        }
        return results;
    }

    /**
     * Use OpenOffice to convert documents between types
     */
    public static Map convertDocument(DispatchContext dctx, Map context) {
        XMultiComponentFactory xmulticomponentfactory = null;
        
        String stringUrl = "file:///" + context.get("filenameFrom");
        String stringConvertedFile = "file:///" + context.get("filenameTo");
        String filterName = "file:///" + context.get("filterName");

        // if these are empty don't worry, the OpenOfficeWorker down below will take care of it
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        
        try {    
            xmulticomponentfactory = OpenOfficeWorker.getRemoteServer(oooHost, oooPort);
            OpenOfficeWorker.convertOODocToFile(xmulticomponentfactory, stringUrl, stringConvertedFile, filterName);
            
            Map results = ServiceUtil.returnSuccess();
            return results;
        } catch (IOException e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
    }

    /**
     * Use OpenOffice to convert documents between types
     */
    public static Map convertDocumentFileToFile(DispatchContext dctx, Map context) {
        XMultiComponentFactory xmulticomponentfactory = null;
        
        String stringUrl = (String) context.get("filenameFrom");
        String stringConvertedFile = (String) context.get("filenameTo");
        String inputMimeType = (String) context.get("inputMimeType");
        String outputMimeType = (String) context.get("outputMimeType");

        // if these are empty don't worry, the OpenOfficeWorker down below will take care of it
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        
        try {    
            xmulticomponentfactory = OpenOfficeWorker.getRemoteServer(oooHost, oooPort);
            File inputFile = new File(stringUrl);
            long fileSize = inputFile.length();
            FileInputStream fis = new FileInputStream(inputFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int)fileSize);
            int c;
            while ((c = fis.read()) != -1) {
                baos.write(c);
            }
            OpenOfficeByteArrayInputStream oobais = new OpenOfficeByteArrayInputStream(baos.toByteArray());
            OpenOfficeByteArrayOutputStream oobaos = OpenOfficeWorker.convertOODocByteStreamToByteStream(xmulticomponentfactory, oobais, inputMimeType, outputMimeType);
            FileOutputStream fos = new FileOutputStream(stringConvertedFile);
            fos.write(oobaos.toByteArray());
            fos.close();
            fis.close();
            oobais.close();
            oobaos.close();
            
            Map results = ServiceUtil.returnSuccess();
            return results;
        } catch (IOException e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
    }

    /**
     * Use OpenOffice to convert documents between types
     */
    public static Map convertDocumentStreamToStream(DispatchContext dctx, Map context) {
        XMultiComponentFactory xmulticomponentfactory = null;
        
        String stringUrl = "file:///" + context.get("filenameFrom");
        String stringConvertedFile = "file:///" + context.get("filenameTo");
        String inputMimeType = (String) context.get("inputMimeType");
        String outputMimeType = (String) context.get("outputMimeType");

        // if these are empty don't worry, the OpenOfficeWorker down below will take care of it
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        
        try {    
            xmulticomponentfactory = OpenOfficeWorker.getRemoteServer(oooHost, oooPort);
            File inputFile = new File(stringUrl);
            long fileSize = inputFile.length();
            FileInputStream fis = new FileInputStream(inputFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int)fileSize);
            int c;
            while ((c = fis.read()) != -1) {
                baos.write(c);
            }
            OpenOfficeByteArrayInputStream oobais = new OpenOfficeByteArrayInputStream(baos.toByteArray());
            OpenOfficeByteArrayOutputStream oobaos = OpenOfficeWorker.convertOODocByteStreamToByteStream(xmulticomponentfactory, oobais, inputMimeType, outputMimeType);
            FileOutputStream fos = new FileOutputStream(stringConvertedFile);
            fos.write(oobaos.toByteArray());
            fos.close();
            fis.close();
            oobais.close();
            oobaos.close();
            
            Map results = ServiceUtil.returnSuccess();
            return results;
        } catch (IOException e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
    }

    /**
     * Use OpenOffice to compare documents
     */
    public static Map compareDocuments(DispatchContext dctx, Map context) {
        XMultiComponentFactory xmulticomponentfactory = null;
        
        String stringUrl = "file:///" + context.get("filenameFrom");
        String stringOriginalFile = "file:///" + context.get("filenameOriginal");
        String stringOutFile = "file:///" + context.get("filenameOut");

        // if these are empty don't worry, the OpenOfficeWorker down below will take care of it
        String oooHost = (String)context.get("oooHost");
        String oooPort = (String)context.get("oooPort");
        
        try {    
            xmulticomponentfactory = OpenOfficeWorker.getRemoteServer(oooHost, oooPort);
        } catch (IOException e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
        //System.out.println("xmulticomponentfactory: " + xmulticomponentfactory);
       
        // Converting the document to the favoured type
        try {
            // Composing the URL
            
            
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
            XDesktop desktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktopObj);
            XComponentLoader xcomponentloader = (XComponentLoader) UnoRuntime.queryInterface(XComponentLoader.class, desktopObj);
           
            
            // Preparing properties for loading the document
            PropertyValue propertyvalue[] = new PropertyValue[ 1 ];
            // Setting the flag for hidding the open document
            propertyvalue[ 0 ] = new PropertyValue();
            propertyvalue[ 0 ].Name = "Hidden";
            propertyvalue[ 0 ].Value = new Boolean(true);
            //TODO: Hardcoding opening word documents -- this will need to change.
            //propertyvalue[ 1 ] = new PropertyValue();
            //propertyvalue[ 1 ].Name = "FilterName";
            //propertyvalue[ 1 ].Value = "HTML (StarWriter)";
            
            // Loading the wanted document
            Object objectDocumentToStore = xcomponentloader.loadComponentFromURL(stringUrl, "_blank", 0, propertyvalue);
            
            // Getting an object that will offer a simple way to store a document to a URL.
            XStorable xstorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, objectDocumentToStore);
            
            // Preparing properties for comparing the document
            propertyvalue = new PropertyValue[ 1 ];
            // Setting the flag for overwriting
            propertyvalue[ 0 ] = new PropertyValue();
            propertyvalue[ 0 ].Name = "URL";
            propertyvalue[ 0 ].Value = stringOriginalFile;
            // Setting the filter name
            //propertyvalue[ 1 ] = new PropertyValue();
            //propertyvalue[ 1 ].Name = "FilterName";
            //propertyvalue[ 1 ].Value = context.get("convertFilterName");
            XFrame frame = desktop.getCurrentFrame();
            //XFrame frame = (XFrame) UnoRuntime.queryInterface(XFrame.class, desktop);
            Object dispatchHelperObj = xmulticomponentfactory.createInstanceWithContext("com.sun.star.frame.DispatchHelper", xcomponentcontext);
            XDispatchHelper dispatchHelper = (XDispatchHelper) UnoRuntime.queryInterface(XDispatchHelper.class, dispatchHelperObj);
            XDispatchProvider dispatchProvider = (XDispatchProvider) UnoRuntime.queryInterface(XDispatchProvider.class, frame);
            dispatchHelper.executeDispatch(dispatchProvider, ".uno:CompareDocuments", "", 0, propertyvalue);       
            
            // Preparing properties for storing the document
            propertyvalue = new PropertyValue[ 1 ];
            // Setting the flag for overwriting
            propertyvalue[ 0 ] = new PropertyValue();
            propertyvalue[ 0 ].Name = "Overwrite";
            propertyvalue[ 0 ].Value = new Boolean(true);
            // Setting the filter name
            //propertyvalue[ 1 ] = new PropertyValue();
            //propertyvalue[ 1 ].Name = "FilterName";
            //propertyvalue[ 1 ].Value = context.get("convertFilterName");
            
            Debug.logInfo("stringOutFile: "+stringOutFile, module);
            // Storing and converting the document
            xstorable.storeToURL(stringOutFile, propertyvalue);
            
            // Getting the method dispose() for closing the document
            XComponent xcomponent = (XComponent) UnoRuntime.queryInterface(XComponent.class,
            xstorable);
            
            // Closing the converted document
            xcomponent.dispose();
            
            Map results = ServiceUtil.returnSuccess();
            return results;
        } catch (Exception e) {
            Debug.logError(e, "Error in OpenOffice operation: ", module);
            return ServiceUtil.returnError("Error converting document: " + e.toString());
        }
    }
}

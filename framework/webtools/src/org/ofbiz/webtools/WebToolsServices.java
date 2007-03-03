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
package org.ofbiz.webtools;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Locale;
import java.util.Map;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityDataLoader;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntitySaxReader;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import org.xml.sax.InputSource;
import freemarker.template.*;
import freemarker.ext.dom.NodeModel;
import freemarker.ext.beans.BeansWrapper;

/**
 * WebTools Services
 */

public class WebToolsServices {

    public static final String module = WebToolsServices.class.getName();

    public static Map entityImport(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("WebtoolsUiLabels", "WebtoolsPermissionError", (Locale) context.get("locale")));
        }

        LocalDispatcher dispatcher = dctx.getDispatcher();

        List messages = new ArrayList();

        String filename = (String)context.get("filename");
        String fmfilename = (String)context.get("fmfilename");
        String fulltext = (String)context.get("fulltext");
        boolean isUrl = (String)context.get("isUrl") != null;
        String mostlyInserts = (String)context.get("mostlyInserts");
        String maintainTimeStamps = (String)context.get("maintainTimeStamps");
        String createDummyFks = (String)context.get("createDummyFks");

        Integer txTimeout = (Integer)context.get("txTimeout");

        if (txTimeout == null) {
            txTimeout = new Integer(7200);
        }
        InputSource ins = null;
        URL url = null;

        // #############################
        // The filename to parse is prepared
        // #############################
        if (filename != null && filename.length() > 0) {
            try {
                url = isUrl?new URL(filename):UtilURL.fromFilename(filename);
                InputStream is = url.openStream();
                ins = new InputSource(is);
            } catch(MalformedURLException mue) {
                return ServiceUtil.returnError("ERROR: invalid file name (" + filename + "): " + mue.getMessage());
            } catch(IOException ioe) {
                return ServiceUtil.returnError("ERROR reading file name (" + filename + "): " + ioe.getMessage());
            } catch(Exception exc) {
                return ServiceUtil.returnError("ERROR: reading file name (" + filename + "): " + exc.getMessage());
            }
        }

        // #############################
        // The text to parse is prepared
        // #############################
        if (fulltext != null && fulltext.length() > 0) {
            StringReader sr = new StringReader(fulltext);
            ins = new InputSource(sr);
        }

        // #############################
        // FM Template
        // #############################
        String s = null;
        if (UtilValidate.isNotEmpty(fmfilename) && ins != null) {
            FileReader templateReader = null;
            try {
                templateReader = new FileReader(fmfilename);
            } catch(FileNotFoundException e) {
                return ServiceUtil.returnError("ERROR reading template file (" + fmfilename + "): " + e.getMessage());
            }

            StringWriter outWriter = new StringWriter();

            Template template = null;
            try {
                Configuration conf = org.ofbiz.base.util.template.FreeMarkerWorker.makeDefaultOfbizConfig();
                template = new Template("FMImportFilter", templateReader, conf);
                Map fmcontext = new HashMap();

                NodeModel nodeModel = NodeModel.parse(ins);
                fmcontext.put("doc", nodeModel);
                BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
                TemplateHashModel staticModels = wrapper.getStaticModels();
                fmcontext.put("Static", staticModels);

                template.process(fmcontext, outWriter);
                s = outWriter.toString();
            } catch(Exception ex) {
                return ServiceUtil.returnError("ERROR processing template file (" + fmfilename + "): " + ex.getMessage());
            }
        }

        // #############################
        // The parsing takes place
        // #############################
        if (s != null || fulltext != null || url != null) {
            try{
                Map inputMap = UtilMisc.toMap("mostlyInserts", mostlyInserts, 
                                              "createDummyFks", createDummyFks,
                                              "maintainTimeStamps", maintainTimeStamps,
                                              "txTimeout", txTimeout,
                                              "userLogin", userLogin);
                if (s != null) {
                    inputMap.put("xmltext", s);
                } else {
                    if (fulltext != null) {
                        inputMap.put("xmltext", fulltext);
                    } else {
                        inputMap.put("url", url);
                    }
                }
                Map outputMap = dispatcher.runSync("parseEntityXmlFile", inputMap);
                if (ServiceUtil.isError(outputMap)) {
                    return ServiceUtil.returnError("ERROR: " + ServiceUtil.getErrorMessage(outputMap));
                } else {
                    Long numberRead = (Long)outputMap.get("rowProcessed");
                    messages.add("Got " + numberRead.longValue() + " entities to write to the datasource.");
                }
            } catch (Exception ex){
                return ServiceUtil.returnError("ERROR parsing Entity Xml file: " + ex.getMessage());
            }
        } else {
            messages.add("No filename/URL or complete XML document specified, doing nothing.");
        }

        // send the notification
        Map resp = UtilMisc.toMap("messages", messages);
        return resp;
    }

    public static Map entityImportDir(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("WebtoolsUiLabels", "WebtoolsPermissionError", (Locale) context.get("locale")));
        }

        LocalDispatcher dispatcher = dctx.getDispatcher();

        List messages = FastList.newInstance();

        String path = (String)context.get("path");
        String mostlyInserts = (String)context.get("mostlyInserts");
        String maintainTimeStamps = (String)context.get("maintainTimeStamps");
        String createDummyFks = (String)context.get("createDummyFks");
        boolean deleteFiles = (String)context.get("deleteFiles") != null;

        Integer txTimeout = (Integer)context.get("txTimeout");
        Long filePause = (Long)context.get("filePause");

        if (txTimeout == null) {
            txTimeout = new Integer(7200);
        }
        if (filePause == null) {
            filePause = new Long(0);
        }

        if (path != null && path.length() > 0) {
            long pauseLong = filePause != null ? filePause.longValue() : 0;
            File baseDir = new File(path);

            if (baseDir.isDirectory() && baseDir.canRead()) {
                File[] fileArray = baseDir.listFiles();
                FastList files = FastList.newInstance();
                for (int a=0; a<fileArray.length; a++){
                    if (fileArray[a].getName().toUpperCase().endsWith("XML")) {
                        files.add(fileArray[a]);
                    }
                }                

                int passes=0;                
                int initialListSize = files.size();
                int lastUnprocessedFilesCount = 0;
                FastList unprocessedFiles = FastList.newInstance();
                while (files.size()>0 && 
                        files.size() != lastUnprocessedFilesCount) {
                    lastUnprocessedFilesCount = files.size();
                    unprocessedFiles = FastList.newInstance();
                    Iterator filesItr = files.iterator();
                    while (filesItr.hasNext()) {
                        Map parseEntityXmlFileArgs = UtilMisc.toMap("mostlyInserts", mostlyInserts, 
                                "createDummyFks", createDummyFks,
                                "maintainTimeStamps", maintainTimeStamps,
                                "txTimeout", txTimeout,
                                "userLogin", userLogin);
                        
                        File f = (File) filesItr.next();
                        try {
                            URL furl = f.toURI().toURL();
                            parseEntityXmlFileArgs.put("url", furl);
                            Map outputMap = dispatcher.runSync("parseEntityXmlFile", parseEntityXmlFileArgs);
                            Long numberRead = (Long) outputMap.get("rowProcessed");
                            messages.add("Got " + numberRead.longValue() + " entities from " + f);
                            if (deleteFiles) {
                                messages.add("Deleting " + f);
                                f.delete();
                            }
                        } catch(Exception e) {
                            unprocessedFiles.add(f);
                            messages.add("Failed " + f + " adding to retry list for next pass");
                        }
                        // pause in between files
                        if (pauseLong > 0) {
                            Debug.log("Pausing for [" + pauseLong + "] seconds - " + UtilDateTime.nowTimestamp());
                            try {
                                Thread.sleep((pauseLong * 1000));
                            } catch(InterruptedException ie) {
                                Debug.log("Pause finished - " + UtilDateTime.nowTimestamp());
                            }
                        }
                    }
                    files = unprocessedFiles;
                    passes++;
                    messages.add("Pass " + passes + " complete");
                    Debug.logInfo("Pass " + passes + " complete", module);
                }
                lastUnprocessedFilesCount=unprocessedFiles.size();
                messages.add("---------------------------------------");
                messages.add("Succeeded: " + (initialListSize-lastUnprocessedFilesCount) + " of " + initialListSize);
                messages.add("Failed:    " + lastUnprocessedFilesCount + " of " + initialListSize);
                messages.add("---------------------------------------");
                messages.add("Failed Files:");
                Iterator unprocessedFilesItr = unprocessedFiles.iterator();
                while (unprocessedFilesItr.hasNext()) {
                    File file = (File) unprocessedFilesItr.next();
                    messages.add("" + file);
                }
            } else {
                messages.add("path not found or can't be read");
            }
        } else {
            messages.add("No path specified, doing nothing.");
        }
        // send the notification
        Map resp = UtilMisc.toMap("messages", messages);
        return resp;
    }

    public static Map entityImportReaders(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("WebtoolsUiLabels", "WebtoolsPermissionError", (Locale) context.get("locale")));
        }

        String readers = (String) context.get("readers");
        String overrideDelegator = (String) context.get("overrideDelegator");
        String overrideGroup = (String) context.get("overrideGroup");
        boolean useDummyFks = "true".equals((String) context.get("createDummyFks"));
        boolean maintainTxs = "true".equals((String) context.get("maintainTimeStamps"));
        boolean tryInserts = "true".equals((String) context.get("mostlyInserts"));

        Integer txTimeoutInt = (Integer) context.get("txTimeout");
        int txTimeout = txTimeoutInt != null ? txTimeoutInt.intValue() : -1;

        List messages = FastList.newInstance();

        // parse the pass in list of readers to use
        List readerNames = null;
        if (UtilValidate.isNotEmpty(readers) && !"none".equalsIgnoreCase(readers)) {
            if (readers.indexOf(",") == -1) {
                readerNames = FastList.newInstance();
                readerNames.add(readers);
            } else {
                readerNames = StringUtil.split(readers, ",");
            }
        }

        String groupNameToUse = overrideGroup != null ? overrideGroup : "org.ofbiz";
        GenericDelegator delegator = UtilValidate.isNotEmpty(overrideDelegator) ? GenericDelegator.getGenericDelegator(overrideDelegator) : dctx.getDelegator();

        String helperName = delegator.getGroupHelperName(groupNameToUse);
        if (helperName == null) {
            return ServiceUtil.returnError("Unable to locate the datasource helper for the group [" + groupNameToUse + "]");
        }

        // get the reader name URLs first
        List urlList = null;
        if (readerNames != null) {
            urlList = EntityDataLoader.getUrlList(helperName, readerNames);
        } else if (!"none".equalsIgnoreCase(readers)) {
            urlList = EntityDataLoader.getUrlList(helperName);
        }

        // need a list if it is empty
        if (urlList == null) {
            urlList = FastList.newInstance();
        }

        // process the list of files
        NumberFormat changedFormat = NumberFormat.getIntegerInstance();
        changedFormat.setMinimumIntegerDigits(5);
        changedFormat.setGroupingUsed(false);
        
        List errorMessages = new LinkedList();
        List infoMessages = new LinkedList();
        int totalRowsChanged = 0;
        if (urlList != null && urlList.size() > 0) {
            messages.add("=-=-=-=-=-=-= Doing a data load with the following files:");
            Iterator urlIter = urlList.iterator();
            while (urlIter.hasNext()) {
                URL dataUrl = (URL) urlIter.next();
                messages.add(dataUrl.toExternalForm());
            }

            messages.add("=-=-=-=-=-=-= Starting the data load...");

            urlIter = urlList.iterator();
            while (urlIter.hasNext()) {
                URL dataUrl = (URL) urlIter.next();
                try {
                    int rowsChanged = EntityDataLoader.loadData(dataUrl, helperName, delegator, errorMessages, txTimeout, useDummyFks, maintainTxs, tryInserts);
                    totalRowsChanged += rowsChanged;
                    infoMessages.add(changedFormat.format(rowsChanged) + " of " + changedFormat.format(totalRowsChanged) + " from " + dataUrl.toExternalForm());
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error loading data file: " + dataUrl.toExternalForm(), module);
                }
            }
        } else {
            messages.add("=-=-=-=-=-=-= No data load files found.");
        }

        if (infoMessages.size() > 0) {
            messages.add("=-=-=-=-=-=-= Here is a summary of the data load:");
            messages.addAll(infoMessages);
        }
        
        if (errorMessages.size() > 0) {
            messages.add("=-=-=-=-=-=-= The following errors occured in the data load:");
            messages.addAll(errorMessages);
        }

        messages.add("=-=-=-=-=-=-= Finished the data load with " + totalRowsChanged + " rows changed.");
        
        Map resultMap = ServiceUtil.returnSuccess();
        resultMap.put("messages", messages);
        return resultMap;
    }
    
    public static Map parseEntityXmlFile(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("WebtoolsUiLabels", "WebtoolsPermissionError", (Locale) context.get("locale")));
        }

        GenericDelegator delegator = dctx.getDelegator();

        URL url = (URL)context.get("url");
        String xmltext = (String)context.get("xmltext");

        if (url == null && xmltext == null) {
            return ServiceUtil.returnError("No entity xml file or text specified");
        }
        boolean mostlyInserts = (String)context.get("mostlyInserts") != null;
        boolean maintainTimeStamps = (String)context.get("maintainTimeStamps") != null;
        boolean createDummyFks = (String)context.get("createDummyFks") != null;
        Integer txTimeout = (Integer)context.get("txTimeout");

        if (txTimeout == null) {
            txTimeout = new Integer(7200);
        }

        Long rowProcessed = new Long(0);
        try {
            EntitySaxReader reader = new EntitySaxReader(delegator);
            reader.setUseTryInsertMethod(mostlyInserts);
            reader.setMaintainTxStamps(maintainTimeStamps);
            reader.setTransactionTimeout(txTimeout.intValue());
            reader.setCreateDummyFks(createDummyFks);

            long numberRead = (url != null? reader.parse(url): reader.parse(xmltext));
            rowProcessed = new Long(numberRead);
        } catch (Exception ex){
            return ServiceUtil.returnError("Error parsing entity xml file: " + ex.toString());
        }
        // send the notification
        Map resp = UtilMisc.toMap("rowProcessed", rowProcessed);
        return resp;
    }

    public static Map entityExportAll(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        if (!security.hasPermission("ENTITY_MAINT", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("WebtoolsUiLabels", "WebtoolsPermissionError", (Locale) context.get("locale")));
        }

        GenericDelegator delegator = dctx.getDelegator();

        String outpath = (String)context.get("outpath"); // mandatory
        Integer txTimeout = (Integer)context.get("txTimeout");
        if (txTimeout == null) {
            txTimeout = new Integer(7200);
        }

        List results = new ArrayList();

        if (outpath != null && outpath.length() > 0) {
            File outdir = new File(outpath);
            if (!outdir.exists()) {
                outdir.mkdir();
            }
            if (outdir.isDirectory() && outdir.canWrite()) {
                
                Iterator passedEntityNames = null;
                try {
                    ModelReader reader = delegator.getModelReader();
                    Collection ec = reader.getEntityNames();
                    TreeSet entityNames = new TreeSet(ec);
                    passedEntityNames = entityNames.iterator();
                } catch(Exception exc) {
                    return ServiceUtil.returnError("Error retrieving entity names.");
                }
                int fileNumber = 1;

                while (passedEntityNames.hasNext()) { 
                    long numberWritten = 0;
                    String curEntityName = (String)passedEntityNames.next();
                    EntityListIterator values = null;

                    try {
                        ModelEntity me = delegator.getModelEntity(curEntityName);
                        if (me instanceof ModelViewEntity) {
                            results.add("["+fileNumber +"] [vvv] " + curEntityName + " skipping view entity");
                            continue;
                        }

                        // some databases don't support cursors, or other problems may happen, so if there is an error here log it and move on to get as much as possible
                        try {
                            values = delegator.findListIteratorByCondition(curEntityName, null, null, null, me.getPkFieldNames(), null);
                        } catch (Exception entityEx) {
                            results.add("["+fileNumber +"] [xxx] Error when writing " + curEntityName + ": " + entityEx);
                            continue;
                        }

                        //Don't bother writing the file if there's nothing
                        //to put into it
                        GenericValue value = (GenericValue) values.next();
                        if (value != null) {
                            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outdir, curEntityName +".xml")), "UTF-8")));
                            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                            writer.println("<entity-engine-xml>");

                            do {
                                value.writeXmlText(writer, "");
                                numberWritten++;
                            } while ((value = (GenericValue) values.next()) != null);
                            writer.println("</entity-engine-xml>");
                            writer.close();
                            results.add("["+fileNumber +"] [" + numberWritten + "] " + curEntityName + " wrote " + numberWritten + " records");
                        } else {
                            results.add("["+fileNumber +"] [---] " + curEntityName + " has no records, not writing file");
                        }
                        values.close();
                    } catch (Exception ex) {
                        if (values != null) {
                            try {
                                values.close();
                            } catch(Exception exc) {
                                //Debug.warning();
                            }
                        }
                        results.add("["+fileNumber +"] [xxx] Error when writing " + curEntityName + ": " + ex);
                    }
                    fileNumber++;
                }
            } else {
                results.add("Path not found or no write access.");
            }
        } else {
            results.add("No path specified, doing nothing.");
        }
        // send the notification
        Map resp = UtilMisc.toMap("results", results);
        return resp;
    }
}

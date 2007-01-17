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
package org.ofbiz.datafile;


import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Flat File definition reader
 */

public class ModelDataFileReader {
    
    public static final String module = ModelDataFileReader.class.getName();

    public static UtilCache readers = new UtilCache("ModelDataFile", 0, 0);

    public URL readerURL = null;
    public Map modelDataFiles = null;

    public static ModelDataFileReader getModelDataFileReader(URL readerURL) {
        ModelDataFileReader reader = null;

        reader = (ModelDataFileReader) readers.get(readerURL);
        if (reader == null) { // don't want to block here
            synchronized (ModelDataFileReader.class) {
                // must check if null again as one of the blocked threads can still enter
                reader = (ModelDataFileReader) readers.get(readerURL);
                if (reader == null) {
                    if (Debug.infoOn()) Debug.logInfo("[ModelDataFileReader.getModelDataFileReader] : creating reader.", module);
                    reader = new ModelDataFileReader(readerURL);
                    readers.put(readerURL, reader);
                }
            }
        }
        if (reader != null && (reader.modelDataFiles == null || reader.modelDataFiles.size() == 0)) {
            readers.remove(readerURL);
            return null;
        }
        if (Debug.infoOn()) Debug.logInfo("[ModelDataFileReader.getModelDataFileReader] : returning reader.", module);
        return reader;
    }

    public ModelDataFileReader(URL readerURL) {
        this.readerURL = readerURL;

        // preload models...
        getModelDataFiles();
    }

    public Map getModelDataFiles() {
        if (modelDataFiles == null) { // don't want to block here
            synchronized (ModelDataFileReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (modelDataFiles == null) { // now it's safe
                    modelDataFiles = new HashMap();

                    UtilTimer utilTimer = new UtilTimer();

                    utilTimer.timerString("Before getDocument in file " + readerURL);
                    Document document = getDocument(readerURL);

                    if (document == null) {
                        modelDataFiles = null;
                        return null;
                    }

                    utilTimer.timerString("Before getDocumentElement in file " + readerURL);
                    Element docElement = document.getDocumentElement();

                    if (docElement == null) {
                        modelDataFiles = null;
                        return null;
                    }
                    docElement.normalize();
                    Node curChild = docElement.getFirstChild();

                    int i = 0;

                    if (curChild != null) {
                        utilTimer.timerString("Before start of dataFile loop in file " + readerURL);
                        do {
                            if (curChild.getNodeType() == Node.ELEMENT_NODE && "data-file".equals(curChild.getNodeName())) {
                                i++;
                                Element curDataFile = (Element) curChild;
                                String dataFileName = UtilXml.checkEmpty(curDataFile.getAttribute("name"));

                                // check to see if dataFile with same name has already been read
                                if (modelDataFiles.containsKey(dataFileName)) {
                                    Debug.logWarning("WARNING: DataFile " + dataFileName +
                                        " is defined more than once, most recent will over-write previous definition(s)", module);
                                }

                                // utilTimer.timerString("  After dataFileName -- " + i + " --");
                                ModelDataFile dataFile = createModelDataFile(curDataFile);

                                // utilTimer.timerString("  After createModelDataFile -- " + i + " --");
                                if (dataFile != null) {
                                    modelDataFiles.put(dataFileName, dataFile);
                                    // utilTimer.timerString("  After modelDataFiles.put -- " + i + " --");
                                    if (Debug.infoOn()) Debug.logInfo("-- getModelDataFile: #" + i + " Loaded dataFile: " + dataFileName, module);
                                } else
                                    Debug.logWarning("-- -- SERVICE ERROR:getModelDataFile: Could not create dataFile for dataFileName: " + dataFileName, module);

                            }
                        } while ((curChild = curChild.getNextSibling()) != null);
                    } else {
                        Debug.logWarning("No child nodes found.", module);
                    }
                    utilTimer.timerString("Finished file " + readerURL + " - Total Flat File Defs: " + i + " FINISHED");
                }
            }
        }
        return modelDataFiles;
    }

    /** Gets an DataFile object based on a definition from the specified XML DataFile descriptor file.
     * @param dataFileName The dataFileName of the DataFile definition to use.
     * @return An DataFile object describing the specified dataFile of the specified descriptor file.
     */
    public ModelDataFile getModelDataFile(String dataFileName) {
        Map ec = getModelDataFiles();

        if (ec != null) {
            return (ModelDataFile) ec.get(dataFileName);
        } else {
            return null;
        }
    }

    /** Creates a Iterator with the dataFileName of each DataFile defined in the specified XML DataFile Descriptor file.
     * @return A Iterator of dataFileName Strings
     */
    public Iterator getDataFileNamesIterator() {
        Collection collection = getDataFileNames();

        if (collection != null) {
            return collection.iterator();
        } else {
            return null;
        }
    }

    /** Creates a Collection with the dataFileName of each DataFile defined in the specified XML DataFile Descriptor file.
     * @return A Collection of dataFileName Strings
     */
    public Collection getDataFileNames() {
        Map ec = getModelDataFiles();

        return ec.keySet();
    }

    protected ModelDataFile createModelDataFile(Element dataFileElement) {
        ModelDataFile dataFile = new ModelDataFile();
        String tempStr;

        dataFile.name = UtilXml.checkEmpty(dataFileElement.getAttribute("name"));
        dataFile.typeCode = UtilXml.checkEmpty(dataFileElement.getAttribute("type-code"));
        dataFile.sender = UtilXml.checkEmpty(dataFileElement.getAttribute("sender"));
        dataFile.receiver = UtilXml.checkEmpty(dataFileElement.getAttribute("receiver"));

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("record-length"));
        if (tempStr != null && tempStr.length() > 0) {
            dataFile.recordLength = Integer.parseInt(tempStr);
        }
        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("delimiter"));
        if (tempStr != null && tempStr.length() == 1) {
            dataFile.delimiter = tempStr.charAt(0);
        }

        dataFile.separatorStyle = UtilXml.checkEmpty(dataFileElement.getAttribute("separator-style"));
        dataFile.description = UtilXml.checkEmpty(dataFileElement.getAttribute("description"));

        NodeList rList = dataFileElement.getElementsByTagName("record");

        for (int i = 0; i < rList.getLength(); i++) {
            Element recordElement = (Element) rList.item(i);
            ModelRecord modelRecord = createModelRecord(recordElement);

            if (modelRecord != null) {
                dataFile.records.add(modelRecord);
            } else {
                Debug.logWarning("[ModelDataFileReader.createModelDataFile] Weird, modelRecord was null", module);
            }
        }

        for (int i = 0; i < dataFile.records.size(); i++) {
            ModelRecord modelRecord = (ModelRecord) dataFile.records.get(i);

            if (modelRecord.parentName.length() > 0) {
                ModelRecord parentRecord = dataFile.getModelRecord(modelRecord.parentName);

                if (parentRecord != null) {
                    parentRecord.childRecords.add(modelRecord);
                    modelRecord.parentRecord = parentRecord;
                } else {
                    Debug.logError("[ModelDataFileReader.createModelDataFile] ERROR: Could not find parentRecord with name " + modelRecord.parentName, module);
                }
            }
        }

        return dataFile;
    }

    protected ModelRecord createModelRecord(Element recordElement) {
        ModelRecord record = new ModelRecord();
        String tempStr;

        record.name = UtilXml.checkEmpty(recordElement.getAttribute("name"));
        record.typeCode = UtilXml.checkEmpty(recordElement.getAttribute("type-code"));

        record.tcMin = UtilXml.checkEmpty(recordElement.getAttribute("tc-min"));
        if (record.tcMin.length() > 0) record.tcMinNum = Long.parseLong(record.tcMin);
        record.tcMax = UtilXml.checkEmpty(recordElement.getAttribute("tc-max"));
        if (record.tcMax.length() > 0) record.tcMaxNum = Long.parseLong(record.tcMax);

        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-isnum"));
        if (tempStr != null && tempStr.length() > 0) {
            record.tcIsNum = Boolean.valueOf(tempStr).booleanValue();
        }

        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-position"));
        if (tempStr != null && tempStr.length() > 0) {
            record.tcPosition = Integer.parseInt(tempStr);
        }
        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-length"));
        if (tempStr != null && tempStr.length() > 0) {
            record.tcLength = Integer.parseInt(tempStr);
        }

        record.description = UtilXml.checkEmpty(recordElement.getAttribute("description"));
        record.parentName = UtilXml.checkEmpty(recordElement.getAttribute("parent-name"));
        record.limit = UtilXml.checkEmpty(recordElement.getAttribute("limit"));

        NodeList fList = recordElement.getElementsByTagName("field");
        int priorEnd = -1;

        for (int i = 0; i < fList.getLength(); i++) {
            Element fieldElement = (Element) fList.item(i);
            ModelField modelField = createModelField(fieldElement);

            // if the position is not specified, assume the start position based on last entry
            if ((i > 0) && (modelField.position == -1)) {
                modelField.position = priorEnd;
            }
            priorEnd = modelField.position + modelField.length;

            if (modelField != null) {
                record.fields.add(modelField);
            } else {
                Debug.logWarning("[ModelDataFileReader.createModelRecord] Weird, modelField was null", module);
            }
        }

        return record;
    }

    protected ModelField createModelField(Element fieldElement) {
        ModelField field = new ModelField();
        String tempStr;

        field.name = UtilXml.checkEmpty(fieldElement.getAttribute("name"));

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("position"));
        if (tempStr != null && tempStr.length() > 0) {
            field.position = Integer.parseInt(tempStr);
        }
        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("length"));
        if (tempStr != null && tempStr.length() > 0) {
            field.length = Integer.parseInt(tempStr);
        }

        field.type = UtilXml.checkEmpty(fieldElement.getAttribute("type"));
        field.format = UtilXml.checkEmpty(fieldElement.getAttribute("format"));
        field.validExp = UtilXml.checkEmpty(fieldElement.getAttribute("valid-exp"));
        field.description = UtilXml.checkEmpty(fieldElement.getAttribute("description"));
        field.defaultValue = UtilXml.checkEmpty(fieldElement.getAttribute("default-value"));
        field.refField = UtilXml.checkEmpty(fieldElement.getAttribute("ref-field"));
        
        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("prim-key"));
        if (tempStr != null && tempStr.length() > 0) {
            field.isPk = Boolean.valueOf(tempStr).booleanValue();
        }

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("ignored"));
        if (tempStr != null && tempStr.length() > 0) {
            field.ignored = Boolean.valueOf(tempStr).booleanValue();
        }

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("expression"));
        if (tempStr != null && tempStr.length() > 0) {
            field.expression = Boolean.valueOf(tempStr).booleanValue();
        }

        return field;
    }

    protected Document getDocument(URL url) {
        if (url == null)
            return null;
        Document document = null;

        try {
            document = UtilXml.readXmlDocument(url);
        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception x = sxe;

            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return document;
    }
}


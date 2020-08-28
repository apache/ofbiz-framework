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
package org.apache.ofbiz.datafile;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Flat File definition reader
 */

public final class ModelDataFileReader {

    private static final String MODULE = ModelDataFileReader.class.getName();
    private static final UtilCache<URL, ModelDataFileReader> READERS = UtilCache.createUtilCache("ModelDataFile", true);

    public static ModelDataFileReader getModelDataFileReader(URL readerURL) throws DataFileException {
        ModelDataFileReader reader = READERS.get(readerURL);
        if (reader == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("[ModelDataFileReader.getModelDataFileReader] : creating reader for " + readerURL, MODULE);
            }
            reader = new ModelDataFileReader(readerURL);
            READERS.putIfAbsent(readerURL, reader);
        }
        if (Debug.infoOn()) {
            Debug.logInfo("[ModelDataFileReader.getModelDataFileReader] : returning reader for " + readerURL, MODULE);
        }
        return reader;
    }

    private final URL readerURL;
    private final Map<String, ModelDataFile> modelDataFiles;

    public ModelDataFileReader(URL readerURL) throws DataFileException {
        this.readerURL = readerURL;
        this.modelDataFiles = Collections.unmodifiableMap(createModelDataFiles());
    }

    private static ModelDataFile createModelDataFile(Element dataFileElement) {
        ModelDataFile dataFile = new ModelDataFile();
        String tempStr;

        dataFile.setName(UtilXml.checkEmpty(dataFileElement.getAttribute("name")));
        dataFile.setTypeCode(UtilXml.checkEmpty(dataFileElement.getAttribute("type-code")));
        dataFile.setSender(UtilXml.checkEmpty(dataFileElement.getAttribute("sender")));
        dataFile.setReceiver(UtilXml.checkEmpty(dataFileElement.getAttribute("receiver")));
        dataFile.setEncodingType(UtilXml.checkEmpty(dataFileElement.getAttribute("encoding-type")));

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("record-length"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            dataFile.setRecordLength(Integer.parseInt(tempStr));
        }
        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("delimiter"));
        if (tempStr != null && tempStr.length() == 1) {
            dataFile.setDelimiter(tempStr.charAt(0));
        }
        tempStr = dataFileElement.getAttribute("start-line");
        if (tempStr != null) {
            dataFile.setStartLine(Integer.valueOf(tempStr));
        }

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("text-delimiter"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            dataFile.setTextDelimiter(tempStr);
        }

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("eol-type"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            dataFile.setEOLType(tempStr);
        }

        dataFile.setSeparatorStyle(UtilXml.checkEmpty(dataFileElement.getAttribute("separator-style")));
        dataFile.setDescription(UtilXml.checkEmpty(dataFileElement.getAttribute("description")));

        NodeList rList = dataFileElement.getElementsByTagName("record");

        for (int i = 0; i < rList.getLength(); i++) {
            Element recordElement = (Element) rList.item(i);
            ModelRecord modelRecord = createModelRecord(recordElement);
            dataFile.getRecords().add(modelRecord);
        }

        for (ModelRecord modelRecord : dataFile.getRecords()) {

            if (modelRecord.getParentName().length() > 0) {
                ModelRecord parentRecord = dataFile.getModelRecord(modelRecord.getParentName());

                if (parentRecord != null) {
                    parentRecord.getChildRecords().add(modelRecord);
                    modelRecord.setParentRecord(parentRecord);
                } else {
                    Debug.logError("[ModelDataFileReader.createModelDataFile] ERROR: Could not find parentRecord with name "
                            + modelRecord.getParentName(), MODULE);
                }
            }
        }

        return dataFile;
    }

    private Map<String, ModelDataFile> createModelDataFiles() throws DataFileException {
        Document document = null;
        Element docElement = null;
        try {
            document = UtilXml.readXmlDocument(this.readerURL);
        } catch (Exception e) {
            Debug.logWarning(e, "Error while reading " + this.readerURL + ": ", MODULE);
            throw new DataFileException("Error while reading " + this.readerURL, e);
        }
        if (document != null) {
            docElement = document.getDocumentElement();
        }
        if (docElement == null) {
            Debug.logWarning("Document element not found in " + this.readerURL, MODULE);
            throw new DataFileException("Document element not found in " + this.readerURL);
        }
        docElement.normalize();
        List<? extends Element> dataFileElements = UtilXml.childElementList(docElement, "data-file");
        if (dataFileElements.isEmpty()) {
            Debug.logWarning("No <data-file> elements found in " + this.readerURL, MODULE);
            throw new DataFileException("No <data-file> elements found in " + this.readerURL);
        }
        Map<String, ModelDataFile> result = new HashMap<>();
        for (Element curDataFile : dataFileElements) {
            String dataFileName = UtilXml.checkEmpty(curDataFile.getAttribute("name"));
            if (result.containsKey(dataFileName)) {
                Debug.logWarning("DataFile " + dataFileName + " is defined more than once, most recent will over-write previous definition(s)",
                        MODULE);
            }
            ModelDataFile dataFile = createModelDataFile(curDataFile);
            result.put(dataFileName, dataFile);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Loaded dataFile: " + dataFileName, MODULE);
            }
        }
        return result;
    }

    private static ModelField createModelField(Element fieldElement) {
        ModelField field = new ModelField();
        String tempStr;

        field.setName(UtilXml.checkEmpty(fieldElement.getAttribute("name")));

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("position"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.setPosition(Integer.parseInt(tempStr));
        }
        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("length"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.setLength(Integer.parseInt(tempStr));
        }

        field.setType(UtilXml.checkEmpty(fieldElement.getAttribute("type")));
        field.setFormat(UtilXml.checkEmpty(fieldElement.getAttribute("format")));
        field.setValidExp(UtilXml.checkEmpty(fieldElement.getAttribute("valid-exp")));
        field.setDescription(UtilXml.checkEmpty(fieldElement.getAttribute("description")));
        field.setDefaultValue(UtilXml.checkEmpty(fieldElement.getAttribute("default-value")));
        field.setRefField(UtilXml.checkEmpty(fieldElement.getAttribute("ref-field")));

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("prim-key"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.setPk(Boolean.parseBoolean(tempStr));
        }

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("ignored"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.setIgnored(Boolean.parseBoolean(tempStr));
        }

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("expression"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.setExpression(Boolean.parseBoolean(tempStr));
        }

        return field;
    }

    private static ModelRecord createModelRecord(Element recordElement) {
        ModelRecord record = new ModelRecord();
        String tempStr;

        record.setName(UtilXml.checkEmpty(recordElement.getAttribute("name")));
        record.setTypeCode(UtilXml.checkEmpty(recordElement.getAttribute("type-code")));

        record.setTcMin(UtilXml.checkEmpty(recordElement.getAttribute("tc-min")));
        if (record.getTcMin().length() > 0) {
            record.setTcMinNum(Long.parseLong(record.getTcMin()));
        }
        record.setTcMax(UtilXml.checkEmpty(recordElement.getAttribute("tc-max")));
        if (record.getTcMax().length() > 0) {
            record.setTcMaxNum(Long.parseLong(record.getTcMax()));
        }

        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-isnum"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            record.setTcIsNum(Boolean.parseBoolean(tempStr));
        }

        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-position"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            record.setTcPosition(Integer.parseInt(tempStr));
        }
        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-length"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            record.setTcLength(Integer.parseInt(tempStr));
        }

        record.setDescription(UtilXml.checkEmpty(recordElement.getAttribute("description")));
        record.setParentName(UtilXml.checkEmpty(recordElement.getAttribute("parent-name")));
        record.setLimit(UtilXml.checkEmpty(recordElement.getAttribute("limit")));

        NodeList fList = recordElement.getElementsByTagName("field");
        int priorEnd = -1;

        for (int i = 0; i < fList.getLength(); i++) {
            Element fieldElement = (Element) fList.item(i);
            ModelField modelField = createModelField(fieldElement);

            // if the position is not specified, assume the start position based on last entry
            if ((i > 0) && (modelField.getPosition() == -1)) {
                modelField.setPosition(priorEnd);
            }
            priorEnd = modelField.getPosition() + modelField.getLength();
            record.getFields().add(modelField);
        }

        return record;
    }

    /**
     * Creates a Collection with the dataFileName of each DataFile defined in the
     * specified XML DataFile Descriptor file.
     * @return A Collection of dataFileName Strings
     */
    public Collection<String> getDataFileNames() {
        return this.modelDataFiles.keySet();
    }

    /**
     * Creates a Iterator with the dataFileName of each DataFile defined in the specified
     * XML DataFile Descriptor file.
     * @return A Iterator of dataFileName Strings
     */
    public Iterator<String> getDataFileNamesIterator() {
        return this.modelDataFiles.keySet().iterator();
    }

    /**
     * Gets an DataFile object based on a definition from the specified XML DataFile
     * descriptor file.
     * @param dataFileName
     *            The dataFileName of the DataFile definition to use.
     * @return An DataFile object describing the specified dataFile of the specified
     *         descriptor file.
     */
    public ModelDataFile getModelDataFile(String dataFileName) {
        return this.modelDataFiles.get(dataFileName);
    }

    public Map<String, ModelDataFile> getModelDataFiles() {
        return this.modelDataFiles;
    }
}

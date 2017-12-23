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

    public static final String module = ModelDataFileReader.class.getName();
    private static final UtilCache<URL, ModelDataFileReader> readers = UtilCache.createUtilCache("ModelDataFile", true);

    public static ModelDataFileReader getModelDataFileReader(URL readerURL) throws DataFileException {
        ModelDataFileReader reader = readers.get(readerURL);
        if (reader == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("[ModelDataFileReader.getModelDataFileReader] : creating reader for " + readerURL, module);
            }
            reader = new ModelDataFileReader(readerURL);
            readers.putIfAbsent(readerURL, reader);
        }
        if (Debug.infoOn()) {
            Debug.logInfo("[ModelDataFileReader.getModelDataFileReader] : returning reader for " + readerURL, module);
        }
        return reader;
    }

    private final URL readerURL;
    private final Map<String, ModelDataFile> modelDataFiles;

    public ModelDataFileReader(URL readerURL) throws DataFileException {
        this.readerURL = readerURL;
        this.modelDataFiles = Collections.unmodifiableMap(createModelDataFiles());
    }

    private ModelDataFile createModelDataFile(Element dataFileElement) {
        ModelDataFile dataFile = new ModelDataFile();
        String tempStr;

        dataFile.name = UtilXml.checkEmpty(dataFileElement.getAttribute("name"));
        dataFile.typeCode = UtilXml.checkEmpty(dataFileElement.getAttribute("type-code"));
        dataFile.sender = UtilXml.checkEmpty(dataFileElement.getAttribute("sender"));
        dataFile.receiver = UtilXml.checkEmpty(dataFileElement.getAttribute("receiver"));

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("record-length"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            dataFile.recordLength = Integer.parseInt(tempStr);
        }
        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("delimiter"));
        if (tempStr != null && tempStr.length() == 1) {
            dataFile.delimiter = tempStr.charAt(0);
        }
        tempStr = dataFileElement.getAttribute("start-line");
        if (tempStr != null) {
            dataFile.startLine = Integer.valueOf(tempStr);
        }

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("text-delimiter"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            dataFile.textDelimiter = tempStr;
        }

        tempStr = UtilXml.checkEmpty(dataFileElement.getAttribute("eol-type"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            dataFile.setEOLType(tempStr);
        }

        dataFile.separatorStyle = UtilXml.checkEmpty(dataFileElement.getAttribute("separator-style"));
        dataFile.description = UtilXml.checkEmpty(dataFileElement.getAttribute("description"));

        NodeList rList = dataFileElement.getElementsByTagName("record");

        for (int i = 0; i < rList.getLength(); i++) {
            Element recordElement = (Element) rList.item(i);
            ModelRecord modelRecord = createModelRecord(recordElement);
            dataFile.records.add(modelRecord);
        }

        for (ModelRecord modelRecord : dataFile.records) {

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

    private Map<String, ModelDataFile> createModelDataFiles() throws DataFileException {
        Document document = null;
        Element docElement = null;
        try {
            document = UtilXml.readXmlDocument(this.readerURL);
        } catch (Exception e) {
            Debug.logWarning(e, "Error while reading " + this.readerURL + ": ", module);
            throw new DataFileException("Error while reading " + this.readerURL, e);
        }
        if (document != null) {
            docElement = document.getDocumentElement();
        }
        if (docElement == null) {
            Debug.logWarning("Document element not found in " + this.readerURL, module);
            throw new DataFileException("Document element not found in " + this.readerURL);
        }
        docElement.normalize();
        List<? extends Element> dataFileElements = UtilXml.childElementList(docElement, "data-file");
        if (dataFileElements.size() == 0) {
            Debug.logWarning("No <data-file> elements found in " + this.readerURL, module);
            throw new DataFileException("No <data-file> elements found in " + this.readerURL);
        }
        Map<String, ModelDataFile> result = new HashMap<>();
        for (Element curDataFile : dataFileElements) {
            String dataFileName = UtilXml.checkEmpty(curDataFile.getAttribute("name"));
            if (result.containsKey(dataFileName)) {
                Debug.logWarning("DataFile " + dataFileName + " is defined more than once, most recent will over-write previous definition(s)", module);
            }
            ModelDataFile dataFile = createModelDataFile(curDataFile);
            result.put(dataFileName, dataFile);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Loaded dataFile: " + dataFileName, module);
            }
        }
        return result;
    }

    private ModelField createModelField(Element fieldElement) {
        ModelField field = new ModelField();
        String tempStr;

        field.name = UtilXml.checkEmpty(fieldElement.getAttribute("name"));

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("position"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.position = Integer.parseInt(tempStr);
        }
        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("length"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.length = Integer.parseInt(tempStr);
        }

        field.type = UtilXml.checkEmpty(fieldElement.getAttribute("type"));
        field.format = UtilXml.checkEmpty(fieldElement.getAttribute("format"));
        field.validExp = UtilXml.checkEmpty(fieldElement.getAttribute("valid-exp"));
        field.description = UtilXml.checkEmpty(fieldElement.getAttribute("description"));
        field.defaultValue = UtilXml.checkEmpty(fieldElement.getAttribute("default-value"));
        field.refField = UtilXml.checkEmpty(fieldElement.getAttribute("ref-field"));

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("prim-key"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.isPk = Boolean.parseBoolean(tempStr);
        }

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("ignored"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.ignored = Boolean.parseBoolean(tempStr);
        }

        tempStr = UtilXml.checkEmpty(fieldElement.getAttribute("expression"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            field.expression = Boolean.parseBoolean(tempStr);
        }

        return field;
    }

    private ModelRecord createModelRecord(Element recordElement) {
        ModelRecord record = new ModelRecord();
        String tempStr;

        record.name = UtilXml.checkEmpty(recordElement.getAttribute("name"));
        record.typeCode = UtilXml.checkEmpty(recordElement.getAttribute("type-code"));

        record.tcMin = UtilXml.checkEmpty(recordElement.getAttribute("tc-min"));
        if (record.tcMin.length() > 0) {
            record.tcMinNum = Long.parseLong(record.tcMin);
        }
        record.tcMax = UtilXml.checkEmpty(recordElement.getAttribute("tc-max"));
        if (record.tcMax.length() > 0) {
            record.tcMaxNum = Long.parseLong(record.tcMax);
        }

        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-isnum"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            record.tcIsNum = Boolean.parseBoolean(tempStr);
        }

        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-position"));
        if (UtilValidate.isNotEmpty(tempStr)) {
            record.tcPosition = Integer.parseInt(tempStr);
        }
        tempStr = UtilXml.checkEmpty(recordElement.getAttribute("tc-length"));
        if (UtilValidate.isNotEmpty(tempStr)) {
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
            record.fields.add(modelField);
        }

        return record;
    }

    /**
     * Creates a Collection with the dataFileName of each DataFile defined in the
     * specified XML DataFile Descriptor file.
     *
     * @return A Collection of dataFileName Strings
     */
    public Collection<String> getDataFileNames() {
        return this.modelDataFiles.keySet();
    }

    /**
     * Creates a Iterator with the dataFileName of each DataFile defined in the specified
     * XML DataFile Descriptor file.
     *
     * @return A Iterator of dataFileName Strings
     */
    public Iterator<String> getDataFileNamesIterator() {
        return this.modelDataFiles.keySet().iterator();
    }

    /**
     * Gets an DataFile object based on a definition from the specified XML DataFile
     * descriptor file.
     *
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

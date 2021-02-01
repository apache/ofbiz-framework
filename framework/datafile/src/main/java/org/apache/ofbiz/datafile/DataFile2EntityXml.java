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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;

public class DataFile2EntityXml {

    private static final String MODULE = DataFile2EntityXml.class.getName();

    /** Creates a new instance of DataFile2EntityXml */
    public DataFile2EntityXml() {
    }

    /**
     * Writes the entity xml
     * @param fileName the file name
     * @param dataFile the data file name
     */
    public static void writeToEntityXml(String fileName, DataFile dataFile) throws DataFileException {
        File file = new File(fileName);

        try (BufferedWriter outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));) {

            outFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            outFile.newLine();
            outFile.write("<entity-engine-xml>");
            outFile.newLine();
            for (Record record : dataFile.getRecords()) {
                ModelRecord modelRecord = record.getModelRecord();
                outFile.write("<" + modelRecord.getName() + " ");
                for (ModelField modelField : modelRecord.getFields()) {
                    if (modelField.isIgnored()) {
                        continue;
                    }
                    Object value = record.get(modelField.getName());
                    if (value == null) {
                        value = modelField.getDefaultValue();
                    }
                    if (value instanceof String) {
                        value = ((String) value).trim();
                        if (((String) value).isEmpty()) {
                            value = modelField.getDefaultValue();
                        }
                    }
                    if (value != null) {
                        if (value instanceof String) {
                            outFile.write(modelField.getName() + "=\"" + UtilFormatOut.encodeXmlValue((String) value) + "\" ");
                        } else {
                            outFile.write(modelField.getName() + "=\"" + value + "\" ");
                        }
                    }
                }
                outFile.write("/>");
                outFile.newLine();
            }
            outFile.write("</entity-engine-xml>");
        } catch (IOException e) {
            throw new DataFileException("Error writing to file " + fileName, e);
        }

    }

    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        String dataFileLoc = args[0];
        String definitionLoc = args[1];
        String definitionName = args[2];

        try (BufferedWriter outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFileLoc + ".xml"),
                StandardCharsets.UTF_8));) {
            URL dataFileUrl = UtilURL.fromFilename(dataFileLoc);
            URL definitionUrl = UtilURL.fromFilename(definitionLoc);

            DataFile dataFile = null;
            if (dataFileUrl != null && definitionUrl != null && UtilValidate.isNotEmpty(definitionName)) {
                try {
                    dataFile = DataFile.readFile(dataFileUrl, definitionUrl, definitionName);
                } catch (DataFileException e) {
                    Debug.logError("Error Occurred while reading Datafile, Exception: " + e, MODULE);
                }
            }

            if (dataFile != null) {
                for (Record record : dataFile.getRecords()) {
                    ModelRecord modelRecord = record.getModelRecord();
                    outFile.write("<" + modelRecord.getName() + " ");
                    for (ModelField modelField : modelRecord.getFields()) {
                        Object value = record.get(modelField.getName());
                        if (value instanceof String) {
                            value = ((String) value).trim();
                            outFile.write(modelField.getName() + "=\"" + UtilFormatOut.encodeXmlValue((String) value) + "\" ");
                        } else {
                            outFile.write(modelField.getName() + "=\"" + value + "\" ");
                        }
                    }
                    outFile.write("/>");
                    outFile.newLine();
                }
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
        }
    }

}

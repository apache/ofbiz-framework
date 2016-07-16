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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Stack;

/**
 *  Record Iterator for reading large files
 *  Note: this is a memory intensive and will not handle files that exceed memory.
 *
 */

public class RecordIterator {

    public static final String module = RecordIterator.class.getName();

    protected BufferedReader br;
    protected ModelDataFile modelDataFile;
    protected InputStream dataFileStream;
    protected boolean closed = false;
    protected String locationInfo;

    protected int nextLineNum = 0;
    protected String curLine = null;
    protected Record curRecord = null;
    protected String nextLine = null;
    protected Record nextRecord = null;
    protected String eof = "\u001A"; // aka ASCII char 26, aka substitute, aka  0x1A, aka CTRL-Z, aka EOF DOS character. Added because problems in some DOS file, specifically file extracted from zip archives.

    public RecordIterator(URL fileUrl, ModelDataFile modelDataFile) throws DataFileException {
        this.modelDataFile = modelDataFile;

        InputStream urlStream = null;
        try {
            urlStream = fileUrl.openStream();
        } catch (IOException e) {
            throw new DataFileException("Error open URL: " + fileUrl.toString(), e);
        }
        this.setupStream(urlStream, fileUrl.toString());
    }

    public RecordIterator(InputStream dataFileStream, ModelDataFile modelDataFile, String locationInfo) throws DataFileException {
        this.modelDataFile = modelDataFile;
        this.setupStream(dataFileStream, locationInfo);
    }

    protected void setupStream(InputStream dataFileStream, String locationInfo) throws DataFileException {
        this.locationInfo = locationInfo;
        this.dataFileStream = dataFileStream;
        try {
            this.br = new BufferedReader(new InputStreamReader(dataFileStream, "UTF-8"));
        } catch (Exception e) {
            throw new DataFileException("UTF-8 is not supported");
        }
        // get the line seeded
        this.getNextLine();
    }

    protected boolean getNextLine() throws DataFileException {
        this.nextLine = null;
        this.nextRecord = null;

        boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle);
        boolean isDelimited = ModelDataFile.SEP_DELIMITED.equals(modelDataFile.separatorStyle);
        // if (Debug.infoOn()) Debug.logInfo("[DataFile.readDataFile] separatorStyle is " + modelDataFile.separatorStyle + ", isFixedRecord: " + isFixedRecord, module);

        if (isFixedRecord) {
            if (modelDataFile.recordLength <= 0) {
                throw new DataFileException("Cannot read a fixed record length file if no record length is specified");
            }
            try {
                char[] charData = new char[modelDataFile.recordLength + 1];

                // if (Debug.infoOn()) Debug.logInfo("[DataFile.readDataFile] reading line " + lineNum + " from position " + (lineNum-1)*modelDataFile.recordLength + ", length is " + modelDataFile.recordLength, module);
                if (br.read(charData, 0, modelDataFile.recordLength) == -1) {
                    nextLine = null;
                    // Debug.logInfo("[DataFile.readDataFile] found end of file, got -1", module);
                } else {
                    nextLine = new String(charData);
                    // if (Debug.infoOn()) Debug.logInfo("[DataFile.readDataFile] read line " + lineNum + " line is: \"" + line + "\"", module);
                }
            } catch (IOException e) {
                throw new DataFileException("Error reading line #" + nextLineNum + " (index " + (nextLineNum - 1) * modelDataFile.recordLength + " length " +
                        modelDataFile.recordLength + ") from location: " + locationInfo, e);
            }
        } else {
            try {
                nextLine = br.readLine();
                //Debug.logInfo("br.readLine()=\"" + nextLine + "\"", module);
            } catch (IOException e) {
                throw new DataFileException("Error reading line #" + nextLineNum + " from location: " + locationInfo, e);
            }
        }

        //if (nextLine != null && !(eof.equals(nextLine.substring(0,1)) && 1 == nextLine.length())) {
        if (nextLine != null && !((nextLine.contains(eof) ) )) {
            nextLineNum++;
            ModelRecord modelRecord = findModelForLine(nextLine, nextLineNum, modelDataFile);
            if (isDelimited) {
                this.nextRecord = Record.createDelimitedRecord(nextLine, nextLineNum, modelRecord, modelDataFile.delimiter, modelDataFile.textDelimiter);
            } else {
                this.nextRecord = Record.createRecord(nextLine, nextLineNum, modelRecord);
            }
            return true;
        } else {
            this.close();
            return false;
        }
    }

    public int getCurrentLineNumber() {
        return this.nextLineNum - 1;
    }

    public boolean hasNext() {
        //return nextLine != null && !(eof.equals(nextLine.substring(0,1)) && 1 == nextLine.length());
        return nextLine != null && !((nextLine.contains(eof) ) );
    }

    public Record next() throws DataFileException {
        if (!hasNext()) {
            return null;
        }

        if (ModelDataFile.SEP_DELIMITED.equals(modelDataFile.separatorStyle) || ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle) || ModelDataFile.SEP_FIXED_LENGTH.equals(modelDataFile.separatorStyle)) {
            boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle);
            // if (Debug.infoOn()) Debug.logInfo("[DataFile.readDataFile] separatorStyle is " + modelDataFile.separatorStyle + ", isFixedRecord: " + isFixedRecord, module);
            // advance the line (we have already checked to make sure there is a next line
            this.curLine = this.nextLine;
            this.curRecord = this.nextRecord;

            // get a new next line
            this.getNextLine();

            // first check to see if the file type has a line size, and if so if this line complies
            if (!isFixedRecord && modelDataFile.recordLength > 0 && curLine.length() != modelDataFile.recordLength) {
                throw new DataFileException("Line number " + this.getCurrentLineNumber() + " was not the expected length; expected: " + modelDataFile.recordLength + ", got: " + curLine.length());
            }

            // if this record has children, put it on the parentStack and get/check the children now
            if (this.curRecord.getModelRecord().childRecords.size() > 0) {
                Stack<Record> parentStack = new Stack<Record>();
                parentStack.push(curRecord);

                while (this.nextRecord != null && this.nextRecord.getModelRecord().parentRecord != null) {
                    // if parent equals top parent on stack, add to that parents child list, otherwise pop off parent and try again
                    Record parentRecord = null;

                    while (parentStack.size() > 0) {
                        parentRecord = parentStack.peek();
                        if (parentRecord.recordName.equals(this.nextRecord.getModelRecord().parentName)) {
                            break;
                        } else {
                            parentStack.pop();
                            parentRecord = null;
                        }
                    }
                    if (parentRecord == null) {
                        throw new DataFileException("Expected Parent Record not found for line " + this.getCurrentLineNumber() + "; record name of expected parent is " + this.nextRecord.getModelRecord().parentName);
                    }
                    parentRecord.addChildRecord(this.nextRecord);

                    // if the child record we just added is also a parent, push it onto the stack
                    if (this.nextRecord.getModelRecord().childRecords.size() > 0) {
                        parentStack.push(this.nextRecord);
                    }
                    // if it can't find a next line it will nextRecord will be null and the loop will break out
                    this.getNextLine();
                }
            }
        } else {
            throw new DataFileException("Separator style " + modelDataFile.separatorStyle + " not recognized.");
        }
        return curRecord;
    }

    public void close() throws DataFileException {
        if (this.closed) {
            return;
        }
        try {
            this.br.close(); // this should also close the stream
            this.closed = true;
        } catch (IOException e) {
            throw new DataFileException("Error closing data file input stream", e);
        }
    }


    /** Searches through the record models to find one with a matching type-code, if no type-code exists that model will always be used if it gets to it
     * @param line
     * @param lineNum
     * @param modelDataFile
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return return the ModelRecord Object found
     */
    protected static ModelRecord findModelForLine(String line, int lineNum, ModelDataFile modelDataFile) throws DataFileException {
        // if (Debug.infoOn()) Debug.logInfo("[DataFile.findModelForLine] line: " + line, module);
        ModelRecord modelRecord = null;

        for (ModelRecord curModelRecord: modelDataFile.records) {
            if (curModelRecord.tcPosition < 0) {
                modelRecord = curModelRecord;
                break;
            }
            String typeCode = line.substring(curModelRecord.tcPosition, curModelRecord.tcPosition + curModelRecord.tcLength);

            // try to match with a single typecode
            if (curModelRecord.typeCode.length() > 0) {
                // if (Debug.infoOn()) Debug.logInfo("[DataFile.findModelForLine] Doing plain typecode match - code=" + curModelRecord.typeCode + ", filelinecode=" + typeCode, module);
                if (typeCode != null && typeCode.equals(curModelRecord.typeCode)) {
                    modelRecord = curModelRecord;
                    break;
                }
            } // try to match a ranged typecode (tcMin <= typeCode <= tcMax)
            else if (curModelRecord.tcMin.length() > 0 || curModelRecord.tcMax.length() > 0) {
                if (curModelRecord.tcIsNum) {
                    // if (Debug.infoOn()) Debug.logInfo("[DataFile.findModelForLine] Doing ranged number typecode match - minNum=" + curModelRecord.tcMinNum + ", maxNum=" + curModelRecord.tcMaxNum + ", filelinecode=" + typeCode, module);
                    long typeCodeNum = Long.parseLong(typeCode);
                    if ((curModelRecord.tcMinNum < 0 || typeCodeNum >= curModelRecord.tcMinNum) &&
                            (curModelRecord.tcMaxNum < 0 || typeCodeNum <= curModelRecord.tcMaxNum)) {
                        modelRecord = curModelRecord;
                        break;
                    }
                } else {
                    // if (Debug.infoOn()) Debug.logInfo("[DataFile.findModelForLine] Doing ranged String typecode match - min=" + curModelRecord.tcMin + ", max=" + curModelRecord.tcMax + ", filelinecode=" + typeCode, module);
                    if ((typeCode.compareTo(curModelRecord.tcMin) >= 0) && (typeCode.compareTo(curModelRecord.tcMax) <= 0)) {
                        modelRecord = curModelRecord;
                        break;
                    }
                }
            }
        }

        if (modelRecord == null) {
            throw new DataFileException("Could not find record definition for line " + lineNum + "; first bytes: " +
                    line.substring(0, (line.length() > 5) ? 5 : line.length()));
        }
        // if (Debug.infoOn()) Debug.logInfo("[DataFile.findModelForLine] Got record model named " + modelRecord.name, module);
        return modelRecord;
    }
}

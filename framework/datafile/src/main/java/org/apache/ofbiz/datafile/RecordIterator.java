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
import java.nio.charset.Charset;
import java.util.Stack;

/**
 * Record Iterator for reading large files
 * Note: this is a memory intensive and will not handle files that exceed memory.
 */
public class RecordIterator {

    private static final String MODULE = RecordIterator.class.getName();

    private BufferedReader br;
    private ModelDataFile modelDataFile;
    private InputStream dataFileStream;
    private boolean closed = false;
    private String locationInfo;

    private int nextLineNum = 0;
    private String curLine = null;
    private Record curRecord = null;
    private String nextLine = null;
    private Record nextRecord = null;
    private String eof = "\u001A"; // aka ASCII char 26, aka substitute, aka  0x1A, aka CTRL-Z, aka EOF DOS character. Added because problems in
    // some DOS file, specifically file extracted from zip archives.

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

    /**
     * Sets stream.
     * @param dataFileStream the data file stream
     * @param locationInfo   the location info
     * @throws DataFileException the data file exception
     */
    protected void setupStream(InputStream dataFileStream, String locationInfo) throws DataFileException {
        this.locationInfo = locationInfo;
        this.dataFileStream = dataFileStream;
        String charsetStr = modelDataFile.getEncodingType();
        try {
            this.br = new BufferedReader(new InputStreamReader(dataFileStream, Charset.forName(charsetStr)));
        } catch (Exception e) {
            throw new DataFileException(charsetStr + " is not supported");
        }
        //move the cursor to the good start line
        try {
            for (int i = 0; i < modelDataFile.getStartLine(); i++) {
                br.readLine();
            }
        } catch (IOException e) {
            throw new DataFileException("Impossible to read the buffer");
        }
        // get the line seeded
        this.getNextLine();
    }

    /**
     * Gets next line.
     * @return the next line
     * @throws DataFileException the data file exception
     */
    protected boolean getNextLine() throws DataFileException {
        this.nextLine = null;
        this.nextRecord = null;

        boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.getSeparatorStyle());
        boolean isDelimited = ModelDataFile.SEP_DELIMITED.equals(modelDataFile.getSeparatorStyle());

        if (isFixedRecord) {
            if (modelDataFile.getRecordLength() <= 0) {
                throw new DataFileException("Cannot read a fixed record length file if no record length is specified");
            }
            try {
                char[] charData = new char[modelDataFile.getRecordLength() + 1];

                if (br.read(charData, 0, modelDataFile.getRecordLength()) == -1) {
                    nextLine = null;
                } else {
                    nextLine = new String(charData);
                }
            } catch (IOException e) {
                throw new DataFileException("Error reading line #" + nextLineNum + " (index " + (nextLineNum - 1) * modelDataFile.getRecordLength()
                        + " length " + modelDataFile.getRecordLength() + ") from location: " + locationInfo, e);
            }
        } else {
            try {
                nextLine = br.readLine();
            } catch (IOException e) {
                throw new DataFileException("Error reading line #" + nextLineNum + " from location: " + locationInfo, e);
            }
        }

        if (nextLine != null && !((nextLine.contains(eof)))) {
            nextLineNum++;
            ModelRecord modelRecord = findModelForLine(nextLine, nextLineNum, modelDataFile);
            if (isDelimited) {
                this.nextRecord = Record.createDelimitedRecord(nextLine, nextLineNum, modelRecord, modelDataFile.getDelimiter(),
                        modelDataFile.getTextDelimiter());
            } else {
                this.nextRecord = Record.createRecord(nextLine, nextLineNum, modelRecord);
            }
            return true;
        } else {
            this.close();
            return false;
        }
    }

    /**
     * Gets current line number.
     * @return the current line number
     */
    public int getCurrentLineNumber() {
        return this.nextLineNum - 1;
    }

    /**
     * Has next boolean.
     * @return the boolean
     */
    public boolean hasNext() {
        return nextLine != null && !((nextLine.contains(eof)));
    }

    /**
     * Next record.
     * @return the record
     * @throws DataFileException the data file exception
     */
    public Record next() throws DataFileException {
        int recordLength = modelDataFile.getRecordLength();
        if (!hasNext()) {
            return null;
        }

        if (ModelDataFile.SEP_DELIMITED.equals(modelDataFile.getSeparatorStyle())
                || ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.getSeparatorStyle())
                || ModelDataFile.SEP_FIXED_LENGTH.equals(modelDataFile.getSeparatorStyle())) {
            boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.getSeparatorStyle());
            // advance the line (we have already checked to make sure there is a next line
            this.curLine = this.nextLine;
            this.curRecord = this.nextRecord;

            // get a new next line
            this.getNextLine();

            // first check to see if the file type has a line size, and if so if this line complies
            if (!isFixedRecord && recordLength > 0 && curLine.length() != recordLength) {
                throw new DataFileException(
                        "Line number " + this.getCurrentLineNumber() + " was not the expected length; expected: " + recordLength
                                + ", got: " + curLine.length());
            }

            // if this record has children, put it on the parentStack and get/check the children now
            if (this.curRecord.getModelRecord().getChildRecords().size() > 0) {
                Stack<Record> parentStack = new Stack<>();
                parentStack.push(curRecord);

                while (this.nextRecord != null && this.nextRecord.getModelRecord().getParentRecord() != null) {
                    // if parent equals top parent on stack, add to that parents child list, otherwise pop off parent and try again
                    Record parentRecord = null;

                    while (!parentStack.isEmpty()) {
                        parentRecord = parentStack.peek();
                        if (parentRecord.getRecordName().equals(this.nextRecord.getModelRecord().getParentName())) {
                            break;
                        } else {
                            parentStack.pop();
                            parentRecord = null;
                        }
                    }
                    if (parentRecord == null) {
                        throw new DataFileException("Expected Parent Record not found for line " + this.getCurrentLineNumber()
                                + "; record name of expected parent is " + this.nextRecord.getModelRecord().getParentName());
                    }
                    parentRecord.addChildRecord(this.nextRecord);

                    // if the child record we just added is also a parent, push it onto the stack
                    if (this.nextRecord.getModelRecord().getChildRecords().size() > 0) {
                        parentStack.push(this.nextRecord);
                    }
                    // if it can't find a next line it will nextRecord will be null and the loop will break out
                    this.getNextLine();
                }
            }
        } else {
            throw new DataFileException("Separator style " + modelDataFile.getSeparatorStyle() + " not recognized.");
        }
        return curRecord;
    }

    /**
     * Close.
     * @throws DataFileException the data file exception
     */
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

    /** Searches through the record models to find one with a matching type-code, if no type-code exists that model will always be used if
     * it gets to it
     * @param line
     * @param lineNum
     * @param modelDataFile
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return return the ModelRecord Object found
     */
    protected static ModelRecord findModelForLine(String line, int lineNum, ModelDataFile modelDataFile) throws DataFileException {
        ModelRecord modelRecord = null;

        for (ModelRecord curModelRecord : modelDataFile.getRecords()) {
            if (curModelRecord.getTcPosition() < 0) {
                modelRecord = curModelRecord;
                break;
            }
            String typeCode = line.substring(curModelRecord.getTcPosition(), curModelRecord.getTcPosition() + curModelRecord.getTcLength());

            // try to match with a single typecode
            if (curModelRecord.getTypeCode().length() > 0) {
                if (!typeCode.isEmpty() && typeCode.equals(curModelRecord.getTypeCode())) {
                    modelRecord = curModelRecord;
                    break;
                }
            } else if (curModelRecord.getTcMin().length() > 0 || curModelRecord.getTcMax().length() > 0) {
                if (curModelRecord.isTcIsNum()) {
                    long typeCodeNum = Long.parseLong(typeCode);
                    if ((curModelRecord.getTcMinNum() < 0 || typeCodeNum >= curModelRecord.getTcMinNum()) && (curModelRecord.getTcMaxNum() < 0
                            || typeCodeNum <= curModelRecord.getTcMaxNum())) {
                        modelRecord = curModelRecord;
                        break;
                    }
                } else {
                    if ((typeCode.compareTo(curModelRecord.getTcMin()) >= 0) && (typeCode.compareTo(curModelRecord.getTcMax()) <= 0)) {
                        modelRecord = curModelRecord;
                        break;
                    }
                }
            }
        }

        if (modelRecord == null) {
            throw new DataFileException("Could not find record definition for line " + lineNum + "; first bytes: "
                    + line.substring(0, (line.length() > 5) ? 5 : line.length()));
        }
        return modelRecord;
    }
}

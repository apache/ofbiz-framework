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

package org.apache.ofbiz.base.util;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.imaging.ImageReadException;
import org.apache.ofbiz.entity.Delegator;

/**
 * HttpRequestFileUpload - Receive a file upload through an HttpServletRequest
 *
 */
public class HttpRequestFileUpload {
    private static final String MODULE = HttpRequestFileUpload.class.getName();
    private static final int BUFFER_SIZE = 4096;
    private static final int WAIT_INTERVAL = 200; // in milliseconds
    private static final int MAX_WAITS = 20;
    private int waitCount = 0;
    private String savePath;
    private String filepath;
    private String filename;
    private String contentType;
    private String overrideFilename = null;
    private Map<String, String> fields;

    /**
     * Gets override filename.
     * @return the override filename
     */
    public String getOverrideFilename() {
        return overrideFilename;
    }

    /**
     * Sets override filename.
     * @param ofName the of name
     */
    public void setOverrideFilename(String ofName) {
        overrideFilename = ofName;
    }

    /**
     * Gets filename.
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Gets filepath.
     * @return the filepath
     */
    public String getFilepath() {
        return filepath;
    }

    /**
     * Sets save path.
     * @param savePath the save path
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    /**
     * Gets content type.
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets field value.
     * @param fieldName the field name
     * @return the field value
     */
    public String getFieldValue(String fieldName) {
        if (fields == null || fieldName == null) {
            return null;
        }
        return fields.get(fieldName);
    }

    private void setFilename(String s) {
        if (s == null) {
            return;
        }

        int pos = s.indexOf("filename=\"");

        if (pos != -1) {
            filepath = s.substring(pos + 10, s.length() - 1);
            // Windows browsers include the full path on the client
            // But Linux/Unix and Mac browsers only send the filename
            // test if this is from a Windows browser
            pos = filepath.lastIndexOf("\\");
            if (pos != -1) {
                filename = filepath.substring(pos + 1);
            } else {
                filename = filepath;
            }
        }
    }

    private void setContentType(String s) {
        if (s == null) {
            return;
        }

        int pos = s.indexOf(": ");

        if (pos != -1) {
            contentType = s.substring(pos + 2, s.length());
        }
    }

    /**
     * Do upload.
     * @param request the request
     * @throws IOException the io exception
     */
    public boolean doUpload(HttpServletRequest request, String fileType) throws IOException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        ServletInputStream in = request.getInputStream();

        String reqLengthString = request.getHeader("content-length");
        Debug.logInfo("expect " + reqLengthString + " bytes.", MODULE);
        int requestLength = 0;

        try {
            requestLength = Integer.parseInt(reqLengthString);
        } catch (NumberFormatException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return false;
        }
        byte[] line = new byte[BUFFER_SIZE];

        int i = -1;

        i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
        requestLength -= i;
        if (i < 3) {
            Debug.logError("Possibly a waitingReadLine error", MODULE);
            return false;
        }
        int boundaryLength = i - 2;

        String boundary = new String(line, 0, boundaryLength, StandardCharsets.UTF_8); // -2 discards the newline character

        Debug.logInfo("boundary=[" + boundary + "] length is " + boundaryLength, MODULE);
        fields = new HashMap<>();

        while (requestLength > 0/* i != -1*/) {
            String newLine = "";

            if (i > -1) {
                newLine = new String(line, 0, i, StandardCharsets.UTF_8);
            }
            if (newLine.startsWith("Content-Disposition: form-data; name=\"")) {
                if (newLine.indexOf("filename=\"") != -1) {
                    setFilename(new String(line, 0, i - 2, StandardCharsets.UTF_8));
                    if (filename == null) {
                        return false;
                    }
                    // this is the file content
                    i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                    requestLength -= i;

                    setContentType(new String(line, 0, i - 2, StandardCharsets.UTF_8));

                    // blank line
                    i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                    requestLength -= i;
                    String filenameToUse = filename;

                    if (overrideFilename != null) {
                        filenameToUse = overrideFilename;
                    }

                    // first line of actual file
                    i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                    requestLength -= i;
                    byte[] lastTwoBytes = new byte[2];

                    if (i > 1) {
                        lastTwoBytes[0] = line[i - 2];
                        lastTwoBytes[1] = line[i - 1];
                    }
                    Debug.logInfo("about to create a file:" + (savePath == null ? "" : savePath) + filenameToUse, MODULE);
                    // before creating the file make sure directory exists
                    if (savePath == null) {
                        throw new IllegalArgumentException("savePath is null");
                    }
                    File savePathFile = new File(savePath);
                    if (!savePathFile.exists()) {
                        if (!savePathFile.mkdirs()) {
                            Debug.logError("Directory could not be created", filenameToUse);
                        }

                    }
                    String fileTocheck = savePath + filenameToUse;
                    try (FileOutputStream fos = new FileOutputStream(fileTocheck);) {
                        boolean bail = (new String(line, 0, i, StandardCharsets.UTF_8).startsWith(boundary));
                        boolean oneByteLine = (i == 1); // handle one-byte lines

                        while ((requestLength > 0/* i != -1 */) && !bail) {

                            // write the current buffer, except the last 2 bytes;
                            if (i > 1) {
                                fos.write(line, 0, i - 2);
                            }

                            oneByteLine = (i == 1); // we need to track on-byte lines differently

                            i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                            requestLength -= i;

                            // the problem is the last line of the file content
                            // contains the new line character.

                            // if the line just read was the last line, we're done.
                            // if not, we must write the last 2 bytes of the previous buffer
                            // just assume that a one-byte line isn't the last line

                            if (requestLength < 1) {
                                bail = true;
                            } else if (oneByteLine) {
                                fos.write(lastTwoBytes, 0, 1); // we only saved one byte
                            } else {
                                fos.write(lastTwoBytes, 0, 2);
                            }

                            if (i > 1) {
                                // save the last 2 bytes of the buffer
                                lastTwoBytes[0] = line[i - 2];
                                lastTwoBytes[1] = line[i - 1];
                            } else {
                                lastTwoBytes[0] = line[0]; // only save one byte
                            }
                        }
                        fos.flush();

                        if (!org.apache.ofbiz.security.SecuredUpload.isValidFile(fileTocheck, fileType, delegator)) {
                            return false;
                        }
                    } catch (ImageReadException e) {
                        Debug.logError(e, MODULE);
                        return false;
                    }
                } else {
                    // this is a field
                    // get the field name
                    int pos = newLine.indexOf("name=\"");
                    String fieldName = newLine.substring(pos + 6, newLine.length() - 3);

                    // blank line
                    i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                    requestLength -= i;
                    i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                    requestLength -= i;
                    newLine = new String(line, 0, i, StandardCharsets.UTF_8);
                    StringBuilder fieldValue = new StringBuilder(BUFFER_SIZE);

                    while (requestLength > 0/* i != -1*/ && !newLine.startsWith(boundary)) {
                        // The last line of the field
                        // contains the new line character.
                        // So, we need to check if the current line is
                        // the last line.
                        i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
                        requestLength -= i;
                        if ((i == boundaryLength + 2 || i == boundaryLength + 4) // + 4 is eof
                                && (new String(line, 0, i).startsWith(boundary))) {
                            fieldValue.append(newLine.substring(0, newLine.length() - 2));
                        } else {
                            fieldValue.append(newLine);
                        }
                        newLine = new String(line, 0, i, StandardCharsets.UTF_8);
                    }
                    fields.put(fieldName, fieldValue.toString());
                }
            }
            i = waitingReadLine(in, line, 0, BUFFER_SIZE, requestLength);
            if (i > -1) {
                requestLength -= i;
            }

        } // end while
        return true;
    }

    // reads a line, waiting if there is nothing available and reqLen > 0
    private int waitingReadLine(ServletInputStream in, byte[] buf, int off, int len, int reqLen) throws IOException {
        int i = -1;

        while (((i = in.readLine(buf, off, len)) == -1) && (reqLen > 0)) {
            Debug.logInfo("waiting", MODULE);
            if (waitCount > MAX_WAITS) {
                Debug.logInfo("waited " + waitCount + " times, bailing out while still expecting "
                        + reqLen + " bytes.", MODULE);
                throw new IOException("waited " + waitCount + " times, bailing out while still expecting "
                        + reqLen + " bytes.");
            }
            waitCount++;
            long endMS = new Date().getTime() + WAIT_INTERVAL;

            while (endMS > (new Date().getTime())) {
                try {
                    wait(WAIT_INTERVAL);
                } catch (InterruptedException e) {
                    Debug.logInfo(".", MODULE);
                }
            }
            Debug.logInfo((new Date().getTime() - endMS) + " ms", MODULE);
        }
        return i;
    }

}

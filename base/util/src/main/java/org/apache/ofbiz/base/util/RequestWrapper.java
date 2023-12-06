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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RequestWrapper extends HttpServletRequestWrapper {

    private static final int INITIAL_BUFFER_SIZE = 1024;
    private HttpServletRequest origRequest;
    private byte[] reqBytes;
    private boolean firstTime = true;
    // private Map<String, String[]> parameterMap = null;

    public RequestWrapper(HttpServletRequest arg) {
        super(arg);
        origRequest = arg;
    }

    /**
     * The default behavior of this method is to return getReader() on the wrapped request object.
     */
    public BufferedReader getReader() throws IOException {

        getBytes();

        InputStreamReader dave = new InputStreamReader(new ByteArrayInputStream(reqBytes));
        BufferedReader br = new BufferedReader(dave);
        return br;
    }

    /**
     * The default behavior of this method is to return getInputStream() on the wrapped request object.
     */
    public ServletInputStream getInputStream() throws IOException {

        getBytes();

        ServletInputStream sis = new ServletInputStream() {
            private int numberOfBytesAlreadyRead;

            @Override
            public int read() throws IOException {
                byte b;
                if (reqBytes.length > numberOfBytesAlreadyRead) {
                    b = reqBytes[numberOfBytesAlreadyRead++];
                } else {
                    b = -1;
                }
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len > (reqBytes.length - numberOfBytesAlreadyRead)) {
                    len = reqBytes.length - numberOfBytesAlreadyRead;
                }
                if (len <= 0) {
                    return -1;
                }
                System.arraycopy(reqBytes, numberOfBytesAlreadyRead, b, off, len);
                numberOfBytesAlreadyRead += len;
                return len;
            }

            @Override
            /**
             * Needed by Servlet 3.1 No worry this is not used in OFBiz code
             */
            public boolean isFinished() {
                return false;
            }

            /**
             * Needed by Servlet 3.1 No worry this is not used in OFBiz code
             */
            @Override
            public boolean isReady() {
                return false;
            }

            /**
             * Needed by Servlet 3.1 No worry this is not used in OFBiz code
             */
            @Override
            public void setReadListener(ReadListener listener) {
            }

        };

        return sis;
    }

    /**
     * Returns the bytes taken from the original request getInputStream
     */
    public byte[] getBytes() throws IOException {
        if (firstTime) {
            firstTime = false;
            // Read the parameters first, because they can't be reached after the inputStream is read.
            getParameterMap();
            int initialSize = origRequest.getContentLength();
            if (initialSize < INITIAL_BUFFER_SIZE) {
                initialSize = INITIAL_BUFFER_SIZE;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(initialSize);
            byte[] buf = new byte[1024];
            InputStream is = origRequest.getInputStream();
            int len = 0;
            while (len >= 0) {
                len = is.read(buf);
                if (len > 0) {
                    baos.write(buf, 0, len);
                }
            }
            reqBytes = baos.toByteArray();
        }
        return reqBytes;
    }

    // Those are not used, just kept in case
    // @Override
    // public String getParameter(String name) {
    // parameterMap = UtilMisc.toMap(getParameterMap());
    // if (parameterMap != null) {
    // String[] a = parameterMap.get(name);
    // if (a == null || a.length == 0) {
    // return null;
    // }
    // return a[0];
    // }
    // return null;
    // }
    //
    // @Override
    // public Map<String, String[]> getParameterMap() {
    // if (parameterMap == null) {
    // parameterMap = new HashMap<String, String[]>();
    // parameterMap.putAll(super.getParameterMap());
    // }
    // return parameterMap;
    // }
    //
    // @SuppressWarnings("unchecked")
    // @Override
    // public Enumeration getParameterNames() {
    // return Collections.enumeration(parameterMap.values());
    // }
    //
    // @Override
    // public String[] getParameterValues(String name) {
    // return parameterMap.get(name);
    // }
}

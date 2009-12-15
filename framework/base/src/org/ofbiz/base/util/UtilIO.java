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
package org.ofbiz.base.util;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public final class UtilIO {
    public static final String module = UtilIO.class.getName();

    public static final String readString(InputStream stream) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(stream));

            String str;
            while ((str = br.readLine()) != null) {
                buf.append(str);
                buf.append(System.getProperty("line.separator"));
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Debug.logError(e, "Error closing after reading text: " + e.toString(), module);
                }
            }
        }
        return buf.toString();
    }

    public static final String readString(Reader reader) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader br = null;
        try {
            br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

            String str;
            while ((str = br.readLine()) != null) {
                buf.append(str);
                buf.append(System.getProperty("line.separator"));
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Debug.logError(e, "Error closing after reading text: " + e.toString(), module);
                }
            }
        }
        return buf.toString();
    }
}

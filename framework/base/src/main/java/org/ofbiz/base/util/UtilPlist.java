/*
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
 */
package org.ofbiz.base.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.ofbiz.base.util.UtilGenerics.checkList;
import static org.ofbiz.base.util.UtilGenerics.checkMap;

/**
 * File Utilities
 *
 */
public final class UtilPlist {

    public static final String module = UtilPlist.class.getName();

    private UtilPlist() {}
    
    /** simple 4 char indentation */
    private static final String indentFourString = "    ";

    public static void writePlistProperty(String name, Object value, int indentLevel, PrintWriter writer) {
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.print(name);
        writer.print(" = ");
        if (value instanceof Map<?, ?>) {
            writer.println();
            Map<String, Object> map = checkMap(value);
            writePlistPropertyMap(map, indentLevel, writer, false);
        } else if (value instanceof List<?>) {
            List<Object> list = checkList(value);
            writePlistPropertyValueList(list, indentLevel, writer);
        } else {
            writer.print(value);
            writer.println(";");
        }
    }
    public static void writePlistPropertyMap(Map<String, Object> propertyMap, int indentLevel, PrintWriter writer, boolean appendComma) {
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.println("{");
        for (Map.Entry<String, Object> property: propertyMap.entrySet()) {
            writePlistProperty(property.getKey(), property.getValue(), indentLevel + 1, writer);
        }
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        if (appendComma) {
            writer.println("},");
        } else {
            writer.println("}");
        }
    }
    public static void writePlistPropertyValueList(List<Object> propertyValueList, int indentLevel, PrintWriter writer) {
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.print("(");

        Iterator<Object> propertyValueIter = propertyValueList.iterator();
        while (propertyValueIter.hasNext()) {
            Object propertyValue = propertyValueIter.next();
            if (propertyValue instanceof Map<?, ?>) {
                Map<String, Object> propertyMap = checkMap(propertyValue);
                writePlistPropertyMap(propertyMap, indentLevel + 1, writer, propertyValueIter.hasNext());
            } else {
                writer.print(propertyValue);
                if (propertyValueIter.hasNext()) writer.print(", ");
            }
        }

        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.println(");");
    }

    public static void writePlistPropertyXml(String name, Object value, int indentLevel, PrintWriter writer) {
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.print("<key>");
        writer.print(name);
        writer.println("</key>");
        if (value instanceof Map<?, ?>) {
            Map<String, Object> map = checkMap(value);
            writePlistPropertyMapXml(map, indentLevel, writer);
        } else if (value instanceof List<?>) {
            List<Object> list = checkList(value);
            writePlistPropertyValueListXml(list, indentLevel, writer);
        } else {
            for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
            writer.print("<string>");
            writer.print(value);
            writer.println("</string>");
        }
    }
    public static void writePlistPropertyMapXml(Map<String, Object> propertyMap, int indentLevel, PrintWriter writer) {
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.println("<dict>");
        for (Map.Entry<String, Object> property: propertyMap.entrySet()) {
            writePlistPropertyXml(property.getKey(), property.getValue(), indentLevel + 1, writer);
        }
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.println("</dict>");
    }
    public static void writePlistPropertyValueListXml(List<Object> propertyValueList, int indentLevel, PrintWriter writer) {
        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.println("<array>");

        indentLevel++;
        Iterator<Object> propertyValueIter = propertyValueList.iterator();
        while (propertyValueIter.hasNext()) {
            Object propertyValue = propertyValueIter.next();
            if (propertyValue instanceof Map<?, ?>) {
                Map<String, Object> propertyMap = checkMap(propertyValue);
                writePlistPropertyMapXml(propertyMap, indentLevel, writer);
            } else {
                for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
                writer.print("<string>");
                writer.print(propertyValue);
                writer.println("</string>");
            }
        }
        indentLevel--;

        for (int i = 0; i < indentLevel; i++) writer.print(indentFourString);
        writer.println("</array>");
    }

    /**
     * Writes model information in the Apple EOModelBundle format.
     *
     * For document structure and definition see: http://developer.apple.com/documentation/InternetWeb/Reference/WO_BundleReference/Articles/EOModelBundle.html
     *
     * @param eoModelMap
     * @param eomodeldFullPath
     * @param filename
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    public static void writePlistFile(Map<String, Object> eoModelMap, String eomodeldFullPath, String filename, boolean useXml) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter plistWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(eomodeldFullPath, filename)), "UTF-8")));
        if (useXml) {
            plistWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            plistWriter.println("<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">");
            plistWriter.println("<plist version=\"1.0\">");
            writePlistPropertyMapXml(eoModelMap, 0, plistWriter);
            plistWriter.println("</plist>");
        } else {
            writePlistPropertyMap(eoModelMap, 0, plistWriter, false);
        }
        plistWriter.close();
    }
}

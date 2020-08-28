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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contains extra information about Messages
 *
 */
@SuppressWarnings("serial")
public class MessageString implements Serializable {

    private static final String MODULE = MessageString.class.getName();

    private String message;
    private String fieldName;
    private String toFieldName;
    private Throwable sourceError;
    private Locale locale;
    private String propertyResource;
    private String propertyName;
    private boolean isError = true;

    public static List<Object> getMessagesForField(String fieldName, boolean convertToStrings, List<Object> messageStringList) {
        if (fieldName == null) {
            return Collections.emptyList();
        }
        Set<String> fieldSet = new TreeSet<>();
        fieldSet.add(fieldName);
        return getMessagesForField(fieldSet, convertToStrings, messageStringList);
    }
    public static List<Object> getMessagesForField(String fieldName1, String fieldName2, String fieldName3, String fieldName4,
                                                   boolean convertToStrings, List<Object> messageStringList) {
        Set<String> fieldSet = new TreeSet<>();
        if (UtilValidate.isNotEmpty(fieldName1)) {
            fieldSet.add(fieldName1);
        }
        if (UtilValidate.isNotEmpty(fieldName2)) {
            fieldSet.add(fieldName2);
        }
        if (UtilValidate.isNotEmpty(fieldName3)) {
            fieldSet.add(fieldName3);
        }
        if (UtilValidate.isNotEmpty(fieldName4)) {
            fieldSet.add(fieldName4);
        }
        return getMessagesForField(fieldSet, convertToStrings, messageStringList);
    }
    public static List<Object> getMessagesForField(Set<String> fieldNameSet, boolean convertToStrings, List<Object> messageStringList) {
        if (messageStringList == null || UtilValidate.isEmpty(fieldNameSet)) {
            return Collections.emptyList();
        }
        List<Object> outList = new ArrayList<>(messageStringList.size());
        for (Object messageStringCur: messageStringList) {
            if (messageStringCur instanceof MessageString) {
                MessageString messageString = (MessageString) messageStringCur;
                if (messageString.isForField(fieldNameSet)) {
                    if (convertToStrings) {
                        outList.add(messageString.toString());
                    } else {
                        outList.add(messageString);
                    }
                }
            } else {
                // not a MessageString, don't know if it is for this field so skip it
                continue;
            }
        }
        return outList;
    }

    /**
     * @param message
     * @param fieldName
     * @param locale
     * @param propertyResource
     * @param propertyName
     */
    public MessageString(String message, String fieldName, String propertyResource, String propertyName, Locale locale, boolean isError) {
        this.message = message;
        this.fieldName = fieldName;
        this.locale = locale;
        this.propertyResource = propertyResource;
        this.propertyName = propertyName;
        this.isError = isError;
    }
    /**
     * @param message
     * @param fieldName
     */
    public MessageString(String message, String fieldName, boolean isError) {
        this.message = message;
        this.fieldName = fieldName;
        this.isError = isError;
    }
    /**
     * @param message
     * @param fieldName
     * @param toFieldName
     * @param sourceError
     */
    public MessageString(String message, String fieldName, String toFieldName, Throwable sourceError) {
        this.message = message;
        this.fieldName = fieldName;
        this.toFieldName = toFieldName;
        this.sourceError = sourceError;
        this.isError = true;
    }
    /**
     * @param message
     * @param sourceError
     */
    public MessageString(String message, Throwable sourceError) {
        this.message = message;
        this.sourceError = sourceError;
        this.isError = true;
    }

    /**
     * @return Returns the fieldName.
     */
    public String getFieldName() {
        return fieldName;
    }
    /**
     * @param fieldName The fieldName to set.
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Is for field boolean.
     * @param fieldNameSet the field name set
     * @return the boolean
     */
    public boolean isForField(Set<String> fieldNameSet) {
        return fieldNameSet == null || fieldNameSet.contains(this.fieldName);
    }

    /**
     * Is for field boolean.
     * @param fieldName the field name
     * @return the boolean
     */
    public boolean isForField(String fieldName) {
        if (this.fieldName == null) {
            return fieldName == null;
        }
        return this.fieldName.equals(fieldName);
    }

    /**
     * @return Returns the message.
     */
    public String getMessage() {
        return message;
    }
    /**
     * @param message The message to set.
     */
    public void setMessage(String message) {
        this.message = message;
    }
    /**
     * @return Returns the sourceError.
     */
    public Throwable getSourceError() {
        return sourceError;
    }
    /**
     * @param sourceError The sourceError to set.
     */
    public void setSourceError(Throwable sourceError) {
        this.sourceError = sourceError;
    }
    /**
     * @return Returns the toFieldName.
     */
    public String getToFieldName() {
        return toFieldName;
    }
    /**
     * @param toFieldName The toFieldName to set.
     */
    public void setToFieldName(String toFieldName) {
        this.toFieldName = toFieldName;
    }

    /**
     * @return Returns the locale.
     */
    public Locale getLocale() {
        return locale;
    }
    /**
     * @param locale The locale to set.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
    /**
     * @return Returns the propertyName.
     */
    public String getPropertyName() {
        return propertyName;
    }
    /**
     * @param propertyName The propertyName to set.
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    /**
     * @return Returns the propertyResource.
     */
    public String getPropertyResource() {
        return propertyResource;
    }
    /**
     * @param propertyResource The propertyResource to set.
     */
    public void setPropertyResource(String propertyResource) {
        this.propertyResource = propertyResource;
    }

    /**
     * @return Returns the isError.
     */
    public boolean isError() {
        return isError;
    }
    /**
     * @param isError The isError to set.
     */
    public void setError(boolean isError) {
        this.isError = isError;
    }

    @Override
    public String toString() {
        return this.message;
    }
}

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
package org.apache.ofbiz.minilang.operation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.MessageString;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * A single operation, does the specified operation on the given field
 */
public abstract class SimpleMapOperation {

    private String fieldName;
    private boolean isProperty = false;
    private String message = null;
    private String propertyResource = null;
    private SimpleMapProcess simpleMapProcess;

    public SimpleMapOperation(Element element, SimpleMapProcess simpleMapProcess) {
        Element failMessage = UtilXml.firstChildElement(element, "fail-message");
        Element failProperty = UtilXml.firstChildElement(element, "fail-property");
        if (failMessage != null) {
            this.message = failMessage.getAttribute("message");
            this.isProperty = false;
        } else if (failProperty != null) {
            this.propertyResource = failProperty.getAttribute("resource");
            this.message = failProperty.getAttribute("property");
            this.isProperty = true;
        }
        this.simpleMapProcess = simpleMapProcess;
        this.fieldName = simpleMapProcess.getFieldName();
    }

    /**
     * Gets field name.
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Add message.
     * @param messages the messages
     * @param loader   the loader
     * @param locale   the locale
     */
    public void addMessage(List<Object> messages, ClassLoader loader, Locale locale) {
        if (!isProperty && message != null) {
            messages.add(new MessageString(message, fieldName, true));
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapOperation.addMessage] Adding message: " + message, MODULE);
        } else if (isProperty && propertyResource != null && message != null) {
            // this one doesn't do the proper i18n: String propMsg = UtilProperties.getPropertyValue(UtilURL
            // .fromResource(propertyResource, loader), message);
            String propMsg = UtilProperties.getMessage(propertyResource, message, locale);
            if (UtilValidate.isEmpty(propMsg)) {
                messages.add(new MessageString("Simple Map Processing error occurred, but no message was found, sorry.", fieldName,
                        propertyResource, message, locale, true));
            } else {
                messages.add(new MessageString(propMsg, fieldName, propertyResource, message, locale, true));
            }
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapOperation.addMessage] Adding property message: " + propMsg, MODULE);
        } else {
            messages.add(new MessageString("Simple Map Processing error occurred, but no message was found, sorry.", fieldName, true));
            // if (Debug.infoOn()) Debug.logInfo("[SimpleMapOperation.addMessage] ERROR: No message found", MODULE);
        }
    }

    public abstract void exec(Map<String, Object> inMap, Map<String, Object> results, List<Object> messages, Locale locale, ClassLoader loader);
}

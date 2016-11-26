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
package org.apache.ofbiz.minilang.method.callops;

import java.io.Serializable;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Simple class to wrap messages that come either from a straight string or a properties file
 */
@SuppressWarnings("serial")
public final class FlexibleMessage implements Serializable {

    private final FlexibleStringExpander messageFse;
    private final FlexibleStringExpander keyFse;
    private final String propertyResource;
    private String propertykey;

    public FlexibleMessage(Element element, String defaultProperty) {
        if (element != null) {
            String message = UtilXml.elementValue(element);
            if (message != null) {
                messageFse = FlexibleStringExpander.getInstance(message);
                keyFse = null;
                propertykey = null;
                propertyResource = null;
            } else {
                messageFse = null;
                propertykey = MiniLangValidate.checkAttribute(element.getAttribute("property"), defaultProperty);
                int exprStart = propertykey.indexOf(FlexibleStringExpander.openBracket);
                int exprEnd = propertykey.indexOf(FlexibleStringExpander.closeBracket, exprStart);
                if (exprStart > -1 && exprStart < exprEnd) {
                    keyFse = FlexibleStringExpander.getInstance(propertykey);
                } else {
                    keyFse = null;
                }
                propertyResource = MiniLangValidate.checkAttribute(element.getAttribute("resource"), "DefaultMessagesUiLabels");
            }
        } else {
            messageFse = null;
            keyFse = null;
            propertykey = defaultProperty;
            propertyResource = "DefaultMessagesUiLabels";
        }
    }

    public String getMessage(ClassLoader loader, MethodContext methodContext) {
        if (messageFse != null) {
            return messageFse.expandString(methodContext.getEnvMap());
        } else {
            if (keyFse != null) {
                propertykey = keyFse.expandString(methodContext.getEnvMap());
            }
            return UtilProperties.getMessage(propertyResource, propertykey, methodContext.getEnvMap(), methodContext.getLocale());
        }
    }
}

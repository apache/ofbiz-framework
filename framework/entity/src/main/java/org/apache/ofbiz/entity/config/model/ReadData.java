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
package org.apache.ofbiz.entity.config.model;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * An object that models the <code>&lt;read-data&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class ReadData extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "read-data";
    private final String xPath;
    public static final String ELEMENT_FIELD_ID_NAME = "reader-name";

    private final String readerName;

    ReadData(Element element, String xPathParent) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String readerName = element.getAttribute("reader-name");
        xPath = xPathParent.concat("/read-data[@reader-name='" + readerName + "']");
        if (readerName.isEmpty()) {
            throw new GenericEntityConfException("<read-data> element reader-name attribute is empty" + lineNumberText);
        }
        this.readerName = readerName;
    }

    ReadData(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        this.xPath = xPath;
        String readerName = config.getValue(configObject, "/@reader-name");
        if (readerName.isEmpty()) {
            throw new GenericEntityConfException("<read-data> element reader-name attribute is empty");
        }
        this.readerName = readerName;
    }

    public String getReaderName() {
        return readerName;
    }

    public static ReadData loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new ReadData(element, xPathParent);
    }

    public static ReadData loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new ReadData(configMap, xPath);
    }

    public boolean allowMultipleSources() {
        return false;
    }

    @Override
    public String getName() {
        return readerName;
    }

}

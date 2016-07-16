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
package org.apache.ofbiz.service.jms;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.serialize.SerializeException;
import org.apache.ofbiz.entity.serialize.XmlSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * A facade class used to connect JMS code to the legacy XML serialization code.
 *
 */
public class JmsSerializer {
    public static final String module = JmsSerializer.class.getName();

    public static Object deserialize(String content, Delegator delegator) throws SerializeException, SAXException, ParserConfigurationException, IOException {
        Document document = UtilXml.readXmlDocument(content, false);
        if (document != null) {
            return XmlSerializer.deserialize(document, delegator);
        } else {
            Debug.logWarning("Serialized document came back null", module);
            return null;
        }
    }

    public static String serialize(Object object) throws SerializeException, FileNotFoundException, IOException {
        Document document = UtilXml.makeEmptyXmlDocument("ofbiz-ser");
        Element rootElement = document.getDocumentElement();
        rootElement.appendChild(XmlSerializer.serializeSingle(object, document));
        return UtilXml.writeXmlDocument(document);
    }
}

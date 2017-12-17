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
package org.apache.ofbiz.entity.serialize;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilObject;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * XmlSerializer class. This class is deprecated - new code should use the
 * Java object marshalling/unmarshalling methods in <code>UtilXml.java</code>.
 *
 */
public class XmlSerializer {
    public static final String module = XmlSerializer.class.getName();

    private volatile static WeakReference<DateFormat> simpleDateFormatter;

    public static String serialize(Object object) throws SerializeException, FileNotFoundException, IOException {
        Document document = UtilXml.makeEmptyXmlDocument("ofbiz-ser");
        Element rootElement = document.getDocumentElement();

        rootElement.appendChild(serializeSingle(object, document));
        return UtilXml.writeXmlDocument(document);
    }

    /** Deserialize a Java object from an XML string. <p>This method should be used with caution.
     * If the XML string contains a serialized <code>GenericValue</code> or <code>GenericPK</code>
     * then it is possible to unintentionally corrupt the database.</p>
     *
     * @param content the content
     * @param delegator the delegator
     * @return return a deserialized object from XML string
     * @throws SerializeException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static Object deserialize(String content, Delegator delegator)
        throws SerializeException, SAXException, ParserConfigurationException, IOException {
        // readXmlDocument with false second parameter to disable validation
        Document document = UtilXml.readXmlDocument(content, false);
        if (document != null) {
            if (!"ofbiz-ser".equals(document.getDocumentElement().getTagName())) {
                return UtilXml.fromXml(content);
            }
            return deserialize(document, delegator);
        }
        Debug.logWarning("Serialized document came back null", module);
        return null;
    }

    /** Deserialize a Java object from a DOM <code>Document</code>.
     * <p>This method should be used with caution. If the DOM <code>Document</code>
     * contains a serialized <code>GenericValue</code> or <code>GenericPK</code>
     * then it is possible to unintentionally corrupt the database.</p>
     *
     * @param document the document
     * @param delegator the delegator
     * @return returns a deserialized object from a DOM document
     * @throws SerializeException
     */
    public static Object deserialize(Document document, Delegator delegator) throws SerializeException {
        Element rootElement = document.getDocumentElement();
        // find the first element below the root element, that should be the object
        Node curChild = rootElement.getFirstChild();
        while (curChild != null && curChild.getNodeType() != Node.ELEMENT_NODE) {
            curChild = curChild.getNextSibling();
        }
        if (curChild == null) {
            return null;
        }
        return deserializeSingle((Element) curChild, delegator);
    }

    public static Element serializeSingle(Object object, Document document) throws SerializeException {
        if (document == null) {
            return null;
        }

        if (object == null) {
            return makeElement("null", null, document);
        }

        // - Standard Objects -
        if (object instanceof String) {
            return makeElement("std-String", object, document);
        } else if (object instanceof Integer) {
            return makeElement("std-Integer", object, document);
        } else if (object instanceof Long) {
            return makeElement("std-Long", object, document);
        } else if (object instanceof Float) {
            return makeElement("std-Float", object, document);
        } else if (object instanceof Double) {
            return makeElement("std-Double", object, document);
        } else if (object instanceof Boolean) {
            return makeElement("std-Boolean", object, document);
        } else if (object instanceof Locale) {
            return makeElement("std-Locale", object, document);
        } else if (object instanceof BigDecimal) {
            String stringValue = ((BigDecimal) object).setScale(10, RoundingMode.HALF_UP).toString();
            return makeElement("std-BigDecimal", stringValue, document);
            // - SQL Objects -
        } else if (object instanceof java.sql.Timestamp) {
            String stringValue = object.toString().replace(' ', 'T');
            return makeElement("sql-Timestamp", stringValue, document);
        } else if (object instanceof java.sql.Date) {
            return makeElement("sql-Date", object, document);
        } else if (object instanceof java.sql.Time) {
            return makeElement("sql-Time", object, document);
        } else if (object instanceof java.util.Date) {
            // NOTE: make sure this is AFTER the java.sql date/time objects since they inherit from java.util.Date
            DateFormat formatter = getDateFormat();
            String stringValue = null;

            synchronized (formatter) {
                stringValue = formatter.format((java.util.Date) object);
            }
            return makeElement("std-Date", stringValue, document);
            // return makeElement("std-Date", object, document);
        } else if (object instanceof Collection<?>) {
            // - Collections -
            String elementName = null;

            // these ARE order sensitive; for instance Stack extends Vector, so if Vector were first we would lose the stack part
            if (object instanceof ArrayList<?>) {
                elementName = "col-ArrayList";
            } else if (object instanceof LinkedList<?>) {
                elementName = "col-LinkedList";
            } else if (object instanceof Stack<?>) {
                elementName = "col-Stack";
            } else if (object instanceof Vector<?>) {
                elementName = "col-Vector";
            } else if (object instanceof TreeSet<?>) {
                elementName = "col-TreeSet";
            } else if (object instanceof HashSet<?>) {
                elementName = "col-HashSet";
            } else {
                // no specific type found, do general Collection, will deserialize as LinkedList
                elementName = "col-Collection";
            }

            Collection<?> value = UtilGenerics.cast(object);
            Element element = document.createElement(elementName);
            Iterator<?> iter = value.iterator();

            while (iter.hasNext()) {
                element.appendChild(serializeSingle(iter.next(), document));
            }
            return element;
        } else if (object instanceof GenericPK) {
            // Do GenericEntity objects as a special case, use std XML import/export routines
            GenericPK value = (GenericPK) object;

            return value.makeXmlElement(document, "eepk-");
        } else if (object instanceof GenericValue) {
            GenericValue value = (GenericValue) object;

            return value.makeXmlElement(document, "eeval-");
        } else if (object instanceof Map<?, ?>) {
            // - Maps -
            String elementName = null;

            // these ARE order sensitive; for instance Properties extends Hashtable, so if Hashtable were first we would lose the Properties part
            if (object instanceof HashMap<?, ?>) {
                elementName = "map-HashMap";
            } else if (object instanceof Properties) {
                elementName = "map-Properties";
            } else if (object instanceof Hashtable<?, ?>) {
                elementName = "map-Hashtable";
            } else if (object instanceof WeakHashMap<?, ?>) {
                elementName = "map-WeakHashMap";
            } else if (object instanceof TreeMap<?, ?>) {
                elementName = "map-TreeMap";
            } else {
                // serialize as a simple Map implementation if nothing else applies, these will deserialize as a HashMap
                elementName = "map-Map";
            }

            Element element = document.createElement(elementName);
            Map<?,?> value = UtilGenerics.cast(object);
            Iterator<Map.Entry<?, ?>> iter = UtilGenerics.cast(value.entrySet().iterator());

            while (iter.hasNext()) {
                Map.Entry<?,?> entry = iter.next();

                Element entryElement = document.createElement("map-Entry");

                element.appendChild(entryElement);

                Element key = document.createElement("map-Key");

                entryElement.appendChild(key);
                key.appendChild(serializeSingle(entry.getKey(), document));
                Element mapValue = document.createElement("map-Value");

                entryElement.appendChild(mapValue);
                mapValue.appendChild(serializeSingle(entry.getValue(), document));
            }
            return element;
        }

        return serializeCustom(object, document);
    }

    public static Element serializeCustom(Object object, Document document) throws SerializeException {
        if (object instanceof Serializable) {
            byte[] objBytes = UtilObject.getBytes(object);
            if (objBytes == null) {
                throw new SerializeException("Unable to serialize object; null byte array returned");
            }
            String byteHex = StringUtil.toHexString(objBytes);
            Element element = document.createElement("cus-obj");
            // this is hex encoded so does not need to be in a CDATA block
            element.appendChild(document.createTextNode(byteHex));
            return element;
        }
        throw new SerializeException("Cannot serialize object of class " + object.getClass().getName());
    }

    public static Element makeElement(String elementName, Object value, Document document) {
        if (value == null) {
            Element element = document.createElement("null");
            element.setAttribute("xsi:nil", "true");
            // I tried to put the schema in the envelope header (in createAndSendSOAPResponse)
            // resEnv.declareNamespace("http://www.w3.org/2001/XMLSchema-instance", null);
            // But it gets prefixed and that does not work. So adding in each instance
            element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            return element;
        }
        Element element = document.createElement(elementName);

        element.setAttribute("value", value.toString());
        return element;
    }

    public static Object deserializeSingle(Element element, Delegator delegator) throws SerializeException {
        String tagName = element.getLocalName();

        if ("null".equals(tagName)) {
            return null;
        }

        if (tagName.startsWith("std-")) {
            // - Standard Objects -
            if ("std-String".equals(tagName)) {
                return element.getAttribute("value");
            } else if ("std-Integer".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return Integer.valueOf(valStr);
            } else if ("std-Long".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return Long.valueOf(valStr);
            } else if ("std-Float".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return Float.valueOf(valStr);
            } else if ("std-Double".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return Double.valueOf(valStr);
            } else if ("std-BigDecimal".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return new BigDecimal(valStr);
            } else if ("std-Boolean".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return Boolean.valueOf(valStr);
            } else if ("std-Locale".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return UtilMisc.parseLocale(valStr);
            } else if ("std-Date".equals(tagName)) {
                String valStr = element.getAttribute("value");
                DateFormat formatter = getDateFormat();
                java.util.Date value = null;

                try {
                    synchronized (formatter) {
                        value = formatter.parse(valStr);
                    }
                } catch (ParseException e) {
                    throw new SerializeException("Could not parse date String: " + valStr, e);
                }
                return value;
            }
        } else if (tagName.startsWith("sql-")) {
            // - SQL Objects -
            if ("sql-Timestamp".equals(tagName)) {
                String valStr = element.getAttribute("value");
                /*
                 * sql-Timestamp is defined as xsd:dateTime in ModelService.getTypes(),
                 * so try to parse the value as xsd:dateTime first.
                 * Fallback is java.sql.Timestamp because it has been this way all the time.
                 */
                try {
                    Calendar cal = DatatypeConverter.parseDate(valStr);
                    return new java.sql.Timestamp(cal.getTimeInMillis());
                }
                catch (Exception e) {
                    Debug.logWarning("sql-Timestamp does not conform to XML Schema definition, try java.sql.Timestamp format", module);
                    return java.sql.Timestamp.valueOf(valStr);
                }
            } else if ("sql-Date".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return java.sql.Date.valueOf(valStr);
            } else if ("sql-Time".equals(tagName)) {
                String valStr = element.getAttribute("value");
                return java.sql.Time.valueOf(valStr);
            }
        } else if (tagName.startsWith("col-")) {
            // - Collections -
            Collection<Object> value = null;

            if ("col-ArrayList".equals(tagName)) {
                value = new ArrayList<>();
            } else if ("col-LinkedList".equals(tagName)) {
                value = new LinkedList<>();
            } else if ("col-Stack".equals(tagName)) {
                value = new Stack<>();
            } else if ("col-Vector".equals(tagName)) {
                value = new Vector<>();
            } else if ("col-TreeSet".equals(tagName)) {
                value = new TreeSet<>();
            } else if ("col-HashSet".equals(tagName)) {
                value = new HashSet<>();
            } else if ("col-Collection".equals(tagName)) {
                value = new LinkedList<>();
            }

            if (value == null) {
                return deserializeCustom(element);
            }
            Node curChild = element.getFirstChild();

            while (curChild != null) {
                if (curChild.getNodeType() == Node.ELEMENT_NODE) {
                    value.add(deserializeSingle((Element) curChild, delegator));
                }
                curChild = curChild.getNextSibling();
            }
            return value;
        } else if (tagName.startsWith("map-")) {
            // - Maps -
            Map<Object, Object> value = null;

            if ("map-HashMap".equals(tagName)) {
                value = new HashMap<>();
            } else if ("map-Properties".equals(tagName)) {
                value = new Properties();
            } else if ("map-Hashtable".equals(tagName)) {
                value = new Hashtable<>();
            } else if ("map-WeakHashMap".equals(tagName)) {
                value = new WeakHashMap<>();
            } else if ("map-TreeMap".equals(tagName)) {
                value = new TreeMap<>();
            } else if ("map-Map".equals(tagName)) {
                value = new HashMap<>();
            }

            if (value == null) {
                return deserializeCustom(element);
            }
            Node curChild = element.getFirstChild();

            while (curChild != null) {
                if (curChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element curElement = (Element) curChild;

                    if ("map-Entry".equals(curElement.getLocalName())) {

                        Element mapKeyElement = UtilXml.firstChildElement(curElement, "map-Key");
                        Element keyElement = null;
                        Node tempNode = mapKeyElement.getFirstChild();

                        while (tempNode != null) {
                            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                                keyElement = (Element) tempNode;
                                break;
                            }
                            tempNode = tempNode.getNextSibling();
                        }
                        if (keyElement == null) {
                            throw new SerializeException("Could not find an element under the map-Key");
                        }

                        Element mapValueElement = UtilXml.firstChildElement(curElement, "map-Value");
                        Element valueElement = null;

                        tempNode = mapValueElement.getFirstChild();
                        while (tempNode != null) {
                            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                                valueElement = (Element) tempNode;
                                break;
                            }
                            tempNode = tempNode.getNextSibling();
                        }
                        if (valueElement == null) {
                            throw new SerializeException("Could not find an element under the map-Value");
                        }

                        value.put(deserializeSingle(keyElement, delegator), deserializeSingle(valueElement, delegator));
                    }
                }
                curChild = curChild.getNextSibling();
            }
            return value;
        } else if (tagName.startsWith("eepk-")) {
            return delegator.makePK(element);
        } else if (tagName.startsWith("eeval-")) {
            return delegator.makeValue(element);
        }

        return deserializeCustom(element);
    }

    public static Object deserializeCustom(Element element) throws SerializeException {
        String tagName = element.getLocalName();
        if ("cus-obj".equals(tagName)) {
            String value = UtilXml.elementValue(element);
            if (value != null) {
                byte[] valueBytes = StringUtil.fromHexString(value);
                if (valueBytes != null) {
                    Object obj = UtilObject.getObject(valueBytes);
                    if (obj != null) {
                        return obj;
                    }
                }
            }
            throw new SerializeException("Problem deserializing object from byte array + " + element.getLocalName());
        }
        throw new SerializeException("Cannot deserialize element named " + element.getLocalName());
    }

    /**
     * Returns the DateFormat used to serialize and deserialize <code>java.util.Date</code> objects.
     * This format is NOT used to format any of the java.sql subtypes of java.util.Date.
     * A <code>WeakReference</code> is used to maintain a reference to the DateFormat object
     * so that it can be created and garbage collected as needed.
     *
     * @return the DateFormat used to serialize and deserialize <code>java.util.Date</code> objects.
     */
    private static DateFormat getDateFormat() {
        DateFormat formatter = null;

        if (simpleDateFormatter != null) {
            formatter = simpleDateFormatter.get();
        }
        if (formatter == null) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            simpleDateFormatter = new WeakReference<>(formatter);
        }
        return formatter;
    }
}

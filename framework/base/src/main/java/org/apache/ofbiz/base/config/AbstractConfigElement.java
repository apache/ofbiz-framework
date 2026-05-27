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
package org.apache.ofbiz.base.config;

import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

import java.util.Map;


/**
 * Abstract class that must be implemented by every class wishing to use the centralized configuration reader tool.
 * Each class implementing this interface must :
 * <ul>
 *     <li>Possess a {@code public static final String ELEMENT_NAME} containing the name of the XML element to which
 *     the class refers to. Often the tag name. For example for the ConnectionFactory class, it will be "connection-factory"</li>
 *     <li>If needed a {@code public static final String ELEMENT_FIELD_ID_NAME} containing the attribut name of the XML element
 *         that identify as unique the element. Else the {@code name} string will be used</li>
 *     <li>Have a first constructor with an {@code Element} and a {@code String} as parameters. It will be used to
 *     construct the object from composite source.</li>
 *     <li>Have a second constructor with a {@code Map} and a {@code String} as parameters. It will be used to
 *     construct the object from overloaded configs.</li>
 *     <li>Implement {@link AbstractConfigElement#loadFromXml(Element, String)} by calling the constructor with {@link Element}</li>
 *     <li>Implement {@link AbstractConfigElement#loadFromConfig(Map, String)} by calling the constructor with {@link Map}</li>
 * </ul>
 */
public abstract class AbstractConfigElement {

    /**
     * Must invoke the constructor reading from an XML file
     *
     * @param element     the {@link Element} represented by the class
     * @param xPathParent the {@link javax.xml.xpath.XPath} of the parent element class invoking this method
     * @return an instance of the class from which it was called from
     * @throws GenericEntityConfException
     */
    static AbstractConfigElement loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return null;
    }

    /**
     * Invoke the constructor reading from a config overload file
     *
     * @param configMap a Map representing the element to create, likely coming from the Config object.
     * @param xPath     the {@link javax.xml.xpath.XPath} to this element in the configuration file
     * @return an instance of the class from which it was called from
     * @throws GenericEntityConfException
     */
    static AbstractConfigElement loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return null;
    }

    /**
     * Return the name of the element, if the class doesn't possess a name, a unique attribute not shared among other element will be returned
     *
     * @return a unique String to identify the element
     */
    public abstract String getName();

    /**
     * Retrieve the name of the last element in the xPath, this name can be :
     * <ul>
     *     <li>Either the last chain of characters at the end of the xPath (for example "foo/bar" will find bar)</li>
     *     <li>Or the unique identifier searched in the xPath (for example "foo/bar[@name='baz']" will return baz</li>
     * </ul>
     *
     * @param xPath the path from which to find the name of the last element
     * @return the name of the last element in the xPath
     */
    public static String getNameFromXPath(String xPath) {
        String lastMember = xPath.substring(xPath.lastIndexOf("/") + 1);
        int firstSimpleQuote = lastMember.indexOf('\'');
        int lastSimpleQuote = lastMember.lastIndexOf('\'');
        if (firstSimpleQuote != -1 && lastSimpleQuote != -1) {
            lastMember = lastMember.substring(firstSimpleQuote + 1, lastSimpleQuote);
        }
        return lastMember.intern();
    }

    /**
     * Override if the element is a list of elements like run-from-pool list.
     * This will make the config system ignore Xml implementations if config overloads are presents.
     *
     * @return if the config system allows multiple sources and should merge those sources
     */
    public boolean allowMultipleSources() {
        return true;
    }

}

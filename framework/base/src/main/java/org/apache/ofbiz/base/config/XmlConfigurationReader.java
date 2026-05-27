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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

public class XmlConfigurationReader implements ConfigurationReaderInterface {
    public static final String MODULE = XmlConfigurationReader.class.getName();
    private Element rootElement;
    private XPath xPath;

    /**
     * Create a XmlConfigurationReader object using {@code init(String resourceName})
     */
    public XmlConfigurationReader(String resourceName) {
        init(resourceName);
    }

    @Override
    public void init(String resourceName) {
        rootElement = XmlFileReader.read(resourceName);
        xPath = XPathFactory.newInstance().newXPath();
    }

    @Override
    public String getValue(String key) {
        String value = "";
        try {
            value = xPath.compile(key).evaluate(rootElement);
        } catch (XPathExpressionException e) {
            Debug.logError("Expression cannot be evaluated: " + e, MODULE);
        }
        return value.isEmpty() ? null : value.intern();
    }

    @Override
    public void clearCache() {
    }

    @Override
    public <T> T findSingleElementWithClassInParent(String xPath, Object parent, Class<T> targetClass) {
        T child = null;
        try {
            String childElementName = (String) targetClass.getDeclaredField("ELEMENT_NAME").get(null);
            Element childElement = UtilXml.firstChildElement((Element) parent, childElementName);
            if (childElement != null) {
                child = UtilGenerics.cast(targetClass.getMethod("loadFromXml", Element.class, String.class).invoke(null, childElement, xPath));
            }
        } catch (NoSuchFieldException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return child;
    }

    @Override
    public <T> List<T> findElementsWithTypeInParent(String xPath, Object parent, Class<T> targetClass) {
        if (parent == null) {
            return new LinkedList<>();
        }
        List<T> childModelObject = new ArrayList<>();
        try {
            String childElementName = (String) targetClass.getDeclaredField("ELEMENT_NAME").get(null);
            Method loadFromXml = targetClass.getMethod("loadFromXml", Element.class, String.class);
            for (Element childElement : UtilXml.childElementList((Element) parent, childElementName)) {
                childModelObject.add(UtilGenerics.cast(loadFromXml.invoke(null, childElement, xPath)));
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return childModelObject;
    }
}

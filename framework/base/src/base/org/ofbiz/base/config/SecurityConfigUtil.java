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
package org.ofbiz.base.config;

import java.util.*;
import org.w3c.dom.*;
import org.ofbiz.base.util.*;

/**
 * <code>SecurityConfigUtil</code>
 *
 * This class allows the loading of a security implementation by a security context name.
 * The security context name has to be specified in security.properties by the property name:
 * security.context=
 *
 * The setup of custom security implementations can be customized in the security.xml file.
 *
 */
public class SecurityConfigUtil {
    
    public static final String module = SecurityConfigUtil.class.getName();

    /** The security config filename */
    public static final String SECURITY_CONFIG_XML_FILENAME = "security.xml";

    protected static Map securityInfos = new HashMap();

    /**
     * Returns the XmlRootElement for the security config
     *
     * @return
     * @throws GenericConfigException
     */
    public static Element getXmlRootElement() throws GenericConfigException {
        return ResourceLoader.getXmlRootElement(SecurityConfigUtil.SECURITY_CONFIG_XML_FILENAME);
    }

    /**
     * Returns the XmlDocument for the security config
     *
     * @return
     * @throws GenericConfigException
     */
    public static Document getXmlDocument() throws GenericConfigException {
        return ResourceLoader.getXmlDocument(SecurityConfigUtil.SECURITY_CONFIG_XML_FILENAME);
    }

    static {
        try {
            initialize(getXmlRootElement());
        } catch (Exception e) {
            Debug.logError(e, "Error loading Security config XML file " + SECURITY_CONFIG_XML_FILENAME, module);
        }
    }

    /**
     * Initializes the security configuration
     *
     * @param rootElement
     * @throws GenericConfigException
     */
    public static void initialize(Element rootElement) throws GenericConfigException {
        List childElements = null;
        Iterator elementIter = null;

        // security-config - securityInfos
        childElements = UtilXml.childElementList(rootElement, "security");
        elementIter = childElements.iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            SecurityConfigUtil.SecurityInfo securityInfo = new SecurityConfigUtil.SecurityInfo(curElement);

            if (Debug.verboseOn()) Debug.logVerbose("LOADED SECURITY CONFIG FROM XML -  NAME: " + securityInfo.name + " ClassName: " + securityInfo.className, module);
            SecurityConfigUtil.securityInfos.put(securityInfo.name, securityInfo);
        }
    }

    /**
     * Returns the security config for a given name
     *
     * @param name
     * @return
     */
    public static SecurityConfigUtil.SecurityInfo getSecurityInfo(String name) {
        return (SecurityConfigUtil.SecurityInfo) securityInfos.get(name);
    }

    /**
     * <code>SecurityInfo</code>
     */
    public static class SecurityInfo {
        public String name;
        public String className;

        /**
         * Creates a SecurityInfo for a given element
         *
         * @param element
         */
        public SecurityInfo(Element element) {
            this.name = element.getAttribute("name");
            this.className = element.getAttribute("class");
        }
    }
}

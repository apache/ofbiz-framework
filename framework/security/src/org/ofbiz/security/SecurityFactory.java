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
package org.ofbiz.security;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>SecurityFactory</code>
 *
 * This Factory class returns an instance of a security implementation.
 *
 * Setting the security implementation className is done in security.xml.
 * If no customized security name is given, the default implementation will be used (OFBizSecurity)
 */
public final class SecurityFactory {

    public static final String module = SecurityFactory.class.getName();
    private static final String DEFAULT_SECURITY_CLASS_NAME = "org.ofbiz.security.OFBizSecurity";
    private static final String SECURITY_CONFIG_XML_FILENAME = "security.xml";
    private static final AtomicReference<SecurityInfo> configRef = new AtomicReference<SecurityInfo>(null);

    /**
     * Returns an instance of a Security implementation as defined in the security.xml by defined name
     * in security.properties.
     *
     * @param delegator the generic delegator
     * @return instance of security implementation (default: OFBizSecurity)
     */
    public static Security getInstance(Delegator delegator) throws SecurityConfigurationException {
        Security security = null;
        String securityClassName = DEFAULT_SECURITY_CLASS_NAME;
        try {
            SecurityInfo securityInfo = getSecurityInfo();
            securityClassName = securityInfo.className;
        } catch (SecurityConfigurationException e) {
            Debug.logError(e, "Exception thrown while getting security configuration, using default security class " + DEFAULT_SECURITY_CLASS_NAME, module);
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> c = loader.loadClass(securityClassName);
            security = (Security) c.newInstance();
            security.setDelegator(delegator);
        } catch (ClassNotFoundException cnf) {
            throw new SecurityConfigurationException("Cannot load security implementation class", cnf);
        } catch (InstantiationException ie) {
            throw new SecurityConfigurationException("Cannot get instance of the security implementation", ie);
        } catch (IllegalAccessException iae) {
            throw new SecurityConfigurationException(iae.getMessage(), iae);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Security implementation created for delegator " + delegator.getDelegatorName(), module);
        }
        return security;
    }

    private static SecurityInfo getSecurityInfo() throws SecurityConfigurationException {
        SecurityInfo instance = configRef.get();
        if (instance == null) {
            URL confUrl = UtilURL.fromResource(SECURITY_CONFIG_XML_FILENAME);
            if (confUrl == null) {
                throw new SecurityConfigurationException("Could not find the " + SECURITY_CONFIG_XML_FILENAME + " file");
            }
            String securityName = UtilProperties.getPropertyValue("security.properties", "security.context");
            if (Debug.verboseOn()) {
                Debug.logVerbose("Security implementation context name from security.properties: " + securityName, module);
            }
            Document document = null;
            try {
                document = UtilXml.readXmlDocument(confUrl, true, true);
            } catch (Exception e) {
                throw new SecurityConfigurationException("Exception thrown while reading " + SECURITY_CONFIG_XML_FILENAME + ": ", e);
            }
            Element securityElement = UtilXml.firstChildElement(document.getDocumentElement(), "security", "name", securityName);
            if (securityElement == null) {
                throw new SecurityConfigurationException("Could not find the <security> element in the " + SECURITY_CONFIG_XML_FILENAME + " file");
            }
            instance = new SecurityInfo(securityElement);
            if (configRef.compareAndSet(null, instance)) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Security configuration read from " + SECURITY_CONFIG_XML_FILENAME + ", using class " + instance.className, module);
                }
            } else {
                instance = configRef.get();
            }
        }
        return instance;
    }

    private static final class SecurityInfo {
        private final String name;
        private final String className;

        private SecurityInfo(Element element) throws SecurityConfigurationException {
            this.name = element.getAttribute("name");
            this.className = element.getAttribute("class");
            if (this.className.isEmpty()) {
                throw new SecurityConfigurationException("<security> element class attribute is empty in the " + SECURITY_CONFIG_XML_FILENAME + " file");
            }
        }
    }

    private SecurityFactory() {}
}

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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * JNDIConfigUtil
 *
 */
public final class JNDIConfigUtil {

    private static final String MODULE = JNDIConfigUtil.class.getName();
    private static final String JNDI_CONFIG_XML_FILENAME = "jndiservers.xml";
    private static final ConcurrentHashMap<String, JndiServerInfo> JNDI_SERVER_INFOS = new ConcurrentHashMap<>();
    private JNDIConfigUtil() { }

    private static Element getXmlRootElement() throws GenericConfigException {
        try {
            return ResourceLoader.readXmlRootElement(JNDIConfigUtil.JNDI_CONFIG_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericConfigException("Could not get JNDI XML root element", e);
        }
    }

    static {
        try {
            initialize(getXmlRootElement());
        } catch (Exception e) {
            Debug.logError(e, "Error loading JNDI config XML file " + JNDI_CONFIG_XML_FILENAME, MODULE);
        }
    }
    public static void initialize(Element rootElement) {
        // jndi-server - JNDI_SERVER_INFOS
        for (Element curElement: UtilXml.childElementList(rootElement, "jndi-server")) {
            JndiServerInfo jndiServerInfo = new JndiServerInfo(curElement);

            JNDI_SERVER_INFOS.putIfAbsent(jndiServerInfo.name, jndiServerInfo);
        }
    }

    public static JndiServerInfo getJndiServerInfo(String name) {
        return JNDI_SERVER_INFOS.get(name);
    }

    public static final class JndiServerInfo {
        private final String name;
        private final String contextProviderUrl;
        private final String initialContextFactory;
        private final String urlPkgPrefixes;
        private final String securityPrincipal;
        private final String securityCredentials;

        public String getContextProviderUrl() {
            return contextProviderUrl;
        }

        public String getInitialContextFactory() {
            return initialContextFactory;
        }

        public String getUrlPkgPrefixes() {
            return urlPkgPrefixes;
        }

        public String getSecurityPrincipal() {
            return securityPrincipal;
        }

        public String getSecurityCredentials() {
            return securityCredentials;
        }

        public JndiServerInfo(Element element) {
            this.name = element.getAttribute("name");
            this.contextProviderUrl = element.getAttribute("context-provider-url");
            this.initialContextFactory = element.getAttribute("initial-context-factory");
            this.urlPkgPrefixes = element.getAttribute("url-pkg-prefixes");
            this.securityPrincipal = element.getAttribute("security-principal");
            this.securityCredentials = element.getAttribute("security-credentials");
        }
    }
}

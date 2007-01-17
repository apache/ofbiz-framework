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

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.SecurityConfigUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.w3c.dom.Element;

/**
 * <code>SecurityFactory</code>
 *
 * This Factory class returns an instance of a security implementation.
 *
 * Setting the security implementation className is done in security.xml.
 * If no customiz security name is given, the default implementation will be used (OFBizSecurity)
 */
public class SecurityFactory {
    
    public static final String module = SecurityFactory.class.getName();
    public static final String DEFAULT_SECURITY = "org.ofbiz.security.OFBizSecurity";
    
    private static String securityName = null;
    private static Element rootElement = null;
    private static SecurityConfigUtil.SecurityInfo securityInfo = null;

    /**
     * Returns an instance of a Security implementation as defined in the security.xml by defined name
     * in security.properties.
     *
     * @param delegator the generic delegator
     * @return instance of security implementation (default: OFBizSecurity)
     */
    public static Security getInstance(GenericDelegator delegator) throws SecurityConfigurationException {
        Security security = null;

        // Make securityName a singleton
        if (securityName == null) {
            String _securityName = UtilProperties.getPropertyValue("security.properties", "security.context");
            securityName = _securityName;
        }

        if (Debug.verboseOn()) Debug.logVerbose("[SecurityFactory.getInstance] Security implementation context name from security.properties: " + securityName, module);

        synchronized (SecurityFactory.class) {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class c = loader.loadClass(getSecurityClass(securityName));
                security = (Security) c.newInstance();
                security.setDelegator(delegator);
            } catch (ClassNotFoundException cnf) {
                throw new SecurityConfigurationException("Cannot load security implementation class", cnf);
            } catch (InstantiationException ie) {
                throw new SecurityConfigurationException("Cannot get instance of the security implementation", ie);
            } catch (IllegalAccessException iae) {
                throw new SecurityConfigurationException(iae.getMessage(), iae);
            }
        }

        if (Debug.verboseOn()) Debug.logVerbose("[SecurityFactory.getInstance] Security implementation successfully loaded!!!", module);

        return security;
    }

    /**
     * Returns the class name of  a custom Security implementation.
     * The default class name (org.ofbiz.security.OFBizSecurity) may be overridden by a customized implementation
     * class name in security.xml.
     *
     * @param securityName the security context name to be looked up
     * @return className the class name of the security implementatin
     * @throws SecurityConfigurationException
     */
    private static String getSecurityClass(String securityName) throws SecurityConfigurationException {
        String className = null;

        if (Debug.verboseOn())
            Debug.logVerbose("[SecurityFactory.getSecurityClass] Security implementation context name: " + securityName, module);

        // Only load rootElement again, if not yet loaded (singleton)
        if (rootElement == null) {
            try {
                SecurityConfigUtil.getXmlDocument();
                Element _rootElement = SecurityConfigUtil.getXmlRootElement();

                rootElement = _rootElement;
            } catch (GenericConfigException e) {
                Debug.logError(e, "Error getting Security Config XML root element", module);
                return null;
            }
        }

        if (securityInfo == null) {
            SecurityConfigUtil.SecurityInfo _securityInfo = SecurityConfigUtil.getSecurityInfo(securityName);

            // Make sure, that the security conetxt name is defined and present
            if (_securityInfo == null) {
                throw new SecurityConfigurationException("ERROR: no security definition was found with the name " + securityName + " in security.xml");
            }
            securityInfo = _securityInfo;
        }

        // This is the default implementation and uses org.ofbiz.security.OFBizSecurity
        if (UtilValidate.isEmpty(securityInfo.className)) {
            className = DEFAULT_SECURITY;
        } else {
            // Use a customized security
            className = securityInfo.className;
        }

        if (Debug.verboseOn()) Debug.logVerbose("[SecurityFactory.getSecurity] Security implementation " + className + " for security name " + securityName + " successfully loaded!!!", module);
        return className;
    }
}

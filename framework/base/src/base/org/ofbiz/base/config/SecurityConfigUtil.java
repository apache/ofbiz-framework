/*
 * $Id: SecurityConfigUtil.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2002 The Open For Business Project and repected authors.
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
 * @author     <a href="mailto:hermanns@aixcept.de">Rainer Hermanns</a>
 * @version    $Rev$
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

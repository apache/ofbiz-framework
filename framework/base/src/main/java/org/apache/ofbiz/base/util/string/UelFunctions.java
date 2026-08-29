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
package org.apache.ofbiz.base.util.string;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import jakarta.el.FunctionMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import static org.apache.ofbiz.base.component.ComponentConfig.getAllUelMappingInfo;

/**
 * Implements Unified Expression Language functions.
 * <p>The uel functions mappings are defined in the <code>ofbiz-component</code> files. An example can
 * be found in the base component of the framework.</p>
 */
public class UelFunctions {

    protected static final Functions FUNCTION_MAPPER = new Functions();
    public static final String MODULE = UelFunctions.class.getName();

    public UelFunctions() { }

    /**
     * Returns a <code>FunctionMapper</code> instance.
     * @return <code>FunctionMapper</code> instance
     */
    public static Functions getFunctionMapper() {
        return FUNCTION_MAPPER;
    }

    public static String dateString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toDateFormat(UtilDateTime.getDateFormat(), timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static String localizedDateString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toDateFormat(null, timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static String dateTimeString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toDateTimeFormat("yyyy-MM-dd HH:mm", timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static String localizedDateTimeString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toDateTimeFormat(null, timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static String timeString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toTimeFormat(UtilDateTime.getTimeFormat(), timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static int getSize(Object obj) {
        if (null == obj) return 0;
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).size();
        }
        if (obj instanceof String) {
            return ((String) obj).length();
        }
        return -1;
    }

    public static boolean endsWith(String str1, String str2) {
        if (null == str1) return false;
        return str1.endsWith(str2);
    }

    public static int indexOf(String str1, String str2) {
        if (null == str1) return -1;
        return str1.indexOf(str2);
    }

    public static int lastIndexOf(String str1, String str2) {
        if (null == str1) return -1;
        return str1.lastIndexOf(str2);
    }

    public static int length(String str1) {
        if (null == str1) return 0;
        return str1.length();
    }

    public static String replace(String str1, String str2, String str3) {
        if (null == str1) return null;
        return str1.replace(str2, str3);
    }

    public static String replaceAll(String str1, String str2, String str3) {
        if (null == str1) return null;
        return str1.replaceAll(str2, str3);
    }

    public static String replaceFirst(String str1, String str2, String str3) {
        if (null == str1) return null;
        int idx = str1.indexOf(str2);
        return idx < 0 ? str1 : str1.substring(0, idx) + str3 + str1.substring(idx + str2.length());
    }

    public static boolean startsWith(String str1, String str2) {
        if (null == str1) return false;
        return str1.startsWith(str2);
    }

    public static String endString(String str, int index) {
        if (null == str) return null;
        return str.substring(index);
    }

    public static String subString(String str, int beginIndex, int endIndex) {
        if (null == str) return null;
        return str.substring(beginIndex, endIndex);
    }

    public static String trim(String str) {
        if (null == str) return null;
        return str.trim();
    }

    public static String toLowerCase(String str) {
        if (null == str) return null;
        return str.toLowerCase(Locale.getDefault());
    }

    public static String toUpperCase(String str) {
        if (null == str) return null;
        return str.toUpperCase(Locale.getDefault());
    }

    public static String toString(Object obj) {
        if (null == obj) return null;
        return obj.toString();
    }

    public static String sysGetEnv(String str) {
        if (null == str) return null;
        return System.getenv(str);
    }

    public static String sysGetProp(String str) {
        if (null == str) return null;
        return System.getProperty(str);
    }

    public static String label(String ressource, String label, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String resolveLabel = UtilProperties.getMessage(ressource, label, locale);
        if (resolveLabel != null) {
            return resolveLabel;
        }
        return label;
    }

    /**
     * Returns the id of the current screen identified on the screen stack
     * @param screenStack
     * @return
     */
    public static String resolveCurrentScreenId(ScreenRenderer.ScreenStack screenStack) {
        if (screenStack != null) {
            return screenStack.resolveCurrentScreenId();
        }
        return null;
    }

    public static Document readHtmlDocument(String str) {
        Document document = null;
        try {
            URL url = FlexibleLocation.resolveLocation(str);
            if (url != null) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                factory.setXIncludeAware(false);
                factory.setExpandEntityReferences(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(url.openStream());
                document.getDocumentElement().normalize();
            } else {
                Debug.logError("Unable to locate HTML document " + str, MODULE);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            Debug.logError(e, "Error while reading HTML document " + str, MODULE);
        }
        return document;
    }

    public static Document readXmlDocument(String str) {
        Document document = null;
        try {
            URL url = FlexibleLocation.resolveLocation(str);
            if (url != null) {
                try (InputStream is = url.openStream();) {
                    document = UtilXml.readXmlDocument(is, str);
                } catch (SAXException | ParserConfigurationException e) {
                    Debug.logError(e, "Error while reading XML document " + str, MODULE);
                }
            } else {
                Debug.logError("Unable to locate XML document " + str, MODULE);
            }
        } catch (IOException e) {
            Debug.logError(e, "Error while reading XML document " + str, MODULE);
        }
        return document;
    }

    public static boolean writeXmlDocument(String str, Node node, String encoding, boolean omitXmlDeclaration, boolean indent, int indentAmount) {
        try {
            File file = FileUtil.getFile(str);
            if (file != null) {
                try (FileOutputStream os = new FileOutputStream(file);) {
                    UtilXml.writeXmlDocument(node, os, encoding, omitXmlDeclaration, indent, indentAmount);
                    return true;
                }
            } else {
                Debug.logError("Unable to create XML document " + str, MODULE);
            }
        } catch (IOException | TransformerException | SecurityException e) {
            Debug.logError(e, "Error while writing XML document " + str, MODULE);
        }
        return false;
    }

    public static String toHtmlString(Node node, String encoding, boolean indent, int indentAmount) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xalan=\"http://xml.apache.org/xslt\" version=\"1"
                    + ".0\">\n");
            sb.append("<xsl:output method=\"html\" encoding=\"");
            sb.append(encoding == null ? "UTF-8" : encoding);
            sb.append("\"");
            sb.append(" indent=\"");
            sb.append(indent ? "yes" : "no");
            sb.append("\"");
            if (indent) {
                sb.append(" xalan:indent-amount=\"");
                sb.append(indentAmount <= 0 ? 4 : indentAmount);
                sb.append("\"");
            }
            sb.append("/>\n<xsl:template match=\"@*|node()\">\n");
            sb.append("<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>\n");
            sb.append("</xsl:template>\n</xsl:stylesheet>\n");
            ByteArrayInputStream bis = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
                UtilXml.transformDomDocument(transformerFactory.newTransformer(new StreamSource(bis)), node, os);
                return os.toString();
            }
        } catch (IOException | TransformerException e) {
            Debug.logError(e, "Error while creating HTML String ", MODULE);
        }
        return null;
    }

    public static String toXmlString(Node node, String encoding, boolean omitXmlDeclaration, boolean indent, int indentAmount) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            UtilXml.writeXmlDocument(node, os, encoding, omitXmlDeclaration, indent, indentAmount);
            return os.toString();
        } catch (IOException | TransformerException e) {
            Debug.logError(e, "Error while creating XML String ", MODULE);
        }
        return null;
    }

    protected static final class Functions extends FunctionMapper {
        private Set<UelMapping> functionList = new HashSet<>();

        public Functions() {
            this.functionList = loadUelFromComponents();
            if (Debug.verboseOn()) {
                Debug.logVerbose("UelFunctions.Functions loaded " + this.functionList.size() + " functions", MODULE);
            }
        }
        /** resolve function */
        @Override
        public Method resolveFunction(String prefix, String localName) {
            Optional<UelMapping> uelCandidate = functionList.stream()
                    .filter(uelMapping -> Objects.equals(uelMapping.getKey(), prefix + ":" + localName))
                    .findFirst();
            return uelCandidate.map(UelMapping::getMethod).orElse(null);
        }

        private Set<UelMapping> loadUelFromComponents() {
            List<ComponentConfig.UelMappingInfo> uelsMappingInfo = getAllUelMappingInfo();
            Set<UelMapping> uelMappings = new HashSet<>();

            uelsMappingInfo.forEach(uelMappingInfo -> {
                String className = uelMappingInfo.getClassName();
                IUelMappingLibrary mapping = null;
                Class<?> lClass;
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                try {
                    lClass = classLoader.loadClass(className);
                    mapping = (IUelMappingLibrary) lClass.getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException | InvocationTargetException | InstantiationException
                         | IllegalAccessException | NoSuchMethodException e) {
                    Debug.logError(e, "Error while initializing UelFunctions.Functions instance", MODULE);
                }
                if (mapping == null) {
                    return;
                }
                uelMappings.addAll(mapping.getUelMappingList());
            });
            return uelMappings;
        }

        public Set<UelMapping> getUelFunctions() {
            return this.functionList;
        }
    }

}

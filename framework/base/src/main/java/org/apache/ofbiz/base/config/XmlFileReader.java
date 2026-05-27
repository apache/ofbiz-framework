package org.apache.ofbiz.base.config;

import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilXml;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

/**
 * Small utility class for wrapping reading Xml files.
 * Mainly for testing purposes (mockito)
 */
public class XmlFileReader {
    public static final String MODULE = XmlFileReader.class.getName();

    public static Element read(String resourceName) {
        URL confUrl = UtilURL.fromResource(resourceName);
        if (confUrl == null) {
            Debug.logError("Could not find the " + resourceName + " file", MODULE);
            throw new RuntimeException("Could not find the " + resourceName + " file");
        }
        try {
            return UtilXml.readXmlDocument(confUrl, true, true).getDocumentElement();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            Debug.logError("Exception thrown while reading " + resourceName + ": " + e, MODULE);
            throw new RuntimeException(e);
        }
    }

}

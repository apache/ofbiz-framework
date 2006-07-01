/*
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.webapp.barcode;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.krysalis.barcode4j.BarcodeException;
import org.krysalis.barcode4j.BarcodeUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.DocumentFragment;

/**
 * Wrapper around Krysalis Barcode for generating barcodes.
 *
 * @author Bryce Ewing
 * @version 0.1
 */
public class BarcodeGenerator {

    public static final String module = BarcodeGenerator.class.getName();

    private Configuration config;
    // removing this because the new version of Barcode4J doesn't have a parameter for it: private Logger log;
    private BarcodeUtil barcodeUtil;

    public BarcodeGenerator(String barcodeFormat) {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        try {
            byte [] configData = barcodeFormat.getBytes();
            config = builder.build(new ByteArrayInputStream(configData));
        }
        catch (Exception e) {
            Debug.logError(e, "Couldn't create config for Barcode Generator", module);
        }

        //log = new Log4JLogger(Debug.getLogger(module));

        barcodeUtil = BarcodeUtil.getInstance();
    }

    public String generateSvgXml(String message) throws BarcodeException, ConfigurationException {
        if (config != null) {
            DocumentFragment fragment = barcodeUtil.generateSVGBarcode(config, message);
            try {
                String barcodeXml = UtilXml.writeXmlDocument(fragment);
                return barcodeXml;
            } catch (IOException e) {
                Debug.logError(e, "Error turning the SVG barcode fragment into a String", module);
                return "";
            }
        } else {
            return "";
        }
    }
}

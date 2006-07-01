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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;

/**
 * Factory for the creation of barcode generators.  The barcode generator is based
 * on the barcode type given.  This selects which barcode specification file to use.
 * The barcode specification file is found in the barcode directory on the classpath.
 * The mapping between barcodeType and the specification file is, e.g:
 *   invoiceShippingId -> barcode/invoiceShippingId.xml
 *
 * The barcode specification file must be formatted based on the formats given at:
 *   http://krysalis.org/barcode/barcode-xml.html
 *
 * @author Bryce Ewing
 * @version 0.1
 */
public class BarcodeFactory {

    public static final String module = BarcodeFactory.class.getName();

    private static Map barcodeGeneratorMap = new HashMap();

    /**
     * Get a barcode generator for the given barcode type.
     *
     * @param barcodeType the barcode type of the barcode generator.
     * @return the given barcode generator.
     */
    public static BarcodeGenerator getBarcodeGenerator(String barcodeType) {
        synchronized (barcodeGeneratorMap) {
            if (barcodeGeneratorMap.containsKey(barcodeType)) {
                return (BarcodeGenerator) barcodeGeneratorMap.get(barcodeType);
            }
            else {
                String specificationFile = "barcode/" + barcodeType + ".xml";

                URL specificationUrl = UtilURL.fromResource(specificationFile);
                if (specificationUrl == null) {
                    Debug.logError("Problem getting the specification URL: " + specificationFile + " not found", module);
                    return null;
                }

                StringBuffer format = new StringBuffer();
                BufferedReader specificationReader = null;
                try {
                    specificationReader = new BufferedReader(new InputStreamReader(specificationUrl.openStream()));
                    String line = null;
                    while ((line = specificationReader.readLine()) != null) {
                        format.append(line);
                    }
                }
                catch (java.io.IOException e) {
                    Debug.logError(e, "Error reading the specification URL: " + specificationFile, module);
                    return null;
                }
                finally {
                    if (specificationReader != null) {
                        try {
                            specificationReader.close();
                        }
                        catch (IOException e) {
                            Debug.logError(e, "Error closing the specification URL: " + specificationFile, module);
                            return null;
                        }
                    }
                }

                BarcodeGenerator generator = new BarcodeGenerator(format.toString());
                barcodeGeneratorMap.put(barcodeType, generator);
                return generator;
            }
        }
    }

}

/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.webapp.view;

import java.io.ByteArrayOutputStream;
import java.io.Writer;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.fop.messaging.MessageHandler;
import org.apache.fop.apps.Driver;
import org.apache.fop.tools.DocumentInputSource;
import org.apache.fop.image.FopImageFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.GeneralException;

/**
 * FopRenderer
 */
public class FopRenderer {

    public static final String module = FopRenderer.class.getName();

    public static ByteArrayOutputStream render(Writer writer) throws GeneralException {
        // configure logging for the FOP
        Logger logger = new Log4JLogger(Debug.getLogger(module));
        MessageHandler.setScreenLogger(logger);

        // load the FOP driver
        Driver driver = new Driver();
        driver.setRenderer(Driver.RENDER_PDF);
        driver.setLogger(logger);

        /*
        try {
            String buf = writer.toString();
            java.io.FileWriter fw = new java.io.FileWriter(new java.io.File("/tmp/xslfo.out"));
            fw.write(buf.toString());
            fw.close();
        } catch (IOException e) {
            throw new GeneralException("Unable write to browser OutputStream", e);
        }
        */

        // read the XSL-FO XML Document
        Document xslfo = null;
        try {
            xslfo = UtilXml.readXmlDocument(writer.toString());
        } catch (Throwable t) {
            throw new GeneralException("Problems reading the parsed content to XML Document", t);
        }

        // create the output stream for the PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        driver.setOutputStream(out);

        // set the input source (XSL-FO) and generate the PDF
        InputSource is = new DocumentInputSource(xslfo);
        driver.setInputSource(is);
        try {
            driver.run();
            FopImageFactory.resetCache();
        } catch (Throwable t) {
            throw new GeneralException("Unable to generate PDF from XSL-FO", t);
        }

        return out;
    }

}

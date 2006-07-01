/*
 * $Id: FopRenderer.java 6535 2006-01-22 03:53:59Z jaz $
 *
 * Copyright (c) 2001-2006 The Open For Business Project - www.ofbiz.org
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.5
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

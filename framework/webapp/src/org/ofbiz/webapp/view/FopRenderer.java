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
package org.ofbiz.webapp.view;

import java.io.*;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FOPException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.Result;

/**
 * FopRenderer
 */
public class FopRenderer {

    public static final String module = FopRenderer.class.getName();

    /**
     * Renders a PDF document from a FO script that is passed in and returns the content as a ByteArrayOutputStream
     * @param writer    a Writer stream that supplies the FO text to be rendered
     * @return  ByteArrayOutputStream containing the binary representation of a PDF document
     * @throws GeneralException
     */
    public static ByteArrayOutputStream render(Writer writer) throws GeneralException {

        FopFactory fopFactory = ApacheFopFactory.instance();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        TransformerFactory transFactory = TransformerFactory.newInstance();

        try {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);
            Transformer transformer = transFactory.newTransformer();

            // set the input source (XSL-FO) and generate the PDF
            Reader reader = new StringReader(writer.toString());
            Source src = new StreamSource(reader);

            // Get handler that is used in the generation process
            Result res = new SAXResult(fop.getDefaultHandler());

            try {
                // Transform the FOP XML source into a PDF, hopefully...
                transformer.transform(src, res);

                // We don't want to cache the images that get loaded by the FOP engine
                fopFactory.getImageFactory().clearCaches();

                return out;

            } catch (TransformerException e) {
                Debug.logError("FOP transform failed:" + e, module );
                throw new GeneralException("Unable to transform FO to PDF", e);
            }

        } catch (TransformerConfigurationException e) {
            Debug.logError("FOP TransformerConfiguration Exception " + e, module);
            throw new GeneralException("Transformer Configuration Error", e);
        } catch (FOPException e) {
            Debug.logError("FOP Exception " + e, module);
            throw new GeneralException("FOP Error", e);
        }
    }
}

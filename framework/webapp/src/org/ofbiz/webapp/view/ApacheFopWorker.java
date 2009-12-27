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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

/**
 * Apache FOP worker class.
 */

public class ApacheFopWorker {

    public static final String module = ApacheFopWorker.class.getName();
    /** File name prefix used for temporary files. Currently set to
     * <code>org.ofbiz.webapp.view.ApacheFopWorker-</code>.
     */
    public static final String tempFilePrefix = "org.ofbiz.webapp.view.ApacheFopWorker-";

    protected static FopFactory fopFactory = null;

    /** Returns an instance of the FopFactory class. FOP documentation recommends
     * the reuse of the factory instance because of the startup time.
     * @return FopFactory The FopFactory instance
     */
    public static FopFactory getFactoryInstance() {
        if (fopFactory == null) {
            synchronized (ApacheFopWorker.class) {
                if (fopFactory != null) {
                    return fopFactory;
                }
                // Create the factory
                fopFactory = FopFactory.newInstance();

                // Limit the validation for backwards compatibility
                fopFactory.setStrictValidation(false);

                try {
                    String fopPath = UtilProperties.getPropertyValue("fop.properties", "fop.path", "framework/webapp/config");
                    File userConfigFile = FileUtil.getFile(fopPath + "/fop.xconf");
                    fopFactory.setUserConfig(userConfigFile);
                    String fopFontBaseUrl = fopFactory.getFontBaseURL();
                    if (fopFontBaseUrl == null) {
                        String ofbizHome = System.getProperty("ofbiz.home");
                        fopFontBaseUrl = UtilProperties.getPropertyValue("fop.properties", "fop.font.base.url", "file:///" + ofbizHome + "/framework/webapp/config/");
                        fopFactory.setFontBaseURL(fopFontBaseUrl);
                    }
                    Debug.logInfo("FOP-FontBaseURL: " + fopFontBaseUrl, module);
                } catch (Exception e) {
                    Debug.logWarning("Error reading FOP configuration", module);
                }
            }
        }
        return fopFactory;
    }

    /** Transform an xsl-fo file to the specified file format.
     * @param srcFile The xsl-fo File instance
     * @param destFile The target (result) File instance
     * @param stylesheetFile Optional stylesheet File instance
     * @param outputFormat Optional output format, defaults to "application/pdf"
     */
    public static void transform(File srcFile, File destFile, File stylesheetFile, String outputFormat) throws IOException, FOPException {
        StreamSource src = new StreamSource(srcFile);
        StreamSource stylesheet = stylesheetFile == null ? null : new StreamSource(stylesheetFile);
        BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile));
        Fop fop = createFopInstance(dest, outputFormat);
        if (fop.getUserAgent().getBaseURL() == null) {
            String baseURL = null;
            try {
                File parentFile = new File(srcFile.getAbsolutePath()).getParentFile();
                baseURL = parentFile.toURI().toURL().toExternalForm();
            } catch (Exception e) {
                baseURL = "";
            }
            fop.getUserAgent().setBaseURL(baseURL);
        }
        transform(src, stylesheet, fop);
        dest.close();
    }

    /** Transform an xsl-fo InputStream to the specified OutputStream format.
     * @param srcStream The xsl-fo InputStream instance
     * @param destStream The target (result) OutputStream instance
     * @param stylesheetStream Optional stylesheet InputStream instance
     * @param outputFormat Optional output format, defaults to "application/pdf"
     */
    public static void transform(InputStream srcStream, OutputStream destStream, InputStream stylesheetStream, String outputFormat) throws FOPException {
        StreamSource src = new StreamSource(srcStream);
        StreamSource stylesheet = stylesheetStream == null ? null : new StreamSource(stylesheetStream);
        Fop fop = createFopInstance(destStream, outputFormat);
        transform(src, stylesheet, fop);
    }

    /** Transform an xsl-fo StreamSource to the specified output format.
     * @param src The xsl-fo StreamSource instance
     * @param stylesheet Optional stylesheet StreamSource instance
     * @param fop
     */
    public static void transform(StreamSource src, StreamSource stylesheet, Fop fop) throws FOPException {
        Result res = new SAXResult(fop.getDefaultHandler());
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;
            if (stylesheet == null) {
                transformer = factory.newTransformer();
            } else {
                transformer = factory.newTransformer(stylesheet);
            }
            transformer.setURIResolver(new LocalResolver(transformer.getURIResolver()));
            transformer.transform(src, res);
        } catch (Exception e) {
            throw new FOPException(e);
        }
    }

    /** Returns a new Fop instance. Note: FOP documentation recommends using
     * a Fop instance for one transform run only.
     * @param out The target (result) OutputStream instance
     * @param outputFormat Optional output format, defaults to "application/pdf"
     * @return Fop instance
     */
    public static Fop createFopInstance(OutputStream out, String outputFormat) throws FOPException {
        if (UtilValidate.isEmpty(outputFormat)) {
            outputFormat = MimeConstants.MIME_PDF;
        }
        FopFactory fopFactory = getFactoryInstance();
        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        Fop fop;
        if (out != null) {
            fop = fopFactory.newFop(outputFormat, foUserAgent, out);
        } else {
            fop = fopFactory.newFop(outputFormat, foUserAgent);
        }
        return fop;
    }

    /** Returns a temporary File instance. The temporary file name starts with
     * <a href="#tempFilePrefix">tempFilePrefix</a> and ends with ".xml".
     * Calling methods are responsible for deleting the temporary file.<p>
     * FOP performs transforms in memory, so if there is any chance FO output
     * will be more than a few pages, it would be best to keep FO input in a temporary
     * file.</p>
     * @return File instance
     */
    public static File createTempFoXmlFile() throws IOException {
        File tempXmlFile = File.createTempFile(tempFilePrefix, ".xml");
        tempXmlFile.deleteOnExit();
        return tempXmlFile;
    }

    /** Returns a temporary File instance. The temporary file name starts with
     * <a href="#tempFilePrefix">tempFilePrefix</a> and ends with ".res".
     * Calling methods are responsible for deleting the temporary file.<p>
     * FOP performs transforms in memory, so if there is any chance FO output
     * will be more than a few pages, it would be best to keep FO output in a temporary
     * file.</p>
     * @return File instance
     */
    public static File createTempResultFile() throws IOException {
        File tempResultFile = File.createTempFile(tempFilePrefix, ".res");
        tempResultFile.deleteOnExit();
        return tempResultFile;
    }

    /** Local URI resolver for the Transformer class.
     */
    public static class LocalResolver implements URIResolver {

        private URIResolver defaultResolver;

        protected LocalResolver() {}

        public LocalResolver(URIResolver defaultResolver) {
            this.defaultResolver = defaultResolver;
        }

        public Source resolve(String href, String base) throws TransformerException {
            URL locationUrl = null;
            try {
                locationUrl = FlexibleLocation.resolveLocation(href);
                if (locationUrl != null) {
                    return new StreamSource(locationUrl.openStream());
                }
            } catch (Exception e) {
                throw new TransformerException(e.getMessage());
            }
            return defaultResolver.resolve(href, base);
        }
    }
}

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
package org.apache.ofbiz.webapp.view;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.io.ResourceResolverFactory;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Apache FOP worker class.
 */

public final class ApacheFopWorker {

    public static final String module = ApacheFopWorker.class.getName();
    /** File name prefix used for temporary files. Currently set to
     * <code>org.apache.ofbiz.webapp.view.ApacheFopWorker-</code>.
     */
    private static final String tempFilePrefix = "org.apache.ofbiz.webapp.view.ApacheFopWorker-";

    private static FopFactory fopFactory = null;
    
    private static final int encryptionLengthBitsDefault = 128;
    
    private static final String encryptionLengthDefault = UtilProperties.getPropertyValue("fop", "fop.encryption-length.default", String.valueOf(encryptionLengthBitsDefault));

    private static final String userPasswordDefault = UtilProperties.getPropertyValue("fop", "fop.userPassword.default");

    private static final String ownerPasswordDefault = UtilProperties.getPropertyValue("fop", "fop.ownerPassword.default");

    private static final String allowPrintDefault = UtilProperties.getPropertyValue("fop", "fop.allowPrint.default", "true");

    private static final String allowCopyContentDefault = UtilProperties.getPropertyValue("fop", "fop.allowCopyContent.default", "true");

    private static final String allowEditContentDefault = UtilProperties.getPropertyValue("fop", "fop.allowEditContent.default", "true");

    private static final String allowEditAnnotationsDefault = UtilProperties.getPropertyValue("fop", "fop.allowEditAnnotations.default", "true");

    private static final String allowFillInFormsDefault = UtilProperties.getPropertyValue("fop", "fop.allowFillInForms.default", "true");

    private static final String allowAccessContentDefault = UtilProperties.getPropertyValue("fop", "fop.allowAccessContent.default", "true");

    private static final String allowAssembleDocumentDefault = UtilProperties.getPropertyValue("fop", "fop.allowAssembleDocument.default", "true");

    private static final String allowPrintHqDefault = UtilProperties.getPropertyValue("fop", "fop.allowPrintHq.default", "true");

    private static final String encryptMetadataDefault = UtilProperties.getPropertyValue("fop", "fop.encrypt-metadata.default", "true");
    
    private static final String fopPath = UtilProperties.getPropertyValue("fop", "fop.path", "/framework/webapp/config");
    
    private static final String fopFontBaseProperty = UtilProperties.getPropertyValue("fop", "fop.font.base.url", "/framework/webapp/config/");

    private ApacheFopWorker() {}

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

                try {
                    URL configFilePath = FlexibleLocation.resolveLocation(fopPath + "/fop.xconf");
                    File userConfigFile = FileUtil.getFile(configFilePath.getFile());
                    if (userConfigFile.exists()) {
                        fopFactory = FopFactory.newInstance(userConfigFile);
                    } else {
                        Debug.logWarning("FOP configuration file not found: " + userConfigFile, module);
                    }
                    URL fontBaseFileUrl = FlexibleLocation.resolveLocation(fopFontBaseProperty);
                    File fontBaseFile = FileUtil.getFile(fontBaseFileUrl.getFile());

                    if (fontBaseFile.isDirectory()) {
                        fopFactory.getFontManager().setResourceResolver(ResourceResolverFactory.createDefaultInternalResourceResolver(fontBaseFile.toURI()));
                    } else {
                        Debug.logWarning("FOP font base URL not found: " + fontBaseFile, module);
                    }
                    Debug.logInfo("FOP FontBaseURL: " + fopFactory.getFontManager().getResourceResolver().getBaseURI(), module);
                } catch (Exception e) {
                    Debug.logWarning(e, "Error reading FOP configuration: ", module);
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
        try (BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile))) {
        Fop fop = createFopInstance(dest, outputFormat);
        transform(src, stylesheet, fop);
        }
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
        return createFopInstance(out, outputFormat, null);
    }

    /** Returns a new Fop instance. Note: FOP documentation recommends using
     * a Fop instance for one transform run only.
     * @param out The target (result) OutputStream instance
     * @param outputFormat Optional output format, defaults to "application/pdf"
     * @param foUserAgent FOUserAgent object which may contains encryption-params in render options
     * @return Fop instance
     */
    public static Fop createFopInstance(OutputStream out, String outputFormat, FOUserAgent foUserAgent) throws FOPException {
        if (UtilValidate.isEmpty(outputFormat)) {
            outputFormat = MimeConstants.MIME_PDF;
        }
        if (UtilValidate.isEmpty(foUserAgent)) {
            FopFactory fopFactory = getFactoryInstance();
            foUserAgent = fopFactory.newFOUserAgent();
        }
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
    private static class LocalResolver implements URIResolver {

        private URIResolver defaultResolver;

        private LocalResolver() {}

        private LocalResolver(URIResolver defaultResolver) {
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

    public static String getEncryptionLengthDefault() {
        return encryptionLengthDefault;
    }

    public static String getUserPasswordDefault() {
        return userPasswordDefault;
    }

    public static String getOwnerPasswordDefault() {
        return ownerPasswordDefault;
    }

    public static String getAllowPrintDefault() {
        return allowPrintDefault;
    }

    public static String getAllowCopyContentDefault() {
        return allowCopyContentDefault;
    }

    public static String getAllowEditContentDefault() {
        return allowEditContentDefault;
    }

    public static String getAllowEditAnnotationsDefault() {
        return allowEditAnnotationsDefault;
    }

    public static String getAllowAccessContentDefault() {
        return allowAccessContentDefault;
    }

    public static String getAllowFillInFormsDefault() {
        return allowFillInFormsDefault;
    }

    public static String getAllowAssembleDocumentDefault() {
        return allowAssembleDocumentDefault;
    }

    public static String getAllowPrintHqDefault() {
        return allowPrintHqDefault;
    }

    public static String getEncryptMetadataDefault() {
        return encryptMetadataDefault;
    }
}

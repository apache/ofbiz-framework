/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.widget.test;

import java.io.InputStream;

import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.SSLUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;


public class WidgetMacroLibraryTests extends OFBizTestCase {

    private String screenUrl = buildScreenUrl();
    private final String authentificationQuery = "?USERNAME=admin&PASSWORD=ofbiz";

    public WidgetMacroLibraryTests(String name) {
        super(name);
    }

    /**
     * Prepare the http client to call the demo layout screen
     */
    public HttpClient initHttpClient() throws HttpClientException {
        HttpClient http = new HttpClient();
        http.followRedirects(true);
        http.setAllowUntrusted(true);
        http.setHostVerificationLevel(SSLUtil.getHostCertNoCheck());
        return http;
    }

    /**
     * Build the demo layout screen URL, accounting for --portoffset since each
     * test method runs against a fresh instance and can't share a mutated field.
     */
    private static String buildScreenUrl() {
        String url = "https://localhost:8443/webtools/control/WebtoolsLayoutDemo"; //use existing screen to present most of layout use case
        int portOffset = Start.getInstance().getConfig().getPortOffset();
        if (portOffset != 0) {
            url = url.replace("8443", String.valueOf(8443 + portOffset));
        }
        return url;
    }

    /**
     * Test html macro library.
     * @throws Exception the exception
     */
    public void testHtmlMacroLibrary() throws Exception {
        HttpClient http = initHttpClient();
        http.setUrl(screenUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull("Response failed from ofbiz", screenOutString);
        assertEquals("Response contentType isn't good : " + http.getResponseContentType(), "text/html;charset=UTF-8", http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse("Html Screen contains Macro on error : see " + screenUrl + " for more detail",
                screenOutString.contains("FreeMarker template error:"));
    }

    /**
     * Test text macro library.
     * @throws Exception the exception
     */
    public void testTextMacroLibrary() throws Exception {
        String screentextUrl = screenUrl.concat("Text");
        HttpClient http = initHttpClient();
        http.setUrl(screentextUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull("Response failed from ofbiz", screenOutString);
        assertEquals("Response contentType isn't good : " + http.getResponseContentType(), "text/html;charset=UTF-8", http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse("Text Screen contains Macro on error : see " + screentextUrl + " for more detail",
                screenOutString.contains("FreeMarker template error:"));
    }

    /**
     * Test xml macro library.
     * @throws Exception the exception
     */
    public void testXmlMacroLibrary() throws Exception {
        String screenxmlUrl = screenUrl.concat("Xml");
        HttpClient http = initHttpClient();
        http.setUrl(screenxmlUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull("Response failed from ofbiz", screenOutString);
        assertEquals("Response contentType isn't good : " + http.getResponseContentType(), "text/xml;charset=UTF-8", http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse("Xml Screen contains Macro on error : see " + screenxmlUrl + " for more detail",
                screenOutString.contains("FreeMarker template error:"));
    }

    /**
     * Test csv macro library.
     * @throws Exception the exception
     */
    public void testCsvMacroLibrary() throws Exception {
        String screencsvUrl = screenUrl.concat("Csv");
        HttpClient http = initHttpClient();
        http.setUrl(screencsvUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull("Response failed from ofbiz", screenOutString);
        assertEquals("Response contentType isn't good : " + http.getResponseContentType(), "text/csv;charset=UTF-8", http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse("Csv Screen contains Macro on error : see " + screencsvUrl + " for more detail",
                screenOutString.contains("FreeMarker template error:"));
    }

    /**
     * Test xls macro library.
     * @throws Exception the exception
     */
    public void testXlsMacroLibrary() throws Exception {
        String screenxlsUrl = screenUrl.concat("Xls");
        HttpClient http = initHttpClient();
        http.setUrl(screenxlsUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull("Response failed from ofbiz", screenOutString);
        assertEquals("Response contentType isn't good : " + http.getResponseContentType(), "application/vnd.ms-excel;charset=UTF-8",
                http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse("Csv Screen contains Macro on error : see " + screenxlsUrl + " for more detail",
                screenOutString.contains("FreeMarker template error:"));
    }

    /**
     * Test fop macro library.
     * @throws Exception the exception
     */
    public void testFopMacroLibrary() throws Exception {
        String screentextUrl = screenUrl.concat("Fop");
        HttpClient http = initHttpClient();
        http.setUrl(screentextUrl.concat(authentificationQuery));
        //FIXME need to check if the stream is an application-pdf that don't contains ftl stack trace
        InputStream screenInputStream = http.postStream();
        assertNotNull("Response failed from ofbiz", screenInputStream);
        assertEquals("Response contentType isn't good : " + http.getResponseContentType(), "application/pdf;charset=UTF-8",
                http.getResponseContentType());

        String screenOutString = "";
        try {
            BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
            Metadata metadata = new Metadata();
            new PDFParser().parse(screenInputStream, handler, metadata, new ParseContext());
            screenOutString = handler.toString();
        } finally {
            screenInputStream.close();
        }
        //Test if a ftl macro error is present
        assertFalse("Fop Screen contains Macro on error : see " + screentextUrl + " for more detail",
                screenOutString.contains("FreeMarker template error:"));
    }
}

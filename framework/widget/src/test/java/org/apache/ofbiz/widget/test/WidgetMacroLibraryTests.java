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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.SSLUtil;
import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;


@JunitJupiterTest
public class WidgetMacroLibraryTests implements JupiterTestHelper {

    private String screenUrl = "https://localhost:8443/webtools/control/WebtoolsLayoutDemo"; //use existing screen to present most of layout use case
    private final String authentificationQuery = "?USERNAME=admin&PASSWORD=ofbiz";

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
     * Test html macro library.
     * @throws Exception the exception
     */
    @Test
    @Order(1)
    public void testHtmlMacroLibrary() throws Exception {
        HttpClient http = initHttpClient();
        if (Start.getInstance().getConfig().getPortOffset() != 0) {
            Integer port = 8443 + Start.getInstance().getConfig().getPortOffset();
            screenUrl = screenUrl.replace("8443", port.toString());
        }
        http.setUrl(screenUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull(screenOutString, "Response failed from ofbiz");
        assertEquals("text/html;charset=UTF-8", http.getResponseContentType(), "Response contentType isn't good : " + http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse(screenOutString.contains("FreeMarker template error:"),
                "Html Screen contains Macro on error : see " + screenUrl + " for more detail");
    }

    /**
     * Test text macro library.
     * @throws Exception the exception
     */
    @Test
    @Order(2)
    public void testTextMacroLibrary() throws Exception {
        String screentextUrl = screenUrl.concat("Text");
        HttpClient http = initHttpClient();
        http.setUrl(screentextUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull(screenOutString, "Response failed from ofbiz");
        assertEquals("text/html;charset=UTF-8", http.getResponseContentType(), "Response contentType isn't good : " + http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse(screenOutString.contains("FreeMarker template error:"),
                "Text Screen contains Macro on error : see " + screentextUrl + " for more detail");
    }

    /**
     * Test xml macro library.
     * @throws Exception the exception
     */
    @Test
    @Order(3)
    public void testXmlMacroLibrary() throws Exception {
        String screenxmlUrl = screenUrl.concat("Xml");
        HttpClient http = initHttpClient();
        http.setUrl(screenxmlUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull(screenOutString, "Response failed from ofbiz");
        assertEquals("text/xml;charset=UTF-8", http.getResponseContentType(), "Response contentType isn't good : " + http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse(screenOutString.contains("FreeMarker template error:"),
                "Xml Screen contains Macro on error : see " + screenxmlUrl + " for more detail");
    }

    /**
     * Test csv macro library.
     * @throws Exception the exception
     */
    @Test
    @Order(4)
    public void testCsvMacroLibrary() throws Exception {
        String screencsvUrl = screenUrl.concat("Csv");
        HttpClient http = initHttpClient();
        http.setUrl(screencsvUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull(screenOutString, "Response failed from ofbiz");
        assertEquals("text/csv;charset=UTF-8", http.getResponseContentType(), "Response contentType isn't good : " + http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse(screenOutString.contains("FreeMarker template error:"),
                "Csv Screen contains Macro on error : see " + screencsvUrl + " for more detail");
    }

    /**
     * Test xls macro library.
     * @throws Exception the exception
     */
    @Test
    @Order(5)
    public void testXlsMacroLibrary() throws Exception {
        String screenxlsUrl = screenUrl.concat("Xls");
        HttpClient http = initHttpClient();
        http.setUrl(screenxlsUrl.concat(authentificationQuery));
        String screenOutString = http.post();
        assertNotNull(screenOutString, "Response failed from ofbiz");
        assertEquals("application/vnd.ms-excel;charset=UTF-8",
                http.getResponseContentType(), "Response contentType isn't good : " + http.getResponseContentType());

        //Test if a ftl macro error is present
        assertFalse(screenOutString.contains("FreeMarker template error:"),
                "Csv Screen contains Macro on error : see " + screenxlsUrl + " for more detail");
    }

    /**
     * Test fop macro library.
     * @throws Exception the exception
     */
    @Test
    @Order(6)
    public void testFopMacroLibrary() throws Exception {
        String screentextUrl = screenUrl.concat("Fop");
        HttpClient http = initHttpClient();
        http.setUrl(screentextUrl.concat(authentificationQuery));
        //FIXME need to check if the stream is an application-pdf that don't contains ftl stack trace
        InputStream screenInputStream = http.postStream();
        assertNotNull(screenInputStream, "Response failed from ofbiz");
        assertEquals("application/pdf;charset=UTF-8",
                http.getResponseContentType(), "Response contentType isn't good : " + http.getResponseContentType());

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
        assertFalse(screenOutString.contains("FreeMarker template error:"),
                "Fop Screen contains Macro on error : see " + screentextUrl + " for more detail");
    }
}

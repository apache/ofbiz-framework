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
package org.ofbiz.webapp.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Utility methods needed to implement a WebDAV servlet. */
public class WebDavUtil {

    public static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
    public static final String RFC1123_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    public static String formatDate(String formatString, Date date) {
        DateFormat df = new SimpleDateFormat(formatString);
        df.setTimeZone(GMT_TIMEZONE);
        return df.format(date);
    }

    public static Document getDocumentFromRequest(HttpServletRequest request) throws IOException, SAXException, ParserConfigurationException {
        InputStream is = request.getInputStream();
        Document document = UtilXml.readXmlDocument(is, false, "WebDAV request");
        is.close();
        return document;
    }

}

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
package org.apache.ofbiz.base.util;

import org.jsoup.parser.ParseError;
import org.jsoup.parser.Parser;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public final class UtilHtml {

    private static final String MODULE = UtilHtml.class.getName();
    private static final Parser JSOUP_HTML_PARSER = createJSoupHtmlParser();
    private static final String[] TAG_SHOULD_CLOSE_LIST = new String[]{"div"};
    private UtilHtml() { }

    private static Parser createJSoupHtmlParser() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(100);
        return parser;
    }

    public static List<ParseError> validateHtmlFragmentWithJSoup(String content) {
        if (content != null) {
            JSOUP_HTML_PARSER.parseInput(content, "");
            if (JSOUP_HTML_PARSER.isTrackErrors()) {
                return JSOUP_HTML_PARSER.getErrors();
            }
        }
        return null;
    }

    /**
     *
     * @param content
     * @param locationInfo for printing location information
     * @return true if there is error
     */
    public static boolean hasUnclosedTag(String content, String locationInfo) {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = null;
        try {
            eventReader = inputFactory.createXMLEventReader(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "utf-8");
        } catch (XMLStreamException e) {
            Debug.logError(e.getMessage(), MODULE);
            return true;
        }

        Stack<StartElement> stack = new Stack<StartElement>();
        boolean hasError = false;
        while (eventReader.hasNext()) {
            try {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    stack.push(startElement);
                }
                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    stack.pop();
                }
            } catch (XMLStreamException e) {
                if (!stack.isEmpty()) {
                    StartElement startElement = stack.pop();
                    String elementName = startElement.getName().getLocalPart();
                    if (Arrays.stream(TAG_SHOULD_CLOSE_LIST).anyMatch(elementName::equals)) {
                        hasError = true;
                        UtilHtml.logFormattedError(content, locationInfo, e.getMessage(), MODULE);
                    }
                } else {
                    UtilHtml.logFormattedError(content, locationInfo, e.getMessage(), MODULE);
                }
                break;
            }
        }
        return hasError;
    }

    public static void logFormattedError(String content, String location, String error, String module) {
        Debug.logError("[Parsing " + location + "]" + error, module);
    }
}

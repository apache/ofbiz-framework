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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.ofbiz.widget.model.ThemeFactory;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.Parser;

public final class UtilHtml {

    private static final String MODULE = UtilHtml.class.getName();
    private static final Parser JSOUP_HTML_PARSER = createJSoupHtmlParser();
    private static final String[] TAG_SHOULD_CLOSE_LIST = new String[]{"div"};
    private static List<String> visualThemeBasePathsName;
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
     * @return list of errors
     */
    public static List<String> hasUnclosedTag(String content) {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader = null;
        List<String> errorList = new ArrayList<>();
        try {
            // wrap with template tag as some content contains multiple root
            eventReader = inputFactory.createXMLEventReader(
                    new ByteArrayInputStream(("<template>" + content + "</template>").getBytes(StandardCharsets.UTF_8)),
                    "utf-8");
            Stack<StartElement> stack = new Stack<StartElement>();
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
                            errorList.add(e.getMessage());
                        }
                    } else {
                        errorList.add(e.getMessage());
                    }
                    break;
                }
            }
        } catch (XMLStreamException e) {
            errorList.add(e.getMessage());
        } finally {
            if (eventReader != null) {
                try {
                    eventReader.close();
                } catch (XMLStreamException e) {
                    Debug.logError(e, MODULE);
                }
            }
        }
        return errorList;
    }

    public static List<String> getVisualThemeFolderNamesToExempt() {
        if (visualThemeBasePathsName == null) {
            try {
                List<File> xmlThemes = ThemeFactory.getThemeXmlFiles();
                visualThemeBasePathsName = new ArrayList<>();
                String themePathKey = "/themes/";
                String pluginPathKey = "/plugins/";
                for (File xmlTheme : xmlThemes) {
                    String path = xmlTheme.toURI().toURL().toString();
                    // get the path after themes or plugins folders
                    if (path.indexOf(themePathKey) > 0) {
                        path = path.substring(path.indexOf(themePathKey) + 8);
                    } else if (path.indexOf(pluginPathKey) > 0) {
                        path = path.substring(path.indexOf(pluginPathKey) + 9);
                    }
                    // get folder name
                    path = path.substring(0, path.indexOf("/"));
                    if (!path.contains("common-theme") && !path.contains("ecommerce")) {
                        visualThemeBasePathsName.add("/" + path + "/");
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, MODULE);
            }
        }
        return Collections.unmodifiableList(visualThemeBasePathsName);
    }

    public static void logHtmlWarning(String content, String location, String error, String module) {
        Debug.logWarning("[Parsing " + location + "] " + error, module);
    }
}

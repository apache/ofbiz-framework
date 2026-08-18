/*
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
 */
package org.apache.ofbiz.testtools.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Sums pass/fail/skip counts across every {@code <testsuite>} JUnit XML file under a directory,
 * matching the {@code tests}/{@code failures}/{@code errors}/{@code skipped} attributes
 * {@code org.apache.ofbiz.testtools.SuiteXmlReportWriter} writes for {@code testIntegration}
 * runs, and that Gradle's own JUnit Platform listener writes for the plain {@code test} task.
 */
public final class JUnitXmlCounter {

    private static final String MODULE = JUnitXmlCounter.class.getName();

    private JUnitXmlCounter() {
    }

    /** Recursively walks {@code resultsDir} for {@code *.xml} files and sums their testsuite counts. */
    public static TestRunManifest.Counts count(File resultsDir) {
        int total = 0;
        int failed = 0;
        int skipped = 0;
        if (resultsDir != null && resultsDir.isDirectory()) {
            for (File xmlFile : listXmlFilesRecursively(resultsDir)) {
                try {
                    TestRunManifest.Counts fileCounts = countOneFile(xmlFile);
                    total += fileCounts.getTotal();
                    failed += fileCounts.getFailed();
                    skipped += fileCounts.getSkipped();
                } catch (Exception e) {
                    // Malformed/partial XML from an interrupted run - skip it, don't fail archiving.
                    Debug.logWarning(e, "JUnitXmlCounter: skipping unparsable file " + xmlFile, MODULE);
                }
            }
        }
        return new TestRunManifest.Counts(total, total - failed - skipped, failed, skipped);
    }

    private static TestRunManifest.Counts countOneFile(File xmlFile)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        NodeList suites = doc.getElementsByTagName("testsuite");
        int total = 0;
        int failed = 0;
        int skipped = 0;
        for (int i = 0; i < suites.getLength(); i++) {
            Element suite = (Element) suites.item(i);
            total += parseIntAttribute(suite, "tests");
            failed += parseIntAttribute(suite, "failures") + parseIntAttribute(suite, "errors");
            skipped += parseIntAttribute(suite, "skipped");
        }
        return new TestRunManifest.Counts(total, total - failed - skipped, failed, skipped);
    }

    private static int parseIntAttribute(Element element, String attributeName) {
        String value = element.getAttribute(attributeName);
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<File> listXmlFilesRecursively(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) {
            return result;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                result.addAll(listXmlFilesRecursively(child));
            } else if (child.getName().endsWith(".xml")) {
                result.add(child);
            }
        }
        return result;
    }
}

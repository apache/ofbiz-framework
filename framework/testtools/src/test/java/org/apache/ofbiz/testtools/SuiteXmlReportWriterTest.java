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
package org.apache.ofbiz.testtools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Asserts SuiteXmlReportWriter's output structurally (via a real DOM parse), the same way
 * test-reports.gradle's parseSuiteXml() reads it - not via brittle exact-string comparison.
 */
class SuiteXmlReportWriterTest {

    @Test
    void writesAMergedSuiteMatchingTheAntFormatterShape() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SuiteXmlReportWriter writer = new SuiteXmlReportWriter(out);

        writer.startSuite("mysuite");
        writer.testStarted("com.example.Foo", "testA");
        writer.testFinished("com.example.Foo", "testA", 18, SuiteReportSink.Outcome.passed());
        writer.testStarted("com.example.Foo", "testB");
        writer.testFinished("com.example.Foo", "testB", 5,
                SuiteReportSink.Outcome.failure("expected true", "java.lang.AssertionError", "stack trace text"));
        writer.testStarted("com.example.Bar", "testC");
        writer.testFinished("com.example.Bar", "testC", 3, SuiteReportSink.Outcome.error(new RuntimeException("boom")));
        writer.endSuite();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(out.toByteArray()));
        Element root = doc.getDocumentElement();

        assertThat(root.getTagName(), is("testsuite"));
        assertThat(root.getAttribute("name"), is("mysuite"));
        assertThat(root.getAttribute("tests"), is("3"));
        assertThat(root.getAttribute("failures"), is("1"));
        assertThat(root.getAttribute("errors"), is("1"));
        assertThat(root.getAttribute("skipped"), is("0"));
        assertThat(Double.parseDouble(root.getAttribute("time")), greaterThanOrEqualTo(0.0));
        assertThat(root.getAttribute("timestamp"), matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        assertThat(root.getAttribute("hostname").isEmpty(), is(false));

        NodeList properties = root.getElementsByTagName("properties");
        assertThat(properties.getLength(), is(1));

        NodeList testcases = root.getElementsByTagName("testcase");
        assertThat(testcases.getLength(), is(3));

        Element first = (Element) testcases.item(0);
        assertThat(first.getAttribute("classname"), is("com.example.Foo"));
        assertThat(first.getAttribute("name"), is("testA"));
        assertThat(Double.parseDouble(first.getAttribute("time")), is(0.018));
        assertThat(first.getElementsByTagName("failure").getLength(), is(0));
        assertThat(first.getElementsByTagName("error").getLength(), is(0));

        Element second = (Element) testcases.item(1);
        assertThat(second.getAttribute("classname"), is("com.example.Foo"));
        assertThat(second.getAttribute("name"), is("testB"));
        Element failure = (Element) second.getElementsByTagName("failure").item(0);
        assertThat(failure.getAttribute("message"), is("expected true"));
        assertThat(failure.getAttribute("type"), is("java.lang.AssertionError"));
        assertThat(failure.getTextContent(), containsString("stack trace text"));

        Element third = (Element) testcases.item(2);
        assertThat(third.getAttribute("classname"), is("com.example.Bar"));
        assertThat(third.getAttribute("name"), is("testC"));
        Element error = (Element) third.getElementsByTagName("error").item(0);
        assertThat(error.getAttribute("message"), is("boom"));
        assertThat(error.getAttribute("type"), is("java.lang.RuntimeException"));
        assertThat(error.getTextContent(), containsString("java.lang.RuntimeException: boom"));
    }

    @Test
    void wasSuccessfulIsFalseWhenAnyFailureOrErrorWasRecorded() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SuiteXmlReportWriter writer = new SuiteXmlReportWriter(out);
        writer.startSuite("mysuite");
        writer.testFinished("com.example.Foo", "testA", 1, SuiteReportSink.Outcome.passed());

        assertThat(writer.wasSuccessful(), is(true));

        writer.testFinished("com.example.Foo", "testB", 1, SuiteReportSink.Outcome.error(new RuntimeException()));

        assertThat(writer.wasSuccessful(), is(false));
    }

    @Test
    void dropsXml10IllegalControlCharactersSoTheOutputStillParses() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SuiteXmlReportWriter writer = new SuiteXmlReportWriter(out);
        writer.startSuite("mysuite");
        // 0x1B is ESC, an XML 1.0 illegal control character - plausible in an arbitrary exception message.
        String esc = "\u001B";
        writer.testFinished("com.example.Foo", "testA", 1,
                SuiteReportSink.Outcome.failure("bad" + esc + "message", "java.lang.AssertionError",
                        "stack" + esc + "trace"));
        writer.endSuite();

        // A real DOM parse - not a string check - is the assertion that matters here: before the fix,
        // the ESC character above made this an XmlSlurper/DocumentBuilder parse failure exactly the way
        // test-reports.gradle's parseSuiteXml() would hit it.
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(out.toByteArray()));
        Element failure = (Element) doc.getElementsByTagName("failure").item(0);

        assertThat(failure.getAttribute("message"), is("badmessage"));
        assertThat(failure.getTextContent(), is("stacktrace"));
    }

    @Test
    void messageAttributeIsOmittedWhenNull() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SuiteXmlReportWriter writer = new SuiteXmlReportWriter(out);
        writer.startSuite("mysuite");
        writer.testFinished("com.example.Foo", "testA", 1, SuiteReportSink.Outcome.error(new RuntimeException()));
        writer.endSuite();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(out.toByteArray()));
        Element error = (Element) doc.getElementsByTagName("error").item(0);

        assertThat(error.hasAttribute("message"), is(false));
    }
}

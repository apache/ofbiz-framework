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

package org.ofbiz.testtools.selenium;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.HttpCommandProcessor;
import com.thoughtworks.selenium.SeleniumException;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SeleniumHtml {
    public static final String module = SeleniumHtml.class.getName();
    public String host;
    public int port;
    public String browser;
    public String baseUrl;

    Document document;
    CommandProcessor commandProcessor;

    class TestSuite {
        public File file;
        public String name;
        public Test tests[];
        public boolean result;
    }
    class Test {
        public String label;
        public File file;
        public String name;
        public Command commands[];
        public boolean result;
    }
    class Command {
        public String cmd;
        public String args[];
        public String result;
        public boolean error;
        public boolean failure;
    }

    public SeleniumHtml() {
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static String runHtmlTestSuite(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map parameters = UtilHttp.getParameterMap(request);
        String para = (String) parameters.get("testSuitePath");

        try {
            SeleniumHtml client = new SeleniumHtml();
            client.host = UtilProperties.getPropertyValue("seleniumXml.properties", "serverHost", "localhost");
            client.port = Integer.parseInt(UtilProperties.getPropertyValue("seleniumXml.properties", "proxyPort", "4444"));
            client.browser = UtilProperties.getPropertyValue("seleniumXml.properties", "browser", "*firefox");
            client.baseUrl = UtilProperties.getPropertyValue("seleniumXml.properties", "startUrlHttps", "https://localhost:8443");

            if (Debug.infoOn()) {
                Debug.logInfo("Parameters used for selenium: host: " + client.host
                        + ", port: " + client.port + ", browser: " + client.browser
                        + ", baseUrl: " + client.baseUrl, module);
            }

            File testFile = new File(para.trim());
            if (testFile.exists()) {
                if (Debug.infoOn()) Debug.logInfo("Running this testsuite: " + testFile.getAbsolutePath(), module);

                Map results = client.runSuite(testFile.getAbsolutePath());
                if ("true".equals(results.get("status").toString())) {
                    request.setAttribute("_EVENT_MESSAGE_LIST_", results.get("logs"));
                } else {
                    request.setAttribute("_ERROR_MESSAGE_LIST_", results.get("logs"));
                }
            }
        } catch (Exception e) {
            Debug.logError(e.getMessage(), module);
            return "error";
        }
        return "success";
   }

    public Map runSuite(String filename) throws Exception {
        TestSuite suite = new TestSuite();
        suite.file = new File(filename);
        File suiteDirectory = suite.file.getParentFile();
        this.document = parseDocument(filename);
        Element table = (Element) this.document.getElementsByTagName("table").item(0);
        NodeList tableRows = table.getElementsByTagName("tr");
        Element tableNameRow = (Element) tableRows.item(0);
        suite.name = tableNameRow.getTextContent();
        suite.result = true;
        suite.tests = new Test[tableRows.getLength() - 1];

        Map<String, Object> results = FastMap.newInstance();
        List<String> messages = FastList.newInstance();
        Map testResults;

        for (int i = 1; i < tableRows.getLength(); i++) {
            Element tableRow = (Element) tableRows.item(i);
            Element cell = (Element) tableRow.getElementsByTagName("td").item(0);
            Element link = (Element) cell.getElementsByTagName("a").item(0);
            Test test = new Test();
            test.label = link.getTextContent();
            test.file = new File(suiteDirectory, link.getAttribute("href"));

            SeleniumHtml subclient = new SeleniumHtml();
            subclient.setHost(this.host);
            subclient.setPort(this.port);
            subclient.setBrowser(this.browser);
            subclient.setBaseUrl(this.baseUrl);
            testResults = subclient.runTest(test);
            suite.result &= test.result;
            suite.tests[i - 1] = test;
            messages = UtilGenerics.checkList(testResults.get("log"));
        }

        results.put("logs", messages);
        results.put("status", suite.result);
        return results;
    }

    public Map runTest(Test test) throws Exception {
        String filename = test.file.toString();
        List<String> messages = FastList.newInstance();

        if (Debug.infoOn()) {
            Debug.logInfo("Running " + filename + " against " + this.host + ":" + this.port + " with " + this.browser, module);
        }
        this.document = parseDocument(filename);

        if (this.baseUrl == null) {
            NodeList links = this.document.getElementsByTagName("link");
            if (links.getLength() != 0) {
                Element link = (Element) links.item(0);
                setBaseUrl(link.getAttribute("href"));
            }
        }
        if (Debug.infoOn()) {
            Debug.logInfo("Base URL=" + this.baseUrl, module);
        }

        Node body = this.document.getElementsByTagName("body").item(0);
        Element resultContainer = document.createElement("div");
        resultContainer.setTextContent("Result: ");
        Element resultElt = document.createElement("span");
        resultElt.setAttribute("id", "result");
        resultElt.setIdAttribute("id", true);
        resultContainer.appendChild(resultElt);
        body.insertBefore(resultContainer, body.getFirstChild());

        Element executionLogContainer = document.createElement("div");
        executionLogContainer.setTextContent("Execution Log:");
        Element executionLog = document.createElement("div");
        executionLog.setAttribute("id", "log");
        executionLog.setIdAttribute("id", true);
        executionLog.setAttribute("style", "white-space: pre;");
        executionLogContainer.appendChild(executionLog);
        body.appendChild(executionLogContainer);

        NodeList tableRows = document.getElementsByTagName("tr");
        Element theadRow = (Element) tableRows.item(0);
        test.name = theadRow.getTextContent();

        this.commandProcessor = new HtmlCommandProcessor(this.host, this.port, this.browser, this.baseUrl);
        String resultState;
        String resultLog;
        test.result = true;
        try {
            this.commandProcessor.start();
            test.commands = new Command[tableRows.getLength() - 1];
            for (int i = 1; i < tableRows.getLength(); i++) {
                Element stepRow = (Element) tableRows.item(i);
                Command command = executeStep(stepRow);
                messages.add(command.result + " --> " + command.cmd + " " + Arrays.asList(command.args));

                test.commands[i - 1] = command;
                if (command.error) {
                    test.result = false;
                }
                if (command.failure) {
                    test.result = false;
                    break;
                }
            }
            resultState = test.result ? "PASSED" : "FAILED";
            resultLog = (test.result ? "Test Complete" : "Error");
            this.commandProcessor.stop();
        } catch (Exception e) {
            test.result = false;
            resultState = "ERROR";
            resultLog = "Failed to initialize session\n" + e;
            e.printStackTrace();
        }
        document.getElementById("result").setTextContent(resultState);
        Element log = document.getElementById("log");
        log.setTextContent(log.getTextContent() + resultLog + "\n");

        Map<String, Object> results = FastMap.newInstance();
        results.put("log", messages);

        return results;
    }

    public Command executeStep(Element stepRow) throws Exception {
        Command command = new Command();
        NodeList stepFields = stepRow.getElementsByTagName("td");
        String cmd = stepFields.item(0).getTextContent().trim();
        command.cmd = cmd;
        ArrayList<String> argList = new ArrayList<String>();
        if (stepFields.getLength() == 1) {
            // skip comments
            command.result = "OK";
            return command;
        }
        for (int i = 1; i < stepFields.getLength(); i++) {
            String content = stepFields.item(i).getTextContent();
            content = content.replaceAll(" +", " ");
            content = content.replace('\u00A0', ' ');
            content = content.trim();
            argList.add(content);
        }
        String args[] = argList.toArray(new String[0]);
        command.args = args;
        if (Debug.infoOn()) {
            Debug.logInfo(cmd + " " + Arrays.asList(args), module);
        }
        try {
            command.result = this.commandProcessor.doCommand(cmd, args);
            command.error = false;
        } catch (Exception e) {
            command.result = e.getMessage();
            command.error = true;
        }
        command.failure = command.error && !cmd.startsWith("verify");
        return command;
    }

    Document parseDocument(String filename) throws Exception {
        FileReader reader = new FileReader(filename);
        String firstLine = new BufferedReader(reader).readLine();
        reader.close();
        Document document = null;
        if (firstLine.startsWith("<?xml")) {
            Debug.logInfo("XML detected; using default XML parser.", module);
        } else {
            try {
                Class nekoParserClass = Class.forName("org.cyberneko.html.parsers.DOMParser");
                Object parser = nekoParserClass.newInstance();
                Method parse = nekoParserClass.getMethod("parse", new Class[] { String.class });
                Method getDocument = nekoParserClass.getMethod("getDocument", new Class[0]);
                parse.invoke(parser, filename);
                document = (Document) getDocument.invoke(parser);
            } catch (Exception e) {
                Debug.logInfo("NekoHTML HTML parser not found; HTML4 support disabled.", module);
            }
        }
        if (document == null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try { // http://www.w3.org/blog/systeam/2008/02/08/w3c_s_excessive_dtd_traffic
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }
            catch (ParserConfigurationException e) {
                Debug.logInfo("Warning: Could not disable external DTD loading", module);
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(filename);
        }
        return document;
    }

    class HtmlCommandProcessor extends HttpCommandProcessor {
        final static String INDEX_SPECIFIER = "index=";
        final static String ID_SPECIFIER = "id=";
        final static String LABEL_SPECIFIER = "label=";
        final static String VALUE_SPECIFIER = "value=";

        boolean expectError;

        public HtmlCommandProcessor(String host, int port, String browser, String baseUrl) {
            super(host, port, browser, baseUrl);
        }

        public String doCommand(String cmd, String args[]) {
            if (cmd.equals("store")) {
                cmd += "Expression";
            } else if (cmd.equals("assertSelected") || cmd.equals("verifySelected")) {
                if (args[1].startsWith(INDEX_SPECIFIER)) {
                    cmd += "Index";
                    args[1] = args[1].substring(INDEX_SPECIFIER.length());
                } else if (args[1].startsWith(ID_SPECIFIER)) {
                    cmd += "Id";
                    args[1] = args[1].substring(ID_SPECIFIER.length());
                } else if (args[1].startsWith(LABEL_SPECIFIER)) {
                    cmd += "Label";
                    args[1] = args[1].substring(LABEL_SPECIFIER.length());
                } else if (args[1].startsWith(VALUE_SPECIFIER)) {
                    cmd += "Value";
                    args[1] = args[1].substring(VALUE_SPECIFIER.length());
                } else {
                    cmd += "Label";
                }
            } else if (cmd.endsWith("ErrorOnNext") || cmd.endsWith("FailureOnNext")) {
                expectError = true;
                return "OK";
            } else if (cmd.equals("echo")) {
                return "OK," + args[0];
            } else if (cmd.equals("pause")) {
                try {
                    Thread.sleep(Integer.parseInt(args[0]));
                    return "OK";
                } catch (InterruptedException e) {
                    return "ERROR: pause interrupted";
                }
            }
            try {
                String result = super.doCommand(cmd, args);
                if (expectError) {
                    throw new SeleniumException("ERROR: Error expected");
                } else {
                    return result;
                }
            } catch (SeleniumException e) {
                if (expectError) {
                    expectError = false;
                    return "OK";
                } else {
                    throw e;
                }
            }
        }
    }
}

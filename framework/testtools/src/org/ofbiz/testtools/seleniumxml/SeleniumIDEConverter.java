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

package org.ofbiz.testtools.seleniumxml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

import org.ofbiz.base.util.UtilGenerics;

public class SeleniumIDEConverter {

    private Document ideFile;
    private Element xmlDestRoot;
    private Namespace ns = Namespace.getNamespace("http://www.w3.org/1999/xhtml");
    private Map root;

    public void convert(String ideFile, String xmlFile) throws JDOMException, IOException, SAXException, ParserConfigurationException {
        readInputFile(ideFile);
        convertIDECommands();
        createSeleniumXml(xmlFile);
    }

    private void readInputFile(String input) throws JDOMException, IOException, SAXException, ParserConfigurationException {
        File xmlFile = new File(input);
        SAXBuilder builder = new SAXBuilder();
        this.ideFile = builder.build(xmlFile);
    }

    private void createSeleniumXml(String outputFile) {
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            XMLOutputter serializer = new XMLOutputter( Format.getPrettyFormat());
            serializer.output(this.xmlDestRoot, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void convertIDECommands() throws JDOMException {
        Element root = this.ideFile.getRootElement();
        this.xmlDestRoot = new Element("testcase");
        //TODO: there must be a better way to do this with JDom
        Element e1 = root.getChild("body",ns);
        Element e2 = e1.getChild("table",ns);
        Element e3 = e2.getChild("tbody",ns);
        List<Element> list = UtilGenerics.cast(e3.getChildren("tr", ns));
        List<Element> commands = UtilGenerics.cast(root.getChild("body",ns).getChild("table",ns).getChild("tbody",ns).getChildren("tr", ns));
        for(Element elem: commands) {
            processIDECommand(elem);
        }
    }

    private void processIDECommand(Element elem) throws JDOMException {
        List<Element> cmd = UtilGenerics.cast(elem.getChildren("td", ns));
        Element cmdElem = cmd.get(0);
        String cmdToCompare = cmdElem.getValue();
        System.out.println("Checking for cmd: " + cmdToCompare);

        if ("clickAndWait".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found clickAndWait");
            this.xmlDestRoot.addContent(buildCommand("click", "locator", cmd.get(1).getValue(), null, null));
            this.xmlDestRoot.addContent(buildCommand("waitForPageToLoad", "value", "10000", null, null));
        } else if ("type".compareTo(cmdElem.getValue()) == 0 ) {
            System.out.println("Found type");
            this.xmlDestRoot.addContent (buildCommand("type", "name", cmd.get(1).getValue(), "value", cmd.get(2).getValue()));
        } else if ("select".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found select");
            this.xmlDestRoot.addContent(buildCommand("select", "locator", cmd.get(1).getValue(), "option", cmd.get(2).getValue()));
        } else if ("open".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found open");
            this.xmlDestRoot.addContent(buildCommand("open", "value", cmd.get(1).getValue(), null, null));
        } else if ("click".compareTo(cmdElem.getValue()) == 0) {
            Element newCmd = new Element("click");
            newCmd.setAttribute("locator", cmd.get(1).getValue());
            this.xmlDestRoot.addContent(newCmd);
        } else if ("doubleClick".compareTo(cmdElem.getValue()) == 0) {
            Element newCmd = new Element("doubleClick");
            newCmd.setAttribute("locator", cmd.get(1).getValue());
            this.xmlDestRoot.addContent(newCmd);
        } else if ("echo".compareTo(cmdElem.getValue()) == 0) {
             System.out.println("Found echo");
             Element newCmd = new Element("print");
             newCmd.setAttribute("value", cmd.get(1).getValue());
             this.xmlDestRoot.addContent(newCmd);
        } else if ("verifyTextPresent".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found verifyTextPresent");
            this.xmlDestRoot.addContent(buildCommand("getBodyText", "out", "bodySource", null, null));
            this.xmlDestRoot.addContent(buildCommand("assertContains", "test", cmd.get(1).getValue(), "src", "${bodySource}"));
        } else if ("verifyTextNotPresent".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found verifyTextNotPresent");
            this.xmlDestRoot.addContent(buildCommand("getBodyText", "out", "bodySource", null, null));
            this.xmlDestRoot.addContent(buildCommand("assertNotContains", "test", cmd.get(1).getValue(), "src", "${bodySource}"));
        } else if ("assertTitle".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found assertTitle");
            this.xmlDestRoot.addContent(buildCommand("assertTitle", "value", cmd.get(1).getValue(), null, null));
        } else if ("assertConfirmation".compareTo(cmdElem.getValue()) == 0) {
            System.out.println("Found assertConfirmation");
            this.xmlDestRoot.addContent(buildCommand("assertConfirmation", "value", cmd.get(1).getValue(), null, null));
        } else {
            System.out.println("WARNING: No definition for " + cmdElem.getValue() + " defaulting to us 'reflection'.");
            Element newCmd = new Element(cmdElem.getValue());
            int size = cmd.size()-1;
            for(int i=1; i<size; i++) {
                String paramValue = cmd.get(i).getValue();
                System.out.println("param" + (i) + " :" + paramValue);
                newCmd.setAttribute("param" + (i), paramValue);
            }
            this.xmlDestRoot.addContent(newCmd);
        }
    }

    private Element buildCommand(String name, String attrib1, String value1, String attrib2, String value2) {
        Element newCmd = new Element(name);
        if (attrib1 != null) {
            newCmd.setAttribute(attrib1, value1);
        }
        if (attrib2 != null) {
            newCmd.setAttribute(attrib2, value2);
        }
        return newCmd;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please include the source and destination file paths.");
        } else {
            SeleniumIDEConverter sel = new SeleniumIDEConverter();
            try {
                sel.convert(args[0], args[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

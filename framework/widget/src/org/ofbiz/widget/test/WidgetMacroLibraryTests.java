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

package org.ofbiz.widget.test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.security.Security;
import org.ofbiz.security.SecurityFactory;
import org.ofbiz.service.testtools.OFBizTestCase;
import org.ofbiz.webapp.view.ApacheFopWorker;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.springframework.mock.web.MockServletContext;

public class WidgetMacroLibraryTests extends OFBizTestCase {

    protected final String screenLocation = "component://webtools/widget/MiscScreens.xml#WebtoolsLayoutDemo"; //use existing screen to present most of layout use case
    protected MapStack<String> context = null;
    protected Appendable writer = null;

    public WidgetMacroLibraryTests(String name) {
        super(name);
    }

    protected void initScreens(String screenType) throws Exception {
        GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
        TimeZone timeZone = TimeZone.getDefault();
        Locale locale = Locale.getDefault();
        context = MapStack.create();
        context.put("userLogin", userLogin);
        context.put("timeZone", timeZone);
        context.put("locale", locale);
        context.put("dispatcher", dispatcher);
        context.put("delegator", delegator);
        context.put("security", SecurityFactory.getInstance(delegator));
        context.put("servletContext", new MockServletContext());
        context.put("parameters", UtilMisc.toMap("mainDecoratorLocation", "component://webtools/widget/CommonScreens.xml"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Appendable writer = new OutputStreamWriter(output, "UTF-8");
        ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", screenType.concat(".name"), delegator), EntityUtilProperties.getPropertyValue("widget", screenType.concat(".screenrenderer"), delegator));
        context.put("screens", new ScreenRenderer(writer, context, screenStringRenderer));
    }

    public void testHtmlMacroLibrary() throws Exception {
        initScreens("screen");
        ScreenRenderer screens = (ScreenRenderer) context.get("screens");
        screens.render(screenLocation);
        String screenOutString = writer.toString();
        assertNotNull("HtmlScreen failed to return the screen " + screenLocation, screenOutString);

        //Test if a ftl macro error is present
        assertTrue("Html Screen contains Macro on error : see https://localhost:8443/webtools/control/WebtoolsLayoutDemo for more detail", screenOutString.contains("FreeMarker template error:"));
    }

    public void testTextMacroLibrary() throws Exception {
        initScreens("screentext");
        ScreenRenderer screens = (ScreenRenderer) context.get("screens");
        screens.render(screenLocation + "Text");
        String screenOutString = writer.toString();
        assertNotNull("TextScreen failed to return the screen " + screenLocation + "Text", screenOutString);

        //Test if a ftl macro error is present
        assertTrue("Text Screen contains Macro on error : see https://localhost:8443/webtools/control/WebtoolsLayoutDemoText for more detail", screenOutString.contains("FreeMarker template error:"));
    }

    public void testXmlMacroLibrary() throws Exception {
        initScreens("screenxml");
        ScreenRenderer screens = (ScreenRenderer) context.get("screens");
        screens.render(screenLocation + "Text");

        String screenOutString = writer.toString();
        assertNotNull("XmlScreen failed to return the screen " + screenLocation + "Text", screenOutString);

        //Test if a ftl macro error is present
        assertTrue("Xml Screen contains Macro on error : see https://localhost:8443/webtools/control/WebtoolsLayoutDemoXml for more detail", screenOutString.contains("FreeMarker template error:"));
    }

    public void testCsvMacroLibrary() throws Exception {
        initScreens("screencsv");
        ScreenRenderer screens = (ScreenRenderer) context.get("screens");
        screens.render(screenLocation + "Text");
        String screenOutString = writer.toString();
        assertNotNull("CsvScreen failed to return the screen " + screenLocation + "Text", screenOutString);

        //Test if a ftl macro error is present
        assertTrue("Csv Screen contains Macro on error : see https://localhost:8443/webtools/control/WebtoolsLayoutDemoCsv for more detail", screenOutString.contains("FreeMarker template error:"));
    }

    public void testFopMacroLibrary() throws Exception {
        initScreens("screenfop");
        ScreenRenderer screens = (ScreenRenderer) context.get("screens");
        screens.render(screenLocation + "Fop");
        String screenOutString = writer.toString();
        assertNotNull("FopScreen failed to return the screen " + screenLocation + "Fop", screenOutString);
        if (!screenOutString.startsWith("<?xml")) {
            screenOutString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + screenOutString;
        }
        Reader reader = new StringReader(screenOutString);
        StreamSource src = new StreamSource(reader);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Fop fop = ApacheFopWorker.createFopInstance(out, "application/pdf");
            ApacheFopWorker.transform(src, null, fop);
        } catch (Exception e) {
            assertTrue("Unable to transform FO file : see https://localhost:8443/webtools/control/WebtoolsLayoutDemoPdf for more detail" , false);
        }
    }
}

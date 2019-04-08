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
package org.apache.ofbiz.base.util.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Properties;

import org.apache.ofbiz.base.test.GenericTestCaseBase;
import org.apache.ofbiz.base.util.UtilProperties;

public class UtilPropertiesTests extends GenericTestCaseBase {

    private final String country = "AU";
    private final String language = "en";
    private final Locale locale = new Locale(language, country);

    public UtilPropertiesTests(String name) {
        super(name);
    }

    /**
     * Old style xml:lang attribute value was of form en_AU. Test this
     * format still works.
     * @throws Exception
     */
    public void testReadXmlLangOldStyle() throws Exception {
        Properties result = xmlToProperties("_");
        assertNotNull(result);
        assertTrue(!result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Key Value", result.getProperty("PropertyKey"));
    }

    /**
     * New (and correct) style xml:lang value is en-AU.
     * Test it works.
      * @throws Exception
     */
    public void testReadXmlLangNewStyle() throws Exception {
        Properties result = xmlToProperties("-");
        assertNotNull(result);
        assertTrue(!result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Key Value", result.getProperty("PropertyKey"));

    }

    private Properties xmlToProperties(String separator) throws IOException {
        String xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<resource xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "          xsi:noNamespaceSchemaLocation=\"http://ofbiz.apache.org/dtds/ofbiz-properties.xsd\">\n" +
                "    <property key=\"PropertyKey\">\n" +
                "        <value xml:lang=\"" +
                language + separator + country +
                "\">Key Value</value>\n" +
                "    </property>\n" +
                "</resource>";
        InputStream in = new ByteArrayInputStream(new String(xmlData.getBytes(), Charset.forName("UTF-8")).getBytes());
        return UtilProperties.xmlToProperties(in, locale, null);
    }
}

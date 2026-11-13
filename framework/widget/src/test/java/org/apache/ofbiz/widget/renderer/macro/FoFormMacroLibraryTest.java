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
package org.apache.ofbiz.widget.renderer.macro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class FoFormMacroLibraryTest {

    @Test
    public void lookupFieldAcceptsDisabledFalse() throws Exception {
        assertLookupFieldRenders(" disabled=false");
    }

    @Test
    public void lookupFieldAcceptsDisabledTrue() throws Exception {
        assertLookupFieldRenders(" disabled=true");
    }

    @Test
    public void lookupFieldAllowsDisabledToBeOmitted() throws Exception {
        assertLookupFieldRenders("");
    }

    private void assertLookupFieldRenders(String disabledParameter) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_35);
        configuration.setDirectoryForTemplateLoading(new File("themes/common-theme/template/macro"));
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        String source = "<#import 'FoFormMacroLibrary.ftl' as fo>"
                + "<@fo.renderLookupField name='productId' formName='InventoryItemList'"
                + " fieldFormName='LookupProduct' conditionGroup='' ajaxEnabled=false"
                + disabledParameter + "/>";
        Template template = new Template("lookupField", new StringReader(source), configuration);
        StringWriter output = new StringWriter();
        template.process(Map.of(), output);
        // Lookup controls are deliberately omitted from PDF output.
        assertEquals("", output.toString());
    }
}

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
package org.apache.ofbiz.widget.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilXml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

public final class ModelMenuItemTest {

    private static final String MENU = "<menu name=\"Test\">"
            + "<menu-item name=\"main\" title=\"Main\">"
            + "<actions><set field=\"itemAction\" value=\"done\"/></actions>"
            + "<link target=\"main\"/>"
            + "</menu-item>"
            + "</menu>";

    @Test
    public void testMenuItemActionsAreLoaded() throws Exception {
        Element menuElement = UtilXml.readXmlDocument(MENU, false, true).getDocumentElement();
        ModelMenuItem item = new ModelMenu(menuElement, "test", null).getModelMenuItemByName("main");
        Assertions.assertEquals(1, item.getActions().size(), "Item actions should be read from the menu-item element");
        Map<String, Object> context = new HashMap<>();
        AbstractModelAction.runSubActions(item.getActions(), context);
        Assertions.assertEquals("done", context.get("itemAction"), "Item action should run");
    }
}

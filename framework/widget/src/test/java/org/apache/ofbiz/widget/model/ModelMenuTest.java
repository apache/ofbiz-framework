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

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilXml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

public final class ModelMenuTest {

    private static final String MENUS = "<menus>"
            + "<menu name=\"Parent\" selected-menuitem-context-field-name=\"headerItem\">"
            + "<menu-item name=\"main\" title=\"Main\"><link target=\"main\"/></menu-item>"
            + "<menu-item name=\"other\" title=\"Other\"><link target=\"other\"/></menu-item>"
            + "</menu>"
            + "<menu name=\"Child\" extends=\"Parent\">"
            + "<menu-item name=\"main\" title=\"Child main\"/>"
            + "</menu>"
            + "</menus>";

    private static ModelMenu childMenu() throws Exception {
        Element rootElement = UtilXml.readXmlDocument(MENUS, false, true).getDocumentElement();
        return new ModelMenu(UtilXml.childElementList(rootElement, "menu").get(1), "test", null);
    }

    @Test
    public void testInheritedMenuItemKeepsItsName() throws Exception {
        ModelMenuItem other = childMenu().getModelMenuItemByName("other");
        Assertions.assertNotNull(other, "Inherited item should keep its name");
        Assertions.assertTrue(other.isSelected(Map.of("headerItem", "other")), "Inherited item should be selectable");
    }

    @Test
    public void testChildMenuItemOverridesInheritedItem() throws Exception {
        List<ModelMenuItem> items = childMenu().getMenuItemList();
        Assertions.assertEquals(2, items.size(), "Child item should replace the inherited one");
        Assertions.assertEquals("main", items.get(0).getName(), "Overridden item should keep its position");
        Assertions.assertEquals("Child main", items.get(0).getTitle().getOriginal(), "Child title should win");
    }
}

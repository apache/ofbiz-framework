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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.JSONConverters;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class ModelFormTest {
    private HashMap<String, Object> context;
    private Delegator delegator;

    @BeforeEach
    public void setUp() throws GenericEntityException {
        delegator = Mockito.mock(Delegator.class);
        GenericValue genericValue = Mockito.mock(GenericValue.class);
        List<GenericValue> list = new ArrayList<>();
        list.add(genericValue);
        when(delegator.findList(any(), any(), any(), any(), any(), any(), Mockito.anyBoolean()))
                .thenReturn(list);
        when(delegator.getDelegator()).thenReturn(delegator);
        context = new HashMap<>();
        context.put("delegator", delegator);
    }

    @Test
    public void testCreateUpdateAreaFromJWTAreaValues() {
        context.put(CommonWidgetModels.JWT_CALLBACK, JWTManager.createJwt(delegator,
                Map.of("areaId", "myAreaId",
                        "areaTarget", "myAreaTarget")));
        ModelForm.UpdateArea updateArea = ModelForm.UpdateArea.fromJwtToken(context);
        Assertions.assertEquals(updateArea.getAreaId(), "myAreaId", "areaId not correct");
        Assertions.assertEquals(updateArea.getAreaTarget(), "myAreaTarget", "areaTarget not correct");
    }

    @Test
    public void testCreateUpdateAreaFromJWTWithParametersMapString() throws ConversionException {
        JSONConverters.MapToJSON converter = new JSONConverters.MapToJSON();
        context.put(CommonWidgetModels.JWT_CALLBACK, JWTManager.createJwt(delegator,
                Map.of("areaId", "myAreaId",
                "areaTarget", "myAreaTarget",
                "parameters", converter.convert(Map.of("entry1", "value1")).toString())));
        ModelForm.UpdateArea updateArea = ModelForm.UpdateArea.fromJwtToken(context);
        CommonWidgetModels.Parameter parameter = getParameterOnly(updateArea);
        Assertions.assertEquals(parameter.getName(), "entry1", "Parameters key name isn't the same");
        Assertions.assertEquals(parameter.getValue().toString(), "value1", "Parameters value isn't the same");
    }

    @Test
    public void testCreateUpdateAreaFromJWTWithParametersMapList() throws ConversionException {
        JSONConverters.MapToJSON converter = new JSONConverters.MapToJSON();
        context.put(CommonWidgetModels.JWT_CALLBACK, JWTManager.createJwt(delegator,
                Map.of("areaId", "myAreaId",
                "areaTarget", "myAreaTarget",
                "parameters", converter.convert(UtilMisc.toMap("entry1", List.of("1", "2"))).toString())));
        ModelForm.UpdateArea updateArea = ModelForm.UpdateArea.fromJwtToken(context);
        CommonWidgetModels.Parameter parameter = getParameterOnly(updateArea);
        Assertions.assertEquals(parameter.getName(), "entry1", "Parameters key name isn't the same");
        Assertions.assertEquals(parameter.getValue().toString(), "[1, 2]", "Parameters value isn't the same");
    }

    @Test
    public void testGridExtendingMissingParentInSameFileLoadsWithoutParent() throws Exception {
        ModelGrid grid = new ModelGrid(widgetExtendingMissingParent("grid"), "test", null, null, null);
        Assertions.assertNull(grid.getParentModelForm(), "Grid should have no parent");
    }

    @Test
    public void testFormExtendingMissingParentInSameFileLoadsWithoutParent() throws Exception {
        ModelSingleForm form = new ModelSingleForm(widgetExtendingMissingParent("form"), "test", null, null, null);
        Assertions.assertNull(form.getParentModelForm(), "Form should have no parent");
    }

    private static Element widgetExtendingMissingParent(String tagName) throws Exception {
        String xml = "<forms><" + tagName + " name=\"Child\" extends=\"Missing\"/></forms>";
        return UtilXml.firstChildElement(UtilXml.readXmlDocument(xml, false, true).getDocumentElement(), tagName);
    }

    @Test
    public void testInheritedFieldGroupRendersWithChildForm() throws Exception {
        String xml = "<forms>"
                + "<form name=\"Parent\" type=\"single\">"
                + "<field name=\"a\"><display/></field>"
                + "<sort-order><field-group title=\"Group\"><sort-field name=\"a\"/></field-group></sort-order>"
                + "</form>"
                + "<form name=\"Child\" type=\"single\" extends=\"Parent\"/>"
                + "</forms>";
        Element rootElement = UtilXml.readXmlDocument(xml, false, true).getDocumentElement();
        ModelSingleForm child = new ModelSingleForm(UtilXml.childElementList(rootElement, "form").get(1), "test",
                null, null, null);
        ModelForm parent = child.getParentModelForm();
        Assertions.assertNotNull(parent, "Child should have a parent");
        ModelForm.FieldGroup fieldGroup = (ModelForm.FieldGroup) child.getFieldGroupMap().get("a");
        Assertions.assertTrue(child.getFieldGroupList().contains(fieldGroup), "Field group should be in the child list");

        FormStringRenderer renderer = Mockito.mock(FormStringRenderer.class);
        StringBuilder writer = new StringBuilder();
        fieldGroup.renderStartString(writer, context, child, renderer);
        fieldGroup.renderEndString(writer, context, child, renderer);
        verify(renderer).renderFormatSingleWrapperOpen(same(writer), same(context), same(child));
        verify(renderer).renderFormatSingleWrapperClose(same(writer), same(context), same(child));
        verify(renderer, never()).renderFormatSingleWrapperOpen(any(), any(), same(parent));
    }

    private static CommonWidgetModels.Parameter getParameterOnly(ModelForm.UpdateArea updateArea) {
        Assertions.assertNotNull(updateArea.getParameterList());
        Assertions.assertEquals(updateArea.getParameterList().size(), 1, "Parameter size should be one");
        return updateArea.getParameterList().get(0);
    }

}

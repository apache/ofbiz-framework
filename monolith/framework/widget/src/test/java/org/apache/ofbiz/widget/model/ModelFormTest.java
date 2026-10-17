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
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
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

    private static CommonWidgetModels.Parameter getParameterOnly(ModelForm.UpdateArea updateArea) {
        Assertions.assertNotNull(updateArea.getParameterList());
        Assertions.assertEquals(updateArea.getParameterList().size(), 1, "Parameter size should be one");
        return updateArea.getParameterList().get(0);
    }

}

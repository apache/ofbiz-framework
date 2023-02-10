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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.JSONConverters;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.webapp.control.JWTManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ModelFormTest {
    private HashMap<String, Object> context;
    private Delegator delegator;

    @Before
    public void setUp() throws GenericEntityException {
        context = new HashMap<>();
        delegator = Mockito.mock(Delegator.class);
        when(delegator.findList(any(), any(), any(), any(), any(), Mockito.anyBoolean()))
                .thenReturn(new ArrayList<>());
    }

    @Test
    public void testCreateUpdateAreaFromJWTAreaValues() {
        context.put(CommonWidgetModels.JWT_CALLBACK, JWTManager.createJwt(delegator,
                Map.of("areaId", "myAreaId",
                        "areaTarget", "myAreaTarget")));
        ModelForm.UpdateArea updateArea = ModelForm.UpdateArea.fromJwtToken(context);
        Assert.assertEquals("areaId not correct", updateArea.getAreaId(), "myAreaId");
        Assert.assertEquals("areaTarget not correct", updateArea.getAreaTarget(), "myAreaTarget");
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
        Assert.assertEquals("Parameters key name isn't the same", parameter.getName(), "entry1");
        Assert.assertEquals("Parameters value isn't the same", parameter.getValue().toString(), "value1");
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
        Assert.assertEquals("Parameters key name isn't the same", parameter.getName(), "entry1");
        Assert.assertEquals("Parameters value isn't the same", parameter.getValue().toString(), "[1, 2]");
    }

    private static CommonWidgetModels.Parameter getParameterOnly(ModelForm.UpdateArea updateArea) {
        Assert.assertNotNull(updateArea.getParameterList());
        Assert.assertEquals("Parameter size should be one", updateArea.getParameterList().size(), 1);
        return updateArea.getParameterList().get(0);
    }

}

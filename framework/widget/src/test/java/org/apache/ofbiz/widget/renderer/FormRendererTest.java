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
package org.apache.ofbiz.widget.renderer;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelFormFieldBuilder;
import org.junit.Before;
import org.junit.Test;

public class FormRendererTest {
    private ModelForm model;
    private FormRenderer renderer;
    private ArrayList<ModelFormField> fields;
    private HashMap<String, Object> context;
    private HashSet<String> useWhenFields;

    @Before
    public void setUp() throws Exception {
        model = mock(ModelForm.class);
        fields = new ArrayList<>();
        when(model.getFieldList()).thenReturn(fields);
        renderer = new FormRenderer(model, null);
        context = new HashMap<>();
        useWhenFields = new HashSet<>();
        when(model.getUseWhenFields()).thenReturn(useWhenFields);
    }

    @Test
    public void fieldsToRenderBasic() {
        ModelFormField a = ModelFormField.from(new ModelFormFieldBuilder().setName("A"));
        fields.add(a);
        ModelFormField b = ModelFormField.from(new ModelFormFieldBuilder().setName("B"));
        fields.add(b);
        assertThat(renderer.getUsedFields(context), containsInAnyOrder(a, b));
    }

    @Test
    public void fieldsToRenderDuplicates() {
        ModelFormField a1 = ModelFormField.from(new ModelFormFieldBuilder().setName("A"));
        fields.add(a1);
        ModelFormField b = ModelFormField.from(new ModelFormFieldBuilder().setName("B"));
        fields.add(b);
        ModelFormField a2 = ModelFormField.from(new ModelFormFieldBuilder().setName("A"));
        fields.add(a2);
        assertThat(renderer.getUsedFields(context), containsInAnyOrder(a1, a2, b));
    }

    @Test
    public void fieldsToRenderBasicUseWhen() {
        ModelFormField a1 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("true"));
        fields.add(a1);
        useWhenFields.add(a1.getName());
        ModelFormField a2 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("false"));
        fields.add(a2);
        useWhenFields.add(a2.getName());
        assertThat(renderer.getUsedFields(context), containsInAnyOrder(a1, a2));
    }

    @Test
    public void fieldsToRenderDuplicatesUseWhen() {
        ModelFormField a1 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("true"));
        fields.add(a1);
        useWhenFields.add(a1.getName());
        ModelFormField a2 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("false"));
        fields.add(a2);
        useWhenFields.add(a2.getName());
        ModelFormField a3 = ModelFormField.from(new ModelFormFieldBuilder().setName("A").setUseWhen("true"));
        fields.add(a3);
        useWhenFields.add(a3.getName());
        assertThat(renderer.getUsedFields(context), containsInAnyOrder(a1, a2));
    }

}

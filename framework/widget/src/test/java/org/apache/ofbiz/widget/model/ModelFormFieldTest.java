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

import static org.apache.ofbiz.widget.model.ModelFormField.from;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class ModelFormFieldTest {
    private HashMap<String, Object> context;

    @Before
    public void setUp() throws Exception {
        context = new HashMap<>();
    }

    /**
     * Filter a list of fields with {@link ModelFormField#usedFields} predicate.
     * <p>
     * This is useful since Hamcrest does not provide any Stream matchers.
     *
     * @param fields  the fields to filter
     * @return a list a filtered fields.
     */
    List<ModelFormField> getUsedField(ModelFormField... fields) {
        return Arrays.stream(fields)
                .filter(ModelFormField.usedFields(context))
                .collect(Collectors.toList());
    }

    @Test
    public void fieldsToRenderBasic() {
        ModelFormField fA = from(b -> b.setName("A"));
        ModelFormField fB = from(b -> b.setName("B"));
        assertThat(getUsedField(fA, fB), containsInAnyOrder(fA, fB));
    }

    @Test
    public void fieldsToRenderDuplicates() {
        ModelFormField fA0 = from(b -> b.setName("A"));
        ModelFormField fB = from(b -> b.setName("B"));
        ModelFormField fA1 = from(b -> b.setName("A"));
        assertThat(getUsedField(fA0, fB, fA1), containsInAnyOrder(fA0, fA1, fB));
    }

    @Test
    public void fieldsToRenderBasicUseWhen() {
        ModelFormField fA0 = from(b -> b.setName("A").setUseWhen("true"));
        ModelFormField fA1 = from(b -> b.setName("A").setUseWhen("false"));
        assertThat(getUsedField(fA0, fA1), containsInAnyOrder(fA0, fA1));
    }

    @Test
    public void fieldsToRenderDuplicatesUseWhen() {
        ModelFormField fA0 = from(b -> b.setName("A").setUseWhen("true"));
        ModelFormField fA1 = from(b -> b.setName("A").setUseWhen("false"));
        ModelFormField fA2 = from(b -> b.setName("A").setUseWhen("true"));
        assertThat(getUsedField(fA0, fA1, fA2), containsInAnyOrder(fA0, fA1));
    }
}

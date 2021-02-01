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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Element;

public class ModelFormFieldTest {
    private HashMap<String, Object> context;

    @Before
    public void setUp() {
        context = new HashMap<>();
    }

    /**
     * Filter a list of fields with {@link ModelFormField#usedFields} predicate.
     * <p>
     * This is useful since Hamcrest does not provide any Stream matchers.
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

    @Test
    public void fieldUsesFlexibleParameterName() {
        ModelFormField field = from(b -> b.setParameterName("${prefix}Param"));
        assertThat(field.getParameterName(ImmutableMap.of("prefix", "P1")), equalTo("P1Param"));
        assertThat(field.getParameterName(ImmutableMap.of("prefix", "P2")), equalTo("P2Param"));
    }

    @Test
    public void dropDownFieldUsesFlexibleParameterNameOther() {
        ModelFormField field = from(b -> b.setParameterName("${prefix}Param"));
        ModelFormField.DropDownField dropDownField = new ModelFormField.DropDownField(field);
        assertThat(dropDownField.getParameterNameOther(ImmutableMap.of("prefix", "P1")), equalTo("P1Param_OTHER"));
        assertThat(dropDownField.getParameterNameOther(ImmutableMap.of("prefix", "P2")), equalTo("P2Param_OTHER"));
    }

    @Test
    public void fieldUsesFlexibleContainerId() {
        ModelFormField field = from(b -> b.setIdName("${prefix}IdValue"));
        assertThat(field.getCurrentContainerId(ImmutableMap.of("prefix", "P1")), equalTo("P1IdValue"));
        assertThat(field.getCurrentContainerId(ImmutableMap.of("prefix", "P2")), equalTo("P2IdValue"));
    }

    /**
     * Ensures behaviour of deprecated method LookupField#getTargetParameterList is maintained while the underlying
     * property type is changed from String to FlexibleStringExpander.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void lookupFieldDeprecatedMethodTreatsTargetParameterAsString() {
        Element element = Mockito.mock(Element.class);
        when(element.getTagName()).thenReturn("lookup");
        when(element.getAttribute("maxlength")).thenReturn("1");
        when(element.getAttribute("size")).thenReturn("1");
        when(element.getAttribute("target-parameter")).thenReturn("${prefix}TargetParam, ${key1}");

        ModelFormField field = from(b -> b.setName("lookup-field"));
        ModelFormField.LookupField lookupField = new ModelFormField.LookupField(element, field);

        assertThat(lookupField.getTargetParameterList(), Matchers.contains("${prefix}TargetParam", "${key1}"));
    }

    @Test
    public void lookupFieldUsesFlexibleTargetParameters() {
        Element element = Mockito.mock(Element.class);
        when(element.getTagName()).thenReturn("lookup");
        when(element.getAttribute("maxlength")).thenReturn("1");
        when(element.getAttribute("size")).thenReturn("1");
        when(element.getAttribute("target-parameter")).thenReturn("${prefix}TargetParam");

        ModelFormField field = from(b -> b.setName("lookup-field"));
        ModelFormField.LookupField lookupField = new ModelFormField.LookupField(element, field);

        assertThat(lookupField.getTargetParameterList(ImmutableMap.of("prefix", "P1")),
                Matchers.contains("P1TargetParam"));
    }

    @Test
    public void lookupFieldEvaluatesExpressionBeforeSplitting() {
        Element element = Mockito.mock(Element.class);
        when(element.getTagName()).thenReturn("lookup");
        when(element.getAttribute("maxlength")).thenReturn("1");
        when(element.getAttribute("size")).thenReturn("1");
        when(element.getAttribute("target-parameter")).thenReturn("${prefix}TargetParam, ${key1}");

        ModelFormField field = from(b -> b.setName("lookup-field"));
        ModelFormField.LookupField lookupField = new ModelFormField.LookupField(element, field);

        final List<String> targetParameterList = lookupField.getTargetParameterList(
                ImmutableMap.of("prefix", "P1", "key1", "AA,BB , CC"));
        assertThat(targetParameterList, Matchers.contains("P1TargetParam", "AA", "BB", "CC"));
    }
}
